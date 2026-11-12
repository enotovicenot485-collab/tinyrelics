package dev.tinyrelics.client;

import dev.tinyrelics.TinyRelics;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/** Client-only registration: the menu key and the renderer of flying weapons. */
public final class ClientSetup {
    public static final KeyMapping MENU_KEY =
            new KeyMapping("key.tinyrelics.menu", GLFW.GLFW_KEY_K, "key.categories.tinyrelics");

    public static final KeyMapping SUPER_KEY =
            new KeyMapping("key.tinyrelics.super", GLFW.GLFW_KEY_R, "key.categories.tinyrelics");

    private ClientSetup() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerKeys);
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerParticles);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MENU_KEY);
        event.register(SUPER_KEY);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        // Six swirl frames over 12 ticks, four spark frames over 8 ticks.
        event.registerSpriteSet(TinyRelics.CHAOS_VORTEX.get(), sprites -> new ChaosParticle.Provider(sprites, 12, 0.5F, 0.0F));
        event.registerSpriteSet(TinyRelics.CHAOS_SPARK.get(), sprites -> new ChaosParticle.Provider(sprites, 8, 0.22F, 0.0F));
        // Blood: five drop frames that fall, six crescent frames that hang in the air.
        event.registerSpriteSet(TinyRelics.BLOOD_DROP.get(), sprites -> new ChaosParticle.Provider(sprites, 10, 0.2F, 0.8F));
        event.registerSpriteSet(TinyRelics.BLOOD_SLASH.get(), sprites -> new ChaosParticle.Provider(sprites, 12, 0.55F, 0.0F));
        // The blood tornado: drops keep one random picture, crescents play their frames.
        event.registerSpriteSet(TinyRelics.VORTEX_DROP.get(), sprites -> new OrbitParticle.Provider(sprites, false, 0.2F));
        event.registerSpriteSet(TinyRelics.VORTEX_SLASH.get(), sprites -> new OrbitParticle.Provider(sprites, true, 0.6F));
        // Hearts beat three times on their way up, mist is half see-through, streaks give the feel of speed.
        event.registerSpriteSet(TinyRelics.VORTEX_HEART.get(), sprites -> new OrbitParticle.Provider(sprites, true, 0.38F, 3, 1.0F));
        event.registerSpriteSet(TinyRelics.VORTEX_MIST.get(), sprites -> new OrbitParticle.Provider(sprites, true, 0.95F, 1, 0.55F));
        event.registerSpriteSet(TinyRelics.VORTEX_STREAK.get(), sprites -> new OrbitParticle.Provider(sprites, true, 0.7F, 2, 1.0F));
        // The heart that floats up from everyone the vortex heals.
        event.registerSpriteSet(TinyRelics.BLOOD_HEART.get(), sprites -> new ChaosParticle.Provider(sprites, 18, 0.3F, -0.15F));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TinyRelics.THROWN_RELIC.get(), ThrownRelicRenderer::new);
    }
}
