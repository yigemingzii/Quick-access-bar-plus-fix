package com.example.hotbarexpand.client;

import com.example.hotbarexpand.client.HotbarConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

public class ExpandedHotbarOverlay {
    private static final ResourceLocation HOTBAR_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
    private static final ResourceLocation HOTBAR_SELECTION_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar_selection.png");
    private static final ResourceLocation HOTBAR_OFFHAND_LEFT_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar_offhand_left.png");
    private static final ResourceLocation HOTBAR_OFFHAND_RIGHT_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar_offhand_right.png");
    private static final ResourceLocation HOTBAR_LAYER = ResourceLocation.withDefaultNamespace("hotbar");

    // 展开动画相关
    private static float expandProgress = 0.0f;
    private static final float EXPAND_SPEED = 0.12f;
    private static boolean isExpanding = false;
    private static boolean targetExpanded = false;

    // 组内选中的快捷栏索引（用于非1x1模式）
    private static int selectedHotbarInGroup = 0;

    public static boolean isExpanded() {
        return HotbarManager.isExpanded();
    }

    /**
     * 检查指定快捷栏是否在当前组内（考虑补齐情况）
     */
    private static boolean isInCurrentGroup(int hotbarIdx) {
        int currentIndex = getCurrentHotbarIndex();
        HotbarConfig.LayoutMode layout = HotbarConfig.getLayout();
        int layoutSize = layout.getTotalHotbars();
        int groupStart = (currentIndex / layoutSize) * layoutSize;
        int maxHotbars = HotbarManager.getHotbarCount();

        // 计算当前组的所有索引（包括补齐的）
        for (int i = 0; i < layoutSize; i++) {
            int idx = getPaddedHotbarIndex(groupStart + i, maxHotbars);
            if (idx == hotbarIdx) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取补齐后的快捷栏索引，如果超出范围则用列表首位循环补齐
     */
    private static int getPaddedHotbarIndex(int index, int maxHotbars) {
        if (index < maxHotbars) {
            return index;
        }
        // 超出范围，用列表首位循环补齐
        return index % maxHotbars;
    }

    public static void setExpanded(boolean expanded) {
        if (expanded != targetExpanded) {
            if (!expanded) {
                HotbarManager.savePlayerInventoryToCurrentHotbar();
            }
            targetExpanded = expanded;
            isExpanding = true;
            HotbarManager.setExpanded(expanded);
        }
    }

    public static int getCurrentHotbarIndex() {
        return HotbarManager.getCurrentHotbarIndex();
    }

    public static void setCurrentHotbarIndex(int index) {
        HotbarManager.setCurrentHotbarIndex(index);
    }

    private static int tickCounter = 0;
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 100 == 0) {
            System.out.println("[HotbarExpand] onClientTick running, expanded=" + isExpanded() + ", expanding=" + isExpanding + " progress=" + expandProgress);
        }
        
        if (isExpanding) {
            if (targetExpanded) {
                expandProgress += EXPAND_SPEED;
                if (expandProgress >= 1.0f) {
                    expandProgress = 1.0f;
                    isExpanding = false;
                }
            } else {
                expandProgress -= EXPAND_SPEED;
                if (expandProgress <= 0.0f) {
                    expandProgress = 0.0f;
                    isExpanding = false;
                }
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        if (ClientSetup.TOGGLE_EXPAND_KEY != null && ClientSetup.TOGGLE_EXPAND_KEY.consumeClick()) {
            if (Screen.hasControlDown()) {
                System.out.println("[HotbarExpand] Toggle expand key pressed");
                setExpanded(!targetExpanded);
            }
        }
        
        // 处理数字键
        handleNumberKeys();
    }

    // 在ClientTick中检测按键（用于非取消性操作）
    private static boolean[] keyWasDown = new boolean[10]; // 0-9
    
    private static void handleNumberKeys() {
        // 只在展开状态下处理
        if (!isExpanded() && !isExpanding) return;
        if (expandProgress < 0.3f) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        
        // 只更新keyWasDown状态，不再处理列表格子选择（避免黄框问题）
        for (int i = 1; i <= 9; i++) {
            int keyCode = 48 + i;
            keyWasDown[i] = org.lwjgl.glfw.GLFW.glfwGetKey(minecraft.getWindow().getWindow(), keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
    }
    
    // 使用InputEvent.Key处理Alt+数字键（可以取消事件）
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // 处理Ctrl+数字键切换布局（不需要展开状态）
        int key = event.getKey();
        if (key >= 49 && key <= 52 && Screen.hasControlDown()) { // Ctrl+1,2,3,4
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) { // 只在游戏界面处理
                int layoutNum = key - 49; // 0,1,2,3 对应 1,2,3,4
                HotbarConfig.LayoutMode newLayout;
                switch (layoutNum) {
                    case 0:
                        newLayout = HotbarConfig.LayoutMode.ONE_X_ONE;
                        break;
                    case 1:
                        newLayout = HotbarConfig.LayoutMode.ONE_X_TWO;
                        break;
                    case 2:
                        newLayout = HotbarConfig.LayoutMode.TWO_X_ONE;
                        break;
                    case 3:
                        newLayout = HotbarConfig.LayoutMode.TWO_X_TWO;
                        break;
                    default:
                        return;
                }
                HotbarConfig.setLayout(newLayout);
                System.out.println("[HotbarExpand] Ctrl+" + (layoutNum + 1) + " switched layout to: " + newLayout.displayName);

                // 取消事件
                try {
                    java.lang.reflect.Method method = event.getClass().getMethod("setCanceled", boolean.class);
                    method.invoke(event, true);
                } catch (Exception e) {
                }
                return;
            }
        }

        // 只在展开状态下处理其他按键
        if (!isExpanded() && !isExpanding) return;
        if (expandProgress < 0.3f) return;

        // 数字键1-9 (GLFW_KEY_1 = 49)
        if (key >= 49 && key <= 57) {
            // 检查是否按下了Alt键
            if (Screen.hasAltDown()) {
                // 获取当前滚动偏移和实际快捷栏数量
                int scrollOffset = getScrollOffsetFromGUI();
                int maxHotbars = getMaxHotbarsFromGUI();
                int visibleHotbars = Math.min(9, maxHotbars - scrollOffset);

                // 获取按下的数字键在可见列表中的位置（0-8）
                int visibleIndex = key - 49; // 0-8

                // 检查是否在可见范围内
                if (visibleIndex < visibleHotbars) {
                    // 计算实际的快捷栏索引
                    int targetIndex = scrollOffset + visibleIndex;
                    System.out.println("[HotbarExpand] Alt+Number key: " + (visibleIndex + 1) + " in visible list, actual hotbar: " + (targetIndex + 1));
                    setCurrentHotbarIndex(targetIndex);
                    // 不设置selectedSlotInList，避免黄框残留
                }

                // 取消事件，阻止原版物品选择
                try {
                    java.lang.reflect.Method method = event.getClass().getMethod("setCanceled", boolean.class);
                    method.invoke(event, true);
                } catch (Exception e) {
                }
            } else {
                // 不按Alt时，如果展开状态，也取消数字键事件，防止影响物品栏
                // 但只在游戏界面（没有打开其他屏幕）时处理
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.screen == null) {
                    try {
                        java.lang.reflect.Method method = event.getClass().getMethod("setCanceled", boolean.class);
                        method.invoke(event, true);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!isExpanded()) return;
        if (event.getAction() != InputConstants.PRESS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.screen != null) return;
        if (!isPickItemButton(event.getButton())) return;

        System.out.println("[HotbarExpand] Overlay pick click detected: button=" + event.getButton() + ", screen=" + minecraft.screen + ", expanded=" + isExpanded());

        ItemStack targetItem = getPickBlockItem();
        if (targetItem.isEmpty()) {
            System.out.println("[HotbarExpand] Overlay pick click targetItem is empty");
            return;
        }

        boolean found = HotbarManager.findAndSelectItem(targetItem);
        System.out.println("[HotbarExpand] Overlay pick click findAndSelectItem returned: " + found + ", target=" + targetItem.getItem());
        if (found) {
            event.setCanceled(true);
            System.out.println("[HotbarExpand] Overlay pick click canceled vanilla behavior");
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        // 非1x1模式下，完全接管滚轮行为（包括收起状态）
        if (HotbarConfig.getLayout() != HotbarConfig.LayoutMode.ONE_X_ONE) {
            // 收起状态下也处理滚轮，进行组内循环
            // 只有在展开/收起动画过程中才不处理
            if (isExpanding && expandProgress > 0.0f && expandProgress < 0.3f) return;
            
            double scrollDelta = event.getScrollDeltaY();
            
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            
            int selectedSlot = minecraft.player.getInventory().selected;
            int maxHotbars = HotbarManager.getHotbarCount();
            HotbarConfig.LayoutMode layout = HotbarConfig.getLayout();
            int currentIndex = getCurrentHotbarIndex();
            
            if (Screen.hasAltDown()) {
                // 按住Alt时直接切换快捷栏（只在展开状态）
                if (isExpanded()) {
                    handleAltScroll(minecraft, scrollDelta, currentIndex, maxHotbars, event);
                }
            } else {
                // 不按Alt时：根据布局模式处理滚动（收起和展开都处理）
                handleScrollByLayout(event, minecraft, scrollDelta, selectedSlot, currentIndex, maxHotbars, layout);
            }
            return;
        }
        
        // 1x1模式下：收起时滚轮跨组切换，展开时Alt+滚轮跨组切换
        if (!isExpanded() && !isExpanding) {
            // 1x1收起状态：滚轮跨组切换
            double scrollDelta = event.getScrollDeltaY();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            
            int selectedSlot = minecraft.player.getInventory().selected;
            int maxHotbars = HotbarManager.getHotbarCount();
            int currentIndex = getCurrentHotbarIndex();
            
            // 处理向上滚动（slot减小到0）- 切换到上一组（上一个快捷栏）
            if (scrollDelta > 0 && selectedSlot == 0) {
                int newIndex = currentIndex - 1;
                if (newIndex < 0) newIndex = maxHotbars - 1;
                minecraft.player.getInventory().selected = 8;
                setCurrentHotbarIndex(newIndex);
                System.out.println("[HotbarExpand] 1x1 collapsed scroll up to previous hotbar: " + (newIndex + 1));
                event.setCanceled(true);
            }
            // 处理向下滚动（slot增大到8）- 切换到下一组（下一个快捷栏）
            else if (scrollDelta < 0 && selectedSlot == 8) {
                int newIndex = currentIndex + 1;
                if (newIndex >= maxHotbars) newIndex = 0;
                minecraft.player.getInventory().selected = 0;
                setCurrentHotbarIndex(newIndex);
                System.out.println("[HotbarExpand] 1x1 collapsed scroll down to next hotbar: " + (newIndex + 1));
                event.setCanceled(true);
            }
        } else {
            // 1x1展开状态：Alt+滚轮跨组切换
            if (expandProgress < 0.3f) return;
            
            double scrollDelta = event.getScrollDeltaY();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            
            int selectedSlot = minecraft.player.getInventory().selected;
            int maxHotbars = getMaxHotbarsFromGUI();
            int currentIndex = getCurrentHotbarIndex();
            
            if (Screen.hasAltDown()) {
                handleAltScroll(minecraft, scrollDelta, currentIndex, maxHotbars, event);
            }
        }
    }
    
    private static void handleAltScroll(Minecraft minecraft, double scrollDelta, int currentIndex, int maxHotbars, InputEvent.MouseScrollingEvent event) {
        int newIndex;
        if (scrollDelta > 0) {
            newIndex = currentIndex - 1;
            if (newIndex < 0) newIndex = maxHotbars - 1;
        } else {
            newIndex = currentIndex + 1;
            if (newIndex >= maxHotbars) newIndex = 0;
        }
        System.out.println("[HotbarExpand] Alt+Scroll switching to hotbar: " + (newIndex + 1));
        setCurrentHotbarIndex(newIndex);
        autoAdjustScrollOffset(newIndex, maxHotbars);
        event.setCanceled(true);
    }
    
    /**
     * 根据布局模式处理滚动
     */
    private static void handleScrollByLayout(InputEvent.MouseScrollingEvent event, Minecraft minecraft, double scrollDelta,
            int selectedSlot, int currentIndex, int maxHotbars, HotbarConfig.LayoutMode layout) {

        // 计算当前组的大小（使用补齐后的逻辑大小）
        int layoutSize = layout.getTotalHotbars();
        int groupStart = (currentIndex / layoutSize) * layoutSize;

        // 计算当前在组内的位置（考虑补齐情况）
        int currentHotbarInGroup = -1;
        for (int i = 0; i < layoutSize; i++) {
            if (getPaddedHotbarIndex(groupStart + i, maxHotbars) == currentIndex) {
                currentHotbarInGroup = i;
                break;
            }
        }
        // 如果找不到，默认在第一个位置
        if (currentHotbarInGroup < 0) {
            currentHotbarInGroup = 0;
        }

        // 处理向上滚动（slot减小）
        if (scrollDelta > 0) {
            if (selectedSlot > 0) {
                // 在当前快捷栏内正常滚动
                int newSlot = selectedSlot - 1;
                minecraft.player.getInventory().selected = newSlot;
                event.setCanceled(true);
                return;
            } else if (selectedSlot == 0) {
                // 已经在第一个槽位，检查是否是组内第一个快捷栏
                if (currentHotbarInGroup > 0) {
                    // 不是组内第一个，切换到组内上一个快捷栏的最后一个槽位
                    int newHotbarIndex = getPaddedHotbarIndex(groupStart + currentHotbarInGroup - 1, maxHotbars);
                    minecraft.player.getInventory().selected = 8;
                    setCurrentHotbarIndex(newHotbarIndex);
                    System.out.println("[HotbarExpand] Scroll up to previous hotbar in group: " + (newHotbarIndex + 1));
                } else {
                    // 是组内第一个快捷栏
                    if (!isExpanded()) {
                        // 列表收起时：循环到组内最后一个快捷栏（使用补齐逻辑）
                        int newHotbarIndex = getPaddedHotbarIndex(groupStart + layoutSize - 1, maxHotbars);
                        minecraft.player.getInventory().selected = 8;
                        setCurrentHotbarIndex(newHotbarIndex);
                        System.out.println("[HotbarExpand] Group cycle to last hotbar: " + (newHotbarIndex + 1));
                    } else {
                        // 列表展开时：切换到上一组
                        int newGroupStart;
                        if (groupStart == 0) {
                            // 当前是第一组，循环到最后一组
                            newGroupStart = ((maxHotbars - 1) / layoutSize) * layoutSize;
                        } else {
                            newGroupStart = groupStart - layoutSize;
                        }
                        // 如果上一组是补齐组，从列表开头开始
                        if (newGroupStart >= maxHotbars) {
                            newGroupStart = 0;
                        }
                        int newHotbarIndex = getPaddedHotbarIndex(newGroupStart + layoutSize - 1, maxHotbars);
                        minecraft.player.getInventory().selected = 8;
                        setCurrentHotbarIndex(newHotbarIndex);
                        System.out.println("[HotbarExpand] Switch to previous group, hotbar: " + (newHotbarIndex + 1));
                    }
                }
                event.setCanceled(true);
                return;
            }
        }
        // 处理向下滚动（slot增大）
        else if (scrollDelta < 0) {
            if (selectedSlot < 8) {
                // 在当前快捷栏内正常滚动
                int newSlot = selectedSlot + 1;
                minecraft.player.getInventory().selected = newSlot;
                event.setCanceled(true);
                return;
            } else if (selectedSlot == 8) {
                // 已经在最后一个槽位，检查是否是组内最后一个快捷栏
                if (currentHotbarInGroup < layoutSize - 1) {
                    // 不是组内最后一个，切换到组内下一个快捷栏的第一个槽位
                    int newHotbarIndex = getPaddedHotbarIndex(groupStart + currentHotbarInGroup + 1, maxHotbars);
                    minecraft.player.getInventory().selected = 0;
                    setCurrentHotbarIndex(newHotbarIndex);
                    System.out.println("[HotbarExpand] Scroll down to next hotbar in group: " + (newHotbarIndex + 1));
                } else {
                    // 是组内最后一个快捷栏
                    if (!isExpanded()) {
                        // 列表收起时：循环到组内第一个快捷栏（使用补齐逻辑）
                        int newHotbarIndex = getPaddedHotbarIndex(groupStart, maxHotbars);
                        minecraft.player.getInventory().selected = 0;
                        setCurrentHotbarIndex(newHotbarIndex);
                        System.out.println("[HotbarExpand] Group cycle to first hotbar: " + (newHotbarIndex + 1));
                    } else {
                        // 列表展开时：切换到下一组
                        int newGroupStart = groupStart + layoutSize;
                        // 如果超出范围或者是补齐组，从列表开头开始
                        if (newGroupStart >= maxHotbars) {
                            newGroupStart = 0;
                        }
                        int newHotbarIndex = getPaddedHotbarIndex(newGroupStart, maxHotbars);
                        minecraft.player.getInventory().selected = 0;
                        setCurrentHotbarIndex(newHotbarIndex);
                        System.out.println("[HotbarExpand] Switch to next group, hotbar: " + (newHotbarIndex + 1));
                    }
                }
                event.setCanceled(true);
                return;
            }
        }
    }
    
    /**
     * 获取当前显示的快捷栏范围
     * @return int[2] {minIdx, maxIdx}
     */
    private static int[] getVisibleHotbarRange(int currentIndex, int maxHotbars, HotbarConfig.LayoutMode layout) {
        int layoutSize = layout.getTotalHotbars();
        
        switch (layout) {
            case ONE_X_ONE:
                // 1x1: 只显示当前快捷栏
                return new int[]{currentIndex, currentIndex};
            case ONE_X_TWO:
            case TWO_X_ONE:
                // 1x2和2x1: 显示2个快捷栏
                int groupStart = (currentIndex / 2) * 2;
                int groupEnd = Math.min(groupStart + 1, maxHotbars - 1);
                return new int[]{groupStart, groupEnd};
            case TWO_X_TWO:
                // 2x2: 显示4个快捷栏
                int groupStart4 = (currentIndex / 4) * 4;
                int groupEnd4 = Math.min(groupStart4 + 3, maxHotbars - 1);
                return new int[]{groupStart4, groupEnd4};
            default:
                return new int[]{currentIndex, currentIndex};
        }
    }
    
    /**
     * 自动调整滚动偏移，确保指定的快捷栏在可见区域内
     */
    private static void autoAdjustScrollOffset(int hotbarIndex, int maxHotbars) {
        if (maxHotbars <= 9) return; // 不需要滚动
        
        int scrollOffset = getScrollOffsetFromGUI();
        int visibleHotbars = 9;
        
        // 如果选中的快捷栏在可见区域上方，向上滚动
        if (hotbarIndex < scrollOffset) {
            setScrollOffsetToGUI(hotbarIndex);
        }
        // 如果选中的快捷栏在可见区域下方，向下滚动
        else if (hotbarIndex >= scrollOffset + visibleHotbars) {
            setScrollOffsetToGUI(hotbarIndex - visibleHotbars + 1);
        }
    }
    
    /**
     * 设置滚动偏移到HotbarInventoryScreen
     */
    private static void setScrollOffsetToGUI(int offset) {
        try {
            Class<?> guiClass = Class.forName("com.example.hotbarexpand.client.gui.HotbarInventoryScreen");
            java.lang.reflect.Method method = guiClass.getMethod("setScrollOffset", int.class);
            method.invoke(null, offset);
        } catch (Exception e) {
            // 忽略错误
        }
    }

    // 保持原版hotbar始终渲染，不再取消

    /**
     * 取消原版快捷栏渲染（非1x1模式下）
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (HotbarConfig.getLayout() == HotbarConfig.LayoutMode.ONE_X_ONE) return;

        // 取消原版快捷栏渲染
        if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.setCanceled(true);
        }
    }

    // 使用 RenderGuiEvent.Post 渲染（在所有GUI层之后）
    private static boolean isPickItemButton(int button) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return button == 2;
        }
        InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(button);
        return minecraft.options.keyPickItem.isActiveAndMatches(mouseKey) || button == 2;
    }

    private static ItemStack getPickBlockItem() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return ItemStack.EMPTY;
        var hitResult = minecraft.hitResult;
        if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            var blockPos = blockHit.getBlockPos();
            var blockState = minecraft.level.getBlockState(blockPos);
            var block = blockState.getBlock();
            ItemStack pickBlock = block.getCloneItemStack(minecraft.level, blockPos, blockState);
            if (!pickBlock.isEmpty()) {
                return pickBlock;
            }
            var item = block.asItem();
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        }

        var pickResult = minecraft.player.pick(20.0, 0.0F, false);
        if (pickResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            var blockPos = blockHit.getBlockPos();
            var blockState = minecraft.level.getBlockState(blockPos);
            var block = blockState.getBlock();
            ItemStack pickBlock = block.getCloneItemStack(minecraft.level, blockPos, blockState);
            if (!pickBlock.isEmpty()) {
                return pickBlock;
            }
            var item = block.asItem();
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.options.hideGui) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int hotbarWidth = 182;
        int hotbarHeight = 22;
        int offhandWidth = 29;

        int centerX = screenWidth / 2;
        int baseY = screenHeight - hotbarHeight;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int currentIndex = getCurrentHotbarIndex();
        float smoothExpandProgress = HotbarManager.getSmoothProgress(expandProgress);

        // 在原版快捷栏和副手框之间渲染白条（显示当前在列表中的位置）
        if (smoothExpandProgress > 0.01f) {
            renderPositionIndicator(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
        }
        
        // 根据配置渲染主快捷栏布局（无论列表是否展开都渲染）
        renderMainHotbarByLayout(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
        
        // 只渲染右下角的快捷栏列表（垂直排列，固定1-9顺序）
        if (smoothExpandProgress > 0.01f) {
            renderHotbarListAtBottomRight(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
        }
        
        RenderSystem.disableBlend();
    }
    
    /**
     * 根据配置的布局模式渲染主快捷栏
     */
    private static void renderMainHotbarByLayout(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        HotbarConfig.LayoutMode layout = HotbarConfig.getLayout();
        
        switch (layout) {
            case ONE_X_ONE:
                // 1x1: 原版样式，只渲染当前快捷栏
                renderMainHotbarWithOffhand(guiGraphics, minecraft, screenWidth / 2, screenHeight - hotbarHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex);
                break;
            case ONE_X_TWO:
                // 1x2: 2个快捷栏垂直排列
                renderOneXTwoLayout(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
                break;
            case TWO_X_ONE:
                // 2x1: 2个快捷栏水平排列
                renderTwoXOneLayout(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
                break;
            case TWO_X_TWO:
                // 2x2: 4个快捷栏网格排列
                renderTwoXTwoLayout(guiGraphics, minecraft, screenWidth, screenHeight, hotbarWidth, hotbarHeight, offhandWidth, currentIndex, smoothExpandProgress);
                break;
        }
    }
    
    /**
     * 渲染1x2布局（2个快捷栏垂直排列）
     */
    private static void renderOneXTwoLayout(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        int centerX = screenWidth / 2;
        int baseY = screenHeight - hotbarHeight * 2;
        int maxHotbars = HotbarManager.getHotbarCount();
        int groupSize = HotbarConfig.LayoutMode.ONE_X_TWO.getTotalHotbars(); // 2

        // 计算当前组的起始索引（固定显示当前组），使用补齐逻辑
        int groupStart = (currentIndex / groupSize) * groupSize;
        int hotbar1Idx = getPaddedHotbarIndex(groupStart, maxHotbars);
        int hotbar2Idx = getPaddedHotbarIndex(groupStart + 1, maxHotbars);

        // 渲染第一个快捷栏 - 副手在左侧
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, centerX, baseY, hotbarWidth, hotbarHeight, offhandWidth, hotbar1Idx, true);

        // 渲染第二个快捷栏 - 副手在右侧
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, centerX, baseY + hotbarHeight, hotbarWidth, hotbarHeight, offhandWidth, hotbar2Idx, false);
    }
    
    /**
     * 渲染2x1布局（2个快捷栏水平排列）
     */
    private static void renderTwoXOneLayout(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        int centerX = screenWidth / 2;
        int baseY = screenHeight - hotbarHeight;
        int maxHotbars = HotbarManager.getHotbarCount();
        int groupSize = HotbarConfig.LayoutMode.TWO_X_ONE.getTotalHotbars(); // 2

        // 计算当前组的起始索引（固定显示当前组），使用补齐逻辑
        int groupStart = (currentIndex / groupSize) * groupSize;
        int hotbar1Idx = getPaddedHotbarIndex(groupStart, maxHotbars);
        int hotbar2Idx = getPaddedHotbarIndex(groupStart + 1, maxHotbars);

        // 两个快捷栏水平排列，中间留间隙
        // 布局：副手(左) + 快捷栏1 + gap + 快捷栏2 + 副手(右)
        int gap = 4;
        int totalWidth = offhandWidth + hotbarWidth + gap + hotbarWidth + offhandWidth;
        int startX = centerX - totalWidth / 2;

        // 渲染第一个快捷栏（左侧）- 副手在左侧
        // 快捷栏1的中心位置 = startX + 副手宽度 + 快捷栏宽度/2
        int hotbar1CenterX = startX + offhandWidth + hotbarWidth / 2;
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, hotbar1CenterX, baseY, hotbarWidth, hotbarHeight, offhandWidth, hotbar1Idx, true);

        // 渲染第二个快捷栏（右侧）- 副手在右侧
        // 快捷栏2的中心位置 = startX + 副手宽度 + 快捷栏宽度 + gap + 快捷栏宽度/2
        int hotbar2CenterX = startX + offhandWidth + hotbarWidth + gap + hotbarWidth / 2;
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, hotbar2CenterX, baseY, hotbarWidth, hotbarHeight, offhandWidth, hotbar2Idx, false);
    }
    
    /**
     * 渲染2x2布局（4个快捷栏网格排列）
     */
    private static void renderTwoXTwoLayout(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        int centerX = screenWidth / 2;
        int baseY = screenHeight - hotbarHeight * 2;
        int hotbarCount = HotbarManager.getHotbarCount();
        int groupSize = HotbarConfig.LayoutMode.TWO_X_TWO.getTotalHotbars(); // 4

        // 计算当前组的起始索引（固定显示当前组），使用补齐逻辑
        int groupStart = (currentIndex / groupSize) * groupSize;
        int[] hotbarIndices = new int[4];
        for (int i = 0; i < 4; i++) {
            hotbarIndices[i] = getPaddedHotbarIndex(groupStart + i, hotbarCount);
        }
        
        // 网格排列：2列2行
        int gap = 4;
        int totalWidth = hotbarWidth * 2 + gap;
        int startX = centerX - totalWidth / 2;
        
        // 第一行 - 左侧快捷栏副手在左，右侧快捷栏副手在右
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, startX + hotbarWidth / 2, baseY, hotbarWidth, hotbarHeight, offhandWidth, hotbarIndices[0], true);
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, startX + hotbarWidth + gap + hotbarWidth / 2, baseY, hotbarWidth, hotbarHeight, offhandWidth, hotbarIndices[1], false);
        
        // 第二行 - 左侧快捷栏副手在左，右侧快捷栏副手在右
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, startX + hotbarWidth / 2, baseY + hotbarHeight, hotbarWidth, hotbarHeight, offhandWidth, hotbarIndices[2], true);
        renderHotbarWithOffhandForLayout(guiGraphics, minecraft, startX + hotbarWidth + gap + hotbarWidth / 2, baseY + hotbarHeight, hotbarWidth, hotbarHeight, offhandWidth, hotbarIndices[3], false);
    }
    
