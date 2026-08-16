package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FabricFireDamageHandler {

    private static final long COOLDOWN_MS = 2000;
    private static final int VILLAGER_RADIUS = 24;
    private static final Map<UUID, Long> playerCooldown = new HashMap<>();
    private static final Map<BlockPos, PlacedTntInfo> trackedTnt = new HashMap<>();

    private record PlacedTntInfo(UUID playerId, long time) {}

    private static final Set<Block> VALUABLE_BLOCKS = Set.of(
        Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
        Blocks.BREWING_STAND, Blocks.CHEST, Blocks.BARREL, Blocks.ANVIL,
        Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.BELL, Blocks.LECTERN,
        Blocks.COMPOSTER, Blocks.LOOM, Blocks.STONECUTTER, Blocks.GRINDSTONE,
        Blocks.SMITHING_TABLE, Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE,
        Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON,
        Blocks.POWDER_SNOW_CAULDRON, Blocks.ENCHANTING_TABLE, Blocks.BOOKSHELF,
        Blocks.JUKEBOX, Blocks.NOTE_BLOCK
    );

    private static final Set<Block> BED_BLOCKS = Set.of(
        Blocks.RED_BED, Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED,
        Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED,
        Blocks.GRAY_BED, Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED,
        Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.BLACK_BED
    );

    private static final Set<Block> CROP_BLOCKS = Set.of(
        Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
        Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.MELON, Blocks.PUMPKIN,
        Blocks.SUGAR_CANE, Blocks.BAMBOO, Blocks.SWEET_BERRY_BUSH,
        Blocks.COCOA, Blocks.NETHER_WART, Blocks.TORCHFLOWER_CROP,
        Blocks.PITCHER_CROP
    );

    private static final Set<Block> ENTRY_BLOCKS = Set.of(
        Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
        Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR,
        Blocks.CHERRY_DOOR, Blocks.BAMBOO_DOOR, Blocks.IRON_DOOR,
        Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR,
        Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
        Blocks.MANGROVE_TRAPDOOR, Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR,
        Blocks.IRON_TRAPDOOR, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE,
        Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE,
        Blocks.DARK_OAK_FENCE_GATE, Blocks.MANGROVE_FENCE_GATE, Blocks.CHERRY_FENCE_GATE,
        Blocks.BAMBOO_FENCE_GATE
    );

    public void registerEvents() {
        UseBlockCallback.EVENT.register(this::onRightClickBlock);
        AttackBlockCallback.EVENT.register(this::onLeftClickBlock);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerLogout);
    }

    private InteractionResult onRightClickBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        boolean holdingFlintSteel = serverPlayer.getMainHandItem().getItem() == Items.FLINT_AND_STEEL
                                 || serverPlayer.getOffhandItem().getItem() == Items.FLINT_AND_STEEL;
        boolean holdingLavaBucket = serverPlayer.getMainHandItem().getItem() == Items.LAVA_BUCKET
                                 || serverPlayer.getOffhandItem().getItem() == Items.LAVA_BUCKET;
        boolean holdingFireCharge = serverPlayer.getMainHandItem().getItem() == Items.FIRE_CHARGE
                                 || serverPlayer.getOffhandItem().getItem() == Items.FIRE_CHARGE;
        boolean holdingTNT = serverPlayer.getMainHandItem().getItem() == Items.TNT
                                 || serverPlayer.getOffhandItem().getItem() == Items.TNT;

        if (!holdingFlintSteel && !holdingLavaBucket && !holdingFireCharge && !holdingTNT) return InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = serverLevel.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);
        if (villagePos.isEmpty()) return InteractionResult.PASS;

        VillageReputationData data = VillageReputationData.get(serverLevel);
        BlockPos village = villagePos.get();
        UUID playerId = serverPlayer.getUUID();

        if (clickedBlock == Blocks.TNT && holdingFlintSteel) {
            AbstractVillager witness = findNearestVillager(serverLevel, clickedPos);
            if (witness != null) {
                data.addReputation(playerId, village, -20);
                int newRep = data.getReputation(playerId, village);
                ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witness,
                    "villagediplomacy.react.tnt_ignite", 3);
                makeVillagersPanic(serverLevel, clickedPos, serverPlayer);
                ModLang.sendReputationSummary(serverPlayer, -20, newRep);
            }
            return InteractionResult.PASS;
        }

        if (holdingLavaBucket) {
            AbstractVillager witness = findNearestVillager(serverLevel, clickedPos);
            if (witness != null) {
                data.addReputation(playerId, village, -25);
                int newRep = data.getReputation(playerId, village);
                ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witness,
                    "villagediplomacy.react.lava_place", 3);
                ModLang.sendReputationSummary(serverPlayer, -25, newRep);
            }
            return InteractionResult.PASS;
        }

        if (!isPlayerObserved(serverLevel, clickedPos)) return InteractionResult.PASS;
        if (isOnCooldown(serverPlayer)) return InteractionResult.PASS;

        if (holdingTNT) {
            data.addReputation(playerId, village, -10);
            int newRep = data.getReputation(playerId, village);
            AbstractVillager witness = findNearestVillager(serverLevel, clickedPos);
            if (witness != null) {
                ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witness,
                    "villagediplomacy.react.tnt_place", 3);
            }
            ModLang.sendReputationSummary(serverPlayer, -10, newRep);
            trackedTnt.put(clickedPos.relative(hitResult.getDirection()).immutable(),
                new PlacedTntInfo(playerId, System.currentTimeMillis()));
            return InteractionResult.PASS;
        }

        if (!holdingFlintSteel && !holdingFireCharge) return InteractionResult.PASS;

        BlockPos firePos = clickedPos.relative(hitResult.getDirection());
        if (!serverLevel.getBlockState(firePos).isAir()) return InteractionResult.PASS;

        int penalty;
        String langKey;

        if (CROP_BLOCKS.contains(clickedBlock)) {
            penalty = -4;
            langKey = "villagediplomacy.react.crop_burn";
        } else if (BED_BLOCKS.contains(clickedBlock)) {
            penalty = -15;
            langKey = "villagediplomacy.react.bed_burn";
        } else if (ENTRY_BLOCKS.contains(clickedBlock)) {
            penalty = -8;
            langKey = "villagediplomacy.react.door_burn";
        } else if (VALUABLE_BLOCKS.contains(clickedBlock)) {
            penalty = -10;
            langKey = "villagediplomacy.react.workstation_burn";
        } else {
            penalty = -5;
            langKey = "villagediplomacy.react.fire_start";
        }

        data.addReputation(playerId, village, penalty);
        int newRep = data.getReputation(playerId, village);
        AbstractVillager witness = findNearestVillager(serverLevel, clickedPos);
        if (witness != null) {
            ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witness, langKey, 3);
        }
        ModLang.sendReputationSummary(serverPlayer, penalty, newRep);

        return InteractionResult.PASS;
    }

    private InteractionResult onLeftClickBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        Block clickedBlock = serverLevel.getBlockState(pos).getBlock();
        if (clickedBlock != Blocks.FIRE && clickedBlock != Blocks.SOUL_FIRE) return InteractionResult.PASS;

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(serverLevel, pos, 200);
        if (villagePos.isEmpty()) return InteractionResult.PASS;
        if (!isPlayerObserved(serverLevel, pos)) return InteractionResult.PASS;
        if (isOnCooldown(serverPlayer)) return InteractionResult.PASS;

        VillageReputationData data = VillageReputationData.get(serverLevel);
        data.addReputation(serverPlayer.getUUID(), villagePos.get(), 3);
        int newRep = data.getReputation(serverPlayer.getUUID(), villagePos.get());

        AbstractVillager witness = findNearestVillager(serverLevel, pos);
        if (witness != null) {
            ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witness,
                "villagediplomacy.react.fire_extinguish", 3);
        }
        ModLang.sendReputationSummary(serverPlayer, 3, newRep);

        return InteractionResult.PASS;
    }

    private void onPlayerLogout(net.minecraft.server.network.ServerGamePacketListenerImpl handler, net.minecraft.server.MinecraftServer server) {
        UUID id = handler.player.getUUID();
        playerCooldown.remove(id);
        trackedTnt.entrySet().removeIf(e -> e.getValue().playerId().equals(id));
    }

    private boolean isPlayerObserved(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(pos).inflate(VILLAGER_RADIUS)).isEmpty();
    }

    private AbstractVillager findNearestVillager(ServerLevel level, BlockPos pos) {
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(pos).inflate(VILLAGER_RADIUS));
        if (villagers.isEmpty()) return null;
        AbstractVillager nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (AbstractVillager v : villagers) {
            double dist = v.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (dist < nearestDist) { nearestDist = dist; nearest = v; }
        }
        return nearest;
    }

    private void onServerTick(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        trackedTnt.entrySet().removeIf(entry ->
            now - entry.getValue().time() > 300000);
    }

    private boolean isOnCooldown(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = playerCooldown.get(id);
        if (last != null && now - last < COOLDOWN_MS) return true;
        playerCooldown.put(id, now);
        return false;
    }

    private void makeVillagersPanic(ServerLevel level, BlockPos tntPos, ServerPlayer player) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
            new AABB(tntPos).inflate(24));
        Vec3 fleeFrom = player.position();
        for (Villager v : villagers) {
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 3, false, false, true));
            Vec3 villagerPos = v.position();
            Vec3 fleeDir = villagerPos.subtract(fleeFrom).normalize();
            BlockPos fleeTarget = BlockPos.containing(
                villagerPos.x + fleeDir.x * 20,
                villagerPos.y,
                villagerPos.z + fleeDir.z * 20
            );
            v.getNavigation().moveTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), 1.5);
            v.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
            v.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            v.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HURT_BY_ENTITY, player);
            v.getNavigation().moveTo(fleeTarget.getX(), fleeTarget.getY(), fleeTarget.getZ(), 1.5);
        }
    }

    public static UUID findTntPlacer(BlockPos centerPos) {
        for (Map.Entry<BlockPos, PlacedTntInfo> entry : trackedTnt.entrySet()) {
            if (entry.getKey().distSqr(centerPos) < 100) {
                return entry.getValue().playerId();
            }
        }
        return null;
    }
}
