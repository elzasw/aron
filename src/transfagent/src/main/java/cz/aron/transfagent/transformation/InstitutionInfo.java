package cz.aron.transfagent.transformation;

import java.util.UUID;

public class InstitutionInfo {

    private final UUID uuid;

    private final String name;

    public InstitutionInfo(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

}
