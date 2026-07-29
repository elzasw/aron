package cz.aron.domain.dto;

import java.util.UUID;

public record IdUuidNameDescriptionOrderDto(long id, UUID uuid, String name, String description, int order) {

}
