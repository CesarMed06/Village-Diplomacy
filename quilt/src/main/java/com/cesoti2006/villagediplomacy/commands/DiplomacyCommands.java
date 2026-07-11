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

import java.util.*;
import java.io.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData;
import com.cesoti2006.villagediplomacy.data.PlayerClaimedVillageData.ClaimedVillage;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

public class DiplomacyCommands {

    public static final Set<UUID> HUD_DISABLED = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diplomacy")
                .executes(DiplomacyCommands::showHelp)
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
                .then(Commands.literal("getrep")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return getReputation(context, player);
                        }))
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
                .then(Commands.literal("claim")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> claimVillage(context, StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("unclaim")
                        .executes(DiplomacyCommands::unclaimVillage))
                .then(Commands.literal("list")
                        .executes(DiplomacyCommands::listVillages))
                .then(Commands.literal("hud")
                        .executes(DiplomacyCommands::toggleHud))
                .then(Commands.literal("test")
                        .then(Commands.literal("caravan")
                                .executes(DiplomacyCommands::testCaravan))
                        .then(Commands.literal("raid")
                                .executes(DiplomacyCommands::testRaid))));
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6===== Village Diplomacy v1.3.0 ====="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy info §7- Ver info de la aldea actual"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy name <nombre> §7- Nombrar aldea"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy list §7- Listar aldeas registradas"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy claim <nombre> §7- Reclamar aldea"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy unclaim §7- Liberar aldea"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy hud §7- Activar/desactivar HUD"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy getrep §7- Ver tu reputación"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy reputation get <jugador> §7- Ver reputación"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy reputation set <jugador> <valor> §7- Fijar reputación"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy reputation add <jugador> <valor> §7- Ajustar reputación"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy villages list §7- Listar todas las aldeas"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy villages relations <id> §7- Relaciones entre aldeas"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/diplomacy villages rename <id> <nombre> §7- Renombrar aldea"), false);
        context.getSource().sendSuccess(() -> Component.literal("§8=========================================="), false);
        return 1;
    }

    private static int toggleHud(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        UUID playerId = player.getUUID();
        if (HUD_DISABLED.contains(playerId)) {
            HUD_DISABLED.remove(playerId);
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.hud_on"));
        } else {
            HUD_DISABLED.add(playerId);
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.hud_off"));
        }
        saveHudState(player.serverLevel());
        return 1;
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

    private static int claimVillage(CommandContext<CommandSourceStack> context, String name) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("villagediplomacy.cmd.players_only"));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        // Check not too close to an existing vanilla village
        Optional<BlockPos> nearVanilla = VillageDetector.findNearestVillage(level, playerPos, 
            VillageDiplomacyConfig.customVillageRadius + 16);
        if (nearVanilla.isPresent()) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_too_close"));
            return 0;
        }

        PlayerClaimedVillageData data = PlayerClaimedVillageData.get(level);

        // Check not too close to other custom villages
        int minDist = VillageDiplomacyConfig.customVillageRadius * 2;
        if (data.isTooCloseToAny(playerPos, minDist)) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_overlap"));
            return 0;
        }

        int radius = VillageDiplomacyConfig.customVillageRadius;
        UUID owner = player.getUUID();
        boolean success = data.addVillage(playerPos, name, owner, radius);

        if (!success) {
            player.sendSystemMessage(Component.translatable("villagediplomacy.cmd.claim_exists"));
            return 0;
        }

        // Register the village in the relationship/reputation system so its name appears in entry/exit messages
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(playerPos, level);
        String relationId = relationData.getVillageId(playerPos);
        relationData.setVillageName(relationId, name);

        // Initialize neutral reputation for the owner
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

        // First try exact position
        boolean removed = data.removeVillage(playerPos, player.getUUID());
        
        if (!removed) {
            // Try nearest custom village within radius
            int radius = VillageDiplomacyConfig.customVillageRadius;
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

    private static final Gson GSON = new Gson();

    public static void loadHudState(ServerLevel level) {
        File file = new File(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), "villagediplomacy_hud.json");
        if (!file.exists()) return;
        try (java.io.Reader reader = new java.io.FileReader(file)) {
            List<String> uuids = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (uuids != null) {
                for (String uuidStr : uuids) {
                    try { HUD_DISABLED.add(UUID.fromString(uuidStr)); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[VillageDiplomacy] Failed to load HUD state: " + e.getMessage());
        }
    }

    private static void saveHudState(ServerLevel level) {
        File file = new File(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), "villagediplomacy_hud.json");
        try (java.io.Writer writer = new java.io.FileWriter(file)) {
            List<String> uuids = new ArrayList<>();
            for (UUID id : HUD_DISABLED) uuids.add(id.toString());
            GSON.toJson(uuids, writer);
        } catch (Exception e) {
            System.err.println("[VillageDiplomacy] Failed to save HUD state: " + e.getMessage());
        }
    }
}
