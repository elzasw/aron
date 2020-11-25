package cz.inqool.eas.common.domain.index.field;

import cz.inqool.eas.common.exception.InvalidAttribute;

import java.util.TreeMap;

import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELD_INVALID_TYPE;
import static cz.inqool.eas.common.exception.InvalidAttribute.ErrorCode.FIELD_NOT_MAPPED;
import static cz.inqool.eas.common.utils.AssertionUtils.cast;

/**
 * Stores information about indexed object fields mappings of a store to ElasticSearch, to provide better searching
 * capabilities.
 * <p>
 * The key of this custom map is always represented by corresponding dot-separated path to the field in JSON
 * representation of entity being indexed (not underscore-separated field name used in indexed classes), i.e. instead of
 * {@code user_id} is used {@code user.id}
 */
public class IndexObjectFields extends TreeMap<String, IndexFieldNode> {

    public <T extends IndexFieldNode> T get(String key, Class<T> type) {
        IndexFieldNode node = super.get(key);
        if (node == null) {
            throw new InvalidAttribute(type, null, key, FIELD_NOT_MAPPED);
        }
        return cast(node, type, () -> new InvalidAttribute(type, null, key, FIELD_INVALID_TYPE));
    }
}
