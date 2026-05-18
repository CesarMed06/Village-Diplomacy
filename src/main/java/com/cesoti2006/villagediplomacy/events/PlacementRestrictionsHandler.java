package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Maneja restricciones de colocación de bloques según reputación
 * - Heroes (800+): Pueden construir libremente
 * - Champiiones-Friendly (100-799): Pueden construir sin penalidad
 * - Neutral (0-99): Pueden construir pero con pequeña penalidad (-3 rep)
 * - Suspicious (-200 a -99): Pueden construir con penalidad media (-8 rep)
 * - Disliked (-200): Pueden construir con penalidad alta (-15 rep)
 * - Unwelcome+ (-400+): BLOQUEADO
 */
public class PlacementRestrictionsHandler {

    private static final int REP_BLOCK_PLACEMENT  = -400;  // Bloqueado desde UNWELCOME
    private static final int REP_PENALTY_DISLIKED = -200;  // Penalidad alta desde DISLIKED
    private static final int REP_PENALTY_SUSPICIOUS = -100; // Penalidad media desde SUSPICIOUS
    private static final int REP_PENALTY_NEUTRAL  = 0;     // Pequeña penalidad desde NEUTRAL
    
    private static final long MSG_COOLDOWN_MS = 8000;

    private final Map<UUID, Long> msgCooldown = new HashMap<>();

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        int rep = data.getReputation(player.getUUID(), nearestVillage.get());

        // Heroes (800+) pueden construir sin restricciones
        if (rep >= 800) {
            return;
        }

        // Bloqueado desde UNWELCOME (-400)
        if (rep <= REP_BLOCK_PLACEMENT) {
            event.setCanceled(true);
            sendThrottled(player, level, "villagediplomacy.restrict.placement.blocked", nearestVillage.get());
            return;
        }

        // Penalidades progresivas por construcción
        int penaltyAmount = 0;
        String messageKey = null;

        if (rep < REP_PENALTY_DISLIKED) {  // DISLIKED: -200 a -399 (penalidad alta)
            penaltyAmount = -15;
            messageKey = "villagediplomacy.restrict.placement.disliked";
        } else if (rep < REP_PENALTY_SUSPICIOUS) {  // SUSPICIOUS: -100 a -199 (penalidad media)
            penaltyAmount = -8;
            messageKey = "villagediplomacy.restrict.placement.suspicious";
        } else if (rep < REP_PENALTY_NEUTRAL) {  // NEUTRAL: 0 a -99 (penalidad baja)
            penaltyAmount = -3;
            messageKey = "villagediplomacy.restrict.placement.neutral";
        }

        // Aplicar penalidad si existe
        if (penaltyAmount < 0) {
            data.addReputation(player.getUUID(), nearestVillage.get(), penaltyAmount);
            
            // Enviar mensaje solo si hay aldeanos cerca
            List<Villager> nearby = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(20));
            if (!nearby.isEmpty()) {
                sendThrottled(player, level, messageKey, nearestVillage.get());
            }
        }
    }

    private void sendThrottled(ServerPlayer player, ServerLevel level, String key, BlockPos villagePos) {
        long now = System.currentTimeMillis();
        if (msgCooldown.getOrDefault(player.getUUID(), 0L) + MSG_COOLDOWN_MS > now) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, 
            new AABB(villagePos).inflate(100));

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
