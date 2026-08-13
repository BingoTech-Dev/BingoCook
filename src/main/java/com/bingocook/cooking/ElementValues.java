package com.bingocook.cooking;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Immutable "item -> elemental values" data map attachment.
 *
 * <p>Each element ID maps to an integer amount (0 = absent). Recipe matching sums
 * the values across the nine input slots, so an explicit 0 behaves the same as a
 * missing entry.
 *
 * <p>JSON format inside a data map entry:
 * <pre>{@code {"elements": {"bingocook:fruit": 2}}</pre>
 */
public record ElementValues(Map<Identifier, Integer> elements) {
    public static final ElementValues EMPTY = new ElementValues(Map.of());

    public static final Codec<ElementValues> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("elements").forGetter(ElementValues::elements))
            .apply(instance, ElementValues::new));
}
