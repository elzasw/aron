package cz.aron.transfagent.peva;

public class Peva2CodeListProvider {
	
	private final Peva2CodeListDownloader codeListDownloader;
	
	private Peva2CodeLists codeLists = null;
	
	public Peva2CodeListProvider(Peva2CodeListDownloader codeListDownloader) {
		this.codeListDownloader = codeListDownloader;
	}

	public Peva2CodeLists getCodeLists() {
		if (codeLists == null) {
			codeLists = codeListDownloader.downloadCodeLists();
		}
		return codeLists;
	}

}
