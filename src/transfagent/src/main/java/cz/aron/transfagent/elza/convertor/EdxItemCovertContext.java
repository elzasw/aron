package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;

public interface EdxItemCovertContext {

	ApuSourceBuilder getApusBuilder();

	Part getActivePart();

	ElzaXmlReader getElzaXmlReader();

}
