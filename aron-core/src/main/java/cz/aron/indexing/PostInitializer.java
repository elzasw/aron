package cz.aron.indexing;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import cz.aron.repository.ApuEntityRepository;
import cz.aron.service.ApuService;

@Component
public class PostInitializer  implements ApplicationListener<ApplicationReadyEvent>  {
	
	private final IndexingService indexingService;
	
	private final ApuEntityRepository apuEntityRepository;
	
	private final ApuService apuService;
	
	public PostInitializer(IndexingService indexingService, ApuEntityRepository apuEntityRepository, ApuService apuService) {
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// TODO split to batch of limited size
		var ids = apuEntityRepository.findAllIds();		
		var entities = apuEntityRepository.findAllByIdIn(ids);
		if (!entities.isEmpty()) {
			apuService.fillTargetLabelsToApuRefs(entities);
			indexingService.indexApus(entities);
		}
	}

	
	
}
