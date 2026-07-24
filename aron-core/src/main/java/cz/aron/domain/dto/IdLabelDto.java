package cz.aron.domain.dto;

import java.util.UUID;

public record IdLabelDto(long id, UUID uuid, String name, String indexedName) {

}
