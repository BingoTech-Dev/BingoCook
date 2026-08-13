package com.bingocook.cooking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Cooking pot recipe: element requirements over the nine input slots.
 *
 * <p>JSON format ({@code data/<namespace>/recipes/<name>.json}):
 * <pre>{@code
 * {
 *   "type": "bingocook:cooking",
 *   "allowed": ["bingocook:vegetable", "bingocook:fruit", "bingocook:seasoning"],
 *   "requirements": {"bingocook:vegetable": {"min": 1}, "bingocook:fruit": {"min": 1}},
 *   "cookingTime": 600,
 *   "result": {"id": "bingocook:vegetable_fruit_stew", "count": 1},
 *   "seasonings": {
 *     "minecraft:sugar": {"nutrition": 1, "saturation": 1}
 *   },
 *   "enabled": true
 * }
 * }</pre>
 *
 * <p>Matching rules: all nine slots must be non-empty; the per-element totals
 * (summed over the nine slots through {@link CookingData#elementsOf}) of
 * elements NOT in {@code allowed} must be zero - element types added by data
 * packs are thereby automatically excluded; the totals of {@code allowed}
 * elements must satisfy {@link Requirement}. A missing {@code allowed} field
 * means every loaded element is allowed.
 *
 * <p>{@code "enabled": false} (default true) makes {@link #matches} always
 * return false. Overwriting a mod-shipped recipe file at the same path with an
 * enabled:false copy is the supported way to "delete" it - the recipe remains
 * in the manager and is shown as disabled by {@code /bingocook elements
 * recipe}. When several recipes match the same input, the caller takes the
 * first one in {@code RecipeManager.recipeMap().byType(...)} iteration order
 * (data pack load order).
 */
public final class CookingRecipe implements Recipe<CookingRecipeInput> {
    public static final RecipeType<CookingRecipe> TYPE = RecipeType.simple(Identifier.fromNamespaceAndPath("bingocook", "cooking"));

    @SuppressWarnings("null")
    public static final MapCodec<CookingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            Codec.list(Identifier.CODEC).optionalFieldOf("allowed").forGetter(recipe -> Optional.ofNullable(recipe.allowed)),
            Codec.unboundedMap(Identifier.CODEC, Requirement.CODEC).optionalFieldOf("requirements", Map.of()).forGetter(CookingRecipe::requirements),
            Codec.INT.optionalFieldOf("cookingTime", 200).forGetter(CookingRecipe::cookingTime),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CookingRecipe::result),
            Codec.unboundedMap(Item.CODEC, SeasoningModifier.CODEC).optionalFieldOf("seasonings", Map.of()).forGetter(CookingRecipe::seasonings),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CookingRecipe::enabled))
            .apply(instance, (commonInfo, allowed, requirements, cookingTime, result, seasonings, enabled) ->
                    new CookingRecipe(commonInfo, allowed.orElse(null), requirements, cookingTime, result, seasonings, enabled)));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, Requirement>> REQUIREMENTS_STREAM_CODEC = StreamCodec.of(
            (buffer, map) -> {
                buffer.writeVarInt(map.size());
                map.forEach((element, requirement) -> {
                    Identifier.STREAM_CODEC.encode(buffer, element);
                    Requirement.STREAM_CODEC.encode(buffer, requirement);
                });
            },
            buffer -> {
                int size = buffer.readVarInt();
                Map<Identifier, Requirement> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(Identifier.STREAM_CODEC.decode(buffer), Requirement.STREAM_CODEC.decode(buffer));
                }
                return map;
            });

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Holder<Item>, SeasoningModifier>> SEASONINGS_STREAM_CODEC = StreamCodec.of(
            (buffer, map) -> {
                buffer.writeVarInt(map.size());
                map.forEach((item, modifier) -> {
                    Item.STREAM_CODEC.encode(buffer, item);
                    SeasoningModifier.STREAM_CODEC.encode(buffer, modifier);
                });
            },
            buffer -> {
                int size = buffer.readVarInt();
                Map<Holder<Item>, SeasoningModifier> map = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    map.put(Item.STREAM_CODEC.decode(buffer), SeasoningModifier.STREAM_CODEC.decode(buffer));
                }
                return map;
            });

    @SuppressWarnings("null")
    public static final StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list())), recipe -> Optional.ofNullable(recipe.allowed),
            REQUIREMENTS_STREAM_CODEC, CookingRecipe::requirements,
            ByteBufCodecs.VAR_INT, CookingRecipe::cookingTime,
            ItemStackTemplate.STREAM_CODEC, CookingRecipe::result,
            SEASONINGS_STREAM_CODEC, CookingRecipe::seasonings,
            ByteBufCodecs.BOOL, CookingRecipe::enabled,
            (commonInfo, allowed, requirements, cookingTime, result, seasonings, enabled) ->
                    new CookingRecipe(commonInfo, allowed.orElse(null), requirements, cookingTime, result, seasonings, enabled));

    public static final RecipeSerializer<CookingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private final Recipe.CommonInfo commonInfo;
    private final @Nullable List<Identifier> allowed;
    private final Map<Identifier, Requirement> requirements;
    private final int cookingTime;
    private final ItemStackTemplate result;
    private final Map<Holder<Item>, SeasoningModifier> seasonings;
    private final boolean enabled;

    public CookingRecipe(
            Recipe.CommonInfo commonInfo,
            @Nullable List<Identifier> allowed,
            Map<Identifier, Requirement> requirements,
            int cookingTime,
            ItemStackTemplate result,
            Map<Holder<Item>, SeasoningModifier> seasonings,
            boolean enabled) {
        this.commonInfo = commonInfo;
        this.allowed = allowed;
        this.requirements = requirements;
        this.cookingTime = cookingTime;
        this.result = result;
        this.seasonings = seasonings;
        this.enabled = enabled;
    }

    @Override
    public boolean matches(CookingRecipeInput input, Level level) {
        if (!enabled) {
            return false;
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).isEmpty()) {
                return false;
            }
        }

        Map<Identifier, Integer> totals = new HashMap<>();
        for (int i = 0; i < input.size(); i++) {
            CookingData.elementsOf(input.getItem(i)).forEach((element, amount) -> totals.merge(element, amount, (t, u) -> Integer.sum(t, u)));
        }

        // Elements not in `allowed` must total 0 - data-pack-added element types
        // are automatically excluded. A null `allowed` means all elements pass.
        for (Map.Entry<Identifier, Integer> entry : totals.entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            if (allowed != null && !allowed.contains(entry.getKey())) {
                return false;
            }
        }

        for (Map.Entry<Identifier, Requirement> entry : requirements.entrySet()) {
            if (!entry.getValue().test(totals.getOrDefault(entry.getKey(), 0))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CookingRecipeInput input) {
        return result.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return commonInfo.showNotification();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<CookingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<CookingRecipe> getType() {
        return TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    public @Nullable List<Identifier> allowed() {
        return allowed;
    }

    public Map<Identifier, Requirement> requirements() {
        return requirements;
    }

    public int cookingTime() {
        return cookingTime;
    }

    public ItemStackTemplate result() {
        return result;
    }

    public Map<Holder<Item>, SeasoningModifier> seasonings() {
        return seasonings;
    }

    public boolean enabled() {
        return enabled;
    }
}
