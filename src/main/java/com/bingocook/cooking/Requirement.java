package com.bingocook.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Inclusive min/max bound on the total value of one element across the nine
 * input slots. JSON format:
 * <pre>{@code {"min": 1}}</pre>
 * or
 * <pre>{@code {"min": 2, "max": 5}}</pre>
 */
public record Requirement(int min, int max) {
    public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min", 0).forGetter(Requirement::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(Requirement::max))
            .apply(instance, Requirement::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Requirement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Requirement::min,
            ByteBufCodecs.VAR_INT, Requirement::max,
            Requirement::new);

    public Requirement {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("Invalid requirement range [" + min + ".." + max + "]");
        }
    }

    public boolean test(int total) {
        return total >= min && total <= max;
    }

    @Override
    public String toString() {
        return "[" + min + ".." + (max == Integer.MAX_VALUE ? "\u221e" : max) + "]";
    }
}
