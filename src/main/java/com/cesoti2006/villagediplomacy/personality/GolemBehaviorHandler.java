package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

/**
 * Maneja el comportamiento y personalidades de los Iron Golems
 */
@Mod.EventBusSubscriber
public class GolemBehaviorHandler {
    
    private static final long INTERACTION_COOLDOWN_MS = 30000; // 30 segundos
    
    /**
     * Cuando un golem spawns, asignarle personalidad
     */
    @SubscribeEvent
    public static void onGolemSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (event.getLevel().isClientSide()) return;
        if (golem.isPlayerCreated()) return; // Solo golems naturales
        
        ServerLevel level = (ServerLevel) event.getLevel();
        GolemPersonalityData data = GolemPersonalityData.get(level);
        
        // Buscar pueblo cercano
        Optional<BlockPos> village = VillageDetector.findNearestVillage(level, golem.blockPosition(), 100);
        String villageName = village.isPresent() ? 
            VillageDetector.getVillageId(village.get()) : 
            "Unknown Village";
        
        // Obtener o crear personalidad
        GolemPersonality personality = data.getOrCreatePersonality(
            golem.getUUID(), 
            villageName,
            new java.util.Random(golem.getUUID().hashCode())
        );
        
        // Asignar nombre personalizado
        golem.setCustomName(Component.literal(personality.getFullTitle()));
        golem.setCustomNameVisible(true);
    }
    
    /**
     * Interactuar con golem (click derecho)
     */
    @SubscribeEvent
    public static void onGolemInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof IronGolem golem)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (golem.isPlayerCreated()) return; // Solo golems naturales
        
        ServerLevel level = (ServerLevel) event.getLevel();
        GolemPersonalityData data = GolemPersonalityData.get(level);
        GolemPersonality personality = data.getPersonality(golem.getUUID());
        
        if (personality == null) return;
        
        // Si el golem está atacando al jugador, mostrar mensaje de peligro
        if (golem.getTarget() == player || 
            (golem.getPersistentAngerTarget() != null && golem.getPersistentAngerTarget().equals(player.getUUID()))) {
            String[] dangerMessages = {
                "§c[" + personality.getName() + "] ¡Te has convertido en enemigo de esta aldea!",
                "§4[" + personality.getName() + "] *levanta el puño amenazadoramente* ¡VETE AHORA!",
                "§c[" + personality.getName() + "] ¡Tus crímenes no quedarán impunes!",
                "§4[" + personality.getName() + "] *PISOTONES ENFADADOS*",
                "§c[" + personality.getName() + "] ¡La aldea exige justicia!"
            };
            player.sendSystemMessage(Component.literal(
                dangerMessages[level.getRandom().nextInt(dangerMessages.length)]));
            return;
        }
        
        // Cooldown de interacción
        if (!data.canInteract(golem.getUUID(), INTERACTION_COOLDOWN_MS)) {
            player.sendSystemMessage(Component.literal(
                "§7[" + personality.getName() + "] *ocupado patrullando*"));
            return;
        }
        
        // Diferentes respuestas según temperamento y si hay enemigos cerca
        boolean hasEnemies = level.getEntitiesOfClass(
            net.minecraft.world.entity.monster.Monster.class,
            golem.getBoundingBox().inflate(20.0D)
        ).size() > 0;
        
        if (hasEnemies) {
            player.sendSystemMessage(Component.literal(personality.getThreatDetectedMessage()));
            return;
        }
        
        // Si no hay amenazas, dar información
        String[] responses = {
            personality.getGreetingMessage(),
            personality.getPatrolMessage(),
            "§7[" + personality.getName() + "] §o" + personality.getCreationStory(),
            getLoyaltyMessage(personality),
            getTemperamentMessage(personality)
        };
        
        int choice = level.getRandom().nextInt(responses.length);
        player.sendSystemMessage(Component.literal(responses[choice]));
        
        // Animación: Mirar al jugador
        golem.getLookControl().setLookAt(player, 10.0F, 10.0F);
        
        // Partículas según temperamento
        spawnPersonalityParticles(level, golem, personality);
    }
    
    private static String getLoyaltyMessage(GolemPersonality personality) {
        return switch (personality.getLoyalty()) {
            case DEVOTED -> "§6[" + personality.getName() + "] ¡Protegeré esta aldea con mi vida!";
            case DUTIFUL -> "§e[" + personality.getName() + "] Solo cumplo mi deber.";
            case INDEPENDENT -> "§b[" + personality.getName() + "] Protejo a quienes lo merecen.";
            default -> "§7[" + personality.getName() + "] Sirvo a la aldea.";
        };
    }
    
    private static String getTemperamentMessage(GolemPersonality personality) {
        return switch (personality.getTemperament()) {
            case GENTLE -> "§a[" + personality.getName() + "] La paz es por lo que luchamos.";
            case STERN -> "§7[" + personality.getName() + "] La vigilancia es eterna.";
            case FIERCE -> "§c[" + personality.getName() + "] *golpea sus puños*";
            default -> "§7[" + personality.getName() + "] *silencioso*";
        };
    }
    
    private static void spawnPersonalityParticles(ServerLevel level, IronGolem golem, GolemPersonality personality) {
        double x = golem.getX();
        double y = golem.getY() + 1.5;
        double z = golem.getZ();
        
        switch (personality.getTemperament()) {
            case GENTLE -> level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                x, y, z, 5, 0.3, 0.3, 0.3, 0.0
            );
            case STERN -> level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                x, y, z, 3, 0.2, 0.2, 0.2, 0.0
            );
            case FIERCE -> level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLAME,
                x, y, z, 5, 0.3, 0.3, 0.3, 0.0
            );
        }
    }
}
