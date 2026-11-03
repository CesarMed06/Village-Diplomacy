package com.cesoti2006.villagediplomacy.diplomacy;

import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageWarData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkirmishManager {

    private static final long CLASH_INTERVAL = 2400L;

    private static final int MIN_SWING = 2;
    private static final int MAX_SWING = 4;

    private static final int SCORE_FLOOR = 1;
    private static final int SCORE_CEIL = 99;

    private static final double NOTIFY_RADIUS = 128.0;

    private static final Map<String, Long> LAST_CLASH = new HashMap<>();

    private SkirmishManager() {
    }

    public static boolean tick(ServerLevel level, VillageRelationshipData relations, String pairKey,
                               VillageWarData.WarRecord war, long now) {

        int attackerKnown = war.attackerStrength > 0 ? war.attackerStrength : war.attackerBaseline;
        int defenderKnown = war.defenderStrength > 0 ? war.defenderStrength : war.defenderBaseline;
        if (attackerKnown <= 0 && defenderKnown <= 0) return false;
        int attackerForce = attackerKnown > 0 ? attackerKnown : defenderKnown;
        int defenderForce = defenderKnown > 0 ? defenderKnown : attackerKnown;

        Long last = LAST_CLASH.get(pairKey);
        if (last == null) {

            LAST_CLASH.put(pairKey, now);
            return false;
        }
        if (now - last < CLASH_INTERVAL) return false;

        int attackerRoll = attackerForce * (2 + level.getRandom().nextInt(3));
        int defenderRoll = defenderForce * (2 + level.getRandom().nextInt(3));
        boolean attackerPushes = attackerRoll >= defenderRoll;

        int swing = MIN_SWING + level.getRandom().nextInt(MAX_SWING - MIN_SWING + 1);
        int attackerScore = war.attackerScore + (attackerPushes ? swing : -swing);
        int defenderScore = war.defenderScore + (attackerPushes ? -swing : swing);

        if (attackerScore < SCORE_FLOOR || attackerScore > SCORE_CEIL) return false;
        if (defenderScore < SCORE_FLOOR || defenderScore > SCORE_CEIL) return false;

        war.attackerScore = attackerScore;
        war.defenderScore = defenderScore;
        LAST_CLASH.put(pairKey, now);

        String winner = attackerPushes ? war.attacker : war.defender;
        String loser = attackerPushes ? war.defender : war.attacker;
        MutableComponent line = Component.translatable("villagediplomacy.war.clash",
                        ModLang.villageName(relations.getVillageName(winner)),
                        ModLang.villageName(relations.getVillageName(loser)),
                        attackerScore, defenderScore)
                .withStyle(ChatFormatting.GRAY);
        announce(level, relations, war.attacker, war.defender, line);
        return true;
    }

    public static void restart(String pairKey, long now) {
        LAST_CLASH.put(pairKey, now);
    }

    public static void forget(String pairKey) {
        LAST_CLASH.remove(pairKey);
    }

    private static void announce(ServerLevel level, VillageRelationshipData relations,
                                 String villageA, String villageB, MutableComponent text) {
        List<ServerPlayer> heard = new ArrayList<>();
        for (String villageId : new String[]{villageA, villageB}) {
            BlockPos center = relations.getVillagePosition(villageId);
            if (center == null) continue;
            for (ServerPlayer player : level.players()) {
                if (heard.contains(player)) continue;
                if (player.blockPosition().distSqr(center) > NOTIFY_RADIUS * NOTIFY_RADIUS) continue;
                heard.add(player);
            }
        }
        for (ServerPlayer player : heard) {
            player.sendSystemMessage(text);
        }
    }

}
