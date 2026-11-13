package dev.tinyrelics;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * The full Chaos Assassin armor: Speed III all the time, and on a key the wearer
 * vanishes in a cloud of steam for ten seconds and runs with Speed V.
 */
public final class ChaosSet {
    /** Player data: game time when the ability is ready again, and when the current vanishing ends. */
    private static final String READY = "tinyrelics_vanish_ready";
    private static final String UNTIL = "tinyrelics_vanish_until";
    /** Two minutes between uses, ten seconds unseen. */
    private static final int COOLDOWN = 2400;
    private static final int DURATION = 200;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ChaosSet() {
    }

    public static boolean hasFullSet(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            if (!(player.getItemBySlot(slot).getItem() instanceof ChaosArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /** True while the wearer is unseen after using the ability. */
    public static boolean isVanished(ServerPlayer player) {
        long left = player.getPersistentData().getLong(UNTIL) - player.serverLevel().getGameTime();
        return left > 0 && left <= DURATION;
    }

    /** The armor ability key. */
    public static void tryVanish(ServerPlayer player) {
        if (!hasFullSet(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        long wait = data.getLong(READY) - now;
        // A wait longer than the cooldown means the world clock was changed: ignore it.
        if (wait > 0 && wait <= COOLDOWN) {
            player.displayClientMessage(Component.translatable("msg.tinyrelics.vanish_wait", (wait + 19) / 20), true);
            return;
        }
        data.putLong(READY, now + COOLDOWN);
        data.putLong(UNTIL, now + DURATION);
        steam(level, player, 1.0F);
        // No potion swirls and no icon flicker for others: nothing gives the wearer away.
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION, 4, false, false, true));
        // Whatever was chasing the wearer loses the trail.
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0D))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
        }
    }

    /** A thick cloud that swallows the player. */
    private static void steam(ServerLevel level, ServerPlayer player, float strength) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        level.sendParticles(ParticleTypes.CLOUD, x, y + 1.0D, z, (int) (90 * strength), 0.7D, 0.9D, 0.7D, 0.02D);
        level.sendParticles(ParticleTypes.POOF, x, y + 1.0D, z, (int) (40 * strength), 0.6D, 0.8D, 0.6D, 0.04D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.6D, z, (int) (30 * strength), 0.6D, 0.6D, 0.6D, 0.01D);
        level.sendParticles(ParticleTypes.WITCH, x, y + 1.0D, z, (int) (25 * strength), 0.6D, 0.8D, 0.6D, 0.0D);
        level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 0.6F);
        level.playSound(null, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Every server tick. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag data = player.getPersistentData();
            long until = data.getLong(UNTIL);
            boolean full = hasFullSet(player);
            if (until > 0) {
                long now = player.serverLevel().getGameTime();
                // Taking a piece off breaks the spell at once.
                if (!full && now < until) {
                    player.removeEffect(MobEffects.INVISIBILITY);
                    player.removeEffect(MobEffects.MOVEMENT_SPEED);
                }
                if (!full || now >= until || until - now > DURATION) {
                    data.remove(UNTIL);
                    steam(player.serverLevel(), player, 0.4F);
                }
            }
            // The set's own speed is renewed a little before it runs out, so it never blinks.
            if (full && player.tickCount % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false, true));
            }
        }
    }
}
