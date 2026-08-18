package cz.aron.search;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;

import cz.aron.domain.types.TypesHolder;
import cz.aron.mapper.KryoSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.RelationRepository;
import cz.aron.service.ApuService;
import cz.aron.service.IdService;

/**
 * Bootstraps the search schema after startup: compares the fingerprint of the
 * configuration that determines the indexed documents ({@link #currentSchemaCrc()})
 * with the value stored in the schema's own metadata
 * ({@link SearchIndex#storedSchemaCrc()}) and rebuilds + reindexes when they
 * differ. The marker lives and dies with the schema it describes, so no external
 * state (the former ./lastConfigCrc.txt) can drift.
 */
@Component
public class SearchIndexManager implements ApplicationListener<ApplicationReadyEvent>, Ordered {

	/** Startup-listener order: runs before anything that needs the search schema. */
	public static final int STARTUP_ORDER = 0;

	private static final Logger log = LoggerFactory.getLogger(SearchIndexManager.class);

	private final IndexingService indexingService;

	private final ApuEntityRepository apuEntityRepository;

	private final RelationRepository relationRepository;

	private final ApuService apuService;

	private final TypesHolder typesHolder;

	private final IdService idService;

	private final ContentLocale contentLocale;

	// self-reference through the Spring proxy so @Transactional on batch methods is honored
	// (calling them directly from reindexAll would be self-invocation and bypass the proxy)
	@Lazy
	@Autowired
	private SearchIndexManager self;

	public SearchIndexManager(IndexingService indexingService, ApuEntityRepository apuEntityRepository,
			ApuService apuService, TypesHolder typesHolder, RelationRepository relationRepository,
			IdService idService, ContentLocale contentLocale) {
		this.contentLocale = contentLocale;
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.typesHolder = typesHolder;
		this.relationRepository = relationRepository;
		this.idService = idService;
	}

	@Override
	public int getOrder() {
		return STARTUP_ORDER;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {

		// seed application-side id counters from current DB maxima before any import can run
		idService.initMetadataIds();
		idService.initDaoIds();

		Long currentCrc = currentSchemaCrc();
		Long storedCrc = indexingService.storedSchemaCrc();
		if (!currentCrc.equals(storedCrc)) {
			log.info("Search configuration changed (stored CRC {}, current {}) - rebuilding the search schema.",
					storedCrc, currentCrc);
			indexingService.dropSchema();
			indexingService.createSchema();
			reindexAll();
			indexingService.storeSchemaCrc(currentCrc);
		} else {
			indexingService.createSchema();
		}
		log.info("Search index bootstrap completed.");
	}

	Long currentSchemaCrc() {
		return schemaCrc(typesHolder.getCurrentIndexedFieldsCrc(), contentLocale.getLanguageTag());
	}

	/**
	 * Fingerprint of everything that decides the content of an indexed document:
	 * the types.yaml indexed fields and the search locale, whose collation key is
	 * baked into {@code nameSort}. A change of either invalidates the indexed
	 * data, so both have to be part of one marker.
	 */
	static long schemaCrc(long indexedFieldsCrc, String languageTag) {
		var crc = new CRC32();
		crc.update((indexedFieldsCrc + "|" + languageTag).getBytes(StandardCharsets.UTF_8));
		return crc.getValue();
	}

	private void reindexAll() {
		log.info("Reindexing APU");
		boolean reindexed = false;
		long after = 0;
		do {
			var ids = apuEntityRepository.findIds(after, 10000);
			if (ids.isEmpty()) {
				reindexed = true;
			} else {
				Iterables.partition(ids, 1000).forEach(partition -> self.reindexApuBatch(partition));
				after = ids.getLast();
			}
		} while (!reindexed);

		log.info("Reindexing Relations");
		reindexed = false;
		after = 0;
		do {
			var ids = relationRepository.findIds(after, 10000);
			if (ids.isEmpty()) {
				reindexed = true;
			} else {
				Iterables.partition(ids, 1000).forEach(partition -> {
					var rels = relationRepository.findAllById(partition);
					if (!rels.isEmpty()) {
						indexingService.indexRels(rels);
					}
				});
				after = ids.getLast();
			}
		} while (!reindexed);
	}

	/**
	 * Loads, label-fills and indexes one batch of APUs inside a read-only transaction so that
	 * lazy associations (e.g. {@code digitalObjects}) can be initialized during
	 * {@link ApuDocumentBuilder#build} — the startup reindex path has no open session otherwise
	 * ({@code spring.jpa.open-in-view=false}). Must be invoked through the Spring proxy ({@link #self}).
	 */
	@Transactional(readOnly = true)
	public void reindexApuBatch(List<Long> ids) {
		var entities = apuEntityRepository.findAllByIdIn(ids);
		if (!entities.isEmpty()) {
			var apuRefLabels = apuService.resolveApuRefLabels(entities);
			KryoSerializer.doWithKryo(kryo -> {
				indexingService.indexApus(kryo, entities, apuRefLabels);
				return null;
			});
		}
	}

}
