package dev.tinyrelics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tinyrelics.TinyRelics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static dev.tinyrelics.client.RoadmapArt.*;

/**
 * The roadmap of coming updates, drawn over a whole spread of the guide book: the map, and on it the three
 * heroes to come, each in his own land and each doing something, the ship rocking, clouds, gulls, waves,
 * petals, a storm flashing, and a marker walking the golden path from the sky to the castle.
 * Everything here runs on the clock; the sprites and their points come from tools/roadmap.py (RoadmapArt).
 */
final class RoadmapMap {
    private static final ResourceLocation MAP = rl("textures/gui/roadmap_map.png");
    private static final ResourceLocation SPRITES = rl("textures/gui/roadmap_sprites.png");
    /** Where things stand on the map, in pixels of the map picture. */
    private static final float[] ARCHER_AT = {288, 572};
    private static final float[] SHIP_AT = {1045, 562};
    /** Where the ship's rail is, as a fraction of the ship's height. */
    private static final float HULL_FROM = 0.69F;
    private static final float[] SHOGUN_AT = {1495, 650};
    private static final float[] PLAQUE_X = {300, 885, 1485};
    private static final int[][] PLAQUES = {PLAQUE0, PLAQUE1, PLAQUE2};
    private static final float[][] PLAQUE_TOPS = {PLAQUE0_TOP, PLAQUE1_TOP, PLAQUE2_TOP};
    private static final String[] HEROES = {"archer", "pirate", "shogun"};
    private static final int[][] PETALS = {PETAL0, PETAL1, PETAL2, PETAL3};
    private static final int[][] GULLS = {GULL0, GULL1, GULL2, GULL1};
    private static final int[][] WAVES = {WAVE0, WAVE1, WAVE2};
    private static final int[][] BANNERS = {BANNER0, BANNER1, BANNER2};
    private static final float[][] POLES = {BANNER0_POLE, BANNER1_POLE, BANNER2_POLE};
    private static final int[][] SLASHES = {SLASH0, SLASH1, SLASH2};
    private static final int[][] FLASHES = {FLASH0, FLASH1, FLASH2};
    private static final int[][] SMOKES = {SMOKE0, SMOKE1, SMOKE2};

    private static GuiGraphics g;
    private static float ox;
    private static float oy;

