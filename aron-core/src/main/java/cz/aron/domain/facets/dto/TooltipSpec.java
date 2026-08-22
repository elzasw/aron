package cz.aron.domain.facets.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.aron.domain.types.dto.LocalizedItem;

/**
 * One entry of a facet's {@code tooltips} - what an option of an enumerated
 * facet means, where its value does not say ("technický výkres" covering
 * technical drawings of buildings and products).
 * <p>
 * {@code value} names the option either by its value or by the label it is
 * displayed under, because a deployment writing about a reference facet's option
 * thinks of its name rather than of its uuid. Which of the two matched is
 * settled where the options are known, by the client.
 */
public class TooltipSpec {

	private String value;

	private String tooltip;

	/**
	 * Translations from the sibling searchConfig_localization.yaml; the configured
	 * {@code tooltip} is the source language and stays the fallback.
	 * <p>
	 * Kept out of JSON: this class is also what the frozen old API republishes
	 * from searchConfig.yaml, and translation plumbing is no part of that file.
	 */
	@JsonIgnore
	private List<LocalizedItem> tooltipTranslations = new ArrayList<>();

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public List<LocalizedItem> getTooltipTranslations() {
		return tooltipTranslations;
	}

}
