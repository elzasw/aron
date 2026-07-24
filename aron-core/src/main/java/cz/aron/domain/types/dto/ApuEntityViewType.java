package cz.aron.domain.types.dto;

import java.util.UUID;

import cz.aron.domain.ApuType;

public record ApuEntityViewType(UUID uuid, String name, String description, int ordr, ApuType type, long apu_id, Long parent_id) {

}
