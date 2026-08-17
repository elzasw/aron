package cz.aron.domain.types;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import cz.aron.domain.DataType;
import cz.aron.domain.types.dto.ApuPartType;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.domain.types.dto.ItemTypeGroup;
import cz.aron.domain.types.dto.LocalizedItem;
import cz.aron.domain.types.dto.TypesConfigDto;

@Service
public class TypesLoader {
	
	private static final Logger log = LoggerFactory.getLogger(TypesLoader.class);

	private final ResourceLoader resourceLoader;

	private final String typesConfig;
    
	public TypesLoader(ResourceLoader resourceLoader, @Value("${types-config}") String typesConfig) {
		this.resourceLoader = resourceLoader;
		this.typesConfig = typesConfig;
	}

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
            Map<String, Object> itemViewOrders = (Map<String,Object>)localization.getOrDefault("viewOrders",Collections.emptyMap());
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

            int viewOrderCounter = 10000;
            Map<String,Object> itLoc = (Map<String,Object>)localization.getOrDefault("itemTypes",Collections.emptyMap());
            for (ItemType itemType : typesConfigDto.getItemTypes()) {
                itLoc.forEach((k,v)->{
                    Map<String,Object> iLoc = (Map<String,Object>)v;
                    var text = (String)iLoc.get(itemType.getCode());
                    if (text!=null) {
                        itemType.getLang().add(new LocalizedItem(k,text));
                    }
                });
                var itemViewOrder = itemViewOrders.get(itemType.getCode());
                if (itemViewOrder!=null) {
                    itemType.setViewOrder((Integer)itemViewOrder);
                } else {
                    itemType.setViewOrder(viewOrderCounter++);
                }
                itemType.setCode(itemType.getCode().replace("_", "~"));
                if (DataType.JSON.equals(itemType.getType())) {
                    // json objects are not indexed
                    itemType.setIndexed(false);
                }
            }
            List<String> indexedFields = new ArrayList<>();
            for (ItemType itemType : typesConfigDto.getItemTypes()) {
                if (itemType.isIndexed()) {
                    // the fulltext flag changes what the allText field contains, so it
                    // is part of the indexed-fields CRC - toggling it must reindex
                    indexedFields.add(itemType.getCode() + itemType.getType().name()
                            + (itemType.isFulltextEnabled() ? "" : "~NOFT"));
                }
            }
            Collections.sort(indexedFields);
            var indexedFieldsCrc = new CRC32();
            for (String s : indexedFields) {
                indexedFieldsCrc.update(s.getBytes(StandardCharsets.UTF_8));
            }
            typesConfigDto.setIndexedFieldsCrc(indexedFieldsCrc.getValue());

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
