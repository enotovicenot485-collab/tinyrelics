package dev.tinyrelics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.RenderGuiEvent;

/**
 * What the player sees when a Charm of Keeping has saved the inventory and broken. The charm appears in the
 * middle of the screen inside a turning circle of runes, cracks spread over it, it bursts into pieces, and
 * golden sparks and little golden things fly out - the kept inventory coming back. Around the player golden
 * sparks rise. Everything is drawn from textures/gui/charm_effect.png (built by tools/charm_effect.py).
 */
public final class CharmEffect {
    private static final ResourceLocation SHEET = new ResourceLocation(TinyRelics.MODID, "textures/gui/charm_effect.png");
    /** The charm's own picture: an animated strip, of which the first frame is used. */
    private static final ResourceLocation CHARM = new ResourceLocation(TinyRelics.MODID, "textures/item/keeping_charm.png");
    private static final int CHARM_STRIP = 576;
    /** The moments of the scene, in ticks from its start. */
    private static final float APPEAR = 10.0F;
    private static final float CRACK = 16.0F;
    private static final float BREAK = 34.0F;
    private static final float END = 74.0F;
    /** The charm is drawn 64 pixels across and breaks into a 4 x 4 grid of pieces. */
    private static final int SIZE = 64;
    private static final int GRID = 4;
    private static final int FLYING = 16;

    /** Wall-clock start of the scene, 0 when nothing plays; and the ticks counted since, for the sounds. */
    private static long startedAt;
    private static int ticks;

    private CharmEffect() {
    }

    public static void start() {
        startedAt = System.currentTimeMillis();
        ticks = 0;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        play(player, SoundEvents.CHEST_OPEN, 0.9F, 1.3F);
        play(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
        // a ring of sparks rises from the ground round the player
        for (int i = 0; i < 28; i++) {
            double angle = i * Math.PI * 2.0D / 28.0D;
            mc.level.addParticle(TinyRelics.CHARM_SPARK.get(), player.getX() + Math.cos(angle) * 1.1D, player.getY() + 0.1D,
                    player.getZ() + Math.sin(angle) * 1.1D, 0.0D, 0.05D + (i % 4) * 0.012D, 0.0D);
        }
    }

    private static void play(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    /** Once a client tick: the sounds and the sparks in the world that belong to moments of the scene. */
    public static void tick() {
        if (startedAt == 0L) {
            return;
        }
        ticks++;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || ticks > END) {
            startedAt = 0L;
            return;
        }
        if (ticks == (int) CRACK) {
            play(player, SoundEvents.AMETHYST_BLOCK_BREAK, 0.8F, 1.4F);
        }
        if (ticks == (int) BREAK) {
            play(player, SoundEvents.GLASS_BREAK, 0.8F, 1.25F);
            play(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 0.7F);
            RandomSource random = player.getRandom();
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.04D + random.nextDouble() * 0.09D;
                mc.level.addParticle(TinyRelics.CHARM_SPARK.get(), player.getX(), player.getY() + 1.0D + random.nextDouble() * 0.6D, player.getZ(),
                        Math.cos(angle) * speed, 0.03D + random.nextDouble() * 0.1D, Math.sin(angle) * speed);
            }
        }
    }

