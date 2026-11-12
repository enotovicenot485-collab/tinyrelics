package dev.tinyrelics;

import dev.tinyrelics.client.ClientSetup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TinyRelics.MODID)
public class TinyRelics {
    public static final String MODID = "tinyrelics";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    /** The purple swirl and spark of the Chaos Boomerang. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHAOS_VORTEX = PARTICLES.register("chaos_vortex", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHAOS_SPARK = PARTICLES.register("chaos_spark", () -> new SimpleParticleType(false));
    /** The falling drop and the crescent streak of the Blood Blade. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_DROP = PARTICLES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_SLASH = PARTICLES.register("blood_slash", () -> new SimpleParticleType(false));
    /** The same pictures, but circling the player in the blood tornado. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VORTEX_DROP = PARTICLES.register("vortex_drop", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VORTEX_SLASH = PARTICLES.register("vortex_slash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VORTEX_HEART = PARTICLES.register("vortex_heart", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VORTEX_MIST = PARTICLES.register("vortex_mist", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VORTEX_STREAK = PARTICLES.register("vortex_streak", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_HEART = PARTICLES.register("blood_heart", () -> new SimpleParticleType(false));
    /** The ghost fire of the Staff of the Undead: eye flame, rising soul, ground fire. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GHOST_FLAME = PARTICLES.register("ghost_flame", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GHOST_WISP = PARTICLES.register("ghost_wisp", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GHOST_FIRE = PARTICLES.register("ghost_fire", () -> new SimpleParticleType(false));

    public static final DeferredItem<KeepingCharmItem> KEEPING_CHARM = ITEMS.register("keeping_charm",
            () -> new KeepingCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<ThorHammerItem> THOR_HAMMER = ITEMS.register("thor_hammer",
            () -> new ThorHammerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<ChaosBoomerangItem> CHAOS_BOOMERANG = ITEMS.register("chaos_boomerang",
            () -> new ChaosBoomerangItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<BloodBladeItem> BLOOD_BLADE = ITEMS.register("blood_blade",
            () -> new BloodBladeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<UndeadStaffItem> UNDEAD_STAFF = ITEMS.register("undead_staff",
            () -> new UndeadStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRelic>> THROWN_RELIC = ENTITIES.register("thrown_relic",
            () -> EntityType.Builder.<ThrownRelic>of(ThrownRelic::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build("thrown_relic"));

    public TinyRelics(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(this::addCreative);
        modBus.addListener(Network::register);
        RelicConfig.load(FMLPaths.CONFIGDIR.get().resolve("tinyrelics.json"));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(THOR_HAMMER.get());
            event.accept(CHAOS_BOOMERANG.get());
            event.accept(BLOOD_BLADE.get());
            event.accept(UNDEAD_STAFF.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(KEEPING_CHARM.get());
        }
    }
}
