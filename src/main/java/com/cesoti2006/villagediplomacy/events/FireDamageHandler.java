package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "villagediplomacy")
public class FireDamageHandler {

    private static final long COOLDOWN_MS = 2000;
    private static final int VILLAGER_RADIUS = 24;
    private static final int EXPLOSION_PLAYER_RADIUS = 50;

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

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();
        Block block = state.getBlock();

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, pos, 200);
        if (villagePos.isEmpty()) return;

        if (!isPlayerObserved(level, player, pos)) return;
        if (isOnCooldown(player)) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        if (block == Blocks.TNT) {
            data.addReputation(playerId, village, -10);
            int newRep = data.getReputation(playerId, village);

            AbstractVillager witness = findNearestVillager(level, pos);
            if (witness != null) {
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.tnt_place", 3);
            }
            ModLang.sendReputationSummary(player, -10, newRep);

            trackedTnt.put(pos.immutable(), new PlacedTntInfo(playerId, System.currentTimeMillis()));
        }

        if (block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON) {
            data.addReputation(playerId, village, -25);
            int newRep = data.getReputation(playerId, village);

            AbstractVillager witness = findNearestVillager(level, pos);
            if (witness != null) {
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.lava_place", 3);
            }
            ModLang.sendReputationSummary(player, -25, newRep);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        boolean holdingFlintSteel = player.getMainHandItem().getItem() == Items.FLINT_AND_STEEL
                                 || player.getOffhandItem().getItem() == Items.FLINT_AND_STEEL;
        boolean holdingLavaBucket = player.getMainHandItem().getItem() == Items.LAVA_BUCKET
                                 || player.getOffhandItem().getItem() == Items.LAVA_BUCKET;
        boolean holdingFireCharge = player.getMainHandItem().getItem() == Items.FIRE_CHARGE
                                 || player.getOffhandItem().getItem() == Items.FIRE_CHARGE;

        if (!holdingFlintSteel && !holdingLavaBucket && !holdingFireCharge) return;

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        Block clickedBlock = clickedState.getBlock();

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, clickedPos, 200);
        if (villagePos.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        if (clickedBlock == Blocks.TNT && holdingFlintSteel) {
            AbstractVillager witness = findNearestVillager(level, clickedPos);
            if (witness != null) {
                data.addReputation(playerId, village, -20);
                int newRep = data.getReputation(playerId, village);
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.tnt_ignite", 3);
                makeVillagersPanic(level, clickedPos, player);
                ModLang.sendReputationSummary(player, -20, newRep);
            }
            return;
        }

        if (holdingLavaBucket) {
            AbstractVillager witness = findNearestVillager(level, clickedPos);
            if (witness != null) {
                data.addReputation(playerId, village, -25);
                int newRep = data.getReputation(playerId, village);
                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.lava_place", 3);
                ModLang.sendReputationSummary(player, -25, newRep);
            }
            return;
        }

        if (!isPlayerObserved(level, player, clickedPos)) return;
        if (isOnCooldown(player)) return;

        if (!holdingFlintSteel && !holdingFireCharge) return;

        BlockPos firePos = clickedPos.relative(event.getFace());
        if (!level.getBlockState(firePos).isAir()) return; 
        if (event.getFace() == Direction.UP && !clickedState.isCollisionShapeFullBlock(level, clickedPos)) return; 

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

            int flammability = clickedBlock.getFlammability(clickedState, level, clickedPos, Direction.UP);
            boolean isFlammable = flammability > 0;

            if (!isFlammable) {

                penalty = -1;
                langKey = "villagediplomacy.react.fire_innocent";
            } else {
                penalty = -5;
                langKey = "villagediplomacy.react.fire_start";
            }
        }

        data.addReputation(playerId, village, penalty);
        int newRep = data.getReputation(playerId, village);

        AbstractVillager witness = findNearestVillager(level, clickedPos);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness, langKey, 3);
        }
        ModLang.sendReputationSummary(player, penalty, newRep);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity exploder = event.getExplosion().getExploder();
        if (!(exploder instanceof PrimedTnt)) return;

        BlockPos centerPos = BlockPos.containing(event.getExplosion().getPosition());

        Optional<BlockPos> villageOpt = VillageDetector.findNearestVillage(level, centerPos, 200);
        if (villageOpt.isEmpty()) return;
        BlockPos villagePos = villageOpt.get();

        if (level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(centerPos).inflate(32)).isEmpty()) return;

        UUID playerId = findTntPlacer(level, centerPos);
        if (playerId == null) {
            ServerPlayer nearest = findNearestPlayer(level, centerPos);
            if (nearest == null) return;
            playerId = nearest.getUUID();
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) return;

        int penalty = 50;

        VillageReputationData data = VillageReputationData.get(level);
        data.addReputation(playerId, villagePos, -penalty);
        int newRep = data.getReputation(playerId, villagePos);

        AbstractVillager witness = findNearestVillager(level, centerPos);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness,
                "villagediplomacy.react.explosion_massive", 3);
        }

        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.explosion_damage", penalty));
        ModLang.sendReputationSummary(player, -penalty, newRep);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() != Blocks.FIRE && state.getBlock() != Blocks.SOUL_FIRE) return;

        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, pos, 200);
        if (villagePos.isEmpty()) return;

        if (!isPlayerObserved(level, player, pos)) return;
        if (isOnCooldown(player)) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos village = villagePos.get();
        UUID playerId = player.getUUID();

        data.addReputation(playerId, village, 3);
        int newRep = data.getReputation(playerId, village);

        AbstractVillager witness = findNearestVillager(level, pos);
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness,
                "villagediplomacy.react.fire_extinguish", 3);
        }
        ModLang.sendReputationSummary(player, 3, newRep);
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        trackedTnt.entrySet().removeIf(entry ->
            now - entry.getValue().time() > 300000); 
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerCooldown.remove(player.getUUID());

            trackedTnt.entrySet().removeIf(entry ->
                entry.getValue().playerId().equals(player.getUUID()));
        }
    }

    private static boolean isPlayerObserved(ServerLevel level, ServerPlayer player, BlockPos pos) {
        return !level.getEntitiesOfClass(AbstractVillager.class, new AABB(pos).inflate(VILLAGER_RADIUS)).isEmpty();
    }

    private static AbstractVillager findNearestVillager(ServerLevel level, BlockPos pos) {
        List<AbstractVillager> villagers = level.getEntitiesOfClass(AbstractVillager.class,
            new AABB(pos).inflate(VILLAGER_RADIUS));
        if (villagers.isEmpty()) return null;

        AbstractVillager nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (AbstractVillager v : villagers) {
            double dist = v.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
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
