package cz.aron.transfagent.elza;

import java.util.HashMap;
import java.util.Map;

/**
 * Seznam typu/constant v Elza
 *
 *
 */
public abstract class ElzaTypes {

    public static final String NM_MAIN = "NM_MAIN";
    public static final String NM_MINOR = "NM_MINOR";
    public static final String NM_DEGREE_PRE = "NM_DEGREE_PRE";
    public static final String NM_DEGREE_POST = "NM_DEGREE_POST";

    public static final String NM_SUP_GEN = "NM_SUP_GEN";
    public static final String NM_SUP_CHRO = "NM_SUP_CHRO";
    public static final String NM_SUP_GEO = "NM_SUP_GEO";

    public static final String NM_TYPE = "NM_TYPE";
    public static final String NT_ACRONYM = "NT_ACRONYM";

    public static final String NM_SUPS[] = {
            NM_SUP_GEN, NM_SUP_CHRO, NM_SUP_GEO
    };

    public static final String IDN_TYPE = "IDN_TYPE";
    public static final String IDN_VALUE = "IDN_VALUE";

    // Stručná charakteristika
    public static final String BRIEF_DESC = "BRIEF_DESC";

    // Administrativní zařazení
    public static final String GEO_ADMIN_CLASS = "GEO_ADMIN_CLASS";

    public static final String CORP_PURPOSE = "CORP_PURPOSE";
    public static final String FOUNDING_NORMS = "FOUNDING_NORMS";
    public static final String HISTORY = "HISTORY";
    public static final String GENEALOGY = "GENEALOGY";
    public static final String BIOGRAPHY = "BIOGRAPHY";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String SCOPE_NORMS = "SCOPE_NORMS";
    public static final String CORP_STRUCTURE = "CORP_STRUCTURE";
    public static final String SOURCE_INFO = "SOURCE_INFO";
    public static final String SOURCE_LINK = "SOURCE_LINK";

    public static final String REL_ENTITY = "REL_ENTITY";

    public static final String CRE_CLASS = "CRE_CLASS";
    public static final String CRE_DATE = "CRE_DATE";
    public static final String CRE_TYPE = "CRE_TYPE";    
    public static final Map<String, String> creTypeMap = new HashMap<>();
    static {
        creTypeMap.put("CRT_ESTAB", "vznik zřízením/založením");
        creTypeMap.put("CRT_COMMENCEMENT", "vznik zahájením činnosti");
        creTypeMap.put("CRT_PREDECESSOR", "vznik změnou předchůdce");
        creTypeMap.put("CRT_RECORDSENTRY", "vznik zápisem do evidence");
        creTypeMap.put("CRT_UNSPECIFIED", "nespecifikovaný vznik");
        creTypeMap.put("CRT_CREATION", "vytvoření");
        creTypeMap.put("CRT_EDITION", "vydání");
        creTypeMap.put("CRT_FIRSTREALIZATION", "první realizace");
        creTypeMap.put("CRT_VALIDITYBEGIN", "počátek platnosti");
    }
    public static final String EXT_CLASS = "EXT_CLASS";
    public static final String EXT_DATE = "EXT_DATE";
    public static final String EXT_TYPE = "EXT_TYPE";
    public static final Map<String, String> extTypeMap = new HashMap<>();
    static {
        // extTypeMap
    }

    public static final String ZP2015_EXTRA_UNITS = "ZP2015_EXTRA_UNITS";
    public static final String ZP2015_UNIT_SUBTYPE = "ZP2015_UNIT_SUBTYPE";
    public static final String ZP2015_RECORD_TYPE = "ZP2015_RECORD_TYPE";
    public static final String ZP2015_LANGUAGE = "ZP2015_LANGUAGE";
    public static final String ZP2015_UNIT_DATE = "ZP2015_UNIT_DATE";
    public static final String ZP2015_DATE_OTHER = "ZP2015_DATE_OTHER";
    public static final String ZP2015_ENTITY_ROLE = "ZP2015_ENTITY_ROLE";

    public static final String ZP2015_ORIGINATOR = "ZP2015_ORIGINATOR";
    public static final String ZP2015_POSITION = "ZP2015_POSITION";
    public static final String ZP2015_ORIENTATION = "ZP2015_ORIENTATION";
    public static final String ZP2015_ITEM_MAT = "ZP2015_ITEM_MAT";
    public static final String ZP2015_PART = "ZP2015_PART";

    public static final String ZP2015_WRITING = "ZP2015_WRITING";
    public static final String ZP2015_CORROBORATION = "ZP2015_CORROBORATION";
    public static final String ZP2015_IMPRINT_COUNT = "ZP2015_IMPRINT_COUNT";
    public static final String ZP2015_IMPRINT_ORDER = "ZP2015_IMPRINT_ORDER";
    public static final String ZP2015_LEGEND = "ZP2015_LEGEND";
    public static final String ZP2015_MOVIE_LENGTH = "ZP2015_MOVIE_LENGTH";
    public static final String ZP2015_RECORD_LENGTH = "ZP2015_RECORD_LENGTH";
    public static final String ZP2015_ITEM_LINK = "ZP2015_ITEM_LINK";
        

