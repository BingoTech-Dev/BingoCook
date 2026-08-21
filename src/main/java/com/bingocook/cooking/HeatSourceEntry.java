package com.bingocook.cooking;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A heat source entry: either a block ID or a block tag ({@code #namespace:tag}).
 */
public record HeatSourceEntry(String raw, boolean tag, Identifier id) {
    /**
     * Parses a heat source string from data packs or commands.
     *
     * @throws IllegalArgumentException if the string is not a valid block ID or tag
     */
    public static HeatSourceEntry parse(String raw) {
        if (raw.startsWith("#")) {
            Identifier tagId = Identifier.parse(raw.substring(1));
            return new HeatSourceEntry(raw, true, tagId);
        }
        Identifier blockId = Identifier.parse(raw);
        return new HeatSourceEntry(raw, false, blockId);
    }

    /**
     * @return true if the given block state matches this entry (block ID or tag membership).
     */
    public boolean matches(BlockState state) {
        if (this.tag) {
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, this.id);
            return state.is(tagKey);
        }
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, this.id);
        return state.is(blockKey);
    }

    @Override
    public String toString() {
        return this.raw;
    }
}
