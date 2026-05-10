package foundationgames.classroomfurniture.client;

import foundationgames.classroomfurniture.CFUtil;
import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.client.entity.PhysicsPropEntityRenderer;
import foundationgames.classroomfurniture.client.item.PropModelSpecialRenderer;
import foundationgames.classroomfurniture.entity.CFEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public class ClassroomFurnitureClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(
				CFEntities.BRICK,
				PhysicsPropEntityRenderer.factory("brick", "main", "brick"));
		EntityRenderers.register(
				CFEntities.PENCIL_SHARPENER_DRUM,
				PhysicsPropEntityRenderer.factory("pencil_sharpener", "drum", "pencil_sharpener/drum"));
		EntityRenderers.register(
				CFEntities.BLUE_PENCIL_SHARPENER,
				PhysicsPropEntityRenderer.factory("pencil_sharpener", "main", "pencil_sharpener/blue"));

		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			EntityRenderers.register(
					CFEntities.TESTBOX,
					PhysicsPropEntityRenderer.factory("box", "main", "box"));
		}

		for (var wt : CFUtil.WOOD) {
			var desk = CFEntities.DESKS.get(wt);
			EntityRenderers.register(desk, PhysicsPropEntityRenderer.factory("desk", "main", "desk/" + wt.name()));

			var chair = CFEntities.CHAIRS.get(wt);
			EntityRenderers.register(chair, PhysicsPropEntityRenderer.factory("desk", "chair", "desk/" + wt.name()));
		}

		EntityRenderers.register(CFEntities.CONSTRAINT, NoopRenderer::new);
		SpecialModelRenderers.ID_MAPPER.put(ClassroomFurniture.id("prop"), PropModelSpecialRenderer.Unbaked.MAP_CODEC);
	}
}