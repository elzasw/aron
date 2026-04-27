package cz.aron.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.ApuAttachment;
import cz.aron.api.rest.model.ApuEntity;
import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.api.rest.model.DigitalObject;
import cz.aron.api.rest.model.DigitalObjectFile;

@Component
public class ApuEntityMapper {

    public ApuEntity toRest(cz.aron.domain.ApuEntity src) {
        ApuEntity dto = new ApuEntity(src.getUuid());
        dto.setName(src.getName());
        dto.setDescription(src.getDescription());
        dto.setPermalink(src.getPermalink());
        dto.setOrder(src.getOrder());
        dto.setPublished(src.isPublished());
        dto.setType(toApuTypeEnum(src.getType()));

        if (src.getParent() != null) {
            dto.setParent(toRest(src.getParent()));
        }

        List<ApuPart> parts = new ArrayList<>(src.getParts().size());
        for (cz.aron.domain.ApuPart part : src.getParts()) {
            parts.add(toRest(part));
        }
        dto.setParts(parts);

        List<ApuAttachment> attachments = new ArrayList<>(src.getAttachments().size());
        for (cz.aron.domain.ApuAttachment att : src.getAttachments()) {
            attachments.add(toRest(att));
        }
        dto.setAttachments(attachments);

        List<DigitalObject> digitalObjects = new ArrayList<>(src.getDigitalObjects().size());
        for (cz.aron.domain.DigitalObject dao : src.getDigitalObjects()) {
            digitalObjects.add(toRest(dao));
        }
        dto.setDigitalObjects(digitalObjects);

        return dto;
    }

    private ApuPart toRest(cz.aron.domain.ApuPart src) {
        ApuPart dto = new ApuPart(String.valueOf(src.getId()));
        dto.setValue(src.getValue());
        dto.setType(src.getType());

        List<ApuPart> children = new ArrayList<>(src.getChildParts().size());
        for (cz.aron.domain.ApuPart child : src.getChildParts()) {
            children.add(toRest(child));
        }
        dto.setChildParts(children);

        List<ApuPartItem> items = new ArrayList<>(src.getItems().size());
        for (cz.aron.domain.ApuPartItem item : src.getItems()) {
            items.add(toRest(item));
        }
        dto.setItems(items);

        return dto;
    }

    private ApuPartItem toRest(cz.aron.domain.ApuPartItem src) {
        ApuPartItem dto = new ApuPartItem(String.valueOf(src.getId()));
        dto.setValue(src.getValue());
        dto.setVisible(src.isVisible());
        dto.setHref(src.getHref());
        dto.setType(src.getType());
        dto.setLabel(src.getTargetLabel());
        return dto;
    }

    private ApuAttachment toRest(cz.aron.domain.ApuAttachment src) {
        ApuAttachment dto = new ApuAttachment(String.valueOf(src.getId()));
        dto.setName(src.getName());
        dto.setOrder(src.getOrder());
        if (src.getFile() != null) {
            dto.setFile(toRest(src.getFile()));
        }
        return dto;
    }

    private DigitalObject toRest(cz.aron.domain.DigitalObject src) {
        DigitalObject dto = new DigitalObject(src.getUuid());
        dto.setName(src.getName());
        dto.setPermalink(src.getPermalink());
        dto.setOrder(src.getOrder());

        List<DigitalObjectFile> files = new ArrayList<>(src.getFiles().size());
        for (cz.aron.domain.DigitalObjectFile f : src.getFiles()) {
            files.add(toRest(f));
        }
        dto.setFiles(files);

        return dto;
    }

    private DigitalObjectFile toRest(cz.aron.domain.DigitalObjectFile src) {
        DigitalObjectFile dto = new DigitalObjectFile(src.getUuid());
        dto.setPermalink(src.getPermalink());
        dto.setOrder(src.getOrder());
        dto.setType(toFileTypeEnum(src.getType()));
        dto.setName(src.getName());
        dto.setReferencedFile(src.getReferencedFile());
        dto.setContentType(src.getContentType());
        dto.setSize(src.getSize());
        return dto;
    }

    private ApuEntity.TypeEnum toApuTypeEnum(cz.aron.domain.ApuType type) {
        if (type == null) {
            return null;
        }
        return ApuEntity.TypeEnum.fromValue(type.name());
    }

    private DigitalObjectFile.TypeEnum toFileTypeEnum(cz.aron.domain.DigitalObjectType type) {
        if (type == null) {
            return null;
        }
        return DigitalObjectFile.TypeEnum.fromValue(type.name());
    }
}
