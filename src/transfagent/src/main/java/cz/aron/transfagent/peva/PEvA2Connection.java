package cz.aron.transfagent.peva;

import cz.aron.peva2.wsdl.PEvA;

public class PEvA2Connection {

    private final PEvA peva;

    private final String institutionId;

    private final String userName;

    private final String userId;

    private final boolean mainConnection;

    public PEvA2Connection(PEvA peva, String institutionId, String userName, String userId, boolean mainConnection) {
        this.peva = peva;
        this.institutionId = institutionId;
        this.userName = userName;
        this.userId = userId;
        this.mainConnection = mainConnection;
    }

    public PEvA getPeva() {
        return peva;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isMainConnection() {
        return mainConnection;
    }

}
