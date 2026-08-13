package com.bingocook.cooking;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Recipe input wrapping the nine ingredient slots of the cooking pot.
 */
public record CookingRecipeInput(NonNullList<ItemStack> items) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
