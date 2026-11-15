package dev.tinyrelics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a shadow loom or a phantom spooler holds and does. Slots: 0 what goes in, 1 what comes out.
 * The loom weaves two enderman skins into one chaos cloth; the spooler winds one piece of phantom wing
 * into one phantom thread. Neither needs fuel, and both work only at night: by day they stand still
 * and keep what they have begun.
 */
public class ShadowMachineBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOTS = 2;
    public static final int SKINS_PER_CLOTH = 2;
    public static final int LOOM_TICKS = 200;
    public static final int SPOOL_TICKS = 160;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int progress;
    /** The material for this piece has left its slot and is on the machine. */
    private boolean loaded;
    private boolean night;
    /** What the open window is told: progress, whether it is night, whether a piece is on the machine. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? ShadowMachineBlockEntity.this.progress
                    : index == 1 ? (ShadowMachineBlockEntity.this.night ? 1 : 0) : (ShadowMachineBlockEntity.this.loaded ? 1 : 0);
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                ShadowMachineBlockEntity.this.progress = value;
            } else if (index == 1) {
                ShadowMachineBlockEntity.this.night = value != 0;
            } else {
                ShadowMachineBlockEntity.this.loaded = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ShadowMachineBlockEntity(BlockPos pos, BlockState state) {
        super(TinyRelics.SHADOW_MACHINE_ENTITY.get(), pos, state);
    }

    public boolean isSpooler() {
        return getBlockState().is(TinyRelics.PHANTOM_SPOOLER.get());
    }

    /** What may lie in the first slot of a loom or of a spooler. */
    public static boolean isInput(ItemStack stack, boolean spooler) {
        return stack.is(spooler ? TinyRelics.PHANTOM_WING.get() : TinyRelics.ENDER_SKIN.get());
    }

    /** Material already taken for a piece that is not finished: a broken machine gives it back. */
    public ItemStack onTheMachine() {
        if (!this.loaded) {
            return ItemStack.EMPTY;
        }
        return isSpooler() ? new ItemStack(TinyRelics.PHANTOM_WING.get()) : new ItemStack(TinyRelics.ENDER_SKIN.get(), SKINS_PER_CLOTH);
    }

    private static boolean fits(ItemStack slot, Item item) {
        return slot.isEmpty() || (slot.is(item) && slot.getCount() < slot.getMaxStackSize());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShadowMachineBlockEntity machine) {
        boolean spooler = state.is(TinyRelics.PHANTOM_SPOOLER.get());
        Item result = spooler ? TinyRelics.PHANTOM_THREAD.get() : TinyRelics.CHAOS_CLOTH.get();
        int needed = spooler ? 1 : SKINS_PER_CLOTH;
        ItemStack input = machine.items.get(SLOT_INPUT);
        boolean changed = false;
        boolean working = false;
        boolean night = level.isNight();
        if (night != machine.night) {
            machine.night = night;
            changed = true;
        }
        boolean ready = machine.loaded || (isInput(input, spooler) && input.getCount() >= needed);
        if (night && ready && fits(machine.items.get(SLOT_OUTPUT), result)) {
            if (!machine.loaded) {
                // a new piece: the material leaves its slot at once and goes onto the machine
                input.shrink(needed);
                machine.loaded = true;
                machine.progress = 0;
            }
            working = true;
            machine.progress++;
            changed = true;
            if (machine.progress >= (spooler ? SPOOL_TICKS : LOOM_TICKS)) {
                machine.progress = 0;
                machine.loaded = false;
                ItemStack there = machine.items.get(SLOT_OUTPUT);
                if (there.isEmpty()) {
                    machine.items.set(SLOT_OUTPUT, new ItemStack(result));
                } else {
                    there.grow(1);
                }
                level.playSound(null, pos, spooler ? SoundEvents.WOOL_PLACE : SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }
        BlockState next = state.setValue(ShadowMachineBlock.LIT, working);
        if (next != state) {
            level.setBlock(pos, next, 3);
        }
        if (changed) {
            machine.setChanged();
        }
    }

    // ---- Saving.

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("Progress", this.progress);
        tag.putBoolean("Loaded", this.loaded);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.progress = tag.getInt("Progress");
        this.loaded = tag.getBoolean("Loaded");
    }

    // ---- The window.

    @Override
    public Component getDisplayName() {
        return Component.translatable(isSpooler() ? "container.tinyrelics.phantom_spooler" : "container.tinyrelics.shadow_loom");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShadowMachineMenu(id, inventory, this, this.data, isSpooler());
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

    /** Hoppers may only put in what the machine works with. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT && isInput(stack, isSpooler());
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
