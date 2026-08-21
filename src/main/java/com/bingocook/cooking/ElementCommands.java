package com.bingocook.cooking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

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
                                        .executes(ElementCommands::listItemElements)))
                        .then(Commands.literal("recipe")
                                .then(Commands.argument("recipe", IdentifierArgument.id())
                                        .executes(ElementCommands::listRecipe))))
                .then(HeatSourceCommands.buildNode()));
    }

    private static int listElements(CommandContext<CommandSourceStack> context) {
        Set<Identifier> elements = CookingData.elements();
        @SuppressWarnings("null")
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
        @SuppressWarnings("null")
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

    @SuppressWarnings("null")
private static int listRecipe(CommandContext<CommandSourceStack> context) {
        Identifier id = IdentifierArgument.getId(context, "recipe");
        RecipeManager manager = context.getSource().getServer().getRecipeManager();
        RecipeHolder<?> holder = manager.recipeMap().byKey(ResourceKey.create(Registries.RECIPE, id));
        if (holder == null) {
            context.getSource().sendFailure(Component.literal("Unknown recipe: " + id));
            return 0;
        }
        if (!(holder.value() instanceof CookingRecipe recipe)) {
            context.getSource().sendFailure(Component.literal("Not a cooking recipe: " + id));
            return 0;
        }

        List<String> lines = new ArrayList<>();
        lines.add("Recipe: " + id + (recipe.enabled() ? "" : " (disabled)")
                + " - cookingTime: " + recipe.cookingTime() + " ticks, result: " + recipe.result().item().unwrapKey().orElseThrow().identifier());
        lines.add("allowed: " + (recipe.allowed() == null
                ? "all"
                : recipe.allowed().stream().map(Identifier::toString).sorted().collect(Collectors.joining(", "))));
        lines.add("requirements: " + recipe.requirements().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", ")));
        lines.add("seasonings: " + (recipe.seasonings().isEmpty()
                ? "none"
                : recipe.seasonings().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(item -> item.unwrapKey().orElseThrow().identifier().toString())))
                        .map(entry -> entry.getKey().unwrapKey().orElseThrow().identifier() + " -> " + entry.getValue())
                        .collect(Collectors.joining(" | "))));
        for (String line : lines) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
