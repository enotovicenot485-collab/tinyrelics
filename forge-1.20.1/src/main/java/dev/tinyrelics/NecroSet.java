package dev.tinyrelics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * The bonus of the full necromancer armor: whatever its wearer kills rises on the
 * spot as a zombie or a skeleton that fights for the wearer.
 */
public final class NecroSet {
    /** At most this many risen servants per wearer at one time. */
    private static final int MAX_RAISED = 5;
    /** They last 30 seconds. */
    private static final int LIFETIME = 600;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private NecroSet() {
    }

    public static boolean hasFullSet(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            if (!(player.getItemBySlot(slot).getItem() instanceof NecroArmorItem)) {
                return false;
            }
        }
        return true;
    }

    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (victim == killer || Minions.isMinion(victim) || !(victim instanceof Mob || victim instanceof Player)) {
            return;
        }
        if (!hasFullSet(killer) || Minions.countRaised(killer.getUUID()) >= MAX_RAISED) {
            return;
        }
        Minions.raiseAt(killer.serverLevel(), killer, victim.getX(), victim.getY(), victim.getZ(), LIFETIME);
    }
}
