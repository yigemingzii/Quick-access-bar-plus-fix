package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.HotbarExpandMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HotbarSettingsScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/options_background.png");
    
    private final Screen lastScreen;
    
    public HotbarSettingsScreen(Screen lastScreen) {
        super(Component.literal("快捷栏拓展设置"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 4;
        
        // 返回按钮
        this.addRenderableWidget(Button.builder(Component.literal("完成"), button -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX - buttonWidth / 2, startY + 120, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 先调用super.render来渲染背景和组件
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染标题（在背景之上）
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 渲染说明文字
        int centerX = this.width / 2;
        int startY = this.height / 4;
        guiGraphics.drawCenteredString(this.font, Component.literal("Ctrl+B: 展开/收起快捷栏"), centerX, startY, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Alt+滚轮: 选择快捷栏"), centerX, startY + 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Alt+数字键: 切换快捷栏"), centerX, startY + 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("数字键: 选择列表中的快捷栏"), centerX, startY + 60, 0xFFFFFF);
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染菜单背景（dirt背景）
        this.renderMenuBackground(guiGraphics);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
