package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.HotbarExpandMod;
import com.example.hotbarexpand.client.HotbarConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HotbarSettingsScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/options_background.png");
    
    private final Screen lastScreen;
    private Button layoutButton;
    
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
        
        // 布局模式选择按钮
        layoutButton = Button.builder(Component.literal("快捷栏布局: " + HotbarConfig.getLayout().displayName), button -> {
            cycleLayoutMode();
        }).bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build();
        this.addRenderableWidget(layoutButton);
        
        // 返回按钮
        this.addRenderableWidget(Button.builder(Component.literal("完成"), button -> {
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(centerX - buttonWidth / 2, startY + 120, buttonWidth, buttonHeight).build());
    }
    
    private void cycleLayoutMode() {
        HotbarConfig.LayoutMode[] modes = HotbarConfig.LayoutMode.values();
        int currentIndex = HotbarConfig.getLayout().ordinal();
        int nextIndex = (currentIndex + 1) % modes.length;
        HotbarConfig.setLayout(modes[nextIndex]);
        layoutButton.setMessage(Component.literal("快捷栏布局: " + HotbarConfig.getLayout().displayName));
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
        guiGraphics.drawCenteredString(this.font, Component.literal("Ctrl+B: 展开/收起快捷栏"), centerX, startY + 60, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Alt+滚轮: 选择快捷栏"), centerX, startY + 80, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Alt+数字键: 切换快捷栏"), centerX, startY + 100, 0xFFFFFF);
        
        // 渲染布局说明
        renderLayoutDescription(guiGraphics, centerX, startY + 145);
    }
    
    private void renderLayoutDescription(GuiGraphics guiGraphics, int centerX, int y) {
        HotbarConfig.LayoutMode layout = HotbarConfig.getLayout();
        String desc = switch (layout) {
            case ONE_X_ONE -> "显示1个快捷栏 (原版样式)";
            case ONE_X_TWO -> "显示2个快捷栏，垂直排列";
            case TWO_X_ONE -> "显示2个快捷栏，水平排列";
            case TWO_X_TWO -> "显示4个快捷栏，2x2网格排列";
        };
        guiGraphics.drawCenteredString(this.font, Component.literal(desc), centerX, y, 0xAAAAAA);
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
