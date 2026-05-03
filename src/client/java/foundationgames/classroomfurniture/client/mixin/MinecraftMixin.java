package foundationgames.classroomfurniture.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundationgames.classroomfurniture.client.MinecraftAccess;
import foundationgames.classroomfurniture.item.FullMouseControlItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftAccess {
    private boolean classroomfurniture$attackCaptured = false;
    private boolean classroomfurniture$useCaptured = false;
    private boolean classroomfurniture$scrollCaptured = false;

    private ItemStack classroomfurniture$previousCapturingItem = ItemStack.EMPTY;

    private boolean classroomfurniture$attackDown = false;
    private boolean classroomfurniture$useDown = false;

    @Inject(method = "handleKeybinds()V", at = @At("HEAD"))
    private void classroomfurniture$recordMouseInputCaptures(CallbackInfo info) {
        var mc = ((Minecraft) (Object) this);
        var player = mc.player;

        if (player != null) {
            var hand = InteractionHand.MAIN_HAND;
            var stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                hand = InteractionHand.OFF_HAND;
                stack = player.getItemInHand(hand);
            }

            if (stack.getItem() instanceof FullMouseControlItem item) {
                var hit = mc.hitResult;
                classroomfurniture$attackCaptured = item.captureAttackKey(player, hand, hit);
                classroomfurniture$useCaptured = item.captureUseKey(player, hand, hit);
                classroomfurniture$scrollCaptured = item.captureScroll(player, hand, hit);

                return;
            }
        }

        classroomfurniture$attackCaptured = false;
        classroomfurniture$useCaptured = false;
        classroomfurniture$scrollCaptured = false;
    }

    @WrapOperation(
            method = "handleKeybinds()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startUseItem()V", ordinal = -1)
    )
    private void classroomfurniture$stopUseWhenCaptured(Minecraft self, Operation<Void> original) {
        if (!classroomfurniture$useCaptured) original.call(self);
    }

    @WrapOperation(
            method = "handleKeybinds()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;startAttack()Z", ordinal = -1)
    )
    private boolean classroomfurniture$stopAttackStartWhenCaptured(Minecraft self, Operation<Boolean> original) {
        if (classroomfurniture$attackCaptured) return false;

        return original.call(self);
    }

    @WrapOperation(
            method = "handleKeybinds()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V", ordinal = -1)
    )
    private void classroomfurniture$stopAttackContinueWhenCaptured(Minecraft self, boolean down, Operation<Void> original) {
        if (!classroomfurniture$attackCaptured) original.call(self, down);
    }

    @Inject(method = "handleKeybinds()V", at = @At("TAIL"))
    private void classroomfurniture$transmitRawMouseInput(CallbackInfo info) {
        var mc = ((Minecraft) (Object) this);

        boolean atkDown = mc.options.keyAttack.isDown();
        boolean useDown = mc.options.keyUse.isDown();

        var player = mc.player;

        if (player != null) {
            var hand = InteractionHand.MAIN_HAND;
            var stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                hand = InteractionHand.OFF_HAND;
                stack = player.getItemInHand(hand);
            }

            var hit = mc.hitResult;
            if (stack == classroomfurniture$previousCapturingItem && stack.getItem() instanceof FullMouseControlItem item) {
                if (classroomfurniture$attackCaptured) {
                    if (!atkDown && classroomfurniture$attackDown) {
                        item.rawAttackKey(false, player, hand, hit);
                    }
                    if (atkDown && !classroomfurniture$attackDown) {
                        item.rawAttackKey(true, player, hand, hit);
                    }
                }

                if (classroomfurniture$useCaptured) {
                    if (!useDown && classroomfurniture$useDown) {
                        item.rawUseKey(false, player, hand, hit);
                    }
                    if (useDown && !classroomfurniture$useDown) {
                        item.rawUseKey(true, player, hand, hit);
                    }
                }
            } else if (classroomfurniture$previousCapturingItem.getItem() instanceof FullMouseControlItem item) {
                if (classroomfurniture$attackDown) {
                    item.rawAttackKey(false, player, hand, hit);
                }
                if (classroomfurniture$useDown) {
                    item.rawUseKey(false, player, hand, hit);
                }
            }

            classroomfurniture$previousCapturingItem = stack;
        } else {
            classroomfurniture$previousCapturingItem = ItemStack.EMPTY;
        }

        classroomfurniture$attackDown = atkDown;
        classroomfurniture$useDown = useDown;
    }

    @Override
    public boolean classroomfurniture$isScrollCaptured() {
        return classroomfurniture$scrollCaptured;
    }
}
