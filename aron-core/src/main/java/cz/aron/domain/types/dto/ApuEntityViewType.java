package cz.aron.domain.types.dto;

import cz.aron.domain.ApuType;

public record ApuEntityViewType(String uuid, String name, String description, int ordr, ApuType type, long apu_id, Long parent_id) {

}
