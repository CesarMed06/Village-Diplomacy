package com.cesoti2006.villagediplomacy.integration.guardvillagers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import com.cesoti2006.villagediplomacy.util.ModLang;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GuardVillagersCompat {

    private static final String GUARD_VILLAGERS_MOD_ID = "guardvillagers";
    private static Boolean loadedCache = null;
    private static final Map<UUID, String> guardNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> guardAttackCooldown = new ConcurrentHashMap<>();
    private static final long GUARD_ATTACK_COOLDOWN_MS = 3000;
    private static final double GUARD_ENGAGE_RADIUS = 48.0;
    private static final double VILLAGE_BOUND_SQR = 16384.0;
    private static final double GUARD_VERTICAL_RANGE = 20.0;
    private static final double GUARD_CHASE_SPEED = 1.1;
    private static final double GUARD_NAME_RADIUS = 48.0;

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

                String custom = ModLang.stripFormatting(guard.getCustomName().getString());
                if (!custom.isBlank()) {
                    return custom;
                }
            }
            return generateGuardName();
        });
    }

    private static void assignGuardName(LivingEntity guard) {
        String name = getGuardName(guard);
        if (!guard.hasCustomName()) {
            guard.setCustomName(Component.literal(name));
        }
        guard.setCustomNameVisible(true);
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
        int randomIdx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.guard.entry." + mood, 3);
        return Optional.of(Component.translatable("villagediplomacy.guard.entry." + mood + "." + randomIdx,
                ModLang.nameComponent(guardName)));
    }

    public static Optional<Component> getGuardKillReaction(ServerLevel level, ServerPlayer player, String mobName, int reputation) {
        if (!isLoaded()) return Optional.empty();
        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), 32.0);

        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        String guardName = getGuardName(guard);

        String mood = getGuardMoodKey(reputation);
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.guard.kill." + mood, 4);
        return Optional.of(Component.translatable("villagediplomacy.guard.kill." + mood + "." + idx,
                ModLang.nameComponent(guardName), mobName));
    }

    public static boolean makeGuardsHostile(ServerLevel level, ServerPlayer player, BlockPos villagePos) {
        if (!isLoaded()) return false;

        boolean engaged = false;
        for (LivingEntity guard : findNearbyGuards(level, player.blockPosition(), GUARD_ENGAGE_RADIUS)) {
            if (guard.blockPosition().distSqr(villagePos) > VILLAGE_BOUND_SQR) continue;
            if (Math.abs(guard.getY() - player.getY()) > GUARD_VERTICAL_RANGE) continue;
            if (!(guard instanceof Mob mob)) continue;

            if (mob.getTarget() != player) {
                mob.setTarget(player);
            }
            mob.setAggressive(true);

            if (mob instanceof NeutralMob neutral) {
                neutral.setPersistentAngerTarget(player.getUUID());
                neutral.startPersistentAngerTimer();
            }

            mob.getNavigation().moveTo(player, GUARD_CHASE_SPEED);
            engaged = true;
        }
        return engaged;
    }

    public static void clearGuardTargets(ServerLevel level, ServerPlayer player, BlockPos villagePos) {
        if (!isLoaded()) return;

        for (LivingEntity guard : findNearbyGuards(level, player.blockPosition(), GUARD_ENGAGE_RADIUS)) {
            if (guard.blockPosition().distSqr(villagePos) > VILLAGE_BOUND_SQR) continue;
            if (!(guard instanceof Mob mob)) continue;

            if (mob.getTarget() == player) {
                mob.setTarget(null);
                mob.setAggressive(false);
                mob.getNavigation().stop();

                if (mob instanceof NeutralMob neutral) {
                    neutral.stopBeingAngry();
                }
            }
        }
    }

    public static void ensureGuardNames(ServerLevel level, ServerPlayer player) {
        if (!isLoaded()) return;
        findNearbyGuards(level, player.blockPosition(), GUARD_NAME_RADIUS);
    }

    public static boolean isGuard(Entity entity) {
        if (!isLoaded()) return false;
        return EntityType.getKey(entity.getType()).toString().contains(GUARD_VILLAGERS_MOD_ID);
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

    public static Optional<Component> getGuardChaseMessage(ServerLevel level, ServerPlayer player, BlockPos villagePos) {
        if (!isLoaded()) return Optional.empty();

        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), GUARD_ENGAGE_RADIUS);
        guards.removeIf(guard -> guard.blockPosition().distSqr(villagePos) > VILLAGE_BOUND_SQR);
        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.guard.chase", 4);
        return Optional.of(Component.translatable("villagediplomacy.guard.chase." + idx,
                ModLang.nameComponent(getGuardName(guard))));
    }

    public static Optional<Component> getGuardAttackReaction(ServerLevel level, ServerPlayer player, LivingEntity guard, int reputation) {
        if (!isLoaded()) return Optional.empty();

        String guardName = getGuardName(guard);
        String mood = getGuardMoodKey(reputation);
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.guard.attack." + mood, 3);
        String key = "villagediplomacy.guard.attack." + mood + "." + idx;
        return Optional.of(Component.translatable(key, ModLang.nameComponent(guardName)));
    }
}
