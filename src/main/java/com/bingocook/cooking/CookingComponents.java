package com.bingocook.cooking;

import java.util.List;

import com.bingocook.BingoCook;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom data component types owned by BingoCook.
 */
public final class CookingComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE, BingoCook.MODID);

    /**
     * Permanent attribute modifiers granted when a dish is eaten. Written by
     * {@link SeasoningModifier#applyTo} and applied by
     * {@link CookingEvents#onUseItemFinish}, persisting in the player's NBT.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<AttributeModifierEntry>>> PERMANENT_ATTRIBUTES = DATA_COMPONENTS.registerComponentType(
            "permanent_attributes",
            builder -> builder.persistent(AttributeModifierEntry.CODEC.listOf()).cacheEncoding());

    private CookingComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
