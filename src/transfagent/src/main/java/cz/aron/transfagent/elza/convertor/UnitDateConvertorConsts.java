package cz.aron.transfagent.elza.convertor;

public class UnitDateConvertorConsts {

    /**
     * Zkratka století
     */
    public static final String CENTURY = "C";

    /**
     * Zkratka roku
     */
    public static final String YEAR = "Y";

    /**
     * Zkratka roku s měsícem
     */
    public static final String YEAR_MONTH = "YM";

    /**
     * Zkratka datumu
     */
    public static final String DATE = "D";

    /**
     * Zkratka datumu s časem
     */
    public static final String DATE_TIME = "DT";

    /**
     * Oddělovač pro interval
     */
    public static final String DEFAULT_INTERVAL_DELIMITER = "-";

    /**
     * Šablona pro odhad
     */
    public static final String ESTIMATED_TEMPLATE = "[%s]";

    /**
     * Maximální negativní datum 5 x 1024 let
     */
    public static final long MAX_NEGATIVE_DATE = 60L*60L*24L*365L*1024L*5L;
}
