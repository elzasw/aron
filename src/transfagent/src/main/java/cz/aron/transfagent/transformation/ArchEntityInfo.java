package cz.aron.transfagent.transformation;

import java.util.UUID;

public class ArchEntityInfo {

    private final UUID uuid;

    private final String entityClass;

    public ArchEntityInfo(UUID uuid, String entityClass) {
        this.uuid = uuid;
        this.entityClass = entityClass;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getEntityClass() {
        return entityClass;
    }

}
