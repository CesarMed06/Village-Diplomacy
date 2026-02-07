package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TradeModifierHandler {

    private final Map<UUID, Long> tradeMessageCooldown = new HashMap<>();
    private static final long TRADE_MESSAGE_COOLDOWN_MS = 30000; // 30 segundos

    @SubscribeEvent
    public void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return; // Evitar duplicación de eventos

        if (villager.isBaby()) {
            return;
        }
        
        // SHIFT + CLICK DERECHO = Mostrar personalidad detallada
        if (player.isShiftKeyDown()) {
            showVillagerPersonality(villager, player, level);
            event.setCanceled(true); // No abrir GUI de comercio
            return;
        }

        BlockPos villagerPos = villager.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty()) {
            return;
        }

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), villagePos);

        if (reputation < -500) {
            event.setCanceled(true);

            String[] rejectMessages = reputation < -800 ? new String[] {
                "§4[Aldeano] ¡UN CRIMINAL! ¡Guardias, ayuda!",
                "§4[Aldeano] ¡Aléjate de mí, criminal!",
                "§4[Aldeano] ¡No ayudaré a alguien como tú!"
            } : new String[] {
                "§c[Aldeano] ¡No quiero comerciar contigo!",
                "§c[Aldeano] ¡Déjame en paz!",
                "§c[Aldeano] Tu reputación te precede..."
            };

            player.sendSystemMessage(Component.literal(
                    rejectMessages[(int)(Math.random() * rejectMessages.length)]));
            
            String statusMsg = reputation < -800 ?
                    "§4[Diplomacia de Aldeas] ¡Los aldeanos se niegan a comerciar con criminales BUSCADOS!" :
                    "§c[Diplomacia de Aldeas] ¡Este aldeano se niega a comerciar debido a tu reputación de ENEMIGO!";
            
            player.sendSystemMessage(Component.literal(statusMsg));
        } else {
            int priceModifier = calculatePriceModifier(reputation);

            if (priceModifier != 0) {
                modifyVillagerOffers(villager, priceModifier);
                
                // Cooldown para no saturar el chat con mensajes
                UUID playerId = player.getUUID();
                long currentTime = System.currentTimeMillis();
                
                if (!tradeMessageCooldown.containsKey(playerId) ||
                        currentTime - tradeMessageCooldown.get(playerId) > TRADE_MESSAGE_COOLDOWN_MS) {
                    
                    // SOLO mostrar mensaje si hay descuento significativo (no neutral)
                    if (priceModifier < 0) {
                        player.sendSystemMessage(Component.literal(
                                "§a[Diplomacia de Aldeas] ¡Tu reputación otorga un " + 
                                Math.abs(priceModifier * 10) + "% de descuento en comercio!"));
                        tradeMessageCooldown.put(playerId, currentTime);
                    } else if (priceModifier > 0) {
                        player.sendSystemMessage(Component.literal(
                                "§6[Diplomacia de Aldeas] Tu reputación añade un " + 
                                (priceModifier * 10) + "% de sobreprecio en comercio."));
                        tradeMessageCooldown.put(playerId, currentTime);
                    }
                }
            }
            // Si priceModifier == 0 (NEUTRAL), NO mostrar nada
        }
    }

    private int calculatePriceModifier(int reputation) {
        if (reputation >= 1000) return -5;
        if (reputation >= 800) return -4;
        if (reputation >= 500) return -3;
        if (reputation >= 300) return -2;
        if (reputation >= 100) return -1;
        if (reputation >= -99) return 0;
        if (reputation >= -299) return 1;
        if (reputation >= -500) return 3;
        return 5;
    }

    private void modifyVillagerOffers(Villager villager, int priceModifier) {
        if (villager.getOffers().isEmpty()) return;

        for (MerchantOffer offer : villager.getOffers()) {
            ItemStack costA = offer.getBaseCostA();
            int newCount = Math.max(1, costA.getCount() + priceModifier);
            costA.setCount(newCount);
        }
    }
    
    /**
     * Mostrar personalidad completa con Shift+Click
     */
    private void showVillagerPersonality(Villager villager, ServerPlayer player, ServerLevel level) {
        com.cesoti2006.villagediplomacy.data.VillagerPersonalityData personalityData = 
            com.cesoti2006.villagediplomacy.data.VillagerPersonalityData.get(level);
        com.cesoti2006.villagediplomacy.personality.VillagerPersonality personality = 
            personalityData.getPersonality(villager.getUUID());
        
        if (personality == null) {
            player.sendSystemMessage(Component.literal("§c[Sistema] El aldeano aún no tiene datos de personalidad."));
            return;
        }
        
        // Get correct professional level
        int profLevel = villager.getVillagerData().getLevel();
        String profLevelName = switch(profLevel) {
            case 1 -> "§7Novato";
            case 2 -> "§fAprendiz";
            case 3 -> "§eOficial";
            case 4 -> "§6Experto";
            case 5 -> "§6✦ Maestro";
            default -> "§7Unknown";
        };
        
        String profession = villager.getVillagerData().getProfession().toString();
        boolean hasJob = !villager.getVillagerData().getProfession().equals(VillagerProfession.NONE);
        
        // HEADER
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendSystemMessage(Component.literal("§6【 " + personality.getCustomName() + " 】"));
        
        // Profession and level
        if (hasJob) {
            player.sendSystemMessage(Component.literal(
                "§7" + profession + " §8| " + profLevelName
            ));
        } else {
            player.sendSystemMessage(Component.literal("§7Desempleado"));
        }
        
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        
        // PERSONALITY (Only show relevant traits)
        player.sendSystemMessage(Component.literal("§e● §7Valentía: " + getTraitColor(personality.getCourage()) + getTraitName(personality.getCourage())));
        player.sendSystemMessage(Component.literal("§e● §7Generosidad: " + getTraitColor(personality.getGenerosity()) + getTraitName(personality.getGenerosity())));
        
        // Only show work ethic if has job
        if (hasJob) {
            player.sendSystemMessage(Component.literal("§e● §7Ética de Trabajo: " + getTraitColor(personality.getWorkEthic()) + getTraitName(personality.getWorkEthic())));
        }
        
        player.sendSystemMessage(Component.literal("§e● §7Social: " + getTraitColor(personality.getSocialBehavior()) + getTraitName(personality.getSocialBehavior())));
        player.sendSystemMessage(Component.literal("§e● §7Temperamento: " + getTraitColor(personality.getTemperament()) + getTraitName(personality.getTemperament())));
        player.sendSystemMessage(Component.literal("§e● §7Honestidad: " + getTraitColor(personality.getHonesty()) + getTraitName(personality.getHonesty())));
        player.sendSystemMessage(Component.literal("§e● §7Perspectiva: " + getTraitColor(personality.getOutlook()) + getTraitName(personality.getOutlook())));
        
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }
    
    private String getTraitColor(PersonalityTrait trait) {
        return switch(trait.name()) {
            case "COWARD", "CAUTIOUS", "GREEDY", "THRIFTY", "LAZY", "SHY", "HOTHEADED", "IMPULSIVE", "CUNNING", "PESSIMISTIC" -> "§c";
            case "FEARLESS", "BRAVE", "CHARITABLE", "GENEROUS", "WORKAHOLIC", "DILIGENT", "EXTROVERTED", "CALM", "TRUSTWORTHY", "HONEST", "CHEERFUL" -> "§a";
            default -> "§7";
        };
    }
    
    private String getTraitName(PersonalityTrait trait) {
        return switch(trait.name()) {
            case "COWARD" -> "Cobarde";
            case "CAUTIOUS" -> "Cauteloso";
            case "NEUTRAL_COURAGE" -> "Normal";
            case "BRAVE" -> "Valiente";
            case "FEARLESS" -> "Intrépido";
            case "GREEDY" -> "Avaro";
            case "THRIFTY" -> "Ahorrativo";
            case "NEUTRAL_GENEROSITY" -> "Normal";
            case "GENEROUS" -> "Generoso";
            case "CHARITABLE" -> "Caritativo";
            case "LAZY" -> "Perezoso";
            case "RELAXED" -> "Relajado";
            case "NEUTRAL_WORK" -> "Normal";
            case "NEUTRAL_WORK_ETHIC" -> "Normal";
            case "HARDWORKING" -> "Trabajador";
            case "DILIGENT" -> "Diligente";
            case "WORKAHOLIC" -> "Adicto al trabajo";
            case "SHY" -> "Tímido";
            case "RESERVED" -> "Reservado";
            case "NEUTRAL_SOCIAL" -> "Normal";
            case "OUTGOING" -> "Sociable";
            case "SOCIABLE" -> "Sociable";
            case "EXTROVERTED" -> "Extrovertido";
            case "CALM" -> "Calmado";
            case "PATIENT" -> "Paciente";
            case "NEUTRAL" -> "Normal";
            case "IRRITABLE" -> "Irritable";
            case "IMPULSIVE" -> "Impulsivo";
            case "HOTHEADED" -> "Irascible";
            case "CUNNING" -> "Astuto";
            case "SHREWD" -> "Sagaz";
            case "NEUTRAL_HONESTY" -> "Normal";
            case "HONEST" -> "Honesto";
            case "TRUSTWORTHY" -> "Confiable";
            case "PESSIMISTIC" -> "Pesimista";
            case "REALISTIC" -> "Realista";
            case "NEUTRAL_OUTLOOK" -> "Normal";
            case "OPTIMISTIC" -> "Optimista";
            case "CHEERFUL" -> "Alegre";
            default -> trait.name();
        };
    }
}
