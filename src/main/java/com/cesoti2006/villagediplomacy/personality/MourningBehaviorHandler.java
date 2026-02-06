package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageMourningData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraftforge.event.TickEvent;

import java.util.*;

/**
 * Sistema de luto - aldeanos reaccionan cuando muere uno de los suyos
 * - Se acercan al puesto de trabajo vacío
 * - Muestran emociones según personalidad
 * - Partículas de tristeza
 * 
 * TEMPORALMENTE DESACTIVADO - Causaba congelamiento del servidor
 */
//@Mod.EventBusSubscriber
public class MourningBehaviorHandler {
    
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    
    // Rastrea qué aldeanos ya visitaron qué puestos vacíos
    private static final Map<UUID, Set<BlockPos>> visitedJobSites = new HashMap<>();
    
    //@SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        
        // Cada 5 segundos
        if (tickCounter % 100 != 0) return;
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            processMourningBehavior(level);
        }
    }
    
    private static void processMourningBehavior(ServerLevel level) {
        VillageMourningData mourningData = VillageMourningData.get(level);
        
        // Encontrar todas las aldeas con muertes recientes
        List<BlockPos> villages = VillageDetector.findAllVillages(level, BlockPos.ZERO, 10000);
        
        for (BlockPos villagePos : villages) {
            String villageId = VillageDetector.getVillageId(villagePos);
            List<VillageMourningData.MourningRecord> deaths = mourningData.getRecentDeaths(villageId);
            
            if (deaths.isEmpty()) continue;
            
            // Encontrar aldeanos cercanos a la aldea
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                new AABB(villagePos).inflate(80.0),
                v -> v.isAlive() && !v.isBaby()
            );
            
            if (nearbyVillagers.isEmpty()) continue;
            
            // Procesar cada muerte reciente
            for (VillageMourningData.MourningRecord death : deaths) {
                if (!death.hasJobSite()) continue;
                
                // Solo durante las primeras 10 minutos (período de luto intenso)
                if (death.getTimeSinceDeath() > 10 * 60 * 1000) continue;
                
                processMourningAtJobSite(level, nearbyVillagers, death);
            }
        }
    }
    
    private static void processMourningAtJobSite(ServerLevel level, List<Villager> villagers, 
                                                  VillageMourningData.MourningRecord death) {
        BlockPos jobSite = death.jobSitePos;
        
        // Encontrar aldeanos que están cerca del puesto vacío
        for (Villager villager : villagers) {
            double distance = villager.blockPosition().distSqr(jobSite);
            
            // Si están muy cerca del puesto (5 bloques)
            if (distance < 25.0) {
                showMourningReaction(level, villager, death);
            }
            // Si están a distancia media (5-15 bloques) y aún no visitaron
            else if (distance < 225.0) {
                UUID villagerId = villager.getUUID();
                Set<BlockPos> visited = visitedJobSites.computeIfAbsent(villagerId, k -> new HashSet<>());
                
                if (!visited.contains(jobSite) && RANDOM.nextInt(100) < 5) {
                    // 5% chance cada check de acercarse al puesto
                    visited.add(jobSite);
                    
                    // Obtener personalidad
                    VillagerPersonalityData personalityData = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = personalityData.getPersonality(villagerId);
                    
                    if (personality != null) {
                        // Solo aldeanos extrovertidos/sociales reaccionan visitando
                        PersonalityTrait social = personality.getSocialBehavior();
                        if (social == PersonalityTrait.OUTGOING || social == PersonalityTrait.EXTROVERTED) {
                            // Hacer que miren hacia el puesto ocasionalmente
                            villager.getLookControl().setLookAt(
                                jobSite.getX() + 0.5,
                                jobSite.getY() + 0.5,
                                jobSite.getZ() + 0.5
                            );
                        }
                    }
                }
            }
        }
    }
    
    private static void showMourningReaction(ServerLevel level, Villager villager, 
                                            VillageMourningData.MourningRecord death) {
        VillagerPersonalityData personalityData = VillagerPersonalityData.get(level);
        VillagerPersonality personality = personalityData.getPersonality(villager.getUUID());
        
        if (personality == null) return;
        
        // Intensidad de reacción según personalidad social
        PersonalityTrait social = personality.getSocialBehavior();
        int intensity = switch (social) {
            case EXTROVERTED, OUTGOING -> 3; // Muy emotivos
            case NEUTRAL_SOCIAL -> 2; // Normal
            case SHY, RESERVED -> 1; // Discretos
            default -> 1;
        };
        
        // Partículas de tristeza (nubes de lluvia)
        if (RANDOM.nextInt(100) < intensity * 15) {
            double x = villager.getX();
            double y = villager.getY() + 2.0;
            double z = villager.getZ();
            
            // Partículas azules/grises (tristeza)
            level.sendParticles(
                ParticleTypes.RAIN,
                x, y, z,
                1,
                0.3, 0.1, 0.3,
                0.0
            );
        }
        
        // Sonido ocasional de tristeza
        if (RANDOM.nextInt(200) < intensity * 10) {
            // Aldeano hace sonido triste
            villager.playSound(
                net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                0.3f,
                0.7f // Tono más bajo = más triste
            );
        }
        
        // Mirar al puesto vacío
        if (RANDOM.nextInt(100) < 30) {
            BlockPos jobSite = death.jobSitePos;
            villager.getLookControl().setLookAt(
                jobSite.getX() + 0.5,
                jobSite.getY() + 0.5,
                jobSite.getZ() + 0.5
            );
        }
    }
    
    /**
     * Limpiar datos antiguos periódicamente
     */
    public static void cleanup() {
        visitedJobSites.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
