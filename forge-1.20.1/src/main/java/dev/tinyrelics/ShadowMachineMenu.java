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

/** The window of a shadow loom or a phantom spooler. Slot positions match the pictures built by tools/shadow_machines.py. */
public class ShadowMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = ShadowMachineBlockEntity.SLOTS;
    /** Where the machine's part of the picture ends and the player's slots begin, in GUI pixels. */
    public static final int LOOM_ROWS = 115;
    public static final int SPOOLER_ROWS = 109;
    private final Container machine;
    private final ContainerData data;
    public final boolean spooler;

    /** On the client: an empty stand-in the server then fills. */
    public ShadowMachineMenu(int id, Inventory inventory, boolean spooler) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(3), spooler);
    }

    public ShadowMachineMenu(int id, Inventory inventory, Container machine, ContainerData data, boolean spooler) {
        super(spooler ? TinyRelics.PHANTOM_SPOOLER_MENU.get() : TinyRelics.SHADOW_LOOM_MENU.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, 3);
        this.machine = machine;
        this.data = data;
        this.spooler = spooler;
        addSlot(new Slot(machine, ShadowMachineBlockEntity.SLOT_INPUT, spooler ? 20 : 19, spooler ? 49 : 51) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ShadowMachineBlockEntity.isInput(stack, spooler);
            }
        });
        addSlot(new Slot(machine, ShadowMachineBlockEntity.SLOT_OUTPUT, 141, spooler ? 49 : 51) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        int top = (spooler ? SPOOLER_ROWS : LOOM_ROWS) + 4;
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

    public boolean night() {
        return this.data.get(1) != 0;
    }

    /** A piece is on the machine (even if the machine stands still because it is day). */
    public boolean loaded() {
        return this.data.get(2) != 0;
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
        } else if (ShadowMachineBlockEntity.isInput(stack, this.spooler)) {
            moved = moveItemStackTo(stack, 0, 1, false);
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
