package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.FindingAidAuthor;
import cz.aron.peva2.wsdl.GetFindingAidAuthorResponse;
import cz.aron.peva2.wsdl.ListFindingAidAuthorRequest;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

public class Peva2ImportFindingAidAuthor extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFindingAidAuthor.class);
	
	public Peva2ImportFindingAidAuthor(PEvA2Connection peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService,
			@Value("${peva2.importFindingAidAuthor:false}") boolean active) {
		super("FINDINGAID_AUTHOR", peva2, propertyRepository, config, tt, storageService, active);
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial,
			Peva2CodeListProvider codeListProvider) {
		String searchAfter = searchAfterInitial;
		int numUpdated = 0;
		while (true) {
			var lfaaReq = new ListFindingAidAuthorRequest();
			lfaaReq.setSize(config.getBatchSize());
			lfaaReq.setUpdatedAfter(updateAfter);
			lfaaReq.setSearchAfter(searchAfter);
			var lfaaResp = peva2.getPeva().listFindingAidAuthor(lfaaReq);
			searchAfter = lfaaResp.getSearchAfter();
			long count = lfaaResp.getFindingAidAuthors().getFindingAidAuthor().size();
			log.info("Downloaded {} finding aid authors to update after {}", count, updateAfter);
			if (count == 0) {
				break;
			}
			
			var facInputDir = storageService.getInputPath().resolve("faauthors");
			try {
				Files.createDirectories(facInputDir);
			} catch (IOException e2) {
				throw new UncheckedIOException(e2);
			}

			int numUpdatedFromBatch = 0;
			try {
				patchFindingAidAuthorsBatch(lfaaResp.getFindingAidAuthors().getFindingAidAuthor(),
						codeListProvider.getCodeLists());
				numUpdatedFromBatch += lfaaResp.getFindingAidAuthors().getFindingAidAuthor().size();
			} catch (Exception e) {
				log.error("Fail to update finding aid authors batch", e);
				// zkusim to po jednom
				for (var findingAidCopy : lfaaResp.getFindingAidAuthors().getFindingAidAuthor()) {
					try {
						patchFindingAidAuthorsBatch(Collections.singletonList(findingAidCopy), codeListProvider.getCodeLists());
						numUpdatedFromBatch++;
					} catch (Exception e1) {
						log.error("Fail to update finding aid author {}", findingAidCopy.getId(), e1);
					}
				}
			}

			storeSearchAfter(searchAfter);
			numUpdated += numUpdatedFromBatch;
			log.info("Updated {}/{} finding aid authors", numUpdated, lfaaResp.getCount());
		}
		log.info("Finding aid authors synchronized");
		return numUpdated;
	}
	
	private void patchFindingAidAuthorsBatch(List<FindingAidAuthor> findingAidAuthors, Peva2CodeLists codeLists) {
		var faaInputDir = storageService.getInputPath().resolve("faauthors");
		for (var findingAidAuthor : findingAidAuthors) {
			var gfacr = new GetFindingAidAuthorResponse();
			gfacr.setFindingAidAuthor(findingAidAuthor);								
			try {
				Path name = faaInputDir.resolve(findingAidAuthor.getId() + ".xml");
				Peva2XmlReader.marshalGetFindingAidAuthorResponse(gfacr, name);
				log.info("Finding aid author downloaded {}",findingAidAuthor.getId());
			} catch (Exception e) {
				log.error("Fail to store finding aid author {} to input dir", findingAidAuthor.getId(), e);
			}
		}
	}
	
    @Override
    protected String getName() {
        return super.getName() + "-" + peva2.getInstitutionId();
    }
	
    @Configuration
    @ConditionalOnProperty(value = "peva2.url")
    public static class Peva2ImportFindingAidAuthorConfig {
        
        private final PropertyRepository propertyRepository;
        
        private final ConfigPeva2 config;
        
        private final TransactionTemplate tt;
        
        private final StorageService storageService;
        
        private final boolean active;
        
        public Peva2ImportFindingAidAuthorConfig(PropertyRepository propertyRepository, ConfigPeva2 config,
                                      TransactionTemplate tt, StorageService storageService,
                                      @Value("${peva2.importFindingAidAuthor:false}") boolean active) {
            this.propertyRepository = propertyRepository;
            this.config = config;
            this.tt = tt;
            this.storageService = storageService;
            this.active = active;
        }

        @Bean
        @Scope("prototype")
        public Peva2ImportFindingAidAuthor importFindingAidAuthor(PEvA2Connection peva2) {
            return new Peva2ImportFindingAidAuthor(peva2, propertyRepository, config, tt, storageService, active); 
        }

    }


}
