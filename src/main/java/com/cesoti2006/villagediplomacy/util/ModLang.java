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

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.personality.NameGenerator;
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
    private static final ChatFormatting COLOR_NAME_INNKEEPER = ChatFormatting.WHITE;
    private static final ChatFormatting COLOR_INNKEEPER_TAG  = ChatFormatting.GOLD;
    private static final ChatFormatting COLOR_GOLEM_TAG      = ChatFormatting.DARK_AQUA;
    private static final ChatFormatting COLOR_GOLEM_NAME     = ChatFormatting.AQUA;
    private static final ChatFormatting COLOR_PROFESSION_DEFAULT = ChatFormatting.GRAY;
    private static final ChatFormatting COLOR_PLAYER_NAME    = ChatFormatting.WHITE;

    /** Bright colours only: every neighbour gets one so they are easy to tell apart. */
    private static final ChatFormatting[] NAME_PALETTE = {
        ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
        ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.WHITE
    };

    private static final String LANG_RESOURCE = "/assets/villagediplomacy/lang/en_us.json";

    /**
     * Words worth colouring inside a dialogue line. It covers the four bundled
     * languages, so the same chat filters a Spanish, English, Portuguese or
     * Russian line without needing per-language keys.
     */
    private static final java.util.regex.Pattern HIGHLIGHT = java.util.regex.Pattern.compile(
        "(?iu)\\b(peligro|cuidado|atenci[o\u00f3]n|aten[c\u00e7][a\u00e3]o|danger|careful|\u043e\u0441\u0442\u043e\u0440\u043e\u0436\u043d\u043e|\u043e\u043f\u0430\u0441\u043d\u043e"
        + "|monstruo|zombi|zombie|zombis|esqueleto|skeleton|saqueador|pillager|bruja|witch|creeper"
        + "|ara[n\u00f1]a|spider|slime|enderman|fantasma|phantom|no-?muerto|undead|asalto|raid"
        + "|esmeralda|esmeraldas|emerald|emeralds|\u0438\u0437\u0443\u043c\u0440\u0443\u0434"
        + "|aldea|aldeanos|village|villagers|plaza|campana|bell"
        + "|noche|night|tormenta|storm|lluvia|rain"
        + "|hambre|oro|gold|hierro|iron)\\b");

    private static final java.util.Map<String, String> HIGHLIGHT_COLORS = buildHighlightColors();

    private static java.util.Map<String, String> buildHighlightColors() {
        java.util.Map<String, String> colors = new java.util.HashMap<>();
        for (String word : new String[]{ "peligro", "cuidado", "atenci\u00f3n", "aten\u00e7\u00e3o", "danger",
                "careful", "\u043e\u0441\u0442\u043e\u0440\u043e\u0436\u043d\u043e", "\u043e\u043f\u0430\u0441\u043d\u043e",
                "monstruo", "zombi", "zombie", "zombis", "esqueleto", "skeleton", "saqueador", "pillager",
                "bruja", "witch", "creeper", "ara\u00f1a", "ara\u00f1a", "spider", "slime", "enderman",
                "fantasma", "phantom", "no-muerto", "nomuerto", "undead", "asalto", "raid" }) {
            colors.put(word, "c");
        }
        for (String word : new String[]{ "esmeralda", "esmeraldas", "emerald", "emeralds", "\u0438\u0437\u0443\u043c\u0440\u0443\u0434" }) {
            colors.put(word, "a");
        }
        for (String word : new String[]{ "aldea", "aldeanos", "village", "villagers", "plaza", "campana", "bell" }) {
            colors.put(word, "6");
        }
        for (String word : new String[]{ "noche", "night", "tormenta", "storm", "lluvia", "rain" }) {
            colors.put(word, "b");
        }
        for (String word : new String[]{ "hambre", "oro", "gold", "hierro", "iron" }) {
            colors.put(word, "e");
        }
        return colors;
    }

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

        // Neutral traits (the unemployed included) still deserve a mood line
        // under the name instead of an empty gap.
        return "calm";
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

    /** A stable colour per villager, so the same neighbour always looks the same. */
    public static ChatFormatting nameColor(java.util.UUID uuid) {
        return NAME_PALETTE[Math.floorMod(uuid.hashCode(), NAME_PALETTE.length)];
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
                    .withStyle(isInnkeeper(villager) ? COLOR_NAME_INNKEEPER : nameColor(villager.getUUID()));
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

    /**
     * Iron golems are people too: they get a real name from the same generator as
     * the villagers instead of a roman numeral, and it is derived from the UUID so
     * every player sees the same golem called the same thing.
     */
    public static MutableComponent golemName(IronGolem golem) {
        String biome = "plains";
        if (golem.level() != null) {
            biome = golem.level().getBiome(golem.blockPosition()).unwrapKey()
                    .map(key -> key.location().getPath()).orElse("plains");
        }
        boolean male = (golem.getUUID().getLeastSignificantBits() & 1L) == 0L;
        String[] names = NameGenerator.namesFor(biome, male);
        String name = names[Math.floorMod(golem.getUUID().hashCode(), names.length)];
        return Component.literal(name).withStyle(COLOR_GOLEM_NAME);
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
        String body = dialogueBody(key, args);
        msg.append(Component.literal(": ").withStyle(COLOR_BRACKET))
           .append(Component.literal(body).withStyle(COLOR_DIALOG));
        deliver(player, speaker, msg, body);
    }

    /**
     * Builds the text of a line: section signs that old language files carry are
     * removed so every line looks the same, then a few meaningful words get their
     * own colour. This is what keeps one villager from printing a yellow line in
     * the middle of an otherwise grey conversation.
     */
    public static String dialogueBody(String key, Object... args) {
        return highlight(stripFormatting(Component.translatable(key, args).getString()));
    }

    public static String highlight(String text) {
        if (text == null || text.isEmpty()) return "";
        java.util.regex.Matcher matcher = HIGHLIGHT.matcher(text);
        StringBuilder builder = new StringBuilder(text.length() + 32);
        while (matcher.find()) {
            String word = matcher.group();
            String color = HIGHLIGHT_COLORS.get(word.toLowerCase(java.util.Locale.ROOT));
            if (color == null) {
                matcher.appendReplacement(builder, java.util.regex.Matcher.quoteReplacement(word));
            } else {
                matcher.appendReplacement(builder, java.util.regex.Matcher.quoteReplacement(
                        "\u00a7" + color + word + "\u00a77"));
            }
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * Sends a dialogue line to its chat box and/or as a speech bubble. If the
     * player turned both outputs off, chat is still used so no information is
     * silently lost.
     */
    private static void deliver(ServerPlayer player, Entity speaker, MutableComponent line, String body) {
        VillageDiplomacyConfig config = VillageDiplomacyConfig.INSTANCE;
        if (config.showDialogueInChat.get()) {
            player.sendSystemMessage(line);
        }
        // Bubbles are always sent: each player chooses in their own settings
        // whether they want them, so one person turning them off cannot mute the
        // village for everyone on the server.
        if (speaker != null) {
            VillageDiplomacyNetwork.sendDialogueBubble(player, speaker.getId(), body, bubbleAccent(speaker));
        }
    }

    private static int bubbleAccent(Entity speaker) {
        ChatFormatting color = null;
        if (speaker instanceof AbstractVillager villager) {
            color = isInnkeeper(villager) ? COLOR_NAME_INNKEEPER : nameColor(villager.getUUID());
        } else if (speaker instanceof IronGolem) {
            color = COLOR_GOLEM_TAG;
        }
        Integer rgb = color == null ? null : color.getColor();
        return rgb == null ? 0xFFE3B44C : (0xFF000000 | rgb);
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
                            .withStyle(innkeeper ? COLOR_NAME_INNKEEPER : nameColor(villager.getUUID())));
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
        String body = dialogueBody(keyPrefix + "." + i, args);
        deliver(player, who, prefix.append(
            Component.literal(body).withStyle(COLOR_DIALOG)), body);
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
        String body = dialogueBody(key, args);
        MutableComponent msg = villagerPrefix(villager)
            .append(Component.literal(body).withStyle(COLOR_DIALOG));
        deliver(player, villager, msg, body);
    }

    public static void sendChatLine(ServerPlayer player, AbstractVillager speaker, AbstractVillager listener,
                                    String key, Object... args) {
        MutableComponent msg = villagerTag(speaker).copy();
        if (listener != null) {
            msg.append(Component.literal(" \u2192 ").withStyle(COLOR_BRACKET))
                .append(villagerTag(listener));
        }
        String body = dialogueBody(key, args);
        msg.append(Component.literal(": ").withStyle(COLOR_BRACKET))
            .append(Component.literal(body).withStyle(COLOR_DIALOG));
        deliver(player, speaker, msg, body);
    }

    public static void sendDialogVillager(ServerPlayer player, AbstractVillager villager, String key, Object... args) {
        String body = dialogueBody(key, args);
        deliver(player, villager, villagerPrefix(villager)
            .append(Component.literal(body).withStyle(COLOR_DIALOG)), body);
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
        String body = dialogueBody(keyPrefix + "." + i, args);
        MutableComponent msg = villagerPrefix(villager)
            .append(Component.literal(body).withStyle(COLOR_DIALOG));
        deliver(player, villager, msg, body);
    }

    public static void sendDialogNamed(ServerPlayer player, String name, String key, Object... args) {
        String body = dialogueBody(key, args);
        MutableComponent msg = namedPrefix(name)
            .append(Component.literal(body).withStyle(COLOR_DIALOG));
        deliver(player, null, msg, body);
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
            Component.literal(dialogueBody(keyPrefix + "." + i)).withStyle(COLOR_DIALOG)
        );
    }

    public static void sendRandomWithArgs(ServerPlayer player, RandomSource random,
                                           String keyPrefix, int count, Object... args) {
        int n = variantCount(keyPrefix, count);
        if (n <= 0) return;
        int i = random.nextInt(n);
        player.sendSystemMessage(
            Component.literal(dialogueBody(keyPrefix + "." + i, args)).withStyle(COLOR_DIALOG)
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
