package com.cesoti2006.villagediplomacy.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Server-side chat using translation keys so the client renders in the player's language.
 */
public final class ModLang {

    private ModLang() {
    }

    public static void send(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args));
    }

    public static void sendRandom(ServerPlayer player, RandomSource random, String keyPrefix, int count) {
        if (count <= 0) {
            return;
        }
        int i = random.nextInt(count);
        player.sendSystemMessage(Component.translatable(keyPrefix + "." + i));
    }

    /** Random line from `keyPrefix.0` … `keyPrefix.{count-1}` with format args (e.g. %s for name). */
    public static void sendRandomWithArgs(ServerPlayer player, RandomSource random, String keyPrefix, int count,
            Object... args) {
        if (count <= 0) {
            return;
        }
        int i = random.nextInt(count);
        player.sendSystemMessage(Component.translatable(keyPrefix + "." + i, args));
    }

    /** Reputation tier translation key (no color codes — use with styled parents if needed). */
    public static String repStatusKey(int reputation) {
        if (reputation >= 1000) {
            return "villagediplomacy.rep.legendary_hero";
        }
        if (reputation >= 800) {
            return "villagediplomacy.rep.hero";
        }
        if (reputation >= 500) {
            return "villagediplomacy.rep.champion";
        }
        if (reputation >= 300) {
            return "villagediplomacy.rep.trusted_friend";
        }
        if (reputation >= 100) {
            return "villagediplomacy.rep.friendly";
        }
        if (reputation >= 0) {
            return "villagediplomacy.rep.neutral";
        }
        if (reputation >= -99) {
            return "villagediplomacy.rep.suspicious";
        }
        if (reputation >= -199) {
            return "villagediplomacy.rep.disliked";
        }
        if (reputation >= -299) {
            return "villagediplomacy.rep.unwelcome";
        }
        if (reputation >= -499) {
            return "villagediplomacy.rep.unfriendly";
        }
        if (reputation >= -699) {
            return "villagediplomacy.rep.hostile";
        }
        if (reputation >= -899) {
            return "villagediplomacy.rep.enemy";
        }
        return "villagediplomacy.rep.wanted_criminal";
    }

    public static Component repStatus(int reputation) {
        return Component.translatable(repStatusKey(reputation));
    }

    /** Chat line: "Reputation {delta} (Total: {total} — {tier})" in the player's language. */
    public static void sendReputationSummary(ServerPlayer player, int reputationDelta, int newTotal) {
        MutableComponent deltaC = Component.literal(String.valueOf(reputationDelta))
                .withStyle(reputationDelta < 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
        player.sendSystemMessage(Component.translatable("villagediplomacy.sys.reputation_summary",
                deltaC,
                Component.literal(String.valueOf(newTotal)),
                repStatus(newTotal)));
    }

    /** HUD / relation line: ally, friendly, neutral, hostile, enemy */
    public static String hudRelationKey(int reputation) {
        if (reputation >= 300) {
            return "ally";
        }
        if (reputation >= 100) {
            return "friendly";
        }
        if (reputation >= -99) {
            return "neutral";
        }
        if (reputation >= -399) {
            return "hostile";
        }
        return "enemy";
    }
}
