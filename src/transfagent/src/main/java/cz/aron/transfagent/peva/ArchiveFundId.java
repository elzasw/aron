package cz.aron.transfagent.peva;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 *  Trida reprezentujici identifikator archivniho fondu
 *  
 */
public class ArchiveFundId {

	private final String archiveId;
	
	private final String fundId;

	private final String fundSubId;

	public ArchiveFundId(String archiveId, String fundId, String fundSubId) {
		this.archiveId = archiveId;
		this.fundId = fundId;
		this.fundSubId = fundSubId;
	}
	
	public String getArchiveId() {
		return archiveId;
	}

	public String getFundId() {
		return fundId;
	}

	public String getFundSubId() {
		return fundSubId;
	}

	
	/**
	 * Rozparsuje identifikator
	 * 
	 * @param fundIdStr identifikator fondu reprezentovany jako string
	 * @return FundIdFormat nebo null pokud format retezce neodpovida identifikatoru
	 * 
	 *         Format retezce: jafa${id_archivu}${id_fondu}{$id_dilci_list}
	 *         
	 *         archiv - 9 znaku
	 *         fond - 5 znaku doplneno znakem '_' zleva
	 *         dilci list - 3 znaky doplneno znakem '_' zleva
	 * 
	 *         Priklady: jafa100000010_1001___ 
	 *                   jafa100000010_1005__2
	 * 
	 */
	public static ArchiveFundId parseFundId(String fundIdStr) {
		Objects.requireNonNull(fundIdStr, "Fund id cannot be null");
		if (fundIdStr.length() == 21 && fundIdStr.startsWith("jafa")) {
			return new ArchiveFundId(fundIdStr.substring(4,13), fundIdStr.substring(13, 18).replace("_", ""),
					fundIdStr.substring(18).replace("_", ""));
		} else {
			return null;
		}
	}
	
	/**
	 * Vytvori identifikator ve formatu jafa
	 * 
	 * @param archiveCode     kod archivu
	 * @param nadSheetCode    kod listu nad
	 * @param subNadSheetCode kod dilciho listu nad (null pokud neni dilci list)
	 * @return identifikator ve formatu jafa
	 */
	public static String createJaFaId(String archiveCode, String nadSheetCode, String subNadSheetCode) {
		StringBuilder sb = new StringBuilder("jafa");
		sb.append(archiveCode).append(String.format("%5s", nadSheetCode).replace(' ', '_'));
		if (subNadSheetCode != null) {
			sb.append(String.format("%3s", subNadSheetCode).replace(' ', '_'));
		} else {
			sb.append("___");
		}
		return sb.toString();
	}

	/**
	 * Vrati ve formatu "nad" nebo "nad"/"dilci nad" 
	 * @return String
	 */
	public String getFundSubFundAsString() {
		if (StringUtils.isBlank(fundSubId)) {
			return fundId;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(fundId).append("/").append(fundSubId);
			return sb.toString();
		}
	}
	
	/**
	 * Vrati identifikator ve formatu jafa
	 * @return String
	 */
	public String toJafa() {
		return createJaFaId(archiveId, fundId, fundSubId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((archiveId == null) ? 0 : archiveId.hashCode());
		result = prime * result + ((fundId == null) ? 0 : fundId.hashCode());
		result = prime * result + ((fundSubId == null) ? 0 : fundSubId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArchiveFundId other = (ArchiveFundId) obj;
		if (archiveId == null) {
			if (other.archiveId != null)
				return false;
		} else if (!archiveId.equals(other.archiveId))
			return false;
		if (fundId == null) {
			if (other.fundId != null)
				return false;
		} else if (!fundId.equals(other.fundId))
			return false;
		if (fundSubId == null) {
			if (other.fundSubId != null)
				return false;
		} else if (!fundSubId.equals(other.fundSubId))
			return false;
		return true;
	}
	
}
