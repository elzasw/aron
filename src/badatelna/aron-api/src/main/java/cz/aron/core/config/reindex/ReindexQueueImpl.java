package cz.aron.core.config.reindex;

import cz.inqool.eas.common.domain.index.reindex.queue.ReindexQueue;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Lukas Jane (inQool) 10.11.2020.
 */
@Entity
@Table(name = "reindex_queue")
public class ReindexQueueImpl extends ReindexQueue<ReindexQueueImpl> {
}
