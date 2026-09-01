package cz.aron.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.domain.ApuEntity;
import cz.aron.mapper.KryoSerializer;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.IndexDirtyRepository;
import cz.aron.service.ApuService;

/**
 * Brings the search index up to date with the database after an import or a
 * withdrawal committed. The import itself never writes the index: inside its
 * transaction it fills the <b>dirty set</b> ({@code index_dirty}) with the uuids
 * of every document that must be re-derived - its own records, the previous
 * delivery's records, and every record whose document carries a label this
 * import changed. A rolled-back import therefore leaves nothing behind, and a
 * crash after the commit leaves exactly the work still to do, which the next
 * start picks up ({@link SearchIndexManager}).
 * <p>
 * One rule consumes the set: load the rows for a batch of distinct uuids,
 * <b>index those that exist</b> (and are flagged {@code indexed}), <b>delete the
 * documents of those that do not</b>. Existence is read from the database at
 * consume time, so "written then omitted", duplicates and ordering all coalesce
 * away, and each document is consistent on its own at every moment - a
 * re-delivered source stays searchable throughout.
 */
@Component
public class IndexSynchronizer {

	private static final Logger log = LoggerFactory.getLogger(IndexSynchronizer.class);

	private static final int BATCH_SIZE = 1000;

	private final IndexingService indexingService;

	private final ApuEntityRepository apuEntityRepository;

	private final IndexDirtyRepository indexDirtyRepository;

	private final ApuService apuService;

	// self-reference through the Spring proxy so @Transactional on the batch methods is honored
	@Lazy
	@Autowired
	private IndexSynchronizer self;

	public IndexSynchronizer(IndexingService indexingService, ApuEntityRepository apuEntityRepository,
			IndexDirtyRepository indexDirtyRepository, ApuService apuService) {
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
		this.indexDirtyRepository = indexDirtyRepository;
		this.apuService = apuService;
	}

	/** Whether anything is pending - lets the startup skip the log line when there is nothing to do. */
	public boolean hasPendingWork() {
		return indexDirtyRepository.count() > 0;
	}

	/** Works the dirty set off, batch by batch. Idempotent. */
	public void synchronize() {
		long start = System.currentTimeMillis();
		int processed = 0;
		while (true) {
			var uuids = indexDirtyRepository.findDistinctUuids(BATCH_SIZE);
			if (uuids.isEmpty()) {
				break;
			}
			self.syncBatch(uuids);
			processed += uuids.size();
		}
		if (processed > 0) {
			log.info("Search index synchronized: {} records, {} ms", processed, System.currentTimeMillis() - start);
		}
	}

	/**
	 * Re-derives the index state of one batch and removes its dirty rows in the
	 * same transaction, so an entry goes only with a write that succeeded. The
	 * transaction also gives the lazy associations {@link ApuDocumentBuilder}
	 * reads a session ({@code spring.jpa.open-in-view=false}). Invoke through the
	 * proxy.
	 */
	@Transactional
	public void syncBatch(List<UUID> uuids) {
		var entities = apuEntityRepository.findAllByUuidIn(uuids);
		if (!entities.isEmpty()) {
			var apuRefLabels = apuService.resolveApuRefLabels(entities);
			KryoSerializer.doWithKryo(kryo -> {
				indexingService.indexApus(kryo, entities, apuRefLabels);
				return null;
			});
		}
		// a uuid with no row - or with an indexed=false row, which gets no document - is deleted;
		// deleting an id the index does not hold is a no-op, so no lookup is needed
		var written = entities.stream().filter(ApuEntity::isIndexed).map(ApuEntity::getUuid).toList();
		var gone = new ArrayList<>(uuids);
		gone.removeAll(written);
		indexingService.deleteApus(gone.stream().map(UUID::toString).toList());
		indexDirtyRepository.deleteByUuids(uuids);
	}

	/**
	 * Loads, label-fills and indexes one batch of APUs by id inside a transaction -
	 * the full-rebuild path of {@link SearchIndexManager}, which iterates every row
	 * and needs no dirty set. Invoke through the proxy.
	 */
	@Transactional(readOnly = true)
	public void reindexBatch(List<Long> ids) {
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
