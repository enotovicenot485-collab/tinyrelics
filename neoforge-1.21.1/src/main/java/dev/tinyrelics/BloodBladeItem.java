package dev.tinyrelics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
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

    public BloodBladeItem(Properties properties) {
        super(properties, 6.0D, -2.4D);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (player.getHealth() > MAX_HEALTH_TO_USE) {
                player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_health"), true);
                return InteractionResultHolder.fail(stack);
            }
            List<LivingEntity> enemies = server.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(DRAIN_RADIUS),
                    e -> e.isAlive() && e instanceof Enemy);
            if (enemies.isEmpty()) {
                player.displayClientMessage(Component.translatable("msg.tinyrelics.blade_no_enemies"), true);
                return InteractionResultHolder.fail(stack);
            }
            Abilities.startDrain(serverPlayer, enemies);
            player.getCooldowns().addCooldown(this, COOLDOWN);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker.getRandom().nextFloat() < BLEED_CHANCE) {
            Abilities.bleed(target, attacker);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.blood_blade.tooltip").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.tinyrelics.blood_blade.tooltip2").withStyle(ChatFormatting.RED));
    }
}
