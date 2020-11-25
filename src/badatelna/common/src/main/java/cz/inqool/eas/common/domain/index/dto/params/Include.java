package cz.inqool.eas.common.domain.index.dto.params;

/**
 * Enum constants for search request modification. Can be used to specify to return items in a special state.
 */
public enum Include {

    /**
     * Return deleted items
     */
    DELETED,

    /**
     * Return replaced items (in case of dictionaries)
     */
    REPLACED,

    /**
     * Return duplicate items (in case of dictionaries)
     */
    DUPLICATES,

    /**
     * Return deactivated items (in case of dictionaries)
     */
    DEACTIVATED,

    /**
     * Return items with invalid {@code validFrom} and/or {@code validTo} values (in case of dictionaries)
     */
    INVALID,

    /**
     * Return items even from other institutions
     */
    OTHER_INSTITUTION
}
