package cz.inqool.eas.common.sequence;

import cz.inqool.eas.common.dictionary.DictionaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

@Slf4j
public class SequenceService extends DictionaryService<
        Sequence,
        SequenceDetail,
        SequenceList,
        SequenceCreate,
        SequenceUpdate,
        SequenceRepository
        > {

    private SequenceGenerator generator;

    @Transactional
    public String generateNextValue(String sequenceId) {
        Sequence sequence = getInternal(Sequence.class, sequenceId);

        return generator.generate(sequence);
    }

    @Transactional
    public String generateNextValueByCode(String code) {
        Sequence sequence = repository.findByCode(Sequence.class, code);
        if (sequence == null) {
            log.warn("Failed to get sequence with code = '{}'. Returning empty string.", code);
            return "";
        }

        return generator.generate(sequence);
    }

    @Autowired
    public void setGenerator(SequenceGenerator generator) {
        this.generator = generator;
    }
}
