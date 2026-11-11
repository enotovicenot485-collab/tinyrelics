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
        event.registerSpriteSet(TinyRelics.CHAOS_VORTEX.get(), sprites -> new ChaosParticle.Provider(sprites, 12, 0.5F));
        event.registerSpriteSet(TinyRelics.CHAOS_SPARK.get(), sprites -> new ChaosParticle.Provider(sprites, 8, 0.22F));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TinyRelics.THROWN_RELIC.get(), ThrownRelicRenderer::new);
    }
}
