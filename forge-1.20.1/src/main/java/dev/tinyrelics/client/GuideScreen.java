package dev.tinyrelics.client;

import dev.tinyrelics.TinyRelics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tiny Relics guide: an open book with a chapter per set, tabs on its right edge and arrows at the
 * bottom. A chapter is a list of blocks (title, banner, text, recipe) poured onto pages one after another;
 * text that does not fit runs on to the next page, so the texts can change freely. Recipes are read from
 * the game itself, so a changed recipe shows up here by itself, and a recipe switched off on the server
 * says so. Pictures come from tools/guide.py; the positions below must match it.
 */
public class GuideScreen extends Screen {
    private static final ResourceLocation BOOK = rl("textures/gui/guide_book.png");
    private static final ResourceLocation PARTS = rl("textures/gui/guide_parts.png");
    /** The book, and the two pages' writing areas, in GUI pixels. */
    private static final int W = 400;
    private static final int H = 233;
    private static final int[] PAGE_X = {38, 226};
    private static final int PAGE_W = 136;
    private static final int PAGE_TOP = 22;
    private static final int PAGE_BOTTOM = 191;
    private static final int TEXT = 0xFF3B2A1A;
    /** The tabs stick out of the right edge of the cover. */
    private static final int TAB_X = 391;
    private static final int TAB_Y = 18;
    private static final int TAB_STEP = 26;
    private static final int TAB_W = 30;
    private static final int TAB_H = 22;
    /** The crafting panel: grid, arrow and result, and where the items go in it. */
    private static final int GRID_W = 66;
    private static final int GRID_H = 65;
    private static final int[] CELL = {7, 25, 43};
    private static final int PANEL_W = 116;

    private static final int TITLE = 0;
    private static final int BANNER = 1;
    private static final int TEXT_BLOCK = 2;
    private static final int RECIPE = 3;
    private static final int DIVIDER = 4;

    private record Block(int kind, String value, int number) {
    }

    private record Chapter(String id, int colour, ItemLike icon, int banner, List<Block> blocks) {
    }

    /** One thing placed on a page: a line of text, a banner, a recipe, a divider. */
    private record Placed(int kind, int y, FormattedCharSequence line, int colour, int number, String recipe) {
    }

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<List<Placed>> pages = new ArrayList<>();
    private final List<Integer> chapterStart = new ArrayList<>();
    /** The left page of the open spread. */
    private int spread;
    private float scale = 1.0F;
    private int left;
    private int top;

