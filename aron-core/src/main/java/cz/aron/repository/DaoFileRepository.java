package cz.aron.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	/**
	 * Deletes the files of all attachments belonging to the given ApuSource. Must run before the
	 * attachments themselves are deleted — {@code digital_object_file.attachment_id} owns the FK.
	 *
	 * @return num deleted records
	 */
	@Modifying
	@Query("DELETE FROM DigitalObjectFile dof WHERE dof.attachment IN "
			+ "(SELECT aa FROM ApuAttachment aa WHERE aa.apu IN "
			+ "(SELECT ae FROM ApuEntity ae WHERE ae.source.id=:apuSourceId))")
	long deleteAttachmentFilesByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT COALESCE(MAX(dof.id), 0) FROM DigitalObjectFile dof")
	long findMaxId();

}
