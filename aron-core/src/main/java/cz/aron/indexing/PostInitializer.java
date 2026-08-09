package cz.aron.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;

import cz.aron.domain.types.TypesHolder;
import cz.aron.mapper.KryoSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.RelationRepository;
import cz.aron.service.ApuService;
import cz.aron.service.IdService;

@Component
public class PostInitializer  implements ApplicationListener<ApplicationReadyEvent>  {
	
	private static final Logger log = LoggerFactory.getLogger(PostInitializer.class);
	
	private final IndexingService indexingService;
	
	private final ApuEntityRepository apuEntityRepository;
	
	private final RelationRepository relationRepository;
	
	private final ApuService apuService;

	private final TypesHolder typesHolder;

	private final IdService idService;

	// self-reference through the Spring proxy so @Transactional on batch methods is honored
	// (calling them directly from reindexAll would be self-invocation and bypass the proxy)
	@Lazy
	@Autowired
	private PostInitializer self;

	public PostInitializer(IndexingService indexingService, ApuEntityRepository apuEntityRepository,
			ApuService apuService, TypesHolder typesHolder, RelationRepository relationRepository,
			IdService idService) {
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.typesHolder = typesHolder;
		this.relationRepository = relationRepository;
		this.idService = idService;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {

		// seed application-side id counters from current DB maxima before any import can run
		idService.initMetadataIds();
		idService.initDaoIds();

		try {
			Path crcPath = Path.of("./lastConfigCrc.txt");
			Long previousCrc = null;
			if (Files.exists(crcPath)) {
				previousCrc = Long.valueOf(Files.readString(crcPath));
			}
			if (!typesHolder.getCurrentIndexedFieldsCrc().equals(previousCrc)) {
				indexingService.dropIndexes();
				indexingService.createIndexes();
				reindexAll();
				Files.writeString(crcPath, String.valueOf(typesHolder.getCurrentIndexedFieldsCrc()));
			} else {
				indexingService.createIndexes();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}				
		log.info("PostInitializer completed.");
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
	 * {@link IndexingService#convert} — the startup reindex path has no open session otherwise
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
