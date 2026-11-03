package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData.GrievanceType;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.diplomacy.GrievanceHandler;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FireDamageHandler {

    private static final long COOLDOWN_MS = 2000;
    private static final int VILLAGER_RADIUS = 24;
    private static final int EXPLOSION_WITNESS_RADIUS = 32;
    private static final int EXPLOSION_PLAYER_RADIUS = 50;

    private static final Map<UUID, Long> playerCooldown = new HashMap<>();
    private static final Map<BlockPos, PlacedTntInfo> trackedTnt = new HashMap<>();

    private static final Map<UUID, BlockPos> liveTnt = new HashMap<>();

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

    public static void onBlockPlaced(ServerPlayer player, ServerLevel level, BlockPos pos, Block placed) {
        BlockState state = placed.defaultBlockState();
        Block block = state.getBlock();

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, pos, 200);
        if (villagePos.isEmpty()) return;

        if (block != Blocks.TNT && block != Blocks.LAVA && block != Blocks.LAVA_CAULDRON) return;

        if (!isPlayerObserved(level, player, pos)) return;
        if (isOnCooldown(player)) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        if (block == Blocks.TNT) {
            int penalty = VillageDiplomacyConfig.INSTANCE.fireTntPlace.get();
            int oldRep = data.getReputation(playerId, village);
            data.addReputation(playerId, village, penalty);
            int newRep = data.getReputation(playerId, village);
            VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

            AbstractVillager witness = findWitness(level, pos, player);
            if (witness != null) {
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.tnt_place", 3);
            }
            ModLang.sendReputationSummary(player, penalty, newRep);

            GrievanceHandler.record(level, village, GrievanceType.SABOTAGE, player);

            trackedTnt.put(pos.immutable(), new PlacedTntInfo(playerId, System.currentTimeMillis()));
        }

        if (block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON) {
            int penalty = VillageDiplomacyConfig.INSTANCE.fireLavaPlace.get();
            int oldRep = data.getReputation(playerId, village);
            data.addReputation(playerId, village, penalty);
            int newRep = data.getReputation(playerId, village);
            VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

            AbstractVillager witness = findWitness(level, pos, player);
            if (witness != null) {
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.lava_place", 3);
            }
            ModLang.sendReputationSummary(player, penalty, newRep);

            GrievanceHandler.record(level, village, GrievanceType.SABOTAGE, player);
        }
    }

    public static void onRightClickBlock(ServerPlayer player, ServerLevel level, BlockPos clickedPos, Direction face) {
        boolean holdingFlintSteel = player.getMainHandItem().getItem() == Items.FLINT_AND_STEEL
                                 || player.getOffhandItem().getItem() == Items.FLINT_AND_STEEL;
        boolean holdingLavaBucket = player.getMainHandItem().getItem() == Items.LAVA_BUCKET
                                 || player.getOffhandItem().getItem() == Items.LAVA_BUCKET;
        boolean holdingFireCharge = player.getMainHandItem().getItem() == Items.FIRE_CHARGE
                                 || player.getOffhandItem().getItem() == Items.FIRE_CHARGE;

        if (!holdingFlintSteel && !holdingLavaBucket && !holdingFireCharge) return;

        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, clickedPos, 200);
        if (villagePos.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        if (clickedBlock == Blocks.TNT && holdingFlintSteel) {
            AbstractVillager witness = findWitness(level, clickedPos, player);
            if (witness != null) {
                int penalty = VillageDiplomacyConfig.INSTANCE.fireTntIgnite.get();
                int oldRep = data.getReputation(playerId, village);
                data.addReputation(playerId, village, penalty);
                int newRep = data.getReputation(playerId, village);
                VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.tnt_ignite", 3);
                makeVillagersPanic(level, clickedPos, player);
                ModLang.sendReputationSummary(player, penalty, newRep);
                GrievanceHandler.record(level, village, GrievanceType.SABOTAGE, player);
            }
            return;
        }

        if (holdingLavaBucket) {
            AbstractVillager witness = findWitness(level, clickedPos, player);
            if (witness != null) {
                int penalty = VillageDiplomacyConfig.INSTANCE.fireLavaPlace.get();
                int oldRep = data.getReputation(playerId, village);
                data.addReputation(playerId, village, penalty);
                int newRep = data.getReputation(playerId, village);
                VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.lava_place", 3);
                ModLang.sendReputationSummary(player, penalty, newRep);
                GrievanceHandler.record(level, village, GrievanceType.SABOTAGE, player);
            }
            return;
        }

        if (!isPlayerObserved(level, player, clickedPos)) return;
        if (isOnCooldown(player)) return;

        if (!holdingFlintSteel && !holdingFireCharge) return;

        BlockPos firePos = clickedPos.relative(face);
        if (!level.getBlockState(firePos).isAir()) return;
        if (face == Direction.UP && !clickedState.isCollisionShapeFullBlock(level, clickedPos)) return;

        int penalty;
        String langKey;

        var cfg = VillageDiplomacyConfig.INSTANCE;

        if (CROP_BLOCKS.contains(clickedBlock)) {
            penalty = cfg.fireCropBurn.get();
            langKey = "villagediplomacy.react.crop_burn";
        } else if (BED_BLOCKS.contains(clickedBlock)) {
            penalty = cfg.fireBedBurn.get();
            langKey = "villagediplomacy.react.bed_burn";
        } else if (ENTRY_BLOCKS.contains(clickedBlock)) {
            penalty = cfg.fireDoorBurn.get();
            langKey = "villagediplomacy.react.door_burn";
        } else if (VALUABLE_BLOCKS.contains(clickedBlock)) {
            penalty = cfg.fireWorkstationBurn.get();
            langKey = "villagediplomacy.react.workstation_burn";
        } else {
            boolean isFlammable = isFlammable(clickedState);

            if (!isFlammable) {
                penalty = cfg.fireInnocent.get();
                langKey = "villagediplomacy.react.fire_innocent";
            } else {
                penalty = cfg.fireStart.get();
                langKey = "villagediplomacy.react.fire_start";
            }
        }

        int oldRep = data.getReputation(playerId, village);
        data.addReputation(playerId, village, penalty);
        int newRep = data.getReputation(playerId, village);
        VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

        AbstractVillager witness = findWitness(level, clickedPos, player);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness, langKey, 3);
        }
        ModLang.sendReputationSummary(player, penalty, newRep);
    }

    public static void onLeftClickBlock(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() != Blocks.FIRE && state.getBlock() != Blocks.SOUL_FIRE) return;

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, pos, 200);
        if (villagePos.isEmpty()) return;

        if (!isPlayerObserved(level, player, pos)) return;
        if (isOnCooldown(player)) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        int bonus = VillageDiplomacyConfig.INSTANCE.fireExtinguish.get();
        int oldRep = data.getReputation(playerId, village);
        data.addReputation(playerId, village, bonus);
        int newRep = data.getReputation(playerId, village);
        VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

        AbstractVillager witness = findWitness(level, pos, player);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness,
                "villagediplomacy.react.fire_extinguish", 3);
        }
        ModLang.sendReputationSummary(player, bonus, newRep);
    }

    public static void trackTnt(PrimedTnt tnt) {
        liveTnt.put(tnt.getUUID(), tnt.blockPosition().immutable());
    }

    public static void pollExplosions(net.minecraft.server.MinecraftServer server) {
        if (liveTnt.isEmpty()) return;

        for (ServerLevel level : server.getAllLevels()) {
            java.util.Iterator<Map.Entry<UUID, BlockPos>> it = liveTnt.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, BlockPos> entry = it.next();
                Entity tnt = level.getEntity(entry.getKey());
                if (tnt instanceof PrimedTnt primed && primed.isAlive()) {
                    entry.setValue(primed.blockPosition().immutable());
                    continue;
                }
                it.remove();
                if (tnt == null && !level.getChunkSource().hasChunk(
                        entry.getValue().getX() >> 4, entry.getValue().getZ() >> 4)) {

                    continue;
                }
                onExplosionAt(level, entry.getValue());
            }
        }
    }

    private static void onExplosionAt(ServerLevel level, BlockPos centerPos) {
        Optional<BlockPos> villageOpt = VillageDetector.findNearestVillage(level, centerPos, 200);
        if (villageOpt.isEmpty()) return;
        BlockPos villagePos = villageOpt.get();

        AbstractVillager witness = findExplosionWitness(level, centerPos);
        if (witness == null) return;

        UUID playerId = findTntPlacer(level, centerPos);
        if (playerId == null) {
            ServerPlayer nearest = findNearestPlayer(level, centerPos);
            if (nearest == null) return;
            playerId = nearest.getUUID();
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) return;

        int penalty = Math.abs(VillageDiplomacyConfig.INSTANCE.fireExplosion.get());

        VillageReputationData data = VillageReputationData.get(level);
        int oldRep = data.getReputation(playerId, villagePos);
        data.addReputation(playerId, villagePos, -penalty);
        int newRep = data.getReputation(playerId, villagePos);
        VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

        ModLang.sendDialogRandom(player, level.getRandom(), witness,
            "villagediplomacy.react.explosion_massive", 3);

        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.explosion_damage", penalty));
        ModLang.sendReputationSummary(player, -penalty, newRep);

        GrievanceHandler.record(level, villagePos, GrievanceType.SABOTAGE, player);
    }

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        long now = System.currentTimeMillis();
        trackedTnt.entrySet().removeIf(entry ->
            now - entry.getValue().time() > 300000);

        pollExplosions(server);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        playerCooldown.remove(player.getUUID());

        trackedTnt.entrySet().removeIf(entry ->
            entry.getValue().playerId().equals(player.getUUID()));
    }

    private static boolean isPlayerObserved(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return findWitness(level, pos, player) != null;
    }

    private static boolean isFlammable(BlockState state) {
        if (state.isAir()) return false;
        return state.is(BlockTags.PLANKS)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.ALL_SIGNS)
                || state.is(BlockTags.BANNERS)
                || state.is(BlockTags.BAMBOO_BLOCKS)
                || state.is(BlockTags.CANDLES)
                || state.is(BlockTags.BEEHIVES)
                || state.is(BlockTags.FIRE)
                || state.is(Blocks.TNT)
                || state.is(Blocks.HAY_BLOCK)
                || state.is(Blocks.BOOKSHELF)
                || state.is(Blocks.CHISELED_BOOKSHELF)
                || state.is(Blocks.LADDER)
                || state.is(Blocks.SCAFFOLDING)
                || state.is(Blocks.COAL_BLOCK)
                || state.is(Blocks.DRIED_KELP_BLOCK)
                || state.is(Blocks.TARGET)
                || state.is(Blocks.NOTE_BLOCK)
                || state.is(Blocks.JUKEBOX)
                || state.is(Blocks.COMPOSTER)
                || state.is(Blocks.BEE_NEST)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.VINE)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.LILY_PAD);
    }

    private static AbstractVillager findWitness(ServerLevel level, BlockPos pos, ServerPlayer player) {
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(pos).inflate(VILLAGER_RADIUS));

        AbstractVillager nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (AbstractVillager v : villagers) {
            if (!VillagerEventHandler.hasLineOfSight(v, player, level)) continue;
            double dist = v.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = v;
            }
        }
        return nearest;
    }

    private static AbstractVillager findExplosionWitness(ServerLevel level, BlockPos centerPos) {
        Vec3 center = Vec3.atCenterOf(centerPos);
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(centerPos).inflate(EXPLOSION_WITNESS_RADIUS));

        AbstractVillager nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (AbstractVillager v : villagers) {
            if (!VillagerEventHandler.hasLineOfSightTo(v, center, level)) continue;
            double dist = v.distanceToSqr(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = v;
            }
        }
        return nearest;
    }

    private static ServerPlayer findNearestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            double dist = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            if (dist < nearestDist && dist < EXPLOSION_PLAYER_RADIUS * EXPLOSION_PLAYER_RADIUS) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private static UUID findTntPlacer(ServerLevel level, BlockPos centerPos) {
        for (Map.Entry<BlockPos, PlacedTntInfo> entry : trackedTnt.entrySet()) {
            if (entry.getKey().distSqr(centerPos) < 100) {
                return entry.getValue().playerId();
            }
        }
        return null;
    }

    private static boolean isOnCooldown(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        Long last = playerCooldown.get(id);
        if (last != null && now - last < COOLDOWN_MS) return true;
        playerCooldown.put(id, now);
        return false;
    }

    private static void makeVillagersPanic(ServerLevel level, BlockPos tntPos, ServerPlayer player) {
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
}
