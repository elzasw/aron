package cz.aron.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuAttachment;

@Repository
public interface ApuAttachmentRepository extends JpaRepository<ApuAttachment, Long> {

	/**
	 * Deletes all attachments of all APUs belonging to the given ApuSource. Their files have to be
	 * deleted first, see {@link DaoFileRepository#deleteAttachmentFilesByApuSourceId(long)}.
	 *
	 * @return num deleted records
	 */
	@Modifying
	@Query("DELETE FROM ApuAttachment aa WHERE aa.apu IN "
			+ "(SELECT ae FROM ApuEntity ae WHERE ae.source.id=:apuSourceId)")
	long deleteAllByApuSourceId(@Param("apuSourceId") long apuSourceId);

	@Query("SELECT COALESCE(MAX(aa.id), 0) FROM ApuAttachment aa")
	long findMaxId();

}
