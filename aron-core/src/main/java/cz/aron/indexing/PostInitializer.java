package cz.aron.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;

import cz.aron.domain.types.TypesHolder;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.RelationRepository;
import cz.aron.service.ApuService;

@Component
public class PostInitializer  implements ApplicationListener<ApplicationReadyEvent>  {
	
	private static final Logger log = LoggerFactory.getLogger(PostInitializer.class);
	
	private final IndexingService indexingService;
	
	private final ApuEntityRepository apuEntityRepository;
	
	private final RelationRepository relationRepository;
	
	private final ApuService apuService;
	
	private final TypesHolder typesHolder;
	
	public PostInitializer(IndexingService indexingService, ApuEntityRepository apuEntityRepository,
			ApuService apuService, TypesHolder typesHolder, RelationRepository relationRepository) {
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.typesHolder = typesHolder;
		this.relationRepository = relationRepository;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {

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
				Iterables.partition(ids, 1000).forEach(partition -> {
					var entities = apuEntityRepository.findAllByIdIn(partition);
					if (!entities.isEmpty()) {
						apuService.fillTargetLabelsToApuRefs(entities);
						indexingService.indexApus(entities);
					}
				});
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

}
