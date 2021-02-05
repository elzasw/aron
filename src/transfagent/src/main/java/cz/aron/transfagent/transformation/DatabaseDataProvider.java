package cz.aron.transfagent.transformation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.DSpaceImportService;
import cz.aron.transfagent.service.StorageService;

@Service
public class DatabaseDataProvider implements ContextDataProvider {

    private static Logger log = LoggerFactory.getLogger(DatabaseDataProvider.class);

    final private InstitutionRepository institutionRepository;

    final private ArchivalEntityRepository entityRepository;

    final private FundRepository fundRepository;

    final private DaoFileRepository daoRepository;

    final private StorageService storageService;

    public DatabaseDataProvider(final InstitutionRepository institutionRepository,
            final ArchivalEntityRepository entityRepository,
            final FundRepository fundRepository,
            final DaoFileRepository daoRepository,
            final StorageService storageService) {
        this.institutionRepository = institutionRepository;
        this.entityRepository = entityRepository;
        this.fundRepository = fundRepository;
        this.daoRepository = daoRepository;
        this.storageService= storageService;
    }

    @Override
    public InstitutionInfo getInstitutionApu(String instCode) {
        Institution institution = institutionRepository.findByCode(instCode);
        if (institution != null) {
            UUID uuid = institution.getUuid();
            ApuSource apuSrc = institution.getApuSource();
            Path fileXml = storageService.getDataPath().resolve(apuSrc.getDataDir()).resolve(apuSrc.getOrigDir() + ".xml");
            ImportInstitution ii = new ImportInstitution();
            ApuSourceBuilder builder;
            String name;
            try {
                builder = ii.importInstitution(fileXml, instCode, uuid);
            } catch (IOException | JAXBException e) {
                log.error("Error processing file={} institution code={}", fileXml, instCode, e);
                throw new RuntimeException(e);
            }
            name = builder.getApusrc().getApus().getApu().get(0).getName();
            return new InstitutionInfo(uuid, name);
        }
        return null;
    }

    @Override
    public List<UUID> getArchivalEntityApuWithParentsByElzaId(Integer elzaId) {
        return entityRepository.findByElzaIdWithParents(elzaId).stream()
                .filter(uuid -> uuid != null)
                .collect(Collectors.toList());
    }

	@Override
	public UUID getFundApu(String institutionCode, String fundCode) {
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	return null;
        }
		var fund = fundRepository.findByCodeAndInstitution(fundCode, institution);
		if(fund==null) {
			return null;
		}
		Validate.notNull(fund.getUuid());
		return fund.getUuid();
	}

    @Override
    public List<UUID> findByUUIDWithParents(UUID apUuid) {        
        return entityRepository.findByUUIDWithParents(apUuid);
    }

    @Override
    public UUID getDao(String daoHandle) {
        var dao = daoRepository.findByHandle(daoHandle);
        if(dao.isPresent()) {
            return dao.get().getUuid();
        } else {
            return null;
        }
    }

}
