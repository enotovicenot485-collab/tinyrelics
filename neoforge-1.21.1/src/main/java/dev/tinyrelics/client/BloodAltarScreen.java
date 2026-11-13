package dev.tinyrelics.client;

import dev.tinyrelics.BloodAltarBlockEntity;
import dev.tinyrelics.BloodAltarMenu;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The blood altar's window. Everything comes from one picture built by tools/altar.py,
 * two texels per GUI pixel: the window with its slots, and the parts that move.
 */
public class BloodAltarScreen extends AbstractContainerScreen<BloodAltarMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "textures/gui/blood_altar.png");
    private static final int SHEET = 512;
    private static final int LABEL_COLOUR = 0xE8C070;
    /** The vial is 38 high; its water runs from row 11 to row 32. */
    private static final int WATER_TOP = 11;
    private static final int WATER_BOTTOM = 32;
    private static final int VIAL_HEIGHT = 38;
    private static final int CHANNEL_WIDTH = 52;

    public BloodAltarScreen(BloodAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** Draws a piece of the picture: place and size in GUI pixels, corner on the sheet in texels. */
    private void part(GuiGraphics graphics, int x, int y, int width, int height, int u, int v) {
        graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, width, height, (float) u, (float) v, width * 2, height * 2, SHEET, SHEET);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        part(graphics, 0, 0, 176, 166, 0, 0);
        int water = this.menu.water();
        if (water > 0) {
            int top = WATER_BOTTOM - (WATER_BOTTOM - WATER_TOP) * Math.min(water, BloodAltarBlockEntity.MAX_WATER) / BloodAltarBlockEntity.MAX_WATER;
            part(graphics, 34, 24 + top, 14, VIAL_HEIGHT - top, 112, 340 + top * 2);
        }
        int progress = this.menu.progress();
        if (progress > 0) {
            // the slot with the blood glows, blood runs along the channel, the basin boils
            part(graphics, 55, 34, 18, 18, 0, 420);
            int filled = Math.max(1, CHANNEL_WIDTH * progress / BloodAltarBlockEntity.WORK_TICKS);
            part(graphics, 74, 38, filled, 10, 0, 340);
            int frame = 1 + (int) (System.currentTimeMillis() / 180L % 3L);
            part(graphics, 87, 30, 26, 26, 160 + frame * 56, 340);
        }
    }

    /** The usual dark grey labels cannot be read on dark red. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOUR, true);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOUR, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(34, 24, 14, VIAL_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(this.font, Component.translatable("container.tinyrelics.blood_altar.water",
                    this.menu.water(), BloodAltarBlockEntity.MAX_WATER), mouseX, mouseY);
        }
    }
}