    public GuideScreen() {
        super(Component.translatable("item.tinyrelics.guide_book"));
        chapter("home", 0x2E5E2E, TinyRelics.GUIDE_BOOK.get(), 0, text("home.text"), recipe("guide_book"));
        chapter("necro", 0x1F4E79, TinyRelics.UNDEAD_STAFF.get(), 1, text("necro.armor"), text("necro.staff"), text("necro.resources"),
                recipe("bone_crusher"), recipe("soul_press"), recipe("necro_helmet"), recipe("necro_chestplate"),
                recipe("necro_leggings"), recipe("necro_boots"), recipe("undead_staff"));
        chapter("chaos", 0x5B2A86, TinyRelics.CHAOS_BOOMERANG.get(), 3, text("chaos.armor"), text("chaos.boomerang"), text("chaos.resources"),
                recipe("shadow_loom"), recipe("phantom_spooler"), recipe("chaos_helmet"), recipe("chaos_chestplate"),
                recipe("chaos_leggings"), recipe("chaos_boots"), recipe("chaos_boomerang"));
        chapter("blood", 0x8B1A1A, TinyRelics.BLOOD_BLADE.get(), 4, text("blood.armor"), text("blood.blade"), text("blood.resources"),
                recipe("blood_altar"), recipe("blood_helmet"), recipe("blood_chestplate"), recipe("blood_leggings"),
                recipe("blood_boots"), recipe("blood_blade"));
        chapter("thor", 0x7A5A00, TinyRelics.THOR_HAMMER.get(), 2, text("thor.hammer"), text("thor.armor"));
        chapter("shop", 0x5A3A1A, TinyRelics.KEEPING_CHARM.get(), 5, text("shop.charm"), recipe("keeping_charm"), text("shop.more"));
    }

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(TinyRelics.MODID, path);
    }

    private static Block text(String key) {
        return new Block(TEXT_BLOCK, "guide.tinyrelics." + key, 0);
    }

    private static Block recipe(String id) {
        return new Block(RECIPE, id, 0);
    }

    private void chapter(String id, int colour, ItemLike icon, int banner, Block... blocks) {
        List<Block> all = new ArrayList<>();
        all.add(new Block(TITLE, "guide.tinyrelics." + id + ".title", 0));
        if (banner > 0) {
            all.add(new Block(BANNER, "", banner));
        } else {
            all.add(new Block(DIVIDER, "", 0));
        }
        all.addAll(List.of(blocks));
        this.chapters.add(new Chapter(id, colour, icon, banner, all));
    }

    /** Opens the book when it is used: listened to on the client only. */
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getItemStack().is(TinyRelics.GUIDE_BOOK.get())) {
            Minecraft.getInstance().setScreen(new GuideScreen());
        }
    }

    // ---- Pouring the chapters onto pages.

    @Override
    protected void init() {
        // the tabs stick out 35 pixels to the right of the book: the whole thing is centred and kept on screen
        int wide = W + 35;
        this.scale = Math.min(1.0F, Math.min((this.width - 8) / (float) wide, (this.height - 8) / (float) H));
        this.left = (int) ((this.width / this.scale - wide) / 2);
        this.top = (int) ((this.height / this.scale - H) / 2);
        this.pages.clear();
        this.chapterStart.clear();
        for (Chapter chapter : this.chapters) {
            if (this.pages.size() % 2 == 1) {
                this.pages.add(new ArrayList<>());          // every chapter opens on a left page
            }
            this.chapterStart.add(this.pages.size());
            pour(chapter);
        }
        if (this.pages.size() % 2 == 1) {
            this.pages.add(new ArrayList<>());
        }
        this.spread = Math.min(this.spread, this.pages.size() - 2);
    }

    private int y;

    private List<Placed> page() {
        return this.pages.get(this.pages.size() - 1);
    }

    /** Room for this many pixels on the current page, or a new page. */
    private void room(int height) {
        if (this.y + height > PAGE_BOTTOM - PAGE_TOP) {
            this.pages.add(new ArrayList<>());
            this.y = 0;
        }
    }

    private void pour(Chapter chapter) {
        this.pages.add(new ArrayList<>());
        this.y = 0;
        for (Block block : chapter.blocks()) {
            switch (block.kind()) {
                case TITLE -> {
                    Component title = Component.translatable(block.value()).withStyle(ChatFormatting.BOLD);
                    page().add(new Placed(TITLE, this.y, title.getVisualOrderText(), chapter.colour(), 0, null));
                    this.y += 13;
                }
                case BANNER -> {
                    room(42);
                    page().add(new Placed(BANNER, this.y, null, 0, block.number(), null));
                    this.y += 45;
                }
                case DIVIDER -> {
                    room(20);
                    page().add(new Placed(DIVIDER, this.y, null, 0, 0, null));
                    this.y += 22;
                }
                case TEXT_BLOCK -> pourText(Component.translatable(block.value()).getString(), chapter.colour());
                default -> {
                    room(78);
                    page().add(new Placed(RECIPE, this.y, null, 0, 0, block.value()));
                    this.y += 80;
                }
            }
        }
    }

    /** Lines starting with "# " are headings in the chapter's colour; an empty line is a small gap. */
    private void pourText(String text, int colour) {
        for (String paragraph : text.split("\n")) {
            if (paragraph.isBlank()) {
                this.y += 4;
                continue;
            }
            boolean heading = paragraph.startsWith("# ");
            Component line = heading ? Component.literal(paragraph.substring(2)).withStyle(ChatFormatting.BOLD) : Component.literal(paragraph);
            if (heading && this.y > 0) {
                this.y += 4;
                room(22);                                    // a heading never stays alone at the bottom of a page
            }
            for (FormattedCharSequence piece : this.font.split(line, PAGE_W)) {
                room(10);
                page().add(new Placed(TEXT_BLOCK, this.y, piece, heading ? colour : TEXT, 0, null));
                this.y += 10;
            }
            this.y += 2;
        }
        this.y += 3;
    }

    // ---- Drawing.

    private ItemStack hovered = ItemStack.EMPTY;
    private Component hoveredText;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
       renderBackground(graphics);
        int mx = (int) (mouseX / this.scale);
        int my = (int) (mouseY / this.scale);
        this.hovered = ItemStack.EMPTY;
        this.hoveredText = null;
        graphics.pose().pushPose();
        graphics.pose().scale(this.scale, this.scale, 1.0F);
        tabs(graphics, mx, my, false);
        graphics.blit(BOOK, this.left, this.top, W, H, 0.0F, 0.0F, W * 2, H * 2, W * 2, H * 2);
        tabs(graphics, mx, my, true);
        for (int side = 0; side < 2; side++) {
            int index = this.spread + side;
            if (index < this.pages.size()) {
                drawPage(graphics, this.pages.get(index), this.left + PAGE_X[side], this.top + PAGE_TOP, mx, my);
                String number = String.valueOf(index + 1);
                graphics.drawString(this.font, number, this.left + PAGE_X[side] + (PAGE_W - this.font.width(number)) / 2,
                        this.top + 199, 0xFF8A6A4A, false);
            }
        }
        if (this.spread > 0) {
            arrow(graphics, this.left + 40, mx, my, 0);
        }
        if (this.spread + 2 < this.pages.size()) {
            arrow(graphics, this.left + W - 78, mx, my, 1);
        }
        graphics.pose().popPose();
       super.render(graphics, mouseX, mouseY, partialTick);
        if (!this.hovered.isEmpty()) {
            graphics.renderTooltip(this.font, this.hovered, mouseX, mouseY);
        } else if (this.hoveredText != null) {
            graphics.renderTooltip(this.font, this.hoveredText, mouseX, mouseY);
        }
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void arrow(GuiGraphics graphics, int x, int mx, int my, int which) {
        int y = this.top + 196;
        boolean lit = over(mx, my, x, y, 20, 16);
        graphics.blit(PARTS, x, y, 20, 16, (float) (which * 40 + (lit ? 80 : 0)), 0.0F, 40, 32, 512, 512);
    }

    /** The tab of the open chapter is drawn over the cover and sticks out further; the rest go under it. */
    private void tabs(GuiGraphics graphics, int mx, int my, boolean front) {
        int open = openChapter();
        for (int i = 0; i < this.chapters.size(); i++) {
            if ((i == open) != front) {
                continue;
            }
            int x = this.left + TAB_X + (i == open ? 5 : 0);
            int y = this.top + TAB_Y + i * TAB_STEP;
            boolean lit = i == open || over(mx, my, x, y, TAB_W, TAB_H);
            graphics.blit(PARTS, x, y, TAB_W, TAB_H, (float) (i * 64), lit ? 88.0F : 40.0F, 60, 44, 512, 512);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 8.5F, y + 4.5F, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(new ItemStack(this.chapters.get(i).icon()), 0, 0);
            graphics.pose().popPose();
            if (over(mx, my, x, y, TAB_W, TAB_H)) {
                this.hoveredText = Component.translatable("guide.tinyrelics." + this.chapters.get(i).id() + ".title");
            }
        }
    }

    private int openChapter() {
        int open = 0;
        for (int i = 0; i < this.chapterStart.size(); i++) {
            if (this.chapterStart.get(i) <= this.spread) {
                open = i;
            }
        }
        return open;
    }

    private void drawPage(GuiGraphics graphics, List<Placed> page, int x, int y, int mx, int my) {
        for (Placed placed : page) {
            int at = y + placed.y();
            switch (placed.kind()) {
                case TITLE -> graphics.drawString(this.font, placed.line(), x + (PAGE_W - this.font.width(placed.line())) / 2, at + 2,
                        0xFF000000 | placed.colour(), false);
                case TEXT_BLOCK -> graphics.drawString(this.font, placed.line(), x, at, 0xFF000000 | placed.colour(), false);
                case DIVIDER -> graphics.blit(PARTS, x, at + 2, PAGE_W, 16, 0.0F, 280.0F, 272, 32, 512, 512);
                case BANNER -> {
                    // the banner reaches under the frame's inner line on every side, so no gap shows
                    TabBanners.draw(graphics, placed.number(), x + 6, at + 7, PAGE_W - 12, 27);
                    graphics.blit(PARTS, x, at, PAGE_W, 40, 0.0F, 320.0F, 272, 80, 512, 512);
                }
                default -> drawRecipe(graphics, placed.recipe(), x, at, mx, my);
            }
        }
    }

    /** The recipe as the game has it now; null if there is none (switched off on the server). */
    private static Recipe<?> find(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
       return mc.level.getRecipeManager().byKey(rl(id)).orElse(null);
    }

    private void drawRecipe(GuiGraphics graphics, String id, int x, int y, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        Recipe<?> recipe = find(id);
        ItemStack result = recipe == null || mc.level == null ? ItemStack.EMPTY : recipe.getResultItem(mc.level.registryAccess());
        Component name = result.isEmpty() ? Component.translatable(nameKey(id)) : result.getHoverName();
        graphics.drawString(this.font, name.copy().withStyle(ChatFormatting.BOLD), x, y, TEXT, false);
        if (recipe == null) {
            for (FormattedCharSequence line : this.font.split(Component.translatable("guide.tinyrelics.off"), PAGE_W)) {
                y += 11;
                graphics.drawString(this.font, line, x, y, 0xFF8B1A1A, false);
            }
            return;
        }
        int panelX = x + (PAGE_W - PANEL_W) / 2;
        int panelY = y + 11;
        graphics.blit(PARTS, panelX, panelY, GRID_W, GRID_H, 0.0F, 140.0F, 132, 130, 512, 512);
        graphics.blit(PARTS, panelX + GRID_W + 2, panelY + 27, 16, 11, 140.0F, 190.0F, 32, 22, 512, 512);
        graphics.blit(PARTS, panelX + GRID_W + 20, panelY + 18, 30, 28, 180.0F, 160.0F, 60, 56, 512, 512);
        List<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int height = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : (ingredients.size() + 2) / 3;
        int dx = (3 - width) / 2;
        int dy = (3 - height) / 2;
        long second = System.currentTimeMillis() / 1000L;
        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            ItemStack[] options = ingredients.get(i).getItems();
            if (options.length == 0) {
                continue;
            }
            ItemStack stack = options[(int) (second % options.length)];
            int sx = panelX + CELL[i % width + dx];
            int sy = panelY + CELL[i / width + dy];
            item(graphics, stack, sx, sy, mx, my);
        }
        item(graphics, result, panelX + GRID_W + 27, panelY + 24, mx, my);
    }

    private static String nameKey(String id) {
        return (id.equals("bone_crusher") || id.equals("soul_press") || id.equals("blood_altar") || id.equals("shadow_loom")
                || id.equals("phantom_spooler") ? "block.tinyrelics." : "item.tinyrelics.") + id;
    }

    private void item(GuiGraphics graphics, ItemStack stack, int x, int y, int mx, int my) {
        graphics.renderItem(stack, x, y);
        if (over(mx, my, x, y, 16, 16)) {
            this.hovered = stack;
        }
    }

    // ---- Turning pages.

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) (mouseX / this.scale);
        int my = (int) (mouseY / this.scale);
        if (this.spread > 0 && over(mx, my, this.left + 40, this.top + 196, 20, 16)) {
            turn(-2);
            return true;
        }
        if (this.spread + 2 < this.pages.size() && over(mx, my, this.left + W - 78, this.top + 196, 20, 16)) {
            turn(2);
            return true;
        }
        int open = openChapter();
        for (int i = 0; i < this.chapters.size(); i++) {
            int x = this.left + TAB_X + (i == open ? 5 : 0);
            if (over(mx, my, x, this.top + TAB_Y + i * TAB_STEP, TAB_W, TAB_H)) {
                this.spread = this.chapterStart.get(i);
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void turn(int by) {
        this.spread = Math.max(0, Math.min(this.pages.size() - 2, this.spread + by));
        click();
    }

    private static void click() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP) {
            if (this.spread > 0) {
                turn(-2);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (this.spread + 2 < this.pages.size()) {
                turn(2);
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