    public static final Map<String, String> unitTypeMap = new HashMap<>();
    static {
        unitTypeMap.put("ZP2015_UNIT_TYPE_LIO", "listina do roku 1850");
        unitTypeMap.put("ZP2015_UNIT_TYPE_LIP", "listina po roce 1850");
        unitTypeMap.put("ZP2015_UNIT_TYPE_UKN", "úřední kniha");
        unitTypeMap.put("ZP2015_UNIT_TYPE_RKP", "rukopis");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PPR", "podací protokol");
        unitTypeMap.put("ZP2015_UNIT_TYPE_IND", "index");
        unitTypeMap.put("ZP2015_UNIT_TYP_ELE", "elench");
        unitTypeMap.put("ZP2015_UNIT_TYPE_REP", "repertář");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT", "kartotéka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PEC", "pečetidlo");
        unitTypeMap.put("ZP2015_UNIT_TYPE_RAZ", "razítko");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTD", "samostatná pečeť, odlitek pečeti, otisk typáře");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTC", "kopie otisku");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MAP", "mapa");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ATL", "atlas");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TVY", "technický výkres");
        unitTypeMap.put("ZP2015_UNIT_TYPE_GLI", "grafický list");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KRE", "kresba");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSN", "fotografie na papírové podložce");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSD", "fotografická deska");
        unitTypeMap.put("ZP2015_UNIT_TYPE_LFI", "listový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_SFI", "svitkový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KIN", "kinofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MF", "mikrofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MFS", "mikrofiš");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FAL", "fotoalbum");
        unitTypeMap.put("ZP2015_UNIT_TYPE_DFO", "digitální fotografie");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KZA", "kinematografický záznam");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ZZA", "zvukový záznam");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIO", "tisk do roku 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIP", "tisk po roce 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_POH", "pohlednice");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PKT", "plakát");
        unitTypeMap.put("ZP2015_UNIT_TYPE_CPA", "cenný papír");
        unitTypeMap.put("ZP2015_UNIT_TYPE_STO", "štoček");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PNP", "předmět numizmatické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PFP", "předmět faleristické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_JIN", "jiná");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_BOX", "kartotéční zásuvka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_ITEM", "kartotéční lístek");
    }

    public static final Map<String, String> extraUnitTypeMap = new HashMap<>();
    static {
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LIO",
                             "listina do roku 1850");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LIP",
                             "listina po roce 1850");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_UKN",
                             "úřední kniha");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_RKP",
                             "rukopis");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PPR",
                             "podací protokol");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_IND",
                             "index");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ELE",
                             "elench");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_REP",
                             "repertář");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KTT",
                             "kartotéka");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PEC",
                             "pečetidlo");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_RAZ",
                             "razítko");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_OTD",
                             "samostatné pečeť, odlitek pečeti a otisk typáře");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MAP", "mapa");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ATL", "atlas");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TVY", "technický výkres");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_GLI", "grafický list");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KRE", "kresba");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FSN", "fotografie na papírové podložce");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FSD", "fotografická deska");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_LFI", "listový film");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_SFI", "svitkový film");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KIN", "kinofilm");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MF", "mikrofilm");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_MFS", "mikrofiš");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_FAL", "fotoalbum");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_DFO", "digitální fotografie");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_KZA", "kinematografický záznam");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_ZZA", "zvukový záznam");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TIO", "tisk do roku 1800");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_TIP", "tisky po roce 1800");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_POH", "pohlednice");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PKT", "plakát");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_CPA", "cenný papír");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_STO", "štoček");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PNP", "předmět numizmatické povahy");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_PFP", "předmět faleristické povahy");
        extraUnitTypeMap.put("ZP2015_EXTRA_UNITS_JIN", "jiná");
    }

    public static Map<String, String> subtypeMap = new HashMap<>();
    static {
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_MTR", "matrika");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_HDB", "hudebnina");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_STV", "vyznamenání / cena");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_PA", "právní akt");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_STVR", "udělení / propůjčení vyznamenání / ceny");
        subtypeMap.put("ZP2015_UNIT_SUBTYPE_JMF", "jmenování / ustanovení do funkce</name");
    }

    public static Map<String, String> recordTypeMap = new HashMap<>();
    static {
        recordTypeMap.put("ZP2015_RECORD_TYPE_BORN", "záznamy narozených");
        recordTypeMap.put("ZP2015_RECORD_TYPE_DIED", "záznamy zemřelých");
        recordTypeMap.put("ZP2015_RECORD_TYPE_MARRIED", "záznamy oddaných");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_BORN", "rejstřík narozených");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_DIED", "rejstřík zemřelých");
        recordTypeMap.put("ZP2015_RECORD_TYPE_INDEX_MARRIED", "rejstřík oddaných");
    }

    public static Map<String, String> roleSpecMap = new HashMap<>();
    static {
        roleSpecMap.put("ZP2015_ENTITY_ROLE_1", "autor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_2",
                        "autor dialogu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_4", "autor fotografií");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_5",
                        "skladatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_10",
                        "autor textové složky/textař");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_6",
                        "autor hudby/skladatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_7",
                        "autor choreografie/choreograf");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_8",
                        "autor komentáře");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_9",
                        "autor námětu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_11",
                        "autor textu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_3",
                        "autor doprovodnéno textu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_12",
                        "autor triků a speciálních efektů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_13",
                        "autor výtvarné a obrazové stránky");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_14",
                        "autor výtvarné stránky");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_15",
                        "vydavatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_16",
                        "vydavatel / nakladatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_17",
                        "pečetitel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_18",
                        "produkční společnost/producent");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_19",
                        "objednatel / příjemce");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_20",
                        "distributor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_21",
                        "příjemce");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_22",
                        "žadatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_23",
                        "držitel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_24",
                        "odesilatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_25",
                        "schvalovatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_26",
                        "stavitel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_27",
                        "režisér");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_28",
                        "scénárista");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_29",
                        "kameraman");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_30",
                        "interpret hudby");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_31",
                        "fotograf");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_32",
                        "redaktor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_33",
                        "kartograf");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_34",
                        "editor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_35",
                        "editor / redaktor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_36",
                        "kreslič");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_37",
                        "majitel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_38",
                        "tvůrce technického zpracování");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_39",
                        "tvůrce výtvarné stránky");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_40",
                        "dramaturg");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_41",
                        "střih/střihač");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_42",
                        "zvuk/zvukař");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_43",
                        "účinkující");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_44",
                        "překladatel");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_45",
                        "lektor");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_46",
                        "svědek");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_47",
                        "ručitel (rukojmě)");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_48",
                        "písař");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_49",
                        "zpracovatel nosičů záznamů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_50", "výrobce nosičů záznamů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_51", "tiskárna");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_52", "tiskárna/tiskař");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_53", "výrobce");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_54", "výrobce typářů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_55", "výrobce odlitků otisků / otisků typářů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_56", "místo natáčení");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_57", "místo vydavatele");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_58", "místo vydání");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_59", "místo vydání dokumentů");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_60", "místo výroby");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_61", "místo vzniku jednotky popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_62", "místo vzniku předlohy popisované kopie");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_63", "typové označení a název výrobku a typové stavby");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_64", "entita zachycená jednotkami popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_65", "ostatní entita zachycená jednotkami popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_66", "osoba/bytost zachycená jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_67", "rod/rodina zachycený/á jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_68", "korporace zachycená jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_69", "událost zachycená jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_70", "dílo/výtvor zachycené/ý jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_71", "geografický objekt zachycený jednotkou popisu");
        roleSpecMap.put("ZP2015_ENTITY_ROLE_72", "obecný pojem vztahující se k jednotce popisu");
    }

    public static Map<String, String> dateOtherMap = new HashMap<>();
    static {
        dateOtherMap.put("ZP2015_DATE_OF_CONTENT", "DATE_OF_CONTENT");
        dateOtherMap.put("ZP2015_DATE_DECLARED", "DATE_DECLARED");
        dateOtherMap.put("ZP2015_DATE_ORIG", "DATE_ORIG");
        dateOtherMap.put("ZP2015_DATE_OF_COPY", "DATE_OF_COPY");
        dateOtherMap.put("ZP2015_DATE_SEALING", "DATE_SEALING");
        dateOtherMap.put("ZP2015_DATE_ACT_PUBLISHING", "DATE_ACT_PUBLISHING");
        dateOtherMap.put("ZP2015_DATE_INSERT", "DATE_INSERT");
        dateOtherMap.put("ZP2015_DATE_MOLD_CREATION", "DATE_MOLD_CREATION");
        dateOtherMap.put("ZP2015_DATE_USAGE", "DATE_USAGE");
        dateOtherMap.put("ZP2015_DATE_PUBLISHING", "DATE_PUBLISHING");
        dateOtherMap.put("ZP2015_DATE_MAP_UPDATE", "DATE_MAP_UPDATE");
        dateOtherMap.put("ZP2015_DATE_CAPTURING", "DATE_CAPTURING");
        dateOtherMap.put("ZP2015_DATE_RECORDING", "DATE_RECORDING");
        dateOtherMap.put("ZP2015_DATE_AWARDING", "DATE_AWARDING");
        dateOtherMap.put("ZP2015_DATE_AWARD_CER", "DATE_AWARD_CER");
        dateOtherMap.put("ZP2015_DATE_WITHDRAWAL", "DATE_WITHDRAWAL");
    }
    
    /**
     * Zjednodussene indexovani jine datace
     */
    public static Map<String, String> dateOtherMapIndex = new HashMap<>();    
    static {
        dateOtherMapIndex.put("ZP2015_DATE_DECLARED", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_ORIG", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_OF_COPY", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_SEALING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_ACT_PUBLISHING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_INSERT", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_MOLD_CREATION", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_USAGE", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_PUBLISHING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_MAP_UPDATE", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_CAPTURING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_RECORDING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_AWARDING", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_AWARD_CER", "DATE_OTHER");
        dateOtherMapIndex.put("ZP2015_DATE_WITHDRAWAL", "DATE_OTHER");
    }

    public static Map<String, String> languageTypeMap = new HashMap<>();
    static {
        languageTypeMap.put("ZP2015_LANGUAGE_1",
                            "blíže neurčený indoevropský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_2",
                            "blíže neurčený baltský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_3",
                            "litevština");

        languageTypeMap.put("ZP2015_LANGUAGE_4",
                            "lotyština");

        languageTypeMap.put("ZP2015_LANGUAGE_5",
                            "blíže neurčený germánský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_6",
                            "afrikánština");

        languageTypeMap.put("ZP2015_LANGUAGE_7",
                            "alsaština");

        languageTypeMap.put("ZP2015_LANGUAGE_8",
                            "angličtina");

        languageTypeMap.put("ZP2015_LANGUAGE_9",
                            "dánština");

        languageTypeMap.put("ZP2015_LANGUAGE_10",
                            "dolnoněmčina");

        languageTypeMap.put("ZP2015_LANGUAGE_11",
                            "faerština");

        languageTypeMap.put("ZP2015_LANGUAGE_12",
                            "fríština");

        languageTypeMap.put("ZP2015_LANGUAGE_13",
                            "holandština (nizozemština)");

        languageTypeMap.put("ZP2015_LANGUAGE_14",
                            "islandština");

        languageTypeMap.put("ZP2015_LANGUAGE_15",
                            "jidiš");

        languageTypeMap.put("ZP2015_LANGUAGE_16",
                            "judendeutsch");

        languageTypeMap.put("ZP2015_LANGUAGE_17",
                            "lucemburština");

        languageTypeMap.put("ZP2015_LANGUAGE_18",
                            "němčina");

        languageTypeMap.put("ZP2015_LANGUAGE_19",
                            "norština");

        languageTypeMap.put("ZP2015_LANGUAGE_20",
                            "švédština");

        languageTypeMap.put("ZP2015_LANGUAGE_21",
                            "blíže neurčený keltský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_22",
                            "bretonština");

        languageTypeMap.put("ZP2015_LANGUAGE_23",
                            "irština");

        languageTypeMap.put("ZP2015_LANGUAGE_24",
                            "skotská gaelština");

        languageTypeMap.put("ZP2015_LANGUAGE_25",
                            "velština");

        languageTypeMap.put("ZP2015_LANGUAGE_26",
                            "blíže neurčený románský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_27",
                            "francouzština");

        languageTypeMap.put("ZP2015_LANGUAGE_28",
                            "galicijština");

        languageTypeMap.put("ZP2015_LANGUAGE_29",
                            "italština");

        languageTypeMap.put("ZP2015_LANGUAGE_30",
                            "katalánština");

        languageTypeMap.put("ZP2015_LANGUAGE_31",
                            "korsičtina");

        languageTypeMap.put("ZP2015_LANGUAGE_32",
                            "latina");

        languageTypeMap.put("ZP2015_LANGUAGE_33",
                            "moldavština");

        languageTypeMap.put("ZP2015_LANGUAGE_34",
                            "okcitánština");

        languageTypeMap.put("ZP2015_LANGUAGE_35",
                            "portugalština");

        languageTypeMap.put("ZP2015_LANGUAGE_36",
                            "rétorománština");

        languageTypeMap.put("ZP2015_LANGUAGE_37",
                            "rumunština");

        languageTypeMap.put("ZP2015_LANGUAGE_38",
                            "španělština");

        languageTypeMap.put("ZP2015_LANGUAGE_39",
                            "blíže neurčený slovanský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_40",
                            "běloruština");

        languageTypeMap.put("ZP2015_LANGUAGE_41",
                            "bosenština");

        languageTypeMap.put("ZP2015_LANGUAGE_42",
                            "bulharština");

        languageTypeMap.put("ZP2015_LANGUAGE_43",
                            "čeština");

        languageTypeMap.put("ZP2015_LANGUAGE_44",
                            "chorvatština");

        languageTypeMap.put("ZP2015_LANGUAGE_45",
                            "kašubština");

        languageTypeMap.put("ZP2015_LANGUAGE_46",
                            "lužická srbština dolní (dolnolužičtina)");

        languageTypeMap.put("ZP2015_LANGUAGE_47",
                            "lužická srbština horní (hornolužičtina)");

        languageTypeMap.put("ZP2015_LANGUAGE_48",
                            "makedonština");

        languageTypeMap.put("ZP2015_LANGUAGE_49",
                            "polština");

        languageTypeMap.put("ZP2015_LANGUAGE_50",
                            "rusínština");

        languageTypeMap.put("ZP2015_LANGUAGE_51",
                            "ruština");

        languageTypeMap.put("ZP2015_LANGUAGE_52",
                            "slovenština");

        languageTypeMap.put("ZP2015_LANGUAGE_53",
                            "slovinština");

        languageTypeMap.put("ZP2015_LANGUAGE_54",
                            "srbochorvatština");

        languageTypeMap.put("ZP2015_LANGUAGE_55",
                            "srbština");

        languageTypeMap.put("ZP2015_LANGUAGE_56",
                            "staroslověnština, církevní slovanština");

        languageTypeMap.put("ZP2015_LANGUAGE_57",
                            "ukrajinština");

        languageTypeMap.put("ZP2015_LANGUAGE_58",
                            "blíže neurčený indoevropský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_59",
                            "albánština");

        languageTypeMap.put("ZP2015_LANGUAGE_60",
                            "arménština");

        languageTypeMap.put("ZP2015_LANGUAGE_61",
                            "řečtina");

        languageTypeMap.put("ZP2015_LANGUAGE_62",
                            "blíže neurčený uralský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_63",
                            "estonština");

        languageTypeMap.put("ZP2015_LANGUAGE_64",
                            "finština");

        languageTypeMap.put("ZP2015_LANGUAGE_65",
                            "maďarština");

        languageTypeMap.put("ZP2015_LANGUAGE_66",
                            "livština");

        languageTypeMap.put("ZP2015_LANGUAGE_67",
                            "baskičtina");

        languageTypeMap.put("ZP2015_LANGUAGE_68",
                            "blíže neurčený afroasijský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_69",
                            "blíže neurčený semitský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_70",
                            "(novo)hebrejština");

        languageTypeMap.put("ZP2015_LANGUAGE_71",
                            "amharština");

        languageTypeMap.put("ZP2015_LANGUAGE_72",
                            "arabština");

        languageTypeMap.put("ZP2015_LANGUAGE_73",
                            "aramejština");

        languageTypeMap.put("ZP2015_LANGUAGE_74",
                            "hauština");

        languageTypeMap.put("ZP2015_LANGUAGE_75",
                            "maltština");

        languageTypeMap.put("ZP2015_LANGUAGE_76",
                            "stará hebrejština");

        languageTypeMap.put("ZP2015_LANGUAGE_77",
                            "akkadština");

        languageTypeMap.put("ZP2015_LANGUAGE_78",
                            "assyrština");

        languageTypeMap.put("ZP2015_LANGUAGE_79",
                            "babylonština");

        languageTypeMap.put("ZP2015_LANGUAGE_80",
                            "chetitština");

        languageTypeMap.put("ZP2015_LANGUAGE_81",
                            "sumerština");

        languageTypeMap.put("ZP2015_LANGUAGE_82",
                            "egyptština");

        languageTypeMap.put("ZP2015_LANGUAGE_83",
                            "koptština");

        languageTypeMap.put("ZP2015_LANGUAGE_84",
                            "blíže neurčený nigerokonžský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_85",
                            "svahilština");

        languageTypeMap.put("ZP2015_LANGUAGE_86",
                            "blíže neurčený altajský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_87",
                            "japonština");

        languageTypeMap.put("ZP2015_LANGUAGE_88",
                            "korejština");

        languageTypeMap.put("ZP2015_LANGUAGE_89",
                            "blíže neurčený mongolský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_90",
                            "mongolština");

        languageTypeMap.put("ZP2015_LANGUAGE_91",
                            "blíže neurčený turkický jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_92",
                            "azerbajdžánština");

        languageTypeMap.put("ZP2015_LANGUAGE_93",
                            "baškirština");

        languageTypeMap.put("ZP2015_LANGUAGE_94",
                            "gagauzština");

        languageTypeMap.put("ZP2015_LANGUAGE_95",
                            "hazarština");

        languageTypeMap.put("ZP2015_LANGUAGE_96",
                            "kyrgyzština");

        languageTypeMap.put("ZP2015_LANGUAGE_97",
                            "krymská tatarština");

        languageTypeMap.put("ZP2015_LANGUAGE_98",
                            "tatarština");

        languageTypeMap.put("ZP2015_LANGUAGE_99",
                            "turečtina");

        languageTypeMap.put("ZP2015_LANGUAGE_100",
                            "turkménština");

        languageTypeMap.put("ZP2015_LANGUAGE_101",
                            "ujgurština");

        languageTypeMap.put("ZP2015_LANGUAGE_102",
                            "uzbečtina");

        languageTypeMap.put("ZP2015_LANGUAGE_103",
                            "blíže neurčený austroasijský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_104",
                            "santalština");

        languageTypeMap.put("ZP2015_LANGUAGE_105",
                            "khmérština");

        languageTypeMap.put("ZP2015_LANGUAGE_106",
                            "vietnamština");

        languageTypeMap.put("ZP2015_LANGUAGE_107",
                            "blíže neurčený austronéský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_108",
                            "indonéština");

        languageTypeMap.put("ZP2015_LANGUAGE_109",
                            "malajština");

        languageTypeMap.put("ZP2015_LANGUAGE_110",
                            "blíže neurčený drávidský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_111",
                            "kannadština (též kannarština)");

        languageTypeMap.put("ZP2015_LANGUAGE_112",
                            "malajalámština");

        languageTypeMap.put("ZP2015_LANGUAGE_113",
                            "tamilština");

        languageTypeMap.put("ZP2015_LANGUAGE_114",
                            "telugština");

        languageTypeMap.put("ZP2015_LANGUAGE_115",
                            "blíže neurčený indoárijský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_116",
                            "asámština");

        languageTypeMap.put("ZP2015_LANGUAGE_117",
                            "bengálština");

        languageTypeMap.put("ZP2015_LANGUAGE_118",
                            "Bihárštiina (maithilština)");

        languageTypeMap.put("ZP2015_LANGUAGE_119",
                            "dógrí");

        languageTypeMap.put("ZP2015_LANGUAGE_120",
                            "gudžarátština");

        languageTypeMap.put("ZP2015_LANGUAGE_121",
                            "hindština");

        languageTypeMap.put("ZP2015_LANGUAGE_122",
                            "kašmírština");

        languageTypeMap.put("ZP2015_LANGUAGE_123",
                            "konkánština");

        languageTypeMap.put("ZP2015_LANGUAGE_124",
                            "maráthština");

        languageTypeMap.put("ZP2015_LANGUAGE_125",
                            "pahárské jazyky (nepálština)");

        languageTypeMap.put("ZP2015_LANGUAGE_126",
                            "pandžábština");

        languageTypeMap.put("ZP2015_LANGUAGE_127",
                            "romština");

        languageTypeMap.put("ZP2015_LANGUAGE_128",
                            "sanskrt");

        languageTypeMap.put("ZP2015_LANGUAGE_129",
                            "sindština");

        languageTypeMap.put("ZP2015_LANGUAGE_130",
                            "sinhálština");

        languageTypeMap.put("ZP2015_LANGUAGE_131",
                            "urdština");

        languageTypeMap.put("ZP2015_LANGUAGE_132",
                            "urijština");

        languageTypeMap.put("ZP2015_LANGUAGE_133",
                            "blíže neurčený indoíránský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_134",
                            "(novo)perština");

        languageTypeMap.put("ZP2015_LANGUAGE_135",
                            "kurdština");

        languageTypeMap.put("ZP2015_LANGUAGE_136",
                            "paštština");

        languageTypeMap.put("ZP2015_LANGUAGE_137",
                            "stará perština");

        languageTypeMap.put("ZP2015_LANGUAGE_138",
                            "střední perština");

        languageTypeMap.put("ZP2015_LANGUAGE_139",
                            "tádžičtina");

        languageTypeMap.put("ZP2015_LANGUAGE_140",
                            "blíže neurčený kartvelský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_141",
                            "gruzínština");

        languageTypeMap.put("ZP2015_LANGUAGE_142",
                            "blíže neurčený severokavkazský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_143",
                            "abcházština");

        languageTypeMap.put("ZP2015_LANGUAGE_144",
                            "blíže neurčený tajsko-kadajský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_145",
                            "laoština");

        languageTypeMap.put("ZP2015_LANGUAGE_146",
                            "thajština");

        languageTypeMap.put("ZP2015_LANGUAGE_147",
                            "blíže neurčený tibetskočínský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_148",
                            "barmština");

        languageTypeMap.put("ZP2015_LANGUAGE_149",
                            "bodo");

        languageTypeMap.put("ZP2015_LANGUAGE_150",
                            "bhútánština");

        languageTypeMap.put("ZP2015_LANGUAGE_151",
                            "manípurština");

        languageTypeMap.put("ZP2015_LANGUAGE_152",
                            "čínština");

        languageTypeMap.put("ZP2015_LANGUAGE_153",
                            "tibetština");

        languageTypeMap.put("ZP2015_LANGUAGE_154",
                            "blíže neurčený aleutský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_155",
                            "grónština");

        languageTypeMap.put("ZP2015_LANGUAGE_156",
                            "blíže neurčený indiánský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_157",
                            "blíže neurčený");

        languageTypeMap.put("ZP2015_LANGUAGE_158",
                            "blíže neurčený izolovaný jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_159",
                            "blíže neurčený kreolský jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_160",
                            "blíže neurčený umělý jazyk");

        languageTypeMap.put("ZP2015_LANGUAGE_161",
                            "esperanto");

        languageTypeMap.put("ZP2015_LANGUAGE_162",
                            "ido");

        languageTypeMap.put("ZP2015_LANGUAGE_163",
                            "interlingua");

        languageTypeMap.put("ZP2015_LANGUAGE_164",
                            "interlingue");

        languageTypeMap.put("ZP2015_LANGUAGE_165",
                            "volapük");

        languageTypeMap.put("ZP2015_LANGUAGE_166",
                            "slovio");

    }
    
    public static Map<String, String> otherIdMap = new HashMap<>();
    static {
        otherIdMap.put("ZP2015_OTHERID_SIG_ORIG", "signatura přidělená původcem");
        otherIdMap.put("ZP2015_OTHERID_SIG", "signatura přidělená při zpracování archiválie");
        otherIdMap.put("ZP2015_OTHERID_STORAGE_ID", "ukládací znak");
        otherIdMap.put("ZP2015_OTHERID_CJ", "číslo jednací");
        otherIdMap.put("ZP2015_OTHERID_DOCID", "značka spisu");
        otherIdMap.put("ZP2015_OTHERID_FORMAL_DOCID", "číslo vložky úřední knihy");
        otherIdMap.put("ZP2015_OTHERID_ADDID", "přírůstkové číslo");
        otherIdMap.put("ZP2015_OTHERID_OLDSIG", "signatura přidělená při předchozím zpracování");
        otherIdMap.put("ZP2015_OTHERID_OLDSIG2", "spisový znak");
        otherIdMap.put("ZP2015_OTHERID_OLDID", "neplatné inventární číslo");
        otherIdMap.put("ZP2015_OTHERID_INVALID_UNITID", "neplatné ukládací číslo");
        otherIdMap.put("ZP2015_OTHERID_INVALID_REFNO", "neplatné referenční označení");
        otherIdMap.put("ZP2015_OTHERID_PRINTID", "pořadové číslo pro tisk");
        otherIdMap.put("ZP2015_OTHERID_PICID", "nakladatelské číslo");
        otherIdMap.put("ZP2015_OTHERID_NEGID", "číslo negativu");
        otherIdMap.put("ZP2015_OTHERID_CDID", "číslo produkce CD");
        otherIdMap.put("ZP2015_OTHERID_ISBN", "kód ISBN");
        otherIdMap.put("ZP2015_OTHERID_ISSN", "kód ISSN");
        otherIdMap.put("ZP2015_OTHERID_ISMN", "kód ISMN");
        otherIdMap.put("ZP2015_OTHERID_MATRIXID", "matriční číslo");
    }
    
    public static Map<String, String> otherIdIndexMap = new HashMap<>();
    static {    
        otherIdIndexMap.put("ZP2015_OTHERID_SIG_ORIG", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_SIG", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_STORAGE_ID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_CJ", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_DOCID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_FORMAL_DOCID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ADDID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDSIG", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDSIG2", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_OLDID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_INVALID_UNITID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_INVALID_REFNO", "UNIT_ID_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_PRINTID", "OTHER_ID_PROC_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_PICID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_NEGID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_CDID", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISBN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISSN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_ISMN", "OTHER_ID_ORIG_INDEX");
        otherIdIndexMap.put("ZP2015_OTHERID_MATRIXID", "OTHER_ID_ORIG_INDEX");
    }
    
    public static Map<String, String> relEntityMap = new HashMap<>();    
    static {
        relEntityMap.put("RT_AUTHOR", "autor/tvůrce");
        relEntityMap.put("RT_AUTHOROFCHANGE",
                         "autor změny / tvůrce změny");
        relEntityMap.put("RT_GRANDMOTHER",
                         "babička");
        relEntityMap.put("RT_BROTHER",
                         "bratr");
        relEntityMap.put("RT_HECOUSIN",
                         "bratranec");
        relEntityMap.put("RT_WHOLE",
                         "celek");
        relEntityMap.put("RT_CEREMONY",
                         "ceremoniál ocenění");
        relEntityMap.put("RT_MEMBERORG",
                         "členská organizace");
        relEntityMap.put("RT_RELATIONS",
                         "další rodinné vztahy");
        relEntityMap.put("RT_GRANDFATHER",
                         "dědeček");
        relEntityMap.put("RT_DOCUMENT",
                         "dokument");
        relEntityMap.put("RT_ENTITYEND",
                         "entita související s koncem");
        relEntityMap.put("RT_ENTITYBIRTH",
                         "entita související s narozením");
        relEntityMap.put("RT_ENTITYDEATH",
                         "entita související s úmrtím");
        relEntityMap.put("RT_ENTITYRISE",
                         "entita související se vznikem");
        relEntityMap.put("RT_ENTITYORIGIN",
                         "entita související se začátkem");
        relEntityMap.put("RT_ENTITYEXTINCTION",
                         "entita související se zánikem");
        relEntityMap.put("RT_FUNCTION",
                         "funkce/činnost");
        relEntityMap.put("RT_GEOSCOPE",
                         "geografická působnost");
        relEntityMap.put("RT_MILESTONE",
                         "historický milník");
        relEntityMap.put("RT_ISPART",
                         "je část");
        relEntityMap.put("RT_ISMEMBER",
                         "je členem");
        relEntityMap.put("RT_GENUSMEMBER",
                         "je členem rodiny/rodu");
        relEntityMap.put("RT_OTHERNAME",
                         "jiná entita reprezentující tutéž osobu");
        relEntityMap.put("RT_ANCESTOR",
                         "jiný předek");
        relEntityMap.put("RT_ACTIVITYCORP",
                         "korporace veřejného působení");
        relEntityMap.put("RT_LIQUIDATOR",
                         "likvidátor");
        relEntityMap.put("RT_OWNER",
                         "majitel");
        relEntityMap.put("RT_HOLDER",
                         "majitel, držitel");
        relEntityMap.put("RT_HUSBAND",
                         "manžel");

        relEntityMap.put("RT_WIFE",
                         "manželka");

        relEntityMap.put("RT_MOTHER",
                         "matka");

        relEntityMap.put("RT_PLACE",
                         "místo");

        relEntityMap.put("RT_VENUE",
                         "místo konání");

        relEntityMap.put("RT_PLACEEND",
                         "místo ukončení");

        relEntityMap.put("RT_PLACESTART",
                         "místo uzavření");

        relEntityMap.put("RT_SUPCORP",
                         "nadřazená korporace");

        relEntityMap.put("RT_SUPTERM",
                         "nadřazený pojem");
        relEntityMap.put("RT_SENIOR",
                         "nadřízený");
        relEntityMap.put("RT_SUCCESSOR",
                         "nástupce");
        relEntityMap.put("RT_ACTIVITYFIELD",
                         "obor činnosti");

        relEntityMap.put("RT_STUDYFIELD",
                         "obor studia");

        relEntityMap.put("RT_AWARD",
                         "ocenění");

        relEntityMap.put("RT_ORGANIZER",
                         "organizátor, svolavatel");
        relEntityMap.put("RT_FATHER",
                         "otec");
        relEntityMap.put("RT_PARTNER",
                         "partner");
        relEntityMap.put("RT_SHEPARTNER",
                         "partnerka");
        relEntityMap.put("RT_SUBTERM",
                         "podřazený pojem");
        relEntityMap.put("RT_NAMEDAFTER",
                         "pojmenováno po");
        relEntityMap.put("RT_LASTKMEMBER",
                         "poslední známý člen rodu/rodiny");
        relEntityMap.put("RT_FIRSTKMEMBER",
                         "první známý člen rodu/rodiny");
        relEntityMap.put("RT_PREDECESSOR",
                         "předchůdce");
        relEntityMap.put("RT_AFFILIATION",
                         "příslušnost k");
        relEntityMap.put("RT_SISTER",
                         "sestra");
        relEntityMap.put("RT_SHECOUSIN",
                         "sestřenice");
        relEntityMap.put("RT_RESIDENCE",
                         "sídlo");
        relEntityMap.put("RT_COMPEVENT",
                         "souborná událost, celek");
        relEntityMap.put("RT_RELATED",
                         "související entita");
        relEntityMap.put("RT_RELATEDTERM",
                         "související pojem");
        relEntityMap.put("RT_COLLABORATOR",
                         "spolupracovník");
        relEntityMap.put("RT_SCHOOLMATE",
                         "spolužák");
        relEntityMap.put("RT_UNCLE",
                         "strýc");
        relEntityMap.put("RT_SCHOOL",
                         "škola");
        relEntityMap.put("RT_BROTHERINLAW",
                         "švagr");
        relEntityMap.put("RT_SISTERINLAW",
                         "švagrová");
        relEntityMap.put("RT_CATEGORY",
                         "taxonomická kategorie");
        relEntityMap.put("RT_THEME",
                         "tematický celek");
        relEntityMap.put("RT_AUNT",
                         "teta");
        relEntityMap.put("RT_FATHERINLAW",
                         "tchán");
        relEntityMap.put("RT_MOTHERINLAW",
                         "tchyně");
        relEntityMap.put("RT_TEACHER",
                         "učitel");
        relEntityMap.put("RT_GRANTAUTH",
                         "udělovatel");
        relEntityMap.put("RT_STORAGE",
                         "uložení");
        relEntityMap.put("RT_LOCATION",
                         "umístění");
        relEntityMap.put("RT_OBJECT",
                         "vazba na objekt");
        relEntityMap.put("RT_ACTIVITIES",
                         "významné aktivity");
        relEntityMap.put("RT_FOUNDER",
                         "zakladatel/zřizovatel");
        relEntityMap.put("RT_EMPLOYER",
                         "zaměstnavatel");
        relEntityMap.put("RT_ROOF",
                         "zjednodušená podniková entita");
        relEntityMap.put("RT_REPRESENT",
                         "zástupce");
        relEntityMap.put("RT_NAMECHANGE",
                         "změna jména/identity");
        relEntityMap.put("RT_MENTION",
                         "zmínka o existenci");
        relEntityMap.put("RT_ORIGINATOR",
                         "zřizovatel");
        relEntityMap.put("RT_GEOPARTNER",
                         "partner");

    }

}
