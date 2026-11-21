package com.cesoti2006.villagediplomacy.client;

import java.util.List;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * In-game settings screen. Also serves as the config screen of the mods list,
 * so the "Config" button is no longer greyed out.
 *
 * Everything here is drawn by the mod itself (no textures, no extra assets), so
 * the screen keeps a dark, coloured look instead of the default grey buttons.
 */
public class VillageConfigScreen extends Screen {

    private static final String[] TAB_KEYS = {
            "villagediplomacy.config.tab.life",
            "villagediplomacy.config.tab.village",
            "villagediplomacy.config.tab.systems",
            "villagediplomacy.config.tab.reputation"
    };

    private static final int[] TAB_ACCENTS = {
            0xFF5FCB7E, 0xFFE0B040, 0xFF4FC3D9, 0xFFB07CE8
    };

    private static final int ROW_H = 22;
    private static final int ROW_GAP = 3;

    private static final int PANEL_BG = 0xF00E1014;
    private static final int PANEL_BORDER = 0xFF2A313B;
    private static final int ROW_BG = 0xFF191D24;
    private static final int ROW_BG_HOVER = 0xFF212733;
    private static final int ROW_BORDER = 0xFF2C333D;
    private static final int TEXT = 0xFFEDEDED;
    private static final int TEXT_DIM = 0xFF98A0A8;
    private static final int SAVE_ACCENT = 0xFF5FCB7E;

