package com.cesoti2006.villagediplomacy.commands;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData;
import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData.ClaimedVillage;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler.ReputationTier;

public class DiplomacyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diplomacy")
                .then(Commands.literal("name")
                        .then(Commands.argument("villageName", StringArgumentType.greedyString())
                                .executes(context -> nameCurrentVillage(context, StringArgumentType.getString(context, "villageName")))))
                .then(Commands.literal("info")
                        .executes(DiplomacyCommands::showInfo))
                .then(Commands.literal("reputation")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> getReputation(context, EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(context -> setReputation(context, EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                                .executes(context -> addReputation(context, EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("setrep")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                        .executes(context -> setReputation(context, EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("getrep")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return getReputation(context, player);
                        }))
                .then(Commands.literal("villages")
                        .then(Commands.literal("list")
                        .executes(DiplomacyCommands::listPlayerReputations))
                        .then(Commands.literal("relations")
                                .then(Commands.argument("villageId", StringArgumentType.string())
                                        .executes(context -> showRelations(context, StringArgumentType.getString(context, "villageId")))))
                        .then(Commands.literal("setrelation")
                                .then(Commands.argument("village1", StringArgumentType.string())
                                        .then(Commands.argument("village2", StringArgumentType.string())
                                                .then(Commands.argument("points", IntegerArgumentType.integer(-100, 100))
                                                        .executes(context -> setRelation(context,
                                                                StringArgumentType.getString(context, "village1"),
                                                                StringArgumentType.getString(context, "village2"),
                                                                IntegerArgumentType.getInteger(context, "points")))))))
                        .then(Commands.literal("rename")
                                .then(Commands.argument("villageId", StringArgumentType.string())
                                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                                .executes(context -> renameVillage(context,
                                                        StringArgumentType.getString(context, "villageId"),
                                                        StringArgumentType.getString(context, "newName")))))))
                .then(Commands.literal("claim")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> claimVillage(context, StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("unclaim")
                        .executes(DiplomacyCommands::unclaimVillage))
                .then(Commands.literal("test")
                        .then(Commands.literal("caravan")
                                .executes(DiplomacyCommands::testCaravan))
                        .then(Commands.literal("raid")
                                .executes(DiplomacyCommands::testRaid)))
                .then(Commands.literal("list")
                        .executes(DiplomacyCommands::listPlayerReputations))
                .then(Commands.literal("gift")
                        .executes(DiplomacyCommands::giftToVillage)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                .executes(context -> giftToVillage(context, IntegerArgumentType.getInteger(context, "amount"))))));
    }

    private static int getReputation(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        VillageReputationData data = VillageReputationData.get(level);

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.error.no_village_near"));
            return 0;
        }

        BlockPos villagePos = nearestVillage.get();
        int reputation = data.getReputation(player.getUUID(), villagePos);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageStored = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.rep_get",
                player.getName(),
                VillageDisplayName.asComponent(villageStored),
                reputation,
                ModLang.repStatus(reputation)), false);
        return reputation;
    }

    private static int setReputation(CommandContext<CommandSourceStack> context, ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();
        VillageReputationData data = VillageReputationData.get(level);

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.error.no_village_near"));
            return 0;
        }

        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.setReputation(player.getUUID(), villagePos, amount);
        int newRep = data.getReputation(player.getUUID(), villagePos);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageStored = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.rep_set",
                player.getName(),
                VillageDisplayName.asComponent(villageStored),
                oldRep,
                newRep,
                ModLang.repStatus(newRep)), false);

        return 1;
    }

    private static int addReputation(CommandContext<CommandSourceStack> context, ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();
        VillageReputationData data = VillageReputationData.get(level);

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);

        if (nearestVillage.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.error.no_village_near"));
            return 0;
        }

        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, amount);
        int newRep = data.getReputation(player.getUUID(), villagePos);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageStored = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.rep_add",
                player.getName(),
                VillageDisplayName.asComponent(villageStored),
                oldRep,
                newRep,
                ModLang.repStatus(newRep)), false);

        return 1;
    }

    private static int listPlayerReputations(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        VillageReputationData repData = VillageReputationData.get(level);
        Map<String, Integer> allReps = repData.getPlayerReputations(player.getUUID());

        if (allReps.isEmpty()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_empty"));
            return 0;
        }

        VillageRelationshipData relData = VillageRelationshipData.get(level);

        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_header"));

        allReps.entrySet().stream()
                .filter(e -> e.getValue() != 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    String villageKey = entry.getKey();
                    int rep = entry.getValue();
                    ReputationTier tier = ReputationTiersHandler.getTierByReputation(rep);

                    String villageName = relData.getVillageName(villageKey);
                    if (villageName.equals(villageKey)) {
                        try {
                            String[] parts = villageKey.split("_");
                            int snapX = Integer.parseInt(parts[0]);
                            int snapZ = Integer.parseInt(parts[1]);
                            String foundName = null;
                            for (Map.Entry<String, BlockPos> vEntry : relData.getAllVillages().entrySet()) {
                                BlockPos vPos = vEntry.getValue();
                                int vSnapX = (vPos.getX() >> 7) << 7;
                                int vSnapZ = (vPos.getZ() >> 7) << 7;
                                if (vSnapX == snapX && vSnapZ == snapZ) {
                                    foundName = relData.getVillageName(vEntry.getKey());
                                    break;
                                }
                            }
                            if (foundName == null) {
                                BlockPos approxPos = new BlockPos(snapX + 64, 80, snapZ + 64);
                                relData.registerVillage(approxPos, level);
                                foundName = relData.getVillageName(relData.getVillageId(approxPos));
                            }
                            villageName = foundName;
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}
                    }

                    MutableComponent tierName = Component.translatable(tier.getTranslationKey());
                    int discount = tier.getDiscountPercent();
                    String discountStr = discount > 0 ? " §a(-" + discount + "%)" : (discount < 0 ? " §c(+" + Math.abs(discount) + "%)" : "");
                    player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_line",
                            VillageDisplayName.asComponent(villageName),
                            rep,
                            tierName,
                            Component.literal(discountStr)));
                });

        return allReps.size();
    }

    private static int showRelations(CommandContext<CommandSourceStack> context, String villageId) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        if (data.getVillagePosition(villageId) == null) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found", villageId));
            return 0;
        }

        String villageStored = data.getVillageName(villageId);
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.relations_header",
                VillageDisplayName.asComponent(villageStored)), false);

        Map<String, BlockPos> allVillages = data.getAllVillages();
        for (String otherVillage : allVillages.keySet()) {
            if (!otherVillage.equals(villageId)) {
                int points = data.getRelationship(villageId, otherVillage);
                VillageRelationshipData.RelationshipStatus status = data.getStatus(villageId, otherVillage);
                String otherStored = data.getVillageName(otherVillage);
                context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.relation_line",
                        VillageDisplayName.asComponent(otherStored),
                        Component.translatable(status.getTranslationKey()),
                        points), false);
            }
        }

        return 1;
    }

    private static int setRelation(CommandContext<CommandSourceStack> context, String village1, String village2, int points) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        data.setRelationship(village1, village2, points);
        VillageRelationshipData.RelationshipStatus status = data.getStatus(village1, village2);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.set_relation_done",
                village1,
                village2,
                Component.translatable(status.getTranslationKey()),
                points), false);

        return 1;
    }

    private static int renameVillage(CommandContext<CommandSourceStack> context, String villageId, String newName) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        if (data.getVillagePosition(villageId) == null) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found", villageId));
            return 0;
        }

        String oldStored = data.getVillageName(villageId);
        data.setVillageName(villageId, newName);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.rename_done",
                VillageDisplayName.asComponent(oldStored),
                VillageDisplayName.asComponent(newName)), false);

        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, playerPos, 100);
        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_not_in_village"));
            return 0;
        }

        VillageReputationData reputationData = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        int reputation = reputationData.getReputation(player.getUUID(), villagePos);

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, context.getSource().getLevel());
        String villageId = relationData.getVillageId(villagePos);
        String villageStored = relationData.getVillageName(villageId);

        int distance = (int) Math.sqrt(playerPos.distSqr(villagePos));
        ReputationTier tier = ReputationTiersHandler.getTierByReputation(reputation);
        MutableComponent tierName = Component.translatable(tier.getTranslationKey());
        int discount = tier.getDiscountPercent();
        String tradeInfo = discount > 0 ? " (-" + discount + "%)" : "";

        int villagerCount = level.getEntitiesOfClass(
            net.minecraft.world.entity.npc.Villager.class,
            new net.minecraft.world.phys.AABB(
                villagePos.getX() - 48, playerPos.getY() - 16,
                villagePos.getZ() - 48,
                villagePos.getX() + 48, playerPos.getY() + 16,
                villagePos.getZ() + 48
            )
        ).size();

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_village",
                VillageDisplayName.asComponent(villageStored)));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_reputation",
                reputation,
                ModLang.repStatus(reputation)));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_tier", tierName));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_trades",
                tradeInfo.isEmpty() ? "§7100%" : "§a" + (100 - discount) + "%" + tradeInfo));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_distance", distance));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_villagers", villagerCount));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_pos",
                villagePos.getX(), villagePos.getY(), villagePos.getZ()));
        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));

        return 1;
    }

    private static int nameCurrentVillage(CommandContext<CommandSourceStack> context, String newName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);

        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.name_need_village"));
            return 0;
        }

        BlockPos villagePos = nearestVillage.get();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, context.getSource().getLevel());
        String villageId = relationData.getVillageId(villagePos);

        relationData.setVillageName(villageId, newName);

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.name_done",
                VillageDisplayName.asComponent(newName)));
        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));

        return 1;
    }

    private static int claimVillage(CommandContext<CommandSourceStack> context, String name) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        Optional<BlockPos> nearVanilla = VillageDetector.findNearestVillage(level, playerPos, 
            com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.customVillageRadius.get() + 16);
        if (nearVanilla.isPresent()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_too_close"));
            return 0;
        }

        PlayerClaimedVillageData data = PlayerClaimedVillageData.get(level);

        int minDist = com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.customVillageRadius.get() * 2;
        if (data.isTooCloseToAny(playerPos, minDist)) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_overlap"));
            return 0;
        }

        int radius = com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.customVillageRadius.get();
        UUID owner = player.getUUID();
        boolean success = data.addVillage(playerPos, name, owner, radius);

        if (!success) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_exists"));
            return 0;
        }

        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(playerPos, level);
        String relationId = relationData.getVillageId(playerPos);
        relationData.setVillageName(relationId, name);

        VillageReputationData repData = VillageReputationData.get(level);
        repData.setReputation(player.getUUID(), playerPos, 0);

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_done", name,
                playerPos.getX(), playerPos.getY(), playerPos.getZ(), radius));
        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        return 1;
    }

    private static int unclaimVillage(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        PlayerClaimedVillageData data = PlayerClaimedVillageData.get(level);

        boolean removed = data.removeVillage(playerPos, player.getUUID());

        if (!removed) {

            int radius = com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.customVillageRadius.get();
            Optional<BlockPos> nearestCustom = data.getNearestVillage(playerPos, radius);
            if (nearestCustom.isPresent()) {
                ClaimedVillage v = data.getVillageAt(nearestCustom.get());
                if (v != null && v.owner.equals(player.getUUID())) {
                    data.removeVillage(nearestCustom.get(), player.getUUID());
                    removed = true;
                }
            }
        }

        if (!removed) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.unclaim_fail"));
            return 0;
        }

        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.unclaim_done"));
        return 1;
    }

    private static int testCaravan(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_disabled"));
        return 0;
    }

    private static int testRaid(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_disabled"));
        return 0;
    }

    private static int giftToVillage(CommandContext<CommandSourceStack> context) {
        return giftToVillage(context, 0);
    }

    private static int giftToVillage(CommandContext<CommandSourceStack> context, int amount) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.gift_empty_hand"));
            return 0;
        }

        int maxGive = held.getCount();
        if (amount <= 0 || amount > maxGive) amount = maxGive;

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.gift_no_village"));
            return 0;
        }

        BlockPos villagePos = nearestVillage.get();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos, level);
        String villageId = relationData.getVillageId(villagePos);
        String villageStored = relationData.getVillageName(villageId);

        int giftValue = 5;
        String itemName = held.getHoverName().getString();
        if (itemName.contains("emerald")) giftValue = 15;
        else if (itemName.contains("diamond")) giftValue = 12;
        else if (itemName.contains("golden_apple") || itemName.contains("enchanted_golden_apple")) giftValue = 20;
        else if (itemName.contains("iron_ingot") || itemName.contains("gold_ingot")) giftValue = 8;
        else if (itemName.contains("bread") || itemName.contains("carrot") || itemName.contains("potato")) giftValue = 6;
        else if (itemName.contains("book")) giftValue = 8;
        else if (itemName.contains("coal")) giftValue = 5;

        int totalGift = giftValue * amount;

        VillageReputationData repData = VillageReputationData.get(level);
        repData.addReputation(player.getUUID(), villagePos, totalGift);
        int newRep = repData.getReputation(player.getUUID(), villagePos);

        held.shrink(amount);

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.gift_done",
                amount, itemName, VillageDisplayName.asComponent(villageStored)));
        ModLang.sendReputationSummary(player, totalGift, newRep);
        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));

        return 1;
    }
}
