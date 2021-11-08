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
	
	private final AttachmentService attachmentService;
	
	//private final FundRepository fundRepository;

	public FindingAidService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			CoreQueueRepository coreQueueRepository, AttachmentRepository attachmentRepository, AttachmentService attachmentService) {
		super();
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentRepository = attachmentRepository;
		this.attachmentService = attachmentService;
	}

	@Transactional
	public FindingAid createFindingAid(String findingaidCode, List<Fund> relatedFunds, Institution institution,
			Path dataDir, Path origDir, ApuSourceBuilder builder, List<Path> attachments, boolean reimportFund) {

		// reload funds from db
		var funds = fundRepository.findAllById(relatedFunds.stream().map(f -> f.getId()).collect(Collectors.toList()));

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
		for (var fund : funds) {
			findingAid.addFund(fund);
			if (reimportFund&&fund.getApuSource()!=null) {
				fund.getApuSource().setReimport(true);
			}
		}
		findingAid = findingAidRepository.save(findingAid);

		attachmentService.updateAttachments(findingAid.getApuSource(), builder, attachments);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("FindingAid created code={}, uuid={}", findingaidCode, findingaidUuid);
		return findingAid;
	}

    @Transactional
	public void updateFindingAid(FindingAid findingAid, Path dataDir, Path origDir, ApuSourceBuilder builder,
			List<Path> attachments, boolean reimportFund) {

		var apuSource = findingAid.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());

		attachmentService.updateAttachments(findingAid.getApuSource(), builder, attachments);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);

		apuSourceRepository.save(apuSource);
		coreQueueRepository.save(coreQueue);
		
		if (reimportFund) {
			for(var fund:findingAid.getFunds()) {
				fund.getApuSource().setReimport(true);
			}
		}

		log.info("FindingAid updated code={}, uuid={}, data dir: {}", findingAid.getCode(), findingAid.getUuid(),
				dataDir.toString());
	}
    
}
