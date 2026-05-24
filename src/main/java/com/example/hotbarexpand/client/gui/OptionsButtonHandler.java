package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.client.HotbarConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;

public class OptionsButtonHandler {
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen)) return;
        
        OptionsScreen screen = (OptionsScreen) event.getScreen();
        
        // 在选项界面添加"快捷栏设置"按钮
        // 放在屏幕顶部下方，与其他选项按钮保持一致
        int buttonWidth = 150;
        int buttonHeight = 20;
        int x = screen.width / 2 - buttonWidth / 2;
        // 放在屏幕顶部下方130像素处（向下移动10像素）
        int y = screen.height / 6 + 130;
        
        Button hotbarSettingsButton = Button.builder(
            Component.literal("快捷栏设置..."),
            button -> {
                Minecraft.getInstance().setScreen(new HotbarSettingsScreen(screen));
            }
        ).bounds(x, y, buttonWidth, buttonHeight).build();
        
        event.addListener(hotbarSettingsButton);
    }
    
    /**
     * 处理快捷键打开设置界面
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 检查是否按下了设置快捷键
        if (HotbarConfig.getSettingsKey().matches(event.getKey(), event.getScanCode()) && event.getAction() == 1) {
            Minecraft minecraft = Minecraft.getInstance();
            // 如果当前没有打开屏幕，或者打开的是游戏界面，打开设置界面
            if (minecraft.screen == null || minecraft.screen instanceof OptionsScreen) {
                minecraft.setScreen(new HotbarSettingsScreen(minecraft.screen));
            }
        }
    }
}
