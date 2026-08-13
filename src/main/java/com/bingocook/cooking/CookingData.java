package com.bingocook.cooking;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side query API for cooking data.
 */
public final class CookingData {
    private CookingData() {
    }

    /**
     * @return the currently loaded set of cooking element IDs; empty before the first
     *         server reload or when data packs removed all elements.
     */
    public static Set<Identifier> elements() {
        return ElementTypeLoader.INSTANCE.getElements().stream()
                .map(CookingElement::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return the elemental values of the given stack's item, or an empty map if the
     *         item has none. No element validation is performed - recipes filter
     *         unknown element IDs through their {@code allowed} whitelist.
     */
    public static Map<Identifier, Integer> elementsOf(ItemStack stack) {
        return elementsOf(stack.typeHolder());
    }

    /**
     * Holder-based overload of {@link #elementsOf(ItemStack)}.
     */
    public static Map<Identifier, Integer> elementsOf(Holder<Item> holder) {
        ElementValues values = holder.getData(DataMaps.ITEM_ELEMENTS);
        return values == null ? Map.of() : values.elements();
    }
}
