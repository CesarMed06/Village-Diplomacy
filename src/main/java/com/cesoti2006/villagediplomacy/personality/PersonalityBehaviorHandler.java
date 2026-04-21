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
    
    /**
     * Cuando un aldeano spawns, asignarle personalidad
     */
    @SubscribeEvent
    public static void onVillagerSpawn(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (event.getLevel().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) event.getLevel();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        
        // Obtener o crear personalidad
        String biome = level.getBiome(villager.blockPosition()).toString();
        VillagerPersonality personality = data.getOrCreatePersonality(
            villager.getUUID(), 
            biome,
            RANDOM
        );
        
        // Actualizar nivel de profesión para títulos
        personality.setProfession(villager.getVillagerData().getProfession().toString());
        personality.setProfessionalLevel(villager.getVillagerData().getLevel());
        
        // Aplicar nombre personalizado CON título si es experto/maestro
        String displayName = personality.getTitle().isEmpty() 
            ? personality.getCustomName()
            : personality.getCustomName() + " " + personality.getTitle();
            
        villager.setCustomName(net.minecraft.network.chat.Component.literal(displayName));
        villager.setCustomNameVisible(true);
        
        // COBARDE: Huir de zombies a mayor distancia
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
            
            // Huir de jugador con espada/arma (versión simplificada)
            villager.goalSelector.addGoal(2, new AvoidEntityGoal<>(
                villager,
                Player.class,
                10.0F,
                1.0D,
                1.2D
            ));
        }
        
        // VALIENTE: Mirar más tiempo a jugadores
        if (courage == PersonalityTrait.BRAVE || courage == PersonalityTrait.FEARLESS) {
            villager.goalSelector.addGoal(9, new LookAtPlayerGoal(villager, Player.class, 15.0F, 1.0F));
        }
    }
    
    /**
     * COMPORTAMIENTOS POR TICK
     */
    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) villager.level();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());
        
        if (personality == null) return;
        
        // Solo hacer esto cada 20 ticks (1 segundo)
        if (villager.tickCount % 20 != 0) return;
        
        // ===== COBARDE: Mensajes al huir =====
        PersonalityTrait courage = personality.getCourage();
        if (courage == PersonalityTrait.COWARD || courage == PersonalityTrait.CAUTIOUS) {
            checkCowardFlee(villager, personality, level);
        }
        
        // ===== HERRAMIENTAS POR HORA DEL DÍA =====
        updateToolInHand(villager, level);
        
        // ===== GENEROSO: Dar pan a jugador herido cercano =====
        PersonalityTrait generosity = personality.getGenerosity();
        if (generosity == PersonalityTrait.GENEROUS || generosity == PersonalityTrait.CHARITABLE) {
            checkGenerousGift(villager, level);
        }
        
        // ===== EMOCIONES: Actualizar estado y partículas =====
        updateEmotionalState(villager, personality, level);
        
        // ===== LUTO: Mirar hacia estación de trabajo vacía =====
        if (personality.getCurrentEmotion() == EmotionalState.MOURNING) {
            checkMourningBehavior(villager, level, data);
        }
    }
    
    /**
     * Detectar cuando cobardes están huyendo y mostrar mensajes
     */
    private static final Map<UUID, Long> lastFleeMessage = new HashMap<>();
    private static void checkCowardFlee(Villager villager, VillagerPersonality personality, ServerLevel level) {
        // Solo si está huyendo (velocidad alta)
        if (villager.getDeltaMovement().horizontalDistanceSqr() < 0.01) return;
        
        // Buscar zombies o jugadores peligrosos cerca
        boolean isFleeingFromZombie = !level.getEntitiesOfClass(
            net.minecraft.world.entity.monster.Zombie.class,
            villager.getBoundingBox().inflate(15.0D)
        ).isEmpty();
        
        boolean isFleeingFromPlayer = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            villager.getBoundingBox().inflate(10.0D),
            p -> !p.getMainHandItem().isEmpty() && 
                (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem ||
                 p.getMainHandItem().getItem() instanceof net.minecraft.world.item.AxeItem)
        ).size() > 0;
        
        if (!isFleeingFromZombie && !isFleeingFromPlayer) return;
        
        // Cooldown de 10 segundos para mensajes
        long currentTime = System.currentTimeMillis();
        UUID villagerId = villager.getUUID();
        if (lastFleeMessage.containsKey(villagerId)) {
            if (currentTime - lastFleeMessage.get(villagerId) < 10000) {
                return;
            }
        }
        
        // Mostrar mensaje a jugadores cercanos
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
                ModLang.sendRandomWithArgs(sp, level.getRandom(), prefix, count, name);
            }
        }
        
        lastFleeMessage.put(villagerId, currentTime);
    }
    
    /**
     * Cambiar herramienta según hora del día CON ANIMACIONES Y MENSAJES
     */
    private static final long EATING_COOLDOWN_MS = 600000; // 10 minutos entre comidas
    private static final Map<UUID, Long> lastToolChange = new HashMap<>();
    private static final Map<UUID, String> lastActivity = new HashMap<>();
    private static final Map<UUID, Long> lastEatingTime = new HashMap<>();
    
    private static void updateToolInHand(Villager villager, Level level) {
        long timeOfDay = level.getDayTime() % 24000;
        UUID villagerId = villager.getUUID();
        ItemStack currentTool = villager.getMainHandItem();
        
        // Determinar qué debería estar sosteniendo
        String activity = "";
        ItemStack newTool = ItemStack.EMPTY;
        
        if (timeOfDay >= 0 && timeOfDay < 6000) {
            // Mañana (6am-12pm): Trabajando
            activity = "working";
            newTool = getWorkTool(villager);
        } else if (timeOfDay >= 6000 && timeOfDay < 12000) {
            // Tarde (12pm-6pm): Comiendo CON COOLDOWN
            Long lastEat = lastEatingTime.get(villagerId);
            long currentTime = System.currentTimeMillis();
            
            // Solo comer si pasó el cooldown
            if (lastEat == null || currentTime - lastEat > EATING_COOLDOWN_MS) {
                activity = "eating";
                newTool = getFoodForVillager(villager, level);
                lastEatingTime.put(villagerId, currentTime);
            } else {
                // Todavía no es hora de comer, seguir trabajando
                activity = "working";
                newTool = getWorkTool(villager);
            }
        } else if (timeOfDay >= 12000 && timeOfDay < 18000) {
            // Atardecer (6pm-12am): Antorcha para iluminar
            activity = "lighting";
            newTool = new ItemStack(Items.TORCH);
        } else {
            // Noche (12am-6am): Durmiendo, sin herramientas
            activity = "sleeping";
            // Limpiar item si tiene alguno
            if (!currentTool.isEmpty()) {
                villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            return;
        }
        
        // Si el item cambió, actualizar y mostrar mensaje
        boolean toolChanged = false;
        if (newTool.isEmpty() != currentTool.isEmpty()) {
            toolChanged = true;
        } else if (!newTool.isEmpty() && !ItemStack.isSameItem(currentTool, newTool)) {
            toolChanged = true;
        }
        
        if (toolChanged || currentTool.isEmpty()) {
            // Cambiar herramienta - forzar múltiples veces para evitar parpadeo
            ItemStack toolCopy = newTool.copy();
            villager.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, toolCopy);
            
            // Animación de comer si tiene pan (duración extendida para visibilidad)
            if (activity.equals("eating") && !newTool.isEmpty()) {
                villager.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            
            // Mensaje contextual (cooldown de 60 segundos)
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
    
    /** Mensaje según actividad; null si no hay que mostrar nada (nitwit / dormir). */
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
    
    /**
     * Obtener herramienta según profesión
     */
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
    
    /**
     * Obtener comida según profesión con VARIEDAD
     */
    private static ItemStack getFoodForVillager(Villager villager, Level level) {
        String profession = villager.getVillagerData().getProfession().toString();
        
        // Comidas específicas por profesión con variedad aleatoria
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
            case "librarian" -> new ItemStack(Items.COOKIE); // Intelectuales comen galletas
            default -> {
                // Variedad aleatoria para otras profesiones
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
    
    /**
     * GENEROSO: Lanzar pan gratis si ve jugador herido
     */
    private static final Map<UUID, Long> lastGiftTime = new HashMap<>();
    private static void checkGenerousGift(Villager villager, ServerLevel level) {
        // Cooldown de 30 segundos entre regalos
        UUID villagerId = villager.getUUID();
        long currentTime = System.currentTimeMillis();
        if (lastGiftTime.containsKey(villagerId)) {
            if (currentTime - lastGiftTime.get(villagerId) < 30000) {
                return;
            }
        }
        
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class,
            villager.getBoundingBox().inflate(10.0D));
        
        for (Player player : nearbyPlayers) {
            // Si jugador tiene menos del 30% de vida
            if (player.getHealth() < player.getMaxHealth() * 0.3F) {
                // Crear y lanzar pan con MEJOR FÍSICA
                ItemStack bread = new ItemStack(Items.BREAD, 1);
                
                // Posición en la mano del aldeano
                double spawnX = villager.getX();
                double spawnY = villager.getEyeY() - 0.3D; // A la altura del pecho
                double spawnZ = villager.getZ();
                
                // Dirección hacia el jugador
                Vec3 direction = new Vec3(
                    player.getX() - villager.getX(),
                    player.getEyeY() - villager.getEyeY() + 0.2, // Hacia arriba ligeramente
                    player.getZ() - villager.getZ()
                ).normalize();
                
                // Item entity con velocidad inicial
                ItemEntity itemEntity = new ItemEntity(
                    level,
                    spawnX + direction.x * 0.5, // Spawn ligeramente adelante
                    spawnY,
                    spawnZ + direction.z * 0.5,
                    bread
                );
                
                // Velocidad MÁS FUERTE hacia el jugador
                itemEntity.setDeltaMovement(
                    direction.x * 0.5,
                    direction.y * 0.4 + 0.3, // Arc projectile
                    direction.z * 0.5
                );
                itemEntity.setPickUpDelay(10); // 0.5 segundos delay
                itemEntity.setDefaultPickUpDelay();
                
                // CRÍTICO: Añadir la entidad al mundo
                boolean spawned = level.addFreshEntity(itemEntity);
                
                if (spawned) {
                    // Partículas de corazón
                    level.sendParticles(ParticleTypes.HEART,
                        villager.getX(), villager.getY() + 2.0, villager.getZ(),
                        5, 0.5, 0.5, 0.5, 0.0);
                    
                    // Mensaje
                    VillagerPersonalityData data = VillagerPersonalityData.get(level);
                    VillagerPersonality personality = data.getPersonality(villagerId);
                    String name = personality != null ? personality.getCustomName() : "Villager";
                    
                    player.sendSystemMessage(Component.translatable("villagediplomacy.personality.gift_bread", name));
                    
                    lastGiftTime.put(villagerId, currentTime);
                }
                
                return; // Solo un regalo por aldeano
            }
        }
    }
    
    /**
     * Actualizar estado emocional y mostrar partículas
     */
    private static void updateEmotionalState(Villager villager, VillagerPersonality personality, ServerLevel level) {
        personality.updateEmotion();
        
        // Mostrar partículas según emoción
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
    
    /**
     * Comportamiento de luto: Mirar hacia estación vacía
     */
    private static void checkMourningBehavior(Villager villager, ServerLevel level, VillagerPersonalityData data) {
        // Limpiar muertes antiguas
        data.cleanupOldDeaths();
        
        // Buscar estación de trabajo cercana
        var jobSiteOptional = villager.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE);
        
        if (jobSiteOptional.isPresent()) {
            BlockPos jobSite = jobSiteOptional.get().pos();
            // Mirar hacia la estación
            villager.getLookControl().setLookAt(
                jobSite.getX() + 0.5,
                jobSite.getY() + 0.5,
                jobSite.getZ() + 0.5
            );
        }
    }
    
    /**
     * Cuando muere un aldeano
     */
    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        
        ServerLevel level = (ServerLevel) villager.level();
        VillagerPersonalityData data = VillagerPersonalityData.get(level);
        VillagerPersonality personality = data.getPersonality(villager.getUUID());
        
        if (personality == null) return;
        
        // Registrar muerte para luto
        data.registerDeath(villager.getUUID(), personality.getCustomName());
        
        // TESTAMENTO: Si tenía buena relación con un jugador (30+ trades = 90 puntos)
        if (personality.getPlayerReputationBonus() >= 30) {
            dropTestament(villager, personality, level);
        }
    }
    
    /**
     * Dropear "testamento" con item especial
     */
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
        
        // Aplicar nombre y lore
        testament.getOrCreateTag().put("display", new net.minecraft.nbt.CompoundTag());
        testament.getTag().getCompound("display").put("Name",
            net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(customName)));
        testament.getTag().getCompound("display").put("Lore", loreList);
        
        // ENCANTAMIENTO para brillo (sin efecto real)
        net.minecraft.nbt.ListTag enchantments = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag enchantment = new net.minecraft.nbt.CompoundTag();
        enchantment.putString("id", "minecraft:unbreaking");
        enchantment.putInt("lvl", 1);
        enchantments.add(enchantment);
        testament.getTag().put("Enchantments", enchantments);
        
        // HideFlags para ocultar el encantamiento en el tooltip (solo brillo)
        testament.getTag().putInt("HideFlags", 1);
        
        ItemEntity itemEntity = new ItemEntity(level, 
            villager.getX(), 
            villager.getY() + 0.5, 
            villager.getZ(), 
            testament);
        
        // Velocidad hacia arriba para que sea m\u00e1s visible + efecto de brillo
        itemEntity.setDeltaMovement(0, 0.4, 0);
        itemEntity.setGlowingTag(true); // Efecto de brillo visual
        
        level.addFreshEntity(itemEntity);
        
        // M\u00daLTIPLES EFECTOS DE PART\u00cdCULAS para drama m\u00e1ximo
        double x = villager.getX();
        double y = villager.getY() + 1.0;
        double z = villager.getZ();
        
        // 1. Part\u00edculas END_ROD (columna de luz dorada)
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            x, y, z, 30, 0.3, 0.5, 0.3, 0.08);
        
        // 2. Part\u00edculas ENCHANT (efecto m\u00e1gico giratorio)
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            x, y, z, 40, 0.4, 0.4, 0.4, 1.0);
        
        // 3. Part\u00edculas SOUL (efecto espiritual)
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
            x, y, z, 15, 0.2, 0.2, 0.2, 0.02);
        
        // 4. Part\u00edculas TOTEM_OF_UNDYING (efecto \u00e9pico dorado)
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
            x, y, z, 10, 0.3, 0.3, 0.3, 0.1);
        
        // EFECTO DE SONIDO ESPECIAL (level up = m\u00e1gico y especial)
        level.playSound(null, x, y, z, 
            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
            net.minecraft.sounds.SoundSource.NEUTRAL,
            0.8f, 1.2f);
    }

    private static void addTestamentLoreLine(net.minecraft.nbt.ListTag loreList, Component line) {
        loreList.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(line)));
    }
}
