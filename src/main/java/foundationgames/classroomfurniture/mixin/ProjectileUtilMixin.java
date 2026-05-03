package foundationgames.classroomfurniture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @WrapOperation(
            method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(Lnet/minecraft/world/phys/Vec3;)Z", ordinal = 0)
    )
    private static boolean classroomfurniture$doNotBoxIntersectPhysProps(AABB box, Vec3 from, Operation<Boolean> original, @Local(ordinal = 2) Entity current) {
        if (current instanceof PhysicsPropEntity) {
            return false;
        }

        return original.call(box, from);
    }

    @WrapOperation(
            method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;", ordinal = 0)
    )
    private static Optional<Vec3> classroomfurniture$pickPhysPropsByTransformedBoxes(AABB box, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original, @Local(ordinal = 2) Entity current) {
        if (current instanceof PhysicsPropEntity prop) {
            return Optional.ofNullable(prop.clip(from, to));
        }

        return original.call(box, from, to);
    }
}
