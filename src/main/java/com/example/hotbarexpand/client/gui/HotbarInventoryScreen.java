package com.example.hotbarexpand.client.gui;

import com.example.hotbarexpand.client.HotbarManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 快捷栏背包界面 - 在原版背包右侧显示可交互的快捷栏面板
 */
public class HotbarInventoryScreen {
    // 使用原版背包纹理
    private static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");
    
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 2;
    private static final int NUMBER_WIDTH = 14; // 编号按钮宽度
    private static final int NUMBER_GAP = 6; // 编号与快捷栏之间的间距
    private static final int CONTENT_WIDTH = 9 * SLOT_SIZE + SLOT_SIZE + 4; // 9格 + 副手格 + 间距
    private static final int PANEL_WIDTH = NUMBER_WIDTH + NUMBER_GAP + CONTENT_WIDTH + 8; // 编号 + 间距 + 内容 + 边距
    
    // 面板位置（相对于屏幕）
    private static int panelX = 0;
    private static int panelY = 0;
    private static int panelHeight = 0;
    private static boolean isPanelVisible = false;
    
    // 当前高亮的快捷栏索引（用于解决残留黄框问题）
    private static int lastHighlightedHotbar = -1;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        
        // 每次打开背包都重新计算位置
        recalculatePanelPosition(screen);
        
