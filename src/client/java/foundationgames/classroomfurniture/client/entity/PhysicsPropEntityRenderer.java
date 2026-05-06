package foundationgames.classroomfurniture.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class PhysicsPropEntityRenderer extends EntityRenderer<PhysicsPropEntity, PhysicsPropRenderState> {
    private final PhysicsPropModel model;
    private final Identifier texture;

    public PhysicsPropEntityRenderer(EntityRendererProvider.Context ctx, ModelLayerLocation model, Identifier texture, Function<Identifier, RenderType> renderType) {
        super(ctx);

        this.model = new PhysicsPropModel(ctx.getModelSet().bakeLayer(model), renderType);
        this.texture = texture;
    }

    public static EntityRendererProvider<PhysicsPropEntity> factory(String model, String layer, String texture, Function<Identifier, RenderType> renderType) {
        return ctx -> new PhysicsPropEntityRenderer(
                ctx,
                new ModelLayerLocation(ClassroomFurniture.id(model), layer),
                ClassroomFurniture.id("textures/entity/" + texture + ".png"),
                renderType);
    }

    public static EntityRendererProvider<PhysicsPropEntity> factory(String model, String layer, String texture) {
        return factory(model, layer, texture, RenderTypes::entityCutout);
    }

    @Override
    public void submit(PhysicsPropRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(state.rotation);
        pose.mulPose(Axis.ZP.rotation((float) Math.PI));
        nodes.submitModel(model, state, pose, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        pose.popPose();

        super.submit(state, pose, nodes, camera);
    }

    @Override
    public void extractRenderState(PhysicsPropEntity entity, PhysicsPropRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        entity.getClientRemoteRotation(state.rotation, partialTicks);
    }

    @Override
    public PhysicsPropRenderState createRenderState() {
        return new PhysicsPropRenderState();
    }
}
