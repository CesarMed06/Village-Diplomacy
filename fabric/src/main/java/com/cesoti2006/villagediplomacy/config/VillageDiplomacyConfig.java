package com.cesoti2006.villagediplomacy.config;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Fabric-compatible configuration for Village Diplomacy.
 * Reads config/villagediplomacy.properties on startup.
 * Same options as the Forge version.
 */
public class VillageDiplomacyConfig {

    // ---- Hostile Kills ----
    public static int repUndead = 50;
    public static int repRaid = 15;
    public static int repWitch = 10;
    public static int repEnderman = 5;
    public static int repSpider = 3;
    public static int repCreeper = 2;
    public static int repBabyZombie = 5;
    public static int repPhantom = 3;
    public static int repGuardian = 10;
    public static int repBoss = 50;
    public static boolean hostileKillRequireWitness = true;

    // ---- Penalties ----
    public static int repAttackVillager = -10;
    public static int repKillVillager = -100;
    public static int repKillBabyVillager = -200;
    public static int repKillGolem = -150;
    public static int repChestOpen = -10;
    public static int repBlockBreak = -10;
    public static int repBreakStructure = -10;
    public static int repAnimalKill = -25;
    public static int repAnimalRelease = -12;
    public static int repTrapdoorFarm = -10;
    public static int repBellRing = -15;
    public static int repCraftingUse = -8;
    public static int repDoorOpen = -5;
    public static int repAnimalAttack = -5;

    // ---- Behavior ----
    public static int villageEnterRadius = 80;
    public static int villageExitConfirmRadius = 120;
    public static int customVillageRadius = 48;
    public static boolean enableEntryMessages = true;

    // ---- Guard Villagers ----
    public static int repGuardAttack = -5;
    public static int repGuardKill = -100;
    public static int repGuardWitness = 3;
    public static boolean guardWelcomeMessage = true;

    public static final VillageDiplomacyConfig INSTANCE = new VillageDiplomacyConfig();

    private VillageDiplomacyConfig() {}

    private static boolean loaded = false;

    /**
     * Load config from config/villagediplomacy.properties.
     * Call this once during mod initialization.
     */
    public static void loadConfig() {
        if (loaded) return;
        loaded = true;

        Path configDir = Paths.get("config");
        Path configFile = configDir.resolve("villagediplomacy.properties");

        // Create config dir if it doesn't exist
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not create config directory: " + e.getMessage());
        }

        // Create default config file if it doesn't exist
        if (!Files.exists(configFile)) {
            writeDefaultConfig(configFile);
            VillageDiplomacy.LOGGER.info("Created default config: " + configFile.toAbsolutePath());
            return;
        }

