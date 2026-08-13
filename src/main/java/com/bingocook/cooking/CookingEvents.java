package com.bingocook.cooking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge event bus subscriptions for the cooking system.
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
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ElementCommands.register(event.getDispatcher(), event.getBuildContext());
    }
}
