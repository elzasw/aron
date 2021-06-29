package cz.inqool.eas.common.reporting.generator;

import cz.inqool.eas.common.exception.ForbiddenOperation;
import cz.inqool.eas.common.exception.MissingObject;
import cz.inqool.eas.common.reporting.access.ReportAccessChecker;
import cz.inqool.eas.common.reporting.dto.ReportDefinition;
import cz.inqool.eas.common.reporting.exception.ReportGeneratorMissingException;
import cz.inqool.eas.common.reporting.report.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.inqool.eas.common.utils.AssertionUtils.isTrue;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;
import static java.util.Comparator.comparing;

@Slf4j
public class DelegateReportGenerator {
    private ReportAccessChecker accessChecker;

    private List<ReportGenerator<?, ?>> generators = new ArrayList<>();

    public Report generate(String definitionId, Map<String, Object> configuration) {
        ReportDefinition definition = getDefinition(definitionId);
        notNull(definition, () -> new MissingObject(ReportDefinition.class, definitionId));
        isTrue(accessChecker.checkAccess(definition), () -> new ForbiddenOperation(ForbiddenOperation.ErrorCode.NOT_ALLOWED));

        ReportGenerator<?, ?> generator = generators
                .stream()
                .filter(g -> g.checkSupport(definitionId))
                .findFirst()
                .orElse(null);

        if (generator == null) {
            log.error("Failed to find relevant generator for report.");
            throw new ReportGeneratorMissingException("Failed to find relevant generator for report.");
        }

        log.trace("Start generating report.");
        Report report = generator.generate(configuration);
        log.trace("Finished generating report.");

        return report;
    }

    public ReportDefinition getDefinition(String id) {
        return getDefinitions()
                .stream().filter(d -> d.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<ReportDefinition> getDefinitions() {
        return generators
                .stream()
                .map(ReportGenerator::getDefinition)
                .sorted(comparing(ReportDefinition::getOrder))
                .collect(Collectors.toList());
    }

    public List<ReportDefinition> getAllowedDefinitions() {
        List<ReportDefinition> definitions = getDefinitions();

        Map<String, Boolean> accessMap = accessChecker.checkAccess(definitions);

        return definitions
                .stream()
                .filter(definition -> accessMap.get(definition.getId()))
                .collect(Collectors.toList());
    }

    @Autowired
    public void setAccessChecker(ReportAccessChecker accessChecker) {
        this.accessChecker = accessChecker;
    }

    @Autowired(required = false)
    public void setGenerators(List<ReportGenerator<?, ?>> generators) {
        this.generators = generators;
    }
}
