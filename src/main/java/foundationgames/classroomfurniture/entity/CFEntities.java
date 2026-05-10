package foundationgames.classroomfurniture.entity;

import foundationgames.classroomfurniture.CFUtil;
import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.PropDefinition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Map;
import java.util.function.Function;

public enum CFEntities {;
    public static final EntityType<PhysicsPropEntity> BRICK = registerProp("brick", PropDefinition.BRICK);
    public static final EntityType<PhysicsPropEntity> PENCIL_SHARPENER_DRUM = registerProp("pencil_sharpener_drum", PropDefinition.PENCIL_SHARPENER_DRUM);
    public static final EntityType<PhysicsPropEntity> BLUE_PENCIL_SHARPENER = registerProp("blue_pencil_sharpener", PropDefinition.PENCIL_SHARPENER);
    public static final Map<WoodType, EntityType<PhysicsPropEntity>> DESKS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> registerProp(wt.name() + "_desk", PropDefinition.DESKS.get(wt), new Vector3d(0, 6.0/16, 5.0/16))
    );
    public static final Map<WoodType, EntityType<PhysicsPropEntity>> CHAIRS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> registerProp(wt.name() + "_chair", PropDefinition.CHAIRS.get(wt), new Vector3d(0, 5.0/16, -2.0/16))
    );

    public static final EntityType<PhysicsPropEntity> TESTBOX = FabricLoader.getInstance().isDevelopmentEnvironment() ?
            registerProp("testbox", PropDefinition.TESTBOX) : null;


    public static final EntityType<PhysicsConstraintEntity> CONSTRAINT = register("physics_constraint",
            k -> EntityType.Builder.of(PhysicsConstraintEntity::new, MobCategory.AMBIENT)
                    .sized(0.25f, 0.25f).build(k));

    public static EntityType<PhysicsPropEntity> registerProp(String name, PropDefinition prop, @Nullable Vector3dc seat) {
        return register(name,
                k -> EntityType.Builder
                        .<PhysicsPropEntity>of(
                                seat != null ?
                                        (type, lvl) -> new PhysicsSeatPropEntity(type, lvl, prop, seat)
                                : (type, lvl) -> new PhysicsPropEntity(type, lvl, prop),
                                MobCategory.AMBIENT
                        )
                        .updateInterval(2)
                        .sized(1, 1)
                        .build(k)
        );
    }

    public static EntityType<PhysicsPropEntity> registerProp(String name, PropDefinition prop) {
        return registerProp(name, prop, null);
    }

    public static <T extends Entity> EntityType<T> register(String name, Function<ResourceKey<EntityType<?>>, EntityType<T>> type) {
        var id = ClassroomFurniture.id(name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type.apply(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void classload() {}
}
