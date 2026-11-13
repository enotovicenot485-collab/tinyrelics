package dev.tinyrelics;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = TinyRelics.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Abilities.tick(server);
            ChaosSet.tick(server);
            BloodSet.tick(server);
        }
    }

    // ---- Chaos Boomerang shield: mobs cannot hurt the player while it circles.

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
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

    // ---- Necromancer armor: with the full set on, whatever the wearer kills rises to serve.

    @SubscribeEvent
    public static void onNecroKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            NecroSet.onKill(killer, event.getEntity());
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
        Entity source = event.getExplosion().getExploder();
        if (source != null && source.getPersistentData().getBoolean(UndeadStaffItem.STAFF_SKULL)
                && !RelicConfig.SERVER.staffSkullGrief) {
            event.getAffectedBlocks().clear();
        }
    }

    // ---- Chaos Assassin armor: nothing can pick a vanished wearer as its target.

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player && ChaosSet.isVanished(player)) {
            event.setCanceled(true);
        }
    }

    // ---- Blood drops from mobs: one to three, clean from animals, dirty from monsters.

    @SubscribeEvent
    public static void onBloodDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        net.minecraft.world.entity.LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT) || Minions.isMinion(victim)) {
            return;
        }
        net.minecraft.world.item.Item blood = BloodSet.bloodOf(victim);
        if (blood == null) {
            return;
        }
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(level, victim.getX(), victim.getY() + 0.3D, victim.getZ(),
                new net.minecraft.world.item.ItemStack(blood, 1 + level.random.nextInt(3))));
    }

    // ---- Blood Master armor: blood hearts take the damage first, animals refill them, death empties them.

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBloodDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setAmount(BloodSet.absorb(player, event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onBloodKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            BloodSet.onKill(killer, event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            BloodSet.onDeath(dead);
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
