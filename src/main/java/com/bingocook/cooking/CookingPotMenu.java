package com.bingocook.cooking;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Cooking pot container menu: nine ingredient slots (0-8) in a 3x3 grid, one
 * output slot (9) and the player inventory. The output slot rejects placement
 * by the player ({@code mayPlace = false}); cooking inserts through the block
 * entity tick only. The two data slots carry the cooking progress and the
 * total cooking time from the block entity's own {@link ContainerData}.
 */
public class CookingPotMenu extends AbstractContainerMenu {
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;

    private final Container container;
    private final ContainerData data;

    /**
     * Client-side constructor used by the registered {@code MenuType} when a
     * container is opened; slot contents arrive via the sync packets.
     */
    public CookingPotMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CookingPotBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(CookingPotBlockEntity.DATA_SIZE));
    }

    /**
     * Server-side constructor; the block entity is its own {@code Container}
     * and {@code ContainerData}.
     */
    public CookingPotMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(CookingRegistries.COOKING_POT_MENU.get(), containerId);
        checkContainerSize(container, CookingPotBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, CookingPotBlockEntity.DATA_SIZE);
        this.container = container;
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
        this.addSlot(new Slot(container, CookingPotBlockEntity.OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    /**
     * @return cooking progress as a fraction of the total cooking time, used
     *         by the screen to crop the progress arrow.
     */
    public float getCookProgress() {
        int progress = this.data.get(CookingPotBlockEntity.DATA_PROGRESS);
        int cookingTime = this.data.get(CookingPotBlockEntity.DATA_COOKING_TIME);
        return cookingTime != 0 && progress != 0 ? Mth.clamp((float) progress / cookingTime, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == CookingPotBlockEntity.OUTPUT_SLOT) {
                // Output -> player inventory, hotbar last.
                if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= INV_SLOT_START && slotIndex < USE_ROW_SLOT_END) {
                // Player inventory -> ingredient grid first, then elsewhere.
                if (!this.moveItemStackTo(stack, 0, CookingPotBlockEntity.OUTPUT_SLOT, false)) {
                    if (slotIndex < INV_SLOT_END) {
                        if (!this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return clicked;
    }
}
