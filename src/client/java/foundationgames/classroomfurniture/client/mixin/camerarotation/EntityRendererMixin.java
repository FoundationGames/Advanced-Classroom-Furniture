package foundationgames.classroomfurniture.client.mixin.camerarotation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import foundationgames.classroomfurniture.client.entity.EntityTransformView;
import foundationgames.classroomfurniture.entity.PhysicsSeatPropEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void classroomfurniture$updateSeatedEntityTransform(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (entity instanceof PhysicsSeatPropEntity) {
            return;
        }

        float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var transformState = (EntityTransformView) state;
        transformState.classroomfurniture$getPose().setIdentity();

        Entity vehicle = entity;
        while (vehicle != null) {
            vehicle = vehicle.getVehicle();

            if (vehicle instanceof PhysicsSeatPropEntity prop) {
                var rot = new Quaternionf();
                prop.getClientRemoteRotation(rot, partialTicks);

                if (rot.isFinite()) {
                    var pose = new PoseStack();

                    pose.mulPose(rot);
                    pose.mulPose(Axis.YP.rotationDegrees(90));

                    transformState.classroomfurniture$getPose().set(pose.last());
                }

                return;
            }
        }
    }
}
