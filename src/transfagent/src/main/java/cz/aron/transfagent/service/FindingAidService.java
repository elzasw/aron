package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;

@Service
public class FindingAidService {
	
	private static final Logger log = LoggerFactory.getLogger(FindingAidService.class);
	
	private final ApuSourceService apuSourceService;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final FindingAidRepository findingAidRepository;
	
	private final FundRepository fundRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	private final AttachmentRepository attachmentRepository;
	
	//private final FundRepository fundRepository;

	public FindingAidService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			CoreQueueRepository coreQueueRepository, AttachmentRepository attachmentRepository) {
		super();
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentRepository = attachmentRepository;
	}

	@Transactional
	public FindingAid createFindingAid(String findingaidCode, List<Fund> relatedFunds, Institution institution, Path dataDir,
			Path origDir, ApuSourceBuilder builder, List<Path> attachments) {
		
		// reload funds from db
		var funds = fundRepository.findAllById(relatedFunds.stream().map(f->f.getId()).collect(Collectors.toList()));
		
		if (funds.size()>1) {
			System.out.println("Nazdar");
		}

		var findingaidUuid = builder.getMainApu().getUuid();
		Validate.notNull(findingaidUuid, "FindingAid UUID is null");

		var apuSourceUuidStr = builder.getApusrc().getUuid();
		var apuSourceUuid = UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FINDING_AID, dataDir,
				origDir.getFileName().toString());

		var findingAid = new FindingAid();
		findingAid.setCode(findingaidCode);
		findingAid.setUuid(UUID.fromString(findingaidUuid));
		findingAid.setApuSource(apuSource);
		findingAid.setInstitution(institution);
		for(var fund:funds) {
			findingAid.addFund(fund);
		}
		findingAid = findingAidRepository.save(findingAid);

		updateAttachments(findingAid, builder, attachments);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("FindingAid created code={}, uuid={}", findingaidCode, findingaidUuid);
		return findingAid;
	}

	private void updateAttachments(FindingAid findingAid, ApuSourceBuilder builder, List<Path> attachments) {
		var dbAttachments = attachmentRepository.findByApuSource(findingAid.getApuSource());
		if (dbAttachments.size() > 0) {
			// drop old attachment
			attachmentRepository.deleteInBatch(dbAttachments);
		}

		if (CollectionUtils.isNotEmpty(attachments)) {
			var mainApu = builder.getMainApu();
			Validate.isTrue(attachments.size() == mainApu.getAttchs().size(),
					"Attachment size does not match, attachments: %i, xml: %i", attachments.size(),
					mainApu.getAttchs().size());

			var attIt = mainApu.getAttchs().iterator();
			var fileIt = attachments.iterator();
			while (attIt.hasNext() && fileIt.hasNext()) {
				var att = attIt.next();
				var attPath = fileIt.next();

				Validate.notNull(att.getFile(), "DaoFile is null");
				Validate.notNull(att.getFile().getUuid(), "DaoFile without UUID");

				createAttachment(findingAid.getApuSource(), attPath.getFileName().toString(),
						UUID.fromString(att.getFile().getUuid()));
			}
		}
	}
	
    private void createAttachment(ApuSource apuSource, String fileName, UUID uuid) {
        var attachment = new Attachment();
        attachment.setApuSource(apuSource);
        attachment.setFileName(fileName);
        attachment.setUuid(uuid);
        attachment = attachmentRepository.save(attachment); 
    }

    @Transactional
	public void updateFindingAid(FindingAid findingAid, Path dataDir, Path origDir, ApuSourceBuilder builder,
			List<Path> attachments) {

		var apuSource = findingAid.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());

		updateAttachments(findingAid, builder, attachments);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);

		apuSourceRepository.save(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("FindingAid updated code={}, uuid={}, data dir: {}", findingAid.getCode(), findingAid.getUuid(),
				dataDir.toString());
	}
    
}
