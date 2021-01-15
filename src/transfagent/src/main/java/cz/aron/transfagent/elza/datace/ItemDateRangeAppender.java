package cz.aron.transfagent.elza.datace;

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
        var items = builder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC);
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
            var objects = part.getItms().getStrOrLnkOrEnm();
            for(Object obj : objects) {
                if(obj instanceof ItemDateRange) {
                    var item = (ItemDateRange) obj;
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
}
