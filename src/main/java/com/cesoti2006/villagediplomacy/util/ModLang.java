package com.cesoti2006.villagediplomacy.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;

import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.personality.PersonalityTrait;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler;
import com.cesoti2006.villagediplomacy.reputation.ReputationTiersHandler.ReputationTier;

public final class ModLang {

    private ModLang() {}

    private static final ChatFormatting COLOR_VILLAGER_NAME  = ChatFormatting.GOLD;
    private static final ChatFormatting COLOR_DIALOG         = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_SYSTEM         = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_REP_POSITIVE   = ChatFormatting.GREEN;
    private static final ChatFormatting COLOR_REP_NEGATIVE   = ChatFormatting.RED;
    private static final ChatFormatting COLOR_REP_TIER       = ChatFormatting.AQUA;
    private static final ChatFormatting COLOR_BRACKET        = ChatFormatting.DARK_GRAY;
    private static final ChatFormatting COLOR_NAME_INNKEEPER = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting COLOR_INNKEEPER_TAG  = ChatFormatting.AQUA;
    private static final ChatFormatting COLOR_GOLEM_TAG      = ChatFormatting.DARK_AQUA;
    private static final ChatFormatting COLOR_GOLEM_NAME     = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_PROFESSION_DEFAULT = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_PLAYER_NAME    = ChatFormatting.WHITE;

    private static final String LANG_RESOURCE = "/assets/villagediplomacy/lang/en_us.json";

    private static final java.util.Map<String, Integer> DIALOG_COUNTS = loadDialogCounts();

    private static final java.util.Set<String> VANILLA_PROFESSIONS = java.util.Set.of(
        "none", "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman",
        "fletcher", "leatherworker", "librarian", "mason", "nitwit", "shepherd",
        "toolsmith", "weaponsmith"
    );

