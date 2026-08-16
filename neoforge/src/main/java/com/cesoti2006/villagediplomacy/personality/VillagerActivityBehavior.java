package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber
public class VillagerActivityBehavior {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> lastActivityTime = new HashMap<>();
    private static final long ACTIVITY_INTERVAL = 45000; 

    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        if (villager.tickCount % 200 != 0) return;

        ServerLevel level = (ServerLevel) villager.level();
        UUID villagerId = villager.getUUID();

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastActivityTime.getOrDefault(villagerId, 0L);
        if (currentTime - lastTime < ACTIVITY_INTERVAL) return;

        if (RANDOM.nextDouble() < 0.25) { 
            performActivityBasedOnPersonality(villager, level);
            lastActivityTime.put(villagerId, currentTime);
        }
    }

    private static void performActivityBasedOnPersonality(Villager villager, ServerLevel level) {
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());

        if (personality == null) return;

        PersonalityTrait generosity = personality.getGenerosity();
        PersonalityTrait courage = personality.getCourage();
        PersonalityTrait workEthic = personality.getWorkEthic();

        if (generosity == PersonalityTrait.GENEROUS || generosity == PersonalityTrait.CHARITABLE) {
            if (RANDOM.nextDouble() < 0.6) {
                shareFood(villager, level);
            } else if (RANDOM.nextDouble() < 0.5) {
                giftToWell(villager, level);
            }
        }

        if (generosity == PersonalityTrait.GREEDY || generosity == PersonalityTrait.THRIFTY) {
            if (RANDOM.nextDouble() < 0.65) {
                storeValueablesInChest(villager, level);
            }
        }

        if (workEthic == PersonalityTrait.HARDWORKING || workEthic == PersonalityTrait.WORKAHOLIC) {
            if (RANDOM.nextDouble() < 0.55) {
                contemplateWork(villager, level);
            }
        }

        if (workEthic == PersonalityTrait.LAZY || workEthic == PersonalityTrait.RELAXED) {
            if (RANDOM.nextDouble() < 0.65) {
                seekBedToRest(villager, level);
            }
        }

        if (courage == PersonalityTrait.BRAVE || courage == PersonalityTrait.FEARLESS) {
            if (RANDOM.nextDouble() < 0.5) {
                patrolVillage(villager, level);
            }
        }
    }

    private static void shareFood(Villager villager, ServerLevel level) {
        var nearby = level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(15.0D));
        if (nearby.isEmpty()) return;

        if (!villager.getMainHandItem().isEmpty() &&
            (villager.getMainHandItem().is(Items.BREAD) ||
             villager.getMainHandItem().is(Items.BEETROOT) ||
             villager.getMainHandItem().is(Items.POTATO))) {

            Villager target = nearby.stream()
                .filter(v -> !v.getUUID().equals(villager.getUUID()))
                .findFirst()
                .orElse(null);

            if (target != null) {
                villager.getNavigation().moveTo(target, 1.0D);
                if (villager.distanceToSqr(target) < 4.0D) {

                    ItemStack food = villager.getMainHandItem().copy();
                    food.setCount(1);
                    villager.spawnAtLocation(food);
                    villager.getMainHandItem().shrink(1);
                }
            }
        }
    }

    private static void giftToWell(Villager villager, ServerLevel level) {
        BlockPos wellPos = findNearestWater(villager.blockPosition(), level, 20);
        if (wellPos == null) return;

        villager.getNavigation().moveTo(wellPos.getX() + 0.5, wellPos.getY(), wellPos.getZ() + 0.5, 1.0D);

        if (villager.distanceToSqr(Vec3.atCenterOf(wellPos)) < 4.0D) {
            if (RANDOM.nextDouble() < 0.3) {
                ItemStack gold = new ItemStack(Items.GOLD_NUGGET, 1);
                villager.spawnAtLocation(gold);
            }
        }
    }

    private static void storeValueablesInChest(Villager villager, ServerLevel level) {
        BlockPos chestPos = findNearestChest(villager.blockPosition(), level, 20);
        if (chestPos == null) return;

        if (!villager.getMainHandItem().isEmpty() && isValuable(villager.getMainHandItem())) {
            villager.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0D);

            if (villager.distanceToSqr(Vec3.atCenterOf(chestPos)) < 2.0D) {
                villager.getMainHandItem().shrink(1);
            }
        }
    }

    private static void contemplateWork(Villager villager, ServerLevel level) {
        BlockPos workArea = villager.blockPosition().offset(RANDOM.nextInt(8) - 4, 0, RANDOM.nextInt(8) - 4);

        if (level.getBlockState(workArea.below()).isCollisionShapeFullBlock(level, workArea.below())) {
            villager.getNavigation().moveTo(workArea.getX() + 0.5, workArea.getY(), workArea.getZ() + 0.5, 0.8D);
        }
    }

    private static void seekBedToRest(Villager villager, ServerLevel level) {
        BlockPos bedPos = findNearestBed(villager.blockPosition(), level, 30);
        if (bedPos != null) {
            villager.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.8D);
        }
    }

    private static void patrolVillage(Villager villager, ServerLevel level) {
        BlockPos patrol = villager.blockPosition().offset(RANDOM.nextInt(40) - 20, 0, RANDOM.nextInt(40) - 20);

        if (level.getBlockState(patrol.below()).isCollisionShapeFullBlock(level, patrol.below())) {
            villager.getNavigation().moveTo(patrol.getX() + 0.5, patrol.getY(), patrol.getZ() + 0.5, 1.2D);
        }
    }

    private static BlockPos findNearestWater(BlockPos center, ServerLevel level, int maxRadius) {

        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> fisherPoi = poiManager.findClosest(
            holder -> holder.is(PoiTypes.FISHERMAN),
            center,
            40,
            PoiManager.Occupancy.ANY
        );
        if (fisherPoi.isPresent()) return fisherPoi.get();

        int r = Math.min(maxRadius, 6);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(mutable).is(net.minecraft.world.level.block.Blocks.WATER))
                        return mutable.immutable();
                }
            }
        }
        return null;
    }

    private static BlockPos findNearestChest(BlockPos center, ServerLevel level, int maxRadius) {

        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> meetingPoi = poiManager.findClosest(
            holder -> holder.is(PoiTypes.MEETING),
            center,
            40,
            PoiManager.Occupancy.ANY
        );
        if (meetingPoi.isPresent()) return meetingPoi.get();

        int r = Math.min(maxRadius, 5);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(net.minecraft.world.level.block.Blocks.CHEST) 
                        || state.is(net.minecraft.world.level.block.Blocks.BARREL))
                        return mutable.immutable();
                }
            }
        }
        return null;
    }

    private static BlockPos findNearestBed(BlockPos center, ServerLevel level, int maxRadius) {
        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> bedPoi = poiManager.findClosest(
            holder -> holder.is(PoiTypes.HOME),
            center,
            Math.min(maxRadius, 40),
            PoiManager.Occupancy.ANY
        );
        return bedPoi.orElse(null);
    }

    private static boolean isValuable(ItemStack item) {
        return item.is(Items.GOLD_INGOT) || item.is(Items.GOLD_NUGGET) ||
               item.is(Items.DIAMOND) || item.is(Items.EMERALD) ||
               item.is(Items.AMETHYST_SHARD);
    }
}
