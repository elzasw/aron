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
	
	public static final Map<String, String> unitTypeMap = new HashMap<>();
	static {
		unitTypeMap.put("ZP2015_UNIT_TYPE_LIO", "Listina do roku 1850");
		unitTypeMap.put("ZP2015_UNIT_TYPE_LIP", "Listina po roce 1850");
		unitTypeMap.put("ZP2015_UNIT_TYPE_UKN", "Úřední kniha");
		unitTypeMap.put("ZP2015_UNIT_TYPE_RKP", "Rukopis");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PPR", "Podací protokol");
        unitTypeMap.put("ZP2015_UNIT_TYPE_IND", "Index");
        unitTypeMap.put("ZP2015_UNIT_TYP_ELE", "Elench");
        unitTypeMap.put("ZP2015_UNIT_TYPE_REP", "Repertář");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT", "Kartotéka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PEC", "Pečetidlo");
        unitTypeMap.put("ZP2015_UNIT_TYPE_RAZ", "Razítko");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTD", "Samostatná pečeť, odlitek pečeti, otisk typáře");
        unitTypeMap.put("ZP2015_UNIT_TYPE_OTC", "Kopie otisku");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MAP", "Mapa");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ATL", "Atlas");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TVY", "Technický výkres");
        unitTypeMap.put("ZP2015_UNIT_TYPE_GLI", "Grafický list");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KRE", "Kresba");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSN", "Fotografie na papírové podložce");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FSD", "Fotografická deska");
        unitTypeMap.put("ZP2015_UNIT_TYPE_LFI", "Listový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_SFI", "Svitkový film");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KIN", "Kinofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MF", "Mikrofilm");
        unitTypeMap.put("ZP2015_UNIT_TYPE_MFS", "Mikrofiš");
        unitTypeMap.put("ZP2015_UNIT_TYPE_FAL", "Fotoalbum");
        unitTypeMap.put("ZP2015_UNIT_TYPE_DFO", "Digitální fotografie");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KZA", "Kinematografický záznam (dílo) v analogové i digitální podobě");
        unitTypeMap.put("ZP2015_UNIT_TYPE_ZZA", "Zvukový záznam (dílo) v analogové i digitální podobě");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIO", "Tisk do roku 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_TIP", "Tisk po roce 1800");
        unitTypeMap.put("ZP2015_UNIT_TYPE_POH", "Pohlednice");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PKT", "Plakát");
        unitTypeMap.put("ZP2015_UNIT_TYPE_CPA", "Cenný papír");
        unitTypeMap.put("ZP2015_UNIT_TYPE_STO", "Štoček");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PNP", "Předmět numizmatické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_PFP", "Předmět faleristické povahy");
        unitTypeMap.put("ZP2015_UNIT_TYPE_JIN", "Jiná");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_BOX", "Kartotéční zásuvka");
        unitTypeMap.put("ZP2015_UNIT_TYPE_KTT_ITEM", "Kartotéční lístek");
	}
	
}
