package cz.aron.core.model.types;

import cz.aron.core.model.types.dto.ApuPartType;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.model.types.dto.TypesConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
@Slf4j
public class TypesLoader {
    @Inject private ResourceLoader resourceLoader;

    public TypesConfigDto loadTypes() {
        log.debug("Loading types from config.");
        try (InputStream inputStream = resourceLoader.getResource("classpath:/types.yaml").getInputStream()) {
            Yaml yaml = new Yaml();
            TypesConfigDto typesConfigDto = yaml.loadAs(inputStream, TypesConfigDto.class);
            //we replace underscores with tildes because otherwise indexing would turn them to dots
            for (ApuPartType apuPartType : typesConfigDto.getPartyTypes()) {
                apuPartType.setCode(apuPartType.getCode().replace("_", "~"));
            }
            for (ItemType itemType : typesConfigDto.getItemTypes()) {
                itemType.setCode(itemType.getCode().replace("_", "~"));
            }
            return typesConfigDto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
