package cz.aron.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.esotericsoftware.kryo.Kryo;

import cz.aron.domain.ApuEntity;
import cz.aron.domain.Relation;
import cz.aron.domain.dto.IdLabelDto;

/**
 * Engine-neutral indexing facade: converts domain entities to search documents
 * (via {@link ApuDocumentBuilder}) and hands them to the configured
 * {@link SearchIndex} adapter. The import pipeline and the startup bootstrap
 * talk only to this service - never to an engine adapter directly.
 */
@Service
public class IndexingService {

	private final SearchIndex searchIndex;

	private final ApuDocumentBuilder documentBuilder;

	public IndexingService(SearchIndex searchIndex, ApuDocumentBuilder documentBuilder) {
		this.searchIndex = searchIndex;
		this.documentBuilder = documentBuilder;
	}

	/**
	 * Indexes the given APUs (those flagged as indexed). The parts blob of each one
	 * is deserialized with the supplied {@link Kryo}, so a caller indexing a whole
	 * batch borrows a single instance (see {@code KryoSerializer.doWithKryo})
	 * instead of one per APU.
	 */
	public void indexApus(Kryo kryo, Collection<ApuEntity> apus, Map<String, IdLabelDto> apuRefLabels) {
		var documents = new ArrayList<ApuDocument>(apus.size());
		for (var apu : apus) {
			if (apu.isIndexed()) {
				documents.add(documentBuilder.build(kryo, apu, apuRefLabels));
			}
		}
		if (!documents.isEmpty()) {
			searchIndex.indexApus(documents);
		}
	}

	public void indexRels(Collection<Relation> rels) {
		var documents = new ArrayList<RelationDocument>(rels.size());
		for (var rel : rels) {
			if (rel.isRemove()) {
				continue;
			}
			documents.add(new RelationDocument(rel.getSource().toString(), rel.getRelation(),
					rel.getTarget().toString()));
		}
		if (!documents.isEmpty()) {
			searchIndex.indexRelations(documents);
		}
	}

	public void deleteApus(long apuSourceId) {
		searchIndex.deleteApusBySource(apuSourceId);
	}

	/** Read side of the port - the only search entry point of the new API. */
	public ApuSearchResult search(ApuSearchQuery query) {
		return searchIndex.search(query);
	}

	public void createSchema() {
		searchIndex.createSchema();
	}

	public void dropSchema() {
		searchIndex.dropSchema();
	}

	public Long storedSchemaCrc() {
		return searchIndex.storedSchemaCrc();
	}

	public void storeSchemaCrc(long crc) {
		searchIndex.storeSchemaCrc(crc);
	}

}
