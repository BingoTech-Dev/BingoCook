package com.bingocook.cooking;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * Recipe-level seasoning modifier: the corrections applied to the produced dish
 * when a defined seasoning item is among the nine input slots. JSON format:
 * <pre>{@code
 * {
 *   "nutrition": 1,
 *   "saturation": 1,
 *   "effects": [{"effect": "minecraft:instant_health", "duration": 1, "amplifier": 0, "probability": 1.0}],
 *   "permanentAttributes": [{"attribute": "minecraft:max_health", "amount": 1.0, "operation": "add_value"}]
 * }
 * }</pre>
 *
 * <p>All fields are optional; a value of 0 for {@code nutrition}/{@code saturation}
 * leaves the food values unchanged. Each distinct seasoning item type in the
 * input applies its modifier at most once per craft, regardless of how many
 * stacks of it are present.
 */
public record SeasoningModifier(
        int nutrition,
        int saturation,
        List<SeasoningEffect> effects,
        List<AttributeModifierEntry> permanentAttributes) {

    public static final Codec<SeasoningModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("nutrition", 0).forGetter(SeasoningModifier::nutrition),
            Codec.INT.optionalFieldOf("saturation", 0).forGetter(SeasoningModifier::saturation),
            SeasoningEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SeasoningModifier::effects),
            AttributeModifierEntry.CODEC.listOf().optionalFieldOf("permanentAttributes", List.of()).forGetter(SeasoningModifier::permanentAttributes))
            .apply(instance, SeasoningModifier::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeasoningModifier> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SeasoningModifier::nutrition,
            ByteBufCodecs.VAR_INT, SeasoningModifier::saturation,
            SeasoningEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), SeasoningModifier::effects,
            AttributeModifierEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), SeasoningModifier::permanentAttributes,
            SeasoningModifier::new);

    /**
     * Applies this modifier to a produced dish stack: rebuilds the
     * {@code minecraft:food} component, appends consume effects to the
     * {@code minecraft:consumable} component and appends permanent attribute
     * entries to {@code bingocook:permanent_attributes}.
     */
    public void applyTo(ItemStack stack) {
        if (nutrition != 0 || saturation != 0) {
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null) {
                stack.set(DataComponents.FOOD, new FoodProperties(
                        Math.max(0, food.nutrition() + nutrition),
                        food.saturation() + saturation,
                        food.canAlwaysEat()));
            }
        }

        if (!effects.isEmpty()) {
            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            if (consumable != null) {
                Consumable.Builder builder = Consumable.builder()
                        .consumeSeconds(consumable.consumeSeconds())
                        .animation(consumable.animation())
                        .sound(consumable.sound())
                        .hasConsumeParticles(consumable.hasConsumeParticles());
                consumable.onConsumeEffects().forEach(builder::onConsume);
                for (SeasoningEffect effect : effects) {
                    builder.onConsume(new ApplyStatusEffectsConsumeEffect(effect.toMobEffectInstance(), effect.probability()));
                }
                stack.set(DataComponents.CONSUMABLE, builder.build());
            }
        }

        if (!permanentAttributes.isEmpty()) {
            List<AttributeModifierEntry> existing = stack.get(CookingComponents.PERMANENT_ATTRIBUTES);
            List<AttributeModifierEntry> merged = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            merged.addAll(permanentAttributes);
            stack.set(CookingComponents.PERMANENT_ATTRIBUTES, merged);
        }
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (nutrition != 0) {
            parts.add("nutrition " + (nutrition > 0 ? "+" : "") + nutrition);
        }
        if (saturation != 0) {
            parts.add("saturation " + (saturation > 0 ? "+" : "") + saturation);
        }
        if (!effects.isEmpty()) {
            parts.add("effects: " + effects);
        }
        if (!permanentAttributes.isEmpty()) {
            parts.add("permanent: " + permanentAttributes);
        }
        return parts.isEmpty() ? "(no changes)" : String.join(", ", parts);
    }
}
