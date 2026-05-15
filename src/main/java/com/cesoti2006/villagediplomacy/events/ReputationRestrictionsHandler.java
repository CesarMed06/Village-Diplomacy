package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
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

    private static final int REP_BLOCK_CHESTS = -500;
    private static final int REP_BLOCK_DOORS  = -700;
    private static final long MSG_COOLDOWN_MS = 5000;

    private final Map<UUID, Long> msgCooldown = new HashMap<>();

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        boolean isChest = block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL;
        boolean isDoor  = block instanceof DoorBlock;

        if (!isChest && !isDoor) return;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(16));
        if (nearby.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        int rep = data.getReputation(player.getUUID(), nearestVillage.get());

        if (isChest && rep <= REP_BLOCK_CHESTS) {
            event.setCanceled(true);
            sendThrottled(player, "villagediplomacy.restrict.chest");
        } else if (isDoor && rep <= REP_BLOCK_DOORS) {
            event.setCanceled(true);
            sendThrottled(player, "villagediplomacy.restrict.door");
        }
    }

    private void sendThrottled(ServerPlayer player, String key) {
        long now = System.currentTimeMillis();
        if (msgCooldown.getOrDefault(player.getUUID(), 0L) + MSG_COOLDOWN_MS > now) return;
        player.sendSystemMessage(Component.translatable(key));
        msgCooldown.put(player.getUUID(), now);
    }
}
