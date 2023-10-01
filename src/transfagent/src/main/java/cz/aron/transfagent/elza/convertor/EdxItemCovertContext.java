package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.tacr.elza.schema.v2.Level;

public interface EdxItemCovertContext {

	ApuSourceBuilder getApusBuilder();

	Part getActivePart();

	ElzaXmlReader getElzaXmlReader();

	/**
	 * Add reference to another APU
	 * @param aei
	 */
	void addArchEntityRef(ArchEntityInfo aei);

	Apu getActiveApu();
	
	Level getProcessedLevel();
}
