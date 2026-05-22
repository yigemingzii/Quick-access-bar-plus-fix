package com.example.hotbarexpand;

import com.example.hotbarexpand.client.ClientSetup;
import com.example.hotbarexpand.client.ExpandedHotbarOverlay;
import com.example.hotbarexpand.client.HotbarManager;
import com.example.hotbarexpand.client.gui.HotbarInventoryScreen;
import com.example.hotbarexpand.client.gui.OptionsButtonHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HotbarExpandMod.MOD_ID)
public class HotbarExpandMod {
    public static final String MOD_ID = "hotbarexpand";

    public HotbarExpandMod(IEventBus modEventBus) {
        System.out.println("[HotbarExpand] Mod constructor called");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            System.out.println("[HotbarExpand] Client side detected");
            modEventBus.addListener(this::clientSetup);
            
            // 手动注册事件到NeoForge事件总线
            IEventBus eventBus = NeoForge.EVENT_BUS;
            System.out.println("[HotbarExpand] Registering events to NeoForge.EVENT_BUS");
            eventBus.register(ExpandedHotbarOverlay.class);
            System.out.println("[HotbarExpand] ExpandedHotbarOverlay registered");
            eventBus.register(HotbarManager.class);
            System.out.println("[HotbarExpand] HotbarManager registered");
            eventBus.register(HotbarInventoryScreen.class);
            System.out.println("[HotbarExpand] HotbarInventoryScreen registered");
            eventBus.register(OptionsButtonHandler.class);
            System.out.println("[HotbarExpand] OptionsButtonHandler registered");
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ClientSetup.setup();
    }
}
