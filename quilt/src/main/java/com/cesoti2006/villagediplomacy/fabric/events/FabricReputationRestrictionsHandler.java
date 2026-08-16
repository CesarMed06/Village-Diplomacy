package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FabricReputationRestrictionsHandler {

    private static final int REP_BLOCK_BELLS        = -100;
    private static final int REP_BLOCK_DOORS        = -400;
    private static final int REP_BLOCK_CHESTS       = -400;
    private static final int REP_BLOCK_BEDS         = -500;
    private static final int REP_PENALTY_CRAFTING   = -200;
    private static final int REP_BLOCK_CRAFTING     = -400;
    private static final long MSG_COOLDOWN_MS = 5000;

    private final Map<UUID, Long> msgCooldown = new HashMap<>();

    public void registerEvents() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer))
                return net.minecraft.world.InteractionResult.PASS;
            if (!(world instanceof ServerLevel level))
                return net.minecraft.world.InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            boolean isChest = block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
            boolean isDoor  = block instanceof DoorBlock;
            boolean isBell  = block instanceof net.minecraft.world.level.block.BellBlock;
            boolean isBed   = block instanceof net.minecraft.world.level.block.BedBlock;
            boolean isCrafting = block == Blocks.CRAFTING_TABLE ||
                    block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE ||
                    block == Blocks.SMOKER || block == Blocks.BREWING_STAND ||
                    block == Blocks.ANVIL || block == Blocks.GRINDSTONE ||
                    block == Blocks.LOOM || block == Blocks.STONECUTTER ||
                    block == Blocks.SMITHING_TABLE || block == Blocks.CARTOGRAPHY_TABLE;

            if (!isChest && !isDoor && !isBell && !isCrafting && !isBed)
                return net.minecraft.world.InteractionResult.PASS;

            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
            if (nearestVillage.isEmpty())
                return net.minecraft.world.InteractionResult.PASS;

            BlockPos villagePos = nearestVillage.get();
            List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(16));

            VillageReputationData data = VillageReputationData.get(level);
            int rep = data.getReputation(serverPlayer.getUUID(), villagePos);

            if (isChest) {
                if (rep <= REP_BLOCK_CHESTS) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.chest", nearby, villagePos);
                    if (rep > -900) {
                        data.addReputation(serverPlayer.getUUID(), villagePos, -10);
                    }
                    return net.minecraft.world.InteractionResult.FAIL;
                }
            }

            else if (isDoor) {
                if (!ReputationTiersHandler.canAccessDoors(rep)) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.door", nearby, villagePos);
                    return net.minecraft.world.InteractionResult.FAIL;
                }
            }

            else if (isBell) {
                if (rep <= REP_BLOCK_BELLS) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.bell", nearby, villagePos);
                    return net.minecraft.world.InteractionResult.FAIL;
                }
            }

            else if (isBed) {
                if (rep <= REP_BLOCK_BEDS) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.bed", nearby, villagePos);
                    if (rep > -900) {
                        data.addReputation(serverPlayer.getUUID(), villagePos, -5);
                    }
                    return net.minecraft.world.InteractionResult.FAIL;
                }
            }

            else if (isCrafting) {
                if (rep <= REP_BLOCK_CRAFTING) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.crafting", nearby, villagePos);
                    if (rep > -900) {
                        data.addReputation(serverPlayer.getUUID(), villagePos, -8);
                    }
                    return net.minecraft.world.InteractionResult.FAIL;
                } else if (rep <= REP_PENALTY_CRAFTING) {
                    sendThrottled(serverPlayer, level, "villagediplomacy.restrict.crafting.warning", nearby, villagePos);
                    if (rep > -900) {
                        data.addReputation(serverPlayer.getUUID(), villagePos, -5);
                    }
                }
            }

            return net.minecraft.world.InteractionResult.PASS;
        });
    }

    private void sendThrottled(ServerPlayer player, ServerLevel level, String key, List<Villager> nearby, BlockPos villagePos) {
        long now = System.currentTimeMillis();
        if (msgCooldown.getOrDefault(player.getUUID(), 0L) + MSG_COOLDOWN_MS > now) return;

        if (!nearby.isEmpty()) {
            Villager villager = nearby.get(level.getRandom().nextInt(nearby.size()));
            String messageKey = key + ".villager";
            ModLang.sendDialogRandom(player, level.getRandom(), villager, messageKey, 3);
        } else {
            player.sendSystemMessage(Component.translatable(key));
        }
        msgCooldown.put(player.getUUID(), now);
    }
}
