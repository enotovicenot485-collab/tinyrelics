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
    static final int[] ARCHER = {630, 238, 69, 116};
    static final float[] ARCHER_FEET = {8.62F, 28.93F};
    static final float[] ARCHER_WING_L = {7.84F, 12.04F};
    static final float[] ARCHER_WING_R = {13.49F, 12.04F};
    static final int[] ARM = {120, 511, 78, 19};
    static final float[] ARM_PIVOT = {0.76F, 2.59F};
    static final float[] ARM_MUZZLE = {19.45F, 1.69F};
    static final int[] BANNER0 = {795, 398, 16, 45};
    static final float[] BANNER0_POLE = {0.41F, 11.19F};
    static final int[] BANNER1 = {813, 398, 18, 45};
    static final float[] BANNER1_POLE = {0.43F, 11.19F};
    static final int[] BANNER2 = {833, 398, 17, 45};
    static final float[] BANNER2_POLE = {0.42F, 11.19F};
    static final int[] BURST0 = {901, 474, 26, 23};
    static final int[] BURST1 = {361, 474, 44, 31};
    static final int[] BURST2 = {105, 474, 53, 33};
    static final int[] BURST3 = {839, 474, 33, 24};
    static final int[] CLOUD0 = {0, 398, 139, 74};
    static final int[] CLOUD1 = {253, 398, 154, 65};
    static final int[] COON_CLOUD = {462, 398, 69, 52};
    static final int[] COON_DAZED = {409, 398, 51, 58};
    static final int[] COON_DIVE = {695, 398, 66, 47};
    static final int[] COON_FALL = {566, 398, 61, 50};
    static final int[] COON_LAND = {852, 398, 63, 45};
    static final int[] COON_RUN0 = {305, 474, 54, 32};
    static final int[] COON_RUN1 = {407, 474, 55, 31};
    static final int[] COON_RUN2 = {464, 474, 50, 31};
    static final int[] COON_RUN3 = {160, 474, 52, 33};
    static final int[] COON_SLIP = {629, 398, 64, 48};
    static final int[] COON_UP = {0, 474, 60, 35};
    static final int[] DUST0 = {87, 511, 31, 20};
    static final int[] DUST1 = {967, 474, 33, 23};
    static final int[] DUST2 = {55, 511, 30, 22};
    static final int[] FEATHER0 = {500, 511, 13, 11};
    static final int[] FEATHER1 = {288, 511, 8, 15};
    static final int[] FEATHER2 = {529, 511, 13, 9};
    static final int[] FEATHER3 = {515, 511, 12, 11};
    static final int[] FEATHER4 = {392, 511, 13, 12};
    static final int[] FLASH0 = {298, 511, 19, 14};
    static final int[] FLASH1 = {771, 474, 30, 24};
    static final int[] FLASH2 = {735, 474, 34, 25};
    static final int[] GLIDE = {425, 511, 73, 11};
    static final int[] GULL0 = {653, 474, 36, 27};
    static final int[] GULL1 = {200, 511, 36, 19};
    static final int[] GULL2 = {319, 511, 40, 14};
    static final int[] GULL3 = {616, 474, 35, 28};
    static final int[] GULL_HIT = {516, 474, 61, 30};
    static final int[] ISLAND = {234, 0, 258, 202};
    static final int[] PARROT0 = {32, 511, 21, 22};
    static final float[] PARROT0_FEET = {2.62F, 5.41F};
    static final int[] PARROT1 = {803, 474, 34, 24};
    static final int[] PARROT2 = {691, 474, 42, 26};
    static final int[] PETAL0 = {272, 511, 14, 16};
    static final int[] PETAL1 = {361, 511, 12, 13};
    static final int[] PETAL2 = {375, 511, 15, 13};
    static final int[] PETAL3 = {407, 511, 16, 11};
    static final int[] PIRATE = {871, 238, 47, 96};
    static final float[] PIRATE_FEET = {5.95F, 23.95F};
    static final float[] PIRATE_SHOULDER = {9.87F, 8.77F};
    static final float[] PIRATE_PERCH = {2.40F, 14.19F};
    static final int[] PLAQUE0 = {494, 0, 327, 158};
    static final float[] PLAQUE0_TOP = {40.88F, 0.00F};
    static final int[] PLAQUE1 = {0, 238, 309, 158};
    static final float[] PLAQUE1_TOP = {38.62F, 0.00F};
    static final int[] PLAQUE2 = {311, 238, 317, 158};
    static final float[] PLAQUE2_TOP = {39.62F, 0.00F};
    static final int[] SHIP = {0, 0, 232, 236};
    static final float[] SHIP_WATER = {28.87F, 56.10F};
    static final float[] SHIP_DECK = {31.77F, 47.67F};
    static final int[] SHOGUN = {819, 238, 50, 103};
    static final float[] SHOGUN_FEET = {6.23F, 25.66F};
    static final float[] SHOGUN_EYE_L = {6.99F, 3.33F};
    static final float[] SHOGUN_EYE_R = {5.21F, 3.33F};
    static final float[] SHOGUN_BACK = {10.24F, 9.92F};
    static final int[] SLASH0 = {533, 398, 31, 50};
    static final int[] SLASH1 = {763, 398, 30, 46};
    static final int[] SLASH2 = {917, 398, 26, 41};
    static final int[] SMOKE0 = {0, 511, 30, 22};
    static final int[] SMOKE1 = {214, 474, 39, 32};
    static final int[] SMOKE2 = {255, 474, 48, 32};
    static final int[] SPLASH0 = {238, 511, 32, 19};
    static final int[] SPLASH1 = {929, 474, 36, 23};
    static final int[] SPLASH2 = {874, 474, 25, 24};
    static final int[] STRIKE = {701, 238, 116, 104};
    static final float[] STRIKE_FEET = {23.25F, 25.91F};
    static final int[] WAVE0 = {579, 474, 35, 29};
    static final int[] WAVE1 = {62, 474, 41, 34};
    static final int[] WAVE2 = {945, 398, 43, 36};
    static final int[] WING_L = {141, 398, 54, 70};
    static final float[] WING_L_ROOT = {12.87F, 7.10F};
    static final int[] WING_R = {197, 398, 54, 70};
    static final float[] WING_R_ROOT = {0.63F, 7.10F};
    /** Where the island cut-out goes, in GUI pixels from the map's corner. */
    static final float[] ISLAND_AT = {110.25F, 54.25F};

    /** The marker's path, in picture pixels of the map. */
    static final float[][] PATH = {{492F, 578F}, {538F, 577F}, {583F, 580F}, {628F, 587F}, {673F, 598F}, {718F, 612F}, {764F, 626F}, {811F, 637F}, {859F, 645F}, {908F, 646F}, {954F, 639F}, {996F, 629F}, {1037F, 617F}, {1076F, 605F}, {1114F, 595F}, {1154F, 588F}, {1195F, 587F}, {1238F, 593F}, {1279F, 600F}, {1317F, 603F}};

    private RoadmapArt() {
    }
}