    /**
     * 为布局渲染快捷栏（带副手）
     * @param offhandOnLeft true表示副手在左侧，false表示副手在右侧
     */
    private static void renderHotbarWithOffhandForLayout(GuiGraphics guiGraphics, Minecraft minecraft, int centerX, int y, int hotbarWidth, int hotbarHeight, int offhandWidth, int hotbarIdx, boolean offhandOnLeft) {
        int itemXOffset = 3;
        int offhandYOffset = 0;
        
        // 渲染副手（左右间距对称，都是2像素）
        if (offhandOnLeft) {
            int offhandX = centerX - hotbarWidth / 2 - offhandWidth - 2;
            renderOffhandForLayout(guiGraphics, minecraft, offhandX, y, offhandWidth, hotbarIdx);
        } else {
            int offhandX = centerX + hotbarWidth / 2 + 10;
            renderOffhandForLayout(guiGraphics, minecraft, offhandX, y, offhandWidth, hotbarIdx);
        }
        
        // 渲染快捷栏背景
        renderHotbarBackground(guiGraphics, centerX - hotbarWidth / 2, y);

        // 渲染选中框（只显示当前选中的快捷栏）
        boolean isCurrent = hotbarIdx == getCurrentHotbarIndex();
        if (isCurrent) {
            int selectedSlot = minecraft.player.getInventory().selected;
            renderHotbarSelection(guiGraphics, centerX - hotbarWidth / 2, y, selectedSlot);
        }

        // 渲染编号（当前快捷栏用黄色，其他用白色）
        int numberColor = isCurrent ? 0xFFFF00 : 0xFFFFFF;
        String numberStr = String.valueOf(hotbarIdx + 1);
        int textWidth = minecraft.font.width(numberStr);
        int numberX = offhandOnLeft ? 
            centerX - hotbarWidth / 2 - offhandWidth - 20 + (14 - textWidth) / 2 :
            centerX + hotbarWidth / 2 + offhandWidth + 6 + (14 - textWidth) / 2;
        guiGraphics.drawString(minecraft.font, numberStr, numberX, y + 6, numberColor);
        
        // 渲染物品
        renderItems(guiGraphics, centerX - hotbarWidth / 2, y, hotbarIdx);
    }
    
