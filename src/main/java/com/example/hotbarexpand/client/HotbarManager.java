package com.example.hotbarexpand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

public class HotbarManager {
    // 9个快捷栏，每个包含9个物品槽
    private static final List<ItemStack>[] hotbars = new ArrayList[9];
    // 9个副手物品槽
    private static final ItemStack[] offhandSlots = new ItemStack[9];
    
    // 当前激活的快捷栏索引（0-8）
    private static int currentHotbarIndex = 0;
    
    // 展开状态
    private static boolean isExpanded = false;
    
    // 切换冷却，防止同步干扰
    private static int switchCooldown = 0;
    
    static {
        for (int i = 0; i < 9; i++) {
            hotbars[i] = new ArrayList<>();
            for (int j = 0; j < 9; j++) {
                hotbars[i].add(ItemStack.EMPTY);
            }
            offhandSlots[i] = ItemStack.EMPTY;
        }
    }
    
    public static List<ItemStack> getHotbar(int index) {
        if (index >= 0 && index < 9) {
            return hotbars[index];
        }
        return hotbars[0];
    }
    
    public static ItemStack getOffhandItem(int index) {
        if (index >= 0 && index < 9) {
            return offhandSlots[index];
        }
        return ItemStack.EMPTY;
    }
    
    public static void setOffhandItem(int index, ItemStack stack) {
        if (index >= 0 && index < 9) {
            offhandSlots[index] = stack.copy();
            
            // 如果设置的是当前快捷栏，同时更新玩家副手
            if (index == currentHotbarIndex) {
                Minecraft minecraft = Minecraft.getInstance();
                Player player = minecraft.player;
                if (player != null) {
                    player.getInventory().offhand.set(0, stack.copy());
                }
            }
        }
    }
    
    public static int getCurrentHotbarIndex() {
        return currentHotbarIndex;
    }
    
    public static void setCurrentHotbarIndex(int index) {
        if (index >= 0 && index < 9 && index != currentHotbarIndex) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;
            
            // 设置切换冷却，防止同步干扰（必须在保存前设置）
            switchCooldown = 20;
            
            // 先保存当前快捷栏
            savePlayerInventoryToHotbar(currentHotbarIndex);
            
            // 更新索引
            currentHotbarIndex = index;
            
            // 加载新快捷栏到玩家背包
            loadHotbarToPlayer(index);
            
            // 调试输出
            System.out.println("[HotbarExpand] Switched to hotbar " + (index + 1));
        }
    }
    
    public static void scrollToNext() {
        int nextIndex = (currentHotbarIndex + 1) % 9;
        setCurrentHotbarIndex(nextIndex);
    }
    
    public static void scrollToPrevious() {
        int prevIndex = (currentHotbarIndex - 1 + 9) % 9;
        setCurrentHotbarIndex(prevIndex);
    }
    
    private static void savePlayerInventoryToHotbar(int index) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        
        List<ItemStack> hotbar = hotbars[index];
        System.out.println("[HotbarExpand] Saving player inventory to hotbar " + (index + 1));
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            System.out.println("[HotbarExpand] Slot " + i + ": " + item.getItem().getDescription().getString() + " x" + item.getCount());
            hotbar.set(i, item.copy());
        }
        // 保存副手物品
        offhandSlots[index] = player.getOffhandItem().copy();
    }
    
    private static void loadHotbarToPlayer(int index) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        
        List<ItemStack> hotbar = hotbars[index];
        System.out.println("[HotbarExpand] Loading hotbar " + (index + 1) + " to player");
        for (int i = 0; i < 9; i++) {
            ItemStack item = hotbar.get(i);
            System.out.println("[HotbarExpand] Slot " + i + ": " + item.getItem().getDescription().getString() + " x" + item.getCount());
            player.getInventory().setItem(i, item.copy());
        }
        // 加载副手物品
        player.getInventory().offhand.set(0, offhandSlots[index].copy());
        System.out.println("[HotbarExpand] Hotbar loaded, switchCooldown = " + switchCooldown);
    }
    
    public static void savePlayerInventoryToCurrentHotbar() {
        savePlayerInventoryToHotbar(currentHotbarIndex);
    }
    
    public static boolean isExpanded() {
        return isExpanded;
    }
    
    public static void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
    
    public static float getSmoothProgress(float progress) {
        if (progress >= 1.0f) return 1.0f;
        if (progress <= 0.0f) return 0.0f;
        return 1.0f - (float) Math.pow(1.0f - progress, 3);
    }
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        
        // 减少切换冷却
        if (switchCooldown > 0) {
            switchCooldown--;
            return; // 冷却期间不进行同步
        }
        
        // 每5 tick同步一次
        if (minecraft.level != null && minecraft.level.getGameTime() % 5 == 0) {
            // 同步当前快捷栏
            List<ItemStack> currentHotbar = hotbars[currentHotbarIndex];
            for (int i = 0; i < 9; i++) {
                ItemStack playerItem = player.getInventory().getItem(i);
                if (!ItemStack.matches(currentHotbar.get(i), playerItem)) {
                    currentHotbar.set(i, playerItem.copy());
                }
            }
            // 同步副手物品
            ItemStack playerOffhand = player.getOffhandItem();
            if (!ItemStack.matches(offhandSlots[currentHotbarIndex], playerOffhand)) {
                offhandSlots[currentHotbarIndex] = playerOffhand.copy();
            }
            
            // 在展开状态下，同步所有非当前快捷栏的物品（用于显示更新）
            if (isExpanded) {
                for (int h = 0; h < 9; h++) {
                    if (h == currentHotbarIndex) continue; // 跳过当前快捷栏
                    
                    // 这里我们只是读取数据，不需要实际同步到玩家背包
                    // 因为非当前快捷栏的物品存储在 hotbars 数组中
                    // 当玩家打开背包GUI时，可以通过GUI修改这些物品
                }
            }
        }
    }
}
