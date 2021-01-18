package cz.aron.transfagent.elza.datace;

import java.util.List;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
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
        var items = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        var item = getCrossingItem(items);
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
        var result = builder.copyItem(item);
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
        builder.removeItem(apu, removeItem);
    }

    private void addItemDateRange(Apu apu, ItemDateRange item) {
        var part = builder.getFirstPart(apu, CoreTypes.PT_ARCH_DESC);
        if (part == null) {
            part = builder.addPart(apu, CoreTypes.PT_ARCH_DESC);
        }
        builder.addDateRange(part, item);
    }
}
