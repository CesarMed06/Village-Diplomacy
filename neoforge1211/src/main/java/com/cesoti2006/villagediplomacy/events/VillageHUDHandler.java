package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.util.ModLang;
import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "villagediplomacy", value = Dist.CLIENT)
public class VillageHUDHandler {

    private static int hudDisplayTimer = 0;

    private static final int HUD_DISPLAY_TICKS = 160;

    private static final int FADE_IN_TICKS = 8;
    private static final int FADE_OUT_TICKS = 25;
    private static String cachedVillageName = null;
    private static int cachedReputation = 0;
    private static String cachedRelation = null;

    public static void onPlayerEnterVillage(String villageName, int reputation, String relation) {
        hudDisplayTimer = 0;
        cachedVillageName = null;
        cachedReputation = 0;
        cachedRelation = null;
        cachedVillageName = villageName;
        cachedReputation = reputation;
        cachedRelation = relation;
        hudDisplayTimer = HUD_DISPLAY_TICKS;
    }

    public static void onPlayerLeaveVillage() {
        if (hudDisplayTimer > FADE_OUT_TICKS) {
            hudDisplayTimer = FADE_OUT_TICKS;
        }
    }

    public static void tick() {
        if (hudDisplayTimer > 0) {
            hudDisplayTimer--;
            if (hudDisplayTimer == 0) {
                cachedVillageName = null;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick();
    }

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiEvent.Post event) {
        if (hudDisplayTimer <= 0 || cachedVillageName == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen != null) {
            return;
        }

        var guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        float alpha = 1.0f;
        if (hudDisplayTimer < FADE_OUT_TICKS) {
            alpha = hudDisplayTimer / (float) FADE_OUT_TICKS;
        } else {
            int elapsed = HUD_DISPLAY_TICKS - hudDisplayTimer;
            if (elapsed < FADE_IN_TICKS) alpha = elapsed / (float) FADE_IN_TICKS;
        }
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        int alphaInt = (int) (alpha * 255);

        String relation = cachedRelation == null ? "neutral" : cachedRelation.toLowerCase();

        MutableComponent villageLine = Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("villagediplomacy.village.name_prefix")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                .append(VillageDisplayName.asComponent(cachedVillageName).copy()
                        .withStyle(ChatFormatting.GOLD));

        MutableComponent repLine = Component.translatable(
                        "villagediplomacy.hud.line_reputation", ModLang.reputationValue(cachedReputation))
                .withStyle(ChatFormatting.GRAY);

        MutableComponent relLine = Component.translatable(
                        "villagediplomacy.hud.line_relations",
                        Component.translatable("villagediplomacy.hud." + relation)
                                .withStyle(relationColor(relation)))
                .withStyle(ChatFormatting.GRAY);

        int w1 = mc.font.width(villageLine);
        int w2 = mc.font.width(repLine);
        int w3 = mc.font.width(relLine);
        int textWidth = Math.max(w1, Math.max(w2, w3));

        int left = (screenWidth - textWidth) / 2;
        int panelLeft = left - 8;
        int panelRight = left + textWidth + 8;
        int panelTop = 6;
        int panelBottom = 46;

        int panelColor = ((int) (alpha * 0x88) << 24) | 0x0E0E14;
        int borderColor = ((int) (alpha * 0xCC) << 24) | 0xC8A24A;
        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, panelColor);

        guiGraphics.fill(panelLeft + 1, panelTop, panelRight - 1, panelTop + 1, borderColor);
        guiGraphics.fill(panelLeft + 1, panelBottom - 1, panelRight - 1, panelBottom, borderColor);
        guiGraphics.fill(panelLeft, panelTop + 1, panelLeft + 1, panelBottom - 1, borderColor);
        guiGraphics.fill(panelRight - 1, panelTop + 1, panelRight, panelBottom - 1, borderColor);

        guiGraphics.fill(panelLeft + 1, panelTop + 1, panelLeft + 2, panelTop + 2, borderColor);
        guiGraphics.fill(panelRight - 2, panelTop + 1, panelRight - 1, panelTop + 2, borderColor);
        guiGraphics.fill(panelLeft + 1, panelBottom - 2, panelLeft + 2, panelBottom - 1, borderColor);
        guiGraphics.fill(panelRight - 2, panelBottom - 2, panelRight - 1, panelBottom - 1, borderColor);

        int white = (alphaInt << 24) | 0xFFFFFF;
        guiGraphics.drawString(mc.font, villageLine, left, 11, white, true);
        guiGraphics.drawString(mc.font, repLine, left, 23, white, true);
        guiGraphics.drawString(mc.font, relLine, left, 35, white, true);
    }

    private static ChatFormatting relationColor(String relation) {
        return switch (relation) {
            case "ally", "friendly" -> ChatFormatting.GREEN;
            case "neutral" -> ChatFormatting.YELLOW;
            case "suspicious" -> ChatFormatting.GOLD;
            case "hostile" -> ChatFormatting.RED;
            case "enemy" -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.YELLOW;
        };
    }
}
