package com.bingocook.cooking;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Cooking pot block entity: nine ingredient slots (0-8), one output slot (9)
 * and the cooking progress state machine.
 *
 * <p>Cooking rules (see the master plan): cooking only runs while all nine
 * ingredient slots are non-empty and a {@link CookingRecipe} matches; a lit
 * campfire directly below provides heat (no fuel slot, no water). Progress
 * advances once per tick when the recipe matches, heat is present and the
 * output slot can accept the result; it pauses with progress preserved when
 * heat is lost, and resets to zero when the input no longer matches or the
 * output slot is blocked (furnace-like). On completion each ingredient slot
 * is decremented by one and one dish is produced, with the recipe's
 * seasonings applied to the produced stack.
 *
 * <p>The block entity implements {@link ContainerData} itself (furnace-style)
 * so the M4 menu can read the progress directly off the block entity.
 */
public class CookingPotBlockEntity extends BaseContainerBlockEntity implements ContainerData {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int CONTAINER_SIZE = 10;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_COOKING_TIME = 1;
    public static final int DATA_SIZE = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int progress;
    private int cookingTime;
    private boolean recipeDirty = true;
    private @Nullable RecipeHolder<CookingRecipe> currentRecipe;
    /**
     * Cached seasoned result for the current recipe. Valid while
     * {@link #currentRecipe} is set and {@link #recipeDirty} is false; inputs
     * cannot change during a cooking run, so the result is deterministic and
     * only needs to be prepared once per recipe (re)selection.
     */
    private ItemStack cachedResult = ItemStack.EMPTY;

    public CookingPotBlockEntity(BlockPos pos, BlockState blockState) {
        super(CookingRegistries.COOKING_POT_BE.get(), pos, blockState);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
        this.recipeDirty = true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bingocook.cooking_pot");
    }

