package cz.aron.indexing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery.OpType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.domain.ApuEntity;
import cz.aron.domain.ApuPart;
import cz.aron.domain.ApuPartItem;
import cz.aron.domain.DataType;
import cz.aron.domain.Relation;
import cz.aron.domain.UniversalDate;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;

@Service
public class IndexingService {
	
	private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

	private final ElasticsearchOperations operations;
	
	private final ElasticsearchConverter converter;
	
	private final TypesHolder typesHolder;
	
	private final ObjectMapper objectMapper;
	
	private final Resource settingsResource;

	public IndexingService(ElasticsearchOperations operations, ElasticsearchConverter converter, TypesHolder typesHolder, ObjectMapper objectMapper,
			@Value("classpath:elasticsearch/es_settings.json") Resource settingsResource) {
		this.operations = operations;
		this.converter = converter;
		this.typesHolder = typesHolder;
		this.objectMapper = objectMapper;
		this.settingsResource = settingsResource;
	}
	
	public void deleteApus(long apuSourceId) {
		var criteria = new Criteria("apuSourceId").is(apuSourceId);
	    var query = new CriteriaQuery(criteria);
	    DeleteQuery deleteQuery = DeleteQuery.builder(query).build();
	    operations.delete(deleteQuery, IndexedApu.class);			
	}

	public void indexApus(Collection<ApuEntity> apus) {
		var indexQueries = new ArrayList<IndexQuery>(apus.size());
		for (var apu : apus) {
			if (apu.isIndexed()) {
				var document = convert(apu);
				var iq = new IndexQuery();
				iq.setId(apu.getUuid());
				iq.setObject(document);
				iq.setOpType(OpType.INDEX);
				iq.setSource("source");
				indexQueries.add(iq);
			}
		}
		if (!indexQueries.isEmpty()) {
			operations.bulkIndex(indexQueries, IndexCoordinates.of("apu"));
		}
	}
	
	public void indexRels(Collection<Relation> rels) {
		var indexQueries = new ArrayList<IndexQuery>(rels.size());
		for (var rel : rels) {
			if (rel.isRemove()) {
				continue;
			}
			var indexedRel = new IndexedRelation(rel.getSource(), rel.getRelation(), rel.getTarget());
			var iq = new IndexQuery();
			iq.setId(UUID.randomUUID().toString());
			iq.setObject(indexedRel);
			iq.setOpType(OpType.INDEX);
			indexQueries.add(iq);
		}
		if (!indexQueries.isEmpty()) {
			operations.bulkIndex(indexQueries, IndexCoordinates.of("rels"));
		}
	}

