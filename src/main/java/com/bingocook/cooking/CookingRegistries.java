package com.bingocook.cooking;

import com.bingocook.BingoCook;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registries owned by the cooking system: example dish items, the custom
 * cooking recipe type and its serializer, and the custom data component types
 * (see {@link CookingComponents}).
 */
public final class CookingRegistries {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BingoCook.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BingoCook.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BingoCook.MODID);

    // Example dishes - the base food values are corrected by the recipe's
    // seasonings when produced.
    public static final DeferredItem<Item> VEGETABLE_FRUIT_STEW = ITEMS.registerSimpleItem("vegetable_fruit_stew",
            properties -> properties.food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> MEAT_VEGETABLE_STEW = ITEMS.registerSimpleItem("meat_vegetable_stew",
            properties -> properties.food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0F).build()));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingRecipe>> COOKING_TYPE = RECIPE_TYPES.register("cooking", () -> CookingRecipe.TYPE);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CookingRecipe>> COOKING_SERIALIZER = RECIPE_SERIALIZERS.register("cooking", () -> CookingRecipe.SERIALIZER);

    private CookingRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CookingComponents.register(modEventBus);
    }
}
