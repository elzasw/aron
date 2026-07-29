package cz.aron.domain.dto;

import java.util.UUID;

/**
 * Projection used to build a redirect to a single digital object file:
 * {@code /apu/{apuId}/dao/{daoId}/file/{fileId}}.
 */
public record DaoFileRedirectDto(UUID apuId, UUID daoId, UUID fileId) {

}
