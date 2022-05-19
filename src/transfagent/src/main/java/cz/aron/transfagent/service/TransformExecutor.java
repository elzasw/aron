package cz.aron.transfagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.domain.TransformState;
import cz.aron.transfagent.repository.TransformRepository;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value = "filestore.path")
@Service
public class TransformExecutor implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(TransformExecutor.class);

	private final TransformRepository transformRepository;

	private final TransformService transformService;

	private final TransactionTemplate transactionTemplate;

	private final DaoFileStoreService daoFileStoreService;

	private volatile ThreadStatus status;

	public TransformExecutor(TransformRepository transformRepository, TransformService transformService,
			TransactionTemplate transactionTemplate, DaoFileStoreService daoFileStoreService) {
		this.transformRepository = transformRepository;
		this.transformService = transformService;
		this.transactionTemplate = transactionTemplate;
		this.daoFileStoreService = daoFileStoreService;
	}

	private void transform() {
		var transforms = transformRepository.findTop1000ByStateOrderById(TransformState.READY);
		for (var transform : transforms) {
			if ("dzi".equals(transform.getType())) {
				var filePath = daoFileStoreService.getDaoDir(transform.getDaoUuid().toString())
						.resolve(transform.getFile());
				transformService.createDziOut(filePath, transform.getFileUuid().toString());
				transactionTemplate.executeWithoutResult(t -> {
					transform.setState(TransformState.TRANSFORMED);
					transformRepository.save(transform);
				});
			} else {
				transactionTemplate.executeWithoutResult(t -> {
					transform.setState(TransformState.FAIL);
					transformRepository.save(transform);
				});
			}
		}
	}

	public void run() {
		while (status == ThreadStatus.RUNNING) {
			try {
				transform();
				Thread.sleep(5000);
			} catch (Exception e) {
				log.error("Error in import file. ", e);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		status = ThreadStatus.STOPPED;
	}

	@Override
	public void start() {
		status = ThreadStatus.RUNNING;
		new Thread(() -> {
			run();
		}).start();
	}

	@Override
	public void stop() {
		status = ThreadStatus.STOP_REQUEST;
	}

	@Override
	public boolean isRunning() {
		return status == ThreadStatus.RUNNING;
	}

}
