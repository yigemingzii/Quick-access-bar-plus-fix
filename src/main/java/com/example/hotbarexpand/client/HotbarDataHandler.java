package com.example.hotbarexpand.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 快捷栏数据处理器 - 负责保存和加载快捷栏数据到本地文件
 * 
 * 存储结构：
 * .minecraft/hotbarexpand/
 * ├── worlds/                    # 单人世界数据
 * │   └── <世界名称>/
 * │       ├── hotbars.dat        # 快捷栏数据
 * │       └── config.dat         # 世界特定配置
 * ├── servers/                   # 多人服务器数据
 * │   └── <服务器地址>/
 * │       ├── hotbars.dat
 * │       └── config.dat
 * └── global.dat                 # 全局配置
 */
public class HotbarDataHandler {
    
    private static final String DATA_FOLDER = "hotbarexpand";
    private static final String WORLDS_FOLDER = "worlds";
    private static final String SERVERS_FOLDER = "servers";
    private static final String HOTBARS_FILE = "hotbars.dat";
    private static final String CONFIG_FILE = "config.dat";
    private static final String GLOBAL_FILE = "global.dat";
    
    // 当前世界/服务器的标识
    private static String currentWorldId = null;
    private static boolean isSinglePlayer = false;
    
    /**
     * 当客户端玩家加入服务器时加载数据
     */
    @SubscribeEvent
    public static void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        System.out.println("[HotbarExpand] Player logging in, initializing storage...");
        
        Minecraft minecraft = Minecraft.getInstance();
        
        // 确定当前世界/服务器标识
        if (minecraft.hasSingleplayerServer()) {
            // 单人游戏
            isSinglePlayer = true;
            String worldName = minecraft.getSingleplayerServer().getWorldData().getLevelName();
            currentWorldId = sanitizeFileName(worldName);
            System.out.println("[HotbarExpand] Single player world: " + worldName + " (ID: " + currentWorldId + ")");
        } else if (minecraft.getCurrentServer() != null) {
            // 多人游戏
            isSinglePlayer = false;
            String serverAddress = minecraft.getCurrentServer().ip;
            currentWorldId = sanitizeFileName(serverAddress);
            System.out.println("[HotbarExpand] Multiplayer server: " + serverAddress + " (ID: " + currentWorldId + ")");
        } else {
            System.out.println("[HotbarExpand] Unknown game mode, cannot determine storage location");
            return;
        }
        
        // 确保存储目录存在
        ensureStorageDirectory();
        
        // 加载数据
        loadHotbarData();
        
