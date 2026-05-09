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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;

public class DiplomacyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diplomacy")
                .then(Commands.literal("name")
                        .then(Commands.argument("villageName", StringArgumentType.greedyString())
                                .executes(context -> nameCurrentVillage(context, StringArgumentType.getString(context, "villageName")))))
                .then(Commands.literal("info")
                        .executes(DiplomacyCommands::showInfo)));

        dispatcher.register(Commands.literal("diplomacy")
                .requires(source -> source.hasPermission(2))
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
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-500, 500))
                                                .executes(context -> addReputation(context, EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("villages")
                        .then(Commands.literal("list")
                                .executes(DiplomacyCommands::listVillages))
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
                .then(Commands.literal("test")
                        .then(Commands.literal("caravan")
                                .executes(DiplomacyCommands::testCaravan))
                        .then(Commands.literal("raid")
                                .executes(DiplomacyCommands::testRaid))));
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

    private static int listVillages(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        Map<String, BlockPos> villages = data.getAllVillages();

        if (villages.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.list_empty"), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.list_header"), false);
        for (Map.Entry<String, BlockPos> entry : villages.entrySet()) {
            BlockPos pos = entry.getValue();
            String villageId = entry.getKey();
            String villageStored = data.getVillageName(villageId);
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.list_line",
                    VillageDisplayName.asComponent(villageStored),
                    villageId,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()), false);
        }

        return villages.size();
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

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
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

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_village",
                VillageDisplayName.asComponent(villageStored)));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.info_reputation",
                reputation,
                ModLang.repStatus(reputation)));
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

    private static int testCaravan(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_disabled"));
        return 0;
    }

    private static int testRaid(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_disabled"));
        return 0;
    }
}
