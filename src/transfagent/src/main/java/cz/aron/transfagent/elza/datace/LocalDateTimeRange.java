package cz.aron.transfagent.elza.datace;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cz.aron.apux._2020.ItemDateRange;

public class LocalDateTimeRange {

    private final LocalDateTime from;

    private final LocalDateTime to;

    public LocalDateTimeRange(ItemDateRange dateRange) {
        from = LocalDateTime.parse(dateRange.getF(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        to = LocalDateTime.parse(dateRange.getTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    public boolean isRangeCrossing(LocalDateTimeRange range) {
        return !(to.isBefore(range.getFrom()) || from.isAfter(range.getTo()));
    }

    public boolean isBefore(LocalDateTimeRange range) {
        return from.isBefore(range.getFrom());
    }

    public boolean isAfter(LocalDateTimeRange range) {
        return to.isAfter(range.getTo());
    }
}
