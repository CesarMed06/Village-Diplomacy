package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.GolemPersonalityData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import com.cesoti2006.villagediplomacy.personality.GolemPersonality;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.integration.guardvillagers.GuardVillagersCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.warden.Warden;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.CauldronBlock;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.*;

public class FabricEventHandler {

    private final Map<UUID, Long> tradeCooldowns = new HashMap<>();
    private final Map<UUID, Long> crimeCommittedTime = new HashMap<>();
    private final Map<UUID, Integer> lastReputationLevel = new HashMap<>();
    private final Map<UUID, Long> greetingCooldown = new HashMap<>();
    private final Map<UUID, String> lastVisitedVillage = new HashMap<>();
    private final Map<UUID, List<Long>> villagerAttackTimes = new HashMap<>();
    private final Map<UUID, Long> chestLootCooldown = new HashMap<>();
    private final Map<UUID, Integer> chestOpenReputation = new HashMap<>();
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
    private final Map<UUID, Map<UUID, Integer>> golemStrikesPerGolem = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> golemLastHitTime = new HashMap<>();
    private final Map<UUID, Long> lastGolemHitTime = new HashMap<>();
    private final Map<UUID, Long> golemForgivenessTime = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> villagerGreetingCooldown = new HashMap<>();
    private final Map<UUID, UUID> zombieVillagerCurers = new HashMap<>();
    private final Map<UUID, Long> playerLoginTime = new HashMap<>();
    private final Map<UUID, Map<Integer, ItemStack>> chestSnapshot = new HashMap<>();
    private final Map<UUID, BlockPos> chestOpenPosition = new HashMap<>();
    private final Map<UUID, UUID> lastChestMenuPlayer = new HashMap<>();
    private final Map<UUID, long[]> trackedExplosiveTnt = new HashMap<>(); // UUID -> [posX, posY, posZ, timestamp]

    private static final long TRADE_WINDOW_MS = 500;
    private static final long MAJOR_CRIME_DURATION_MS = 120000;
    private static final long MINOR_CRIME_DURATION_MS = 30000;
    private static final long GREETING_COOLDOWN_MS = 600000;
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
    private static final long GOLEM_RESET_COOLDOWN_MS = 1000;

    private final Map<UUID, Long> golemResetCooldown = new HashMap<>();

    private static final int THEFT_CHEST_ADULT = 18;
    private static final int THEFT_CHEST_BABY = 13;
    private static final int THEFT_LOOT_ADULT = 15;
    private static final int THEFT_LOOT_BABY = 10;

