package com.cesoti2006.villagediplomacy.integration.guardvillagers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;

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
            loadedCache = ModList.get() != null && ModList.get().isLoaded(GUARD_VILLAGERS_MOD_ID);
        }
        return loadedCache;
    }

    public static void invalidateCache() {
        loadedCache = null;
    }

    public static boolean hasGuardsNearby(ServerLevel level, BlockPos pos, double radius) {
        if (!isLoaded()) return false;
        return !findNearbyGuards(level, pos, radius).isEmpty();
    }

    public static List<LivingEntity> findNearbyGuards(ServerLevel level, BlockPos pos, double radius) {
        List<LivingEntity> guards = new ArrayList<>();
        if (!isLoaded()) return guards;

        for (Entity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(radius))) {
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

    public static String getGuardMoodKey(int reputation) {
        if (reputation >= 800) return "hero";
        if (reputation >= 300) return "trusted";
        if (reputation >= 100) return "friendly";
        if (reputation >= 0) return "neutral";
        if (reputation >= -200) return "suspicious";
        if (reputation >= -500) return "hostile";
        return "enemy";
    }

    public static Optional<Component> getGuardEntryMessage(ServerLevel level, ServerPlayer player, BlockPos villagePos, int reputation) {
        if (!isLoaded()) return Optional.empty();

        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), 48.0);
        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        String guardName = getGuardName(guard);

        String mood = getGuardMoodKey(reputation);
        int randomIdx = level.getRandom().nextInt(3);
        return Optional.of(Component.translatable("villagediplomacy.guard.entry." + mood + "." + randomIdx, guardName));
    }

    public static Optional<Component> getGuardKillReaction(ServerLevel level, ServerPlayer player, String mobName, int reputation) {
        if (!isLoaded()) return Optional.empty();
        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), 32.0);

        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        String guardName = getGuardName(guard);

        String mood = getGuardMoodKey(reputation);
        int idx = level.getRandom().nextInt(4);
        return Optional.of(Component.translatable("villagediplomacy.guard.kill." + mood + "." + idx, guardName, mobName));
    }

    public static boolean tryAttackCooldown(UUID playerId) {
        long now = System.currentTimeMillis();
        Long lastAttack = guardAttackCooldown.get(playerId);
        if (lastAttack != null && now - lastAttack < GUARD_ATTACK_COOLDOWN_MS) {
            return false;
        }
        guardAttackCooldown.put(playerId, now);
        return true;
    }

    public static Optional<Component> getGuardAttackReaction(ServerLevel level, ServerPlayer player, LivingEntity guard, int reputation) {
        if (!isLoaded()) return Optional.empty();

        String guardName = getGuardName(guard);
        String mood = getGuardMoodKey(reputation);
        int idx = level.getRandom().nextInt(3);
        String key = "villagediplomacy.guard.attack." + mood + "." + idx;
        return Optional.of(Component.translatable(key, guardName));
    }
}
