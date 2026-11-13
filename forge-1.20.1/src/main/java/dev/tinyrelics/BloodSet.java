package dev.tinyrelics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * The full Blood Master armor. Passive: four extra "blood hearts" that take damage before
 * the wearer's own health, never heal by themselves and are refilled only by killing
 * peaceful animals. Ability key: a rain of blood from drifting clouds, nine blocks across by default.
 */
public final class BloodSet {
    /** Blood hearts in half-hearts: four hearts. */
    public static final float MAX_HEARTS = 8.0F;
    private static final float HEARTS_PER_KILL = 2.0F;
    /** Kept through death, so dying does not hand the hearts back. */
    private static final String HEARTS = "tinyrelics_blood_hearts";
    private static final String HEARTS_GIVEN = "tinyrelics_blood_hearts_given";
    private static final String RAIN_READY = "tinyrelics_blood_rain_ready";

    /** A rain falls for six seconds. How long until the next one, and how wide it is, are settings. */
    private static final int LONGEST_COOLDOWN = RelicConfig.MAX_RAIN_COOLDOWN * 20;
    private static final int RAIN_TICKS = 120;
    private static final int HIT_EVERY = 10;
    /** The most one rain takes from one victim: three hearts. */
    private static final float MAX_DAMAGE = 6.0F;
    /** Separate clouds drift round the centre, so from the side it is a sky of clouds and not a slab. */
    private static final int CLOUDS = 6;
    private static final double CLOUD_HEIGHT = 6.0D;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final List<Rain> RAINS = new ArrayList<>();
    /** What each player's screen was last told, so the hearts are sent only when they change. */
    private static final Map<UUID, Float> SENT = new HashMap<>();

    private BloodSet() {
    }

