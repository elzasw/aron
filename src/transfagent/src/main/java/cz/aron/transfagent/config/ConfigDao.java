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
     * Pouziva podadresare pro ulozeni nahledu
     */
    private boolean useSubdirs = false;

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

}
