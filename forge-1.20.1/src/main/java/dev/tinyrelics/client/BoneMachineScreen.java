package dev.tinyrelics.client;

import dev.tinyrelics.BoneMachineBlockEntity;
import dev.tinyrelics.BoneMachineMenu;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The window of the bone crusher and of the soul press. Each has one picture built by tools/machines.py,
 * two texels per GUI pixel: the window with its slots, and the parts that move.
 */
public class BoneMachineScreen extends AbstractContainerScreen<BoneMachineMenu> {
    private static final ResourceLocation CRUSHER = new ResourceLocation(TinyRelics.MODID, "textures/gui/bone_crusher.png");
    private static final ResourceLocation PRESS = new ResourceLocation(TinyRelics.MODID, "textures/gui/soul_press.png");
    private static final int SHEET = 512;
    /** How far the head of the press comes down. */
    private static final int PRESS_TRAVEL = 9;
    private final ResourceLocation texture;

    public BoneMachineScreen(BoneMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.texture = menu.press ? PRESS : CRUSHER;
        this.imageWidth = 176;
        this.imageHeight = menu.press ? 189 : 212;
    }

    /** Draws a piece of the picture: place and size in GUI pixels, corner on the sheet in texels. */
    private void part(GuiGraphics graphics, int x, int y, int width, int height, int u, int v) {
        graphics.blit(this.texture, this.leftPos + x, this.topPos + y, width, height, (float) u, (float) v, width * 2, height * 2, SHEET, SHEET);
    }

    /** The gauge: full of green fire as long as the gunpowder lasts. */
    private void gauge(GuiGraphics graphics, int x, int y, int width, int height) {
        int fuel = Math.min(this.menu.fuel(), BoneMachineBlockEntity.JOBS_PER_GUNPOWDER);
        if (fuel > 0) {
            int filled = Math.max(1, height * fuel / BoneMachineBlockEntity.JOBS_PER_GUNPOWDER);
            part(graphics, x, y + height - filled, width, filled, 360, (height - filled) * 2);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        part(graphics, 0, 0, this.imageWidth, this.imageHeight, 0, 0);
        long now = System.currentTimeMillis();
        int progress = this.menu.progress();
        if (this.menu.press) {
            press(graphics, now, progress / (float) BoneMachineBlockEntity.PRESS_TICKS, progress > 0);
        } else {
            crusher(graphics, now, progress / (float) BoneMachineBlockEntity.CRUSH_TICKS, progress > 0);
        }
    }

    /** A little sprite drawn smaller than it is on the sheet. */
    private void chip(GuiGraphics graphics, float x, float y, int size, int u, int v) {
        graphics.blit(this.texture, this.leftPos + Math.round(x), this.topPos + Math.round(y), size, size, (float) u, (float) v, 28, 28, SHEET, SHEET);
    }

    private void crusher(GuiGraphics graphics, long now, float t, boolean working) {
        gauge(graphics, 21, 31, 14, 35);
        int mode = this.menu.mode();
        // bones are the first row of little sprites, lumps of soul sand the second
        int row = mode == BoneMachineBlockEntity.MODE_PLAIN ? 0 : 36;
        if (working) {
            // the piece drops onto the rollers and is dragged down between them, shaking; the rollers hide what they have eaten
            float y = t < 0.3F ? 26.0F + t / 0.3F * 24.0F : 50.0F + (t - 0.3F) / 0.7F * 16.0F;
            int shake = t < 0.3F ? 0 : (int) (now / 45L % 3L) - 1;
            part(graphics, 86 + shake, Math.round(y), 14, 14, 400, row);
        }
        // the rollers turn towards each other
        int frame = working ? (int) (now / 45L % 6L) : 0;
        part(graphics, 60, 49, 34, 34, frame * 72, 440);
        part(graphics, 91, 49, 34, 34, ((6 - frame) % 6) * 72, 440);
        if (!working) {
            return;
        }
        if (t >= 0.25F) {
            // splinters fly up out of the gap in arcs, left and right, one after another
            for (int i = 0; i < 8; i++) {
                float p = ((now % 800L) / 800.0F + i / 8.0F) % 1.0F;
                float side = (i % 2 == 0 ? 1.0F : -1.0F) * (7.0F + (i * 5) % 11);
                float height = 12.0F + (i * 7) % 10;
                float x = 92.5F + side * p * 1.5F;
                float y = 60.0F - (float) Math.sin(p * Math.PI) * height + p * 8.0F;
                int size = 4 + i % 3;
                chip(graphics, x - size / 2.0F, y - size / 2.0F, size, 436 + (i % 2) * 36, row);
            }
        }
        if (mode == BoneMachineBlockEntity.MODE_PLAIN && t >= 0.4F) {
            part(graphics, 85, 77, 16, 24, 360 + (int) (now / 100L % 4L) * 36, 80);
        }
        if (mode == BoneMachineBlockEntity.MODE_SAND_SOUL && t >= 0.5F) {
            // the soul drifts down into the slot, swaying
            int sway = Math.round((float) Math.sin(now / 180.0D) * 2.0F);
            part(graphics, 85 + sway, 64 + (int) ((t - 0.5F) / 0.5F * 22.0F), 16, 28, 360 + (int) (now / 150L % 4L) * 36, 140);
        }
    }

    private void press(GuiGraphics graphics, long now, float t, boolean working) {
        gauge(graphics, 39, 66, 10, 24);
        int down = 0;
        if (working) {
            if (t >= 0.5F && t < 0.6F) {
                down = (int) ((t - 0.5F) / 0.1F * PRESS_TRAVEL);
            } else if (t >= 0.6F && t < 0.75F) {
                down = PRESS_TRAVEL;
            } else if (t >= 0.75F && t < 0.9F) {
                down = (int) ((1.0F - (t - 0.75F) / 0.15F) * PRESS_TRAVEL);
            }
            if (t < 0.5F) {
                // the heap of powder rides the belt under the press
                float along = t / 0.5F;
                part(graphics, 56 + (int) (along * 27.0F), 59 - (int) (along * 6.0F), 14, 10, 360, 60);
            } else if (t < 0.6F) {
                part(graphics, 83, 53, 14, 10, 360, 60);
            } else if (t >= 0.72F) {
                // and the ingot rides on, to the slot on the right
                float along = (t - 0.72F) / 0.28F;
                part(graphics, 83 + (int) (along * 42.0F), 53 + (int) (along * 6.0F), 14, 10, 392, 60);
            }
        }
        // the screw stretches as the head goes down
        graphics.blit(this.texture, this.leftPos + 81, this.topPos + 13, 17, 20 + down, 460.0F, 0.0F, 34, 40, SHEET, SHEET);
        part(graphics, 75, 33 + down, 29, 22, 390, 0);
        if (working && t >= 0.58F && t < 0.72F) {
            int frame = Math.min(2, (int) ((t - 0.58F) / 0.14F * 3.0F));
            part(graphics, 79, 44, 22, 22, 360 + frame * 48, 90);
        }
    }

    /** The pictures leave no room for the usual labels. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        boolean overGauge = this.menu.press ? isHovering(39, 66, 10, 24, mouseX, mouseY) : isHovering(21, 31, 14, 35, mouseX, mouseY);
        if (overGauge) {
            graphics.renderTooltip(this.font, Component.translatable("container.tinyrelics.machine.fuel",
                    this.menu.fuel(), BoneMachineBlockEntity.JOBS_PER_GUNPOWDER), mouseX, mouseY);
        }
    }
}
