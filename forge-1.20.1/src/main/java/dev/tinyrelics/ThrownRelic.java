package dev.tinyrelics;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * A weapon in flight: the thrown hammer, the thrown boomerang and the boomerang
 * circling its owner as a shield. The real item never leaves the inventory,
 * this entity is only what everyone sees, so nothing can be lost.
 */
public class ThrownRelic extends Entity {
    /** Flies straight, hits the first thing it touches, then returns. */
    public static final int STRAIGHT = 0;
    /** Jumps from enemy to enemy, then returns. */
    public static final int CHAIN = 1;
    public static final int RETURN = 2;
    /** Circles the owner. */
    public static final int ORBIT = 3;

    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(ThrownRelic.class, EntityDataSerializers.ITEM_STACK);

    private static final double SPEED = 1.3D;
    private static final double STRAIGHT_RANGE = 28.0D;
    private static final double BOOMERANG_STRAIGHT_RANGE = 12.0D;
    private static final double FIRST_TARGET_RANGE = 16.0D;
    private static final double NEXT_TARGET_RANGE = 10.0D;
    private static final int MAX_CHAIN = 10;
    private static final int MAX_FLIGHT_TICKS = 160;
    private static final double ORBIT_RADIUS = 1.7D;
    private static final double SHIELD_REACH = 3.5D;
    private static final float SHIELD_DAMAGE = 4.0F;

    @Nullable
    private ServerPlayer owner;
    private int mode;
    private boolean shield;
    private Vec3 direction = Vec3.ZERO;
    private double travelled;
    private int life;
    private double angle;
    @Nullable
    private LivingEntity target;
    private final Set<Integer> alreadyHit = new HashSet<>();

