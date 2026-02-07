package com.cesoti2006.villagediplomacy.commands;

import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
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
        // Comando /pardon me - Perdón personal (sin permisos de admin)
        dispatcher.register(Commands.literal("pardon")
                .then(Commands.literal("me")
                        .executes(DiplomacyCommands::pardonMe)));
        
        // Comandos de diplomacy (sin permisos)
        dispatcher.register(Commands.literal("diplomacy")
                .then(Commands.literal("name")
                        .then(Commands.argument("villageName", StringArgumentType.greedyString())
                                .executes(context -> nameCurrentVillage(context, StringArgumentType.getString(context, "villageName")))))
                .then(Commands.literal("info")
                        .executes(DiplomacyCommands::showInfo)));
        
        // Comandos de admin
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
            context.getSource().sendFailure(Component.literal("§cNo hay aldea cercana. Debes estar dentro de 200 bloques de una aldea."));
            return 0;
        }
        
        BlockPos villagePos = nearestVillage.get();
        int reputation = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(reputation);
        
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.literal(
                "§6Reputación de " + player.getName().getString() + " en §e" + villageName + "§6: §f" + reputation + " §7(" + status + "§7)"), false);
        return reputation;
    }

    private static int setReputation(CommandContext<CommandSourceStack> context, ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();
        VillageReputationData data = VillageReputationData.get(level);

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        
        if (nearestVillage.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo hay aldea cercana. Debes estar dentro de 200 bloques de una aldea."));
            return 0;
        }
        
        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.setReputation(player.getUUID(), villagePos, amount);
        int newRep = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(newRep);
        
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.literal(
                "§aReputación de " + player.getName().getString() + " en §e" + villageName + "§a: §f" + oldRep + " §7→ §f" + newRep + " §7(" + status + "§7)"), false);

        return 1;
    }

    private static int addReputation(CommandContext<CommandSourceStack> context, ServerPlayer player, int amount) {
        ServerLevel level = player.serverLevel();
        VillageReputationData data = VillageReputationData.get(level);

        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 200);
        
        if (nearestVillage.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo hay aldea cercana. Debes estar dentro de 200 bloques de una aldea."));
            return 0;
        }
        
        BlockPos villagePos = nearestVillage.get();
        int oldRep = data.getReputation(player.getUUID(), villagePos);
        data.addReputation(player.getUUID(), villagePos, amount);
        int newRep = data.getReputation(player.getUUID(), villagePos);
        String status = getReputationStatus(newRep);
        
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);

        context.getSource().sendSuccess(() -> Component.literal(
                "§aReputación de " + player.getName().getString() + " en §e" + villageName + "§a: §f" + oldRep + " §7→ §f" + newRep +
                        " §7(" + status + "§7)"), false);

        return 1;
    }

    private static int listVillages(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        Map<String, BlockPos> villages = data.getAllVillages();

        if (villages.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eNo hay aldeas registradas."), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("§6=== Aldeas Registradas ==="), false);
        for (Map.Entry<String, BlockPos> entry : villages.entrySet()) {
            BlockPos pos = entry.getValue();
            String villageId = entry.getKey();
            String villageName = data.getVillageName(villageId);
            context.getSource().sendSuccess(() -> Component.literal(
                    "§7- §6" + villageName + " §8(" + villageId + ") §7@ [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"), false);
        }

        return villages.size();
    }

    private static int showRelations(CommandContext<CommandSourceStack> context, String villageId) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        if (data.getVillagePosition(villageId) == null) {
            context.getSource().sendFailure(Component.literal("§cAldea no encontrada: " + villageId));
            return 0;
        }

        String villageName = data.getVillageName(villageId);
        context.getSource().sendSuccess(() -> Component.literal("§6=== Relaciones de " + villageName + " ==="), false);

        Map<String, BlockPos> allVillages = data.getAllVillages();
        for (String otherVillage : allVillages.keySet()) {
            if (!otherVillage.equals(villageId)) {
                int points = data.getRelationship(villageId, otherVillage);
                VillageRelationshipData.RelationshipStatus status = data.getStatus(villageId, otherVillage);
                String otherVillageName = data.getVillageName(otherVillage);
                context.getSource().sendSuccess(() -> Component.literal(
                        "§7- §6" + otherVillageName + "§7: " + status.getDisplay() + " §7(" + points + " puntos)"), false);
            }
        }

        return 1;
    }

    private static int setRelation(CommandContext<CommandSourceStack> context, String village1, String village2, int points) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        data.setRelationship(village1, village2, points);
        VillageRelationshipData.RelationshipStatus status = data.getStatus(village1, village2);

        context.getSource().sendSuccess(() -> Component.literal(
                "§aRelación establecida: " + village1 + " ↔ " + village2 +
                        " = " + status.getDisplay() + " §7(" + points + " puntos)"), false);

        return 1;
    }

    private static int renameVillage(CommandContext<CommandSourceStack> context, String villageId, String newName) {
        ServerLevel level = context.getSource().getLevel();
        VillageRelationshipData data = VillageRelationshipData.get(level);

        if (data.getVillagePosition(villageId) == null) {
            context.getSource().sendFailure(Component.literal("§cAldea no encontrada: " + villageId));
            return 0;
        }

        String oldName = data.getVillageName(villageId);
        data.setVillageName(villageId, newName);

        context.getSource().sendSuccess(() -> Component.literal(
                "§aAldea renombrada: §6" + oldName + " §7→ §6" + newName), false);

        return 1;
    }
    
    /**
     * Comando /pardon me - Perdón personal que cuesta reputación
     */
    private static int pardonMe(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§cSolo los jugadores pueden usar este comando."));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        
        // Verificar si está en una aldea
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c¡Debes estar dentro de una aldea para pedir perdón!"));
            return 0;
        }
        
        // Verificar reputación
        VillageReputationData reputationData = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        int currentReputation = reputationData.getReputation(player.getUUID(), villagePos);
        
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);
        
        // Limpiar crímenes usando el handler (GRATIS - solo para testing)
        int golemsCalmed = com.cesoti2006.villagediplomacy.VillageDiplomacyMod.eventHandler.clearCrimes(player, level);
        
        // Mensajes de éxito
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendSystemMessage(Component.literal("§a[" + villageName + "] Crímenes perdonados (Modo de Prueba - GRATIS)"));
        player.sendSystemMessage(Component.literal("§7Reputación actual: §e" + currentReputation));
        if (golemsCalmed > 0) {
            player.sendSystemMessage(Component.literal("§7" + golemsCalmed + " Golem(s) de Hierro calmado(s)."));
        }
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        
        return 1;
    }
    
    /**
     * Comando para mostrar información de reputación actual
     */
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        
        // Verificar si está en una aldea
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c¡No estás en una aldea!"));
            return 0;
        }
        
        VillageReputationData reputationData = VillageReputationData.get(level);
        BlockPos villagePos = nearestVillage.get();
        int reputation = reputationData.getReputation(player.getUUID(), villagePos);
        
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos);
        String villageId = relationData.getVillageId(villagePos);
        String villageName = relationData.getVillageName(villageId);
        String status = getReputationStatus(reputation);
        
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendSystemMessage(Component.literal("§6✦ Village: §e" + villageName));
        player.sendSystemMessage(Component.literal("§7Reputation: §e" + reputation + " §8(§7" + status + "§8)"));
        player.sendSystemMessage(Component.literal("§7Position: §e" + villagePos.getX() + ", " + villagePos.getY() + ", " + villagePos.getZ()));
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        
        return 1;
    }
    
    /**
     * Comando para nombrar la aldea actual (cualquier jugador)
     */
    private static int nameCurrentVillage(CommandContext<CommandSourceStack> context, String newName) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("§c¡Solo los jugadores pueden usar este comando!"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        Optional<BlockPos> nearestVillage = VillageDetector.findNearestVillage(level, player.blockPosition(), 100);
        
        if (nearestVillage.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c¡Debes estar dentro de una aldea para nombrarla!"));
            return 0;
        }
        
        BlockPos villagePos = nearestVillage.get();
        VillageRelationshipData relationData = VillageRelationshipData.get(level);
        relationData.registerVillage(villagePos);
        String villageId = relationData.getVillageId(villagePos);
        
        // Guardar el nombre
        relationData.setVillageName(villageId, newName);
        
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        player.sendSystemMessage(Component.literal("  §6✦ Aldea nombrada: §e" + newName));
        player.sendSystemMessage(Component.literal("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        
        return 1;
    }

    private static String getReputationStatus(int reputation) {
        if (reputation >= 1000) return "§6HÉROE LEGENDARIO";
        if (reputation >= 800) return "§6HÉROE";
        if (reputation >= 500) return "§aCAPEÓN";
        if (reputation >= 300) return "§aAMIGO DE CONFIANZA";
        if (reputation >= 100) return "§aAMISTOSO";
        if (reputation >= 0) return "§7NEUTRAL";
        if (reputation >= -99) return "§6SOSPECHOSO";
        if (reputation >= -199) return "§cMAL VISTO";
        if (reputation >= -299) return "§cNO BIENVENIDO";
        if (reputation >= -499) return "§cPOCO AMISTOSO";
        if (reputation >= -699) return "§4HOSTIL";
        if (reputation >= -899) return "§4ENEMIGO";
        return "§4CRIMINAL BUSCADO";
    }
    
    /**
     * TEST COMMAND: Force spawn trade caravan (DISABLED)
     */
    private static int testCaravan(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("§c[System] Caravan system disabled - focusing on custom items"));
        return 0;
    }
    
    /**
     * TEST COMMAND: Force spawn war raid (DISABLED)
     */
    private static int testRaid(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("§c[System] War raid system disabled - focusing on custom items"));
        return 0;
    }
}
