package com.example.hotbarexpand.mixin;

import com.example.hotbarexpand.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HotSwap算法实现 - 生存模式物品交换
 * 参考HotSwap模组的实现方式，使用windowClick和SWAP类型进行服务器同步的物品交换
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    /**
     * 在发送使用物品包之前，确保使用正确的物品
     * HotSwap算法：物品已经通过SWAP交换到快捷栏，直接使用即可
     */
    @Inject(method = "useItem", at = @At("HEAD"))
    private void beforeUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!HotbarManager.isSurvivalMode()) return;
        if (HotbarManager.getCurrentHotbarIndex() == 0) return;

        // HotSwap模式下，物品已经交换到快捷栏，不需要额外处理
        // 服务器看到的是快捷栏中的物品
    }

    /**
     * 在发送放置物品包之前，确保使用正确的物品
     */
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void beforeUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!HotbarManager.isSurvivalMode()) return;
        if (HotbarManager.getCurrentHotbarIndex() == 0) return;

        // HotSwap模式下，物品已经交换到快捷栏
    }

    /**
     * 在攻击/破坏方块之前，确保使用正确的物品
     */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void beforeDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!HotbarManager.isSurvivalMode()) return;
        if (HotbarManager.getCurrentHotbarIndex() == 0) return;

        // HotSwap模式下，物品已经交换到快捷栏
    }

    /**
     * 客户端tick - HotSwap不需要延迟恢复逻辑
     * 因为物品交换是即时通过服务器同步的
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // HotSwap算法不需要恢复逻辑
        // 物品交换在切换时就已经通过windowClick完成
    }
}