    public ThrownRelic(EntityType<? extends ThrownRelic> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void launch(ServerLevel level, ServerPlayer owner, ItemStack stack, int mode) {
        ThrownRelic relic = new ThrownRelic(TinyRelics.THROWN_RELIC.get(), level);
        relic.owner = owner;
        relic.mode = mode;
        relic.shield = mode == ORBIT;
        relic.entityData.set(ITEM, stack.copyWithCount(1));
        relic.direction = owner.getLookAngle();
        Vec3 start = owner.getEyePosition().add(relic.direction.scale(0.8D)).subtract(0.0D, 0.3D, 0.0D);
        relic.setPos(start.x, start.y, start.z);
        level.addFreshEntity(relic);
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    private boolean isHammer() {
        return getItem().getItem() instanceof ThorHammerItem;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        this.life++;
        if (this.owner == null || !this.owner.isAlive() || this.owner.level() != server) {
            finish(false);
            return;
        }
        if (this.shield) {
            tickOrbit(server, this.owner);
            return;
        }
        if (this.life > MAX_FLIGHT_TICKS) {
            finish(false);
            return;
        }
        if (isHammer()) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.3D, getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
        } else {
            // A trail of purple sparks, and a swirl left hanging in the air every few blocks.
            server.sendParticles(TinyRelics.CHAOS_SPARK.get(), getX(), getY() + 0.3D, getZ(), 3, 0.15D, 0.15D, 0.15D, 0.0D);
            if (this.life % 3 == 0) {
                server.sendParticles(TinyRelics.CHAOS_VORTEX.get(), getX(), getY() + 0.3D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        switch (this.mode) {
            case STRAIGHT -> tickStraight(server, this.owner);
            case CHAIN -> tickChain(server, this.owner);
            default -> tickReturn(this.owner);
        }
    }

    private void tickStraight(ServerLevel server, ServerPlayer owner) {
        Vec3 next = position().add(this.direction.scale(SPEED));
        BlockPos block = BlockPos.containing(next);
        if (!server.getBlockState(block).getCollisionShape(server, block).isEmpty()) {
            if (isHammer()) {
                ThorHammerItem.lightning(server, getX(), getY(), getZ());
            }
            this.mode = RETURN;
            return;
        }
        setPos(next.x, next.y, next.z);
        this.travelled += SPEED;

        boolean monstersOnly = isHammer() && RelicConfig.SERVER.hammerMonstersOnly;
        List<LivingEntity> touched = server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6D),
                e -> e != owner && e.isAlive() && !(e instanceof ArmorStand) && !Minions.isOwnedBy(e, owner)
                        && (!monstersOnly || e instanceof Enemy));
        if (!touched.isEmpty()) {
            strike(server, touched.get(0), owner);
            this.mode = RETURN;
            return;
        }
        if (this.travelled >= (isHammer() ? STRAIGHT_RANGE : BOOMERANG_STRAIGHT_RANGE)) {
            this.mode = RETURN;
        }
    }

    private void tickChain(ServerLevel server, ServerPlayer owner) {
        if (this.target == null || !this.target.isAlive()) {
            this.target = findEnemy(server, owner, this.alreadyHit.isEmpty() ? FIRST_TARGET_RANGE : NEXT_TARGET_RANGE);
            if (this.target == null) {
                // Nobody to chase: a first throw just flies ahead, a finished chain comes home.
                this.mode = this.alreadyHit.isEmpty() ? STRAIGHT : RETURN;
                return;
            }
        }
        Vec3 goal = this.target.position().add(0.0D, this.target.getBbHeight() * 0.5D, 0.0D);
        Vec3 delta = goal.subtract(position());
        if (delta.length() <= SPEED + 0.6D) {
            setPos(goal.x, goal.y, goal.z);
            strike(server, this.target, owner);
            this.alreadyHit.add(this.target.getId());
            this.target = null;
            if (this.alreadyHit.size() >= MAX_CHAIN) {
                this.mode = RETURN;
            }
            return;
        }
        Vec3 next = position().add(delta.normalize().scale(SPEED));
        setPos(next.x, next.y, next.z);
    }

    private void tickReturn(ServerPlayer owner) {
        Vec3 goal = owner.position().add(0.0D, owner.getBbHeight() * 0.6D, 0.0D);
        Vec3 delta = goal.subtract(position());
        // Speeds up over time so it always catches a running player.
        double speed = SPEED + this.life * 0.02D;
        if (delta.length() <= speed + 0.8D) {
            finish(true);
            return;
        }
        Vec3 next = position().add(delta.normalize().scale(speed));
        setPos(next.x, next.y, next.z);
    }

    private void tickOrbit(ServerLevel server, ServerPlayer owner) {
        if (this.life > ChaosBoomerangItem.SHIELD_TICKS) {
            discard();
            return;
        }
        this.angle += 0.45D;
        setPos(owner.getX() + Math.cos(this.angle) * ORBIT_RADIUS,
                owner.getY() + owner.getBbHeight() * 0.5D,
                owner.getZ() + Math.sin(this.angle) * ORBIT_RADIUS);
        // The shield: sparks follow the boomerang and swirls mark the ring it draws.
        server.sendParticles(TinyRelics.CHAOS_SPARK.get(), getX(), getY() + 0.3D, getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
        if (this.life % 2 == 0) {
            server.sendParticles(TinyRelics.CHAOS_VORTEX.get(), getX(), getY() + 0.3D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        if (this.life % 5 == 0) {
            List<LivingEntity> near = server.getEntitiesOfClass(LivingEntity.class,
                    owner.getBoundingBox().inflate(SHIELD_REACH),
                    e -> e.isAlive() && Minions.isEnemyOf(e, owner));
            for (LivingEntity enemy : near) {
                enemy.hurt(server.damageSources().thrown(this, owner), SHIELD_DAMAGE);
                enemy.knockback(0.9D, owner.getX() - enemy.getX(), owner.getZ() - enemy.getZ());
            }
        }
    }

    private void strike(ServerLevel server, LivingEntity victim, ServerPlayer owner) {
        if (isHammer()) {
            ThorHammerItem.shock(server, victim, owner);
        } else {
            victim.hurt(server.damageSources().thrown(this, owner), ChaosBoomerangItem.THROW_DAMAGE);
            // A burst of swirls on the victim and sparks flying out of it.
            server.sendParticles(TinyRelics.CHAOS_VORTEX.get(),
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    5, victim.getBbWidth() * 0.4D, victim.getBbHeight() * 0.3D, victim.getBbWidth() * 0.4D, 0.0D);
            server.sendParticles(TinyRelics.CHAOS_SPARK.get(),
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                    16, 0.2D, 0.3D, 0.2D, 0.12D);
            server.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    @Nullable
    private LivingEntity findEnemy(ServerLevel server, ServerPlayer owner, double range) {
        Vec3 from = position();
        AABB box = new AABB(from, from).inflate(range);
        return server.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != owner && e.isAlive() && Minions.isEnemyOf(e, owner) && !this.alreadyHit.contains(e.getId()))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(from)))
                .orElse(null);
    }

    /** The flight is over: fix the item's cooldown and disappear. */
    private void finish(boolean caught) {
        if (this.owner != null && !this.shield) {
            ItemStack stack = getItem();
            if (isHammer()) {
                this.owner.getCooldowns().removeCooldown(stack.getItem());
            } else {
                this.owner.getCooldowns().addCooldown(stack.getItem(), ChaosBoomerangItem.COOLDOWN);
            }
            if (caught) {
                this.owner.level().playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
            }
        }
        discard();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
