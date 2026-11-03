package com.cesoti2006.villagediplomacy.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class VillageDiplomacyConfig {

    public static final VillageDiplomacyConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    public final ModConfigSpec.IntValue repKillHostileBase;
    public final ModConfigSpec.IntValue repKillRaid;
    public final ModConfigSpec.IntValue repKillUndead;
    public final ModConfigSpec.IntValue repKillCreeper;
    public final ModConfigSpec.IntValue repKillSpider;
    public final ModConfigSpec.IntValue repKillOther;
    public final ModConfigSpec.IntValue repKillEnderman;
    public final ModConfigSpec.IntValue repKillPhantom;
    public final ModConfigSpec.IntValue repKillSlime;
    public final ModConfigSpec.IntValue repKillGuardian;

    public final ModConfigSpec.BooleanValue hostileKillRequireWitness;

    public final ModConfigSpec.IntValue repAttackVillager;
    public final ModConfigSpec.IntValue repKillVillager;
    public final ModConfigSpec.IntValue repKillBabyVillager;
    public final ModConfigSpec.IntValue repKillGolem;
    public final ModConfigSpec.IntValue repAttackAnimal;
    public final ModConfigSpec.IntValue repKillAnimal;
    public final ModConfigSpec.IntValue repTrade;
    public final ModConfigSpec.IntValue repWorkstationUse;
    public final ModConfigSpec.IntValue repDoorTrespass;
    public final ModConfigSpec.IntValue repBellSpam;
    public final ModConfigSpec.IntValue repChestOpen;
    public final ModConfigSpec.IntValue repChestLoot;
    public final ModConfigSpec.IntValue repBlockBreak;
    public final ModConfigSpec.IntValue repStructureBreak;
    public final ModConfigSpec.IntValue repBedUse;
    public final ModConfigSpec.IntValue repTrapdoorFarm;
    public final ModConfigSpec.IntValue repFenceGateOpen;

    public final ModConfigSpec.IntValue repCureVillager;

    public final ModConfigSpec.IntValue fireTntPlace;
    public final ModConfigSpec.IntValue fireTntIgnite;
    public final ModConfigSpec.IntValue fireLavaPlace;
    public final ModConfigSpec.IntValue fireExplosion;
    public final ModConfigSpec.IntValue fireCropBurn;
    public final ModConfigSpec.IntValue fireBedBurn;
    public final ModConfigSpec.IntValue fireDoorBurn;
    public final ModConfigSpec.IntValue fireWorkstationBurn;
    public final ModConfigSpec.IntValue fireStart;
    public final ModConfigSpec.IntValue fireInnocent;
    public final ModConfigSpec.IntValue fireExtinguish;

    public final ModConfigSpec.IntValue restrictBellThreshold;
    public final ModConfigSpec.IntValue restrictChestThreshold;
    public final ModConfigSpec.IntValue restrictBedThreshold;
    public final ModConfigSpec.IntValue restrictCraftingWarnThreshold;
    public final ModConfigSpec.IntValue restrictCraftingBlockThreshold;
    public final ModConfigSpec.IntValue restrictPlacementBlockThreshold;
    public final ModConfigSpec.IntValue restrictPlacementDislikedThreshold;
    public final ModConfigSpec.IntValue restrictPlacementSuspiciousThreshold;

    public final ModConfigSpec.IntValue restrictBedPenalty;
    public final ModConfigSpec.IntValue restrictCraftingWarnPenalty;
    public final ModConfigSpec.IntValue restrictPlacementDislikedPenalty;
    public final ModConfigSpec.IntValue restrictPlacementSuspiciousPenalty;
    public final ModConfigSpec.IntValue restrictPlacementNeutralPenalty;

    public final ModConfigSpec.IntValue repGuardAttack;
    public final ModConfigSpec.IntValue repGuardKill;
    public final ModConfigSpec.IntValue repGuardWitness;
    public final ModConfigSpec.BooleanValue guardWelcomeMessage;
    public final ModConfigSpec.BooleanValue enableGuardAttacks;
    public final ModConfigSpec.IntValue guardAttackThreshold;

    public final ModConfigSpec.IntValue villageEnterRadius;
    public final ModConfigSpec.IntValue villageExitConfirmRadius;

    public final ModConfigSpec.IntValue customVillageRadius;
    public final ModConfigSpec.BooleanValue enableEntryMessages;

    public final ModConfigSpec.IntValue reputationDecayPerDay;
    public final ModConfigSpec.BooleanValue tierChangeSound;
    public final ModConfigSpec.BooleanValue tierChangeTitle;
    public final ModConfigSpec.BooleanValue enablePersonalities;
    public final ModConfigSpec.BooleanValue enableMourning;
    public final ModConfigSpec.BooleanValue enableGolemPersonalities;
    public final ModConfigSpec.BooleanValue enableVillageRelations;

    public final ModConfigSpec.IntValue placedBlockRetentionDays;

    static {
        Pair<VillageDiplomacyConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(VillageDiplomacyConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private VillageDiplomacyConfig(ModConfigSpec.Builder builder) {

        builder.push("hostile_kills").comment(
            "Reputation gained by killing hostile mobs near a village.",
            "",
            "If 'requireWitness' is true, a villager must directly see the player",
            "killing the mob. If true (recommended), the reputation is only",
            "granted when a villager witnesses the kill (line of sight).",
            "",
            "Requires world restart after changing.");

        this.hostileKillRequireWitness = builder
            .comment("Require a villager to have line-of-sight to give reputation?",
                     "false = always give rep if near a village",
                     "true = only give rep if a villager witnesses the kill (recommended)")

            .define("requireWitness", true);

        this.repKillHostileBase = builder
            .comment("Base reputation bonus per hostile kill (used if no specific type matches)")

            .defineInRange("repBase", 5, 0, 500);

        this.repKillRaid = builder
            .comment("Reputation for killing illagers/raiders (Pillager, Vindicator, Evoker, Ravager, Witch)")

            .defineInRange("repRaid", 15, 0, 500);

        this.repKillUndead = builder
            .comment("Reputation for killing undead (Zombie, Husk, Drowned, Skeleton, etc.)")

            .defineInRange("repUndead", 12, 0, 500);

        this.repKillCreeper = builder
            .comment("Reputation for killing Creepers")

            .defineInRange("repCreeper", 10, 0, 500);

        this.repKillSpider = builder
            .comment("Reputation for killing Spiders")

            .defineInRange("repSpider", 8, 0, 500);

        this.repKillOther = builder
            .comment("Reputation for killing other hostile mobs (not classified above)")

            .defineInRange("repOther", 5, 0, 500);

        this.repKillEnderman = builder
            .comment("Reputation for killing Endermen")

            .defineInRange("repEnderman", 8, 0, 500);

        this.repKillPhantom = builder
            .comment("Reputation for killing Phantoms")

            .defineInRange("repPhantom", 8, 0, 500);

        this.repKillSlime = builder
            .comment("Reputation for killing Slimes and Magma Cubes")

            .defineInRange("repSlime", 5, 0, 500);

        this.repKillGuardian = builder
            .comment("Reputation for killing Guardians and Elder Guardians")

            .defineInRange("repGuardian", 10, 0, 500);

        builder.pop();

        builder.push("penalties").comment(
            "Reputation penalties for negative actions near a village.",
            "",
            "Negative values = reputation loss.",
            "Set to 0 to disable a specific penalty.",
            "",
            "Requires world restart after changing.");

        this.repTrade = builder
            .comment("Reputation gained per trade with a villager")
            .defineInRange("trade", 5, 0, 500);

        this.repAttackVillager = builder
            .comment("Penalty per hit on a villager")
            .defineInRange("attackVillager", -10, -500, 0);

        this.repKillVillager = builder
            .comment("Penalty for killing an adult villager")
            .defineInRange("killVillager", -100, -500, 0);

        this.repKillBabyVillager = builder
            .comment("Penalty for killing a baby villager")
            .defineInRange("killBabyVillager", -200, -500, 0);

        this.repKillGolem = builder
            .comment("Penalty for killing an iron golem")
            .defineInRange("killGolem", -150, -500, 0);

        this.repAttackAnimal = builder
            .comment("Penalty per hit on a village animal (cow, sheep, pig, chicken)")
            .defineInRange("attackAnimal", -5, -500, 0);

        this.repKillAnimal = builder
            .comment("Penalty for killing a village animal")
            .defineInRange("killAnimal", -25, -500, 0);

        this.repWorkstationUse = builder
            .comment("Penalty for using a village workstation without permission")
            .defineInRange("workstationUse", -8, -500, 0);

        this.repDoorTrespass = builder
            .comment("Penalty for opening doors with negative reputation")
            .defineInRange("doorTrespass", -5, -500, 0);

        this.repBellSpam = builder
            .comment("Penalty for ringing the bell with low reputation")
            .defineInRange("bellSpam", -15, -500, 0);

        this.repChestOpen = builder
            .comment("Penalty for opening a chest with low reputation")
            .defineInRange("chestOpen", -10, -500, 0);

        this.repChestLoot = builder
            .comment("Penalty for taking items from a chest")
            .defineInRange("chestLoot", -15, -500, 0);

        this.repBlockBreak = builder
            .comment("Penalty for breaking blocks in a village")
            .defineInRange("blockBreak", -10, -500, 0);

        this.repStructureBreak = builder
            .comment("Penalty for breaking village structure blocks")
            .defineInRange("structureBreak", -10, -500, 0);

        this.repBedUse = builder
            .comment("Penalty for sleeping in a village bed")
            .defineInRange("bedUse", -20, -500, 0);

        this.repTrapdoorFarm = builder
            .comment("Penalty for opening farm trapdoors")
            .defineInRange("trapdoorFarm", -10, -500, 0);

        this.repFenceGateOpen = builder
            .comment("Penalty for opening fence gates with animals nearby")
            .defineInRange("fenceGateOpen", -12, -500, 0);

        builder.pop();

        builder.push("bonuses").comment(
            "Reputation rewards for positive actions near a village.",
            "",
            "Positive values = reputation gain.",
            "",
            "Requires world restart after changing.");

        this.repCureVillager = builder
            .comment("Reputation gained for curing a zombie villager with a golden apple",
                     "Default: 100")
            .defineInRange("cureVillager", 100, 0, 500);

        builder.pop();

        builder.push("fire").comment(
            "Reputation values for fire, TNT and lava incidents near a village.",
            "",
            "Negative values = reputation loss, positive = reputation gain.",
            "\"Lava\" covers both placing lava and the lava bucket interaction.",
            "",
            "Requires world restart after changing.");

        this.fireTntPlace = builder
            .comment("Penalty for placing a TNT block near a village")
            .defineInRange("tntPlace", -10, -500, 0);

        this.fireTntIgnite = builder
            .comment("Penalty for igniting a TNT block with flint and steel")
            .defineInRange("tntIgnite", -20, -500, 0);

        this.fireLavaPlace = builder
            .comment("Penalty for placing lava or a lava cauldron near a village")
            .defineInRange("lavaPlace", -25, -500, 0);

        this.fireExplosion = builder
            .comment("Penalty for a TNT explosion near a village")
            .defineInRange("explosion", -50, -500, 0);

        this.fireCropBurn = builder
            .comment("Penalty for setting fire to village crops")
            .defineInRange("cropBurn", -4, -500, 0);

        this.fireBedBurn = builder
            .comment("Penalty for setting fire to a bed")
            .defineInRange("bedBurn", -15, -500, 0);

        this.fireDoorBurn = builder
            .comment("Penalty for setting fire to a door, trapdoor or fence gate")
            .defineInRange("doorBurn", -8, -500, 0);

        this.fireWorkstationBurn = builder
            .comment("Penalty for setting fire to a workstation or other valuable block")
            .defineInRange("workstationBurn", -10, -500, 0);

        this.fireStart = builder
            .comment("Penalty for starting a fire on a flammable block")
            .defineInRange("fireStart", -5, -500, 0);

        this.fireInnocent = builder
            .comment("Penalty for starting a fire on a non-flammable block")
            .defineInRange("fireInnocent", -1, -500, 0);

        this.fireExtinguish = builder
            .comment("Reputation gained for putting out a village fire while being seen")
            .defineInRange("extinguish", 3, 0, 50);

        builder.pop();

        builder.push("restrictions").comment(
            "Reputation thresholds that unlock or lock village interactions, and the",
            "penalties applied when a restricted interaction is attempted.",
            "",
            "Thresholds are compared with <=, so -400 blocks the interaction at -400 or lower.",
            "",
            "Requires world restart after changing.");

        this.restrictBellThreshold = builder
            .comment("Reputation at or below which villagers stop you from ringing the bell")
            .defineInRange("bellThreshold", -100, -1000, 0);

        this.restrictChestThreshold = builder
            .comment("Reputation at or below which villagers stop you from opening chests")
            .defineInRange("chestThreshold", -400, -1000, 0);

        this.restrictBedThreshold = builder
            .comment("Reputation at or below which villagers stop you from using beds")
            .defineInRange("bedThreshold", -500, -1000, 0);

        this.restrictCraftingBlockThreshold = builder
            .comment("Reputation at or below which villagers stop you from using workstations")
            .defineInRange("craftingBlockThreshold", -400, -1000, 0);

        this.restrictCraftingWarnThreshold = builder
            .comment("Reputation at or below which using a workstation gives a warning")
            .defineInRange("craftingWarnThreshold", -200, -1000, 0);

        this.restrictPlacementBlockThreshold = builder
            .comment("Reputation at or below which villagers stop you from placing blocks")
            .defineInRange("placementBlockThreshold", -400, -1000, 0);

        this.restrictPlacementDislikedThreshold = builder
            .comment("Reputation below which placing blocks is penalised heavily")
            .defineInRange("placementDislikedThreshold", -200, -1000, 0);

        this.restrictPlacementSuspiciousThreshold = builder
            .comment("Reputation below which placing blocks is penalised lightly")
            .defineInRange("placementSuspiciousThreshold", -100, -1000, 0);

        this.restrictBedPenalty = builder
            .comment("Penalty applied when villagers catch you using a restricted bed")
            .defineInRange("bedPenalty", -5, -500, 0);

        this.restrictCraftingWarnPenalty = builder
            .comment("Penalty applied when you use a workstation while only warned")
            .defineInRange("craftingWarnPenalty", -5, -500, 0);

        this.restrictPlacementDislikedPenalty = builder
            .comment("Penalty for placing blocks with a reputation below dislikedThreshold")
            .defineInRange("placementDislikedPenalty", -15, -500, 0);

        this.restrictPlacementSuspiciousPenalty = builder
            .comment("Penalty for placing blocks with a reputation below suspiciousThreshold")
            .defineInRange("placementSuspiciousPenalty", -8, -500, 0);

        this.restrictPlacementNeutralPenalty = builder
            .comment("Penalty for placing blocks with any negative reputation")
            .defineInRange("placementNeutralPenalty", -3, -500, 0);

        builder.pop();

        builder.push("guard_villagers").comment(
            "Guard integration settings: Guard Villagers and MCA Reborn.",
            "These only take effect if one of those mods is installed.",
            "",
            "Requires world restart after changing.");

        this.repGuardAttack = builder
            .comment("Reputation penalty per hit when a player attacks a guard",
                     "Default: -5")

            .defineInRange("attackGuard", -5, -500, 0);

        this.repGuardKill = builder
            .comment("Reputation penalty for killing a guard",
                     "Default: -100")

            .defineInRange("killGuard", -100, -500, 0);

        this.repGuardWitness = builder
            .comment("Extra reputation bonus when a guard witnesses you killing a hostile mob",
                     "Guards 'speak well of you' to the village.",
                     "Default: 3")

            .defineInRange("witnessBonus", 3, 0, 50);        this.guardWelcomeMessage = builder
            .comment("Show a welcome message when joining a world with Guard Villagers installed?",
                     "Informs players that guards react to their village reputation.",
                     "Default: true")
            .define("welcomeMessage", true);

        this.enableGuardAttacks = builder
            .comment("Should guards defend the village from players with a very bad reputation?",
                     "When true, guards inside the village attack on sight once your reputation",
                     "with that village reaches guardAttackThreshold.",
                     "Default: true")
            .worldRestart()
            .define("guardAttacks", true);

        this.guardAttackThreshold = builder
            .comment("Reputation at or below which the guards attack you inside their village.",
                     "Default: -100, the same point where villagers start fleeing and iron golems",
                     "turn hostile, so the whole village reacts as one.",
                     "Default: -100")
            .worldRestart()
            .defineInRange("guardAttackThreshold", -100, -1000, 0);

        builder.pop();

        builder.push("behavior").comment(
            "General behavior settings.",
            "",
            "Requires world restart after changing.");

        this.villageEnterRadius = builder
            .comment("Radius (in blocks) to detect when a player enters a village",
                     "Default: 80. Lower = need to be closer, Higher = detect from farther")
            .worldRestart()
            .defineInRange("villageEnterRadius", 80, 16, 256);

        this.villageExitConfirmRadius = builder
            .comment("Radius (in blocks) to confirm the player has left the village",
                     "Must be larger than villageEnterRadius.",
                     "Default: 120 (80 * 1.5)")
            .worldRestart()
            .defineInRange("villageExitConfirmRadius", 120, 32, 512);

        this.customVillageRadius = builder
            .comment("Radius (in blocks) for player-claimed villages via /diplomacy claim",
                     "Default: 48")

            .defineInRange("customVillageRadius", 48, 16, 128);

        this.enableEntryMessages = builder
            .comment("Show village entry/exit banners?",
                     "Set to false to disable the \"Entering Village\" message",
                     "Default: true")

            .define("enableEntryMessages", true);

        builder.pop();

        builder.push("reputation_decay").comment(
            "Reputation Decay: reputation slowly moves toward 0 over time.",
            "Set decayPerDay to 0 to disable.",
            "Each Minecraft day (20 min), reputation moves this many points toward 0.",
            "Example: decayPerDay=1 means losing 1 point per day if positive, gaining 1 if negative.");

        this.reputationDecayPerDay = builder
            .comment("How many reputation points decay per Minecraft day",
                     "Default: 0 (disabled)")
            .defineInRange("decayPerDay", 0, 0, 100);

        builder.pop();

        builder.push("notifications").comment(
            "Control how tier change notifications appear.");

        this.tierChangeSound = builder
            .comment("Play a sound when reputation tier changes?",
                     "Default: true")
            .define("tierChangeSound", true);

        this.tierChangeTitle = builder
            .comment("Show a title on screen when reputation tier changes?",
                     "Default: true")
            .define("tierChangeTitle", true);

        builder.pop();

        builder.push("modules").comment(
            "Enable or disable entire subsystems of the mod.",
            "Useful for compatibility with other mods or modpacks.");

        this.enablePersonalities = builder
            .comment("Enable villager personality system?",
                     "Default: true")
            .define("enablePersonalities", true);

        this.enableMourning = builder
            .comment("Enable villager mourning behavior?",
                     "Default: true")
            .define("enableMourning", true);

        this.enableGolemPersonalities = builder
            .comment("Enable iron golem personalities?",
                     "Default: true")
            .define("enableGolemPersonalities", true);

        this.enableVillageRelations = builder
            .comment("Enable inter-village relationship system?",
                     "Default: true")
            .define("enableVillageRelations", true);

        builder.pop();

        builder.push("maintenance").comment(
            "Housekeeping for the data the mod stores about the world.",
            "",
            "Requires world restart after changing.");

        this.placedBlockRetentionDays = builder
            .comment("How many real days the mod remembers that a block was placed by a player.",
                     "This memory is what stops villagers from getting angry about your own builds,",
                     "so keep it generous. Older entries are purged to keep the world save small.",
                     "Default: 90")
            .worldRestart()
            .defineInRange("placedBlockRetentionDays", 90, 1, 3650);

        builder.pop();
    }
}
