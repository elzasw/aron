package cz.aron.transfagent.elza.dao;

import java.util.UUID;

@FunctionalInterface
public interface DaoRefRegistration {
    
    /**
     * Registruje vytvorene dao
     * @param source zdroj dat
     * @param handle handle pro ziskani dao
     * @param uuid uuid dao
     */
    void registerDao(String source, String handle, UUID uuid);

}
