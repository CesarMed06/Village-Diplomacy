package com.cesoti2006.villagediplomacy.diplomacy;

import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class VillageLeader {

    public static final int DECLARE_AGGRESSION = 60;

    private static final String UNKNOWN_KEY = "villagediplomacy.leader.unknown";

    private final String villageId;
    private final UUID uuid;
    private final Component name;
    private final int aggression;

    private VillageLeader(String villageId, UUID uuid, Component name, int aggression) {
        this.villageId = villageId;
        this.uuid = uuid;
        this.name = name;
        this.aggression = aggression;
    }

    public String villageId() {
        return villageId;
    }

    public UUID uuid() {
        return uuid;
    }

    public MutableComponent nameComponent(ChatFormatting color) {
        return name.copy().withStyle(color);
    }

    public int aggression() {
        return aggression;
    }

    public boolean isAggressive() {
        return aggression >= DECLARE_AGGRESSION;
    }

    public static VillageLeader resolve(ServerLevel level, String villageId) {
        VillageRelationshipData relations = VillageRelationshipData.get(level);
        BlockPos center = relations.getVillagePosition(villageId);

        if (center != null) {

            AABB area = new AABB(center.getX() - 80.0, level.getMinBuildHeight(), center.getZ() - 80.0,
                    center.getX() + 81.0, level.getMaxBuildHeight(), center.getZ() + 81.0);
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area, villager -> !villager.isBaby());

            Villager best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Villager villager : villagers) {
                int score = leadershipScore(villager);
                if (best == null || score > bestScore
                        || (score == bestScore && villager.getUUID().compareTo(best.getUUID()) < 0)) {
                    best = villager;
                    bestScore = score;
                }
            }
            if (best != null) {
                return new VillageLeader(villageId, best.getUUID(), nameOf(best), aggressionOf(level, best));
            }
        }

        return fallback(villageId);
    }

    private static int leadershipScore(Villager villager) {
        int weight = professionWeight(ModLang.professionPath(villager.getVillagerData().getProfession()));
        return weight + villager.getVillagerData().getLevel() * 6;
    }

    private static int professionWeight(String path) {
        return switch (path) {
            case "weaponsmith" -> 40;
            case "armorer" -> 35;
            case "toolsmith" -> 32;
            case "cleric" -> 30;
            case "librarian" -> 28;
            case "mason" -> 25;
            case "cartographer" -> 22;
            case "butcher" -> 20;
            case "shepherd", "farmer" -> 18;
            case "fletcher", "leatherworker" -> 16;
            case "fisherman" -> 12;
            case "nitwit" -> 4;
            default -> 10;
        };
    }

    private static int aggressionOf(ServerLevel level, Villager villager) {
        int base;
        VillagerPersonality personality = VillagerPersonalityData.get(level).getPersonality(villager.getUUID());
        if (personality != null) {
            base = temperamentBase(personality.getTemperament()) + courageMod(personality.getCourage());
        } else {
            base = Math.floorMod(villager.getUUID().hashCode(), 101);
        }
        base += professionAggression(ModLang.professionPath(villager.getVillagerData().getProfession()));
        return Math.max(0, Math.min(100, base));
    }

    private static int temperamentBase(PersonalityTrait temperament) {
        return switch (temperament) {
            case CALM -> 5;
            case PATIENT -> 15;
            case IRRITABLE -> 60;
            case HOTHEADED -> 85;
            default -> 35;
        };
    }

    private static int courageMod(PersonalityTrait courage) {
        return switch (courage) {
            case COWARD -> -10;
            case CAUTIOUS -> -5;
            case BRAVE -> 5;
            case FEARLESS -> 10;
            default -> 0;
        };
    }

    private static int professionAggression(String path) {
        return switch (path) {
            case "weaponsmith" -> 10;
            case "armorer" -> 8;
            case "butcher" -> 5;
            case "farmer", "librarian" -> -5;
            case "nitwit" -> -10;
            default -> 0;
        };
    }

    private static Component nameOf(Villager villager) {
        if (villager.hasCustomName() && villager.getCustomName() != null) {
            return Component.literal(ModLang.stripFormatting(villager.getCustomName().getString()));
        }
        return villager.getName().copy();
    }

    private static VillageLeader fallback(String villageId) {
        int aggression = Math.floorMod(villageId.hashCode(), 101);
        return new VillageLeader(villageId, null, Component.translatable(UNKNOWN_KEY), aggression);
    }
}