    /**
     * 为布局渲染选中框
     */
    private static void renderHotbarSelectionForLayout(GuiGraphics guiGraphics, int x, int y, int hotbarIdx, Minecraft minecraft) {
        if (hotbarIdx == getCurrentHotbarIndex()) {
            int selectedSlot = minecraft.player.getInventory().selected;
            renderHotbarSelection(guiGraphics, x, y, selectedSlot);
        }
    }
    
    /**
     * 为布局渲染副手
     */
    private static void renderOffhandForLayout(GuiGraphics guiGraphics, Minecraft minecraft, int x, int y, int offhandWidth, int hotbarIdx) {
        ItemStack offhandItem = HotbarManager.getOffhandItem(hotbarIdx);
        boolean isLeftHanded = minecraft.options.mainHand().get() == net.minecraft.world.entity.HumanoidArm.LEFT;
        int itemXOffset = 3;
        int offhandYOffset = 0;
        
        if (isLeftHanded) {
            guiGraphics.blit(HOTBAR_OFFHAND_RIGHT_LOCATION, x, y + offhandYOffset, 0, 0, 29, 24, 29, 24);
        } else {
            guiGraphics.blit(HOTBAR_OFFHAND_LEFT_LOCATION, x, y + offhandYOffset, 0, 0, 29, 24, 29, 24);
        }
        
        if (!offhandItem.isEmpty()) {
            guiGraphics.renderItem(offhandItem, x + itemXOffset, y + offhandYOffset + 4);
            guiGraphics.renderItemDecorations(minecraft.font, offhandItem, x + itemXOffset, y + offhandYOffset + 4);
        }
    }
    
