package com.cesoti2006.villagediplomacy.fabric.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.personality.EmotionalState;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.*;
import java.util.Optional;


public class FabricPersonalityBehaviorHandler {

    private static final Random RANDOM = new Random();
    private final Set<UUID> initializedVillagers = new HashSet<>();
    private int tickCounter = 0;

    private static final long EATING_COOLDOWN_MS = 600000; 
    private static final long ACTIVITY_INTERVAL_MS = 45000; 
    private static final long GIFT_COOLDOWN_MS = 30000;
    private static final long FLEE_MESSAGE_COOLDOWN_MS = 10000;
    private static final long BAD_REP_FLEE_MSG_COOLDOWN_MS = 15000;
    private static final long ACTIVITY_CHANGE_COOLDOWN_MS = 60000;

    private final Map<UUID, Long> lastFleeMessage = new HashMap<>();
    private final Map<UUID, Long> lastBadRepFleeMessage = new HashMap<>();
    private final Map<UUID, Long> lastToolChange = new HashMap<>();
    private final Map<UUID, String> lastActivity = new HashMap<>();
    private final Map<UUID, Long> lastEatingTime = new HashMap<>();
    private final Map<UUID, Long> lastGiftTime = new HashMap<>();
    private final Map<UUID, Long> lastActivityTime = new HashMap<>();

    
    private static final Map<UUID, Long> lastBellRing = new HashMap<>();
    private static final long BELL_COOLDOWN = 15000;

    public void registerEvents() {
        
        ServerLivingEntityEvents.AFTER_DEATH.register(this::onVillagerDeath);

        
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onVillagerAttackedByMonster);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                
                List<Villager> villagers = level.getEntitiesOfClass(Villager.class, 
                    new net.minecraft.world.phys.AABB(level.getSharedSpawnPos()).inflate(10000));

