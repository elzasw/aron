package cz.aron.transfagent.domain;

public enum EntityStatus {

	/**
	 * Na zaklade jineho vstupu je mozne entitu publikovat.
	 * Vznikne napr. diky referenci z archivniho popisu. 
	 * Bude stazena z externiho systemu
	 */
    ACCESSIBLE,

    /**
     * Stazena z externiho systemu do transformacniho agenta
     * Je pripravena k odeslani nebo jiz byla odeslana
     */
    AVAILABLE,
    
    /**
     * Entita neni referencovana z zadneho archivniho popisu
     * Drive pravdepodobne byla
     */
    NOT_ACCESSIBLE,
    
	/**
	 * Nepodarilo se stahnout z externiho systemu protoze entitu s danym
	 * identifikatorem nezna
	 */
	NOT_AVAILABLE

}
