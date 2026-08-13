package com.bingocook.cooking;

import com.bingocook.BingoCook;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registries owned by the cooking system: the cooking pot block, block entity
 * type and item, example dish items, the custom cooking recipe type and its
 * serializer, and the custom data component types (see
 * {@link CookingComponents}).
 */
public final class CookingRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BingoCook.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BingoCook.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BingoCook.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BingoCook.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, BingoCook.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, BingoCook.MODID);

    // Cooking pot block (functionality in CookingPotBlockEntity; menu in M4).
    // registerBlock (not plain register) is required: it assigns the block id
    // to the properties, which 26.1 resolves lazily for loot tables etc.
    public static final DeferredBlock<CookingPotBlock> COOKING_POT = BLOCKS.registerBlock("cooking_pot",
            CookingPotBlock::new,
            properties -> properties.mapColor(MapColor.STONE).sound(SoundType.STONE).strength(1.5F, 6.0F));
    public static final DeferredItem<BlockItem> COOKING_POT_ITEM = ITEMS.registerSimpleBlockItem("cooking_pot", COOKING_POT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingPotBlockEntity>> COOKING_POT_BE =
            BLOCK_ENTITY_TYPES.register("cooking_pot", () -> new BlockEntityType<>(CookingPotBlockEntity::new, COOKING_POT.get()));

    // Cooking pot menu (client screen registered in BingoCookClient).
    public static final DeferredHolder<MenuType<?>, MenuType<CookingPotMenu>> COOKING_POT_MENU =
            MENUS.register("cooking_pot", () -> new MenuType<>(CookingPotMenu::new, FeatureFlags.DEFAULT_FLAGS));

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
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CookingComponents.register(modEventBus);
    }
}
