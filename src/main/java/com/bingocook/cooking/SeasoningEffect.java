package com.bingocook.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * One status-effect entry of a {@link SeasoningModifier}, applied to the dish
 * when it is consumed. JSON format:
 * <pre>{@code
 * {"effect": "minecraft:instant_health", "duration": 1, "amplifier": 0, "probability": 1.0}
 * }</pre>
 *
 * <p>Note: instant effects such as {@code minecraft:instant_health} are applied
 * immediately on eat with vanilla granularity (4 health points per amplifier
 * level).
 */
public record SeasoningEffect(Holder<MobEffect> effect, int duration, int amplifier, float probability) {
    public static final Codec<SeasoningEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(SeasoningEffect::effect),
            Codec.INT.optionalFieldOf("duration", 1).forGetter(SeasoningEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(SeasoningEffect::amplifier),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(SeasoningEffect::probability))
            .apply(instance, SeasoningEffect::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeasoningEffect> STREAM_CODEC = StreamCodec.composite(
            MobEffect.STREAM_CODEC, SeasoningEffect::effect,
            ByteBufCodecs.VAR_INT, SeasoningEffect::duration,
            ByteBufCodecs.VAR_INT, SeasoningEffect::amplifier,
            ByteBufCodecs.FLOAT, SeasoningEffect::probability,
            SeasoningEffect::new);

    public MobEffectInstance toMobEffectInstance() {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    @Override
    public String toString() {
        return effect.unwrapKey().orElseThrow().identifier() + " d=" + duration + " amp=" + amplifier + " p=" + probability;
    }
}
