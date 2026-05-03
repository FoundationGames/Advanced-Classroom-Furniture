package foundationgames.classroomfurniture.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundationgames.classroomfurniture.item.FullMouseControlItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow @Final
    private Minecraft minecraft;

    @WrapOperation(
            method = "turnPlayer(D)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V", ordinal = 0)
    )
    private void classroomfurniture$consumePlayerTurn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        var hand = InteractionHand.MAIN_HAND;
        var stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            hand = InteractionHand.OFF_HAND;
            stack = player.getItemInHand(hand);
        }

        if (stack.getItem() instanceof FullMouseControlItem item) {
            var hit = this.minecraft.hitResult;

            if (item.captureMotion(player, hand, hit)) {
                item.rawMotion(dx, dy, player, hand, hit);

                return;
            }
        }

        original.call(player, dx, dy);
    }
}
