package dev.tinyrelics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BloodBladeItem extends RelicWeaponItem {
    /** The ability only works at one heart (2 health points) or less. */
    private static final float MAX_HEALTH_TO_USE = 2.0F;
    private static final double DRAIN_RADIUS = 8.0D;
    /** Two minutes. */
    private static final int COOLDOWN = 2400;
    private static final float BLEED_CHANCE = 0.2F;
    /** Player data keys: the game time from which each ability may be used again. */
    private static final String DRAIN_READY = "tinyrelics_blade_drain_ready";
    private static final String VORTEX_READY = "tinyrelics_blade_vortex_ready";

    public BloodBladeItem(Properties properties) {
        super(properties, 6.0D, -2.4D);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            long now = server.getGameTime();
            CompoundTag data = serverPlayer.getPersistentData();
            if (player.isShiftKeyDown()) {
                // Blood vortex: heal allies, keep two hearts.
                if (isWaiting(player, data, VORTEX_READY, now)) {
                    return InteractionResultHolder.fail(stack);
                }
                if (player.getHealth() <= Abilities.VORTEX_HEALTH_LEFT) {
                    player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_vortex_health"), true);
                    return InteractionResultHolder.fail(stack);
                }
                Abilities.startVortex(serverPlayer);
                data.putLong(VORTEX_READY, now + COOLDOWN);
                player.getCooldowns().addCooldown(this, 10);
                wear(stack, player);
                player.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.success(stack);
            }
            if (isWaiting(player, data, DRAIN_READY, now)) {
                return InteractionResultHolder.fail(stack);
            }
            if (player.getHealth() > MAX_HEALTH_TO_USE) {
                player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_health"), true);
                return InteractionResultHolder.fail(stack);
            }
            List<LivingEntity> enemies = server.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(DRAIN_RADIUS),
                    e -> e.isAlive() && Minions.isEnemyOf(e, player));
            if (enemies.isEmpty()) {
                player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_no_enemies"), true);
                return InteractionResultHolder.fail(stack);
            }
            Abilities.startDrain(serverPlayer, enemies);
            // Each ability has its own two minutes, so one never locks the other.
            data.putLong(DRAIN_READY, now + COOLDOWN);
            player.getCooldowns().addCooldown(this, 10);
            wear(stack, player);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Tells the player how long is left and returns true while the ability is still recharging. */
    private static boolean isWaiting(Player player, CompoundTag data, String key, long now) {
        long ready = data.getLong(key);
        // A timer far in the future comes from another world: ignore it.
        if (now >= ready || ready - now > COOLDOWN) {
            return false;
        }
        player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_wait", (ready - now + 19) / 20), true);
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            Abilities.hitSplash(target);
            if (attacker.getRandom().nextFloat() < BLEED_CHANCE) {
                Abilities.bleed(target, attacker);
            }
        }
        wear(stack, attacker);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.blood_blade.tooltip").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.tinyrelics.blood_blade.tooltip2").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.tinyrelics.blood_blade.tooltip3").withStyle(ChatFormatting.RED));
    }
}
