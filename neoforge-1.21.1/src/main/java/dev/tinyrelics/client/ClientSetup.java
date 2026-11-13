package dev.tinyrelics.client;

import dev.tinyrelics.TinyRelics;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
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
        // The banner pictures of the creative tab: drawing them, and keeping hands off the cells under them.
        NeoForge.EVENT_BUS.addListener(TabBanners::onForeground);
        NeoForge.EVENT_BUS.addListener(TabBanners::onMousePressed);
        NeoForge.EVENT_BUS.addListener(TabBanners::onMouseReleased);
        NeoForge.EVENT_BUS.addListener(TabBanners::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(TabBanners::onTooltip);
        modBus.addListener(ClientSetup::addLayers);
        modBus.addListener(ClientSetup::registerExtensions);
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
        // The staff's ghost fire. Negative gravity makes a particle float upwards.
        event.registerSpriteSet(TinyRelics.GHOST_FLAME.get(), sprites -> new ChaosParticle.Provider(sprites, 12, 0.3F, -0.25F));
        event.registerSpriteSet(TinyRelics.GHOST_WISP.get(), sprites -> new ChaosParticle.Provider(sprites, 24, 0.4F, -0.12F));
        event.registerSpriteSet(TinyRelics.GHOST_FIRE.get(), sprites -> new ChaosParticle.Provider(sprites, 12, 0.5F, -0.03F));
        // Thor's Hammer electricity; the crack lies flat on the ground.
        event.registerSpriteSet(TinyRelics.THOR_SPARK.get(), sprites -> new ChaosParticle.Provider(sprites, 10, 0.25F, 0.0F));
        event.registerSpriteSet(TinyRelics.THOR_BOLT.get(), sprites -> new ChaosParticle.Provider(sprites, 8, 0.45F, 0.0F));
        event.registerSpriteSet(TinyRelics.THOR_FLASH.get(), sprites -> new ChaosParticle.Provider(sprites, 10, 0.95F, 0.0F));
        event.registerSpriteSet(TinyRelics.THOR_CRACK.get(), GroundParticle.Provider::new);
    }

    /** The necromancer armor is drawn with its own 3D model instead of the plain armor shape. */
    private static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return NecroArmorModels.get(slot);
            }
        }, TinyRelics.NECRO_HELMET.get(), TinyRelics.NECRO_CHESTPLATE.get(), TinyRelics.NECRO_LEGGINGS.get(), TinyRelics.NECRO_BOOTS.get());
    }

    /** Players and armor stands get the pulsing glow of the necromancer armor. */
    @SuppressWarnings("rawtypes")
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            addGlow((EntityRenderer) event.getSkin(skin));
        }
        addGlow((EntityRenderer) event.getRenderer(EntityType.ARMOR_STAND));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addGlow(EntityRenderer renderer) {
        if (renderer instanceof LivingEntityRenderer living) {
            living.addLayer(new NecroGlowLayer(living));
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TinyRelics.THROWN_RELIC.get(), ThrownRelicRenderer::new);
    }
}
