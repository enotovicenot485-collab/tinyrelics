package dev.tinyrelics.client;

/** Made by tools/roadmap.py: where each sprite of the roadmap is in its atlas, and its points. Do not edit by hand. */
final class RoadmapArt {
    static final int K = 4;
    static final int ATLAS = 1024;
    static final int MAP_W = 332;
    static final int MAP_H = 166;
    /** GUI pixels of the map per pixel of the picture it was drawn from: places on the map are given in picture pixels. */
    static final float MAP_SCALE = 0.187148F;

    /** u, v, width, height in texels of the atlas. */
    static final int[] ARCHER = {319, 197, 85, 144};
    static final float[] ARCHER_FEET = {10.62F, 35.92F};
    static final float[] ARCHER_WING_L = {9.66F, 14.95F};
    static final float[] ARCHER_WING_R = {16.61F, 14.95F};
    static final int[] ARM = {0, 424, 84, 20};
    static final float[] ARM_PIVOT = {0.82F, 2.72F};
    static final float[] ARM_MUZZLE = {20.95F, 1.78F};
    static final int[] BANNER0 = {235, 357, 20, 55};
    static final float[] BANNER0_POLE = {0.52F, 13.68F};
    static final int[] BANNER1 = {257, 357, 22, 55};
    static final float[] BANNER1_POLE = {0.52F, 13.68F};
    static final int[] BANNER2 = {281, 357, 21, 55};
    static final float[] BANNER2_POLE = {0.52F, 13.68F};
    static final int[] CLOUD0 = {805, 197, 139, 74};
    static final int[] CLOUD1 = {0, 357, 154, 65};
    static final int[] FLASH0 = {86, 424, 20, 16};
    static final int[] FLASH1 = {780, 357, 33, 26};
    static final int[] FLASH2 = {694, 357, 37, 27};
    static final int[] GULL0 = {380, 357, 42, 38};
    static final int[] GULL1 = {733, 357, 45, 27};
    static final int[] GULL2 = {854, 357, 41, 26};
    static final int[] MARKER = {337, 357, 41, 42};
    static final float[] MARKER_TIP = {5.10F, 10.50F};
    static final int[] PARROT0 = {931, 357, 22, 24};
    static final float[] PARROT0_FEET = {2.75F, 5.90F};
    static final int[] PARROT1 = {815, 357, 37, 26};
    static final int[] PARROT2 = {647, 357, 45, 28};
    static final int[] PETAL0 = {108, 424, 14, 16};
    static final int[] PETAL1 = {124, 424, 12, 13};
    static final int[] PETAL2 = {138, 424, 15, 13};
    static final int[] PETAL3 = {155, 424, 16, 11};
    static final int[] PIRATE = {614, 197, 51, 104};
    static final float[] PIRATE_FEET = {6.45F, 25.95F};
    static final float[] PIRATE_SHOULDER = {10.70F, 9.50F};
    static final float[] PIRATE_PERCH = {2.60F, 15.37F};
    static final int[] PLAQUE0 = {194, 0, 327, 158};
    static final float[] PLAQUE0_TOP = {40.88F, 0.00F};
    static final int[] PLAQUE1 = {523, 0, 309, 158};
    static final float[] PLAQUE1_TOP = {38.62F, 0.00F};
    static final int[] PLAQUE2 = {0, 197, 317, 158};
    static final float[] PLAQUE2_TOP = {39.62F, 0.00F};
    static final int[] SHIP = {0, 0, 192, 195};
    static final float[] SHIP_WATER = {23.89F, 46.36F};
    static final float[] SHIP_DECK = {27.38F, 34.71F};
    static final int[] SHOGUN = {551, 197, 61, 127};
    static final float[] SHOGUN_FEET = {7.61F, 31.64F};
    static final float[] SHOGUN_EYE_L = {8.53F, 4.10F};
    static final float[] SHOGUN_EYE_R = {6.36F, 4.10F};
    static final float[] SHOGUN_BACK = {12.49F, 12.23F};
    static final int[] SLASH0 = {156, 357, 38, 61};
    static final int[] SLASH1 = {196, 357, 37, 56};
    static final int[] SLASH2 = {304, 357, 31, 50};
    static final int[] SMOKE0 = {897, 357, 32, 24};
    static final int[] SMOKE1 = {469, 357, 42, 34};
    static final int[] SMOKE2 = {513, 357, 52, 34};
    static final int[] STRIKE = {406, 197, 143, 128};
    static final float[] STRIKE_FEET = {28.67F, 31.89F};
    static final int[] WAVE0 = {610, 357, 35, 29};
    static final int[] WAVE1 = {567, 357, 41, 34};
    static final int[] WAVE2 = {424, 357, 43, 36};
    static final int[] WING_L = {667, 197, 67, 87};
    static final float[] WING_L_ROOT = {15.97F, 8.83F};
    static final int[] WING_R = {736, 197, 67, 87};
    static final float[] WING_R_ROOT = {0.78F, 8.83F};

    /** The marker's path, in picture pixels of the map. */
    static final float[][] PATH = {{290F, 552F}, {492F, 578F}, {538F, 577F}, {583F, 580F}, {628F, 587F}, {673F, 598F}, {718F, 612F}, {764F, 626F}, {811F, 637F}, {859F, 645F}, {908F, 646F}, {954F, 639F}, {996F, 629F}, {1037F, 617F}, {1076F, 605F}, {1114F, 595F}, {1154F, 588F}, {1195F, 587F}, {1238F, 593F}, {1279F, 600F}, {1317F, 603F}, {1490F, 640F}};

    private RoadmapArt() {
    }
}
