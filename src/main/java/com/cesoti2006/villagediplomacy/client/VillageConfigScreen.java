package com.cesoti2006.villagediplomacy.client;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * In-game settings screen. Also serves as the config screen of the mods list,
 * so the "Config" button is no longer greyed out.
 */
public class VillageConfigScreen extends Screen {

    private static final String[] TAB_KEYS = {
            "villagediplomacy.config.tab.life",
            "villagediplomacy.config.tab.village",
            "villagediplomacy.config.tab.systems",
            "villagediplomacy.config.tab.reputation"
    };

    private static final int ROW_HEIGHT = 26;

    private final Screen parent;
    private int tab = 0;
    private boolean saved = false;

    public VillageConfigScreen(Screen parent) {
        super(Component.translatable("villagediplomacy.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        saved = false;

        int tabWidth = 74;
        int gap = 4;
        int startX = (width - (tabWidth * 4 + gap * 3)) / 2;
        for (int i = 0; i < 4; i++) {
            final int index = i;
            Button button = Button.builder(Component.translatable(TAB_KEYS[i]), b -> {
                tab = index;
                rebuild();
            }).bounds(startX + i * (tabWidth + gap), 34, tabWidth, 20).build();
            button.active = tab != i;
            addRenderableWidget(button);
        }

        VillageDiplomacyConfig config = VillageDiplomacyConfig.INSTANCE;
        int y = 74;
        switch (tab) {
            case 0 -> {
                addToggle(y, "enableVillageLife", config.enableVillageLife);
                addToggle(y + ROW_HEIGHT, "enableBellRhythm", config.enableBellRhythm);
                addToggle(y + ROW_HEIGHT * 2, "enableMarketDay", config.enableMarketDay);
                addToggle(y + ROW_HEIGHT * 3, "enableVillagerChat", config.enableVillagerChat);
            }
            case 1 -> {
                addSlider(y, "villageEnterRadius", config.villageEnterRadius, 16, 256, 4);
                addSlider(y + ROW_HEIGHT, "villageExitConfirmRadius", config.villageExitConfirmRadius, 32, 512, 4);
                addSlider(y + ROW_HEIGHT * 2, "customVillageRadius", config.customVillageRadius, 16, 128, 4);
                addToggle(y + ROW_HEIGHT * 3, "enableEntryMessages", config.enableEntryMessages);
            }
            case 2 -> {
                addToggle(y, "enablePersonalities", config.enablePersonalities);
                addToggle(y + ROW_HEIGHT, "enableGolemPersonalities", config.enableGolemPersonalities);
                addToggle(y + ROW_HEIGHT * 2, "enableVillageRelations", config.enableVillageRelations);
                addToggle(y + ROW_HEIGHT * 3, "enableGuardAttacks", config.enableGuardAttacks);
            }
            default -> {
                addSlider(y, "reputationDecayPerDay", config.reputationDecayPerDay, 0, 100, 1);
                addSlider(y + ROW_HEIGHT, "repTrade", config.repTrade, 0, 100, 1);
                addSlider(y + ROW_HEIGHT * 2, "repKillHostileBase", config.repKillHostileBase, 0, 100, 1);
                addSlider(y + ROW_HEIGHT * 3, "repKillVillager", config.repKillVillager, -300, 0, 5);
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("villagediplomacy.config.save"), b -> saveSettings())
                .bounds(width / 2 - 102, height - 52, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 2, height - 52, 100, 20).build());
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void addToggle(int y, String optionKey, ForgeConfigSpec.BooleanValue value) {
        if (!VillageDiplomacyConfig.SPEC.isLoaded()) return;
        CycleButton<Boolean> button = CycleButton.onOffBuilder(value.get())
                .create(width / 2 - 110, y, 220, 20,
                        Component.translatable("villagediplomacy.config.option." + optionKey),
                        (widget, state) -> value.set(state));
        addRenderableWidget(button);
    }

    private void addSlider(int y, String optionKey, ForgeConfigSpec.IntValue value,
                           int min, int max, int step) {
        if (!VillageDiplomacyConfig.SPEC.isLoaded()) return;
        addRenderableWidget(new IntSlider(width / 2 - 110, y, 220, 20, value,
                "villagediplomacy.config.option." + optionKey, min, max, step));
    }

    private void saveSettings() {
        if (VillageDiplomacyConfig.SPEC.isLoaded()) {
            VillageDiplomacyConfig.SPEC.save();
        }
        saved = true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.restart"),
                width / 2, height - 74, 0x9A9A9A);
        if (saved) {
            graphics.drawCenteredString(font, Component.translatable("villagediplomacy.config.saved"),
                    width / 2, height - 68, 0x55FF55);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private static final class IntSlider extends AbstractSliderButton {

        private final ForgeConfigSpec.IntValue option;
        private final String labelKey;
        private final int min;
        private final int max;
        private final int step;

        private IntSlider(int x, int y, int width, int height, ForgeConfigSpec.IntValue option,
                          String labelKey, int min, int max, int step) {
            super(x, y, width, height, Component.empty(), fraction(option, min, max));
            this.option = option;
            this.labelKey = labelKey;
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            updateMessage();
        }

        private static double fraction(ForgeConfigSpec.IntValue option, int min, int max) {
            if (max <= min) return 0.0D;
            int current = option.get();
            return Math.max(0.0D, Math.min(1.0D, (double) (current - min) / (double) (max - min)));
        }

        private int currentValue() {
            int raw = (int) Math.round(min + value * (max - min));
            int snapped = Math.round((float) raw / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(labelKey)
                    .append(Component.literal(": " + currentValue())));
        }

        @Override
        protected void applyValue() {
            if (VillageDiplomacyConfig.SPEC.isLoaded()) {
                option.set(currentValue());
            }
        }
    }
}
