package cz.inqool.eas.common.pdfa.mock;

import cz.inqool.eas.common.sequence.SequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for PDFA converter subsystem.
 *
 * If application wants to use PDFA converter subsystem,
 * it needs to extend this class and add {@link Configuration} annotation.
 *
 */
@Slf4j
public abstract class MockPdfaConfiguration {

    /**
     * Constructs {@link SequenceRepository} bean.
     */
    @Bean
    public MockPdfaConverter pdfaConverter() {
        return new MockPdfaConverter();
    }
}
