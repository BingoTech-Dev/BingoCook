package com.bingocook.cooking;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

/**
 * A cooking element type.
 *
 * <p>In v1 an element carries no data beyond its ID. Element IDs are loaded from
 * data packs by {@link ElementTypeLoader}. Unknown element IDs found in item data
 * maps are intentionally not validated here - recipes filter them out through
 * their {@code allowed} whitelist.
 */
public record CookingElement(Identifier id) {
    public static final Codec<CookingElement> CODEC = Identifier.CODEC.xmap(CookingElement::new, CookingElement::id);

    @Override
    public String toString() {
        return id.toString();
    }
}
