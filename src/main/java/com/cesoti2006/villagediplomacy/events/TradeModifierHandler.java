package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TradeModifierHandler {

    private final Map<UUID, Long> tradeMessageCooldown = new HashMap<>();
    private static final long TRADE_MESSAGE_COOLDOWN_MS = 30000;

    @SubscribeEvent
    public void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        if (villager.isBaby()) {
            return;
        }

        if (player.isShiftKeyDown()) {
            showVillagerPersonality(villager, player, level);
            event.setCanceled(true);
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

            if (reputation < -800) {
                ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.trade.reject.criminal", 3);
            } else {
                ModLang.sendRandom(player, level.getRandom(), "villagediplomacy.trade.reject.enemy", 3);
            }

            player.sendSystemMessage(Component.translatable(
                    reputation < -800 ? "villagediplomacy.trade.refuse_wanted" : "villagediplomacy.trade.refuse_enemy"));
        } else {
            int priceModifier = calculatePriceModifier(reputation);

            if (priceModifier != 0) {
                modifyVillagerOffers(villager, priceModifier);

                UUID playerId = player.getUUID();
                long currentTime = System.currentTimeMillis();

                if (!tradeMessageCooldown.containsKey(playerId)
                        || currentTime - tradeMessageCooldown.get(playerId) > TRADE_MESSAGE_COOLDOWN_MS) {

                    if (priceModifier < 0) {
                        player.sendSystemMessage(Component.translatable("villagediplomacy.trade.discount",
                                Math.abs(priceModifier * 10)));
                        tradeMessageCooldown.put(playerId, currentTime);
                    } else if (priceModifier > 0) {
                        player.sendSystemMessage(Component.translatable("villagediplomacy.trade.surcharge",
                                priceModifier * 10));
                        tradeMessageCooldown.put(playerId, currentTime);
                    }
                }
            }
        }
    }

    private int calculatePriceModifier(int reputation) {
        if (reputation >= 1000) {
            return -5;
        }
        if (reputation >= 800) {
            return -4;
        }
        if (reputation >= 500) {
            return -3;
        }
        if (reputation >= 300) {
            return -2;
        }
        if (reputation >= 100) {
            return -1;
        }
        if (reputation >= -99) {
            return 0;
        }
        if (reputation >= -299) {
            return 1;
        }
        if (reputation >= -500) {
            return 3;
        }
        return 5;
    }

    private void modifyVillagerOffers(Villager villager, int priceModifier) {
        if (villager.getOffers().isEmpty()) {
            return;
        }

        for (MerchantOffer offer : villager.getOffers()) {
            ItemStack costA = offer.getBaseCostA();
            int newCount = Math.max(1, costA.getCount() + priceModifier);
            costA.setCount(newCount);
        }
    }

    private void showVillagerPersonality(Villager villager, ServerPlayer player, ServerLevel level) {
        com.cesoti2006.villagediplomacy.data.VillagerPersonalityData personalityData =
                com.cesoti2006.villagediplomacy.data.VillagerPersonalityData.get(level);
        com.cesoti2006.villagediplomacy.personality.VillagerPersonality personality =
                personalityData.getPersonality(villager.getUUID());

        if (personality == null) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.trade.no_personality"));
            return;
        }

        int profLevel = villager.getVillagerData().getLevel();
        MutableComponent profLevelName = Component.translatable(switch (profLevel) {
            case 1 -> "villagediplomacy.personality.level.novice";
            case 2 -> "villagediplomacy.personality.level.apprentice";
            case 3 -> "villagediplomacy.personality.level.journeyman";
            case 4 -> "villagediplomacy.personality.level.expert";
            case 5 -> "villagediplomacy.personality.level.master";
            default -> "villagediplomacy.personality.level.unknown";
        });

        boolean hasJob = !villager.getVillagerData().getProfession().equals(VillagerProfession.NONE);

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.personality.title", personality.getCustomName()));

        if (hasJob) {
            VillagerProfession prof = villager.getVillagerData().getProfession();
            ResourceLocation profKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
            java.util.Map<String, String> profEs = java.util.Map.ofEntries(
                java.util.Map.entry("farmer", "Granjero"),
                java.util.Map.entry("librarian", "Bibliotecario"),
                java.util.Map.entry("cleric", "Clérigo"),
                java.util.Map.entry("armorer", "Armero"),
                java.util.Map.entry("weaponsmith", "Herrero"),
                java.util.Map.entry("toolsmith", "Forjador"),
                java.util.Map.entry("butcher", "Carnicero"),
                java.util.Map.entry("leatherworker", "Peletero"),
                java.util.Map.entry("shepherd", "Pastor"),
                java.util.Map.entry("fisherman", "Pescador"),
                java.util.Map.entry("fletcher", "Flechero"),
                java.util.Map.entry("cartographer", "Cartógrafo"),
                java.util.Map.entry("mason", "Cantero")
            );
            String profPath = profKey != null ? profKey.getPath() : prof.toString().toLowerCase();
            Component profName = Component.literal(profEs.getOrDefault(profPath, 
                Character.toUpperCase(profPath.charAt(0)) + profPath.substring(1)));
            player.sendSystemMessage(Component.translatable("villagediplomacy.personality.job_line",
                    profName, profLevelName));
        } else {
            player.sendSystemMessage(Component.translatable("villagediplomacy.personality.unemployed"));
        }

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));

        sendTraitRow(player, "villagediplomacy.personality.label.courage", personality.getCourage());
        sendTraitRow(player, "villagediplomacy.personality.label.generosity", personality.getGenerosity());
        if (hasJob) {
            sendTraitRow(player, "villagediplomacy.personality.label.work_ethic", personality.getWorkEthic());
        }
        sendTraitRow(player, "villagediplomacy.personality.label.social", personality.getSocialBehavior());
        sendTraitRow(player, "villagediplomacy.personality.label.temperament", personality.getTemperament());
        sendTraitRow(player, "villagediplomacy.personality.label.honesty", personality.getHonesty());
        sendTraitRow(player, "villagediplomacy.personality.label.outlook", personality.getOutlook());

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
    }

    private static void sendTraitRow(ServerPlayer player, String labelKey, PersonalityTrait trait) {
        MutableComponent name = Component.translatable(
                "villagediplomacy.trait." + trait.name().toLowerCase(Locale.ROOT));
        name.withStyle(traitStyle(trait));
        player.sendSystemMessage(Component.translatable("villagediplomacy.personality.trait_row",
                Component.translatable(labelKey), name));
    }

    private static ChatFormatting traitStyle(PersonalityTrait trait) {
        return switch (trait.name()) {
            case "COWARD", "CAUTIOUS", "GREEDY", "THRIFTY", "LAZY", "SHY", "HOTHEADED", "CUNNING", "PESSIMISTIC" -> ChatFormatting.RED;
            case "FEARLESS", "BRAVE", "CHARITABLE", "GENEROUS", "WORKAHOLIC", "HARDWORKING", "EXTROVERTED", "CALM", "TRUSTWORTHY", "HONEST", "CHEERFUL" -> ChatFormatting.GREEN;
            default -> ChatFormatting.GRAY;
        };
    }
}
