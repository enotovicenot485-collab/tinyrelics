package dev.tinyrelics;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;

public class ThorHammerItem extends RelicWeaponItem {
    /** The strike never reaches further than this up and down. */
    private static final double MAX_HEIGHT = 24.0D;
    /** Damage of one lightning strike (20 = 10 hearts). */
    public static final float STRIKE_DAMAGE = 20.0F;
    private static final int MAX_TARGETS = 256;
    /** Safety cooldown while the thrown hammer is in the air. */
    private static final int FLIGHT_COOLDOWN = 200;

    public ThorHammerItem(Properties properties) {
        super(properties, 14.0D, -3.0D);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                ThrownRelic.launch(server, serverPlayer, stack, ThrownRelic.STRAIGHT);
                player.getCooldowns().addCooldown(this, FLIGHT_COOLDOWN);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
            } else {
                strikeAround(server, serverPlayer);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void strikeAround(ServerLevel server, ServerPlayer player) {
        RelicConfig config = RelicConfig.SERVER;
        double radius = config.hammerRadius;
        AABB area = player.getBoundingBox().inflate(radius, Math.min(radius, MAX_HEIGHT), radius);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area,
                e -> isTarget(e, player, config.hammerMonstersOnly));

        int count = 0;
        for (LivingEntity target : targets) {
            if (count++ >= MAX_TARGETS) {
                break;
            }
            shock(server, target, player);
        }

        // A ring of lightning runs along the ground from the wielder's feet.
        Network.sendShockwave(player, player.getX(), player.getY(), player.getZ(), 6, false);
    }

    /** A lightning bolt that is only for looks: it never hurts the wielder. */
    public static void lightning(ServerLevel server, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        if (RelicConfig.SERVER.hammerFire) {
            BlockPos pos = BlockPos.containing(x, y, z);
            if (server.getBlockState(pos).isAir() && BaseFireBlock.canBePlacedAt(server, pos, Direction.UP)) {
                server.setBlockAndUpdate(pos, BaseFireBlock.getState(server, pos));
            }
        }
    }

    /** Lightning on one target, credited to the wielder. */
    public static void shock(ServerLevel server, LivingEntity target, ServerPlayer wielder) {
        lightning(server, target.getX(), target.getY(), target.getZ());
        server.sendParticles(TinyRelics.THOR_FLASH.get(),
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        DamageSource source = new DamageSource(
                server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),
                wielder);
        target.hurt(source, STRIKE_DAMAGE);
        target.setSecondsOnFire(4);
    }

    public static void ignite(LivingEntity entity, int seconds) {
        entity.setSecondsOnFire(seconds);
    }

    public static boolean isTarget(LivingEntity entity, Player wielder, boolean monstersOnly) {
        if (entity == wielder || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        if (Minions.isOwnedBy(entity, wielder)) {
            return false;
        }
        if (monstersOnly) {
            return entity instanceof Enemy;
        }
        if (entity instanceof Player other && other.isCreative()) {
            return false;
        }
        // Do not fry the wielder's own pets.
        return !(entity instanceof TamableAnimal pet && pet.isOwnedBy(wielder));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living
                && (selected || living.getOffhandItem() == stack) && entity.tickCount % 3 == 0) {
            server.sendParticles(TinyRelics.THOR_SPARK.get(),
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    2, 0.35D, 0.4D, 0.35D, 0.02D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip2").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip3", Component.keybind("key.tinyrelics.super")).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip4").withStyle(ChatFormatting.DARK_GRAY));
    }
}