                for (Villager villager : villagers) {
                    UUID villagerId = villager.getUUID();
                    
                    
                    if (!initializedVillagers.contains(villagerId)) {
                        initializeVillager(villager, level);
                        initializedVillagers.add(villagerId);
                    }

                    
                    if (villager.tickCount % 40 != 0) continue;

                    VillagerPersonalityData data = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = data.getPersonality(villagerId);
                    if (personality == null) continue;

                    
                    PersonalityTrait courage = personality.getCourage();
                    if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
                        checkCowardFlee(villager, personality, level);
                    }

                    
                    checkBadReputationFlee(villager, personality, level);

                    
                    updateToolInHand(villager, level, data);

                    
                    PersonalityTrait generosity = personality.getGenerosity();
                    if (generosity == PersonalityTrait.GENEROUS || generosity == PersonalityTrait.CHARITABLE) {
                        checkGenerousGift(villager, level);
                    }

                    
                    updateEmotionalState(villager, personality, level);

                    
                    if (personality.getCurrentEmotion() == EmotionalState.MOURNING) {
                        checkMourningBehavior(villager, level, data);
                    }
                }

                
                if (tickCounter++ % 7 == 0) {
                    for (Villager villager : villagers) {
                        UUID villagerId = villager.getUUID();
                        long currentTime = System.currentTimeMillis();
                        Long lastTime = lastActivityTime.getOrDefault(villagerId, 0L);
                        if (currentTime - lastTime < ACTIVITY_INTERVAL_MS) continue;

                        if (RANDOM.nextDouble() < 0.25) {
                            performActivityBasedOnPersonality(villager, level);
                            lastActivityTime.put(villagerId, currentTime);
                        }
                    }
                }
            }
        });
    }

    private void initializeVillager(Villager villager, ServerLevel level) {
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        String biome = level.getBiome(villager.blockPosition()).toString();
        VillagerPersonality personality = data.getOrCreatePersonality(
            villager.getUUID(),
            biome,
            RANDOM
        );

        personality.setProfession(villager.getVillagerData().getProfession().toString());
        personality.setProfessionalLevel(villager.getVillagerData().getLevel());

        String displayName = personality.getTitle().isEmpty()
            ? personality.getCustomName()
            : personality.getCustomName() + " " + personality.getTitle();

        villager.setCustomName(Component.literal(displayName));
        villager.setCustomNameVisible(true);

        
        
    }

    private void checkCowardFlee(Villager villager, VillagerPersonality personality, ServerLevel level) {
        if (villager.getDeltaMovement().horizontalDistanceSqr() < 0.01) return;

        
        boolean isFleeingFromZombie = !level.getEntitiesOfClass(
            Zombie.class,
            villager.getBoundingBox().inflate(10.0D)
        ).isEmpty();

        
        boolean isFleeingFromPlayer = level.getEntitiesOfClass(
            Player.class,
            villager.getBoundingBox().inflate(8.0D),
            p -> !p.getMainHandItem().isEmpty() &&
                (p.getMainHandItem().getItem() instanceof SwordItem ||
                 p.getMainHandItem().getItem() instanceof AxeItem)
        ).size() > 0;

        if (!isFleeingFromZombie && !isFleeingFromPlayer) return;

        long currentTime = System.currentTimeMillis();
        UUID villagerId = villager.getUUID();
        if (lastFleeMessage.containsKey(villagerId)) {
            if (currentTime - lastFleeMessage.get(villagerId) < FLEE_MESSAGE_COOLDOWN_MS) return;
        }

        String name = personality.getCustomName();
        boolean coward = personality.getCourage() == PersonalityTrait.COWARD;
        String prefix = coward ? "villagediplomacy.personality.flee.coward"
                : "villagediplomacy.personality.flee.cautious";
        int count = coward ? 7 : 6;

        List<Player> nearbyPlayers = level.getEntitiesOfClass(
            Player.class,
            villager.getBoundingBox().inflate(30.0D));

        for (Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer sp) {
                ModLang.sendDialogNamedRandom(sp, level.getRandom(), name, prefix, count);
            }
        }

        lastFleeMessage.put(villagerId, currentTime);
    }

    private void checkBadReputationFlee(Villager villager, VillagerPersonality personality, ServerLevel level) {
        if (villager.getDeltaMovement().horizontalDistanceSqr() < 0.01) return;

        long currentTime = System.currentTimeMillis();
        UUID villagerId = villager.getUUID();
        Long lastMessage = lastBadRepFleeMessage.getOrDefault(villagerId, 0L);
        if (currentTime - lastMessage < BAD_REP_FLEE_MSG_COOLDOWN_MS) return;

        VillageReputationData repData = VillageReputationData.get(level);
        Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
        if (villagePos.isEmpty()) return;

        List<Player> nearbyPlayers = level.getEntitiesOfClass(
            Player.class,
            villager.getBoundingBox().inflate(16.0D));

        for (Player player : nearbyPlayers) {
            if (!(player instanceof ServerPlayer sp)) continue;
            int reputation = repData.getReputation(sp.getUUID(), villagePos.get());
            if (reputation < -100) {
                String name = personality.getCustomName();
                ModLang.sendDialogNamedRandom(sp, level.getRandom(), name,
                    "villagediplomacy.personality.flee.reputation", 5);
                lastBadRepFleeMessage.put(villagerId, currentTime);
                return;
            }
        }
    }

    private void updateToolInHand(Villager villager, ServerLevel level, VillagerPersonalityData data) {
        long timeOfDay = level.getDayTime() % 24000;
        UUID villagerId = villager.getUUID();
        ItemStack currentTool = villager.getMainHandItem();

        String activity = "";
        ItemStack newTool = ItemStack.EMPTY;

        if (timeOfDay >= 0 && timeOfDay < 6000) {
            
            activity = "working";
            newTool = getWorkTool(villager);
        } else if (timeOfDay >= 6000 && timeOfDay < 12000) {
            
            Long lastEat = lastEatingTime.get(villagerId);
            long currentTime = System.currentTimeMillis();
            if (lastEat == null || currentTime - lastEat > EATING_COOLDOWN_MS) {
                activity = "eating";
                newTool = getFoodForVillager(villager, level);
                lastEatingTime.put(villagerId, currentTime);
            } else {
                activity = "working";
                newTool = getWorkTool(villager);
            }
        } else if (timeOfDay >= 12000 && timeOfDay < 18000) {
            
            activity = "lighting";
            newTool = new ItemStack(Items.TORCH);
        } else {
            
            activity = "sleeping";
            if (!currentTool.isEmpty()) {
                villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            return;
        }

        
        boolean toolChanged = false;
        if (newTool.isEmpty() != currentTool.isEmpty()) {
            toolChanged = true;
        } else if (!newTool.isEmpty() && !ItemStack.isSameItem(currentTool, newTool)) {
            toolChanged = true;
        }

        if (toolChanged || currentTool.isEmpty()) {
            villager.setItemInHand(InteractionHand.MAIN_HAND, newTool.copy());

            if (activity.equals("eating") && !newTool.isEmpty()) {
                villager.startUsingItem(InteractionHand.MAIN_HAND);
            }

            String lastAct = lastActivity.get(villagerId);
            Long lastChange = lastToolChange.get(villagerId);
            long currentTime = System.currentTimeMillis();

            if (!activity.equals(lastAct) && (lastChange == null || currentTime - lastChange > ACTIVITY_CHANGE_COOLDOWN_MS)) {
                VillagerPersonality personality = data.getPersonality(villagerId);
                if (personality != null) {
                    String name = personality.getCustomName();
                    Component message = getActivityMessageComponent(name, activity, villager);
                    if (message != null) {
                        List<? extends Player> nearbyPlayers = level.players().stream()
                            .filter(p -> p.distanceToSqr(villager) < 400)
                            .toList();
                        for (Player player : nearbyPlayers) {
                            if (player instanceof ServerPlayer sp) {
                                sp.sendSystemMessage(message);
                            }
                        }
                    }
                }
                lastActivity.put(villagerId, activity);
                lastToolChange.put(villagerId, currentTime);
            }
        }
    }

    private Component getActivityMessageComponent(String name, String activity, Villager villager) {
        String profession = villager.getVillagerData().getProfession().toString();
        if (profession.equals("none") || profession.equals("nitwit")) return null;

        return switch (activity) {
            case "working" -> {
                String key = switch (profession) {
                    case "farmer" -> "villagediplomacy.villager.activity.work.farmer";
                    case "armorer", "weaponsmith", "toolsmith" -> "villagediplomacy.villager.activity.work.smith";
                    case "butcher" -> "villagediplomacy.villager.activity.work.butcher";
                    case "librarian" -> "villagediplomacy.villager.activity.work.librarian";
                    case "cleric" -> "villagediplomacy.villager.activity.work.cleric";
                    case "fisherman" -> "villagediplomacy.villager.activity.work.fisherman";
                    default -> "villagediplomacy.villager.activity.work.default";
                };
                yield Component.translatable(key, name);
            }
            case "eating" -> Component.translatable("villagediplomacy.villager.activity.eating", name);
            case "lighting" -> Component.translatable("villagediplomacy.villager.activity.lighting", name);
            default -> null;
        };
    }

    private static ItemStack getWorkTool(Villager villager) {
        String profession = villager.getVillagerData().getProfession().toString();
        return switch (profession) {
            case "farmer" -> new ItemStack(Items.IRON_HOE);
            case "armorer", "weaponsmith", "toolsmith" -> new ItemStack(Items.IRON_INGOT);
            case "butcher" -> new ItemStack(Items.IRON_AXE);
            case "cartographer" -> new ItemStack(Items.MAP);
            case "cleric" -> new ItemStack(Items.GLASS_BOTTLE);
            case "fisherman" -> new ItemStack(Items.FISHING_ROD);
            case "fletcher" -> new ItemStack(Items.ARROW);
            case "leatherworker" -> new ItemStack(Items.LEATHER);
            case "librarian" -> new ItemStack(Items.BOOK);
            case "mason" -> new ItemStack(Items.BRICK);
            case "shepherd" -> new ItemStack(Items.SHEARS);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack getFoodForVillager(Villager villager, Level level) {
        String profession = villager.getVillagerData().getProfession().toString();
        return switch (profession) {
            case "farmer" -> level.random.nextBoolean() ?
                new ItemStack(Items.BREAD) :
                (level.random.nextBoolean() ? new ItemStack(Items.CARROT) : new ItemStack(Items.POTATO));
            case "fisherman" -> level.random.nextBoolean() ?
                new ItemStack(Items.COD) : new ItemStack(Items.SALMON);
            case "shepherd" -> new ItemStack(Items.MUTTON);
            case "butcher" -> level.random.nextInt(3) == 0 ?
                new ItemStack(Items.COOKED_BEEF) :
                (level.random.nextBoolean() ? new ItemStack(Items.COOKED_PORKCHOP) : new ItemStack(Items.COOKED_CHICKEN));
            case "cleric" -> new ItemStack(Items.GOLDEN_APPLE);
            case "librarian" -> new ItemStack(Items.COOKIE);
            default -> {
                int choice = level.random.nextInt(6);
                yield switch (choice) {
                    case 0 -> new ItemStack(Items.BREAD);
                    case 1 -> new ItemStack(Items.APPLE);
                    case 2 -> new ItemStack(Items.CARROT);
                    case 3 -> new ItemStack(Items.POTATO);
                    case 4 -> new ItemStack(Items.MILK_BUCKET);
                    default -> new ItemStack(Items.BEETROOT_SOUP);
                };
            }
        };
    }

    private void checkGenerousGift(Villager villager, ServerLevel level) {
        UUID villagerId = villager.getUUID();
        long currentTime = System.currentTimeMillis();
        if (lastGiftTime.containsKey(villagerId)) {
            if (currentTime - lastGiftTime.get(villagerId) < GIFT_COOLDOWN_MS) return;
        }

        
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class,
            villager.getBoundingBox().inflate(8.0D));

        for (Player player : nearbyPlayers) {
            if (player.getHealth() < player.getMaxHealth() * 0.3F) {
                VillageReputationData repData = VillageReputationData.get(level);
                Optional<BlockPos> villagePos = VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
                if (villagePos.isPresent()) {
                    int reputation = repData.getReputation(player.getUUID(), villagePos.get());
                    if (reputation < -50) return;
                }

                ItemStack bread = new ItemStack(Items.BREAD, 1);
                double spawnX = villager.getX();
                double spawnY = villager.getEyeY() - 0.3D;
                double spawnZ = villager.getZ();

                Vec3 direction = new Vec3(
                    player.getX() - villager.getX(),
                    player.getEyeY() - villager.getEyeY() + 0.2,
                    player.getZ() - villager.getZ()
                ).normalize();

                ItemEntity itemEntity = new ItemEntity(
                    level,
                    spawnX + direction.x * 0.5,
                    spawnY,
                    spawnZ + direction.z * 0.5,
                    bread
                );

                itemEntity.setDeltaMovement(
                    direction.x * 0.5,
                    direction.y * 0.4 + 0.3,
                    direction.z * 0.5
                );
                itemEntity.setPickUpDelay(10);

                boolean spawned = level.addFreshEntity(itemEntity);

                if (spawned) {
                    level.sendParticles(ParticleTypes.HEART,
                        villager.getX(), villager.getY() + 2.0, villager.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0);

                    VillagerPersonalityData data = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = data.getPersonality(villagerId);
                    String name = personality != null ? personality.getCustomName() : "Villager";
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.translatable("villagediplomacy.personality.gift_bread", name));
                    }
                    lastGiftTime.put(villagerId, currentTime);
                }
                return;
            }
        }
    }

    private void updateEmotionalState(Villager villager, VillagerPersonality personality, ServerLevel level) {
        personality.updateEmotion();
        EmotionalState emotion = personality.getCurrentEmotion();
        if (emotion == EmotionalState.NEUTRAL) return;

        String particleType = emotion.getParticleEffect();
        switch (particleType) {
            case "heart" -> level.sendParticles(ParticleTypes.HEART,
                villager.getX(), villager.getY() + 2.0, villager.getZ(),
                1, 0.3, 0.3, 0.3, 0.0);
            case "angry_villager" -> level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                villager.getX(), villager.getY() + 2.0, villager.getZ(),
                1, 0.3, 0.3, 0.3, 0.0);
            case "rain" -> level.sendParticles(ParticleTypes.RAIN,
                villager.getX(), villager.getY() + 2.0, villager.getZ(),
                2, 0.2, 0.2, 0.2, 0.0);
            case "cloud" -> level.sendParticles(ParticleTypes.CLOUD,
                villager.getX(), villager.getY() + 2.0, villager.getZ(),
                1, 0.3, 0.3, 0.3, 0.0);
        }
    }

    private void checkMourningBehavior(Villager villager, ServerLevel level, VillagerPersonalityData data) {
        data.cleanupOldDeaths();

        // Advanced mourning: check for recent deaths in the village
        com.cesoti2006.villagediplomacy.data.VillageMourningData mourningData =
            com.cesoti2006.villagediplomacy.data.VillageMourningData.get(level);

        Optional<BlockPos> villagePos = com.cesoti2006.villagediplomacy.data.VillageDetector
            .findNearestVillage(level, villager.blockPosition(), 200);

        if (villagePos.isPresent()) {
            String villageId = com.cesoti2006.villagediplomacy.data.VillageDetector.getVillageId(villagePos.get());
            java.util.List<com.cesoti2006.villagediplomacy.data.VillageMourningData.MourningRecord> deaths =
                mourningData.getRecentDeaths(villageId);

            for (com.cesoti2006.villagediplomacy.data.VillageMourningData.MourningRecord death : deaths) {
                if (!death.hasJobSite()) continue;
                if (death.getTimeSinceDeath() > 10 * 60 * 1000) continue;

                BlockPos jobSite = death.jobSitePos;
                double distance = villager.blockPosition().distSqr(jobSite);

                // Close to job site: show full mourning reaction
                if (distance < 25.0) {
                    showFullMourningReaction(villager, level, death);
                }
                // Further away: look toward the job site
                else if (distance < 225.0) {
                    villager.getLookControl().setLookAt(
                        jobSite.getX() + 0.5,
                        jobSite.getY() + 0.5,
                        jobSite.getZ() + 0.5);
                }
            }
        }

        // Also look at own job site as fallback
        var jobSiteOptional = villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE);
        if (jobSiteOptional.isPresent()) {
            BlockPos jobSite = jobSiteOptional.get().pos();
            villager.getLookControl().setLookAt(
                jobSite.getX() + 0.5,
                jobSite.getY() + 0.5,
                jobSite.getZ() + 0.5
            );
        }
    }

    private void showFullMourningReaction(Villager villager, ServerLevel level,
                                          com.cesoti2006.villagediplomacy.data.VillageMourningData.MourningRecord death) {
        VillagerPersonalityData personalityData = VillagerPersonalityData.get(level);
        VillagerPersonality personality = personalityData.getPersonality(villager.getUUID());
        if (personality == null) return;

        // Rain particle effect above villager (tears)
        if (RANDOM.nextInt(100) < 30) {
            level.sendParticles(ParticleTypes.RAIN,
                villager.getX(), villager.getY() + 2.0, villager.getZ(),
                1, 0.3, 0.1, 0.3, 0.0);
        }

        // Sad villager sound
        if (RANDOM.nextInt(200) < 20) {
            villager.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 0.3f, 0.7f);
        }

        // Look at the deceased villager's job site
        BlockPos jobSite = death.jobSitePos;
        villager.getLookControl().setLookAt(
            jobSite.getX() + 0.5,
            jobSite.getY() + 0.5,
            jobSite.getZ() + 0.5);
    }

    

    private void performActivityBasedOnPersonality(Villager villager, ServerLevel level) {
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());
        if (personality == null) return;

        PersonalityTrait generosity = personality.getGenerosity();
        PersonalityTrait courage = personality.getCourage();
        PersonalityTrait workEthic = personality.getWorkEthic();

        if (generosity == PersonalityTrait.GENEROUS || generosity == PersonalityTrait.CHARITABLE) {
            if (RANDOM.nextDouble() < 0.6) shareFood(villager, level);
            else if (RANDOM.nextDouble() < 0.5) giftToWell(villager, level);
        }

        if (generosity == PersonalityTrait.GREEDY || generosity == PersonalityTrait.THRIFTY) {
            if (RANDOM.nextDouble() < 0.65) storeValueablesInChest(villager, level);
        }

        if (workEthic == PersonalityTrait.HARDWORKING || workEthic == PersonalityTrait.WORKAHOLIC) {
            if (RANDOM.nextDouble() < 0.55) contemplateWork(villager, level);
        }

        if (workEthic == PersonalityTrait.LAZY || workEthic == PersonalityTrait.RELAXED) {
            if (RANDOM.nextDouble() < 0.65) seekBedToRest(villager, level);
        }

        if (courage == PersonalityTrait.BRAVE || courage == PersonalityTrait.FEARLESS) {
            if (RANDOM.nextDouble() < 0.5) patrolVillage(villager, level);
        }
    }

    private void shareFood(Villager villager, ServerLevel level) {
        var nearby = level.getEntitiesOfClass(Villager.class, villager.getBoundingBox().inflate(15.0D));
        if (nearby.isEmpty()) return;

        if (!villager.getMainHandItem().isEmpty() &&
            (villager.getMainHandItem().is(Items.BREAD) ||
             villager.getMainHandItem().is(Items.BEETROOT) ||
             villager.getMainHandItem().is(Items.POTATO))) {

            Villager target = nearby.stream()
                .filter(v -> !v.getUUID().equals(villager.getUUID()))
                .findFirst().orElse(null);

            if (target != null) {
                villager.getNavigation().moveTo(target, 1.0D);
                if (villager.distanceToSqr(target) < 4.0D) {
                    ItemStack food = villager.getMainHandItem().copy();
                    food.setCount(1);
                    villager.spawnAtLocation(food);
                    villager.getMainHandItem().shrink(1);
                }
            }
        }
    }

    private void giftToWell(Villager villager, ServerLevel level) {
        BlockPos wellPos = findNearestWater(villager.blockPosition(), level, 20);
        if (wellPos == null) return;

        villager.getNavigation().moveTo(wellPos.getX() + 0.5, wellPos.getY(), wellPos.getZ() + 0.5, 1.0D);
        if (villager.distanceToSqr(Vec3.atCenterOf(wellPos)) < 4.0D) {
            if (RANDOM.nextDouble() < 0.3) {
                ItemStack gold = new ItemStack(Items.GOLD_NUGGET, 1);
                villager.spawnAtLocation(gold);
            }
        }
    }

    private void storeValueablesInChest(Villager villager, ServerLevel level) {
        BlockPos chestPos = findNearestChest(villager.blockPosition(), level, 20);
        if (chestPos == null) return;

        if (!villager.getMainHandItem().isEmpty() && isValuable(villager.getMainHandItem())) {
            villager.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0D);
            if (villager.distanceToSqr(Vec3.atCenterOf(chestPos)) < 2.0D) {
                villager.getMainHandItem().shrink(1);
            }
        }
    }

    private void contemplateWork(Villager villager, ServerLevel level) {
        BlockPos workArea = villager.blockPosition().offset(RANDOM.nextInt(8) - 4, 0, RANDOM.nextInt(8) - 4);
        if (level.getBlockState(workArea.below()).isCollisionShapeFullBlock(level, workArea.below())) {
            villager.getNavigation().moveTo(workArea.getX() + 0.5, workArea.getY(), workArea.getZ() + 0.5, 0.8D);
        }
    }

    private void seekBedToRest(Villager villager, ServerLevel level) {
        BlockPos bedPos = findNearestBed(villager.blockPosition(), level, 30);
        if (bedPos != null) {
            villager.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.8D);
        }
    }

    private void patrolVillage(Villager villager, ServerLevel level) {
        BlockPos patrol = villager.blockPosition().offset(RANDOM.nextInt(40) - 20, 0, RANDOM.nextInt(40) - 20);
        if (level.getBlockState(patrol.below()).isCollisionShapeFullBlock(level, patrol.below())) {
            villager.getNavigation().moveTo(patrol.getX() + 0.5, patrol.getY(), patrol.getZ() + 0.5, 1.2D);
        }
    }

    
    private static BlockPos findNearestWater(BlockPos center, ServerLevel level, int maxRadius) {
        
        var poiManager = level.getPoiManager();
        var fisherPoi = poiManager.findClosest(
            holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.FISHERMAN),
            center, 40, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        if (fisherPoi.isPresent()) return fisherPoi.get();
        
        
        int r = Math.min(maxRadius, 6);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(mutable).is(net.minecraft.world.level.block.Blocks.WATER))
                        return mutable.immutable();
                }
            }
        }
        return null;
    }

    
    private static BlockPos findNearestChest(BlockPos center, ServerLevel level, int maxRadius) {
        var poiManager = level.getPoiManager();
        var meetingPoi = poiManager.findClosest(
            holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING),
            center, 40, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        if (meetingPoi.isPresent()) return meetingPoi.get();
        
        int r = Math.min(maxRadius, 5);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = level.getBlockState(mutable);
                    if (state.is(net.minecraft.world.level.block.Blocks.CHEST) || state.is(net.minecraft.world.level.block.Blocks.BARREL))
                        return mutable.immutable();
                }
            }
        }
        return null;
    }

    
    private static BlockPos findNearestBed(BlockPos center, ServerLevel level, int maxRadius) {
        var poiManager = level.getPoiManager();
        var bedPoi = poiManager.findClosest(
            holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),
            center, Math.min(maxRadius, 40), net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        return bedPoi.orElse(null);
    }

    private static boolean isValuable(ItemStack item) {
        return item.is(Items.GOLD_INGOT) || item.is(Items.GOLD_NUGGET) ||
               item.is(Items.DIAMOND) || item.is(Items.EMERALD) ||
               item.is(Items.AMETHYST_SHARD);
    }

    
    
    private boolean onVillagerAttackedByMonster(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(entity instanceof Villager victim)) return true;
        if (victim.level().isClientSide()) return true;
        if (!(source.getEntity() instanceof net.minecraft.world.entity.monster.Monster)) return true;
        if (!(victim.level() instanceof ServerLevel level)) return true;

        VillagerPersonalityData data = VillagerPersonalityData.get(level);

        
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class,
            victim.getBoundingBox().inflate(25.0D));

        for (Villager villager : nearbyVillagers) {
            if (villager.getUUID().equals(victim.getUUID())) continue;

            VillagerPersonality personality = data.getPersonality(villager.getUUID());
            if (personality == null) continue;

            
            PersonalityTrait courage = personality.getCourage();
            if (courage != PersonalityTrait.BRAVE && courage != PersonalityTrait.FEARLESS) continue;

            
            long currentTime = System.currentTimeMillis();
            Long lastRing = lastBellRing.get(villager.getUUID());
            if (lastRing != null && currentTime - lastRing < BELL_COOLDOWN) continue;

            
            BlockPos bellPos = findNearestBell(villager.blockPosition(), level, 30);
            if (bellPos == null) continue;

            
            ringBellWithAnimation(bellPos, level, villager);
            lastBellRing.put(villager.getUUID(), currentTime);

            
            personality.setCurrentEmotion(EmotionalState.ANGRY);

            
            String name = personality.getCustomName();
            List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                villager.getBoundingBox().inflate(40.0D));
            for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
                player.sendSystemMessage(Component.translatable("villagediplomacy.bell.brave_ring", name));
            }

            
            makeVillagersRunToBell(bellPos, level, 40);
            break; 
        }

        return true;
    }

    private static BlockPos findNearestBell(BlockPos center, ServerLevel level, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        double closestDistance = Double.MAX_VALUE;
        BlockPos closestBell = null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var state = level.getBlockState(mutablePos);
                    if (state.is(net.minecraft.world.level.block.Blocks.BELL)) {
                        double distance = center.distSqr(mutablePos);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestBell = mutablePos.immutable();
                        }
                    }
                }
            }
        }
        return closestBell;
    }

    private static void ringBellWithAnimation(BlockPos bellPos, ServerLevel level, Villager ringer) {
        var bellState = level.getBlockState(bellPos);
        if (!(bellState.getBlock() instanceof net.minecraft.world.level.block.BellBlock bellBlock)) return;

        
        net.minecraft.core.Direction direction = net.minecraft.core.Direction.fromYRot(ringer.getYRot());

        
        bellBlock.attemptToRing(level, bellPos, direction);

        
        level.playSound(null, bellPos, net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.BLOCKS, 3.0F, 1.0F);
    }

    private static void makeVillagersRunToBell(BlockPos bellPos, ServerLevel level, int radius) {
        List<Villager> allVillagers = level.getEntitiesOfClass(Villager.class,
            new net.minecraft.world.phys.AABB(bellPos).inflate(radius));

        for (Villager villager : allVillagers) {
            villager.getNavigation().moveTo(
                bellPos.getX() + 0.5,
                bellPos.getY(),
                bellPos.getZ() + 0.5,
                1.5D);

            
            VillagerPersonalityData data = VillagerPersonalityData.get(level);
            VillagerPersonality personality = data.getPersonality(villager.getUUID());
            if (personality != null) {
                PersonalityTrait courage = personality.getCourage();
                if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
                    personality.setCurrentEmotion(EmotionalState.SCARED);
                } else {
                    personality.setCurrentEmotion(EmotionalState.ANGRY);
                }
            }
        }
    }

    

    public void onVillagerDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) villager.level();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());
        if (personality == null) return;

        data.registerDeath(villager.getUUID(), personality.getCustomName());

        if (personality.getPlayerReputationBonus() >= 30) {
            dropTestament(villager, personality, level);
        }
    }

    private void dropTestament(Villager villager, VillagerPersonality personality, ServerLevel level) {
        ItemStack testament = new ItemStack(Items.PAPER);
        Component customName = Component.translatable("villagediplomacy.testament.item_name", personality.getCustomName());

        ListTag loreList = new ListTag();
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.bar"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.title"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.of", personality.getFullName()));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.bar"));
        addTestamentLoreLine(loreList, Component.literal("§7"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.quote1"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.quote2"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.quote3"));
        addTestamentLoreLine(loreList, Component.literal("§7"));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.died",
                villager.blockPosition().toShortString()));
        addTestamentLoreLine(loreList, Component.translatable("villagediplomacy.testament.lore.job", personality.getProfession()));

        CompoundTag display = new CompoundTag();
        display.put("Name", StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(customName)));
        display.put("Lore", loreList);
        testament.getOrCreateTag().put("display", display);

        
        ListTag enchantments = new ListTag();
        CompoundTag enchantment = new CompoundTag();
        enchantment.putString("id", "minecraft:unbreaking");
        enchantment.putInt("lvl", 1);
        enchantments.add(enchantment);
        testament.getTag().put("Enchantments", enchantments);
        testament.getTag().putInt("HideFlags", 1);

        ItemEntity itemEntity = new ItemEntity(level,
            villager.getX(), villager.getY() + 0.5, villager.getZ(), testament);
        itemEntity.setDeltaMovement(0, 0.4, 0);
        itemEntity.setGlowingTag(true);
        level.addFreshEntity(itemEntity);

        double x = villager.getX(), y = villager.getY() + 1.0, z = villager.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.3, 0.5, 0.3, 0.08);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 40, 0.4, 0.4, 0.4, 1.0);
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 15, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 10, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
    }

    private void addTestamentLoreLine(ListTag loreList, Component line) {
        loreList.add(StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(line)));
    }
}
