package foundationgames.classroomfurniture.physics;

import foundationgames.classroomfurniture.physics.body.PhysBody;
import foundationgames.classroomfurniture.physics.body.PhysSolid;
import foundationgames.classroomfurniture.physics.body.PhysSurface;
import foundationgames.classroomfurniture.physics.constraint.PhysConstraint;
import foundationgames.classroomfurniture.physics.geometry.PhysAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class PhysSimulation {
    private final ExecutorService simulationThreads = Executors.newFixedThreadPool(3);

    public static final double DT_TICK = 1 / 20.0;

    private final Level level;

    private final HashMap<UUID, PhysConstraintHolder> constraintsForTick = new HashMap<>();
    private final HashMap<UUID, PhysBody> bodies = new HashMap<>();
    private final Set<UUID> removedBodies = new HashSet<>();

    public PhysSimulation(Level level) {
        this.level = level;
    }

    public static PhysSimulation get(Level level) {
        return ((LevelAccess) level).classroomfurniture$getPhysSimulation();
    }

    public void tick() {
        for (var uuid : removedBodies) {
            bodies.remove(uuid);
        }
        removedBodies.clear();

        var staticSolids = new ArrayList<PhysSolid>();
        var solverBodies = new ArrayList<PhysBody>();
        var constraints = new ArrayList<PhysMotionSolver.PhysConstraintSolution>();

        for (var pair : constraintsForTick.entrySet()) {
            var holder = pair.getValue();
            if (bodies.containsKey(holder.firstBody) && bodies.containsKey(holder.secondBody)) {
                constraints.add(new PhysMotionSolver.PhysConstraintSolution(holder.constraint, bodies.get(holder.firstBody), bodies.get(holder.secondBody)));
            }
        }

        var motionSolver = new PhysMotionSolver(staticSolids, solverBodies, constraints, 3);

        for (var pair : bodies.entrySet()) {
            var body = pair.getValue();
            if (!body.remote) {
                body.motionSanityCheck();
                var bounds = body.bounds();
                bounds = bounds.inflate(Math.min(64, body.velocityCG(new Vector3d()).length() * 0.1));

                var surface = new PhysSurface(0, 0.3, 0.4);

                var combinedShape = new VoxelShape[] {Shapes.empty()};
                BlockPos.betweenClosedStream(bounds).forEach(pos -> {
                    var shape = level.getBlockState(pos).getCollisionShape(level, pos);

                    shape.forAllBoxes(
                            (x1, y1, z1, x2, y2, z2) ->
                                    combinedShape[0] = Shapes.joinUnoptimized(
                                            combinedShape[0],
                                            Shapes.box(
                                                    x1 + pos.getX(),
                                                    y1 + pos.getY(),
                                                    z1 + pos.getZ(),
                                                    x2 + pos.getX(),
                                                    y2 + pos.getY(),
                                                    z2 + pos.getZ()
                                            ),
                                            BooleanOp.OR)
                    );
                });

                combinedShape[0].forAllBoxes(
                        (x1, y1, z1, x2, y2, z2) ->
                                staticSolids.add(new PhysSolid(surface, new PhysAABB().setAABB(x1, y1, z1, x2, y2, z2)))
                );

                solverBodies.add(body);
            }
        }

        motionSolver.tick();

        constraintsForTick.clear();
    }

    public PhysBody getOrCreateBody(UUID id, Supplier<PhysBody> constructor) {
        return this.bodies.computeIfAbsent(id, u -> constructor.get().withId(u));
    }

    public boolean hasBody(UUID id) {
        return this.bodies.containsKey(id);
    }

    public void removeBody(UUID id) {
        this.removedBodies.add(id);
    }

    public void removeBodyByReference(PhysBody body) {
        for (var e : this.bodies.entrySet()) {
            if (e.getValue() == body) {
                removeBody(e.getKey());
                return;
            }
        }
    }

    public void addConstraintForTick(UUID constraintId, PhysConstraint constraint, UUID firstBody, UUID secondBody) {
        constraintsForTick.put(constraintId, new PhysConstraintHolder(constraint, firstBody, secondBody));
    }

    public interface LevelAccess {
        PhysSimulation classroomfurniture$getPhysSimulation();
    }

    public record PhysConstraintHolder(PhysConstraint constraint, UUID firstBody, UUID secondBody) {}
}
