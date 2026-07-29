package cz.aron.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import cz.aron.repository.ApuAttachmentRepository;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.ApuSourceRepository;
import cz.aron.repository.DaoFileRepository;
import cz.aron.repository.DaoRepository;
import cz.aron.repository.RelationRepository;

/**
 * Assigns application-side primary keys for the persisted entities. Each entity class has its own
 * {@link AtomicLong} counter; {@link #initMetadataIds()} / {@link #initDaoIds()} seed the counters
 * from the current DB maxima, after which {@code getNext...Id()} hands out the following value.
 */
@Service
public class IdService {

	private final ApuSourceRepository apuSourceRepository;
	private final ApuEntityRepository apuEntityRepository;
	private final RelationRepository relationRepository;
	private final DaoRepository daoRepository;
	private final DaoFileRepository daoFileRepository;
	private final ApuAttachmentRepository apuAttachmentRepository;

	private final AtomicLong apuSourceId = new AtomicLong();
	private final AtomicLong apuEntityId = new AtomicLong();
	private final AtomicLong relationId = new AtomicLong();
	private final AtomicLong digitalObjectId = new AtomicLong();
	private final AtomicLong digitalObjectFileId = new AtomicLong();
	private final AtomicLong apuAttachmentId = new AtomicLong();

	public IdService(ApuSourceRepository apuSourceRepository, ApuEntityRepository apuEntityRepository,
			RelationRepository relationRepository, DaoRepository daoRepository,
			DaoFileRepository daoFileRepository, ApuAttachmentRepository apuAttachmentRepository) {
		this.apuSourceRepository = apuSourceRepository;
		this.apuEntityRepository = apuEntityRepository;
		this.relationRepository = relationRepository;
		this.daoRepository = daoRepository;
		this.daoFileRepository = daoFileRepository;
		this.apuAttachmentRepository = apuAttachmentRepository;
	}

	/**
	 * Seeds the metadata counters ({@link cz.aron.domain.ApuSource}, {@link cz.aron.domain.ApuEntity},
	 * {@link cz.aron.domain.Relation}, {@link cz.aron.domain.DigitalObject},
	 * {@link cz.aron.domain.ApuAttachment}) from the current DB maxima.
	 */
	public void initMetadataIds() {
		apuSourceId.set(apuSourceRepository.findMaxId());
		apuEntityId.set(apuEntityRepository.findMaxId());
		relationId.set(relationRepository.findMaxId());
		digitalObjectId.set(daoRepository.findMaxId());
		apuAttachmentId.set(apuAttachmentRepository.findMaxId());
	}

	/**
	 * Seeds the DAO counters ({@link cz.aron.domain.DigitalObjectFile}) from the current DB maxima.
	 */
	public void initDaoIds() {
		digitalObjectFileId.set(daoFileRepository.findMaxId());
	}

	public long getNextApuSourceId() {
		return apuSourceId.incrementAndGet();
	}

	public long getNextApuEntityId() {
		return apuEntityId.incrementAndGet();
	}

	public long getNextRelationId() {
		return relationId.incrementAndGet();
	}

	public long getNextDigitalObjectId() {
		return digitalObjectId.incrementAndGet();
	}

	public long getNextDigitalObjectFileId() {
		return digitalObjectFileId.incrementAndGet();
	}

	public long getNextApuAttachmentId() {
		return apuAttachmentId.incrementAndGet();
	}

}
