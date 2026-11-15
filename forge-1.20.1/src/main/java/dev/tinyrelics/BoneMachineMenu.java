package dev.tinyrelics;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The window of a bone crusher or a soul press. Slot positions match the pictures built by tools/machines.py. */
public class BoneMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = BoneMachineBlockEntity.SLOTS;
    /** Where the machine's part of the picture ends and the player's slots begin, in GUI pixels. */
    public static final int CRUSHER_ROWS = 126;
    public static final int PRESS_ROWS = 103;
    private final Container machine;
    private final ContainerData data;
    public final boolean press;

    /** On the client: an empty stand-in the server then fills. */
    public BoneMachineMenu(int id, Inventory inventory, boolean press) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(3), press);
    }

    public BoneMachineMenu(int id, Inventory inventory, Container machine, ContainerData data, boolean press) {
        super(press ? TinyRelics.SOUL_PRESS_MENU.get() : TinyRelics.BONE_CRUSHER_MENU.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, 3);
        this.machine = machine;
        this.data = data;
        this.press = press;
        addSlot(new Slot(machine, BoneMachineBlockEntity.SLOT_INPUT, press ? 19 : 85, press ? 41 : 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BoneMachineBlockEntity.isInput(stack, press);
            }
        });
        addSlot(new Slot(machine, BoneMachineBlockEntity.SLOT_FUEL, press ? 17 : 21, press ? 69 : 78) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BoneMachineBlockEntity.isFuel(stack);
            }
        });
        addSlot(new Slot(machine, BoneMachineBlockEntity.SLOT_OUTPUT, press ? 141 : 85, press ? 51 : 101) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        int top = (press ? PRESS_ROWS : CRUSHER_ROWS) + 4;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, top + 58));
        }
        addDataSlots(data);
    }

    public int progress() {
        return this.data.get(0);
    }

    public int fuel() {
        return this.data.get(1);
    }

    public int mode() {
        return this.data.get(2);
    }

    /** Shift-click: out of the machine into the inventory; from the inventory into the slot the thing belongs to. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int inventoryEnd = MACHINE_SLOTS + 27;
        int hotbarEnd = inventoryEnd + 9;
        boolean moved;
        if (index < MACHINE_SLOTS) {
            moved = moveItemStackTo(stack, MACHINE_SLOTS, hotbarEnd, true);
        } else if (BoneMachineBlockEntity.isInput(stack, this.press)) {
            moved = moveItemStackTo(stack, 0, 1, false);
        } else if (BoneMachineBlockEntity.isFuel(stack)) {
            moved = moveItemStackTo(stack, 1, 2, false);
        } else if (index < inventoryEnd) {
            moved = moveItemStackTo(stack, inventoryEnd, hotbarEnd, false);
        } else {
            moved = moveItemStackTo(stack, MACHINE_SLOTS, inventoryEnd, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
    }
}
