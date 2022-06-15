package cz.aron.transfagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dao")
public class ConfigDao {

	/**
	 * Cesta k adresari s dao
	 */
    private String dir;
    
    /**
     * Po odeslani adresar s dao smaze
     */
    private boolean deleteSent;
    
    private int sendInterval = 60;
    
    /**
     * Maximalni pocet neodeslanych dao
     */
    private int queueSize = -1;
    
    /**
     * Pouziva podadresare pro ulozeni nahledu
     */
    private boolean useSubdirs = false;
    
    private SendType send = SendType.data;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

	public boolean isDeleteSent() {
		return deleteSent;
	}

	public void setDeleteSent(boolean deleteSent) {
		this.deleteSent = deleteSent;
	}

	public int getSendInterval() {
		return sendInterval;
	}

	public void setSendInterval(int sendInterval) {
		this.sendInterval = sendInterval;
	}

	public boolean isUseSubdirs() {
		return useSubdirs;
	}

	public void setUseSubdirs(boolean useSubdirs) {
		this.useSubdirs = useSubdirs;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public SendType getSend() {
		return send;
	}

	public void setSend(SendType send) {
		this.send = send;
	}

	public static enum SendType {
		reference,
		data,
		none
	}

}
