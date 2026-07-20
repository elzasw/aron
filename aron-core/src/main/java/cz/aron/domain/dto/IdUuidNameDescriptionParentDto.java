package cz.aron.domain.dto;

public record IdUuidNameDescriptionParentDto(long id, String uuid, String name, String description, Long parentId, int depth, int childCnt, int ordr, int pos) {

}
