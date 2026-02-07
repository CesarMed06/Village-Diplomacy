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

    private final String[] adultChestMessages = {
            "§c[Aldeano] ¡OYE! ¡Eso es MÍO!",
            "§c[Aldeano] *jadea* ¡Un LADRÓN!",
            "§c[Aldeano] ¡DETENTE! ¡Devuelve eso!",
            "§c[Aldeano] ¡Nos... nos estás robando!",
            "§c[Aldeano] ¡No puedo creer que hagas esto!",
            "§c[Aldeano] ¡GUARDIAS! ¡Tenemos un ladrón!",
            "§c[Aldeano] ¡Cómo TE ATREVES a abrir eso!",
            "§c[Aldeano] ¡Ese cofre NO es tuyo!",
            "§c[Aldeano] ¡Aléjate de mis pertenencias!",
            "§c[Aldeano] ¿¡Qué crees que estás haciendo!?",
            "§c[Aldeano] ¡Trabajé AÑOS por lo que hay ahí!",
            "§c[Aldeano] ¡Te arrepentirás de esto, ladrón!",
            "§c[Aldeano] ¡Eso es propiedad privada!",
            "§c[Aldeano] ¡Mis ahorros de toda la vida están ahí!",
            "§c[Aldeano] ¡AYUDA! ¡ROBO!",
            "§c[Aldeano] ¡Esto es una violación de confianza!",
            "§c[Aldeano] ¡Aléjate de mi cofre!",
            "§c[Aldeano] ¡No eres mejor que un saqueador!"
    };

    private final String[] babyChestMessages = {
            "§c[Aldeano Bebé] *llora* ¡Mami! ¡Están robando nuestras cosas!",
            "§c[Aldeano Bebé] ¡Noooo! ¡Ese es el cofre de nuestra familia!",
            "§c[Aldeano Bebé] ¿Por qué eres malo? *solloza*",
            "§c[Aldeano Bebé] ¡Se lo voy a decir a mi papá!",
            "§c[Aldeano Bebé] *huye llorando* ¡LADRÓN!",
            "§c[Aldeano Bebé] ¡Eso no es tuyo! *llora*",
            "§c[Aldeano Bebé] ¡Mala persona! ¡Malo!",
            "§c[Aldeano Bebé] ¡Mis juguetes están ahí!",
            "§c[Aldeano Bebé] *grita* ¡PAAAAARA!",
            "§c[Aldeano Bebé] ¡Eres un gran abusón!",
            "§c[Aldeano Bebé] ¡Tengo miedo! *llora*",
            "§c[Aldeano Bebé] ¡Papi dijo que los extraños son peligrosos!"
    };

    private final String[] adultLootMessages = {
            "§c[Aldeano] ¡Esos son NUESTROS suministros!",
            "§c[Aldeano] ¡Estás tomando todo lo que tenemos!",
            "§c[Aldeano] ¡LADRÓN! ¡Alguien ayude!",
            "§c[Aldeano] ¡Trabajé DURO por esos objetos!",
            "§c[Aldeano] ¡Nos estás robando sin piedad!",
            "§c[Aldeano] ¡Que seas maldito por siempre!",
            "§c[Aldeano] ¡El Gólem de Hierro se enterará de esto!",
            "§c[Aldeano] ¡Nos estás dejando sin NADA!",
            "§c[Aldeano] ¿¡Cómo sobreviviremos ahora!?",
            "§c[Aldeano] ¡Eso era para el invierno!",
            "§c[Aldeano] ¡Eres peor que los saqueadores!",
            "§c[Aldeano] ¡Espero que el karma te alcance!",
            "§c[Aldeano] ¡Nos has condenado a todos!",
            "§c[Aldeano] ¡Nuestros niños morirán de hambre por tu culpa!",
            "§c[Aldeano] ¡Esto es imperdonable!"
    };

    private final String[] babyLootMessages = {
            "§c[Aldeano Bebé] *solloza* ¡Ese era mi juguete favorito!",
            "§c[Aldeano Bebé] ¡No no no! ¡Nuestra comida no!",
            "§c[Aldeano Bebé] ¡Eres un gran malo!",
            "§c[Aldeano Bebé] ¡Te odio! *llora fuerte*",
            "§c[Aldeano Bebé] ¡Devuélveloooo! *grita*",
            "§c[Aldeano Bebé] ¡Eso no es justo!",
            "§c[Aldeano Bebé] ¡Eres malvado! *solloza*",
            "§c[Aldeano Bebé] ¡Nunca olvidaré esto!",
            "§c[Aldeano Bebé] ¿¡Por qué harías eso!? *llora*",
            "§c[Aldeano Bebé] ¡Mami se va a enojar mucho!"
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
                "§4[Diplomacia de Aldeas] ¡Mataste a un NIÑO! ¡La aldea NUNCA te perdonará!",
                "§4[Diplomacia de Aldeas] ¡MONSTRUO! ¡Asesinaste a un niño inocente!",
                "§4[Diplomacia de Aldeas] Un bebé...mataste a un bebé. ¡Eres PURA MALDAD!",
                "§4[Diplomacia de Aldeas] La aldea llora...has matado a uno de sus niños."
        }
                : new String[] {
                        "§c[Diplomacia de Aldeas] ¡Mataste a un aldeano!",
                        "§c[Diplomacia de Aldeas] ¡ASESINATO! ¡La aldea fue testigo de tu crimen!",
                        "§c[Diplomacia de Aldeas] ¡Un aldeano murió por tu mano!",
                        "§c[Diplomacia de Aldeas] ¡Has quitado una vida inocente!"
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
                        "§4[Diplomacia de Aldeas] ¡ASESINATO! ¡Tiempo de crimen extendido! Total: " + totalSeconds + " segundos"));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§4[Diplomacia de Aldeas] ¡ASESINATO PRESENCIADO! ¡Los Golems de Hierro te cazarán por 2 minutos!"));
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
                "§4[Diplomacia de Aldeas] ¡Mataste a un Golem de Hierro! Reputación -150 (Total: " +
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
                "§c[Aldeano Bebé] *grita* ¡AYUDA! ¡Alguien me está pegando!",
                "§c[Aldeano Bebé] ¡AY AY AY! ¡DETENTE!",
                "§c[Aldeano Bebé] *llora* ¡Eso duele!",
                "§c[Aldeano Bebé] ¡MAMI! ¡PAPI! ¡AYUDA!",
                "§c[Aldeano Bebé] ¿¡Por qué me estás lastimando!? *solloza*",
                "§c[Aldeano Bebé] ¡No hice nada! *gime*",
                "§c[Aldeano Bebé] *llora* ¡Déjame en paz!",
                "§c[Aldeano Bebé] ¡Eres una persona mala!",
                "§c[Aldeano Bebé] *gritando y llorando* ¡PARA!",
                "§c[Aldeano Bebé] ¡Se lo diré al Gólem de Hierro!",
                "§c[Aldeano Bebé] *aterrado* ¡Por favor no me lastimes!"
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
                            "§c[Granjero] ¡Detente! ¡Solo intento alimentar a la aldea!",
                            "§c[Granjero] ¡No me lastimes! ¿¡Quién cuidará los cultivos!?",
                            "§c[Granjero] ¡Trabajo los campos día y noche, y ASÍ es mi agradecimiento!?",
                            "§c[Granjero] ¡Déjame en paz! ¡La cosecha no se recogerá sola!",
                            "§c[Granjero] ¡Estás atacando al que cultiva tu comida!",
                            "§c[Granjero] ¡Tengo zanahorias que plantar! ¡Guardias!",
                            "§c[Granjero] ¡Mi espalda ya duele de tanto cultivar!",
                            "§c[Granjero] ¡Sin mí, morirías de hambre!"
                    };
                    break;

                case "librarian":
                    professionMessages = new String[] {
                            "§c[Bibliotecario] ¡La violencia no es la respuesta! ¡Lee un libro!",
                            "§c[Bibliotecario] ¡Cesa este barbarismo de inmediato!",
                            "§c[Bibliotecario] ¡Soy un hombre de CONOCIMIENTO, no de combate!",
                            "§c[Bibliotecario] ¡Este comportamiento no es nada académico!",
                            "§c[Bibliotecario] ¡Detente! ¡Piensa en los libros!",
                            "§c[Bibliotecario] ¡Preferiría resolver esto intelectualmente!",
                            "§c[Bibliotecario] ¡Mis encantamientos no me salvarán de ESTO!",
                            "§c[Bibliotecario] ¡Bárbaro! ¡Simplemente bárbaro!"
                    };
                    break;

                case "armorer":
                    professionMessages = new String[] {
                            "§c[Armero] ¿¡Te atreves a atacar a un MAESTRO de armaduras!?",
                            "§c[Armero] ¿¡Forjo protección, y tú me golpeas!?",
                            "§c[Armero] ¡Irónico! ¡Atacar a quien hace armaduras!",
                            "§c[Armero] ¡Debería haber dejado una espada cerca!",
                            "§c[Armero] ¿¡Quién reparará tu equipo AHORA, tonto!?",
                            "§c[Armero] ¡Mi martillo de herrería está AHÍ MISMO!",
                            "§c[Armero] ¡Tipo rudo, atacando a un armero!",
                            "§c[Armero] ¡Trabajo con METAL todo el día! ¡Mala idea!"
                    };
                    break;

                case "weaponsmith":
                    professionMessages = new String[] {
                            "§c[Armero] ¿¡Atacar a un ARMERO!? ¿¡Estás LOCO!?",
                            "§c[Armero] ¡Forjo ESPADAS, tonto!",
                            "§c[Armero] ¡Movimiento audaz atacar a quien hace armas!",
                            "§c[Armero] ¡Espera a que tome mi mejor hoja!",
                            "§c[Armero] ¡Conozco 50 formas de lastimarte con ESTE martillo!",
                            "§c[Armero] ¡Aldeano equivocado para meterse, amigo!",
                            "§c[Armero] ¡Estoy rodeado de armas! ¡Piensa!",
                            "§c[Armero] ¡Cada hoja aquí es mía!"
                    };
                    break;

                case "toolsmith":
                    professionMessages = new String[] {
                            "§c[Herrero] ¡Detente! ¡Soy quien hace tus herramientas!",
                            "§c[Herrero] ¡No más picos para TI!",
                            "§c[Herrero] ¡Sin mí, buena suerte minando!",
                            "§c[Herrero] ¡Tengo martillos por todas partes, mala idea!",
                            "§c[Herrero] ¿¡Quién forjará tus herramientas ahora!?",
                            "§c[Herrero] ¿¡Crees que puedes minar con tus PUÑOS!?",
                            "§c[Herrero] ¡Soy la razón por la que TIENES herramientas!",
                            "§c[Herrero] ¡Buena suerte fabricando sin mí!"
                    };
                    break;

                case "cleric":
                    professionMessages = new String[] {
                            "§c[Clérigo] ¿¡Atacas a un sanador!? ¡BLASFEMIA!",
                            "§c[Clérigo] ¡Que los dioses te perdonen, porque yo no lo haré!",
                            "§c[Clérigo] ¡Esto es sacrilegio!",
                            "§c[Clérigo] ¡Curo a los enfermos, y ASÍ es mi recompensa!?",
                            "§c[Clérigo] ¡Necesitarás mis pociones de curación después!",
                            "§c[Clérigo] ¡Los dioses están observando, pecador!",
                            "§c[Clérigo] ¡Rezaré para que veas el error de tus caminos!",
                            "§c[Clérigo] ¡La retribución divina te espera!"
                    };
                    break;

                case "butcher":
                    professionMessages = new String[] {
                            "§c[Carnicero] ¡Persona equivocada! ¡Tengo CUCHILLAS!",
                            "§c[Carnicero] ¡Trabajo con CARNE y CUCHILLOS diariamente!",
                            "§c[Carnicero] ¡Mala idea atacar a alguien con cuchillos!",
                            "§c[Carnicero] ¡Descuartizo ANIMALES, no personas!",
                            "§c[Carnicero] ¡Mi cuchilla está AFILADA, amigo!",
                            "§c[Carnicero] ¡No más chuletas de cerdo cocidas para TI!",
                            "§c[Carnicero] ¡Trabajo con carne cruda, puedo manejarte!",
                            "§c[Carnicero] ¡Estás cometiendo un ERROR GRAVE!"
                    };
                    break;

                case "cartographer":
                    professionMessages = new String[] {
                            "§c[Cartógrafo] ¡Detente! ¡Hago tus MAPAS!",
                            "§c[Cartógrafo] ¡Sin mí, te PERDERÁS!",
                            "§c[Cartógrafo] ¡Trazo el mundo, y así es mi agradecimiento!?",
                            "§c[Cartógrafo] ¡Buena suerte navegando sin mis mapas!",
                            "§c[Cartógrafo] ¡Guardias! ¡Alguien está atacando al cartografiador!",
                            "§c[Cartógrafo] ¡Exploro para que tú no tengas que hacerlo!",
                            "§c[Cartógrafo] ¿¡Perdido!? ¡No vengas a MÍ nunca más!",
                            "§c[Cartógrafo] ¡Mis mapas no te mostrarán ningún tesoro ahora!"
                    };
                    break;

                case "fisherman":
                    professionMessages = new String[] {
                            "§c[Pescador] ¡Detente! ¿¡Qué te hice!?",
                            "§c[Pescador] ¡Solo pesco todo el día!",
                            "§c[Pescador] ¡Deja en paz a un simple pescador!",
                            "§c[Pescador] ¡Tengo una caña de pescar, mantente atrás!",
                            "§c[Pescador] ¡No más comercio de pescado para ti!",
                            "§c[Pescador] ¡Todo lo que hago es PESCAR! ¿¡Por qué atacarme!?",
                            "§c[Pescador] ¡Proporciono COMIDA para todos!",
                            "§c[Pescador] ¡Atrapa tu propio pescado de ahora en adelante!"
                    };
                    break;

                case "fletcher":
                    professionMessages = new String[] {
                            "§c[Flechero] ¡Hago FLECHAS! ¡Tengo muchas aquí mismo!",
                            "§c[Flechero] ¡Artesano equivocado para atacar, amigo!",
                            "§c[Flechero] ¡Detente! ¡Estoy rodeado de proyectiles!",
                            "§c[Flechero] ¡No más flechas para TU arco!",
                            "§c[Flechero] ¡Puedo disparar desde AQUÍ, lo sabes!",
                            "§c[Flechero] ¿¡Atacar al fabricante de flechas!? ¡Audaz!",
                            "§c[Flechero] ¿¡Quién proveerá tus flechas AHORA!?",
                            "§c[Flechero] ¡Tengo un arco justo detrás de mí!"
                    };
                    break;

                case "leatherworker":
                    professionMessages = new String[] {
                            "§c[Peletero] ¡Detente! ¡Hago tu armadura de cuero!",
                            "§c[Peletero] ¿¡Quién fabricará tu equipo!?",
                            "§c[Peletero] ¡Trabajo duro en cada pieza!",
                            "§c[Peletero] ¡No más productos de cuero para ti!",
                            "§c[Peletero] ¡Guardias! ¡Ayuda!",
                            "§c[Peletero] ¿¡Así es como tratas a un artesano!?",
                            "§c[Peletero] ¡Mi taller es mi sustento!",
                            "§c[Peletero] ¡Ingrato! ¡Hago productos de calidad!"
                    };
                    break;

                case "mason":
                    professionMessages = new String[] {
                            "§c[Albañil] ¡Construyo con PIEDRA! ¡Soy fuerte!",
                            "§c[Albañil] ¡Mala idea! ¡Manejo bloques pesados diariamente!",
                            "§c[Albañil] ¡Detente! ¿¡Quién construirá tus estructuras!?",
                            "§c[Albañil] ¡Estas manos dan forma a la PIEDRA, pueden manejarte!",
                            "§c[Albañil] ¡Soy un ALBAÑIL! ¡Somos gente resistente!",
                            "§c[Albañil] ¡Guardias! ¡Alguien está atacando al constructor!",
                            "§c[Albañil] ¡Mi trabajo es permanente, igual que mi memoria!",
                            "§c[Albañil] ¡Te metes con la piedra, recibes la roca!"
                    };
                    break;

                case "shepherd":
                    professionMessages = new String[] {
                            "§c[Pastor] ¡Déjame en paz! ¡Solo cuido ovejas!",
                            "§c[Pastor] ¡Todo lo que hago es esquilar lana!",
                            "§c[Pastor] ¿¡Por qué atacar a un pastor pacífico!?",
                            "§c[Pastor] ¡Mis ovejas me necesitan!",
                            "§c[Pastor] ¡Guardias! ¡Ayuden al pastor!",
                            "§c[Pastor] ¡Proporciono lana para todos!",
                            "§c[Pastor] ¿¡Así es como tratas a un pastor!?",
                            "§c[Pastor] ¡No más lana para tus camas!"
                    };
                    break;

                case "nitwit":
                    professionMessages = new String[] {
                            "§c[Tonto] *confundido* ¿¡Por qué!? ¡Ni siquiera tengo trabajo!",
                            "§c[Tonto] ¡Solo estoy... existiendo! ¡Déjame en paz!",
                            "§c[Tonto] *entra en pánico* ¡AYUDA! ¡Alguien!",
                            "§c[Tonto] ¡No entiendo! ¿¡Qué hice!?",
                            "§c[Tonto] *gime* ¡Por favor detente!",
                            "§c[Tonto] ¡Soy inofensivo! ¿¡Por qué yo!?",
                            "§c[Tonto] ¡Esto no tiene sentido!"
                    };
                    break;

                default:
                    // Profesión desconocida o sin profesión
                    professionMessages = new String[] {
                            "§c[Aldeano] ¡AY! ¿¡Por qué me atacas!?",
                            "§c[Aldeano] ¡Detente de inmediato!",
                            "§c[Aldeano] ¿¡Qué te hice!?",
                            "§c[Aldeano] ¡Guardias! ¡GUARDIAS!",
                            "§c[Aldeano] ¡Te arrepentirás de esto!",
                            "§c[Aldeano] ¿¡Has perdido la cabeza!?",
                            "§c[Aldeano] ¡Déjame en paz, bruto!",
                            "§c[Aldeano] ¡Esto es violencia!",
                            "§c[Aldeano] ¡El Gólem de Hierro se enterará de esto!"
                    };
            }

            message = professionMessages[level.getRandom().nextInt(professionMessages.length)];
        }

        player.sendSystemMessage(Component.literal(message));
        player.sendSystemMessage(Component.literal(
                "§c[Diplomacia de Aldeas] ¡Atacaste a un aldeano! Reputación -10 (Total: " +
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

                        // MENSAJES ESPECÍFICOS POR TIPO DE ANIMAL
                        String[] babyMessages;
                        String[] adultMessages;

                        switch (animalType) {
                            case "cow":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes a nuestras vacas! ¡Necesitamos leche!",
                                        "§c[Aldeano Bebé] ¡Esa vaca nos da leche! *llora*",
                                        "§c[Aldeano Bebé] ¡Muu es mi amiga!",
                                        "§c[Aldeano Bebé] ¡Deja la vaca en paz!",
                                        "§c[Aldeano Bebé] ¡Amo esa vaca!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Detente! ¡Esa vaca proporciona leche a la aldea!",
                                        "§c[Aldeano] ¡Nuestro suministro de lácteos! ¡Déjala en paz!",
                                        "§c[Aldeano] ¡Esa vaca alimenta a nuestros niños!",
                                        "§c[Aldeano] ¡Dependemos de esas vacas para leche y cuero!",
                                        "§c[Aldeano] ¡Aléjate de nuestro ganado!",
                                        "§c[Aldeano] ¡Esas vacas son esenciales para nuestra supervivencia!",
                                        "§c[Aldeano] ¡Son semanas de leche lo que estás amenazando!"
                                };
                                break;

                            case "sheep":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes a Pelusa! *llora*",
                                        "§c[Aldeano Bebé] ¡Esa oveja hace nuestras camas!",
                                        "§c[Aldeano Bebé] ¡Me gusta acariciar las ovejas!",
                                        "§c[Aldeano Bebé] ¡Deja a la lanuda en paz!",
                                        "§c[Aldeano Bebé] ¡La oveja es tan suave!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Esa oveja proporciona nuestra lana!",
                                        "§c[Aldeano] ¡Detente! ¡Necesitamos esa lana para mantas!",
                                        "§c[Aldeano] ¡Nuestro suministro textil! ¡Déjala en paz!",
                                        "§c[Aldeano] ¡Esas ovejas nos mantienen calientes en invierno!",
                                        "§c[Aldeano] ¡Esquilamos esas ovejas para ropa!",
                                        "§c[Aldeano] ¡Esa es nuestra fuente de lana, bruto!",
                                        "§c[Aldeano] ¡Sin lana, nos congelamos!"
                                };
                                break;

                            case "pig":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes al cerdito! *solloza*",
                                        "§c[Aldeano Bebé] ¡Ese cerdo hace sonidos graciosos!",
                                        "§c[Aldeano Bebé] ¡Oinc-oinc es lindo!",
                                        "§c[Aldeano Bebé] ¡Deja al cerdito en paz!",
                                        "§c[Aldeano Bebé] ¡Alimento a ese cerdo todos los días!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Ese cerdo es ganado valioso!",
                                        "§c[Aldeano] ¡Detente! ¡Esos cerdos son para cría!",
                                        "§c[Aldeano] ¡Criamos esos cerdos con cuidado!",
                                        "§c[Aldeano] ¡Ese cerdo alimentará familias este invierno!",
                                        "§c[Aldeano] ¡Deja nuestro suministro de carne en paz!",
                                        "§c[Aldeano] ¡Esos cerdos son nuestra inversión!",
                                        "§c[Aldeano] ¡Atrás! ¡Ese cerdo está reservado!"
                                };
                                break;

                            case "chicken":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes la gallina! *llora*",
                                        "§c[Aldeano Bebé] ¡Esa gallina nos da huevos!",
                                        "§c[Aldeano Bebé] ¡Colecciono huevos de ellas!",
                                        "§c[Aldeano Bebé] ¡Las gallinas son mi trabajo!",
                                        "§c[Aldeano Bebé] ¡Plumitas es tan linda!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Esas gallinas ponen nuestros huevos!",
                                        "§c[Aldeano] ¡Detente! ¡Esa gallina es nuestro desayuno!",
                                        "§c[Aldeano] ¡Necesitamos esos huevos diariamente!",
                                        "§c[Aldeano] ¡Esa gallina es parte de nuestra granja!",
                                        "§c[Aldeano] ¡Deja nuestras aves en paz!",
                                        "§c[Aldeano] ¡Esas gallinas son productoras de huevos!",
                                        "§c[Aldeano] ¡Sin gallinas, no hay huevos!"
                                };
                                break;

                            case "rabbit":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes al conejito! *llora*",
                                        "§c[Aldeano Bebé] ¡Los conejitos son tan lindos!",
                                        "§c[Aldeano Bebé] ¡Ese es mi conejo favorito!",
                                        "§c[Aldeano Bebé] ¡Deja al saltador en paz!",
                                        "§c[Aldeano Bebé] ¡Quiero acariciarlos!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Esos conejos son parte de nuestro ecosistema!",
                                        "§c[Aldeano] ¡Deja los conejos en paz!",
                                        "§c[Aldeano] ¡Son criaturas inofensivas!",
                                        "§c[Aldeano] ¡Deja de atacar animales inocentes!",
                                        "§c[Aldeano] ¡Esos conejos ayudan a nuestros jardines!",
                                        "§c[Aldeano] ¿¡Qué te hizo ese conejo!?",
                                        "§c[Aldeano] ¡Solo son conejos!"
                                };
                                break;

                            case "horse":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes al caballito! *llora*",
                                        "§c[Aldeano Bebé] ¡Quiero montar caballos cuando sea grande!",
                                        "§c[Aldeano Bebé] ¡Ese caballo es tan bonito!",
                                        "§c[Aldeano Bebé] ¡Deja el caballo en paz!",
                                        "§c[Aldeano Bebé] ¡Los caballos son nobles!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Ese caballo es nuestro transporte!",
                                        "§c[Aldeano] ¡Detente! ¡Necesitamos ese caballo para viajar!",
                                        "§c[Aldeano] ¡Esos caballos son costosos!",
                                        "§c[Aldeano] ¡Eso son semanas de trabajo de cría!",
                                        "§c[Aldeano] ¡Deja nuestros caballos en paz!",
                                        "§c[Aldeano] ¡Usamos esos caballos para rutas comerciales!",
                                        "§c[Aldeano] ¡Ese caballo lleva nuestros suministros!",
                                        "§c[Aldeano] ¡Estás atacando nuestra movilidad!"
                                };
                                break;

                            case "camel":
                                babyMessages = new String[] {
                                        "§c[Aldeano Bebé] ¡No lastimes el camello! *llora*",
                                        "§c[Aldeano Bebé] ¡Los camellos son para el desierto!",
                                        "§c[Aldeano Bebé] ¡Ese camello es genial!",
                                        "§c[Aldeano Bebé] ¡Deja el camello en paz!",
                                        "§c[Aldeano Bebé] ¡Me gustan los camellos!"
                                };
                                adultMessages = new String[] {
                                        "§c[Aldeano] ¡Ese camello es nuestro transporte del desierto!",
                                        "§c[Aldeano] ¡Detente! ¡Necesitamos ese camello para viajar!",
                                        "§c[Aldeano] ¡Esos camellos son costosos!",
                                        "§c[Aldeano] ¡Eso son semanas de trabajo de cría!",
                                        "§c[Aldeano] ¡Deja nuestros camellos en paz!",
                                        "§c[Aldeano] ¡Usamos esos camellos para rutas del desierto!",
                                        "§c[Aldeano] ¡Ese camello lleva nuestros suministros!",
                                        "§c[Aldeano] ¡Estás atacando nuestra movilidad!"
                                };
                                break;

                            default:
                                babyMessages = new String[] {"§c[Aldeano Bebé] ¡No los lastimes!"};
                                adultMessages = new String[] {"§c[Aldeano] ¡Detente!"};
                        }

                        String[] messages = villager.isBaby() ? babyMessages : adultMessages;

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Diplomacia de Aldeas] ¡Atacaste " + animalType + "! Reputación -5 (Total: " +
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
                                    "§c[Aldeano Bebé] ¡NOOOO! ¡Mataste a Bessie! *grita*",
                                    "§c[Aldeano Bebé] ¡No más leche ahora! *solloza*",
                                    "§c[Aldeano Bebé] ¡Esa vaca tenía un ternero! *llora*",
                                    "§c[Aldeano Bebé] ¿¡Por qué!? ¡Nos daba leche! *desconsolado*",
                                    "§c[Aldeano Bebé] Muu se fue... *solloza*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡ASESINASTE NUESTRA VACA!",
                                    "§c[Aldeano] ¡Son MESES de producción de leche PERDIDOS!",
                                    "§c[Aldeano] ¿¡Cómo alimentaremos a nuestros niños sin leche!?",
                                    "§c[Aldeano] ¡Esa vaca valía 10 esmeraldas!",
                                    "§c[Aldeano] ¡Has destruido nuestra granja láctea!",
                                    "§c[Aldeano] ¡ASESINO DE ANIMALES! ¡Esa vaca tenía terneros!",
                                    "§c[Aldeano] ¡Criamos esa vaca desde su nacimiento!",
                                    "§c[Aldeano] ¡No hay leche, ni queso, ni cuero! ¡Gracias a TI!"
                            };
                            break;

                        case "sheep":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste a Pelusa! ¡NOOO! *llora*",
                                    "§c[Aldeano Bebé] ¡No más lana ahora! *solloza*",
                                    "§c[Aldeano Bebé] ¡Esa oveja era tan suave! *grita*",
                                    "§c[Aldeano Bebé] ¿¡Por qué matar a la lanuda!? *desconsolado*",
                                    "§c[Aldeano Bebé] ¡Iba a esquilarla mañana! *devastado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MATASTE NUESTRA OVEJA!",
                                    "§c[Aldeano] ¡Esa oveja producía lana por AÑOS!",
                                    "§c[Aldeano] ¿¡Cómo haremos mantas ahora!?",
                                    "§c[Aldeano] ¡Nos CONGELAREMOS sin esa lana!",
                                    "§c[Aldeano] ¡Esa oveja era nuestra fuente textil!",
                                    "§c[Aldeano] ¡ASESINO! ¡Criamos esa oveja cuidadosamente!",
                                    "§c[Aldeano] ¡No hay lana significa no hay ropa abrigada!",
                                    "§c[Aldeano] ¡Esa oveja era de lana blanca - RARA!"
                            };
                            break;

                        case "pig":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste al cerdito! ¡MONSTRUO! *llora*",
                                    "§c[Aldeano Bebé] ¡Ese cerdo iba a tener bebés! *solloza*",
                                    "§c[Aldeano Bebé] ¡Oinc-oinc se fue! *grita*",
                                    "§c[Aldeano Bebé] ¿¡Por qué!? ¡Era tan gracioso! *devastado*",
                                    "§c[Aldeano Bebé] ¡Le daba zanahorias a ese cerdo! *desconsolado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MASACRASTE NUESTRO CERDO!",
                                    "§c[Aldeano] ¡Ese cerdo era ganado reproductor!",
                                    "§c[Aldeano] ¡Acabas de matar el SUMINISTRO DE CARNE del invierno!",
                                    "§c[Aldeano] ¡Criamos ese cerdo por MESES!",
                                    "§c[Aldeano] ¡LADRÓN! ¡Ese cerdo era nuestra inversión!",
                                    "§c[Aldeano] ¿¡Cómo TE ATREVES a matar nuestro ganado!?",
                                    "§c[Aldeano] ¡Ese cerdo iba a alimentar familias!",
                                    "§c[Aldeano] ¡Has arruinado nuestro programa de cría!"
                            };
                            break;

                        case "chicken":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste a Plumitas! ¡NOOO! *llora*",
                                    "§c[Aldeano Bebé] ¡No más huevos ahora! *solloza*",
                                    "§c[Aldeano Bebé] ¡Esa gallina ponía huevos todos los días! *grita*",
                                    "§c[Aldeano Bebé] ¿¡Por qué!? ¡Era linda! *devastado*",
                                    "§c[Aldeano Bebé] ¡Yo recogía sus huevos! *desconsolado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MATASTE NUESTRA GALLINA!",
                                    "§c[Aldeano] ¡Esa era nuestra FUENTE DE DESAYUNO!",
                                    "§c[Aldeano] ¡Necesitábamos esos huevos diariamente!",
                                    "§c[Aldeano] ¡Esa gallina ponía huevos confiablemente!",
                                    "§c[Aldeano] ¿¡Cómo hornearemos sin huevos!?",
                                    "§c[Aldeano] ¡ASESINO DE AVES! ¡Eso era seguridad alimentaria!",
                                    "§c[Aldeano] ¡Un huevo al día - PERDIDO por tu culpa!",
                                    "§c[Aldeano] ¡Criamos esa gallina desde pollito!"
                            };
                            break;

                        case "rabbit":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste al conejito! ¡CRUEL! *solloza*",
                                    "§c[Aldeano Bebé] ¡Los conejitos son tan lindos! ¿¡Por qué!? *llora*",
                                    "§c[Aldeano Bebé] ¡El saltador está muerto! *grita*",
                                    "§c[Aldeano Bebé] ¡Eso fue tan malo! *devastado*",
                                    "§c[Aldeano Bebé] ¡Amaba ese conejo! *desconsolado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MATASTE EL CONEJO!",
                                    "§c[Aldeano] ¡Ese conejo ayudaba a controlar plagas del jardín!",
                                    "§c[Aldeano] ¿¡Qué clase de MONSTRUO mata conejos!?",
                                    "§c[Aldeano] ¡Son criaturas inofensivas!",
                                    "§c[Aldeano] ¡Los niños amaban ese conejo!",
                                    "§c[Aldeano] ¡Eres un BRUTO y un ABUSÓN!",
                                    "§c[Aldeano] ¡Matar animales inocentes! ¡Vergüenza!",
                                    "§c[Aldeano] ¡Ese conejo nunca lastimó a nadie!"
                            };
                            break;

                        case "horse":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste al caballito! ¡NOOO! *grita*",
                                    "§c[Aldeano Bebé] ¡Ese caballo era tan fuerte! *llora*",
                                    "§c[Aldeano Bebé] ¡Quería montarlo! *devastado*",
                                    "§c[Aldeano Bebé] ¡Los caballos son nobles! ¿¡Por qué!? *solloza*",
                                    "§c[Aldeano Bebé] ¡Eso es lo peor! *desconsolado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MATASTE NUESTRO CABALLO!",
                                    "§c[Aldeano] ¡Ese caballo era COSTOSO! ¡20 esmeraldas!",
                                    "§c[Aldeano] ¡Necesitábamos ese caballo para VIAJAR!",
                                    "§c[Aldeano] ¡Eso tomó SEMANAS de domar y criar!",
                                    "§c[Aldeano] ¿¡Cómo transportaremos bienes ahora!?",
                                    "§c[Aldeano] ¡ASESINO DE CABALLOS! ¡Ese era nuestro SUSTENTO!",
                                    "§c[Aldeano] ¡Usábamos ese caballo para rutas comerciales!",
                                    "§c[Aldeano] ¡Has paralizado nuestro comercio!",
                                    "§c[Aldeano] ¡Ese caballo era parte de la familia!",
                                    "§c[Aldeano] ¿¡Matar un caballo!? ¡No tienes CORAZÓN!"
                            };
                            break;

                        case "camel":
                            babyMessages = new String[] {
                                    "§c[Aldeano Bebé] ¡Mataste el camello! ¡NOOO! *grita*",
                                    "§c[Aldeano Bebé] ¡Ese camello era tan alto! *llora*",
                                    "§c[Aldeano Bebé] ¡Quería montarlo! *devastado*",
                                    "§c[Aldeano Bebé] ¡Los camellos son increíbles! ¿¡Por qué!? *solloza*",
                                    "§c[Aldeano Bebé] ¡Eso es tan cruel! *desconsolado*"
                            };
                            adultMessages = new String[] {
                                    "§c[Aldeano] ¡MATASTE NUESTRO CAMELLO!",
                                    "§c[Aldeano] ¡Ese camello era COSTOSO! ¡30 esmeraldas!",
                                    "§c[Aldeano] ¡Necesitábamos ese camello para VIAJES EN EL DESIERTO!",
                                    "§c[Aldeano] ¡Eso tomó SEMANAS de domar y criar!",
                                    "§c[Aldeano] ¿¡Cómo cruzaremos el desierto ahora!?",
                                    "§c[Aldeano] ¡ASESINO DE CAMELLOS! ¡Ese era nuestro SUSTENTO!",
                                    "§c[Aldeano] ¡Usábamos ese camello para rutas del desierto!",
                                    "§c[Aldeano] ¡Has paralizado nuestro comercio del desierto!",
                                    "§c[Aldeano] ¡Ese camello era irremplazable!",
                                    "§c[Aldeano] ¿¡Matar un camello!? ¡No tienes CORAZÓN!"
                            };
                            break;

                        default:
                            babyMessages = new String[] {"§c[Aldeano Bebé] ¡Lo mataste! *llora*"};
                            adultMessages = new String[] {"§c[Aldeano] ¡ASESINO!"};
                    }

                    String[] messages = villager.isBaby() ? babyMessages : adultMessages;

                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                    player.sendSystemMessage(Component.literal(
                            "§c[Diplomacia de Aldeas] ¡Mataste " + animalType + "! Reputación -25 (Total: " +
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
                        "§a[Diplomacia de Aldeas] ¡Completaste " + trades
                                + " intercambio(s)! La aldea aprecia tu negocio.",
                        "§a[Diplomacia de Aldeas] ¡" + trades + " intercambio(s) exitoso(s)! Tu reputación crece.",
                        "§a[Diplomacia de Aldeas] ¡Excelente comercio! Los aldeanos están complacidos.",
                        "§a[Diplomacia de Aldeas] " + trades + " intercambio(s) completado(s). La aldea confía más en ti.",
                        "§a[Diplomacia de Aldeas] ¡Buen comercio! La aldea valora tu negocio.",
                        "§a[Diplomacia de Aldeas] " + trades + " intercambio(s) hecho(s). Te estás convirtiendo en un cliente valioso."
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
                        "§a[Aldeano Bebé] ¡Hola héroe! *saluda*",
                        "§a[Aldeano Bebé] ¡Entra, eres el mejor!",
                        "§a[Aldeano Bebé] ¡Bienvenido! *se ríe*",
                        "§a[Aldeano Bebé] ¡Nuestro héroe está aquí!",
                        "§a[Aldeano Bebé] ¡Mamá dice que eres muy bueno!"
                }
                        : isNight ? new String[] {
                                "§a[Aldeano] Bienvenido, amigo. ¡Viaja seguro en la noche!",
                                "§a[Aldeano] Entra de la oscuridad, ¡campeón!",
                                "§a[Aldeano] ¡Nuestra casa es tu casa, incluso de noche!",
                                "§a[Aldeano] ¡Por favor, quédate seguro adentro!"
                        }
                        : isMorning ? new String[] {
                                "§a[Aldeano] ¡Buenos días, héroe! ¡Entra!",
                                "§a[Aldeano] ¡Madrugador! ¡Por favor, entra!",
                                "§a[Aldeano] ¡Día fresco, bienvenido amigo!",
                                "§a[Aldeano] ¡Buenos días! ¡Nuestras puertas siempre están abiertas para ti!"
                        }
                        : new String[] {
                                "§a[Aldeano] ¡Bienvenido, campeón!",
                                "§a[Aldeano] ¡Por favor, pasa!",
                                "§a[Aldeano] ¡Nuestras puertas están abiertas para ti!",
                                "§a[Aldeano] ¡Siéntete libre de entrar, amigo!",
                                "§a[Aldeano] ¡Entra, ponte cómodo!",
                                "§a[Aldeano] ¡Siempre eres bienvenido aquí!",
                                "§a[Aldeano] ¡Ah, nuestro protector llega!",
                                "§a[Aldeano] ¡Entra libremente, valiente!"
                        };

                String[] positiveCloseMessages = caughtByBaby ? new String[] {
                        "§a[Aldeano Bebé] ¡Gracias! *sonríe*",
                        "§a[Aldeano Bebé] ¡Buenos modales!",
                        "§a[Aldeano Bebé] ¡Eres tan amable!",
                        "§a[Aldeano Bebé] ¡Mamá también me enseñó eso!"
                }
                        : new String[] {
                                "§a[Aldeano] ¡Gracias por cerrarla!",
                                "§a[Aldeano] ¡Aprecio la cortesía, amigo!",
                                "§a[Aldeano] ¡Qué buenos modales!",
                                "§a[Aldeano] ¡Eres tan considerado!",
                                "§a[Aldeano] ¡Gracias, mantiene el frío afuera!",
                                "§a[Aldeano] ¡Muy apreciado, héroe!"
                        };

                String[] messages = isClosing ? positiveCloseMessages : positiveOpenMessages;
                player.sendSystemMessage(Component.literal(
                        messages[level.getRandom().nextInt(messages.length)]));

            } else if (reputation >= 100) {
                // MENSAJES EXPANDIDOS: Reputación neutral/buena
                String[] neutralOpenMessages = isNight ? new String[] {
                        "§e[Aldeano] Entra. Cuidado, está oscuro afuera.",
                        "§e[Aldeano] Adelante. Cuidado con los mobs.",
                        "§e[Aldeano] Seguro. No te quedes afuera mucho tiempo."
                }
                        : new String[] {
                                "§e[Aldeano] Adelante.",
                                "§e[Aldeano] Seguro, entra.",
                                "§e[Aldeano] De acuerdo.",
                                "§e[Aldeano] Siéntete libre.",
                                "§e[Aldeano] Sí, está bien.",
                                "§e[Aldeano] Entra si lo necesitas."
                        };

                String[] neutralCloseMessages = new String[] {
                        "§e[Aldeano] Gracias.",
                        "§e[Aldeano] De acuerdo.",
                        "§e[Aldeano] Apreciado.",
                        "§e[Aldeano] Bien."
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
                        "§6[Aldeano Bebé] Umm... Te estoy observando...",
                        "§6[Aldeano Bebé] Mami no confía en ti...",
                        "§6[Aldeano Bebé] *se esconde detrás de la puerta*"
                }
                        : new String[] {
                                "§6[Aldeano] *observa sospechosamente*",
                                "§6[Aldeano] Te tengo vigilado...",
                                "§6[Aldeano] No intentes nada gracioso.",
                                "§6[Aldeano] Hazlo rápido.",
                                "§6[Aldeano] No estoy feliz con esto.",
                                "§6[Aldeano] Mejor que no robes nada..."
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
                        "§c[Aldeano Bebé] ¡Deja de tocar nuestras puertas!",
                        "§c[Aldeano Bebé] ¡Eso no es tuyo!",
                        "§c[Aldeano Bebé] ¡Mami! ¡Hay una mala persona! *llora*",
                        "§c[Aldeano Bebé] ¡Vete! *asustado*",
                        "§c[Aldeano Bebé] ¡Das miedo!",
                        "§c[Aldeano Bebé] ¡AYUDA! *corre*"
                }
                        : new String[] {
                                "§c[Aldeano] ¡Quita tus manos de nuestras puertas!",
                                "§c[Aldeano] ¡Eso es propiedad privada!",
                                "§c[Aldeano] ¡No eres bienvenido aquí!",
                                "§c[Aldeano] ¡Deja de entrar a nuestros hogares!",
                                "§c[Aldeano] ¡SAL de aquí!",
                                "§c[Aldeano] ¿¡Cómo TE ATREVES!?",
                                "§c[Aldeano] ¡GUARDIAS! ¡Intruso!",
                                "§c[Aldeano] ¡Esta es NUESTRA casa, ladrón!",
                                "§c[Aldeano] ¡NO tienes derecho a estar aquí!",
                                "§c[Aldeano] ¡Debería llamar a los Gólems de Hierro!"
                        };

                player.sendSystemMessage(Component.literal(
                        negativeMessages[level.getRandom().nextInt(negativeMessages.length)]));
                player.sendSystemMessage(Component.literal(
                        "§c[Diplomacia de Aldeas] ¡Entraste sin invitación! Reputación -5 (Total: " +
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
                            "§c[Diplomacia de Aldeas] ¡Abriste cofre de la aldea! Reputación -10 (Total: " +
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
                        "§c[Diplomacia de Aldeas] ¡Robaste de la aldea! Reputación -15 (Total: " +
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
                            "§c[Diplomacia de Aldeas] " + blockType.systemMessage + " Reputación " + penalty +
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
                            "§a[Aldeano] ¡Ponte cómodo, amigo!",
                            "§a[Aldeano] Siéntete libre de descansar aquí cuando quieras.",
                            "§a[Aldeano] ¡Tu propia cama! Ya eres parte de la aldea.",
                            "§a[Aldeano] ¡Bienvenido a casa!",
                            "§7[Aldeano] *sonríe cálidamente* Es un buen lugar.",
                            "§a[Aldeano] ¡Es bueno tenerte viviendo con nosotros!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Poniéndote cómodo!?",
                            "§e[Aldeano] Ah, reclamando una cama ya veo...",
                            "§e[Aldeano] ¿¡Planeas quedarte un tiempo!?",
                            "§7[Aldeano] *asiente* Es un buen lugar para una cama.",
                            "§e[Aldeano] ¿¡Instalándote!?",
                            "§e[Aldeano] ¿¡Ahora construyes aquí!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Qué estás haciendo!?",
                            "§c[Aldeano] ¡Oye! ¡No eres bienvenido a construir aquí!",
                            "§7[Aldeano] *mira con furia sospechosamente*",
                            "§c[Aldeano] ¡No queremos que vivas aquí!",
                            "§c[Aldeano] ¿¡Crees que puedes mudarte así sin más!?",
                            "§c[Aldeano] ¡Saca eso de nuestra aldea!"
                        };
                    }
                } else if (placedBlock instanceof ChestBlock || placedBlock instanceof BarrelBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¿¡Preparando almacenamiento!? ¡Inteligente!",
                            "§a[Aldeano] Mantén tus objetos valiosos seguros, amigo.",
                            "§7[Aldeano] Ese es un lugar perfecto para un cofre.",
                            "§a[Aldeano] ¡Realmente estás haciendo de esto tu hogar!",
                            "§a[Aldeano] ¿¡Necesitas ayuda organizando tus cosas!?",
                            "§a[Aldeano] ¿¡Un cofre! ¿¡Te quedas a largo plazo entonces!?"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Almacenando tus pertenencias aquí!?",
                            "§e[Aldeano] Ah, preparando almacenamiento...",
                            "§7[Aldeano] Ese es un buen lugar para un cofre.",
                            "§e[Aldeano] ¿¡Haciendo de esto tu hogar!?",
                            "§e[Aldeano] Realmente te estás instalando...",
                            "§e[Aldeano] ¿¡Planeas quedarte!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Qué hay en ese cofre!?",
                            "§c[Aldeano] ¡No queremos TUS cosas aquí!",
                            "§c[Aldeano] ¿¡Te apoderas de nuestra aldea, verdad!?",
                            "§7[Aldeano] *observa sospechosamente*",
                            "§c[Aldeano] ¡No creas que no revisaremos eso!",
                            "§c[Aldeano] Estás tramando algo..."
                        };
                    }
                } else if (placedBlock instanceof FurnaceBlock || placedBlock instanceof BlastFurnaceBlock || placedBlock instanceof SmokerBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¿¡Un horno! ¡Podríamos usar más industria!",
                            "§a[Aldeano] ¡Excelente! ¡Un nuevo taller!",
                            "§a[Aldeano] ¡Estás contribuyendo a la economía de la aldea!",
                            "§7[Aldeano] Eso será útil para todos.",
                            "§a[Aldeano] ¡Buen pensamiento! Necesitamos más artesanos.",
                            "§a[Aldeano] ¡Realmente estás ayudando a crecer la aldea!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Preparando un horno!?",
                            "§e[Aldeano] ¡Oh, vas a fundir!",
                            "§e[Aldeano] ¿¡Planeas fabricar aquí!?",
                            "§7[Aldeano] Eso será útil.",
                            "§e[Aldeano] ¿¡Construyendo tu propio taller!?",
                            "§e[Aldeano] ¿¡Vas a hacer algo!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Qué estás construyendo!?",
                            "§c[Aldeano] ¡No necesitamos TUS hornos!",
                            "§c[Aldeano] ¡Deja de saturar nuestra aldea!",
                            "§7[Aldeano] *frunce el ceño profundamente*",
                            "§c[Aldeano] ¡No eres herrero aquí!",
                            "§c[Aldeano] ¡Lleva eso a otro lugar!"
                        };
                    }
                } else if (placedBlock instanceof CraftingTableBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¿¡Una mesa de crafteo! ¡Perfecto!",
                            "§a[Aldeano] ¡Oh, preparando un taller! ¡Buena idea!",
                            "§a[Aldeano] ¡Eres un verdadero artesano!",
                            "§7[Aldeano] Apreciamos a los artesanos hábiles.",
                            "§a[Aldeano] ¡La aldea se beneficia de tus habilidades!",
                            "§a[Aldeano] ¡Te estás convirtiendo en todo un artesano!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Una mesa de crafteo! Muy útil.",
                            "§e[Aldeano] ¡Oh, preparando un taller!",
                            "§e[Aldeano] Realmente te estás poniendo cómodo.",
                            "§7[Aldeano] Pensamiento inteligente.",
                            "§e[Aldeano] ¿¡Planeas hacer algo!?",
                            "§e[Aldeano] ¿¡Vas a fabricar aquí!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Tenemos nuestras PROPIAS mesas de crafteo!",
                            "§c[Aldeano] ¡No eres parte de esta aldea!",
                            "§c[Aldeano] ¡Deja de construir en nuestro territorio!",
                            "§7[Aldeano] *cruza los brazos*",
                            "§c[Aldeano] ¡No eres bienvenido a fabricar aquí!",
                            "§c[Aldeano] ¡Lleva esa mesa a otro lugar!"
                        };
                    }
                } else if (placedBlock instanceof BellBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¡Una campana! ¡Eso ayudará a coordinar a todos!",
                            "§a[Aldeano] ¡Excelente! ¡Ahora podemos señalarnos entre nosotros!",
                            "§a[Aldeano] ¡Estás pensando como un verdadero aldeano!",
                            "§7[Aldeano] *impresionado* Eso es muy considerado.",
                            "§a[Aldeano] ¡Una campana para la comunidad! ¡Gracias!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Una campana! Interesante...",
                            "§7[Aldeano] Eso es... inusual.",
                            "§e[Aldeano] ¿¡Planeas llamar reuniones!?",
                            "§e[Aldeano] ¡Una campana! Eso podría ser útil.",
                            "§7[Aldeano] *mira con curiosidad*"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡No te ATREVAS a tocarla!",
                            "§c[Aldeano] ¡Ya TENEMOS una campana!",
                            "§c[Aldeano] ¡Tú no estás a cargo aquí!",
                            "§7[Aldeano] *mira con furia*",
                            "§c[Aldeano] ¡Ese es NUESTRO símbolo de reunión!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¡Un soporte de pociones! ¡Eres alquimista!",
                            "§a[Aldeano] ¡Oh! ¿¡Sabes hacer pociones!",
                            "§a[Aldeano] ¡Podríamos usar alguien con tus habilidades!",
                            "§7[Aldeano] *muy impresionado*",
                            "§a[Aldeano] ¡Un alquimista en nuestra aldea! ¡Maravilloso!",
                            "§a[Aldeano] ¡Tus pociones beneficiarán a todos!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿¡Un soporte de pociones! ¡Elegante!",
                            "§e[Aldeano] ¡Oh! ¿¡Vas a hacer pociones!",
                            "§e[Aldeano] ¿¡Conoces alquimia!?",
                            "§7[Aldeano] *impresionado*",
                            "§e[Aldeano] ¡Eso es bastante avanzado!",
                            "§e[Aldeano] ¿¡Un alquimista, eh!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Qué pociones estás preparando!",
                            "§c[Aldeano] ¡No confiamos en tu alquimia!",
                            "§c[Aldeano] ¡Probablemente estás haciendo VENENO!",
                            "§7[Aldeano] *retrocede nerviosamente*",
                            "§c[Aldeano] ¡Mantén tus pociones sospechosas lejos de nosotros!",
                            "§c[Aldeano] ¡Estás tramando algo malo!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.EnchantmentTableBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§d[Aldeano] ¡Una mesa de encantamientos! ¡Increíble!",
                            "§d[Aldeano] ¡Practicas magia! ¡Asombroso!",
                            "§a[Aldeano] ¡Esto hará la aldea mucho más fuerte!",
                            "§7[Aldeano] *mira con asombro las runas mágicas*",
                            "§d[Aldeano] ¡Un verdadero mago entre nosotros! ¡Es un honor!",
                            "§a[Aldeano] ¡Tu experiencia mágica nos protegerá a todos!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§d[Aldeano] ¡Una mesa de encantamientos! ¡Wow!",
                            "§e[Aldeano] ¿¡Conoces magia!?",
                            "§7[Aldeano] *mira el libro flotante con asombro*",
                            "§e[Aldeano] ¡Eso es... eso es magia real!",
                            "§d[Aldeano] ¡Nunca había visto una de cerca!",
                            "§e[Aldeano] ¿¡Eres un mago!?"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Magia oscura! ¡Sabía que eras problemático!",
                            "§c[Aldeano] ¡No queremos brujería en nuestra aldea!",
                            "§c[Aldeano] Esa mesa me da mala espina...",
                            "§7[Aldeano] *hace un gesto de protección*",
                            "§c[Aldeano] ¡Estás maldiciendo nuestra tierra!",
                            "§c[Aldeano] ¡Lleva tus artes oscuras a otro lugar!"
                        };
                    }
                } else if (placedBlock == Blocks.BOOKSHELF) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§6[Aldeano] ¡Libros! ¡Estás construyendo una biblioteca!",
                            "§a[Aldeano] ¡El conocimiento es precioso! ¡Gracias!",
                            "§7[Aldeano] A los niños les encantará tener libros para leer.",
                            "§6[Aldeano] ¡Un erudito! Tenemos suerte de tenerte.",
                            "§a[Aldeano] ¡Estás enriqueciendo nuestra cultura!",
                            "§6[Aldeano] ¡Una librería! Realmente eres civilizado."
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Una librería! ¿¡Te gusta leer!?",
                            "§7[Aldeano] Los libros son valiosos por aquí.",
                            "§e[Aldeano] ¿¡Construyendo una biblioteca personal!?",
                            "§6[Aldeano] *examina los libros con curiosidad*",
                            "§e[Aldeano] ¿¡Coleccionas conocimiento!?",
                            "§7[Aldeano] Eso es bastante erudito."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Robando nuestras tradiciones de conocimiento!",
                            "§c[Aldeano] ¡Esos libros deberían estar en NUESTRA biblioteca!",
                            "§c[Aldeano] ¡No mereces nuestra sabiduría!",
                            "§7[Aldeano] *frunce el ceño ante la librería*",
                            "§c[Aldeano] Intentas parecer inteligente, ¿¡verdad!",
                            "§c[Aldeano] ¡Sabemos que ni siquiera puedes leer!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.LecternBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§6[Aldeano] ¡Un atril! ¡Para leer y estudiar!",
                            "§a[Aldeano] ¡Estás preparando un estudio apropiado!",
                            "§7[Aldeano] La marca de un verdadero erudito.",
                            "§6[Aldeano] ¿¡Compartirás tu conocimiento con nosotros!",
                            "§a[Aldeano] ¡Un atril muestra dedicación al aprendizaje!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Un atril! ¿¡Planeas leer!",
                            "§7[Aldeano] Eso es para exhibir libros, ¿¡verdad!",
                            "§e[Aldeano] ¿¡Preparando un área de estudio!",
                            "§6[Aldeano] *asiente con aprobación*",
                            "§e[Aldeano] Valoras el conocimiento, veo."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Quién te crees que eres, un maestro!",
                            "§c[Aldeano] ¡No necesitamos TUS lecciones!",
                            "§c[Aldeano] Fingiendo estar educado...",
                            "§7[Aldeano] *se burla*",
                            "§c[Aldeano] ¡No puedes enseñarnos nada!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.AnvilBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§8[Aldeano] ¡Un yunque! ¡Una forja apropiada!",
                            "§a[Aldeano] ¡Eres herrero! ¡Excelente!",
                            "§7[Aldeano] Siempre necesitamos trabajadores del metal hábiles.",
                            "§8[Aldeano] *asiente con respeto*",
                            "§a[Aldeano] ¡Tu trabajo de herrería servirá bien a la aldea!",
                            "§8[Aldeano] ¡Un maestro artesano entre nosotros!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Un yunque! ¿¡Eres herrero!",
                            "§7[Aldeano] Ese es equipo pesado.",
                            "§e[Aldeano] ¿¡Preparando una forja!",
                            "§8[Aldeano] ¿¡Trabajas el metal!",
                            "§e[Aldeano] ¿¡Planeas reparar herramientas!",
                            "§7[Aldeano] Eso será útil."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Haciendo armas contra nosotros!",
                            "§c[Aldeano] ¡No confiamos en ti con un yunque!",
                            "§c[Aldeano] ¡Forjarás armas para atacarnos!",
                            "§7[Aldeano] *mira el yunque sospechosamente*",
                            "§c[Aldeano] ¡Lleva tu forja a otro lugar!",
                            "§c[Aldeano] ¡No queremos armas en NUESTRA aldea!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.GrindstoneBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§7[Aldeano] ¡Una piedra de afilar! ¡Muy práctico!",
                            "§a[Aldeano] ¡Piensas en todo!",
                            "§7[Aldeano] Todos podemos usar eso para reparaciones.",
                            "§a[Aldeano] ¡Las herramientas comunitarias siempre son bienvenidas!",
                            "§7[Aldeano] *aprecia la utilidad*"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Una piedra de afilar! Útil.",
                            "§7[Aldeano] ¿¡Para afilar y reparar!",
                            "§e[Aldeano] Eso es práctico.",
                            "§7[Aldeano] Bueno para mantener herramientas.",
                            "§e[Aldeano] ¿¡Preparando una estación de reparación!"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Afilando armas, apuesto!",
                            "§c[Aldeano] ¡Sabemos lo que estás planeando!",
                            "§c[Aldeano] ¡Eso es para hacer espadas más afiladas!",
                            "§7[Aldeano] *observa con cautela*",
                            "§c[Aldeano] Preparándote para la violencia..."
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.LoomBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§d[Aldeano] ¡Un telar! ¡Eres tejedor!",
                            "§a[Aldeano] ¡La aldea necesita más artesanos!",
                            "§7[Aldeano] ¡Hermosos tapices nos esperan!",
                            "§d[Aldeano] ¡Harás estandartes maravillosos!",
                            "§a[Aldeano] ¡Un verdadero artista entre nosotros!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Un telar! ¿¡Haces estandartes!",
                            "§7[Aldeano] ¿¡Para tejer patrones!",
                            "§e[Aldeano] Eso es creativo.",
                            "§d[Aldeano] ¿¡Eres artístico!",
                            "§e[Aldeano] ¿¡Planeas hacer decoraciones!"
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Haciendo TUS estandartes en NUESTRA aldea!",
                            "§c[Aldeano] ¡No queremos tus símbolos aquí!",
                            "§c[Aldeano] ¡Intentas marcar esto como TU territorio!",
                            "§7[Aldeano] *desaprueba fuertemente*",
                            "§c[Aldeano] ¡Quita ese telar!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.ComposterBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§2[Aldeano] ¡Una compostera! ¡Estás cultivando!",
                            "§a[Aldeano] ¡Excelente! ¡Necesitamos más granjeros!",
                            "§7[Aldeano] ¡Ayudarás a crecer nuestros cultivos!",
                            "§2[Aldeano] ¡Contribuyendo a nuestra agricultura!",
                            "§a[Aldeano] ¡Los campos prosperarán!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Una compostera! ¿¡Planeas cultivar!",
                            "§7[Aldeano] ¿¡Para hacer harina de huesos!",
                            "§2[Aldeano] ¿¡Estás cultivando!",
                            "§e[Aldeano] Eso es útil para cultivar.",
                            "§7[Aldeano] Bueno para los jardines."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Robando nuestros métodos de cultivo!",
                            "§c[Aldeano] ¡Eso es para NUESTROS cultivos, no los tuyos!",
                            "§c[Aldeano] ¡Arruinarás el suelo!",
                            "§7[Aldeano] *protector de las tierras de cultivo*",
                            "§c[Aldeano] ¡Deja nuestra agricultura en paz!"
                        };
                    }
                } else if (placedBlock instanceof net.minecraft.world.level.block.CauldronBlock) {
                    if (isWelcome) {
                        messages = new String[]{
                            "§b[Aldeano] ¡Un caldero! ¡Muy útil!",
                            "§a[Aldeano] ¡Todos podemos usar eso!",
                            "§7[Aldeano] Para almacenar agua y lavar.",
                            "§b[Aldeano] ¡Pensamiento práctico!",
                            "§a[Aldeano] ¡La aldea se beneficia de esto!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¡Un caldero! ¿¡Para agua!",
                            "§7[Aldeano] Eso es práctico tener a mano.",
                            "§b[Aldeano] Herramienta multifunción.",
                            "§e[Aldeano] ¿¡Para pociones o lavar!",
                            "§7[Aldeano] Práctico."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¿¡Qué estás preparando ahí!",
                            "§c[Aldeano] ¡Probablemente estás haciendo veneno!",
                            "§c[Aldeano] ¡Bruja! ¡Bruja!",
                            "§7[Aldeano] *retrocede del caldero*",
                            "§c[Aldeano] ¡No queremos magia oscura aquí!"
                        };
                    }
                } else {
                    // CUALQUIER otro bloque: mensaje genérico según reputación
                    if (isWelcome) {
                        messages = new String[]{
                            "§a[Aldeano] ¿Construyendo algo? ¡Bien!",
                            "§a[Aldeano] ¡Siéntete libre de expandir la aldea!",
                            "§7[Aldeano] *asiente con aprobación*",
                            "§a[Aldeano] ¡Haciendo mejoras!",
                            "§a[Aldeano] ¡Ya eres parte de la aldea!"
                        };
                    } else if (isNeutral) {
                        messages = new String[]{
                            "§e[Aldeano] ¿Construyendo aquí?",
                            "§e[Aldeano] ¿Qué estás haciendo?",
                            "§7[Aldeano] *observa con curiosidad*",
                            "§e[Aldeano] Añadiendo a la aldea...",
                            "§e[Aldeano] Hmm, interesante..."
                        };
                    } else {
                        messages = new String[]{
                            "§c[Aldeano] ¡Oye! ¿¡Qué haces!?",
                            "§c[Aldeano] ¡No eres bienvenido a construir aquí!",
                            "§c[Aldeano] ¡Deja de poner bloques en NUESTRA aldea!",
                            "§7[Aldeano] *mira con furia*",
                            "§c[Aldeano] ¡Vete con tus construcciones!",
                            "§c[Aldeano] ¡Esta NO es TU tierra!"
                        };
                    }
                }
                
                // SIEMPRE mostrar mensaje si hay aldeanos mirando
                if (messages != null) {
                    player.sendSystemMessage(Component.literal(
                        messages[level.getRandom().nextInt(messages.length)]));
                }
                
                // PENALIZACIÓN solo si tienes mala reputación
                if (isUnwelcome) {
                    int oldRep = data.getReputation(player.getUUID(), nearestVillage.get());
                    data.addReputation(player.getUUID(), nearestVillage.get(), -5);
                    int newRep = data.getReputation(player.getUUID(), nearestVillage.get());
                    checkAndNotifyReputationChange(player, oldRep, newRep);
                    
                    player.sendSystemMessage(Component.literal(
                        "§c[Diplomacia de Aldeas] ¡Construyendo en la aldea con mala reputación! -5 (Total: " +
                                newRep + " - " + getReputationStatus(newRep) + ")"));
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
            String[] messages;
            
            if (reputation >= 100) {
                // FRIENDLY - menos agresivos
                messages = new String[]{
                    "§e[Aldeano] ¡Oye, cuidado con eso!",
                    "§e[Aldeano] Eso es parte de nuestra aldea...",
                    "§7[Aldeano] *parece preocupado*",
                    "§e[Aldeano] ¿Seguro que necesitas romper eso?"
                };
            } else if (reputation >= 0) {
                // NEUTRAL
                messages = new String[]{
                    "§e[Aldeano] ¿Qué estás haciendo?",
                    "§e[Aldeano] ¡Eso es propiedad de la aldea!",
                    "§7[Aldeano] *frunce el ceño*",
                    "§e[Aldeano] ¡Oye! ¡Nosotros construimos eso!"
                };
            } else {
                // UNFRIENDLY/ENEMY - muy agresivos
                messages = new String[]{
                    "§c[Aldeano] ¡DETENTE! ¡Eso es NUESTRO!",
                    "§c[Aldeano] ¡Estás destruyendo nuestro hogar!",
                    "§c[Aldeano] ¡VÁNDALO! ¡LADRÓN!",
                    "§7[Aldeano] *grita furioso*",
                    "§c[Aldeano] ¡Cómo TE ATREVES!",
                    "§c[Aldeano] ¡Guardias! ¡Detengan a este criminal!"
                };
            }
            
            player.sendSystemMessage(Component.literal(
                messages[level.getRandom().nextInt(messages.length)]));
            player.sendSystemMessage(Component.literal(
                "§c[Diplomacia de Aldeas] ¡Rompiste estructura de la aldea! Reputación -10 (Total: " +
                        newRep + " - " + getReputationStatus(newRep) + ")"));
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
                
                String[] denialMessages = reputation < -500 ? new String[] {
                        "§4[Aldeano] ¡FUERA! ¡No eres bienvenido aquí!",
                        "§4[Aldeano] ¿¡Un criminal en NUESTRAS camas!? ¡NUNCA!",
                        "§4[Aldeano] ¡Guardias! ¡Remuevan a este intruso!",
                        "§4[Aldeano] ¡NO tienes derecho a descansar aquí!"
                } : new String[] {
                        "§c[Aldeano] No eres bienvenido a usar nuestras camas.",
                        "§c[Aldeano] Busca otro lugar para dormir, forastero.",
                        "§c[Aldeano] Estas camas son solo para aldeanos.",
                        "§c[Aldeano] Tu reputación no te otorga este privilegio."
                };
                
                player.sendSystemMessage(Component.literal(
                        denialMessages[level.getRandom().nextInt(denialMessages.length)]));
                player.sendSystemMessage(Component.literal(
                        "§c✗ ¡Denegado! Tu reputación es muy baja para usar camas de aldea."));
                
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
                        "§c[Aldeano] ¡OYE! ¡Esa es MI cama!",
                        "§c[Aldeano] ¡Sal de mi cama, raro!",
                        "§c[Aldeano] ¿¡No tienes vergüenza!?",
                        "§c[Aldeano] ¡Eso es propiedad privada!",
                        "§c[Aldeano] *enojado* ¡FUERA!"
                };

                String[] babyMessages = {
                        "§c[Aldeano Bebé] ¡Ahí es donde duermo! *llora*",
                        "§c[Aldeano Bebé] ¡Noooo! ¡Mi cama!",
                        "§c[Aldeano Bebé] ¡Mami! ¡Un extraño está en mi cama!"
                };

                String message = caughtByBaby ? babyMessages[level.getRandom().nextInt(babyMessages.length)]
                        : adultMessages[level.getRandom().nextInt(adultMessages.length)];

                player.sendSystemMessage(Component.literal(message));
                player.sendSystemMessage(Component.literal(
                        "§c[Diplomacia de Aldeas] ¡Usaste la cama de un aldeano! Reputación -20 (Total: " +
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
                            "§4[Aldeano] ¡NO tienes derecho a tocar nuestra campana!",
                            "§4[Aldeano] ¡Aléjate de ahí, criminal!",
                            "§c[Aldeano] ¡Esa campana no es para gente como tú!"
                        };
                    } else {
                        denialMessages = new String[]{
                            "§c[Aldeano] No podemos confiar en ti con la campana de la aldea.",
                            "§c[Aldeano] Ganáte nuestra confianza primero.",
                            "§e[Aldeano] La campana es solo para aldeanos."
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
                                "§a[Aldeano] ¡Reuniendo a todos, campeón!",
                                "§a[Aldeano] ¡Llamando a la aldea por ti!",
                                "§a[Aldeano] *asiente con aprobación*"
                        };
                        player.sendSystemMessage(Component.literal(
                                positiveMessages[level.getRandom().nextInt(positiveMessages.length)]));
                    } else if (reputation < 100) {
                        data.addReputation(playerId, -15);
                        int newRep = data.getReputation(playerId);

                        String[] messages = {
                                "§c[Aldeano] ¡Deja de tocar la campana!",
                                "§c[Aldeano] ¡Eso es solo para emergencias!",
                                "§c[Aldeano] ¿¡Estás intentando causar pánico!?",
                                "§c[Aldeano] ¡Esto no es gracioso!",
                                "§c[Aldeano] *se cubre los oídos* ¡DETENTE!",
                                "§c[Aldeano] ¡Estás abusando de nuestro sistema de emergencias!",
                                "§c[Aldeano] ¡Los guardias se§ enterarán de esto!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Diplomacia de Aldeas] ¡Tocaste la campana de la aldea! Reputación -15 (Total: " +
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
                                "§c[Aldeano] ¡OYE! ¡No dejes salir a los animales!",
                                "§c[Aldeano] ¡Esa es nuestra granja! ¡Aléjate!",
                                "§c[Aldeano] ¿¡Qué estás haciendo!?",
                                "§c[Aldeano] ¡Deja nuestros cultivos en paz!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Diplomacia de Aldeas] ¡Abriste trampilla de granja! Reputación -10 (Total: " +
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
                            "§c[Aldeano] ¡Oye! ¡Esa es nuestra " + blockName + "!",
                            "§c[Aldeano] ¡No toques nuestro equipo!",
                            "§c[Aldeano] ¡Estás usando NUESTROS recursos!",
                            "§c[Aldeano] ¡Eso es propiedad de la aldea!",
                            "§c[Aldeano] ¡Deja de usar nuestras herramientas!"
                    };

                    player.sendSystemMessage(Component.literal(
                            messages[level.getRandom().nextInt(messages.length)]));
                    player.sendSystemMessage(Component.literal(
                            "§c[Diplomacia de Aldeas] ¡Usaste " + blockName + " de la aldea! Reputación -8 (Total: " +
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
                                "§c[Aldeano] ¡Esa es MI mesa de crafteo!",
                                "§c[Aldeano] ¡Haz tus propias herramientas!",
                                "§c[Aldeano] ¡Oye! ¡No uses mis cosas!"
                        };

                        player.sendSystemMessage(Component.literal(
                                messages[level.getRandom().nextInt(messages.length)]));
                        player.sendSystemMessage(Component.literal(
                                "§c[Diplomacia de Aldeas] ¡Usaste mesa de crafteo del aldeano! Reputación -8 (Total: " +
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
                                "§c[Aldeano] ¡OYE! ¡Estás dejando salir a los animales!",
                                "§c[Aldeano] ¡NO! ¡Cierra esa verja!",
                                "§c[Aldeano] ¡El ganado escapará!",
                                "§c[Aldeano] ¿¡Qué estás haciendo!? ¡Esos son NUESTROS animales!",
                                "§c[Aldeano] ¡DETENTE! ¡Tomó una eternidad meterlos ahí!",
                                "§c[Aldeano] ¡Estás liberando nuestro sustento!",
                                "§c[Aldeano] ¡Esos animales alimentan a toda la aldea!",
                                "§c[Aldeano] ¿¡Estás intentando sabotearnos!?",
                                "§c[Aldeano] ¡Cierra esa verja AHORA!",
                                "§c[Aldeano] ¡Están escapando! ¡Alguien ayude!"
                        };

                        String[] babyMessages = {
                                "§c[Aldeano Bebé] ¡Noooo! ¡Los animales se están saliendo!",
                                "§c[Aldeano Bebé] *llora* ¡Deténlos!",
                                "§c[Aldeano Bebé] ¡Mi vaca mascota! ¡Se está escapando!",
                                "§c[Aldeano Bebé] ¿¡Por qué harías eso!?",
                                "§c[Aldeano Bebé] ¡Papi se va a enojar mucho!",
                                "§c[Aldeano Bebé] ¡Malo! ¡Mala persona!",
                                "§c[Aldeano Bebé] *solloza* ¡Están escapando!"
                        };

                        String message = caughtByBaby ? babyMessages[level.getRandom().nextInt(babyMessages.length)]
                                : adultMessages[level.getRandom().nextInt(adultMessages.length)];

                        player.sendSystemMessage(Component.literal(message));
                        player.sendSystemMessage(Component.literal(
                                "§c[Diplomacia de Aldeas] ¡Liberaste animales de la aldea! Reputación -12 (Total: " +
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
                    "§4[Diplomacia de Aldeas] ¡Los Golems de Hierro ahora son HOSTILES por 30 segundos!"));

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
                    message = "§a[Aldeano] ¡Para nuestro héroe!";
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
            case 8 -> "§6✦✦✦ [Aldeano] *se arrodilla* ¡Una LEYENDA camina entre nosotros! ¡Todos saluden!§6✦✦✦";
            case 7 -> "§6✦ [Aldeano] *se inclina respetuosamente* ¡Nuestro héroe! ¡Bienvenido de vuelta, campeón!";
            case 6 -> "§a[Aldeano] *vitorea* ¡El campeón de la aldea regresa!";
            case 5 -> "§a[Aldeano] *sonríe cálidamente* ¡Bienvenido de vuelta, amigo!";
            case 4 -> "§2[Aldeano] Es bueno verte de nuevo.";
            case 3 -> "§7[Aldeano] *asiente*";
            case 2 -> "§e[Aldeano] *mira con cautela* ...";
            case 1 -> "§c[Aldeano] *frunce el ceño* Mantén tu distancia...";
            case 0 -> "§4[Aldeano] *mira hacia otro lado con miedo* ¡Aléjate de nosotros!";
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
            player.sendSystemMessage(Component.literal("§e§l         ¡REPUTACIÓN CAMBIADA!"));
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("  §7" + oldStatus + " §8" + arrow + " " + color + "§l" + newStatus));
            player.sendSystemMessage(Component.literal(""));
            
            if (isPositive) {
                if (newRep >= 1000) {
                    player.sendSystemMessage(Component.literal("  §6§l" + emoji + " ¡Los aldeanos se inclinan ante tu presencia!"));
                } else if (newRep >= 800) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " ¡Eres un héroe para esta aldea!"));
                } else if (newRep >= 500) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " ¡Los aldeanos te celebran!"));
                } else if (newRep >= 300) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " ¡La aldea confía en ti completamente!"));
                } else if (newRep >= 100) {
                    player.sendSystemMessage(Component.literal("  §a§l" + emoji + " ¡Los aldeanos te saludan cálidamente!"));
                } else if (newRep >= 0) {
                    player.sendSystemMessage(Component.literal("  §e§l" + emoji + " Las relaciones están mejorando..."));
                }
            } else {
                if (newRep < -899) {
                    player.sendSystemMessage(Component.literal("  §4§l" + emoji + " ¡Los Golems atacan a la vista!"));
                } else if (newRep < -699) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " ¡Eres un enemigo de la aldea!"));
                } else if (newRep < -499) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " ¡Los aldeanos se niegan a comerciar contigo!"));
                } else if (newRep < -200) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " ¡No eres bienvenido aquí!"));
                } else if (newRep < -100) {
                    player.sendSystemMessage(Component.literal("  §c§l" + emoji + " ¡Los aldeanos te desagradan!"));
                } else if (newRep < 0) {
                    player.sendSystemMessage(Component.literal("  §e§l" + emoji + " Las relaciones se están deteriorando..."));
                }
            }
            
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendSystemMessage(Component.literal(""));
        }
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
                        "§4[Aldeano] ¡LA CAMPANA! ¡Nuestro sistema de emergencia!",
                        "§4[Aldeano] ¡NO! ¡Esa era nuestra campana de alerta!",
                        "§4[Aldeano] ¡Destruiste nuestra campana!",
                        "§4[Aldeano] *horrorizado* ¡La campana es nuestra línea de vida!",
                        "§4[Aldeano] ¿¡Cómo pediremos ayuda ahora!",
                        "§4[Aldeano] ¡Esa campana ha salvado vidas!",
                        "§4[Aldeano] ¡Guardias! ¡La campana está destruida!",
                        "§4[Aldeano] ¡Esto es imperdonable!",
                        "§c[Aldeano] *en pánico* ¡Nuestro sistema de alerta!",
                        "§c[Aldeano] ¡Los saqueadores atacarán y no lo sabremos!"
                },
                new String[] {
                        "§c[Aldeano Bebé] *llora en voz alta* ¡La campana! ¡Está rota!",
                        "§c[Aldeano Bebé] ¿¡Por qué la rompiste!",
                        "§c[Aldeano Bebé] ¡Mamá! ¡La campana!",
                        "§c[Aldeano Bebé] *sollozando* ¡Eso era importante!"
                }),
        BED(-20, "Broke a villager's bed!",
                new String[] {
                        "§c[Aldeano] ¡Esa es MI cama!",
                        "§c[Aldeano] ¿¡Dónde se supone que duerma ahora!",
                        "§c[Aldeano] ¡Monstruo!",
                        "§c[Aldeano] *furioso* ¡Me tomó semanas hacerla!",
                        "§c[Aldeano] ¡ACABO de hacer esa cama!",
                        "§c[Aldeano] ¡No tienes respeto por los demás!",
                        "§c[Aldeano] ¡Esa es mi única cama!",
                        "§c[Aldeano] ¡Trabajo todo el día y rompes mi cama!",
                        "§c[Aldeano] *indignado* ¡Dormiré en el suelo por tu culpa!",
                        "§c[Aldeano] ¿¡Romper la cama de alguien! ¿¡En serio!"
                },
                new String[] {
                        "§c[Aldeano Bebé] ¡Mi cama! *solloza incontrolablemente*",
                        "§c[Aldeano Bebé] ¡Necesito eso para dormir!",
                        "§c[Aldeano Bebé] *llorando* ¿¡Dónde dormiré!",
                        "§c[Aldeano Bebé] ¡Papá! ¡Mi cama está rota!",
                        "§c[Aldeano Bebé] *lamentando* ¿¡Por qué!"
                }),
        CROP(-15, "Destroyed village crops!",
                new String[] {
                        "§c[Aldeano] ¡Nuestra comida! ¡Estás destruyendo nuestros cultivos!",
                        "§c[Aldeano] ¡Los necesitamos para sobrevivir!",
                        "§c[Aldeano] ¡DEJA de pisotear nuestra granja!",
                        "§c[Aldeano] ¡Eso tomó MESES en crecer!",
                        "§c[Aldeano] ¡Pasaremos hambre por tu culpa!",
                        "§c[Aldeano] *desesperado* ¡Esa es nuestra reserva de invierno!",
                        "§c[Aldeano] ¿¡Sabes lo difícil que es cultivar!",
                        "§c[Aldeano] ¡Esos cultivos alimentan a toda la aldea!",
                        "§c[Aldeano] *enojado* ¡Aléjate de nuestros campos!",
                        "§c[Aldeano] ¡Estás destruyendo nuestro sustento!",
                        "§c[Aldeano] ¡Toda la aldea depende de estos cultivos!",
                        "§c[Aldeano] ¿¡No tienes vergüenza!"
                },
                new String[] {
                        "§c[Aldeano Bebé] ¡La comida! *llora*",
                        "§c[Aldeano Bebé] ¡Mamá dijo que no toqué los cultivos!",
                        "§c[Aldeano Bebé] *señala* ¡Malo! ¡Malo!",
                        "§c[Aldeano Bebé] ¡Esos iban a ser pan!"
                }),
        WORKSTATION(-25, "Broke a workstation!",
                new String[] {
                        "§4[Aldeano] ¡Ese es mi sustento!",
                        "§4[Aldeano] ¡Necesito eso para trabajar!",
                        "§4[Aldeano] ¿¡Cómo te atreves!",
                        "§4[Aldeano] *conmocionado* ¡Mi mesa de trabajo!",
                        "§4[Aldeano] ¡La he tenido por AÑOS!",
                        "§4[Aldeano] ¡Así es como me gano la vida!",
                        "§4[Aldeano] ¡Acabas de destruir mi trabajo!",
                        "§4[Aldeano] *furioso* ¿¡Cómo se supone que trabaje ahora!",
                        "§4[Aldeano] ¡Esa estación era esencial para la aldea!",
                        "§c[Aldeano] ¡Sin eso no puedo ganar esmeraldas!",
                        "§c[Aldeano] ¿¡Sabes lo caras que son esas cosas!",
                        "§c[Aldeano] ¡Mi profesión entera depende de eso!"
                },
                new String[] {
                        "§c[Aldeano Bebé] ¡La estación de trabajo de papá!",
                        "§c[Aldeano Bebé] *jadea* ¡La rompiste!"
                }),
        DECORATION(-5, "Broke village decoration!",
                new String[] {
                        "§c[Aldeano] ¡Oye! ¡Eso hacía que la aldea se viera bonita!",
                        "§c[Aldeano] ¿¡Por qué harías eso!",
                        "§c[Aldeano] ¡Trabajamos duro para decorar!",
                        "§6[Aldeano] *suspira* Eso era bonito...",
                        "§6[Aldeano] ¿¡No podemos tener cosas lindas!",
                        "§c[Aldeano] ¡Muestra algo de respeto por nuestra aldea!",
                        "§6[Aldeano] Acabo de colocar eso ayer...",
                        "§c[Aldeano] ¿¡Ahora rompes nuestras decoraciones!"
                },
                new String[] {}),
        WELL(-30, "Damaged the village well!",
                new String[] {
                        "§4[Aldeano] ¡EL POZO! ¡Nuestra fuente de agua!",
                        "§4[Aldeano] ¡Ese es nuestro único suministro de agua!",
                        "§4[Aldeano] *horrorizado* ¡El pozo está destruido!",
                        "§4[Aldeano] ¡Moriremos de sed!",
                        "§c[Aldeano] ¿¡Cómo obtendremos agua ahora!",
                        "§c[Aldeano] ¡Ese pozo nos ha servido por generaciones!",
                        "§c[Aldeano] ¡Nos has condenado a todos!",
                        "§c[Aldeano] *en pánico* ¡Nuestra agua! ¡Nuestra preciosa agua!",
                        "§4[Aldeano] ¡Esto es una catástrofe!"
                },
                new String[] {
                        "§c[Aldeano Bebé] *llorando* ¿¡Dónde obtenemos agua!",
                        "§c[Aldeano Bebé] ¡Tengo sed! ¡El pozo!",
                        "§c[Aldeano Bebé] ¡Mamá! ¡El lugar del agua está roto!"
                }),
        HOUSE(-15, "Damaged a house!",
                new String[] {
                        "§c[Aldeano] ¡Estás destruyendo mi hogar!",
                        "§c[Aldeano] ¡DETENTE! ¡Aquí es donde vivo!",
                        "§c[Aldeano] ¡Mi casa! ¡La estás destrozando!",
                        "§c[Aldeano] *desesperado* ¡No tengo otro lugar a donde ir!",
                        "§c[Aldeano] ¡Esa es MI CASA que estás rompiendo!",
                        "§c[Aldeano] ¡Construí esto con mis propias manos!",
                        "§c[Aldeano] *furioso* ¡Aléjate de mi hogar!",
                        "§c[Aldeano] ¡Esta casa me protege de los monstruos!",
                        "§c[Aldeano] ¡Me estás dejando sin hogar!",
                        "§c[Aldeano] ¿¡No tienes decencia!"
                },
                new String[] {
                        "§c[Aldeano Bebé] ¡Nuestra casa! *llora*",
                        "§c[Aldeano Bebé] *sollozando* ¿¡Dónde viviremos!",
                        "§c[Aldeano Bebé] ¡No rompas nuestro hogar!",
                        "§c[Aldeano Bebé] ¡Papá! ¡Nuestra casa!"
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
                                "§4[Aldeano] ¡CORRAN! ¡El criminal está aquí!",
                                "§4[Aldeano] ¡AYUDA! ¡Alguien ayúdenos!",
                                "§4[Aldeano] ¡Aléjense de nosotros!",
                                "§4[Aldeano] ¡Guardias! ¡GUARDIAS!",
                                "§4[Aldeano Bebé] *gritando* ¡DA MIEDO!"
                        }
                                : reputation <= -500 ? new String[] {
                                        "§c[Aldeano] ¡Aléjate de mí!",
                                        "§c[Aldeano] ¡No quiero problemas!",
                                        "§c[Aldeano] ¡Déjanos en paz!",
                                        "§c[Aldeano Bebé] ¡Mamá! ¡Tengo miedo!"
                                }
                                        : new String[] {
                                                "§6[Aldeano] Por favor, mantente atrás...",
                                                "§6[Aldeano] Preferiría que mantuvieras tu distancia.",
                                                "§6[Aldeano] No me siento cómodo contigo cerca."
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
                            "§4[Guardia de la Aldea] ¡Eres un criminal buscado! ¡Ríndete ahora!",
                            "§c[Guardia de la Aldea] *pisa agresivamente* ¡SAL DE ESTA ALDEA!",
                            "§4[Guardia de la Aldea] ¡Tus crímenes no quedarán impunes!",
                            "§c[Guardia de la Aldea] *levanta el puño* ¡Se hará justicia!",
                            "§4[Diplomacia de Aldeas] ¡Estás BUSCADO! ¡Los guardias atacan a la vista!"
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
                "§a[Diplomacia de Aldeas] ¡Curaste a un aldeano zombie! +100 Reputación (Total: " +
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