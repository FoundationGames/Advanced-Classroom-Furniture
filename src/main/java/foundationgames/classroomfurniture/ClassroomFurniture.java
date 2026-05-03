package foundationgames.classroomfurniture;

import foundationgames.classroomfurniture.entity.CFEntities;
import foundationgames.classroomfurniture.item.CFItems;
import foundationgames.classroomfurniture.network.GrabPropPackets;
import foundationgames.classroomfurniture.physics.PhysSimulation;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassroomFurniture implements ModInitializer {
	public static final String ID = "classroomfurniture";
	public static final Logger LOG = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize() {
		CFEntities.classload();
		CFItems.classload();

		ServerTickEvents.START_LEVEL_TICK.register(level -> PhysSimulation.get(level).tick());
		GrabPropPackets.register();
	}

	public static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(ID, name);
	}
}