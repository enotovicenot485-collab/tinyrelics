package dev.tinyrelics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The full armor of Thor (it has no helmet: chestplate, leggings, boots). Whoever hurts
 * the wearer is struck by lightning in return.
 */
public final class ThorSet {
    /** At most one answering bolt every half second, however many blows land. */
    private static final int PAUSE = 10;
    private static final String READY = "tinyrelics_thor_answer_ready";
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ThorSet() {
    }

    public static boolean hasFullSet(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            if (!(player.getItemBySlot(slot).getItem() instanceof ThorArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /** The wearer has just been hurt: answer the one who did it. */
    public static void strikeBack(ServerPlayer wearer, DamageSource source) {
        // Lightning is never answered, or two wearers would strike each other for ever.
        if (source.is(DamageTypes.LIGHTNING_BOLT) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == wearer || !attacker.isAlive() || !hasFullSet(wearer)) {
            return;
        }
        ServerLevel level = wearer.serverLevel();
        if (attacker.level() != level) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = wearer.getPersistentData();
        long wait = data.getLong(READY) - now;
        if (wait > 0 && wait <= PAUSE) {
            return;
        }
        data.putLong(READY, now + PAUSE);
        ThorHammerItem.shock(level, attacker, wearer);
    }
}
