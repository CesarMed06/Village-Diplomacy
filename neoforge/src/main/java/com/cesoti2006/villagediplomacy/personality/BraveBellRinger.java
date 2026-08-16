package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.PlayerPlacedBlocks;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class BraveBellRinger {

    private static final Map<UUID, Long> lastBellRing = new HashMap<>();
    private static final Map<BlockPos, Long> lastBellAnimation = new HashMap<>();
    private static final long BELL_COOLDOWN = 15000; 

    @SubscribeEvent
    public static void onVillagerAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Villager victim)) return;
        if (victim.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Monster)) return;

        ServerLevel level = (ServerLevel) victim.level();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);

        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
            victim.getBoundingBox().inflate(25.0D));

        for (Villager villager : nearbyVillagers) {
            if (villager.getUUID().equals(victim.getUUID())) continue;

            VillagerPersonality personality = data.getPersonality(villager.getUUID());
            if (personality == null) continue;

            PersonalityTrait courage = personality.getCourage();
            if (courage != PersonalityTrait.BRAVE && courage != PersonalityTrait.FEARLESS) continue;

            Long lastRing = lastBellRing.get(villager.getUUID());
            long currentTime = System.currentTimeMillis();
            if (lastRing != null && currentTime - lastRing < BELL_COOLDOWN) continue;

            BlockPos bellPos = findNearestBell(villager.blockPosition(), level, 30);
            if (bellPos == null) continue;

            ringBellWithAnimation(bellPos, level, villager);
            lastBellRing.put(villager.getUUID(), currentTime);

            personality.setCurrentEmotion(EmotionalState.ANGRY);

            String name = personality.getCustomName();
            List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                villager.getBoundingBox().inflate(40.0D)
            );
            for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.bell.brave_ring", name));
            }

            makeVillagersRunToBell(bellPos, level, 40);

            break; 
        }
    }

    private static BlockPos findNearestBell(BlockPos center, ServerLevel level, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double closestDistance = Double.MAX_VALUE;
        BlockPos closestBell = null;

        PlayerPlacedBlocks placedBlocks = PlayerPlacedBlocks.get(level);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutablePos);

                    if (state.is(Blocks.BELL)) {

                        if (placedBlocks.hasOwner(level, mutablePos)) {
                            continue;
                        }
                        double distance = center.distSqr(mutablePos);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestBell = mutablePos.immutable();
                        }
                    }
                }
            }
        }

        return closestBell;
    }

    private static void ringBellWithAnimation(BlockPos bellPos, ServerLevel level, Villager ringer) {
        BlockState bellState = level.getBlockState(bellPos);
        if (!(bellState.getBlock() instanceof BellBlock bellBlock)) return;

        Direction direction = Direction.fromYRot(ringer.getYRot());

        bellBlock.attemptToRing(level, bellPos, direction);

        level.playSound(null, bellPos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 1.0F);

        lastBellAnimation.put(bellPos, System.currentTimeMillis());
    }

    private static void makeVillagersRunToBell(BlockPos bellPos, ServerLevel level, int radius) {
        List<Villager> allVillagers = level.getEntitiesOfClass(Villager.class,
            new net.minecraft.world.phys.AABB(bellPos).inflate(radius));

        for (Villager villager : allVillagers) {

            villager.getNavigation().moveTo(
                bellPos.getX() + 0.5, 
                bellPos.getY(), 
                bellPos.getZ() + 0.5, 
                1.5D 
            );

            VillagerPersonalityData data = VillagerPersonalityData.get(level);
            VillagerPersonality personality = data.getPersonality(villager.getUUID());
            if (personality != null) {
                PersonalityTrait courage = personality.getCourage();
                if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
                    personality.setCurrentEmotion(EmotionalState.SCARED);
                } else {
                    personality.setCurrentEmotion(EmotionalState.ANGRY);
                }
            }
        }
    }
}
