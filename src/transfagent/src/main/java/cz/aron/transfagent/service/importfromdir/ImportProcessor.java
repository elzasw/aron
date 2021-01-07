package cz.aron.transfagent.service.importfromdir;


public interface ImportProcessor {

	void importData(ImportContext ic);
	
	/**
	 * Higher priority processor will be run first
	 * @return
	 */
	default int getPriority() { return 0; }

}
