package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.PlayerPlacedBlocks;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = "villagediplomacy")
public class VillagerActivityBehavior {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> lastActivityTime = new HashMap<>();
    private static final long ACTIVITY_INTERVAL = 45000;

    @SubscribeEvent
    public static void onVillagerTick(EntityTickEvent.Post event) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enablePersonalities.get()) return;
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
        ItemStack hand = villager.getMainHandItem();
        if (hand.isEmpty() || !isValuable(hand)) return;

        BlockPos chestPos = findChestWithSpaceFor(villager.blockPosition(), level, hand);
        if (chestPos == null) return;

        villager.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0D);

        if (villager.distanceToSqr(Vec3.atCenterOf(chestPos)) < 2.0D) {

            if (depositOne(villager.getMainHandItem(), level, chestPos)) {
                villager.getMainHandItem().shrink(1);
            }
        }
    }

    public static BlockPos findChestFor(Villager villager, ServerLevel level) {
        return findNearestChest(villager.blockPosition(), level, 20);
    }

    public static BlockPos findAnyContainerFor(Villager villager, ServerLevel level) {
        List<BlockPos> containers = findAnyContainers(villager.blockPosition(), level, 20);
        return containers.isEmpty() ? null : containers.get(0);
    }

    public static boolean isChestOrBarrelAt(ServerLevel level, BlockPos pos) {
        return isVillageContainerBlock(level.getBlockState(pos))
                && level.getBlockEntity(pos) instanceof Container;
    }

    public static BlockPos forceDeposit(Villager villager, ServerLevel level, BlockPos chestPos) {
        if (!isChestOrBarrelAt(level, chestPos)) return null;

        ItemStack hand = ensureValuableInHand(villager);
        if (depositOne(hand, level, chestPos, false)) {
            hand.shrink(1);
            return chestPos;
        }
        return null;
    }

    public static boolean isPlayerMadeContainer(ServerLevel level, BlockPos pos) {
        return isPlayerOwned(level, pos, PlayerPlacedBlocks.get(level));
    }

    public static int freeSpaceFor(ServerLevel level, BlockPos pos, ItemStack item) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) return 0;

        ItemStack one = item.copy();
        one.setCount(1);
        int free = 0;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                free += Math.min(one.getMaxStackSize(), container.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(current, one)) {
                free += Math.max(0, Math.min(current.getMaxStackSize(), container.getMaxStackSize())
                        - current.getCount());
            }
        }
        return free;
    }

    public static List<BlockPos> findContainersWithSpaceFor(BlockPos center, ServerLevel level, ItemStack item) {
        List<BlockPos> withSpace = new ArrayList<>();
        for (BlockPos pos : findContainers(center, level, 20, false)) {
            if (freeSpaceFor(level, pos, item) > 0) withSpace.add(pos);
        }
        return withSpace;
    }

    public static boolean isHoldingValuable(Villager villager) {
        ItemStack hand = villager.getMainHandItem();
        return !hand.isEmpty() && isValuable(hand);
    }

    public static BlockPos depositValuableInChest(Villager villager, ServerLevel level, BlockPos chestPos) {
        ItemStack hand = ensureValuableInHand(villager);

        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(chestPos);
        candidates.addAll(findUsableContainers(villager.blockPosition(), level, 20));

        for (BlockPos candidate : candidates) {
            if (depositOne(hand, level, candidate)) {
                hand.shrink(1);
                return candidate;
            }
        }
        return null;
    }

    private static ItemStack ensureValuableInHand(Villager villager) {
        ItemStack hand = villager.getMainHandItem();
        if (hand.isEmpty() || !isValuable(hand)) {
            villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(Items.GOLD_INGOT, 1));
            hand = villager.getMainHandItem();
        }
        return hand;
    }

    private static boolean depositOne(ItemStack item, ServerLevel level, BlockPos chestPos) {
        return depositOne(item, level, chestPos, true);
    }

    private static boolean depositOne(ItemStack item, ServerLevel level, BlockPos chestPos,
                                      boolean respectOwnership) {
        if (!isVillageContainerBlock(level.getBlockState(chestPos))) return false;
        if (respectOwnership && isPlayerOwned(level, chestPos, PlayerPlacedBlocks.get(level))) return false;
        if (!(level.getBlockEntity(chestPos) instanceof Container container)) return false;

        ItemStack one = item.copy();
        one.setCount(1);

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                container.setItem(slot, one);
                container.setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(current, one)
                    && current.getCount() < Math.min(current.getMaxStackSize(), container.getMaxStackSize())) {
                current.grow(1);
                container.setChanged();
                return true;
            }
        }
        return false;
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
        List<BlockPos> containers = findUsableContainers(center, level, maxRadius);
        return containers.isEmpty() ? null : containers.get(0);
    }

    private static List<BlockPos> findUsableContainers(BlockPos center, ServerLevel level, int maxRadius) {
        return findContainers(center, level, maxRadius, true);
    }

    private static List<BlockPos> findAnyContainers(BlockPos center, ServerLevel level, int maxRadius) {
        return findContainers(center, level, maxRadius, false);
    }

    private static List<BlockPos> findContainers(BlockPos center, ServerLevel level, int maxRadius,
                                                 boolean usableOnly) {
        int r = Math.min(maxRadius, 8);
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        PlayerPlacedBlocks placed = PlayerPlacedBlocks.get(level);

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -3; y <= 3; y++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(mutable)) continue;
                    BlockState state = level.getBlockState(mutable);
                    if (!isVillageContainerBlock(state)) continue;
                    if (usableOnly && isPlayerOwned(level, mutable, placed)) continue;
                    if (!(level.getBlockEntity(mutable) instanceof Container)) continue;
                    found.add(mutable.immutable());
                }
            }
        }

        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found;
    }

    private static boolean isVillageContainerBlock(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock;
    }

    private static boolean isPlayerOwned(ServerLevel level, BlockPos pos, PlayerPlacedBlocks placed) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.getPersistentData().hasUUID("VillageDiplomacyOwner")) {
            return true;
        }
        return placed.hasOwner(level, pos);
    }

    private static BlockPos findChestWithSpaceFor(BlockPos center, ServerLevel level, ItemStack item) {
        for (BlockPos pos : findUsableContainers(center, level, 20)) {
            if (canAccept(level, pos, item)) return pos;
        }
        return null;
    }

    private static boolean canAccept(ServerLevel level, BlockPos pos, ItemStack item) {
        if (!(level.getBlockEntity(pos) instanceof Container container)) return false;

        ItemStack one = item.copy();
        one.setCount(1);

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(current, one)
                    && current.getCount() < Math.min(current.getMaxStackSize(), container.getMaxStackSize())) {
                return true;
            }
        }
        return false;
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
