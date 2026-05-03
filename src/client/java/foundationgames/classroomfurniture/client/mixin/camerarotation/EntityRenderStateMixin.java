package foundationgames.classroomfurniture.client.mixin.camerarotation;

import com.mojang.blaze3d.vertex.PoseStack;
import foundationgames.classroomfurniture.client.entity.EntityTransformView;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements EntityTransformView {
    private final PoseStack.Pose classroomfurniture$pose = new PoseStack.Pose();

    @Override
    public PoseStack.Pose classroomfurniture$getPose() {
        return classroomfurniture$pose;
    }
}
