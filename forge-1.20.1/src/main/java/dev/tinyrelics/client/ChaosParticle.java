package dev.tinyrelics.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The mod's own particles (boomerang swirls, blood drops): plays its frames once
 * from first to last and glows at full brightness. Gravity is optional.
 */
public class ChaosParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected ChaosParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed,
                            SpriteSet sprites, int lifetime, float size, float gravity) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.lifetime = lifetime;
        this.quadSize = size;
        this.gravity = gravity;
        this.hasPhysics = false;
        // The parent constructor adds a random kick: use exactly the speed we were given.
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final int lifetime;
        private final float size;
        private final float gravity;

        public Provider(SpriteSet sprites, int lifetime, float size, float gravity) {
            this.sprites = sprites;
            this.lifetime = lifetime;
            this.size = size;
            this.gravity = gravity;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ChaosParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, this.lifetime, this.size, this.gravity);
        }
    }
}
