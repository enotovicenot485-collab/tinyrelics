package dev.tinyrelics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tinyrelics.ShadowMachineBlockEntity;
import dev.tinyrelics.ShadowMachineMenu;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The window of the shadow loom and of the phantom spooler. Each has one picture built by
 * tools/shadow_machines.py, two texels per GUI pixel: the window with its slots, and the parts that move.
 */
public class ShadowMachineScreen extends AbstractContainerScreen<ShadowMachineMenu> {
    private static final ResourceLocation LOOM = new ResourceLocation(TinyRelics.MODID, "textures/gui/shadow_loom.png");
    private static final ResourceLocation SPOOLER = new ResourceLocation(TinyRelics.MODID, "textures/gui/phantom_spooler.png");
    private static final int SHEET = 512;
    /** The loom's threads, where the cloth grows: corner and size. */
    private static final int WARP_X = 59;
    private static final int WARP_Y = 36;
    private static final int WARP_WIDTH = 61;
    private static final int WARP_HEIGHT = 52;
    /** The colour of the phantom thread. */
    private static final int THREAD = 0xFFC4DDE6;
    private final ResourceLocation texture;

    public ShadowMachineScreen(ShadowMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.texture = menu.spooler ? SPOOLER : LOOM;
        this.imageWidth = 176;
        this.imageHeight = menu.spooler ? 195 : 201;
    }

    /** Draws a piece of the picture: place and size in GUI pixels, corner on the sheet in texels. */
    private void part(GuiGraphics graphics, int x, int y, int width, int height, int u, int v) {
        graphics.blit(this.texture, this.leftPos + x, this.topPos + y, width, height, (float) u, (float) v, width * 2, height * 2, SHEET, SHEET);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        part(graphics, 0, 0, this.imageWidth, this.imageHeight, 0, 0);
        long now = System.currentTimeMillis();
        boolean working = this.menu.night() && this.menu.loaded();
        if (this.menu.spooler) {
            spooler(graphics, now, this.menu.progress() / (float) ShadowMachineBlockEntity.SPOOL_TICKS, working);
        } else {
            loom(graphics, now, this.menu.progress() / (float) ShadowMachineBlockEntity.LOOM_TICKS, working);
        }
    }

    private void loom(GuiGraphics graphics, long now, float t, boolean working) {
        if (this.menu.night()) {
            part(graphics, 79, 5, 20, 20, 430, 110);
        }
        if (!this.menu.loaded()) {
            return;
        }
        // the cloth grows up the threads from the bottom, row after row
        int woven = Math.max(1, Math.min(WARP_HEIGHT, (int) (t * WARP_HEIGHT)));
        int edge = WARP_Y + WARP_HEIGHT - woven;
        part(graphics, WARP_X, edge, WARP_WIDTH, woven, 360, (WARP_HEIGHT - woven) * 2);
        // the shuttle runs along the edge of the cloth, there and back; by day it lies where it stopped
        float run = working ? (now % 900L) / 900.0F : 0.0F;
        float across = run < 0.5F ? run * 2.0F : 2.0F - run * 2.0F;
        int shuttle = WARP_X + (int) (across * (WARP_WIDTH - 30));
        part(graphics, shuttle, Math.max(WARP_Y, edge - 5), 30, 8, 360, 110);
        if (working) {
            // sparks where the shuttle has just passed
            for (int i = 0; i < 4; i++) {
                if ((now / 130L + i) % 3L != 0L) {
                    int x = shuttle + (run < 0.5F ? -6 - i * 5 : 30 + i * 5);
                    if (x >= WARP_X && x <= WARP_X + WARP_WIDTH - 8) {
                        part(graphics, x, Math.max(WARP_Y, edge - 6 + (i % 2) * 3), 8, 8, 360 + i * 20, 160);
                    }
                }
            }
        }
    }

    private void spooler(GuiGraphics graphics, long now, float t, boolean working) {
        if (this.menu.night()) {
            part(graphics, 76, 6, 22, 22, 446, 50);
        }
        // the reel: empty, and over it the full one, showing through more and more as the thread winds on
        int frame = working ? (int) (now / 70L % 4L) : 0;
        part(graphics, 86, 35, 40, 40, frame * 84, 394);
        if (this.menu.loaded()) {
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, 0.15F + t * 0.85F));
            part(graphics, 86, 35, 40, 40, 360, frame * 84);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            // the piece of wing in the clamp gets smaller as it is unpicked
            int height = 6 + (int) ((1.0F - t) * 14.0F);
            int width = 5 + (int) ((1.0F - t) * 9.0F);
            graphics.blit(this.texture, this.leftPos + 65 - width / 2, this.topPos + 56 - height / 2, width, height, 446.0F, 0.0F, 28, 40, SHEET, SHEET);
            // the thread from the wing to the top of the reel, trembling while it is pulled
            int tremble = working ? (int) (now / 90L % 2L) : 0;
            for (int i = 0; i <= 22; i++) {
                int x = this.leftPos + 70 + i;
                int y = this.topPos + 54 - (i * 13) / 22 + (i > 4 && i < 18 ? tremble : 0);
                graphics.fill(x, y, x + 1, y + 1, THREAD);
            }
            if (working) {
                // wisps of ghostly dust ride the thread
                for (int i = 0; i < 3; i++) {
                    float along = ((now % 1200L) / 1200.0F + i / 3.0F) % 1.0F;
                    part(graphics, 66 + (int) (along * 22.0F), 46 - (int) (along * 13.0F), 8, 12, 446 + i * 20, 100);
                }
            }
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
        boolean overMoon = this.menu.spooler ? isHovering(76, 6, 22, 22, mouseX, mouseY) : isHovering(79, 5, 20, 20, mouseX, mouseY);
        if (overMoon) {
            graphics.renderTooltip(this.font, Component.translatable(this.menu.night()
                    ? "container.tinyrelics.shadow.night" : "container.tinyrelics.shadow.day"), mouseX, mouseY);
        }
    }
}
