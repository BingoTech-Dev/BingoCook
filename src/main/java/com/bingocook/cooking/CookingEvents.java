package com.bingocook.cooking;

import java.util.List;
import java.util.UUID;

import com.bingocook.BingoCook;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * NeoForge event bus subscriptions for the cooking system: registers the
 * element type reload listener, the /bingocook commands, and applies
 * permanent attribute modifiers when a dish is fully eaten.
 *
 * <p>An instance is registered on {@code NeoForge.EVENT_BUS} by
 * {@link com.bingocook.BingoCook}.
 */
public final class CookingEvents {
    public static final CookingEvents INSTANCE = new CookingEvents();

    private CookingEvents() {
    }

    /**
     * Registers the element type loader as a server resource reload listener. The
     * loader has no dependencies on other listeners (item data maps are handled by
     * NeoForge's own loader), so no ordering edge is added.
     */
    @SubscribeEvent
    public void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ElementTypeLoader.LISTENER_ID, ElementTypeLoader.INSTANCE);
        event.addListener(HeatSourceLoader.LISTENER_ID, HeatSourceLoader.INSTANCE);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ElementCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    /**
     * Applies {@code bingocook:permanent_attributes} when a food is fully eaten.
     *
     * <p>NeoForge 26.1 has no FoodEvents class, so the stable
     * {@link LivingEntityUseItemEvent.Finish} is used: the event's item is the
     * stack BEFORE it was consumed, so the component is still present. Every
     * entry is applied through {@code AttributeMap.addPermanentModifier} with a
     * freshly generated modifier ID, so each eaten dish stacks and the modifiers
     * persist in the player's NBT. The component stays on the stack (it is
     * never removed), so a stack that somehow survives eating can grant the
     * attributes again - repeatable gains are a feature.
     */
    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getItem();
        List<AttributeModifierEntry> entries = stack.get(CookingComponents.PERMANENT_ATTRIBUTES);
        if (entries == null || entries.isEmpty() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        for (AttributeModifierEntry entry : entries) {
            var instance = player.getAttributes().getInstance(entry.attribute());
            if (instance != null) {
                Identifier modifierId = Identifier.fromNamespaceAndPath(BingoCook.MODID, "permanent/" + UUID.randomUUID());
                instance.addPermanentModifier(entry.toModifier(modifierId));
            }
        }
    }
}
