package foundationgames.classroomfurniture.physics;

import foundationgames.classroomfurniture.ClassroomFurniture;
import foundationgames.classroomfurniture.physics.body.PhysBody;

import java.util.List;

public class PhysRestSolver implements PhysTicker {
    public final List<PhysBody> bodies;

    public PhysRestSolver(List<PhysBody> bodies) {
        this.bodies = bodies;
    }

    @Override
    public void tick() {
        try {
            for (var b : bodies) {
                b.mutex.acquire();

                b.linearMomentum.zero();
                b.angularMomentum.zero();

                b.mutex.release();
            }
        } catch (InterruptedException ex) {
            ClassroomFurniture.LOG.error("Physics resting body solver was interrupted", ex);
        }
    }
}
