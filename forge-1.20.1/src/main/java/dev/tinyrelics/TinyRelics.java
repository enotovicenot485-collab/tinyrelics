package dev.tinyrelics;

import dev.tinyrelics.client.ClientSetup;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TinyRelics.MODID)
public class TinyRelics {
    public static final String MODID = "tinyrelics";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);

    /** The purple swirl and spark of the Chaos Boomerang. */
    public static final RegistryObject<SimpleParticleType> CHAOS_VORTEX = PARTICLES.register("chaos_vortex", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CHAOS_SPARK = PARTICLES.register("chaos_spark", () -> new SimpleParticleType(false));
    /** The falling drop and the crescent streak of the Blood Blade. */
    public static final RegistryObject<SimpleParticleType> BLOOD_DROP = PARTICLES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_SLASH = PARTICLES.register("blood_slash", () -> new SimpleParticleType(false));
    /** The same pictures, but circling the player in the blood tornado. */
    public static final RegistryObject<SimpleParticleType> VORTEX_DROP = PARTICLES.register("vortex_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VORTEX_SLASH = PARTICLES.register("vortex_slash", () -> new SimpleParticleType(false));

    public static final RegistryObject<Item> KEEPING_CHARM = ITEMS.register("keeping_charm",
            () -> new KeepingCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> THOR_HAMMER = ITEMS.register("thor_hammer",
            () -> new ThorHammerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CHAOS_BOOMERANG = ITEMS.register("chaos_boomerang",
            () -> new ChaosBoomerangItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BLOOD_BLADE = ITEMS.register("blood_blade",
            () -> new BloodBladeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> UNDEAD_STAFF = ITEMS.register("undead_staff",
            () -> new UndeadStaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<EntityType<ThrownRelic>> THROWN_RELIC = ENTITIES.register("thrown_relic",
            () -> EntityType.Builder.<ThrownRelic>of(ThrownRelic::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build("thrown_relic"));

    // The no-argument constructor works on every Forge 47.x build, old and new.
    @SuppressWarnings({"removal", "deprecation"})
    public TinyRelics() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(this::addCreative);
        Network.register();
        RelicConfig.load(FMLPaths.CONFIGDIR.get().resolve("tinyrelics.json"));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modBus));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(THOR_HAMMER);
            event.accept(CHAOS_BOOMERANG);
            event.accept(BLOOD_BLADE);
            event.accept(UNDEAD_STAFF);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(KEEPING_CHARM);
        }
    }
}
