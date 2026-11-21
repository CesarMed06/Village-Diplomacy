package com.cesoti2006.villagediplomacy.client;

import java.util.ArrayList;
import java.util.List;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * In-game settings screen. Also serves as the config screen of the mods list,
 * so the "Config" button is no longer greyed out.
 *
 * Everything here is drawn by the mod itself (no textures, no extra assets). The
 * list scrolls when a tab grows and every number can be typed into directly, so
 * new options do not need a redesign.
 */
public class VillageConfigScreen extends Screen {

    private static final String[] TAB_KEYS = {
            "villagediplomacy.config.tab.life",
            "villagediplomacy.config.tab.village",
            "villagediplomacy.config.tab.systems",
            "villagediplomacy.config.tab.reputation"
    };

    private static final int[] TAB_ACCENTS = {
            0xFF6FD98A, 0xFFE3B44C, 0xFF5FC9DE, 0xFFBE8CF0
    };

    private static final int ROW_H = 24;
    private static final int ROW_GAP = 4;

    private static final int PANEL_BG = 0xF20B0E12;
    private static final int PANEL_BORDER = 0xFF2A323C;
    private static final int ROW_BG = 0xFF171B21;
    private static final int ROW_BG_HOVER = 0xFF1F242C;
    private static final int ROW_BORDER = 0xFF2B333D;
    private static final int CHIP_OFF_BG = 0xFF1B2027;
    private static final int CHIP_ON_BG = 0xFF1B3324;
    private static final int TEXT = 0xFFECECEC;
    private static final int TEXT_DIM = 0xFF98A2AC;
    private static final int TEXT_FAINT = 0xFF7E8995;
    private static final int SAVE_ACCENT = 0xFF6FD98A;

    private final Screen parent;
    private final List<OptionRow> rows = new ArrayList<>();

    private int tab = 0;
    private int scroll = 0;
    private int maxScroll = 0;
    private long savedUntil = 0L;

    private EditBox editing;
    private OptionRow editingRow;

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
        rows.clear();
        editing = null;
        editingRow = null;

        panelW = Math.min(400, width - 40);
        panelX = width / 2 - panelW / 2;
        panelY = 62;
        panelH = Math.max(120, height - panelY - 76);

        int tabGap = 6;
        int tabWidth = Math.min(96, (width - 40 - tabGap * 3) / 4);
        int tabsX = width / 2 - (tabWidth * 4 + tabGap * 3) / 2;
        for (int i = 0; i < 4; i++) {
            addRenderableWidget(new ProTab(tabsX + i * (tabWidth + tabGap), 40, tabWidth, 20, i));
        }

        if (VillageDiplomacyConfig.SPEC.isLoaded()) {
            for (Option option : optionsFor(tab)) {
                rows.add(new OptionRow(option, TAB_ACCENTS[tab]));
            }
        }
        scroll = 0;
        layoutRows();

