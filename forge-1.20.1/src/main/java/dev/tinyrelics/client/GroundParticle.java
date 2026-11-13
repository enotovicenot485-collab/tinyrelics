package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A picture lying flat on the ground instead of facing the camera: the glowing
 * crack left by the hammer. Four frames: it opens, spreads, glows, then fades.
 */
public class GroundParticle extends TextureSheetParticle {
    private static final int OPEN_TICKS = 3;
    private static final int SPREAD_TICKS = 6;
    private static final int GLOW_TICKS = 45;
    private static final int LIFETIME = 70;

    private final SpriteSet sprites;
    private final float yaw;
    /** More than zero: a plain animation of this many frames played once (the steam swirl), not the crack. */
    private final int plainFrames;

    protected GroundParticle(ClientLevel level, double x, double y, double z, float size, SpriteSet sprites, int plainFrames, int lifetime) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = lifetime;
        this.plainFrames = plainFrames;
        this.quadSize = size;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.yaw = this.random.nextFloat() * Mth.TWO_PI;
        setSprite(sprites.get(0, plainFrames > 0 ? plainFrames - 1 : 3));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            remove();
            return;
        }
        if (this.plainFrames > 0) {
            setSprite(this.sprites.get(Math.min(this.plainFrames - 1, this.age * this.plainFrames / this.lifetime), this.plainFrames - 1));
            return;
        }
        int frame = this.age < OPEN_TICKS ? 0 : this.age < SPREAD_TICKS ? 1 : this.age < GLOW_TICKS ? 2 : 3;
        setSprite(this.sprites.get(frame, 3));
        this.alpha = this.age < GLOW_TICKS ? 1.0F : Math.max(0.0F, 1.0F - (this.age - GLOW_TICKS) / (float) (LIFETIME - GLOW_TICKS));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float cy = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float cz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
        float cos = Mth.cos(this.yaw) * this.quadSize;
        float sin = Mth.sin(this.yaw) * this.quadSize;
        // The four corners of a square in the ground plane, turned by the random yaw.
        float[] xs = {-cos + sin, -cos - sin, cos - sin, cos + sin};
        float[] zs = {-sin - cos, -sin + cos, sin + cos, sin - cos};
        float[] us = {getU0(), getU0(), getU1(), getU1()};
        float[] vs = {getV0(), getV1(), getV1(), getV0()};
        int light = getLightColor(partialTick);
        // Drawn once in each winding so it shows whichever way the renderer culls.
        for (int i = 0; i < 4; i++) {
            corner(buffer, cx + xs[i], cy, cz + zs[i], us[i], vs[i], light);
        }
        for (int i = 3; i >= 0; i--) {
            corner(buffer, cx + xs[i], cy, cz + zs[i], us[i], vs[i], light);
        }
    }

    private void corner(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.vertex(x, y, z).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    /** The first "speed" number of the spawn call carries the half-width of the picture in blocks. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        private final int plainFrames;
        private final int lifetime;

        public Provider(SpriteSet sprites) {
            this(sprites, 0, LIFETIME);
        }

        public Provider(SpriteSet sprites, int plainFrames, int lifetime) {
            this.sprites = sprites;
            this.plainFrames = plainFrames;
            this.lifetime = lifetime;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double size, double unused1, double unused2) {
            return new GroundParticle(level, x, y, z, (float) size, this.sprites, this.plainFrames, this.lifetime);
        }
    }
}
