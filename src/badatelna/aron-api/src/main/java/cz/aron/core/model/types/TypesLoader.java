package cz.aron.core.model.types;

import cz.aron.core.model.DataType;
import cz.aron.core.model.types.dto.ApuPartType;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.model.types.dto.ItemTypeGroup;
import cz.aron.core.model.types.dto.LocalizedItem;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        var localizationsPath = Paths.get(typesConfig).getParent().resolve("types_localization.yaml");
        Map<String,Object> localization = Collections.emptyMap();
        if (Files.isRegularFile(localizationsPath)) {
            try(InputStream inputStream = Files.newInputStream(localizationsPath)) {
                Yaml yaml = new Yaml();
                localization = yaml.load(inputStream);
                log.info("Localizations downloaded {}",localizationsPath);
            } catch (IOException ex) {
                log.error("Fail to load localizations {}", localizationsPath, ex);
                throw new RuntimeException(ex);
            }
        }
        try (InputStream inputStream = Files.newInputStream(Paths.get(typesConfig));
            CheckedInputStream checkedInputStream = new CheckedInputStream(inputStream, new CRC32())) {
            Yaml yaml = new Yaml();
            TypesConfigDto typesConfigDto = yaml.loadAs(checkedInputStream, TypesConfigDto.class);
            //we replace underscores with tildes because otherwise indexing would turn them to dots
            Map<String,Object> ptLoc = (Map<String,Object>)localization.getOrDefault("partTypes",Collections.emptyMap());
            for (ApuPartType apuPartType : typesConfigDto.getPartTypes()) {
                ptLoc.forEach((k,v)->{
                    Map<String,Object> pLoc = (Map<String,Object>)v;
                    var text = (String)pLoc.get(apuPartType.getCode());
                    if (text!=null) {
                        apuPartType.getLang().add(new LocalizedItem(k,text));
                    }
                });
                apuPartType.setCode(apuPartType.getCode().replace("_", "~"));
            }
            Map<String,Object> itLoc = (Map<String,Object>)localization.getOrDefault("itemTypes",Collections.emptyMap());
            for (ItemType itemType : typesConfigDto.getItemTypes()) {
                itLoc.forEach((k,v)->{
                    Map<String,Object> iLoc = (Map<String,Object>)v;
                    var text = (String)iLoc.get(itemType.getCode());
                    if (text!=null) {
                        itemType.getLang().add(new LocalizedItem(k,text));
                    }
                });
                itemType.setCode(itemType.getCode().replace("_", "~"));
                if (DataType.JSON.equals(itemType.getType())) {
                    // json objects are not indexed
                    itemType.setIndexed(false);
                }
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
