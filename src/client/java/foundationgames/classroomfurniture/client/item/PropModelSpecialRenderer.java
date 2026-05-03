package foundationgames.classroomfurniture.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundationgames.classroomfurniture.client.entity.PhysicsPropModel;
import foundationgames.classroomfurniture.client.entity.PhysicsPropRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class PropModelSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final PhysicsPropRenderState state = new PhysicsPropRenderState();

    public final PhysicsPropModel model;
    public final Identifier texture;

    public PropModelSpecialRenderer(PhysicsPropModel model, Identifier texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, boolean glint, int outline) {
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.translate(-0.5, -0.5, 0.5);
        nodes.submitModel(model, state, pose, texture, light, overlay, 0, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        this.model.root().getExtentsForGui(new PoseStack(), output);
    }

    public record Unbaked(Identifier model, String layer, Identifier texture) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("entity_model").forGetter(Unbaked::model),
                Codec.STRING.fieldOf("layer").forGetter(Unbaked::layer),
                Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::model)
        ).apply(i, Unbaked::new));

        @Override
        public @Nullable SpecialModelRenderer<Void> bake(BakingContext context) {
            return new PropModelSpecialRenderer(
                    new PhysicsPropModel(
                            context.entityModelSet().bakeLayer(new ModelLayerLocation(model(), layer())),
                            RenderTypes::entityCutout
                    ),
                    texture()
            );
        }

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
