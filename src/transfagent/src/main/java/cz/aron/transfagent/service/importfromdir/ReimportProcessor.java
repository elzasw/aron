package cz.aron.transfagent.service.importfromdir;

import cz.aron.transfagent.domain.ApuSource;

public interface ReimportProcessor {

	/**
	 * 
	 * @param apuSource
	 * @return Return true if apuSource was reimported
	 */
	boolean reimport(ApuSource apuSource);

}
