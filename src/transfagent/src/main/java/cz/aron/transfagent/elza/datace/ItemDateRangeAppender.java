package cz.aron.transfagent.elza.datace;

import java.util.List;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.transfagent.transformation.CoreTypes;

public class ItemDateRangeAppender {
    
    /**
     * Normalized from and to, this interval will be merged
     */    
    private LocalDateTimeRange dateRange;
        
    public ItemDateRangeAppender(final ItemDateRange itemDateRange) {
        // Copy source item
        var copied = ApuSourceBuilder.copyItem(itemDateRange);
        // appended is invisible by default
        copied.setVisible(false);
        dateRange = new LocalDateTimeRange(copied);
    }
    

    public void appendTo(Apu apu) {
        var items = ApuSourceBuilder.getItemDateRanges(apu, CoreTypes.PT_ARCH_DESC, CoreTypes.UNIT_DATE);
        var item = getCrossingItem(items);
        while (item != null) {
            // if merged item is visible then
            //    whole new item is also visible
            if(item.isVisible()) {
                dateRange.getDateRange().setVisible(true);
            }
            removeItemDateRange(apu, items, item.getDateRange());
            
            dateRange = dateRange.merge(item);
            
            item = getCrossingItem(items);
        }
        addItemDateRange(apu, dateRange.getDateRange());
    }

    /**
     * Return first crossing item
     * @param items
     * @return
     */
    private LocalDateTimeRange getCrossingItem(List<ItemDateRange> items) {
        for(ItemDateRange item : items) {
            var ldtr = new LocalDateTimeRange(item);
            if(dateRange.isRangeCrossing(ldtr)) {
                return ldtr;
            }
        }
        return null;
    }

    private void removeItemDateRange(Apu apu, List<ItemDateRange> items, ItemDateRange removeItem) {
        items.remove(removeItem);
        ApuSourceBuilder.removeItem(apu, removeItem);
    }

    private void addItemDateRange(Apu apu, ItemDateRange item) {
        var part = ApuSourceBuilder.getFirstPart(apu, CoreTypes.PT_ARCH_DESC);
        if (part == null) {
            part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_ARCH_DESC);
        }
        ApuSourceBuilder.addDateRange(part, item);
    }
}
