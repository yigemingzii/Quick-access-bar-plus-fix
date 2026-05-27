package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.HotbarExpandMod;
import com.example.hotbarexpand.client.HotbarConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HotbarSettingsScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/options_background.png");

    private final Screen lastScreen;
    private Button layoutButton;
    private SliderWidget maxVisibleSlider;
    private int maxVisibleTemp;

    public HotbarSettingsScreen(Screen lastScreen) {
        super(Component.literal("快捷栏拓展设置"));
        this.lastScreen = lastScreen;
        this.maxVisibleTemp = HotbarConfig.getMaxVisibleHotbars();
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

        // 最大显示数量滑块
        maxVisibleSlider = new SliderWidget(centerX - buttonWidth / 2, startY + 30, buttonWidth, buttonHeight,
                Component.literal("列表最大显示"), maxVisibleTemp, HotbarConfig.getMinVisibleHotbars(), HotbarConfig.getMaxVisibleHotbarsLimit()) {
            @Override
            protected void applyValue() {
                maxVisibleTemp = (int) (this.value * (max - min) + min);
            }
        };
        this.addRenderableWidget(maxVisibleSlider);

        // 返回按钮
        this.addRenderableWidget(Button.builder(Component.literal("完成"), button -> {
            // 保存设置
            if (maxVisibleTemp != HotbarConfig.getMaxVisibleHotbars()) {
                HotbarConfig.setMaxVisibleHotbars(maxVisibleTemp);
            }
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

        // 渲染警告（当大于9时）- 放在完成按钮上方
        if (maxVisibleTemp > 9) {
            guiGraphics.drawCenteredString(this.font, Component.literal("警告: Alt+滚轮仅覆盖前9个快捷栏"), centerX, startY + 110, 0xFF0000);
        }

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

    /**
     * 滑块组件 - 用于设置数值
     */
    public static abstract class SliderWidget extends AbstractWidget {
        protected double value;
        protected final int min;
        protected final int max;
        private boolean isDragging = false;

        public SliderWidget(int x, int y, int width, int height, Component message, int initialValue, int min, int max) {
            super(x, y, width, height, message);
            this.min = min;
            this.max = max;
            this.value = (double) (initialValue - min) / (max - min);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();

            // 绘制背景
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);

            // 绘制滑块轨道
            int trackY = this.getY() + this.height / 2 - 1;
            guiGraphics.fill(this.getX() + 4, trackY, this.getX() + this.width - 4, trackY + 2, 0xFF555555);

            // 计算滑块位置
            int thumbWidth = 8;
            int thumbX = this.getX() + 4 + (int) (this.value * (this.width - 8 - 8));

            // 绘制滑块
            guiGraphics.fill(thumbX, this.getY() + 2, thumbX + thumbWidth, this.getY() + this.height - 2, this.isHovered() ? 0xFFFFFFFF : 0xFFAAAAAA);

            // 绘制数值文字
            int currentValue = (int) (this.value * (max - min) + min);
            Component valueText = Component.literal(this.getMessage().getString() + ": " + currentValue);
            guiGraphics.drawCenteredString(minecraft.font, valueText, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.isDragging = true;
            this.setValueFromMouse(mouseX);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            if (this.isDragging) {
                this.setValueFromMouse(mouseX);
            }
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.isDragging = false;
        }

        private void setValueFromMouse(double mouseX) {
            this.value = Mth.clamp((mouseX - (this.getX() + 4)) / (this.width - 8), 0.0, 1.0);
            this.applyValue();
        }

        protected abstract void applyValue();

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