    private static java.util.Map<String, Integer> loadDialogCounts() {
        java.util.Map<String, java.util.BitSet> indexes = new java.util.HashMap<>();
        try (java.io.InputStream in = ModLang.class.getResourceAsStream(LANG_RESOURCE)) {
            if (in != null) {
                com.google.gson.JsonObject root = com.google.gson.JsonParser
                        .parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
                for (String key : root.keySet()) {
                    int dot = key.lastIndexOf('.');
                    if (dot <= 0 || dot == key.length() - 1)
                        continue;
                    String tail = key.substring(dot + 1);
                    if (!tail.chars().allMatch(Character::isDigit))
                        continue;
                    indexes.computeIfAbsent(key.substring(0, dot), k -> new java.util.BitSet())
                            .set(Integer.parseInt(tail));
                }
            }
        } catch (Throwable ignored) {
        }
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, java.util.BitSet> entry : indexes.entrySet()) {
            java.util.BitSet bits = entry.getValue();
            int n = 0;
            while (bits.get(n))
                n++;
            if (n > 1)
                counts.put(entry.getKey(), n);
        }
        return counts;
    }

    public static int variantCount(String keyPrefix, int fallback) {
        Integer detected = DIALOG_COUNTS.get(keyPrefix);
        return detected != null ? detected : fallback;
    }

    public static int randomIndex(RandomSource random, String keyPrefix, int fallback) {
        int n = variantCount(keyPrefix, fallback);
        return n <= 0 ? 0 : random.nextInt(n);
    }

    public static String professionPath(net.minecraft.world.entity.npc.VillagerProfession profession) {
        String path = profession.toString().toLowerCase();
        path = path.contains(":") ? path.substring(path.indexOf(":") + 1) : path;
        return VANILLA_PROFESSIONS.contains(path) ? path : "none";
    }

    public static Component professionName(net.minecraft.world.entity.npc.VillagerProfession profession) {
        String path = professionPath(profession);
        if ("none".equals(path)) {
            return Component.translatable("villagediplomacy.prof.none");
        }
        return Component.translatable("entity.minecraft.villager." + path);
    }

    /**
     * Groups the sixteen vanilla professions into five flavours of small talk, so a
     * conversation sounds like the people having it instead of a generic villager.
     */
    public static String professionFamily(net.minecraft.world.entity.npc.VillagerProfession profession) {
        return switch (professionPath(profession)) {
            case "farmer", "fisherman", "shepherd", "butcher" -> "food";
            case "toolsmith", "weaponsmith", "armorer", "leatherworker" -> "craft";
            case "librarian", "cartographer", "cleric" -> "learned";
            case "mason", "fletcher" -> "builder";
            default -> "folk";
        };
    }

    /** Mood derived from a villager's personality, used to pick how they open a chat. */
    public static String moodKey(AbstractVillager villager) {
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager v)) return null;
        if (!(v.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        VillagerPersonality personality = VillagerPersonalityData.get(level).getPersonality(v.getUUID());
        if (personality == null) return null;

        PersonalityTrait temperament = personality.getTemperament();
        if (temperament == PersonalityTrait.IRRITABLE || temperament == PersonalityTrait.HOTHEADED) return "gruff";

        PersonalityTrait outlook = personality.getOutlook();
        if (outlook == PersonalityTrait.CHEERFUL || outlook == PersonalityTrait.OPTIMISTIC) return "cheerful";

        PersonalityTrait work = personality.getWorkEthic();
        if (work == PersonalityTrait.LAZY || work == PersonalityTrait.RELAXED) return "tired";

        PersonalityTrait social = personality.getSocialBehavior();
        if (social == PersonalityTrait.SHY || social == PersonalityTrait.RESERVED) return "shy";

        PersonalityTrait courage = personality.getCourage();
        if (courage == PersonalityTrait.BRAVE || courage == PersonalityTrait.FEARLESS) return "proud";

        return null;
    }

    public static String randomLineKey(RandomSource random, String keyPrefix, int fallback) {
        int n = variantCount(keyPrefix, fallback);
        if (n <= 0) return null;
        return keyPrefix + "." + random.nextInt(n);
    }

    public static void sendTradePitch(ServerPlayer player, RandomSource random,
                                      net.minecraft.world.entity.npc.Villager villager) {
        String key = "villagediplomacy.trade.pitch."
                + professionPath(villager.getVillagerData().getProfession());
        if (variantCount(key, 0) > 0) {
            int i = random.nextInt(variantCount(key, 0));
            sendDialog(player, villager, key + "." + i);
        } else {
            sendDialogRandom(player, random, villager, "villagediplomacy.dialog.trade", 11);
        }
    }

    public static ChatFormatting professionColor(net.minecraft.world.entity.npc.VillagerProfession profession) {
        return switch (professionPath(profession)) {
            case "farmer" -> ChatFormatting.GREEN;
            case "fisherman" -> ChatFormatting.AQUA;
            case "shepherd" -> ChatFormatting.WHITE;
            case "fletcher" -> ChatFormatting.YELLOW;
            case "librarian" -> ChatFormatting.BLUE;
            case "cartographer" -> ChatFormatting.DARK_AQUA;
            case "cleric" -> ChatFormatting.DARK_PURPLE;
            case "armorer" -> ChatFormatting.DARK_GRAY;
            case "weaponsmith" -> ChatFormatting.DARK_RED;
            case "toolsmith" -> ChatFormatting.RED;
            case "butcher" -> ChatFormatting.RED;
            case "leatherworker" -> ChatFormatting.DARK_GREEN;
            case "mason" -> ChatFormatting.GRAY;
            case "nitwit" -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }

    public static String plainName(AbstractVillager villager) {
        if (villager == null) return null;

        if (villager.level() instanceof net.minecraft.server.level.ServerLevel level) {
            VillagerPersonality personality = VillagerPersonalityData.get(level)
                    .getPersonality(villager.getUUID());
            if (personality != null) {
                String name = stripFormatting(personality.getCustomName()).trim();
                if (!name.isEmpty()) return name;
            }
        }

        if (villager.hasCustomName()) {
            String name = stripFormatting(villager.getCustomName().getString()).trim();
            if (!name.isEmpty()) return name;
        }
        return null;
    }

    private static boolean isInnkeeper(AbstractVillager villager) {
        if (!(villager instanceof net.minecraft.world.entity.npc.Villager v)) return false;
        if (!(v.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        return InnkeeperHelper.isInnkeeper(level, v);
    }

    public static MutableComponent displayName(AbstractVillager villager) {
        String name = plainName(villager);
        if (name != null) {
            return Component.literal(name)
                    .withStyle(isInnkeeper(villager) ? COLOR_NAME_INNKEEPER : COLOR_VILLAGER_NAME);
        }
        if (villager instanceof net.minecraft.world.entity.npc.Villager v) {
            return professionName(v.getVillagerData().getProfession())
                    .copy().withStyle(COLOR_VILLAGER_NAME);
        }
        if (villager instanceof WanderingTrader) {
            return Component.translatable("entity.minecraft.wandering_trader")
                    .withStyle(COLOR_VILLAGER_NAME);
        }
        return Component.translatable("entity.minecraft.villager.none").withStyle(COLOR_VILLAGER_NAME);
    }

    public static MutableComponent golemName(IronGolem golem) {
        String[] numerals = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX" };
        String numeral = numerals[Math.floorMod(golem.getUUID().hashCode(), numerals.length)];
        return Component.translatable("villagediplomacy.golem.name", Component.literal(numeral))
                .withStyle(COLOR_GOLEM_NAME);
    }

    public static MutableComponent golemTag(IronGolem golem) {
        return Component.literal("[").withStyle(COLOR_BRACKET)
                .append(Component.translatable("villagediplomacy.tag.golem").withStyle(COLOR_GOLEM_TAG))
                .append(Component.literal("] ").withStyle(COLOR_BRACKET))
                .append(golemName(golem));
    }

    public static MutableComponent entityTag(Entity entity) {
        if (entity instanceof AbstractVillager villager) return villagerTag(villager);
        if (entity instanceof IronGolem golem) return golemTag(golem);
        return Component.literal("?").withStyle(COLOR_BRACKET);
    }

    public static void sendEntityChatLine(ServerPlayer player, Entity speaker, Entity listener,
                                          String key, Object... args) {
        MutableComponent msg = entityTag(speaker).copy();
        if (listener != null) {
            msg.append(Component.literal(" \u2192 ").withStyle(COLOR_BRACKET))
               .append(entityTag(listener));
        }
        msg.append(Component.literal(": ").withStyle(COLOR_BRACKET))
           .append(Component.translatable(key, args).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static MutableComponent playerDisplayName(ServerPlayer player) {
        return Component.literal(player.getGameProfile().getName()).withStyle(COLOR_PLAYER_NAME);
    }

    public static MutableComponent villagerTag(AbstractVillager villager) {
        if (villager == null) {
            return Component.literal("[").withStyle(COLOR_BRACKET)
                    .append(Component.translatable("entity.minecraft.villager.none").withStyle(COLOR_VILLAGER_NAME))
                    .append(Component.literal("]").withStyle(COLOR_BRACKET));
        }

        boolean innkeeper = isInnkeeper(villager);
        Component label;
        ChatFormatting labelColor;

        if (innkeeper) {
            label = Component.translatable("villagediplomacy.tag.innkeeper");
            labelColor = COLOR_INNKEEPER_TAG;
        } else if (villager instanceof net.minecraft.world.entity.npc.Villager v) {
            label = professionName(v.getVillagerData().getProfession());
            labelColor = professionColor(v.getVillagerData().getProfession());
        } else if (villager instanceof WanderingTrader) {
            label = Component.translatable("entity.minecraft.wandering_trader");
            labelColor = ChatFormatting.LIGHT_PURPLE;
        } else {
            label = Component.translatable("entity.minecraft.villager.none");
            labelColor = COLOR_PROFESSION_DEFAULT;
        }

        MutableComponent tag = Component.literal("[").withStyle(COLOR_BRACKET)
                .append(label.copy().withStyle(labelColor))
                .append(Component.literal("]").withStyle(COLOR_BRACKET));

        String name = plainName(villager);
        if (name != null) {
            tag.append(Component.literal(" "))
                    .append(Component.literal(name)
                            .withStyle(innkeeper ? COLOR_NAME_INNKEEPER : COLOR_VILLAGER_NAME));
        }
        return tag;
    }

    public static MutableComponent villagerName(AbstractVillager villager) {
        return displayName(villager);
    }

    public static MutableComponent villagerPrefix(AbstractVillager villager) {
        return villagerTag(villager).copy()
            .append(Component.literal(": ").withStyle(COLOR_BRACKET));
    }

    public static MutableComponent namedPrefix(String name) {
        return Component.literal("[").withStyle(COLOR_BRACKET)
            .append(nameComponent(name))
            .append(Component.literal("] ").withStyle(COLOR_BRACKET));
    }

    public static MutableComponent namedPrefixClean(String name) {
        return nameComponent(name).append(Component.literal(" "));
    }

    public static MutableComponent nameComponent(String name) {
        return coloredName(name, COLOR_VILLAGER_NAME);
    }

    public static MutableComponent coloredName(String name, ChatFormatting color) {
        return Component.literal(stripFormatting(name)).withStyle(color);
    }

    public static MutableComponent villageName(String stored) {
        return VillageDisplayName.asComponent(stored).copy().withStyle(COLOR_VILLAGER_NAME);
    }

    public static MutableComponent reputationValue(int value) {
        return Component.literal((value > 0 ? "+" : "") + value)
                .withStyle(value < 0 ? COLOR_REP_NEGATIVE : COLOR_REP_POSITIVE);
    }

    public static MutableComponent tierName(int reputation) {
        return Component.translatable(repStatusKey(reputation)).withStyle(COLOR_REP_TIER);
    }

    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()
                    && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(text.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            cleaned.append(c);
        }
        return cleaned.toString();
    }

    public static MutableComponent genericVillagerPrefix() {
        return villagerTag(null).copy()
            .append(Component.literal(": ").withStyle(COLOR_BRACKET));
    }

    public static net.minecraft.world.entity.npc.Villager nearestVillager(ServerPlayer player, String keyPrefix) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return null;
        }
        boolean wantBaby = keyPrefix.contains(".baby");
        net.minecraft.world.entity.npc.Villager fallback = null;
        for (net.minecraft.world.entity.npc.Villager villager : level.getEntitiesOfClass(
                net.minecraft.world.entity.npc.Villager.class, player.getBoundingBox().inflate(16.0))) {
            if (villager.isBaby() == wantBaby) {
                return villager;
            }
            if (fallback == null) {
                fallback = villager;
            }
        }
        return fallback;
    }

    public static void sendVillagerLine(ServerPlayer player, RandomSource random, AbstractVillager speaker,
                                        String keyPrefix, int count, Object... args) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        AbstractVillager who = speaker != null ? speaker : nearestVillager(player, keyPrefix);
        MutableComponent prefix = who != null ? villagerPrefix(who) : genericVillagerPrefix();
        player.sendSystemMessage(prefix.append(
            Component.translatable(keyPrefix + "." + i, args).withStyle(COLOR_DIALOG)));
    }

    public static void sendVillagerRandom(ServerPlayer player, RandomSource random,
                                          String keyPrefix, int count) {
        sendVillagerLine(player, random, null, keyPrefix, count);
    }

    public static void send(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(
            Component.translatable(key, args).withStyle(COLOR_SYSTEM)
        );
    }

    public static void sendRaw(ServerPlayer player, MutableComponent component) {
        player.sendSystemMessage(component);
    }

    public static void sendDialog(ServerPlayer player, AbstractVillager villager, String key, Object... args) {
        MutableComponent msg = villagerPrefix(villager)
            .append(Component.translatable(key, args).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static void sendChatLine(ServerPlayer player, AbstractVillager speaker, AbstractVillager listener,
                                    String key, Object... args) {
        MutableComponent msg = villagerTag(speaker).copy();
        if (listener != null) {
            msg.append(Component.literal(" \u2192 ").withStyle(COLOR_BRACKET))
                .append(villagerTag(listener));
        }
        msg.append(Component.literal(": ").withStyle(COLOR_BRACKET))
            .append(Component.translatable(key, args).withStyle(COLOR_DIALOG));
        player.sendSystemMessage(msg);
    }

    public static void sendDialogVillager(ServerPlayer player, AbstractVillager villager, String key, Object... args) {
        player.sendSystemMessage(villagerPrefix(villager)
            .append(Component.translatable(key, args).withStyle(COLOR_DIALOG)));
    }

    public static void sendDialogRandom(ServerPlayer player, RandomSource random,
                                         AbstractVillager villager, String keyPrefix, int count) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        sendDialog(player, villager, keyPrefix + "." + i);
    }

    public static void sendDialogRandomWithArgs(ServerPlayer player, RandomSource random,
                                                 AbstractVillager villager, String keyPrefix, int count,
                                                 Object... args) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
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
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        sendDialogNamed(player, name, keyPrefix + "." + i);
    }

    public static void sendRandom(ServerPlayer player, RandomSource random, String keyPrefix, int count) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        player.sendSystemMessage(
            Component.translatable(keyPrefix + "." + i).withStyle(COLOR_DIALOG)
        );
    }

    public static void sendRandomWithArgs(ServerPlayer player, RandomSource random,
                                           String keyPrefix, int count, Object... args) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        player.sendSystemMessage(
            Component.translatable(keyPrefix + "." + i, args).withStyle(COLOR_DIALOG)
        );
    }

    public static void sendReputationSummary(ServerPlayer player, int delta, int newTotal) {
        ChatFormatting direction = delta < 0 ? COLOR_REP_NEGATIVE : COLOR_REP_POSITIVE;

        MutableComponent deltaC = Component.literal((delta > 0 ? "+" : "") + delta)
            .withStyle(direction, ChatFormatting.BOLD);
        MutableComponent totalC = Component.literal(String.valueOf(newTotal))
            .withStyle(ChatFormatting.WHITE);
        MutableComponent tierC  = repStatus(newTotal).withStyle(COLOR_REP_TIER);

        MutableComponent body = Component
            .translatable("villagediplomacy.sys.reputation_body", deltaC, totalC, tierC)
            .withStyle(direction);

        player.sendSystemMessage(
            Component.translatable("villagediplomacy.sys.reputation_summary", body)
        );
    }

    public static String repStatusKey(int reputation) {
        return ReputationTiersHandler.getTierByReputation(reputation).getTranslationKey();
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
        int min = ReputationTiersHandler.getTierByReputation(reputation).getMinReputation();
        if (min >= 300)  return "ally";
        if (min >= 100)  return "friendly";
        if (min >= 0)    return "neutral";
        if (min >= -200) return "suspicious";
        if (min >= -700) return "hostile";
        return "enemy";
    }
}
