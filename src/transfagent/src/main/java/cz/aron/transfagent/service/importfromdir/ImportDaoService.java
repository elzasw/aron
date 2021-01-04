package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.domain.DaoFile;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.service.StorageService;

@Service
public class ImportDaoService extends ImportDirProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ImportDaoService.class);
	
	private final StorageService storageService;
	
	private final DaoFileRepository daoFileRepository;
	
	private final TransactionTemplate transactionTemplate;
	
	private final TransformService transformService;
	
	final private String DAO_DIR = "dao";
	
    public ImportDaoService(StorageService storageService, DaoFileRepository daoFileRepository,
            TransactionTemplate transactionTemplate, TransformService transformService) {
        this.storageService = storageService;
        this.daoFileRepository = daoFileRepository;
        this.transactionTemplate = transactionTemplate;
        this.transformService = transformService;
    }
    
	@Override
	protected Path getInputDir() {
		return storageService.getInputPath().resolve(DAO_DIR);
	}        
	
    @Override
	public boolean processDirectory(Path dir) {
		
		var uuidStr = dir.getFileName().toString();
		var uuid = UUID.fromString(dir.getFileName().toString());
		
		var daoFileOpt = daoFileRepository.findByUuid(uuid);
		if (!daoFileOpt.isPresent()) {
			try {
				storageService.moveToErrorDir(dir);
			} catch (IOException e) {
				log.error("Fail to move {} to error dir",dir,e);
				throw new UncheckedIOException(e);
			}
		}
		
		var daoFile = daoFileOpt.get();		
		var daoUuidXml = dir.resolve("dao-"+uuidStr+".xml");
		if (Files.isRegularFile(daoUuidXml)) {
			importCompleteDao(daoFile,dir);
		} else {
			transformAndImportDao(daoFile,dir);
		}
		return true;
	}

	private void importCompleteDao(DaoFile daoFile, Path path) {
		
		Path dataPath;
		try {
			dataPath = storageService.moveToDataDir(path);
		} catch (IOException e) {
			log.error("Fail to move {} to data dir.",path,e);
			throw new UncheckedIOException(e);
		}
		
		transactionTemplate.execute(t->{			
			daoFile.setDataDir(dataPath.toString());
			daoFile.setState(DaoState.READY);
			daoFile.setTransferred(false);
			daoFileRepository.save(daoFile);
			return null;
		});
		log.info("Imported dao {}", path.getFileName());
	}
	
	private void transformAndImportDao(DaoFile daoFile, Path path) {	    
	    try {
            transformService.transform(path);
        } catch (Exception e) {
            log.error("Fail to move {} to data dir.",path,e);
            throw new IllegalStateException(e);
        }
	    
	    importCompleteDao(daoFile,path);		
	}
	
}