    public void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onVillagerDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onIronGolemDeath);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onGolemAttack);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onVillagerAttack);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onAnimalAttack);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onHostileMobKill);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onAnimalDeath);
        
        UseBlockCallback.EVENT.register(this::onDoorOpen);
        UseBlockCallback.EVENT.register(this::onChestOpen);
        UseBlockCallback.EVENT.register(this::onBellRing);
        UseBlockCallback.EVENT.register(this::onTrapdoorOpen);
        UseBlockCallback.EVENT.register(this::onCraftingTableUse);
        UseBlockCallback.EVENT.register(this::onFenceGateOpen);
        
        PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreak);
        PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreakInVillage);
        
        ServerPlayConnectionEvents.JOIN.register(this::onPlayerLogin);
        ServerPlayConnectionEvents.DISCONNECT.register(this::onPlayerLogout);
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onGuardAttack);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onGuardDeath);

        // Zombie villager curing detection
        UseEntityCallback.EVENT.register(this::onZombieVillagerInteract);
        // Bed sleep detection
        UseBlockCallback.EVENT.register(this::onBedUse);
        UseBlockCallback.EVENT.register(this::onBlockPlace);
    }


    private net.minecraft.world.InteractionResult onZombieVillagerInteract(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(entity instanceof ZombieVillager zombie))
            return net.minecraft.world.InteractionResult.PASS;

        // Check if player is using golden apple on zombie with weakness
        var held = serverPlayer.getItemInHand(hand);
        if (held.getItem() != Items.GOLDEN_APPLE)
            return net.minecraft.world.InteractionResult.PASS;
        if (!zombie.hasEffect(MobEffects.WEAKNESS))
            return net.minecraft.world.InteractionResult.PASS;

        // Track this curing attempt
        zombieVillagerCurers.put(zombie.getUUID(), serverPlayer.getUUID());
        return net.minecraft.world.InteractionResult.PASS;
    }

    private void onVillagerDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof AbstractVillager))
            return;
        if (!(entity.level() instanceof ServerLevel level))
            return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player))
            return;

        AbstractVillager villager = (AbstractVillager) entity;
        BlockPos villagerPos = villager.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty())
            return;

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get(), level);

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();

        int reputationLoss = villager.isBaby() ? -200 : -100;
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, reputationLoss);

        int newRep = data.getReputation(player.getUUID(), villagePos);
        checkAndNotifyReputationChange(player, oldRep, newRep);

        if (villager.isBaby()) {
            ModLang.sendDialogRandom(player, level.getRandom(), villager, "villagediplomacy.react.villagerdeath.baby", 4);
        } else {
            ModLang.sendDialogRandom(player, level.getRandom(), villager, "villagediplomacy.react.villagerdeath.adult", 4);
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

    private void onIronGolemDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof IronGolem))
            return;
        if (!(entity.level() instanceof ServerLevel level))
            return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player))
            return;

        IronGolem golem = (IronGolem) entity;
        BlockPos golemPos = golem.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, golemPos, 200);

        if (nearestVillage.isEmpty())
            return;

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get(), level);

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

    private boolean onGolemAttack(LivingEntity entity, DamageSource damageSource, float originalDamage) {
        if (!(entity instanceof IronGolem golem))
            return true;
        if (golem.isPlayerCreated())
            return true;
        if (!(damageSource.getEntity() instanceof ServerPlayer player))
            return true;
        if (!(golem.level() instanceof ServerLevel level))
            return true;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, golem.blockPosition(), 200);
        if (nearestVillage.isEmpty())
            return true;

        UUID golemId = golem.getUUID();
        long currentTime = System.currentTimeMillis();

        golemLastHitTime.putIfAbsent(player.getUUID(), new HashMap<>());
        Map<UUID, Long> playerGolemHitTimes = golemLastHitTime.get(player.getUUID());
        
        Long lastHit = playerGolemHitTimes.get(golemId);
        if (lastHit == null || currentTime - lastHit > 5000) {
            playerGolemHitTimes.put(golemId, currentTime);
            
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
        
        return true;
    }

    private boolean onVillagerAttack(LivingEntity entity, DamageSource damageSource, float originalDamage) {
        if (!(entity instanceof AbstractVillager))
            return true;
        if (!(damageSource.getEntity() instanceof ServerPlayer player))
            return true;
        if (!(entity.level() instanceof ServerLevel level))
            return true;

        AbstractVillager villager = (AbstractVillager) entity;
        BlockPos villagerPos = villager.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty())
            return true;

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        data.addReputation(player.getUUID(), villagePos, -10);
        
        spawnNegativeFeedback(level, villager);

        int newRep = data.getReputation(player.getUUID(), villagePos);

        ModLang.sendDialogRandom(player, level.getRandom(), villager,
                newRep >= 75 ? "villagediplomacy.dialog.friendly" :
                newRep >= 25 ? "villagediplomacy.dialog.neutral" :
                newRep < 0   ? "villagediplomacy.dialog.hostile" :
                               "villagediplomacy.dialog.greeting",
                newRep >= 75 ? 7 : newRep >= 25 ? 6 : newRep < 0 ? 6 : 7);
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.villager_attacked",
                -10, newRep, ModLang.repStatus(newRep)));

        checkReputationLevelChange(player, level, newRep);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(nearestVillage.get(), level);

        processStrikeSystem(player, level, villagerPos);
        
        return true;
    }

    private boolean onAnimalAttack(LivingEntity entity, DamageSource damageSource, float originalDamage) {
        if (!(damageSource.getEntity() instanceof ServerPlayer player))
            return true;
        if (!(entity.level() instanceof ServerLevel level))
            return true;

        String animalType = null;
        if (entity instanceof Cow) animalType = "cow";
        else if (entity instanceof Sheep) animalType = "sheep";
        else if (entity instanceof Pig) animalType = "pig";
        else if (entity instanceof Chicken) animalType = "chicken";
        
        if (animalType == null) return true;

        BlockPos animalPos = entity.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, animalPos, 200);

        if (nearestVillage.isPresent() && isAnimalInEnclosure(level, animalPos)) {
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
                        ModLang.sendDialogRandom(player, level.getRandom(), villager,
                                "villagediplomacy.react.animalattack." + attackKind.key() + suffix, lineCount);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_attack_warn",
                                Component.translatable("entity.minecraft." + animalType)));
                        ModLang.sendReputationSummary(player, -5, newRep);

                        tradeCooldowns.put(playerId, currentTime);
                    }
                    break;
                }
            }
        }
        
        return true;
    }

    private void onHostileMobKill(LivingEntity entity, DamageSource damageSource) {
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        
        boolean isHostile = entity instanceof Monster
            || entity instanceof Slime
            || entity instanceof MagmaCube
            || entity instanceof Ghast
            || entity instanceof Phantom
            || entity instanceof Shulker
            || entity instanceof ElderGuardian
            || entity instanceof Guardian
            || entity instanceof Hoglin
            || entity instanceof Zoglin
            || entity instanceof WitherBoss
            || entity instanceof EnderDragon;
        if (!isHostile) return;

        LivingEntity killed = entity;
        BlockPos deathPos = killed.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, deathPos, 200);
        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();

        List<AbstractVillager> nearbyVillagers = level.getEntitiesOfClass(
                AbstractVillager.class, AABB.ofSize(Vec3.atCenterOf(deathPos), 32, 32, 32));
        boolean witnessed = false;
        for (AbstractVillager v : nearbyVillagers) {
            if (hasLineOfSight(v, player, level)) { witnessed = true; break; }
        }
        if (!witnessed) return;

        HostileKillKind kind = hostileKillKindFor(killed);
        VillageReputationData data = VillageReputationData.get(level);
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, kind.repBonus());
        int newRep = data.getReputation(player.getUUID(), villagePos);
        checkAndNotifyReputationChange(player, oldRep, newRep);

        AbstractVillager hostileWitness = nearbyVillagers.stream()
                .filter(v -> hasLineOfSight(v, player, level)).findFirst().orElse(null);
        if (hostileWitness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), hostileWitness,
                    "villagediplomacy.react.hostilekill." + kind.key() + "." + ModLang.repTier(newRep), kind.lineCount());
        } else {
            ModLang.sendRandom(player, level.getRandom(),
                    "villagediplomacy.react.hostilekill." + kind.key() + "." + ModLang.repTier(newRep), kind.lineCount());
        }
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.hostile_killed",
                Component.translatable(killed.getType().getDescriptionId()).getString(),
                kind.repBonus(), newRep, ModLang.repStatus(newRep)));

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);

        // Guard Villagers witness bonus
        if (GuardVillagersCompat.isLoaded() && VillageDiplomacyConfig.repGuardWitness > 0) {
            if (GuardVillagersCompat.hasGuardsNearby(level, deathPos, 48.0)) {
                int bonus = VillageDiplomacyConfig.repGuardWitness;
                data.addReputation(player.getUUID(), villagePos, bonus);
                int postBonus = data.getReputation(player.getUUID(), villagePos);
                player.sendSystemMessage(Component.translatable(
                    "villagediplomacy.guard.witness_bonus", bonus, postBonus));
                GuardVillagersCompat.sendGuardKillReaction(player, level, postBonus);
            }
        }
    }

    private void onAnimalDeath(LivingEntity entity, DamageSource damageSource) {
        if (entity instanceof Monster) return;
        if (entity instanceof Slime) return;
        if (entity instanceof MagmaCube) return;
        if (entity instanceof Ghast) return;
        if (entity instanceof Phantom) return;
        if (entity instanceof ElderGuardian) return;
        if (entity instanceof Guardian) return;
        if (entity instanceof Shulker) return;
        if (entity instanceof Endermite) return;
        if (entity instanceof Silverfish) return;
        if (entity instanceof Blaze) return;
        if (entity instanceof WitherSkeleton) return;
        if (entity instanceof Stray) return;
        if (entity instanceof Vex) return;
        if (entity instanceof Illusioner) return;
        if (entity instanceof ZombifiedPiglin) return;
        if (entity instanceof Piglin) return;
        if (entity instanceof PiglinBrute) return;
        if (entity instanceof Hoglin) return;
        if (entity instanceof Zoglin) return;
        if (entity instanceof WitherBoss) return;
        if (entity instanceof EnderDragon) return;
        if (entity instanceof EnderMan) return;
        if (entity instanceof Warden) return;
        if (entity instanceof Villager) return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        LivingEntity killed = entity;
        BlockPos deathPos = killed.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, deathPos, 200);
        if (nearestVillage.isEmpty()) return;

        BlockPos villagePos = nearestVillage.get();

        if (!isAnimalInEnclosure(level, deathPos)) return;

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
        if (witness != null) {
            ModLang.sendDialogRandom(player, level.getRandom(), witness,
                    "villagediplomacy.react.animaldeath." + react.key + suffix, lineCount);
        } else {
            ModLang.sendRandom(player, level.getRandom(),
                    "villagediplomacy.react.animaldeath." + react.key + suffix, lineCount);
        }

        String killedAnimalName = killed instanceof Cow ? "Vaca"
                : killed instanceof Sheep ? "Oveja"
                : killed instanceof Pig ? "Cerdo"
                : killed instanceof Chicken ? "Pollo"
                : killed.getType().getDescriptionId();
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_killed",
                killedAnimalName,
                newRep,
                ModLang.repStatus(newRep)));

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);
    }


    private net.minecraft.world.InteractionResult onBedUse(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        if (!(serverLevel.getBlockState(clickedPos).getBlock() instanceof BedBlock))
            return net.minecraft.world.InteractionResult.PASS;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);
        if (nearestVillage.isEmpty())
            return net.minecraft.world.InteractionResult.PASS;

        VillageReputationData data = VillageReputationData.get(serverLevel);
        UUID playerId = serverPlayer.getUUID();
        int reputation = data.getReputation(playerId, nearestVillage.get());

        // Deny sleep to criminals and enemies
        if (reputation < -400) {
            String denyKey = reputation < -600
                ? "villagediplomacy.react.bed.denied.criminal." + serverLevel.getRandom().nextInt(4)
                : "villagediplomacy.react.bed.denied.low." + serverLevel.getRandom().nextInt(4);
            serverPlayer.sendSystemMessage(Component.translatable(denyKey));
            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.bed_denied"));
            return net.minecraft.world.InteractionResult.FAIL;
        }

        long currentTime = System.currentTimeMillis();

        if (bedUsageCooldown.containsKey(playerId) &&
                currentTime - bedUsageCooldown.get(playerId) < BED_COOLDOWN_MS) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                Villager.class,
                serverPlayer.getBoundingBox().inflate(16.0D));

        boolean caughtByVillager = false;
        boolean caughtByBaby = false;

        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                caughtByVillager = true;
                if (villager.isBaby()) caughtByBaby = true;
                break;
            }
        }

        if (caughtByVillager) {
            int oldRep = data.getReputation(playerId, nearestVillage.get());
            data.addReputation(playerId, nearestVillage.get(), -20);
            int newRep = data.getReputation(playerId, nearestVillage.get());
            checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

            String bedPrefix = caughtByBaby
                ? "villagediplomacy.react.bed.caught.baby" : "villagediplomacy.react.bed.caught.adult";
            int bedCount = caughtByBaby ? 3 : 5;
            serverPlayer.sendSystemMessage(Component.translatable(bedPrefix + "." + serverLevel.getRandom().nextInt(bedCount)));
            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.bed_use"));
            ModLang.sendReputationSummary(serverPlayer, -20, newRep);

            bedUsageCooldown.put(playerId, currentTime);

            VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
            relationData.registerVillage(nearestVillage.get(), serverLevel);
        }

        return net.minecraft.world.InteractionResult.PASS;
    }


    private net.minecraft.world.InteractionResult onBlockPlace(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        // Only detect when player is holding a block item to place
        var heldItem = serverPlayer.getItemInHand(hand);
        if (!(heldItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItem))
            return net.minecraft.world.InteractionResult.PASS;

        Block placedBlock = blockItem.getBlock();

        // Skip TNT, lava, fire (handled by FireDamageHandler)
        if (placedBlock == Blocks.TNT || placedBlock == Blocks.LAVA || placedBlock == Blocks.FIRE || placedBlock == Blocks.SOUL_FIRE)
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, placedPos, 200);
        if (nearestVillage.isEmpty())
            return net.minecraft.world.InteractionResult.PASS;

        // Check for villager witness
        List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                Villager.class,
                new AABB(placedPos).inflate(32));

        boolean caughtByVillager = false;
        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                caughtByVillager = true;
                break;
            }
        }

        if (!caughtByVillager)
            return net.minecraft.world.InteractionResult.PASS;

        VillageReputationData data = VillageReputationData.get(serverLevel);
        BlockPos villagePos = nearestVillage.get();
        int reputation = data.getReputation(serverPlayer.getUUID(), villagePos);

        // Determine block category for dialog
        String placeKey;
        if (placedBlock instanceof BedBlock) {
            placeKey = "bed";
        } else if (placedBlock instanceof ChestBlock || placedBlock instanceof BarrelBlock) {
            placeKey = "chest";
        } else if (placedBlock instanceof FurnaceBlock || placedBlock instanceof BlastFurnaceBlock || placedBlock instanceof SmokerBlock) {
            placeKey = "furnace";
        } else if (placedBlock instanceof CraftingTableBlock) {
            placeKey = "crafting";
        } else if (placedBlock instanceof BellBlock) {
            placeKey = "bell";
        } else if (placedBlock instanceof BrewingStandBlock) {
            placeKey = "brewing";
        } else if (placedBlock instanceof EnchantmentTableBlock) {
            placeKey = "enchanting";
        } else if (placedBlock == Blocks.BOOKSHELF) {
            placeKey = "bookshelf";
        } else if (placedBlock instanceof LecternBlock) {
            placeKey = "lectern";
        } else if (placedBlock instanceof AnvilBlock) {
            placeKey = "anvil";
        } else if (placedBlock instanceof GrindstoneBlock) {
            placeKey = "grindstone";
        } else if (placedBlock instanceof LoomBlock) {
            placeKey = "loom";
        } else if (placedBlock instanceof ComposterBlock) {
            placeKey = "composter";
        } else if (placedBlock instanceof CauldronBlock) {
            placeKey = "cauldron";
        } else {
            placeKey = "generic";
        }

        boolean isWelcome = reputation >= 100;
        boolean isUnwelcome = reputation < 0;

        // Send place reaction dialog (Component.translatable shows raw key if missing, no exception)
        String tier = isWelcome ? "welcome" : isUnwelcome ? "unwelcome" : "neutral";
        serverPlayer.sendSystemMessage(Component.translatable(
            "villagediplomacy.react.place." + tier + "." + placeKey + "." + serverLevel.getRandom().nextInt(4)));

        // Penalize if unwelcome
        if (isUnwelcome) {
            int oldRep = data.getReputation(serverPlayer.getUUID(), villagePos);
            data.addReputation(serverPlayer.getUUID(), villagePos, -5);
            int newRep = data.getReputation(serverPlayer.getUUID(), villagePos);
            checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);
            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.build_low_rep"));
            ModLang.sendReputationSummary(serverPlayer, -5, newRep);
        }

        VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
        relationData.registerVillage(villagePos, serverLevel);

        return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onDoorOpen(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        Block clickedBlock = serverLevel.getBlockState(clickedPos).getBlock();

        if (!(clickedBlock instanceof DoorBlock))
            return net.minecraft.world.InteractionResult.PASS;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);
        if (nearestVillage.isEmpty())
            return net.minecraft.world.InteractionResult.PASS;

        UUID playerId = serverPlayer.getUUID();
        long currentTime = System.currentTimeMillis();

        if (doorOpenCooldown.containsKey(playerId) &&
                currentTime - doorOpenCooldown.get(playerId) < 1500) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        VillageReputationData data = VillageReputationData.get(serverLevel);
        int reputation = data.getReputation(playerId, nearestVillage.get());

        boolean doorIsOpen = serverLevel.getBlockState(clickedPos).getValue(DoorBlock.OPEN);
        boolean isClosing = doorIsOpen;

        long dayTime = serverLevel.getDayTime() % 24000;
        boolean isNight = dayTime >= 13000 && dayTime < 23000;
        boolean isMorning = dayTime >= 0 && dayTime < 6000;

        List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                Villager.class,
                serverPlayer.getBoundingBox().inflate(20.0D));

        boolean caughtByVillager = false;
        boolean caughtByBaby = false;
        Villager witnessVillager = null;

        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
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
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.close.baby", 4);
                    } else {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.close.adult", 6);
                    }
                } else if (caughtByBaby) {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.open.baby", 5);
                } else if (isNight) {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.open.night", 4);
                } else if (isMorning) {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.open.morning", 4);
                } else {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.high.open.day", 8);
                }

            } else if (reputation >= 100) {
                if (serverLevel.getRandom().nextInt(2) == 0) {
                    if (isClosing) {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.neutral.close", 4);
                    } else if (isNight) {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.neutral.open.night", 3);
                    } else {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.neutral.open.day", 6);
                    }
                }

            } else if (reputation >= -99) {
                if (serverLevel.getRandom().nextInt(2) == 0) {
                    if (caughtByBaby) {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.low.baby", 3);
                    } else {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.low.adult", 6);
                    }
                }

            } else {
                data.addReputation(playerId, nearestVillage.get(), -5);
                int newRep = data.getReputation(playerId, nearestVillage.get());

                if (caughtByBaby) {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.neg.baby", 6);
                } else {
                    ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), witnessVillager, "villagediplomacy.react.door.neg.adult", 10);
                }
                serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.trespass_door"));
                ModLang.sendReputationSummary(serverPlayer, -5, newRep);
            }
        }
        
        return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onChestOpen(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        Block clickedBlock = serverLevel.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof ChestBlock || clickedBlock instanceof BarrelBlock) {
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(serverLevel);
                UUID playerId = serverPlayer.getUUID();
                BlockPos villagePos = nearestVillage.get();

                List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                        Villager.class,
                        serverPlayer.getBoundingBox().inflate(12.0D));

                boolean caughtByVillager = false;
                boolean caughtByBaby = false;
                Villager spottingVillager = null;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                        caughtByVillager = true;
                        spottingVillager = villager;
                        if (villager.isBaby()) {
                            caughtByBaby = true;
                        }
                        break;
                    }
                }

                if (caughtByVillager) {
                    int reputation = data.getReputation(playerId, villagePos);
                    chestOpenReputation.put(playerId, reputation);
                    lastChestMenuPlayer.put(playerId, playerId);
                    int penalty = reputation >= 800 ? -5 : -10; 
                    int oldRep = data.getReputation(serverPlayer.getUUID(), villagePos);
                    data.addReputation(serverPlayer.getUUID(), villagePos, penalty);
                    int newRep = data.getReputation(serverPlayer.getUUID(), villagePos);
                    checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

                    if (reputation >= 800) {
                        int idx = serverLevel.getRandom().nextInt(4);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.theft.chest.hero." + idx));
                    } else if (caughtByBaby) {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), spottingVillager, "villagediplomacy.react.theft.chest.baby", THEFT_CHEST_BABY);
                    } else {
                        ModLang.sendDialogRandom(serverPlayer, serverLevel.getRandom(), spottingVillager, "villagediplomacy.react.theft.chest.adult", THEFT_CHEST_ADULT);
                    }

                    serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.chest_open",
                            penalty, newRep, ModLang.repStatus(newRep)));

                    VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                    relationData.registerVillage(villagePos, serverLevel);
                }
            }
        }
        
        return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onBellRing(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        Block clickedBlock = serverLevel.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof BellBlock) {
            if (hitResult.getLocation().y < clickedPos.getY() + 0.1) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(serverLevel);
                UUID playerId = serverPlayer.getUUID();
                BlockPos villagePos = nearestVillage.get();
                int reputation = data.getReputation(playerId, villagePos);
                
                if (reputation < -200) {
                    String prefix = reputation < -500 ? "villagediplomacy.react.bell.ring.neg" : "villagediplomacy.react.bell.ring.neutral";
                    int idx = serverLevel.getRandom().nextInt(3);
                    serverPlayer.sendSystemMessage(Component.translatable(prefix + "." + idx));
                    return net.minecraft.world.InteractionResult.FAIL;
                }
                
                long currentTime = System.currentTimeMillis();

                if (bellRingCooldown.containsKey(playerId) &&
                        currentTime - bellRingCooldown.get(playerId) < BELL_COOLDOWN_MS) {
                    return net.minecraft.world.InteractionResult.PASS;
                }

                List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                        Villager.class,
                        serverPlayer.getBoundingBox().inflate(20.0D));

                boolean caughtByVillager = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                        caughtByVillager = true;
                        break;
                    }
                }

                if (caughtByVillager) {
                    if (reputation >= 500) {
                        int idx = serverLevel.getRandom().nextInt(3);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.bell.ring.ally." + idx));
                    } else if (reputation < 100) {
                        data.addReputation(playerId, villagePos, -15);
                        int newRep = data.getReputation(playerId, villagePos);
                        int idx = serverLevel.getRandom().nextInt(7);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.bell.spam." + idx));
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.bell_ring"));
                        ModLang.sendReputationSummary(serverPlayer, -15, newRep);
                    }

                    bellRingCooldown.put(playerId, currentTime);
                    VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                    relationData.registerVillage(nearestVillage.get(), serverLevel);
                }
            }
        }
        
 return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onTrapdoorOpen(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();

        if (serverLevel.getBlockState(clickedPos).getBlock() instanceof TrapDoorBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                boolean isFarmTrapdoor = false;

                for (int x = -3; x <= 3; x++) {
                    for (int y = -2; y <= 2; y++) {
                        for (int z = -3; z <= 3; z++) {
                            BlockPos checkPos = clickedPos.offset(x, y, z);
                            Block block = serverLevel.getBlockState(checkPos).getBlock();

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

                            List<net.minecraft.world.entity.animal.Animal> animals = serverLevel
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
                    UUID playerId = serverPlayer.getUUID();

                    if (trapdoorCooldown.containsKey(playerId) &&
                            currentTime - trapdoorCooldown.get(playerId) < TRAPDOOR_COOLDOWN_MS) {
                        return net.minecraft.world.InteractionResult.PASS;
                    }

                    List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                            Villager.class,
                            serverPlayer.getBoundingBox().inflate(12.0D));

                    boolean caughtByVillager = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                            caughtByVillager = true;
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(serverLevel);
                        int oldRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        data.addReputation(serverPlayer.getUUID(), nearestVillage.get(), -10);
                        int newRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

                        String[] escapeKeys = {
                                "villagediplomacy.react.animal.escape.0",
                                "villagediplomacy.react.animal.escape.1",
                                "villagediplomacy.react.animal.escape.2",
                                "villagediplomacy.react.crop.0"
                        };
                        serverPlayer.sendSystemMessage(Component.translatable(
                                escapeKeys[serverLevel.getRandom().nextInt(escapeKeys.length)]));
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.trapdoor_farm"));
                        ModLang.sendReputationSummary(serverPlayer, -10, newRep);

                        trapdoorCooldown.put(playerId, currentTime);

                        VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                        relationData.registerVillage(nearestVillage.get(), serverLevel);
                    }
                }
            }
        }
        
     return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onCraftingTableUse(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        Block clickedBlock = serverLevel.getBlockState(clickedPos).getBlock();
        
        if (clickedBlock instanceof FurnaceBlock || 
            clickedBlock instanceof BlastFurnaceBlock || clickedBlock instanceof SmokerBlock ||
            clickedBlock instanceof BrewingStandBlock || clickedBlock instanceof LoomBlock ||
            clickedBlock instanceof SmithingTableBlock || clickedBlock instanceof CartographyTableBlock ||
            clickedBlock instanceof FletchingTableBlock || clickedBlock instanceof GrindstoneBlock ||
            clickedBlock instanceof StonecutterBlock || clickedBlock instanceof ComposterBlock) {
            
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);
            if (nearestVillage.isPresent()) {
                VillageReputationData data = VillageReputationData.get(serverLevel);
                UUID playerId = serverPlayer.getUUID();
                BlockPos villagePos = nearestVillage.get();
                int reputation = data.getReputation(playerId, villagePos);
                
                List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                        Villager.class,
                        serverPlayer.getBoundingBox().inflate(16.0D));

                boolean caughtByVillager = false;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                        caughtByVillager = true;
                        break;
                    }
                }

                if (caughtByVillager) {
                    int penalty;
                    if (reputation >= 500) {
                        penalty = 0; 
                    } else if (reputation >= 300) {
                        penalty = -4; 
                    } else {
                        penalty = -8;
                    }
                    if (penalty < 0) {
                        data.addReputation(playerId, villagePos, penalty);
                    }
                    int newRep = data.getReputation(playerId, villagePos);

                    String blockName = "workstation";
                    if (clickedBlock instanceof FurnaceBlock || clickedBlock instanceof BlastFurnaceBlock || clickedBlock instanceof SmokerBlock) {
                        blockName = "furnace";
                    } else if (clickedBlock instanceof CraftingTableBlock) {
                        blockName = "crafting table";
                    } else if (clickedBlock instanceof BrewingStandBlock) {
                        blockName = "brewing stand";
                    } else if (clickedBlock instanceof LoomBlock) {
                        blockName = "loom";
                    } else if (clickedBlock instanceof SmithingTableBlock) {
                        blockName = "smithing table";
                    } else if (clickedBlock instanceof CartographyTableBlock) {
                        blockName = "cartography table";
                    } else if (clickedBlock instanceof FletchingTableBlock) {
                        blockName = "fletching table";
                    } else if (clickedBlock instanceof GrindstoneBlock) {
                        blockName = "grindstone";
                    } else if (clickedBlock instanceof StonecutterBlock) {
                        blockName = "stonecutter";
                    } else if (clickedBlock instanceof ComposterBlock) {
                        blockName = "composter";
                    }

                    if (reputation >= 500) {
                        int idx = serverLevel.getRandom().nextInt(4);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.workblock.trusted." + idx, blockName));
                    } else if (reputation >= 300) {
                        int idx = serverLevel.getRandom().nextInt(4);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.workblock.friendly." + idx, blockName));
                    } else {
                        int wbIdx = serverLevel.getRandom().nextInt(5);
                        if (wbIdx == 0) {
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.workblock.0", blockName));
                        } else {
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.workblock." + wbIdx));
                        }
                    }
                    serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.village_block_use",
                            clickedBlock.getName()));
                    if (penalty < 0) {
                        ModLang.sendReputationSummary(serverPlayer, penalty, newRep);
                    } else {
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.no_penalty_trusted"));
                    }

                    VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                    relationData.registerVillage(nearestVillage.get(), serverLevel);
                }
            }
        }

        if (serverLevel.getBlockState(clickedPos).getBlock() instanceof CraftingTableBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                boolean isInHouse = false;

                for (int y = -2; y <= 4; y++) {
                    for (int x = -4; x <= 4; x++) {
                        for (int z = -4; z <= 4; z++) {
                            BlockPos checkPos = clickedPos.offset(x, y, z);
                            if (serverLevel.getBlockState(checkPos).getBlock() instanceof BedBlock) {
                                isInHouse = true;
                                break;
                            }
                        }
                    }
                }

                if (isInHouse) {
                    long currentTime = System.currentTimeMillis();
                    UUID playerId = serverPlayer.getUUID();

                    if (craftingTableCooldown.containsKey(playerId) &&
                            currentTime - craftingTableCooldown.get(playerId) < CRAFTING_COOLDOWN_MS) {
                        return net.minecraft.world.InteractionResult.PASS;
                    }

                    List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                            Villager.class,
                            serverPlayer.getBoundingBox().inflate(10.0D));

                    boolean caughtByVillager = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                            caughtByVillager = true;
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(serverLevel);
                        int reputation = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        int penalty;
                        if (reputation >= 500) {
                            penalty = 0; 
                        } else if (reputation >= 300) {
                            penalty = -4; 
                        } else {
                            penalty = -8;
                        }
                        int oldRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        if (penalty < 0) {
                            data.addReputation(serverPlayer.getUUID(), nearestVillage.get(), penalty);
                        }
                        int newRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

                        if (reputation >= 500) {
                            int idx = serverLevel.getRandom().nextInt(4);
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.crafting.trusted." + idx));
                        } else if (reputation >= 300) {
                            int idx = serverLevel.getRandom().nextInt(3);
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.crafting.friendly." + idx));
                        } else {
                            int crIdx = serverLevel.getRandom().nextInt(3);
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.crafting." + crIdx));
                        }
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.crafting_use"));
                        if (penalty < 0) {
                            ModLang.sendReputationSummary(serverPlayer, penalty, newRep);
                        } else {
                            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.no_penalty_trusted"));
                        }

                        craftingTableCooldown.put(playerId, currentTime);
                    }
                }
            }
        }
        
        return net.minecraft.world.InteractionResult.PASS;
    }

    private net.minecraft.world.InteractionResult onFenceGateOpen(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel))
            return net.minecraft.world.InteractionResult.PASS;

        BlockPos clickedPos = hitResult.getBlockPos();
        Block clickedBlock = serverLevel.getBlockState(clickedPos).getBlock();

        if (clickedBlock instanceof FenceGateBlock) {
            Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, clickedPos, 200);

            if (nearestVillage.isPresent()) {
                long currentTime = System.currentTimeMillis();
                UUID playerId = serverPlayer.getUUID();

                if (fenceGateCooldown.containsKey(playerId) &&
                        currentTime - fenceGateCooldown.get(playerId) < FENCE_GATE_COOLDOWN_MS) {
                    return net.minecraft.world.InteractionResult.PASS;
                }

                AABB animalCheckBox = new AABB(
                        clickedPos.getX() - 4, clickedPos.getY() - 1, clickedPos.getZ() - 4,
                        clickedPos.getX() + 5, clickedPos.getY() + 2, clickedPos.getZ() + 5);

                List<net.minecraft.world.entity.animal.Animal> nearbyAnimals = serverLevel
                        .getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, animalCheckBox);

                if (!nearbyAnimals.isEmpty()) {
                    List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                            Villager.class,
                            serverPlayer.getBoundingBox().inflate(15.0D));

                    boolean caughtByVillager = false;
                    boolean caughtByBaby = false;

                    for (Villager villager : nearbyVillagers) {
                        if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                            caughtByVillager = true;
                            if (villager.isBaby()) {
                                caughtByBaby = true;
                            }
                            break;
                        }
                    }

                    if (caughtByVillager) {
                        VillageReputationData data = VillageReputationData.get(serverLevel);
                        int oldRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        data.addReputation(serverPlayer.getUUID(), nearestVillage.get(), -12);
                        int newRep = data.getReputation(serverPlayer.getUUID(), nearestVillage.get());
                        checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

                        String gatePrefix = caughtByBaby ? "villagediplomacy.react.gate.baby" : "villagediplomacy.react.gate.adult";
                        int gateCount = caughtByBaby ? 7 : 10;
                        int gateIdx = serverLevel.getRandom().nextInt(gateCount);
                        serverPlayer.sendSystemMessage(Component.translatable(gatePrefix + "." + gateIdx));
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.animal_release"));
                        ModLang.sendReputationSummary(serverPlayer, -12, newRep);

                        fenceGateCooldown.put(playerId, currentTime);

                        VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                        relationData.registerVillage(nearestVillage.get(), serverLevel);
                    }
                }
            }
        }
        
        return net.minecraft.world.InteractionResult.PASS;
    }

    private boolean onBlockBreak(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return true;
        if (player.level().isClientSide)
            return true;

        ServerLevel serverLevel = (ServerLevel) player.level();
        BlockPos brokenPos = pos;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, brokenPos, 200);

        if (nearestVillage.isPresent()) {
            VillageReputationData data = VillageReputationData.get(serverLevel);
            BlockPos villagePosBreak = nearestVillage.get();

            Block brokenBlock = state.getBlock();

            BlockType blockType = categorizeBlock(brokenBlock, serverLevel, brokenPos);

            if (blockType != BlockType.NONE) {
                List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                        Villager.class,
                        AABB.ofSize(Vec3.atCenterOf(brokenPos), 48, 48, 48));

                boolean caughtByVillager = false;
                boolean caughtByBaby = false;
                Villager spottingVillager = null;

                for (Villager villager : nearbyVillagers) {
                    if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                        caughtByVillager = true;
                        spottingVillager = villager;
                        if (villager.isBaby()) {
                            caughtByBaby = true;
                        }
                        break;
                    }
                }

                if (caughtByVillager) {
                    int reputation = data.getReputation(serverPlayer.getUUID(), villagePosBreak);
                    int basePenalty = blockType.penalty;
                    int penalty;
                    if (reputation >= 800) {
                        penalty = 0; 
                    } else if (reputation >= 500) {
                        penalty = basePenalty / 2; 
                    } else if (reputation >= 300) {
                        penalty = (int)(basePenalty * 0.75); 
                    } else {
                        penalty = basePenalty;
                    }
                    int oldRep = data.getReputation(serverPlayer.getUUID(), villagePosBreak);
                    data.addReputation(serverPlayer.getUUID(), villagePosBreak, penalty);
                    int newRep = data.getReputation(serverPlayer.getUUID(), villagePosBreak);
                    checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);

                    if (reputation >= 500) {
                        int idx = serverLevel.getRandom().nextInt(4);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.break.trusted." + idx));
                    } else if (reputation >= 300) {
                        int idx = serverLevel.getRandom().nextInt(4);
                        serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.react.break.friendly." + idx));
                    } else {
                        sendBlockBreakVillagerLine(blockType, caughtByBaby, serverLevel, serverPlayer, spottingVillager);
                    }

                    serverPlayer.sendSystemMessage(Component.translatable(blockType.systemMessageKey,
                            penalty, newRep, ModLang.repStatus(newRep)));

                    VillageRelationshipData relationData = VillageRelationshipData.get(serverLevel);
                    relationData.registerVillage(villagePosBreak, serverLevel);
                }
            }
        }
        
        return true;
    }

    private boolean onBlockBreakInVillage(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (!(player.level() instanceof ServerLevel serverLevel)) return true;
        
        BlockPos blockPos = pos;
        Block block = state.getBlock();
        
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, blockPos, 100);
        if (nearestVillage.isEmpty()) return true;
        
        boolean isVillageBlock = 
            block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE || block == Blocks.STONE ||
            block == Blocks.SMOOTH_STONE || block == Blocks.STONE_BRICKS || block == Blocks.MOSSY_STONE_BRICKS ||
            block == Blocks.CRACKED_STONE_BRICKS || block == Blocks.CHISELED_STONE_BRICKS ||
            block == Blocks.DIORITE || block == Blocks.POLISHED_DIORITE || block == Blocks.ANDESITE ||
            block == Blocks.POLISHED_ANDESITE || block == Blocks.GRANITE || block == Blocks.POLISHED_GRANITE ||
            
            block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS || block == Blocks.BIRCH_PLANKS ||
            block == Blocks.ACACIA_PLANKS || block == Blocks.DARK_OAK_PLANKS || block == Blocks.JUNGLE_PLANKS ||
            
            block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG ||
            block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG || block == Blocks.JUNGLE_LOG ||
            block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_BIRCH_LOG ||
            
            block == Blocks.COBBLESTONE_STAIRS || block == Blocks.STONE_BRICK_STAIRS ||
            block == Blocks.MOSSY_COBBLESTONE_STAIRS || block == Blocks.MOSSY_STONE_BRICK_STAIRS ||
            block == Blocks.DIORITE_STAIRS || block == Blocks.ANDESITE_STAIRS || block == Blocks.GRANITE_STAIRS ||
            block == Blocks.POLISHED_DIORITE_STAIRS || block == Blocks.POLISHED_ANDESITE_STAIRS ||
            block == Blocks.POLISHED_GRANITE_STAIRS || block == Blocks.OAK_STAIRS || block == Blocks.SPRUCE_STAIRS ||
            block == Blocks.BIRCH_STAIRS || block == Blocks.ACACIA_STAIRS || block == Blocks.DARK_OAK_STAIRS ||
            
            block == Blocks.COBBLESTONE_SLAB || block == Blocks.STONE_SLAB || block == Blocks.SMOOTH_STONE_SLAB ||
            block == Blocks.STONE_BRICK_SLAB || block == Blocks.MOSSY_COBBLESTONE_SLAB ||
            block == Blocks.MOSSY_STONE_BRICK_SLAB || block == Blocks.DIORITE_SLAB || block == Blocks.ANDESITE_SLAB ||
            block == Blocks.GRANITE_SLAB || block == Blocks.POLISHED_DIORITE_SLAB ||
            block == Blocks.POLISHED_ANDESITE_SLAB || block == Blocks.POLISHED_GRANITE_SLAB ||
            block == Blocks.OAK_SLAB || block == Blocks.SPRUCE_SLAB || block == Blocks.BIRCH_SLAB ||
            block == Blocks.ACACIA_SLAB || block == Blocks.DARK_OAK_SLAB ||
            
            block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE || block == Blocks.BIRCH_FENCE ||
            block == Blocks.ACACIA_FENCE || block == Blocks.DARK_OAK_FENCE ||
            block == Blocks.OAK_FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE ||
            block == Blocks.BIRCH_FENCE_GATE || block == Blocks.ACACIA_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE ||
            
            block == Blocks.OAK_DOOR || block == Blocks.SPRUCE_DOOR || block == Blocks.BIRCH_DOOR ||
            block == Blocks.ACACIA_DOOR || block == Blocks.DARK_OAK_DOOR || block == Blocks.IRON_DOOR ||
            
            block == Blocks.GLASS_PANE || block == Blocks.GLASS || block == Blocks.WHITE_STAINED_GLASS ||
            block == Blocks.WHITE_STAINED_GLASS_PANE || block == Blocks.YELLOW_STAINED_GLASS ||
            block == Blocks.YELLOW_STAINED_GLASS_PANE ||
            
            block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN ||
            
            block == Blocks.HAY_BLOCK || block == Blocks.DIRT_PATH || block == Blocks.COBBLESTONE_WALL ||
            block == Blocks.MOSSY_COBBLESTONE_WALL || block == Blocks.TERRACOTTA || block == Blocks.WHITE_TERRACOTTA ||
            block == Blocks.BELL || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK ||
            block == Blocks.GRAVEL || block == Blocks.SAND;
        
        if (!isVillageBlock) return true;
        
        if (isJobSiteBlock(block)) return true;
        
        VillageReputationData data = VillageReputationData.get(serverLevel);
        BlockPos villagePos = nearestVillage.get();
        List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(
                Villager.class,
                AABB.ofSize(Vec3.atCenterOf(blockPos), 32, 32, 32));
        
        boolean caughtByVillager = false;
        for (Villager villager : nearbyVillagers) {
            if (hasLineOfSight(villager, serverPlayer, serverLevel)) {
                caughtByVillager = true;
                break;
            }
        }
        
        if (caughtByVillager) {
            int reputation = data.getReputation(serverPlayer.getUUID(), villagePos);
            int penalty;
            if (reputation >= 800) {
                penalty = 0; 
            } else if (reputation >= 500) {
                penalty = -5; 
            } else if (reputation >= 300) {
                penalty = -7; 
            } else {
                penalty = -10;
            }
            int oldRep = data.getReputation(serverPlayer.getUUID(), villagePos);
            data.addReputation(serverPlayer.getUUID(), villagePos, penalty);
            int newRep = data.getReputation(serverPlayer.getUUID(), villagePos);
            checkAndNotifyReputationChange(serverPlayer, oldRep, newRep);
            
            Component structureMsg;
            if (reputation >= 500) {
                int i = serverLevel.getRandom().nextInt(4);
                structureMsg = Component.translatable("villagediplomacy.react.structure.trusted." + i);
            } else if (reputation >= 100) {
                int i = serverLevel.getRandom().nextInt(4);
                structureMsg = Component.translatable("villagediplomacy.react.structure.friendly." + i);
            } else if (reputation >= 0) {
                int i = serverLevel.getRandom().nextInt(4);
                structureMsg = Component.translatable("villagediplomacy.react.structure.neutral." + i);
            } else {
                int i = serverLevel.getRandom().nextInt(6);
                structureMsg = Component.translatable("villagediplomacy.react.structure.hostile." + i);
            }
            serverPlayer.sendSystemMessage(structureMsg);
            serverPlayer.sendSystemMessage(Component.translatable("villagediplomacy.sys.structure_break"));
            ModLang.sendReputationSummary(serverPlayer, penalty, newRep);
        }
        
        return true;
    }

    private void onPlayerLogin(net.minecraft.server.network.ServerGamePacketListenerImpl handler, net.fabricmc.fabric.api.networking.v1.PacketSender sender, net.minecraft.server.MinecraftServer server) {
        net.minecraft.server.level.ServerPlayer player = handler.player;
        UUID id = player.getUUID();
        lastVisitedVillage.remove(id);
        greetingCooldown.remove(id);
        playerLoginTime.put(id, System.currentTimeMillis());

        // Restore HUD preference from world data
        com.cesoti2006.villagediplomacy.commands.DiplomacyCommands.loadHudState(handler.player.serverLevel());

        // Guard Villagers welcome message
        if (VillageDiplomacyConfig.guardWelcomeMessage && GuardVillagersCompat.isLoaded()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("villagediplomacy.guard.welcome"));
        }
    }

    private void onPlayerLogout(net.minecraft.server.network.ServerGamePacketListenerImpl handler, net.minecraft.server.MinecraftServer server) {
        UUID id = handler.player.getUUID();
        playerLoginTime.remove(id);
        zombieVillagerCurers.entrySet().removeIf(e -> e.getValue().equals(id));
        chestSnapshot.remove(id);
        chestOpenPosition.remove(id);
        chestOpenReputation.remove(id);
        lastChestMenuPlayer.remove(id);
    }

    private void onServerTick(net.minecraft.server.MinecraftServer server) {
        long currentTime = System.currentTimeMillis();

        
        if (server.getTickCount() % 600 == 0) {
            cleanupExpiredMaps(currentTime);
        }

        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;

            ServerLevel serverLevel = level;
            UUID playerId = player.getUUID();

            if (tradeWindowStart.containsKey(playerId) &&
                    currentTime - tradeWindowStart.get(playerId) > TRADE_WINDOW_MS) {

                int trades = pendingTrades.getOrDefault(playerId, 0);
                if (trades > 0) {
                    VillageReputationData data = VillageReputationData.get(serverLevel);
                    Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(serverLevel, player.blockPosition(), 200);
                    
                    if (nearestVillage.isPresent()) {
                        BlockPos villagePos = nearestVillage.get();
                        int oldRep = data.getReputation(playerId, villagePos);

                        data.addReputation(playerId, villagePos, trades * 5);
                        int newRep = data.getReputation(playerId, villagePos);
                        checkAndNotifyReputationChange(player, oldRep, newRep);
                    
                        spawnPositiveFeedback(serverLevel, player);

                        int ti = serverLevel.getRandom().nextInt(6);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.react.trade." + ti, trades));
                        ModLang.sendReputationSummary(player, trades * 5, newRep);

                        pendingTrades.remove(playerId);
                        tradeWindowStart.remove(playerId);
                    }
                }
            }

            if (player.tickCount % 20 == 0) {
                manageCrimeStatus(player, serverLevel);
                checkForVillageEntry(player, serverLevel);
                giveRandomGifts(player, serverLevel);
                makeVillagersFleeFromHostilePlayers(player, serverLevel);
                makeGolemsProtectVillageBasedOnReputation(player, serverLevel);
                checkForVillagerGreetings(player, serverLevel);
                checkChestClose(player, serverLevel, currentTime);
                checkZombieCuring(player, serverLevel);
            }
        }

        // Track TNT entities for explosion detection
        trackExplosiveTnt(server, currentTime);
    }


    private void checkChestClose(ServerPlayer player, ServerLevel level, long currentTime) {
        UUID playerId = player.getUUID();

        // Check if player had a chest open and now closed it
        if (lastChestMenuPlayer.containsKey(playerId)) {
            if (!(player.containerMenu instanceof ChestMenu)) {
                // Player closed the chest GUI - apply looting penalty
                lastChestMenuPlayer.remove(playerId);
                chestSnapshot.remove(playerId);
                chestOpenPosition.remove(playerId);

                if (chestOpenReputation.containsKey(playerId)) {
                    int reputation = chestOpenReputation.remove(playerId);
                    Optional<BlockPos> village = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
                    if (village.isPresent()) {
                        VillageReputationData data = VillageReputationData.get(level);
                        BlockPos vp = village.get();
                        int penalty = reputation >= 800 ? -7 : -15;
                        int oldRep = data.getReputation(playerId, vp);
                        data.addReputation(playerId, vp, penalty);
                        int newRep = data.getReputation(playerId, vp);
                        checkAndNotifyReputationChange(player, oldRep, newRep);

                        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                            player.getBoundingBox().inflate(12.0D));
                        boolean caught = false;
                        boolean caughtByBaby = false;
                        Villager witness = null;
                        for (Villager v : nearby) {
                            if (hasLineOfSight(v, player, level)) {
                                caught = true;
                                witness = v;
                                if (v.isBaby()) caughtByBaby = true;
                                break;
                            }
                        }

                        if (caught) {
                            if (reputation >= 800) {
                                int idx = level.getRandom().nextInt(3);
                                player.sendSystemMessage(Component.translatable("villagediplomacy.react.theft.loot.hero." + idx));
                            } else if (caughtByBaby) {
                                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                                    "villagediplomacy.react.theft.loot.baby", THEFT_LOOT_BABY);
                            } else {
                                ModLang.sendDialogRandom(player, level.getRandom(), witness,
                                    "villagediplomacy.react.theft.loot.adult", THEFT_LOOT_ADULT);
                            }
                            player.sendSystemMessage(Component.translatable("villagediplomacy.sys.loot_village",
                                penalty, newRep, ModLang.repStatus(newRep)));
                        }
                    }
                }
            }
        }
    }

    private void checkZombieCuring(ServerPlayer player, ServerLevel level) {
        // Iterate tracked zombie curings
        var iter = zombieVillagerCurers.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            UUID zombieId = entry.getKey();
            UUID curerId = entry.getValue();

            if (!curerId.equals(player.getUUID())) continue;

            // Check if zombie no longer exists (cured or died)
            net.minecraft.world.entity.Entity e = level.getEntity(zombieId);
            if (e == null || e.isRemoved()) {
                // Zombie is gone - check if a Villager spawned nearby (successful curing)
                boolean villagerNearby = !level.getEntitiesOfClass(Villager.class,
                    new AABB(player.blockPosition()).inflate(16)).isEmpty();

                if (villagerNearby) {
                    Optional<BlockPos> village = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
                    BlockPos vp = village.orElse(player.blockPosition());
                    VillageReputationData data = VillageReputationData.get(level);
                    int oldRep = data.getReputation(player.getUUID(), vp);
                    data.addReputation(player.getUUID(), vp, 25);
                    int newRep = data.getReputation(player.getUUID(), vp);
                    checkAndNotifyReputationChange(player, oldRep, newRep);
                    player.sendSystemMessage(Component.translatable("villagediplomacy.sys.cure_zombie"));
                    ModLang.sendReputationSummary(player, 25, newRep);
                    spawnPositiveFeedback(level, player);
                }
                iter.remove();
            }
        }
    }

    private void trackExplosiveTnt(net.minecraft.server.MinecraftServer server, long currentTime) {
        // Only check every 5 ticks for performance
        if (server.getTickCount() % 5 != 0) return;

        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            // Scan for TNT within a reasonable area around spawn
            List<PrimedTnt> tntList = level.getEntitiesOfClass(PrimedTnt.class,
                new AABB(level.getSharedSpawnPos()).inflate(300));

                for (PrimedTnt tnt : tntList) {
                    BlockPos tntPos = tnt.blockPosition();
                    UUID tntId = tnt.getUUID();

                    // Check if near village
                    Optional<BlockPos> village = VillageDetector.findNearestVillage(level, tntPos, 200);
                    if (village.isEmpty()) continue;

                    if (tnt.isRemoved()) {
                        if (trackedExplosiveTnt.containsKey(tntId)) {
                            long[] data_arr = trackedExplosiveTnt.remove(tntId);
                            BlockPos storedPos = new BlockPos((int)data_arr[0], (int)data_arr[1], (int)data_arr[2]);

                            // Try to find the actual TNT placer first, fall back to nearest player
                            ServerPlayer nearest = null;
                            UUID placerId = com.cesoti2006.villagediplomacy.fabric.events.FabricFireDamageHandler.findTntPlacer(storedPos);
                            if (placerId != null) {
                                nearest = level.getServer().getPlayerList().getPlayer(placerId);
                            }
                            if (nearest == null) {
                                double nearestDist = Double.MAX_VALUE;
                                for (ServerPlayer sp : level.players()) {
                                    double d = sp.distanceToSqr(storedPos.getX(), storedPos.getY(), storedPos.getZ());
                                    if (d < nearestDist && d < 2500) { // 50 block radius
                                        nearestDist = d;
                                        nearest = sp;
                                    }
                                }
                            }
                            if (nearest != null) {
                                VillageReputationData data = VillageReputationData.get(level);
                                BlockPos vp = village.get();
                                data.addReputation(nearest.getUUID(), vp, -30);
                                int newRep = data.getReputation(nearest.getUUID(), vp);
                                nearest.sendSystemMessage(Component.translatable("villagediplomacy.sys.explosion_damage", 0, 0, 0, 1, 30));
                                ModLang.sendReputationSummary(nearest, -30, newRep);
                            }
                        }
                    } else {
                        trackedExplosiveTnt.putIfAbsent(tntId, new long[]{tntPos.getX(), tntPos.getY(), tntPos.getZ(), currentTime});
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
        
        if (golemAlreadyAngry) {
            GolemPersonalityData personalityData = GolemPersonalityData.get(level);
            GolemPersonality personality = personalityData.getPersonality(angryGolem.getUUID());
            Object nameArg = golemStrikeNameArg(angryGolem, personality);
            if (level.getRandom().nextInt(2) == 0) {
                ModLang.sendRandomWithArgs(player, level.getRandom(), "villagediplomacy.golem.strike.violent", 8,
                        nameArg);
            }
            return;
        }

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

    private void manageCrimeStatus(ServerPlayer player, ServerLevel level) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (!crimeCommittedTime.containsKey(playerId))
            return;

        long crimeEndTime = crimeCommittedTime.get(playerId);

        if (currentTime >= crimeEndTime) {
            crimeCommittedTime.remove(playerId);
            golemStrikesPerGolem.remove(playerId);
            golemLastHitTime.remove(playerId);
            lastGolemHitTime.remove(playerId);

            List<IronGolem> allGolems = level.getEntitiesOfClass(IronGolem.class,
                    new AABB(player.blockPosition()).inflate(1000.0D),
                    golem -> !golem.isPlayerCreated());

            int calmadosCount = 0;
            for (IronGolem golem : allGolems) {
                LivingEntity target = golem.getTarget();
                UUID angerTarget = golem.getPersistentAngerTarget();

                boolean isAngryAtPlayer = (target != null && target.getUUID().equals(playerId)) ||
                        (angerTarget != null && angerTarget.equals(playerId));

                if (isAngryAtPlayer) {
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

        // Skip entry/exit if recently logged in
        Long loginTime = playerLoginTime.get(playerId);
        if (loginTime != null && currentTime - loginTime < 5000) return;

        // Skip messages if disabled in config
        if (!VillageDiplomacyConfig.enableEntryMessages) return;

        int configRadius = VillageDiplomacyConfig.villageEnterRadius;
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), configRadius);

        String lastVillage = lastVisitedVillage.get(playerId);
        if (nearestVillage.isEmpty()) {
            if (lastVillage != null) {
                if (greetingCooldown.containsKey(playerId)) {
                    long lastGreeting = greetingCooldown.get(playerId);
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

        BlockPos villagePos = nearestVillage.get();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);
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
            
            MutableComponent line1 = Component.literal("  " + icon + " §6Entrando a ")
                    .append(VillageDisplayName.asComponent(villageNameStored));
            player.sendSystemMessage(line1);
            
            player.sendSystemMessage(Component.translatable(
                    "villagediplomacy.enter.line2",
                    reputation,
                    ModLang.repStatus(reputation)));
            player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
            
            VillageDiplomacyNetwork.sendCloseHud(player);
            if (!com.cesoti2006.villagediplomacy.commands.DiplomacyCommands.HUD_DISABLED.contains(playerId)) {
                VillageDiplomacyNetwork.sendOpenHud(player, villageNameStored, reputation,
                        ModLang.hudRelationKey(reputation));
            }

            lastVisitedVillage.put(playerId, villageId);
            greetingCooldown.put(playerId, currentTime);

            // Guard Villagers entry reaction
            if (GuardVillagersCompat.isLoaded()) {
                GuardVillagersCompat.sendGuardEntryReaction(player, level, reputation);
            }
        }
    }

    private void giveRandomGifts(ServerPlayer player, ServerLevel level) {
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        if (nearestVillage.isEmpty())
            return;

        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), nearestVillage.get());

        if (reputation < 500) return;
        
        float baseChance = reputation >= 1000 ? 0.002F
                : reputation >= 800 ? 0.001F
                : 0.0007F;

        if (level.getRandom().nextFloat() >= baseChance) return;

        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(8.0D));

        for (Villager villager : nearbyVillagers) {
            if (villager.isBaby()) continue;
            String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
            if (profession.equals("none") || profession.equals("nitwit")) continue;
            
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
                
                spawnPositiveFeedback(level, villager);
                
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
            return 3;
        if (reputation >= -299)
            return 2;
        if (reputation >= -500)
            return 1;
        return 0;
    }

    private void checkAndNotifyReputationChange(ServerPlayer player, int oldRep, int newRep) {
        if (oldRep != newRep) {
            int change = newRep - oldRep;
            if (change > 0) {
                player.sendSystemMessage(Component.literal("§a+§r" + change + " §6reputación"));
            } else {
                player.sendSystemMessage(Component.literal("§c" + change + "§r §6reputación"));
            }
        }
    }

    private void spawnPositiveFeedback(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                3, 0.5D, 0.5D, 0.5D, 0.0D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    private void spawnNegativeFeedback(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.SMOKE,
                entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                3, 0.5D, 0.5D, 0.5D, 0.1D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    private void makeVillagersFleeFromHostilePlayers(ServerPlayer player, ServerLevel level) {
        if (crimeCommittedTime.containsKey(player.getUUID())) {
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                    player.getBoundingBox().inflate(24.0D));
            for (Villager v : villagers) {
                if (v.getTarget() == null && v.getNavigation().isDone()) {
                    net.minecraft.world.phys.Vec3 away = v.position().subtract(player.position()).normalize().scale(10);
                    v.getNavigation().moveTo(v.getX() + away.x, v.getY(), v.getZ() + away.z, 1.5);
                }
            }
        }
    }

    private void makeGolemsProtectVillageBasedOnReputation(ServerPlayer player, ServerLevel level) {
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        if (nearestVillage.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), nearestVillage.get());

        if (reputation < -200) {
            List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class,
                    player.getBoundingBox().inflate(32.0D),
                    g -> !g.isPlayerCreated());
            for (IronGolem g : golems) {
                if (g.getTarget() == null) {
                    g.setTarget(player);
                }
            }
        }
    }

    private void checkForVillagerGreetings(ServerPlayer player, ServerLevel level) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(16.0D));
        for (Villager v : villagers) {
            if (hasLineOfSight(v, player, level)) {
                UUID villagerUUID = v.getUUID();
                UUID playerUUID = player.getUUID();
                
                villagerGreetingCooldown.putIfAbsent(playerUUID, new HashMap<>());
                Map<UUID, Long> playerGreetings = villagerGreetingCooldown.get(playerUUID);
                
                long now = System.currentTimeMillis();
                long lastGreeting = playerGreetings.getOrDefault(villagerUUID, 0L);
                
                if (now - lastGreeting > GREETING_COOLDOWN_MS) {
                    playerGreetings.put(villagerUUID, now);
                }
            }
        }
    }

    private static boolean hasLineOfSight(LivingEntity entity, ServerPlayer player, ServerLevel level) {
        Vec3 entityEyes = entity.getEyePosition();
        Vec3 playerEyes = player.getEyePosition();
        ClipContext ctx = new ClipContext(entityEyes, playerEyes,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult hit = level.clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    private static boolean isAnimalInEnclosure(ServerLevel level, BlockPos animalPos) {
        int fenceCount = 0;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = -1; y <= 1; y++) {
                    Block b = level.getBlockState(animalPos.offset(x, y, z)).getBlock();
                    if (b instanceof net.minecraft.world.level.block.FenceBlock
                            || b instanceof net.minecraft.world.level.block.FenceGateBlock
                            || b instanceof net.minecraft.world.level.block.WallBlock) {
                        fenceCount++;
                        if (fenceCount >= 4) return true;
                    }
                }
            }
        }
        return false;
    }

    private static Villager findNearestWitnessVillager(ServerLevel level, LivingEntity killed, double radius) {
        AABB box = killed.getBoundingBox().inflate(radius);
        List<Villager> list = level.getEntitiesOfClass(Villager.class, box);
        Villager best = null;
        double bestD = Double.MAX_VALUE;
        Vec3 center = killed.position();
        for (Villager v : list) {
            Vec3 villagerEyes = v.getEyePosition();
            ClipContext ctx = new ClipContext(villagerEyes, center.add(0, 1, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, v);
            BlockHitResult hit = level.clip(ctx);
            if (hit.getType() != HitResult.Type.MISS) continue;
            double d = v.position().distanceToSqr(center);
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    private record AnimalDeathKind(String key, int babyCount, int adultCount) { }

    private static AnimalDeathKind animalDeathKindFor(LivingEntity killed) {
        if (killed instanceof Cow) return new AnimalDeathKind("cow", 5, 8);
        if (killed instanceof Sheep) return new AnimalDeathKind("sheep", 5, 8);
        if (killed instanceof Pig) return new AnimalDeathKind("pig", 5, 8);
        if (killed instanceof Rabbit) return new AnimalDeathKind("rabbit", 4, 8);
        if (killed instanceof Camel) return new AnimalDeathKind("camel", 5, 5);
        if (killed instanceof AbstractHorse) return new AnimalDeathKind("horse", 5, 10);
        if (killed instanceof Chicken) return new AnimalDeathKind("chicken", 3, 5);
        return new AnimalDeathKind("other", 1, 1);
    }

    private record HostileKillKind(String key, int repBonus, int lineCount) { }

    private static HostileKillKind hostileKillKindFor(LivingEntity killed) {
        if (killed instanceof Pillager || killed instanceof Vindicator
                || killed instanceof Evoker || killed instanceof Ravager
                || killed instanceof Witch) {
            return new HostileKillKind("raid", 15, 5);
        }
        if (killed instanceof Zombie || killed instanceof Husk
                || killed instanceof Drowned || killed instanceof AbstractSkeleton) {
            return new HostileKillKind("undead", 10, 5);
        }
        if (killed instanceof Creeper) {
            return new HostileKillKind("creeper", 8, 4);
        }
        if (killed instanceof Spider || killed instanceof CaveSpider) {
            return new HostileKillKind("spider", 5, 4);
        }
        return new HostileKillKind("other", 5, 4);
    }

    private record AnimalAttackKind(String key, int babyCount, int adultCount) { }

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

    private enum BlockType {
        DOOR(Blocks.OAK_DOOR, -15, "villagediplomacy.sys.door_break"),
        BED(Blocks.RED_BED, -20, "villagediplomacy.sys.bed_break"),
        CRAFTING(Blocks.CRAFTING_TABLE, -18, "villagediplomacy.sys.crafting_break"),
        WORKSTATION(Blocks.COMPOSTER, -12, "villagediplomacy.sys.workstation_break"),
        NONE(Blocks.AIR, 0, "villagediplomacy.sys.generic_break");

        private Block block;
        private int penalty;
        private String systemMessageKey;

        BlockType(Block block, int penalty, String systemMessageKey) {
            this.block = block;
            this.penalty = penalty;
            this.systemMessageKey = systemMessageKey;
        }
    }

    private static BlockType categorizeBlock(Block block, ServerLevel level, BlockPos pos) {
        if (block instanceof DoorBlock) return BlockType.DOOR;
        if (block instanceof BedBlock) return BlockType.BED;
        if (block instanceof CraftingTableBlock) return BlockType.CRAFTING;
        if (block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock ||
            block instanceof SmokerBlock || block instanceof BrewingStandBlock ||
            block instanceof AnvilBlock || block instanceof CartographyTableBlock ||
            block instanceof FletchingTableBlock || block instanceof LoomBlock ||
            block instanceof GrindstoneBlock || block instanceof StonecutterBlock ||
            block instanceof SmithingTableBlock) return BlockType.WORKSTATION;
        return BlockType.NONE;
    }

    private static boolean isJobSiteBlock(Block block) {
        return block instanceof FurnaceBlock || block instanceof BlastFurnaceBlock ||
               block instanceof SmokerBlock || block instanceof CraftingTableBlock ||
               block instanceof AnvilBlock || block instanceof CartographyTableBlock ||
               block instanceof FletchingTableBlock || block instanceof LoomBlock ||
               block instanceof GrindstoneBlock || block instanceof StonecutterBlock ||
               block instanceof SmithingTableBlock || block instanceof BrewingStandBlock ||
               block == Blocks.COMPOSTER || block == Blocks.LECTERN ||
               block == Blocks.CAULDRON;
    }

    private static void sendBlockBreakVillagerLine(BlockType blockType, boolean caughtByBaby, ServerLevel level,
                                                   ServerPlayer player, Villager villager) {
        if (blockType == BlockType.DOOR) {
            ModLang.sendDialogRandom(player, level.getRandom(), villager,
                    caughtByBaby ? "villagediplomacy.react.break.door.baby" : "villagediplomacy.react.break.door.adult",
                    caughtByBaby ? 5 : 8);
        } else if (blockType == BlockType.BED) {
            ModLang.sendDialogRandom(player, level.getRandom(), villager,
                    caughtByBaby ? "villagediplomacy.react.break.bed.baby" : "villagediplomacy.react.break.bed.adult",
                    caughtByBaby ? 5 : 8);
        } else if (blockType == BlockType.CRAFTING) {
            ModLang.sendDialogRandom(player, level.getRandom(), villager,
                    caughtByBaby ? "villagediplomacy.react.break.crafting.baby" : "villagediplomacy.react.break.crafting.adult",
                    caughtByBaby ? 5 : 8);
        } else if (blockType == BlockType.WORKSTATION) {
            ModLang.sendDialogRandom(player, level.getRandom(), villager,
                    caughtByBaby ? "villagediplomacy.react.break.workstation.baby" : "villagediplomacy.react.break.workstation.adult",
                    caughtByBaby ? 5 : 8);
        }
    }

    
    private void cleanupExpiredMaps(long currentTime) {
        tradeCooldowns.entrySet().removeIf(e -> currentTime - e.getValue() > 60000);
        crimeCommittedTime.entrySet().removeIf(e -> currentTime > e.getValue());
        greetingCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 600000);
        chestLootCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        bedUsageCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        trackedExplosiveTnt.entrySet().removeIf(e -> currentTime - e.getValue()[3] > 300000);
        // zombieVillagerCurers are cleaned per-player on logout - no need for global cleanup
        bellRingCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        trapdoorCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        doorUsageCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        craftingTableCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        fenceGateCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        animalReleaseCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        doorOpenCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 10000);
        lastGolemHitTime.entrySet().removeIf(e -> currentTime - e.getValue() > 60000);
        golemForgivenessTime.entrySet().removeIf(e -> currentTime - e.getValue() > 120000);
        golemResetCooldown.entrySet().removeIf(e -> currentTime - e.getValue() > 5000);
        villagerAttackTimes.entrySet().removeIf(e -> {
            List<Long> times = e.getValue();
            times.removeIf(t -> currentTime - t > 60000);
            return times.isEmpty();
        });
        golemStrikesPerGolem.entrySet().removeIf(e -> {
            Map<UUID, Integer> strikes = e.getValue();
            strikes.clear();
            return true;
        });
        golemLastHitTime.entrySet().removeIf(e -> {
            Map<UUID, Long> hits = e.getValue();
            hits.entrySet().removeIf(h -> currentTime - h.getValue() > 60000);
            return hits.isEmpty();
        });
        villagerGreetingCooldown.entrySet().removeIf(e -> {
            Map<UUID, Long> greetings = e.getValue();
            greetings.entrySet().removeIf(g -> currentTime - g.getValue() > 600000);
            return greetings.isEmpty();
        });
    }

    // ========== Guard Villagers Compatibility ==========

    private boolean onGuardAttack(LivingEntity entity, DamageSource damageSource, float originalDamage) {
        if (!GuardVillagersCompat.isLoaded()) return true;
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;

        String typeId = net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString();
        if (!typeId.contains("guardvillagers")) return true;

        LivingEntity guard = entity;
        BlockPos guardPos = guard.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, guardPos, 200);
        if (nearestVillage.isEmpty()) return true;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();

        int guardAttackPenalty = VillageDiplomacyConfig.repGuardAttack;
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, guardAttackPenalty);
        int newRep = data.getReputation(player.getUUID(), villagePos);

        checkAndNotifyReputationChange(player, oldRep, newRep);
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.guard_attacked"));
        ModLang.sendReputationSummary(player, guardAttackPenalty, newRep);

        GuardVillagersCompat.sendGuardAttackReaction(player, level, guard);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);

        return true;
    }

    private void onGuardDeath(LivingEntity entity, DamageSource damageSource) {
        if (!GuardVillagersCompat.isLoaded()) return;
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        String typeId = net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString();
        if (!typeId.contains("guardvillagers")) return;

        LivingEntity guard = entity;
        BlockPos guardPos = guard.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, guardPos, 200);
        if (nearestVillage.isEmpty()) return;

        VillageReputationData data = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();

        int guardKillPenalty = VillageDiplomacyConfig.repGuardKill;
        String guardName = GuardVillagersCompat.getGuardName(guard);
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, guardKillPenalty);
        int newRep = data.getReputation(player.getUUID(), villagePos);

        checkAndNotifyReputationChange(player, oldRep, newRep);
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.guard_killed", guardName));
        ModLang.sendReputationSummary(player, guardKillPenalty, newRep);

        GuardVillagersCompat.sendGuardDeathReaction(player, level, guard);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);

        // Aggro nearby golems
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        long newCrimeEnd = currentTime + MAJOR_CRIME_DURATION_MS;
        crimeCommittedTime.put(playerId, newCrimeEnd);
    }
}
