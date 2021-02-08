package cz.aron.apux;

import java.util.UUID;

import javax.xml.bind.JAXBElement;

import cz.aron.apux._2020.Dao;
import cz.aron.apux._2020.DaoBundle;
import cz.aron.apux._2020.DaoBundleType;
import cz.aron.apux._2020.DaoFile;

public class DaoBuilder {

    private Dao dao = ApuxFactory.getObjFactory().createDao();

    public void setUuid(String daoUuid) {
        dao.setUuid(daoUuid);        
    }

    public DaoBundle createDaoBundle(DaoBundleType bundleType) {
        var daoBundle = ApuxFactory.getObjFactory().createDaoBundle();
        daoBundle.setType(bundleType);
        dao.getBndl().add(daoBundle);
        return daoBundle;
    }
    
    public JAXBElement<Dao> build() {
        JAXBElement<Dao> result = ApuxFactory.getObjFactory().createDao(dao);
        return result;
    }

    public static void addMimeType(DaoFile daoFile, String mimeType) {                
        var metadataItem = ApuxFactory.getObjFactory().createMetadataItem();        
        metadataItem.setCode("mimeType");
        metadataItem.setValue(mimeType);
        
        var metadata = daoFile.getMtdt();
        if(metadata==null) {
            metadata = ApuxFactory.getObjFactory().createMetadata();
            daoFile.setMtdt(metadata);
        }
        
        metadata.getItms().add(metadataItem);        
    }

    public static DaoFile createDaoFile(int pos, String mimetype) {
        DaoFile daoFile = ApuxFactory.getObjFactory().createDaoFile();
        daoFile.setUuid(UUID.randomUUID().toString());
        daoFile.setPos(pos);
        
        DaoBuilder.addMimeType(daoFile, mimetype);
        
        return daoFile;
    }
    
}
