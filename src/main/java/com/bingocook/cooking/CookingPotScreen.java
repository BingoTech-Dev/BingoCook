package com.bingocook.cooking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Cooking pot screen: the vanilla crafting table background (its 3x3 grid,
 * player inventory and hotbar line up with this container's slot layout) with
 * the vanilla furnace burn progress sprite drawn between the grid and the
 * output slot, cropped by the cooking progress.
 *
 * <p>Client-only: referenced solely from {@code BingoCookClient}, which never
 * loads on a dedicated server. Deliberately not annotated {@code @OnlyIn} —
 * 26.1 dropped runtime member-stripping and the NeoForge warning handler
 * reports the annotation at ERROR level for mod classes.
 */
public class CookingPotScreen extends AbstractContainerScreen<CookingPotMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public CookingPotScreen(CookingPotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo, yo, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
        int burnWidth = Mth.ceil(this.menu.getCookProgress() * 24.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0,
                xo + 88, yo + 35, burnWidth, 16);
    }
}
