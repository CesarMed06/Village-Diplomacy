package com.cesoti2006.villagediplomacy.integration.mca;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.fabricmc.loader.api.FabricLoader;

import com.cesoti2006.villagediplomacy.util.ModLang;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MCARebornCompat {

    private static final String MCA_MOD_ID = "mca";

    private static final String[] VILLAGER_CLASS_CANDIDATES = {
            "net.mca.entity.VillagerEntityMCA",
            "forge.net.mca.entity.VillagerEntityMCA",
            "fabric.net.mca.entity.VillagerEntityMCA",
            "quilt.net.mca.entity.VillagerEntityMCA"
    };

    private static final String[] PROFESSIONS_CLASS_CANDIDATES = {
            "net.mca.ProfessionsMCA",
            "forge.net.mca.ProfessionsMCA",
            "fabric.net.mca.ProfessionsMCA",
            "quilt.net.mca.ProfessionsMCA"
    };

    private static Boolean loadedCache = null;
    private static boolean classesResolved = false;
    private static Method isGuardMethod = null;
    private static Class<?> mcaVillagerClass = null;
    private static Class<?> mcaProfessionsClass = null;
    private static final Map<UUID, String> guardNames = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> guardAttackCooldown = new ConcurrentHashMap<>();
    private static final long GUARD_ATTACK_COOLDOWN_MS = 3000;
    private static final double GUARD_ENGAGE_RADIUS = 48.0;
    private static final double VILLAGE_BOUND_SQR = 16384.0;
    private static final double GUARD_VERTICAL_RANGE = 20.0;
    private static final double GUARD_CHASE_SPEED = 1.1;
    private static final double GUARD_NAME_RADIUS = 48.0;
    private static final double PROMOTE_RANGE = 32.0;

    private static final long GUARD_PURSUIT_TTL_MS = 3000L;

    private static final long GUARD_REPATH_INTERVAL_MS = 500L;

    private static final long GUARD_MELEE_COOLDOWN_MS = 1200L;

    private static final float GUARD_MELEE_DAMAGE = 4.0F;

    private static final float GUARD_MELEE_DAMAGE_WANTED = 1.5F;

    private static final int WANTED_REPUTATION = -1000;

    private static final double GUARD_MELEE_REACH_SQR = 6.25;

    private static final double GUARD_LEASH_FACTOR = 2.0;

    private static final Map<UUID, Hunt> activeHunts = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> guardRepathAt = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> guardAttackAt = new ConcurrentHashMap<>();

    private static final class Hunt {
        private final Set<UUID> guards = ConcurrentHashMap.newKeySet();
        private volatile long expiresAt;
        private volatile int reputation;
    }

    private MCARebornCompat() {}

    public static boolean isLoaded() {
        if (loadedCache == null) {
            loadedCache = FabricLoader.getInstance().isModLoaded(MCA_MOD_ID);
        }
        return loadedCache;
    }

    private static boolean resolveClasses() {
        if (!classesResolved) {
            classesResolved = true;
            mcaVillagerClass = firstLoadedClass(VILLAGER_CLASS_CANDIDATES);
            mcaProfessionsClass = firstLoadedClass(PROFESSIONS_CLASS_CANDIDATES);
            if (mcaVillagerClass != null) {
                try {
                    isGuardMethod = mcaVillagerClass.getMethod("isGuard");
                } catch (Exception ignored) {
                    isGuardMethod = null;
                }
            }
        }
        return mcaVillagerClass != null;
    }

    private static Class<?> firstLoadedClass(String[] candidates) {
        for (String candidate : candidates) {
            try {
                return Class.forName(candidate);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static boolean isSupported() {
        return isLoaded() && resolveClasses();
    }

    public static boolean isMCAVillager(Entity entity) {
        if (!isSupported()) return false;
        return mcaVillagerClass.isInstance(entity);
    }

    public static boolean isMCAGuard(Entity entity) {
        if (!isSupported() || isGuardMethod == null) return false;
        try {
            if (mcaVillagerClass.isInstance(entity)) {
                return (boolean) isGuardMethod.invoke(entity);
            }
        } catch (Exception ignored) {}
        return false;
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
            if (isMCAGuard(entity)) {
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
        getGuardName(guard);
    }

    private static String generateGuardName() {
        String[] names = {"Aldric", "Baldric", "Cedric", "Dorian", "Eldric", "Fredric",
                "Garrick", "Harrick", "Isolde", "Jareck", "Karrick", "Lorrick",
                "Morrick", "Nadric", "Orrick", "Pendrick"};
        return names[new Random().nextInt(names.length)];
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

    public static Optional<Component> getGuardEntryMessage(ServerLevel level, ServerPlayer player,
                                                            BlockPos villagePos, int reputation) {
        if (!isLoaded()) return Optional.empty();

        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), 48.0);
        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        String guardName = getGuardName(guard);
        String mood = getGuardMoodKey(reputation);
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.mcaguard.entry." + mood, 3);
        return Optional.of(Component.translatable(
                "villagediplomacy.mcaguard.entry." + mood + "." + idx, ModLang.nameComponent(guardName)));
    }

    public static Optional<Component> getGuardChaseMessage(ServerLevel level, ServerPlayer player, BlockPos villagePos) {
        if (!isLoaded()) return Optional.empty();

        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), GUARD_ENGAGE_RADIUS);
        guards.removeIf(guard -> guard.blockPosition().distSqr(villagePos) > VILLAGE_BOUND_SQR);
        if (guards.isEmpty()) return Optional.empty();

        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.mcaguard.chase", 4);
        return Optional.of(Component.translatable("villagediplomacy.mcaguard.chase." + idx,
                ModLang.nameComponent(getGuardName(guard))));
    }

    public static Optional<Component> getGuardAttackReaction(ServerLevel level, ServerPlayer player,
                                                              LivingEntity guard, int reputation) {
        if (!isLoaded()) return Optional.empty();
        String guardName = getGuardName(guard);
        String mood = getGuardMoodKey(reputation);
        int idx = ModLang.randomIndex(level.getRandom(), "villagediplomacy.mcaguard.attack." + mood, 3);
        return Optional.of(Component.translatable(
                "villagediplomacy.mcaguard.attack." + mood + "." + idx, ModLang.nameComponent(guardName)));
    }

    public static boolean makeGuardsHostile(ServerLevel level, ServerPlayer player, BlockPos villagePos,
                                            int reputation) {
        if (!isLoaded()) return false;

        boolean engaged = false;
        Hunt hunt = null;
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

            if (hunt == null) {
                hunt = activeHunts.computeIfAbsent(player.getUUID(), k -> new Hunt());
            }
            hunt.expiresAt = System.currentTimeMillis() + GUARD_PURSUIT_TTL_MS;
            hunt.reputation = reputation;
            hunt.guards.add(guard.getUUID());
            engaged = true;
        }
        return engaged;
    }

    public static void tickGuardPursuit(MinecraftServer server) {
        if (server == null || activeHunts.isEmpty() || !isSupported()) return;

        long now = System.currentTimeMillis();
        activeHunts.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt);

        double leashSqr = GUARD_ENGAGE_RADIUS * GUARD_ENGAGE_RADIUS * GUARD_LEASH_FACTOR * GUARD_LEASH_FACTOR;

        for (Map.Entry<UUID, Hunt> entry : activeHunts.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()) continue;

            Hunt hunt = entry.getValue();
            ServerLevel level = player.serverLevel();

            for (Iterator<UUID> it = hunt.guards.iterator(); it.hasNext(); ) {
                UUID guardId = it.next();
                Entity found = level.getEntity(guardId);

                if (!(found instanceof Mob guard) || !guard.isAlive() || !isMCAGuard(guard)
                        || guard.distanceToSqr(player) > leashSqr) {
                    it.remove();
                    guardRepathAt.remove(guardId);
                    guardAttackAt.remove(guardId);
                    continue;
                }

                pursueGuard(guard, player, now, hunt.reputation);
            }

            if (hunt.guards.isEmpty()) {
                activeHunts.remove(entry.getKey());
            }
        }
    }

    private static void pursueGuard(Mob guard, ServerPlayer player, long now, int reputation) {
        UUID guardId = guard.getUUID();
        guard.getLookControl().setLookAt(player, 30.0F, 30.0F);

        Long nextRepath = guardRepathAt.get(guardId);
        if (nextRepath == null || now >= nextRepath) {
            guard.getNavigation().moveTo(player, GUARD_CHASE_SPEED);

            Brain<?> brain = guard.getBrain();
            if (brain != null) {
                brain.setMemory(MemoryModuleType.WALK_TARGET,
                        new WalkTarget(player, (float) GUARD_CHASE_SPEED, 1));
            }
            guardRepathAt.put(guardId, now + GUARD_REPATH_INTERVAL_MS);
        }

        double distanceSqr = guard.distanceToSqr(player);
        if (distanceSqr > GUARD_MELEE_REACH_SQR) return;

        if (distanceSqr > 2.25 && !guard.hasLineOfSight(player)) return;

        Long nextAttack = guardAttackAt.get(guardId);
        if (nextAttack != null && now < nextAttack) return;

        guardAttackAt.put(guardId, now + GUARD_MELEE_COOLDOWN_MS);
        guard.swing(InteractionHand.MAIN_HAND);

        player.hurt(guard.damageSources().mobAttack(guard),
                guardMeleeDamage(player.serverLevel(), reputation));
    }

    private static float guardMeleeDamage(ServerLevel level, int reputation) {
        float damage = GUARD_MELEE_DAMAGE;
        if (reputation <= WANTED_REPUTATION) {
            damage += GUARD_MELEE_DAMAGE_WANTED;
        }
        switch (level.getDifficulty()) {
            case EASY -> damage *= 0.6F;
            case HARD -> damage *= 1.5F;
            default -> { }
        }
        return damage;
    }

    public static boolean isMCAGuardAttack(net.minecraft.world.damagesource.DamageSource source) {
        return source != null && isMCAGuard(source.getEntity());
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

        Hunt hunt = activeHunts.remove(player.getUUID());
        if (hunt != null) {
            for (UUID guardId : hunt.guards) {
                guardRepathAt.remove(guardId);
                guardAttackAt.remove(guardId);
            }
        }
    }

    public static void ensureGuardNames(ServerLevel level, ServerPlayer player) {
        if (!isLoaded()) return;
        findNearbyGuards(level, player.blockPosition(), GUARD_NAME_RADIUS);
    }

    public enum TestRole {
        GUARD("GUARD"),
        ARCHER("ARCHER"),
        CIVIL(null);

        private final String professionField;

        TestRole(String professionField) {
            this.professionField = professionField;
        }
    }

    public static boolean hasNearbyVillager(ServerLevel level, ServerPlayer player) {
        if (!isSupported()) return false;
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(PROMOTE_RANGE))) {
            if (mcaVillagerClass.isInstance(entity)) return true;
        }
        return false;
    }

    public static boolean promoteNearest(ServerLevel level, ServerPlayer player, TestRole role) {
        if (!isSupported()) return false;

        Entity target = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : level.getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(PROMOTE_RANGE))) {
            if (!mcaVillagerClass.isInstance(entity)) continue;
            double distance = entity.distanceToSqr(player);
            if (distance < best) {
                best = distance;
                target = entity;
            }
        }
        if (target == null) return false;

        VillagerProfession profession = VillagerProfession.NONE;
        if (role.professionField != null) {
            if (mcaProfessionsClass == null) return false;
            try {
                Object supplier = mcaProfessionsClass.getField(role.professionField).get(null);
                profession = (VillagerProfession) ((java.util.function.Supplier<?>) supplier).get();
            } catch (Exception e) {
                return false;
            }
        }

        try {
            mcaVillagerClass.getMethod("setProfession", VillagerProfession.class).invoke(target, profession);
        } catch (Exception e) {
            return false;
        }
        return true;
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

    public static Optional<Component> getGuardKillReaction(ServerLevel level, ServerPlayer player,
                                                            String mobName, int reputation) {
        if (!isLoaded()) return Optional.empty();
        List<LivingEntity> guards = findNearbyGuards(level, player.blockPosition(), 32.0);
        if (guards.isEmpty()) return Optional.empty();
        LivingEntity guard = guards.get(level.getRandom().nextInt(guards.size()));
        String guardName = getGuardName(guard);
        String mood = getGuardMoodKey(reputation);
        int idx = level.getRandom().nextInt(4);
        return Optional.of(Component.translatable(
                "villagediplomacy.mcaguard.kill." + mood + "." + idx, ModLang.nameComponent(guardName), mobName));
    }
}
