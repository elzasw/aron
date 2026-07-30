package cz.aron.domain.dto;

import java.util.UUID;

public record IdStructuredResultDto(UUID uuid, byte[] result) {
}
