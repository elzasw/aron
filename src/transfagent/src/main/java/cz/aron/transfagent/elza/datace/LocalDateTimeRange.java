package cz.aron.transfagent.elza.datace;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ItemDateRange;

public class LocalDateTimeRange {

    private final LocalDateTime from;

    private final LocalDateTime to;
    
    private final ItemDateRange dateRange;

    public LocalDateTimeRange(final ItemDateRange dateRange) {
        this.dateRange = dateRange;
        from = LocalDateTime.parse(dateRange.getF(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        to = LocalDateTime.parse(dateRange.getTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    private LocalDateTimeRange(final ItemDateRange dateRange, 
                               final LocalDateTime dateRangeFrom, 
                               final LocalDateTime dateRangeTo) {
        this.dateRange = dateRange;
        this.from = dateRangeFrom;
        this.to = dateRangeTo;
    }

    public ItemDateRange getDateRange() {
        return dateRange;
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

    /**
     * Return new object as a result of merge
     * @param item
     * @return
     */
    public LocalDateTimeRange merge(LocalDateTimeRange item) {
        var localItem = ApuSourceBuilder.copyItem(item.dateRange);
        var localItemFrom = item.from;
        var localItemTo = item.to;

        if(from.isBefore(localItemFrom)) {
            localItem.setF(dateRange.getF());
            localItemFrom = from;
        }
        if(to.isAfter(localItemTo)) {
            localItem.setTo(dateRange.getTo());
            localItemTo = to;
        }
        var result = new LocalDateTimeRange(localItem, localItemFrom, localItemTo);
        return result;
    }

    public boolean isVisible() {
        Boolean b = dateRange.isVisible();
        return b==null||b.booleanValue();
    }
}
