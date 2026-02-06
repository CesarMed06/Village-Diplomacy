package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class VillagerBehaviorHandler {

    private final Map<UUID, Long> effectCooldown = new HashMap<>();
    private final Set<UUID> processedVillagers = new HashSet<>();

    private static final long EFFECT_DURATION_MS = 20000;

    @SubscribeEvent
    public void onVillagerSpawn(EntityJoinLevelEvent event) {
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
                    if (!(livingEntity instanceof ServerPlayer)) return false;
                    ServerPlayer player = (ServerPlayer) livingEntity;

                    if (!(villager.level() instanceof ServerLevel)) return false;
                    ServerLevel level = (ServerLevel) villager.level();

                    Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
                    if (nearestVillage.isEmpty()) return false;

                    VillageReputationData data = VillageReputationData.get(level);
                    int reputation = data.getReputation(player.getUUID());

                    return reputation <= -500;
                }
        ));

        processedVillagers.add(villagerId);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (player.tickCount % 20 != 0) return;

        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), villagePos);

        applyVillageEffects(player, level, reputation);

        if (reputation <= -500) {
            makeGolemsHostile(player, level);
        }
    }

    private void applyVillageEffects(ServerPlayer player, ServerLevel level, int reputation) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        if (effectCooldown.containsKey(playerId) &&
                currentTime - effectCooldown.get(playerId) < EFFECT_DURATION_MS) {
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

    private void makeGolemsHostile(ServerPlayer player, ServerLevel level) {
        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(24.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {
            if (golem.getTarget() != player) {
                golem.setTarget(player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDamageGolem(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        if (!golem.isPlayerCreated()) {
            golem.setTarget(player);
        }
    }
}
