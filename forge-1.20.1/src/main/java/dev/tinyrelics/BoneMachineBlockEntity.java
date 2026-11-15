package dev.tinyrelics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a bone crusher or a soul press holds and does. Slots: 0 what goes in, 1 gunpowder, 2 what comes out.
 * The crusher grinds a bone into bone powder, and soul sand into a soul - if the soul does not slip away.
 * The press squeezes eight heaps of powder into one bone ingot. One gunpowder pays for eight jobs.
 */
public class BoneMachineBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOTS = 3;
    /** Jobs one gunpowder pays for. */
    public static final int JOBS_PER_GUNPOWDER = 8;
    /** Heaps of powder in one ingot. */
    public static final int POWDER_PER_INGOT = 8;
    /** How often soul sand gives up its soul. */
    public static final float SOUL_CHANCE = 0.5F;
    public static final int CRUSH_TICKS = 60;
    public static final int PRESS_TICKS = 100;
    /** The plate of the press is down between these two moments of a job. */
    public static final int PRESS_DOWN_FROM = 55;
    public static final int PRESS_DOWN_TO = 78;
    /** What a job is: nothing, the ordinary one, soul sand that will give nothing, soul sand that will give a soul. */
    public static final int MODE_PLAIN = 0;
    public static final int MODE_SAND_EMPTY = 1;
    public static final int MODE_SAND_SOUL = 2;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int progress;
    private int fuel;
    private int mode;
    /** What the open window is told: progress, jobs of fuel left, the kind of job. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? BoneMachineBlockEntity.this.progress : index == 1 ? BoneMachineBlockEntity.this.fuel : BoneMachineBlockEntity.this.mode;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                BoneMachineBlockEntity.this.progress = value;
            } else if (index == 1) {
                BoneMachineBlockEntity.this.fuel = value;
            } else {
                BoneMachineBlockEntity.this.mode = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BoneMachineBlockEntity(BlockPos pos, BlockState state) {
        super(TinyRelics.BONE_MACHINE_ENTITY.get(), pos, state);
    }

    public boolean isPress() {
        return getBlockState().is(TinyRelics.SOUL_PRESS.get());
    }

    public static boolean isSoulSand(ItemStack stack) {
        return stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL);
    }

    /** What may lie in the first slot of a crusher or of a press. */
    public static boolean isInput(ItemStack stack, boolean press) {
        return press ? stack.is(TinyRelics.BONE_POWDER.get()) : stack.is(Items.BONE) || isSoulSand(stack);
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.GUNPOWDER);
    }

    // ---- Work.

    /** 0 = nothing to do, 1 = bones into powder or powder into an ingot, 2 = soul sand. */
    private int job(boolean press) {
        ItemStack input = this.items.get(SLOT_INPUT);
        ItemStack output = this.items.get(SLOT_OUTPUT);
        if (press) {
            return input.is(TinyRelics.BONE_POWDER.get()) && input.getCount() >= POWDER_PER_INGOT
                    && fits(output, TinyRelics.BONE_INGOT.get()) ? 1 : 0;
        }
        if (input.is(Items.BONE)) {
            return fits(output, TinyRelics.BONE_POWDER.get()) ? 1 : 0;
        }
        if (isSoulSand(input)) {
            return fits(output, TinyRelics.SOUL.get()) ? 2 : 0;
        }
        return 0;
    }

    private static boolean fits(ItemStack slot, Item item) {
        return slot.isEmpty() || (slot.is(item) && slot.getCount() < slot.getMaxStackSize());
    }

    private void give(Item item) {
        ItemStack there = this.items.get(SLOT_OUTPUT);
        if (there.isEmpty()) {
            this.items.set(SLOT_OUTPUT, new ItemStack(item));
        } else {
            there.grow(1);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoneMachineBlockEntity machine) {
        boolean press = state.is(TinyRelics.SOUL_PRESS.get());
        boolean changed = false;
        boolean working = false;
        int job = machine.job(press);
        if (job != 0) {
            ItemStack gunpowder = machine.items.get(SLOT_FUEL);
            if (machine.fuel <= 0 && isFuel(gunpowder)) {
                gunpowder.shrink(1);
                machine.fuel = JOBS_PER_GUNPOWDER;
                changed = true;
            }
            if (machine.fuel > 0) {
                working = true;
                boolean sand = job == 2;
                if (machine.progress == 0 || sand != (machine.mode != MODE_PLAIN)) {
                    // a new job: whether the sand gives up its soul is settled now, so the window can show it
                    machine.progress = 0;
                    machine.mode = !sand ? MODE_PLAIN : level.random.nextFloat() < SOUL_CHANCE ? MODE_SAND_SOUL : MODE_SAND_EMPTY;
                }
                machine.progress++;
                changed = true;
                if (press && machine.progress == PRESS_DOWN_FROM) {
                    level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35F, 0.6F);
                }
                if (machine.progress >= (press ? PRESS_TICKS : CRUSH_TICKS)) {
                    machine.finish(level, pos, press);
                }
            }
        }
        if (!working && machine.progress != 0) {
            machine.progress = 0;
            changed = true;
        }
        boolean pressed = press && working && machine.progress >= PRESS_DOWN_FROM && machine.progress < PRESS_DOWN_TO;
        BlockState next = state.setValue(BoneMachineBlock.LIT, working).setValue(BoneMachineBlock.PRESSED, pressed);
        if (next != state) {
            level.setBlock(pos, next, 3);
        }
        if (changed) {
            machine.setChanged();
        }
    }

    private void finish(Level level, BlockPos pos, boolean press) {
        this.progress = 0;
        this.fuel--;
        if (press) {
            this.items.get(SLOT_INPUT).shrink(POWDER_PER_INGOT);
            give(TinyRelics.BONE_INGOT.get());
            return;
        }
        this.items.get(SLOT_INPUT).shrink(1);
        if (this.mode == MODE_PLAIN) {
            give(TinyRelics.BONE_POWDER.get());
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4F, 0.7F);
        } else if (this.mode == MODE_SAND_SOUL) {
            give(TinyRelics.SOUL.get());
            level.playSound(null, pos, SoundEvents.VEX_CHARGE, SoundSource.BLOCKS, 0.6F, 1.4F);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 6, 0.2D, 0.1D, 0.2D, 0.02D);
            }
        } else {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.4F, 0.5F);
        }
    }

    // ---- Saving.

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
        tag.putInt("Fuel", this.fuel);
        tag.putInt("Mode", this.mode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
        this.fuel = tag.getInt("Fuel");
        this.mode = tag.getInt("Mode");
    }

    // ---- The window.

    @Override
    public Component getDisplayName() {
        return Component.translatable(isPress() ? "container.tinyrelics.soul_press" : "container.tinyrelics.bone_crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BoneMachineMenu(id, inventory, this, this.data, isPress());
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

    /** Hoppers may only put in what the machine works with, and gunpowder. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT ? isInput(stack, isPress()) : slot == SLOT_FUEL && isFuel(stack);
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
