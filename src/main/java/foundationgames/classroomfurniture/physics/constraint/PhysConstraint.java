package foundationgames.classroomfurniture.physics.constraint;

import foundationgames.classroomfurniture.physics.body.PhysBody;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3d;

import java.util.List;
import java.util.function.Supplier;

public interface PhysConstraint {
    List<Supplier<PhysConstraint>> BY_ID = List.of(
            SwivelPinConstraint::new,
            FastenedPinConstraint::new
    );

    void solveConstraint(PhysBody first, PhysBody second);

    void getConstraintCentroid(PhysBody first, PhysBody second, Vector3d ctr);

    void writeData(ValueOutput out);

    void readData(ValueInput in);

    int getId();
}