    /**
     * 在原版快捷栏和副手框之间渲染白条指示器
     * 显示当前快捷栏在列表中的位置
     * 限宽6格，纵向无限排列（即每列6个，向右新开列）
     */
    private static void renderPositionIndicator(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        boolean isLeftHanded = minecraft.options.mainHand().get() == net.minecraft.world.entity.HumanoidArm.LEFT;
        
        int centerX = screenWidth / 2;
        int baseY = screenHeight - hotbarHeight;
        
        // 获取实际快捷栏数量
        int maxHotbars = getMaxHotbarsFromGUI();
        
        // 白条的位置：在快捷栏和副手框之间
        int barWidth = 4; // 白条宽度
        int barHeight = 3; // 每个小段高度
        int barGapX = 2; // 横向间隔
        int barGapY = 1; // 纵向间隔
        int gap = 2; // 与快捷栏/副手框的间隙
        int maxPerCol = 6; // 每列最多6格
        
        // 计算需要多少列
        int numCols = (maxHotbars + maxPerCol - 1) / maxPerCol; // 向上取整
        
        // 计算白条区域的总宽度
        int totalWidth = numCols * barWidth + (numCols - 1) * barGapX;
        
        int baseBarX;
        if (isLeftHanded) {
            // 左手模式：副手在右边，白条在快捷栏右侧
            baseBarX = centerX + hotbarWidth / 2 + gap;
        } else {
            // 右手模式：副手在左边，白条在副手框右侧（即快捷栏左侧）
            // 从右向左排列，最右边是第一列
            baseBarX = centerX - hotbarWidth / 2 - offhandWidth - gap - totalWidth;
        }
        
        // 计算起始Y位置（垂直居中）
        int colHeight = maxPerCol * barHeight + (maxPerCol - 1) * barGapY;
        int startY = baseY + (hotbarHeight - colHeight) / 2;
        
        // 渲染所有小段（按列渲染，纵向排列）
        for (int i = 0; i < maxHotbars; i++) {
            int col = i / maxPerCol; // 列索引
            int row = i % maxPerCol; // 行索引
            
            int barX;
            if (isLeftHanded) {
                // 左手模式：从左向右排列列
                barX = baseBarX + col * (barWidth + barGapX);
            } else {
                // 右手模式：从左向右排列列（最右边是第一列）
                barX = baseBarX + col * (barWidth + barGapX);
            }
            
            int segmentY = startY + row * (barHeight + barGapY);
            int color = (i == currentIndex) ? 0xFFFFFFFF : 0xFF808080; // 当前位置白色，其他灰色
            guiGraphics.fill(barX, segmentY, barX + barWidth, segmentY + barHeight, color);
        }
    }
    
