package foundationgames.classroomfurniture.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class PhysicsPropModel extends EntityModel<PhysicsPropRenderState> {
    public PhysicsPropModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
    }
}
