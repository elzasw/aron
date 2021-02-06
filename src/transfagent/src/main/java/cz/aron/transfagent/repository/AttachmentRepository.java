package cz.aron.transfagent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {

    List<Attachment> findByApuSource(ApuSource apuSource);

}
