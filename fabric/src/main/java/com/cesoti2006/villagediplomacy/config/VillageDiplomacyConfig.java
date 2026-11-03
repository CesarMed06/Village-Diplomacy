package com.cesoti2006.villagediplomacy.config;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class VillageDiplomacyConfig {

    public static final VillageDiplomacyConfig INSTANCE = new VillageDiplomacyConfig();

    public static final Spec SPEC = new Spec();

    public static final class Spec {
        private boolean loaded;

        public boolean isLoaded() { return loaded; }

        void markLoaded() { this.loaded = true; }
    }

    public static final class IntValue {
        private int value;
        private final int defaultValue;

        IntValue(int defaultValue) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public int get() { return value; }

        public void set(int value) { this.value = value; }

        int defaultValue() { return defaultValue; }
    }

    public static final class BooleanValue {
        private boolean value;
        private final boolean defaultValue;

        BooleanValue(boolean defaultValue) {
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public boolean get() { return value; }

        public void set(boolean value) { this.value = value; }

        boolean defaultValue() { return defaultValue; }
    }

    public final IntValue repKillHostileBase = new IntValue(5);
    public final IntValue repKillRaid = new IntValue(15);
    public final IntValue repKillUndead = new IntValue(12);
    public final IntValue repKillCreeper = new IntValue(10);
    public final IntValue repKillSpider = new IntValue(8);
    public final IntValue repKillOther = new IntValue(5);
    public final IntValue repKillEnderman = new IntValue(8);
    public final IntValue repKillPhantom = new IntValue(8);
    public final IntValue repKillSlime = new IntValue(5);
    public final IntValue repKillGuardian = new IntValue(10);
    public final BooleanValue hostileKillRequireWitness = new BooleanValue(true);

    public final IntValue repTrade = new IntValue(5);
    public final IntValue repAttackVillager = new IntValue(-10);
    public final IntValue repKillVillager = new IntValue(-100);
    public final IntValue repKillBabyVillager = new IntValue(-200);
    public final IntValue repKillGolem = new IntValue(-150);
    public final IntValue repAttackAnimal = new IntValue(-5);
    public final IntValue repKillAnimal = new IntValue(-25);
    public final IntValue repWorkstationUse = new IntValue(-8);
    public final IntValue repDoorTrespass = new IntValue(-5);
    public final IntValue repBellSpam = new IntValue(-15);
    public final IntValue repChestOpen = new IntValue(-10);
    public final IntValue repChestLoot = new IntValue(-15);
    public final IntValue repBlockBreak = new IntValue(-10);
    public final IntValue repStructureBreak = new IntValue(-10);
    public final IntValue repBedUse = new IntValue(-20);
    public final IntValue repTrapdoorFarm = new IntValue(-10);
    public final IntValue repFenceGateOpen = new IntValue(-12);
    public final IntValue repCureVillager = new IntValue(100);

    public final IntValue fireTntPlace = new IntValue(-10);
    public final IntValue fireTntIgnite = new IntValue(-20);
    public final IntValue fireLavaPlace = new IntValue(-25);
    public final IntValue fireExplosion = new IntValue(-50);
    public final IntValue fireCropBurn = new IntValue(-4);
    public final IntValue fireBedBurn = new IntValue(-15);
    public final IntValue fireDoorBurn = new IntValue(-8);
    public final IntValue fireWorkstationBurn = new IntValue(-10);
    public final IntValue fireStart = new IntValue(-5);
    public final IntValue fireInnocent = new IntValue(-1);
    public final IntValue fireExtinguish = new IntValue(3);

    public final IntValue restrictBellThreshold = new IntValue(-100);
    public final IntValue restrictChestThreshold = new IntValue(-400);
    public final IntValue restrictBedThreshold = new IntValue(-500);
    public final IntValue restrictCraftingBlockThreshold = new IntValue(-400);
    public final IntValue restrictCraftingWarnThreshold = new IntValue(-200);
    public final IntValue restrictPlacementBlockThreshold = new IntValue(-400);
    public final IntValue restrictPlacementDislikedThreshold = new IntValue(-200);
    public final IntValue restrictPlacementSuspiciousThreshold = new IntValue(-100);
    public final IntValue restrictBedPenalty = new IntValue(-5);
    public final IntValue restrictCraftingWarnPenalty = new IntValue(-5);
    public final IntValue restrictPlacementDislikedPenalty = new IntValue(-15);
    public final IntValue restrictPlacementSuspiciousPenalty = new IntValue(-8);
    public final IntValue restrictPlacementNeutralPenalty = new IntValue(-3);

    public final IntValue repGuardAttack = new IntValue(-5);
    public final IntValue repGuardKill = new IntValue(-100);
    public final IntValue repGuardWitness = new IntValue(3);
    public final BooleanValue guardWelcomeMessage = new BooleanValue(true);
    public final BooleanValue enableGuardAttacks = new BooleanValue(true);
    public final IntValue guardAttackThreshold = new IntValue(-100);

    public final IntValue villageEnterRadius = new IntValue(80);
    public final IntValue villageExitConfirmRadius = new IntValue(120);
    public final IntValue customVillageRadius = new IntValue(48);
    public final BooleanValue enableEntryMessages = new BooleanValue(true);

    public final IntValue reputationDecayPerDay = new IntValue(0);
    public final BooleanValue tierChangeSound = new BooleanValue(true);
    public final BooleanValue tierChangeTitle = new BooleanValue(true);
    public final BooleanValue enablePersonalities = new BooleanValue(true);
    public final BooleanValue enableMourning = new BooleanValue(true);
    public final BooleanValue enableGolemPersonalities = new BooleanValue(true);
    public final BooleanValue enableVillageRelations = new BooleanValue(true);
    public final IntValue placedBlockRetentionDays = new IntValue(90);

    private VillageDiplomacyConfig() {}

    private static boolean loaded = false;

    private final Map<String, IntValue> intEntries = new LinkedHashMap<>();
    private final Map<String, BooleanValue> booleanEntries = new LinkedHashMap<>();

    {
        intEntries.put("hostile_kills.repBase", repKillHostileBase);
        intEntries.put("hostile_kills.repRaid", repKillRaid);
        intEntries.put("hostile_kills.repUndead", repKillUndead);
        intEntries.put("hostile_kills.repCreeper", repKillCreeper);
        intEntries.put("hostile_kills.repSpider", repKillSpider);
        intEntries.put("hostile_kills.repOther", repKillOther);
        intEntries.put("hostile_kills.repEnderman", repKillEnderman);
        intEntries.put("hostile_kills.repPhantom", repKillPhantom);
        intEntries.put("hostile_kills.repSlime", repKillSlime);
        intEntries.put("hostile_kills.repGuardian", repKillGuardian);
        booleanEntries.put("hostile_kills.requireWitness", hostileKillRequireWitness);

        intEntries.put("penalties.trade", repTrade);
        intEntries.put("penalties.attackVillager", repAttackVillager);
        intEntries.put("penalties.killVillager", repKillVillager);
        intEntries.put("penalties.killBabyVillager", repKillBabyVillager);
        intEntries.put("penalties.killGolem", repKillGolem);
        intEntries.put("penalties.attackAnimal", repAttackAnimal);
        intEntries.put("penalties.killAnimal", repKillAnimal);
        intEntries.put("penalties.workstationUse", repWorkstationUse);
        intEntries.put("penalties.doorTrespass", repDoorTrespass);
        intEntries.put("penalties.bellSpam", repBellSpam);
        intEntries.put("penalties.chestOpen", repChestOpen);
        intEntries.put("penalties.chestLoot", repChestLoot);
        intEntries.put("penalties.blockBreak", repBlockBreak);
        intEntries.put("penalties.structureBreak", repStructureBreak);
        intEntries.put("penalties.bedUse", repBedUse);
        intEntries.put("penalties.trapdoorFarm", repTrapdoorFarm);
        intEntries.put("penalties.fenceGateOpen", repFenceGateOpen);
        intEntries.put("bonuses.cureVillager", repCureVillager);

        intEntries.put("fire.tntPlace", fireTntPlace);
        intEntries.put("fire.tntIgnite", fireTntIgnite);
        intEntries.put("fire.lavaPlace", fireLavaPlace);
        intEntries.put("fire.explosion", fireExplosion);
        intEntries.put("fire.cropBurn", fireCropBurn);
        intEntries.put("fire.bedBurn", fireBedBurn);
        intEntries.put("fire.doorBurn", fireDoorBurn);
        intEntries.put("fire.workstationBurn", fireWorkstationBurn);
        intEntries.put("fire.fireStart", fireStart);
        intEntries.put("fire.fireInnocent", fireInnocent);
        intEntries.put("fire.extinguish", fireExtinguish);

        intEntries.put("restrictions.bellThreshold", restrictBellThreshold);
        intEntries.put("restrictions.chestThreshold", restrictChestThreshold);
        intEntries.put("restrictions.bedThreshold", restrictBedThreshold);
        intEntries.put("restrictions.craftingBlockThreshold", restrictCraftingBlockThreshold);
        intEntries.put("restrictions.craftingWarnThreshold", restrictCraftingWarnThreshold);
        intEntries.put("restrictions.placementBlockThreshold", restrictPlacementBlockThreshold);
        intEntries.put("restrictions.placementDislikedThreshold", restrictPlacementDislikedThreshold);
        intEntries.put("restrictions.placementSuspiciousThreshold", restrictPlacementSuspiciousThreshold);
        intEntries.put("restrictions.bedPenalty", restrictBedPenalty);
        intEntries.put("restrictions.craftingWarnPenalty", restrictCraftingWarnPenalty);
        intEntries.put("restrictions.placementDislikedPenalty", restrictPlacementDislikedPenalty);
        intEntries.put("restrictions.placementSuspiciousPenalty", restrictPlacementSuspiciousPenalty);
        intEntries.put("restrictions.placementNeutralPenalty", restrictPlacementNeutralPenalty);

        intEntries.put("guard_villagers.attackGuard", repGuardAttack);
        intEntries.put("guard_villagers.killGuard", repGuardKill);
        intEntries.put("guard_villagers.witnessBonus", repGuardWitness);
        booleanEntries.put("guard_villagers.welcomeMessage", guardWelcomeMessage);
        booleanEntries.put("guard_villagers.guardAttacks", enableGuardAttacks);
        intEntries.put("guard_villagers.guardAttackThreshold", guardAttackThreshold);

        intEntries.put("behavior.villageEnterRadius", villageEnterRadius);
        intEntries.put("behavior.villageExitConfirmRadius", villageExitConfirmRadius);
        intEntries.put("behavior.customVillageRadius", customVillageRadius);
        booleanEntries.put("behavior.enableEntryMessages", enableEntryMessages);

        intEntries.put("reputation_decay.decayPerDay", reputationDecayPerDay);
        booleanEntries.put("notifications.tierChangeSound", tierChangeSound);
        booleanEntries.put("notifications.tierChangeTitle", tierChangeTitle);
        booleanEntries.put("modules.enablePersonalities", enablePersonalities);
        booleanEntries.put("modules.enableMourning", enableMourning);
        booleanEntries.put("modules.enableGolemPersonalities", enableGolemPersonalities);
        booleanEntries.put("modules.enableVillageRelations", enableVillageRelations);
        intEntries.put("maintenance.placedBlockRetentionDays", placedBlockRetentionDays);
    }

    public static void loadConfig() {
        if (loaded) return;
        loaded = true;
        SPEC.markLoaded();

        Path configDir = Paths.get("config");
        Path configFile = configDir.resolve("villagediplomacy.properties");

        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not create config directory: " + e.getMessage());
        }

        if (!Files.exists(configFile)) {
            writeDefaultConfig(configFile);
            VillageDiplomacy.LOGGER.info("Created default config: " + configFile.toAbsolutePath());
            return;
        }

        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(configFile)) {
            props.load(reader);
        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not read config file, using defaults: " + e.getMessage());
            return;
        }

        for (Map.Entry<String, IntValue> entry : INSTANCE.intEntries.entrySet()) {
            String raw = props.getProperty(entry.getKey());
            if (raw == null) continue;
            try {
                entry.getValue().set(Integer.parseInt(raw.trim()));
            } catch (NumberFormatException e) {
                VillageDiplomacy.LOGGER.warn("Invalid integer for " + entry.getKey() + ": " + raw
                        + ", using default " + entry.getValue().defaultValue());
            }
        }
        for (Map.Entry<String, BooleanValue> entry : INSTANCE.booleanEntries.entrySet()) {
            String raw = props.getProperty(entry.getKey());
            if (raw == null) continue;
            entry.getValue().set(Boolean.parseBoolean(raw.trim()));
        }

        VillageDiplomacy.LOGGER.info("Village Diplomacy config loaded from " + configFile.toAbsolutePath());
    }

    private static void writeDefaultConfig(Path configFile) {
        StringBuilder out = new StringBuilder();
        out.append("# Village Diplomacy - Configuration File\n");
        out.append("# Edit these values and reload the world to apply changes.\n");
        out.append("# Lines starting with # are comments.\n\n");

        String section = "";
        for (Map.Entry<String, IntValue> entry : INSTANCE.intEntries.entrySet()) {
            String s = sectionOf(entry.getKey());
            if (!s.equals(section)) {
                section = s;
                out.append('[').append(section).append("]\n");
            }
            out.append(keyOf(entry.getKey())).append(" = ").append(entry.getValue().get()).append('\n');
        }
        section = "";
        out.append('\n');
        for (Map.Entry<String, BooleanValue> entry : INSTANCE.booleanEntries.entrySet()) {
            String s = sectionOf(entry.getKey());
            if (!s.equals(section)) {
                section = s;
                out.append('[').append(section).append("]\n");
            }
            out.append(keyOf(entry.getKey())).append(" = ").append(entry.getValue().get()).append('\n');
        }

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            writer.write(out.toString());
        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not write default config: " + e.getMessage());
        }
    }

    private static String sectionOf(String fullKey) {
        int dot = fullKey.indexOf('.');
        return dot < 0 ? "general" : fullKey.substring(0, dot);
    }

    private static String keyOf(String fullKey) {
        int dot = fullKey.indexOf('.');
        return dot < 0 ? fullKey : fullKey.substring(dot + 1);
    }
}
