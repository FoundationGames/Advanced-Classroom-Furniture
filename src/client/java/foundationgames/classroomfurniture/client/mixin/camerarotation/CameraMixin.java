package foundationgames.classroomfurniture.client.mixin.camerarotation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Axis;
import foundationgames.classroomfurniture.entity.PhysicsSeatPropEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow private Entity entity;

    @WrapOperation(
            method = "alignWithEntity(F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0)
    )
    private void classroomfurniture$updatePropRiderCamPos(Camera self, double x, double y, double z, Operation<Void> setPosition) {
        var vehicle = entity;
        while (vehicle != null) {
            vehicle = vehicle.getVehicle();

            if (vehicle instanceof PhysicsSeatPropEntity prop) {
                float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                var rot = new Quaternionf();
                prop.getClientRemoteRotation(rot, partialTicks);

                if (rot.isFinite()) {
                    var e = entity.getPosition(partialTicks);
                    var eyePos = new Vector3f((float) (x - e.x), (float) (y - e.y), (float) (z - e.z));
                    eyePos.rotate(rot);

                    setPosition.call(self, e.x + eyePos.x, e.y + eyePos.y, e.z + eyePos.z);

                    return;
                }

                break;
            }
        }

        setPosition.call(self, x, y, z);
    }

    @Inject(method = "setRotation(FF)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", remap = false))
    private void classroomfurniture$updatePropRiderCamRot(float yaw, float pitch, CallbackInfo ci) {
        var vehicle = entity;
        while (vehicle != null) {
            vehicle = vehicle.getVehicle();

            if (vehicle instanceof PhysicsSeatPropEntity prop) {
                float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
                var rot = new Quaternionf();
                prop.getClientRemoteRotation(rot, partialTicks);

                if (rot.isFinite()) {
                    rot.mul(Axis.YP.rotationDegrees(90).mul(rotation, rotation), rotation);
                }

                return;
            }
        }
    }
}
