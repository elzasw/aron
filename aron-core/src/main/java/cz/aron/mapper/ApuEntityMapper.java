package cz.aron.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import cz.aron.api.rest.model.ApuAttachment;
import cz.aron.api.rest.model.ApuEntity;
import cz.aron.domain.ApuPartSerializer;
import cz.aron.api.rest.model.DigitalObject;
import cz.aron.api.rest.model.DigitalObjectFile;
import cz.aron.api.rest.model.FileInfo;

@Component
public class ApuEntityMapper {

    public ApuEntity toRest(cz.aron.domain.ApuEntity src) {
        ApuEntity dto = new ApuEntity(src.getUuid().toString());
        dto.setName(src.getName());
        dto.setDescription(src.getDescription());
        dto.setPermalink(src.getPermalink());
        dto.setOrder(src.getOrder());
        dto.setPublished(src.isPublished());
        dto.setType(toApuTypeEnum(src.getType()));
        dto.setPos(src.getPos());
        dto.setDepth(src.getDepth());
        dto.setChildCnt(src.getChildCnt());

        if (src.getParent() != null) {
            dto.setParent(toRest(src.getParent()));
        }

        // parts are stored (and deserialized) directly as the REST model
        dto.setParts(ApuPartSerializer.deserialize(src.getData()));

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

    public List<ApuAttachment> toRestAttachments(List<cz.aron.domain.ApuAttachment> src) {
        List<ApuAttachment> attachments = new ArrayList<>(src.size());
        for (cz.aron.domain.ApuAttachment att : src) {
            attachments.add(toRest(att));
        }
        return attachments;
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

    public List<DigitalObject> toRestDigitalObjects(List<cz.aron.domain.DigitalObject> src) {
        List<DigitalObject> digitalObjects = new ArrayList<>(src.size());
        for (cz.aron.domain.DigitalObject dao : src) {
            digitalObjects.add(toRest(dao));
        }
        return digitalObjects;
    }

    private DigitalObject toRest(cz.aron.domain.DigitalObject src) {
        DigitalObject dto = new DigitalObject(src.getUuid().toString());
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
		DigitalObjectFile dto = new DigitalObjectFile(src.getUuid().toString());
		dto.setPermalink(src.getPermalink());
		dto.setOrder(src.getOrder());
		dto.setType(toFileTypeEnum(src.getType()));
		dto.setName(src.getName());
		dto.setReferencedFile(src.getReferencedFile());
		dto.setContentType(src.getContentType());
		dto.setSize(src.getSize());
		dto.setSelected(src.isSelected());

		//TODO only for compatifility with UI
		var file = new FileInfo();
		file.setId(dto.getId());
		file.setContentType(src.getContentType());
		dto.setFile(file);
		return dto;
	}

    public static ApuEntity.TypeEnum toApuTypeEnum(cz.aron.domain.ApuType type) {
        if (type == null) {
            return null;
        }
        return ApuEntity.TypeEnum.fromValue(type.name());
    }

    public static DigitalObjectFile.TypeEnum toFileTypeEnum(cz.aron.domain.DigitalObjectType type) {
        if (type == null) {
            return null;
        }
        return DigitalObjectFile.TypeEnum.fromValue(type.name());
    }
}
