package dev.tinyrelics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Things that play out over time on the server: bleeding, the blade's drain, recipe switches. */
public final class Abilities {
    private static final int BLEED_TICKS = 100;
    private static final int DRAIN_TICKS = 60;
    private static final DustParticleOptions BLOOD_DROP = new DustParticleOptions(new Vector3f(0.75F, 0.0F, 0.0F), 1.3F);

    private static final ResourceLocation CHARM_RECIPE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "keeping_charm");
    private static final ResourceLocation BOOMERANG_RECIPE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "chaos_boomerang");
    private static final ResourceLocation BLADE_RECIPE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "blood_blade");
    private static final ResourceLocation STAFF_RECIPE = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "undead_staff");

    /** Soul fire burns for 5 seconds over a 30 x 30 block square. */
    private static final int SOUL_FIRE_TICKS = 100;
    private static final double SOUL_FIRE_HALF = 15.0D;
    private static final int SOUL_FIRE_FLAMES_PER_TICK = 60;
    private static final float SOUL_FIRE_DAMAGE = 5.0F;

    /** Bleeding entities and how many ticks they still bleed. */
    private static final Map<LivingEntity, Integer> BLEEDING = new HashMap<>();
    private static final List<Drain> DRAINS = new ArrayList<>();
    private static final List<SoulFire> SOUL_FIRES = new ArrayList<>();
    /** Recipes taken out of the game, kept here so they can be switched back on. */
    private static final Map<ResourceLocation, RecipeHolder<?>> STASHED = new HashMap<>();
    private static int tickCounter;

    private Abilities() {
    }

    /** Starts 5 seconds of bleeding with one burst of blood. */
    public static void bleed(LivingEntity target, @Nullable LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel server) || !target.isAlive()) {
            return;
        }
        if (attacker instanceof Player player) {
            target.setLastHurtByPlayer(player);
        }
        BLEEDING.put(target, BLEED_TICKS);
        bloodBurst(server, target, 30);
        server.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    public static void startDrain(ServerPlayer player, List<LivingEntity> targets) {
        DRAINS.add(new Drain(player, new ArrayList<>(targets)));
    }

    /** The Undead Staff's super ability: a field of blue fire around where the player stands. */
    public static void startSoulFire(ServerPlayer player) {
        ServerLevel server = player.serverLevel();
        SOUL_FIRES.add(new SoulFire(player, server, player.getX(), player.getY(), player.getZ()));
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0F, 0.5F);
    }

    public static void tick(MinecraftServer server) {
        tickBleeding();
        tickDrains();
        tickSoulFires();
        Minions.tick(server);
        if (++tickCounter % 20 == 0) {
            syncRecipes(server);
        }
    }

    private static void tickBleeding() {
        Iterator<Map.Entry<LivingEntity, Integer>> it = BLEEDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Integer> entry = it.next();
            LivingEntity entity = entry.getKey();
            int left = entry.getValue() - 1;
            if (left <= 0 || !entity.isAlive() || entity.isRemoved() || !(entity.level() instanceof ServerLevel server)) {
                it.remove();
                continue;
            }
            entry.setValue(left);
            if (left % 4 == 0) {
                server.sendParticles(BLOOD_DROP, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        2, entity.getBbWidth() * 0.3D, entity.getBbHeight() * 0.25D, entity.getBbWidth() * 0.3D, 0.0D);
            }
            if (left % 20 == 0) {
                entity.hurt(server.damageSources().magic(), 1.0F);
            }
        }
    }

    private static void tickDrains() {
        Iterator<Drain> it = DRAINS.iterator();
        while (it.hasNext()) {
            Drain drain = it.next();
            ServerPlayer player = drain.player;
            if (!player.isAlive() || player.isRemoved()) {
                it.remove();
                continue;
            }
            drain.age++;
            ServerLevel server = player.serverLevel();
            Vec3 chest = player.position().add(0.0D, player.getBbHeight() * 0.6D, 0.0D);
            boolean pulse = drain.age % 10 == 0;

            for (LivingEntity target : drain.targets) {
                if (!target.isAlive() || target.level() != server) {
                    continue;
                }
                // A heart sliding from the mob to the player, again and again.
                Vec3 from = target.position().add(0.0D, target.getBbHeight() * 0.7D, 0.0D);
                Vec3 at = from.lerp(chest, (drain.age % 12) / 12.0D);
                server.sendParticles(ParticleTypes.HEART, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if (pulse) {
                    target.setLastHurtByPlayer(player);
                    target.hurt(server.damageSources().magic(), 4.0F);
                    bloodBurst(server, target, 12);
                }
            }
            if (pulse) {
                player.heal(player.getMaxHealth() / 6.0F);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 1.0F);
            }
            if (drain.age >= DRAIN_TICKS) {
                player.setHealth(player.getMaxHealth());
                it.remove();
            }
        }
    }

    private static void tickSoulFires() {
        Iterator<SoulFire> it = SOUL_FIRES.iterator();
        while (it.hasNext()) {
            SoulFire fire = it.next();
            ServerLevel server = fire.level;
            if (++fire.age > SOUL_FIRE_TICKS) {
                it.remove();
                continue;
            }
            RandomSource random = server.getRandom();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < SOUL_FIRE_FLAMES_PER_TICK; i++) {
                double x = fire.x + (random.nextDouble() - 0.5D) * 2.0D * SOUL_FIRE_HALF;
                double z = fire.z + (random.nextDouble() - 0.5D) * 2.0D * SOUL_FIRE_HALF;
                // Look for the ground near the player's height so flames sit on the floor, in caves too.
                int top = Mth.floor(fire.y) + 4;
                for (int y = top; y >= top - 10; y--) {
                    pos.set(Mth.floor(x), y, Mth.floor(z));
                    boolean solidBelow = !server.getBlockState(pos.below()).getCollisionShape(server, pos.below()).isEmpty();
                    boolean freeHere = server.getBlockState(pos).getCollisionShape(server, pos).isEmpty();
                    if (solidBelow && freeHere) {
                        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.2D, z, 2, 0.15D, 0.25D, 0.15D, 0.01D);
                        break;
                    }
                }
            }
            if (fire.age % 20 == 1) {
                server.playSound(null, fire.x, fire.y, fire.z, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 2.0F, 0.6F);
            }
            if (fire.age % 10 == 0) {
                AABB area = new AABB(fire.x - SOUL_FIRE_HALF, fire.y - 6.0D, fire.z - SOUL_FIRE_HALF,
                        fire.x + SOUL_FIRE_HALF, fire.y + 8.0D, fire.z + SOUL_FIRE_HALF);
                List<LivingEntity> burning = server.getEntitiesOfClass(LivingEntity.class, area,
                        e -> ThorHammerItem.isTarget(e, fire.owner, false));
                for (LivingEntity victim : burning) {
                    victim.setLastHurtByPlayer(fire.owner);
                    victim.hurt(server.damageSources().magic(), SOUL_FIRE_DAMAGE);
                    victim.igniteForSeconds(3.0F);
                }
            }
        }
    }

    private static void bloodBurst(ServerLevel server, LivingEntity entity, int count) {
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(),
                count, entity.getBbWidth() * 0.3D, entity.getBbHeight() * 0.2D, entity.getBbWidth() * 0.3D, 0.15D);
    }

    /**
     * Makes the loaded recipes match the settings: a switched-off recipe is removed
     * from the game (crafting table, recipe book and automation all lose it) and put
     * back when it is switched on again. Checked once a second, so it also survives /reload.
     */
    public static void syncRecipes(MinecraftServer server) {
        RelicConfig config = RelicConfig.SERVER;
        Map<ResourceLocation, Boolean> wanted = new LinkedHashMap<>();
        wanted.put(CHARM_RECIPE, config.charmCraft);
        wanted.put(BOOMERANG_RECIPE, config.boomerangCraft);
        wanted.put(BLADE_RECIPE, config.bladeCraft);
        wanted.put(STAFF_RECIPE, config.staffCraft);

        RecipeManager manager = server.getRecipeManager();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, Boolean> entry : wanted.entrySet()) {
            Optional<RecipeHolder<?>> loaded = manager.byKey(entry.getKey());
            if (loaded.isPresent()) {
                STASHED.put(entry.getKey(), loaded.get());
                if (!entry.getValue()) {
                    changed = true;
                }
            } else if (entry.getValue() && STASHED.containsKey(entry.getKey())) {
                changed = true;
            }
        }
        if (!changed) {
            return;
        }

        List<RecipeHolder<?>> recipes = new ArrayList<>();
        for (RecipeHolder<?> recipe : manager.getRecipes()) {
            if (!wanted.containsKey(recipe.id())) {
                recipes.add(recipe);
            }
        }
        for (Map.Entry<ResourceLocation, Boolean> entry : wanted.entrySet()) {
            RecipeHolder<?> stashed = STASHED.get(entry.getKey());
            if (entry.getValue() && stashed != null) {
                recipes.add(stashed);
            }
        }
        manager.replaceRecipes(recipes);
        server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(manager.getRecipes()));
    }

    private static final class SoulFire {
        final ServerPlayer owner;
        final ServerLevel level;
        final double x;
        final double y;
        final double z;
        int age;

        SoulFire(ServerPlayer owner, ServerLevel level, double x, double y, double z) {
            this.owner = owner;
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class Drain {
        final ServerPlayer player;
        final List<LivingEntity> targets;
        int age;

        Drain(ServerPlayer player, List<LivingEntity> targets) {
            this.player = player;
            this.targets = targets;
        }
    }
}
