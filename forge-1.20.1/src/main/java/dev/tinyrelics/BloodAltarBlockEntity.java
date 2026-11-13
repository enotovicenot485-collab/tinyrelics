package dev.tinyrelics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a blood altar holds and does. Slots: 0 blood in, 1 water bottles in, 2 result,
 * 3 emptied bottles. Thirty clean drops make an ingot; thirty dirty drops and four
 * bottles of water make thirty clean drops.
 */
public class BloodAltarBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_WATER = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_BOTTLES = 3;
    public static final int SLOTS = 4;
    /** Drops used by one job. */
    public static final int BATCH = 30;
    /** Bottles of water one washing takes, and how many the altar can hold. */
    public static final int WATER_PER_WASH = 4;
    public static final int MAX_WATER = 8;
    /** A job takes five seconds. */
    public static final int WORK_TICKS = 100;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int progress;
    private int water;
    /** What the open window is told: progress and water. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? BloodAltarBlockEntity.this.progress : BloodAltarBlockEntity.this.water;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                BloodAltarBlockEntity.this.progress = value;
            } else {
                BloodAltarBlockEntity.this.water = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BloodAltarBlockEntity(BlockPos pos, BlockState state) {
        super(TinyRelics.BLOOD_ALTAR_ENTITY.get(), pos, state);
    }

    public static boolean isBlood(ItemStack stack) {
        return stack.is(TinyRelics.BLOOD_DROP_ITEM.get()) || stack.is(TinyRelics.DIRTY_BLOOD_ITEM.get());
    }

    public static boolean isWater(ItemStack stack) {
        return stack.is(Items.POTION) && PotionUtils.getPotion(stack) == Potions.WATER;
    }

    // ---- Work.

    /** 0 = nothing to do, 1 = clean blood into an ingot, 2 = wash dirty blood. */
    private int job() {
        ItemStack input = this.items.get(SLOT_INPUT);
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (input.getCount() < BATCH) {
            return 0;
        }
        if (input.is(TinyRelics.BLOOD_DROP_ITEM.get())) {
            return fits(output, TinyRelics.BLOOD_INGOT.get(), 1) ? 1 : 0;
        }
        if (input.is(TinyRelics.DIRTY_BLOOD_ITEM.get()) && this.water >= WATER_PER_WASH) {
            return fits(output, TinyRelics.BLOOD_DROP_ITEM.get(), BATCH) ? 2 : 0;
        }
        return 0;
    }

    private static boolean fits(ItemStack slot, Item item, int count) {
        return slot.isEmpty() || (slot.is(item) && slot.getCount() + count <= slot.getMaxStackSize());
    }

    private void give(int slot, Item item, int count) {
        ItemStack there = this.items.get(slot);
        if (there.isEmpty()) {
            this.items.set(slot, new ItemStack(item, count));
        } else {
            there.grow(count);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BloodAltarBlockEntity altar) {
        boolean changed = false;
        // A bottle of water in its slot is poured in, and the empty bottle goes to the slot below.
        ItemStack bottle = altar.items.get(SLOT_WATER);
        if (altar.water < MAX_WATER && isWater(bottle) && fits(altar.items.get(SLOT_BOTTLES), Items.GLASS_BOTTLE, 1)) {
            bottle.shrink(1);
            altar.give(SLOT_BOTTLES, Items.GLASS_BOTTLE, 1);
            altar.water++;
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7F, 1.0F);
            changed = true;
        }
        int job = altar.job();
        if (job == 0) {
            if (altar.progress != 0) {
                altar.progress = 0;
                changed = true;
            }
        } else {
            altar.progress++;
            changed = true;
            if (altar.progress >= WORK_TICKS) {
                altar.progress = 0;
                altar.items.get(SLOT_INPUT).shrink(BATCH);
                if (job == 1) {
                    altar.give(SLOT_OUTPUT, TinyRelics.BLOOD_INGOT.get(), 1);
                } else {
                    altar.water -= WATER_PER_WASH;
                    altar.give(SLOT_OUTPUT, TinyRelics.BLOOD_DROP_ITEM.get(), BATCH);
                }
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 0.7F);
                if (level instanceof ServerLevel server) {
                    server.sendParticles(TinyRelics.BLOOD_DROP.get(), pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                            14, 0.25D, 0.1D, 0.25D, 0.05D);
                }
            }
        }
        if (changed) {
            altar.setChanged();
        }
    }

    // ---- Saving.

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
        tag.putInt("Water", this.water);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
        this.water = tag.getInt("Water");
    }

    // ---- The window.

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tinyrelics.blood_altar");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BloodAltarMenu(id, inventory, this, this.data);
    }

    // ---- The slots.

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = ContainerHelper.removeItem(this.items, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
    }

    /** Hoppers may only put blood and water in, each into its own slot. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT ? isBlood(stack) : slot == SLOT_WATER && isWater(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