    // 渲染主快捷栏（带副手格和编号）
    private static void renderMainHotbarWithOffhand(GuiGraphics guiGraphics, Minecraft minecraft, int centerX, int baseY, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIdx) {
        boolean isLeftHanded = minecraft.options.mainHand().get() == net.minecraft.world.entity.HumanoidArm.LEFT;
        
        // 渲染副手格
        int offhandYOffset = 0;
        int itemXOffset = 3;
        if (isLeftHanded) {
            int offhandX = centerX + hotbarWidth / 2 + 4;
            guiGraphics.blit(HOTBAR_OFFHAND_RIGHT_LOCATION, offhandX, baseY + offhandYOffset, 0, 0, 29, 24, 29, 24);
            ItemStack offhandItem = minecraft.player.getOffhandItem();
            if (!offhandItem.isEmpty()) {
                guiGraphics.renderItem(offhandItem, offhandX + itemXOffset, baseY + offhandYOffset + 4);
                guiGraphics.renderItemDecorations(minecraft.font, offhandItem, offhandX + itemXOffset, baseY + offhandYOffset + 4);
            }
        } else {
            int offhandX = centerX - hotbarWidth / 2 - offhandWidth;
            guiGraphics.blit(HOTBAR_OFFHAND_LEFT_LOCATION, offhandX, baseY + offhandYOffset, 0, 0, 29, 24, 29, 24);
            ItemStack offhandItem = minecraft.player.getOffhandItem();
            if (!offhandItem.isEmpty()) {
                guiGraphics.renderItem(offhandItem, offhandX + itemXOffset, baseY + offhandYOffset + 4);
                guiGraphics.renderItemDecorations(minecraft.font, offhandItem, offhandX + itemXOffset, baseY + offhandYOffset + 4);
            }
        }
        
        // 渲染主快捷栏背景
        renderHotbarBackground(guiGraphics, centerX - hotbarWidth / 2, baseY);
        
        // 渲染选中框
        renderHotbarSelection(guiGraphics, centerX - hotbarWidth / 2, baseY, minecraft.player.getInventory().selected);
        
        // 渲染编号（黄色）
        int numberX = isLeftHanded ? centerX + hotbarWidth / 2 + 27 : centerX - hotbarWidth / 2 - 37;
        guiGraphics.drawString(minecraft.font, String.valueOf(currentIdx + 1), numberX, baseY + 6, 0xFFFF00);
        
        // 渲染物品
        renderItems(guiGraphics, centerX - hotbarWidth / 2, baseY, currentIdx);
    }
    
