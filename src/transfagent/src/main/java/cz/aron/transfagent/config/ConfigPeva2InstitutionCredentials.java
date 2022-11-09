package cz.aron.transfagent.config;

import java.util.Objects;

public class ConfigPeva2InstitutionCredentials {
    
    private String institutionId;
    
    private String username;
    
    private String password;

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(institutionId, password, username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigPeva2InstitutionCredentials other = (ConfigPeva2InstitutionCredentials) obj;
        return Objects.equals(institutionId, other.institutionId) && Objects.equals(password, other.password) && Objects
                .equals(username, other.username);
    }    

}
