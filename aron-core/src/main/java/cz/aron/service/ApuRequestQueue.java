package cz.aron.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cz.aron.domain.QueuedApu;
import cz.aron.repository.QueuedApuRepository;
import cz.aron.service.transformagent.TransformAgentClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ApuRequestQueue {

    private final QueuedApuRepository queuedApuRepository;
    private final TransformAgentClient transformAgentClient;
    
    private final ReentrantLock lock = new ReentrantLock();
    
    public ApuRequestQueue(QueuedApuRepository queuedApuRepository, TransformAgentClient transformAgentClient) {
    	this.queuedApuRepository = queuedApuRepository;
    	this.transformAgentClient = transformAgentClient;
    }

    //TODO refactor to batch processing
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    @Async
	public void add(String apuId) {    	
    	try {
    		lock.lock();
    		var queuedApu = queuedApuRepository.findByApuId(apuId);
    		if (queuedApu == null) {
    			queuedApu = new QueuedApu();
    			queuedApu.setApuId(apuId);
    			queuedApu.setCreated(Instant.now());
    			queuedApuRepository.save(queuedApu);
    		}
    	} finally {
    		lock.unlock();
    	}	
	}

    /**
     * Send batch of apu requests to transformagent
     * @return true - some apus were requested, false - nothing to send
     * 
     * Note: call repeatedly until return false
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public boolean sendRequestsBatch() {
        List<QueuedApu> batchToResolve = queuedApuRepository.findTop1000ByRequestSentIsFalse();
        if (batchToResolve.isEmpty()) {
            return false;
        }
        List<String> requestedIdsList = new ArrayList<>();
        for (QueuedApu apu : batchToResolve) {
            requestedIdsList.add(apu.getApuId());
            apu.setRequestSent(true);
        }
        transformAgentClient.requestApus(requestedIdsList);
        //queuedApuStore.update(batchToResolve);
        return true;
    }
    
    public int removeForApuId(String apuId) {
        return queuedApuRepository.removeForApuId(apuId);
    }
}