        // Load from file
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(configFile)) {
            props.load(reader);
        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not read config file, using defaults: " + e.getMessage());
            return;
        }

        // Read all values, using defaults if missing
        repUndead = getInt(props, "hostile_kills.repUndead", 50);
        repRaid = getInt(props, "hostile_kills.repRaid", 15);
        repWitch = getInt(props, "hostile_kills.repWitch", 10);
        repEnderman = getInt(props, "hostile_kills.repEnderman", 5);
        repSpider = getInt(props, "hostile_kills.repSpider", 3);
        repCreeper = getInt(props, "hostile_kills.repCreeper", 2);
        repBabyZombie = getInt(props, "hostile_kills.repBabyZombie", 5);
        repPhantom = getInt(props, "hostile_kills.repPhantom", 3);
        repGuardian = getInt(props, "hostile_kills.repGuardian", 10);
        repBoss = getInt(props, "hostile_kills.repBoss", 50);
        hostileKillRequireWitness = getBool(props, "hostile_kills.hostileKillRequireWitness", true);

        repAttackVillager = getInt(props, "penalties.repAttackVillager", -10);
        repKillVillager = getInt(props, "penalties.repKillVillager", -100);
        repKillBabyVillager = getInt(props, "penalties.repKillBabyVillager", -200);
        repKillGolem = getInt(props, "penalties.repKillGolem", -150);
        repChestOpen = getInt(props, "penalties.repChestOpen", -10);
        repBlockBreak = getInt(props, "penalties.repBlockBreak", -10);
        repBreakStructure = getInt(props, "penalties.repBreakStructure", -10);
        repAnimalKill = getInt(props, "penalties.repAnimalKill", -25);
        repAnimalRelease = getInt(props, "penalties.repAnimalRelease", -12);
        repTrapdoorFarm = getInt(props, "penalties.repTrapdoorFarm", -10);
        repBellRing = getInt(props, "penalties.repBellRing", -15);
        repCraftingUse = getInt(props, "penalties.repCraftingUse", -8);
        repDoorOpen = getInt(props, "penalties.repDoorOpen", -5);
        repAnimalAttack = getInt(props, "penalties.repAnimalAttack", -5);

        villageEnterRadius = getInt(props, "behavior.villageEnterRadius", 80);
        villageExitConfirmRadius = getInt(props, "behavior.villageExitConfirmRadius", 120);
        customVillageRadius = getInt(props, "behavior.customVillageRadius", 48);
        enableEntryMessages = getBool(props, "behavior.enableEntryMessages", true);

        repGuardAttack = getInt(props, "guard_villagers.repGuardAttack", -5);
        repGuardKill = getInt(props, "guard_villagers.repGuardKill", -100);
        repGuardWitness = getInt(props, "guard_villagers.repGuardWitness", 3);
        guardWelcomeMessage = getBool(props, "guard_villagers.guardWelcomeMessage", true);

        VillageDiplomacy.LOGGER.info("Village Diplomacy config loaded from " + configFile.toAbsolutePath());
    }

    private static void writeDefaultConfig(Path configFile) {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            writer.write("# Village Diplomacy - Configuration File\n");
            writer.write("# Edit these values and reload the world to apply changes.\n");
            writer.write("# Lines starting with # are comments.\n\n");

            writer.write("[hostile_kills]\n");
            writer.write("# Reputation gained for killing hostile mobs near a village\n");
            writer.write("# Range: 0 to 500\n");
            writer.write("hostileKillRequireWitness = " + hostileKillRequireWitness + "\n");
            writer.write("repUndead = " + repUndead + "\n");
            writer.write("repRaid = " + repRaid + "\n");
            writer.write("repWitch = " + repWitch + "\n");
            writer.write("repEnderman = " + repEnderman + "\n");
            writer.write("repSpider = " + repSpider + "\n");
            writer.write("repCreeper = " + repCreeper + "\n");
            writer.write("repBabyZombie = " + repBabyZombie + "\n");
            writer.write("repPhantom = " + repPhantom + "\n");
            writer.write("repGuardian = " + repGuardian + "\n");
            writer.write("repBoss = " + repBoss + "\n\n");

            writer.write("[penalties]\n");
            writer.write("# Reputation penalties for bad actions (negative values)\n");
            writer.write("# Range: -500 to 0\n");
            writer.write("repAttackVillager = " + repAttackVillager + "\n");
            writer.write("repKillVillager = " + repKillVillager + "\n");
            writer.write("repKillBabyVillager = " + repKillBabyVillager + "\n");
            writer.write("repKillGolem = " + repKillGolem + "\n");
            writer.write("repChestOpen = " + repChestOpen + "\n");
            writer.write("repBlockBreak = " + repBlockBreak + "\n");
            writer.write("repBreakStructure = " + repBreakStructure + "\n");
            writer.write("repAnimalKill = " + repAnimalKill + "\n");
            writer.write("repAnimalRelease = " + repAnimalRelease + "\n");
            writer.write("repTrapdoorFarm = " + repTrapdoorFarm + "\n");
            writer.write("repBellRing = " + repBellRing + "\n");
            writer.write("repCraftingUse = " + repCraftingUse + "\n");
            writer.write("repDoorOpen = " + repDoorOpen + "\n");
            writer.write("repAnimalAttack = " + repAnimalAttack + "\n\n");

            writer.write("[behavior]\n");
            writer.write("# Radius (blocks) to detect village entry. Default: 80\n");
            writer.write("villageEnterRadius = " + villageEnterRadius + "\n");
            writer.write("# Radius to confirm player has LEFT the village. Default: 120\n");
            writer.write("villageExitConfirmRadius = " + villageExitConfirmRadius + "\n");
            writer.write("# Radius for player-claimed villages (/diplomacy claim). Default: 48, Range: 16-128\n");
            writer.write("customVillageRadius = " + customVillageRadius + "\n");
            writer.write("# Show Entering/Leaving village banners? Set to false to disable. Default: true\n");
            writer.write("enableEntryMessages = " + enableEntryMessages + "\n\n");

            writer.write("[guard_villagers]\n");
            writer.write("# Reputation penalty for attacking a guard. Default: -5\n");
            writer.write("repGuardAttack = " + repGuardAttack + "\n");
            writer.write("# Reputation penalty for killing a guard. Default: -100\n");
            writer.write("repGuardKill = " + repGuardKill + "\n");
            writer.write("# Extra reputation bonus when guards witness a hostile mob kill. Default: 3\n");
            writer.write("repGuardWitness = " + repGuardWitness + "\n");
            writer.write("# Show welcome message when Guard Villagers is detected. Default: true\n");
            writer.write("guardWelcomeMessage = " + guardWelcomeMessage + "\n");

        } catch (IOException e) {
            VillageDiplomacy.LOGGER.warn("Could not write default config: " + e.getMessage());
        }
    }

    private static int getInt(Properties props, String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            VillageDiplomacy.LOGGER.warn("Invalid integer for " + key + ": " + val + ", using default " + defaultValue);
            return defaultValue;
        }
    }

    private static boolean getBool(Properties props, String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }
}
