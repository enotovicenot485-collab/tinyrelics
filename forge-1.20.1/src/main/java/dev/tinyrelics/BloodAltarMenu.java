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

/** The blood altar's window. Slot positions match the picture built by tools/altar.py. */
public class BloodAltarMenu extends AbstractContainerMenu {
    private static final int ALTAR_SLOTS = BloodAltarBlockEntity.SLOTS;
    private final Container altar;
    private final ContainerData data;

    /** On the client: an empty stand-in the server then fills. */
    public BloodAltarMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(ALTAR_SLOTS), new SimpleContainerData(2));
    }

    public BloodAltarMenu(int id, Inventory inventory, Container altar, ContainerData data) {
        super(TinyRelics.BLOOD_ALTAR_MENU.get(), id);
        checkContainerSize(altar, ALTAR_SLOTS);
        checkContainerDataCount(data, 2);
        this.altar = altar;
        this.data = data;
        addSlot(new Slot(altar, BloodAltarBlockEntity.SLOT_INPUT, 56, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BloodAltarBlockEntity.isBlood(stack);
            }
        });
        addSlot(new Slot(altar, BloodAltarBlockEntity.SLOT_WATER, 14, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BloodAltarBlockEntity.isWater(stack);
            }
        });
        addSlot(new Slot(altar, BloodAltarBlockEntity.SLOT_OUTPUT, 130, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(altar, BloodAltarBlockEntity.SLOT_BOTTLES, 14, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public int progress() {
        return this.data.get(0);
    }

    public int water() {
        return this.data.get(1);
    }

    /** Shift-click: out of the altar into the inventory, blood and water from the inventory into their slots. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int inventoryEnd = ALTAR_SLOTS + 27;
        int hotbarEnd = inventoryEnd + 9;
        boolean moved;
        if (index < ALTAR_SLOTS) {
            moved = moveItemStackTo(stack, ALTAR_SLOTS, hotbarEnd, true);
        } else if (BloodAltarBlockEntity.isBlood(stack)) {
            moved = moveItemStackTo(stack, 0, 1, false);
        } else if (BloodAltarBlockEntity.isWater(stack)) {
            moved = moveItemStackTo(stack, 1, 2, false);
        } else if (index < inventoryEnd) {
            moved = moveItemStackTo(stack, inventoryEnd, hotbarEnd, false);
        } else {
            moved = moveItemStackTo(stack, ALTAR_SLOTS, inventoryEnd, false);
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
        return this.altar.stillValid(player);
    }
}
