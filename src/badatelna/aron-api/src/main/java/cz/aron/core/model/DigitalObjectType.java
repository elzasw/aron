package cz.aron.core.model;

import cz.aron.apux._2020.DaoBundleType;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
public enum DigitalObjectType {
    ORIGINAL,
    PUBLISHED,
    THUMBNAIL,
    TILE;

    public static DigitalObjectType fromXmlType(DaoBundleType daoBundleType) {
        switch (daoBundleType) {
            case HIGHT_RES_VIEW:
                return ORIGINAL;
            case PUBLISHED:
                return PUBLISHED;
            case THUMBNAIL:
                return THUMBNAIL;
            default:
                throw new RuntimeException("unrecognized type: " + daoBundleType);
        }
    }
}
