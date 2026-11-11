package dev.tinyrelics;

import dev.tinyrelics.client.ClientSetup;
import net.minecraft.core.registries.Registries;
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
