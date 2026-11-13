package dev.tinyrelics.client;

import dev.tinyrelics.Network;
import dev.tinyrelics.RelicConfig;
import dev.tinyrelics.TinyRelics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only visuals and the menu key. Never touched on a dedicated server. */
@EventBusSubscriber(modid = TinyRelics.MODID, value = Dist.CLIENT)
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

    /** Blood hearts of the Blood Master set in half-hearts, as the server last told us; below zero = the set is not on. */
    public static float bloodHearts = -1.0F;

    public static void receiveBloodHearts(float hearts) {
        bloodHearts = hearts;
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
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientSetup.MENU_KEY.consumeClick()) {
            if (mc.screen == null && canConfigure(mc)) {
                mc.setScreen(new RelicsScreen());
            }
        }
        tickVortexes(mc);
        tickShockwaves(mc);
        tickBladeDrips(mc);
        while (ClientSetup.ARMOR_KEY.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                Network.requestArmorAbility();
            }
        }
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
    /** Rings of lightning still spreading: x, y, z, final radius, age. */
    private static final List<double[]> SHOCKWAVES = new ArrayList<>();
    private static final int SHOCKWAVE_TICKS = 10;

    /** A ring of lightning runs out along the ground; a full strike also leaves a flash and a crack. */
    public static void startShockwave(double x, double y, double z, int radius, boolean full) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        SHOCKWAVES.add(new double[] {x, y, z, radius, 0});
        if (!full) {
            return;
        }
        RandomSource random = mc.level.random;
        // The big crack under the hammer and a few smaller ones around it.
        // Each one is laid on the surface it is actually over, so hills and steps are followed.
        mc.level.addParticle(TinyRelics.THOR_CRACK.get(), true, x, groundY(mc.level, x, y, z) + 0.03D, z,
                Math.max(1.8D, radius * 0.28D), 0.0D, 0.0D);
        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = radius * (0.35D + random.nextDouble() * 0.35D);
            double cx = x + Math.cos(angle) * distance;
            double cz = z + Math.sin(angle) * distance;
            mc.level.addParticle(TinyRelics.THOR_CRACK.get(), true, cx, groundY(mc.level, cx, y, cz) + 0.03D, cz, 1.0D, 0.0D, 0.0D);
        }
        y = groundY(mc.level, x, y, z);
        for (int i = 0; i < 6; i++) {
            mc.level.addParticle(TinyRelics.THOR_FLASH.get(), true,
                    x + (random.nextDouble() - 0.5D) * 2.5D, y + 0.6D + random.nextDouble() * 1.5D,
                    z + (random.nextDouble() - 0.5D) * 2.5D, 0.0D, 0.0D, 0.0D);
        }
        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double speed = 0.15D + random.nextDouble() * 0.35D;
            mc.level.addParticle(TinyRelics.THOR_SPARK.get(), x, y + 0.3D, z,
                    Math.cos(angle) * speed, 0.1D + random.nextDouble() * 0.4D, Math.sin(angle) * speed);
        }
    }

    /**
     * The height of the ground at this spot: the top of the first solid block found
     * looking down from a little above the given height. Falls back to the given height.
     */
    private static double groundY(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Start two blocks up to catch a step or a slope; under a low roof start at the feet instead.
        for (double above : new double[] {2.0D, 0.5D}) {
            int top = (int) Math.floor(y + above);
            pos.set((int) Math.floor(x), top, (int) Math.floor(z));
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            for (int blockY = top - 1; blockY >= top - 10; blockY--) {
                pos.setY(blockY);
                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                if (!shape.isEmpty()) {
                    return blockY + shape.max(Direction.Axis.Y);
                }
            }
            return y;
        }
        return y;
    }

    /** Now and then a drop of blood falls off a Blood Blade that someone is holding. */
    private static void tickBladeDrips(Minecraft mc) {
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        for (net.minecraft.world.entity.player.Player player : mc.level.players()) {
            if (player.isInvisible() || player.getRandom().nextInt(50) != 0) {
                continue;
            }
            boolean main = player.getMainHandItem().getItem() instanceof dev.tinyrelics.BloodBladeItem;
            if (!main && !(player.getOffhandItem().getItem() instanceof dev.tinyrelics.BloodBladeItem)) {
                continue;
            }
            // which side of the body the sword is on, then a point somewhere along the blade
            boolean rightSide = (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) == main;
            boolean ownEyes = player == mc.player && mc.options.getCameraType().isFirstPerson();
            double yaw = Math.toRadians(ownEyes ? player.getYRot() : player.yBodyRot);
            double side = rightSide ? 0.42D : -0.42D;
            double ahead = 0.45D + player.getRandom().nextDouble() * 0.6D;
            double x = player.getX() - Math.cos(yaw) * side - Math.sin(yaw) * ahead;
            double z = player.getZ() - Math.sin(yaw) * side + Math.cos(yaw) * ahead;
            double y = player.getY() + 0.85D + (ahead - 0.45D) * 0.9D;
            mc.level.addParticle(TinyRelics.BLOOD_DROP.get(), x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void tickShockwaves(Minecraft mc) {
        if (mc.level == null) {
            SHOCKWAVES.clear();
            return;
        }
        if (SHOCKWAVES.isEmpty() || mc.isPaused()) {
            return;
        }
        RandomSource random = mc.level.random;
        Iterator<double[]> it = SHOCKWAVES.iterator();
        while (it.hasNext()) {
            double[] wave = it.next();
            if (++wave[4] > SHOCKWAVE_TICKS) {
                it.remove();
                continue;
            }
            double radius = wave[3] * wave[4] / SHOCKWAVE_TICKS;
            int arcs = Math.max(10, Math.min(120, (int) (radius * 4.0D)));
            double turn = random.nextDouble() * Math.PI * 2.0D;
            for (int i = 0; i < arcs; i++) {
                double angle = turn + i * Math.PI * 2.0D / arcs;
                double px = wave[0] + Math.cos(angle) * radius;
                double pz = wave[2] + Math.sin(angle) * radius;
                double py = groundY(mc.level, px, wave[1], pz);
                mc.level.addParticle(TinyRelics.THOR_BOLT.get(), px, py + 0.3D, pz, 0.0D, 0.0D, 0.0D);
                if (i % 2 == 0) {
                    mc.level.addParticle(TinyRelics.THOR_SPARK.get(), px, py + 0.2D, pz,
                            0.0D, 0.05D + random.nextDouble() * 0.15D, 0.0D);
                }
            }
        }
    }

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
            int jets = (int) Math.round(4 * strength);
            int mist = (int) Math.round(3 * strength);
            int hearts = (int) Math.round(2 * strength);

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
            for (int i = 0; i < jets; i++) {
                // Fast streaks anywhere in the funnel.
                mc.level.addParticle(TinyRelics.VORTEX_STREAK.get(), centre.getX(), centre.getY(), centre.getZ(),
                        random.nextDouble() * Math.PI * 2.0D,
                        random.nextDouble() * 0.6D,
                        random.nextDouble());
            }
            for (int i = 0; i < mist; i++) {
                // Mist fills the body of the funnel and makes it look solid.
                mc.level.addParticle(TinyRelics.VORTEX_MIST.get(), centre.getX(), centre.getY(), centre.getZ(),
                        random.nextDouble() * Math.PI * 2.0D,
                        random.nextDouble() * 0.5D,
                        random.nextDouble() * 0.8D);
            }
            for (int i = 0; i < hearts; i++) {
                // Beating hearts ride the loose outer layer, where they are easy to see.
                mc.level.addParticle(TinyRelics.VORTEX_HEART.get(), centre.getX(), centre.getY(), centre.getZ(),
                        random.nextDouble() * Math.PI * 2.0D,
                        random.nextDouble() * 0.5D,
                        0.6D + random.nextDouble() * 0.4D);
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
