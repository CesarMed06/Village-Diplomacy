package com.cesoti2006.villagediplomacy.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.Entity;

/**
 * Client-side copy of the identities the server assigns to villagers. Entity ids
 * can be reused after a reload, so every lookup is validated against the UUID.
 */
public final class VillagerIdentityCache {

    public record Identity(UUID uuid, String name, String mood, boolean innkeeper) {
    }

    private static final Map<Integer, Identity> IDENTITIES = new HashMap<>();

    private VillagerIdentityCache() {
    }

    public static void put(int entityId, UUID uuid, String name, String mood, boolean innkeeper) {
        IDENTITIES.put(entityId, new Identity(uuid, name, mood, innkeeper));
    }

    public static Identity get(Entity entity) {
        Identity identity = IDENTITIES.get(entity.getId());
        if (identity == null) return null;
        if (!identity.uuid().equals(entity.getUUID())) {
            IDENTITIES.remove(entity.getId());
            return null;
        }
        return identity;
    }

    public static void clear() {
        IDENTITIES.clear();
    }
}