        isPanelVisible = true;
        lastHighlightedHotbar = HotbarManager.getCurrentHotbarIndex();
        System.out.println("[HotbarExpand] Hotbar panel positioned at (" + panelX + ", " + panelY + "), height=" + panelHeight);
    }
    
    /**
     * 重新计算面板位置
     */
    private static void recalculatePanelPosition(AbstractContainerScreen<?> screen) {
        // 计算面板位置：与背包上下对齐，靠右侧
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop()-15;
        int guiHeight = 166; // 原版背包高度
        
        panelHeight = 9 * (SLOT_SIZE + PADDING) + 8; // 9个快捷栏 + 边距
        panelY = guiTop + (guiHeight - panelHeight) / 2; // 垂直居中
        
        // 计算X位置：背包右侧 + 间距
        panelX = guiLeft + 176 + 25; // 背包宽度176 + 间距6
        
        // 如果超出屏幕右侧，显示在左侧
        if (panelX + PANEL_WIDTH > screen.width) {
            panelX = Math.max(5, guiLeft - PANEL_WIDTH - 6);
        }
    }
    
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        isPanelVisible = false;
    }
    
    @SubscribeEvent
    public static void onContainerBackground(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) {
            return;
        }
        
        // 每次渲染前重新计算位置，确保位置正确
        recalculatePanelPosition(screen);
        isPanelVisible = true;
        
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        
        // 实时获取当前高亮索引，确保与HotbarManager同步
        int currentHotbarIndex = HotbarManager.getCurrentHotbarIndex();
        lastHighlightedHotbar = currentHotbarIndex;
        
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // 绘制背景（使用背包背景色）
        renderPanelBackground(guiGraphics, panelX, panelY, PANEL_WIDTH, panelHeight);
        
        // 绘制9个快捷栏
        for (int hotbarIdx = 0; hotbarIdx < 9; hotbarIdx++) {
            int rowY = panelY + 4 + hotbarIdx * (SLOT_SIZE + PADDING);
            boolean isCurrent = (hotbarIdx == currentHotbarIndex);
            
            // 绘制编号按钮背景
            int numberX = panelX + 4;
            renderNumberButton(guiGraphics, numberX, rowY, hotbarIdx + 1, isCurrent);
            
            // 绘制9个物品槽位（添加间距）
            int contentStartX = panelX + NUMBER_WIDTH + NUMBER_GAP + 4;
            for (int slotIdx = 0; slotIdx < 9; slotIdx++) {
                int slotX = contentStartX + slotIdx * SLOT_SIZE;
                renderSlot(guiGraphics, slotX, rowY, hotbarIdx, slotIdx, isCurrent, minecraft);
            }
            
            // 绘制副手槽位
            int offhandX = contentStartX + 9 * SLOT_SIZE + 2;
            renderOffhandSlot(guiGraphics, offhandX, rowY, hotbarIdx, isCurrent, minecraft);
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * 绘制面板背景 - 使用背包颜色
     */
    private static void renderPanelBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 使用纯色填充背景（背包背景色：#C6C6C6）
        guiGraphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
        
        // 绘制边框（深色）
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF373737); // 上边框
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF373737); // 下边框
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF373737); // 左边框
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF373737); // 右边框
        
        // 绘制内边框（亮色）
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFFFFFFF); // 上内边框
        guiGraphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFFFFFFF); // 左内边框
        
        // 绘制阴影（深色）
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF8B8B8B); // 下阴影
        guiGraphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B); // 右阴影
    }
    
    /**
     * 绘制编号按钮
     */
    private static void renderNumberButton(GuiGraphics guiGraphics, int x, int y, int number, boolean isCurrent) {
        int btnWidth = NUMBER_WIDTH;
        int btnHeight = SLOT_SIZE;
        
        // 按钮背景色 - 始终使用灰色，不随选中状态变化
        int bgColor = 0xFF8B8B8B;
        int highlightColor = 0xFFFFFFFF;
        int shadowColor = 0xFF373737;
        
        // 绘制按钮背景
        guiGraphics.fill(x, y, x + btnWidth, y + btnHeight, bgColor);
        
        // 绘制按钮边框（3D效果）
        guiGraphics.fill(x, y, x + btnWidth, y + 1, highlightColor); // 上亮边
        guiGraphics.fill(x, y, x + 1, y + btnHeight, highlightColor); // 左亮边
        guiGraphics.fill(x, y + btnHeight - 1, x + btnWidth, y + btnHeight, shadowColor); // 下阴影
        guiGraphics.fill(x + btnWidth - 1, y, x + btnWidth, y + btnHeight, shadowColor); // 右阴影
        
        // 绘制数字 - 选中时变黄，未选中时白色
        int textColor = isCurrent ? 0xFFFFFF00 : 0xFFFFFFFF;
        String text = String.valueOf(number);
        int textX = x + (btnWidth - 6) / 2;
        int textY = y + (btnHeight - 8) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, textColor);
    }
    
    private static void renderSlot(GuiGraphics guiGraphics, int x, int y, int hotbarIdx, int slotIdx, boolean isCurrentHotbar, Minecraft minecraft) {
        // 绘制槽位背景
        guiGraphics.blit(INVENTORY_LOCATION, x, y, 7, 83, SLOT_SIZE, SLOT_SIZE, 256, 256);
        
        // 如果是当前快捷栏，绘制黄色高亮
        if (isCurrentHotbar) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 0.0f, 0.3f);
            guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FFFF00);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // 渲染物品
        ItemStack itemStack = HotbarManager.getHotbar(hotbarIdx).get(slotIdx);
        if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, x + 1, y + 1);
            guiGraphics.renderItemDecorations(minecraft.font, itemStack, x + 1, y + 1);
        }
    }
    
    private static void renderOffhandSlot(GuiGraphics guiGraphics, int x, int y, int hotbarIdx, boolean isCurrentHotbar, Minecraft minecraft) {
        // 绘制槽位背景
        guiGraphics.blit(INVENTORY_LOCATION, x, y, 7, 83, SLOT_SIZE, SLOT_SIZE, 256, 256);
        
        // 如果是当前快捷栏，绘制黄色高亮
        if (isCurrentHotbar) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 0.0f, 0.3f);
            guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x80FFFF00);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        
        // 渲染副手物品
        ItemStack offhandItem = HotbarManager.getOffhandItem(hotbarIdx);
        if (!offhandItem.isEmpty()) {
            guiGraphics.renderItem(offhandItem, x + 1, y + 1);
            guiGraphics.renderItemDecorations(minecraft.font, offhandItem, x + 1, y + 1);
        }
    }
    
    // ========== 鼠标交互 ==========
    
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post event) {
        if (!isPanelVisible) return;
        if (!(event.getScreen() instanceof InventoryScreen) && !(event.getScreen() instanceof CreativeModeInventoryScreen)) {
            return;
        }
        
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        
        // 检查是否点击了编号按钮（切换快捷栏）
        for (int i = 0; i < 9; i++) {
            int numberX = panelX + 4;
            int numberY = panelY + 4 + i * (SLOT_SIZE + PADDING);
            
            if (mouseX >= numberX && mouseX <= numberX + NUMBER_WIDTH && 
                mouseY >= numberY && mouseY <= numberY + SLOT_SIZE) {
                // 点击了编号，切换快捷栏
                HotbarManager.setCurrentHotbarIndex(i);
                lastHighlightedHotbar = i; // 立即更新高亮
                
                // 播放按钮音效
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                
                System.out.println("[HotbarExpand] Clicked hotbar number: " + (i + 1));
                return;
            }
        }
        
        // 检查是否点击了物品槽位
        int contentStartX = panelX + NUMBER_WIDTH + NUMBER_GAP + 4;
        for (int hotbarIdx = 0; hotbarIdx < 9; hotbarIdx++) {
            int rowY = panelY + 4 + hotbarIdx * (SLOT_SIZE + PADDING);
            
            // 检查9个物品槽位
            for (int slotIdx = 0; slotIdx < 9; slotIdx++) {
                int slotX = contentStartX + slotIdx * SLOT_SIZE;
                
                if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE && 
                    mouseY >= rowY && mouseY <= rowY + SLOT_SIZE) {
                    // 点击了物品槽位
                    handleSlotClick(hotbarIdx, slotIdx, event.getButton());
                    return;
                }
            }
            
            // 检查副手槽位
            int offhandX = contentStartX + 9 * SLOT_SIZE + 2;
            if (mouseX >= offhandX && mouseX <= offhandX + SLOT_SIZE && 
                mouseY >= rowY && mouseY <= rowY + SLOT_SIZE) {
                // 点击了副手槽位
                handleOffhandClick(hotbarIdx, event.getButton());
                return;
            }
        }
    }
    
    private static void handleSlotClick(int hotbarIdx, int slotIdx, int button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        // 获取玩家光标上的物品
        ItemStack carriedItem = minecraft.player.containerMenu.getCarried();
        ItemStack slotItem = HotbarManager.getHotbar(hotbarIdx).get(slotIdx);
        
        // 创建新的ItemStack来存储结果（避免引用问题）
        ItemStack newCarried = carriedItem.copy();
        ItemStack newSlot = slotItem.copy();
        
        if (button == 0) { // 左键
            if (newCarried.isEmpty() && !newSlot.isEmpty()) {
                // 拿起物品
                newCarried = newSlot.copy();
                newSlot = ItemStack.EMPTY;
            } else if (!newCarried.isEmpty() && newSlot.isEmpty()) {
                // 放下物品
                newSlot = newCarried.copy();
                newCarried = ItemStack.EMPTY;
            } else if (!newCarried.isEmpty() && !newSlot.isEmpty()) {
                // 交换物品
                if (ItemStack.isSameItemSameComponents(newCarried, newSlot)) {
                    // 合并物品
                    int maxStack = newSlot.getMaxStackSize();
                    int canAdd = maxStack - newSlot.getCount();
                    int toAdd = Math.min(canAdd, newCarried.getCount());
                    
                    if (toAdd > 0) {
                        newSlot.grow(toAdd);
                        newCarried.shrink(toAdd);
                        if (newCarried.isEmpty()) {
                            newCarried = ItemStack.EMPTY;
                        }
                    }
                } else {
                    // 交换
                    ItemStack temp = newSlot.copy();
                    newSlot = newCarried.copy();
                    newCarried = temp;
                }
            }
        } else if (button == 1) { // 右键
            if (!newCarried.isEmpty()) {
                // 放下一半
                int count = (newCarried.getCount() + 1) / 2;
                ItemStack toPlace = newCarried.copy();
                toPlace.setCount(count);
                newCarried.shrink(count);
                
                if (newSlot.isEmpty()) {
                    newSlot = toPlace;
                } else if (ItemStack.isSameItemSameComponents(newCarried, newSlot)) {
                    newSlot.grow(toPlace.getCount());
                }
                
                if (newCarried.isEmpty()) {
                    newCarried = ItemStack.EMPTY;
                }
            } else if (!newSlot.isEmpty()) {
                // 拿起一半
                int count = (newSlot.getCount() + 1) / 2;
                newCarried = newSlot.copy();
                newCarried.setCount(count);
                newSlot.shrink(count);
                
                if (newSlot.isEmpty()) {
                    newSlot = ItemStack.EMPTY;
                }
            }
        }
        
        // 应用更改
        minecraft.player.containerMenu.setCarried(newCarried);
        HotbarManager.getHotbar(hotbarIdx).set(slotIdx, newSlot);
        
        // 如果修改的是当前快捷栏，同步到玩家背包
        if (hotbarIdx == HotbarManager.getCurrentHotbarIndex()) {
            minecraft.player.getInventory().setItem(slotIdx, newSlot.copy());
        }
        
        System.out.println("[HotbarExpand] Slot clicked: hotbar=" + (hotbarIdx + 1) + ", slot=" + (slotIdx + 1));
    }
    
    private static void handleOffhandClick(int hotbarIdx, int button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        ItemStack carriedItem = minecraft.player.containerMenu.getCarried();
        ItemStack offhandItem = HotbarManager.getOffhandItem(hotbarIdx);
        
        ItemStack newCarried = carriedItem.copy();
        ItemStack newOffhand = offhandItem.copy();
        
        if (button == 0) { // 左键
            if (newCarried.isEmpty() && !newOffhand.isEmpty()) {
                newCarried = newOffhand.copy();
                newOffhand = ItemStack.EMPTY;
            } else if (!newCarried.isEmpty() && newOffhand.isEmpty()) {
                newOffhand = newCarried.copy();
                newCarried = ItemStack.EMPTY;
            } else if (!newCarried.isEmpty() && !newOffhand.isEmpty()) {
                ItemStack temp = newOffhand.copy();
                newOffhand = newCarried.copy();
                newCarried = temp;
            }
        }
        
        // 应用更改
        minecraft.player.containerMenu.setCarried(newCarried);
        HotbarManager.setOffhandItem(hotbarIdx, newOffhand);
        
        System.out.println("[HotbarExpand] Offhand clicked: hotbar=" + (hotbarIdx + 1));
    }
}
