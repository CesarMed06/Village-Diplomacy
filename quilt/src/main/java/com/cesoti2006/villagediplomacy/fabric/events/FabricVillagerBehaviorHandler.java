package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class FabricVillagerBehaviorHandler {

    
    private static final Map<UUID, Long> effectCooldown = new ConcurrentHashMap<>();
    private static final long EFFECT_DURATION_MS = 20000;
    
    private static final int TICK_INTERVAL = 20;
    
    
    private static final Map<String, Boolean> hostileCache = new ConcurrentHashMap<>();
    private static final long HOSTILE_CACHE_TTL_MS = 3000;
    private static long lastHostileCacheCleanup = 0;

    public void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            
            
            if (now - lastHostileCacheCleanup > 30000) {
                hostileCache.clear();
                lastHostileCacheCleanup = now;
            }
            
            for (ServerLevel level : server.getAllLevels()) {
                
                for (ServerPlayer player : level.players()) {
                    if (player.tickCount % TICK_INTERVAL != 0) continue;

                    Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
                    if (nearestVillage.isEmpty()) continue;

                    BlockPos villagePos = nearestVillage.get();
                    VillageReputationData data = VillageReputationData.get(level);
                    int reputation = data.getReputation(player.getUUID(), villagePos);

                    applyVillageEffects(player, reputation);

                    if (reputation < -100) {
                        makeGolemsHostile(player, level, villagePos);
                    } else {
                        removeGolemTargets(player, level, villagePos);
                    }
                }
                
                
                for (ServerPlayer player : level.players()) {
                    if (player.tickCount % TICK_INTERVAL != 0) continue;
                    
                    List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
                        player.getBoundingBox().inflate(32.0D)); 
                    
                    for (Villager villager : nearbyVillagers) {
                        if (villager.distanceToSqr(player) > 100.0) continue; 
                        
                        
                        String cacheKey = villager.getUUID() + ":" + player.getUUID();
                        Boolean cached = hostileCache.get(cacheKey);
                        boolean hostile;
                        
                        if (cached != null) {
                            hostile = cached;
                        } else {
                            Optional<BlockPos> v = VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
                            if (v.isEmpty()) {
                                hostileCache.put(cacheKey, false);
                                continue;
                            }
                            VillageReputationData d = VillageReputationData.get(level);
                            int rep = d.getReputation(player.getUUID(), v.get());
                            hostile = rep < -100;
                            hostileCache.put(cacheKey, hostile);
                        }
                        
                        if (hostile) {
                            net.minecraft.world.phys.Vec3 away = new net.minecraft.world.phys.Vec3(
                                villager.position().x - player.position().x,
                                0,
                                villager.position().z - player.position().z
                            ).normalize();
                            net.minecraft.world.phys.Vec3 fleePos = villager.position().add(away.scale(8.0));
                            villager.getNavigation().moveTo(fleePos.x, villager.getY(), fleePos.z, 0.8D);
                        }
                    }
                }
            }
        });
    }

    private static void applyVillageEffects(ServerPlayer player, int reputation) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastEffect = effectCooldown.get(playerId);
        if (lastEffect != null && currentTime - lastEffect < EFFECT_DURATION_MS) return;

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
}
