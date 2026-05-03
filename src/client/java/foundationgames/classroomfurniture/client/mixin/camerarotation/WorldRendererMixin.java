package foundationgames.classroomfurniture.client.mixin.camerarotation;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//@Mixin(value = {WorldRenderer.class}, priority = 1500)
public class WorldRendererMixin {
//    @Shadow @Final private MinecraftClient client;
//
//    @ModifyExpressionValue(method = "updateCamera",
//            require = 0, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/render/ChunkRenderingDataPreparer;updateFrustum()Z"))
//    private boolean updateChunkOcclusionCulling(boolean old) {
//            var entity = client.getCameraEntity();
//            while (entity != null) {
//                entity = entity.getVehicle();
//
//                if (entity instanceof ) {
//                    return true;
//                }
//            }
//
//        return old;
//    }
}
