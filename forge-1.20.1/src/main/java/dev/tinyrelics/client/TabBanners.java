package dev.tinyrelics.client;

import dev.tinyrelics.RelicTab;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * The banner pictures in the mod's creative tab. A banner is not an item: it is
 * drawn over two rows of invisible filler items, and those cells ignore every
 * click and key, so nothing can be picked up from under it.
 */
public final class TabBanners {
    /** Size of a banner on screen, in GUI pixels: nine slots wide, two slots tall, borders included. */
    private static final int WIDTH = RelicTab.COLUMNS * 18;
    private static final int HEIGHT = RelicTab.BANNER_ROWS * 18;
    /** The texture holds the frames one under another, each drawn at four times the on-screen size. */
    private static final int FRAME_WIDTH = WIDTH * 4;
    private static final int FRAME_HEIGHT = HEIGHT * 4;
    /** Per banner, same order as the textures: how many frames its picture holds and how long each one stays. */
    private static final int[] FRAMES = {0, 12, 24, 36};
    private static final int[] FRAME_MILLIS = {0, 150, 100, 100};
    /** The scrolling grid of the creative screen: five rows of nine slots. */
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 17;
    private static final int GRID_ROWS = 5;

    /** Indexed by banner number; number 0 is "no banner". */
    private static final ResourceLocation[] TEXTURES = {
            null,
            new ResourceLocation(TinyRelics.MODID, "textures/gui/banner_necromancer.png"),
            new ResourceLocation(TinyRelics.MODID, "textures/gui/banner_thor.png"),
            new ResourceLocation(TinyRelics.MODID, "textures/gui/banner_chaos.png"),
    };

    private TabBanners() {
    }

    /** Runs after the slots are drawn and before tooltips, already shifted to the window's corner. */
    public static void onForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        boolean[] drawn = new boolean[TEXTURES.length];
        for (Slot slot : screen.getMenu().slots) {
            int code = TinyRelics.fillerCode(slot.getItem());
            int banner = RelicTab.bannerOf(code);
            if (banner <= 0 || banner >= TEXTURES.length || drawn[banner]) {
                continue;
            }
            drawn[banner] = true;
            // Any visible cell tells where the banner's corner is, even when the list is scrolled halfway past it.
            int cell = RelicTab.cellOf(code);
            int x = slot.x - 1 - (cell % RelicTab.COLUMNS) * 18;
            int y = slot.y - 1 - (cell / RelicTab.COLUMNS) * 18;
            int frame = (int) (System.currentTimeMillis() / FRAME_MILLIS[banner] % FRAMES[banner]);

            int left = screen.getGuiLeft();
            int top = screen.getGuiTop();
            graphics.enableScissor(left + GRID_LEFT, top + GRID_TOP, left + GRID_LEFT + WIDTH, top + GRID_TOP + GRID_ROWS * 18);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.blit(TEXTURES[banner], x, y, WIDTH, HEIGHT, 0.0F, frame * FRAME_HEIGHT,
                    FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT * FRAMES[banner]);
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private static boolean overFiller(Screen screen) {
        if (!(screen instanceof CreativeModeInventoryScreen creative)) {
            return false;
        }
        Slot slot = creative.getSlotUnderMouse();
        return slot != null && TinyRelics.fillerCode(slot.getItem()) >= 0;
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (overFiller(event.getScreen())) {
            event.setCanceled(true);
        }
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (overFiller(event.getScreen())) {
            event.setCanceled(true);
        }
    }

    /** Number keys and the like would move the hovered item to the hotbar: not over a filler. */
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!overFiller(event.getScreen())) {
            return;
        }
        boolean closes = event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE
                || Minecraft.getInstance().options.keyInventory.matches(event.getKeyCode(), event.getScanCode());
        if (!closes) {
            event.setCanceled(true);
        }
    }

    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (TinyRelics.fillerCode(event.getItemStack()) >= 0) {
            event.setCanceled(true);
        }
    }
}
