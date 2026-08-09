package com.academy.trafficviolationsystem.audit;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class AuditIdCodec {

    private AuditIdCodec() {
    }

    public static UUID toStorableEntityId(Object rawId, String entityType) {
        if (rawId == null) {
            return null;
        }

        if (rawId instanceof UUID uuid) {
            return uuid;
        }

        String seed = (entityType != null ? entityType : "entity") + "-" + rawId;

        return UUID.nameUUIDFromBytes(
                seed.getBytes(StandardCharsets.UTF_8)
        );
    }
}
