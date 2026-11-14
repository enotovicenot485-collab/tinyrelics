package dev.tinyrelics;

import dev.tinyrelics.client.ClientSetup;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHAOS_STEAM = PARTICLES.register("chaos_steam", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHAOS_SWIRL = PARTICLES.register("chaos_swirl", () -> new SimpleParticleType(false));
    /** The falling drop and the crescent streak of the Blood Blade. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_DROP = PARTICLES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_CLOUD = PARTICLES.register("blood_cloud", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_RAIN = PARTICLES.register("blood_rain", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOOD_SPLASH = PARTICLES.register("blood_splash", () -> new SimpleParticleType(false));
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
    /** Thor's Hammer: spark, lightning arc, impact flash and the crack on the ground. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THOR_SPARK = PARTICLES.register("thor_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THOR_BOLT = PARTICLES.register("thor_bolt", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THOR_FLASH = PARTICLES.register("thor_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THOR_CRACK = PARTICLES.register("thor_crack", () -> new SimpleParticleType(false));

    public static final DeferredItem<KeepingCharmItem> KEEPING_CHARM = ITEMS.register("keeping_charm",
            () -> new KeepingCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<ThorHammerItem> THOR_HAMMER = ITEMS.register("thor_hammer",
            () -> new ThorHammerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<ChaosBoomerangItem> CHAOS_BOOMERANG = ITEMS.register("chaos_boomerang",
            () -> new ChaosBoomerangItem(new Item.Properties().durability(1561).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<BloodBladeItem> BLOOD_BLADE = ITEMS.register("blood_blade",
            () -> new BloodBladeItem(new Item.Properties().durability(1561).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    /** 2 + 5 + 3 + 1 = 11 armor points for the full set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NECRO_MATERIAL = ARMOR_MATERIALS.register("necromancer",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.HELMET, 2);
                        defense.put(ArmorItem.Type.CHESTPLATE, 5);
                        defense.put(ArmorItem.Type.LEGGINGS, 3);
                        defense.put(ArmorItem.Type.BOOTS, 1);
                        defense.put(ArmorItem.Type.BODY, 5);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.BONE_BLOCK),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "necromancer"))),
                    1.0F,
                    0.0F));

    /** 3 + 6 + 3 + 1 = 13 armor points for the full set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHAOS_MATERIAL = ARMOR_MATERIALS.register("chaos",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.HELMET, 3);
                        defense.put(ArmorItem.Type.CHESTPLATE, 6);
                        defense.put(ArmorItem.Type.LEGGINGS, 3);
                        defense.put(ArmorItem.Type.BOOTS, 1);
                        defense.put(ArmorItem.Type.BODY, 6);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.AMETHYST_SHARD),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "chaos"))),
                    1.0F,
                    0.0F));

    /** 4 + 7 + 5 + 2 = 18 armor points for the full set. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLOOD_MATERIAL = ARMOR_MATERIALS.register("blood",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.HELMET, 4);
                        defense.put(ArmorItem.Type.CHESTPLATE, 7);
                        defense.put(ArmorItem.Type.LEGGINGS, 5);
                        defense.put(ArmorItem.Type.BOOTS, 2);
                        defense.put(ArmorItem.Type.BODY, 7);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(TinyRelics.BLOOD_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "blood"))),
                    1.0F,
                    0.0F));

    public static final DeferredItem<NecroArmorItem> NECRO_HELMET = ITEMS.register("necro_helmet",
            () -> new NecroArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<NecroArmorItem> NECRO_CHESTPLATE = ITEMS.register("necro_chestplate",
            () -> new NecroArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<NecroArmorItem> NECRO_LEGGINGS = ITEMS.register("necro_leggings",
            () -> new NecroArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<NecroArmorItem> NECRO_BOOTS = ITEMS.register("necro_boots",
            () -> new NecroArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<ChaosArmorItem> CHAOS_HELMET = ITEMS.register("chaos_helmet",
            () -> new ChaosArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<ChaosArmorItem> CHAOS_CHESTPLATE = ITEMS.register("chaos_chestplate",
            () -> new ChaosArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<ChaosArmorItem> CHAOS_LEGGINGS = ITEMS.register("chaos_leggings",
            () -> new ChaosArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<ChaosArmorItem> CHAOS_BOOTS = ITEMS.register("chaos_boots",
            () -> new ChaosArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<BloodArmorItem> BLOOD_HELMET = ITEMS.register("blood_helmet",
            () -> new BloodArmorItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BloodArmorItem> BLOOD_CHESTPLATE = ITEMS.register("blood_chestplate",
            () -> new BloodArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BloodArmorItem> BLOOD_LEGGINGS = ITEMS.register("blood_leggings",
            () -> new BloodArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BloodArmorItem> BLOOD_BOOTS = ITEMS.register("blood_boots",
            () -> new BloodArmorItem(ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    /** Invisible: fills the cells of the creative tab that hold a banner picture or nothing at all. */
    public static final DeferredItem<Item> TAB_FILLER = ITEMS.register("tab_filler",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // ---- Blood: what mobs drop, the ingot made of it, and the altar that makes it.
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<Block, BloodAltarBlock> BLOOD_ALTAR = BLOCKS.register("blood_altar",
            () -> new BloodAltarBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.NETHER_WOOD)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloodAltarBlockEntity>> BLOOD_ALTAR_ENTITY = BLOCK_ENTITIES.register("blood_altar",
            () -> BlockEntityType.Builder.of(BloodAltarBlockEntity::new, BLOOD_ALTAR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BloodAltarMenu>> BLOOD_ALTAR_MENU = MENUS.register("blood_altar",
            () -> IMenuTypeExtension.create((id, inventory, extra) -> new BloodAltarMenu(id, inventory)));
    public static final DeferredItem<Item> BLOOD_DROP_ITEM = ITEMS.register("blood_drop", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIRTY_BLOOD_ITEM = ITEMS.register("dirty_blood_drop", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLOOD_INGOT = ITEMS.register("blood_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> BLOOD_ALTAR_ITEM = ITEMS.register("blood_altar",
            () -> new BlockItem(BLOOD_ALTAR.get(), new Item.Properties()));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** The mod's own creative tab, with Thor's Hammer as its icon. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tinyrelics"))
                    .icon(() -> new ItemStack(TinyRelics.THOR_HAMMER.get()))
                    .displayItems((parameters, output) -> RelicTab.fill(output))
                    .build());

    /** A filler carrying a number; every cell of the tab needs a different one. */
    public static ItemStack filler(int code) {
        ItemStack stack = new ItemStack(TAB_FILLER.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(code));
        return stack;
    }

    /** The number a filler carries, or -1 for anything that is not a filler. */
    public static int fillerCode(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return stack.is(TAB_FILLER.get()) && data != null ? data.value() : -1;
    }

    public static final DeferredItem<UndeadStaffItem> UNDEAD_STAFF = ITEMS.register("undead_staff",
            () -> new UndeadStaffItem(new Item.Properties().durability(2031).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRelic>> THROWN_RELIC = ENTITIES.register("thrown_relic",
            () -> EntityType.Builder.<ThrownRelic>of(ThrownRelic::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build("thrown_relic"));

    public TinyRelics(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        TABS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener(Network::register);
        RelicConfig.load(FMLPaths.CONFIGDIR.get().resolve("tinyrelics.json"));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus);
        }
    }
}
