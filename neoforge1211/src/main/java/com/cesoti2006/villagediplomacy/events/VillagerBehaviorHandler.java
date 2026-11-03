package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.integration.guardvillagers.GuardVillagersCompat;
import com.cesoti2006.villagediplomacy.integration.mca.MCARebornCompat;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

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
    private static final int GUARD_TICK_INTERVAL = 40;
    private static final int GUARD_NAME_TICK_INTERVAL = 100;

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
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.tickCount % PLAYER_TICK_INTERVAL != 0) return;

        ServerLevel level = (ServerLevel) player.level();

        if (player.isCreative() || player.isSpectator()) {
            releaseGolems(level, player);
            return;
        }

        if (player.tickCount % GUARD_NAME_TICK_INTERVAL == 0) {
            GuardVillagersCompat.ensureGuardNames(level, player);
            MCARebornCompat.ensureGuardNames(level, player);
        }

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), villagePos);

        applyVillageEffects(player, level, reputation);

        if (reputation < -100) {
            makeGolemsHostile(player, level, villagePos, reputation);
        } else {
            removeGolemTargets(player, level, villagePos);
        }

        defendWithGuards(player, level, villagePos, reputation);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MCARebornCompat.tickGuardPursuit(event.getServer());
    }

    private static void defendWithGuards(ServerPlayer player, ServerLevel level, BlockPos villagePos, int reputation) {
        if (!VillageDiplomacyConfig.INSTANCE.enableGuardAttacks.get()) return;

        if (player.tickCount % GUARD_TICK_INTERVAL != 0) return;

        if (reputation > VillageDiplomacyConfig.INSTANCE.guardAttackThreshold.get()) {
            GuardVillagersCompat.clearGuardTargets(level, player, villagePos);
            MCARebornCompat.clearGuardTargets(level, player, villagePos);
            return;
        }

        boolean engaged = GuardVillagersCompat.makeGuardsHostile(level, player, villagePos);
        engaged |= MCARebornCompat.makeGuardsHostile(level, player, villagePos, reputation);

        if (engaged) {
            notifyGuardsHostile(player);
            notifyGuardsChase(player, level, villagePos);
        }
    }

    private static final Map<UUID, Long> guardHostileNotice = new ConcurrentHashMap<>();
    private static final long GUARD_NOTICE_COOLDOWN_MS = 30000L;

    private static void notifyGuardsHostile(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = guardHostileNotice.get(player.getUUID());
        if (last != null && now - last < GUARD_NOTICE_COOLDOWN_MS) return;
        guardHostileNotice.put(player.getUUID(), now);

        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.guard_hostile")
                .withStyle(ChatFormatting.RED));
        player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private static final Map<UUID, Long> guardChaseNotice = new ConcurrentHashMap<>();
    private static final long GUARD_CHASE_COOLDOWN_MS = 12000L;

    private static void notifyGuardsChase(ServerPlayer player, ServerLevel level, BlockPos villagePos) {
        long now = System.currentTimeMillis();
        Long last = guardChaseNotice.get(player.getUUID());
        if (last != null && now - last < GUARD_CHASE_COOLDOWN_MS) return;
        guardChaseNotice.put(player.getUUID(), now);

        Optional<Component> line = GuardVillagersCompat.getGuardChaseMessage(level, player, villagePos);
        if (line.isEmpty()) {
            line = MCARebornCompat.getGuardChaseMessage(level, player, villagePos);
        }
        line.ifPresent(msg -> player.sendSystemMessage(msg));
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

    private static final Map<UUID, Long> golemHostileNotice = new ConcurrentHashMap<>();
    private static final long GOLEM_NOTICE_COOLDOWN_MS = 15000L;

    private static void makeGolemsHostile(ServerPlayer player, ServerLevel level, BlockPos playerVillage, int reputation) {
        if (player.isCreative() || player.isSpectator()) return;

        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(24.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {

            if (golem.blockPosition().distSqr(playerVillage) > 16384) continue;
            if (Math.abs(golem.getY() - player.getY()) > 20.0D) continue;
            if (golem.getTarget() == player) continue;

            golem.setTarget(player);
            notifyGolemHostile(player, golem, reputation);
        }
    }

    private static void notifyGolemHostile(ServerPlayer player, IronGolem golem, int reputation) {

        if (reputation > -100) return;

        long now = System.currentTimeMillis();
        Long last = golemHostileNotice.get(player.getUUID());
        if (last != null && now - last < GOLEM_NOTICE_COOLDOWN_MS) return;
        golemHostileNotice.put(player.getUUID(), now);

        String golemName = golem.hasCustomName()
                ? golem.getCustomName().getString()
                : Component.translatable("villagediplomacy.golem.generic_name").getString();
        int idx = player.level().getRandom().nextInt(5);

        player.sendSystemMessage(Component.translatable("villagediplomacy.react.golem.wanted." + idx,
                ModLang.coloredName(golemName, ChatFormatting.AQUA)));
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

    private static void releaseGolems(ServerLevel level, ServerPlayer player) {
        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(64.0D),
                golem -> !golem.isPlayerCreated());

        UUID playerId = player.getUUID();
        for (IronGolem golem : golems) {
            LivingEntity target = golem.getTarget();
            UUID angerTarget = golem.getPersistentAngerTarget();

            boolean targetingPlayer = target != null && target.getUUID().equals(playerId);
            boolean angryAtPlayer = angerTarget != null && angerTarget.equals(playerId);
            if (!targetingPlayer && !angryAtPlayer) continue;

            golem.setPersistentAngerTarget(playerId);
            golem.startPersistentAngerTimer();

            if (targetingPlayer) {
                golem.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onGolemChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (golem.isPlayerCreated()) return;
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) return;
        if (!(event.getNewAboutToBeSetTarget().level() instanceof ServerLevel level)) return;

        if (!player.isCreative() && !player.isSpectator()) return;

        event.setNewAboutToBeSetTarget(null);
        releaseGolems(level, player);
    }

    private static final float MCA_GUARD_MAX_HIT_DAMAGE = 8.0F;

    @SubscribeEvent
    public static void onMCAGuardHitPlayer(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= MCA_GUARD_MAX_HIT_DAMAGE) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!MCARebornCompat.isMCAGuardAttack(event.getSource())) return;

        event.setAmount(MCA_GUARD_MAX_HIT_DAMAGE);
    }

    @SubscribeEvent
    public static void onPlayerDamageGolem(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        if (player.isCreative() || player.isSpectator()) return;

        if (!golem.isPlayerCreated()) {
            golem.setTarget(player);
        }
    }
}
