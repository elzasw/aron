package cz.aron.core.model.types;

import cz.aron.core.model.types.dto.ApuPartType;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.model.types.dto.ItemTypeGroup;
import cz.aron.core.model.types.dto.TypesConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
@Slf4j
public class TypesLoader {
    @Inject private ResourceLoader resourceLoader;

    @Value("${types-config}")
    private String typesConfig;

    public TypesConfigDto loadTypes() {
        log.debug("Loading types from config.");
        try (InputStream inputStream = Files.newInputStream(Paths.get(typesConfig));
            CheckedInputStream checkedInputStream = new CheckedInputStream(inputStream, new CRC32())) {
            Yaml yaml = new Yaml();
            TypesConfigDto typesConfigDto = yaml.loadAs(checkedInputStream, TypesConfigDto.class);
            //we replace underscores with tildes because otherwise indexing would turn them to dots
            for (ApuPartType apuPartType : typesConfigDto.getPartTypes()) {
                apuPartType.setCode(apuPartType.getCode().replace("_", "~"));
            }
            for (ItemType itemType : typesConfigDto.getItemTypes()) {
                itemType.setCode(itemType.getCode().replace("_", "~"));
            }
            for (ItemTypeGroup itemTypeGroup : typesConfigDto.getItemGroups()) {
                itemTypeGroup.setCode(itemTypeGroup.getCode().replace("_", "~"));
                List<String> modifiedItems = new ArrayList<>();
                for (String item : itemTypeGroup.getItems()) {
                    modifiedItems.add(item.replace("_", "~"));
                }
                itemTypeGroup.setItems(modifiedItems);
            }
            typesConfigDto.setCurrentCrc(checkedInputStream.getChecksum().getValue());
            return typesConfigDto;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
