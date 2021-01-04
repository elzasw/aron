package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;

public interface EdxItemCovertContext {

	ApuSourceBuilder getApusBuilder();

	Part getActivePart();

	ElzaXmlReader getElzaXmlReader();
	
	/**
	 * Add reference to another APU
	 * @param uuid
	 */
	void addArchEntityRef(String uuid);

	Apu getActiveApu();

}
