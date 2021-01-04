package cz.aron.transfagent.service.importfromdir;

public class ImportContext {

	int numProcessed = 0;
	
	boolean failed = false;
	
	public void addProcessed() {
		numProcessed++;
	}

	public int getNumProcessed() {
		return numProcessed;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}	
}
