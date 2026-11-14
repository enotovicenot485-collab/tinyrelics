package dev.tinyrelics;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

/**
 * What the mod's own creative tab shows, row by row: a banner picture, then the
 * equipment that belongs under it.
 *
 * A tab can only hold items, so every cell that is not real equipment holds an
 * invisible filler item carrying a number. Cells under a banner carry
 * banner * 100 + cell (cell 0 is the banner's top-left corner); the client draws
 * the picture over them and blocks every click. Plain gaps that pad a row out to
 * nine cells count up from {@link #FIRST_GAP}.
 */
public final class RelicTab {
    public static final int COLUMNS = 9;
    /** A banner is two rows of slots tall. */
    public static final int BANNER_ROWS = 2;
    private static final int FIRST_GAP = 10000;

    /** Banner numbers; the client has one picture for each. */
    public static final int NECROMANCER = 1;
    public static final int THOR = 2;
    public static final int CHAOS = 3;
    public static final int BLOOD = 4;

    private RelicTab() {
    }

    /** The banner this filler belongs to, or 0 if it is a plain gap or no filler at all. */
    public static int bannerOf(int code) {
        return code >= 100 && code < FIRST_GAP ? code / 100 : 0;
    }

    /** Which cell of its banner this filler is: 0 to 17, left to right, top row first. */
    public static int cellOf(int code) {
        return code % 100;
    }

    public static void fill(CreativeModeTab.Output output) {
        int[] nextGap = {FIRST_GAP};

        banner(output, NECROMANCER);
        row(output, nextGap,
                TinyRelics.NECRO_HELMET.get(), TinyRelics.NECRO_CHESTPLATE.get(),
                TinyRelics.NECRO_LEGGINGS.get(), TinyRelics.NECRO_BOOTS.get(),
                TinyRelics.UNDEAD_STAFF.get());

        banner(output, THOR);
        row(output, nextGap, TinyRelics.THOR_HAMMER.get(), TinyRelics.THOR_CHESTPLATE.get(),
                TinyRelics.THOR_LEGGINGS.get(), TinyRelics.THOR_BOOTS.get());

        banner(output, CHAOS);
        row(output, nextGap,
                TinyRelics.CHAOS_HELMET.get(), TinyRelics.CHAOS_CHESTPLATE.get(),
                TinyRelics.CHAOS_LEGGINGS.get(), TinyRelics.CHAOS_BOOTS.get(),
                TinyRelics.CHAOS_BOOMERANG.get());
        banner(output, BLOOD);
        row(output, nextGap,
                TinyRelics.BLOOD_HELMET.get(), TinyRelics.BLOOD_CHESTPLATE.get(),
                TinyRelics.BLOOD_LEGGINGS.get(), TinyRelics.BLOOD_BOOTS.get(),
                TinyRelics.BLOOD_BLADE.get(), TinyRelics.BLOOD_INGOT.get(),
                TinyRelics.BLOOD_DROP_ITEM.get(), TinyRelics.DIRTY_BLOOD_ITEM.get(),
                TinyRelics.BLOOD_ALTAR_ITEM.get());
        row(output, nextGap, TinyRelics.KEEPING_CHARM.get());
    }

    private static void banner(CreativeModeTab.Output output, int banner) {
        for (int cell = 0; cell < COLUMNS * BANNER_ROWS; cell++) {
            output.accept(TinyRelics.filler(banner * 100 + cell), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }

    /** The items, then invisible gaps up to the end of the row, so the next banner starts on a new line. */
    private static void row(CreativeModeTab.Output output, int[] nextGap, ItemLike... items) {
        for (ItemLike item : items) {
            output.accept(item);
        }
        for (int i = items.length; i % COLUMNS != 0; i++) {
            output.accept(TinyRelics.filler(nextGap[0]++), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }
}