    private RoadmapMap() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, path);
    }

    private static float px(float x) {
        return ox + x * MAP_SCALE;
    }

    private static float py(float y) {
        return oy + y * MAP_SCALE;
    }

    private static float wave(double t, double period) {
        return (float) Math.sin(t * Math.PI * 2.0D / period);
    }

    private static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static float ease(float v) {
        v = clamp(v);
        return v * v * (3.0F - 2.0F * v);
    }

    /** A sprite with its top-left corner at (x, y), in GUI pixels. */
    private static void sprite(int[] s, float x, float y) {
        part(s, x, y, 0.0F, 1.0F);
    }

    /** Only the part of a sprite between two fractions of its width (a petal cut in half). */
    private static void part(int[] s, float x, float y, float from, float to) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(1.0F / K, 1.0F / K, 1.0F);
        int u0 = Math.round(s[2] * from);
        int u1 = Math.round(s[2] * to);
        g.blit(SPRITES, u0, 0, u1 - u0, s[3], (float) (s[0] + u0), (float) s[1], u1 - u0, s[3], ATLAS, ATLAS);
        pose.popPose();
    }

    private static float w(int[] s) {
        return s[2] / (float) K;
    }

    private static float h(int[] s) {
        return s[3] / (float) K;
    }

    /** Only the rows of a sprite from a fraction of its height down. */
    private static void rows(int[] s, float x, float y, float from) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(1.0F / K, 1.0F / K, 1.0F);
        int v0 = Math.round(s[3] * from);
        g.blit(SPRITES, 0, v0, s[2], s[3] - v0, (float) s[0], (float) (s[1] + v0), s[2], s[3] - v0, ATLAS, ATLAS);
        pose.popPose();
    }

    /** A sprite turned about one of its points, that point put at (x, y). */
    private static void turned(int[] s, float[] point, float x, float y, float degrees) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        sprite(s, -point[0], -point[1]);
        pose.popPose();
    }

    private static void alpha(float a) {
        g.setColor(1.0F, 1.0F, 1.0F, a);
    }

    /**
     * Draws the map with its top-left corner at (x, y), in the book's coordinates; the book is drawn at
     * {@code scale}. Returns the tooltip of the hero under the mouse, or null.
     */
    static List<Component> draw(GuiGraphics graphics, Font font, int x, int y, int mx, int my, float scale) {
        g = graphics;
        ox = x;
        oy = y;
        double t = (System.currentTimeMillis() % 3_600_000L) / 1000.0D;
        RenderSystem.enableBlend();
        g.enableScissor((int) (x * scale), (int) (y * scale), (int) Math.ceil((x + MAP_W) * scale), (int) Math.ceil((y + MAP_H) * scale));
        g.blit(MAP, x, y, MAP_W, MAP_H, 0.0F, 0.0F, MAP_W * K, MAP_H * K, MAP_W * K, MAP_H * K);
        storm(t);
        clouds(t);
        waves(t);
        gulls(t);
        archer(t);
        ship(t);
        shogun(t);
        plaques(font);
        marker(t);
        alpha(1.0F);
        g.disableScissor();
        // a picture glued over the fold: a thin frame and a soft shadow down the middle
        int cx = x + MAP_W / 2;
        g.fill(cx - 3, y, cx - 1, y + MAP_H, 0x14000000);
        g.fill(cx - 1, y, cx + 1, y + MAP_H, 0x22000000);
        g.fill(cx + 1, y, cx + 3, y + MAP_H, 0x14000000);
        g.fill(x - 1, y - 1, x + MAP_W + 1, y, 0xFF3B2A1A);
        g.fill(x - 1, y + MAP_H, x + MAP_W + 1, y + MAP_H + 1, 0xFF3B2A1A);
        g.fill(x - 1, y, x, y + MAP_H, 0xFF3B2A1A);
        g.fill(x + MAP_W, y, x + MAP_W + 1, y + MAP_H, 0xFF3B2A1A);
        return tooltip(mx, my);
    }

    // ---- The world round the heroes.

    private static void storm(double t) {
        double c = t % 8.5D;
        if (c < 0.12D || (c > 0.24D && c < 0.32D)) {
            g.fill((int) px(560), (int) py(110), (int) px(1250), (int) py(360), 0x30FFFFFF);
        }
    }

    private static void clouds(double t) {
        int[][] clouds = {CLOUD0, CLOUD1};
        float[] heights = {26.0F, 48.0F};
        for (int i = 0; i < 2; i++) {
            float span = MAP_W + w(clouds[i]) + 20.0F;
            float along = (float) ((t * (3.0D + i) + i * 150.0D) % span);
            alpha(0.88F);
            sprite(clouds[i], ox - w(clouds[i]) - 10.0F + along, oy + heights[i] + wave(t + i, 7.0D));
        }
        alpha(1.0F);
    }

    private static void waves(double t) {
        float[][] spots = {{720, 700}, {980, 770}, {1150, 470}, {830, 600}, {640, 820}};
        for (int i = 0; i < spots.length; i++) {
            double c = (t + i * 0.73D) % 3.0D;
            if (c < 1.2D) {
                int[] s = WAVES[Math.min(2, (int) (c / 0.4D))];
                alpha(1.0F - (float) Math.max(0.0D, c - 0.8D) / 0.4F);
                sprite(s, px(spots[i][0]) - w(s) / 2, py(spots[i][1]) - h(s));
            }
        }
        alpha(1.0F);
    }

    private static void gulls(double t) {
        for (int i = 0; i < 2; i++) {
            double period = 13.0D + i * 4.0D;
            float along = (float) (((t + i * 5.0D) % period) / period);
            float x = px(520) + along * (1380 - 520) * MAP_SCALE;
            float y = py(280 + i * 70) + wave(t + i, 3.0D) * 3.0F;
            int[] s = GULLS[(int) ((t * 7.0D + i) % 4.0D)];
            sprite(s, x, y);
        }
    }

    // ---- The archer: hovering over his island, wings beating slowly.

    private static float[] archerBox = new float[4];

    private static void archer(double t) {
        float fx = px(ARCHER_AT[0]);
        float fy = py(ARCHER_AT[1]) - 3.0F + wave(t, 3.0D) * 1.3F;
        float bx = fx - ARCHER_FEET[0];
        float by = fy - ARCHER_FEET[1];
        float flap = 9.0F * wave(t, 1.6D);
        turned(WING_L, WING_L_ROOT, bx + ARCHER_WING_L[0], by + ARCHER_WING_L[1], flap);
        turned(WING_R, WING_R_ROOT, bx + ARCHER_WING_R[0], by + ARCHER_WING_R[1], -flap);
        sprite(ARCHER, bx, by);
        archerBox = new float[] {bx, by, w(ARCHER), h(ARCHER)};
    }

    // ---- The captain on his rocking ship: now and then he aims his musket and fires, and the parrot takes off.

    private static float[] pirateBox = new float[4];

    private static void ship(double t) {
        PoseStack pose = g.pose();
        float rock = 2.5F * wave(t, 4.6D);
        float heave = 0.8F * wave(t, 3.1D);
        float sx = px(SHIP_AT[0]);
        float sy = py(SHIP_AT[1]) + heave;
        pose.pushPose();
        pose.translate(sx, sy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(rock));
        sprite(SHIP, -SHIP_WATER[0], -SHIP_WATER[1]);
        pirate(t, SHIP_DECK[0] - SHIP_WATER[0], SHIP_DECK[1] - SHIP_WATER[1]);
        // the hull below the rail once more, over his boots: he stands on the deck, behind the rail
        rows(SHIP, -SHIP_WATER[0], -SHIP_WATER[1], HULL_FROM);
        pose.popPose();
        pirateBox = new float[] {sx + SHIP_DECK[0] - SHIP_WATER[0] - PIRATE_FEET[0], sy + SHIP_DECK[1] - SHIP_WATER[1] - PIRATE_FEET[1],
                w(PIRATE), h(PIRATE)};
    }

    private static void pirate(double t, float fx, float fy) {
        double c = t % 9.0D;
        float bx = fx - PIRATE_FEET[0];
        float by = fy - PIRATE_FEET[1] + 0.4F * wave(t, 2.4D);
        float rest = -70.0F;
        float angle;
        if (c < 5.0D) {
            angle = rest;
        } else if (c < 5.4D) {
            angle = rest * (1.0F - ease((float) (c - 5.0D) / 0.4F));
        } else if (c < 5.8D) {
            angle = 0.0F;
        } else if (c < 6.2D) {
            angle = -10.0F * (1.0F - ease((float) (c - 5.8D) / 0.4F));           // the kick of the shot
        } else if (c < 6.6D) {
            angle = 0.0F;
        } else if (c < 7.2D) {
            angle = rest * ease((float) (c - 6.6D) / 0.6F);
        } else {
            angle = rest;
        }
        sprite(PIRATE, bx, by);
        float shx = bx + PIRATE_SHOULDER[0];
        float shy = by + PIRATE_SHOULDER[1];
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(shx, shy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        sprite(ARM, -ARM_PIVOT[0], -ARM_PIVOT[1]);
        float mx = ARM_MUZZLE[0] - ARM_PIVOT[0];
        float my = ARM_MUZZLE[1] - ARM_PIVOT[1];
        if (c >= 5.8D && c < 6.05D) {
            int[] s = FLASHES[Math.min(2, (int) ((c - 5.8D) / 0.084D))];
            sprite(s, mx - 0.5F, my - h(s) / 2);
        }
        pose.popPose();
        if (c >= 5.85D && c < 7.3D) {
            float p = (float) ((c - 5.85D) / 1.45D);
            int[] s = SMOKES[Math.min(2, (int) (p * 3.0F))];
            alpha(1.0F - p * 0.9F);
            sprite(s, shx + mx + 1.0F + p * 5.0F - w(s) / 2, shy + my - p * 6.0F - h(s) / 2);
            alpha(1.0F);
        }
        // the parrot: sits on the sabre arm, ruffles now and then, flies up at the shot and comes back
        float perchX = bx + PIRATE_PERCH[0];
        float perchY = by + PIRATE_PERCH[1];
        if (c >= 5.8D && c < 7.8D) {
            float u = (float) ((c - 5.8D) / 2.0D);
            float dx = (float) Math.sin(u * Math.PI * 2.0D) * 6.0F;
            float dy = -(float) Math.sin(u * Math.PI) * 12.0F;
            int[] s = ((int) (t * 10.0D)) % 2 == 0 ? PARROT1 : PARROT2;
            sprite(s, perchX + dx - w(s) / 2, perchY + dy - h(s));
        } else {
            boolean ruffle = (c > 2.4D && c < 2.6D) || (c > 2.75D && c < 2.95D);
            int[] s = ruffle ? PARROT1 : PARROT0;
            sprite(s, perchX - (ruffle ? w(s) / 2 : PARROT0_FEET[0]), perchY - (ruffle ? h(s) : PARROT0_FEET[1]));
        }
    }

    // ---- The shogun: still as stone, then a strike faster than the eye; petals that drift into it are cut in two.

    private static float[] shogunBox = new float[4];
    private static final double STRIKE_AT = 5.6D;

    private static void shogun(double t) {
        double c = t % 7.0D;
        float fx = px(SHOGUN_AT[0]);
        float fy = py(SHOGUN_AT[1]);
        float bx = fx - SHOGUN_FEET[0];
        float by = fy - SHOGUN_FEET[1];
        int frame = (int) (t * 6.0D) % 3;
        sprite(BANNERS[frame], bx + SHOGUN_BACK[0] - POLES[frame][0], by + SHOGUN_BACK[1] - POLES[frame][1]);
        boolean striking = c >= STRIKE_AT && c < 6.4D;
        if (striking) {
            sprite(STRIKE, fx - STRIKE_FEET[0], fy - STRIKE_FEET[1]);
        } else {
            sprite(SHOGUN, bx, by);
        }
        float glow = c < 5.0D ? 0.0F : c < STRIKE_AT ? (float) (c - 5.0D) / 0.6F : c < 6.4D ? 1.0F : clamp(1.0F - (float) (c - 6.4D) / 0.4F);
        if (glow > 0.0F && !striking) {
            int red = ((int) (glow * 255.0F) << 24) | 0xFF2A1A;
            for (float[] eye : new float[][] {SHOGUN_EYE_L, SHOGUN_EYE_R}) {
                PoseStack pose = g.pose();
                pose.pushPose();
                pose.translate(bx + eye[0] - 0.6F, by + eye[1] - 0.35F, 0.0F);
                pose.scale(1.2F, 0.7F, 1.0F);
                g.fill(0, 0, 1, 1, red);
                pose.popPose();
            }
        }
        float cutX = fx - 15.0F;
        float cutY = fy - 17.0F;
        if (c >= STRIKE_AT && c < STRIKE_AT + 0.26D) {
            int[] s = SLASHES[Math.min(2, (int) ((c - STRIKE_AT) / 0.087D))];
            sprite(s, cutX - w(s) / 2, cutY - h(s) / 2);
        }
        petals(t, c, cutX, cutY);
        shogunBox = new float[] {bx, by, w(SHOGUN), h(SHOGUN)};
    }

    private static void petals(double t, double c, float cutX, float cutY) {
        // petals blowing off the cherry trees, across the shogun's land
        for (int i = 0; i < 7; i++) {
            double period = 6.0D + i * 0.9D;
            float u = (float) (((t + i * 1.7D) % period) / period);
            float x = px(1760 - i * 25) - u * 120.0F;
            float y = py(300 + i * 70) + u * 40.0F + wave(t + i, 2.2D) * 3.0F;
            alpha(Math.min(1.0F, Math.min(u * 6.0F, (1.0F - u) * 6.0F)));
            spin(PETALS[i % 4], x, y, (float) (t * 90.0D + i * 50.0D), 0.0F, 1.0F);
        }
        // and two that drift right into his strike
        for (int i = 0; i < 2; i++) {
            float tx = cutX + (i == 0 ? -2.0F : 3.0F);
            float ty = cutY + (i == 0 ? -4.0F : 4.0F);
            if (c > STRIKE_AT - 2.5D && c < STRIKE_AT) {
                float u = (float) ((c - (STRIKE_AT - 2.5D)) / 2.5D);
                alpha(Math.min(1.0F, u * 5.0F));
                spin(PETALS[i], tx + (1.0F - u) * 30.0F, ty - (1.0F - u) * 12.0F + wave(t + i, 1.3D) * (1.0F - u) * 2.0F,
                        (float) (t * 120.0D), 0.0F, 1.0F);
            } else if (c >= STRIKE_AT && c < STRIKE_AT + 1.0D) {
                float u = (float) (c - STRIKE_AT);
                alpha(1.0F - u);
                spin(PETALS[i], tx - u * 4.0F, ty + u * 7.0F, -u * 160.0F, 0.0F, 0.5F);
                spin(PETALS[i], tx + u * 4.0F, ty + u * 9.0F, u * 200.0F, 0.5F, 1.0F);
            }
        }
        alpha(1.0F);
    }

    private static void spin(int[] s, float x, float y, float degrees, float from, float to) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        part(s, -w(s) / 2, -h(s) / 2, from, to);
        pose.popPose();
    }

    // ---- The plaques with the heroes' names, and the marker on the path.

    private static void plaques(Font font) {
        for (int i = 0; i < 3; i++) {
            int[] s = PLAQUES[i];
            float left = px(PLAQUE_X[i]) - PLAQUE_TOPS[i][0];
            sprite(s, left, oy);
            String[] lines = Component.translatable("guide.tinyrelics.roadmap." + HEROES[i]).getString().split("\n");
            float scale = 0.8F;
            float centre = oy + h(s) * 0.64F;
            float top = centre - lines.length * 9.0F * scale / 2.0F + 0.5F;
            for (int k = 0; k < lines.length; k++) {
                PoseStack pose = g.pose();
                pose.pushPose();
                pose.translate(left + w(s) / 2 - font.width(lines[k]) * scale / 2, top + k * 9.0F * scale, 0.0F);
                pose.scale(scale, scale, 1.0F);
                g.drawString(font, lines[k], 0, 0, 0xFFF6E3B4, true);
                pose.popPose();
            }
        }
    }

    private static float[] lengths;

    private static void marker(double t) {
        if (lengths == null) {
            lengths = new float[PATH.length];
            for (int i = 1; i < PATH.length; i++) {
                float dx = PATH[i][0] - PATH[i - 1][0];
                float dy = PATH[i][1] - PATH[i - 1][1];
                lengths[i] = lengths[i - 1] + (float) Math.sqrt(dx * dx + dy * dy);
            }
        }
        double c = t % 16.0D;
        float a = (float) Math.min(1.0D, Math.min(c / 0.8D, (16.0D - c) / 0.8D));
        float along = ease((float) ((c - 1.0D) / 12.0D)) * lengths[lengths.length - 1];
        int i = 1;
        while (i < PATH.length - 1 && lengths[i] < along) {
            i++;
        }
        float f = (along - lengths[i - 1]) / Math.max(1.0F, lengths[i] - lengths[i - 1]);
        float x = PATH[i - 1][0] + (PATH[i][0] - PATH[i - 1][0]) * clamp(f);
        float y = PATH[i - 1][1] + (PATH[i][1] - PATH[i - 1][1]) * clamp(f);
        alpha(a);
        sprite(MARKER, px(x) - MARKER_TIP[0], py(y) - MARKER_TIP[1] - 2.0F - Math.abs(wave(t, 0.9D)) * 2.0F);
        alpha(1.0F);
    }

    // ---- Who is under the mouse.

    private static List<Component> tooltip(int mx, int my) {
        float[][] boxes = {archerBox, pirateBox, shogunBox};
        int[] colours = {0xFFE9C46A, 0xFF7FB3D5, 0xFFE07A5F};
        for (int i = 0; i < 3; i++) {
            float[] b = boxes[i];
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                List<Component> lines = new ArrayList<>();
                int colour = colours[i] & 0xFFFFFF;
                String name = Component.translatable("guide.tinyrelics.roadmap." + HEROES[i]).getString().replace('\n', ' ');
                lines.add(Component.literal(name).withStyle(style -> style.withColor(colour).withBold(true)));
                for (String line : Component.translatable("guide.tinyrelics.roadmap." + HEROES[i] + ".about").getString().split("\n")) {
                    lines.add(Component.literal(line));
                }
                lines.add(Component.translatable("guide.tinyrelics.roadmap.soon").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return lines;
            }
        }
        return null;
    }
}
