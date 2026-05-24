package com.example.hotbarexpand.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HotbarConfig {
    // 快捷栏布局模式
    public enum LayoutMode {
        ONE_X_ONE(1, 1, "1x1"),
        ONE_X_TWO(1, 2, "1x2"),
        TWO_X_ONE(2, 1, "2x1"),
        TWO_X_TWO(2, 2, "2x2");

        public final int columns;
        public final int rows;
        public final String displayName;

        LayoutMode(int columns, int rows, String displayName) {
            this.columns = columns;
            this.rows = rows;
            this.displayName = displayName;
        }

        public int getTotalHotbars() {
            return columns * rows;
        }

        public static LayoutMode byName(String name) {
            for (LayoutMode mode : values()) {
                if (mode.displayName.equals(name)) {
                    return mode;
                }
            }
            return ONE_X_ONE;
        }
    }

    private static LayoutMode currentLayout = LayoutMode.ONE_X_ONE;
    
    // 设置界面快捷键（默认Ctrl+N）
    private static KeyMapping settingsKey = new KeyMapping(
        "key.hotbarexpand.settings",
        KeyConflictContext.IN_GAME,
        KeyModifier.CONTROL,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_N,
        "key.categories.hotbarexpand"
    );

    public static LayoutMode getLayout() {
        return currentLayout;
    }

    public static void setLayout(LayoutMode layout) {
        currentLayout = layout;
        saveConfig();
    }
    
    public static KeyMapping getSettingsKey() {
        return settingsKey;
    }

    public static void loadConfig() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.toString(), "config", "hotbarexpand");
        try {
            if (!configDir.toFile().exists()) {
                configDir.toFile().mkdirs();
            }

            Path configFile = configDir.resolve("layout_config.dat");
            if (configFile.toFile().exists()) {
                CompoundTag tag = NbtIo.read(configFile);
                if (tag != null) {
                    String layoutName = tag.getString("layout");
                    currentLayout = LayoutMode.byName(layoutName);
                }
            }
        } catch (IOException e) {
            System.err.println("[HotbarExpand] Failed to load config: " + e.getMessage());
        }
    }

    public static void saveConfig() {
        Path configDir = Paths.get(Minecraft.getInstance().gameDirectory.toString(), "config", "hotbarexpand");
        try {
            if (!configDir.toFile().exists()) {
                configDir.toFile().mkdirs();
            }

            Path configFile = configDir.resolve("layout_config.dat");
            CompoundTag tag = new CompoundTag();
            tag.putString("layout", currentLayout.displayName);
            NbtIo.write(tag, configFile);
        } catch (IOException e) {
            System.err.println("[HotbarExpand] Failed to save config: " + e.getMessage());
        }
    }
}