        // 初始化栏追踪
        System.out.println("[HotbarExpand] Hotbar tracking initialized");
    }
    
    /**
     * 当客户端玩家离开服务器时保存数据
     */
    @SubscribeEvent
    public static void onPlayerLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        System.out.println("[HotbarExpand] Player logging out, saving hotbar data...");
        saveHotbarData();
        currentWorldId = null;
    }
    
    /**
     * 保存快捷栏数据到文件
     */
    public static void saveHotbarData() {
        if (currentWorldId == null) {
            System.out.println("[HotbarExpand] No world ID, cannot save data");
            return;
        }
        
        try {
            File dataDir = getDataDirectory();
            if (dataDir == null) {
                System.out.println("[HotbarExpand] Cannot get data directory");
                return;
            }
            
            // 保存快捷栏数据
            saveHotbarsData(dataDir);
            
            // 保存配置数据
            saveConfigData(dataDir);
            
            System.out.println("[HotbarExpand] Data saved to " + dataDir.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error saving hotbar data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存快捷栏数据
     */
    private static void saveHotbarsData(File dataDir) {
        try {
            File dataFile = new File(dataDir, HOTBARS_FILE);
            CompoundTag rootTag = new CompoundTag();
            
            // 保存版本信息（用于未来兼容性）
            rootTag.putInt("Version", 1);
            rootTag.putString("ModVersion", "1.0.0");
            
            // 保存当前选中的快捷栏索引
            rootTag.putInt("CurrentHotbarIndex", HotbarManager.getCurrentHotbarIndex());
            
            // 保存栏身份映射（生存模式使用）
            int[] identityMap = getHotbarIdentityMap();
            rootTag.putIntArray("IdentityMap", identityMap);
            
            // 保存所有快捷栏数据
            ListTag hotbarsList = new ListTag();
            int hotbarCount = HotbarManager.getHotbarCount();
            
            for (int i = 0; i < hotbarCount; i++) {
                CompoundTag hotbarTag = new CompoundTag();
                hotbarTag.putInt("Index", i);
                
                // 保存9个槽位的物品
                ListTag itemsList = new ListTag();
                List<ItemStack> hotbar = HotbarManager.getHotbar(i);
                int itemCount = 0;
                for (int j = 0; j < 9; j++) {
                    ItemStack item = hotbar.get(j);
                    if (!item.isEmpty()) {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putInt("Slot", j);
                        CompoundTag savedItem = (CompoundTag) item.save(null);
                        if (savedItem != null) {
                            itemTag.put("Item", savedItem);
                            itemsList.add(itemTag);
                            itemCount++;
                        }
                    }
                }
                hotbarTag.put("Items", itemsList);
                
                // 保存副手物品
                ItemStack offhandItem = HotbarManager.getOffhandItem(i);
                if (!offhandItem.isEmpty()) {
                    CompoundTag savedOffhand = (CompoundTag) offhandItem.save(null);
                    if (savedOffhand != null) {
                        hotbarTag.put("Offhand", savedOffhand);
                    }
                }
                
                hotbarsList.add(hotbarTag);
                System.out.println("[HotbarExpand] Saving hotbar " + (i + 1) + " with " + itemCount + " items");
            }
            
            rootTag.put("Hotbars", hotbarsList);
            rootTag.putInt("HotbarCount", hotbarCount);
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(dataFile)) {
                net.minecraft.nbt.NbtIo.writeCompressed(rootTag, fos);
            }
            
            System.out.println("[HotbarExpand] Saved " + hotbarCount + " hotbars");
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error saving hotbars data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存配置数据
     */
    private static void saveConfigData(File dataDir) {
        try {
            File configFile = new File(dataDir, CONFIG_FILE);
            CompoundTag configTag = new CompoundTag();
            
            // 保存配置
            configTag.putString("Layout", HotbarConfig.getLayout().name());
            
            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                net.minecraft.nbt.NbtIo.writeCompressed(configTag, fos);
            }
            
            System.out.println("[HotbarExpand] Config saved");
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从文件加载快捷栏数据
     */
    public static void loadHotbarData() {
        if (currentWorldId == null) {
            System.out.println("[HotbarExpand] No world ID, cannot load data");
            return;
        }
        
        try {
            File dataDir = getDataDirectory();
            if (dataDir == null) {
                System.out.println("[HotbarExpand] Cannot get data directory");
                return;
            }
            
            // 加载快捷栏数据
            loadHotbarsData(dataDir);
            
            // 加载配置数据
            loadConfigData(dataDir);
            
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error loading hotbar data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载快捷栏数据
     */
    private static void loadHotbarsData(File dataDir) {
        try {
            File dataFile = new File(dataDir, HOTBARS_FILE);
            if (!dataFile.exists()) {
                System.out.println("[HotbarExpand] No saved hotbar data found");
                return;
            }
            
            CompoundTag rootTag;
            try (FileInputStream fis = new FileInputStream(dataFile)) {
                rootTag = net.minecraft.nbt.NbtIo.readCompressed(fis, NbtAccounter.unlimitedHeap());
            }
            
            // 检查版本
            int version = rootTag.getInt("Version");
            System.out.println("[HotbarExpand] Loading data version " + version);
            
            // 加载当前选中的快捷栏索引
            int currentIndex = rootTag.getInt("CurrentHotbarIndex");
            
            // 加载栏身份映射
            if (rootTag.contains("IdentityMap")) {
                int[] identityMap = rootTag.getIntArray("IdentityMap");
                setHotbarIdentityMap(identityMap);
            }
            
            // 加载保存的快捷栏数量
            int savedHotbarCount = rootTag.getInt("HotbarCount");
            
            // 加载所有快捷栏数据
            ListTag hotbarsList = rootTag.getList("Hotbars", Tag.TAG_COMPOUND);
            int loadedCount = 0;
            
            // 先清空现有数据
            HotbarManager.clearAllHotbars();
            
            // 确保加载正确数量的快捷栏
            for (int i = 0; i < savedHotbarCount; i++) {
                HotbarManager.getHotbar(i);
            }
            
            for (int i = 0; i < hotbarsList.size(); i++) {
                CompoundTag hotbarTag = hotbarsList.getCompound(i);
                int hotbarIndex = hotbarTag.getInt("Index");
                
                // 确保快捷栏存在
                List<ItemStack> hotbar = HotbarManager.getHotbar(hotbarIndex);
                
                // 加载9个槽位的物品
                ListTag itemsList = hotbarTag.getList("Items", Tag.TAG_COMPOUND);
                for (int j = 0; j < itemsList.size(); j++) {
                    CompoundTag itemTag = itemsList.getCompound(j);
                    int slot = itemTag.getInt("Slot");
                    if (itemTag.contains("Item")) {
                        CompoundTag itemData = itemTag.getCompound("Item");
                        ItemStack item = ItemStack.parse(null, itemData).orElse(ItemStack.EMPTY);
                        if (slot >= 0 && slot < 9) {
                            hotbar.set(slot, item);
                        }
                    }
                }
                
                // 加载副手物品
                if (hotbarTag.contains("Offhand")) {
                    CompoundTag offhandTag = hotbarTag.getCompound("Offhand");
                    ItemStack offhandItem = ItemStack.parse(null, offhandTag).orElse(ItemStack.EMPTY);
                    HotbarManager.setOffhandItem(hotbarIndex, offhandItem);
                }
                
                loadedCount++;
            }
            
            // 设置当前选中的快捷栏
            HotbarManager.setCurrentHotbarIndex(currentIndex);
            
            System.out.println("[HotbarExpand] Loaded " + loadedCount + " hotbars from " + dataFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error loading hotbars data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载配置数据
     */
    private static void loadConfigData(File dataDir) {
        try {
            File configFile = new File(dataDir, CONFIG_FILE);
            if (!configFile.exists()) {
                System.out.println("[HotbarExpand] No saved config found");
                return;
            }
            
            CompoundTag configTag;
            try (FileInputStream fis = new FileInputStream(configFile)) {
                configTag = net.minecraft.nbt.NbtIo.readCompressed(fis, NbtAccounter.unlimitedHeap());
            }
            
            // 加载布局
            if (configTag.contains("Layout")) {
                String layoutName = configTag.getString("Layout");
                try {
                    HotbarConfig.LayoutMode layout = HotbarConfig.LayoutMode.valueOf(layoutName);
                    HotbarConfig.setLayout(layout);
                } catch (IllegalArgumentException e) {
                    System.out.println("[HotbarExpand] Unknown layout: " + layoutName);
                }
            }
            
            System.out.println("[HotbarExpand] Config loaded");
        } catch (Exception e) {
            System.out.println("[HotbarExpand] Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取数据存储目录
     */
    private static File getDataDirectory() {
        if (currentWorldId == null) return null;
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) return null;
        
        File baseDir = new File(minecraft.gameDirectory, DATA_FOLDER);
        File typeDir = new File(baseDir, isSinglePlayer ? WORLDS_FOLDER : SERVERS_FOLDER);
        File worldDir = new File(typeDir, currentWorldId);
        
        if (!worldDir.exists()) {
            worldDir.mkdirs();
        }
        
        return worldDir;
    }
    
    /**
     * 确保存储目录存在
     */
    private static void ensureStorageDirectory() {
        File dataDir = getDataDirectory();
        if (dataDir != null && !dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private static String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        // 替换Windows和Unix中的非法字符
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * 获取栏身份映射（从HotbarManager）
     */
    private static int[] getHotbarIdentityMap() {
        return HotbarManager.getHotbarIdentityMap();
    }
    
    /**
     * 设置栏身份映射（到HotbarManager）
     */
    private static void setHotbarIdentityMap(int[] map) {
        HotbarManager.setHotbarIdentityMap(map);
    }
}
