package cz.aron.transfagent.service;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;

@Service
public class ApuSourceService {
	
	
	final ApuSourceRepository apuSourceRepository;
	
	public ApuSourceService(final ApuSourceRepository apuSourceRepository) {
		this.apuSourceRepository = apuSourceRepository;
	}

	@Transactional
	public ApuSource createApuSource(UUID apusrcUuid, SourceType sourceType, Path dataDir, String origDir) {
		
		var apuSource = new cz.aron.transfagent.domain.ApuSource();
		apuSource.setDataDir(dataDir.toString());
		apuSource.setOrigDir(origDir);
		apuSource.setSourceType(sourceType);
		apuSource.setUuid(apusrcUuid);
		apuSource.setDeleted(false);
		apuSource.setReimport(false);
		apuSource.setDateImported(ZonedDateTime.now());
		apuSource = apuSourceRepository.save(apuSource);
		return apuSource;
	}

	public void reimport(ApuSource apuSource) {
		apuSource.setReimport(true);
		apuSourceRepository.save(apuSource);
	}

}
