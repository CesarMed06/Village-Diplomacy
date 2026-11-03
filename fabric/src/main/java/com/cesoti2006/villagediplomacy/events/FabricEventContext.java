package com.cesoti2006.villagediplomacy.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FabricEventContext {

    private Entity entity;
    private Level level;
    private BlockPos pos;
    private DamageSource source;
    private BlockHitResult hitVec;
    private Direction face;
    private InteractionHand hand;
    private Entity target;
    private Player player;
    private BlockState block;
    private BlockState placedBlock;
    private AbstractContainerMenu container;
    private Container openedContainer;
    private AbstractVillager abstractVillager;
    private ItemStack itemInHand;
    private boolean attackStart = true;
    private boolean canceled;
    private Player.BedSleepingProblem bedResult;

    private FabricEventContext() {}

    public static FabricEventContext ofEntity(Entity entity, DamageSource source) {
        FabricEventContext context = new FabricEventContext();
        context.entity = entity;
        context.level = entity == null ? null : entity.level();
        context.pos = entity == null ? null : entity.blockPosition();
        context.source = source;
        return context;
    }

    public static FabricEventContext ofEntity(Entity entity) {
        return ofEntity(entity, null);
    }

    public static FabricEventContext ofPlayer(Player player) {
        FabricEventContext context = ofEntity(player, null);
        context.player = player;
        return context;
    }

    public static FabricEventContext ofBlock(Player player, Level level, BlockPos pos, BlockHitResult hitVec) {
        FabricEventContext context = new FabricEventContext();
        context.player = player;
        context.entity = player;
        context.level = level;
        context.pos = pos;
        context.hitVec = hitVec;
        context.face = hitVec == null ? null : hitVec.getDirection();
        context.block = level == null || pos == null ? null : level.getBlockState(pos);
        return context;
    }

    public static FabricEventContext ofBlockBreak(Player player, Level level, BlockPos pos, BlockState state) {
        FabricEventContext context = new FabricEventContext();
        context.player = player;
        context.entity = player;
        context.level = level;
        context.pos = pos;
        context.block = state;
        return context;
    }

    public static FabricEventContext ofEntityInteract(Player player, Entity target, InteractionHand hand) {
        FabricEventContext context = new FabricEventContext();
        context.player = player;
        context.entity = player;
        context.level = player == null ? null : player.level();
        context.target = target;
        context.hand = hand;
        context.abstractVillager = target instanceof AbstractVillager v ? v : null;
        context.itemInHand = player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        return context;
    }

    public static FabricEventContext ofSleep(Player player, BlockPos bedPos) {
        FabricEventContext context = new FabricEventContext();
        context.player = player;
        context.entity = player;
        context.level = player == null ? null : player.level();
        context.pos = bedPos;
        return context;
    }

    public static FabricEventContext ofContainer(Player player, AbstractContainerMenu menu) {
        FabricEventContext context = new FabricEventContext();
        context.player = player;
        context.entity = player;
        context.level = player == null ? null : player.level();
        context.container = menu;
        context.pos = player == null ? null : player.blockPosition();
        return context;
    }

    public static FabricEventContext ofBonemeal(Player player, Level level, BlockPos pos, BlockState state) {
        FabricEventContext context = ofBlock(player, level, pos, null);
        context.block = state;
        return context;
    }

    public static FabricEventContext ofBlockPlace(Player player, Level level, BlockPos pos, BlockState placed) {
        FabricEventContext context = ofBlock(player, level, pos, null);
        context.placedBlock = placed;
        return context;
    }

    public Entity getEntity() { return entity; }

    public Level getLevel() { return level; }

    public BlockPos getPos() { return pos; }

    public DamageSource getSource() { return source; }

    public BlockHitResult getHitVec() { return hitVec; }

    public Direction getFace() { return face; }

    public InteractionHand getHand() { return hand; }

    public Entity getTarget() { return target; }

    public Player getPlayer() { return player; }

    public BlockState getBlock() { return block; }

    public BlockState getState() { return block; }

    public BlockState getPlacedBlock() { return placedBlock; }

    public AbstractContainerMenu getContainer() { return container; }

    public Container getOpenedContainer() { return openedContainer; }

    public AbstractVillager getAbstractVillager() { return abstractVillager; }

    public ItemStack getItemInHand() { return itemInHand == null ? ItemStack.EMPTY : itemInHand; }

    public boolean isAttackStart() { return attackStart; }

    public void setCanceled(boolean canceled) { this.canceled = canceled; }

    public boolean isCanceled() { return canceled; }

    public void setResult(Player.BedSleepingProblem problem) { this.bedResult = problem; }

    public Player.BedSleepingProblem getResultStatus() { return bedResult; }

    public LivingEntity asLiving() { return entity instanceof LivingEntity living ? living : null; }
}
