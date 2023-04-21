package cz.aron.domain.types.dto;

import java.util.List;

import cz.aron.domain.ApuType;

public record ApuEntityTreeView(String id, String name, String description, int order,
		ApuType type, List<ApuEntityTreeView> children) {

}
