package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.PlayerPlacedBlocks;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ReputationRestrictionsHandler {

    private static final int REP_BLOCK_BELLS        = -100; 
    private static final int REP_BLOCK_DOORS        = -400; 
    private static final int REP_BLOCK_CHESTS       = -400; 
    private static final int REP_BLOCK_BEDS         = -500; 
    private static final int REP_PENALTY_CRAFTING   = -200; 
    private static final int REP_BLOCK_CRAFTING     = -400; 
    private static final long MSG_COOLDOWN_MS = 5000;

    private final Map<UUID, Long> msgCooldown = new HashMap<>();

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

        
        boolean isChest = block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
        boolean isDoor  = block instanceof DoorBlock;
        boolean isBell  = block instanceof net.minecraft.world.level.block.BellBlock;
        boolean isBed   = block instanceof net.minecraft.world.level.block.BedBlock;
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

        if (!isChest && !isDoor && !isBell && !isCrafting && !isBed) return;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(16));

        VillageReputationData data = VillageReputationData.get(level);
        int rep = data.getReputation(player.getUUID(), nearestVillage.get());

        
        if (isChest) {
            if (rep <= REP_BLOCK_CHESTS) {
                event.setCanceled(true);
                sendThrottled(player, level, "villagediplomacy.restrict.chest", nearby);
                
                if (rep > -900) {  
                    data.addReputation(player.getUUID(), nearestVillage.get(), -10);
                }
            }
        }
        
        else if (isDoor) {
            if (!ReputationTiersHandler.canAccessDoors(rep)) {
                event.setCanceled(true);
                sendThrottled(player, level, "villagediplomacy.restrict.door", nearby);
            }
        }
        
        else if (isBell) {
            if (rep <= REP_BLOCK_BELLS) {
                event.setCanceled(true);
                sendThrottled(player, level, "villagediplomacy.restrict.bell", nearby);
            }
        }
        
        else if (isBed) {
            if (rep <= REP_BLOCK_BEDS) {
                event.setCanceled(true);
                sendThrottled(player, level, "villagediplomacy.restrict.bed", nearby);
                
                if (rep > -900) {
                    data.addReputation(player.getUUID(), nearestVillage.get(), -5);
                }
            }
        }
        
        else if (isCrafting) {
            if (rep <= REP_BLOCK_CRAFTING) {
                event.setCanceled(true);
                sendThrottled(player, level, "villagediplomacy.restrict.crafting", nearby);
                
                if (rep > -900) {
                    data.addReputation(player.getUUID(), nearestVillage.get(), -8);
                }
            } else if (rep <= REP_PENALTY_CRAFTING && rep > REP_BLOCK_CRAFTING) {
                
                sendThrottled(player, level, "villagediplomacy.restrict.crafting.warning", nearby);
                
                if (rep > -900) {
                    data.addReputation(player.getUUID(), nearestVillage.get(), -5);
                }
            }
        }
    }

    private void sendThrottled(ServerPlayer player, ServerLevel level, String key, List<Villager> nearby) {
        long now = System.currentTimeMillis();
        if (msgCooldown.getOrDefault(player.getUUID(), 0L) + MSG_COOLDOWN_MS > now) return;
        
        
        if (!nearby.isEmpty()) {
            Villager villager = nearby.get(level.getRandom().nextInt(nearby.size()));
            String messageKey = key + ".villager";
            com.cesoti2006.villagediplomacy.util.ModLang.sendDialogRandom(player, level.getRandom(), villager, messageKey, 3);
        } else {
            
            player.sendSystemMessage(Component.translatable(key));
        }
        msgCooldown.put(player.getUUID(), now);
    }
}
