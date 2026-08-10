package cz.aron.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.aron.domain.ApuAttachment;

@Repository
public interface ApuAttachmentRepository extends JpaRepository<ApuAttachment, Long> {

	@Query("SELECT COALESCE(MAX(aa.id), 0) FROM ApuAttachment aa")
	long findMaxId();

}
