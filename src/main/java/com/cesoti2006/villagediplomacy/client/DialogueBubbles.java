package com.cesoti2006.villagediplomacy.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Client-side copy of the last line each villager said. The renderer turns
 * these into speech bubbles; nothing here is persisted, so a bubble simply
 * disappears when the entity does or when its time runs out.
 */
public final class DialogueBubbles {

    /** A line that was just spoken, with the accent colour of the speaker. */
    public record Bubble(String text, int accent, long spokenAt) {
    }

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();

    private DialogueBubbles() {
    }

    public static void put(int entityId, String text, int accent) {
        if (text == null || text.isBlank()) return;
        BUBBLES.put(entityId, new Bubble(text, accent, System.currentTimeMillis()));
        if (BUBBLES.size() > 256) {
            long cut = System.currentTimeMillis() - 60_000L;
            BUBBLES.entrySet().removeIf(entry -> entry.getValue().spokenAt() < cut);
        }
    }

    public static Bubble get(int entityId) {
        return BUBBLES.get(entityId);
    }

    /**
     * Turns the colour codes the server sent into real styles. Doing this before
     * splitting the text keeps the colours on every wrapped line; splitting a raw
     * string would leave the section signs visible.
     */
    public static Component styled(String text) {
        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(text.charAt(i + 1));
                if (format != null) {
                    if (buffer.length() > 0) {
                        root.append(Component.literal(buffer.toString()).withStyle(style));
                        buffer.setLength(0);
                    }
                    style = format == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(format);
                    i++;
                    continue;
                }
            }
            buffer.append(c);
        }
        if (buffer.length() > 0) {
            root.append(Component.literal(buffer.toString()).withStyle(style));
        }
        return root;
    }

    public static void clear() {
        BUBBLES.clear();
    }
}
