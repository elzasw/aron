package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;

@Service
public class FundService {

	private static final Logger log = LoggerFactory.getLogger(FundService.class);

	private final ApuSourceService apuSourceService;

	private final ApuSourceRepository apuSourceRepository;

	private final FundRepository fundRepository;

	private final CoreQueueRepository coreQueueRepository;
	
	private final AttachmentService attachmentService;

	public FundService(ApuSourceService apuSourceService, ApuSourceRepository apuSourceRepository,
			FundRepository fundRepository, CoreQueueRepository coreQueueRepository, AttachmentService attachmentService) {
		this.apuSourceService = apuSourceService;
		this.apuSourceRepository = apuSourceRepository;
		this.fundRepository = fundRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.attachmentService = attachmentService;
	}

	/**
	 * Vytvori novy fond.
	 * 
	 * Vytvori ApuSource, vytvori Fund, vlozi udalost do CoreQueue
	 * 
	 * @param institution   Instituce
	 * @param dataDir       cesta k adresari/zipu s daty
	 * @param origDir       puvodni cesta k adresari/zipu s daty
	 * @param apusrcBuilder apu builder
	 * @param fundCode      kod fondu
	 */
	@Transactional
	public Fund createFund(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder,
			String fundCode, String source) {

		var fundUuid = apusrcBuilder.getMainApu().getUuid();
		Validate.notNull(fundUuid, "Fund UUID is null");

		var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
		var apuSourceUuid = apuSourceUuidStr == null ? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr);

		var apuSource = apuSourceService.createApuSource(apuSourceUuid, SourceType.FUND, dataDir,
				origDir.getFileName().toString());

		var fund = new Fund();
		fund.setApuSource(apuSource);
		fund.setInstitution(institution);
		fund.setCode(fundCode);
		fund.setSource(source);
		fund.setUuid(UUID.fromString(fundUuid));
		fund = fundRepository.save(fund);

		var coreQueue = new CoreQueue();
		coreQueue.setApuSource(apuSource);
		coreQueueRepository.save(coreQueue);

		log.info("Fund created code={}, uuid={}", fundCode, fundUuid);
		return fund;
	}

	/**
	 * Aktualizuje fond
	 * 
	 * Aktualizuje ApuSource a vlozi udalost do CoreQueue
	 * 
	 * @param fund aktualizovany fond
	 * @param dataDir adresar/zip s daty
	 * @param origDir puvodni adresar/zip s daty
	 * @param insertToCoreQueue pokud je true vlozi udalost do CoreQueue
	 */
	@Transactional
	public void updateFund(Fund fund, Path dataDir, Path origDir, boolean insertToCoreQueue) {
		var oldDir = fund.getApuSource().getDataDir();
		var apuSource = fund.getApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir.getFileName().toString());
		apuSourceRepository.save(apuSource);
		if (insertToCoreQueue) {
			var coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);			
			coreQueueRepository.save(coreQueue);
		}
		log.info("Fund updated code={}, uuid={}, original data dir {}", fund.getCode(), fund.getUuid(), oldDir);
	}

}
