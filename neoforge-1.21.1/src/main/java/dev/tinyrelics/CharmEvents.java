package dev.tinyrelics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Charm of Keeping: on death one charm is used up, the inventory is put aside
 * so nothing drops, and it is handed back when the player respawns.
 */
@EventBusSubscriber(modid = TinyRelics.MODID)
public final class CharmEvents {
    private static final String KEPT = "tinyrelics_kept_inventory";
    private static final String BROKEN = "tinyrelics_charm_broken";

    private CharmEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Inventory inventory = player.getInventory();
        int slot = findCharm(inventory);
        if (slot < 0) {
            return;
        }
        inventory.getItem(slot).shrink(1);
        player.getPersistentData().put(KEPT, inventory.save(new ListTag()));
        inventory.clearContent();
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        CompoundTag oldData = event.getOriginal().getPersistentData();
        if (!oldData.contains(KEPT, Tag.TAG_LIST)) {
            return;
        }
        ListTag kept = oldData.getList(KEPT, Tag.TAG_COMPOUND);
        oldData.remove(KEPT);

        Player fresh = event.getEntity();
        fresh.getInventory().load(kept);
        fresh.getPersistentData().putBoolean(BROKEN, true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().getBoolean(BROKEN)) {
            player.getPersistentData().remove(BROKEN);
            player.inventoryMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(player, TinyRelics.CharmBroken.INSTANCE);
        }
    }

    private static int findCharm(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(TinyRelics.KEEPING_CHARM.get())) {
                return i;
            }
        }
        return -1;
    }
}
