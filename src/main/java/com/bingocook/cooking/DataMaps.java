package com.bingocook.cooking;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Registry data maps owned by BingoCook.
 *
 * <p>{@link #ITEM_ELEMENTS} maps items to their {@link ElementValues}. NeoForge
 * loads it from {@code <mapNamespace>/data_maps/item/item_elements.json} - with the
 * map namespace "bingocook" the default file lives at
 * {@code data/bingocook/data_maps/item/item_elements.json}. Pack authors can add
 * files in their own namespace, remove entries with {@code "remove"}, use tag
 * values with {@code "#namespace:tag"} and element-level {@code "replace": true}
 * (wrapping the value under {@code "value"}); /reload picks up changes.
 *
 * <p>The map is synced to clients: NeoForge serializes the attachment with the
 * network codec ({@link ElementValues#CODEC}) into JSON during world join, so
 * client-side consumers such as {@link ElementTooltips} see the same values as
 * the server. Sync is not mandatory, so vanilla clients can still connect and
 * simply have no element data.
 */
public final class DataMaps {
    public static final DataMapType<Item, ElementValues> ITEM_ELEMENTS = DataMapType.builder(
            Identifier.fromNamespaceAndPath("bingocook", "item_elements"),
            Registries.ITEM,
            ElementValues.CODEC)
            .synced(ElementValues.CODEC, false)
            .build();

    private DataMaps() {
    }

    public static void registerDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(ITEM_ELEMENTS);
    }
}
