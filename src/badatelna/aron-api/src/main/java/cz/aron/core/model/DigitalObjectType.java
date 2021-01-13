package cz.aron.core.model;

import cz.aron.apux._2020.DaoBundleType;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
public enum DigitalObjectType {
    PUBLISHED,
    THUMBNAIL,
    TILE;

    public static DigitalObjectType fromXmlType(DaoBundleType daoBundleType) {
        switch (daoBundleType) {
            case PUBLISHED:
                return PUBLISHED;
            case THUMBNAIL:
                return THUMBNAIL;
            case HIGH_RES_VIEW:
                return TILE;
            default:
                throw new RuntimeException("unrecognized type: " + daoBundleType);
        }
    }
}