    // 在屏幕右下角渲染快捷栏列表（垂直排列，固定1-9顺序，从上到下）
    // 最多显示9个，超过9个时通过滚动查看
    private static void renderHotbarListAtBottomRight(GuiGraphics guiGraphics, Minecraft minecraft, int screenWidth, int screenHeight, int hotbarWidth, int hotbarHeight, int offhandWidth, int currentIndex, float smoothExpandProgress) {
        boolean isLeftHanded = minecraft.options.mainHand().get() == net.minecraft.world.entity.HumanoidArm.LEFT;
        
        // 计算起始位置（绝对右下角，紧贴右边缘）
        int listCenterX = screenWidth - hotbarWidth / 2;
        
        // 获取实际快捷栏数量和滚动偏移
        int maxHotbars = getMaxHotbarsFromGUI();
        int scrollOffset = getScrollOffsetFromGUI();
        int visibleHotbars = Math.min(9, maxHotbars);
        
        // 固定1-9顺序，从上到下排列
        // 根据滚动偏移显示不同的9个快捷栏
        for (int displayIdx = 0; displayIdx < visibleHotbars; displayIdx++) {
            int actualIdx = scrollOffset + displayIdx; // 实际的快捷栏索引
            if (actualIdx >= maxHotbars) break;
            
            // 从上到下：displayIdx=0在最上面
            // 从屏幕底部开始计算位置（右下角）
            int targetY = screenHeight - (visibleHotbars - displayIdx) * hotbarHeight;
            
            // 动画：从下方滑入（延迟效果：下面的先出现）
            float slotProgress = Math.min(1.0f, Math.max(0.0f, smoothExpandProgress * 1.5f - (visibleHotbars - 1 - displayIdx) * 0.08f));
            int startY = screenHeight + hotbarHeight;
            int renderY = (int) (startY + (targetY - startY) * HotbarManager.getSmoothProgress(slotProgress));
            
            int offhandYOffset = 0;
            int itemXOffset = 3;
            
            // 只有当前快捷栏（currentIndex）用黄色高亮
            boolean isCurrentHotbar = (actualIdx == currentIndex);
            boolean isHighlighted = isCurrentHotbar;

            // 渲染副手格（高亮项用黄色）
            if (isHighlighted) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 0.0f, 1.0f);
            }
            if (isLeftHanded) {
                guiGraphics.blit(HOTBAR_OFFHAND_RIGHT_LOCATION, listCenterX + hotbarWidth / 2 + 4, renderY + offhandYOffset, 0, 0, 29, 24, 29, 24);
            } else {
                guiGraphics.blit(HOTBAR_OFFHAND_LEFT_LOCATION, listCenterX - hotbarWidth / 2 - offhandWidth, renderY + offhandYOffset, 0, 0, 29, 24, 29, 24);
            }
            if (isHighlighted) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            
            // 渲染快捷栏背景（当前快捷栏或选中项用黄色高亮）
            if (isHighlighted) {
                renderHotbarBackgroundWithColor(guiGraphics, listCenterX - hotbarWidth / 2, renderY, 1.0f, 1.0f, 0.0f);
            } else {
                renderHotbarBackground(guiGraphics, listCenterX - hotbarWidth / 2, renderY);
            }
            
