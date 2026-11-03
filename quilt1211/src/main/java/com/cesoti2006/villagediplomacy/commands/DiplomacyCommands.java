package com.cesoti2006.villagediplomacy.commands;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.integration.mca.MCARebornCompat;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageDiplomacyData;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData;
import com.cesoti2006.villagediplomacy.data.VillageWarData;
import com.cesoti2006.villagediplomacy.diplomacy.DiplomaticState;
import com.cesoti2006.villagediplomacy.diplomacy.GrievanceHandler;
import com.cesoti2006.villagediplomacy.diplomacy.VillageLeader;
import com.cesoti2006.villagediplomacy.diplomacy.WarDeclarationHandler;
import com.cesoti2006.villagediplomacy.personality.VillagerActivityBehavior;
import com.cesoti2006.villagediplomacy.util.ModLang;
import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

                .executes(DiplomacyCommands::showCommandHelp)
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
                        .then(relationsCommand())
                        .then(setRelationCommand())
                        .then(stateCommand())
                        .then(setStateCommand())
                        .then(setTensionCommand())
                        .then(renameCommand())
                        .then(grievancesCommand())
                        .then(warReadinessCommand())
                        .then(declareCommand())
                        .then(ultimatumCommand())
                        .then(warStatusCommand())
                        .then(peaceCommand()))

                .then(relationsCommand())
                .then(setRelationCommand())
                .then(stateCommand())
                .then(setStateCommand())
                .then(setTensionCommand())
                .then(renameCommand())
                .then(grievancesCommand())
                .then(warReadinessCommand())
                .then(declareCommand())
                .then(ultimatumCommand())
                .then(warStatusCommand())
                .then(peaceCommand())
                .then(Commands.literal("claim")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> claimVillage(context, StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("unclaim")
                        .executes(DiplomacyCommands::unclaimVillage))
                .then(Commands.literal("test")
                        .then(Commands.literal("chest")
                                .executes(DiplomacyCommands::testChestDeposit))
                        .then(Commands.literal("caravan")
                                .executes(DiplomacyCommands::testCaravan))
                        .then(Commands.literal("raid")
                                .executes(DiplomacyCommands::testRaid))
                        .then(Commands.literal("mcaguard")
                                .executes(context -> testMcARole(context, MCARebornCompat.TestRole.GUARD)))
                        .then(Commands.literal("mcaarcher")
                                .executes(context -> testMcARole(context, MCARebornCompat.TestRole.ARCHER)))
                        .then(Commands.literal("mcacivil")
                                .executes(context -> testMcARole(context, MCARebornCompat.TestRole.CIVIL)))
                        .then(Commands.literal("grievance")
                                .then(Commands.literal("age")
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 365))
                                                .suggests(AGE_SUGGESTIONS)
                                                .executes(context -> ageGrievances(context,
                                                        IntegerArgumentType.getInteger(context, "days")))))
                                .then(Commands.literal("resolve")
                                        .executes(DiplomacyCommands::resolveGrievances))))
                .then(Commands.literal("list")
                        .executes(DiplomacyCommands::listPlayerReputations))
                .then(Commands.literal("gift")
                        .executes(DiplomacyCommands::giftToVillage)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                                .executes(context -> giftToVillage(context, IntegerArgumentType.getInteger(context, "amount")))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                .executes(context -> setVillagerLevel(context, IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static final SuggestionProvider<CommandSourceStack> VILLAGE_SUGGESTIONS = (context, builder) -> {
        VillageRelationshipData data = VillageRelationshipData.get(context.getSource().getLevel());

        String typed = ModLang.stripFormatting(builder.getRemaining()).replace("\"", "").trim()
                .toLowerCase(Locale.ROOT);

        for (String id : data.getAllVillages().keySet()) {
            String stored = data.getVillageName(id);

            String visible = VillageDisplayName.visibleName(stored);
            boolean idMatches = suggestionMatches(typed, id);
            boolean nameMatches = visible != null && !visible.equals(id) && suggestionMatches(typed, visible);
            if (!idMatches && !nameMatches) continue;

            if (idMatches) {

                builder.suggest(id, ModLang.villageName(stored));
            }
            if (nameMatches) {

                builder.suggest("\"" + visible + "\"",
                        Component.literal(id).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return builder.buildFuture();
    };

    private static boolean suggestionMatches(String typed, String suggestion) {
        return typed.isEmpty() || suggestion.toLowerCase(Locale.ROOT).startsWith(typed);
    }

    private static final String[][] HELP_ROWS = {
            {"/diplomacy list", "villagediplomacy.cmd.help.list"},
            {"/diplomacy state", "villagediplomacy.cmd.help.state"},
            {"/diplomacy relations", "villagediplomacy.cmd.help.relations"},
            {"/diplomacy grievances", "villagediplomacy.cmd.help.grievances"},
            {"/diplomacy warreadiness", "villagediplomacy.cmd.help.warreadiness"},
            {"/diplomacy declare", "villagediplomacy.cmd.help.declare"},
            {"/diplomacy ultimatum", "villagediplomacy.cmd.help.ultimatum"},
            {"/diplomacy war", "villagediplomacy.cmd.help.war"},
            {"/diplomacy peace", "villagediplomacy.cmd.help.peace"},
            {"/diplomacy settension", "villagediplomacy.cmd.help.settension"},
            {"/diplomacy setstate", "villagediplomacy.cmd.help.setstate"},
            {"/diplomacy setrelation", "villagediplomacy.cmd.help.setrelation"},
            {"/diplomacy rename", "villagediplomacy.cmd.help.rename"},
            {"/diplomacy getrep", "villagediplomacy.cmd.help.getrep"},
            {"/diplomacy test chest", "villagediplomacy.cmd.help.test_chest"},
    };

    private static int showCommandHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("===== ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("villagediplomacy.cmd.help_header")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" ✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("=====").withStyle(ChatFormatting.DARK_GRAY)), false);

        for (String[] row : HELP_ROWS) {

            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.help_line",
                    Component.literal(row[0]).withStyle(ChatFormatting.YELLOW),
                    Component.translatable(row[1]).withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.help_tab",
                Component.literal("TAB").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> POINTS_SUGGESTIONS = (context, builder) ->
            builder.suggest(-100).suggest(-50).suggest(0).suggest(50).suggest(100).buildFuture();

    private static final SuggestionProvider<CommandSourceStack> TENSION_SUGGESTIONS = (context, builder) ->
            builder.suggest(0).suggest(25).suggest(50).suggest(75).suggest(100).buildFuture();

    private static final SuggestionProvider<CommandSourceStack> AGE_SUGGESTIONS = (context, builder) ->
            builder.suggest(1).suggest(15).suggest(30).suggest(60).buildFuture();

    private static final SuggestionProvider<CommandSourceStack> STATE_SUGGESTIONS = (context, builder) -> {
        for (DiplomaticState state : DiplomaticState.values()) {
            builder.suggest(state.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    };

    private static LiteralArgumentBuilder<CommandSourceStack> relationsCommand() {
        return Commands.literal("relations")
                .executes(DiplomacyCommands::showRelationsNearest)
                .then(Commands.argument("village", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> showRelations(context, StringArgumentType.getString(context, "village"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setRelationCommand() {
        return Commands.literal("setrelation")
                .then(Commands.argument("points", IntegerArgumentType.integer(-100, 100))
                        .suggests(POINTS_SUGGESTIONS)
                        .executes(context -> setRelationNearest(context, IntegerArgumentType.getInteger(context, "points"))))
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .then(Commands.argument("points", IntegerArgumentType.integer(-100, 100))
                                        .suggests(POINTS_SUGGESTIONS)
                                        .executes(context -> setRelation(context,
                                                StringArgumentType.getString(context, "village1"),
                                                StringArgumentType.getString(context, "village2"),
                                                IntegerArgumentType.getInteger(context, "points"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stateCommand() {
        return Commands.literal("state")
                .executes(DiplomacyCommands::showDiplomaticStateNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> showDiplomaticStateWithNearestOther(context,
                                StringArgumentType.getString(context, "village1")))
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> showDiplomaticState(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setStateCommand() {
        return Commands.literal("setstate")
                .executes(DiplomacyCommands::showSetStateUsage)
                .then(Commands.argument("state", StringArgumentType.word())
                        .suggests(STATE_SUGGESTIONS)
                        .executes(context -> setDiplomaticStateNearest(context,
                                StringArgumentType.getString(context, "state"))))
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("state2", StringArgumentType.word())
                                .suggests(STATE_SUGGESTIONS)
                                .executes(context -> setDiplomaticStateWithNearestOther(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "state2"))))
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .suggests(STATE_SUGGESTIONS)
                                        .executes(context -> setDiplomaticState(context,
                                                StringArgumentType.getString(context, "village1"),
                                                StringArgumentType.getString(context, "village2"),
                                                StringArgumentType.getString(context, "state"))))))

                .then(Commands.argument("raw", StringArgumentType.greedyString())
                        .executes(DiplomacyCommands::showSetStateUsage));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setTensionCommand() {
        return Commands.literal("settension")
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                        .suggests(TENSION_SUGGESTIONS)
                        .executes(context -> setDiplomaticTensionNearest(context,
                                IntegerArgumentType.getInteger(context, "amount"))))
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                                        .suggests(TENSION_SUGGESTIONS)
                                        .executes(context -> setDiplomaticTension(context,
                                                StringArgumentType.getString(context, "village1"),
                                                StringArgumentType.getString(context, "village2"),
                                                IntegerArgumentType.getInteger(context, "amount"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> renameCommand() {
        return Commands.literal("rename")
                .then(Commands.argument("village", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                .executes(context -> renameVillage(context,
                                        StringArgumentType.getString(context, "village"),
                                        StringArgumentType.getString(context, "newName")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> grievancesCommand() {
        return Commands.literal("grievances")
                .executes(DiplomacyCommands::showGrievancesNearest)
                .then(Commands.argument("village", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> showGrievances(context,
                                StringArgumentType.getString(context, "village"))));
    }

    private static List<String> villagesByDistance(CommandContext<CommandSourceStack> context) {
        VillageRelationshipData data = VillageRelationshipData.get(context.getSource().getLevel());
        BlockPos from = BlockPos.containing(context.getSource().getPosition());
        return data.getAllVillages().entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparingDouble(entry -> entry.getValue().distSqr(from)))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static int showRelationsNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.list_empty"));
            return 0;
        }
        return showRelations(context, ids.get(0));
    }

    private static int setRelationNearest(CommandContext<CommandSourceStack> context, int points) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return setRelation(context, ids.get(0), ids.get(1), points);
    }

    private static int showDiplomaticStateNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return showDiplomaticState(context, ids.get(0), ids.get(1));
    }

    private static int showDiplomaticStateWithNearestOther(CommandContext<CommandSourceStack> context,
                                                          String villageArg) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        String named = resolveVillageId(data, villageArg);
        if (named == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }

        for (String id : villagesByDistance(context)) {
            if (!id.equals(named)) return showDiplomaticState(context, named, id);
        }

        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
        return 0;
    }

    private static int setDiplomaticStateWithNearestOther(CommandContext<CommandSourceStack> context,
                                                          String villageArg, String stateName) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        String named = resolveVillageId(data, villageArg);
        if (named == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }

        for (String id : villagesByDistance(context)) {
            if (!id.equals(named)) return setDiplomaticState(context, named, id, stateName);
        }

        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
        return 0;
    }

    private static int showSetStateUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendFailure(Component.translatable("villagediplomacy.cmd.setstate_usage"));
        source.sendFailure(Component.translatable("villagediplomacy.cmd.setstate_quotes"));

        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) return 0;

        VillageRelationshipData data = VillageRelationshipData.get(source.getLevel());
        String first = visibleNameOf(data, ids.get(0));
        String second = visibleNameOf(data, ids.get(1));
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.setstate_example",
                Component.literal("/diplomacy setstate \"" + first + "\" \"" + second + "\" war")
                        .withStyle(ChatFormatting.YELLOW)), false);
        return 0;
    }

    private static String visibleNameOf(VillageRelationshipData data, String villageId) {
        String visible = com.cesoti2006.villagediplomacy.util.VillageDisplayName
                .visibleName(data.getVillageName(villageId));
        return visible != null ? visible : villageId;
    }

    private static int setDiplomaticStateNearest(CommandContext<CommandSourceStack> context, String stateName) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return setDiplomaticState(context, ids.get(0), ids.get(1), stateName);
    }

    private static int setDiplomaticTensionNearest(CommandContext<CommandSourceStack> context, int amount) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return setDiplomaticTension(context, ids.get(0), ids.get(1), amount);
    }

    private static void sendVillageNotFound(CommandContext<CommandSourceStack> context, String input) {
        CommandSourceStack source = context.getSource();
        source.sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found", input));

        List<String> ids = villagesByDistance(context);
        if (ids.isEmpty()) {
            source.sendFailure(Component.translatable("villagediplomacy.cmd.list_empty"));
            return;
        }

        VillageRelationshipData data = VillageRelationshipData.get(source.getLevel());

        String closest = closestVillageName(data, input, ids);
        if (closest != null) {
            source.sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found_did_you_mean",
                    Component.literal(closest).withStyle(ChatFormatting.YELLOW)));
        }

        MutableComponent known = Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) known.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
            known.append(villageLabel(data, ids.get(i)));
        }
        source.sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found_known", known));

        if (ids.size() >= 2) {
            source.sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found_usage",
                    Component.literal("/diplomacy villages settension " + ids.get(0) + " " + ids.get(1) + " 40")
                            .withStyle(ChatFormatting.YELLOW)));
        }
    }

    private static MutableComponent villageLabel(VillageRelationshipData data, String villageId) {
        return Component.translatable("villagediplomacy.cmd.village_label",
                ModLang.villageName(data.getVillageName(villageId)),
                Component.literal(villageId).withStyle(ChatFormatting.GRAY));
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
        com.cesoti2006.villagediplomacy.events.VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

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
        com.cesoti2006.villagediplomacy.events.VillagerEventHandler.checkAndNotifyReputationChange(player, oldRep, newRep);

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

        List<Map.Entry<String, Integer>> rows = allReps.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();

        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_header"));

        for (Map.Entry<String, Integer> entry : rows) {
            String villageKey = entry.getKey();
            int rep = entry.getValue();
            ReputationTier tier = ReputationTiersHandler.getTierByReputation(rep);

            String villageName = relData.getVillageName(villageKey);
            if (villageName.equals(villageKey)) {
                villageName = recoverVillageName(level, relData, villageKey);
            }

            int discount = tier.getDiscountPercent();
            MutableComponent discountC = Component.literal(discount == 0 ? ""
                    : (discount > 0 ? " (-" + discount + "%)" : " (+" + Math.abs(discount) + "%)"))
                    .withStyle(discount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);

            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_line",
                    ModLang.villageName(villageName),
                    ModLang.reputationValue(rep),
                    ModLang.tierName(rep),
                    discountC));
        }

        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.replist_footer", rows.size()));
        return rows.size();
    }

    private static String recoverVillageName(ServerLevel level, VillageRelationshipData relData,
                                             String villageKey) {
        try {
            String[] parts = villageKey.split("_");
            int snapX = Integer.parseInt(parts[0]);
            int snapZ = Integer.parseInt(parts[1]);

            for (Map.Entry<String, BlockPos> vEntry : relData.getAllVillages().entrySet()) {
                BlockPos vPos = vEntry.getValue();
                if (((vPos.getX() >> 7) << 7) == snapX && ((vPos.getZ() >> 7) << 7) == snapZ) {
                    return relData.getVillageName(vEntry.getKey());
                }
            }

            BlockPos approxPos = new BlockPos(snapX + 64, 80, snapZ + 64);
            relData.registerVillage(approxPos, level);
            return relData.getVillageName(relData.getVillageId(approxPos));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return villageKey;
        }
    }

    private static int showRelations(CommandContext<CommandSourceStack> context, String villageIdArg) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        String villageId = resolveVillageId(data, villageIdArg);
        if (villageId == null) {
            sendVillageNotFound(context, villageIdArg);
            return 0;
        }

        List<String> others = data.getAllVillages().keySet().stream()
                .filter(other -> !other.equals(villageId))
                .sorted(Comparator.comparingInt((String other) -> data.getRelationship(villageId, other))
                        .reversed()
                        .thenComparing(other -> data.getVillageName(other)))
                .toList();

        context.getSource().sendSuccess(
                () -> Component.translatable("villagediplomacy.cmd.relations_header"), false);
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.relations_village",
                ModLang.villageName(data.getVillageName(villageId))), false);

        if (others.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("villagediplomacy.cmd.relations_empty"), false);
            return 1;
        }

        for (String otherVillage : others) {
            int points = data.getRelationship(villageId, otherVillage);
            VillageRelationshipData.RelationshipStatus status = data.getStatus(villageId, otherVillage);
            MutableComponent statusC = Component.translatable(status.getTranslationKey())
                    .withStyle(relationshipColor(status));
            MutableComponent pointsC = ModLang.reputationValue(points);
            String otherStored = data.getVillageName(otherVillage);
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.relation_line",
                    ModLang.villageName(otherStored),
                    statusC,
                    pointsC), false);
        }

        return 1;
    }

    private static int setRelation(CommandContext<CommandSourceStack> context, String village1, String village2, int points) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        String id1 = resolveVillageId(data, village1);
        String id2 = resolveVillageId(data, village2);
        if (id1 == null || id2 == null) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.village_not_found",
                    id1 == null ? village1 : village2));
            return 0;
        }

        data.setRelationship(id1, id2, points);
        VillageRelationshipData.RelationshipStatus status = data.getStatus(id1, id2);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.set_relation_done",
                ModLang.villageName(data.getVillageName(id1)),
                ModLang.villageName(data.getVillageName(id2)),
                Component.translatable(status.getTranslationKey()).withStyle(relationshipColor(status)),
                ModLang.reputationValue(points)), false);

        return 1;
    }

    private static int showDiplomaticState(CommandContext<CommandSourceStack> context,
                                           String village1Arg, String village2Arg) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relationData, village1Arg);
        String village2 = resolveVillageId(relationData, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        DiplomaticState state = diplomacy.getState(village1, village2);
        int tension = diplomacy.getTension(village1, village2);

        MutableComponent stateC = Component.translatable(state.getTranslationKey())
                .withStyle(stateColor(state));
        MutableComponent tensionC = Component.literal(String.valueOf(tension))
                .withStyle(tensionColor(tension));
        MutableComponent tensionBar = tensionBar(tension);

        context.getSource().sendSuccess(
                () -> Component.translatable("villagediplomacy.cmd.state_header"), false);
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_pair",
                ModLang.villageName(relationData.getVillageName(village1)),
                ModLang.villageName(relationData.getVillageName(village2))), false);
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_line",
                stateC), false);
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.tension_line",
                tensionC, tensionBar), false);
        if (state == DiplomaticState.TRUCE) {
            VillageDiplomacyData.PairState pair = diplomacy.peekPair(village1, village2);
            long remaining = pair != null ? Math.max(0L, pair.truceUntil - level.getGameTime()) : 0L;
            long days = remaining > 0L
                    ? Math.max(1L, (remaining + VillageGrievanceData.TICKS_PER_DAY - 1L)
                            / VillageGrievanceData.TICKS_PER_DAY) : 0L;
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_truce_countdown",
                    Component.literal(String.valueOf(days)).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int setDiplomaticState(CommandContext<CommandSourceStack> context,
                                          String village1Arg, String village2Arg, String stateName) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        if (!DiplomaticState.isValid(stateName)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.state_invalid", stateName));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relationData, village1Arg);
        String village2 = resolveVillageId(relationData, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        DiplomaticState state = DiplomaticState.fromString(stateName);
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        diplomacy.setState(village1, village2, state);

        if (state.isWar()) {

            VillageWarData.get(level).endWar(village1, village2);
            VillageDiplomacyData.PairState pair = diplomacy.getOrCreatePair(village1, village2);
            pair.declaredDay = VillageGrievanceData.dayOf(level);
            if (pair.warAggressor == null || pair.warAggressor.isEmpty()) {
                pair.warAggressor = village1;
            }
            diplomacy.markDirty();
        }

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_done",
                ModLang.villageName(relationData.getVillageName(village1)),
                ModLang.villageName(relationData.getVillageName(village2)),
                Component.translatable(state.getTranslationKey()).withStyle(stateColor(state))), false);
        return 1;
    }

    private static int setDiplomaticTension(CommandContext<CommandSourceStack> context,
                                            String village1Arg, String village2Arg, int amount) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relationData, village1Arg);
        String village2 = resolveVillageId(relationData, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        int old = diplomacy.getTension(village1, village2);
        diplomacy.setTension(village1, village2, amount);

        MutableComponent oldC = Component.literal(String.valueOf(old)).withStyle(tensionColor(old));
        MutableComponent newC = Component.literal(String.valueOf(amount)).withStyle(tensionColor(amount));
        MutableComponent bar = tensionBar(amount);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.tension_done",
                ModLang.villageName(relationData.getVillageName(village1)),
                ModLang.villageName(relationData.getVillageName(village2)),
                oldC, newC, bar), false);
        return 1;
    }

    private static int renameVillage(CommandContext<CommandSourceStack> context, String villageIdArg, String newName) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        String villageId = resolveVillageId(data, villageIdArg);
        if (villageId == null) {
            sendVillageNotFound(context, villageIdArg);
            return 0;
        }

        String oldStored = data.getVillageName(villageId);
        data.setVillageName(villageId, newName);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.rename_done",
                ModLang.villageName(oldStored),
                ModLang.villageName(newName)), false);

        return 1;
    }

    private static int showGrievancesNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.list_empty"));
            return 0;
        }
        return showGrievances(context, ids.get(0));
    }

    private static int showGrievances(CommandContext<CommandSourceStack> context, String villageArg) {
        VillageRelationshipData relations = VillageRelationshipData.get(context.getSource().getLevel());

        String villageId = resolveVillageId(relations, villageArg);
        if (villageId == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }
        return printGrievances(context, relations, villageId);
    }

    private static int printGrievances(CommandContext<CommandSourceStack> context,
                                       VillageRelationshipData relations, String villageId) {
        ServerLevel level = context.getSource().getLevel();
        long today = VillageGrievanceData.dayOf(level);
        List<VillageGrievanceData.Grievance> active = VillageGrievanceData.get(level).getActive(villageId, today);

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("===== ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("villagediplomacy.cmd.grievances_header",
                        ModLang.villageName(relations.getVillageName(villageId))).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" ✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("=====").withStyle(ChatFormatting.DARK_GRAY)), false);

        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.grievances_empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (VillageGrievanceData.Grievance grievance : active) {
            MutableComponent type = Component.translatable(grievance.type.getTranslationKey())
                    .withStyle(ChatFormatting.YELLOW);
            MutableComponent culprit = ModLang.coloredName(grievance.culpritName, ChatFormatting.GOLD);
            MutableComponent time = GrievanceHandler.timeComponent(grievance, today);
            MutableComponent impact = Component.literal("+" + grievance.impact)
                    .withStyle(ChatFormatting.WHITE);

            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.grievances_line",
                    type, culprit, time, impact).withStyle(ChatFormatting.GRAY), false);
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            int total = VillageGrievanceData.get(level)
                    .totalImpact(villageId, GrievanceHandler.playerCulprit(player), today);
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.grievances_total",
                    Component.literal(String.valueOf(total)).withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int ageGrievances(CommandContext<CommandSourceStack> context, int days) {
        if (!GrievanceHandler.isEnabled()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);
        long today = VillageGrievanceData.dayOf(level);
        int alive = VillageGrievanceData.get(level).age(days, today);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.test_grievance_aged",
                Component.literal(String.valueOf(days)).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.valueOf(alive)).withStyle(ChatFormatting.WHITE)), false);

        List<String> ids = villagesByDistance(context);
        if (!ids.isEmpty()) printGrievances(context, relations, ids.get(0));
        return 1;
    }

    private static int resolveGrievances(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }
        if (!GrievanceHandler.isEnabled()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);

        List<String> ids = villagesByDistance(context);
        if (ids.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.list_empty"));
            return 0;
        }

        String villageId = ids.get(0);
        long today = VillageGrievanceData.dayOf(level);
        int resolved = VillageGrievanceData.get(level)
                .resolve(villageId, GrievanceHandler.playerCulprit(player), today);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.test_grievance_resolved",
                ModLang.villageName(relations.getVillageName(villageId)),
                Component.literal(String.valueOf(resolved)).withStyle(ChatFormatting.WHITE)), false);

        return printGrievances(context, relations, villageId);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> warReadinessCommand() {
        return Commands.literal("warreadiness")
                .executes(DiplomacyCommands::showWarReadinessNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> showWarReadinessWithNearestOther(context,
                                StringArgumentType.getString(context, "village1")))
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> showWarReadiness(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> declareCommand() {
        return Commands.literal("declare")
                .executes(DiplomacyCommands::declareWarNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> forceWarState(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2"), true))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ultimatumCommand() {
        return Commands.literal("ultimatum")
                .executes(DiplomacyCommands::sendUltimatumNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> forceWarState(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2"), false))));
    }

    private static int showWarReadinessNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return showWarReadiness(context, ids.get(0), ids.get(1));
    }

    private static int showWarReadinessWithNearestOther(CommandContext<CommandSourceStack> context, String villageArg) {
        VillageRelationshipData data = VillageRelationshipData.get(context.getSource().getLevel());
        String named = resolveVillageId(data, villageArg);
        if (named == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }
        for (String id : villagesByDistance(context)) {
            if (!id.equals(named)) return showWarReadiness(context, named, id);
        }
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
        return 0;
    }

    private static int showWarReadiness(CommandContext<CommandSourceStack> context,
                                        String village1Arg, String village2Arg) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relations, village1Arg);
        String village2 = resolveVillageId(relations, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        DiplomaticState current = diplomacy.getState(village1, village2);
        WarDeclarationHandler.Assessment assessment = WarDeclarationHandler.assess(level, village1, village2);

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("===== ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("villagediplomacy.cmd.war_header").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" ✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("=====").withStyle(ChatFormatting.DARK_GRAY)), false);

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_pair",
                ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                ModLang.villageName(relations.getVillageName(assessment.defender)))
                .withStyle(ChatFormatting.RED), false);

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_line",
                Component.translatable(current.getTranslationKey()).withStyle(stateColor(current))), false);

        MutableComponent tensionC = Component.literal(String.valueOf(assessment.tension))
                .withStyle(tensionColor(assessment.tension));
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.tension_line",
                tensionC, tensionBar(assessment.tension)), false);

        MutableComponent desireC = Component.literal(assessment.warDesire + " / 100")
                .withStyle(assessment.desireOk() ? ChatFormatting.GREEN : ChatFormatting.RED);
        MutableComponent desireMinC = Component.literal(String.valueOf(WarDeclarationHandler.WAR_DESIRE_MIN))
                .withStyle(ChatFormatting.DARK_GRAY);
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_desire_line", desireC, desireMinC)
                .withStyle(ChatFormatting.GRAY), false);

        MutableComponent casusC = Component.translatable(assessment.casus.getTranslationKey())
                .withStyle(assessment.casusOk() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_casus_line", casusC)
                .withStyle(ChatFormatting.GRAY), false);

        MutableComponent goalC = Component.translatable(assessment.goal.getTranslationKey())
                .withStyle(assessment.goal.isPresent() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY);
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_goal_line", goalC)
                .withStyle(ChatFormatting.GRAY), false);

        if (!current.isWar() && current != DiplomaticState.ULTIMATUM && current != DiplomaticState.TRUCE) {
            checkLine(source, assessment.desireOk(), "villagediplomacy.cmd.war_req_desire");
            checkLine(source, assessment.casusOk(), "villagediplomacy.cmd.war_req_casus");
            checkLine(source, assessment.tensionOk(), "villagediplomacy.cmd.war_req_tension");
        }

        VillageLeader leader = VillageLeader.resolve(level, assessment.aggressor);
        MutableComponent stance = Component.translatable(leader.isAggressive()
                        ? "villagediplomacy.leader.aggressive" : "villagediplomacy.leader.calm")
                .withStyle(leader.isAggressive() ? ChatFormatting.RED : ChatFormatting.GREEN);

        if (!current.isWar() && current != DiplomaticState.ULTIMATUM) {
            stance.append(Component.literal(" ")).append(Component.translatable(leader.isAggressive()
                    ? "villagediplomacy.leader.will_declare" : "villagediplomacy.leader.will_ultimatum"));
        }
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_leader_line",
                ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                leader.nameComponent(ChatFormatting.GOLD),
                stance).withStyle(ChatFormatting.GRAY), false);

        long remaining = 0L;
        if (current == DiplomaticState.ULTIMATUM || current == DiplomaticState.TRUCE) {
            VillageDiplomacyData.PairState pair = diplomacy.peekPair(village1, village2);
            if (pair != null) {
                long until = current == DiplomaticState.TRUCE ? pair.truceUntil : pair.ultimatumUntil;
                remaining = Math.max(0L, until - level.getGameTime());
            }
        }
        final long remainingTicks = remaining;
        source.sendSuccess(() -> warConclusion(current, assessment, remainingTicks)
                .withStyle(warConclusionColor(current, assessment)), false);
        return 1;
    }

    private static MutableComponent warConclusion(DiplomaticState current,
                                                  WarDeclarationHandler.Assessment assessment,
                                                  long remainingTicks) {
        if (current.isWar()) return Component.translatable("villagediplomacy.cmd.war_ongoing");
        if (current == DiplomaticState.ULTIMATUM) {
            if (remainingTicks > 0L) {
                long days = Math.max(1L, remainingTicks / VillageGrievanceData.TICKS_PER_DAY);
                return Component.translatable("villagediplomacy.cmd.war_ultimatum_countdown",
                        Component.literal(String.valueOf(days)).withStyle(ChatFormatting.GOLD));
            }
            return Component.translatable("villagediplomacy.cmd.war_ultimatum_pending");
        }
        if (current == DiplomaticState.TRUCE) {
            long days = remainingTicks > 0L
                    ? Math.max(1L, (remainingTicks + VillageGrievanceData.TICKS_PER_DAY - 1L)
                            / VillageGrievanceData.TICKS_PER_DAY) : 0L;
            return Component.translatable("villagediplomacy.cmd.war_truce_countdown",
                    Component.literal(String.valueOf(days)).withStyle(ChatFormatting.GOLD));
        }
        return Component.translatable(assessment.gate()
                ? "villagediplomacy.cmd.war_ready" : "villagediplomacy.cmd.war_not_ready");
    }

    private static ChatFormatting warConclusionColor(DiplomaticState current,
                                                     WarDeclarationHandler.Assessment assessment) {
        if (current.isWar()) return ChatFormatting.RED;
        if (current == DiplomaticState.ULTIMATUM || current == DiplomaticState.TRUCE) return ChatFormatting.YELLOW;
        return assessment.gate() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY;
    }

    private static void checkLine(CommandSourceStack source, boolean ok, String requirementKey) {
        source.sendSuccess(() -> Component.translatable(ok
                                ? "villagediplomacy.cmd.war_check_ok" : "villagediplomacy.cmd.war_check_no",
                        Component.translatable(requirementKey))
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY), false);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> warStatusCommand() {
        return Commands.literal("war")
                .executes(DiplomacyCommands::showWarStatusNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> showWarStatusWithNearestOther(context,
                                StringArgumentType.getString(context, "village1")))
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> showWarStatus(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> peaceCommand() {
        return Commands.literal("peace")
                .executes(DiplomacyCommands::signPeaceNearest)
                .then(Commands.argument("village1", StringArgumentType.string())
                        .suggests(VILLAGE_SUGGESTIONS)
                        .executes(context -> signPeaceWithNearestOther(context,
                                StringArgumentType.getString(context, "village1")))
                        .then(Commands.argument("village2", StringArgumentType.string())
                                .suggests(VILLAGE_SUGGESTIONS)
                                .executes(context -> signPeace(context,
                                        StringArgumentType.getString(context, "village1"),
                                        StringArgumentType.getString(context, "village2")))));
    }

    private static int signPeaceNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return signPeace(context, ids.get(0), ids.get(1));
    }

    private static int signPeaceWithNearestOther(CommandContext<CommandSourceStack> context, String villageArg) {
        VillageRelationshipData relations = VillageRelationshipData.get(context.getSource().getLevel());
        String named = resolveVillageId(relations, villageArg);
        if (named == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }
        for (String id : villagesByDistance(context)) {
            if (!id.equals(named)) return signPeace(context, named, id);
        }
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
        return 0;
    }

    private static int signPeace(CommandContext<CommandSourceStack> context,
                                 String village1Arg, String village2Arg) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);
        String village1 = resolveVillageId(relations, village1Arg);
        String village2 = resolveVillageId(relations, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        VillageDiplomacyData.PairState pair = diplomacy.peekPair(village1, village2);
        if (pair == null || !pair.state.isWar()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.war_not_at_war",
                    ModLang.villageName(relations.getVillageName(village1)),
                    ModLang.villageName(relations.getVillageName(village2))));
            return 0;
        }

        long now = level.getServer().overworld().getGameTime();
        int reducedRelationship = Math.max(-100, relations.getRelationship(village1, village2)
                - WarDeclarationHandler.PEACE_RELATION_PENALTY);
        relations.setRelationship(village1, village2, reducedRelationship);
        int reducedTension = WarDeclarationHandler.PEACE_RESIDUAL_TENSION;
        diplomacy.setTension(village1, village2, reducedTension);
        pair.state = DiplomaticState.TRUCE;
        pair.truceUntil = now + (long) WarDeclarationHandler.PEACE_TRUCE_DAYS
                * VillageGrievanceData.TICKS_PER_DAY;
        pair.warAggressor = "";
        diplomacy.markDirty();
        VillageWarData.get(level).endWar(village1, village2);
        com.cesoti2006.villagediplomacy.diplomacy.SkirmishManager.forget(
                VillageDiplomacyData.pairKey(village1, village2));

        long today = VillageGrievanceData.dayOf(level);
        VillageGrievanceData grievances = VillageGrievanceData.get(level);
        int resolvedFromFirst = grievances.resolve(village1, "village:" + village2, today);
        int resolvedFromSecond = grievances.resolve(village2, "village:" + village1, today);
        String firstName = VillageDisplayName.visibleName(relations.getVillageName(village1));
        if (firstName == null) firstName = village1;
        String secondName = VillageDisplayName.visibleName(relations.getVillageName(village2));
        if (secondName == null) secondName = village2;
        int archivedByFirst = archiveWarMemory(grievances, village1, village2, secondName, today);
        int archivedBySecond = archiveWarMemory(grievances, village2, village1, firstName, today);

        MutableComponent notice = GrievanceHandler.prefix()
                .append(Component.translatable("villagediplomacy.war.peace",
                        ModLang.villageName(relations.getVillageName(village1)),
                        ModLang.villageName(relations.getVillageName(village2)),
                        Component.literal(String.valueOf(WarDeclarationHandler.PEACE_TRUCE_DAYS))
                                .withStyle(ChatFormatting.GOLD),
                        Component.literal(String.valueOf(resolvedFromFirst + resolvedFromSecond))
                                .withStyle(ChatFormatting.AQUA),
                        Component.literal(String.valueOf(archivedByFirst + archivedBySecond))
                                .withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
        context.getSource().sendSuccess(() -> notice, false);
        for (ServerPlayer nearby : level.players()) {
            if (nearby == context.getSource().getEntity()) continue;
            BlockPos firstCenter = relations.getVillagePosition(village1);
            BlockPos secondCenter = relations.getVillagePosition(village2);
            if ((firstCenter != null && nearby.blockPosition().distSqr(firstCenter) <= 128L * 128L)
                    || (secondCenter != null && nearby.blockPosition().distSqr(secondCenter) <= 128L * 128L)) {
                nearby.sendSystemMessage(notice.copy());
            }
        }
        return 1;
    }

    private static int archiveWarMemory(VillageGrievanceData grievances, String villageId,
                                        String formerEnemyId, String formerEnemyName, long today) {
        VillageGrievanceData.Recorded recorded = grievances.recordWarMemory(
                villageId, formerEnemyId, formerEnemyName, today);
        return recorded.isNew ? 1 : 0;
    }

    private static int showWarStatusNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return showWarStatus(context, ids.get(0), ids.get(1));
    }

    private static int showWarStatusWithNearestOther(CommandContext<CommandSourceStack> context,
                                                     String villageArg) {
        VillageRelationshipData data = VillageRelationshipData.get(context.getSource().getLevel());
        String named = resolveVillageId(data, villageArg);
        if (named == null) {
            sendVillageNotFound(context, villageArg);
            return 0;
        }
        for (String id : villagesByDistance(context)) {
            if (!id.equals(named)) return showWarStatus(context, named, id);
        }
        context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
        return 0;
    }

    private static int showWarStatus(CommandContext<CommandSourceStack> context,
                                     String village1Arg, String village2Arg) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relations, village1Arg);
        String village2 = resolveVillageId(relations, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageWarData.WarRecord war = VillageWarData.get(level).getWar(village1, village2);
        CommandSourceStack source = context.getSource();
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        VillageDiplomacyData.PairState pair = diplomacy.peekPair(village1, village2);
        DiplomaticState state = pair != null ? pair.state : DiplomaticState.PEACE;
        long now = level.getServer().overworld().getGameTime();
        if (war == null) {
            if (state.isWar()) {
                source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_declared_not_mobilized")
                        .withStyle(ChatFormatting.YELLOW), false);
                return 1;
            }

            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_not_at_war",
                    ModLang.villageName(relations.getVillageName(village1)),
                    ModLang.villageName(relations.getVillageName(village2)))
                    .withStyle(ChatFormatting.GRAY), false);
            if (state == DiplomaticState.TRUCE && pair != null) {
                source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_line",
                        Component.translatable(state.getTranslationKey()).withStyle(stateColor(state))), false);
                long remaining = Math.max(0L, pair.truceUntil - now);
                long days = remaining > 0L
                        ? Math.max(1L, (remaining + VillageGrievanceData.TICKS_PER_DAY - 1L)
                                / VillageGrievanceData.TICKS_PER_DAY) : 0L;
                source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_truce_countdown",
                        Component.literal(String.valueOf(days)).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.YELLOW), false);
            }
            return 1;
        }

        source.sendSuccess(() -> Component.literal("===== ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.translatable("villagediplomacy.cmd.war_status_header")
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" ✦ ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("=====").withStyle(ChatFormatting.DARK_GRAY)), false);

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_pair",
                ModLang.villageName(relations.getVillageName(war.attacker)),
                ModLang.villageName(relations.getVillageName(war.defender)))
                .withStyle(ChatFormatting.RED), false);

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.state_line",
                Component.translatable(state.getTranslationKey()).withStyle(stateColor(state))), false);

        if (pair != null) {
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_casus_line",
                    Component.translatable(pair.casusBelli.getTranslationKey()).withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_goal_line",
                    Component.translatable(pair.warGoal.getTranslationKey()).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.GRAY), false);
        }

        boolean mobilised = now < war.mobilizationUntil;
        MutableComponent mobilisedC = Component.translatable(mobilised
                        ? "villagediplomacy.cmd.war_mobilized_yes" : "villagediplomacy.cmd.war_mobilized_no")
                .withStyle(mobilised ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_mobilization_line", mobilisedC)
                .withStyle(ChatFormatting.GRAY), false);

        MutableComponent forceAttacker = forceSide(relations, war.attacker, war.attackerStrength);
        MutableComponent forceDefender = forceSide(relations, war.defender, war.defenderStrength);
        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_force_line",
                forceAttacker, forceDefender).withStyle(ChatFormatting.RED), false);

        source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_score_line",
                ModLang.villageName(relations.getVillageName(war.attacker)).withStyle(ChatFormatting.RED),
                warScore(war.attackerScore).withStyle(ChatFormatting.RED),
                ModLang.villageName(relations.getVillageName(war.defender)).withStyle(ChatFormatting.RED),
                warScore(war.defenderScore).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.RED), false);

        boolean attackerAhead = war.attackerScore >= war.defenderScore;
        String winner = attackerAhead ? war.attacker : war.defender;
        if (Math.max(war.attackerScore, war.defenderScore) >= 75) {
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_tilted",
                    ModLang.villageName(relations.getVillageName(winner)))
                    .withStyle(ChatFormatting.GOLD), false);
        } else {
            source.sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_even")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }

    private static MutableComponent forceSide(VillageRelationshipData relations, String villageId, int strength) {
        return ModLang.villageName(relations.getVillageName(villageId)).withStyle(ChatFormatting.RED)
                .append(Component.literal(" " + strength).withStyle(ChatFormatting.RED));
    }

    private static MutableComponent warScore(int score) {
        ChatFormatting color = score >= 60 ? ChatFormatting.GREEN
                : score <= 40 ? ChatFormatting.RED : ChatFormatting.YELLOW;
        return Component.literal(String.valueOf(score)).withStyle(color);
    }

    private static int declareWarNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return forceWarState(context, ids.get(0), ids.get(1), true);
    }

    private static int sendUltimatumNearest(CommandContext<CommandSourceStack> context) {
        List<String> ids = villagesByDistance(context);
        if (ids.size() < 2) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.need_two_villages"));
            return 0;
        }
        return forceWarState(context, ids.get(0), ids.get(1), false);
    }

    private static int forceWarState(CommandContext<CommandSourceStack> context, String village1Arg,
                                     String village2Arg, boolean declare) {
        if (!com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.module_disabled"));
            return 0;
        }

        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData relations = VillageRelationshipData.get(level);

        String village1 = resolveVillageId(relations, village1Arg);
        String village2 = resolveVillageId(relations, village2Arg);
        if (village1 == null || village2 == null) {
            sendVillageNotFound(context, village1 == null ? village1Arg : village2Arg);
            return 0;
        }

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        WarDeclarationHandler.Assessment assessment = WarDeclarationHandler.assess(level, village1, village2);
        VillageDiplomacyData.PairState pair = diplomacy.getOrCreatePair(village1, village2);

        pair.warDesire = assessment.warDesire;
        pair.casusBelli = assessment.casus;
        pair.warGoal = assessment.goal;
        if (declare) {
            pair.state = DiplomaticState.WAR;
            pair.warAggressor = assessment.aggressor;
            pair.declaredDay = VillageGrievanceData.dayOf(level);
        } else {
            pair.state = DiplomaticState.ULTIMATUM;
            pair.ultimatumUntil = level.getServer().overworld().getGameTime()
                    + WarDeclarationHandler.ULTIMATUM_DAYS * VillageGrievanceData.TICKS_PER_DAY;
        }
        diplomacy.markDirty();

        final boolean declared = declare;

        MutableComponent announcement = Component.translatable(declared
                        ? "villagediplomacy.cmd.declare_done" : "villagediplomacy.cmd.ultimatum_done",
                ModLang.villageName(relations.getVillageName(assessment.aggressor)),
                ModLang.villageName(relations.getVillageName(assessment.defender)));
        if (!declared) {
            announcement.append(Component.translatable("villagediplomacy.cmd.ultimatum_threat"))
                    .append(VillageLeader.resolve(level, assessment.aggressor)
                            .nameComponent(ChatFormatting.GOLD));
        }
        context.getSource().sendSuccess(() -> announcement
                .withStyle(declared ? ChatFormatting.RED : ChatFormatting.YELLOW), false);

        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.war_forced")
                .withStyle(ChatFormatting.DARK_GRAY), false);

        return showWarReadiness(context, village1, village2);
    }

    private static String resolveVillageId(VillageRelationshipData data, String input) {
        if (input == null) return null;
        String cleaned = ModLang.stripFormatting(input).replace("\"", "").trim();
        if (cleaned.isEmpty()) return null;
        if (data.getVillagePosition(cleaned) != null) return cleaned;

        for (Map.Entry<String, String> entry : data.getAllVillageNames().entrySet()) {
            String stored = entry.getValue();
            String candidate = ModLang.stripFormatting(stored).trim();
            if (candidate.equalsIgnoreCase(cleaned)) return entry.getKey();

            String visible = VillageDisplayName.visibleName(stored);
            if (visible != null && visible.equalsIgnoreCase(cleaned)) return entry.getKey();
        }
        return null;
    }

    private static String closestVillageName(VillageRelationshipData data, String input, List<String> ids) {
        String cleaned = ModLang.stripFormatting(input).replace("\"", "").trim().toLowerCase(Locale.ROOT);
        if (cleaned.length() < 4) return null;

        String best = null;
        int bestDistance = 3;
        for (String id : ids) {
            String stored = data.getVillageName(id);
            String visible = VillageDisplayName.visibleName(stored);
            for (String candidate : new String[] { visible, stored }) {
                if (candidate == null || candidate.isEmpty()) continue;
                String plain = ModLang.stripFormatting(candidate).replace("\"", "").trim();
                if (plain.isEmpty()) continue;
                int distance = editDistance(cleaned, plain.toLowerCase(Locale.ROOT));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = plain;
                }
            }
        }
        return best;
    }

    private static int editDistance(String from, String to) {
        int[] previous = new int[to.length() + 1];
        int[] current = new int[to.length() + 1];
        for (int j = 0; j <= to.length(); j++) previous[j] = j;

        for (int i = 1; i <= from.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= to.length(); j++) {
                int cost = from.charAt(i - 1) == to.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[to.length()];
    }

    private static ChatFormatting stateColor(DiplomaticState state) {
        return switch (state) {
            case PEACE -> ChatFormatting.GREEN;
            case TRUCE -> ChatFormatting.AQUA;
            case TENSION -> ChatFormatting.YELLOW;
            case CRISIS -> ChatFormatting.GOLD;
            case ULTIMATUM -> ChatFormatting.RED;
            case MOBILIZING, WAR, SIEGE, OCCUPATION -> ChatFormatting.RED;
        };
    }

    private static ChatFormatting relationshipColor(VillageRelationshipData.RelationshipStatus status) {
        return switch (status) {
            case ALLIED -> ChatFormatting.GREEN;
            case HOSTILE -> ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GRAY;
        };
    }

    private static ChatFormatting tensionColor(int tension) {
        if (tension >= 75) return ChatFormatting.RED;
        if (tension >= 50) return ChatFormatting.GOLD;
        if (tension >= 25) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

    private static MutableComponent tensionBar(int tension) {
        int value = Math.max(0, Math.min(100, tension));
        int filled = (int) Math.round(value / 10.0);
        MutableComponent bar = Component.literal("§8[");
        if (filled > 0) {
            bar.append(Component.literal("█".repeat(filled)).withStyle(tensionColor(value)));
        }
        if (filled < 10) {
            bar.append(Component.literal("░".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY));
        }
        return bar.append(Component.literal("§8]"));
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
        int oldClaimRep = repData.getReputation(player.getUUID(), playerPos);
        repData.setReputation(player.getUUID(), playerPos, 0);
        com.cesoti2006.villagediplomacy.events.VillagerEventHandler.checkAndNotifyReputationChange(player, oldClaimRep, 0);

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

    private static int setVillagerLevel(CommandContext<CommandSourceStack> context, int newLevel) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(12.0D));
        if (nearby.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.no_villager_near"));
            return 0;
        }

        Villager target = nearby.get(0);
        double bestDistance = target.distanceToSqr(player);
        for (Villager villager : nearby) {
            double distance = villager.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = villager;
            }
        }

        target.setVillagerData(target.getVillagerData().setLevel(newLevel));
        target.setCustomNameVisible(true);

        Villager chosen = target;
        Component shownName = chosen.getCustomName() != null
                ? chosen.getCustomName()
                : ModLang.professionName(chosen.getVillagerData().getProfession());
        context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.setlevel_done",
                shownName, newLevel), false);
        return 1;
    }

    private static int testMcARole(CommandContext<CommandSourceStack> context, MCARebornCompat.TestRole role) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        if (!MCARebornCompat.isLoaded()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.mca_missing"));
            return 0;
        }

        if (!MCARebornCompat.isSupported()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.mca_unsupported"));
            return 0;
        }

        if (!MCARebornCompat.hasNearbyVillager(player.serverLevel(), player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.no_mca_villager_near"));
            return 0;
        }

        if (!MCARebornCompat.promoteNearest(player.serverLevel(), player, role)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.mca_promote_failed"));
            return 0;
        }

        Component roleName = Component.translatable(
                "villagediplomacy.cmd.test_mca_role." + role.name().toLowerCase(java.util.Locale.ROOT));
        context.getSource().sendSuccess(
                () -> Component.translatable("villagediplomacy.cmd.test_mca_done", roleName), false);
        return 1;
    }

    private static BlockPos aimedContainer(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(6.0D));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos pos = hit.getBlockPos();
        return VillagerActivityBehavior.isChestOrBarrelAt(level, pos) ? pos : null;
    }

    private static int testChestDeposit(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0D));
        if (nearby.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.no_villager_near"));
            return 0;
        }

        Villager target = nearby.get(0);
        double bestDistance = target.distanceToSqr(player);
        for (Villager villager : nearby) {
            double distance = villager.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = villager;
            }
        }

        Component villagerName = target.getCustomName() != null
                ? target.getCustomName()
                : ModLang.professionName(target.getVillagerData().getProfession());

        BlockPos aimed = aimedContainer(player, level);
        if (aimed != null && VillagerActivityBehavior.isPlayerMadeContainer(level, aimed)) {
            BlockPos forced = VillagerActivityBehavior.forceDeposit(target, level, aimed);
            if (forced == null) {
                context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_chest_full",
                        aimed.getX(), aimed.getY(), aimed.getZ()));
                return 0;
            }
            context.getSource().sendSuccess(
                    () -> Component.translatable("villagediplomacy.cmd.test_chest_forced",
                            villagerName, aimed.getX(), aimed.getY(), aimed.getZ()), false);
            return 1;
        }

        BlockPos chestPos = aimed != null ? aimed : VillagerActivityBehavior.findChestFor(target, level);
        if (chestPos == null) {
            BlockPos rejected = VillagerActivityBehavior.findAnyContainerFor(target, level);
            if (rejected != null) {

                context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_chest_owned",
                        rejected.getX(), rejected.getY(), rejected.getZ()));
                return 0;
            }
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_chest_no_chest"));
            return 0;
        }

        boolean givenForTest = !VillagerActivityBehavior.isHoldingValuable(target);

        ItemStack sample = givenForTest
                ? new ItemStack(Items.GOLD_INGOT, 1)
                : target.getMainHandItem().copy();

        BlockPos used = VillagerActivityBehavior.depositValuableInChest(target, level, chestPos);
        if (used == null) {

            BlockPos playerMade = null;
            for (BlockPos pos : VillagerActivityBehavior.findContainersWithSpaceFor(
                    target.blockPosition(), level, sample)) {
                if (VillagerActivityBehavior.isPlayerMadeContainer(level, pos)) {
                    playerMade = pos;
                    break;
                }
            }
            if (playerMade != null) {
                context.getSource().sendFailure(
                        Component.translatable("villagediplomacy.cmd.test_chest_only_player",
                                playerMade.getX(), playerMade.getY(), playerMade.getZ()));
                return 0;
            }
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.test_chest_full",
                    chestPos.getX(), chestPos.getY(), chestPos.getZ()));
            return 0;
        }

        if (!used.equals(chestPos)) {

            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.test_chest_moved",
                    chestPos.getX(), chestPos.getY(), chestPos.getZ(),
                    villagerName,
                    used.getX(), used.getY(), used.getZ()), false);
            return 1;
        }

        if (givenForTest) {
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.test_chest_test_gold",
                    villagerName, used.getX(), used.getY(), used.getZ()), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("villagediplomacy.cmd.test_chest_done",
                    villagerName, used.getX(), used.getY(), used.getZ()), false);
        }

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
        int oldGiftRep = repData.getReputation(player.getUUID(), villagePos);
        repData.addReputation(player.getUUID(), villagePos, totalGift);
        int newRep = repData.getReputation(player.getUUID(), villagePos);
        com.cesoti2006.villagediplomacy.events.VillagerEventHandler.checkAndNotifyReputationChange(player, oldGiftRep, newRep);

        held.shrink(amount);

        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));
        player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.gift_done",
                amount, itemName, VillageDisplayName.asComponent(villageStored)));
        ModLang.sendReputationSummary(player, totalGift, newRep);
        player.sendSystemMessage(Component.translatable("villagediplomacy.enter.bar"));

        return 1;
    }
}
