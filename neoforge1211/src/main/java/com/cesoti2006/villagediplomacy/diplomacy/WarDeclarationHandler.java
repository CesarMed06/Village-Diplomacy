package com.cesoti2006.villagediplomacy.diplomacy;

import com.cesoti2006.villagediplomacy.data.VillageDiplomacyData;
import com.cesoti2006.villagediplomacy.data.VillageDiplomacyData.PairState;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;

public final class WarDeclarationHandler {

    public static final int WAR_DESIRE_MIN = 70;

    public static final int TENSION_MIN = 60;

    public static final int ULTIMATUM_DAYS = 3;

    public static final int PEACE_TRUCE_DAYS = 3;

    public static final int PEACE_RELATION_PENALTY = 20;

    public static final int PEACE_RESIDUAL_TENSION = 20;

    private static final int POST_TRUCE_TENSION_CAP = 24;

    private static final int TENSION_ENTRY = 25;

    private static final int BORDER_DISTANCE = 300;

    private static final long TICKS_PER_DAY = 24000L;
    private static final String VILLAGE_PREFIX = "village:";
    private static final int TICK_INTERVAL = 200;
    private static final double NOTIFY_RADIUS = 128.0;

    private static int tickCounter = 0;

    private WarDeclarationHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!GrievanceHandler.isEnabled()) return;

        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        ServerLevel level = event.getServer().overworld();
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        for (String key : new ArrayList<>(diplomacy.getAllPairs().keySet())) {
            String[] parts = key.split(":");
            if (parts.length != 2) continue;
            evaluate(level, parts[0], parts[1], true);
        }
    }

    public static void evaluate(ServerLevel level, String villageA, String villageB, boolean notify) {
        VillageRelationshipData relations = VillageRelationshipData.get(level);
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        PairState pair = diplomacy.getOrCreatePair(villageA, villageB);

        long now = level.getServer().overworld().getGameTime();
        DiplomaticState state = pair.state;

        if (state.isWar() || state == DiplomaticState.OCCUPATION) return;

        if (state == DiplomaticState.TRUCE) {
            if (now < pair.truceUntil) return;
            state = DiplomaticState.PEACE;
            pair.state = DiplomaticState.PEACE;
            pair.truceUntil = 0L;
            pair.tension = Math.min(pair.tension, POST_TRUCE_TENSION_CAP);
            pair.warDesire = 0;
            pair.casusBelli = CasusBelli.NONE;
            pair.warGoal = WarGoal.NONE;
            diplomacy.markDirty();
            if (notify) {
                broadcast(level, relations, villageA, villageB,
                        message("villagediplomacy.war.truce_ended",
                                ModLang.villageName(relations.getVillageName(villageA)),
                                ModLang.villageName(relations.getVillageName(villageB))));
            }
        }

        Assessment assessment = assess(level, villageA, villageB);

        pair.warDesire = assessment.warDesire;
        pair.casusBelli = assessment.casus;
        pair.warGoal = assessment.goal;
        diplomacy.markDirty();

        if (state == DiplomaticState.ULTIMATUM) {
            if (!assessment.gate()) {
                DiplomaticState target = tensionState(assessment.tension);
                if (target != DiplomaticState.ULTIMATUM) {
                    pair.state = target;
                    diplomacy.markDirty();
                    if (notify && target != DiplomaticState.CRISIS) {
                        broadcast(level, relations, villageA, villageB,
                                message("villagediplomacy.war.ultimatum_withdrawn",
                                        ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                                        ModLang.villageName(relations.getVillageName(assessment.defender))));
                    }
                }
                return;
            }
            if (now >= pair.ultimatumUntil) {
                declare(level, diplomacy, relations, pair, assessment);
            }
            return;
        }

        if (assessment.gate()) {
            VillageLeader leader = VillageLeader.resolve(level, assessment.aggressor);
            if (leader.isAggressive()) {
                declare(level, diplomacy, relations, pair, assessment);
            } else {
                pair.state = DiplomaticState.ULTIMATUM;
                pair.ultimatumUntil = now + ULTIMATUM_DAYS * TICKS_PER_DAY;
                diplomacy.markDirty();
                if (notify) {
                    broadcast(level, relations, villageA, villageB,
                            message("villagediplomacy.war.ultimatum",
                                    ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                                    ModLang.villageName(relations.getVillageName(assessment.defender)),
                                    Component.translatable(assessment.goal.getTranslationKey())
                                            .withStyle(ChatFormatting.YELLOW),
                                    leader.nameComponent(ChatFormatting.GOLD)));
                }
            }
            return;
        }

        DiplomaticState target = tensionState(assessment.tension);
        if (target != state) {
            pair.state = target;
            diplomacy.markDirty();
            if (notify) notifyEscalation(level, relations, villageA, villageB, target);
        }
    }

    private static void declare(ServerLevel level, VillageDiplomacyData diplomacy,
                                VillageRelationshipData relations, PairState pair, Assessment assessment) {
        pair.state = DiplomaticState.WAR;
        pair.warAggressor = assessment.aggressor;
        pair.casusBelli = assessment.casus;
        pair.warGoal = assessment.goal;
        pair.declaredDay = VillageGrievanceData.dayOf(level);
        diplomacy.markDirty();

        broadcast(level, relations, assessment.aggressor, assessment.defender,
                message("villagediplomacy.war.declared",
                        ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                        ModLang.villageName(relations.getVillageName(assessment.defender)),
                        Component.translatable(assessment.casus.getTranslationKey())
                                .withStyle(ChatFormatting.YELLOW)));
    }

    private static void notifyEscalation(ServerLevel level, VillageRelationshipData relations,
                                         String villageA, String villageB, DiplomaticState target) {
        String key = switch (target) {
            case TENSION -> "villagediplomacy.war.tension";
            case CRISIS -> "villagediplomacy.war.crisis";
            case PEACE -> "villagediplomacy.war.calm";
            default -> null;
        };
        if (key == null) return;

        broadcast(level, relations, villageA, villageB,
                message(key,
                        ModLang.villageName(relations.getVillageName(villageA)),
                        ModLang.villageName(relations.getVillageName(villageB))));
    }

    public static final class Assessment {

        public final int tension;
        public final int relationship;
        public final int impact;
        public final CasusBelli casus;
        public final WarGoal goal;
        public final int warDesire;
        public final String aggressor;
        public final String defender;

        Assessment(int tension, int relationship, int impact, CasusBelli casus, WarGoal goal,
                   int warDesire, String aggressor, String defender) {
            this.tension = tension;
            this.relationship = relationship;
            this.impact = impact;
            this.casus = casus;
            this.goal = goal;
            this.warDesire = warDesire;
            this.aggressor = aggressor;
            this.defender = defender;
        }

        public boolean desireOk() {
            return warDesire >= WAR_DESIRE_MIN;
        }

        public boolean casusOk() {
            return casus.isPresent();
        }

        public boolean tensionOk() {
            return tension >= TENSION_MIN;
        }

        public boolean gate() {
            return desireOk() && casusOk() && tensionOk();
        }
    }

    public static Assessment assess(ServerLevel level, String villageA, String villageB) {
        VillageRelationshipData relations = VillageRelationshipData.get(level);
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        PairState pair = diplomacy.peekPair(villageA, villageB);

        int tension = pair != null ? pair.tension : 0;
        int relationship = relations.getRelationship(villageA, villageB);

        long today = VillageGrievanceData.dayOf(level);
        VillageGrievanceData grievances = VillageGrievanceData.get(level);
        int impactAB = grievances.totalImpact(villageA, VILLAGE_PREFIX + villageB, today);
        int impactBA = grievances.totalImpact(villageB, VILLAGE_PREFIX + villageA, today);

        boolean borderClose = isBorderClose(relations, villageA, villageB);
        CasusBelli casusAB = casus(impactAB, relationship, borderClose);
        CasusBelli casusBA = casus(impactBA, relationship, borderClose);
        int desireAB = desire(tension, relationship, impactAB);
        int desireBA = desire(tension, relationship, impactBA);

        boolean aLeads = desireAB >= desireBA;
        return new Assessment(
                tension,
                relationship,
                Math.max(impactAB, impactBA),
                aLeads ? casusAB : casusBA,
                (aLeads ? casusAB : casusBA).warGoal(),
                Math.max(desireAB, desireBA),
                aLeads ? villageA : villageB,
                aLeads ? villageB : villageA);
    }

    public static int desire(int tension, int relationship, int impact) {
        int tensionPart = tension * 50 / 100;
        int opinionPart = Math.max(0, -relationship) * 30 / 100;
        int grievancePart = Math.min(Math.max(0, impact), 100) * 20 / 100;
        return Math.min(100, tensionPart + opinionPart + grievancePart);
    }

    private static CasusBelli casus(int impact, int relationship, boolean borderClose) {
        if (impact > 0) return CasusBelli.INCIDENT;
        if (relationship <= -50) return borderClose ? CasusBelli.BORDER_DISPUTE : CasusBelli.OLD_GRUDGE;
        return CasusBelli.NONE;
    }

    private static DiplomaticState tensionState(int tension) {
        if (tension >= TENSION_MIN) return DiplomaticState.CRISIS;
        if (tension >= TENSION_ENTRY) return DiplomaticState.TENSION;
        return DiplomaticState.PEACE;
    }

    private static boolean isBorderClose(VillageRelationshipData relations, String villageA, String villageB) {
        BlockPos a = relations.getVillagePosition(villageA);
        BlockPos b = relations.getVillagePosition(villageB);
        if (a == null || b == null) return false;
        return a.distSqr(b) <= (double) BORDER_DISTANCE * BORDER_DISTANCE;
    }

    private static void broadcast(ServerLevel level, VillageRelationshipData relations,
                                  String villageA, String villageB, MutableComponent text) {
        BlockPos a = relations.getVillagePosition(villageA);
        BlockPos b = relations.getVillagePosition(villageB);
        for (ServerPlayer player : level.players()) {
            if (near(player, a) || near(player, b)) {
                player.sendSystemMessage(text);
            }
        }
    }

    private static boolean near(ServerPlayer player, BlockPos center) {
        if (center == null) return false;
        return player.blockPosition().distSqr(center) <= NOTIFY_RADIUS * NOTIFY_RADIUS;
    }

    private static MutableComponent message(String key, Object... args) {
        return GrievanceHandler.prefix()
                .append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }
}
