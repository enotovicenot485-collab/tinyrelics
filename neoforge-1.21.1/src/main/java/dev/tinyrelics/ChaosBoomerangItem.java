package dev.tinyrelics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ChaosBoomerangItem extends RelicWeaponItem {
    /** Player data key: game time until which the shield blocks mob hits. */
    public static final String SHIELD_UNTIL = "tinyrelics_shield_until";
    /** The shield lasts 20 seconds. */
    public static final int SHIELD_TICKS = 400;
    /** After the shield is over: 30 seconds. Kept apart from the item's own cooldown, so throws are not held up by it. */
    public static final int SHIELD_COOLDOWN = 600;
    /** Player data key: game time from which the shield can be called again. */
    public static final String SHIELD_READY = "tinyrelics_shield_ready";
    /** Cooldown after either ability: 10 seconds. */
    public static final int COOLDOWN = 200;
    /** After a throw, once the boomerang is back in the hand: half a second. It is the assassin's main weapon. */
    public static final int THROW_COOLDOWN = 10;
    /** While the boomerang is in the air it can not be thrown again: this cooldown lasts until it comes back. */
    private static final int FLIGHT_COOLDOWN = 400;
    /** Damage of a thrown hit, the same as a melee hit. */
    public static final float THROW_DAMAGE = 8.0F;

    public ChaosBoomerangItem(Properties properties) {
        super(properties, 8.0D, -2.4D);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                long now = server.getGameTime();
                long ready = serverPlayer.getPersistentData().getLong(SHIELD_READY);
                if (now < ready) {
                    serverPlayer.displayClientMessage(Component.translatable("message.tinyrelics.shield_wait", (ready - now + 19) / 20), true);
                    return InteractionResultHolder.fail(stack);
                }
                ThrownRelic.launch(server, serverPlayer, stack, ThrownRelic.ORBIT);
                serverPlayer.getPersistentData().putLong(SHIELD_UNTIL, now + SHIELD_TICKS);
                serverPlayer.getPersistentData().putLong(SHIELD_READY, now + SHIELD_TICKS + SHIELD_COOLDOWN);
                // only while it circles: the moment the shield is over it can be thrown again
                player.getCooldowns().addCooldown(this, SHIELD_TICKS);
                wear(stack, player);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            } else {
                ThrownRelic.launch(server, serverPlayer, stack, ThrownRelic.CHAIN);
                // Lasts until the boomerang comes back, then becomes half a second (ThrownRelic.finish).
                player.getCooldowns().addCooldown(this, FLIGHT_COOLDOWN);
                wear(stack, player);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.chaos_boomerang.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.tinyrelics.chaos_boomerang.tooltip2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
