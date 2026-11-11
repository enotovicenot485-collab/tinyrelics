package dev.tinyrelics;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class UndeadStaffItem extends RelicWeaponItem {
    /** Entity data key marking a skull fired from the staff. */
    public static final String STAFF_SKULL = "tinyrelics_staff_skull";
    private static final String KILLS = "tinyrelics_staff_kills";
    private static final String SUMMON_READY = "tinyrelics_staff_summon_ready";
    /** Kept across death, the same place Forge itself copies on respawn. */
    private static final String PERSISTED = "PlayerPersisted";

    /** 1.5 seconds between skulls. */
    private static final int SKULL_COOLDOWN = 30;
    /** Two minutes between summons. */
    private static final int SUMMON_COOLDOWN = 2400;

    public UndeadStaffItem(Properties properties) {
        super(properties, 5.0D, -2.4D);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                CompoundTag data = data(serverPlayer);
                long wait = data.getLong(SUMMON_READY) - server.getGameTime();
                // A wait longer than the cooldown means the world clock was changed: ignore it.
                if (wait > 0 && wait <= SUMMON_COOLDOWN) {
                    player.displayClientMessage(Component.translatable("msg.tinyrelics.staff_summon_wait", (wait + 19) / 20), true);
                    return InteractionResultHolder.fail(stack);
                }
                Minions.summon(server, serverPlayer);
                data.putLong(SUMMON_READY, server.getGameTime() + SUMMON_COOLDOWN);
                server.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 0.8F);
            } else {
                shootSkull(server, serverPlayer);
                player.getCooldowns().addCooldown(this, SKULL_COOLDOWN);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void shootSkull(ServerLevel server, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        WitherSkull skull = new WitherSkull(server, player, look.x, look.y, look.z);
        skull.setPos(player.getX() + look.x, player.getEyeY() - 0.2D + look.y, player.getZ() + look.z);
        skull.getPersistentData().putBoolean(STAFF_SKULL, true);
        server.addFreshEntity(skull);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    /** One more soul for a player who carries the staff. */
    public static void addKill(ServerPlayer player) {
        if (!player.getInventory().hasAnyMatching(stack -> stack.getItem() instanceof UndeadStaffItem)) {
            return;
        }
        int needed = RelicConfig.SERVER.staffKills;
        CompoundTag data = data(player);
        int kills = Math.min(needed, data.getInt(KILLS) + 1);
        data.putInt(KILLS, kills);
        player.displayClientMessage(kills >= needed
                ? Component.translatable("msg.tinyrelics.staff_ready", Component.keybind("key.tinyrelics.super")).withStyle(ChatFormatting.AQUA)
                : Component.translatable("msg.tinyrelics.staff_souls", kills, needed), true);
    }

    /** The super ability key was pressed. */
    public static void trySuper(ServerPlayer player) {
        boolean holding = player.getMainHandItem().getItem() instanceof UndeadStaffItem
                || player.getOffhandItem().getItem() instanceof UndeadStaffItem;
        if (!holding) {
            return;
        }
        int needed = RelicConfig.SERVER.staffKills;
        CompoundTag data = data(player);
        int kills = data.getInt(KILLS);
        if (kills < needed) {
            player.displayClientMessage(Component.translatable("msg.tinyrelics.staff_souls", kills, needed), true);
            return;
        }
        data.putInt(KILLS, 0);
        Abilities.startSoulFire(player);
    }

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(PERSISTED, Tag.TAG_COMPOUND)) {
            root.put(PERSISTED, new CompoundTag());
        }
        return root.getCompound(PERSISTED);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.undead_staff.tooltip").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.undead_staff.tooltip2").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.undead_staff.tooltip3", Component.keybind("key.tinyrelics.super")).withStyle(ChatFormatting.DARK_AQUA));
    }
}
