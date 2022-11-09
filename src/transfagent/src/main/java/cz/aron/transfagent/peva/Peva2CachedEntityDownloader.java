package cz.aron.transfagent.peva;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.peva2.wsdl.GeoObject;
import cz.aron.peva2.wsdl.GetGeoObjectRequest;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CachedEntityDownloader {
	
	private static final Logger log = LoggerFactory.getLogger(Peva2CachedEntityDownloader.class);
	
	private final PEvA2Connection peva2;
	
	private LRUMap<String,GeoObject> geoObjects = new LRUMap<>(1000);

	public Peva2CachedEntityDownloader(PEvA2Connection peva2) {
		super();
		this.peva2 = peva2;						
	}

	public synchronized void clearAll() {
		geoObjects.clear();
	}
	
	public synchronized GeoObject getGeoObject(String id) {		
		var geoObject = geoObjects.get(id);
		if (geoObject!=null) {
			return geoObject;
		}		
		var goReq = new GetGeoObjectRequest();
		var goResp = peva2.getPeva().getGeoObject(goReq);
		if (goResp.getGeoObject()==null) {
			log.error("Geo object not exist {}", id);
			throw new IllegalStateException();
		}
		geoObjects.put(id, goResp.getGeoObject());		
		return goResp.getGeoObject();		
	}	

}