    private final Screen parent;
    private int tab = 0;
    private long savedUntil = 0L;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public VillageConfigScreen(Screen parent) {
        super(Component.translatable("villagediplomacy.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();

        panelW = 380;
        panelX = width / 2 - panelW / 2;
        panelY = 66;
        panelH = Math.max(148, height - panelY - 56);

        int tabWidth = 96;
        int tabGap = 6;
        int tabsX = width / 2 - (tabWidth * 4 + tabGap * 3) / 2;
        for (int i = 0; i < 4; i++) {
            addRenderableWidget(new ProTab(tabsX + i * (tabWidth + tabGap), 40, tabWidth, 20, i));
        }

        if (VillageDiplomacyConfig.SPEC.isLoaded()) {
            List<Option> options = optionsFor(tab);
            int rowX = panelX + 12;
            int rowWidth = panelW - 24;
            int rowY = panelY + 12;
            for (int i = 0; i < options.size(); i++) {
                addRenderableWidget(new ProRow(rowX, rowY + i * (ROW_H + ROW_GAP), rowWidth, ROW_H,
                        options.get(i), TAB_ACCENTS[tab]));
            }
        }

        addRenderableWidget(new ProButton(width / 2 - 104, height - 40, 100, 22,
                Component.translatable("villagediplomacy.config.save"), true));
        addRenderableWidget(new ProButton(width / 2 + 4, height - 40, 100, 22,
                CommonComponents.GUI_DONE, false));
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private static List<Option> optionsFor(int tab) {
        VillageDiplomacyConfig config = VillageDiplomacyConfig.INSTANCE;
        return switch (tab) {
            case 0 -> List.of(
                    Option.toggle("enableVillageLife", Items.BELL, config.enableVillageLife),
                    Option.toggle("enableBellRhythm", Items.CLOCK, config.enableBellRhythm),
                    Option.toggle("enableMarketDay", Items.EMERALD, config.enableMarketDay),
                    Option.toggle("enableVillagerChat", Items.BOOK, config.enableVillagerChat));
            case 1 -> List.of(
                    Option.slider("villageEnterRadius", Items.COMPASS, config.villageEnterRadius, 16, 256, 4),
                    Option.slider("villageExitConfirmRadius", Items.MAP, config.villageExitConfirmRadius, 32, 512, 4),
                    Option.slider("customVillageRadius", Items.STONE_BRICKS, config.customVillageRadius, 16, 128, 4),
                    Option.toggle("enableEntryMessages", Items.OAK_SIGN, config.enableEntryMessages),
                    Option.slider("nameplateRange", Items.SPYGLASS, config.nameplateRange, 8, 48, 2));
            case 2 -> List.of(
                    Option.toggle("enablePersonalities", Items.NAME_TAG, config.enablePersonalities),
                    Option.toggle("enableGolemPersonalities", Items.IRON_BLOCK, config.enableGolemPersonalities),
                    Option.toggle("enableNameplates", Items.ITEM_FRAME, config.enableNameplates),
                    Option.toggle("enableVillageRelations", Items.MAP, config.enableVillageRelations),
                    Option.toggle("enableGuardAttacks", Items.IRON_SWORD, config.enableGuardAttacks));
            default -> List.of(
                    Option.slider("reputationDecayPerDay", Items.CLOCK, config.reputationDecayPerDay, 0, 100, 1),
                    Option.slider("repTrade", Items.EMERALD, config.repTrade, 0, 100, 1),
                    Option.slider("repKillHostileBase", Items.ROTTEN_FLESH, config.repKillHostileBase, 0, 100, 1),
                    Option.slider("repKillVillager", Items.IRON_SWORD, config.repKillVillager, -300, 0, 5));
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xE8080A0E, 0xF0040508);
        graphics.fillGradient(0, 0, width, 74, 0xAA10141A, 0x0010141A);

        int accent = TAB_ACCENTS[tab];
        graphics.drawCenteredString(font, title, width / 2, 16, TEXT);
        graphics.fill(width / 2 - 120, 58, width / 2 + 120, 59, (accent & 0x00FFFFFF) | 0x99000000);

        roundedBox(graphics, panelX, panelY, panelW, panelH, PANEL_BG, PANEL_BORDER);

        if (!VillageDiplomacyConfig.SPEC.isLoaded()) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.not_loaded"),
                    width / 2, panelY + panelH / 2, TEXT_DIM);
        }

        graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.restart"),
                width / 2, height - 54, TEXT_DIM);

        if (System.currentTimeMillis() < savedUntil) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.saved"),
                    width / 2, height - 66, SAVE_ACCENT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void saveSettings() {
        if (VillageDiplomacyConfig.SPEC.isLoaded()) {
            VillageDiplomacyConfig.SPEC.save();
        }
        savedUntil = System.currentTimeMillis() + 2500L;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
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

    private record Option(String key, ItemStack icon, ForgeConfigSpec.BooleanValue toggle,
                          ForgeConfigSpec.IntValue slider, int min, int max, int step) {

        static Option toggle(String key, Item icon, ForgeConfigSpec.BooleanValue value) {
            return new Option(key, new ItemStack(icon), value, null, 0, 0, 1);
        }

        static Option slider(String key, Item icon, ForgeConfigSpec.IntValue value,
                             int min, int max, int step) {
            return new Option(key, new ItemStack(icon), null, value, min, max, step);
        }
    }

    private final class ProTab extends AbstractWidget {

        private final int index;

        private ProTab(int x, int y, int width, int height, int index) {
            super(x, y, width, height, Component.translatable(TAB_KEYS[index]));
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = tab == index;
            boolean hover = isMouseOver(mouseX, mouseY);
            int accent = TAB_ACCENTS[index];

            roundedBox(graphics, getX(), getY(), width, height,
                    selected ? 0xFF20262F : (hover ? 0xFF1B2027 : 0xFF14181E),
                    selected ? accent : ROW_BORDER);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                    selected ? accent : TEXT_DIM);
            if (selected) {
                graphics.fill(getX() + 14, getY() + height - 3, getX() + width - 14,
                        getY() + height - 1, accent);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            tab = index;
            rebuild();
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private final class ProRow extends AbstractWidget {

        private final Option option;
        private final int accent;
        private boolean dragging;

        private ProRow(int x, int y, int width, int height, Option option, int accent) {
            super(x, y, width, height,
                    Component.translatable("villagediplomacy.config.option." + option.key()));
            this.option = option;
            this.accent = accent;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isMouseOver(mouseX, mouseY);
            roundedBox(graphics, getX(), getY(), width, height,
                    hover ? ROW_BG_HOVER : ROW_BG, hover ? accent : ROW_BORDER);

            graphics.renderItem(option.icon(), getX() + 8, getY() + 3);
            graphics.drawString(font, getMessage(), getX() + 30, getY() + 4, TEXT, false);

            boolean enabled;
            String state;
            if (option.toggle() != null) {
                enabled = option.toggle().get();
                state = Component.translatable(enabled ? "options.on" : "options.off").getString();
            } else {
                int value = option.slider().get();
                enabled = value != option.min();
                state = String.valueOf(value);
            }

            int chipWidth = font.width(state) + 10;
            int chipX = getX() + width - chipWidth - 8;
            roundedBox(graphics, chipX, getY() + 4, chipWidth, 14,
                    enabled ? 0xFF1E3A29 : 0xFF1C2027, enabled ? accent : ROW_BORDER);
            graphics.drawString(font, state, chipX + 5, getY() + 7,
                    enabled ? accent : TEXT_DIM, false);

            if (option.slider() != null) {
                int trackX = getX() + 30;
                int trackY = getY() + height - 5;
                int trackWidth = Math.max(24, chipX - 8 - trackX);
                graphics.fill(trackX, trackY, trackX + trackWidth, trackY + 2, 0xFF2A3038);

                int filled = (int) Math.round(trackWidth * fraction());
                graphics.fill(trackX, trackY, trackX + filled, trackY + 2, accent);
                graphics.fill(trackX + Math.max(0, filled - 2), trackY - 2,
                        trackX + Math.min(trackWidth, filled + 2), trackY + 4, 0xFFFFFFFF);
            }

            if (hover) {
                graphics.renderTooltip(font,
                        Component.translatable("villagediplomacy.config.desc." + option.key())
                                .withStyle(ChatFormatting.GRAY),
                        mouseX, mouseY);
            }
        }

        private double fraction() {
            int min = option.min();
            int max = option.max();
            if (max <= min) return 0.0D;
            double value = option.slider().get();
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (double) (max - min)));
        }

        private void dragTo(double mouseX) {
            int trackX = getX() + 30;
            int trackWidth = Math.max(24, getX() + width - 62 - trackX);
            double fraction = Math.max(0.0D, Math.min(1.0D, (mouseX - trackX) / (double) trackWidth));
            int raw = (int) Math.round(option.min() + fraction * (option.max() - option.min()));
            int step = Math.max(1, option.step());
            int snapped = Math.round((float) raw / step) * step;
            option.slider().set(Math.max(option.min(), Math.min(option.max(), snapped)));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            if (option.toggle() != null) {
                option.toggle().set(!option.toggle().get());
                return true;
            }
            dragging = true;
            dragTo(mouseX);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (dragging && button == 0) {
                dragTo(mouseX);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private final class ProButton extends AbstractWidget {

        private final boolean primary;

        private ProButton(int x, int y, int width, int height, Component message, boolean primary) {
            super(x, y, width, height, message);
            this.primary = primary;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isMouseOver(mouseX, mouseY);
            int accent = primary ? SAVE_ACCENT : 0xFF8892A0;
            roundedBox(graphics, getX(), getY(), width, height,
                    primary
                            ? (hover ? 0xFF23472F : 0xFF1B3324)
                            : (hover ? 0xFF232833 : 0xFF191D24),
                    hover ? accent : ROW_BORDER);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 7,
                    primary ? accent : TEXT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            if (primary) {
                saveSettings();
            } else {
                onClose();
            }
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
