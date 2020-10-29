package cz.inqool.aron.integration;

import cz.aron.apux._2020.*;
import cz.inqool.aron.model.ApuType;
import cz.inqool.aron.model.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class ApuProcessor {

    @Inject private ApuPartDao apuPartDao;
    @Inject private ApuPartTypeDao apuPartTypeDao;
    @Inject private ItemTypeDao itemTypeDao;
    @Inject private MetadataTypeDao metadataTypeDao;

    public void processApu(Apu apu) {
        ApuEntity apuEntity = new ApuEntity();
        apuEntity.setId(apu.getUuid());
        apuEntity.setName(apu.getName());
        apuEntity.setPermalink(apu.getPrmLnk());
        apuEntity.setType(ApuType.valueOf(apu.getType().name()));
//        apuEntity.setParent(apu.getPrnt()); todo
//        apuEntity.setSource(); todo
        processPart(apu.getPrts().getPart(), apuEntity);
        processAttachments(apu.getAttchs(), apuEntity);
    }

    private void processPart(List<Part> parts, ApuEntity apuEntity) {
        for (Part part : parts) {
            ApuPart apuPart = new ApuPart();
            apuPart.setId(part.getId());
            apuPart.setValue(part.getValue());
    //        apuPart.setOrder();   todo
            apuPart.setType(apuPartTypeDao.findForCode(part.getType()));
    //        apuPart.getChildParts() todo
    //        part.getPrnt(); todo
    //        apuPart.setParent(); todo
            processPartItems(part.getItms(), apuPart);
            apuPart.setApu(apuEntity);
            apuEntity.getParts().add(apuPart);
        }
    }

    private void processPartItems(DescItems itms, ApuPart apuPart) {
        for (Object o : itms.getStrOrLnkOrEnm()) {
            ApuPartItem item = new ApuPartItem();
//            item.setOrder(); todo
            if (o instanceof ItemString) {
                ItemString itemString = (ItemString) o;
                item.setType(itemTypeDao.findForCode(itemString.getType()));
                item.setValue(itemString.getValue());
            }
            else if(o instanceof ItemLink) {
                ItemLink itemLink = (ItemLink) o;
                item.setType(itemTypeDao.findForCode(itemLink.getType()));
                item.setValue(itemLink.getLinl());
//                itemLink.getName()    todo

            }
            else if(o instanceof ItemEnum) {
                ItemEnum itemEnum = (ItemEnum) o;
                item.setType(itemTypeDao.findForCode(itemEnum.getType()));
                item.setValue(itemEnum.getValue());
            }
            else if(o instanceof ItemRef) {
                ItemRef itemRef = (ItemRef) o;
                item.setType(itemTypeDao.findForCode(itemRef.getType()));
                item.setValue(itemRef.getValue());
            }
            else if(o instanceof ItemDateRange) {
                ItemDateRange itemDateRange = (ItemDateRange) o;
                item.setType(itemTypeDao.findForCode(itemDateRange.getType()));
                String value = "";
//                itemDateRange.getF(); todo
//                itemDateRange.getTo();
//                itemDateRange.getFmt();
//                itemDateRange.isFe();
//                itemDateRange.isToe();
                item.setValue(value);
            }
            item.setApuPart(apuPart);
            apuPart.getItems().add(item);
        }
    }

    private void processAttachments(List<Attachment> attchs, ApuEntity apuEntity) {
        for (Attachment attch : attchs) {
            ApuAttachment apuAttachment = new ApuAttachment();
            apuAttachment.setName(attch.getName());

            processFile(attch.getFile(), apuAttachment);

            apuAttachment.setApu(apuEntity);
            apuEntity.getAttachments().add(apuAttachment);
        }
    }

    private void processFile(DaoFile file, ApuAttachment apuAttachment) {
        DigitalObjectFile digitalObjectFile = new DigitalObjectFile();
        digitalObjectFile.setId(file.getUuid());
        digitalObjectFile.setPermalink(file.getPrmLnk());
        digitalObjectFile.setOrder(file.getPos());
        digitalObjectFile.setType(DigitalObjectType.ORIGINAL);

        digitalObjectFile.setAttachment(apuAttachment);
        apuAttachment.setFile(digitalObjectFile);

        for (MetadataItem metadataItem : file.getMtdt().getItms()) {
            Metadatum metadatum = new Metadatum();
            metadatum.setValue(metadataItem.getValue());
            metadatum.setType(metadataTypeDao.findForCode(metadataItem.getCode()));
            metadatum.setFile(digitalObjectFile);
            digitalObjectFile.getMetadata().add(metadatum);
        }
    }
}
