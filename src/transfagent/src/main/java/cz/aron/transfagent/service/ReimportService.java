package cz.aron.transfagent.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;

@Service
public class ReimportService implements ImportProcessor {
	
	private final static Logger log = LoggerFactory.getLogger(ReimportService.class);  
	
	@Autowired
	FileImportService importService;
	
	@Autowired
	ApuSourceRepository apuSourceRepository;
	
	@Autowired
	private TransactionTemplate transactionTemplate;
	
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

	private Object importTrans(ImportContext ic) {
		// check from DB reimport request
		List<ApuSource> reimportRequests = apuSourceRepository.findByReimport(true);
		apuSourceLabel:
		for(var apuSource: reimportRequests) {
			log.debug("Reimporting apuSource: {}", apuSource.getId());
			for(var proc: processors) {
				if(proc.reimport(apuSource)) {
					log.info("Reimported apuSource: {}", apuSource.getId());
					apuSource.setReimport(false);
					apuSourceRepository.save(apuSource);
					continue apuSourceLabel;
				}
			}
			log.error("Item cannot be reimported: {}", apuSource.getId());
			ic.setFailed(true);
		}
		return null;
	}

}
