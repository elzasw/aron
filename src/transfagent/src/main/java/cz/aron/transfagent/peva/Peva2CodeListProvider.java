package cz.aron.transfagent.peva;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class Peva2CodeListProvider {
	
	private final Peva2CodeListDownloader codeListDownloader;
	
	private Peva2CodeLists codeLists = null;
	
	public Peva2CodeListProvider(Peva2CodeListDownloader codeListDownloader) {
		this.codeListDownloader = codeListDownloader;
	}

	public synchronized Peva2CodeLists getCodeLists() {
		if (codeLists == null) {
			codeLists = codeListDownloader.downloadCodeLists();
		}
		return codeLists;
	}
	
	public synchronized void reset() {
		codeLists = null;
	}

}
