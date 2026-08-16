package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.personality.GolemPersonality;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import java.util.*;

public class FabricGolemBehaviorHandler {

    private final Set<UUID> initializedGolems = new HashSet<>();
    private static final long INTERACTION_COOLDOWN_MS = 30000;

    public void registerEvents() {

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class,
                    new net.minecraft.world.phys.AABB(level.getSharedSpawnPos()).inflate(10000),
                    golem -> !golem.isPlayerCreated());

                for (IronGolem golem : golems) {
                    UUID golemId = golem.getUUID();
                    if (!initializedGolems.contains(golemId)) {
                        initializeGolem(golem, level);
                        initializedGolems.add(golemId);
                    }
                }
            }
        });

        UseEntityCallback.EVENT.register(this::onGolemInteract);
    }

    private void initializeGolem(IronGolem golem, ServerLevel level) {
        GolemPersonalityData data = GolemPersonalityData.get(level);

        Optional<BlockPos> village = VillageDetector.findNearestVillage(level, golem.blockPosition(), 100);
        String villageName = village.isPresent() ? VillageDetector.getVillageId(village.get()) : "Aldea Desconocida";

        GolemPersonality personality = data.getOrCreatePersonality(
                golem.getUUID(),
                villageName,
                new Random(golem.getUUID().hashCode()));

        golem.setCustomName(personality.getFullTitleComponent());
        golem.setCustomNameVisible(true);
    }

    private InteractionResult onGolemInteract(Player player, net.minecraft.world.level.Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof IronGolem golem)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (golem.isPlayerCreated()) return InteractionResult.PASS;

        if (!initializedGolems.contains(golem.getUUID())) {
            initializeGolem(golem, serverLevel);
            initializedGolems.add(golem.getUUID());
        }

        GolemPersonalityData data = GolemPersonalityData.get(serverLevel);
        GolemPersonality personality = data.getPersonality(golem.getUUID());
        if (personality == null) return InteractionResult.PASS;

        Optional<BlockPos> village = VillageDetector.findNearestVillage(serverLevel, golem.blockPosition(), 100);
        String villageRef = village.map(VillageDetector::getVillageId).orElse("?");

        if (golem.getTarget() == serverPlayer ||
                (golem.getPersistentAngerTarget() != null && golem.getPersistentAngerTarget().equals(serverPlayer.getUUID()))) {
            ModLang.sendRandomWithArgs(serverPlayer, serverLevel.getRandom(), "villagediplomacy.golem.danger", 5,
                    personality.getName());
            return InteractionResult.SUCCESS;
        }

        if (!data.canInteract(golem.getUUID(), INTERACTION_COOLDOWN_MS)) {
            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.golem.busy", personality.getName()));
            return InteractionResult.SUCCESS;
        }

        boolean hasEnemies = serverLevel.getEntitiesOfClass(
                Monster.class,
                golem.getBoundingBox().inflate(20.0D)).size() > 0;

        if (hasEnemies) {
            serverPlayer.sendSystemMessage(personality.getThreatDetectedComponent());
            return InteractionResult.SUCCESS;
        }

        Component storyLine = Component.literal("[")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(personality.getName()).withStyle(personality.getTemperament().chatColor()))
                .append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
                .append(personality.getCreationStoryComponent(villageRef).copy().withStyle(ChatFormatting.ITALIC));

        Component[] responses = {
                personality.getGreetingComponent(),
                personality.getPatrolComponent(),
                storyLine,
                personality.getLoyaltyLineComponent(),
                personality.getTemperamentLineComponent(),
        };

        int choice = serverLevel.getRandom().nextInt(responses.length);
        serverPlayer.sendSystemMessage(responses[choice]);

        golem.getLookControl().setLookAt(serverPlayer, 10.0F, 10.0F);

        spawnPersonalityParticles(serverLevel, golem, personality);

        return InteractionResult.SUCCESS;
    }

    private void spawnPersonalityParticles(ServerLevel level, IronGolem golem, GolemPersonality personality) {
        double x = golem.getX();
        double y = golem.getY() + 1.5;
        double z = golem.getZ();

        switch (personality.getTemperament()) {
            case GENTLE -> level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.0);
            case STERN, DEVOTED, DUTIFUL, INDEPENDENT -> level.sendParticles(
                    ParticleTypes.SMOKE,
                    x, y, z, 3, 0.2, 0.2, 0.2, 0.0);
            case FIERCE -> level.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.0);
        }
    }
}
