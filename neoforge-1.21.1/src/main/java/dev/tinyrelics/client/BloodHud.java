package dev.tinyrelics.client;

import dev.tinyrelics.BloodSet;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.Gui;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The Blood Master's blood hearts: their own row right above the health hearts, the way the
 * golden hearts of a golden apple sit, with the armor bar moved up one row. Drawn only while
 * the full set is on. Full hearts drip blood now and then.
 */
public final class BloodHud {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "textures/gui/blood_hearts.png");
    /** Cells left to right: empty, half, full, then four frames of a drop forming under a full heart and falling. */
    private static final int CELL_WIDTH = 9;
    private static final int CELL_HEIGHT = 12;
    private static final int SHEET_WIDTH = 63;
    private static final int SHEET_HEIGHT = 12;
    /** The same step as the game's own hearts, so the rows line up. */
    private static final int SPACING = 8;
    private static final int ROW_HEIGHT = 10;
    /** A drop takes eight ticks of a forty-tick cycle; the hearts drip one after another. */
    private static final int DRIP_CYCLE = 40;

    private BloodHud() {
    }

    /** Runs right after the game has drawn the health hearts and before it draws the armor bar. */
    public static void onRender(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        float hearts = ClientEffects.bloodHearts;
        if (player == null || hearts < 0.0F || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        Gui gui = mc.gui;
        GuiGraphics graphics = event.getGuiGraphics();
        // leftHeight is how far up the left column of bars has grown: our row goes there, and
        // raising it by a row makes the armor bar (drawn next) step up.
        int y = graphics.guiHeight() - gui.leftHeight;
        gui.leftHeight += ROW_HEIGHT;
        int x = graphics.guiWidth() / 2 - 91;
        int slots = (int) (BloodSet.MAX_HEARTS / 2.0F);
        for (int i = 0; i < slots; i++) {
            float filled = hearts - i * 2.0F;
            int cell = filled >= 2.0F ? 2 : filled >= 1.0F ? 1 : 0;
            if (cell == 2) {
                int moment = (player.tickCount + i * 11) % DRIP_CYCLE;
                if (moment < 8) {
                    cell = 3 + moment / 2;
                }
            }
            graphics.blit(TEXTURE, x + i * SPACING, y, CELL_WIDTH, CELL_HEIGHT, cell * CELL_WIDTH, 0.0F,
                    CELL_WIDTH, CELL_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        }
    }
}
