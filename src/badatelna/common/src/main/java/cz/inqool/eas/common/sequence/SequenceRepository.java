package cz.inqool.eas.common.sequence;

import cz.inqool.eas.common.dictionary.DictionaryRepository;
import cz.inqool.eas.common.dictionary.index.DictionaryIndex;
import cz.inqool.eas.common.dictionary.store.DictionaryStore;

public class SequenceRepository extends DictionaryRepository<
        Sequence,
        Sequence,
        SequenceIndexedObject,
        DictionaryStore<Sequence, Sequence, QSequence>,
        DictionaryIndex<Sequence, Sequence, SequenceIndexedObject>> {
}
