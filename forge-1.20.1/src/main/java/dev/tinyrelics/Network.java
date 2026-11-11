package dev.tinyrelics;

import dev.tinyrelics.client.ClientEffects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TinyRelics.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, CharmBroken.class, CharmBroken::encode, CharmBroken::decode, CharmBroken::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendCharmBroken(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CharmBroken());
    }

    /** Server -> client: "your charm has just saved your inventory, play the animation". */
    public static final class CharmBroken {
        public static void encode(CharmBroken message, FriendlyByteBuf buffer) {
        }

        public static CharmBroken decode(FriendlyByteBuf buffer) {
            return new CharmBroken();
        }

        public static void handle(CharmBroken message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEffects::charmBroken));
            context.get().setPacketHandled(true);
        }
    }
}
