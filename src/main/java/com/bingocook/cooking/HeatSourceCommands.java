package com.bingocook.cooking;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * {@code /bingocook heat_sources ...} command handlers (permission level 2).
 */
public final class HeatSourceCommands {
    private static final SimpleCommandExceptionType INVALID_SOURCE = new SimpleCommandExceptionType(
            Component.literal("Invalid heat source: must be a block id or #block_tag"));

    private HeatSourceCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildNode() {
        return Commands.literal("heat_sources")
                .then(Commands.literal("list")
                        .executes(HeatSourceCommands::listHeatSources))
                .then(Commands.literal("add")
                        .then(Commands.argument("source", StringArgumentType.greedyString())
                                .executes(HeatSourceCommands::addHeatSource)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("source", StringArgumentType.greedyString())
                                .executes(HeatSourceCommands::removeHeatSource)));
    }

    private static int listHeatSources(CommandContext<CommandSourceStack> context) {
        var annotated = HeatSourceManager.INSTANCE.effectiveAnnotated();
        if (annotated.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No heat sources loaded"), false);
            return 0;
        }
        String message = "Heat sources (" + annotated.size() + "): " + HeatSourceManager.INSTANCE.effectiveSummary();
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return annotated.size();
    }

    private static int addHeatSource(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HeatSourceEntry entry = parseAndValidate(context);
        boolean added = HeatSourceManager.INSTANCE.addRuntime(entry);
        String message = added
                ? "Added heat source: " + entry.raw() + " (runtime, until reload)"
                : "Heat source already active: " + entry.raw();
        context.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }

    private static int removeHeatSource(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        HeatSourceEntry entry = parseAndValidate(context);
        boolean removed = HeatSourceManager.INSTANCE.removeRuntime(entry);
        if (!removed) {
            context.getSource().sendFailure(Component.literal("Heat source not active: " + entry.raw()));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Removed heat source: " + entry.raw() + " (runtime, until reload)"), true);
        return 1;
    }

    private static HeatSourceEntry parseAndValidate(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, "source").trim();
        HeatSourceEntry entry;
        try {
            entry = HeatSourceEntry.parse(raw);
        } catch (IllegalArgumentException exception) {
            throw INVALID_SOURCE.create();
        }
        validateExists(context.getSource(), entry);
        return entry;
    }

    private static void validateExists(CommandSourceStack source, HeatSourceEntry entry) throws CommandSyntaxException {
        if (entry.tag()) {
            return;
        }
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, entry.id());
        if (source.registryAccess().lookupOrThrow(Registries.BLOCK).get(blockKey).isEmpty()) {
            throw INVALID_SOURCE.create();
        }
    }
}
