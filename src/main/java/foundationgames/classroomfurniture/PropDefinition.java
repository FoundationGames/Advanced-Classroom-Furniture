package foundationgames.classroomfurniture;

import foundationgames.classroomfurniture.entity.CFEntities;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import foundationgames.classroomfurniture.physics.body.PhysSolid;
import foundationgames.classroomfurniture.physics.body.PhysSurface;
import foundationgames.classroomfurniture.physics.geometry.PhysAABB;
import foundationgames.classroomfurniture.physics.geometry.PhysBox;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public record PropDefinition(double density, Set<PhysSolid> solids, Supplier<EntityType<PhysicsPropEntity>> entity) {
    public static final PropDefinition BRICK = new PropDefinition(
            1890, Set.of(
                    new PhysSolid(
                            new PhysSurface(0, 0.5, 0.6),
                            new PhysBox().setPx(7, 3, 4)
                    )
            ), () -> CFEntities.BRICK);
    public static final PropDefinition PENCIL_SHARPENER_DRUM = new PropDefinition(
            4320,
            Set.of(
                    new PhysSolid(
                            new PhysSurface(0.1, 0.1, 0.2),
                            new PhysAABB().setCornerSizePx(-2, -1.5, -5.01, 4, 4, 1)
                    ),
                    new PhysSolid(
                            new PhysSurface(0.1, 0.1, 0.2),
                            new PhysAABB().setCornerSizePx(-1, -0.5, 4.01, 2, 2, 1.99)
                    ),
                    new PhysSolid(
                            new PhysSurface(0.1, 0.1, 0.2),
                            new PhysAABB().setCornerSizePx(-1, -0.5, 6, 2, 4, 1)
                    ),
                    new PhysSolid(
                            new PhysSurface(0.1, 0.1, 0.2),
                            new PhysAABB().setCornerSizePx(-0.5, 2, 7, 1, 1, 2)
                    )
            ), () -> CFEntities.PENCIL_SHARPENER_DRUM);
    public static final PropDefinition PENCIL_SHARPENER = new PropDefinition(
            320,
            Set.of(
                    new PhysSolid(
                            new PhysSurface(0, 0.5, 0.6),
                            new PhysAABB().setCornerSizePx(-3, -3, -4, 6, 1, 8)
                    ),
                    new PhysSolid(
                            new PhysSurface(0.1, 0.1, 0.2),
                            new PhysAABB().setCornerSizePx(-2.5, -2, -4, 5, 8, 8)
                    )
            ), () -> CFEntities.BLUE_PENCIL_SHARPENER);

    public static final Set<PhysSolid> DESK_SOLIDS = Set.of(
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    new PhysAABB().setCornerSizePx(-8 ,11, -14, 16, 2, 11)
            ),
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    new PhysAABB().setCornerSizePx(4 ,11, -3, 4, 2, 8)
            ),
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    new PhysAABB().setCornerSizePx(-5.5, 4, 0, 11, 2, 11)
            ),
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -5.5, 10.7153, 4.7745,
                            11, 2, 12,
                            0, 11, 11,
                            107.5, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0.1, 0.3, 0.4),
                    new PhysAABB().setCornerSizePx(-6, 2, 2.25, 11, 2, 2)
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -7, -7, -13,
                            2, 18, 2,
                            0, 11, -11,
                            10, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            5, -7, -13,
                            2, 18, 2,
                            0, 11, -11,
                            10, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -5, -7, 8,
                            2, 11, 2,
                            0, 11, 8,
                            -10, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            3, -7, 8,
                            2, 11, 2,
                            0, 11, 8,
                            -10, 0, 0
                    )
            )
    );

    public static final Set<PhysSolid> CHAIR_SOLIDS = Set.of(
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    new PhysAABB().setCornerSizePx(-5.5 ,3, -7.5, 11, 2, 11)
            ),
            new PhysSolid(
                    new PhysSurface(0.2, 0.2, 0.4),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -5.5, 10.016, -1.7718,
                            11, 2, 12,
                            0, 10, 3.5,
                            107.5, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            3, -7.7352, 0.9071,
                            2, 11, 2,
                            0, 4.0912, 0.8919,
                            -16, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -5, -7.7352, 0.9071,
                            2, 11, 2,
                            0, 4.0912, 0.8919,
                            -16, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            3, -7.5852, -5.6929,
                            2, 11, 2,
                            0, 4.2412, -3.7081,
                            8, 0, 0
                    )
            ),
            new PhysSolid(
                    new PhysSurface(0, 0.6, 0.7),
                    PhysAABB.cornerSizePivotRotatedPx(
                            -5, -7.5852, -5.6929,
                            2, 11, 2,
                            0, 4.2412, -3.7081,
                            8, 0, 0
                    )
            )
    );

    public static final Map<WoodType, PropDefinition> DESKS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> new PropDefinition(3970, DESK_SOLIDS, () -> CFEntities.DESKS.get(wt))
    );

    public static final Map<WoodType, PropDefinition> CHAIRS = CFUtil.buildMapFromStream(
            CFUtil.WOOD.stream(),
            wt -> new PropDefinition(3970, CHAIR_SOLIDS, () -> CFEntities.CHAIRS.get(wt))
    );

    public static final @Nullable PropDefinition TESTBOX = FabricLoader.getInstance().isDevelopmentEnvironment() ?
            new PropDefinition(
                    1,
                    Set.of(new PhysSolid(new PhysSurface(0.2, 0.23, 0.4), new PhysBox().set(1, 1, 1))),
                    () -> null
            ) : null;
}
