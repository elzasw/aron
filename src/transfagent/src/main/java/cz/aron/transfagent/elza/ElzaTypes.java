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
	
	public static final String REL_ENTITY = "REL_ENTITY";
	
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
