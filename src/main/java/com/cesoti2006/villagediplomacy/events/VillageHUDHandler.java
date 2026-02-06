package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VillageHUDHandler {

    private final Map<UUID, String> currentVillage = new HashMap<>();
    private final Map<UUID, Long> lastVillageChangeTime = new HashMap<>();
    private final Map<UUID, Long> playerSpawnTime = new HashMap<>();

    private static final long VILLAGE_CHECK_INTERVAL = 40;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (player.tickCount % VILLAGE_CHECK_INTERVAL != 0) return;
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        
        // Registrar spawn time si es la primera vez que vemos este jugador
        if (!playerSpawnTime.containsKey(playerId)) {
            playerSpawnTime.put(playerId, System.currentTimeMillis());
        }
        
        // No mostrar mensajes durante los primeros 5 segundos después de spawn
        long timeSinceSpawn = System.currentTimeMillis() - playerSpawnTime.get(playerId);
        if (timeSinceSpawn < 5000) {
            return;
        }

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isPresent()) {
            VillageRelationshipData relationData = VillageRelationshipData.get(level);
            relationData.registerVillage(nearestVillage.get());

            String villageId = relationData.getVillageId(nearestVillage.get());
            String previousVillage = currentVillage.get(playerId);

            if (!villageId.equals(previousVillage)) {
                long currentTime = System.currentTimeMillis();
                Long lastChange = lastVillageChangeTime.get(playerId);

                if (lastChange == null || currentTime - lastChange > 3000) {
                    String villageName = relationData.getVillageName(villageId);

                    VillageReputationData repData = VillageReputationData.get(level);
                    int reputation = repData.getReputation(playerId);

                    String title = "§6§l" + villageName;
                    String subtitle = getReputationSubtitle(reputation);

                    player.sendSystemMessage(Component.literal("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    player.sendSystemMessage(Component.literal(""));
                    player.sendSystemMessage(Component.literal("         " + title));
                    player.sendSystemMessage(Component.literal("         " + subtitle));
                    player.sendSystemMessage(Component.literal(""));
                    player.sendSystemMessage(Component.literal("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

                    currentVillage.put(playerId, villageId);
                    lastVillageChangeTime.put(playerId, currentTime);
                }
            }
        } else {
            if (currentVillage.containsKey(playerId)) {
                long currentTime = System.currentTimeMillis();
                Long lastChange = lastVillageChangeTime.get(playerId);

                if (lastChange == null || currentTime - lastChange > 3000) {
                    player.sendSystemMessage(Component.literal("§7You left the village"));
                    currentVillage.remove(playerId);
                    lastVillageChangeTime.put(playerId, currentTime);
                }
            }
        }
    }

    private String getReputationSubtitle(int reputation) {
        if (reputation >= 500) return "§6✦ Hero of the Village ✦";
        if (reputation >= 200) return "§aFriendly Village";
        if (reputation >= -200) return "§7Neutral Territory";
        if (reputation >= -500) return "§cUnfriendly Village";
        return "§4⚠ Hostile Territory ⚠";
    }
}
