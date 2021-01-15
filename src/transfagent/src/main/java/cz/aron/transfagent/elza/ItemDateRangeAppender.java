package cz.aron.transfagent.elza;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.transformation.CoreTypes;

public class ItemDateRangeAppender {

    private ItemDateRange itemDateRange;
    private LocalDateTimeRange dateRange;
    private ApuSourceBuilder builder;

    public ItemDateRangeAppender(ItemDateRange itemDateRange) {
        this.itemDateRange = itemDateRange;
        dateRange = new LocalDateTimeRange(itemDateRange);
        builder = new ApuSourceBuilder();
    }

    public void appendTo(Apu apu) {
        List<ItemDateRange> items = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC);
        ItemDateRange item = getCrossingItem(items);
        while (item != null) {
            itemDateRange = mergeItemDateRangeTo(item);
            dateRange = new LocalDateTimeRange(itemDateRange);
            removeItemDateRange(apu, items, item);
            item = getCrossingItem(items);
        }
        addItemDateRange(apu, itemDateRange);
    }

    private ItemDateRange getCrossingItem(List<ItemDateRange> items) {
        for(ItemDateRange item : items) {
            if(dateRange.isRangeCrossing(new LocalDateTimeRange(item))) {
                return item;
            }
        }
        return null;
    }

    private ItemDateRange mergeItemDateRangeTo(ItemDateRange item) {
        var result = builder.createDateRange(item.getType(), item.getF(), item.isFe(), item.getTo(), item.isToe(), item.getFmt());
        var itemRange = new LocalDateTimeRange(result);
        if(dateRange.isBefore(itemRange)) {
            result.setF(itemDateRange.getF());
        }
        if(dateRange.isAfter(itemRange)) {
            result.setTo(itemDateRange.getTo());
        }
        return result;
    }

    private void removeItemDateRange(Apu apu, List<ItemDateRange> items, ItemDateRange removeItem) {
        items.remove(removeItem);
        for(Part part : apu.getPrts().getPart()) {
            List<Object> objects = part.getItms().getStrOrLnkOrEnm();
            for(Object obj : objects) {
                if(obj instanceof ItemDateRange) {
                    ItemDateRange item = (ItemDateRange) obj;
                    if(removeItem.equals(item)) {
                        objects.remove(obj);
                        break;
                    }
                }
            }
        }
    }

    private void addItemDateRange(Apu apu, ItemDateRange item) {
        for(Part part : apu.getPrts().getPart()) {
            if (part.getType().equals(CoreTypes.PT_ARCH_DESC)) {
                part.getItms().getStrOrLnkOrEnm().add(item);
            }
        }
    }

    class LocalDateTimeRange {
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

}