    private static float hash(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0F;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /** Drawn over the finished HUD. */
    public static void onRender(RenderGuiEvent.Post event) {
        if (startedAt == 0L) {
            return;
        }
        float age = (System.currentTimeMillis() - startedAt) / 50.0F;
        if (age > END) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        PoseStack pose = graphics.pose();
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F - 12.0F, 300.0F);

        // the circle of runes: opens, turns, then widens and fades when the charm breaks
        float grown = clamp(age / APPEAR);
        grown = 1.0F - (1.0F - grown) * (1.0F - grown);
        float after = Math.max(0.0F, age - BREAK);
        float circle = age < BREAK ? clamp(age / 8.0F) : clamp(1.0F - after / 16.0F);
        if (circle > 0.0F) {
            pose.pushPose();
            float scale = (0.4F + 0.6F * grown) * (1.0F + after * 0.06F);
            pose.scale(scale, scale, 1.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(age * 3.5F));
            graphics.setColor(1.0F, 1.0F, 1.0F, circle);
            graphics.blit(SHEET, -64, -64, 128, 128, 0.0F, 0.0F, 256, 256, 512, 512);
            pose.popPose();
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (age < BREAK) {
            // the charm itself, whole: it trembles harder and harder as the cracks spread
            float tremble = age < CRACK ? 0.0F : (age - CRACK) / (BREAK - CRACK) * 1.8F;
            pose.pushPose();
            pose.translate((float) Math.sin(age * 2.9F) * tremble, (float) Math.cos(age * 3.7F) * tremble * 0.6F, 0.0F);
            pose.scale(grown, grown, 1.0F);
            graphics.blit(CHARM, -SIZE / 2, -SIZE / 2, SIZE, SIZE, 0.0F, 0.0F, 64, 64, 64, CHARM_STRIP);
            if (age >= CRACK) {
                int frame = Math.min(3, (int) ((age - CRACK) / (BREAK - CRACK) * 4.0F));
                graphics.blit(SHEET, -26, -22, 52, 52, 256.0F + frame * 64, 0.0F, 64, 64, 512, 512);
            }
            pose.popPose();
            // a flash from the little chest when the charm has appeared
            float flash = 1.0F - Math.abs(age - APPEAR - 2.0F) / 5.0F;
            if (flash > 0.0F) {
                graphics.setColor(1.0F, 1.0F, 1.0F, flash);
                graphics.blit(SHEET, -28, -24, 56, 56, 256.0F, 64.0F, 128, 128, 512, 512);
                graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } else {
            // the pieces fly apart, turning, and fall
            float fade = clamp(1.0F - after / 24.0F);
            int piece = SIZE / GRID;
            graphics.setColor(1.0F, 1.0F, 1.0F, fade);
            for (int i = 0; i < GRID * GRID; i++) {
                int column = i % GRID;
                int row = i / GRID;
                float x = (column - (GRID - 1) / 2.0F) * piece;
                float y = (row - (GRID - 1) / 2.0F) * piece;
                float length = (float) Math.sqrt(x * x + y * y);
                float speed = 2.2F + hash(i, 1) * 2.4F;
                pose.pushPose();
                pose.translate(x + x / length * speed * after, y + y / length * speed * after - after * 1.2F + after * after * 0.09F, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(after * (hash(i, 2) - 0.5F) * 50.0F));
                graphics.blit(CHARM, -piece / 2, -piece / 2, piece, piece, (float) (column * piece), (float) (row * piece), piece, piece, 64, CHARM_STRIP);
                pose.popPose();
            }
            // the burst of light
            if (after < 14.0F) {
                int frame = Math.min(3, (int) (after / 14.0F * 4.0F));
                float size = 70.0F + after * 6.0F;
                graphics.setColor(1.0F, 1.0F, 1.0F, clamp(1.4F - after / 12.0F));
                pose.pushPose();
                pose.scale(size / 128.0F, size / 128.0F, 1.0F);
                graphics.blit(SHEET, -64, -64, 128, 128, 256.0F + (frame % 2) * 128, 64.0F + (frame / 2) * 128, 128, 128, 512, 512);
                pose.popPose();
            }
            // sparks, and the little golden things: the kept inventory coming back
            for (int i = 0; i < FLYING; i++) {
                float angle = (i + hash(i, 3)) * (float) (Math.PI * 2.0D) / FLYING;
                float reach = (float) Math.pow(after, 0.8D) * (5.0F + hash(i, 4) * 4.5F);
                float alpha = clamp(1.3F - after / 34.0F);
                if (alpha <= 0.0F) {
                    continue;
                }
                graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
                pose.pushPose();
                pose.translate((float) Math.cos(angle) * reach, (float) Math.sin(angle) * reach * 0.8F + after * after * 0.02F, 0.0F);
                if (i % 2 == 0) {
                    pose.mulPose(Axis.ZP.rotationDegrees(after * (hash(i, 5) - 0.5F) * 16.0F));
                    graphics.blit(SHEET, -7, -7, 14, 14, (float) ((i / 2 % 6) * 32), 288.0F, 32, 32, 512, 512);
                } else {
                    int frame = (int) (after / 3.0F + i) % 4;
                    graphics.blit(SHEET, -6, -6, 12, 12, (float) (frame * 32), 256.0F, 32, 32, 512, 512);
                }
                pose.popPose();
            }
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }
}
