package cz.aron.transfagent.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.core.send.items.DirReader;

import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.DaoRepository;

@Service
public class DaoSendService implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(DaoSendService.class);

	private final StorageService storageService;

	private final DaoRepository daoRepository;

	private final ConfigAronCore configAronCore;
	
	private final ConfigDao configDao;

	private ThreadStatus status;
	
	private Client client = null;

	public DaoSendService(StorageService storageService, DaoRepository daoFileRepository,
			ConfigAronCore configAronCore, ConfigDao configDao) {
		this.storageService = storageService;
		this.daoRepository = daoFileRepository;
		this.configAronCore = configAronCore;
		this.configDao = configDao;
	}

	private void uploadDaos() {
		var ids = daoRepository.findTopByStateAndTransferredOrderById(DaoState.READY, false, PageRequest.of(0, 1000));
		if (!ids.isEmpty()) {
			createClient();
			try {
				while (!ids.isEmpty()) {
					for (var id : ids) {
						uploadDao(id);
						if (status != ThreadStatus.RUNNING) {
							return;
						}
					}
					ids = daoRepository.findTopByStateAndTransferredOrderById(DaoState.READY, false,
							PageRequest.of(0, 1000));
				}
			} finally {
				if (client != null) {
					try {
						client.stop();
					} catch (Exception e) {
						log.error("Fail to stop filetransfer client", e);
					}
				}
				client = null;
			}
		}
	}

	private void uploadDao(int id) {		
		var daoOpt = daoRepository.findById(id);
		daoOpt.ifPresentOrElse(dao -> {
		    log.info("Dao uuid={}, start sending", dao.getUuid());
			var daoPath = storageService.getDaoPath().resolve(dao.getDataDir());
			UploadRequestImpl request = UploadRequestImpl.buildDaoRequest(new DirReader(daoPath), dao.getUuid().toString());
			try {
				client.uploadSync(request);
			} finally {
				client.stop();
			}

			if (request.isCanceled()) {
				log.error("Dao uuid={}, transfer to server canceled.", dao.getUuid());
				throw new IllegalStateException();
			}
			if (request.isFailed()) {
				log.error("Dao uuid={}, transfer to server failed.", dao.getUuid());
				throw new IllegalStateException();
			}
			
			if (configDao.isDeleteSent()) {
				// smazu adresar s dao
				try {
					FileSystemUtils.deleteRecursively(daoPath);
					log.info("Dao directory deleted {}",dao.getDataDir());
				} catch (IOException e) {
					log.error("Fail to delete dao directory {}", daoPath, e);
				}
				dao.setDataDir(null);
			}
			
			dao.setTransferred(true);
			daoRepository.save(dao);			
			log.info("Dao uuid={}, sent", dao.getUuid());			
		}, () -> {
			log.warn("Dao id={}, not exist",id);
		});
	}
	
	/**
	 * Vytvoreni klienta FT
	 */
	private void createClient() {
		ClientConfig clientConfig = new ClientConfig(configAronCore.getFt().getUrl());
		clientConfig.setSoapLogging(configAronCore.getFt().getSoapLogging());
		clientConfig.setRecoveryDelay(1);
		client = FileTransfer.createClient(clientConfig);
	}

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                uploadDaos();
            } catch (Exception e) {
                log.error("Error in sending dao. ", e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                log.error("Dao send service, thread interrupted", e1);
                return;
            }
        }
        status = ThreadStatus.STOPPED;
    }

    @Override
    public void start() {
        if (configAronCore.isDisabled()) {
            log.info("Dao send service is disabled.");
            status = ThreadStatus.STOPPED;
            return;
        }
        log.info("Starting dao send service.");
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
