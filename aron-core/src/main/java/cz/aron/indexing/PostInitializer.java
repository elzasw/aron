package cz.aron.indexing;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import cz.aron.repository.ApuEntityRepository;

@Component
public class PostInitializer  implements ApplicationListener<ApplicationReadyEvent>  {
	
	private final IndexingService indexingService;
	
	private final ApuEntityRepository apuEntityRepository;
	
	public PostInitializer(IndexingService indexingService, ApuEntityRepository apuEntityRepository) {
		this.indexingService = indexingService;
		this.apuEntityRepository = apuEntityRepository;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// TODO split to batch of limited size
		var ids = apuEntityRepository.findAllIds();		
		var entities = apuEntityRepository.findAllByIdIn(ids);		
		indexingService.indexApus(entities);		
	}

	
	
}
