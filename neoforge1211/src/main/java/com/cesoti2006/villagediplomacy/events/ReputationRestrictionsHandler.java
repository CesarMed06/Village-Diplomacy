package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.PlayerPlacedBlocks;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.util.InnkeeperHelper;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReputationRestrictionsHandler {

    private static final int REP_FLOOR = -900;
    private static final long MSG_COOLDOWN_MS = 2500;
    private static final double WITNESS_RANGE = 16.0D;

    private final Map<String, Long> msgCooldown = new HashMap<>();

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        PlayerPlacedBlocks placedCheck = PlayerPlacedBlocks.get(level);
        if (placedCheck.isPlacedBy(level, pos, player.getUUID())) {
            return;
        }
        if (placedCheck.isPlayerBuiltZone(level, pos, player.getUUID())) {
            return;
        }

        boolean isChest = block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
        boolean isDoor  = block instanceof DoorBlock;
        boolean isBed   = block instanceof BedBlock;
        boolean isCrafting = block == Blocks.CRAFTING_TABLE ||
                            block == Blocks.FURNACE ||
                            block == Blocks.BLAST_FURNACE ||
                            block == Blocks.SMOKER ||
                            block == Blocks.BREWING_STAND ||
                            block == Blocks.ANVIL ||
                            block == Blocks.GRINDSTONE ||
                            block == Blocks.LOOM ||
                            block == Blocks.STONECUTTER ||
                            block == Blocks.SMITHING_TABLE ||
                            block == Blocks.CARTOGRAPHY_TABLE;

        if (!isChest && !isDoor && !isCrafting && !isBed) return;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();
        var cfg = VillageDiplomacyConfig.INSTANCE;
        VillageReputationData data = VillageReputationData.get(level);
        int rep = data.getReputation(player.getUUID(), villagePos);

        if (isChest && rep <= cfg.restrictChestThreshold.get()) {
            event.setCanceled(true);
            notifyRestriction(player, level, pos, "villagediplomacy.restrict.chest", data, villagePos, rep,
                    cfg.repChestOpen.get());
        } else if (isDoor && !ReputationTiersHandler.canAccessDoors(rep)) {
            event.setCanceled(true);
            notifyRestriction(player, level, pos, "villagediplomacy.restrict.door", data, villagePos, rep, 0);
        } else if (isBed && rep <= cfg.restrictBedThreshold.get()
                && !InnkeeperHelper.isInnBed(level, pos, villagePos)) {
            event.setCanceled(true);
            notifyRestriction(player, level, pos, "villagediplomacy.restrict.bed", data, villagePos, rep,
                    cfg.restrictBedPenalty.get());
        } else if (isCrafting && rep <= cfg.restrictCraftingBlockThreshold.get()) {
            event.setCanceled(true);
            notifyRestriction(player, level, pos, "villagediplomacy.restrict.crafting", data, villagePos, rep,
                    cfg.repWorkstationUse.get());
        } else if (isCrafting && rep <= cfg.restrictCraftingWarnThreshold.get()) {
            notifyRestriction(player, level, pos, "villagediplomacy.restrict.crafting.warning", data, villagePos, rep,
                    cfg.restrictCraftingWarnPenalty.get());
        }
    }

    private void notifyRestriction(ServerPlayer player, ServerLevel level, BlockPos pos, String messageKey,
                                   VillageReputationData data, BlockPos villagePos, int reputation, int penalty) {
        long now = System.currentTimeMillis();
        String throttleKey = player.getUUID() + "|" + messageKey;
        if (msgCooldown.getOrDefault(throttleKey, 0L) + MSG_COOLDOWN_MS > now) return;
        msgCooldown.put(throttleKey, now);

        Villager witness = findWitness(level, player, pos);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness, messageKey + ".villager", 3);
        }

        player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.RED));

        if (penalty < 0 && reputation > REP_FLOOR) {
            data.addReputation(player.getUUID(), villagePos, penalty);
            int newRep = data.getReputation(player.getUUID(), villagePos);
            VillagerEventHandler.checkAndNotifyReputationChange(player, reputation, newRep);
            ModLang.sendReputationSummary(player, penalty, newRep);
        }
    }

    private Villager findWitness(ServerLevel level, ServerPlayer player, BlockPos pos) {
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(WITNESS_RANGE));
        for (Villager villager : nearby) {
            if (VillagerEventHandler.hasLineOfSight(villager, player, level)) {
                return villager;
            }
        }
        return null;
    }
}
