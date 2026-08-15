package cz.aron.search;

/** Engine-neutral representation of one APU-to-APU relation in the search index. */
public record RelationDocument(String source, String relation, String target) {
}