            // 渲染编号（白色，高亮项用黄色）
            int numberColor = isHighlighted ? 0xFFFF00 : 0xFFFFFF;
            String numberStr = String.valueOf(actualIdx + 1);
            // 2位数居中渲染 - 编号在副手格左侧，居中显示
            int textWidth = minecraft.font.width(numberStr);
            // 副手格宽度29，编号在副手格左侧，向左90像素
            int numberXOffset = isLeftHanded ? 27 + (14 - textWidth) / 2 : -29 - 14 - 180 + (14 - textWidth) / 2;
            guiGraphics.drawString(minecraft.font, numberStr, listCenterX + hotbarWidth / 2 + numberXOffset, renderY + 6, numberColor);
            
            // 渲染物品
            renderItems(guiGraphics, listCenterX - hotbarWidth / 2, renderY, actualIdx);
            
            // 渲染副手物品
            renderOffhandItem(guiGraphics, minecraft, listCenterX, renderY, hotbarWidth, offhandWidth, isLeftHanded, itemXOffset, offhandYOffset, actualIdx);
            
            // 如果是当前快捷栏，渲染选中框（显示当前选中的物品槽位）
            if (isCurrentHotbar) {
                int selectedSlot = minecraft.player.getInventory().selected;
                renderHotbarSelection(guiGraphics, listCenterX - hotbarWidth / 2, renderY, selectedSlot);
            }
        }
        
        // 如果快捷栏数量超过9个，渲染滚动指示器（小白条）
        if (maxHotbars > 9) {
            renderScrollIndicator(guiGraphics, screenWidth, screenHeight, hotbarHeight, scrollOffset, maxHotbars, visibleHotbars, smoothExpandProgress);
        }
    }
    
    /**
     * 渲染滚动指示器（小白条）
     */
    private static void renderScrollIndicator(GuiGraphics guiGraphics, int screenWidth, int screenHeight, int hotbarHeight, int scrollOffset, int maxHotbars, int visibleHotbars, float smoothExpandProgress) {
        // 隐藏小白条（滚动指示器）- 备用代码，如需恢复请取消注释
        /*
        // 滚动指示器位置：在快捷栏列表的左侧，往右2像素
        int indicatorX = screenWidth - 182 - 8 - 4 + 10 - 5 + 2; // 快捷栏左侧留出空间，往右2像素
        int listHeight = visibleHotbars * hotbarHeight;
        int startY = screenHeight - listHeight;
        
        // 计算指示器高度和位置
        float ratio = (float) visibleHotbars / maxHotbars;
        int indicatorHeight = Math.max(8, (int)(listHeight * ratio));
        int maxScroll = maxHotbars - visibleHotbars;
        float scrollRatio = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int indicatorY = startY + (int)((listHeight - indicatorHeight) * scrollRatio);
        
        // 渲染指示器背景（灰色）
        guiGraphics.fill(indicatorX, startY, indicatorX + 3, startY + listHeight, 0x40808080);
        // 渲染指示器滑块（白色）
        guiGraphics.fill(indicatorX, indicatorY, indicatorX + 3, indicatorY + indicatorHeight, 0xFFFFFFFF);
        */
    }
    
    /**
     * 从HotbarInventoryScreen获取滚动偏移
     */
    private static int getScrollOffsetFromGUI() {
        try {
            Class<?> guiClass = Class.forName("com.example.hotbarexpand.client.gui.HotbarInventoryScreen");
            java.lang.reflect.Method method = guiClass.getMethod("getScrollOffset");
            return (int) method.invoke(null);
        } catch (Exception e) {
            return 0; // 默认从0开始
        }
    }
    
    /**
     * 从HotbarInventoryScreen获取最大快捷栏数量
     */
    private static int getMaxHotbarsFromGUI() {
        try {
            Class<?> guiClass = Class.forName("com.example.hotbarexpand.client.gui.HotbarInventoryScreen");
            java.lang.reflect.Field field = guiClass.getDeclaredField("maxHotbars");
            field.setAccessible(true);
            return (int) field.get(null);
        } catch (Exception e) {
            return 9; // 默认9个
        }
    }
    
    // 渲染副手物品
    private static void renderOffhandItem(GuiGraphics guiGraphics, Minecraft minecraft, int listCenterX, int renderY, int hotbarWidth, int offhandWidth, boolean isLeftHanded, int itemXOffset, int offhandYOffset, int hotbarIndex) {
        ItemStack offhandItem = HotbarManager.getOffhandItem(hotbarIndex);
        if (offhandItem.isEmpty()) return;
        
        if (isLeftHanded) {
            int offhandX = listCenterX + hotbarWidth / 2 + 4;
            guiGraphics.renderItem(offhandItem, offhandX + itemXOffset, renderY + offhandYOffset + 4);
            guiGraphics.renderItemDecorations(minecraft.font, offhandItem, offhandX + itemXOffset, renderY + offhandYOffset + 4);
        } else {
            int offhandX = listCenterX - hotbarWidth / 2 - offhandWidth;
            guiGraphics.renderItem(offhandItem, offhandX + itemXOffset, renderY + offhandYOffset + 4);
            guiGraphics.renderItemDecorations(minecraft.font, offhandItem, offhandX + itemXOffset, renderY + offhandYOffset + 4);
        }
    }

    private static void renderHotbarBackground(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(HOTBAR_LOCATION, x, y, 0, 0, 182, 22, 182, 22);
    }
    
    private static void renderHotbarBackgroundWithColor(GuiGraphics guiGraphics, int x, int y, float r, float g, float b) {
        RenderSystem.setShaderColor(r, g, b, 1.0f);
        guiGraphics.blit(HOTBAR_LOCATION, x, y, 0, 0, 182, 22, 182, 22);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    private static void renderHotbarSelection(GuiGraphics guiGraphics, int x, int y, int selectedSlot) {
        int selectX = x - 1 + selectedSlot * 20;
        int selectY = y - 1;
        guiGraphics.blit(HOTBAR_SELECTION_LOCATION, selectX, selectY, 0, 0, 24, 24, 24, 24);
    }

    private static void renderItems(GuiGraphics guiGraphics, int x, int y, int hotbarIndex) {
        List<ItemStack> hotbar = HotbarManager.getHotbar(hotbarIndex);
        for (int i = 0; i < 9; i++) {
            int slotX = x + 3 + i * 20;
            int slotY = y + 3;
            ItemStack itemStack = hotbar.get(i);
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, slotX, slotY);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, itemStack, slotX, slotY);
            }
        }
    }
}
