package cz.aron.transfagent.peva;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
    	ApuSourceBuilder.addDateRange(partFundInfo, itemDateRange);
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
    	StringJoiner sj = new StringJoiner(" ");    	    	
    	if (StringUtils.isNotBlank(timeRange.getPrior())) {
    		sj.add("("+timeRange.getPrior()+")");    		
    	}    	    	    	
    	var timeRangeFrom = timeRange.getTimeRangeFrom();
    	var timeRangeTo = timeRange.getTimeRangeTo();
    	if (timeRangeFrom!=null) {
    		if (timeRangeTo!=null&&!timeRangeFrom.equals(timeRangeTo)) {
    			sj.add(timeRangeFrom+ "-" + timeRangeTo);
    		} else {
    			sj.add(timeRangeFrom);
    		}
    	}
    	if (StringUtils.isNotBlank(timeRange.getPosterior())) {
    		sj.add("("+timeRange.getPosterior()+")");
    	}
    	return sj.toString();
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
	
	
	private static final Map<String,String> CAM_CODE_MAP = new HashMap<>();
	
	static {
		CAM_CODE_MAP.put("CRC_BIRTH", CoreTypes.CRC_BIRTH_DATE);
		CAM_CODE_MAP.put("CRC_RISE", CoreTypes.CRC_RISE_DATE);
		CAM_CODE_MAP.put("CRC_BEGINSCOPE", CoreTypes.CRC_BEGINSCOPE_DATE);
		CAM_CODE_MAP.put("CRC_FIRSTMBIRTH", CoreTypes.CRC_FIRSTMBIRTH_DATE);
		CAM_CODE_MAP.put("CRC_FIRSTWMENTION", CoreTypes.CRC_FIRSTWMENTION_DATE);
		CAM_CODE_MAP.put("CRC_ORIGIN", CoreTypes.CRC_ORIGIN_DATE);
		CAM_CODE_MAP.put("CRC_BEGINVALIDNESS", CoreTypes.CRC_BEGINVALIDNESS_DATE);
	}
	
	public static String transformCamCode(String camCode) {
		var converted = CAM_CODE_MAP.get(camCode);
		if (converted!=null) {
			return converted;
		} else {
			return camCode;
		}
	}
	
	
	private static Pattern DATE_PATTERN = Pattern.compile("^(\\d{1,2}).(\\d{1,2}).(\\d+)$");
	private static Pattern DATE_PATTERN_YEAR = Pattern.compile("^\\d+$");
	
	/**
	 * Prevede datum z formatu dd.mm.yyyy na ItemDateRange
	 * @param date
	 * @param type typ nastavovany 
	 * @return
	 */
	public static ItemDateRange parseDating(String date, String type) {
		var idr = new ItemDateRange();
		idr.setType(type);
		var matcher = DATE_PATTERN_YEAR.matcher(date);
		if (matcher.matches()) {
			var localDate = LocalDate.of(Integer.parseInt(matcher.group(0)), 1, 1);
			idr.setFmt("Y-Y");
			idr.setF(localDate.with(TemporalAdjusters.firstDayOfYear()).atTime(0,0,0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			idr.setFe(false);
			idr.setTo(localDate.with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59).toString());
			idr.setToe(false);
			idr.setVisible(true);
			return idr;
		}
		matcher = DATE_PATTERN.matcher(date);
		if (matcher.matches()) {
			var localDate = LocalDate.of(Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(1)));
			idr.setFmt("D-D");
			idr.setF(localDate.atTime(0,0,0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			idr.setFe(false);
			idr.setTo(localDate.atTime(23, 59, 59).toString());
			idr.setToe(false);
			idr.setVisible(true);
			return idr;
		}
		return null;
	}

	
}
