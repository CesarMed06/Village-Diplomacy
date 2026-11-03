package com.cesoti2006.villagediplomacy.util;

import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FrogspawnBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangroveRootsBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.NetherSproutsBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RootsBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.Blocks;

public final class BlockRules {

    private BlockRules() {
    }

    public static boolean isPlant(Block block) {
        return block instanceof BushBlock
            || block instanceof LeavesBlock
            || block instanceof CactusBlock
            || block instanceof SugarCaneBlock
            || block instanceof BambooStalkBlock
            || block instanceof BambooSaplingBlock
            || block instanceof VineBlock
            || block instanceof MultifaceBlock
            || block instanceof GrowingPlantBlock
            || block instanceof RootsBlock
            || block instanceof NetherSproutsBlock
            || block instanceof HangingRootsBlock
            || block instanceof MangroveRootsBlock
            || block instanceof ChorusPlantBlock
            || block instanceof ChorusFlowerBlock
            || block instanceof WaterlilyBlock
            || block instanceof SeaPickleBlock
            || block instanceof SporeBlossomBlock
            || block instanceof PinkPetalsBlock
            || block instanceof FrogspawnBlock
            || block instanceof CocoaBlock;
    }

    public static boolean isFireHazard(Block block) {
        return block == Blocks.FIRE
            || block == Blocks.SOUL_FIRE
            || block == Blocks.TNT
            || block == Blocks.LAVA
            || block == Blocks.LAVA_CAULDRON;
    }
}
