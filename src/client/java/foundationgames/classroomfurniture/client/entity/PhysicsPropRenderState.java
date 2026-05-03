package foundationgames.classroomfurniture.client.entity;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;

public class PhysicsPropRenderState extends EntityRenderState {
    public final Quaternionf rotation = new Quaternionf();
}
