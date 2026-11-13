package dev.tinyrelics.client;

import dev.tinyrelics.Network;
import dev.tinyrelics.RelicConfig;
import dev.tinyrelics.TinyRelics;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
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
    private static final String[] IDS = {"keeping_charm", "thor_hammer", "chaos_boomerang", "blood_blade", "undead_staff", "necro_armor", "chaos_armor"};

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
                new ItemStack(TinyRelics.UNDEAD_STAFF.get()),
                new ItemStack(TinyRelics.NECRO_CHESTPLATE.get()),
                new ItemStack(TinyRelics.CHAOS_CHESTPLATE.get()),
        };
    }

    /** A tab is named after its item, except the armor tab, which stands for all four pieces. */
    private Component tabName(int index) {
        return index >= 5 ? Component.translatable("screen.tinyrelics.name." + IDS[index]) : this.icons[index].getHoverName();
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
            Button tab = Button.builder(tabName(i), button -> {
                this.selected = index;
                rebuildWidgets();
            }).bounds(this.left + 8, this.top + 26 + i * (TAB_HEIGHT + 2), TAB_WIDTH, TAB_HEIGHT).build();
            tab.active = i != this.selected;
            addRenderableWidget(tab);
        }

        switch (this.selected) {
            case 0 -> addToggle(0, "craft", this.edit.charmCraft, value -> this.edit.charmCraft = value);
            case 1 -> {
                addSlider(0, "hammer_radius", RelicConfig.MIN_RADIUS, RelicConfig.MAX_RADIUS,
                        () -> this.edit.hammerRadius, value -> this.edit.hammerRadius = value);
                addToggle(1, 0, 2, "hammer_fire", this.edit.hammerFire, value -> this.edit.hammerFire = value);
                addToggle(1, 1, 2, "hammer_monsters", this.edit.hammerMonstersOnly, value -> this.edit.hammerMonstersOnly = value);
                addSlider(2, 0, 2, "hammer_slam_radius", RelicConfig.MIN_SLAM, RelicConfig.MAX_SLAM,
                        () -> this.edit.hammerSlamRadius, value -> this.edit.hammerSlamRadius = value);
                addToggle(2, 1, 2, "hammer_slam_fire", this.edit.hammerSlamFire, value -> this.edit.hammerSlamFire = value);
            }
            case 2 -> addToggle(0, "craft", this.edit.boomerangCraft, value -> this.edit.boomerangCraft = value);
            case 3 -> addToggle(0, "craft", this.edit.bladeCraft, value -> this.edit.bladeCraft = value);
            case 5 -> addToggle(0, "craft", this.edit.armorCraft, value -> this.edit.armorCraft = value);
            case 6 -> {
                // nothing to set up yet: the Chaos Assassin armor has no recipe
            }
            default -> {
                addSlider(0, "staff_kills", RelicConfig.MIN_KILLS, RelicConfig.MAX_KILLS,
                        () -> this.edit.staffKills, value -> this.edit.staffKills = value);
                addToggle(1, "staff_grief", this.edit.staffSkullGrief, value -> this.edit.staffSkullGrief = value);
                addToggle(2, "craft", this.edit.staffCraft, value -> this.edit.staffCraft = value);
            }
        }

        int buttonY = this.top + PANEL_HEIGHT - 26;
        addRenderableWidget(Button.builder(Component.translatable("screen.tinyrelics.apply"),
                        button -> Network.requestConfigChange(this.edit))
                .bounds(this.left + PANEL_WIDTH - 10 - 100 - 6 - 100, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tinyrelics.close"), button -> onClose())
                .bounds(this.left + PANEL_WIDTH - 10 - 100, buttonY, 100, 20).build());
    }

    /** Left edge of a widget when a row is split into several columns. */
    private int columnX(int column, int columns) {
        return rightX() + column * (columnWidth(columns) + 6);
    }

    private int columnWidth(int columns) {
        return (RIGHT_WIDTH - 6 * (columns - 1)) / columns;
    }

    private void addToggle(int row, String key, boolean value, Consumer<Boolean> setter) {
        addToggle(row, 0, 1, key, value, setter);
    }

    private void addToggle(int row, int column, int columns, String key, boolean value, Consumer<Boolean> setter) {
        CycleButton<Boolean> toggle = CycleButton.onOffBuilder(value).create(columnX(column, columns), rowY(row), columnWidth(columns), 20,
                Component.translatable("screen.tinyrelics." + key), (button, newValue) -> setter.accept(newValue));
        toggle.setTooltip(Tooltip.create(Component.translatable("screen.tinyrelics." + key + ".tip")));
        addRenderableWidget(toggle);
    }

    private void addSlider(int row, String key, int min, int max, IntSupplier getter, IntConsumer setter) {
        addSlider(row, 0, 1, key, min, max, getter, setter);
    }

    private void addSlider(int row, int column, int columns, String key, int min, int max, IntSupplier getter, IntConsumer setter) {
        IntSlider slider = new IntSlider(columnX(column, columns), rowY(row), columnWidth(columns), 20, "screen.tinyrelics." + key, min, max, getter, setter);
        slider.setTooltip(Tooltip.create(Component.translatable("screen.tinyrelics." + key + ".tip")));
        addRenderableWidget(slider);
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

        graphics.drawString(this.font, tabName(this.selected), x + 40, this.top + 29, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("screen.tinyrelics.stats." + id), x + 40, this.top + 42, 0xA0A0A0);
        graphics.drawWordWrap(this.font, Component.translatable("screen.tinyrelics.desc." + id), x, this.top + 62, RIGHT_WIDTH, 0xD8D8D8);
        graphics.drawString(this.font, Component.translatable("screen.tinyrelics.settings"), x, this.top + 118, 0xFFD070);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A slider over whole numbers that shows its value in its own label. */
    private static class IntSlider extends AbstractSliderButton {
        private final String key;
        private final int min;
        private final int max;
        private final IntSupplier getter;
        private final IntConsumer setter;

        IntSlider(int x, int y, int width, int height, String key, int min, int max, IntSupplier getter, IntConsumer setter) {
            super(x, y, width, height, Component.empty(), (getter.getAsInt() - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(this.key, this.getter.getAsInt()));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.min + (int) Math.round(this.value * (this.max - this.min)));
        }
    }
}
