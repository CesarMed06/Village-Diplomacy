package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.LiquidBlock;
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
    private static final long GREETING_COOLDOWN_MS = 600000; // 10 minutos
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

    private static final int THEFT_CHEST_ADULT = 18;
    private static final int THEFT_CHEST_BABY = 13;
    private static final int THEFT_LOOT_ADULT = 15;
    private static final int THEFT_LOOT_BABY = 10;

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
        checkAndNotifyReputationChange(player, oldRep, newRep);

        if (villager.isBaby()) {
            ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.villagerdeath.baby", 4);
        } else {
            ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.villagerdeath.adult", 4);
        }
        ModLang.sendReputationSummary(player, reputationLoss, newRep);

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
                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.crime_extended", totalSeconds));
            } else {
                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.crime_golems"));
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

        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.golem_killed",
                newRep,
                ModLang.repStatus(newRep)));

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
            
            if (personality != null) {
                String key = switch (personality.getTemperament()) {
                    case GENTLE -> "villagediplomacy.golem.player_hit.gentle";
                    case STERN -> "villagediplomacy.golem.player_hit.stern";
                    case FIERCE -> "villagediplomacy.golem.player_hit.fierce";
                    case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.player_hit.default";
                };
                player.sendSystemMessage(Component.translatable(key, personality.getName()));
            } else {
                Component nameComp = golem.hasCustomName() ? golem.getCustomName()
                        : Component.translatable("villagediplomacy.golem.generic_name");
                player.sendSystemMessage(Component.translatable("villagediplomacy.golem.generic_warn", nameComp));
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
        checkAndNotifyReputationChange(player, oldRep, newRep);

        player.sendSystemMessage(getDialogComponent(newRep));
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.villager_attacked",
                -10, newRep, ModLang.repStatus(newRep)));

        checkReputationLevelChange(player, level, newRep);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get());

        processStrikeSystem(player, level, villagerPos);
    }

    /**
     * Obtiene un diálogo aleatorio según la categoría y la reputación actual.
     * Usa el sistema de traducción de Minecraft para EN/ES automático.
     */
    private static Component getDialogComponent(int reputation) {
        java.util.Random random = new java.util.Random();
        if (reputation >= 75) {
            return Component.translatable("villagediplomacy.dialog.friendly." + random.nextInt(7));
        } else if (reputation >= 25) {
            return Component.translatable("villagediplomacy.dialog.neutral." + random.nextInt(6));
        } else if (reputation < 0) {
            return Component.translatable("villagediplomacy.dialog.hostile." + random.nextInt(6));
        } else {
            return Component.translatable("villagediplomacy.dialog.greeting." + random.nextInt(7));
        }
    }

    /**
     * Diálogo de comercio aleatorio.
     */
    private static String getTradeDialog() {
        java.util.Random random = new java.util.Random();
        int index = random.nextInt(6);
        return Component.translatable("villagediplomacy.dialog.trade." + index).getString();
    }

    /**
     * Diálogo de advertencia aleatorio (nuevo en v1.1.0).
     */
    private static String getWarningDialog() {
        java.util.Random random = new java.util.Random();
        int index = random.nextInt(6);
        return Component.translatable("villagediplomacy.dialog.warning." + index).getString();
    }

    /**
     * Diálogo de quest aleatorio (nuevo en v1.1.0).
     */
    private static String getQuestDialog() {
        java.util.Random random = new java.util.Random();
        int index = random.nextInt(5);
        return Component.translatable("villagediplomacy.dialog.quest." + index).getString();
    }

    @SubscribeEvent
    public void onAnimalAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        if (!(event.getEntity().level() instanceof ServerLevel level))
            return;

        // Detectar tipo específico de animal (Camel ANTES de AbstractHorse porque Camel extends AbstractHorse)
        String animalType = null;
        if (event.getEntity() instanceof Cow) animalType = "cow";
        else if (event.getEntity() instanceof Sheep) animalType = "sheep";
        else if (event.getEntity() instanceof Pig) animalType = "pig";
        else if (event.getEntity() instanceof Chicken) animalType = "chicken";
        else if (event.getEntity() instanceof Rabbit) animalType = "rabbit";
        else if (event.getEntity() instanceof Camel) animalType = "camel";
        else if (event.getEntity() instanceof AbstractHorse) animalType = "horse";
        
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

                        AnimalAttackKind attackKind = animalAttackKindFor(animalType);
                        String suffix = villager.isBaby() ? ".baby" : ".adult";
                        int lineCount = villager.isBaby() ? attackKind.babyCount() : attackKind.adultCount();
                        ModLang.sendRandom(player, level.getRandom(),
                                "villagediplomacy.react.animalattack." + attackKind.key() + suffix, lineCount);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_attack_warn",
                                Component.translatable(event.getEntity().getType().getDescriptionId())));
                        ModLang.sendReputationSummary(player, -5, newRep);

                        tradeCooldowns.put(playerId, currentTime);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onAnimalDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity killed = event.getEntity();
        BlockPos deathPos = killed.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, deathPos, 200);
        if (nearestVillage.isEmpty()) {
            return;
        }

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        UUID playerId = player.getUUID();
        int oldRep = data.getReputation(playerId, villagePos);
        data.addReputation(playerId, villagePos, -25);
        int newRep = data.getReputation(playerId, villagePos);
        checkAndNotifyReputationChange(player, oldRep, newRep);

        Villager witness = findNearestWitnessVillager(level, killed, 16.0);
        boolean useBaby = witness != null && witness.isBaby();
        AnimalDeathKind react = animalDeathKindFor(killed);
        String suffix = useBaby ? ".baby" : ".adult";
        int lineCount = useBaby ? react.babyCount : react.adultCount;
        ModLang.sendRandom(player, level.getRandom(),
                "villagediplomacy.react.animaldeath." + react.key + suffix, lineCount);

        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_killed",
                Component.translatable(killed.getType().getDescriptionId()),
                newRep,
                ModLang.repStatus(newRep)));

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos);
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
                ModLang.send(player, "villagediplomacy.debug.trade_bond", personality.getCustomName(),
                        String.valueOf(currentBonus));
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

                int ti = level.getRandom().nextInt(6);
                player.sendSystemMessage(Component.translatable("villagediplomacy.react.trade." + ti, trades));
                ModLang.sendReputationSummary(player, trades * 5, newRep);

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
                if (isClosing) {
                    if (caughtByBaby) {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.close.baby", 4);
                    } else {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.close.adult", 6);
                    }
                } else if (caughtByBaby) {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.open.baby", 5);
                } else if (isNight) {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.open.night", 4);
                } else if (isMorning) {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.open.morning", 4);
                } else {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.high.open.day", 8);
                }

            } else if (reputation >= 100) {
                if (level.getRandom().nextInt(2) == 0) {
                    if (isClosing) {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.neutral.close", 4);
                    } else if (isNight) {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.neutral.open.night", 3);
                    } else {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.neutral.open.day", 6);
                    }
                }

            } else if (reputation >= -99) {
                if (level.getRandom().nextInt(2) == 0) {
                    if (caughtByBaby) {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.low.baby", 3);
                    } else {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.low.adult", 6);
                    }
                }

            } else {
                data.addReputation(playerId, nearestVillage.get(), -5);
                int newRep = data.getReputation(playerId, nearestVillage.get());

                if (caughtByBaby) {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.neg.baby", 6);
                } else {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.door.neg.adult", 10);
                }
                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.trespass_door"));
                ModLang.sendReputationSummary(player, -5, newRep);
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
                BlockPos villagePos = nearestVillage.get();

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
                    int oldRep = data.getReputation(player.getUUID(), villagePos);
                    data.addReputation(player.getUUID(), villagePos, -10);
                    int newRep = data.getReputation(player.getUUID(), villagePos);
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    if (caughtByBaby) {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.theft.chest.baby", THEFT_CHEST_BABY);
                    } else {
                        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.theft.chest.adult", THEFT_CHEST_ADULT);
                    }

                    player.sendSystemMessage(Component.translatable("villagediplomacy.sys.chest_open",
                            -10, newRep, ModLang.repStatus(newRep)));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(villagePos);
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
            BlockPos villagePos = nearestVillage.get();

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
                int oldRep = data.getReputation(player.getUUID(), villagePos);
                data.addReputation(player.getUUID(), villagePos, -15);
                int newRep = data.getReputation(player.getUUID(), villagePos);
                checkAndNotifyReputationChange(player, oldRep, newRep);

                if (caughtByBaby) {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.theft.loot.baby", THEFT_LOOT_BABY);
                } else {
                    ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.theft.loot.adult", THEFT_LOOT_ADULT);
                }

                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.loot_village",
                        -15, newRep, ModLang.repStatus(newRep)));

                chestLootCooldown.put(playerId, currentTime);

                VillageRelationshipData relationData = VillageRelationshipData.get(level);
                relationData.registerVillage(villagePos);
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
            BlockPos villagePosBreak = nearestVillage.get();

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
                    int oldRep = data.getReputation(player.getUUID(), villagePosBreak);
                    data.addReputation(player.getUUID(), villagePosBreak, penalty);
                    int newRep = data.getReputation(player.getUUID(), villagePosBreak);
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    sendBlockBreakVillagerLine(blockType, caughtByBaby, level, player);

                    player.sendSystemMessage(Component.translatable(blockType.systemMessageKey,
                            penalty, newRep, ModLang.repStatus(newRep)));

                    VillageRelationshipData relationData = VillageRelationshipData.get(level);
                    relationData.registerVillage(villagePosBreak);
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
                VillageReputationData data = VillageReputationData.get(level);
                BlockPos villagePos = nearestVillage.get();
                int reputation = data.getReputation(player.getUUID(), villagePos);
                boolean isWelcome = reputation >= 100;
                boolean isNeutral = reputation >= 0 && reputation < 100;
                boolean isUnwelcome = reputation < 0;

                String placeKey;
                if (placedBlock instanceof BedBlock) {
                    placeKey = "bed";
                } else if (placedBlock instanceof ChestBlock || placedBlock instanceof BarrelBlock) {
                    placeKey = "chest";
                } else if (placedBlock instanceof FurnaceBlock || placedBlock instanceof BlastFurnaceBlock
                        || placedBlock instanceof SmokerBlock) {
                    placeKey = "furnace";
                } else if (placedBlock instanceof CraftingTableBlock) {
                    placeKey = "crafting";
                } else if (placedBlock instanceof BellBlock) {
                    placeKey = "bell";
                } else if (placedBlock instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                    placeKey = "brewing";
                } else if (placedBlock instanceof net.minecraft.world.level.block.EnchantmentTableBlock) {
                    placeKey = "enchanting";
                } else if (placedBlock == Blocks.BOOKSHELF) {
                    placeKey = "bookshelf";
                } else if (placedBlock instanceof net.minecraft.world.level.block.LecternBlock) {
                    placeKey = "lectern";
                } else if (placedBlock instanceof net.minecraft.world.level.block.AnvilBlock) {
                    placeKey = "anvil";
                } else if (placedBlock instanceof net.minecraft.world.level.block.GrindstoneBlock) {
                    placeKey = "grindstone";
                } else if (placedBlock instanceof net.minecraft.world.level.block.LoomBlock) {
                    placeKey = "loom";
                } else if (placedBlock instanceof net.minecraft.world.level.block.ComposterBlock) {
                    placeKey = "composter";
                } else if (placedBlock instanceof net.minecraft.world.level.block.CauldronBlock) {
                    placeKey = "cauldron";
                } else {
                    placeKey = "generic";
                }

                sendPlaceReaction(player, level, placeKey, isWelcome, isNeutral);

                if (isUnwelcome) {
                    int oldRep = data.getReputation(player.getUUID(), villagePos);
                    data.addReputation(player.getUUID(), villagePos, -5);
                    int newRep = data.getReputation(player.getUUID(), villagePos);
                    checkAndNotifyReputationChange(player, oldRep, newRep);

                    player.sendSystemMessage(Component.translatable("villagediplomacy.sys.build_low_rep"));
                    ModLang.sendReputationSummary(player, -5, newRep);
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
        
        // Village structure blocks - LISTA COMPLETA de bloques de aldeas
        boolean isVillageBlock = 
            // Piedras
            block == Blocks.COBBLESTONE ||
            block == Blocks.MOSSY_COBBLESTONE ||
            block == Blocks.STONE ||
            block == Blocks.SMOOTH_STONE ||
            block == Blocks.STONE_BRICKS ||
            block == Blocks.MOSSY_STONE_BRICKS ||
            block == Blocks.CRACKED_STONE_BRICKS ||
            block == Blocks.CHISELED_STONE_BRICKS ||
            block == Blocks.DIORITE ||
            block == Blocks.POLISHED_DIORITE ||
            block == Blocks.ANDESITE ||
            block == Blocks.POLISHED_ANDESITE ||
            block == Blocks.GRANITE ||
            block == Blocks.POLISHED_GRANITE ||
            
            // Maderas - Planks
            block == Blocks.OAK_PLANKS ||
            block == Blocks.SPRUCE_PLANKS ||
            block == Blocks.BIRCH_PLANKS ||
            block == Blocks.ACACIA_PLANKS ||
            block == Blocks.DARK_OAK_PLANKS ||
            block == Blocks.JUNGLE_PLANKS ||
            
            // Logs
            block == Blocks.OAK_LOG ||
            block == Blocks.SPRUCE_LOG ||
            block == Blocks.BIRCH_LOG ||
            block == Blocks.ACACIA_LOG ||
            block == Blocks.DARK_OAK_LOG ||
            block == Blocks.JUNGLE_LOG ||
            block == Blocks.STRIPPED_OAK_LOG ||
            block == Blocks.STRIPPED_SPRUCE_LOG ||
            block == Blocks.STRIPPED_BIRCH_LOG ||
            
            // Stairs
            block == Blocks.COBBLESTONE_STAIRS ||
            block == Blocks.STONE_BRICK_STAIRS ||
            block == Blocks.MOSSY_COBBLESTONE_STAIRS ||
            block == Blocks.MOSSY_STONE_BRICK_STAIRS ||
            block == Blocks.DIORITE_STAIRS ||
            block == Blocks.ANDESITE_STAIRS ||
            block == Blocks.GRANITE_STAIRS ||
            block == Blocks.POLISHED_DIORITE_STAIRS ||
            block == Blocks.POLISHED_ANDESITE_STAIRS ||
            block == Blocks.POLISHED_GRANITE_STAIRS ||
            block == Blocks.OAK_STAIRS ||
            block == Blocks.SPRUCE_STAIRS ||
            block == Blocks.BIRCH_STAIRS ||
            block == Blocks.ACACIA_STAIRS ||
            block == Blocks.DARK_OAK_STAIRS ||
            
            // Slabs
            block == Blocks.COBBLESTONE_SLAB ||
            block == Blocks.STONE_SLAB ||
            block == Blocks.SMOOTH_STONE_SLAB ||
            block == Blocks.STONE_BRICK_SLAB ||
            block == Blocks.MOSSY_COBBLESTONE_SLAB ||
            block == Blocks.MOSSY_STONE_BRICK_SLAB ||
            block == Blocks.DIORITE_SLAB ||
            block == Blocks.ANDESITE_SLAB ||
            block == Blocks.GRANITE_SLAB ||
            block == Blocks.POLISHED_DIORITE_SLAB ||
            block == Blocks.POLISHED_ANDESITE_SLAB ||
            block == Blocks.POLISHED_GRANITE_SLAB ||
            block == Blocks.OAK_SLAB ||
            block == Blocks.SPRUCE_SLAB ||
            block == Blocks.BIRCH_SLAB ||
            block == Blocks.ACACIA_SLAB ||
            block == Blocks.DARK_OAK_SLAB ||
            
            // Fences y Gates
            block == Blocks.OAK_FENCE ||
            block == Blocks.SPRUCE_FENCE ||
            block == Blocks.BIRCH_FENCE ||
            block == Blocks.ACACIA_FENCE ||
            block == Blocks.DARK_OAK_FENCE ||
            block == Blocks.OAK_FENCE_GATE ||
            block == Blocks.SPRUCE_FENCE_GATE ||
            block == Blocks.BIRCH_FENCE_GATE ||
            block == Blocks.ACACIA_FENCE_GATE ||
            block == Blocks.DARK_OAK_FENCE_GATE ||
            
            // Doors
            block == Blocks.OAK_DOOR ||
            block == Blocks.SPRUCE_DOOR ||
            block == Blocks.BIRCH_DOOR ||
            block == Blocks.ACACIA_DOOR ||
            block == Blocks.DARK_OAK_DOOR ||
            block == Blocks.IRON_DOOR ||
            
            // Glass
            block == Blocks.GLASS_PANE ||
            block == Blocks.GLASS ||
            block == Blocks.WHITE_STAINED_GLASS ||
            block == Blocks.WHITE_STAINED_GLASS_PANE ||
            block == Blocks.YELLOW_STAINED_GLASS ||
            block == Blocks.YELLOW_STAINED_GLASS_PANE ||
            
            // Iluminación
            block == Blocks.TORCH ||
            block == Blocks.WALL_TORCH ||
            block == Blocks.LANTERN ||
            block == Blocks.SOUL_LANTERN ||
            
            // Otros bloques comunes de aldeas
            block == Blocks.HAY_BLOCK ||
            block == Blocks.DIRT_PATH ||
            block == Blocks.COBBLESTONE_WALL ||
            block == Blocks.MOSSY_COBBLESTONE_WALL ||
            block == Blocks.TERRACOTTA ||
            block == Blocks.WHITE_TERRACOTTA ||
            block == Blocks.BELL ||
            block == Blocks.DIRT ||
            block == Blocks.GRASS_BLOCK ||
            block == Blocks.GRAVEL ||
            block == Blocks.SAND;
        
        if (!isVillageBlock) return;
        
        // Excluir job site blocks (el jugador puede romper sus propios bloques de trabajo sin penalización)
        if (isJobSiteBlock(block)) return;
        
        // Penalize
        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, -10);
        int newRep = data.getReputation(player.getUUID(), villagePos);
        checkAndNotifyReputationChange(player, oldRep, newRep);
        
        // Buscar aldeanos cercanos para mensajes
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                AABB.ofSize(Vec3.atCenterOf(blockPos), 32, 32, 32));
        
        boolean caughtByVillager = false;
        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, player, level)) {
                caughtByVillager = true;
                break;
            }
        }
        
        if (caughtByVillager) {
            // Mensajes según reputación
            int reputation = data.getReputation(player.getUUID(), villagePos);
            Component structureMsg;
            if (reputation >= 100) {
                int i = level.getRandom().nextInt(4);
                structureMsg = Component.translatable("villagediplomacy.react.structure.friendly." + i);
            } else if (reputation >= 0) {
                int i = level.getRandom().nextInt(4);
                structureMsg = Component.translatable("villagediplomacy.react.structure.neutral." + i);
            } else {
                int i = level.getRandom().nextInt(6);
                structureMsg = Component.translatable("villagediplomacy.react.structure.hostile." + i);
            }
            player.sendSystemMessage(structureMsg);
            player.sendSystemMessage(Component.translatable("villagediplomacy.sys.structure_break"));
            ModLang.sendReputationSummary(player, -10, newRep);
        }
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
                
                Component bedDenied;
                if (reputation < -500) {
                    bedDenied = Component.translatable(
                        "villagediplomacy.react.bed.denied.criminal." + level.getRandom().nextInt(4));
                } else {
                    bedDenied = Component.translatable(
                        "villagediplomacy.react.bed.denied.low." + level.getRandom().nextInt(4));
                }
                player.sendSystemMessage(bedDenied);
                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.bed_denied"));
                
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

                Component bedMsg;
                if (caughtByBaby) {
                    bedMsg = Component.translatable(
                        "villagediplomacy.react.bed.stolen.baby." + level.getRandom().nextInt(3));
                } else {
                    bedMsg = Component.translatable(
                        "villagediplomacy.react.bed.stolen.adult." + level.getRandom().nextInt(5));
                }
                player.sendSystemMessage(bedMsg);
                player.sendSystemMessage(Component.translatable("villagediplomacy.sys.bed_use"));
                ModLang.sendReputationSummary(player, -20, newRep);

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
                            Component.translatable("villagediplomacy.react.bell.ring.neg.0").getString(),
                            Component.translatable("villagediplomacy.react.bell.ring.neg.1").getString(),
                            Component.translatable("villagediplomacy.react.bell.ring.neg.2").getString()
                        };
                    } else {
                        denialMessages = new String[]{
                            Component.translatable("villagediplomacy.react.bell.ring.neutral.0").getString(),
                            Component.translatable("villagediplomacy.react.bell.ring.neutral.1").getString(),
                            Component.translatable("villagediplomacy.react.bell.ring.neutral.2").getString()
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
                                Component.translatable("villagediplomacy.react.bell.ring.ally.0").getString(),
                                Component.translatable("villagediplomacy.react.bell.ring.ally.1").getString(),
                                Component.translatable("villagediplomacy.react.bell.ring.ally.2").getString()
                        };
                        player.sendSystemMessage(Component.literal(
                                positiveMessages[level.getRandom().nextInt(positiveMessages.length)]));
                    } else if (reputation < 100) {
                        data.addReputation(playerId, -15);
                        int newRep = data.getReputation(playerId);

                        String[] messages = {
                                Component.translatable("villagediplomacy.react.bell.spam.0").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.1").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.2").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.3").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.4").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.5").getString(),
                                Component.translatable("villagediplomacy.react.bell.spam.6").getString()
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.bell_ring"));
                        ModLang.sendReputationSummary(player, -15, newRep);
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
                                Component.translatable("villagediplomacy.react.animal.escape.0").getString(),
                                Component.translatable("villagediplomacy.react.animal.escape.1").getString(),
                                Component.translatable("villagediplomacy.react.animal.escape.2").getString(),
                                Component.translatable("villagediplomacy.react.crop.0").getString()
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.trapdoor_farm"));
                        ModLang.sendReputationSummary(player, -10, newRep);

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
                            Component.translatable("villagediplomacy.react.workblock.0", blockName).getString(),
                            Component.translatable("villagediplomacy.react.workblock.1").getString(),
                            Component.translatable("villagediplomacy.react.workblock.2").getString(),
                            Component.translatable("villagediplomacy.react.workblock.3").getString(),
                            Component.translatable("villagediplomacy.react.workblock.4").getString()
                    };

                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                    player.sendSystemMessage(Component.translatable("villagediplomacy.sys.village_block_use",
                            clickedBlock.getName()));
                    ModLang.sendReputationSummary(player, -8, newRep);

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
                                Component.translatable("villagediplomacy.react.crafting.0").getString(),
                                Component.translatable("villagediplomacy.react.crafting.1").getString(),
                                Component.translatable("villagediplomacy.react.crafting.2").getString()
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.crafting_use"));
                        ModLang.sendReputationSummary(player, -8, newRep);

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
                                Component.translatable("villagediplomacy.react.gate.adult.0").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.1").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.2").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.3").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.4").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.5").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.6").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.7").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.8").getString(),
                                Component.translatable("villagediplomacy.react.gate.adult.9").getString()
                        };

                        String[] babyMessages = {
                                Component.translatable("villagediplomacy.react.gate.baby.0").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.1").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.2").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.3").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.4").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.5").getString(),
                                Component.translatable("villagediplomacy.react.gate.baby.6").getString()
                        };

                        String message = caughtByBaby ? babyMessages[level.getRandom().nextInt(babyMessages.length)]
                                : adultMessages[level.getRandom().nextInt(adultMessages.length)];

                        player.sendSystemMessage(Component.literal(message));
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_release"));
                        ModLang.sendReputationSummary(player, -12, newRep);

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
            GolemPersonalityData personalityData = GolemPersonalityData.get(level);
            GolemPersonality personality = personalityData.getPersonality(angryGolem.getUUID());
            Object nameArg = golemStrikeNameArg(angryGolem, personality);
            if (level.getRandom().nextInt(2) == 0) {
                ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.strike.violent", 8,
                        nameArg);
            }
            return; // No procesar sistema de strikes
        }

        // Sistema de strikes normal solo si el golem NO está atacando
        List<Long> strikes = villagerAttackTimes.getOrDefault(playerId, new ArrayList<>());
        strikes.removeIf(time -> currentTime - time > STRIKE_WINDOW_MS);
        strikes.add(currentTime);
        villagerAttackTimes.put(playerId, strikes);

        int strikeCount = strikes.size();

        IronGolem closestGolem = nearbyGolems.get(0);
        GolemPersonalityData strikePersonalityData = GolemPersonalityData.get(level);
        GolemPersonality strikePersonality = strikePersonalityData.getPersonality(closestGolem.getUUID());
        Object strikeNameArg = golemStrikeNameArg(closestGolem, strikePersonality);

        if (strikeCount == 1) {
            ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.strike.warn1", 10,
                    strikeNameArg);
        } else if (strikeCount == 2) {
            ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.strike.warn2", 10,
                    strikeNameArg);
        } else if (strikeCount >= STRIKES_REQUIRED) {
            ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.strike.final", 10,
                    strikeNameArg);

            crimeCommittedTime.put(playerId, currentTime + MINOR_CRIME_DURATION_MS);
            villagerAttackTimes.remove(playerId);

            ModLang.send(player, "villagediplomacy.sys.golem_hostile_timer");

            for (IronGolem golem : nearbyGolems) {
                golem.setTarget(player);
            }
        }
    }

    private static Object golemStrikeNameArg(IronGolem golem, GolemPersonality personality) {
        if (personality != null) {
            return personality.getName();
        }
        if (golem.hasCustomName()) {
            return golem.getCustomName();
        }
        return Component.translatable("villagediplomacy.golem.generic_name");
    }

    private static int placeReactionLineCount(String placeId, String tier) {
        if ("generic".equals(placeId) && "unwelcome".equals(tier)) {
            return 6;
        }
        if ("generic".equals(placeId)) {
            return 5;
        }
        if ("bell".equals(placeId) || "lectern".equals(placeId) || "grindstone".equals(placeId)
                || "loom".equals(placeId) || "composter".equals(placeId) || "cauldron".equals(placeId)) {
            return 5;
        }
        return 6;
    }

    private static void sendPlaceReaction(ServerPlayer player, ServerLevel level, String placeId, boolean isWelcome,
            boolean isNeutral) {
        String tier = isWelcome ? "welcome" : isNeutral ? "neutral" : "unwelcome";
        int n = placeReactionLineCount(placeId, tier);
        ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.react.place." + placeId + "." + tier, n);
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
                    ModLang.send(player, "villagediplomacy.golem.forgive.one");
                } else {
                    player.sendSystemMessage(Component.translatable("villagediplomacy.golem.forgive.many", calmadosCount));
                }
            } else {
                ModLang.send(player, "villagediplomacy.golem.forgive.none");
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

    private record AnimalDeathKind(String key, int babyCount, int adultCount) {
    }

    private static AnimalDeathKind animalDeathKindFor(LivingEntity killed) {
        if (killed instanceof Cow) {
            return new AnimalDeathKind("cow", 5, 8);
        }
        if (killed instanceof Sheep) {
            return new AnimalDeathKind("sheep", 5, 8);
        }
        if (killed instanceof Pig) {
            return new AnimalDeathKind("pig", 5, 8);
        }
        if (killed instanceof Rabbit) {
            return new AnimalDeathKind("rabbit", 4, 8);
        }
        if (killed instanceof Camel) {
            return new AnimalDeathKind("camel", 5, 5);
        }
        if (killed instanceof AbstractHorse) {
            return new AnimalDeathKind("horse", 5, 10);
        }
        if (killed instanceof Chicken) {
            return new AnimalDeathKind("chicken", 3, 5);
        }
        return new AnimalDeathKind("other", 1, 1);
    }

    private record AnimalAttackKind(String key, int babyCount, int adultCount) {
    }

    private static AnimalAttackKind animalAttackKindFor(String animalType) {
        return switch (animalType) {
            case "cow" -> new AnimalAttackKind("cow", 5, 7);
            case "sheep" -> new AnimalAttackKind("sheep", 5, 7);
            case "pig" -> new AnimalAttackKind("pig", 5, 7);
            case "chicken" -> new AnimalAttackKind("chicken", 5, 7);
            case "rabbit" -> new AnimalAttackKind("rabbit", 5, 7);
            case "horse" -> new AnimalAttackKind("horse", 5, 8);
            case "camel" -> new AnimalAttackKind("camel", 5, 8);
            default -> new AnimalAttackKind("other", 1, 1);
        };
    }

    private static Villager findNearestWitnessVillager(ServerLevel level, LivingEntity killed, double radius) {
        AABB box = killed.getBoundingBox().inflate(radius);
        List<Villager> list = level.getEntitiesOfClass(Villager.class, box);
        Villager best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 center = killed.position();
        for (Villager v : list) {
            double d = v.position().distanceToSqr(center);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
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
                        String storedName = relationData.getVillageName(lastVillage);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
                        player.sendSystemMessage(Component.translatable(
                                "villagediplomacy.enter.leaving",
                                VillageDisplayName.asComponent(storedName)));
                        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
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
        String villageNameStored = relationData.getVillageName(villageId);

        boolean isDifferentVillage = lastVillage == null || !lastVillage.equals(villageId);

        if (isDifferentVillage) {
            VillageReputationData data = VillageReputationData.get(level);
            int reputation = data.getReputation(playerId, villagePos);

            String icon = reputation >= 1000 ? "§6✦"
                    : reputation >= 800 ? "§6✦"
                            : reputation >= 500 ? "§a♥"
                                    : reputation >= 300 ? "§a+"
                                            : reputation >= 100 ? "§a+"
                                                    : reputation > -100 ? "§7●"
                                                            : reputation >= -299 ? "§c-"
                                                                    : reputation >= -500 ? "§c×" : "§4☠";

            player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
            player.sendSystemMessage(Component.translatable(
                    "villagediplomacy.enter.line1",
                    Component.literal(icon),
                    VillageDisplayName.asComponent(villageNameStored)));
            player.sendSystemMessage(Component.translatable(
                    "villagediplomacy.enter.line2",
                    reputation,
                    ModLang.repStatus(reputation)));
            player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
            // Cierra HUD anterior antes de abrir el nuevo
            VillageDiplomacyNetwork.sendCloseHud(player);
            VillageDiplomacyNetwork.sendOpenHud(player, villageNameStored, reputation,
                    ModLang.hudRelationKey(reputation));

            lastVisitedVillage.put(playerId, villageId);
            greetingCooldown.put(playerId, currentTime);
        }
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
            String giftKey = "villagediplomacy.gift.generic";

            switch (profession) {
                case "farmer":
                    gift = reputation >= 1000 ? new ItemStack(Items.GOLDEN_CARROT, 3)
                            : reputation >= 800 ? new ItemStack(Items.BREAD, 6)
                            : new ItemStack(Items.CARROT, 8);
                    giftKey = "villagediplomacy.gift.farmer";
                    break;

                case "librarian":
                    gift = reputation >= 1000 ? new ItemStack(Items.ENCHANTED_BOOK)
                            : reputation >= 800 ? new ItemStack(Items.BOOK, 3)
                            : new ItemStack(Items.PAPER, 6);
                    giftKey = "villagediplomacy.gift.librarian";
                    break;

                case "armorer":
                case "weaponsmith":
                case "toolsmith":
                    gift = reputation >= 1000 ? new ItemStack(Items.DIAMOND, 1)
                            : reputation >= 800 ? new ItemStack(Items.IRON_INGOT, 4)
                            : new ItemStack(Items.IRON_INGOT, 2);
                    giftKey = "villagediplomacy.gift.smith";
                    break;

                case "cleric":
                    gift = reputation >= 1000 ? new ItemStack(Items.GOLDEN_APPLE, 1)
                            : reputation >= 800 ? new ItemStack(Items.GLISTERING_MELON_SLICE, 3)
                            : new ItemStack(Items.REDSTONE, 4);
                    giftKey = "villagediplomacy.gift.cleric";
                    break;

                case "butcher":
                    gift = reputation >= 1000 ? new ItemStack(Items.COOKED_BEEF, 6)
                            : reputation >= 800 ? new ItemStack(Items.COOKED_PORKCHOP, 4)
                            : new ItemStack(Items.COOKED_CHICKEN, 3);
                    giftKey = "villagediplomacy.gift.butcher";
                    break;

                case "cartographer":
                    gift = reputation >= 1000 ? new ItemStack(Items.MAP, 1)
                            : reputation >= 800 ? new ItemStack(Items.COMPASS, 1)
                            : new ItemStack(Items.PAPER, 8);
                    giftKey = "villagediplomacy.gift.cartographer";
                    break;

                case "fisherman":
                    gift = reputation >= 1000 ? new ItemStack(Items.COOKED_SALMON, 5)
                            : reputation >= 800 ? new ItemStack(Items.COOKED_COD, 4)
                            : new ItemStack(Items.COD, 6);
                    giftKey = "villagediplomacy.gift.fisherman";
                    break;

                case "fletcher":
                    gift = reputation >= 1000 ? new ItemStack(Items.ARROW, 16)
                            : reputation >= 800 ? new ItemStack(Items.ARROW, 10)
                            : new ItemStack(Items.STICK, 8);
                    giftKey = "villagediplomacy.gift.fletcher";
                    break;

                case "leatherworker":
                    gift = reputation >= 1000 ? new ItemStack(Items.LEATHER, 8)
                            : reputation >= 800 ? new ItemStack(Items.LEATHER, 5)
                            : new ItemStack(Items.RABBIT_HIDE, 6);
                    giftKey = "villagediplomacy.gift.leatherworker";
                    break;

                case "mason":
                    gift = reputation >= 1000 ? new ItemStack(Items.QUARTZ, 8)
                            : reputation >= 800 ? new ItemStack(Items.BRICK, 16)
                            : new ItemStack(Items.COBBLESTONE, 32);
                    giftKey = "villagediplomacy.gift.mason";
                    break;

                case "shepherd":
                    gift = reputation >= 1000 ? new ItemStack(Items.WHITE_WOOL, 8)
                            : reputation >= 800 ? new ItemStack(Items.WHITE_WOOL, 5)
                            : new ItemStack(Items.STRING, 8);
                    giftKey = "villagediplomacy.gift.shepherd";
                    break;

                default:
                    gift = reputation >= 1000 ? new ItemStack(Items.EMERALD, 2)
                            : reputation >= 800 ? new ItemStack(Items.EMERALD, 1)
                            : new ItemStack(Items.BREAD, 3);
            }

            if (gift != null) {
                if (!player.getInventory().add(gift)) {
                    player.drop(gift, false);
                }

                player.sendSystemMessage(Component.translatable(giftKey));
                
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
            if (newLevel >= 0 && newLevel <= 8) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.level." + newLevel));
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

    private void checkAndNotifyReputationChange(ServerPlayer player, int oldRep, int newRep) {
        if (ModLang.repStatusKey(oldRep).equals(ModLang.repStatusKey(newRep))) {
            return;
        }
        boolean isPositive = newRep > oldRep;
        MutableComponent emoji = Component.literal(isPositive ? "✦" : "✖");

        ModLang.send(player, "villagediplomacy.rep.notify.blank");
        ModLang.send(player, "villagediplomacy.rep.notify.bar");
        ModLang.send(player, "villagediplomacy.rep.notify.title");
        ModLang.send(player, "villagediplomacy.rep.notify.blank");

        MutableComponent oldLine = ModLang.repStatus(oldRep).copy().withStyle(ChatFormatting.GRAY);
        MutableComponent arrow = Component.translatable(
                isPositive ? "villagediplomacy.rep.notify.arrow_up" : "villagediplomacy.rep.notify.arrow_down");
        MutableComponent newLine = ModLang.repStatus(newRep).copy().withStyle(
                isPositive ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD);
        player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.line", oldLine, arrow, newLine));
        ModLang.send(player, "villagediplomacy.rep.notify.blank");

        if (isPositive) {
            if (newRep >= 1000) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.legend", emoji));
            } else if (newRep >= 800) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.hero", emoji));
            } else if (newRep >= 500) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.champion", emoji));
            } else if (newRep >= 300) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.trusted", emoji));
            } else if (newRep >= 100) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.friendly", emoji));
            } else if (newRep >= 0) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.up.neutral", emoji));
            }
        } else {
            if (newRep < -899) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.wanted", emoji));
            } else if (newRep < -699) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.enemy", emoji));
            } else if (newRep < -499) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.untrade", emoji));
            } else if (newRep < -200) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.unwelcome", emoji));
            } else if (newRep < -100) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.disliked", emoji));
            } else if (newRep < 0) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.rep.notify.down.sour", emoji));
            }
        }

        ModLang.send(player, "villagediplomacy.rep.notify.blank");
        ModLang.send(player, "villagediplomacy.rep.notify.bar");
        ModLang.send(player, "villagediplomacy.rep.notify.blank");
    }

    private BlockType categorizeBlock(Block block, ServerLevel level, BlockPos pos) {
        // Excluir job site blocks (los maneja el jugador)
        if (isJobSiteBlock(block)) {
            return BlockType.NONE;
        }
        
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
        } else if (isWell(level, pos)) {
            return BlockType.WELL;
        }
        // HOUSE y WORKSTATION ya no se procesan aquí
        // Los bloques genéricos los maneja onBlockBreakInVillage()
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

    private void sendBlockBreakVillagerLine(BlockType type, boolean isBaby, ServerLevel level, ServerPlayer player) {
        boolean useBaby = isBaby && type.babyCount > 0;
        String prefix = useBaby ? type.babyKeyPrefix : type.adultKeyPrefix;
        int count = useBaby ? type.babyCount : type.adultCount;
        ModLang.sendRandom(player, level.getRandom(), prefix, count);
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
        BELL(-50, "villagediplomacy.sys.break_bell",
                "villagediplomacy.react.break.bell.adult", 12,
                "villagediplomacy.react.break.bell.baby", 6),
        BED(-20, "villagediplomacy.sys.break_bed",
                "villagediplomacy.react.break.bed.adult", 12,
                "villagediplomacy.react.break.bed.baby", 7),
        CROP(-15, "villagediplomacy.sys.break_crop",
                "villagediplomacy.react.break.crop.adult", 14,
                "villagediplomacy.react.break.crop.baby", 6),
        WORKSTATION(-25, "villagediplomacy.sys.break_workstation",
                "villagediplomacy.react.break.workstation.adult", 14,
                "villagediplomacy.react.break.workstation.baby", 4),
        DECORATION(-5, "villagediplomacy.sys.break_decoration",
                "villagediplomacy.react.break.decoration.adult", 12,
                "", 0),
        WELL(-30, "villagediplomacy.sys.break_well",
                "villagediplomacy.react.break.well.adult", 11,
                "villagediplomacy.react.break.well.baby", 5),
        HOUSE(-15, "villagediplomacy.sys.break_house",
                "villagediplomacy.react.break.house.adult", 12,
                "villagediplomacy.react.break.house.baby", 6),
        NONE(0, "", "", 0, "", 0);

        final int penalty;
        final String systemMessageKey;
        final String adultKeyPrefix;
        final int adultCount;
        final String babyKeyPrefix;
        final int babyCount;

        BlockType(int penalty, String systemMessageKey,
                String adultKeyPrefix, int adultCount,
                String babyKeyPrefix, int babyCount) {
            this.penalty = penalty;
            this.systemMessageKey = systemMessageKey;
            this.adultKeyPrefix = adultKeyPrefix;
            this.adultCount = adultCount;
            this.babyKeyPrefix = babyKeyPrefix;
            this.babyCount = babyCount;
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
                                Component.translatable("villagediplomacy.react.criminal.0").getString(),
                                Component.translatable("villagediplomacy.react.criminal.1").getString(),
                                Component.translatable("villagediplomacy.react.flee.hostile.0").getString(),
                                Component.translatable("villagediplomacy.react.flee.hostile.1").getString(),
                                Component.translatable("villagediplomacy.react.flee.hostile.baby.0").getString()
                        }
                                : reputation <= -500 ? new String[] {
                                        Component.translatable("villagediplomacy.react.flee.neg.0").getString(),
                                        Component.translatable("villagediplomacy.react.flee.neg.1").getString(),
                                        Component.translatable("villagediplomacy.react.flee.neg.2").getString(),
                                        Component.translatable("villagediplomacy.react.flee.neg.baby.0").getString()
                                }
                                        : new String[] {
                                                Component.translatable("villagediplomacy.react.flee.low.0").getString(),
                                                Component.translatable("villagediplomacy.react.flee.low.1").getString(),
                                                Component.translatable("villagediplomacy.react.flee.low.2").getString()
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
                            Component.translatable("villagediplomacy.react.guard.0").getString(),
                            "§c[Guardia de la Aldea] *pisa agresivamente* ¡SAL DE ESTA ALDEA!",
                            Component.translatable("villagediplomacy.react.guard.1").getString(),
                            Component.translatable("villagediplomacy.react.guard.2").getString(),
                            Component.translatable("villagediplomacy.react.guard.3").getString()
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
                    Component.translatable("villagediplomacy.react.greet.hero.0", villagerName).getString(),
                    Component.translatable("villagediplomacy.react.greet.hero.1", villagerName).getString(),
                    "§6[" + villagerName + "] ¡Valiente! ¡Bienvenido de vuelta!"
                };
                case "SHY" -> new String[]{
                    Component.translatable("villagediplomacy.react.greet.hero.shy.0", villagerName).getString(),
                    Component.translatable("villagediplomacy.react.greet.hero.shy.1", villagerName).getString(),
                    "§b[" + villagerName + "] ¡Te... te admiro mucho!"
                };
                case "GREEDY" -> new String[]{
                    Component.translatable("villagediplomacy.react.greet.ally.0", villagerName).getString(),
                    Component.translatable("villagediplomacy.react.greet.ally.1", villagerName).getString(),
                    "§e[" + villagerName + "] ¡El mejor cliente! ¡Bienvenido!"
                };
                case "WISE" -> new String[]{
                    "§d[" + villagerName + "] Saludos, legendario.",
                    "§d[" + villagerName + "] Tu reputación te precede, héroe.",
                    "§d[" + villagerName + "] La aldea te debe mucho."
                };
                case "GOSSIP" -> new String[]{
                    "§a[" + villagerName + "] ¡HÉROE! ¡Todos hablan de ti!",
                    "§a[" + villagerName + "] ¿¡Escuchaste! ¡Oh espera, TÚ ERES el héroe!",
                    "§a[" + villagerName + "] ¡Toda la aldea te ama!"
                };
                case "CHEERFUL" -> new String[]{
                    "§a[" + villagerName + "] ¡GENIAL! ¡Nuestro héroe está aquí!",
                    "§a[" + villagerName + "] ¡El mejor día! ¡Has vuelto!",
                    "§a[" + villagerName + "] *salta emocionado* ¡HÉROE!"
                };
                default -> new String[]{
                    "§6[" + villagerName + "] ¡Bienvenido de vuelta, héroe!",
                    "§6[" + villagerName + "] ¡Bueno verte, campeón!",
                    "§6[" + villagerName + "] ¡La aldea está segura contigo aquí!"
                };
            };
        }
        
        // Mensajes para ALLY (200-499)
        return switch (temperament) {
            case "BRAVE" -> new String[]{
                "§a[" + villagerName + "] ¡Buen día, amigo!",
                "§a[" + villagerName + "] ¡Aliado! ¡Bienvenido!",
                "§a[" + villagerName + "] ¡Un placer verte!"
            };
            case "SHY" -> new String[]{
                "§b[" + villagerName + "] *sonríe* Hola...",
                "§b[" + villagerName + "] *saluda* ¡H-hola!",
                "§b[" + villagerName + "] ¡Oh! Hola, amigo..."
            };
            case "GREEDY" -> new String[]{
                "§e[" + villagerName + "] ¡Mi cliente favorito!",
                "§e[" + villagerName + "] ¡Buenos tratos hoy, amigo!",
                "§e[" + villagerName + "] ¡Siempre es un placer!"
            };
            case "WISE" -> new String[]{
                "§d[" + villagerName + "] Saludos, de confianza.",
                "§d[" + villagerName + "] La paz sea contigo, amigo.",
                "§d[" + villagerName + "] Bienvenido, aliado."
            };
            case "GOSSIP" -> new String[]{
                "§a[" + villagerName + "] ¡Oh! ¡Hola! ¿¡Escuchaste sobre...!?",
                "§a[" + villagerName + "] ¡Perfecto momento! ¡Tengo noticias!",
                "§a[" + villagerName + "] ¡Tú! ¡Justo estaba hablando de ti!"
            };
            case "FRIENDLY" -> new String[]{
                "§a[" + villagerName + "] ¡Hola, amigo!",
                "§a[" + villagerName + "] ¡Genial verte!",
                "§a[" + villagerName + "] ¿Cómo te va, amigo?"
            };
            case "CHEERFUL" -> new String[]{
                "§a[" + villagerName + "] ¡Hola! ¡Qué día tan hermoso!",
                "§a[" + villagerName + "] *sonríe* ¡Hola, amigo!",
                "§a[" + villagerName + "] ¡Maravilloso verte!"
            };
            default -> new String[]{
                "§a[" + villagerName + "] ¡Hola, amigo!",
                "§a[" + villagerName + "] ¡Buen día!",
                "§a[" + villagerName + "] ¡Bienvenido!"
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

            curer.sendSystemMessage(Component.translatable("villagediplomacy.sys.cure_zombie"));
            ModLang.sendReputationSummary(curer, 100, newRep);
            checkAndNotifyReputationChange(curer, oldRep, newRep);
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