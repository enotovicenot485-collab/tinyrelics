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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * The full Blood Master armor. Passive: four extra "blood hearts" that take damage before
 * the wearer's own health, never heal by themselves and are refilled only by killing
 * peaceful animals. Ability key: a rain of blood over a 9x9 area.
 */
public final class BloodSet {
    /** Blood hearts in half-hearts: four hearts. */
    public static final float MAX_HEARTS = 8.0F;
    private static final float HEARTS_PER_KILL = 2.0F;
    /** Kept through death, so dying does not hand the hearts back. */
    private static final String HEARTS = "tinyrelics_blood_hearts";
    private static final String HEARTS_GIVEN = "tinyrelics_blood_hearts_given";
    private static final String RAIN_READY = "tinyrelics_blood_rain_ready";

    /** Three minutes between rains; a rain falls for six seconds. */
    private static final int COOLDOWN = 3600;
    private static final int RAIN_TICKS = 120;
    private static final int HIT_EVERY = 10;
    /** The most one rain takes from one victim: three hearts. */
    private static final float MAX_DAMAGE = 6.0F;
    private static final double HALF_SIDE = 4.5D;
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
        if (wait > 0 && wait <= COOLDOWN) {
            player.displayClientMessage(Component.translatable("msg.tinyrelics.rain_wait", (wait + 19) / 20), true);
            return;
        }
        data.putLong(RAIN_READY, now + COOLDOWN);
        RAINS.add(new Rain(level, player.getUUID(), player.getX(), player.getY(), player.getZ()));
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
            // The cloud: thick puffs over the whole square, drifting a little.
            for (int i = 0; i < 5; i++) {
                level.sendParticles(TinyRelics.BLOOD_CLOUD.get(),
                        rain.x + (random.nextDouble() * 2.0D - 1.0D) * HALF_SIDE, top + random.nextDouble() * 1.2D,
                        rain.z + (random.nextDouble() * 2.0D - 1.0D) * HALF_SIDE, 0, random.nextDouble() - 0.5D, 0.0D, random.nextDouble() - 0.5D, 0.03D);
            }
            // The rain: every drop falls straight from the cloud and splashes where it lands.
            for (int i = 0; i < 12; i++) {
                double dx = rain.x + (random.nextDouble() * 2.0D - 1.0D) * HALF_SIDE;
                double dz = rain.z + (random.nextDouble() * 2.0D - 1.0D) * HALF_SIDE;
                // count 0: the three numbers are the direction and the last one the speed
                level.sendParticles(TinyRelics.BLOOD_RAIN.get(), dx, top - random.nextDouble() * 1.5D, dz, 0, 0.0D, -1.0D, 0.0D, 0.8D);
                if (i < 5) {
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
            AABB area = new AABB(rain.x - HALF_SIDE, rain.y - 4.0D, rain.z - HALF_SIDE, rain.x + HALF_SIDE, top, rain.z + HALF_SIDE);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!victim.isAlive() || victim.getUUID().equals(rain.owner) || isFriend(victim, owner)) {
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
        final Map<UUID, Float> dealt = new HashMap<>();
        int age;

        Rain(ServerLevel level, UUID owner, double x, double y, double z) {
            this.level = level;
            this.owner = owner;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
