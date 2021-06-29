package cz.inqool.eas.common.reporting.generator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.eas.common.reporting.dto.ReportDefinition;
import cz.inqool.eas.common.reporting.report.Report;
import cz.inqool.eas.common.reporting.report.ReportColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class ReportGenerator<INPUT, ITEM> {
    private final ObjectMapper objectMapper;

    private final Class<INPUT> configurationClass;

    public ReportGenerator(Class<INPUT> configurationClass) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configurationClass = configurationClass;
    }

    @Transactional
    public Report generate(Map<String, Object> configuration) {
        String definitionId = getDefinition().getId();

        INPUT input = objectMapper.convertValue(configuration, configurationClass);


        GeneratorResult<ITEM> result = generateInternal(input);
        List<ReportColumn> columns = result.getColumns();
        List<ITEM> items = result.getItems();

        // @SuppressWarnings("unchecked")
        List data = items.stream()
                .map(item -> objectMapper.convertValue(item, Map.class))
                .collect(Collectors.toList());

        Report report = new Report();
        report.setDefinitionId(definitionId);
        report.setConfiguration(configuration);
        report.setData(data);
        report.setColumns(columns);

        return report;
    }

    protected boolean checkSupport(String definitionId) {
        return getDefinition().getId().equals(definitionId);
    }

    protected abstract ReportDefinition getDefinition();

    protected abstract GeneratorResult<ITEM> generateInternal(INPUT input);

    @AllArgsConstructor
    @Data
    public static class GeneratorResult<ITEM> {
        List<ReportColumn> columns;
        List<ITEM> items;
    }
}
