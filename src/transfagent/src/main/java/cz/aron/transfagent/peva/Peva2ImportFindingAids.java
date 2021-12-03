package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.peva2.wsdl.GetFindingAidRequest;
import cz.aron.peva2.wsdl.GetFindingAidResponse;
import cz.aron.peva2.wsdl.ListFindingAidRequest;
import cz.aron.peva2.wsdl.PEvA;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.StorageService;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2ImportFindingAids extends Peva2Downloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2ImportFindingAids.class);
	
	private static final String PREFIX_DASH = "pevafa-";

	public Peva2ImportFindingAids(PEvA peva2, PropertyRepository propertyRepository, ConfigPeva2 config,
			TransactionTemplate tt, StorageService storageService,
			@Value("${peva2.importFindingAid:false}") boolean active) {
		super("FINDINGAID", peva2, propertyRepository, config, tt, storageService, active);
	}

	@Override
	protected int synchronizeAgenda(XMLGregorianCalendar updateAfter, long eventId, String searchAfterInitial, Peva2CodeListProvider codeListProvider) {		
        
        String searchAfter = searchAfterInitial;
        int numUpdated = 0;
        Map<String, List<Path>> attachments = null;
        while (true) {
            var lfar = new ListFindingAidRequest();
            lfar.setSize(config.getBatchSize());
            lfar.setUpdatedAfter(updateAfter);
            lfar.setSearchAfter(searchAfter);
            var lfaResp = peva2.listFindingAid(lfar);            
            searchAfter = lfaResp.getSearchAfter();
            long count = lfaResp.getFindingAids().getFindingAid().size();
            log.info("Downloaded {} finding aids to update", count);
            if (count == 0) {
                break;
            }
            
            if (attachments==null) {
            	attachments = initAttachments();
            }
            
            int numUpdatedFromBatch = 0;
            try {
                patchFindingAidsBatch(lfaResp.getFindingAids().getFindingAid(), codeListProvider.getCodeLists(),attachments);
                numUpdatedFromBatch+=lfaResp.getFindingAids().getFindingAid().size();
            } catch (Exception e) {
                log.error("Fail to update finding aids batch", e);
                // zkusim to po jednom
                for (var findingAid : lfaResp.getFindingAids().getFindingAid()) {
                    try {
                        patchFindingAidsBatch(Collections.singletonList(findingAid), codeListProvider.getCodeLists(),attachments);
                        numUpdatedFromBatch++;
                    } catch (Exception e1) {
                        log.error("Fail to update finding aid {}", findingAid.getId(), e1);
                    }
                }                
            }

            storeSearchAfter(searchAfter);            
            numUpdated += numUpdatedFromBatch;
            log.info("Updated {}/{} finding aids", numUpdated,
                     lfaResp.getCount());
        }
        log.info("Finding aids synchronized");
        return numUpdated;
	}

	private void patchFindingAidsBatch(List<FindingAid> findingAids, Peva2CodeLists codeLists,
			Map<String, List<Path>> attachments) {
		var faInputDir = storageService.getInputPath().resolve("findingaids");
		for (var findingAid : findingAids) {
			if (findingAid.getNadSheets() == null || findingAid.getNadSheets().getNadSheet() == null
					|| findingAid.getNadSheets().getNadSheet().isEmpty()) {
				log.warn("Finding aid without NadSheet institution:{}, finding aid:{}, id:{}",
						findingAid.getInstitution().getExternalId(), findingAid.getEvidenceNumber(),
						findingAid.getId());
				continue;
			}
			var gfar = new GetFindingAidResponse();
			gfar.setFindingAid(findingAid);
			String id = findingAid.getInstitution().getExternalId() + "_" + findingAid.getId();
			var faDir = faInputDir.resolve(PREFIX_DASH + id);
			try {
				Files.createDirectories(faDir);
				Path name = faDir.resolve(PREFIX_DASH + id + ".xml");
				Peva2XmlReader.marshalGetFindingAidResponse(gfar, name);

				var attachmentFiles = attachments.get(findingAid.getEvidenceNumber());
				if (attachmentFiles != null) {
					for (var attachmentFile : attachmentFiles) {
						Files.copy(attachmentFile, faDir.resolve(attachmentFile.getFileName()));
					}
				}
			} catch (Exception e) {
				log.error("Fail to store peva finding aid {} to input dir", findingAid.getId(), e);
			}
		}
	}
	
	private Map<String, List<Path>> initAttachments() {
		if (config.getAttachmentDir() == null) {
			return Collections.emptyMap();
		} else {									
			Pattern pattern = Pattern.compile("^(\\d+)_?(.*$)");
			Map<String, List<Path>> ret = new HashMap<>();
			Path attachmentDir = Path.of(config.getAttachmentDir());			
			try (var stream = Files.list(attachmentDir)) {
				stream.forEach(p->{
					if (Files.isDirectory(p)) {
						var fileName = p.getFileName().toString();
						Matcher m = pattern.matcher(fileName);					
						if (m.matches()) {
							var code = m.group(1);
							// odstraneni pocatecnich nul
							code = ""+Integer.parseInt(code);
							var pathList = ret.get(code);
							if (pathList==null) {
								pathList = new ArrayList<Path>();
								ret.put(code, pathList);
							}
							
							final var pathListFinal = pathList;
							try (var files=Files.list(p)) {
								files.forEach(f->{
									if (Files.isRegularFile(f)) {
										pathListFinal.add(f);
									}									
								});
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						}					
					}					
				});				
			} catch (IOException e) {
				log.error("Fail to init finding aid attachments dir {}",config.getAttachmentDir());
				throw new UncheckedIOException(e);
			}			
			return ret;
		}
	}

	@Override
	protected boolean processCommand(Path path, Peva2CodeListProvider codeListProvider) {
		var fileName = path.getFileName().toString();
		if (!fileName.startsWith(PREFIX_DASH)) {
			// not my command
			return false;
		}
		var id = fileName.substring(PREFIX_DASH.length());
		var gfaReq = new GetFindingAidRequest();
		gfaReq.setId(id);
		var gfaResp = peva2.getFindingAid(gfaReq);
		var attMap = initAttachments();
		patchFindingAidsBatch(Collections.singletonList(gfaResp.getFindingAid()), codeListProvider.getCodeLists(),
				attMap);
		return true;
	}


}
