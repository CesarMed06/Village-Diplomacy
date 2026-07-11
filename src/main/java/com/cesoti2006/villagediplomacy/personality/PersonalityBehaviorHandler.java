package com.cesoti2006.villagediplomacy.personality;

import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class PersonalityBehaviorHandler {
    
    private static final Random RANDOM = new Random();
    
    
    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (event.getLevel().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) event.getLevel();
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
            
        villager.setCustomName(net.minecraft.network.chat.Component.literal(displayName));
        villager.setCustomNameVisible(true);
        
        
        PersonalityTrait courage = personality.getCourage();
        if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
            float distance = courage == PersonalityTrait.COWARD ? 20.0F : 15.0F;
            villager.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                villager, 
                Zombie.class, 
                distance,
                1.0D, 
                1.2D
            ));
            
            
            
        }
        
        
        if (courage == PersonalityTrait.BRAVE || courage == PersonalityTrait.FEARLESS) {
            villager.goalSelector.addGoal(9, new LookAtPlayerGoal(villager, Player.class, 15.0F, 1.0F));
        }
    }
    
    
    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) villager.level();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());
        
        if (personality == null) return;
        
        
        if (villager.tickCount % 40 != 0) return;
        
        
        PersonalityTrait courage = personality.getCourage();
        if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
            checkCowardFlee(villager, personality, level);
        }
        
        
        
        if (villager.tickCount % 80 == 0) {
            checkBadReputationFlee(villager, personality, level);
        }
        
        
        updateToolInHand(villager, level);
        
        
        PersonalityTrait generosity = personality.getGenerosity();
        if (generosity == PersonalityTrait.GENEROUS || generosity == PersonalityTrait.CHARITABLE) {
            checkGenerousGift(villager, level);
        }
        
        
        updateEmotionalState(villager, personality, level);
        
        
        if (personality.getCurrentEmotion() == EmotionalState.MOURNING) {
            checkMourningBehavior(villager, level, data);
        }
    }
    
    
    private static final Map<UUID, Long> lastBadRepFleeMessage = new HashMap<>();
    private static void checkBadReputationFlee(Villager villager, VillagerPersonality personality, ServerLevel level) {
        
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            villager.getBoundingBox().inflate(16.0D));
        
        if (nearbyPlayers.isEmpty()) return;
        
        
        if (villager.getDeltaMovement().horizontalDistanceSqr() < 0.01) return;
        
        long currentTime = System.currentTimeMillis();
        UUID villagerId = villager.getUUID();
        Long lastMessage = lastBadRepFleeMessage.getOrDefault(villagerId, 0L);
        
        
        if (currentTime - lastMessage < 15000) return;
        
        
        com.cesoti2006.villagediplomacy.data.VillageReputationData repData = 
            com.cesoti2006.villagediplomacy.data.VillageReputationData.get(level);
        
        java.util.Optional<BlockPos> villagePos = 
            com.cesoti2006.villagediplomacy.data.VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
        
        if (villagePos.isEmpty()) return;
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
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
    
    
    private static final Map<UUID, Long> lastFleeMessage = new HashMap<>();
    private static void checkCowardFlee(Villager villager, VillagerPersonality personality, ServerLevel level) {
        
        if (villager.getDeltaMovement().horizontalDistanceSqr() < 0.01) return;
        
        
        boolean isFleeingFromZombie = !level.getEntitiesOfClass(
            net.minecraft.world.entity.monster.Zombie.class,
            villager.getBoundingBox().inflate(10.0D)
        ).isEmpty();
        
        
        boolean isFleeingFromPlayer = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            villager.getBoundingBox().inflate(8.0D),
            p -> !p.getMainHandItem().isEmpty() && 
                (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem ||
                 p.getMainHandItem().getItem() instanceof net.minecraft.world.item.AxeItem)
        ).size() > 0;
        
        if (!isFleeingFromZombie && !isFleeingFromPlayer) return;
        
        
        long currentTime = System.currentTimeMillis();
        UUID villagerId = villager.getUUID();
        if (lastFleeMessage.containsKey(villagerId)) {
            if (currentTime - lastFleeMessage.get(villagerId) < 10000) {
                return;
            }
        }
        
        
        String name = personality.getCustomName();
        boolean coward = personality.getCourage() == PersonalityTrait.COWARD;
        String prefix = coward ? "villagediplomacy.personality.flee.coward"
                : "villagediplomacy.personality.flee.cautious";
        int count = coward ? 7 : 6;

        List<net.minecraft.world.entity.player.Player> nearbyPlayers = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            villager.getBoundingBox().inflate(30.0D));

        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer sp) {
                ModLang.sendDialogNamedRandom(sp, level.getRandom(), name, prefix, count);
            }
        }
        
        lastFleeMessage.put(villagerId, currentTime);
    }
    
    
    private static final long EATING_COOLDOWN_MS = 600000; 
    private static final Map<UUID, Long> lastToolChange = new HashMap<>();
    private static final Map<UUID, String> lastActivity = new HashMap<>();
    private static final Map<UUID, Long> lastEatingTime = new HashMap<>();
    
    private static void updateToolInHand(Villager villager, Level level) {
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
                villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
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
            
            ItemStack toolCopy = newTool.copy();
            villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, toolCopy);
            
            
            if (activity.equals("eating") && !newTool.isEmpty()) {
                villager.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            
            
            String lastAct = lastActivity.get(villagerId);
            Long lastChange = lastToolChange.get(villagerId);
            long currentTime = System.currentTimeMillis();
            
            if (!activity.equals(lastAct) && (lastChange == null || currentTime - lastChange > 60000)) {
                VillagerPersonalityData data = VillagerPersonalityData.get((ServerLevel) level);
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
    
    
    private static Component getActivityMessageComponent(String name, String activity, Villager villager) {
        String profession = villager.getVillagerData().getProfession().toString();

        if (profession.equals("none") || profession.equals("nitwit")) {
            return null;
        }

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
    
    
    private static final Map<UUID, Long> lastGiftTime = new HashMap<>();
    private static void checkGenerousGift(Villager villager, ServerLevel level) {
        
        UUID villagerId = villager.getUUID();
        long currentTime = System.currentTimeMillis();
        if (lastGiftTime.containsKey(villagerId)) {
            if (currentTime - lastGiftTime.get(villagerId) < 30000) {
                return;
            }
        }
        
        
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class,
            villager.getBoundingBox().inflate(8.0D));
        
        for (Player player : nearbyPlayers) {
            
            if (player.getHealth() < player.getMaxHealth() * 0.3F) {
                
                com.cesoti2006.villagediplomacy.data.VillageReputationData repData = 
                    com.cesoti2006.villagediplomacy.data.VillageReputationData.get(level);
                
                java.util.Optional<BlockPos> villagePos = 
                    com.cesoti2006.villagediplomacy.data.VillageDetector.findNearestVillage(level, villager.blockPosition(), 200);
                
                if (villagePos.isPresent()) {
                    int reputation = repData.getReputation(player.getUUID(), villagePos.get());
                    
                    
                    if (reputation < -50) {
                        return; 
                    }
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
                itemEntity.setDefaultPickUpDelay();
                
                
                boolean spawned = level.addFreshEntity(itemEntity);
                
                if (spawned) {
                    
                    level.sendParticles(ParticleTypes.HEART,
                        villager.getX(), villager.getY() + 2.0, villager.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0);
                    
                    
                    VillagerPersonalityData data = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = data.getPersonality(villagerId);
                    String name = personality != null ? personality.getCustomName() : "Villager";
                    
                    player.sendSystemMessage(Component.translatable("villagediplomacy.personality.gift_bread", name));
                    
                    lastGiftTime.put(villagerId, currentTime);
                }
                
                return; 
            }
        }
    }
    
    
    private static void updateEmotionalState(Villager villager, VillagerPersonality personality, ServerLevel level) {
        personality.updateEmotion();
        
        
        EmotionalState emotion = personality.getCurrentEmotion();
        if (emotion == EmotionalState.NEUTRAL) return;
        
        String particleType = emotion.getParticleEffect();
        
        switch (particleType) {
            case "heart":
                level.sendParticles(ParticleTypes.HEART,
                    villager.getX(), villager.getY() + 2.0, villager.getZ(),
                    1, 0.3, 0.3, 0.3, 0.0);
                break;
            case "angry_villager":
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    villager.getX(), villager.getY() + 2.0, villager.getZ(),
                    1, 0.3, 0.3, 0.3, 0.0);
                break;
            case "rain":
                level.sendParticles(ParticleTypes.RAIN,
                    villager.getX(), villager.getY() + 2.0, villager.getZ(),
                    2, 0.2, 0.2, 0.2, 0.0);
                break;
            case "cloud":
                level.sendParticles(ParticleTypes.CLOUD,
                    villager.getX(), villager.getY() + 2.0, villager.getZ(),
                    1, 0.3, 0.3, 0.3, 0.0);
                break;
        }
    }
    
    
    private static void checkMourningBehavior(Villager villager, ServerLevel level, VillagerPersonalityData data) {
        
        data.cleanupOldDeaths();
        
        
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
    
    
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
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
    
    
    private static void dropTestament(Villager villager, VillagerPersonality personality, ServerLevel level) {
        ItemStack testament = new ItemStack(Items.PAPER);

        Component customName = Component.translatable("villagediplomacy.testament.item_name", personality.getCustomName());

        net.minecraft.nbt.ListTag loreList = new net.minecraft.nbt.ListTag();
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
        
        
        testament.getOrCreateTag().put("display", new net.minecraft.nbt.CompoundTag());
        testament.getTag().getCompound("display").put("Name",
            net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(customName)));
        testament.getTag().getCompound("display").put("Lore", loreList);
        
        
        net.minecraft.nbt.ListTag enchantments = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag enchantment = new net.minecraft.nbt.CompoundTag();
        enchantment.putString("id", "minecraft:unbreaking");
        enchantment.putInt("lvl", 1);
        enchantments.add(enchantment);
        testament.getTag().put("Enchantments", enchantments);
        
        
        testament.getTag().putInt("HideFlags", 1);
        
        ItemEntity itemEntity = new ItemEntity(level, 
            villager.getX(), 
            villager.getY() + 0.5, 
            villager.getZ(), 
            testament);
        
        
        itemEntity.setDeltaMovement(0, 0.4, 0);
        itemEntity.setGlowingTag(true); 
        
        level.addFreshEntity(itemEntity);
        
        
        double x = villager.getX();
        double y = villager.getY() + 1.0;
        double z = villager.getZ();
        
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            x, y, z, 30, 0.3, 0.5, 0.3, 0.08);
        
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            x, y, z, 40, 0.4, 0.4, 0.4, 1.0);
        
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
            x, y, z, 15, 0.2, 0.2, 0.2, 0.02);
        
        
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
            x, y, z, 10, 0.3, 0.3, 0.3, 0.1);
        
        
        level.playSound(null, x, y, z, 
            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
            net.minecraft.sounds.SoundSource.NEUTRAL,
            0.8f, 1.2f);
    }

    private static void addTestamentLoreLine(net.minecraft.nbt.ListTag loreList, Component line) {
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(line)));
    }
}
