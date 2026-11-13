package dev.tinyrelics.client;

import dev.tinyrelics.BloodSet;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The Blood Master's blood hearts: a row of four above the armor bar, drawn only while
 * the full set is on. Full hearts drip blood now and then.
 */
public final class BloodHud {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "textures/gui/blood_hearts.png");
    /** Cells left to right: empty, half, full, then four frames of a drop forming under a full heart and falling. */
    private static final int CELL_WIDTH = 13;
    private static final int CELL_HEIGHT = 18;
    private static final int SHEET_WIDTH = 91;
    private static final int SHEET_HEIGHT = 18;
    /** The heart itself is 11 high; the rest of a cell is room for the drop. */
    private static final int SPACING = 14;
    /** A drop takes eight ticks of a forty-tick cycle; the hearts drip one after another. */
    private static final int DRIP_CYCLE = 40;

    private BloodHud() {
    }

    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        float hearts = ClientEffects.bloodHearts;
        if (player == null || hearts < 0.0F || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // The same sums the game does for its own bars: rows of health, then the armor row, then us.
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int healthRows = Mth.ceil((maxHealth + Mth.ceil(player.getAbsorptionAmount())) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);
        int y = height - 39 - (healthRows - 1) * rowHeight - (player.getArmorValue() > 0 ? 10 : 0) - CELL_HEIGHT - 1;
        int x = width / 2 - 91;
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