        addRenderableWidget(new ProButton(width / 2 - 104, height - 38, 100, 22,
                Component.translatable("villagediplomacy.config.save"), true));
        addRenderableWidget(new ProButton(width / 2 + 4, height - 38, 100, 22,
                CommonComponents.GUI_DONE, false));
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void layoutRows() {
        int top = panelY + 12;
        int viewport = panelH - 24;
        int content = rows.size() * ROW_H + Math.max(0, rows.size() - 1) * ROW_GAP;
        maxScroll = Math.max(0, content - viewport);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        int rowX = panelX + 12;
        int rowW = panelW - 24;
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).place(rowX, top + i * (ROW_H + ROW_GAP) - scroll, rowW, ROW_H);
        }
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
        roundedBox(graphics, panelX, panelY, panelW, panelH, PANEL_BG, PANEL_BORDER);

        if (!VillageDiplomacyConfig.SPEC.isLoaded()) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.not_loaded"),
                    width / 2, panelY + panelH / 2, TEXT_DIM);
        }

        OptionRow hovered = null;
        graphics.enableScissor(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2);
        for (OptionRow row : rows) {
            boolean hover = row.contains(mouseX, mouseY) && row.insidePanel();
            row.render(graphics, hover);
            if (hover) hovered = row;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackTop = panelY + 10;
            int trackHeight = panelH - 20;
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            int barX = panelX + panelW - 7;
            graphics.fill(barX, trackTop, barX + 3, trackTop + trackHeight, 0xFF20262D);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, accent);
        }

        if (System.currentTimeMillis() < savedUntil) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.saved"),
                    width / 2, height - 70, SAVE_ACCENT);
        } else if (hasSlider()) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.edit_hint"),
                    width / 2, height - 70, TEXT_FAINT);
        }
        graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.restart"),
                width / 2, height - 57, TEXT_DIM);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null && editing == null) {
            hovered.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private boolean hasSlider() {
        for (OptionRow row : rows) {
            if (row.option.slider() != null) return true;
        }
        return false;
    }

    private void startEdit(OptionRow row) {
        if (row.option.slider() == null) return;
        cancelEdit();

        int boxWidth = 74;
        EditBox box = new EditBox(font, row.x + row.width - boxWidth - 6, row.y + 3,
                boxWidth, 17, Component.empty());
        box.setValue(String.valueOf(row.option.slider().get()));
        box.setMaxLength(5);
        box.setFilter(text -> text.isEmpty() || "-".equals(text) || text.matches("-?\\d{1,4}"));
        box.setCanLoseFocus(false);
        addRenderableWidget(box);
        setFocused(box);
        box.setFocused(true);
        editing = box;
        editingRow = row;
    }

    private void commitEdit() {
        if (editing == null) return;
        OptionRow row = editingRow;
        String text = editing.getValue().trim();
        cancelEdit();

        if (row == null || row.option.slider() == null) return;
        if (text.isEmpty() || "-".equals(text)) return;
        try {
            int value = Integer.parseInt(text);
            int step = Math.max(1, row.option.step());
            int snapped = Math.round((float) value / step) * step;
            row.option.slider().set(Mth.clamp(snapped, row.option.min(), row.option.max()));
        } catch (NumberFormatException ignored) {
        }
    }

    private void cancelEdit() {
        if (editing == null) return;
        removeWidget(editing);
        setFocused(null);
        editing = null;
        editingRow = null;
    }

    private static boolean insideWidget(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight();
    }

    private void saveSettings() {
        if (VillageDiplomacyConfig.SPEC.isLoaded()) {
            VillageDiplomacyConfig.SPEC.save();
        }
        savedUntil = System.currentTimeMillis() + 2500L;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing != null) {
            if (insideWidget(editing, mouseX, mouseY)) {
                return editing.mouseClicked(mouseX, mouseY, button);
            }
            commitEdit();
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            for (OptionRow row : rows) {
                if (row.mouseClicked(mouseX, mouseY)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (editing != null && editing.isFocused()) {
            return editing.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        for (OptionRow row : rows) {
            if (row.mouseDragged(mouseX)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (editing != null) {
            editing.mouseReleased(mouseX, mouseY, button);
        }
        for (OptionRow row : rows) {
            row.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean overPanel = mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= panelY && mouseY <= panelY + panelH;
        if (maxScroll > 0 && overPanel) {
            scroll = Mth.clamp(scroll - (int) Math.round(delta * 24), 0, maxScroll);
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing != null) {
            if (keyCode == 257 || keyCode == 335) {
                commitEdit();
                return true;
            }
            if (keyCode == 256) {
                cancelEdit();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        commitEdit();
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
                    selected ? 0xFF1E252D : (hover ? 0xFF191F26 : 0xFF12161B),
                    selected ? accent : ROW_BORDER);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                    selected ? accent : TEXT_DIM);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            commitEdit();
            tab = index;
            rebuild();
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private final class OptionRow {

        private final Option option;
        private final int accent;

        private int x;
        private int y;
        private int width;
        private int height;
        private boolean dragging;

        private OptionRow(Option option, int accent) {
            this.option = option;
            this.accent = accent;
        }

        private void place(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        private boolean insidePanel() {
            return y + height > panelY + 2 && y < panelY + panelH - 2;
        }

        private boolean mouseClicked(double mouseX, double mouseY) {
            if (!contains(mouseX, mouseY) || !insidePanel()) return false;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (option.toggle() != null) {
                option.toggle().set(!option.toggle().get());
                return true;
            }
            if (mouseX >= chipX() - 2) {
                startEdit(this);
                return true;
            }
            dragging = true;
            dragTo(mouseX);
            return true;
        }

        private boolean mouseDragged(double mouseX) {
            if (!dragging) return false;
            dragTo(mouseX);
            return true;
        }

        private void mouseReleased() {
            dragging = false;
        }

        private String stateText() {
            if (option.toggle() != null) {
                return Component.translatable(option.toggle().get() ? "options.on" : "options.off").getString();
            }
            return String.valueOf(option.slider().get());
        }

        private int chipWidth() {
            return font.width(stateText()) + 12;
        }

        private int chipX() {
            return x + width - chipWidth() - 8;
        }

        private boolean valueOn() {
            return option.toggle() != null ? option.toggle().get() : option.slider().get() != option.min();
        }

        private void render(GuiGraphics graphics, boolean hover) {
            roundedBox(graphics, x, y, width, height,
                    hover ? ROW_BG_HOVER : ROW_BG, hover ? accent : ROW_BORDER);

            graphics.renderItem(option.icon(), x + 7, y + 4);
            graphics.drawString(font,
                    Component.translatable("villagediplomacy.config.option." + option.key()),
                    x + 29, y + 4, TEXT, false);

            boolean on = valueOn();
            int chipW = chipWidth();
            int chipX = chipX();
            roundedBox(graphics, chipX, y + 4, chipW, 15,
                    on ? CHIP_ON_BG : CHIP_OFF_BG, on ? accent : ROW_BORDER);
            graphics.drawString(font, stateText(), chipX + 6, y + 8, on ? accent : TEXT_DIM, false);

            if (option.slider() != null) {
                int trackX = x + 29;
                int trackY = y + height - 5;
                int trackWidth = Math.max(20, chipX - 10 - trackX);
                graphics.fill(trackX, trackY, trackX + trackWidth, trackY + 2, 0xFF2A3038);

                int filled = (int) Math.round(trackWidth * fraction());
                if (filled > 0) {
                    graphics.fill(trackX, trackY, trackX + filled, trackY + 2, accent);
                }
                int knobX = Mth.clamp(trackX + filled, trackX, trackX + trackWidth);
                graphics.fill(knobX - 1, trackY - 2, knobX + 1, trackY + 4, 0xFFFFFFFF);
            }
        }

        private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.renderTooltip(font,
                    Component.translatable("villagediplomacy.config.desc." + option.key())
                            .withStyle(ChatFormatting.GRAY),
                    mouseX, mouseY);
        }

        private double fraction() {
            int min = option.min();
            int max = option.max();
            if (max <= min) return 0.0D;
            double value = option.slider().get();
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (double) (max - min)));
        }

        private void dragTo(double mouseX) {
            int trackX = x + 29;
            int trackWidth = Math.max(20, chipX() - 10 - trackX);
            double fraction = Math.max(0.0D, Math.min(1.0D, (mouseX - trackX) / (double) trackWidth));
            int raw = (int) Math.round(option.min() + fraction * (option.max() - option.min()));
            int step = Math.max(1, option.step());
            int snapped = Math.round((float) raw / step) * step;
            option.slider().set(Mth.clamp(snapped, option.min(), option.max()));
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
