package cz.aron.core.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.aron.apux._2020.*;
import cz.aron.core.model.ApuSource;
import cz.aron.core.model.ApuType;
import cz.aron.core.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
@Slf4j
public class ApuProcessor {
    @Inject private ApuSourceRepository apuSourceRepository;
    @Inject private ApuRepository apuRepository;
    @Inject private FileInputProcessor fileInputProcessor;
    @Inject private ObjectMapper objectMapper;

    public void processApuAndFiles(String metadata, Map<String, Path> filesMap) {
        cz.aron.apux._2020.ApuSource apuSource;
        try (StringReader reader = new StringReader(metadata)) {
            apuSource = JAXB.unmarshal(reader, cz.aron.apux._2020.ApuSource.class);
        }
        ApuSource ourApuSource = new cz.aron.core.model.ApuSource();
        ourApuSource.setId(apuSource.getUuid());
        ourApuSource.setData(metadata);
        apuSourceRepository.create(ourApuSource);
        for (Apu apu : apuSource.getApus().getApu()) {
            processApu(apu, ourApuSource, filesMap);
        }
    }

    public void processTestingInputStream(InputStream path) throws IOException {
        String data = Streams.asString(path);
        processApuAndFiles(data, null);
    }

    public void processApu(Apu apu, cz.aron.core.model.ApuSource apuSource, Map<String, Path> filesMap) {
        ApuEntity apuEntity = new ApuEntity();
        apuEntity.setId(apu.getUuid());
        apuEntity.setName(apu.getName());
        apuEntity.setDescription(apu.getDesc());
        apuEntity.setPermalink(apu.getPrmLnk());
        apuEntity.setType(ApuType.valueOf(apu.getType().name().toUpperCase())); //fixme names don't match
        if (apu.getPrnt() != null) {
            ApuEntity parentApu = apuRepository.find(apu.getPrnt());
            if (parentApu == null) {
                throw new RuntimeException("parent apu not found yet");
            }
            apuEntity.setParent(parentApu);
        }
        apuEntity.setSource(apuSource);
        processParts(apu.getPrts().getPart(), apuEntity);
        processAttachments(apu.getAttchs(), apuEntity, filesMap);
        apuRepository.create(apuEntity);
    }

    private void processParts(List<Part> parts, ApuEntity apuEntity) {
        Map<String, ApuPart> processedPartCache = new HashMap<>();
        for (Part part : parts) {
            ApuPart apuPart = new ApuPart();
            if (part.getId() != null) {
                apuPart.setId(part.getId());
            }
            apuPart.setValue(part.getValue());
            apuPart.setType(part.getType().replace("_", "~"));
            Object prnt = part.getPrnt();
            if (prnt != null) {
                String parentPartId = ((Part) prnt).getId();
                ApuPart parentPart = processedPartCache.get(parentPartId);
                if (parentPart == null) {
                    throw new RuntimeException("parent part not processed yet, move the connecting at the end");
                }
                apuPart.setParentPart(parentPart);
                parentPart.getChildParts().add(apuPart);
            }
            else {
                apuPart.setApu(apuEntity);
                apuEntity.getParts().add(apuPart);
                processedPartCache.put(apuPart.getId(), apuPart);
            }
            processPartItems(part.getItms(), apuPart);
        }
    }

    private void processPartItems(DescItems itms, ApuPart apuPart) {
        for (Object o : itms.getStrOrLnkOrEnm()) {
            ApuPartItem item = new ApuPartItem();
            if (o instanceof ItemString) {
                ItemString itemString = (ItemString) o;
                item.setType(itemString.getType().replace("_", "~"));
                item.setValue(itemString.getValue());
                item.setVisible(itemString.isVisible() == null || itemString.isVisible());
            }
            else if(o instanceof ItemLink) {
                ItemLink itemLink = (ItemLink) o;
                item.setType(itemLink.getType().replace("_", "~"));
                item.setValue(itemLink.getName());
                item.setHref(itemLink.getLink());
                item.setVisible(itemLink.isVisible() == null || itemLink.isVisible());
            }
            else if(o instanceof ItemEnum) {
                ItemEnum itemEnum = (ItemEnum) o;
                item.setType(itemEnum.getType().replace("_", "~"));
                item.setValue(itemEnum.getValue());
                item.setVisible(itemEnum.isVisible() == null || itemEnum.isVisible());
            }
            else if(o instanceof ItemRef) {
                ItemRef itemRef = (ItemRef) o;
                item.setType(itemRef.getType().replace("_", "~"));
                item.setValue(itemRef.getValue());
                item.setVisible(itemRef.isVisible() == null || itemRef.isVisible());
            }
            else if(o instanceof ItemDateRange) {
                ItemDateRange itemDateRange = (ItemDateRange) o;
                item.setType(itemDateRange.getType().replace("_", "~"));
                UniversalDate universalDate = new UniversalDate();
                universalDate.setFormat(itemDateRange.getFmt());
                universalDate.setFrom(itemDateRange.getF());
                universalDate.setTo(itemDateRange.getTo());
                universalDate.setValueFromEstimated(itemDateRange.isFe() != null && itemDateRange.isFe());
                universalDate.setValueToEstimated(itemDateRange.isToe() != null && itemDateRange.isToe());
                try {
                    item.setValue(objectMapper.writeValueAsString(universalDate));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                item.setVisible(itemDateRange.isVisible() == null || itemDateRange.isVisible());
            }
            item.setApuPart(apuPart);
            apuPart.getItems().add(item);
        }
    }

    private void processAttachments(List<Attachment> attchs, ApuEntity apuEntity, Map<String, Path> filesMap) {
        for (Attachment attch : attchs) {
            ApuAttachment apuAttachment = new ApuAttachment();
            apuAttachment.setName(attch.getName());
            fileInputProcessor.processFile(
                    attch.getFile(),
                    DigitalObjectType.ORIGINAL,
                    apuAttachment,
                    null,
                    filesMap);
            apuAttachment.setApu(apuEntity);
            apuEntity.getAttachments().add(apuAttachment);
        }
    }
}
