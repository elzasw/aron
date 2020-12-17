package cz.aron.transfagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.core.send.items.DirReader;

import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.DaoFileRepository;

@Service
public class DaoSendService implements SmartLifecycle {
	
	private static final Logger log = LoggerFactory.getLogger(DaoSendService.class);
	
	private final StorageService storageService;
	
	private final DaoFileRepository daoFileRepository;
	
	private final ConfigAronCore configAronCore;
	
	private final int sendInterval;
	
	private ThreadStatus status;
	
	public DaoSendService(StorageService storageService, DaoFileRepository daoFileRepository,
			ConfigAronCore configAronCore, @Value("${dao.sendInterval:60}") Integer sendInterval) {
		this.storageService = storageService;
		this.daoFileRepository = daoFileRepository;
		this.configAronCore = configAronCore;
		this.sendInterval = sendInterval;
	}
	
	private void uploadDaos() {
		var ids = daoFileRepository.findTop1000ByStateAndTransferredOrderById(DaoState.READY,false);
		while (!ids.isEmpty()) {
			for (var id : ids) {
				uploadDao(id.getId());
				if (status != ThreadStatus.RUNNING) {
					return;
				}
			}
			ids = daoFileRepository.findTop1000ByStateAndTransferredOrderById(DaoState.READY,false);
		}
	}
	
	private void uploadDao(int id) {
		
		var daoOpt = daoFileRepository.findById(id);
		daoOpt.ifPresentOrElse(dao -> {
			
			// vytváření Clienta
			ClientConfig clientConfig = new ClientConfig(configAronCore.getFt().getUrl());
			clientConfig.setSoapLogging(configAronCore.getFt().getSoapLogging());
			Client client = FileTransfer.createClient(clientConfig);

			var daoPath = storageService.getDataPath().resolve(dao.getDataDir());
			UploadRequestImpl request = UploadRequestImpl.buildDaoRequest(new DirReader(daoPath), dao.getUuid().toString());
			try {
				client.uploadSync(request);
			} finally {
				client.stop();
			}

			if (request.isCanceled()) {
				log.error("transfer to server canceled.");
				throw new IllegalStateException();
			}
			if (request.isFailed()) {
				log.error("transfer to server failed.");
				throw new IllegalStateException();
			}
			
			dao.setTransferred(true);
			daoFileRepository.save(dao);			
			log.info("Dao uuid={}, sent", dao.getUuid());			
		}, () -> {
			log.warn("Dao id={}, not exist",id);
		});
	}

	public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                uploadDaos();
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
