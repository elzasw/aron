package cz.aron.integration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import cz.aron.apux._2020.DaoFile;
import cz.aron.apux._2020.MetadataItem;
import cz.aron.domain.ApuAttachment;
import cz.aron.domain.DigitalObject;
import cz.aron.domain.DigitalObjectFile;
import cz.aron.domain.DigitalObjectType;
import cz.aron.service.FileManagerService;

@Service
public class FileInputProcessor {

    // full path to file on local filesystem
    private static String ATTR_PATH = "path";
    // mimetype of file
    private static String ATTR_MIMETYPE = "mimeType";
    // reference flag, no data only uuid and/or path
    private static String ATTR_REFERENCE = "reference";
    // size of file
    private static String ATTR_SIZE = "size";
    // file is selected
    private static String ATTR_SELECTED = "selected";
    // file name
    private static String ATTR_NAME = "name";
    
    private final FileManagerService fileManager;
        
    public FileInputProcessor(FileManagerService fileManager) {
    	this.fileManager = fileManager;
    }

    public void processFile(DaoFile daoFile, DigitalObjectType digitalObjectType, ApuAttachment apuAttachment, DigitalObject digitalObject, Map<String, Path> filesMap) {

        String name;
        DigitalObjectFile digitalObjectFile;
        if (apuAttachment != null) {
            digitalObjectFile = getNewOrExisting(apuAttachment, UUID.fromString(daoFile.getUuid()));
            name = apuAttachment.getName();
        }
        else if (digitalObject != null) {
            digitalObjectFile = getNewOrExisting(digitalObject, UUID.fromString(daoFile.getUuid()));
            name = digitalObject.getName();
        }
        else {
            throw new RuntimeException("missing parent object");
        }

        digitalObjectFile.setType(digitalObjectType);
        digitalObjectFile.setOrder(daoFile.getPos());
        digitalObjectFile.setPermalink(daoFile.getPrmLnk());
        digitalObjectFile.setReferencedFile(null);
        digitalObjectFile.setContentType(null);
        digitalObjectFile.setName(null);
        digitalObjectFile.setSize(null);
        digitalObjectFile.setSelected(isSelected(daoFile));

        var attributes = processMetadata(daoFile, digitalObjectFile);
        var mimeType = attributes.get(ATTR_MIMETYPE);
        if (mimeType==null) {
            mimeType = "application/octet-stream";
        }
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                if ("mimeType".equals(itm.getCode())) {
                    mimeType = itm.getValue();
                }
            }
        }
        Path uploadedFile = filesMap.get(digitalObjectFile.getUuid().toString());
        if (name == null) {
            name = UUID.randomUUID().toString();
        }
        try (InputStream is = Files.newInputStream(uploadedFile)) {
            var handle = fileManager.storeFile(is,null);
            digitalObjectFile.setReferencedFile("file://"+handle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processFileReference(DaoFile daoFile, DigitalObjectType digitalObjectType, DigitalObject digitalObject) {
        DigitalObjectFile digitalObjectFile = getNewOrExisting(digitalObject, UUID.fromString(daoFile.getUuid()));
        digitalObjectFile.setType(digitalObjectType);
        digitalObjectFile.setOrder(daoFile.getPos());
        digitalObjectFile.setPermalink(daoFile.getPrmLnk());
        digitalObjectFile.setDigitalObject(digitalObject);
        digitalObjectFile.setSelected(isSelected(daoFile));        

        var attributes = processMetadata(daoFile, digitalObjectFile);
        digitalObjectFile.setReferencedFile(attributes.get(ATTR_PATH));
        digitalObjectFile.setContentType(attributes.get(ATTR_MIMETYPE));
        var size = attributes.get(ATTR_SIZE);
        if (size!=null) {
             digitalObjectFile.setSize(Long.parseLong(size));
        } else {
             digitalObjectFile.setSize(null);
        }
        //TODO transfer filename from transformagent
        String name = attributes.get(ATTR_NAME);
        if (name==null&&digitalObjectFile.getReferencedFile()!=null) {
            name = Paths.get(digitalObjectFile.getReferencedFile()).getFileName().toString();
        }
        digitalObjectFile.setName(name);
    }

    /**
     * Add metadata. Remove unused when exist
     */
    private Map<String, String> processMetadata(DaoFile daoFile, DigitalObjectFile digitalObjectFile) {
        var attributes = new HashMap<String,String>();
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                attributes.put(itm.getCode(), itm.getValue());
                if (!ATTR_PATH.equals(itm.getCode())&&!ATTR_REFERENCE.equals(itm.getCode())) {
                    attributes.put(itm.getCode(), itm.getValue());
                }
            }
        }       
        return attributes;
    }

	private DigitalObjectFile getNewOrExisting(DigitalObject digitalObject, UUID uuid) {
		for (DigitalObjectFile digitalObjectFile : digitalObject.getFiles()) {
			if (uuid.equals(digitalObjectFile.getUuid())) {
				return digitalObjectFile;
			}
		}
		DigitalObjectFile digitalObjectFile = new DigitalObjectFile();
		digitalObjectFile.setUuid(uuid);
		digitalObjectFile.setDigitalObject(digitalObject);
		digitalObject.getFiles().add(digitalObjectFile);
		return digitalObjectFile;
	}

    private DigitalObjectFile getNewOrExisting(ApuAttachment apuAttachment, UUID uuid) {
        DigitalObjectFile digitalObjectFile = apuAttachment.getFile();
        if (digitalObjectFile!=null && uuid.equals(digitalObjectFile.getUuid())) {
            return digitalObjectFile;
        }
        digitalObjectFile = new DigitalObjectFile();
        digitalObjectFile.setUuid(uuid);
        digitalObjectFile.setAttachment(apuAttachment);
        apuAttachment.setFile(digitalObjectFile);
        return digitalObjectFile;
    }

    public static boolean isReference(DaoFile daoFile) {
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                if (ATTR_REFERENCE.equals(itm.getCode()) && "1".equals(itm.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean isSelected(DaoFile daoFile) {
        if (daoFile.getMtdt() != null) {
            for (MetadataItem itm : daoFile.getMtdt().getItms()) {
                if (ATTR_SELECTED.equals(itm.getCode()) && "1".equals(itm.getValue())) {
                    return true;
                }
            }
        }
        return false;    	
    }


}
