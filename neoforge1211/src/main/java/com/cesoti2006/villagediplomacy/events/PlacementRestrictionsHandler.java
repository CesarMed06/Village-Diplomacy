package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.util.BlockRules;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlacementRestrictionsHandler {

    private static final int REP_FLOOR = -900;
    private static final long WINDOW_MS = 8000;
    private static final double WITNESS_RANGE = 32.0D;

    private static final Map<UUID, Long> windowStart = new HashMap<>();
    private static final Map<UUID, Integer> windowPenalty = new HashMap<>();
    private static final Map<UUID, Integer> windowBlocks = new HashMap<>();
    private static final Map<UUID, BlockPos> windowVillage = new HashMap<>();

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();

        Block placed = event.getPlacedBlock().getBlock();
        if (BlockRules.isPlant(placed) || BlockRules.isFireHazard(placed)) return;

        flushPending(player);

        if (ownsVillageAt(level, pos, player)) return;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;

        var cfg = VillageDiplomacyConfig.INSTANCE;
        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        UUID playerId = player.getUUID();
        int rep = data.getReputation(playerId, villagePos);

        if (rep >= ReputationTiersHandler.ReputationTier.HERO.getMinReputation()) {
            return;
        }

        if (rep <= cfg.restrictPlacementBlockThreshold.get()) {
            event.setCanceled(true);
            announce(player, level, pos, "villagediplomacy.restrict.placement.blocked", villagePos, rep, 0, rep);
            return;
        }

        int penalty;
        String messageKey;
        if (rep < cfg.restrictPlacementDislikedThreshold.get()) {
            penalty = cfg.restrictPlacementDislikedPenalty.get();
            messageKey = "villagediplomacy.restrict.placement.disliked";
        } else if (rep < cfg.restrictPlacementSuspiciousThreshold.get()) {
            penalty = cfg.restrictPlacementSuspiciousPenalty.get();
            messageKey = "villagediplomacy.restrict.placement.suspicious";
        } else if (rep < 0) {
            penalty = cfg.restrictPlacementNeutralPenalty.get();
            messageKey = "villagediplomacy.restrict.placement.neutral";
        } else {
            return;
        }

        int newRep = rep;
        if (rep > REP_FLOOR) {
            data.addReputation(playerId, villagePos, penalty);
            newRep = data.getReputation(playerId, villagePos);
        }

        announce(player, level, pos, messageKey, villagePos, rep, penalty, newRep);
    }

    private static void announce(ServerPlayer player, ServerLevel level, BlockPos pos, String messageKey,
                                 BlockPos villagePos, int oldRep, int penalty, int newRep) {
        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();
        Long start = windowStart.get(playerId);

        if (start != null && now - start < WINDOW_MS) {
            windowPenalty.merge(playerId, penalty, Integer::sum);
            windowBlocks.merge(playerId, 1, Integer::sum);
            windowVillage.put(playerId, villagePos);
            return;
        }

        windowStart.put(playerId, now);

        windowPenalty.put(playerId, penalty);
        windowBlocks.put(playerId, 1);
        windowVillage.put(playerId, villagePos);

        player.sendSystemMessage(Component.translatable(messageKey));

        Villager witness = findWitness(level, player, pos);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness, messageKey + ".villager", 3);
        }

        if (penalty < 0) {
            VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);
            ModLang.sendReputationSummary(player, penalty, newRep);
        }
    }

    public static void flushPending(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Long start = windowStart.get(playerId);
        if (start == null || System.currentTimeMillis() - start < WINDOW_MS) return;

        int blocks = windowBlocks.getOrDefault(playerId, 0);
        int penalty = windowPenalty.getOrDefault(playerId, 0);
        BlockPos villagePos = windowVillage.remove(playerId);
        windowStart.remove(playerId);
        windowBlocks.remove(playerId);
        windowPenalty.remove(playerId);

        if (blocks <= 1 || villagePos == null || penalty == 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        player.sendSystemMessage(Component.translatable("villagediplomacy.restrict.placement.more",
                (penalty > 0 ? "+" : "") + penalty, String.valueOf(blocks)));
        ModLang.sendReputationSummary(player, penalty,
                VillageReputationData.get(level).getReputation(playerId, villagePos));
    }

    public static void forget(UUID playerId) {
        windowStart.remove(playerId);
        windowBlocks.remove(playerId);
        windowPenalty.remove(playerId);
        windowVillage.remove(playerId);
    }

    private static Villager findWitness(ServerLevel level, ServerPlayer player, BlockPos pos) {
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(WITNESS_RANGE));
        for (Villager villager : nearby) {
            if (VillagerEventHandler.hasLineOfSight(villager, player, level)) {
                return villager;
            }
        }
        return null;
    }

    private static boolean ownsVillageAt(ServerLevel level, BlockPos pos, ServerPlayer player) {
        PlayerClaimedVillageData data = PlayerClaimedVillageData.get(level);
        for (PlayerClaimedVillageData.ClaimedVillage village : data.getAllVillages()) {
            if (!village.owner.equals(player.getUUID())) continue;
            if (Math.abs(pos.getX() - village.center.getX()) > village.radius) continue;
            if (Math.abs(pos.getZ() - village.center.getZ()) > village.radius) continue;
            return true;
        }
        return false;
    }
}
