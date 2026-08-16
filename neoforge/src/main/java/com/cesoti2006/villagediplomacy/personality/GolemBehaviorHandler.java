package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.util.ModLang;
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

@Mod.EventBusSubscriber
public class GolemBehaviorHandler {

    private static final long INTERACTION_COOLDOWN_MS = 30000; 

    @SubscribeEvent
    public static void onGolemSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem))
            return;
        if (event.getLevel().isClientSide())
            return;
        if (golem.isPlayerCreated())
            return; 

        ServerLevel level = (ServerLevel) event.getLevel();
        GolemPersonalityData data = GolemPersonalityData.get(level);

        Optional<BlockPos> village = VillageDetector.findNearestVillage(level, golem.blockPosition(), 100);
        String villageName = village.isPresent() ? VillageDetector.getVillageId(village.get()) : "Aldea Desconocida";

        GolemPersonality personality = data.getOrCreatePersonality(
                golem.getUUID(),
                villageName,
                new java.util.Random(golem.getUUID().hashCode()));

        golem.setCustomName(personality.getFullTitleComponent());
        golem.setCustomNameVisible(true);
    }

    @SubscribeEvent
    public static void onGolemInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof IronGolem golem))
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (event.getHand() != InteractionHand.MAIN_HAND)
            return;
        if (golem.isPlayerCreated())
            return; 

        ServerLevel level = (ServerLevel) event.getLevel();
        GolemPersonalityData data = GolemPersonalityData.get(level);
        GolemPersonality personality = data.getPersonality(golem.getUUID());

        if (personality == null)
            return;

        Optional<BlockPos> village = VillageDetector.findNearestVillage(level, golem.blockPosition(), 100);
        String villageRef = village.map(VillageDetector::getVillageId).orElse("?");

        if (golem.getTarget() == player ||
                (golem.getPersistentAngerTarget() != null && golem.getPersistentAngerTarget().equals(player.getUUID()))) {
            ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.danger", 5,
                    personality.getName());
            return;
        }

        if (!data.canInteract(golem.getUUID(), INTERACTION_COOLDOWN_MS)) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.golem.busy", personality.getName()));
            return;
        }

        boolean hasEnemies = level.getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class,
                golem.getBoundingBox().inflate(20.0D)).size() > 0;

        if (hasEnemies) {
            player.sendSystemMessage(personality.getThreatDetectedComponent());
            return;
        }

        Component storyLine = Component.literal("[")
                .withStyle(net.minecraft.ChatFormatting.GRAY)
                .append(Component.literal(personality.getName()).withStyle(personality.getTemperament().chatColor()))
                .append(Component.literal("] ").withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(personality.getCreationStoryComponent(villageRef).copy().withStyle(net.minecraft.ChatFormatting.ITALIC));

        Component[] responses = {
                personality.getGreetingComponent(),
                personality.getPatrolComponent(),
                storyLine,
                personality.getLoyaltyLineComponent(),
                personality.getTemperamentLineComponent(),
        };

        int choice = level.getRandom().nextInt(responses.length);
        player.sendSystemMessage(responses[choice]);

        golem.getLookControl().setLookAt(player, 10.0F, 10.0F);

        spawnPersonalityParticles(level, golem, personality);
    }

    private static void spawnPersonalityParticles(ServerLevel level, IronGolem golem, GolemPersonality personality) {
        double x = golem.getX();
        double y = golem.getY() + 1.5;
        double z = golem.getZ();

        switch (personality.getTemperament()) {
            case GENTLE -> level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.0);
            case STERN, DEVOTED, DUTIFUL, INDEPENDENT -> level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    x, y, z, 3, 0.2, 0.2, 0.2, 0.0);
            case FIERCE -> level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.0);
        }
    }
}
