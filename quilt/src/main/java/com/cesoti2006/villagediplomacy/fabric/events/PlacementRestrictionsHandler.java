package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import java.util.*;


public class PlacementRestrictionsHandler {

    private static final long MSG_COOLDOWN_MS = 8000;
    private final Map<UUID, Long> msgCooldown = new HashMap<>();

    public void registerEvents() {
        
        
        
    }

    private void sendThrottled(ServerPlayer player, ServerLevel level, String key, BlockPos villagePos) {
        long now = System.currentTimeMillis();
        if (msgCooldown.getOrDefault(player.getUUID(), 0L) + MSG_COOLDOWN_MS > now) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
            new AABB(villagePos).inflate(100));

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
