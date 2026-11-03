package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.integration.guardvillagers.GuardVillagersCompat;
import com.cesoti2006.villagediplomacy.integration.mca.MCARebornCompat;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TradeModifierHandler {

    private final Map<UUID, Long> tradeMessageCooldown = new HashMap<>();
    private static final long TRADE_MESSAGE_COOLDOWN_MS = 10000;
    private final Map<UUID, Long> tradePitchCooldown = new HashMap<>();
    private static final long TRADE_PITCH_COOLDOWN_MS = 20000;

    private static final class InteractMemo {
        private final int entityId;
        private final long at;
        private boolean answered;
        private boolean canceled;

        private InteractMemo(int entityId, long at) {
            this.entityId = entityId;
            this.at = at;
        }
    }

    private final Map<UUID, InteractMemo> lastClick = new HashMap<>();
    private static final long CLICK_REPEAT_WINDOW_MS = 150L;

    public boolean onVillagerInteract(Entity target, ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!(target instanceof Villager villager)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        InteractMemo click = click(player, villager);
        if (click.answered) {
            return click.canceled;
        }
        click.answered = true;

        if (player.isShiftKeyDown()) {
            showVillagerPersonality(villager, player, level);
            return cancelClick(click);
        }

        boolean mcaVillager = MCARebornCompat.isMCAVillager(villager);

        BlockPos villagerPos = villager.blockPosition();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, villagerPos, 200);

        if (nearestVillage.isEmpty() && mcaVillager) {
            nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
        }

        if (nearestVillage.isEmpty()) {
            return false;
        }

        BlockPos villagePos = nearestVillage.get();
        VillageReputationData data = VillageReputationData.get(level);
        int reputation = data.getReputation(player.getUUID(), villagePos);

        if (!ReputationTiersHandler.canTrade(reputation)) {
            cancelClick(click);

            if (reputation < -800) {
                ModLang.sendDialogRandom(player, level.getRandom(), villager,
                        "villagediplomacy.trade.reject.criminal", 3);
            } else {
                ModLang.sendDialogRandom(player, level.getRandom(), villager,
                        "villagediplomacy.trade.reject.enemy", 3);
            }

            String refuseKey;
            if (mcaVillager) {
                refuseKey = reputation < -800
                        ? "villagediplomacy.mca.refuse_wanted"
                        : "villagediplomacy.mca.refuse_enemy";
            } else {
                refuseKey = reputation < -800
                        ? "villagediplomacy.trade.refuse_wanted"
                        : "villagediplomacy.trade.refuse_enemy";
            }
            player.sendSystemMessage(Component.translatable(refuseKey));
        } else if (isGuard(villager) && reputation < 0) {

            cancelClick(click);
            ModLang.sendDialogRandom(player, level.getRandom(), villager,
                    "villagediplomacy.trade.reject.guard", 3);
            player.sendSystemMessage(Component.translatable("villagediplomacy.trade.refuse_guard"));
        } else if (mcaVillager) {

        } else {
            long pitchNow = System.currentTimeMillis();
            if (!tradePitchCooldown.containsKey(player.getUUID())
                    || pitchNow - tradePitchCooldown.get(player.getUUID()) > TRADE_PITCH_COOLDOWN_MS) {
                tradePitchCooldown.put(player.getUUID(), pitchNow);
                ModLang.sendTradePitch(player, level.getRandom(), villager);
            }

            float multiplier = ReputationTiersHandler.getTradeMultiplier(reputation);
            int priceModifier = calculatePriceModifier(reputation);

            if (priceModifier != 0) {
                modifyVillagerOffers(villager, priceModifier);

                UUID playerId = player.getUUID();
                long currentTime = System.currentTimeMillis();

                if (!tradeMessageCooldown.containsKey(playerId)
                        || currentTime - tradeMessageCooldown.get(playerId) > TRADE_MESSAGE_COOLDOWN_MS) {

                    if (multiplier < 1.0f) {
                        int discountPercent = Math.round((1.0f - multiplier) * 100);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.trade.discount",
                                discountPercent));
                        tradeMessageCooldown.put(playerId, currentTime);
                    } else if (multiplier > 1.0f) {
                        int surchargePercent = Math.round((multiplier - 1.0f) * 100);
                        player.sendSystemMessage(Component.translatable("villagediplomacy.trade.surcharge",
                                surchargePercent));
                        tradeMessageCooldown.put(playerId, currentTime);
                    }
                }
            }
        }
        return click.canceled;
    }

    private InteractMemo click(ServerPlayer player, Villager villager) {
        long now = System.currentTimeMillis();
        InteractMemo memo = lastClick.get(player.getUUID());
        if (memo == null || memo.entityId != villager.getId() || now - memo.at > CLICK_REPEAT_WINDOW_MS) {
            memo = new InteractMemo(villager.getId(), now);
            lastClick.put(player.getUUID(), memo);
        }
        return memo;
    }

    private static boolean cancelClick(InteractMemo click) {
        click.canceled = true;
        return true;
    }

    private static boolean isGuard(Villager villager) {
        if (MCARebornCompat.isLoaded() && MCARebornCompat.isMCAGuard(villager)) return true;
        return GuardVillagersCompat.isLoaded() && GuardVillagersCompat.isGuard(villager);
    }

    private int calculatePriceModifier(int reputation) {
        float multiplier = ReputationTiersHandler.getTradeMultiplier(reputation);

        if (multiplier < 1.0f) {

            return Math.round((multiplier - 1.0f) * 5);
        } else if (multiplier > 1.0f) {

            return Math.round((multiplier - 1.0f) * 5);
        }
        return 0;
    }

    private static final Map<MerchantOffer, Integer> VANILLA_BASE_COST = new java.util.WeakHashMap<>();

    private void modifyVillagerOffers(Villager villager, int priceModifier) {
        if (villager.getOffers().isEmpty()) {
            return;
        }

        for (MerchantOffer offer : villager.getOffers()) {
            Integer base = VANILLA_BASE_COST.get(offer);
            if (base == null) {
                base = offer.getBaseCostA().getCount();
                VANILLA_BASE_COST.put(offer, base);
            }

            ItemStack costA = offer.getBaseCostA();
            int newCount = Math.max(1, base + priceModifier);
            if (costA.getCount() != newCount) {
                costA.setCount(newCount);
            }
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
        player.sendSystemMessage(Component.translatable("villagediplomacy.personality.title",
                ModLang.nameComponent(personality.getCustomName())));

        if (hasJob) {
            VillagerProfession prof = villager.getVillagerData().getProfession();
            Component profName = ModLang.professionName(prof);
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
