package cz.aron.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.DigitalObjectFile;
import cz.aron.domain.dto.DaoFileRedirectDto;

@Repository
public interface DaoFileRepository  extends JpaRepository<DigitalObjectFile, Long> {

	DigitalObjectFile findByUuid(UUID uuid);
	
	DigitalObjectFile findByFileId(UUID uuid);

	@Query("SELECT dof.uuid FROM DigitalObjectFile dof WHERE dof.attachment.name=:name")
	List<UUID> findUuidsByAttachmentName(@Param("name") String name);

	@Query("SELECT new cz.aron.domain.dto.DaoFileRedirectDto(dof.digitalObject.apu.uuid, dof.digitalObject.uuid, dof.uuid) FROM DigitalObjectFile dof WHERE dof.permalink=:permalink")
	List<DaoFileRedirectDto> findRedirectByPermalink(@Param("permalink") String permalink);

	@Query("SELECT COALESCE(MAX(dof.id), 0) FROM DigitalObjectFile dof")
	long findMaxId();

}
