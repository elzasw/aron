package cz.aron.transfagent.peva.codelist;

import java.util.Map;

import cz.aron.peva2.wsdl.PEvA;

public abstract class CodeProvider<T> {
	
	protected final PEvA peva2;

	private final Map<String, T> cache;

	public CodeProvider(PEvA peva2, Map<String, T> cached) {
		this.peva2 = peva2;
		this.cache = cached;
	}

	public synchronized T getItemById(String id) {
		var cached = cache.get(id);
		if (cached != null || cache.containsKey(id)) {
			return cached;
		}
		T item = downloadItem(id);
		cache.put(id, item);
		return item;
	}

	public abstract T downloadItem(String id);

}
