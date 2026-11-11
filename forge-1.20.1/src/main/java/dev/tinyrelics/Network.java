package dev.tinyrelics;

import dev.tinyrelics.client.ClientEffects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TinyRelics.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, CharmBroken.class, CharmBroken::encode, CharmBroken::decode, CharmBroken::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, ConfigSync.class, ConfigSync::encode, ConfigSync::decode, ConfigSync::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, ConfigUpdate.class, ConfigUpdate::encode, ConfigUpdate::decode, ConfigUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendCharmBroken(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CharmBroken());
    }

    public static void sendConfig(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ConfigSync(RelicConfig.SERVER.copy()));
    }

    /** Called on the client when "Apply" is pressed in the settings menu. */
    public static void requestConfigChange(RelicConfig config) {
        CHANNEL.sendToServer(new ConfigUpdate(config.copy()));
    }

    /** Server side: a player asked to change the settings. */
    private static void applyConfig(ServerPlayer sender, RelicConfig requested) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }
        boolean allowed = sender.hasPermissions(2) || server.isSingleplayerOwner(sender.getGameProfile());
        if (!allowed) {
            sender.displayClientMessage(Component.translatable("msg.tinyrelics.no_permission"), false);
            sendConfig(sender);
            return;
        }
        requested.clamp();
        RelicConfig.SERVER = requested;
        RelicConfig.save();
        Abilities.syncRecipes(server);
        CHANNEL.send(PacketDistributor.ALL.noArg(), new ConfigSync(RelicConfig.SERVER.copy()));
        sender.displayClientMessage(Component.translatable("msg.tinyrelics.saved"), true);
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

    /** Server -> client: the current settings, for the menu. */
    public static final class ConfigSync {
        private final RelicConfig config;

        public ConfigSync(RelicConfig config) {
            this.config = config;
        }

        public static void encode(ConfigSync message, FriendlyByteBuf buffer) {
            message.config.write(buffer);
        }

        public static ConfigSync decode(FriendlyByteBuf buffer) {
            return new ConfigSync(RelicConfig.read(buffer));
        }

        public static void handle(ConfigSync message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEffects.receiveConfig(message.config)));
            context.get().setPacketHandled(true);
        }
    }

    /** Client -> server: new settings from the menu. */
    public static final class ConfigUpdate {
        private final RelicConfig config;

        public ConfigUpdate(RelicConfig config) {
            this.config = config;
        }

        public static void encode(ConfigUpdate message, FriendlyByteBuf buffer) {
            message.config.write(buffer);
        }

        public static ConfigUpdate decode(FriendlyByteBuf buffer) {
            return new ConfigUpdate(RelicConfig.read(buffer));
        }

        public static void handle(ConfigUpdate message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer sender = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (sender != null) {
                    applyConfig(sender, message.config);
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}
