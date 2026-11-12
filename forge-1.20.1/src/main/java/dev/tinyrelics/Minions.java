package dev.tinyrelics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Undead Staff's servants: ordinary skeletons and zombies whose own target
 * choice is switched off. They only go after whoever fights their owner.
 */
public final class Minions {
    /** Entity data key holding the owner's UUID. */
    public static final String OWNER = "tinyrelics_minion_owner";
    /** Servants live for one minute. */
    private static final int LIFETIME = 1200;
    /** Climbing out of the ground takes a second and a half. */
    private static final int RISE_TICKS = 30;
    private static final int SKELETONS = 2;
    private static final int ZOMBIES = 3;

    private static final Map<Mob, Servant> ACTIVE = new HashMap<>();

    private Minions() {
    }

    @Nullable
    public static UUID ownerOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(OWNER) ? data.getUUID(OWNER) : null;
    }

    public static boolean isMinion(Entity entity) {
        return entity.getPersistentData().hasUUID(OWNER);
    }

    public static boolean isOwnedBy(Entity entity, Player player) {
        return player.getUUID().equals(ownerOf(entity));
    }

    /** A servant that is not on the list was left over from before a restart or a chunk reload. */
    public static boolean isOrphan(Entity entity) {
        return isMinion(entity) && !ACTIVE.containsKey(entity);
    }

    /** A hostile mob that is not this player's own servant. */
    public static boolean isEnemyOf(LivingEntity entity, Player player) {
        return entity instanceof Enemy && !isOwnedBy(entity, player);
    }

    public static void summon(ServerLevel level, ServerPlayer owner) {
        int total = SKELETONS + ZOMBIES;
        for (int i = 0; i < total; i++) {
            boolean skeleton = i < SKELETONS;
            Mob mob = skeleton ? EntityType.SKELETON.create(level) : EntityType.ZOMBIE.create(level);
            if (mob == null) {
                continue;
            }
            double angle = i * (Math.PI * 2.0D / total);
            mob.moveTo(owner.getX() + Math.cos(angle) * 2.5D, owner.getY(), owner.getZ() + Math.sin(angle) * 2.5D,
                    owner.getYRot(), 0.0F);
            if (!level.noCollision(mob)) {
                mob.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
            }

            mob.getPersistentData().putUUID(OWNER, owner.getUUID());
            // No targets of their own: they never turn on players or villagers.
            mob.targetSelector.removeAllGoals(goal -> true);
            // The helmet keeps them from burning in daylight.
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
            if (skeleton) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
            mob.setCustomName(Component.translatable("entity.tinyrelics.minion", owner.getDisplayName()));

            // Start buried under the spot and climb out. Until it is out it does not move,
            // collide or take damage, so it cannot suffocate in the ground.
            double groundY = mob.getY();
            mob.setNoAi(true);
            mob.setInvulnerable(true);
            mob.noPhysics = true;
            mob.setPos(mob.getX(), groundY - mob.getBbHeight(), mob.getZ());

            ACTIVE.put(mob, new Servant(owner.getUUID(), LIFETIME, groundY));
            level.addFreshEntity(mob);
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<Mob, Servant>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Mob, Servant> entry = it.next();
            Mob mob = entry.getKey();
            Servant servant = entry.getValue();
            if (!mob.isAlive() || mob.isRemoved()) {
                it.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(servant.owner);
            if (--servant.ticksLeft <= 0 || owner == null || !owner.isAlive() || owner.level() != mob.level()) {
                if (mob.level() instanceof ServerLevel level) {
                    puff(level, mob);
                }
                mob.discard();
                it.remove();
                continue;
            }
            if (servant.rise > 0) {
                rise(mob, servant);
                continue;
            }
            if (servant.ticksLeft % 10 == 0) {
                command(mob, owner);
            }
        }
    }

    /** One tick of climbing out of the ground: a little higher, with dirt flying. */
    private static void rise(Mob mob, Servant servant) {
        servant.rise--;
        double buried = mob.getBbHeight() * servant.rise / (double) RISE_TICKS;
        mob.setPos(mob.getX(), servant.groundY - buried, mob.getZ());
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos below = BlockPos.containing(mob.getX(), servant.groundY - 0.5D, mob.getZ());
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    mob.getX(), servant.groundY + 0.1D, mob.getZ(), 6, 0.3D, 0.1D, 0.3D, 0.1D);
        }
        if (servant.rise % 5 == 0) {
            // A soul drifts up out of the grave while the servant climbs.
            level.sendParticles(TinyRelics.GHOST_WISP.get(), mob.getX(), servant.groundY + 0.3D, mob.getZ(),
                    1, 0.3D, 0.1D, 0.3D, 0.0D);
        }
        if (servant.rise % 6 == 0) {
            level.playSound(null, mob.getX(), servant.groundY, mob.getZ(),
                    SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
        if (servant.rise == 0) {
            mob.setNoAi(false);
            mob.setInvulnerable(false);
            mob.noPhysics = false;
            puff(level, mob);
        }
    }

    private static void command(Mob mob, ServerPlayer owner) {
        // First whoever hit the owner, then whoever the owner is hitting.
        LivingEntity wanted = owner.getLastHurtByMob();
        if (!isValidTarget(wanted, owner)) {
            wanted = owner.tickCount - owner.getLastHurtMobTimestamp() < 200 ? owner.getLastHurtMob() : null;
        }
        if (isValidTarget(wanted, owner)) {
            mob.setTarget(wanted);
            return;
        }
        if (mob.getTarget() != null && !isValidTarget(mob.getTarget(), owner)) {
            mob.setTarget(null);
        }
        if (mob.getTarget() == null) {
            double distance = mob.distanceToSqr(owner);
            if (distance > 32.0D * 32.0D) {
                mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            } else if (distance > 6.0D * 6.0D) {
                mob.getNavigation().moveTo(owner, 1.2D);
            }
        }
    }

    private static boolean isValidTarget(@Nullable LivingEntity target, ServerPlayer owner) {
        if (target == null || !target.isAlive() || target == owner || target.level() != owner.level()) {
            return false;
        }
        if (target instanceof ArmorStand || isOwnedBy(target, owner)) {
            return false;
        }
        if (target instanceof TamableAnimal pet && pet.isOwnedBy(owner)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return target.distanceToSqr(owner) < 48.0D * 48.0D;
    }

    private static void puff(ServerLevel level, Mob mob) {
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                10, 0.3D, 0.6D, 0.3D, 0.03D);
        level.sendParticles(TinyRelics.GHOST_WISP.get(), mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                6, 0.35D, 0.6D, 0.35D, 0.0D);
    }

    private static final class Servant {
        final UUID owner;
        /** The height of the ground the servant climbs out of. */
        final double groundY;
        int ticksLeft;
        /** Ticks of climbing still to go. */
        int rise = RISE_TICKS;

        Servant(UUID owner, int ticksLeft, double groundY) {
            this.owner = owner;
            this.ticksLeft = ticksLeft;
            this.groundY = groundY;
        }
    }
}
