package com.cesoti2006.villagediplomacy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Shown right after saving settings that only take effect on the next join. A
 * chat line was too easy to miss (and looked out of place), so the mod says it
 * in the same language as the rest of its screens.
 */
public class ReloadNoticeScreen extends Screen {

    private static final int PANEL_BG = 0xF20B0E12;
    private static final int PANEL_BORDER = 0xFF2A323C;
    private static final int WARN_ACCENT = 0xFFE3B44C;
    private static final int TEXT = 0xFFECECEC;
    private static final int TEXT_DIM = 0xFF98A2AC;

    private final Screen parent;
    private List<FormattedCharSequence> lines;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ReloadNoticeScreen(Screen parent) {
        super(Component.translatable("villagediplomacy.config.reload_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(340, width - 40);
        panelH = 132;
        panelX = width / 2 - panelW / 2;
        panelY = height / 2 - panelH / 2;
        lines = font.split(Component.translatable("villagediplomacy.config.reload_body"), panelW - 28);

        int buttonWidth = 110;
        addRenderableWidget(new NoticeButton(width / 2 - buttonWidth / 2, panelY + panelH - 32,
                buttonWidth, 20, Component.translatable("villagediplomacy.config.reload_ok"), this::close));
    }

    private void close() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xE8080A0E, 0xF0040508);

        roundedBox(graphics, panelX, panelY, panelW, panelH, PANEL_BG, PANEL_BORDER);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, WARN_ACCENT);

        graphics.drawCenteredString(font, title, width / 2, panelY + 14, WARN_ACCENT);

        int y = panelY + 34;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, width / 2, y, TEXT_DIM);
            y += 10;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font,
                Component.translatable("villagediplomacy.config.saved_hint"),
                width / 2, panelY + panelH - 48, 0xFF6FD98A);
    }

    private static void roundedBox(GuiGraphics graphics, int x, int y, int width, int height,
                                   int fill, int border) {
        fillRounded(graphics, x, y, width, height, border);
        fillRounded(graphics, x + 1, y + 1, width - 2, height - 2, fill);
    }

    private static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 2, y, x + width - 2, y + height, color);
        graphics.fill(x, y + 2, x + 2, y + height - 2, color);
        graphics.fill(x + width - 2, y + 2, x + width, y + height - 2, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    @Override
    public void onClose() {
        close();
    }

    private final class NoticeButton extends net.minecraft.client.gui.components.AbstractWidget {

        private final Runnable action;

        private NoticeButton(int x, int y, int width, int height, Component message, Runnable action) {
            super(x, y, width, height, message);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isMouseOver(mouseX, mouseY);
            roundedBox(graphics, getX(), getY(), width, height,
                    hover ? 0xFF23472F : 0xFF1B3324, hover ? 0xFF6FD98A : PANEL_BORDER);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                    hover ? 0xFF6FD98A : TEXT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
            action.run();
            return true;
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