    /**
     * Opens the cooking pot menu; this block entity is both the
     * {@code Container} and the {@code ContainerData} the menu reads from.
     */
    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CookingPotMenu(containerId, inventory, this, this);
    }

    /**
     * The output slot must never receive items through container interactions
     * (hoppers, droppers, player menu clicks). Slot-level {@code mayPlace}
     * checks only cover the player, so this guard is required at the container
     * level. {@code /item replace} writes through {@link #setItem} and is not
     * affected, which the verification procedure relies on.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != OUTPUT_SLOT;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        this.recipeDirty = true;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        this.recipeDirty = true;
        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        this.recipeDirty = true;
        return super.removeItemNoUpdate(slot);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
        this.cookingTime = input.getIntOr("cookingTime", 0);
        this.recipeDirty = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
        output.putInt("cookingTime", this.cookingTime);
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case DATA_PROGRESS -> this.progress;
            case DATA_COOKING_TIME -> this.cookingTime;
            default -> 0;
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case DATA_PROGRESS -> this.progress = value;
            case DATA_COOKING_TIME -> this.cookingTime = value;
        }
    }

    @Override
    public int getCount() {
        return DATA_SIZE;
    }

    /**
     * Server-side ticker entry point (registered by {@link CookingPotBlock}).
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingPotBlockEntity blockEntity) {
        boolean full = true;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (blockEntity.items.get(i).isEmpty()) {
                full = false;
                break;
            }
        }
        if (!full) {
            if (blockEntity.progress != 0 || blockEntity.currentRecipe != null) {
                blockEntity.resetProgress();
            }
            updateLitState(level, pos, state, false);
            return;
        }

        NonNullList<ItemStack> ingredients = NonNullList.withSize(INPUT_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ingredients.set(i, blockEntity.items.get(i));
        }
        CookingRecipeInput input = new CookingRecipeInput(ingredients);
        if (blockEntity.recipeDirty || blockEntity.currentRecipe == null) {
            blockEntity.currentRecipe = findRecipe(level, input);
            blockEntity.recipeDirty = false;
            // Persist the reset even when no recipe matches (mirrors the
            // output-blocked reset below and resetProgress()).
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                blockEntity.setChanged();
            }
            if (blockEntity.currentRecipe == null) {
                updateLitState(level, pos, state, false);
                return;
            }
            blockEntity.cookingTime = blockEntity.currentRecipe.value().cookingTime();
            // Prepare the seasoned result once per recipe selection and cache
            // it: the inputs do not change until the craft, so the result is
            // deterministic for the whole run. The cache is invalidated via
            // recipeDirty (see setItem/removeItem/craft), avoiding per-tick
            // ItemStack allocation and the seasoning scan.
            blockEntity.cachedResult = prepareResult(blockEntity.currentRecipe.value(), blockEntity);
        }

        if (!hasLitCampfireBelow(level, pos)) {
            // No heat: pause, progress is preserved.
            updateLitState(level, pos, state, false);
            return;
        }

        // The seasoned result is what ends up in the output slot, so stacking
        // must be checked against it, not the raw recipe template (raw vs
        // seasoned components differ).
        if (!canAcceptOutput(blockEntity.items.get(OUTPUT_SLOT), blockEntity.cachedResult)) {
            if (blockEntity.progress != 0) {
                blockEntity.progress = 0;
                blockEntity.setChanged();
            }
            updateLitState(level, pos, state, false);
            return;
        }

        blockEntity.progress++;
        blockEntity.setChanged();
        updateLitState(level, pos, state, true);
        if (blockEntity.progress >= blockEntity.cookingTime) {
            craft(blockEntity, blockEntity.cachedResult);
            blockEntity.progress = 0;
            LOGGER.info("Crafted {} at {} - food: {}, permanent: {}, consumeEffects: {}",
                    blockEntity.cachedResult, pos, blockEntity.cachedResult.get(DataComponents.FOOD),
                    blockEntity.cachedResult.get(CookingComponents.PERMANENT_ATTRIBUTES),
                    blockEntity.cachedResult.has(DataComponents.CONSUMABLE)
                            ? blockEntity.cachedResult.get(DataComponents.CONSUMABLE).onConsumeEffects().size()
                            : 0);
        }
    }

    /**
     * Flips the {@code CookingPotBlock.LIT} state to match whether the pot is
     * actively cooking. No-op when the state already matches, so the block
     * update (and the client-side model swap) only fires on actual changes.
     */
    private static void updateLitState(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(CookingPotBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CookingPotBlock.LIT, lit), 3);
        }
    }

    /**
     * Creates the recipe result and applies the seasoning corrections: each
     * seasoning item kind defined by the recipe applies its modifier at most
     * once, regardless of how many stacks of it are present. Called once per
     * tick on a freshly created stack, so repeated calls do not accumulate.
     */
    private static ItemStack prepareResult(CookingRecipe recipe, CookingPotBlockEntity blockEntity) {
        ItemStack result = recipe.result().create();
        for (var entry : recipe.seasonings().entrySet()) {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (blockEntity.items.get(i).is(holder -> holder == entry.getKey())) {
                    entry.getValue().applyTo(result);
                    break;
                }
            }
        }
        return result;
    }

    private void resetProgress() {
        this.progress = 0;
        this.cookingTime = 0;
        this.currentRecipe = null;
        this.cachedResult = ItemStack.EMPTY;
        this.setChanged();
    }

    /**
     * @return the first matching recipe in {@code RecipeMap.byType} iteration
     *         order (data pack load order, see {@link CookingRecipe}).
     */
    private static @Nullable RecipeHolder<CookingRecipe> findRecipe(Level level, CookingRecipeInput input) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        for (RecipeHolder<CookingRecipe> holder
                : serverLevel.getServer().getRecipeManager().recipeMap().byType(CookingRecipe.TYPE)) {
            if (holder.value().matches(input, level)) {
                return holder;
            }
        }
        return null;
    }

    /**
     * @return true if the block directly below is a lit campfire.
     */
    private static boolean hasLitCampfireBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
    }

    /**
     * @return true if the produced result can be merged into the current
     *         output slot (empty, or same item/components and still stackable).
     */
    private static boolean canAcceptOutput(ItemStack output, ItemStack result) {
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    /**
     * Produces one dish into the output slot, decrementing every ingredient
     * slot by one. The result has already been prepared (seasonings applied)
     * by {@link #prepareResult}, so it is the exact stack that was checked
     * against the output slot.
     */
    private static void craft(CookingPotBlockEntity blockEntity, ItemStack result) {
        ItemStack output = blockEntity.items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            blockEntity.setItem(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
        }
        for (int i = 0; i < INPUT_SLOTS; i++) {
            blockEntity.items.get(i).shrink(1);
        }
        // The inputs changed (one per slot consumed): invalidate the cached
        // recipe/result so the next tick re-matches and re-prepares, e.g. a
        // seasoning item kind may have run out.
        blockEntity.recipeDirty = true;
        blockEntity.setChanged();
    }
}
