
package com.cesoti2006.villagediplomacy.events;

import com.cesoti2006.villagediplomacy.util.VillageDisplayName;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "villagediplomacy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillageHUDHandler {

    private static int hudDisplayTimer = 0;
    private static final int HUD_DISPLAY_TICKS = 100;
    private static String cachedVillageName = null;
    private static int cachedReputation = 0;
    private static String cachedRelation = null;

    public static void onPlayerEnterVillage(String villageName, int reputation, String relation) {
        cachedVillageName = villageName;
        cachedReputation = reputation;
        cachedRelation = relation;
        hudDisplayTimer = HUD_DISPLAY_TICKS;
    }

    public static void tick() {
        if (hudDisplayTimer > 0) {
            hudDisplayTimer--;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tick();
        }
    }

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
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
        if (hudDisplayTimer < 20) {
            alpha = hudDisplayTimer / 20.0f;
        }
        int alphaInt = (int) (alpha * 255);
        int white = (alphaInt << 24) | 0xFFFFFF;

        Component villageLine = Component.translatable(
                "villagediplomacy.hud.line_village",
                Component.translatable("villagediplomacy.village.name_prefix"),
                VillageDisplayName.asComponent(cachedVillageName));
        Component repLine = Component.translatable("villagediplomacy.hud.line_reputation", cachedReputation);
        String relSub = cachedRelation == null ? "neutral" : cachedRelation.toLowerCase();
        Component relLine = Component.translatable(
                "villagediplomacy.hud.line_relations",
                Component.translatable("villagediplomacy.hud." + relSub));

        int x1 = (screenWidth - mc.font.width(villageLine)) / 2;
        int x2 = (screenWidth - mc.font.width(repLine)) / 2;
        int x3 = (screenWidth - mc.font.width(relLine)) / 2;

        guiGraphics.drawString(mc.font, villageLine, x1, 12, white, true);
        guiGraphics.drawString(mc.font, repLine, x2, 24, white, true);
        guiGraphics.drawString(mc.font, relLine, x3, 36, white, true);
    }
}
