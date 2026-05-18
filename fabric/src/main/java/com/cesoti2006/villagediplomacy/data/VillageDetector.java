package com.cesoti2006.villagediplomacy.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import java.util.Optional;

public class VillageDetector {

    public static Optional<BlockPos> findNearestVillage(ServerLevel level, BlockPos playerPos, int radius) {
        PoiManager poiManager = level.getPoiManager();

        return poiManager.findClosest(
                holder -> holder.is(PoiTypes.MEETING),
                playerPos,
                radius,
                PoiManager.Occupancy.ANY
        );
    }

    public static String getVillageId(BlockPos villagePos) {
        return villagePos.getX() + "_" + villagePos.getZ();
    }

    public static boolean isNearVillage(ServerLevel level, BlockPos playerPos, int radius) {
        return findNearestVillage(level, playerPos, radius).isPresent();
    }

    public static List<BlockPos> findAllVillages(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> villages = new ArrayList<>();
        PoiManager poiManager = level.getPoiManager();

        poiManager.getInRange(
                poiType -> poiType.is(PoiTypes.MEETING),
                center,
                radius,
                PoiManager.Occupancy.ANY
        ).forEach(poi -> {
            BlockPos pos = poi.getPos();
            if (!villages.contains(pos)) {
                villages.add(pos);
            }
        });

        return villages;
    }

}

