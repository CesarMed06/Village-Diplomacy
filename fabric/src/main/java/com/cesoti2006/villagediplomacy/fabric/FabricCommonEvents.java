package com.cesoti2006.villagediplomacy.fabric;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.diplomacy.GrievanceHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarDeclarationHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarManager;
import com.cesoti2006.villagediplomacy.events.FabricEventContext;
import com.cesoti2006.villagediplomacy.events.FireDamageHandler;
import com.cesoti2006.villagediplomacy.events.PlacementRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.ReputationRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.events.VillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.events.VillagerEventHandler;
import com.cesoti2006.villagediplomacy.personality.BraveBellRinger;
import com.cesoti2006.villagediplomacy.personality.GolemBehaviorHandler;
import com.cesoti2006.villagediplomacy.personality.MourningBehaviorHandler;
import com.cesoti2006.villagediplomacy.personality.PersonalityBehaviorHandler;
import com.cesoti2006.villagediplomacy.personality.VillagerActivityBehavior;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FabricCommonEvents {

    private static final VillagerEventHandler VILLAGER_EVENTS = new VillagerEventHandler();
    private static final TradeModifierHandler TRADE = new TradeModifierHandler();
    private static final PlacementRestrictionsHandler PLACEMENT = new PlacementRestrictionsHandler();
    private static final ReputationRestrictionsHandler RESTRICTIONS = new ReputationRestrictionsHandler();

    private static final Map<UUID, AbstractContainerMenu> LAST_MENU = new HashMap<>();

    private static final Map<UUID, Boolean> WAS_SLEEPING = new HashMap<>();

    private static final Map<UUID, Integer> LAST_TRADE_XP = new HashMap<>();

    private FabricCommonEvents() {}

    public static void register() {
        registerCombatAndDeath();
        registerInteraction();
        registerTick();
        registerConnection();
    }

    private static void registerCombatAndDeath() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            FabricEventContext context = FabricEventContext.ofEntity(entity, source);

            VILLAGER_EVENTS.onGuardAttack(context);
            VILLAGER_EVENTS.onGolemAttack(context);
            VILLAGER_EVENTS.onVillagerAttack(context);
            VillagerEventHandler current = VILLAGER_EVENTS;
            current.onAnimalAttack(context);

            BraveBellRinger.onVillagerAttacked(entity, source);
            VillagerBehaviorHandler.onPlayerDamageGolem(entity, source);

            return !context.isCanceled();
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            FabricEventContext context = FabricEventContext.ofEntity(entity, source);

            VILLAGER_EVENTS.onVillagerDeath(context);
            VILLAGER_EVENTS.onIronGolemDeath(context);
            VILLAGER_EVENTS.onGuardDeath(context);
            VILLAGER_EVENTS.onHostileMobKill(context);
            VILLAGER_EVENTS.onAnimalDeath(context);

            PersonalityBehaviorHandler.onVillagerDeath(entity, source);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            PersonalityBehaviorHandler.onVillagerSpawn(entity);
            GolemBehaviorHandler.onGolemSpawn(entity);
            VillagerBehaviorHandler.onVillagerSpawn(entity);

            if (entity instanceof PrimedTnt tnt) {
                FireDamageHandler.trackTnt(tnt);
            }
            if (entity instanceof Villager) {

                VILLAGER_EVENTS.onZombieVillagerCured(FabricEventContext.ofEntity(entity));
            }
        });
    }

    private static void registerInteraction() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = serverLevel.getBlockState(clickedPos);
            FabricEventContext context = FabricEventContext.ofBlock(serverPlayer, serverLevel, clickedPos, hitResult);

            VILLAGER_EVENTS.onDoorOpen(context);
            VILLAGER_EVENTS.onChestOpen(context);
            VILLAGER_EVENTS.onBellRing(context);
            VILLAGER_EVENTS.onTrapdoorOpen(context);
            VILLAGER_EVENTS.onCraftingTableUse(context);
            VILLAGER_EVENTS.onFenceGateOpen(context);

            boolean blocked = RESTRICTIONS.onBlockInteract(serverPlayer, serverLevel, clickedPos);

            ItemStack used = serverPlayer.getItemInHand(hand);
            if (used.getItem() instanceof BoneMealItem) {
                VILLAGER_EVENTS.onBonemealCrop(
                        FabricEventContext.ofBonemeal(serverPlayer, serverLevel, clickedPos, clickedState));
            }

            Block placedBlock = placedBlockOf(used);
            if (placedBlock != null) {
                BlockPos targetPos = clickedState.canBeReplaced()
                        ? clickedPos
                        : clickedPos.relative(hitResult.getDirection());
                BlockState placedState = placedBlock.defaultBlockState();

                blocked |= PLACEMENT.onBlockPlace(serverPlayer, serverLevel, targetPos, placedBlock);
                FireDamageHandler.onBlockPlaced(serverPlayer, serverLevel, targetPos, placedBlock);
                VILLAGER_EVENTS.onBlockPlace(
                        FabricEventContext.ofBlockPlace(serverPlayer, serverLevel, targetPos, placedState));
            }

            FireDamageHandler.onRightClickBlock(serverPlayer, serverLevel, clickedPos, hitResult.getDirection());

            blocked |= context.isCanceled();
            return blocked ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            FabricEventContext context = FabricEventContext.ofBlock(serverPlayer, serverLevel, pos, null);
            VILLAGER_EVENTS.onCreativeCropHarvest(context);
            VILLAGER_EVENTS.onBellHit(context);

            FireDamageHandler.onLeftClickBlock(serverPlayer, serverLevel, pos);

            return context.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true;
            if (!(level instanceof ServerLevel serverLevel)) return true;

            FabricEventContext context = FabricEventContext.ofBlockBreak(serverPlayer, serverLevel, pos, state);
            VILLAGER_EVENTS.onBlockBreak(context);
            VILLAGER_EVENTS.onBlockBreakInVillage(context);

            return !context.isCanceled();
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel)) return InteractionResult.PASS;

            FabricEventContext context = FabricEventContext.ofEntityInteract(serverPlayer, entity, hand);

            VILLAGER_EVENTS.onPlayerInteractWithZombieVillager(context);
            GolemBehaviorHandler.onGolemInteract(serverPlayer, entity, hand);

            boolean blocked = TRADE.onVillagerInteract(entity, serverPlayer, hand);
            blocked |= context.isCanceled();

            return blocked ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    private static Block placedBlockOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem blockItem) return blockItem.getBlock();
        if (stack.getItem() == Items.LAVA_BUCKET) return Blocks.LAVA;
        return null;
    }

    private static void registerTick() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GrievanceHandler.onServerTick(server);
            WarDeclarationHandler.onServerTick(server);
            WarManager.onServerTick(server);
            MourningBehaviorHandler.onServerTick(server);
            VillagerBehaviorHandler.onServerTick(server);
            FireDamageHandler.onServerTick(server);

            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (!(entity instanceof Villager villager)) continue;
                    PersonalityBehaviorHandler.onVillagerTick(villager);
                    VillagerActivityBehavior.onVillagerTick(villager);
                }
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                VILLAGER_EVENTS.onPlayerTick(FabricEventContext.ofPlayer(player));
                VillagerBehaviorHandler.onPlayerTick(player);
                pollContainers(player);
                pollTrade(player);
                pollSleep(player);
            }

            for (ServerLevel level : server.getAllLevels()) {
                VillagerBehaviorHandler.enforceNoTargetOnCreative(level);
            }
        });
    }

    private static void pollContainers(ServerPlayer player) {
        UUID id = player.getUUID();
        AbstractContainerMenu previous = LAST_MENU.get(id);
        AbstractContainerMenu current = player.containerMenu;

        if (previous == current) return;
        LAST_MENU.put(id, current);

        if (previous instanceof ChestMenu || previous instanceof AbstractFurnaceMenu) {
            FabricEventContext closing = FabricEventContext.ofContainer(player, previous);
            if (previous instanceof ChestMenu) {
                VILLAGER_EVENTS.onChestClose(closing);
            } else {
                VILLAGER_EVENTS.onFurnaceClose(closing);
            }
        }

        if (current instanceof ChestMenu) {
            VILLAGER_EVENTS.onChestGuiOpen(FabricEventContext.ofContainer(player, current));
        } else if (current instanceof AbstractFurnaceMenu) {
            VILLAGER_EVENTS.onFurnaceGuiOpen(FabricEventContext.ofContainer(player, current));
        }
    }

    private static void pollTrade(ServerPlayer player) {
        UUID id = player.getUUID();

        if (!(player.containerMenu instanceof MerchantMenu menu)) {
            LAST_TRADE_XP.remove(id);
            return;
        }

        int xp = menu.getTraderXp();
        Integer previous = LAST_TRADE_XP.put(id, xp);
        if (previous == null || xp <= previous) return;

        Villager villager = tradingVillager(player);
        if (villager == null || villager.getVillagerXp() != xp) return;

        VILLAGER_EVENTS.onPlayerTrade(
                FabricEventContext.ofEntityInteract(player, villager, InteractionHand.MAIN_HAND));
    }

    private static Villager tradingVillager(ServerPlayer player) {
        for (Villager villager : player.level().getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(16.0))) {
            if (villager.getTradingPlayer() == player) return villager;
        }
        return null;
    }

    private static void pollSleep(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean sleeping = player.isSleeping();
        Boolean was = WAS_SLEEPING.put(id, sleeping);

        if (!sleeping || Boolean.TRUE.equals(was)) return;

        BlockPos bedPos = player.getSleepingPos().orElse(player.blockPosition());
        FabricEventContext context = FabricEventContext.ofSleep(player, bedPos);
        VILLAGER_EVENTS.onBedSleep(context);

        if (context.getResultStatus() != null) {
            player.stopSleeping();
        }
    }

    private static void registerConnection() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            VILLAGER_EVENTS.onPlayerLogin(FabricEventContext.ofPlayer(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            VILLAGER_EVENTS.onPlayerLogout(FabricEventContext.ofPlayer(player));
            FireDamageHandler.onPlayerLogout(player);
            PlacementRestrictionsHandler.forget(player.getUUID());

            LAST_MENU.remove(player.getUUID());
            WAS_SLEEPING.remove(player.getUUID());
            LAST_TRADE_XP.remove(player.getUUID());
        });
    }
}
