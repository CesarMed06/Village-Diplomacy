package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerBehaviorHandler {

    private static final Map<UUID, Long> effectCooldown = new ConcurrentHashMap<>();
    private static final Set<UUID> processedVillagers = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, Map<UUID, Boolean>> hostilePlayerCache = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> hostileCacheTimestamps = new ConcurrentHashMap<>();
    private static final long HOSTILE_CACHE_TTL_MS = 2000;
    private static long lastHostileCacheCleanup = 0;
    private static final long HOSTILE_CLEANUP_INTERVAL_MS = 30000; 

    private static final long EFFECT_DURATION_MS = 20000;

    private static final int PLAYER_TICK_INTERVAL = 20;

    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (event.getLevel().isClientSide) return;

        UUID villagerId = villager.getUUID();
        if (processedVillagers.contains(villagerId)) return;

        villager.goalSelector.addGoal(1, new AvoidEntityGoal<>(
            villager,
            Player.class,
            10.0F,
            0.6D,
            0.6D,
            (LivingEntity livingEntity) -> {

                if (!(livingEntity instanceof ServerPlayer player)) return false;
                return isPlayerHostileToVillager(villager, player);
            }
        ));

        processedVillagers.add(villagerId);
    }

    private static boolean isPlayerHostileToVillager(Villager villager, ServerPlayer player) {
        UUID villagerId = villager.getUUID();
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();

        Long lastUpdate = hostileCacheTimestamps.get(villagerId);
        if (lastUpdate != null && (now - lastUpdate) < HOSTILE_CACHE_TTL_MS) {
            Map<UUID, Boolean> playerCache = hostilePlayerCache.get(villagerId);
            if (playerCache != null) {
                Boolean cached = playerCache.get(playerId);
                if (cached != null) return cached;
            }
        }

        if (!(villager.level() instanceof ServerLevel level)) return false;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
        if (nearestVillage.isEmpty()) {

            cacheHostileStatus(villagerId, playerId, false, now);
            return false;
        }

        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(playerId, nearestVillage.get());
        boolean hostile = reputation < -100;

        cacheHostileStatus(villagerId, playerId, hostile, now);
        return hostile;
    }

    private static void cacheHostileStatus(UUID villagerId, UUID playerId, boolean hostile, long timestamp) {
        hostilePlayerCache.computeIfAbsent(villagerId, k -> new HashMap<>()).put(playerId, hostile);
        hostileCacheTimestamps.put(villagerId, timestamp);

        if (timestamp - lastHostileCacheCleanup > HOSTILE_CLEANUP_INTERVAL_MS) {
            long now = System.currentTimeMillis();
            hostileCacheTimestamps.entrySet().removeIf(e -> (now - e.getValue()) > HOSTILE_CACHE_TTL_MS * 3);
            hostilePlayerCache.keySet().removeIf(k -> !hostileCacheTimestamps.containsKey(k));
            lastHostileCacheCleanup = now;
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        if (event.phase != TickEvent.Phase.END) return;

        if (player.tickCount % PLAYER_TICK_INTERVAL != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), villagePos);

        applyVillageEffects(player, level, reputation);

        if (reputation < -100) {
            makeGolemsHostile(player, level, villagePos);
        } else {
            removeGolemTargets(player, level, villagePos);
        }
    }

    private static void applyVillageEffects(ServerPlayer player, ServerLevel level, int reputation) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastEffect = effectCooldown.get(playerId);
        if (lastEffect != null && currentTime - lastEffect < EFFECT_DURATION_MS) {
            return;
        }

        if (reputation >= 800) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false, true));
            effectCooldown.put(playerId, currentTime);
        } else if (reputation >= 500) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false, true));
            effectCooldown.put(playerId, currentTime);
        } else if (reputation <= -500) {
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 200, 0, false, false, true));
            effectCooldown.put(playerId, currentTime);
        }
    }

    private static void makeGolemsHostile(ServerPlayer player, ServerLevel level, BlockPos playerVillage) {
        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(24.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {

            if (golem.blockPosition().distSqr(playerVillage) > 32400) continue; 

            if (golem.getTarget() != player) {
                golem.setTarget(player);
            }
        }
    }

    private static void removeGolemTargets(ServerPlayer player, ServerLevel level, BlockPos playerVillage) {
        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(24.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {

            if (golem.blockPosition().distSqr(playerVillage) > 32400) continue;

            if (golem.getTarget() == player) {
                golem.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDamageGolem(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        if (!golem.isPlayerCreated()) {
            golem.setTarget(player);
        }
    }
}
