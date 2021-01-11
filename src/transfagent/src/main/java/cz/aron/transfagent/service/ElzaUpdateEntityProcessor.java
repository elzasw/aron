package cz.aron.transfagent.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import cz.aron.transfagent.config.ConfigElza;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Property;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.PropertyRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.tacr.elza.ws.types.v1.SearchEntityUpdates;

public class ElzaUpdateEntityProcessor implements ImportProcessor {

    private final ElzaExportService elzaExportService;

    private final FileImportService importService;

    private final ConfigElza configElza;

    private final ArchivalEntityRepository archivalEntityRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final PropertyRepository propertyRepository;

    public ElzaUpdateEntityProcessor(ElzaExportService elzaExportService, FileImportService importService,
            ConfigElza configElza, ArchivalEntityRepository archivalEntityRepository, ApuSourceRepository apuSourceRepository,
            PropertyRepository propertyRepository) {
        this.elzaExportService = elzaExportService;
        this.importService = importService;
        this.configElza = configElza;
        this.archivalEntityRepository = archivalEntityRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.propertyRepository = propertyRepository;
    }

    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }

    @Override
    public void importData(ImportContext ic) {
        if (configElza.isDisabled()) {
            return;
        }

        var property = propertyRepository.findFirstByOrderByIdDesc();
        var fromTrans = property == null? "0" : property.getName();

        var exportService = elzaExportService.get();
        var request = new SearchEntityUpdates();
        request.setFromTrans(fromTrans);
        var entityUpdates = exportService.searchEntityUpdates(request);

        property = new Property();
        property.setName(entityUpdates.getToTrans());
        property.setValue(entityUpdates.getEntityIds().getIdentifier().toString());
        propertyRepository.save(property);

        List<Integer> ids = entityUpdates.getEntityIds().getIdentifier().stream()
                .map(p -> Integer.valueOf(p))
                .collect(Collectors.toList());

        archivalEntityRepository.setDownloadTrueByIds(ids);
        apuSourceRepository.setReimportTrueByIds(ids);
    }

}
