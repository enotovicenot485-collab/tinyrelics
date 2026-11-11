package dev.tinyrelics.client;

import dev.tinyrelics.Network;
import dev.tinyrelics.RelicConfig;
import dev.tinyrelics.TinyRelics;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The settings menu: a tab per item on the left, its picture, description and switches on the right. */
public class RelicsScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 232;
    private static final int TAB_WIDTH = 140;
    private static final int TAB_HEIGHT = 22;
    private static final int RIGHT_WIDTH = 234;
    private static final String[] IDS = {"keeping_charm", "thor_hammer", "chaos_boomerang", "blood_blade"};

    private final RelicConfig edit;
    private final ItemStack[] icons;
    private int selected;
    private int left;
    private int top;

    public RelicsScreen() {
        super(Component.translatable("screen.tinyrelics.title"));
        this.edit = ClientEffects.config.copy();
        this.icons = new ItemStack[] {
                new ItemStack(TinyRelics.KEEPING_CHARM.get()),
                new ItemStack(TinyRelics.THOR_HAMMER.get()),
                new ItemStack(TinyRelics.CHAOS_BOOMERANG.get()),
                new ItemStack(TinyRelics.BLOOD_BLADE.get()),
        };
    }

    private int rightX() {
        return this.left + TAB_WIDTH + 16;
    }

    private int rowY(int row) {
        return this.top + 130 + row * 24;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;

        for (int i = 0; i < IDS.length; i++) {
            final int index = i;
            Button tab = Button.builder(this.icons[i].getHoverName(), button -> {
                this.selected = index;
                rebuildWidgets();
            }).bounds(this.left + 8, this.top + 26 + i * (TAB_HEIGHT + 2), TAB_WIDTH, TAB_HEIGHT).build();
            tab.active = i != this.selected;
            addRenderableWidget(tab);
        }

        switch (this.selected) {
            case 0 -> addToggle(0, "craft", this.edit.charmCraft, value -> this.edit.charmCraft = value);
            case 1 -> {
                RadiusSlider slider = new RadiusSlider(rightX(), rowY(0), RIGHT_WIDTH, 20);
                slider.setTooltip(Tooltip.create(Component.translatable("screen.tinyrelics.hammer_radius.tip")));
                addRenderableWidget(slider);
                addToggle(1, "hammer_fire", this.edit.hammerFire, value -> this.edit.hammerFire = value);
                addToggle(2, "hammer_monsters", this.edit.hammerMonstersOnly, value -> this.edit.hammerMonstersOnly = value);
            }
            case 2 -> addToggle(0, "craft", this.edit.boomerangCraft, value -> this.edit.boomerangCraft = value);
            default -> addToggle(0, "craft", this.edit.bladeCraft, value -> this.edit.bladeCraft = value);
        }

        int buttonY = this.top + PANEL_HEIGHT - 26;
        addRenderableWidget(Button.builder(Component.translatable("screen.tinyrelics.apply"),
                        button -> Network.requestConfigChange(this.edit))
                .bounds(this.left + PANEL_WIDTH - 10 - 100 - 6 - 100, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tinyrelics.close"), button -> onClose())
                .bounds(this.left + PANEL_WIDTH - 10 - 100, buttonY, 100, 20).build());
    }

    private void addToggle(int row, String key, boolean value, Consumer<Boolean> setter) {
        CycleButton<Boolean> toggle = CycleButton.onOffBuilder(value).create(rightX(), rowY(row), RIGHT_WIDTH, 20,
                Component.translatable("screen.tinyrelics." + key), (button, newValue) -> setter.accept(newValue));
        toggle.setTooltip(Tooltip.create(Component.translatable("screen.tinyrelics." + key + ".tip")));
        addRenderableWidget(toggle);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21 super.render draws the background (our panel included) and then the buttons.
        super.render(graphics, mouseX, mouseY, partialTick);
        drawContents(graphics);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, 0xFF6A4FB0);
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, 0xF0141420);
        graphics.fill(this.left + TAB_WIDTH + 11, this.top + 24, this.left + TAB_WIDTH + 12, this.top + PANEL_HEIGHT - 32, 0xFF3A3A50);
    }

    private void drawContents(GuiGraphics graphics) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.top + 9, 0xFFFFFF);

        for (int i = 0; i < IDS.length; i++) {
            graphics.renderItem(this.icons[i], this.left + 11, this.top + 26 + i * (TAB_HEIGHT + 2) + 3);
        }

        int x = rightX();
        String id = IDS[this.selected];
        graphics.pose().pushPose();
        graphics.pose().translate(x, this.top + 24, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(this.icons[this.selected], 0, 0);
        graphics.pose().popPose();

        graphics.drawString(this.font, this.icons[this.selected].getHoverName(), x + 40, this.top + 29, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("screen.tinyrelics.stats." + id), x + 40, this.top + 42, 0xA0A0A0);
        graphics.drawWordWrap(this.font, Component.translatable("screen.tinyrelics.desc." + id), x, this.top + 62, RIGHT_WIDTH, 0xD8D8D8);
        graphics.drawString(this.font, Component.translatable("screen.tinyrelics.settings"), x, this.top + 118, 0xFFD070);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class RadiusSlider extends AbstractSliderButton {
        private static final int SPAN = RelicConfig.MAX_RADIUS - RelicConfig.MIN_RADIUS;

        RadiusSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    (RelicsScreen.this.edit.hammerRadius - RelicConfig.MIN_RADIUS) / (double) SPAN);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.tinyrelics.hammer_radius", RelicsScreen.this.edit.hammerRadius));
        }

        @Override
        protected void applyValue() {
            RelicsScreen.this.edit.hammerRadius = RelicConfig.MIN_RADIUS + (int) Math.round(this.value * SPAN);
        }
    }
}
