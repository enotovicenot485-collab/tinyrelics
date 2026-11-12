package dev.tinyrelics.client;

import dev.tinyrelics.Network;
import dev.tinyrelics.RelicConfig;
import dev.tinyrelics.TinyRelics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only visuals and the menu key. Never touched on a dedicated server. */
@Mod.EventBusSubscriber(modid = TinyRelics.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEffects {
    /** The settings as the server last told us. */
    public static RelicConfig config = new RelicConfig();

    /** Ticks left until the charm shatters; -1 when nothing is playing. */
    private static int shatterTimer = -1;

    /** How long a blood tornado lasts, the same five seconds as the healing on the server. */
    private static final int VORTEX_TICKS = 100;
    private static final double VORTEX_HEAL_RADIUS = 8.0D;
    /** Running tornadoes as {entity id, age in ticks}. */
    private static final List<int[]> VORTEXES = new ArrayList<>();
    /** The entity a tornado particle should circle; only set while such particles are being spawned. */
    public static Entity orbitCentre;

    private ClientEffects() {
    }

    public static void receiveConfig(RelicConfig received) {
        config = received;
    }

    /** The charm floats up in front of the screen, like a totem of undying. */
    public static void charmBroken() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        mc.gameRenderer.displayItemActivation(new ItemStack(TinyRelics.KEEPING_CHARM.get()));
        mc.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.2F, false);
        shatterTimer = 36;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (ClientSetup.MENU_KEY.consumeClick()) {
            if (mc.screen == null && canConfigure(mc)) {
                mc.setScreen(new RelicsScreen());
            }
        }
        tickVortexes(mc);
        while (ClientSetup.SUPER_KEY.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                Network.requestStaffSuper();
            }
        }
        if (shatterTimer >= 0 && --shatterTimer < 0) {
            shatter();
        }
    }

    /**
     * The settings menu is only for server operators, and for the owner of a
     * single-player world. For everyone else the key does nothing. The server
     * checks the same thing again before it accepts any change.
     */
    private static boolean canConfigure(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        return mc.getSingleplayerServer() != null
                && mc.getSingleplayerServer().isSingleplayerOwner(player.getGameProfile());
    }

    /** The server says: a blood tornado has started around this entity. */
    public static void startVortex(int entityId) {
        VORTEXES.add(new int[] {entityId, 0});
    }

    /**
     * Feeds every running tornado with new particles. It is drawn here on the client,
     * not sent from the server piece by piece, so it can be dense without any lag.
     */
    private static void tickVortexes(Minecraft mc) {
        if (mc.level == null) {
            VORTEXES.clear();
            return;
        }
        if (VORTEXES.isEmpty() || mc.isPaused()) {
            return;
        }
        RandomSource random = mc.level.random;
        Iterator<int[]> it = VORTEXES.iterator();
        while (it.hasNext()) {
            int[] vortex = it.next();
            Entity centre = mc.level.getEntity(vortex[0]);
            if (centre == null || !centre.isAlive() || ++vortex[1] > VORTEX_TICKS) {
                it.remove();
                continue;
            }
            int age = vortex[1];
            // Grows for the first second and thins out during the last one.
            double strength = Math.min(1.0D, Math.min(age / 20.0D, (VORTEX_TICKS - age) / 20.0D + 0.2D));
            int drops = (int) Math.round(14 * strength);
            int streaks = (int) Math.round(4 * strength);

            orbitCentre = centre;
            for (int i = 0; i < drops; i++) {
                // angle, start height (most start low), spread (most stay near the core)
                mc.level.addParticle(TinyRelics.VORTEX_DROP.get(), centre.getX(), centre.getY(), centre.getZ(),
                        random.nextDouble() * Math.PI * 2.0D,
                        random.nextDouble() * random.nextDouble() * 0.7D,
                        random.nextDouble() * random.nextDouble());
            }
            for (int i = 0; i < streaks; i++) {
                mc.level.addParticle(TinyRelics.VORTEX_SLASH.get(), centre.getX(), centre.getY(), centre.getZ(),
                        random.nextDouble() * Math.PI * 2.0D,
                        random.nextDouble() * 0.4D,
                        random.nextDouble() * 0.6D);
            }
            orbitCentre = null;

            // Drops splash up along the edge of the healing circle.
            for (int i = 0; i < 6; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                mc.level.addParticle(TinyRelics.BLOOD_DROP.get(),
                        centre.getX() + Math.cos(angle) * VORTEX_HEAL_RADIUS, centre.getY() + 0.1D,
                        centre.getZ() + Math.sin(angle) * VORTEX_HEAL_RADIUS,
                        0.0D, 0.25D + random.nextDouble() * 0.15D, 0.0D);
            }
        }
    }

    /** At the end of the animation the charm breaks into pieces in front of the camera. */
    private static void shatter() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        ItemStack stack = new ItemStack(TinyRelics.KEEPING_CHARM.get());
        ItemParticleOption crumbs = new ItemParticleOption(ParticleTypes.ITEM, stack);
        RandomSource random = player.getRandom();
        Vec3 centre = player.getEyePosition().add(player.getLookAngle().scale(0.7D));
        for (int i = 0; i < 30; i++) {
            mc.level.addParticle(crumbs,
                    centre.x + (random.nextDouble() - 0.5D) * 0.3D,
                    centre.y + (random.nextDouble() - 0.5D) * 0.3D,
                    centre.z + (random.nextDouble() - 0.5D) * 0.3D,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    random.nextDouble() * 0.15D,
                    (random.nextDouble() - 0.5D) * 0.2D);
        }
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F, false);
    }
}
