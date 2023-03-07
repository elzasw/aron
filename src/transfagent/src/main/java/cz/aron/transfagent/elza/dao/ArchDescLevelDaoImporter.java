package cz.aron.transfagent.elza.dao;

import cz.aron.apux._2020.Apu;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.Level;

public interface ArchDescLevelDaoImporter {

    /**
     * Importuje dao k jedne urovni
     * @param lvl aktualne zpracovavana uroven popisu
     * @param apu vytvarene apu
     * @param dataProvider data provider
     * @param daoRefReg rozhrani pro registraci vytvorenych dao pro dalsi zpracovani
     * @return pocet vytvorenych dao 
     */
    int importDaos(Level lvl, Apu apu, ContextDataProvider dataProvider, DaoRefRegistration daoRefReg);
    
}
