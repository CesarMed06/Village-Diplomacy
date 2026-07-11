package com.cesoti2006.villagediplomacy.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;


public class VillageDiplomacyConfig {

    public static final VillageDiplomacyConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    
    public final ForgeConfigSpec.IntValue repKillHostileBase;
    public final ForgeConfigSpec.IntValue repKillRaid;
    public final ForgeConfigSpec.IntValue repKillUndead;
    public final ForgeConfigSpec.IntValue repKillCreeper;
    public final ForgeConfigSpec.IntValue repKillSpider;
    public final ForgeConfigSpec.IntValue repKillOther;
    public final ForgeConfigSpec.IntValue repKillEnderman;
    public final ForgeConfigSpec.IntValue repKillPhantom;
    public final ForgeConfigSpec.IntValue repKillSlime;
    public final ForgeConfigSpec.IntValue repKillGuardian;

    public final ForgeConfigSpec.BooleanValue hostileKillRequireWitness;

    public final ForgeConfigSpec.IntValue repAttackVillager;
    public final ForgeConfigSpec.IntValue repKillVillager;
    public final ForgeConfigSpec.IntValue repKillBabyVillager;
    public final ForgeConfigSpec.IntValue repKillGolem;
    public final ForgeConfigSpec.IntValue repAttackAnimal;
    public final ForgeConfigSpec.IntValue repKillAnimal;
    public final ForgeConfigSpec.IntValue repTrade;
    public final ForgeConfigSpec.IntValue repWorkstationUse;
    public final ForgeConfigSpec.IntValue repDoorTrespass;
    public final ForgeConfigSpec.IntValue repBellSpam;
    public final ForgeConfigSpec.IntValue repChestOpen;
    public final ForgeConfigSpec.IntValue repChestLoot;
    public final ForgeConfigSpec.IntValue repBlockBreak;
    public final ForgeConfigSpec.IntValue repStructureBreak;
    public final ForgeConfigSpec.IntValue repBedUse;
    public final ForgeConfigSpec.IntValue repTrapdoorFarm;
    public final ForgeConfigSpec.IntValue repFenceGateOpen;
    public final ForgeConfigSpec.IntValue repBuildLowRep;

    
    // Guard Villagers integration
    public final ForgeConfigSpec.IntValue repGuardAttack;
    public final ForgeConfigSpec.IntValue repGuardKill;
    public final ForgeConfigSpec.IntValue repGuardWitness;
    public final ForgeConfigSpec.BooleanValue guardWelcomeMessage;

    public final ForgeConfigSpec.IntValue villageEnterRadius;
    public final ForgeConfigSpec.IntValue villageExitConfirmRadius;

    
    public final ForgeConfigSpec.IntValue customVillageRadius;
    public final ForgeConfigSpec.BooleanValue enableEntryMessages;

