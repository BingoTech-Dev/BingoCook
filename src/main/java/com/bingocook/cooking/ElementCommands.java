package com.bingocook.cooking;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * {@code /bingocook elements ...} commands (permission level 2).
 *
 * <p>Registered through
 * {@link net.neoforged.neoforge.event.RegisterCommandsEvent} on the NeoForge event
 * bus - see {@link CookingEvents}. The NeoForge documentation library has no
 * commands chapter; this event (NeoForge.EVENT_BUS + Brigadier, stable since 1.16)
 * was verified against the 26.1.2.94 sources jar.
 */
public final class ElementCommands {
    private ElementCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("bingocook")
                // Op level 2 (GAMEMASTERS) - 26.1 replaced the numeric hasPermission(int)
                // with named PermissionCheck levels.
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("elements")
                        .then(Commands.literal("list")
                                .executes(ElementCommands::listElements))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ItemArgument.item(context))
                                        .executes(ElementCommands::listItemElements)))));
    }

    private static int listElements(CommandContext<CommandSourceStack> context) {
        Set<Identifier> elements = CookingData.elements();
        String summary = elements.stream()
                .map(Identifier::toString)
                .sorted()
                .collect(Collectors.joining(", "));
        String message = elements.isEmpty()
                ? "No elements loaded"
                : "Elements (" + elements.size() + "): " + summary;
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return elements.size();
    }

    private static int listItemElements(CommandContext<CommandSourceStack> context) {
        ItemInput input = ItemArgument.getItem(context, "item");
        Holder<Item> holder = input.item();
        Map<Identifier, Integer> values = CookingData.elementsOf(holder);
        String summary = values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        String message = values.isEmpty()
                ? "No elements"
                : summary;
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return values.size();
    }
}
