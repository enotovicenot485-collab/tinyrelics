package dev.tinyrelics;

import dev.tinyrelics.client.ClientEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TinyRelics.MODID)
public class TinyRelics {
    public static final String MODID = "tinyrelics";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<KeepingCharmItem> KEEPING_CHARM = ITEMS.register("keeping_charm",
            () -> new KeepingCharmItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredItem<ThorHammerItem> THOR_HAMMER = ITEMS.register("thor_hammer",
            () -> new ThorHammerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .attributes(ThorHammerItem.createAttributes())));

    public TinyRelics(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addCreative);
        modBus.addListener(this::registerPayloads);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(THOR_HAMMER.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(KEEPING_CHARM.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(CharmBroken.TYPE, CharmBroken.STREAM_CODEC,
                (payload, context) -> ClientEffects.charmBroken());
    }

    /** Server -> client: "your charm has just saved your inventory, play the animation". */
    public record CharmBroken() implements CustomPacketPayload {
        public static final CharmBroken INSTANCE = new CharmBroken();
        public static final CustomPacketPayload.Type<CharmBroken> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "charm_broken"));
        public static final StreamCodec<ByteBuf, CharmBroken> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
