package cz.aron.transfagent.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.transfagent.domain.Transform;
import cz.aron.transfagent.domain.TransformState;
import cz.aron.transfagent.repository.TransformRepository;
import cz.aron.transfagent.service.importfromdir.TransformService;

@ConditionalOnProperty(value = "filestore.path")
@Service
public class TransformExecutor implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(TransformExecutor.class);
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final TransformRepository transformRepository;

	private final TransformService transformService;

	private final TransactionTemplate transactionTemplate;

	private final DaoFileStoreService daoFileStoreService;

	private final List<String> workers;

	private volatile ThreadStatus status;

	public TransformExecutor(TransformRepository transformRepository, TransformService transformService,
			TransactionTemplate transactionTemplate, DaoFileStoreService daoFileStoreService,
			@Value("${externaltransformers:#{null}}") List<String> workers) {
		this.transformRepository = transformRepository;
		this.transformService = transformService;
		this.transactionTemplate = transactionTemplate;
		this.daoFileStoreService = daoFileStoreService;
		this.workers = workers;
	}

	private void transform() {		
		if (CollectionUtils.isEmpty(workers)) {
			transformLocal();
		} else {
			transformByWorkers();
		}				
	}
	
	private void transformByWorkers() {

		Set<Integer> justSending = Collections.synchronizedSet(new HashSet<>());
		justSending.add(-1);
		
		var transforms = transformRepository.findTop1000ByStateAndIdNotInOrderById(TransformState.READY,
				justSending);
		if (CollectionUtils.isEmpty(transforms)) {
			return;
		}

		HttpClient client = new HttpClient();
		try {
			client.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		try {
			ArrayBlockingQueue<SendContext> queue = new ArrayBlockingQueue<SendContext>(workers.size());
			workers.forEach(w -> {
				queue.add(new SendContext(w, true));
			});
			do {
				for (var transform : transforms) {					
					if (!"dzi".equals(transform.getType())) {
						// neznama transformace
						transactionTemplate.executeWithoutResult(t -> {
							transform.setState(TransformState.FAIL);
							transformRepository.save(transform);
						});
						continue;
					}
					// vyzvednu workera z fronty
					SendContext sendContext;
					try {
						sendContext = queue.take();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}					
					sendRequest(transform, sendContext, client, queue, justSending);									
					if(!isRunning()) {
						return;
					}
				}
				transforms = transformRepository.findTop1000ByStateAndIdNotInOrderById(TransformState.READY, justSending);
			} while (!CollectionUtils.isEmpty(transforms));
			
			// pockam nez bude vse odeslano, max jednu minutu
			long minuteMillis = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
			long start = System.currentTimeMillis();
			while(queue.size()!=workers.size()&&System.currentTimeMillis()-start<minuteMillis) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			}			
		} finally {
			try {
				client.stop();
			} catch (Exception e) {
				log.error("Fail to stop client", e);
				throw new RuntimeException(e);
			}			
		}
	}
	
	private void sendRequest(Transform transform, SendContext sendContext, HttpClient client,
			ArrayBlockingQueue<SendContext> queue, Set<Integer> justSending) {
		var filePath = daoFileStoreService.getDaoDir(transform.getDaoUuid().toString()).resolve(transform.getFile());

		TransformRequest tr = new TransformRequest();
		tr.setSrcFile(filePath.toString());
		tr.setTargetDir(transformService.getTileDir(transform.getFileUuid().toString()).toString());
		byte[] bytes;
		try {
			bytes = OBJECT_MAPPER.writeValueAsBytes(tr);
		} catch (JsonProcessingException e1) {
			log.error("Fail to create request object transform id {} ", transform.getId(), e1);
			throw new RuntimeException(e1);
		}

		boolean removeJustSending = true;
		try {
			justSending.add(transform.getId());
			client.newRequest(sendContext.getUrl()).method(HttpMethod.POST).timeout(60, TimeUnit.SECONDS)
					.content(new BytesContentProvider(bytes), "application/json").send(result -> {
						justSending.remove(transform.getId());
						if (result.isSucceeded()) {
							transactionTemplate.executeWithoutResult(t -> {
								transform.setState(TransformState.TRANSFORMED);
								transformRepository.save(transform);
							});
							log.info("Transformed id={}, uuid={}, file={}, executor={}", transform.getId(),
									transform.getFileUuid(), transform.getFile(), sendContext.getUrl());
						} else {
							log.error("Fail to transform id={}, uuid={}, file={}, executor={}", transform.getId(),
									transform.getFileUuid(), transform.getFile(), sendContext.getUrl(),
									result.getFailure());
						}
						// vratim workera do fronty
						try {
							queue.put(sendContext);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new RuntimeException(e);
						}
					});
			removeJustSending = false;
		} finally {
			if (removeJustSending) {
				justSending.remove(transform.getId());
			}
		}
	}

	static class TransformRequest {
		private String srcFile;
		private String targetDir;
		public String getSrcFile() {
			return srcFile;
		}
		public void setSrcFile(String srcFile) {
			this.srcFile = srcFile;
		}
		public String getTargetDir() {
			return targetDir;
		}
		public void setTargetDir(String targetDir) {
			this.targetDir = targetDir;
		}				
	}

	static class SendContext {
		private final String url;
		private boolean lastSuccess;
		
		public SendContext(String url, boolean lastSuccess) {
			this.url = url;
			this.lastSuccess = lastSuccess;
		}

		public boolean isLastSuccess() {
			return lastSuccess;
		}

		public void setLastSuccess(boolean lastSuccess) {
			this.lastSuccess = lastSuccess;
		}

		public String getUrl() {
			return url;
		}		
		
	}
	
	private void transformLocal() {
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
