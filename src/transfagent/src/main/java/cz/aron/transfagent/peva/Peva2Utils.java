package cz.aron.transfagent.peva;

import java.util.regex.Pattern;

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

    public static String getAsString(UniversalTimeRange timeRange) {    	
    	var timeRangeFrom = timeRange.getTimeRangeFrom();
    	var timeRangeTo = timeRange.getTimeRangeTo();
    	if (timeRangeFrom!=null) {
    		if (timeRangeTo!=null&&!timeRangeFrom.equals(timeRangeTo)) {
    			return timeRangeFrom + "-" + timeRangeTo;
    		} else {
    			return timeRangeFrom;
    		}
    	} else {
    		return null;
    	}    	
    }
    
    private static Pattern pattern = Pattern.compile("(\\\r[^\n])|(\\\r$)");
    
    /**
     * Opravi samostatne \r na \r\n
     * @param original vstupni retezec
     * @return opraveny retezec
     */
	public static String correctLineSeparators(String original) {
		if (original == null) {
			return null;
		}
		var matcher = pattern.matcher(original);
		if (!matcher.find()) {
			return original;
		}
		int lastIndex = 0;
		StringBuilder output = new StringBuilder();
		do {
			output.append(original, lastIndex, matcher.start()).append("\r\n");
			if (matcher.end()-matcher.start()>1) {
				output.append(original, matcher.end() - 1, matcher.end());
			}
			lastIndex = matcher.end();
		} while (matcher.find());
		if (lastIndex < original.length()) {
			output.append(original, lastIndex, original.length());
		}
		return output.toString();
	}

}