    static {
        Pair<VillageDiplomacyConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(VillageDiplomacyConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private VillageDiplomacyConfig(ForgeConfigSpec.Builder builder) {

        
        builder.push("hostile_kills").comment(
            "Reputation gained by killing hostile mobs near a village.",
            "",
            "If 'requireWitness' is true, a villager must directly see the player",
            "killing the mob. If false (recommended), the reputation is granted",
            "just by being near the village (villagers hear the fight).",
            "",
            "Requires world restart after changing.");

        this.hostileKillRequireWitness = builder
            .comment("Require a villager to have line-of-sight to give reputation?",
                     "false = always give rep if near a village (recommended)",
                     "true = only give rep if a villager witnesses the kill")
            .worldRestart()
            .define("requireWitness", false);

        this.repKillHostileBase = builder
            .comment("Base reputation bonus per hostile kill (used if no specific type matches)")
            .worldRestart()
            .defineInRange("repBase", 5, 0, 500);

        this.repKillRaid = builder
            .comment("Reputation for killing illagers/raiders (Pillager, Vindicator, Evoker, Ravager, Witch)")
            .worldRestart()
            .defineInRange("repRaid", 15, 0, 500);

        this.repKillUndead = builder
            .comment("Reputation for killing undead (Zombie, Husk, Drowned, Skeleton, etc.)")
            .worldRestart()
            .defineInRange("repUndead", 10, 0, 500);

        this.repKillCreeper = builder
            .comment("Reputation for killing Creepers")
            .worldRestart()
            .defineInRange("repCreeper", 8, 0, 500);

        this.repKillSpider = builder
            .comment("Reputation for killing Spiders")
            .worldRestart()
            .defineInRange("repSpider", 5, 0, 500);

        this.repKillOther = builder
            .comment("Reputation for killing other hostile mobs (not classified above)")
            .worldRestart()
            .defineInRange("repOther", 5, 0, 500);

        this.repKillEnderman = builder
            .comment("Reputation for killing Endermen")
            .worldRestart()
            .defineInRange("repEnderman", 8, 0, 500);

        this.repKillPhantom = builder
            .comment("Reputation for killing Phantoms")
            .worldRestart()
            .defineInRange("repPhantom", 8, 0, 500);

        this.repKillSlime = builder
            .comment("Reputation for killing Slimes and Magma Cubes")
            .worldRestart()
            .defineInRange("repSlime", 5, 0, 500);

        this.repKillGuardian = builder
            .comment("Reputation for killing Guardians and Elder Guardians")
            .worldRestart()
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
            .worldRestart().defineInRange("trade", 5, 0, 500);

        this.repAttackVillager = builder
            .comment("Penalty per hit on a villager")
            .worldRestart().defineInRange("attackVillager", -10, -500, 0);

        this.repKillVillager = builder
            .comment("Penalty for killing an adult villager")
            .worldRestart().defineInRange("killVillager", -100, -500, 0);

        this.repKillBabyVillager = builder
            .comment("Penalty for killing a baby villager")
            .worldRestart().defineInRange("killBabyVillager", -200, -500, 0);

        this.repKillGolem = builder
            .comment("Penalty for killing an iron golem")
            .worldRestart().defineInRange("killGolem", -150, -500, 0);

        this.repAttackAnimal = builder
            .comment("Penalty per hit on a village animal (cow, sheep, pig, chicken)")
            .worldRestart().defineInRange("attackAnimal", -5, -500, 0);

        this.repKillAnimal = builder
            .comment("Penalty for killing a village animal")
            .worldRestart().defineInRange("killAnimal", -25, -500, 0);

        this.repWorkstationUse = builder
            .comment("Penalty for using a village workstation without permission")
            .worldRestart().defineInRange("workstationUse", -8, -500, 0);

        this.repDoorTrespass = builder
            .comment("Penalty for opening doors with negative reputation")
            .worldRestart().defineInRange("doorTrespass", -5, -500, 0);

        this.repBellSpam = builder
            .comment("Penalty for ringing the bell with low reputation")
            .worldRestart().defineInRange("bellSpam", -15, -500, 0);

        this.repChestOpen = builder
            .comment("Penalty for opening a chest with low reputation")
            .worldRestart().defineInRange("chestOpen", -10, -500, 0);

        this.repChestLoot = builder
            .comment("Penalty for taking items from a chest")
            .worldRestart().defineInRange("chestLoot", -15, -500, 0);

        this.repBlockBreak = builder
            .comment("Penalty for breaking blocks in a village")
            .worldRestart().defineInRange("blockBreak", -10, -500, 0);

        this.repStructureBreak = builder
            .comment("Penalty for breaking village structure blocks")
            .worldRestart().defineInRange("structureBreak", -10, -500, 0);

        this.repBedUse = builder
            .comment("Penalty for sleeping in a village bed")
            .worldRestart().defineInRange("bedUse", -20, -500, 0);

        this.repTrapdoorFarm = builder
            .comment("Penalty for opening farm trapdoors")
            .worldRestart().defineInRange("trapdoorFarm", -10, -500, 0);

        this.repFenceGateOpen = builder
            .comment("Penalty for opening fence gates with animals nearby")
            .worldRestart().defineInRange("fenceGateOpen", -12, -500, 0);

        this.repBuildLowRep = builder
            .comment("Penalty for placing blocks with negative reputation")
            .worldRestart().defineInRange("buildLowRep", -5, -500, 0);

        builder.pop();

        
        // Guard Villagers integration
        builder.push("guard_villagers").comment(
            "Guard Villagers integration settings.",
            "These only take effect if the Guard Villagers mod is installed.",
            "",
            "Requires world restart after changing.");

        this.repGuardAttack = builder
            .comment("Reputation penalty per hit when a player attacks a guard",
                     "Default: -5")
            .worldRestart()
            .defineInRange("attackGuard", -5, -500, 0);

        this.repGuardKill = builder
            .comment("Reputation penalty for killing a guard",
                     "Default: -100")
            .worldRestart()
            .defineInRange("killGuard", -100, -500, 0);

        this.repGuardWitness = builder
            .comment("Extra reputation bonus when a guard witnesses you killing a hostile mob",
                     "Guards 'speak well of you' to the village.",
                     "Default: 3")
            .worldRestart()
            .defineInRange("witnessBonus", 3, 0, 50);

        this.guardWelcomeMessage = builder
            .comment("Show a welcome message when joining a world with Guard Villagers installed?",
                     "Informs players that guards react to their village reputation.",
                     "Default: true")
            .worldRestart()
            .define("welcomeMessage", true);

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
            .worldRestart()
            .defineInRange("customVillageRadius", 48, 16, 128);

        this.enableEntryMessages = builder
            .comment("Show village entry/exit banners?",
                     "Set to false to disable the \"Entering Village\" message",
                     "Default: true")
            .worldRestart()
            .define("enableEntryMessages", true);

        builder.pop();
    }
}
