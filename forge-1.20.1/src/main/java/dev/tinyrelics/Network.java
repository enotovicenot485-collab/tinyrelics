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
    private static final String VERSION = "6";

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
        CHANNEL.registerMessage(3, StaffSuper.class, StaffSuper::encode, StaffSuper::decode, StaffSuper::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, VortexStart.class, VortexStart::encode, VortexStart::decode, VortexStart::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, Shockwave.class, Shockwave::encode, Shockwave::decode, Shockwave::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Called on the client when the super ability key is pressed. */
    public static void requestStaffSuper() {
        CHANNEL.sendToServer(new StaffSuper());
    }

    /** Tells the player and everyone who can see them to draw a blood tornado around them. */
    /** Everyone who can see the player gets the ring of lightning (and, for a full strike, the crack). */
    public static void sendShockwave(ServerPlayer player, double x, double y, double z, int radius, boolean full) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new Shockwave(x, y, z, radius, full));
    }

    public static void sendVortex(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new VortexStart(player.getId()));
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

    /** Server -> client: a blood tornado starts around this entity. */
    public static final class Shockwave {
        private final double x;
        private final double y;
        private final double z;
        private final int radius;
        private final boolean full;

        public Shockwave(double x, double y, double z, int radius, boolean full) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.full = full;
        }

        public static void encode(Shockwave message, FriendlyByteBuf buffer) {
            buffer.writeDouble(message.x);
            buffer.writeDouble(message.y);
            buffer.writeDouble(message.z);
            buffer.writeVarInt(message.radius);
            buffer.writeBoolean(message.full);
        }

        public static Shockwave decode(FriendlyByteBuf buffer) {
            return new Shockwave(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean());
        }

        public static void handle(Shockwave message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEffects.startShockwave(message.x, message.y, message.z, message.radius, message.full)));
            context.get().setPacketHandled(true);
        }
    }

    /** Server -> client: a blood vortex starts around this entity. */
    public static final class VortexStart {
        private final int entityId;

        public VortexStart(int entityId) {
            this.entityId = entityId;
        }

        public static void encode(VortexStart message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId);
        }

        public static VortexStart decode(FriendlyByteBuf buffer) {
            return new VortexStart(buffer.readVarInt());
        }

        public static void handle(VortexStart message, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientEffects.startVortex(message.entityId)));
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

    /** Client -> server: the super ability key was pressed. */
    public static final class StaffSuper {
        public static void encode(StaffSuper message, FriendlyByteBuf buffer) {
        }

        public static StaffSuper decode(FriendlyByteBuf buffer) {
            return new StaffSuper();
        }

        public static void handle(StaffSuper message, Supplier<NetworkEvent.Context> context) {
            ServerPlayer sender = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (sender != null) {
                    Slam.superKey(sender);
                }
            });
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
