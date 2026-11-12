package dev.tinyrelics;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TinyRelics.MODID)
public final class GameEvents {
    private static final String KEPT = "tinyrelics_kept_inventory";
    private static final String BROKEN = "tinyrelics_charm_broken";

    private GameEvents() {
    }

    // ---- Charm of Keeping: one charm is used up on death, the inventory is put
    // ---- aside so nothing drops, and it is handed back on respawn.

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
            Network.sendCharmBroken(player);
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

    // ---- Settings reach every player who joins.

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Network.sendConfig(player);
        }
    }

    // ---- Bleeding, the blade's drain and recipe switches run every server tick.

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Abilities.tick(event.getServer());
    }

    // ---- Chaos Boomerang shield: mobs cannot hurt the player while it circles.

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (victim instanceof ServerPlayer player
                && attacker instanceof Mob
                && player.getPersistentData().getLong(ChaosBoomerangItem.SHIELD_UNTIL) > player.level().getGameTime()) {
            event.setCanceled(true);
            return;
        }
        if (attacker != null && isFriendlyFire(victim, attacker)) {
            event.setCanceled(true);
        }
    }

    // ---- Undead Staff: servants and their owner cannot hurt each other, servants
    // ---- leave nothing behind, kills charge the staff, skulls spare the blocks.

    private static boolean isFriendlyFire(LivingEntity victim, Entity attacker) {
        if (victim instanceof Player player && Minions.isOwnedBy(attacker, player)) {
            return true;
        }
        UUID owner = Minions.ownerOf(victim);
        return owner != null && (owner.equals(attacker.getUUID()) || owner.equals(Minions.ownerOf(attacker)));
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() instanceof Mob mob && !Minions.isMinion(mob)) {
            UndeadStaffItem.addKill(player);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (Minions.isMinion(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        if (Minions.isMinion(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        // A servant left over from before a restart has no master any more.
        if (!event.getLevel().isClientSide() && Minions.isOrphan(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (source != null && source.getPersistentData().getBoolean(UndeadStaffItem.STAFF_SKULL)
                && !RelicConfig.SERVER.staffSkullGrief) {
            event.getAffectedBlocks().clear();
        }
    }

    // ---- Thor's Hammer: no fall damage during and right after the strike from the sky.

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            long left = player.getPersistentData().getLong(Slam.NO_FALL_UNTIL) - player.level().getGameTime();
            if (left > 0 && left < 400) {
                event.setCanceled(true);
            }
        }
    }
}
