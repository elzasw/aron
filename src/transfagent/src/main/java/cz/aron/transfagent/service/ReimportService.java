package cz.aron.transfagent.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;

@Service
public class ReimportService implements ImportProcessor {
	
	private final static Logger log = LoggerFactory.getLogger(ReimportService.class);  
	
	@Autowired
	FileImportService importService;
	
	@Autowired
	ApuSourceRepository apuSourceRepository;
	
	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	private CoreQueueRepository coreQueueRepository;
	
	List<ReimportProcessor> processors = new ArrayList<>();
	
	@PostConstruct
	void init() {
		importService.registerImportProcessor(this);
	}
	
	public void registerReimportProcessor(ReimportProcessor p) {
		processors.add(p);
	}

	@Override
	public void importData(final ImportContext ic) {
		transactionTemplate.execute(t -> importTrans(ic));		
	}
	
	@Override
	public int getPriority() { return -100; }

	private Object importTrans(ImportContext ic) {
		// check from DB reimport request
		List<ApuSource> reimportRequests = apuSourceRepository.findByReimport(true);
		apuSourceLabel:
		for(var apuSource: reimportRequests) {
			log.debug("Reimporting apuSource: {}", apuSource.getId());
			for(var proc: processors) {
			    var result = proc.reimport(apuSource);
				if(result==Result.REIMPORTED||result==Result.NOCHANGES) {
					log.info("Reimported apuSource: {}", apuSource.getId());
					apuSource.setReimport(false);
					apuSource.setDateImported(ZonedDateTime.now());
					apuSourceRepository.save(apuSource);
					
					if(result==Result.REIMPORTED) {
                        // send to Core
					    CoreQueue cq = new CoreQueue();
					    cq.setApuSource(apuSource);
					    coreQueueRepository.save(cq);
					}
					
					continue apuSourceLabel;
				} else 
				if(result==Result.FAILED) {
				    log.error("Reimport failed");
				    break;
				}
			}
			log.error("Item cannot be reimported: {}", apuSource.getId());
			ic.setFailed(true);
			break;
		}
		return null;
	}

}
