package com.cesoti2006.villagediplomacy.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;

public final class ModLang {

    private ModLang() {}

    // ── Colores centralizados ──────────────────────────────────────────────────

    private static final ChatFormatting COLOR_VILLAGER_NAME  = ChatFormatting.GOLD;
    private static final ChatFormatting COLOR_DIALOG         = ChatFormatting.YELLOW;
    private static final ChatFormatting COLOR_SYSTEM         = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_REP_POSITIVE   = ChatFormatting.GREEN;
    private static final ChatFormatting COLOR_REP_NEGATIVE   = ChatFormatting.RED;
    private static final ChatFormatting COLOR_REP_TIER       = ChatFormatting.AQUA;
    private static final ChatFormatting COLOR_BRACKET        = ChatFormatting.DARK_GRAY;

    // ── Prefijo [Nombre] ───────────────────────────────────────────────────────

    private static final java.util.Map<String, String> PROF_ES = java.util.Map.ofEntries(
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
        java.util.Map.entry("mason", "Cantero"),
        java.util.Map.entry("nitwit", "Aldeano")
    );

    public static MutableComponent villagerPrefix(AbstractVillager villager) {
        String name;
        if (villager.hasCustomName()) {
            name = villager.getCustomName().getString();
        } else if (villager instanceof net.minecraft.world.entity.npc.Villager v) {
            String prof = v.getVillagerData().getProfession().toString().toLowerCase();
            prof = prof.contains(":") ? prof.substring(prof.indexOf(":") + 1) : prof;
            name = PROF_ES.getOrDefault(prof, "Aldeano");
        } else {
            name = "Aldeano";
        }
        return Component.literal("[").withStyle(COLOR_BRACKET)
            .append(Component.literal(name).withStyle(COLOR_VILLAGER_NAME))
            .append(Component.literal("] ").withStyle(COLOR_BRACKET));
    }

    public static MutableComponent namedPrefix(String name) {
        return Component.literal("[")
            .withStyle(COLOR_BRACKET)
            .append(Component.literal(name).withStyle(COLOR_VILLAGER_NAME))
            .append(Component.literal("] ").withStyle(COLOR_BRACKET));
    }

    // ── Envío básico ──────────────────────────────────────────────────────────

    public static void send(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(
            Component.translatable(key, args).withStyle(COLOR_SYSTEM)
        );
    }

    public static void sendRaw(ServerPlayer player, MutableComponent component) {
        player.sendSystemMessage(component);
    }

    // ── Diálogo de aldeano (con prefijo [Nombre]) ──────────────────────────────

    public static void sendDialog(ServerPlayer player, AbstractVillager villager, String key) {
        MutableComponent msg = villagerPrefix(villager)
            .append(Component.translatable(key).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static void sendDialogRandom(ServerPlayer player, RandomSource random,
                                         AbstractVillager villager, String keyPrefix, int count) {
        if (count <= 0) return;
        int i = random.nextInt(count);
        sendDialog(player, villager, keyPrefix + "." + i);
    }

    public static void sendDialogRandomWithArgs(ServerPlayer player, RandomSource random,
                                                 AbstractVillager villager, String keyPrefix, int count,
                                                 Object... args) {
        if (count <= 0) return;
        int i = random.nextInt(count);
        MutableComponent msg = villagerPrefix(villager)
            .append(Component.translatable(keyPrefix + "." + i, args).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static void sendDialogNamed(ServerPlayer player, String name, String key, Object... args) {
        MutableComponent msg = namedPrefix(name)
            .append(Component.translatable(key, args).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static void sendDialogNamedRandom(ServerPlayer player, RandomSource random,
                                              String name, String keyPrefix, int count) {
        if (count <= 0) return;
        int i = random.nextInt(count);
        sendDialogNamed(player, name, keyPrefix + "." + i);
    }

    // ── Líneas del sistema (grises, sin prefijo aldeano) ──────────────────────

    public static void sendRandom(ServerPlayer player, RandomSource random, String keyPrefix, int count) {
        if (count <= 0) return;
        int i = random.nextInt(count);
        player.sendSystemMessage(
            Component.translatable(keyPrefix + "." + i).withStyle(COLOR_DIALOG)
        );
    }

    public static void sendRandomWithArgs(ServerPlayer player, RandomSource random,
                                           String keyPrefix, int count, Object... args) {
        if (count <= 0) return;
        int i = random.nextInt(count);
        player.sendSystemMessage(
            Component.translatable(keyPrefix + "." + i, args).withStyle(COLOR_DIALOG)
        );
    }

    // ── Resumen de reputación ──────────────────────────────────────────────────

    public static void sendReputationSummary(ServerPlayer player, int delta, int newTotal) {
        MutableComponent deltaC = Component.literal((delta >= 0 ? "+" : "") + delta)
            .withStyle(delta < 0 ? COLOR_REP_NEGATIVE : COLOR_REP_POSITIVE);
        MutableComponent totalC = Component.literal(String.valueOf(newTotal))
            .withStyle(COLOR_REP_TIER);
        MutableComponent tierC  = repStatus(newTotal).withStyle(COLOR_REP_TIER);

        player.sendSystemMessage(
            Component.translatable("villagediplomacy.sys.reputation_summary", deltaC, totalC, tierC)
                .withStyle(COLOR_SYSTEM)
        );
    }

    // ── Estado de reputación ──────────────────────────────────────────────────

    public static String repStatusKey(int reputation) {
        if (reputation >= 1000) return "villagediplomacy.rep.legendary_hero";
        if (reputation >= 800)  return "villagediplomacy.rep.hero";
        if (reputation >= 500)  return "villagediplomacy.rep.champion";
        if (reputation >= 300)  return "villagediplomacy.rep.trusted_friend";
        if (reputation >= 100)  return "villagediplomacy.rep.friendly";
        if (reputation >= 0)    return "villagediplomacy.rep.neutral";
        if (reputation >= -99)  return "villagediplomacy.rep.suspicious";
        if (reputation >= -199) return "villagediplomacy.rep.disliked";
        if (reputation >= -299) return "villagediplomacy.rep.unwelcome";
        if (reputation >= -499) return "villagediplomacy.rep.unfriendly";
        if (reputation >= -699) return "villagediplomacy.rep.hostile";
        if (reputation >= -899) return "villagediplomacy.rep.enemy";
        return "villagediplomacy.rep.wanted_criminal";
    }

    public static MutableComponent repStatus(int reputation) {
        return Component.translatable(repStatusKey(reputation));
    }

    public static String repTier(int reputation) {
        if (reputation >= 300) return "warm";
        if (reputation >= 0)   return "neutral";
        return "cold";
    }

    public static String hudRelationKey(int reputation) {
        if (reputation >= 300)  return "ally";
        if (reputation >= 100)  return "friendly";
        if (reputation >= -99)  return "neutral";
        if (reputation >= -399) return "hostile";
        return "enemy";
    }
}
