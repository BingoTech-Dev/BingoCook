package com.bingocook.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Query API for configured cooking heat sources.
 */
public final class HeatSources {
    private HeatSources() {
    }

    /**
     * @return true if the block directly below {@code pos} is an active heat source
     *         according to the current effective configuration.
     */
    public static boolean isActiveBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (HeatSourceManager.INSTANCE.effective().stream().noneMatch(entry -> entry.matches(below))) {
            return false;
        }
        if (below.hasProperty(BlockStateProperties.LIT)) {
            return below.getValue(BlockStateProperties.LIT);
        }
        return true;
    }
}
