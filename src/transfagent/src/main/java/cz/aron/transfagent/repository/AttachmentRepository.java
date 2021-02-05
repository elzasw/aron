package cz.aron.transfagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {

    Attachment findByApuSource(ApuSource apuSource);

}
