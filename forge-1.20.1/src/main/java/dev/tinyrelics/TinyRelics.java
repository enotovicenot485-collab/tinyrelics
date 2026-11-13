package dev.tinyrelics;

import dev.tinyrelics.client.ClientSetup;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
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
    public static final RegistryObject<SimpleParticleType> CHAOS_STEAM = PARTICLES.register("chaos_steam", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CHAOS_SWIRL = PARTICLES.register("chaos_swirl", () -> new SimpleParticleType(false));
    /** The falling drop and the crescent streak of the Blood Blade. */
    public static final RegistryObject<SimpleParticleType> BLOOD_DROP = PARTICLES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_SLASH = PARTICLES.register("blood_slash", () -> new SimpleParticleType(false));
    /** The same pictures, but circling the player in the blood tornado. */
    public static final RegistryObject<SimpleParticleType> VORTEX_DROP = PARTICLES.register("vortex_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VORTEX_SLASH = PARTICLES.register("vortex_slash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VORTEX_HEART = PARTICLES.register("vortex_heart", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VORTEX_MIST = PARTICLES.register("vortex_mist", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> VORTEX_STREAK = PARTICLES.register("vortex_streak", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_HEART = PARTICLES.register("blood_heart", () -> new SimpleParticleType(false));
    /** The ghost fire of the Staff of the Undead: eye flame, rising soul, ground fire. */
    public static final RegistryObject<SimpleParticleType> GHOST_FLAME = PARTICLES.register("ghost_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GHOST_WISP = PARTICLES.register("ghost_wisp", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> GHOST_FIRE = PARTICLES.register("ghost_fire", () -> new SimpleParticleType(false));
    /** Thor's Hammer: spark, lightning arc, impact flash and the crack on the ground. */
    public static final RegistryObject<SimpleParticleType> THOR_SPARK = PARTICLES.register("thor_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> THOR_BOLT = PARTICLES.register("thor_bolt", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> THOR_FLASH = PARTICLES.register("thor_flash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> THOR_CRACK = PARTICLES.register("thor_crack", () -> new SimpleParticleType(false));

    public static final RegistryObject<Item> KEEPING_CHARM = ITEMS.register("keeping_charm",
            () -> new KeepingCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> THOR_HAMMER = ITEMS.register("thor_hammer",
            () -> new ThorHammerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CHAOS_BOOMERANG = ITEMS.register("chaos_boomerang",
            () -> new ChaosBoomerangItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BLOOD_BLADE = ITEMS.register("blood_blade",
            () -> new BloodBladeItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> NECRO_HELMET = ITEMS.register("necro_helmet",
            () -> new NecroArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> NECRO_CHESTPLATE = ITEMS.register("necro_chestplate",
            () -> new NecroArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> NECRO_LEGGINGS = ITEMS.register("necro_leggings",
            () -> new NecroArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> NECRO_BOOTS = ITEMS.register("necro_boots",
            () -> new NecroArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> CHAOS_HELMET = ITEMS.register("chaos_helmet",
            () -> new ChaosArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> CHAOS_CHESTPLATE = ITEMS.register("chaos_chestplate",
            () -> new ChaosArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> CHAOS_LEGGINGS = ITEMS.register("chaos_leggings",
            () -> new ChaosArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> CHAOS_BOOTS = ITEMS.register("chaos_boots",
            () -> new ChaosArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BLOOD_HELMET = ITEMS.register("blood_helmet",
            () -> new BloodArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_CHESTPLATE = ITEMS.register("blood_chestplate",
            () -> new BloodArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_LEGGINGS = ITEMS.register("blood_leggings",
            () -> new BloodArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_BOOTS = ITEMS.register("blood_boots",
            () -> new BloodArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    /** Invisible: fills the cells of the creative tab that hold a banner picture or nothing at all. */
    public static final RegistryObject<Item> TAB_FILLER = ITEMS.register("tab_filler",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** The mod's own creative tab, with Thor's Hammer as its icon. */
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tinyrelics"))
                    .icon(() -> new ItemStack(TinyRelics.THOR_HAMMER.get()))
                    .displayItems((parameters, output) -> RelicTab.fill(output))
                    .build());

    /** A filler carrying a number; every cell of the tab needs a different one. */
    public static ItemStack filler(int code) {
        ItemStack stack = new ItemStack(TAB_FILLER.get());
        stack.getOrCreateTag().putInt("cell", code);
        return stack;
    }

    /** The number a filler carries, or -1 for anything that is not a filler. */
    public static int fillerCode(ItemStack stack) {
        return stack.is(TAB_FILLER.get()) && stack.hasTag() ? stack.getTag().getInt("cell") : -1;
    }

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
        TABS.register(modBus);
        PARTICLES.register(modBus);
        Network.register();
        RelicConfig.load(FMLPaths.CONFIGDIR.get().resolve("tinyrelics.json"));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modBus));
    }
}
