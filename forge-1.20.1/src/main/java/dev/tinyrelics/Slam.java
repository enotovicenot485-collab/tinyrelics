package dev.tinyrelics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Thor's Hammer super ability: the wielder shoots about twenty blocks up, dives
 * straight down and the landing sends a ring of lightning across the ground.
 */
public final class Slam {
    /** Player data: the game time until which fall damage is ignored. */
    public static final String NO_FALL_UNTIL = "tinyrelics_slam_no_fall";
    private static final String READY = "tinyrelics_slam_ready";

    /** Upward speed that carries a player about 20 blocks high in 18 ticks. */
    private static final double LAUNCH_SPEED = 2.0D;
    private static final double DIVE_SPEED = -3.5D;
    private static final int RISE_TICKS = 18;
    private static final int MAX_TICKS = 120;
    /** Three seconds between landings. */
    private static final int COOLDOWN = 60;

    private static final List<Jump> JUMPS = new ArrayList<>();

    private Slam() {
    }

    /** The super ability key: whichever weapon is in the main hand answers. */
    public static void superKey(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ThorHammerItem) {
            tryStart(player);
        } else {
            UndeadStaffItem.trySuper(player);
        }
    }

    private static void tryStart(ServerPlayer player) {
        for (Jump jump : JUMPS) {
            if (jump.player == player) {
                return;
            }
        }
        ServerLevel server = player.serverLevel();
        long now = server.getGameTime();
        CompoundTag data = player.getPersistentData();
        long ready = data.getLong(READY);
        // A timer far in the future comes from another world: ignore it.
        if (now < ready && ready - now <= COOLDOWN) {
            return;
        }
        data.putLong(NO_FALL_UNTIL, now + MAX_TICKS + 40);
        if (player.getAbilities().flying) {
            // Creative flight would cancel the fall.
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        push(player, LAUNCH_SPEED);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.5F);
        server.sendParticles(TinyRelics.THOR_FLASH.get(), player.getX(), player.getY() + 0.3D, player.getZ(),
                3, 0.4D, 0.1D, 0.4D, 0.0D);
        JUMPS.add(new Jump(player));
    }

    /** Sets the player's vertical speed; the client is told so it actually moves. */
    private static void push(ServerPlayer player, double ySpeed) {
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * 0.2D, ySpeed, motion.z * 0.2D);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    public static void tick() {
        Iterator<Jump> it = JUMPS.iterator();
        while (it.hasNext()) {
            Jump jump = it.next();
            ServerPlayer player = jump.player;
            if (!player.isAlive() || player.isRemoved()) {
                it.remove();
                continue;
            }
            jump.age++;
            ServerLevel server = player.serverLevel();
            player.resetFallDistance();

            // A crackling trail on the way up and down.
            server.sendParticles(TinyRelics.THOR_SPARK.get(), player.getX(), player.getY() + 0.8D, player.getZ(),
                    4, 0.3D, 0.4D, 0.3D, 0.05D);
            if (jump.age % 2 == 0) {
                server.sendParticles(TinyRelics.THOR_BOLT.get(), player.getX(), player.getY() + 0.8D, player.getZ(),
                        1, 0.3D, 0.4D, 0.3D, 0.0D);
            }

            if (!jump.diving) {
                if (jump.age >= RISE_TICKS) {
                    jump.diving = true;
                    push(player, DIVE_SPEED);
                }
                continue;
            }
            if (jump.age % 3 == 0) {
                // Keep the dive fast against air drag.
                push(player, DIVE_SPEED);
            }
            if ((player.onGround() && jump.age > RISE_TICKS + 2) || player.isInWater() || jump.age > MAX_TICKS) {
                land(server, player);
                it.remove();
            }
        }
    }

    private static void land(ServerLevel server, ServerPlayer player) {
        RelicConfig config = RelicConfig.SERVER;
        double radius = config.hammerSlamRadius;
        long now = server.getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putLong(NO_FALL_UNTIL, now + 20);
        data.putLong(READY, now + COOLDOWN);
        player.resetFallDistance();

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        ThorHammerItem.lightning(server, x, y, z);

        DamageSource source = new DamageSource(
                server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),
                player);
        List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, 6.0D, radius),
                e -> ThorHammerItem.isTarget(e, player, config.hammerMonstersOnly)
                        && (e.getX() - x) * (e.getX() - x) + (e.getZ() - z) * (e.getZ() - z) <= radius * radius);
        for (LivingEntity victim : victims) {
            victim.hurt(source, ThorHammerItem.STRIKE_DAMAGE);
            if (config.hammerSlamFire) {
                ThorHammerItem.ignite(victim, 5);
            }
            // Thrown away from the point of impact.
            victim.knockback(1.2D, x - victim.getX(), z - victim.getZ());
        }

        // Chunks of the ground fly up; the crack and the ring are drawn by the clients.
        BlockPos below = BlockPos.containing(x, y - 0.5D, z);
        BlockState ground = server.getBlockState(below);
        if (!ground.isAir()) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, y + 0.2D, z,
                    60, 1.2D, 0.2D, 1.2D, 0.3D);
        }
        server.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F, 0.7F);
        server.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        Network.sendShockwave(player, x, y, z, (int) radius, true);
    }

    private static final class Jump {
        final ServerPlayer player;
        int age;
        boolean diving;

        Jump(ServerPlayer player) {
            this.player = player;
        }
    }
}
