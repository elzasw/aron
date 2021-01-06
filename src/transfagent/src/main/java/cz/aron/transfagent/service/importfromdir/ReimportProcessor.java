package cz.aron.transfagent.service.importfromdir;

import cz.aron.transfagent.domain.ApuSource;

public interface ReimportProcessor {
    
    enum Result {
        REIMPORTED,
        NOCHANGES,
        UNSUPPORTED,
        FAILED
    };

	/**
	 * 
	 * @param apuSource
	 * @return Return if apuSource was reimported
	 */
	Result reimport(ApuSource apuSource);

}
