package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageMourningData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;

import java.util.*;

public class MourningBehaviorHandler {

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    private static final Map<UUID, Set<BlockPos>> visitedJobSites = new HashMap<>();

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        if (tickCounter % 100 != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            processMourningBehavior(level);
        }
    }

    private static void processMourningBehavior(ServerLevel level) {
        VillageMourningData mourningData = VillageMourningData.get(level);

        List<BlockPos> villages = VillageDetector.findAllVillages(level, BlockPos.ZERO, 10000);

        for (BlockPos villagePos : villages) {
            String villageId = VillageDetector.getVillageId(villagePos);
            List<VillageMourningData.MourningRecord> deaths = mourningData.getRecentDeaths(villageId);

            if (deaths.isEmpty()) continue;

            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                new AABB(villagePos).inflate(80.0),
                v -> v.isAlive() && !v.isBaby()
            );

            if (nearbyVillagers.isEmpty()) continue;

            for (VillageMourningData.MourningRecord death : deaths) {
                if (!death.hasJobSite()) continue;

                if (death.getTimeSinceDeath() > 10 * 60 * 1000) continue;

                processMourningAtJobSite(level, nearbyVillagers, death);
            }
        }
    }

    private static void processMourningAtJobSite(ServerLevel level, List<Villager> villagers, 
                                                  VillageMourningData.MourningRecord death) {
        BlockPos jobSite = death.jobSitePos;

        for (Villager villager : villagers) {
            double distance = villager.blockPosition().distSqr(jobSite);

            if (distance < 25.0) {
                showMourningReaction(level, villager, death);
            }

            else if (distance < 225.0) {
                UUID villagerId = villager.getUUID();
                Set<BlockPos> visited = visitedJobSites.computeIfAbsent(villagerId, k -> new HashSet<>());

                if (!visited.contains(jobSite) && RANDOM.nextInt(100) < 5) {

                    visited.add(jobSite);

                    VillagerPersonalityData personalityData = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = personalityData.getPersonality(villagerId);

                    if (personality != null) {

                        PersonalityTrait social = personality.getSocialBehavior();
                        if (social == PersonalityTrait.OUTGOING || social == PersonalityTrait.EXTROVERTED) {

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

        PersonalityTrait social = personality.getSocialBehavior();
        int intensity = switch (social) {
            case EXTROVERTED, OUTGOING -> 3; 
            case NEUTRAL_SOCIAL -> 2; 
            case SHY, RESERVED -> 1; 
            default -> 1;
        };

        if (RANDOM.nextInt(100) < intensity * 15) {
            double x = villager.getX();
            double y = villager.getY() + 2.0;
            double z = villager.getZ();

            level.sendParticles(
                ParticleTypes.RAIN,
                x, y, z,
                1,
                0.3, 0.1, 0.3,
                0.0
            );
        }

        if (RANDOM.nextInt(200) < intensity * 10) {

            villager.playSound(
                net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                0.3f,
                0.7f 
            );
        }

        if (RANDOM.nextInt(100) < 30) {
            BlockPos jobSite = death.jobSitePos;
            villager.getLookControl().setLookAt(
                jobSite.getX() + 0.5,
                jobSite.getY() + 0.5,
                jobSite.getZ() + 0.5
            );
        }
    }

    public static void cleanup() {
        visitedJobSites.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
