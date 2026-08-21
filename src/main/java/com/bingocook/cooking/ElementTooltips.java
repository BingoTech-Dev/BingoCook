package com.bingocook.cooking;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.bingocook.BingoCook;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Appends the item's cooking element values to its tooltip (client only).
 *
 * <p>Values come from the client-side copy of the {@link DataMaps#ITEM_ELEMENTS}
 * data map, which NeoForge syncs during world join, so the displayed amounts
 * always match the server's. The set of element types itself is server-only;
 * display names are derived from the element ID (see {@link #displayName}).
 *
 * <p>Format: a gray "Element Values" header followed by one line per element
 * with the value, e.g. {@code Fruit: 2}. Entries with a non-positive amount
 * are omitted (a 0 means the element is absent).
 */
@EventBusSubscriber(modid = BingoCook.MODID, value = Dist.CLIENT)
public final class ElementTooltips {
    private static final Component HEADER = Component.translatable("tooltip.bingocook.element_values")
            .withStyle(ChatFormatting.GRAY);

    private ElementTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        List<Map.Entry<Identifier, Integer>> entries = CookingData.elementsOf(event.getItemStack()).entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .toList();
        if (entries.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(HEADER);
        for (Map.Entry<Identifier, Integer> entry : entries) {
            tooltip.add(Component.translatableWithFallback(entry.getKey().toLanguageKey("element"), displayName(entry.getKey()))
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(": " + entry.getValue()).withStyle(ChatFormatting.GRAY)));
        }
    }

    /**
     * Fallback display name for an element ID: the path with underscores
     * replaced by spaces and each word capitalised. IDs outside the mod's own
     * namespace keep their full form so data-pack elements stay unambiguous.
     */
    private static String displayName(Identifier id) {
        if (!id.getNamespace().equals(BingoCook.MODID)) {
            return id.toString();
        }
        StringBuilder name = new StringBuilder();
        boolean capitalize = true;
        for (char c : id.getPath().toCharArray()) {
            if (c == '_') {
                name.append(' ');
                capitalize = true;
            } else if (capitalize) {
                name.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                name.append(c);
            }
        }
        return name.toString();
    }
}
