package com.bingocook.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A permanent attribute modifier granted by eating a seasoned dish. JSON format:
 * <pre>{@code
 * {"attribute": "minecraft:max_health", "amount": 1.0, "operation": "add_value"}
 * }</pre>
 *
 * <p>Stored in the {@code bingocook:permanent_attributes} data component and
 * applied through {@code AttributeMap.addPermanentModifier} when the dish is
 * eaten, so it persists in the player's NBT across death and respawn.
 */
public record AttributeModifierEntry(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    public static final Codec<AttributeModifierEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(AttributeModifierEntry::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeModifierEntry::amount),
            AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(AttributeModifierEntry::operation))
            .apply(instance, AttributeModifierEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributeModifierEntry> STREAM_CODEC = StreamCodec.composite(
            Attribute.STREAM_CODEC, AttributeModifierEntry::attribute,
            ByteBufCodecs.DOUBLE, AttributeModifierEntry::amount,
            AttributeModifier.Operation.STREAM_CODEC, AttributeModifierEntry::operation,
            AttributeModifierEntry::new);

    /**
     * Builds the modifier with a fresh unique ID, so every eaten dish stacks.
     */
    public AttributeModifier toModifier(Identifier id) {
        return new AttributeModifier(id, amount, operation);
    }

    @Override
    public String toString() {
        return attribute.unwrapKey().orElseThrow().identifier() + " " + (amount >= 0 ? "+" : "") + amount + " " + operation.getSerializedName();
    }
}
