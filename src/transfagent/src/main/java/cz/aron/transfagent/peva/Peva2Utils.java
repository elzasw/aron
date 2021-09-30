package cz.aron.transfagent.peva;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.UniversalTimeRange;
import cz.aron.transfagent.transformation.CoreTypes;

public class Peva2Utils {
	
	public static void fillDateRange(UniversalTimeRange timeRange, Part partFundInfo, ApuSourceBuilder apusBuilder) {    	
    	String timeRangeFrom = timeRange.getTimeRangeFrom();
    	String timeRangeTo = timeRange.getTimeRangeTo();    	
    	var itemDateRange = new ItemDateRange();    	
    	if (timeRangeFrom!=null) {
        	itemDateRange.setF(getDate(timeRangeFrom));
        	itemDateRange.setFe(isEstimate(timeRangeFrom));    		
    	}    	
    	if (timeRangeTo!=null) {
        	itemDateRange.setTo(getDate(timeRangeTo));
        	itemDateRange.setToe(isEstimate(timeRangeTo));    		
    	}    	
    	itemDateRange.setFmt("Y-Y");
    	itemDateRange.setType(CoreTypes.UNIT_DATE);
    	itemDateRange.setVisible(true);    	
    	apusBuilder.addDateRange(partFundInfo, itemDateRange);
    }
    
    public static String getDate(String date) {
    	if (isEstimate(date)) {
    		return date.substring(1,date.length()-1);
    	} else {
    		return date;
    	}
    }
    
    public static boolean isEstimate(String date) {
    	return date.startsWith("[");
    }

}