    public static boolean hasFullSet(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            if (!(player.getItemBySlot(slot).getItem() instanceof BloodArmorItem)) {
                return false;
            }
        }
        return true;
    }

    private static CompoundTag kept(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static float hearts(Player player) {
        return kept(player).getFloat(HEARTS);
    }

    // ---- Blood as loot.

    /** Hostile mobs bleed dirty blood, peaceful animals clean blood, skeletons have none. Null = nothing. */
    public static Item bloodOf(LivingEntity victim) {
        if (victim instanceof Enemy) {
            return victim instanceof AbstractSkeleton ? null : TinyRelics.DIRTY_BLOOD_ITEM.get();
        }
        if (victim instanceof Animal && !(victim instanceof SkeletonHorse)) {
            return TinyRelics.BLOOD_DROP_ITEM.get();
        }
        return null;
    }

    // ---- Blood hearts.

    /** Damage about to reach the wearer's health: the blood hearts take it first. Returns what is left. */
    public static float absorb(ServerPlayer player, float damage) {
        if (damage <= 0.0F || !hasFullSet(player)) {
            return damage;
        }
        float hearts = hearts(player);
        if (hearts <= 0.0F) {
            return damage;
        }
        float taken = Math.min(hearts, damage);
        kept(player).putFloat(HEARTS, hearts - taken);
        return damage - taken;
    }

    /** The armor wants peaceful blood: every animal the wearer kills gives one blood heart back. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (!(victim instanceof Animal) || !hasFullSet(killer)) {
            return;
        }
        float hearts = hearts(killer);
        if (hearts >= MAX_HEARTS) {
            return;
        }
        kept(killer).putFloat(HEARTS, Math.min(MAX_HEARTS, hearts + HEARTS_PER_KILL));
        ServerLevel level = killer.serverLevel();
        level.sendParticles(TinyRelics.BLOOD_HEART.get(), victim.getX(), victim.getY() + victim.getBbHeight() * 0.6D, victim.getZ(),
                4, 0.25D, 0.25D, 0.25D, 0.02D);
        level.sendParticles(TinyRelics.BLOOD_DROP.get(), victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(),
                12, 0.3D, 0.3D, 0.3D, 0.05D);
    }

    /** The wearer died: the blood hearts are spent. */
    public static void onDeath(ServerPlayer player) {
        if (kept(player).getBoolean(HEARTS_GIVEN)) {
            kept(player).putFloat(HEARTS, 0.0F);
        }
    }

    // ---- Blood rain.

    /** The armor ability key. */
    public static void tryRain(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        long wait = data.getLong(RAIN_READY) - now;
        // A wait longer than the cooldown means the world clock was changed: ignore it.
        if (wait > 0 && wait <= LONGEST_COOLDOWN) {
            player.displayClientMessage(Component.translatable("msg.tinyrelics.rain_wait", (wait + 19) / 20), true);
            return;
        }
        data.putLong(RAIN_READY, now + RelicConfig.SERVER.rainCooldown * 20L);
        RAINS.add(new Rain(level, player.getUUID(), player.getX(), player.getY(), player.getZ(), RelicConfig.SERVER.rainRadius + 0.5D));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7F, 0.5F);
    }

    private static void tickRains() {
        Iterator<Rain> it = RAINS.iterator();
        while (it.hasNext()) {
            Rain rain = it.next();
            if (++rain.age > RAIN_TICKS) {
                it.remove();
                continue;
            }
            ServerLevel level = rain.level;
            RandomSource random = level.random;
            double top = rain.y + CLOUD_HEIGHT;
            double radius = rain.radius;
            // The clouds: each one circles the centre at its own distance, height and pace and
            // puffs where it is, so there are gaps between them and the edge is ragged.
            int puffs = Math.max(2, (int) Math.round(radius * 0.55D));
            for (int i = 0; i < puffs; i++) {
                int cloud = random.nextInt(CLOUDS);
                double angle = rain.seed + cloud * 2.4D + rain.age * (0.010D + 0.004D * (cloud % 3)) * (cloud % 2 == 0 ? 1.0D : -1.0D);
                double away = radius * (0.15D + 0.75D * ((cloud * 37 % 10) / 9.0D));
                double cx = rain.x + Math.cos(angle) * away + random.nextGaussian() * 0.9D;
                double cz = rain.z + Math.sin(angle) * away + random.nextGaussian() * 0.9D;
                double cy = top + ((cloud * 53 % 7) / 6.0D) * 1.6D - 0.4D + random.nextDouble() * 0.5D;
                level.sendParticles(TinyRelics.BLOOD_CLOUD.get(), cx, cy, cz, 0, Math.cos(angle + 1.6D), 0.0D, Math.sin(angle + 1.6D), 0.025D);
            }
            // The rain: every drop falls straight down and splashes where it lands. Spread evenly over the circle.
            int drops = Math.max(3, (int) Math.round(radius * radius * 0.3D));
            for (int i = 0; i < drops; i++) {
                double turn = random.nextDouble() * Math.PI * 2.0D;
                double far = Math.sqrt(random.nextDouble()) * radius;
                double dx = rain.x + Math.cos(turn) * far;
                double dz = rain.z + Math.sin(turn) * far;
                // count 0: the three numbers are the direction and the last one the speed
                level.sendParticles(TinyRelics.BLOOD_RAIN.get(), dx, top - 0.5D - random.nextDouble() * 1.5D, dz, 0, 0.0D, -1.0D, 0.0D, 0.8D);
                if (i % 2 == 0) {
                    int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(dx), (int) Math.floor(dz));
                    double floor = Math.abs(ground - rain.y) <= 5.0D ? ground : rain.y;
                    level.sendParticles(TinyRelics.BLOOD_SPLASH.get(), dx, floor + 0.3D, dz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (rain.age % 30 == 1) {
                level.playSound(null, rain.x, rain.y, rain.z, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.9F, 0.7F);
            }
            if (rain.age % HIT_EVERY != 0) {
                continue;
            }
            Player owner = level.getPlayerByUUID(rain.owner);
            AABB area = new AABB(rain.x - radius, rain.y - 4.0D, rain.z - radius, rain.x + radius, top, rain.z + radius);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!victim.isAlive() || victim.getUUID().equals(rain.owner) || isFriend(victim, owner)) {
                    continue;
                }
                double offX = victim.getX() - rain.x;
                double offZ = victim.getZ() - rain.z;
                if (offX * offX + offZ * offZ > radius * radius) {
                    continue;
                }
                float dealt = rain.dealt.getOrDefault(victim.getUUID(), 0.0F);
                if (dealt >= MAX_DAMAGE) {
                    continue;
                }
                // The usual pause between hits would swallow every second drop.
                victim.invulnerableTime = 0;
                boolean hurt = victim.hurt(owner != null ? level.damageSources().indirectMagic(owner, owner) : level.damageSources().magic(), 1.0F);
                if (hurt) {
                    rain.dealt.put(victim.getUUID(), dealt + 1.0F);
                }
            }
        }
    }

    private static boolean isFriend(LivingEntity victim, Player owner) {
        if (owner == null) {
            return false;
        }
        if (victim instanceof TamableAnimal pet && pet.isOwnedBy(owner)) {
            return true;
        }
        return Minions.isOwnedBy(victim, owner);
    }

    // ---- Every server tick.

    public static void tick(MinecraftServer server) {
        tickRains();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean full = hasFullSet(player);
            CompoundTag kept = kept(player);
            if (full && !kept.getBoolean(HEARTS_GIVEN)) {
                // The first time the set is put on, the hearts come full.
                kept.putBoolean(HEARTS_GIVEN, true);
                kept.putFloat(HEARTS, MAX_HEARTS);
            }
            float shown = full ? kept.getFloat(HEARTS) : -1.0F;
            Float sent = SENT.get(player.getUUID());
            if (sent == null || sent != shown || player.tickCount % 100 == 0) {
                SENT.put(player.getUUID(), shown);
                Network.sendBloodHearts(player, shown);
            }
        }
    }

    private static final class Rain {
        final ServerLevel level;
        final UUID owner;
        final double x;
        final double y;
        final double z;
        final double radius;
        /** Where the clouds start on their circles: different for every rain. */
        final double seed;
        final Map<UUID, Float> dealt = new HashMap<>();
        int age;

        Rain(ServerLevel level, UUID owner, double x, double y, double z, double radius) {
            this.level = level;
            this.owner = owner;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.seed = level.random.nextDouble() * 6.28D;
        }
    }
}
