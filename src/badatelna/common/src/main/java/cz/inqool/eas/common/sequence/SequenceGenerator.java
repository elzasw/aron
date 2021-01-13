package cz.inqool.eas.common.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;

/**
 * Class with synchronized methods for generating sequence, classes are divided into Synchronized and Transactional
 * because of duplicities. Method are synchronized and requires a new transaction explicitly by annotation.
 * In order to be used, this annotation must be in separate method
 */
public class SequenceGenerator {
    private SequenceRepository repository;

    private TransactionTemplate transactionTemplate;

    public synchronized Long generatePlain(@NotNull Sequence sequence) {
        return transactionTemplate.execute(status -> {
                    Long counter = sequence.getCounter();

                    sequence.setCounter(counter + 1);
                    repository.update(sequence);

                    return counter;
                }
        );
    }

    public synchronized String generate(@NotNull Sequence sequence) {
        return transactionTemplate.execute(status -> {
            Long counter = sequence.getCounter();

            sequence.setCounter(counter + 1);
            repository.update(sequence);

            DecimalFormat format = new DecimalFormat(sequence.getFormat());

            return format.format(counter);
        });
    }

    @Autowired
    public void setRepository(SequenceRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}
