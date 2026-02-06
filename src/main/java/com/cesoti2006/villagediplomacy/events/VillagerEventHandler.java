package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.personality.GolemPersonality;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class VillagerEventHandler {

    private final Map<UUID, Long> tradeCooldowns = new HashMap<>();
    private final Map<UUID, Long> crimeCommittedTime = new HashMap<>();
    private final Map<UUID, Integer> lastReputationLevel = new HashMap<>();
    private final Map<UUID, Long> greetingCooldown = new HashMap<>();
    private final Map<UUID, String> lastVisitedVillage = new HashMap<>();
    private final Map<UUID, List<Long>> villagerAttackTimes = new HashMap<>();
    private final Map<UUID, Long> chestLootCooldown = new HashMap<>();
    private final Map<UUID, Integer> pendingTrades = new HashMap<>();
    private final Map<UUID, Long> tradeWindowStart = new HashMap<>();
    private final Map<UUID, Long> bedUsageCooldown = new HashMap<>();
    private final Map<UUID, Long> bellRingCooldown = new HashMap<>();
    private final Map<UUID, Long> trapdoorCooldown = new HashMap<>();
    private final Map<UUID, Long> doorUsageCooldown = new HashMap<>();
    private final Map<UUID, Long> craftingTableCooldown = new HashMap<>();
    private final Map<UUID, Long> fenceGateCooldown = new HashMap<>();
    private final Map<UUID, Long> animalReleaseCooldown = new HashMap<>();
    private final Map<UUID, Long> doorOpenCooldown = new HashMap<>();
    // Sistema de golems POR GOLEM INDIVIDUAL
    private final Map<UUID, Map<UUID, Integer>> golemStrikesPerGolem = new HashMap<>(); // player -> (golem -> strikes)
    private final Map<UUID, Map<UUID, Long>> golemLastHitTime = new HashMap<>(); // player -> (golem -> time)
    private final Map<UUID, Long> lastGolemHitTime = new HashMap<>();
    private final Map<UUID, Long> golemForgivenessTime = new HashMap<>();
    // Sistema de saludos (para HERO/ALLY)
    private final Map<UUID, Map<UUID, Long>> villagerGreetingCooldown = new HashMap<>();
    // Sistema de curación de zombie villagers (player que curó -> zombie curado)
    private final Map<UUID, UUID> zombieVillagerCurers = new HashMap<>();

    private static final long TRADE_WINDOW_MS = 500;
    private static final long MAJOR_CRIME_DURATION_MS = 120000;
    private static final long MINOR_CRIME_DURATION_MS = 30000;
    private static final long GREETING_COOLDOWN_MS = 180000;
    private static final long STRIKE_WINDOW_MS = 60000;
    private static final int STRIKES_REQUIRED = 3;
    private static final long CHEST_LOOT_COOLDOWN_MS = 3000;
    private static final long BED_COOLDOWN_MS = 5000;
    private static final long BELL_COOLDOWN_MS = 3000;
    private static final long TRAPDOOR_COOLDOWN_MS = 2000;
    private static final long DOOR_COOLDOWN_MS = 2000;
    private static final long CRAFTING_COOLDOWN_MS = 5000;
    private static final long FENCE_GATE_COOLDOWN_MS = 3000;
    private static final long ANIMAL_RELEASE_COOLDOWN_MS = 4000;
    private static final long DOOR_OPEN_COOLDOWN_MS = 3000;
    private static final long GOLEM_RESET_COOLDOWN_MS = 1000; // Revisar golems cada 1 segundo

    // Cooldown para resetear golems (evitar spam)
    private final Map<UUID, Long> golemResetCooldown = new HashMap<>();

    private final String[] adultChestMessages = {
            "§c[Villager] HEY! That's MINE!",
            "§c[Villager] *gasps* A THIEF!",
            "§c[Villager] STOP! Put that back!",
            "§c[Villager] You... you're stealing from us!",
            "§c[Villager] I can't believe you'd do this!",
            "§c[Villager] GUARDS! We have a thief!",
            "§c[Villager] How DARE you open that!",
            "§c[Villager] That chest is NOT yours!",
            "§c[Villager] Get away from my belongings!",
            "§c[Villager] What do you think you're doing?!",
            "§c[Villager] I worked YEARS for what's in there!",
            "§c[Villager] You'll regret this, thief!",
            "§c[Villager] That's private property!",
            "§c[Villager] My life savings are in there!",
            "§c[Villager] HELP! ROBBERY!",
            "§c[Villager] This is a violation of trust!",
            "§c[Villager] Stay away from my chest!",
            "§c[Villager] You're no better than a pillager!"
    };

    private final String[] babyChestMessages = {
            "§c[Baby Villager] *cries* Mommy! They're taking our stuff!",
            "§c[Baby Villager] Noooo! That's our family's chest!",
            "§c[Baby Villager] Why are you being mean? *sniffles*",
            "§c[Baby Villager] I'm telling my dad on you!",
            "§c[Baby Villager] *runs away crying* THIEF!",
            "§c[Baby Villager] That's not yours! *sobs*",
            "§c[Baby Villager] Bad person! Bad!",
            "§c[Baby Villager] My toys are in there!",
            "§c[Baby Villager] *wails* STOOOOOP!",
            "§c[Baby Villager] You're a big bully!",
            "§c[Baby Villager] I'm scared! *cries*",
            "§c[Baby Villager] Daddy said strangers are dangerous!"
    };

    private final String[] adultLootMessages = {
            "§c[Villager] Those are OUR supplies!",
            "§c[Villager] You're taking everything we have!",
            "§c[Villager] THIEF! Someone help!",
            "§c[Villager] I worked HARD for those items!",
            "§c[Villager] You're robbing us blind!",
            "§c[Villager] May you forever be cursed!",
            "§c[Villager] The Iron Golem will hear about this!",
            "§c[Villager] You're leaving us with NOTHING!",
            "§c[Villager] How will we survive now?!",
            "§c[Villager] Those were for the winter!",
            "§c[Villager] You're worse than raiders!",
            "§c[Villager] I hope karma catches up with you!",
            "§c[Villager] You've doomed us all!",
            "§c[Villager] Our children will starve because of you!",
            "§c[Villager] This is unforgivable!"
    };

    private final String[] babyLootMessages = {
            "§c[Baby Villager] *sobs* That was my favorite toy!",
            "§c[Baby Villager] No no no! Not our food!",
            "§c[Baby Villager] You're a big meanie!",
            "§c[Baby Villager] I hate you! *cries loudly*",
            "§c[Baby Villager] Give it baaaack! *wails*",
            "§c[Baby Villager] That's not fair!",
            "§c[Baby Villager] You're evil! *sniffles*",
            "§c[Baby Villager] I'll never forget this!",
            "§c[Baby Villager] Why would you do that?! *cries*",
            "§c[Baby Villager] Mommy's gonna be so upset!"
    };

    @SubscribeEvent
    public void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        Villager villager = (Villager) event.getEntity();
        BlockPos villagerPos = villager.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty())
            return;

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get());

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();

        // SISTEMA DE LUTO: DESACTIVADO TEMPORALMENTE - Causaba congelamiento
        /*
        if (!villager.isBaby()) {
            com.cesoti2006.villagediplomacy.data.VillageMourningData mourningData = 
                com.cesoti2006.villagediplomacy.data.VillageMourningData.get(level);
            
            String villagerName = villager.hasCustomName() ? 
                villager.getCustomName().getString() : "Villager";
            String profession = villager.getVillagerData().getProfession().toString();
            
            // Obtener job site del brain memory
            BlockPos jobSite = villager.getBrain()
                .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE)
                .map(poi -> poi.pos())
                .orElse(null);
            
            String villageId = VillageDetector.getVillageId(villagePos);
            mourningData.registerDeath(villageId, villagerName, profession, jobSite);
        }
        */

        int reputationLoss = villager.isBaby() ? -200 : -100;
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, reputationLoss);

        int newRep = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(newRep);
        checkAndNotifyReputationChange(player, oldRep, newRep);

        String[] deathMessages = villager.isBaby() ? new String[] {
                "§4[Village Diplomacy] You killed a CHILD! The village will NEVER forgive you!",
                "§4[Village Diplomacy] MONSTER! You murdered an innocent child!",
                "§4[Village Diplomacy] A baby...you killed a baby. You are pure EVIL!",
                "§4[Village Diplomacy] The village mourns...you've killed one of their children."
        }
                : new String[] {
                        "§c[Village Diplomacy] You killed a villager!",
                        "§c[Village Diplomacy] MURDER! The village witnessed your crime!",
                        "§c[Village Diplomacy] A villager is dead by your hand!",
                        "§c[Village Diplomacy] You've taken an innocent life!"
                };

        player.sendSystemMessage(Component.literal(
                deathMessages[level.getRandom().nextInt(deathMessages.length)] +
                        " Reputation " + reputationLoss + " (Total: " + newRep + " - " + status + ")"));

        checkReputationLevelChange(player, level, newRep);

        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(24.0D),
                golem -> !golem.isPlayerCreated());

        if (!nearbyGolems.isEmpty()) {
            UUID playerId = player.getUUID();
            long currentTime = System.currentTimeMillis();
            long existingCrimeEnd = crimeCommittedTime.getOrDefault(playerId, 0L);
            long newCrimeEnd = currentTime + MAJOR_CRIME_DURATION_MS;

            if (existingCrimeEnd > currentTime) {
                newCrimeEnd = existingCrimeEnd + MAJOR_CRIME_DURATION_MS;
                int totalSeconds = (int) ((newCrimeEnd - currentTime) / 1000);
                player.sendSystemMessage(Component.literal(
                        "§4[Village Diplomacy] MURDER! Crime time extended! Total: " + totalSeconds + " seconds"));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§4[Village Diplomacy] MURDER WITNESSED! Iron Golems will hunt you for 2 minutes!"));
            }

            crimeCommittedTime.put(playerId, newCrimeEnd);
            villagerAttackTimes.remove(playerId);
        }
    }

    // GOLEMS: 100% VANILLA - La reputación solo afecta mensajes de interacción, NO al combate

    @SubscribeEvent
    public void onIronGolemDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        IronGolem golem = (IronGolem) event.getEntity();
        BlockPos golemPos = golem.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, golemPos, 200);

        if (nearestVillage.isEmpty())
            return;

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get());

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        data.addReputation(player.getUUID(), villagePos, -150);

        int newRep = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(newRep);

        player.sendSystemMessage(Component.literal(
                "§4[Village Diplomacy] You killed an Iron Golem! Reputation -150 (Total: " +
                        newRep + " - " + status + ")"));

        checkReputationLevelChange(player, level, newRep);

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        long existingCrimeEnd = crimeCommittedTime.getOrDefault(playerId, 0L);
        long newCrimeEnd = currentTime + MAJOR_CRIME_DURATION_MS;

        if (existingCrimeEnd > currentTime) {
            newCrimeEnd = existingCrimeEnd + MAJOR_CRIME_DURATION_MS;
        }

        crimeCommittedTime.put(playerId, newCrimeEnd);
    }

    // MENSAJE ÚNICO: cuando atacas al golem, te responde según su personalidad
    // Luego comportamiento 100% VANILLA - te ataca normal
    @SubscribeEvent
    public void onGolemAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem))
            return;
        if (golem.isPlayerCreated())
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!(golem.level() instanceof ServerLevel level))
            return;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, golem.blockPosition(), 200);
        if (nearestVillage.isEmpty())
            return;

        UUID golemId = golem.getUUID();
        long currentTime = System.currentTimeMillis();

        // Cooldown para el mensaje: solo 1 vez cada 5 segundos
        golemLastHitTime.putIfAbsent(player.getUUID(), new HashMap<>());
        Map<UUID, Long> playerGolemHitTimes = golemLastHitTime.get(player.getUUID());
        
        Long lastHit = playerGolemHitTimes.get(golemId);
        if (lastHit == null || currentTime - lastHit > 5000) {
            playerGolemHitTimes.put(golemId, currentTime);
            
            // Obtener personalidad del golem
            GolemPersonalityData personalityData = GolemPersonalityData.get(level);
            GolemPersonality personality = personalityData.getPersonality(golemId);
            
            // Mensaje según personalidad
            if (personality != null) {
                String message = switch (personality.getTemperament()) {
                    case GENTLE -> "§e[" + personality.getName() + "] Why...? I must defend myself!";
                    case STERN -> "§c[" + personality.getName() + "] You've made your choice.";
                    case FIERCE -> "§4[" + personality.getName() + "] *ROARS* YOU DIE NOW!";
                    default -> "§c[" + personality.getName() + "] So be it!";
                };
                player.sendSystemMessage(Component.literal(message));
            } else {
                // Si no tiene personalidad, mensaje genérico
                String golemName = golem.hasCustomName() ? 
                    golem.getCustomName().getString() : "Iron Golem";
                player.sendSystemMessage(Component.literal(
                    "§c[" + golemName + "] You asked for this!"));
            }
        }
        
        // NO CANCELAR - comportamiento vanilla completo
    }

    @SubscribeEvent
    public void onVillagerAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Villager))
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;

        Villager villager = (Villager) event.getEntity();
        BlockPos villagerPos = villager.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty())
            return;

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, -10);
        
        // Feedback visual negativo (partículas + sonido)
        spawnNegativeFeedback(level, villager);

        int newRep = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(newRep);
        checkAndNotifyReputationChange(player, oldRep, newRep);

        // Mensajes básicos para bebés (no tienen profesión)
        String[] babyMessages = {
                "§c[Baby Villager] *screams* HELP! Someone's hitting me!",
                "§c[Baby Villager] OW OW OW! Stop it!",
                "§c[Baby Villager] *cries* That hurts!",
                "§c[Baby Villager] MOMMY! DADDY! HELP!",
                "§c[Baby Villager] Why are you hurting me?! *sobs*",
                "§c[Baby Villager] I didn't do anything! *whimpers*",
                "§c[Baby Villager] *wails* Leave me alone!",
                "§c[Baby Villager] You're a mean person!",
                "§c[Baby Villager] *screaming and crying* STOP!",
                "§c[Baby Villager] I'm telling the Iron Golem!",
                "§c[Baby Villager] *terrified* Please don't hurt me!"
        };

        String message;

        if (villager.isBaby()) {
            message = babyMessages[level.getRandom().nextInt(babyMessages.length)];
        } else {
            // MENSAJES ESPECÍFICOS POR PROFESIÓN
            String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
            String[] professionMessages;

            switch (profession) {
                case "farmer":
                    professionMessages = new String[] {
                            "§c[Farmer] Stop! I'm just trying to feed the village!",
                            "§c[Farmer] Don't hurt me! Who will tend the crops?!",
                            "§c[Farmer] I work the fields day and night, and THIS is my thanks?!",
                            "§c[Farmer] Leave me alone! The harvest won't gather itself!",
                            "§c[Farmer] You're attacking the one who grows your food!",
                            "§c[Farmer] I've got carrots to plant! Guards!",
                            "§c[Farmer] My back already hurts from farming!",
                            "§c[Farmer] Without me, you'd starve!"
                    };
                    break;

                case "librarian":
                    professionMessages = new String[] {
                            "§c[Librarian] Violence is not the answer! Read a book!",
                            "§c[Librarian] Cease this barbarism at once!",
                            "§c[Librarian] I'm a man of KNOWLEDGE, not combat!",
                            "§c[Librarian] This is most unscholarly behavior!",
                            "§c[Librarian] Stop! Think of the books!",
                            "§c[Librarian] I'd prefer to resolve this intellectually!",
                            "§c[Librarian] My enchantments won't save me from THIS!",
                            "§c[Librarian] Barbaric! Simply barbaric!"
                    };
                    break;

                case "armorer":
                    professionMessages = new String[] {
                            "§c[Armorer] You dare attack a MASTER of armor?!",
                            "§c[Armorer] I forge protection, and you strike me?!",
                            "§c[Armorer] Ironic! Attacking the one who makes armor!",
                            "§c[Armorer] I should've kept a sword nearby!",
                            "§c[Armorer] Who'll repair your gear NOW, fool?!",
                            "§c[Armorer] My smithing hammer is RIGHT THERE!",
                            "§c[Armorer] Tough guy, attacking an armorer!",
                            "§c[Armorer] I work with METAL all day! Bad idea!"
                    };
                    break;

                case "weaponsmith":
                    professionMessages = new String[] {
                            "§c[Weaponsmith] Attacking a WEAPONSMITH?! Are you MAD?!",
                            "§c[Weaponsmith] I forge SWORDS, you fool!",
                            "§c[Weaponsmith] Bold move attacking someone who makes weapons!",
                            "§c[Weaponsmith] Wait till I grab my finest blade!",
                            "§c[Weaponsmith] I know 50 ways to hurt you with THIS hammer!",
                            "§c[Weaponsmith] Wrong villager to mess with, friend!",
                            "§c[Weaponsmith] I'm surrounded by weapons! Think!",
                            "§c[Weaponsmith] Every blade here is mine!"
                    };
                    break;

                case "toolsmith":
                    professionMessages = new String[] {
                            "§c[Toolsmith] Stop! I'm the one who makes your tools!",
                            "§c[Toolsmith] No more pickaxes for YOU!",
                            "§c[Toolsmith] Without me, good luck mining!",
                            "§c[Toolsmith] I have hammers everywhere, bad idea!",
                            "§c[Toolsmith] Who will forge your tools now?!",
                            "§c[Toolsmith] Think you can mine with your FISTS?!",
                            "§c[Toolsmith] I'm the reason you HAVE tools!",
                            "§c[Toolsmith] Good luck crafting without me!"
                    };
                    break;

                case "cleric":
                    professionMessages = new String[] {
                            "§c[Cleric] You attack a healer?! BLASPHEMY!",
                            "§c[Cleric] May the gods forgive you, for I won't!",
                            "§c[Cleric] This is sacrilege!",
                            "§c[Cleric] I heal the sick, and THIS is my reward?!",
                            "§c[Cleric] You'll need my healing potions later!",
                            "§c[Cleric] The gods are watching, sinner!",
                            "§c[Cleric] I'll pray you see the error of your ways!",
                            "§c[Cleric] Divine retribution awaits you!"
                    };
                    break;

                case "butcher":
                    professionMessages = new String[] {
                            "§c[Butcher] Wrong person! I have CLEAVERS!",
                            "§c[Butcher] I work with MEAT and BLADES daily!",
                            "§c[Butcher] Bad idea attacking someone with knives!",
                            "§c[Butcher] I butcher ANIMALS, not people!",
                            "§c[Butcher] My cleaver is SHARP, buddy!",
                            "§c[Butcher] No more cooked porkchops for YOU!",
                            "§c[Butcher] I deal with raw meat, I can handle you!",
                            "§c[Butcher] You're making a GRAVE mistake!"
                    };
                    break;

                case "cartographer":
                    professionMessages = new String[] {
                            "§c[Cartographer] Stop! I make your MAPS!",
                            "§c[Cartographer] Without me, you'll get LOST!",
                            "§c[Cartographer] I chart the world, and this is my thanks?!",
                            "§c[Cartographer] Good luck navigating without my maps!",
                            "§c[Cartographer] Guards! Someone's attacking the map maker!",
                            "§c[Cartographer] I explore so you don't have to!",
                            "§c[Cartographer] Lost? Don't come to ME anymore!",
                            "§c[Cartographer] My maps won't show YOU any treasures now!"
                    };
                    break;

                case "fisherman":
                    professionMessages = new String[] {
                            "§c[Fisherman] Stop! What did I ever do to you?!",
                            "§c[Fisherman] I just fish all day!",
                            "§c[Fisherman] Leave a simple fisherman alone!",
                            "§c[Fisherman] I've got a fishing rod, stay back!",
                            "§c[Fisherman] No more fish trades for you!",
                            "§c[Fisherman] All I do is FISH! Why attack me?!",
                            "§c[Fisherman] I provide FOOD for everyone!",
                            "§c[Fisherman] Catch your own fish from now on!"
                    };
                    break;

                case "fletcher":
                    professionMessages = new String[] {
                            "§c[Fletcher] I make ARROWS! I have plenty right here!",
                            "§c[Fletcher] Wrong craftsman to attack, pal!",
                            "§c[Fletcher] Stop! I'm surrounded by projectiles!",
                            "§c[Fletcher] No more arrows for YOUR bow!",
                            "§c[Fletcher] I can shoot from HERE, you know!",
                            "§c[Fletcher] Attacking the arrow maker?! Bold!",
                            "§c[Fletcher] Who'll supply your arrows NOW?!",
                            "§c[Fletcher] I have a bow right behind me!"
                    };
                    break;

                case "leatherworker":
                    professionMessages = new String[] {
                            "§c[Leatherworker] Stop! I make your leather armor!",
                            "§c[Leatherworker] Who will craft your gear?!",
                            "§c[Leatherworker] I work hard on every piece!",
                            "§c[Leatherworker] No more leather goods for you!",
                            "§c[Leatherworker] Guards! Help!",
                            "§c[Leatherworker] This is how you treat a craftsman?!",
                            "§c[Leatherworker] My workshop is my livelihood!",
                            "§c[Leatherworker] Ungrateful! I make quality goods!"
                    };
                    break;

                case "mason":
                    professionMessages = new String[] {
                            "§c[Mason] I build with STONE! I'm tough!",
                            "§c[Mason] Bad idea! I handle heavy blocks daily!",
                            "§c[Mason] Stop! Who will build your structures?!",
                            "§c[Mason] These hands shape STONE, they can handle you!",
                            "§c[Mason] I'm a MASON! We're sturdy folk!",
                            "§c[Mason] Guards! Someone's attacking the builder!",
                            "§c[Mason] My work is permanent, so is my memory!",
                            "§c[Mason] You mess with stone, you get the rock!"
                    };
                    break;

                case "shepherd":
                    professionMessages = new String[] {
                            "§c[Shepherd] Leave me alone! I just tend sheep!",
                            "§c[Shepherd] All I do is shear wool!",
                            "§c[Shepherd] Why attack a peaceful shepherd?!",
                            "§c[Shepherd] My sheep need me!",
                            "§c[Shepherd] Guards! Help the shepherd!",
                            "§c[Shepherd] I provide wool for everyone!",
                            "§c[Shepherd] This is how you treat a herder?!",
                            "§c[Shepherd] No more wool for your beds!"
                    };
                    break;

                case "nitwit":
                    professionMessages = new String[] {
                            "§c[Nitwit] *confused* Why?! I don't even have a job!",
                            "§c[Nitwit] I'm just... existing! Leave me alone!",
                            "§c[Nitwit] *panics* HELP! Someone!",
                            "§c[Nitwit] I don't understand! What did I do?!",
                            "§c[Nitwit] *whimpers* Please stop!",
                            "§c[Nitwit] I'm harmless! Why me?!",
                            "§c[Nitwit] This makes no sense!"
                    };
                    break;

                default:
                    // Profesión desconocida o sin profesión
                    professionMessages = new String[] {
                            "§c[Villager] OW! Why are you attacking me?!",
                            "§c[Villager] Stop this at once!",
                            "§c[Villager] What did I ever do to you?!",
                            "§c[Villager] Guards! GUARDS!",
                            "§c[Villager] You're going to regret this!",
                            "§c[Villager] Have you lost your mind?!",
                            "§c[Villager] Leave me alone you brute!",
                            "§c[Villager] This is assault!",
                            "§c[Villager] The Iron Golem will hear about this!"
                    };
            }

            message = professionMessages[level.getRandom().nextInt(professionMessages.length)];
        }

        player.sendSystemMessage(Component.literal(message));
        player.sendSystemMessage(Component.literal(
                "§c[Village Diplomacy] Attacked a villager! Reputation -10 (Total: " +
                        newRep + " - " + status + ")"));

        checkReputationLevelChange(player, level, newRep);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get());

        processStrikeSystem(player, level, villagerPos);
    }

    @SubscribeEvent
    public void onAnimalAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;

        // Detectar tipo específico de animal
        String animalType = null;
        if (event.getEntity() instanceof Cow) animalType = "cow";
        else if (event.getEntity() instanceof Sheep) animalType = "sheep";
        else if (event.getEntity() instanceof Pig) animalType = "pig";
        else if (event.getEntity() instanceof Chicken) animalType = "chicken";
        else if (event.getEntity() instanceof Rabbit) animalType = "rabbit";
        else if (event.getEntity() instanceof AbstractHorse) animalType = "horse";
        else if (event.getEntity() instanceof Camel) animalType = "camel";
        
        if (animalType == null) return;

        BlockPos animalPos = event.getEntity().blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, animalPos, 200);

        if (nearestVillage.isPresent()) {
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    AABB.ofSize(Vec3.atCenterOf(animalPos), 48, 48, 48));

            for (Villager villager : nearbyVillagers) {
                if (hasLineOfSight(villager, player, level)) {
                    VillageReputationData data = VillageReputationData.get(level);

                    long currentTime = System.currentTimeMillis();
                    UUID playerId = player.getUUID();

                    if (!tradeCooldowns.containsKey(playerId) ||
                            currentTime - tradeCooldowns.get(playerId) > 2000) {

                        int oldRep = data.getReputation(player.getUUID(), nearestVillage.get());
                        data.addReputation(player.getUUID(), nearestVillage.get(), -5);
                        int newRep = data.getReputation(player.getUUID(), nearestVillage.get());
                        checkAndNotifyReputationChange(player, oldRep, newRep);

                        // MENSAJES ESPECÍFICOS POR TIPO DE ANIMAL
                        String[] babyMessages;
                        String[] adultMessages;

                        switch (animalType) {
                            case "cow":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt our cows! We need milk!",
                                        "§c[Baby Villager] That cow gives us milk! *cries*",
                                        "§c[Baby Villager] Moosy is my friend!",
                                        "§c[Baby Villager] Leave the cow alone!",
                                        "§c[Baby Villager] I love that cow!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] Stop! That cow provides milk for the village!",
                                        "§c[Villager] Our dairy supply! Leave it alone!",
                                        "§c[Villager] That cow feeds our children!",
                                        "§c[Villager] We depend on those cows for milk and leather!",
                                        "§c[Villager] Back away from our cattle!",
                                        "§c[Villager] Those cows are essential to our survival!",
                                        "§c[Villager] That's weeks of milk you're threatening!"
                                };
                                break;

                            case "sheep":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt Fluffy! *cries*",
                                        "§c[Baby Villager] That sheep makes our beds!",
                                        "§c[Baby Villager] I like petting the sheep!",
                                        "§c[Baby Villager] Leave the woolly alone!",
                                        "§c[Baby Villager] The sheep is so soft!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] That sheep provides our wool!",
                                        "§c[Villager] Stop! We need that wool for blankets!",
                                        "§c[Villager] Our textile supply! Leave it be!",
                                        "§c[Villager] Those sheep keep us warm in winter!",
                                        "§c[Villager] We shear those sheep for clothing!",
                                        "§c[Villager] That's our wool source, you brute!",
                                        "§c[Villager] Without wool, we freeze!"
                                };
                                break;

                            case "pig":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt the piggy! *sobs*",
                                        "§c[Baby Villager] That pig makes funny sounds!",
                                        "§c[Baby Villager] Oink-oink is nice!",
                                        "§c[Baby Villager] Leave the piggy alone!",
                                        "§c[Baby Villager] I feed that pig every day!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] That pig is valuable livestock!",
                                        "§c[Villager] Stop! Those pigs are for breeding!",
                                        "§c[Villager] We raise those pigs with care!",
                                        "§c[Villager] That pig will feed families this winter!",
                                        "§c[Villager] Leave our pork supply alone!",
                                        "§c[Villager] Those pigs are our investment!",
                                        "§c[Villager] Back off! That pig is spoken for!"
                                };
                                break;

                            case "chicken":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt the chicken! *cries*",
                                        "§c[Baby Villager] That chicken gives us eggs!",
                                        "§c[Baby Villager] I collect eggs from them!",
                                        "§c[Baby Villager] The chickens are my job!",
                                        "§c[Baby Villager] Clucky is so nice!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] Those chickens lay our eggs!",
                                        "§c[Villager] Stop! That chicken is our breakfast!",
                                        "§c[Villager] We need those eggs daily!",
                                        "§c[Villager] That chicken is part of our farm!",
                                        "§c[Villager] Leave our poultry alone!",
                                        "§c[Villager] Those chickens are egg producers!",
                                        "§c[Villager] Without chickens, no eggs!"
                                };
                                break;

                            case "rabbit":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt the bunny! *cries*",
                                        "§c[Baby Villager] Bunnies are so cute!",
                                        "§c[Baby Villager] That's my favorite rabbit!",
                                        "§c[Baby Villager] Leave the hoppy alone!",
                                        "§c[Baby Villager] I want to pet them!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] Those rabbits are part of our ecosystem!",
                                        "§c[Villager] Leave the rabbits be!",
                                        "§c[Villager] They're harmless creatures!",
                                        "§c[Villager] Stop attacking innocent animals!",
                                        "§c[Villager] Those rabbits help our gardens!",
                                        "§c[Villager] What did that rabbit ever do to you?!",
                                        "§c[Villager] They're just rabbits!"
                                };
                                break;

                            case "horse":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt the horsie! *cries*",
                                        "§c[Baby Villager] I want to ride horses when I grow up!",
                                        "§c[Baby Villager] That horse is so pretty!",
                                        "§c[Baby Villager] Leave the horse alone!",
                                        "§c[Baby Villager] Horses are noble!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] That horse is our transportation!",
                                        "§c[Villager] Stop! We need that horse for travel!",
                                        "§c[Villager] Those horses are expensive!",
                                        "§c[Villager] That's weeks of breeding work!",
                                        "§c[Villager] Leave our horses alone!",
                                        "§c[Villager] We use those horses for trade routes!",
                                        "§c[Villager] That horse carries our supplies!",
                                        "§c[Villager] You're attacking our mobility!"
                                };
                                break;

                            case "camel":
                                babyMessages = new String[] {
                                        "§c[Baby Villager] Don't hurt the camel! *cries*",
                                        "§c[Baby Villager] Camels are for the desert!",
                                        "§c[Baby Villager] That camel is cool!",
                                        "§c[Baby Villager] Leave the camel alone!",
                                        "§c[Baby Villager] I like camels!"
                                };
                                adultMessages = new String[] {
                                        "§c[Villager] That camel is our desert transportation!",
                                        "§c[Villager] Stop! We need that camel for travel!",
                                        "§c[Villager] Those camels are expensive!",
                                        "§c[Villager] That's weeks of breeding work!",
                                        "§c[Villager] Leave our camels alone!",
                                        "§c[Villager] We use those camels for desert routes!",
                                        "§c[Villager] That camel carries our supplies!",
                                        "§c[Villager] You're attacking our mobility!"
                                };
                                break;

                            default:
                                babyMessages = new String[] {"§c[Baby Villager] Don't hurt them!"};
                                adultMessages = new String[] {"§c[Villager] Stop that!"};
                        }

                        String[] messages = villager.isBaby() ? babyMessages : adultMessages;

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Village Diplomacy] Attacked " + animalType + "! Reputation -5 (Total: " +
                                        newRep + " - " + getReputationStatus(newRep) + ")"));

                        tradeCooldowns.put(playerId, currentTime);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onAnimalDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;

        // Detectar tipo específico de animal
        String animalType = null;
        if (event.getEntity() instanceof Cow) animalType = "cow";
        else if (event.getEntity() instanceof Sheep) animalType = "sheep";
        else if (event.getEntity() instanceof Pig) animalType = "pig";
        else if (event.getEntity() instanceof Chicken) animalType = "chicken";
        else if (event.getEntity() instanceof Rabbit) animalType = "rabbit";
        else if (event.getEntity() instanceof AbstractHorse) animalType = "horse";
        else if (event.getEntity() instanceof Camel) animalType = "camel";
        
        if (animalType == null) return;

        BlockPos animalPos = event.getEntity().blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, animalPos, 200);

        if (nearestVillage.isPresent()) {
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    AABB.ofSize(Vec3.atCenterOf(animalPos), 48, 48, 48));

            for (Villager villager : nearbyVillagers) {
                if (hasLineOfSight(villager, player, level)) {
                    VillageReputationData data = VillageReputationData.get(level);
                    int oldRep = data.getReputation(player.getUUID(), nearestVillage.get());
                    data.addReputation(player.getUUID(), nearestVillage.get(), -25);
                    int newRep = data.getReputation(player.getUUID(), nearestVillage.get());
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    // MENSAJES ESPECÍFICOS POR TIPO DE ANIMAL - MUERTE
                    String[] babyMessages;
                    String[] adultMessages;

                    switch (animalType) {
                        case "cow":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] NOOOO! You killed Bessie! *wails*",
                                    "§c[Baby Villager] No more milk now! *sobs*",
                                    "§c[Baby Villager] That cow had a calf! *cries*",
                                    "§c[Baby Villager] Why?! She gave us milk! *heartbroken*",
                                    "§c[Baby Villager] Moosy is gone... *sniffles*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU MURDERED OUR COW!",
                                    "§c[Villager] That's MONTHS of milk production GONE!",
                                    "§c[Villager] How will we feed our children without milk?!",
                                    "§c[Villager] That cow was worth 10 emeralds!",
                                    "§c[Villager] You've destroyed our dairy farm!",
                                    "§c[Villager] ANIMAL MURDERER! That cow had calves!",
                                    "§c[Villager] We raised that cow from birth!",
                                    "§c[Villager] No milk, no cheese, no leather! Thanks to YOU!"
                            };
                            break;

                        case "sheep":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed Fluffy! NOOO! *cries*",
                                    "§c[Baby Villager] No more wool now! *sobs*",
                                    "§c[Baby Villager] That sheep was so soft! *wails*",
                                    "§c[Baby Villager] Why kill the woolly?! *heartbroken*",
                                    "§c[Baby Villager] I was gonna shear it tomorrow! *devastated*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU KILLED OUR SHEEP!",
                                    "§c[Villager] That sheep produced wool for YEARS!",
                                    "§c[Villager] How will we make blankets now?!",
                                    "§c[Villager] We'll FREEZE without that wool!",
                                    "§c[Villager] That sheep was our textile source!",
                                    "§c[Villager] MURDERER! We bred that sheep carefully!",
                                    "§c[Villager] No wool means no warm clothing!",
                                    "§c[Villager] That sheep was white wool - RARE!"
                            };
                            break;

                        case "pig":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed the piggy! MONSTER! *cries*",
                                    "§c[Baby Villager] That pig was gonna have babies! *sobs*",
                                    "§c[Baby Villager] Oink-oink is gone! *wails*",
                                    "§c[Baby Villager] Why?! He was so funny! *devastated*",
                                    "§c[Baby Villager] I fed that pig carrots! *heartbroken*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU SLAUGHTERED OUR PIG!",
                                    "§c[Villager] That pig was breeding stock!",
                                    "§c[Villager] You just killed WINTER'S MEAT SUPPLY!",
                                    "§c[Villager] We raised that pig for MONTHS!",
                                    "§c[Villager] THIEF! That pig was our investment!",
                                    "§c[Villager] How DARE you kill our livestock!",
                                    "§c[Villager] That pig was going to feed families!",
                                    "§c[Villager] You've ruined our breeding program!"
                            };
                            break;

                        case "chicken":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed Clucky! NOOO! *cries*",
                                    "§c[Baby Villager] No more eggs now! *sobs*",
                                    "§c[Baby Villager] That chicken laid eggs every day! *wails*",
                                    "§c[Baby Villager] Why?! She was nice! *devastated*",
                                    "§c[Baby Villager] I collected her eggs! *heartbroken*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU KILLED OUR CHICKEN!",
                                    "§c[Villager] That was our BREAKFAST SOURCE!",
                                    "§c[Villager] We needed those eggs daily!",
                                    "§c[Villager] That chicken laid eggs reliably!",
                                    "§c[Villager] How will we bake without eggs?!",
                                    "§c[Villager] POULTRY KILLER! That was food security!",
                                    "§c[Villager] One egg a day - GONE because of you!",
                                    "§c[Villager] We raised that chicken from a chick!"
                            };
                            break;

                        case "rabbit":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed the bunny! CRUEL! *sobs*",
                                    "§c[Baby Villager] Bunnies are so cute! Why?! *cries*",
                                    "§c[Baby Villager] The hoppy is dead! *wails*",
                                    "§c[Baby Villager] That was so mean! *devastated*",
                                    "§c[Baby Villager] I loved that rabbit! *heartbroken*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU KILLED THE RABBIT!",
                                    "§c[Villager] That rabbit helped control garden pests!",
                                    "§c[Villager] What kind of MONSTER kills rabbits?!",
                                    "§c[Villager] They're harmless creatures!",
                                    "§c[Villager] The children loved that rabbit!",
                                    "§c[Villager] You're a BRUTE and a BULLY!",
                                    "§c[Villager] Killing innocent animals! Shame on you!",
                                    "§c[Villager] That rabbit never hurt anyone!"
                            };
                            break;

                        case "horse":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed the horsie! NOOO! *wails*",
                                    "§c[Baby Villager] That horse was so strong! *cries*",
                                    "§c[Baby Villager] I wanted to ride it! *devastated*",
                                    "§c[Baby Villager] Horses are noble! Why?! *sobs*",
                                    "§c[Baby Villager] That's the worst thing ever! *heartbroken*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU KILLED OUR HORSE!",
                                    "§c[Villager] That horse was EXPENSIVE! 20 emeralds!",
                                    "§c[Villager] We needed that horse for TRAVEL!",
                                    "§c[Villager] That took WEEKS to tame and breed!",
                                    "§c[Villager] How will we transport goods now?!",
                                    "§c[Villager] HORSE KILLER! That was our LIVELIHOOD!",
                                    "§c[Villager] We used that horse for trade routes!",
                                    "§c[Villager] You've crippled our commerce!",
                                    "§c[Villager] That horse was part of the family!",
                                    "§c[Villager] Killing a horse?! You're HEARTLESS!"
                            };
                            break;

                        case "camel":
                            babyMessages = new String[] {
                                    "§c[Baby Villager] You killed the camel! NOOO! *wails*",
                                    "§c[Baby Villager] That camel was so tall! *cries*",
                                    "§c[Baby Villager] I wanted to ride it! *devastated*",
                                    "§c[Baby Villager] Camels are amazing! Why?! *sobs*",
                                    "§c[Baby Villager] That's so cruel! *heartbroken*"
                            };
                            adultMessages = new String[] {
                                    "§c[Villager] YOU KILLED OUR CAMEL!",
                                    "§c[Villager] That camel was EXPENSIVE! 30 emeralds!",
                                    "§c[Villager] We needed that camel for DESERT TRAVEL!",
                                    "§c[Villager] That took WEEKS to tame and breed!",
                                    "§c[Villager] How will we cross the desert now?!",
                                    "§c[Villager] CAMEL KILLER! That was our LIVELIHOOD!",
                                    "§c[Villager] We used that camel for desert routes!",
                                    "§c[Villager] You've crippled our desert commerce!",
                                    "§c[Villager] That camel was irreplaceable!",
                                    "§c[Villager] Killing a camel?! You're HEARTLESS!"
                            };
                            break;

                        default:
                            babyMessages = new String[] {"§c[Baby Villager] You killed it! *cries*"};
                            adultMessages = new String[] {"§c[Villager] MURDERER!"};
                    }

                    String[] messages = villager.isBaby() ? babyMessages : adultMessages;

                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                    player.sendSystemMessage(Component.literal(
                            "§c[Village Diplomacy] Killed " + animalType + "! Reputation -25 (Total: " +
                                    newRep + " - " + getReputationStatus(newRep) + ")"));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(nearestVillage.get());

                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTrade(net.minecraftforge.event.entity.player.TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(player.level() instanceof ServerLevel level))
            return;

        Villager villager = event.getAbstractVillager() instanceof Villager ? (Villager) event.getAbstractVillager()
                : null;
        if (villager == null)
            return;

        BlockPos villagerPos = villager.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty())
            return;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (!tradeWindowStart.containsKey(playerId) ||
                currentTime - tradeWindowStart.get(playerId) > TRADE_WINDOW_MS) {
            tradeWindowStart.put(playerId, currentTime);
            pendingTrades.put(playerId, 1);
        } else {
            pendingTrades.put(playerId, pendingTrades.getOrDefault(playerId, 0) + 1);
        }
        
        // INCREMENTAR BONIFICACIÓN DE REPUTACIÓN PERSONAL DEL ALDEANO
        com.cesoti2006.villagediplomacy.data.VillagerPersonalityData personalityData = 
            com.cesoti2006.villagediplomacy.data.VillagerPersonalityData.get(level);
        com.cesoti2006.villagediplomacy.personality.VillagerPersonality personality = 
            personalityData.getPersonality(villager.getUUID());
        
        if (personality != null) {
            // Cada trade aumenta +3 la bonificación personal (máximo 100)
            personality.addPlayerReputationBonus(3);
            personalityData.setDirty();
            
            // Debug message para testear
            int currentBonus = personality.getPlayerReputationBonus();
            if (currentBonus >= 30 && currentBonus % 10 == 0) {
                player.sendSystemMessage(Component.literal(
                    "§7[Debug] " + personality.getCustomName() + "'s bond: " + currentBonus + "/30 for testament"));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player))
            return;
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END)
            return;

        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (tradeWindowStart.containsKey(playerId) &&
                currentTime - tradeWindowStart.get(playerId) > TRADE_WINDOW_MS) {

            int trades = pendingTrades.getOrDefault(playerId, 0);
            if (trades > 0) {
                VillageReputationData data = VillageReputationData.get(level);
                int oldRep = data.getReputation(playerId);

                data.addReputation(playerId, trades * 5);
                int newRep = data.getReputation(playerId);
                checkAndNotifyReputationChange(player, oldRep, newRep);
                
                // Feedback visual positivo (partículas + sonido)
                spawnPositiveFeedback(level, player);

                String[] tradeMessages = {
                        "§a[Village Diplomacy] Completed " + trades
                                + " trade(s)! The village appreciates your business.",
                        "§a[Village Diplomacy] " + trades + " successful trade(s)! Your reputation grows.",
                        "§a[Village Diplomacy] Excellent trading! The villagers are pleased.",
                        "§a[Village Diplomacy] " + trades + " trade(s) completed. The village trusts you more.",
                        "§a[Village Diplomacy] Fair trading! The village values your commerce.",
                        "§a[Village Diplomacy] " + trades + " trade(s) done. You're becoming a valued customer."
                };

                player.sendSystemMessage(Component.literal(
                        tradeMessages[level.getRandom().nextInt(tradeMessages.length)] +
                                " Reputation +" + (trades * 5) + " (Total: " + newRep + " - "
                                + getReputationStatus(newRep) + ")"));

                pendingTrades.remove(playerId);
                tradeWindowStart.remove(playerId);
            }
        }

        if (player.tickCount % 20 == 0) {
            manageCrimeStatus(player, level);
            checkForVillageEntry(player, level);
            giveRandomGifts(player, level);
            makeVillagersFleeFromHostilePlayers(player, level);
            makeGolemsProtectVillageBasedOnReputation(player, level);
            checkForVillagerGreetings(player, level);
            // ELIMINADO: resetGolemsForPardonedPlayers - causaba bugs
            // Los golems ahora solo perdonan cuando usas /pardon me (via onGolemAttackPlayer)
        }
    }

    @SubscribeEvent
    public void onDoorOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();

        if (!(clickedBlock instanceof DoorBlock))
            return;

        // FIX: Buscar aldea cercana PRIMERO
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);
        if (nearestVillage.isEmpty())
            return;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // FIX: Cooldown más corto para que funcione mejor (reducido de 3s a 1.5s)
        if (doorOpenCooldown.containsKey(playerId) &&
                currentTime - doorOpenCooldown.get(playerId) < 1500) {
            return;
        }

        VillageReputationData data = VillageReputationData.get(level);
        // FIX CRÍTICO: Usar reputación de LA ALDEA donde está la puerta, no global
        int reputation = data.getReputation(playerId, nearestVillage.get());

        // Detectar si la puerta se está abriendo o cerrando
        boolean doorIsOpen = level.getBlockState(clickedPos).getValue(DoorBlock.OPEN);
        boolean isClosing = doorIsOpen;

        // Hora del día para contexto
        long dayTime = level.getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        boolean isMorning = dayTime >= 0 && dayTime < 6000;

        // FIX: Aumentar radio de detección (de 16 a 20 bloques)
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(20.0D));

        boolean caughtByVillager = false;
        boolean caughtByBaby = false;
        Villager witnessVillager = null;

        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, player, level)) {
                caughtByVillager = true;
                witnessVillager = villager;
                if (villager.isBaby()) {
                    caughtByBaby = true;
                }
                break;
            }
        }

        if (caughtByVillager) {
            doorOpenCooldown.put(playerId, currentTime);

            if (reputation >= 500) {
                // MENSAJES EXPANDIDOS: Héroe/Amigo de confianza
                String[] positiveOpenMessages = caughtByBaby ? new String[] {
                        "§a[Baby Villager] Hi hero! *waves*",
                        "§a[Baby Villager] Come in, you're the best!",
                        "§a[Baby Villager] Welcome! *giggles*",
                        "§a[Baby Villager] Our hero is here!",
                        "§a[Baby Villager] Mom says you're really nice!"
                }
                        : isNight ? new String[] {
                                "§a[Villager] Welcome, friend. Safe travels at night!",
                                "§a[Villager] Come in from the darkness, champion!",
                                "§a[Villager] Our home is your home, even at night!",
                                "§a[Villager] Please, stay safe inside!"
                        }
                        : isMorning ? new String[] {
                                "§a[Villager] Good morning, hero! Come in!",
                                "§a[Villager] Early riser! Please, enter!",
                                "§a[Villager] Fresh day, welcome friend!",
                                "§a[Villager] Morning! Our doors are always open to you!"
                        }
                        : new String[] {
                                "§a[Villager] Welcome, champion!",
                                "§a[Villager] Please, come right in!",
                                "§a[Villager] Our doors are open to you!",
                                "§a[Villager] Feel free to enter, friend!",
                                "§a[Villager] Come in, make yourself at home!",
                                "§a[Villager] You're always welcome here!",
                                "§a[Villager] Ah, our protector arrives!",
                                "§a[Villager] Enter freely, brave one!"
                        };

                String[] positiveCloseMessages = caughtByBaby ? new String[] {
                        "§a[Baby Villager] Thanks! *smiles*",
                        "§a[Baby Villager] Good manners!",
                        "§a[Baby Villager] You're so nice!",
                        "§a[Baby Villager] Mom taught me that too!"
                }
                        : new String[] {
                                "§a[Villager] Thank you for closing it!",
                                "§a[Villager] Appreciate the courtesy, friend!",
                                "§a[Villager] Such good manners!",
                                "§a[Villager] You're so considerate!",
                                "§a[Villager] Thanks, keeps the cold out!",
                                "§a[Villager] Much appreciated, hero!"
                        };

                String[] messages = isClosing ? positiveCloseMessages : positiveOpenMessages;
                player.sendSystemMessage(Component.literal(
                        messages[level.getRandom().nextInt(messages.length)]));

            } else if (reputation >= 100) {
                // MENSAJES EXPANDIDOS: Reputación neutral/buena
                String[] neutralOpenMessages = isNight ? new String[] {
                        "§e[Villager] Come in. Careful, it's dark outside.",
                        "§e[Villager] Go ahead. Watch for mobs.",
                        "§e[Villager] Sure. Don't stay out too long."
                }
                        : new String[] {
                                "§e[Villager] Go ahead.",
                                "§e[Villager] Sure, come in.",
                                "§e[Villager] Alright.",
                                "§e[Villager] Feel free.",
                                "§e[Villager] Yeah, okay.",
                                "§e[Villager] Come in if you need to."
                        };

                String[] neutralCloseMessages = new String[] {
                        "§e[Villager] Thanks.",
                        "§e[Villager] Alright.",
                        "§e[Villager] Appreciated.",
                        "§e[Villager] Good."
                };

                // 50% chance para mensajes neutrales (antes era 33%)
                if (level.getRandom().nextInt(2) == 0) {
                    String[] messages = isClosing ? neutralCloseMessages : neutralOpenMessages;
                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                }

            } else if (reputation >= -99) {
                // NUEVO: Rango de reputación baja pero no criminal
                String[] lowRepMessages = caughtByBaby ? new String[] {
                        "§6[Baby Villager] Umm... I'm watching you...",
                        "§6[Baby Villager] Mommy doesn't trust you...",
                        "§6[Baby Villager] *hides behind door*"
                }
                        : new String[] {
                                "§6[Villager] *watches suspiciously*",
                                "§6[Villager] I've got my eye on you...",
                                "§6[Villager] Don't try anything funny.",
                                "§6[Villager] Make it quick.",
                                "§6[Villager] I'm not happy about this.",
                                "§6[Villager] You better not steal anything..."
                        };

                if (level.getRandom().nextInt(2) == 0) { // 50% chance
                    player.sendSystemMessage(Component.literal(
                            lowRepMessages[level.getRandom().nextInt(lowRepMessages.length)]));
                }

            } else {
                // MENSAJES EXPANDIDOS: Criminal/No bienvenido
                data.addReputation(playerId, nearestVillage.get(), -5);
                int newRep = data.getReputation(playerId, nearestVillage.get());

                String[] negativeMessages = caughtByBaby ? new String[] {
                        "§c[Baby Villager] Stop touching our doors!",
                        "§c[Baby Villager] That's not yours!",
                        "§c[Baby Villager] Mommy! There's a bad person! *cries*",
                        "§c[Baby Villager] Go away! *scared*",
                        "§c[Baby Villager] You're scary!",
                        "§c[Baby Villager] HELP! *runs*"
                }
                        : new String[] {
                                "§c[Villager] Keep your hands off our doors!",
                                "§c[Villager] That's private property!",
                                "§c[Villager] You're not welcome here!",
                                "§c[Villager] Stop entering our homes!",
                                "§c[Villager] Get OUT of here!",
                                "§c[Villager] How DARE you!",
                                "§c[Villager] GUARDS! Intruder!",
                                "§c[Villager] This is OUR home, thief!",
                                "§c[Villager] You have NO right to be here!",
                                "§c[Villager] I should call the Iron Golems!"
                        };

                player.sendSystemMessage(Component.literal(
                        negativeMessages[level.getRandom().nextInt(negativeMessages.length)]));
                player.sendSystemMessage(Component.literal(
                        "§c[Village Diplomacy] Entered uninvited! Reputation -5 (Total: " +
                                newRep + " - " + getReputationStatus(newRep) + ")"));
            }
        }
    }

    @SubscribeEvent
    public void onChestOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof ChestBlock || clickedBlock instanceof BarrelBlock) {
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(level);
                UUID playerId = player.getUUID();
                int reputation = data.getReputation(playerId);

                List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(12.0D));

                boolean caughtByVillager = false;
                boolean caughtByBaby = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, player, level)) {
                        caughtByVillager = true;
                        if (villager.isBaby()) {
                            caughtByBaby = true;
                        }
                        break;
                    }
                }

                if (caughtByVillager) {
                    int oldRep = data.getReputation(player.getUUID());
                    data.addReputation(player.getUUID(), -10);
                    int newRep = data.getReputation(player.getUUID());
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    String message = caughtByBaby
                            ? babyChestMessages[level.getRandom().nextInt(babyChestMessages.length)]
                            : adultChestMessages[level.getRandom().nextInt(adultChestMessages.length)];

                    player.sendSystemMessage(Component.literal(message));
                    player.sendSystemMessage(Component.literal(
                            "§c[Village Diplomacy] Opened village chest! Reputation -10 (Total: " +
                                    newRep + " - " + getReputationStatus(newRep) + ")"));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(nearestVillage.get());
                }
            }
        }
    }

    @SubscribeEvent
    public void onChestClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getContainer() instanceof ChestMenu))
            return;
        if (!(player.level() instanceof ServerLevel level))
            return;

        BlockPos playerPos = player.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, playerPos, 200);

        if (nearestVillage.isPresent()) {
            VillageReputationData data = VillageReputationData.get(level);

            long currentTime = System.currentTimeMillis();
            UUID playerId = player.getUUID();

            if (chestLootCooldown.containsKey(playerId) &&
                    currentTime - chestLootCooldown.get(playerId) < CHEST_LOOT_COOLDOWN_MS) {
                return;
            }

            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(12.0D));

            boolean caughtByVillager = false;
            boolean caughtByBaby = false;

            for (Villager villager : nearbyVillagers) {
                if (hasLineOfSight(villager, player, level)) {
                    caughtByVillager = true;
                    if (villager.isBaby()) {
                        caughtByBaby = true;
                    }
                    break;
                }
            }

            if (caughtByVillager) {
                int oldRep = data.getReputation(player.getUUID());
                data.addReputation(player.getUUID(), -15);
                int newRep = data.getReputation(player.getUUID());
                checkAndNotifyReputationChange(player, oldRep, newRep);

                String message = caughtByBaby ? babyLootMessages[level.getRandom().nextInt(babyLootMessages.length)]
                        : adultLootMessages[level.getRandom().nextInt(adultLootMessages.length)];

                player.sendSystemMessage(Component.literal(message));
                player.sendSystemMessage(Component.literal(
                        "§c[Village Diplomacy] You stole from the village! Reputation -15 (Total: " +
                                newRep + " - " + getReputationStatus(newRep) + ")"));

                chestLootCooldown.put(playerId, currentTime);

                VillageRelationshipData relationData = VillageRelationshipData.get(level);
                relationData.registerVillage(nearestVillage.get());
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;
        if (player.level().isClientSide)
            return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos brokenPos = event.getPos();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, brokenPos, 200);

        if (nearestVillage.isPresent()) {
            VillageReputationData data = VillageReputationData.get(level);

            Block brokenBlock = event.getState().getBlock();

            BlockType blockType = categorizeBlock(brokenBlock, level, brokenPos);

            if (blockType != BlockType.NONE) {
                List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                        Villager.class,
                        AABB.ofSize(Vec3.atCenterOf(brokenPos), 48, 48, 48));

                boolean caughtByVillager = false;
                boolean caughtByBaby = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, player, level)) {
                        caughtByVillager = true;
                        if (villager.isBaby()) {
                            caughtByBaby = true;
                        }
                        break;
                    }
                }

                if (caughtByVillager) {
                    int penalty = blockType.penalty;
                    int oldRep = data.getReputation(player.getUUID());
                    data.addReputation(player.getUUID(), penalty);
                    int newRep = data.getReputation(player.getUUID());
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    String villagerMessage = getBlockBreakMessage(blockType, caughtByBaby, level);

                    player.sendSystemMessage(Component.literal(villagerMessage));
                    player.sendSystemMessage(Component.literal(
                            "§c[Village Diplomacy] " + blockType.systemMessage + " Reputation " + penalty +
                                    " (Total: " + newRep + " - " + getReputationStatus(newRep) + ")"));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(nearestVillage.get());
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (player.level().isClientSide)
            return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos placedPos = event.getPos();
        Block placedBlock = event.getPlacedBlock().getBlock();
        
        // Mensajes contextuales cuando colocas bloques en la aldea
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, placedPos, 200);
        if (nearestVillage.isPresent()) {
            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    AABB.ofSize(Vec3.atCenterOf(placedPos), 32, 32, 32));

            boolean caughtByVillager = false;
            for (Villager villager : nearbyVillagers) {
                if (hasLineOfSight(villager, player, level)) {
                    caughtByVillager = true;
                    break;
                }
            }

            if (caughtByVillager) {
                String[] messages = null;
                
                // Obtener reputación del jugador para mensajes contextuales
                VillageReputationData data = VillageReputationData.get(level);
                int reputation = data.getReputation(player.getUUID(), nearestVillage.get());
                boolean isWelcome = reputation >= 100; // FRIENDLY o mejor
                boolean isNeutral = reputation >= 0 && reputation < 100;
                boolean isUnwelcome = reputation < 0;
                
                if (placedBlock instanceof BedBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] Make yourself at home, friend!",
                            "§a[Villager] Feel free to rest here anytime.",
                            "§a[Villager] Your own bed! You're really part of the village now.",
                            "§a[Villager] Welcome home!",
                            "§7[Villager] *smiles warmly* That's a nice spot.",
                            "§a[Villager] It's good to have you living with us!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] Making yourself at home?",
                            "§e[Villager] Ah, claiming a bed I see...",
                            "§e[Villager] Planning to stay a while?",
                            "§7[Villager] *nods* That's a good spot for a bed.",
                            "§e[Villager] Settling in?",
                            "§e[Villager] You're building here now?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] What are you doing?!",
                            "§c[Villager] Hey! You're not welcome to build here!",
                            "§7[Villager] *glares suspiciously*",
                            "§c[Villager] We don't want you living here!",
                            "§c[Villager] You think you can just move in?!",
                            "§c[Villager] Get that out of our village!"
                        };
                    }
                } else if (placedBlock instanceof ChestBlock || placedBlock instanceof BarrelBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] Setting up storage? Smart!",
                            "§a[Villager] Keep your valuables safe, friend.",
                            "§7[Villager] That's a perfect spot for a chest.",
                            "§a[Villager] You're really making this your home!",
                            "§a[Villager] Need help organizing your things?",
                            "§a[Villager] A chest! You're staying long-term then?"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] Storing your belongings here?",
                            "§e[Villager] Ah, setting up storage...",
                            "§7[Villager] That's a good spot for a chest.",
                            "§e[Villager] Making this your home?",
                            "§e[Villager] You're really settling in...",
                            "§e[Villager] Planning to stay?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] What's in that chest?!",
                            "§c[Villager] We don't want YOUR stuff here!",
                            "§c[Villager] Taking over our village, are you?!",
                            "§7[Villager] *watches suspiciously*",
                            "§c[Villager] Don't think we won't check that!",
                            "§c[Villager] You're up to something..."
                        };
                    }
                } else if (placedBlock instanceof FurnaceBlock || placedBlock instanceof BlastFurnaceBlock || placedBlock instanceof SmokerBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] A furnace! We could use more industry!",
                            "§a[Villager] Excellent! A new workshop!",
                            "§a[Villager] You're contributing to the village economy!",
                            "§7[Villager] That'll come in handy for everyone.",
                            "§a[Villager] Good thinking! We need more crafters.",
                            "§a[Villager] You're really helping the village grow!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] Setting up a furnace?",
                            "§e[Villager] Oh, you're going to do some smelting!",
                            "§e[Villager] Planning to craft here?",
                            "§7[Villager] That'll come in handy.",
                            "§e[Villager] Building your own workshop?",
                            "§e[Villager] Going to make something?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] What are you building?!",
                            "§c[Villager] We don't need YOUR furnaces!",
                            "§c[Villager] Stop cluttering our village!",
                            "§7[Villager] *frowns deeply*",
                            "§c[Villager] You're not a blacksmith here!",
                            "§c[Villager] Take that somewhere else!"
                        };
                    }
                } else if (placedBlock instanceof CraftingTableBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] A crafting table! Perfect!",
                            "§a[Villager] Oh, setting up a workshop! Great idea!",
                            "§a[Villager] You're a true artisan!",
                            "§7[Villager] We appreciate skilled crafters.",
                            "§a[Villager] The village benefits from your skills!",
                            "§a[Villager] You're becoming quite the craftsman!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A crafting table! Very useful.",
                            "§e[Villager] Oh, setting up a workshop!",
                            "§e[Villager] You're really making yourself at home.",
                            "§7[Villager] Smart thinking.",
                            "§e[Villager] Planning to make something?",
                            "§e[Villager] Going to craft here?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] We have our OWN crafting tables!",
                            "§c[Villager] You're not part of this village!",
                            "§c[Villager] Stop building in our territory!",
                            "§7[Villager] *crosses arms*",
                            "§c[Villager] You're not welcome to craft here!",
                            "§c[Villager] Take that table elsewhere!"
                        };
                    }
                } else if (placedBlock instanceof BellBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] A bell! That'll help coordinate everyone!",
                            "§a[Villager] Excellent! Now we can signal each other!",
                            "§a[Villager] You're thinking like a true villager!",
                            "§7[Villager] *impressed* That's very thoughtful.",
                            "§a[Villager] A bell for the community! Thank you!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A bell? Interesting...",
                            "§7[Villager] That's... unusual.",
                            "§e[Villager] Planning to call meetings?",
                            "§e[Villager] A bell! That could be useful.",
                            "§7[Villager] *looks curiously*"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Don't you DARE ring that!",
                            "§c[Villager] We already HAVE a bell!",
                            "§c[Villager] You're not in charge here!",
                            "§7[Villager] *glares angrily*",
                            "§c[Villager] That's OUR gathering symbol!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Villager] A brewing stand! You're an alchemist!",
                            "§a[Villager] Oh! You know potion-making?",
                            "§a[Villager] We could use someone with your skills!",
                            "§7[Villager] *very impressed*",
                            "§a[Villager] An alchemist in our village! Wonderful!",
                            "§a[Villager] Your potions will benefit everyone!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A brewing stand? Fancy!",
                            "§e[Villager] Oh! Going to make potions?",
                            "§e[Villager] You know alchemy?",
                            "§7[Villager] *impressed*",
                            "§e[Villager] That's quite advanced!",
                            "§e[Villager] An alchemist, eh?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] What potions are you brewing?!",
                            "§c[Villager] We don't trust your alchemy!",
                            "§c[Villager] Probably making POISON!",
                            "§7[Villager] *backs away nervously*",
                            "§c[Villager] Keep your suspicious potions away from us!",
                            "§c[Villager] You're up to no good!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.EnchantmentTableBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§d[Villager] An enchanting table! Incredible!",
                            "§d[Villager] You practice magic! Amazing!",
                            "§a[Villager] This will make the village so much stronger!",
                            "§7[Villager] *stares in awe at the magical runes*",
                            "§d[Villager] A true mage among us! We're honored!",
                            "§a[Villager] Your magical expertise will protect us all!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§d[Villager] An enchanting table! Wow!",
                            "§e[Villager] You know magic?",
                            "§7[Villager] *looks at the floating book with wonder*",
                            "§e[Villager] That's... that's real magic!",
                            "§d[Villager] I've never seen one up close!",
                            "§e[Villager] Are you a wizard?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Dark magic! I knew you were trouble!",
                            "§c[Villager] We don't want witchcraft in our village!",
                            "§c[Villager] That table gives me a bad feeling...",
                            "§7[Villager] *makes warding gesture*",
                            "§c[Villager] You're cursing our land!",
                            "§c[Villager] Take your dark arts elsewhere!"
                        };
                    }
                } else if (placedBlock == Blocks.BOOKSHELF) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§6[Villager] Books! You're building a library!",
                            "§a[Villager] Knowledge is precious! Thank you!",
                            "§7[Villager] The children will love having books to read.",
                            "§6[Villager] A scholar! We're lucky to have you.",
                            "§a[Villager] You're enriching our culture!",
                            "§6[Villager] A bookshelf! You truly are civilized."
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A bookshelf! You like to read?",
                            "§7[Villager] Books are valuable around here.",
                            "§e[Villager] Building a personal library?",
                            "§6[Villager] *examines the books curiously*",
                            "§e[Villager] You collect knowledge?",
                            "§7[Villager] That's quite scholarly."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Stealing our knowledge traditions!",
                            "§c[Villager] Those books should be in OUR library!",
                            "§c[Villager] You don't deserve our wisdom!",
                            "§7[Villager] *frowns at the bookshelf*",
                            "§c[Villager] Trying to look smart, are you?",
                            "§c[Villager] We know you can't even read!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.LecternBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§6[Villager] A lectern! For reading and study!",
                            "§a[Villager] You're setting up a proper study!",
                            "§7[Villager] The mark of a true scholar.",
                            "§6[Villager] Will you share your knowledge with us?",
                            "§a[Villager] A lectern shows dedication to learning!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A lectern! Planning to read?",
                            "§7[Villager] That's for displaying books, right?",
                            "§e[Villager] Setting up a study area?",
                            "§6[Villager] *nods approvingly*",
                            "§e[Villager] You value knowledge, I see."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Who do you think you are, a teacher?!",
                            "§c[Villager] We don't need YOUR lectures!",
                            "§c[Villager] Pretending to be educated...",
                            "§7[Villager] *scoffs*",
                            "§c[Villager] You can't teach us anything!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.AnvilBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§8[Villager] An anvil! A proper smithy!",
                            "§a[Villager] You're a blacksmith! Excellent!",
                            "§7[Villager] We always need skilled metalworkers.",
                            "§8[Villager] *nods with respect*",
                            "§a[Villager] Your smithing will serve the village well!",
                            "§8[Villager] A master craftsman among us!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] An anvil! Are you a smith?",
                            "§7[Villager] That's heavy-duty equipment.",
                            "§e[Villager] Setting up a smithy?",
                            "§8[Villager] You work metal?",
                            "§e[Villager] Planning to repair tools?",
                            "§7[Villager] That'll come in handy."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Making weapons against us?!",
                            "§c[Villager] We don't trust you with an anvil!",
                            "§c[Villager] You'll forge weapons to attack us!",
                            "§7[Villager] *eyes the anvil suspiciously*",
                            "§c[Villager] Take your smithy elsewhere!",
                            "§c[Villager] No weapons in OUR village!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.GrindstoneBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§7[Villager] A grindstone! Very practical!",
                            "§a[Villager] You think of everything!",
                            "§7[Villager] We can all use that for repairs.",
                            "§a[Villager] Community tools are always welcome!",
                            "§7[Villager] *appreciates the utility*"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A grindstone! Useful.",
                            "§7[Villager] For sharpening and repairs?",
                            "§e[Villager] That's practical.",
                            "§7[Villager] Good for maintaining tools.",
                            "§e[Villager] Setting up a repair station?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Sharpening weapons, I bet!",
                            "§c[Villager] We know what you're planning!",
                            "§c[Villager] That's for making sharper swords!",
                            "§7[Villager] *watches warily*",
                            "§c[Villager] Preparing for violence..."
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.LoomBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§d[Villager] A loom! You're a weaver!",
                            "§a[Villager] The village needs more artisans!",
                            "§7[Villager] Beautiful tapestries await!",
                            "§d[Villager] You'll make wonderful banners!",
                            "§a[Villager] A true artist among us!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A loom! You make banners?",
                            "§7[Villager] For weaving patterns?",
                            "§e[Villager] That's creative.",
                            "§d[Villager] You're artistic?",
                            "§e[Villager] Planning to make decorations?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Making YOUR banners in OUR village?!",
                            "§c[Villager] We don't want your symbols here!",
                            "§c[Villager] Trying to mark this as YOUR territory!",
                            "§7[Villager] *disapproves strongly*",
                            "§c[Villager] Remove that loom!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.ComposterBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§2[Villager] A composter! You're farming!",
                            "§a[Villager] Excellent! We need more farmers!",
                            "§7[Villager] You'll help our crops grow!",
                            "§2[Villager] Contributing to our agriculture!",
                            "§a[Villager] The fields will prosper!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A composter! Planning to farm?",
                            "§7[Villager] For making bone meal?",
                            "§2[Villager] You're growing crops?",
                            "§e[Villager] That's useful for farming.",
                            "§7[Villager] Good for the gardens."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] Stealing our farming methods!",
                            "§c[Villager] That's for OUR crops, not yours!",
                            "§c[Villager] You'll ruin the soil!",
                            "§7[Villager] *protective of the farmland*",
                            "§c[Villager] Leave our agriculture alone!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.CauldronBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§b[Villager] A cauldron! Very useful!",
                            "§a[Villager] We can all use that!",
                            "§7[Villager] For water storage and washing.",
                            "§b[Villager] Practical thinking!",
                            "§a[Villager] The village benefits from this!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Villager] A cauldron! For water?",
                            "§7[Villager] That's handy to have around.",
                            "§b[Villager] Multi-purpose tool.",
                            "§e[Villager] For potions or washing?",
                            "§7[Villager] Practical."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Villager] What are you brewing in there?!",
                            "§c[Villager] Probably making poison!",
                            "§c[Villager] Witch! Witch!",
                            "§7[Villager] *backs away from the cauldron*",
                            "§c[Villager] No dark magic here!"
                        };
                    }
                }
                
                if (messages != null) {
                    player.sendSystemMessage(Component.literal(
                        messages[level.getRandom().nextInt(messages.length)]));
                    
                    // PENALIZACIÓN si tienes mala reputación
                    if (isUnwelcome) {
                        int oldRep = data.getReputation(player.getUUID(), nearestVillage.get());
                        data.addReputation(player.getUUID(), nearestVillage.get(), -5);
                        int newRep = data.getReputation(player.getUUID(), nearestVillage.get());
                        checkAndNotifyReputationChange(player, oldRep, newRep);
                        
                        player.sendSystemMessage(Component.literal(
                            "§c[Village Diplomacy] Building in village with bad reputation! -5 (Total: " +
                                    newRep + " - " + getReputationStatus(newRep) + ")"));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreakInVillage(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        
        BlockPos blockPos = event.getPos();
        Block block = event.getState().getBlock();
        
        // Check if in village
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, blockPos, 100);
        if (nearestVillage.isEmpty()) return;
        
        // Village structure blocks
        boolean isVillageBlock = 
            block == Blocks.COBBLESTONE ||
            block == Blocks.STONE_BRICKS ||
            block == Blocks.OAK_PLANKS ||
            block == Blocks.SPRUCE_PLANKS ||
            block == Blocks.BIRCH_PLANKS ||
            block == Blocks.OAK_LOG ||
            block == Blocks.SPRUCE_LOG ||
            block == Blocks.GLASS_PANE ||
            block == Blocks.GLASS ||
            block == Blocks.TORCH ||
            block == Blocks.LANTERN;
        
        if (!isVillageBlock) return;
        
        // Penalize
        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, -10);
        int newRep = data.getReputation(player.getUUID(), villagePos);
        checkAndNotifyReputationChange(player, oldRep, newRep);
        
        // Penalización silenciosa para bloques comunes (sin mensajes spam)
        // Los aldeanos no se quejan por cada bloque individual
    }

    @SubscribeEvent
    public void onBedSleep(PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(player.level() instanceof ServerLevel level))
            return;

        BlockPos bedPos = event.getPos();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, bedPos, 200);

        if (nearestVillage.isPresent()) {
            // BLOQUEO DE CAMAS CON MALA REPUTACIÓN
            VillageReputationData data = VillageReputationData.get(level);
            int reputation = data.getReputation(player.getUUID(), nearestVillage.get());
            
            if (reputation < -100) {
                // Cancelar el intento de dormir
                event.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
                
                String[] denialMessages = reputation < -500 ? new String[] {
                        "§4[Villager] Get OUT! You're not welcome here!",
                        "§4[Villager] A criminal in OUR beds?! NEVER!",
                        "§4[Villager] Guards! Remove this intruder!",
                        "§4[Villager] You have NO right to rest here!"
                } : new String[] {
                        "§c[Villager] You're not welcome to use our beds.",
                        "§c[Villager] Find somewhere else to sleep, outsider.",
                        "§c[Villager] These beds are for villagers only.",
                        "§c[Villager] Your reputation doesn't grant you this privilege."
                };
                
                player.sendSystemMessage(Component.literal(
                        denialMessages[level.getRandom().nextInt(denialMessages.length)]));
                player.sendSystemMessage(Component.literal(
                        "§c✗ Denied! Your reputation is too low to use village beds."));
                
                return; // Salir sin procesar más
            }
        }

        // Only penalize if player actually sleeps, not just right-click
        if (event.getResultStatus() != null && event.getResultStatus() != net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_POSSIBLE_HERE) {
            return; // Player can't sleep here, don't penalize
        }

        if (nearestVillage.isPresent()) {
            long currentTime = System.currentTimeMillis();
            UUID playerId = player.getUUID();

            if (bedUsageCooldown.containsKey(playerId) &&
                    currentTime - bedUsageCooldown.get(playerId) < BED_COOLDOWN_MS) {
                return;
            }

            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    player.getBoundingBox().inflate(16.0D));

            boolean caughtByVillager = false;
            boolean caughtByBaby = false;

            for (Villager villager : nearbyVillagers) {
                if (hasLineOfSight(villager, player, level)) {
                    caughtByVillager = true;
                    if (villager.isBaby()) {
                        caughtByBaby = true;
                    }
                    break;
                }
            }

            if (caughtByVillager) {
                VillageReputationData data = VillageReputationData.get(level);
                int oldRep = data.getReputation(player.getUUID());
                data.addReputation(player.getUUID(), -20);
                int newRep = data.getReputation(player.getUUID());
                checkAndNotifyReputationChange(player, oldRep, newRep);

                String[] adultMessages = {
                        "§c[Villager] HEY! That's MY bed!",
                        "§c[Villager] Get out of my bed, you creep!",
                        "§c[Villager] Have you no shame?!",
                        "§c[Villager] That's private property!",
                        "§c[Villager] *angrily* OUT!"
                };

                String[] babyMessages = {
                        "§c[Baby Villager] That's where I sleep! *cries*",
                        "§c[Baby Villager] Noooo! My bed!",
                        "§c[Baby Villager] Mommy! A stranger is in my bed!"
                };

                String message = caughtByBaby ? babyMessages[level.getRandom().nextInt(babyMessages.length)]
                        : adultMessages[level.getRandom().nextInt(adultMessages.length)];

                player.sendSystemMessage(Component.literal(message));
                player.sendSystemMessage(Component.literal(
                        "§c[Village Diplomacy] Used a villager's bed! Reputation -20 (Total: " +
                                newRep + " - " + getReputationStatus(newRep) + ")"));

                bedUsageCooldown.put(playerId, currentTime);

                VillageRelationshipData relationData = VillageRelationshipData.get(level);
                relationData.registerVillage(nearestVillage.get());
            }
        }
    }

    @SubscribeEvent
    public void onBellRing(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof BellBlock) {
            // Verificar que realmente tocó la campana (no el aire alrededor)
            if (event.getHitVec().getLocation().y < clickedPos.getY() + 0.1) {
                return; // Click en la base, no en la campana
            }
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(level);
                UUID playerId = player.getUUID();
                int reputation = data.getReputation(playerId);
                
                // Bloquear campana si reputación es muy baja
                if (reputation < -200) {
                    event.setCanceled(true);
                    
                    String[] denialMessages;
                    if (reputation < -500) {
                        denialMessages = new String[]{
                            "§4[Villager] You have NO right to ring our bell!",
                            "§4[Villager] Get away from there, criminal!",
                            "§c[Villager] That bell is not for the likes of you!"
                        };
                    } else {
                        denialMessages = new String[]{
                            "§c[Villager] We can't trust you with the village bell.",
                            "§c[Villager] Earn our trust first.",
                            "§e[Villager] The bell is for villagers only."
                        };
                    }
                    
                    player.sendSystemMessage(Component.literal(
                        denialMessages[level.getRandom().nextInt(denialMessages.length)]));
                    return;
                }
                
                long currentTime = System.currentTimeMillis();

                if (bellRingCooldown.containsKey(playerId) &&
                        currentTime - bellRingCooldown.get(playerId) < BELL_COOLDOWN_MS) {
                    return;
                }

                List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(20.0D));

                boolean caughtByVillager = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, player, level)) {
                        caughtByVillager = true;
                        break;
                    }
                }

                if (caughtByVillager) {
                    // Reutilizar las variables ya definidas arriba
                    if (reputation >= 500) {
                        String[] positiveMessages = {
                                "§a[Villager] Gathering everyone, champion!",
                                "§a[Villager] Calling the village for you!",
                                "§a[Villager] *nods approvingly*"
                        };
                        player.sendSystemMessage(Component.literal(
                                positiveMessages[level.getRandom().nextInt(positiveMessages.length)]));
                    } else if (reputation < 100) {
                        data.addReputation(playerId, -15);
                        int newRep = data.getReputation(playerId);

                        String[] messages = {
                                "§c[Villager] Stop ringing the bell!",
                                "§c[Villager] That's for emergencies only!",
                                "§c[Villager] Are you trying to cause panic?!",
                                "§c[Villager] This isn't funny!",
                                "§c[Villager] *covers ears* STOP IT!",
                                "§c[Villager] You're abusing our emergency system!",
                                "§c[Villager] The guards will hear about this!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Village Diplomacy] Rang the village bell! Reputation -15 (Total: " +
                                        newRep + " - " + getReputationStatus(newRep) + ")"));
                    }

                    bellRingCooldown.put(playerId, currentTime);
                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(nearestVillage.get());
                }
            }
        }
    }

    @SubscribeEvent
    public void onTrapdoorOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();

        if (level.getBlockState(clickedPos).getBlock() instanceof TrapDoorBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                boolean isFarmTrapdoor = false;

                for (int x = -3; x <= 3; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -3; z <= 3; z++) {
                            BlockPos checkPos = clickedPos.offset(x, y, z);
                            Block block = level.getBlockState(checkPos).getBlock();

                            if (block instanceof CropBlock ||
                                    block instanceof CarrotBlock ||
                                    block instanceof PotatoBlock ||
                                    block instanceof BeetrootBlock) {
                                isFarmTrapdoor = true;
                                break;
                            }

                            AABB animalBox = new AABB(
                                    checkPos.getX(), checkPos.getY(), checkPos.getZ(),
                                    checkPos.getX() + 1, checkPos.getY() + 1, checkPos.getZ() + 1).inflate(1.0);

                            List<net.minecraft.world.entity.animal.Animal> animals = level
                                    .getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, animalBox);

                            if (!animals.isEmpty()) {
                                isFarmTrapdoor = true;
                                break;
                            }
                        }
                        if (isFarmTrapdoor)
                            break;
                    }
                    if (isFarmTrapdoor)
                        break;
                }

                if (isFarmTrapdoor) {
                    long currentTime = System.currentTimeMillis();
                    UUID playerId = player.getUUID();

                    if (trapdoorCooldown.containsKey(playerId) &&
                            currentTime - trapdoorCooldown.get(playerId) < TRAPDOOR_COOLDOWN_MS) {
                        return;
                    }

                    List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                            Villager.class,
                            player.getBoundingBox().inflate(12.0D));

                    boolean caughtByVillager = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, player, level)) {
                            caughtByVillager = true;
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(level);
                        int oldRep = data.getReputation(player.getUUID());
                        data.addReputation(player.getUUID(), -10);
                        int newRep = data.getReputation(player.getUUID());
                        checkAndNotifyReputationChange(player, oldRep, newRep);

                        String[] messages = {
                                "§c[Villager] HEY! Don't let the animals out!",
                                "§c[Villager] That's our farm! Stay away!",
                                "§c[Villager] What are you doing?!",
                                "§c[Villager] Leave our crops alone!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Village Diplomacy] Opened farm trapdoor! Reputation -10 (Total: " +
                                        newRep + " - " + getReputationStatus(newRep) + ")"));

                        trapdoorCooldown.put(playerId, currentTime);

                        VillageRelationshipData relationData = VillageRelationshipData.get(level);
                        relationData.registerVillage(nearestVillage.get());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onCraftingTableUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();
        
        // Detectar uso de hornos, mesas de trabajo y brewing stands
        if (clickedBlock instanceof CraftingTableBlock || clickedBlock instanceof FurnaceBlock || 
            clickedBlock instanceof BlastFurnaceBlock || clickedBlock instanceof SmokerBlock ||
            clickedBlock instanceof net.minecraft.world.level.block.BrewingStandBlock) {
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);
            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(level);
                UUID playerId = player.getUUID();
                int reputation = data.getReputation(playerId);
                
                // Penalización por usar bloques de trabajo de la aldea
                List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(16.0D));

                boolean caughtByVillager = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, player, level)) {
                        caughtByVillager = true;
                        break;
                    }
                }

                if (caughtByVillager) {
                    data.addReputation(playerId, -8);
                    int newRep = data.getReputation(playerId);

                    String blockName = "workstation";
                    if (clickedBlock instanceof FurnaceBlock || clickedBlock instanceof BlastFurnaceBlock || clickedBlock instanceof SmokerBlock) {
                        blockName = "furnace";
                    } else if (clickedBlock instanceof CraftingTableBlock) {
                        blockName = "crafting table";
                    } else if (clickedBlock instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                        blockName = "brewing stand";
                    }

                    String[] messages = {
                            "§c[Villager] Hey! That's our " + blockName + "!",
                            "§c[Villager] Don't touch our equipment!",
                            "§c[Villager] You're using OUR resources!",
                            "§c[Villager] That's village property!",
                            "§c[Villager] Stop using our tools!"
                    };

                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                    player.sendSystemMessage(Component.literal(
                            "§c[Village Diplomacy] Used village " + blockName + "! Reputation -8 (Total: " +
                                    newRep + " - " + getReputationStatus(newRep) + ")"));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(nearestVillage.get());
                }
            }
        }

        if (level.getBlockState(clickedPos).getBlock() instanceof CraftingTableBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                boolean isInHouse = false;

                for (int y = -2; y <= 4; y++) {
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 4; z++) {
                            BlockPos checkPos = clickedPos.offset(x, y, z);
                            if (level.getBlockState(checkPos).getBlock() instanceof BedBlock) {
                                isInHouse = true;
                                break;
                            }
                        }
                    }
                }

                if (isInHouse) {
                    long currentTime = System.currentTimeMillis();
                    UUID playerId = player.getUUID();

                    if (craftingTableCooldown.containsKey(playerId) &&
                            currentTime - craftingTableCooldown.get(playerId) < CRAFTING_COOLDOWN_MS) {
                        return;
                    }

                    List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                            Villager.class,
                            player.getBoundingBox().inflate(10.0D));

                    boolean caughtByVillager = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, player, level)) {
                            caughtByVillager = true;
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(level);
                        int oldRep = data.getReputation(player.getUUID());
                        data.addReputation(player.getUUID(), -8);
                        int newRep = data.getReputation(player.getUUID());
                        checkAndNotifyReputationChange(player, oldRep, newRep);

                        String[] messages = {
                                "§c[Villager] That's MY crafting table!",
                                "§c[Villager] Make your own tools!",
                                "§c[Villager] Hey! Don't use my stuff!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Village Diplomacy] Used villager's crafting table! Reputation -8 (Total: " +
                                        newRep + " - " + getReputationStatus(newRep) + ")"));

                        craftingTableCooldown.put(playerId, currentTime);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onFenceGateOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos clickedPos = event.getPos();
        Block clickedBlock = level.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof FenceGateBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                long currentTime = System.currentTimeMillis();
                UUID playerId = player.getUUID();

                if (fenceGateCooldown.containsKey(playerId) &&
                        currentTime - fenceGateCooldown.get(playerId) < FENCE_GATE_COOLDOWN_MS) {
                    return;
                }

                AABB animalCheckBox = new AABB(
                        clickedPos.getX() - 4, clickedPos.getY() - 1, clickedPos.getZ() - 4,
                        clickedPos.getX() + 5, clickedPos.getY() + 2, clickedPos.getZ() + 5);

                List<net.minecraft.world.entity.animal.Animal> nearbyAnimals = level
                        .getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, animalCheckBox);

                if (!nearbyAnimals.isEmpty()) {
                    List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                            Villager.class,
                            player.getBoundingBox().inflate(15.0D));

                    boolean caughtByVillager = false;
                    boolean caughtByBaby = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, player, level)) {
                            caughtByVillager = true;
                            if (villager.isBaby()) {
                                caughtByBaby = true;
                            }
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(level);
                        int oldRep = data.getReputation(player.getUUID());
                        data.addReputation(player.getUUID(), -12);
                        int newRep = data.getReputation(player.getUUID());
                        checkAndNotifyReputationChange(player, oldRep, newRep);

                        String[] adultMessages = {
                                "§c[Villager] HEY! You're letting the animals out!",
                                "§c[Villager] NO! Close that gate!",
                                "§c[Villager] The livestock will escape!",
                                "§c[Villager] What are you doing?! Those are OUR animals!",
                                "§c[Villager] STOP! It took forever to get them in there!",
                                "§c[Villager] You're releasing our livelihood!",
                                "§c[Villager] Those animals feed the whole village!",
                                "§c[Villager] Are you trying to sabotage us?!",
                                "§c[Villager] Close that gate NOW!",
                                "§c[Villager] They're escaping! Someone help!"
                        };

                        String[] babyMessages = {
                                "§c[Baby Villager] Nooo! The animals are getting out!",
                                "§c[Baby Villager] *cries* Stop them!",
                                "§c[Baby Villager] My pet cow! She's running away!",
                                "§c[Baby Villager] Why would you do that?!",
                                "§c[Baby Villager] Daddy's gonna be so mad!",
                                "§c[Baby Villager] Bad! Bad person!",
                                "§c[Baby Villager] *sobs* They're escaping!"
                        };

                        String message = caughtByBaby ? babyMessages[level.getRandom().nextInt(babyMessages.length)]
                                : adultMessages[level.getRandom().nextInt(adultMessages.length)];

                        player.sendSystemMessage(Component.literal(message));
                        player.sendSystemMessage(Component.literal(
                                "§c[Village Diplomacy] Released village animals! Reputation -12 (Total: " +
                                        newRep + " - " + getReputationStatus(newRep) + ")"));

                        fenceGateCooldown.put(playerId, currentTime);

                        VillageRelationshipData relationData = VillageRelationshipData.get(level);
                        relationData.registerVillage(nearestVillage.get());
                    }
                }
            }
        }
    }

    private void processStrikeSystem(ServerPlayer player, ServerLevel level, BlockPos villagerPos) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                AABB.ofSize(Vec3.atCenterOf(villagerPos), 48, 48, 48),
                golem -> !golem.isPlayerCreated());

        if (nearbyGolems.isEmpty())
            return;
        
        // PRIMERO: Verificar si algún golem ya está atacando al jugador
        boolean golemAlreadyAngry = false;
        IronGolem angryGolem = null;
        
        for (IronGolem golem : nearbyGolems) {
            LivingEntity target = golem.getTarget();
            UUID angerTarget = golem.getPersistentAngerTarget();
            
            if ((target != null && target.getUUID().equals(playerId)) ||
                (angerTarget != null && angerTarget.equals(playerId))) {
                golemAlreadyAngry = true;
                angryGolem = golem;
                break;
            }
        }
        
        // Si el golem ya está atacando, mostrar mensajes VIOLENTOS en vez de strikes
        if (golemAlreadyAngry) {
            String golemName = "Iron Golem";
            GolemPersonalityData personalityData = GolemPersonalityData.get(level);
            GolemPersonality personality = personalityData.getPersonality(angryGolem.getUUID());
            if (personality != null) {
                golemName = personality.getName();
            } else if (angryGolem.hasCustomName()) {
                golemName = angryGolem.getCustomName().getString();
            }
            
            String[] violentMessages = {
                "§c[" + golemName + "] YOU DARE ATTACK MORE VILLAGERS?!",
                "§4[" + golemName + "] *FURIOUS STOMPING* STOP THIS NOW!",
                "§c[" + golemName + "] YOU'RE MAKING IT WORSE FOR YOURSELF!",
                "§4[" + golemName + "] I'LL CRUSH YOU FOR THIS!",
                "§c[" + golemName + "] *ROARS* ENOUGH OF YOUR VIOLENCE!",
                "§4[" + golemName + "] EVERY HIT SEALS YOUR DOOM!",
                "§c[" + golemName + "] YOU'LL PAY FOR EVERY VILLAGER YOU HURT!",
                "§4[" + golemName + "] *POUNDS FISTS* THIS ENDS NOW!"
            };
            
            if (level.getRandom().nextInt(2) == 0) { // 50% chance para no spamear
                player.sendSystemMessage(Component.literal(
                    violentMessages[level.getRandom().nextInt(violentMessages.length)]));
            }
            return; // No procesar sistema de strikes
        }

        // Sistema de strikes normal solo si el golem NO está atacando
        List<Long> strikes = villagerAttackTimes.getOrDefault(playerId, new ArrayList<>());
        strikes.removeIf(time -> currentTime - time > STRIKE_WINDOW_MS);
        strikes.add(currentTime);
        villagerAttackTimes.put(playerId, strikes);

        int strikeCount = strikes.size();
        
        // Obtener nombre del golem más cercano
        IronGolem closestGolem = nearbyGolems.get(0);
        String golemName = "Iron Golem";
        
        // Intentar obtener personalidad del golem
        GolemPersonalityData personalityData = GolemPersonalityData.get(level);
        GolemPersonality personality = personalityData.getPersonality(closestGolem.getUUID());
        if (personality != null) {
            golemName = personality.getName();
        } else if (closestGolem.hasCustomName()) {
            golemName = closestGolem.getCustomName().getString();
        }

        if (strikeCount == 1) {
            String[] messages = {
                "§e[" + golemName + "] Hey! Stop that.",
                "§e[" + golemName + "] Don't touch them.",
                "§e[" + golemName + "] I'm watching you...",
                "§e[" + golemName + "] Leave them alone.",
                "§e[" + golemName + "] That's enough.",
                "§e[" + golemName + "] Back away from them.",
                "§e[" + golemName + "] Don't make me come over there.",
                "§e[" + golemName + "] You don't want trouble.",
                "§e[" + golemName + "] These villagers are under MY protection.",
                "§e[" + golemName + "] Keep your hands to yourself."
            };
            player.sendSystemMessage(Component.literal(
                    messages[level.getRandom().nextInt(messages.length)]));
        } else if (strikeCount == 2) {
            String[] messages = {
                "§6[" + golemName + "] I said STOP!",
                "§6[" + golemName + "] You're pushing your luck...",
                "§6[" + golemName + "] Back off NOW!",
                "§6[" + golemName + "] Final warning!",
                "§6[" + golemName + "] You don't want to test me!",
                "§6[" + golemName + "] This is your LAST chance!",
                "§6[" + golemName + "] I'm losing my patience!",
                "§6[" + golemName + "] Step away or face the consequences!",
                "§6[" + golemName + "] You're making a BIG mistake!",
                "§6[" + golemName + "] One more hit and you're DONE!"
            };
            player.sendSystemMessage(Component.literal(
                    messages[level.getRandom().nextInt(messages.length)]));
        } else if (strikeCount >= STRIKES_REQUIRED) {
            String[] messages = {
                "§c[" + golemName + "] THAT'S IT!",
                "§c[" + golemName + "] You've crossed the line!",
                "§c[" + golemName + "] NOW YOU'VE DONE IT!",
                "§c[" + golemName + "] PREPARE YOURSELF!",
                "§c[" + golemName + "] I'VE HAD ENOUGH!",
                "§c[" + golemName + "] TIME TO PAY!",
                "§c[" + golemName + "] YOU'RE FINISHED!",
                "§c[" + golemName + "] NO MORE MERCY!",
                "§c[" + golemName + "] FACE MY WRATH!",
                "§c[" + golemName + "] YOU'VE SEALED YOUR FATE!"
            };
            player.sendSystemMessage(Component.literal(
                    messages[level.getRandom().nextInt(messages.length)]));

            crimeCommittedTime.put(playerId, currentTime + MINOR_CRIME_DURATION_MS);
            villagerAttackTimes.remove(playerId);

            player.sendSystemMessage(Component.literal(
                    "§4[Village Diplomacy] Iron Golems are now HOSTILE for 30 seconds!"));

            for (IronGolem golem : nearbyGolems) {
                golem.setTarget(player);
            }
        }
    }

    private void manageCrimeStatus(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (!crimeCommittedTime.containsKey(playerId))
            return;

        long crimeEndTime = crimeCommittedTime.get(playerId);

        if (currentTime >= crimeEndTime) {
            // LIMPIAR COMPLETAMENTE TODOS LOS DATOS
            crimeCommittedTime.remove(playerId);
            golemStrikesPerGolem.remove(playerId);
            golemLastHitTime.remove(playerId);
            lastGolemHitTime.remove(playerId);

            // BUSCAR GOLEMS EN TODO EL MUNDO (sin límite de distancia)
            List<IronGolem> allGolems = level.getEntitiesOfClass(IronGolem.class,
                    new AABB(player.blockPosition()).inflate(1000.0D),
                    golem -> !golem.isPlayerCreated());

            int calmadosCount = 0;
            for (IronGolem golem : allGolems) {
                // Verificar si el golem tenía al jugador como objetivo
                LivingEntity target = golem.getTarget();
                UUID angerTarget = golem.getPersistentAngerTarget();

                boolean isAngryAtPlayer = (target != null && target.getUUID().equals(playerId)) ||
                        (angerTarget != null && angerTarget.equals(playerId));

                if (isAngryAtPlayer) {
                    // RESETEO EXTREMO: 5 pasadas para asegurar limpieza total
                    for (int i = 0; i < 5; i++) {
                        golem.setTarget(null);
                        golem.setLastHurtByMob(null);
                        golem.setLastHurtByPlayer(null);
                        golem.setPersistentAngerTarget(null);
                        golem.setRemainingPersistentAngerTime(0);
                        golem.stopBeingAngry();
                        golem.forgetCurrentTargetAndRefreshUniversalAnger();
                    }

                    calmadosCount++;
                }
            }

            // Mensaje inmersivo sin información de debug
            if (calmadosCount > 0) {
                if (calmadosCount == 1) {
                    player.sendSystemMessage(Component.literal(
                            "§a✓ The village guard has forgiven you."));
                } else {
                    player.sendSystemMessage(Component.literal(
                            "§a✓ The village guards have forgiven you. (§7" + calmadosCount + " guards§a)"));
                }
            } else {
                player.sendSystemMessage(Component.literal(
                        "§e⚠ No angry guards found nearby."));
            }
            return;
        }

        // Mantener hostilidad activa mientras dure el timer
        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(32.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {
            if (golem.getTarget() != player) {
                golem.setTarget(player);
                golem.setRemainingPersistentAngerTime(600);
                golem.setPersistentAngerTarget(playerId);
            }
        }
    }

    private void checkForVillageEntry(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 80);

        // Detectar salida de aldea
        String lastVillage = lastVisitedVillage.get(playerId);
        if (nearestVillage.isEmpty()) {
            if (lastVillage != null) {
                // Verificar que realmente estuvo en una aldea (cooldown)
                if (greetingCooldown.containsKey(playerId)) {
                    long lastGreeting = greetingCooldown.get(playerId);
                    // Solo mostrar "leaving" si pasaron más de 5 segundos desde entrar
                    if (currentTime - lastGreeting > 5000) {
                        VillageRelationshipData relationData = VillageRelationshipData.get(level);
                        String villageName = relationData.getVillageName(lastVillage);
                        
                        player.sendSystemMessage(Component.literal(
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                        player.sendSystemMessage(Component.literal(
                                "  §7◄ Leaving §6" + villageName));
                        player.sendSystemMessage(Component.literal(
                                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
                    }
                }
                lastVisitedVillage.remove(playerId);
            }
            return;
        }

        // Detectar entrada a aldea
        BlockPos villagePos = nearestVillage.get();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);

        // Verificar si es una aldea diferente o primera entrada
        boolean isDifferentVillage = lastVillage == null || !lastVillage.equals(villageId);

        if (isDifferentVillage) {
            // SOLO al entrar: Mostrar mensaje UNA VEZ
            VillageReputationData data = VillageReputationData.get(level);
            int reputation = data.getReputation(playerId, villagePos);
            String status = getReputationStatus(reputation);

            String icon = reputation >= 1000 ? "§6✦"
                    : reputation >= 800 ? "§6✦"
                            : reputation >= 500 ? "§a♥"
                                    : reputation >= 300 ? "§a+"
                                            : reputation >= 100 ? "§a+"
                                                    : reputation > -100 ? "§7●"
                                                            : reputation >= -299 ? "§c-"
                                                                    : reputation >= -500 ? "§c×" : "§4☠";

            player.sendSystemMessage(Component.literal(
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendSystemMessage(Component.literal(
                    "  " + icon + " §6Entering " + villageName));
            player.sendSystemMessage(Component.literal(
                    "  §7Reputation: §e" + reputation + " §8[§f" + status + "§8]"));
            player.sendSystemMessage(Component.literal(
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));

            lastVisitedVillage.put(playerId, villageId);
            greetingCooldown.put(playerId, currentTime);
        }
        // Si ya está en la misma aldea, NO hacer nada (no spam)
    }

    private void giveRandomGifts(ServerPlayer player, ServerLevel level) {
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        if (nearestVillage.isEmpty())
            return;

        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), nearestVillage.get());

        // Solo dar regalos con reputación positiva (500+)
        if (reputation < 500) return;
        
        // Probabilidad base ajustada
        float baseChance = reputation >= 1000 ? 0.002F
                : reputation >= 800 ? 0.001F
                : 0.0007F;

        if (level.getRandom().nextFloat() >= baseChance) return;

        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(8.0D));

        for (Villager villager : nearbyVillagers) {
            // Filtrar: solo aldeanos con profesión válida (NO nitwits ni bebés)
            if (villager.isBaby()) continue;
            String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
            if (profession.equals("none") || profession.equals("nitwit")) continue;
            
            // Items según profesión
            ItemStack gift = null;
            String message = null;
            
            switch (profession) {
                case "farmer":
                    gift = reputation >= 1000 ? new ItemStack(Items.GOLDEN_CARROT, 3)
                            : reputation >= 800 ? new ItemStack(Items.BREAD, 6)
                            : new ItemStack(Items.CARROT, 8);
                    message = "§a[Farmer] Fresh from my harvest, hero!";
                    break;
                    
                case "librarian":
                    gift = reputation >= 1000 ? new ItemStack(Items.ENCHANTED_BOOK)
                            : reputation >= 800 ? new ItemStack(Items.BOOK, 3)
                            : new ItemStack(Items.PAPER, 6);
                    message = "§6[Librarian] Knowledge is power, friend!";
                    break;
                    
                case "armorer":
                case "weaponsmith":
                case "toolsmith":
                    gift = reputation >= 1000 ? new ItemStack(Items.DIAMOND, 1)
                            : reputation >= 800 ? new ItemStack(Items.IRON_INGOT, 4)
                            : new ItemStack(Items.IRON_INGOT, 2);
                    message = "§7[Smith] From my forge to you!";
                    break;
                    
                case "cleric":
                    gift = reputation >= 1000 ? new ItemStack(Items.GOLDEN_APPLE, 1)
                            : reputation >= 800 ? new ItemStack(Items.GLISTERING_MELON_SLICE, 3)
                            : new ItemStack(Items.REDSTONE, 4);
                    message = "§d[Cleric] May this bless you!";
                    break;
                    
                case "butcher":
                    gift = reputation >= 1000 ? new ItemStack(Items.COOKED_BEEF, 6)
                            : reputation >= 800 ? new ItemStack(Items.COOKED_PORKCHOP, 4)
                            : new ItemStack(Items.COOKED_CHICKEN, 3);
                    message = "§c[Butcher] The finest cuts for you!";
                    break;
                    
                case "cartographer":
                    gift = reputation >= 1000 ? new ItemStack(Items.MAP, 1)
                            : reputation >= 800 ? new ItemStack(Items.COMPASS, 1)
                            : new ItemStack(Items.PAPER, 8);
                    message = "§b[Cartographer] May you never get lost!";
                    break;
                    
                case "fisherman":
                    gift = reputation >= 1000 ? new ItemStack(Items.COOKED_SALMON, 5)
                            : reputation >= 800 ? new ItemStack(Items.COOKED_COD, 4)
                            : new ItemStack(Items.COD, 6);
                    message = "§3[Fisherman] Fresh catch of the day!";
                    break;
                    
                case "fletcher":
                    gift = reputation >= 1000 ? new ItemStack(Items.ARROW, 16)
                            : reputation >= 800 ? new ItemStack(Items.ARROW, 10)
                            : new ItemStack(Items.STICK, 8);
                    message = "§e[Fletcher] Straight and true!";
                    break;
                    
                case "leatherworker":
                    gift = reputation >= 1000 ? new ItemStack(Items.LEATHER, 8)
                            : reputation >= 800 ? new ItemStack(Items.LEATHER, 5)
                            : new ItemStack(Items.RABBIT_HIDE, 6);
                    message = "§6[Leatherworker] Quality materials!";
                    break;
                    
                case "mason":
                    gift = reputation >= 1000 ? new ItemStack(Items.QUARTZ, 8)
                            : reputation >= 800 ? new ItemStack(Items.BRICK, 16)
                            : new ItemStack(Items.COBBLESTONE, 32);
                    message = "§8[Mason] Building materials for you!";
                    break;
                    
                case "shepherd":
                    gift = reputation >= 1000 ? new ItemStack(Items.WHITE_WOOL, 8)
                            : reputation >= 800 ? new ItemStack(Items.WHITE_WOOL, 5)
                            : new ItemStack(Items.STRING, 8);
                    message = "§f[Shepherd] Softest wool in the land!";
                    break;
                    
                default:
                    // Fallback genérico
                    gift = reputation >= 1000 ? new ItemStack(Items.EMERALD, 2)
                            : reputation >= 800 ? new ItemStack(Items.EMERALD, 1)
                            : new ItemStack(Items.BREAD, 3);
                    message = "§a[Villager] For our hero!";
            }
            
            if (gift != null) {
                if (!player.getInventory().add(gift)) {
                    player.drop(gift, false);
                }
                
                player.sendSystemMessage(Component.literal(message));
                
                // Feedback visual positivo
                spawnPositiveFeedback(level, villager);
                
                // Solo un regalo por tick
                break;
            }
        }
    }

    private void checkReputationLevelChange(ServerPlayer player, ServerLevel level, int newRep) {
        UUID playerId = player.getUUID();
        Integer lastLevel = lastReputationLevel.get(playerId);

        int newLevel = getReputationLevel(newRep);

        if (lastLevel == null || lastLevel != newLevel) {
            String message = getReputationLevelChangeMessage(newLevel, newRep);
            if (message != null) {
                player.sendSystemMessage(Component.literal(message));
            }
            lastReputationLevel.put(playerId, newLevel);
        }
    }

    private int getReputationLevel(int reputation) {
        if (reputation >= 1000)
            return 8;
        if (reputation >= 800)
            return 7;
        if (reputation >= 500)
            return 6;
        if (reputation >= 300)
            return 5;
        if (reputation >= 100)
            return 4;
        if (reputation > -100)
            return 3; // -99 a 99 = NEUTRAL
        if (reputation >= -299)
            return 2; // -100 a -299 = DISLIKED
        if (reputation >= -500)
            return 1;
        return 0;
    }

    private String getReputationLevelChangeMessage(int level, int reputation) {
        return switch (level) {
            case 8 -> "§6✦✦✦ [Villager] *kneels* A LEGEND walks among us! All hail!§6✦✦✦";
            case 7 -> "§6✦ [Villager] *bows respectfully* Our hero! Welcome back, champion!";
            case 6 -> "§a[Villager] *cheers* The village champion returns!";
            case 5 -> "§a[Villager] *smiles warmly* Welcome back, friend!";
            case 4 -> "§2[Villager] Good to see you again.";
            case 3 -> "§7[Villager] *nods*";
            case 2 -> "§e[Villager] *looks warily* ...";
            case 1 -> "§c[Villager] *frowns* Keep your distance...";
            case 0 -> "§4[Villager] *looks away in fear* Stay away from us!";
            default -> null;
        };
    }

    /**
     * Método público para limpiar crímenes de un jugador (usado por comandos)
     */
    public int clearCrimes(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        
        // Limpiar crimen
        boolean hadCrime = crimeCommittedTime.containsKey(playerId);
        crimeCommittedTime.remove(playerId);
        
        // Resetear todos los golems hostiles
        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
            player.getBoundingBox().inflate(100.0D),
            golem -> !golem.isPlayerCreated());
        
        int golemsCalmed = 0;
        for (IronGolem golem : nearbyGolems) {
            LivingEntity target = golem.getTarget();
            UUID angerTarget = golem.getPersistentAngerTarget();
            
            boolean isAngryAtPlayer = (target != null && target.getUUID().equals(playerId)) ||
                    (angerTarget != null && angerTarget.equals(playerId));
            
            if (isAngryAtPlayer) {
                golem.setTarget(null);
                golem.setLastHurtByMob(null);
                golem.setLastHurtByPlayer(null);
                golem.setPersistentAngerTarget(null);
                golem.setRemainingPersistentAngerTime(0);
                golem.stopBeingAngry();
                golem.getNavigation().stop();
                golem.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
                golem.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT);
                golemsCalmed++;
            }
        }
        
        return golemsCalmed;
    }

    private String getReputationStatus(int reputation) {
        if (reputation >= 1000)
            return "LEGENDARY HERO";
        if (reputation >= 800)
            return "HERO";
        if (reputation >= 500)
            return "CHAMPION";
        if (reputation >= 300)
            return "TRUSTED FRIEND";
        if (reputation >= 100)
            return "FRIENDLY";
        if (reputation >= 0)
            return "NEUTRAL";
        if (reputation >= -99)
            return "SUSPICIOUS";
        if (reputation >= -100)
            return "DISLIKED";
        if (reputation >= -200)
            return "UNWELCOME";
        if (reputation >= -499)
            return "UNFRIENDLY";
        if (reputation >= -699)
            return "HOSTILE";
        if (reputation >= -899)
            return "ENEMY";
        return "WANTED CRIMINAL";
    }
    
    private void checkAndNotifyReputationChange(ServerPlayer player, int oldRep, int newRep) {
        String oldStatus = getReputationStatus(oldRep);
        String newStatus = getReputationStatus(newRep);
        
        if (!oldStatus.equals(newStatus)) {
            boolean isPositive = newRep > oldRep;
            String color = isPositive ? "§a" : "§c";
            String arrow = isPositive ? "▲" : "▼";
            String emoji = isPositive ? "✦" : "✖";
            
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendSystemMessage(Component.literal("§e§l         REPUTATION CHANGED!"));
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("  §7" + oldStatus + " §8" + arrow + " " + color + "§l" + newStatus));
            player.sendSystemMessage(Component.literal(""));
            
            if (isPositive) {
                if (newRep >= 1000) {
                    player.sendSystemMessage(Component.literal("  §6§l" + emoji + " Villagers bow in your presence!"));
                } else if (newRep >= 800) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " You are a hero to this village!"));
                } else if (newRep >= 500) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " The villagers celebrate you!"));
                } else if (newRep >= 300) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " The village trusts you completely!"));
                } else if (newRep >= 100) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " Villagers greet you warmly!"));
                } else if (newRep >= 0) {
                    player.sendSystemMessage(Component.literal("  §e§l" + emoji + " Relations are improving..."));
                }
            } else {
                if (newRep < -899) {
                    player.sendSystemMessage(Component.literal("  §4§l" + emoji + " Golems attack on sight!"));
                } else if (newRep < -699) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " You are an enemy of the village!"));
                } else if (newRep < -499) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " Villagers refuse to trade with you!"));
                } else if (newRep < -200) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " You are not welcome here!"));
                } else if (newRep < -100) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " Villagers dislike you!"));
                } else if (newRep < 0) {
                    player.sendSystemMessage(Component.literal("  §e§l" + emoji + " Relations are deteriorating..."));
                }
            }
            
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendSystemMessage(Component.literal(""));
        }
    }

    private BlockType categorizeBlock(Block block, ServerLevel level, BlockPos pos) {
        if (block instanceof BellBlock) {
            return BlockType.BELL;
        } else if (block instanceof BedBlock) {
            return BlockType.BED;
        } else if (block instanceof CropBlock || block instanceof CarrotBlock ||
                block instanceof PotatoBlock || block instanceof BeetrootBlock) {
            return BlockType.CROP;
        } else if (block instanceof FlowerPotBlock || block instanceof TorchBlock ||
                block instanceof LanternBlock) {
            return BlockType.DECORATION;
        } else if (isWorkstation(block)) {
            return BlockType.WORKSTATION;
        } else if (isWell(level, pos)) {
            return BlockType.WELL;
        } else if (isHouseBlock(level, pos, block)) {
            return BlockType.HOUSE;
        }
        return BlockType.NONE;
    }

    private boolean isWorkstation(Block block) {
        return block instanceof CraftingTableBlock || block instanceof FurnaceBlock ||
                block instanceof SmokerBlock || block instanceof BlastFurnaceBlock ||
                block instanceof BrewingStandBlock || block instanceof AnvilBlock ||
                block instanceof GrindstoneBlock || block instanceof LoomBlock ||
                block instanceof StonecutterBlock || block instanceof SmithingTableBlock ||
                block instanceof CartographyTableBlock || block instanceof FletchingTableBlock ||
                block instanceof ComposterBlock || block instanceof BarrelBlock ||
                block == Blocks.BOOKSHELF || block == Blocks.LECTERN ||
                block == Blocks.CAULDRON || block == Blocks.WATER_CAULDRON ||
                block == Blocks.LAVA_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON;
    }

    private boolean isWell(ServerLevel level, BlockPos pos) {
        int waterCount = 0;
        int cobbleCount = 0;

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    Block block = level.getBlockState(checkPos).getBlock();

                    if (block instanceof LiquidBlock)
                        waterCount++;
                    if (block.toString().toLowerCase().contains("stone") ||
                            block.toString().toLowerCase().contains("cobble")) {
                        cobbleCount++;
                    }
                }
            }
        }

        return waterCount >= 3 && cobbleCount >= 6;
    }

    private boolean isHouseBlock(ServerLevel level, BlockPos pos, Block block) {
        for (int y = -3; y <= 3; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (level.getBlockState(pos.offset(x, y, z)).getBlock() instanceof BedBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getBlockBreakMessage(BlockType type, boolean isBaby, ServerLevel level) {
        String[] adultMessages = type.adultMessages;
        String[] babyMessages = type.babyMessages;

        String[] messages = isBaby && babyMessages.length > 0 ? babyMessages : adultMessages;
        return messages[level.getRandom().nextInt(messages.length)];
    }

    private boolean hasLineOfSight(Villager villager, ServerPlayer player, ServerLevel level) {
        Vec3 villagerEyes = villager.getEyePosition();
        Vec3 playerEyes = player.getEyePosition();

        ClipContext context = new ClipContext(
                villagerEyes,
                playerEyes,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                villager);

        BlockHitResult result = level.clip(context);

        return result.getType() == HitResult.Type.MISS;
    }

    private enum BlockType {
        BELL(-50, "Broke the village bell!",
                new String[] {
                        "§4[Villager] THE BELL! Our emergency system!",
                        "§4[Villager] NO! That was our warning bell!",
                        "§4[Villager] You destroyed our bell!",
                        "§4[Villager] *horrified* The bell is our lifeline!",
                        "§4[Villager] How will we call for help now?!",
                        "§4[Villager] That bell has saved lives!",
                        "§4[Villager] Guards! The bell is destroyed!",
                        "§4[Villager] This is unforgivable!",
                        "§c[Villager] *panicking* Our warning system!",
                        "§c[Villager] Raiders will attack and we won't know!"
                },
                new String[] {
                        "§c[Baby Villager] *cries loudly* The bell! It's broken!",
                        "§c[Baby Villager] Why did you break it?!",
                        "§c[Baby Villager] Mommy! The bell!",
                        "§c[Baby Villager] *sobbing* That was important!"
                }),
        BED(-20, "Broke a villager's bed!",
                new String[] {
                        "§c[Villager] That's MY bed!",
                        "§c[Villager] Where am I supposed to sleep now?!",
                        "§c[Villager] You monster!",
                        "§c[Villager] *furious* That took me weeks to make!",
                        "§c[Villager] I JUST made that bed!",
                        "§c[Villager] You have no respect for others!",
                        "§c[Villager] That's my only bed!",
                        "§c[Villager] I work all day and you break my bed?!",
                        "§c[Villager] *outraged* I'll sleep on the floor because of you!",
                        "§c[Villager] Breaking someone's bed? Really?!"
                },
                new String[] {
                        "§c[Baby Villager] My bed! *sobs uncontrollably*",
                        "§c[Baby Villager] I need that to sleep!",
                        "§c[Baby Villager] *crying* Where will I sleep?!",
                        "§c[Baby Villager] Daddy! My bed is broken!",
                        "§c[Baby Villager] *wailing* Why?!"
                }),
        CROP(-15, "Destroyed village crops!",
                new String[] {
                        "§c[Villager] Our food! You're destroying our crops!",
                        "§c[Villager] We need those to survive!",
                        "§c[Villager] STOP trampling our farm!",
                        "§c[Villager] That took MONTHS to grow!",
                        "§c[Villager] We'll go hungry because of you!",
                        "§c[Villager] *desperate* That's our winter supply!",
                        "§c[Villager] Do you know how hard farming is?!",
                        "§c[Villager] Those crops feed the entire village!",
                        "§c[Villager] *angry* Stay away from our fields!",
                        "§c[Villager] You're destroying our livelihood!",
                        "§c[Villager] The whole village depends on these crops!",
                        "§c[Villager] Have you no shame?!"
                },
                new String[] {
                        "§c[Baby Villager] The food! *cries*",
                        "§c[Baby Villager] Mommy said not to touch the crops!",
                        "§c[Baby Villager] *points* Bad! Bad!",
                        "§c[Baby Villager] Those were going to be bread!"
                }),
        WORKSTATION(-25, "Broke a workstation!",
                new String[] {
                        "§4[Villager] That's my livelihood!",
                        "§4[Villager] I need that to work!",
                        "§4[Villager] How dare you!",
                        "§4[Villager] *shocked* My crafting table!",
                        "§4[Villager] I've had that for YEARS!",
                        "§4[Villager] That's how I make a living!",
                        "§4[Villager] You just destroyed my job!",
                        "§4[Villager] *furious* How am I supposed to work now?!",
                        "§4[Villager] That station was essential to the village!",
                        "§c[Villager] Without that I can't earn emeralds!",
                        "§c[Villager] Do you know how expensive those are?!",
                        "§c[Villager] My entire profession depends on that!"
                },
                new String[] {
                        "§c[Baby Villager] Daddy's workstation!",
                        "§c[Baby Villager] *gasps* You broke it!"
                }),
        DECORATION(-5, "Broke village decoration!",
                new String[] {
                        "§c[Villager] Hey! That made the village look nice!",
                        "§c[Villager] Why would you do that?",
                        "§c[Villager] We worked hard to decorate!",
                        "§6[Villager] *sighs* That was pretty...",
                        "§6[Villager] Can't we have nice things?",
                        "§c[Villager] Show some respect for our village!",
                        "§6[Villager] I just placed that yesterday...",
                        "§c[Villager] Breaking our decorations now?"
                },
                new String[] {}),
        WELL(-30, "Damaged the village well!",
                new String[] {
                        "§4[Villager] THE WELL! Our water source!",
                        "§4[Villager] That's our only water supply!",
                        "§4[Villager] *horrified* The well is destroyed!",
                        "§4[Villager] We'll die of thirst!",
                        "§c[Villager] How will we get water now?!",
                        "§c[Villager] That well has served us for generations!",
                        "§c[Villager] You've doomed us all!",
                        "§c[Villager] *panicking* Our water! Our precious water!",
                        "§4[Villager] This is a catastrophe!"
                },
                new String[] {
                        "§c[Baby Villager] *crying* Where do we get water?!",
                        "§c[Baby Villager] I'm thirsty! The well!",
                        "§c[Baby Villager] Mommy! The water place is broken!"
                }),
        HOUSE(-15, "Damaged a house!",
                new String[] {
                        "§c[Villager] You're destroying my home!",
                        "§c[Villager] STOP! This is where I live!",
                        "§c[Villager] My house! You're tearing it apart!",
                        "§c[Villager] *desperate* I have nowhere else to go!",
                        "§c[Villager] That's my HOUSE you're breaking!",
                        "§c[Villager] I built this with my own hands!",
                        "§c[Villager] *furious* Get away from my home!",
                        "§c[Villager] This house protects me from mobs!",
                        "§c[Villager] You're making me homeless!",
                        "§c[Villager] Have you no decency?!"
                },
                new String[] {
                        "§c[Baby Villager] Our house! *cries*",
                        "§c[Baby Villager] *sobbing* Where will we live?!",
                        "§c[Baby Villager] Don't break our home!",
                        "§c[Baby Villager] Daddy! Our house!"
                }),
        NONE(0, "", new String[] {}, new String[] {});

        final int penalty;
        final String systemMessage;
        final String[] adultMessages;
        final String[] babyMessages;

        BlockType(int penalty, String systemMessage, String[] adultMessages, String[] babyMessages) {
            this.penalty = penalty;
            this.systemMessage = systemMessage;
            this.adultMessages = adultMessages;
            this.babyMessages = babyMessages;
        }
    }

    private void makeVillagersFleeFromHostilePlayers(ServerPlayer player, ServerLevel level) {
        VillageReputationData reputationData = VillageReputationData.get(level.getServer().overworld());
        int reputation = reputationData.getReputation(player.getUUID());

        if (reputation >= -299)
            return;

        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(10.0D));

        for (Villager villager : nearbyVillagers) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villager.blockPosition(),
                    200);
            if (nearestVillage.isEmpty())
                continue;

            if (villager.getNavigation() != null && villager.isAlive()) {
                double dx = villager.getX() - player.getX();
                double dz = villager.getZ() - player.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance < 10.0 && distance > 0.1) {
                    dx = (dx / distance) * 5.0;
                    dz = (dz / distance) * 5.0;

                    villager.getNavigation().moveTo(
                            villager.getX() + dx,
                            villager.getY(),
                            villager.getZ() + dz,
                            1.2); // Velocidad de huida

                    if (level.getRandom().nextInt(50) == 0) {
                        String[] fearMessages = reputation <= -800 ? new String[] {
                                "§4[Villager] RUN! The criminal is here!",
                                "§4[Villager] HELP! Someone help us!",
                                "§4[Villager] Stay away from us!",
                                "§4[Villager] Guards! GUARDS!",
                                "§4[Baby Villager] *screaming* SCARY!"
                        }
                                : reputation <= -500 ? new String[] {
                                        "§c[Villager] Get away from me!",
                                        "§c[Villager] I don't want trouble!",
                                        "§c[Villager] Leave us alone!",
                                        "§c[Baby Villager] Mama! I'm scared!"
                                }
                                        : new String[] {
                                                "§6[Villager] Please, stay back...",
                                                "§6[Villager] I'd rather you keep your distance.",
                                                "§6[Villager] Not comfortable with you around."
                                        };

                        player.sendSystemMessage(Component.literal(
                                fearMessages[level.getRandom().nextInt(fearMessages.length)]));
                    }
                }
            }
        }
    }

    private void makeGolemsProtectVillageBasedOnReputation(ServerPlayer player, ServerLevel level) {
        // No atacar en creative o spectator
        if (player.isCreative() || player.isSpectator())
            return;
            
        VillageReputationData reputationData = VillageReputationData.get(level.getServer().overworld());
        int reputation = reputationData.getReputation(player.getUUID());

        if (reputation >= -500)
            return;

        List<IronGolem> nearbyGolems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(30.0D),
                golem -> !golem.isPlayerCreated());

        for (IronGolem golem : nearbyGolems) {
            if (reputation < -800) {
                if (golem.getTarget() == null && level.getRandom().nextInt(100) < 5) {
                    double distance = golem.distanceTo(player);
                    if (distance < 20.0) {
                        golem.setTarget(player);

                        // Mensajes contextuales cuando el golem empieza a perseguir
                        String[] warningMessages = {
                            "§4[Village Guard] You are a wanted criminal! Surrender now!",
                            "§c[Village Guard] *stomps aggressively* LEAVE THIS VILLAGE!",
                            "§4[Village Guard] Your crimes will not go unpunished!",
                            "§c[Village Guard] *raises fist* Justice will be served!",
                            "§4[Village Diplomacy] You are WANTED! Guards attack on sight!"
                        };
                        
                        if (level.getRandom().nextInt(3) == 0) {
                            player.sendSystemMessage(Component.literal(
                                    warningMessages[level.getRandom().nextInt(warningMessages.length)]));
                        }
                    }
                }
            }
        }
    }

    /**
     * DEPRECATED - War Golem system removed for simplification
     */
    @SubscribeEvent
    public void onWarGolemDeath(LivingDeathEvent event) {
        // Sistema de raids deshabilitado
    }
    
    /**
     * DEPRECATED - Caravan system removed for simplification
     */
    @SubscribeEvent
    public void onCaravanDeath(LivingDeathEvent event) {
        // Sistema de caravanas deshabilitado
    }

    // ==================== GEOPOLITICS SYSTEM ====================
    
    private long lastGeopoliticsTick = 0;
    
    /**
     * Main tick handler for geopolitics systems (OPTIMIZED - only every 20 ticks)
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getGameTime();
            
            // Only check every 20 ticks (1 second) instead of every tick
            if (gameTime - lastGeopoliticsTick < 20) {
                continue;
            }
            
            lastGeopoliticsTick = gameTime;
            
            // Sistema de geopolítica deshabilitado (movido a DLC futuro)
        }
    }
    
    /**
     * DISABLED - Village discovery system (moved to future DLC)
     */
    private final Set<String> discoveredVillages = new HashSet<>();
    
    @SubscribeEvent
    public void onVillagerDiscovery(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        // Sistema de descubrimiento de aldeas deshabilitado temporalmente
    }
    
    /**
     * DEPRECATED - Handle caravan merchant death (system removed)
     */
    @SubscribeEvent
    public void onCaravanMerchantKilled(LivingDeathEvent event) {
        // Sistema de caravanas deshabilitado
    }
    
    /**
     * DEPRECATED - Handle war golem death (system removed)
     */
    @SubscribeEvent
    public void onWarGolemKilled(LivingDeathEvent event) {
        // Sistema de raids deshabilitado
    }
    
    /**
     * DEPRECATED - Check player escort when near caravan (system removed)
     */
    @SubscribeEvent
    public void onPlayerMoveNearCaravan(TickEvent.PlayerTickEvent event) {
        // Sistema de caravanas deshabilitado
    }
    
    // ==================== FEEDBACK VISUAL Y SONORO ====================
    
    /**
     * Spawns heart particles and plays XP sound when reputation increases
     */
    private void spawnPositiveFeedback(ServerLevel level, LivingEntity entity) {
        if (entity == null) return;
        
        Vec3 pos = entity.position();
        // Spawn heart particles around the entity
        level.sendParticles(ParticleTypes.HEART, 
            pos.x, pos.y + 2.0, pos.z,
            3, // count
            0.3, 0.3, 0.3, // spread
            0.0); // speed
        
        // Play experience orb pickup sound
        level.playSound(null, entity.blockPosition(), 
            SoundEvents.EXPERIENCE_ORB_PICKUP, 
            SoundSource.NEUTRAL, 
            0.6f, // volume
            1.2f); // pitch
    }
    
    /**
     * Spawns angry particles and plays villager "no" sound when reputation decreases
     */
    private void spawnNegativeFeedback(ServerLevel level, LivingEntity entity) {
        if (entity == null) return;
        
        Vec3 pos = entity.position();
        // Spawn angry cloud particles
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, 
            pos.x, pos.y + 2.0, pos.z,
            5, // count
            0.4, 0.4, 0.4, // spread
            0.0); // speed
        
        // Play villager "no" sound
        level.playSound(null, entity.blockPosition(), 
            SoundEvents.VILLAGER_NO, 
            SoundSource.NEUTRAL, 
            0.8f, // volume
            0.9f); // pitch (slightly lower = more serious)
    }

    private void checkForVillagerGreetings(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        
        // Buscar aldea cercana
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        if (nearestVillage.isEmpty()) return;
        
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(playerId, nearestVillage.get());
        
        // Solo saludar si eres HERO (500+) o ALLY (200+)
        if (reputation < 200) return;
        
        // Buscar aldeanos cercanos (8 bloques)
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(8.0D));
        
        if (nearbyVillagers.isEmpty()) return;
        
        // Inicializar mapa de cooldowns si no existe
        if (!villagerGreetingCooldown.containsKey(playerId)) {
            villagerGreetingCooldown.put(playerId, new HashMap<>());
        }
        
        Map<UUID, Long> playerGreetings = villagerGreetingCooldown.get(playerId);
        
        // Encontrar un aldeano que pueda saludar
        for (Villager villager : nearbyVillagers) {
            UUID villagerId = villager.getUUID();
            
            // Verificar cooldown (60 segundos por aldeano)
            if (playerGreetings.containsKey(villagerId) &&
                currentTime - playerGreetings.get(villagerId) < 60000) {
                continue;
            }
            
            // Verificar línea de visión
            if (!hasLineOfSight(villager, player, level)) {
                continue;
            }
            
            // Este aldeano puede saludar
            VillagerPersonalityData personalityData = VillagerPersonalityData.get(level);
            VillagerPersonality personality = personalityData.getPersonality(villagerId);
            String temperament = personality != null ? personality.getTemperament().name() : "NEUTRAL";
            String villagerName = personality != null ? personality.getCustomName() : "Villager";
            
            String[] greetings = getGreetingsForTemperament(temperament, reputation, villagerName);
            String greeting = greetings[level.getRandom().nextInt(greetings.length)];
            
            player.sendSystemMessage(Component.literal(greeting));
            
            // Efecto visual - corazones
            Vec3 villagerPos = villager.position();
            level.sendParticles(ParticleTypes.HEART,
                villagerPos.x, villagerPos.y + 2.0, villagerPos.z,
                2, 0.3, 0.3, 0.3, 0.0);
            
            // Sonido de aldeano feliz
            level.playSound(null, villager.blockPosition(),
                SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL,
                0.6f, 1.0f + level.getRandom().nextFloat() * 0.2f);
            
            // Registrar cooldown
            playerGreetings.put(villagerId, currentTime);
            
            // Solo un saludo por tick
            return;
        }
    }
    
    private String[] getGreetingsForTemperament(String temperament, int reputation, String villagerName) {
        boolean isHero = reputation >= 500;
        
        // Mensajes especiales para HERO
        if (isHero) {
            return switch (temperament) {
                case "BRAVE" -> new String[]{
                    "§6[" + villagerName + "] Hero! It's an honor!",
                    "§6[" + villagerName + "] Our champion returns!",
                    "§6[" + villagerName + "] Brave one! Welcome back!"
                };
                case "SHY" -> new String[]{
                    "§b[" + villagerName + "] H-hero! *bows nervously*",
                    "§b[" + villagerName + "] *waves shyly* You're amazing!",
                    "§b[" + villagerName + "] I... I admire you so much!"
                };
                case "GREEDY" -> new String[]{
                    "§e[" + villagerName + "] Our most valued customer!",
                    "§e[" + villagerName + "] The hero! Care to trade?",
                    "§e[" + villagerName + "] Best customer ever! Welcome!"
                };
                case "WISE" -> new String[]{
                    "§d[" + villagerName + "] Greetings, legendary one.",
                    "§d[" + villagerName + "] Your reputation precedes you, hero.",
                    "§d[" + villagerName + "] The village owes you much."
                };
                case "GOSSIP" -> new String[]{
                    "§a[" + villagerName + "] HERO! Everyone talks about you!",
                    "§a[" + villagerName + "] Did you hear? Oh wait, YOU'RE the hero!",
                    "§a[" + villagerName + "] The whole village loves you!"
                };
                case "CHEERFUL" -> new String[]{
                    "§a[" + villagerName + "] YAY! Our hero is here!",
                    "§a[" + villagerName + "] Best day ever! You're back!",
                    "§a[" + villagerName + "] *jumps excitedly* HERO!"
                };
                default -> new String[]{
                    "§6[" + villagerName + "] Welcome back, hero!",
                    "§6[" + villagerName + "] Good to see you, champion!",
                    "§6[" + villagerName + "] The village is safe with you here!"
                };
            };
        }
        
        // Mensajes para ALLY (200-499)
        return switch (temperament) {
            case "BRAVE" -> new String[]{
                "§a[" + villagerName + "] Good day, friend!",
                "§a[" + villagerName + "] Ally! Welcome!",
                "§a[" + villagerName + "] A pleasure to see you!"
            };
            case "SHY" -> new String[]{
                "§b[" + villagerName + "] *smiles* Hello...",
                "§b[" + villagerName + "] *waves* H-hi there!",
                "§b[" + villagerName + "] Oh! Hello, friend..."
            };
            case "GREEDY" -> new String[]{
                "§e[" + villagerName + "] My favorite customer!",
                "§e[" + villagerName + "] Good deals today, friend!",
                "§e[" + villagerName + "] Always a pleasure!"
            };
            case "WISE" -> new String[]{
                "§d[" + villagerName + "] Greetings, trusted one.",
                "§d[" + villagerName + "] Peace be with you, friend.",
                "§d[" + villagerName + "] Welcome, ally."
            };
            case "GOSSIP" -> new String[]{
                "§a[" + villagerName + "] Oh! Hi! Did you hear about...?",
                "§a[" + villagerName + "] Perfect timing! I have news!",
                "§a[" + villagerName + "] You! I was just talking about you!"
            };
            case "FRIENDLY" -> new String[]{
                "§a[" + villagerName + "] Hey there, buddy!",
                "§a[" + villagerName + "] Great to see you!",
                "§a[" + villagerName + "] How's it going, friend?"
            };
            case "CHEERFUL" -> new String[]{
                "§a[" + villagerName + "] Hi! What a lovely day!",
                "§a[" + villagerName + "] *smiles* Hello, friend!",
                "§a[" + villagerName + "] Wonderful to see you!"
            };
            default -> new String[]{
                "§a[" + villagerName + "] Hello, friend!",
                "§a[" + villagerName + "] Good day!",
                "§a[" + villagerName + "] Welcome!"
            };
        };
    }

    @SubscribeEvent
    public void onZombieVillagerCured(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager))
            return;
        if (!(villager.level() instanceof ServerLevel level))
            return;

        // Buscar quién curó a este zombie villager
        UUID curerUUID = zombieVillagerCurers.remove(event.getEntity().getUUID());
        if (curerUUID == null)
            return;

        ServerPlayer curer = level.getServer().getPlayerList().getPlayer(curerUUID);
        if (curer == null)
            return;

        // Dar +100 de reputación por curar
        BlockPos villagerPos = villager.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);
        
        if (nearestVillage.isPresent()) {
            VillageReputationData data = VillageReputationData.get(level);
            int oldRep = data.getReputation(curerUUID, nearestVillage.get());
            data.addReputation(curerUUID, nearestVillage.get(), 100);
            int newRep = data.getReputation(curerUUID, nearestVillage.get());
            
            curer.sendSystemMessage(Component.literal(
                "§a[Village Diplomacy] Cured a zombie villager! +100 Reputation (Total: " +
                newRep + " - " + getReputationStatus(newRep) + ")"));
                
            checkReputationLevelChange(curer, level, newRep);
        }
    }

    @SubscribeEvent
    public void onPlayerInteractWithZombieVillager(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getTarget() instanceof ZombieVillager zombieVillager))
            return;
            
        ItemStack held = player.getItemInHand(event.getHand());
        
        // Detectar si el player está curando (golden apple + weakness)
        if (held.getItem() == Items.GOLDEN_APPLE && zombieVillager.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
            // Registrar quién está curando a este zombie
            zombieVillagerCurers.put(zombieVillager.getUUID(), player.getUUID());
        }
    }

    private boolean isJobSiteBlock(Block block) {
        // Lista completa de job site blocks que los aldeanos usan
        return block instanceof net.minecraft.world.level.block.BarrelBlock ||
               block instanceof net.minecraft.world.level.block.BlastFurnaceBlock ||
               block instanceof net.minecraft.world.level.block.BrewingStandBlock ||
               block instanceof net.minecraft.world.level.block.CartographyTableBlock ||
               block instanceof net.minecraft.world.level.block.CauldronBlock ||
               block instanceof net.minecraft.world.level.block.ComposterBlock ||
               block instanceof net.minecraft.world.level.block.FletchingTableBlock ||
               block instanceof net.minecraft.world.level.block.GrindstoneBlock ||
               block instanceof net.minecraft.world.level.block.LecternBlock ||
               block instanceof net.minecraft.world.level.block.LoomBlock ||
               block instanceof net.minecraft.world.level.block.SmokerBlock ||
               block instanceof net.minecraft.world.level.block.SmithingTableBlock ||
               block instanceof net.minecraft.world.level.block.StonecutterBlock;
    }
}