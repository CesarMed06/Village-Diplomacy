package com.cesoti2006.villagediplomacy.integration.guardvillagers;

import com.cesoti2006.villagediplomacy.personality.NameGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.fabricmc.loader.api.FabricLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GuardVillagersCompat {

    private static final String GUARD_VILLAGERS_MOD_ID = "guardvillagers";
    private static Boolean loadedCache = null;
    private static final Map<UUID, String> guardNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> guardAttackCooldown = new ConcurrentHashMap<>();
    private static final long GUARD_ATTACK_COOLDOWN_MS = 3000;

    private GuardVillagersCompat() {}

    public static boolean isLoaded() {
        if (loadedCache == null) {
            loadedCache = FabricLoader.getInstance().isModLoaded(GUARD_VILLAGERS_MOD_ID);
        }
        return loadedCache;
    }

    public static List<LivingEntity> findNearbyGuards(ServerLevel level, BlockPos pos, double radius) {
        List<LivingEntity> guards = new ArrayList<>();
        if (!isLoaded()) return guards;

        for (Entity entity : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(radius))) {
            String typeId = EntityType.getKey(entity.getType()).toString();
            if (typeId.contains("guardvillagers")) {
                guards.add((LivingEntity) entity);

                if (!guardNames.containsKey(entity.getUUID())) {
                    assignGuardName((LivingEntity) entity);
                }
            }
        }
        return guards;
    }

    public static boolean hasGuardsNearby(ServerLevel level, BlockPos pos, double radius) {
        if (!isLoaded()) return false;
        return !findNearbyGuards(level, pos, radius).isEmpty();
    }

    public static String getGuardName(LivingEntity guard) {
        return guardNames.computeIfAbsent(guard.getUUID(), k -> {
            if (guard.hasCustomName()) {
                return guard.getCustomName().getString();
            }
            return generateGuardName();
        });
    }

    private static void assignGuardName(LivingEntity guard) {
        String name = getGuardName(guard);
        if (!guard.hasCustomName()) {
            guard.setCustomName(Component.literal(name));
        }
    }

    private static String generateGuardName() {
        String[] firstNames = {"Jack", "Oliver", "Mary", "Leonard", "Thomas", "William", "Henry", "Arthur",
                "Edmund", "Geoffrey", "Bartholomew", "Roland", "Cedric", "Godfrey", "Humphrey"};
        Random random = new Random();
        return firstNames[random.nextInt(firstNames.length)];
    }

    public static String getMoodKey(int reputation) {
        if (reputation >= 1000) return "hero";
        if (reputation >= 500) return "trusted";
        if (reputation >= 100) return "friendly";
        if (reputation >= 0) return "neutral";
        if (reputation >= -100) return "suspicious";
        if (reputation >= -299) return "hostile";
        return "enemy";
    }

    public static String getKillReactionKey(int reputation) {
        if (reputation >= 1000) return "hero";
        if (reputation >= 500) return "trusted";
        if (reputation >= 0) return "neutral";
        return "suspicious";
    }

    public static void sendGuardReaction(ServerPlayer player, ServerLevel level, String type, int reputation) {
        if (!isLoaded()) return;

        List<LivingEntity> nearbyGuards = findNearbyGuards(level, player.blockPosition(), 48.0);
        if (nearbyGuards.isEmpty()) return;

        LivingEntity guard = nearbyGuards.get(level.getRandom().nextInt(nearbyGuards.size()));
        String guardName = getGuardName(guard);
        String mood = getMoodKey(reputation);

        String key = "villagediplomacy.guard." + type + "." + mood;
        int idx = level.getRandom().nextInt(4);
        player.sendSystemMessage(Component.translatable(key + "." + idx, guardName));
    }

    public static void sendGuardEntryReaction(ServerPlayer player, ServerLevel level, int reputation) {
        sendGuardReaction(player, level, "entry", reputation);
    }

    public static void sendGuardKillReaction(ServerPlayer player, ServerLevel level, int reputation) {
        sendGuardReaction(player, level, "kill", reputation);
    }

    public static void sendGuardAttackReaction(ServerPlayer player, ServerLevel level, LivingEntity guard) {
        String guardName = getGuardName(guard);
        UUID guardId = guard.getUUID();
        long now = System.currentTimeMillis();

        if (guardAttackCooldown.containsKey(guardId) &&
                now - guardAttackCooldown.get(guardId) < GUARD_ATTACK_COOLDOWN_MS) {
            return;
        }
        guardAttackCooldown.put(guardId, now);

        int idx = level.getRandom().nextInt(6);
        player.sendSystemMessage(Component.translatable(
                "villagediplomacy.guard.attacked." + idx, guardName));
    }

    public static void sendGuardDeathReaction(ServerPlayer player, ServerLevel level, LivingEntity guard) {
        String guardName = getGuardName(guard);
        int idx = level.getRandom().nextInt(6);
        player.sendSystemMessage(Component.translatable(
                "villagediplomacy.guard.killed." + idx, guardName));
    }

    public static void resetCache() {
        loadedCache = null;
    }
}
