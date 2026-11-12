package dev.tinyrelics;

import dev.tinyrelics.client.ClientEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("4");
        registrar.playToClient(CharmBroken.TYPE, CharmBroken.STREAM_CODEC,
                (payload, context) -> ClientEffects.charmBroken());
        registrar.playToClient(ConfigSync.TYPE, ConfigSync.STREAM_CODEC,
                (payload, context) -> ClientEffects.receiveConfig(payload.config()));
        registrar.playToServer(ConfigUpdate.TYPE, ConfigUpdate.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer sender) {
                        applyConfig(sender, payload.config());
                    }
                });
        registrar.playToClient(VortexStart.TYPE, VortexStart.STREAM_CODEC,
                (payload, context) -> ClientEffects.startVortex(payload.entityId()));
        registrar.playToServer(StaffSuper.TYPE, StaffSuper.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer sender) {
                        UndeadStaffItem.trySuper(sender);
                    }
                });
    }

    /** Called on the client when the super ability key is pressed. */
    public static void requestStaffSuper() {
        PacketDistributor.sendToServer(StaffSuper.INSTANCE);
    }

    /** Tells the player and everyone who can see them to draw a blood tornado around them. */
    public static void sendVortex(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new VortexStart(player.getId()));
    }

    public static void sendCharmBroken(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, CharmBroken.INSTANCE);
    }

    public static void sendConfig(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ConfigSync(RelicConfig.SERVER.copy()));
    }

    /** Called on the client when "Apply" is pressed in the settings menu. */
    public static void requestConfigChange(RelicConfig config) {
        PacketDistributor.sendToServer(new ConfigUpdate(config.copy()));
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
        PacketDistributor.sendToAllPlayers(new ConfigSync(RelicConfig.SERVER.copy()));
        sender.displayClientMessage(Component.translatable("msg.tinyrelics.saved"), true);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, path);
    }

    /** Server -> client: "your charm has just saved your inventory, play the animation". */
    public record CharmBroken() implements CustomPacketPayload {
        public static final CharmBroken INSTANCE = new CharmBroken();
        public static final CustomPacketPayload.Type<CharmBroken> TYPE = new CustomPacketPayload.Type<>(id("charm_broken"));
        public static final StreamCodec<ByteBuf, CharmBroken> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the super ability key was pressed. */
    public record StaffSuper() implements CustomPacketPayload {
        public static final StaffSuper INSTANCE = new StaffSuper();
        public static final CustomPacketPayload.Type<StaffSuper> TYPE = new CustomPacketPayload.Type<>(id("staff_super"));
        public static final StreamCodec<ByteBuf, StaffSuper> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: a blood tornado starts around this entity. */
    public record VortexStart(int entityId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VortexStart> TYPE = new CustomPacketPayload.Type<>(id("vortex_start"));
        public static final StreamCodec<ByteBuf, VortexStart> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, VortexStart::entityId, VortexStart::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: the current settings, for the menu. */
    public record ConfigSync(RelicConfig config) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigSync> TYPE = new CustomPacketPayload.Type<>(id("config_sync"));
        public static final StreamCodec<FriendlyByteBuf, ConfigSync> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> payload.config().write(buffer), buffer -> new ConfigSync(RelicConfig.read(buffer)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: new settings from the menu. */
    public record ConfigUpdate(RelicConfig config) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigUpdate> TYPE = new CustomPacketPayload.Type<>(id("config_update"));
        public static final StreamCodec<FriendlyByteBuf, ConfigUpdate> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> payload.config().write(buffer), buffer -> new ConfigUpdate(RelicConfig.read(buffer)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
