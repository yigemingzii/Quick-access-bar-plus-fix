package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.client.ExpandedHotbarOverlay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HotbarControlWidget extends AbstractWidget {
    private static final ResourceLocation HOTBAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
    private static final ResourceLocation HOTBAR_SELECTION_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar_selection.png");

    private final int hotbarIndex;
    private final Minecraft minecraft;

    public HotbarControlWidget(int x, int y, int hotbarIndex) {
        super(x, y, 182, 22, Component.literal("Hotbar " + (hotbarIndex + 1)));
        this.hotbarIndex = hotbarIndex;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();

        guiGraphics.blit(HOTBAR_LOCATION, getX(), getY(), 0, 0, 182, 22, 182, 22);

        int numberColor = (hotbarIndex == ExpandedHotbarOverlay.getCurrentHotbarIndex()) ? 0xFFFF00 : 0xFFFFFF;
        guiGraphics.drawString(minecraft.font, String.valueOf(hotbarIndex + 1), getX() - 15, getY() + 6, numberColor);

        if (hotbarIndex == ExpandedHotbarOverlay.getCurrentHotbarIndex()) {
            guiGraphics.blit(HOTBAR_SELECTION_LOCATION, getX() - 1, getY() - 1, 0, 0, 24, 23, 24, 23);
        }

        RenderSystem.disableBlend();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ExpandedHotbarOverlay.setCurrentHotbarIndex(hotbarIndex);
    }

    public int getHotbarIndex() {
        return hotbarIndex;
    }
}