	private Document convert(ApuEntity apu) {
	
		var indexedApu = new IndexedApu();
		Map<String, List<Object>> additionalDataToIndex = new HashMap<>();
		
        var apuSourceIdArr = new ArrayList<Object>();
        apuSourceIdArr.add(apu.getSource().getId());
        additionalDataToIndex.put("apuSourceId", apuSourceIdArr);
        
        String indexedName = apu.getName();
		
        for (ApuPart part : apu.getParts()) {
            for (ApuPartItem item : part.getItems()) {
                String value = item.getValue();
                ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                if (itemType == null) {
                    log.warn("Item type not recognized: " + item.getType());
                    continue;
                }
                if (!itemType.isIndexed()) {
                    continue;
                }
                Object data;
                switch (itemType.getType()) {
                    case STRING:
                    	if ("INT~NAME~INDEX".equals(item.getType())) {
                    		// alternativni hodnota pro indexovani "name"
                    		indexedName = value;
                    		continue;
                    	} else {
                    		data = value;
                    	}
                        break;
                    case ENUM:
                        data = value;
                        break;
                    case INTEGER:
                        data = Integer.valueOf(value);
                        break;
                    case APU_REF:
                        data = value;
                        break;
                    case UNITDATE:
                        try {
                            UniversalDate universalDate = objectMapper.readValue(value, UniversalDate.class);                                                                                    
                            var fromYear = universalDate.getFrom();
                            var toYear = universalDate.getTo();                            
                            //var range = Range.<Integer>closed(fromYear, toYear);                            
                            var r = new LinkedHashMap<String,String>();
                            r.put("gte", fromYear);
                            r.put("lte", toYear);
                            data = r;                            
                            var origL = additionalDataToIndex.get(itemType.getCode() + "~L");
                            if (origL == null||UniversalDate.isLower(fromYear,(String)origL.get(0))) {
                                additionalDataToIndex.put(itemType.getCode() + "~L", Collections.singletonList(fromYear));                                
                            }                            
                            var origH = additionalDataToIndex.get(itemType.getCode() + "~H");
                            if (origH == null||UniversalDate.isHigher(toYear,(String)origH.get(0))) {
                                additionalDataToIndex.put(itemType.getCode() + "~H", Collections.singletonList(toYear));
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case LINK:
                        data = value;
                        break;
                    default:
                        throw new RuntimeException("Unknown type");
                }
                additionalDataToIndex.computeIfAbsent(itemType.getCode(), k -> new ArrayList<>()).add(data);
                if (itemType.getType() == DataType.APU_REF && item.getTargetLabel() != null) {
                    List<String> itemTypeGroups = typesHolder.getItemGroupsForItemType(item.getType());
                    indexedApu.getRels().add(new IndexedApu.NestedRelation((String) data, item.getType(), itemTypeGroups, item.getTargetLabel(), data + "|" + item.getTargetLabel()));
                    additionalDataToIndex.computeIfAbsent(itemType.getCode() + "~LABEL", k -> new ArrayList<>()).add(item.getTargetLabel());
                    additionalDataToIndex.computeIfAbsent(itemType.getCode() + "~ID~LABEL", k -> new ArrayList<>()).add(data + "|" + item.getTargetLabel());
                }
            }
        }
		
		indexedApu.setContainsDigitalObjects(false);
		indexedApu.setDescription(apu.getDescription());
		indexedApu.setIncomingRelTypeGroups(null);
		indexedApu.setIncomingRelTypes(null);
		indexedApu.setName(indexedName);
		indexedApu.setType(apu.getType().toString());				
		var doc = converter.mapObject(indexedApu);
		doc.putAll(additionalDataToIndex);
		return doc;
	}
	
	public void createIndexes() {
		if (!operations.indexOps(IndexCoordinates.of("apu")).exists()) {
			Settings settings;
			try {
				settings = Settings.parse(settingsResource.getContentAsString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			var mapping = operations.indexOps(IndexCoordinates.of("apu")).createMapping(IndexedApu.class);
			var props = (Map<String, Object>) mapping.get("properties");
			props.putAll(createCustomMapping());
			operations.indexOps(IndexCoordinates.of("apu")).create(settings, mapping);
		}
		if (!operations.indexOps(IndexCoordinates.of("rels")).exists()) {
			Settings settings;
			try {
				settings = Settings.parse(settingsResource.getContentAsString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			var mapping = operations.indexOps(IndexCoordinates.of("rels")).createMapping(IndexedRelation.class);
			operations.indexOps(IndexCoordinates.of("rels")).create(settings, mapping);
		}
	}
	
	public void dropIndexes() {
		operations.indexOps(IndexCoordinates.of("apu")).delete();
		operations.indexOps(IndexCoordinates.of("rels")).delete();
	}
	
    private Map<String, Object> createCustomMapping() {
        Map<String, Object> customMapping = new HashMap<>();
        for (ItemType allItemType : typesHolder.getAllItemTypes()) {
            if (!allItemType.isIndexed()) {
                continue;
            }
            Map<String, Object> fieldProperties = new HashMap<>();
            String dataType;
            switch (allItemType.getType()) {
                case STRING:
                    dataType = "text";
                    if (allItemType.getIndexFolding() == null || allItemType.getIndexFolding()) { //defaults to folded
                        fieldProperties.put("analyzer", IndexConfig.FOLDING_AND_TOKENIZING_STOP);
                    } else if (Boolean.FALSE.equals(allItemType.getIndexFolding())&&Boolean.TRUE.equals(allItemType.getCaseInsensitive())) {
                        fieldProperties.put("analyzer", IndexConfig.TEXT_LONG_KEYWORD_CI);
                        fieldProperties.put("search_analyzer", IndexConfig.TEXT_LONG_KEYWORD_CI);
                    } else {  //unfolded also means untokenized for us
                        fieldProperties.put("analyzer", IndexConfig.TEXT_LONG_KEYWORD);
                    }
                    break;
                case ENUM:
                    dataType = "keyword";
                    break;
                case INTEGER:
                    dataType = "integer";
                    break;
                case APU_REF:
                    dataType = "keyword";
                    break;
                case UNITDATE:
                    dataType = "date_range";                	
                    break;
                case LINK:
                    dataType = "keyword";
                    break;
                case ITEM_AGGREG:
                    continue;
                default:
                    throw new RuntimeException("unknown type");
            }
            fieldProperties.put("type", dataType);
            customMapping.put(allItemType.getCode(), fieldProperties);
            if (allItemType.getType() == DataType.APU_REF) {
                Map<String, Object> labelFieldProperties = new HashMap<>();
                labelFieldProperties.put("analyzer", IndexConfig.FOLDING_AND_TOKENIZING);
                labelFieldProperties.put("type", "text");
                customMapping.put(allItemType.getCode() + "~LABEL", labelFieldProperties);
                customMapping.put(allItemType.getCode() + "~ID~LABEL", fieldProperties);
            }
            /*
            else if (allItemType.getType() == DataType.UNITDATE) {
            	Map<String, Object> machineDateProperties = new HashMap<>();
            	machineDateProperties.put("type", "integer_range");
            	customMapping.put(allItemType.getCode() + "~MACHINE", machineDateProperties);
            }*/
            else if (allItemType.getType() == DataType.UNITDATE) {
                var lowProperties = new HashMap<String,Object>();
                lowProperties.put("type", "date");
                customMapping.put(allItemType.getCode() + "~L", lowProperties);
                var highProperties = new HashMap<String,Object>();
                highProperties.put("type", "date");
                customMapping.put(allItemType.getCode() + "~H", highProperties);
            }
            
            
        }
        return customMapping;
    }

	
	
	
	/*
    public Map<String, IndexFieldNode> getDynamicFields() {
        Map<String, IndexFieldNode> dynamicFields = new HashMap<>();
        for (ItemType allItemType : typesHolder.getAllItemTypes()) {
            if (!allItemType.isIndexed()) {
                continue;
            }
            FieldType fieldType;
            String analyzer = null;
            Class<?> javaType;
            switch (allItemType.getType()) {
                case STRING:
                    fieldType = FieldType.Text;
                    javaType = String.class;
                    if (allItemType.getIndexFolding() == null || allItemType.getIndexFolding()) { //defaults to folded
                        analyzer = FOLDING_AND_TOKENIZING_STOP;
                    }
                    else {  //unfolded also means untokenized for us
                        analyzer = TEXT_LONG_KEYWORD;
                    }
                    break;
                case ENUM:
                case LINK:
                case APU_REF:
                    fieldType = FieldType.Keyword;
                    javaType = String.class;
                    break;
                case INTEGER:
                    fieldType = FieldType.Integer;
                    javaType = Integer.class;
                    break;
                case UNITDATE:
                    fieldType = FieldType.Date;
                    javaType = String.class;
                    break;
                case ITEM_AGGREG:
                    continue;
                default:
                    throw new RuntimeException("unknown type");
            }
            boolean fulltext = false;
            if (fieldType == FieldType.Text) {
                fulltext = true;
            }

            String fieldName = allItemType.getCode();
            IndexedFieldProps indexedFieldProps = new IndexedFieldProps(fieldType, true, analyzer, false);

            IndexFieldLeafNode indexFieldLeafNode = new IndexFieldLeafNode(IndexedApu.class, fieldName, javaType, indexedFieldProps, null, fulltext, 1.0f, new HashSet<>());
            dynamicFields.put(fieldName, indexFieldLeafNode);

            if (allItemType.getType() == DataType.APU_REF) {
                String labelFieldName = fieldName + "~LABEL";
                indexedFieldProps = new IndexedFieldProps(FieldType.Text, true, FOLDING_AND_TOKENIZING, false);
                IndexFieldLeafNode indexLabelFieldLeafNode = new IndexFieldLeafNode(IndexedApu.class, labelFieldName, String.class, indexedFieldProps, null, true, 1.0f, new HashSet<>());
                dynamicFields.put(labelFieldName, indexLabelFieldLeafNode);
                labelFieldName = fieldName + "~ID~LABEL";
                indexedFieldProps = new IndexedFieldProps(fieldType, true, null, false);
                indexLabelFieldLeafNode = new IndexFieldLeafNode(IndexedApu.class, labelFieldName, String.class, indexedFieldProps, null, false, 1.0f, new HashSet<>());
                dynamicFields.put(labelFieldName, indexLabelFieldLeafNode);
            }
        }
        return dynamicFields;
    }
*/
	
	
}
