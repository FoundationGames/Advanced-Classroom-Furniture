package foundationgames.classroomfurniture.physics;

import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.physics.body.PhysBody;
import foundationgames.classroomfurniture.physics.body.PhysSolid;
import foundationgames.classroomfurniture.physics.constraint.PhysConstraint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhysMotionSolver {
    public final List<PhysSolid> staticSolids;
    public final List<PhysBody> bodies;
    public final List<PhysConstraintSolution> constraints;
    public final int substeps;

    public PhysMotionSolver(List<PhysSolid> staticSolids, List<PhysBody> bodies, List<PhysConstraintSolution> constraints, int substeps) {
        this.staticSolids = staticSolids;
        this.bodies = bodies;
        this.constraints = constraints;
        this.substeps = substeps;
    }

    public void tick() {
        try {
            double dt = PhysSimulation.DT_TICK / substeps;

            for (var body : bodies) {
                body.clearCollisionMemory();
            }

            for (int i = 0; i < substeps; i++) {
                process(dt);
            }
        } catch (InterruptedException ex) {
            ClassroomFurniture.LOG.error("Physics motion solver was interrupted", ex);
        }
    }

    public void process(double dt) throws InterruptedException {
        //double invDt = 1.0 / dt;

        for (var body : bodies) {
            body.mutex.acquire();

            body.integrateAccelerations(dt);
        }

        var doNotCollde = new HashMap<PhysBody, Set<PhysBody>>();

        for (var solver : constraints) {
            var constraint = solver.constraint;
            constraint.solveConstraint(solver.first, solver.second);

            doNotCollde.computeIfAbsent(solver.first, _ -> new HashSet<>()).add(solver.second);
            doNotCollde.computeIfAbsent(solver.second, _ -> new HashSet<>()).add(solver.first);
        }

        var nobody = new HashSet<PhysBody>();
        for (int fi = 0; fi < bodies.size(); fi++) {
            for (int si = fi + 1; si < bodies.size(); si++) {
                var f = bodies.get(fi);
                var s = bodies.get(si);

                if (doNotCollde.getOrDefault(f, nobody).contains(s) || doNotCollde.getOrDefault(s, nobody).contains(f)) continue;

                PhysBody.collideBodies(f, s, dt);
            }
        }

        for (var solver : constraints) {
            var constraint = solver.constraint;
            constraint.solveConstraint(solver.first, solver.second);
        }

        for (var body : bodies) {
            PhysBody.collideStaticSolids(body, staticSolids, dt);
        }

        for (var solver : constraints) {
            var constraint = solver.constraint;
            constraint.solveConstraint(solver.first, solver.second);
        }

        for (var body : bodies) {
            PhysUtil.orthonormalize(body.transform, body.transform);
            body.integrateMomenta(dt);
            body.finalizeMotion();

            body.mutex.release();
        }
    }

    public record PhysConstraintSolution(PhysConstraint constraint, PhysBody first, PhysBody second) {}
}
