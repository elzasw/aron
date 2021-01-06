package cz.aron.transfagent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.InstitutionRepository;

public abstract class AbstractCommonTest {

    @Autowired
    EntitySourceRepository entitySourceRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    ApuSourceRepository apuSourceRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    DaoFileRepository daoFileRepository;

    @BeforeEach
    protected void deleteAll() {
        entitySourceRepository.deleteAll();
        daoFileRepository.deleteAll();
        coreQueueRepository.deleteAll();
        institutionRepository.deleteAll();
        apuSourceRepository.deleteAll();
    }

    /**
     * Kontrola - je adresář prázdný?
     * 
     * @param path
     * @return
     * @throws IOException
     */
    protected boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }
}
