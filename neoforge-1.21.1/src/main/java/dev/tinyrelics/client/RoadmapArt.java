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
    static final int[] ARCHER = {319, 197, 69, 116};
    static final float[] ARCHER_FEET = {8.62F, 28.93F};
    static final float[] ARCHER_WING_L = {7.84F, 12.04F};
    static final float[] ARCHER_WING_R = {13.49F, 12.04F};
    static final int[] ARM = {699, 357, 65, 16};
    static final float[] ARM_PIVOT = {0.63F, 2.18F};
    static final float[] ARM_MUZZLE = {16.21F, 1.42F};
    static final int[] BANNER0 = {65, 357, 16, 45};
    static final float[] BANNER0_POLE = {0.41F, 11.19F};
    static final int[] BANNER1 = {83, 357, 18, 45};
    static final float[] BANNER1_POLE = {0.43F, 11.19F};
    static final int[] BANNER2 = {103, 357, 17, 45};
    static final float[] BANNER2_POLE = {0.42F, 11.19F};
    static final int[] CLOUD0 = {601, 197, 139, 74};
    static final int[] CLOUD1 = {854, 197, 154, 65};
    static final int[] FLASH0 = {813, 357, 15, 12};
    static final int[] FLASH1 = {596, 357, 25, 20};
    static final int[] FLASH2 = {565, 357, 29, 21};
    static final int[] GULL0 = {193, 357, 42, 38};
    static final int[] GULL1 = {362, 357, 45, 27};
    static final int[] GULL2 = {485, 357, 41, 26};
    static final int[] MARKER = {122, 357, 41, 42};
    static final float[] MARKER_TIP = {5.10F, 10.50F};
    static final int[] PARROT0 = {680, 357, 17, 18};
    static final float[] PARROT0_FEET = {2.12F, 4.43F};
    static final int[] PARROT1 = {623, 357, 28, 20};
    static final int[] PARROT2 = {528, 357, 35, 22};
    static final int[] PETAL0 = {766, 357, 14, 16};
    static final int[] PETAL1 = {782, 357, 12, 13};
    static final int[] PETAL2 = {796, 357, 15, 13};
    static final int[] PETAL3 = {830, 357, 16, 11};
    static final int[] PIRATE = {560, 197, 39, 80};
    static final float[] PIRATE_FEET = {4.94F, 19.96F};
    static final float[] PIRATE_SHOULDER = {8.19F, 7.30F};
    static final float[] PIRATE_PERCH = {1.99F, 11.82F};
    static final int[] PLAQUE0 = {194, 0, 327, 158};
    static final float[] PLAQUE0_TOP = {40.88F, 0.00F};
    static final int[] PLAQUE1 = {523, 0, 309, 158};
    static final float[] PLAQUE1_TOP = {38.62F, 0.00F};
    static final int[] PLAQUE2 = {0, 197, 317, 158};
    static final float[] PLAQUE2_TOP = {39.62F, 0.00F};
    static final int[] SHIP = {0, 0, 192, 195};
    static final float[] SHIP_WATER = {23.89F, 46.36F};
    static final float[] SHIP_DECK = {26.29F, 39.39F};
    static final int[] SHOGUN = {508, 197, 50, 103};
    static final float[] SHOGUN_FEET = {6.23F, 25.66F};
    static final float[] SHOGUN_EYE_L = {6.99F, 3.33F};
    static final float[] SHOGUN_EYE_R = {5.21F, 3.33F};
    static final float[] SHOGUN_BACK = {10.24F, 9.92F};
    static final int[] SLASH0 = {0, 357, 31, 50};
    static final int[] SLASH1 = {33, 357, 30, 46};
    static final int[] SLASH2 = {165, 357, 26, 41};
    static final int[] SMOKE0 = {653, 357, 25, 18};
    static final int[] SMOKE1 = {409, 357, 32, 26};
    static final int[] SMOKE2 = {443, 357, 40, 26};
    static final int[] STRIKE = {390, 197, 116, 104};
    static final float[] STRIKE_FEET = {23.25F, 25.91F};
    static final int[] WAVE0 = {325, 357, 35, 29};
    static final int[] WAVE1 = {282, 357, 41, 34};
    static final int[] WAVE2 = {237, 357, 43, 36};
    static final int[] WING_L = {742, 197, 54, 70};
    static final float[] WING_L_ROOT = {12.87F, 7.10F};
    static final int[] WING_R = {798, 197, 54, 70};
    static final float[] WING_R_ROOT = {0.63F, 7.10F};

    /** The marker's path, in picture pixels of the map. */
    static final float[][] PATH = {{492F, 578F}, {538F, 577F}, {583F, 580F}, {628F, 587F}, {673F, 598F}, {718F, 612F}, {764F, 626F}, {811F, 637F}, {859F, 645F}, {908F, 646F}, {954F, 639F}, {996F, 629F}, {1037F, 617F}, {1076F, 605F}, {1114F, 595F}, {1154F, 588F}, {1195F, 587F}, {1238F, 593F}, {1279F, 600F}, {1317F, 603F}};

    private RoadmapArt() {
    }
}
