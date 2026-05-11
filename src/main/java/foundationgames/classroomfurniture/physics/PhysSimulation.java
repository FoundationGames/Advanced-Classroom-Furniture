package foundationgames.classroomfurniture.physics;

import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.physics.body.PhysBody;
import foundationgames.classroomfurniture.physics.body.PhysSolid;
import foundationgames.classroomfurniture.physics.body.PhysSurface;
import foundationgames.classroomfurniture.physics.constraint.PhysConstraint;
import foundationgames.classroomfurniture.physics.geometry.PhysAABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class PhysSimulation {
    private final ExecutorService simulationThreads = Executors.newFixedThreadPool(4);

    public static final double DT_TICK = 1 / 20.0;

    private final Level level;

    private final HashMap<UUID, ConstrainedBodyKeyPair> constraintsForTick = new HashMap<>();
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

        var clusters = new ArrayList<Cluster>();

        var vel = new Vector3d();
        for (var body : bodies.values()) {
            var cluster = new Cluster();
            cluster.bodies.add(body);
            cluster.bounds = body.bounds().inflate(0.1 + Math.min(8, body.velocityCG(vel).length() * 0.1));
            clusters.add(cluster);
        }

        for (var pair : constraintsForTick.entrySet()) {
            var holder = pair.getValue();
            var cluster = new Cluster();
            cluster.constraints.add(new PhysMotionSolver.ConstrainedBodyPair(holder.constraint, bodies.get(holder.firstBody), bodies.get(holder.secondBody)));
            clusters.add(cluster);
        }

        boolean allClustered = false;
        int maxClusterIters = clusters.size() + 2;
        for (int n = 0; n <= maxClusterIters; n++) {
            if (allClustered || clusters.size() <= 1) {
                break;
            }

            allClustered = true;
            for (int a = 0; a < clusters.size(); a++) {
                var ca = clusters.get(a);
                if (ca.removed) continue;

                for (int b = a + 1; b < clusters.size(); b++) {
                    var cb = clusters.get(b);
                    if (cb.removed) continue;

                    if (ca.canMerge(cb)) {
                        allClustered = false;
                        ca.merge(cb);
                        cb.removed = true;
                    }
                }
            }
            clusters.removeIf(c -> c.removed);
        }

        var tickers = new HashSet<PhysTicker>();
        var surface = new PhysSurface(0.1, 0.3, 0.4);
        for (var c : clusters) {
            var staticSolids = new ArrayList<PhysSolid>();

            var combinedShape = new VoxelShape[] {Shapes.empty()};
            if (c.bounds != null) BlockPos.betweenClosedStream(c.bounds).forEach(pos -> {
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

            boolean resting = true;
            if (!staticSolids.isEmpty()) {
                for (var b : c.bodies) {
                    if (!b.isEffectivelyAtRest()) {
                        resting = false;
                        break;
                    }
                }
            } else {
                resting = false;
            }

            if (resting) {
                var rs = new PhysRestSolver(c.bodies);
                tickers.add(rs);
            } else {
                var ms = new PhysMotionSolver(staticSolids, c.bodies, c.constraints, 3);
                tickers.add(ms);
            }
        }

        var futures = new HashSet<Future<?>>();
        for (var t : tickers) {
            futures.add(simulationThreads.submit(t::tick));
        }

        try {
            for (var f : futures) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            ClassroomFurniture.LOG.error("Physics simulation encountered an error while waiting for threads to finish", ex);
        }

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
        constraintsForTick.put(constraintId, new ConstrainedBodyKeyPair(constraint, firstBody, secondBody));
    }

    public interface LevelAccess {
        PhysSimulation classroomfurniture$getPhysSimulation();
    }

    public record ConstrainedBodyKeyPair(PhysConstraint constraint, UUID firstBody, UUID secondBody) {}

    public class Cluster {
        public final List<PhysBody> bodies = new ArrayList<>();
        public final List<PhysMotionSolver.ConstrainedBodyPair> constraints = new ArrayList<>();
        public @Nullable AABB bounds = null;
        public boolean removed = false;

        public boolean canMerge(Cluster other) {
            for (var c : constraints) {
                if (other.bodies.contains(c.first()) || other.bodies.contains(c.second())) {
                    return true;
                }
            }
            for (var c : other.constraints) {
                if (bodies.contains(c.first()) || bodies.contains(c.second())) {
                    return true;
                }
            }

            return bounds != null && other.bounds != null && bounds.intersects(other.bounds);
        }

        public void merge(Cluster other) {
            for (var ob : other.bodies) {
                if (!bodies.contains(ob)) bodies.add(ob);
            }

            for (var oc : other.constraints) {
                if (!constraints.contains(oc)) constraints.add(oc);
            }

            if (bounds == null) bounds = other.bounds;
            else if (other.bounds != null) {
                bounds = new AABB(
                        Math.min(bounds.minX, other.bounds.minX),
                        Math.min(bounds.minY, other.bounds.minY),
                        Math.min(bounds.minZ, other.bounds.minZ),
                        Math.max(bounds.maxX, other.bounds.maxX),
                        Math.max(bounds.maxY, other.bounds.maxY),
                        Math.max(bounds.maxZ, other.bounds.maxZ)
                );
            }
        }
    }
}
