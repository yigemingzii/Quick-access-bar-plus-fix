package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.HotbarExpandMod;
import com.example.hotbarexpand.client.ExpandedHotbarOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = HotbarExpandMod.MOD_ID, value = Dist.CLIENT)
public class InventoryHotbarControl {
    private static final List<HotbarControlWidget> hotbarWidgets = new ArrayList<>();
    private static boolean widgetsInitialized = false;

    @SubscribeEvent
    public static void onInventoryOpen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) return;

        hotbarWidgets.clear();
        widgetsInitialized = false;

        int startX = inventoryScreen.getGuiLeft() + 8;
        int startY = inventoryScreen.getGuiTop() - 25;

        for (int i = 0; i < 9; i++) {
            HotbarControlWidget widget = new HotbarControlWidget(startX, startY - (i * 24), i);
            hotbarWidgets.add(widget);
            event.addListener(widget);
        }

        widgetsInitialized = true;
    }

    @SubscribeEvent
    public static void onInventoryRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) return;
        if (!widgetsInitialized) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();

        for (HotbarControlWidget widget : hotbarWidgets) {
            widget.render(guiGraphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onInventoryClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof InventoryScreen) {
            hotbarWidgets.clear();
            widgetsInitialized = false;
        }
    }
}
