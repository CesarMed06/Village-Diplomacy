package com.cesoti2006.villagediplomacy.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
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
 *
 * Closing without saving puts every value back the way it was, which is what a
 * settings screen is expected to do. There is a single "Save and close" button
 * plus a reset-to-defaults button, and options marked "world" warn that they are
 * only applied after rejoining the world.
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
    private static final int WARN_ACCENT = 0xFFE3B44C;
    private static final int DANGER_ACCENT = 0xFFD9534F;

    private enum Tone { PRIMARY, NEUTRAL, DANGER }

    private final Screen parent;
    private final List<OptionRow> rows = new ArrayList<>();
    private final Map<String, Integer> snapshotInts = new HashMap<>();
    private final Map<String, Boolean> snapshotBools = new HashMap<>();

    private boolean snapshotTaken = false;
    private boolean saved = false;
    private boolean worldChanged = false;

    private int tab = 0;
    private int scroll = 0;
    private int maxScroll = 0;
    private long resetNoticeUntil = 0L;

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
        saved = false;

        if (!snapshotTaken && VillageDiplomacyConfig.SPEC.isLoaded()) {
            captureSnapshot();
            snapshotTaken = true;
        }

        panelW = Math.min(400, width - 40);
        panelX = width / 2 - panelW / 2;
        panelY = 62;
        panelH = Math.max(120, height - panelY - 84);

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

        int buttonY = height - 38;
        int buttonWidth = Mth.clamp((width - 60) / 3, 70, 108);
        int gap = 6;
        int total = buttonWidth * 3 + gap * 2;
        int startX = width / 2 - total / 2;
        addRenderableWidget(new ProButton(startX, buttonY, buttonWidth, 20,
                Component.translatable("villagediplomacy.config.reset"), Tone.NEUTRAL, this::resetDefaults));
        addRenderableWidget(new ProButton(startX + buttonWidth + gap, buttonY, buttonWidth, 20,
                Component.translatable("villagediplomacy.config.exit"), Tone.DANGER, this::discardAndClose));
        addRenderableWidget(new ProButton(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                Component.translatable("villagediplomacy.config.save"), Tone.PRIMARY, this::saveAndClose));
    }

    private void rebuild() {
        boolean wasSnapshot = snapshotTaken;
        init();
        snapshotTaken = wasSnapshot;
    }

    private void captureSnapshot() {
        snapshotInts.clear();
        snapshotBools.clear();
        worldChanged = false;
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (option.toggle() != null) {
                    snapshotBools.put(option.key(), option.toggle().get());
                } else if (option.slider() != null) {
                    snapshotInts.put(option.key(), option.slider().get());
                }
            }
        }
    }

    private void applySnapshot() {
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (option.toggle() != null && snapshotBools.containsKey(option.key())) {
                    option.toggle().set(snapshotBools.get(option.key()));
                } else if (option.slider() != null && snapshotInts.containsKey(option.key())) {
                    option.slider().set(snapshotInts.get(option.key()));
                }
            }
        }
    }

    private void resetDefaults() {
        if (!VillageDiplomacyConfig.SPEC.isLoaded()) return;
        commitEdit();
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (option.toggle() != null) {
                    option.toggle().set(option.defaultToggle());
                } else if (option.slider() != null) {
                    option.slider().set(option.defaultSlider());
                }
            }
        }
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (option.reload() && snapshotUnchanged(option)) {
                    continue;
                }
                if (option.reload()) {
                    worldChanged = true;
                }
            }
        }
        resetNoticeUntil = System.currentTimeMillis() + 2500L;
    }

    /** True while an option still holds the value it had when the screen opened. */
    private boolean snapshotUnchanged(Option option) {
        if (option.toggle() != null && snapshotBools.containsKey(option.key())) {
            return snapshotBools.get(option.key()).equals(option.toggle().get());
        }
        if (option.slider() != null && snapshotInts.containsKey(option.key())) {
            return snapshotInts.get(option.key()).equals(option.slider().get());
        }
        return true;
    }

    private boolean isDirty() {
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (option.toggle() != null && snapshotBools.containsKey(option.key())
                        && !snapshotBools.get(option.key()).equals(option.toggle().get())) {
                    return true;
                }
                if (option.slider() != null && snapshotInts.containsKey(option.key())
                        && !snapshotInts.get(option.key()).equals(option.slider().get())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean reloadChanged() {
        for (int t = 0; t < 4; t++) {
            for (Option option : optionsFor(t)) {
                if (!option.reload()) continue;
                if (option.toggle() != null && snapshotBools.containsKey(option.key())
                        && !snapshotBools.get(option.key()).equals(option.toggle().get())) {
                    return true;
                }
                if (option.slider() != null && snapshotInts.containsKey(option.key())
                        && !snapshotInts.get(option.key()).equals(option.slider().get())) {
                    return true;
                }
            }
        }
        return false;
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
                    Option.toggle("enableVillageLife", Items.BELL, config.enableVillageLife, true, false),
                    Option.toggle("enableBellRhythm", Items.NOTE_BLOCK, config.enableBellRhythm, true, false),
                    Option.toggle("enableMarketDay", Items.EMERALD, config.enableMarketDay, true, false),
                    Option.toggle("enableVillagerChat", Items.BOOK, config.enableVillagerChat, true, false),
                    Option.toggle("showDialogueInChat", Items.WRITABLE_BOOK, config.showDialogueInChat, true, false),
                    Option.toggle("showDialogueBubbles", Items.PAPER, config.showDialogueBubbles, true, false),
                    Option.slider("bubbleSeconds", Items.CLOCK, config.bubbleSeconds, 2, 30, 1, 7, false));
            case 1 -> List.of(
                    Option.slider("villageEnterRadius", Items.COMPASS, config.villageEnterRadius, 16, 256, 4, 80, true),
                    Option.slider("villageExitConfirmRadius", Items.MAP, config.villageExitConfirmRadius, 32, 512, 4, 120, true),
                    Option.slider("customVillageRadius", Items.STONE_BRICKS, config.customVillageRadius, 16, 128, 4, 48, false),
                    Option.toggle("enableEntryMessages", Items.OAK_SIGN, config.enableEntryMessages, true, false),
                    Option.slider("nameplateRange", Items.SPYGLASS, config.nameplateRange, 8, 48, 2, 20, false));
            case 2 -> List.of(
                    Option.toggle("enablePersonalities", Items.NAME_TAG, config.enablePersonalities, true, false),
                    Option.toggle("enableGolemPersonalities", Items.IRON_BLOCK, config.enableGolemPersonalities, true, false),
                    Option.toggle("enableNameplates", Items.ITEM_FRAME, config.enableNameplates, true, false),
                    Option.toggle("enableVillageRelations", Items.MAP, config.enableVillageRelations, true, false),
                    Option.toggle("enableGuardAttacks", Items.IRON_SWORD, config.enableGuardAttacks, true, true));
            default -> List.of(
                    Option.slider("reputationDecayPerDay", Items.CLOCK, config.reputationDecayPerDay, 0, 100, 1, 0, false),
                    Option.slider("repTrade", Items.EMERALD, config.repTrade, 0, 100, 1, 5, false),
                    Option.slider("repKillHostileBase", Items.ROTTEN_FLESH, config.repKillHostileBase, 0, 100, 1, 5, false),
                    Option.slider("repKillVillager", Items.IRON_SWORD, config.repKillVillager, -300, 0, 5, -100, false));
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

        if (System.currentTimeMillis() < resetNoticeUntil) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.reset_done"),
                    width / 2, height - 84, SAVE_ACCENT);
        } else if (isDirty()) {
            // Kept out of the list: drawn on the title row, right side, where it
            // cannot cover an option or the tabs.
            Component unsaved = Component.translatable("villagediplomacy.config.unsaved");
            int unsavedWidth = font.width(unsaved);
            int titleRight = width / 2 + font.width(title) / 2;
            int unsavedX = width - unsavedWidth - 8;
            if (unsavedX > titleRight + 12) {
                graphics.drawString(font, unsaved, unsavedX, 16, WARN_ACCENT, false);
            }
        }

        if (editing == null && hasSlider()) {
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

    /** Closes the screen and puts every value back, without touching the file. */
    private void discardAndClose() {
        commitEdit();
        saved = false;
        onClose();
    }

    private void saveAndClose() {
        commitEdit();
        if (!VillageDiplomacyConfig.SPEC.isLoaded()) {
            onClose();
            return;
        }
        VillageDiplomacyConfig.SPEC.save();
        // Only ask for a rejoin when a world-restart option really changed in
        // this session, not because the config file was written.
        boolean reload = worldChanged;
        saved = true;
        if (reload && minecraft != null && minecraft.player != null) {
            minecraft.setScreen(new ReloadNoticeScreen(parent));
            return;
        }
        onClose();
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
        // Closing without saving restores the values from when the screen opened,
        // so "Esc" can never silently change the config.
        if (!saved) {
            applySnapshot();
        }
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
                          ForgeConfigSpec.IntValue slider, int min, int max, int step,
                          boolean defaultToggle, int defaultSlider, boolean reload) {

        static Option toggle(String key, Item icon, ForgeConfigSpec.BooleanValue value,
                             boolean defaultValue, boolean reload) {
            return new Option(key, new ItemStack(icon), value, null, 0, 0, 1, defaultValue, 0, reload);
        }

        static Option slider(String key, Item icon, ForgeConfigSpec.IntValue value,
                             int min, int max, int step, int defaultValue, boolean reload) {
            return new Option(key, new ItemStack(icon), null, value, min, max, step, false, defaultValue, reload);
        }

        String defaultText() {
            if (toggle != null) {
                return Component.translatable(defaultToggle ? "options.on" : "options.off").getString();
            }
            return String.valueOf(defaultSlider);
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
                if (option.reload()) worldChanged = true;
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
            MutableComponent label = Component.translatable("villagediplomacy.config.option." + option.key());
            graphics.drawString(font, label, x + 29, y + 4, TEXT, false);

            if (option.reload()) {
                int labelEnd = x + 29 + font.width(label) + 5;
                graphics.drawString(font,
                        Component.translatable("villagediplomacy.config.world_tag"),
                        labelEnd, y + 4, TEXT_FAINT, false);
            }

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
            List<FormattedCharSequence> desc = font.split(
                    Component.translatable("villagediplomacy.config.desc." + option.key()), 186);
            Component def = Component.translatable("villagediplomacy.config.default", option.defaultText());

            int textWidth = font.width(def);
            for (FormattedCharSequence line : desc) {
                textWidth = Math.max(textWidth, font.width(line));
            }

            int boxWidth = textWidth + 16;
            int boxHeight = desc.size() * 10 + 25;
            int tooltipX = Mth.clamp(mouseX + 12, 6, Math.max(6, width - boxWidth - 6));
            int below = mouseY + 14;
            int above = mouseY - boxHeight - 12;
            int tooltipY = (below + boxHeight <= height - 8)
                    ? below
                    : Math.max(34, above);
            tooltipY = Mth.clamp(tooltipY, 34, Math.max(34, height - boxHeight - 8));

            roundedBox(graphics, tooltipX, tooltipY, boxWidth, boxHeight, 0xF2101418, accent);
            int lineY = tooltipY + 8;
            for (FormattedCharSequence line : desc) {
                graphics.drawString(font, line, tooltipX + 8, lineY, 0xFFD7DEE6, false);
                lineY += 10;
            }
            graphics.drawString(font, def, tooltipX + 8, lineY + 3, TEXT_FAINT, false);
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
            if (option.reload()) worldChanged = true;
        }
    }

    private final class ProButton extends AbstractWidget {

        private final Tone tone;
        private final Runnable action;

        private ProButton(int x, int y, int width, int height, Component message,
                          Tone tone, Runnable action) {
            super(x, y, width, height, message);
            this.tone = tone;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isMouseOver(mouseX, mouseY);
            int accent = switch (tone) {
                case PRIMARY -> SAVE_ACCENT;
                case DANGER -> DANGER_ACCENT;
                case NEUTRAL -> 0xFF8892A0;
            };
            int background = switch (tone) {
                case PRIMARY -> hover ? 0xFF23472F : 0xFF1B3324;
                case DANGER -> hover ? 0xFF3A1E1E : 0xFF241616;
                case NEUTRAL -> hover ? 0xFF232833 : 0xFF191D24;
            };
            roundedBox(graphics, getX(), getY(), width, height, background,
                    hover ? accent : ROW_BORDER);
            graphics.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + 6,
                    tone == Tone.NEUTRAL && !hover ? TEXT : accent);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) return false;
            playDownSound(Minecraft.getInstance().getSoundManager());
            action.run();
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
