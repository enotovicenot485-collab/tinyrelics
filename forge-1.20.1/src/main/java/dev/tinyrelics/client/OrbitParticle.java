package dev.tinyrelics.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;

/**
 * One piece of the blood tornado. It circles an entity, climbs, and drifts outwards
 * as it climbs, so thousands of them together form a funnel that is narrow at the
 * feet and wide at the top, and that follows the entity when it walks.
 */
public class OrbitParticle extends TextureSheetParticle {
    /** How tall the tornado is, in blocks. */
    private static final double HEIGHT = 5.5D;

    private final SpriteSet sprites;
    private final boolean animated;
    /** How many times the frames play during the particle's life. */
    private final int loops;
    /** Opacity before the fade-out at the end. */
    private final float maxAlpha;
    private final Entity centre;
    /** 0 = hugs the core, 1 = flies in the loose outer layer. */
    private final double spread;
    private final double riseSpeed;
    private double angle;
    private double height;

    protected OrbitParticle(ClientLevel level, Entity centre, double angle, double startFraction, double spread,
                            SpriteSet sprites, boolean animated, float size, int loops, float maxAlpha) {
        super(level, centre.getX(), centre.getY(), centre.getZ());
        this.loops = loops;
        this.maxAlpha = maxAlpha;
        this.alpha = maxAlpha;
        this.sprites = sprites;
        this.animated = animated;
        this.centre = centre;
        this.angle = angle;
        this.spread = spread;
        this.lifetime = 28 + this.random.nextInt(22);
        this.height = startFraction * HEIGHT;
        this.riseSpeed = (HEIGHT - this.height) / this.lifetime;
        this.quadSize = size * (0.7F + this.random.nextFloat() * 0.7F);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        if (animated) {
            setSpriteFromAge(sprites);
        } else {
            pickSprite(sprites);
        }
        place();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    private double radius() {
        double t = this.height / HEIGHT;
        // A tight core that flares towards the top, plus a looser layer around it.
        return 0.5D + t * t * 2.6D + this.spread * (0.4D + t * 1.8D);
    }

    private void place() {
        double radius = radius();
        setPos(this.centre.getX() + Math.cos(this.angle) * radius,
                this.centre.getY() + this.height,
                this.centre.getZ() + Math.sin(this.angle) * radius);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime || !this.centre.isAlive()) {
            remove();
            return;
        }
        // The same sideways speed everywhere: the narrow part spins faster.
        this.angle += 0.6D / (radius() + 0.7D);
        this.height += this.riseSpeed;
        place();
        if (this.animated) {
            setSprite(this.sprites.get((this.age * this.loops) % this.lifetime, this.lifetime));
        }
        float left = 1.0F - (float) this.age / this.lifetime;
        this.alpha = this.maxAlpha * Math.min(1.0F, left * 4.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    /**
     * The three "speed" numbers of the spawn call carry the start angle, the start
     * height (0..1) and the spread (0..1); the entity to circle comes from ClientEffects.
     */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean animated;
        private final float size;
        private final int loops;
        private final float maxAlpha;

        public Provider(SpriteSet sprites, boolean animated, float size) {
            this(sprites, animated, size, 1, 1.0F);
        }

        public Provider(SpriteSet sprites, boolean animated, float size, int loops, float maxAlpha) {
            this.sprites = sprites;
            this.animated = animated;
            this.size = size;
            this.loops = loops;
            this.maxAlpha = maxAlpha;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double angle, double startFraction, double spread) {
            Entity centre = ClientEffects.orbitCentre;
            if (centre == null) {
                return null;
            }
            return new OrbitParticle(level, centre, angle, startFraction, spread, this.sprites, this.animated, this.size, this.loops, this.maxAlpha);
        }
    }
}
