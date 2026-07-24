package cz.aron.domain.dto;

import java.util.UUID;

public record IdUuidNameDescriptionParentDto(long id, UUID uuid, String name, String description, Long parentId, int depth, int childCnt, int ordr, int pos) {

}
