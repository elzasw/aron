package cz.aron.domain.types.dto;

import java.util.ArrayList;
import java.util.List;

public class ApuPartType {
	private String code;
	private String name;
	private ViewType viewType;
	private List<LocalizedItem> lang = new ArrayList<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ViewType getViewType() {
		return viewType;
	}

	public void setViewType(ViewType viewType) {
		this.viewType = viewType;
	}

	public List<LocalizedItem> getLang() {
		return lang;
	}

	public void setLang(List<LocalizedItem> lang) {
		this.lang = lang;
	}

}
