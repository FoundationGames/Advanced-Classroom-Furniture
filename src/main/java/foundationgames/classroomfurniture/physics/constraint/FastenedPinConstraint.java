package foundationgames.classroomfurniture.physics.constraint;

import foundationgames.classroomfurniture.CFData;
import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.body.PhysBody;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class FastenedPinConstraint implements PhysConstraint {
    public Vector3d firstPin = new Vector3d();
    public Vector3d secondPin = new Vector3d();
    public Vector3d firstSwivelAxis1 = new Vector3d(1, 0, 0);
    public Vector3d firstSwivelAxis2 = new Vector3d(0, 1, 0);
    public Vector3d secondSwivelAxis1 = new Vector3d(1, 0, 0);
    public Vector3d secondSwivelAxis2 = new Vector3d(0, 1, 0);

    @Override
    public void solveConstraint(PhysBody first, PhysBody second) {
        var mFirst = new Vector3d();
        var mSecond = new Vector3d();
        var mTotal = new Vector3d();

        var p = new Vector3d();

        // SOLVE LINEAR SEPARATION
        var firstPinWS = first.transform.transformPosition(firstPin, new Vector3d());
        var secondPinWS = second.transform.transformPosition(secondPin, new Vector3d());
        mFirst.set(first.linearMomentum);
        mSecond.set(second.linearMomentum);

        var solvePush = secondPinWS.sub(firstPinWS, new Vector3d());
        double firstWt = mFirst.lengthSquared() + 1e-7;
        double secondWt = mSecond.lengthSquared() + 1e-7;

        first.transform.translateLocal(p.set(solvePush).mul(firstWt / (firstWt + secondWt)));
        second.transform.translateLocal(p.set(solvePush).mul(-secondWt / (firstWt + secondWt)));

        // Final combined linear velocity from inelastic "collision"
        mTotal.set(mFirst).add(mSecond).div((1.0 / first.inverseMass) + (1.0 / second.inverseMass));

        if (first.inverseMass > 0 && second.inverseMass > 0) {
            first.linearMomentum.set(mTotal).div(first.inverseMass);
            second.linearMomentum.set(mTotal).div(second.inverseMass);
        } else {
            first.linearMomentum.zero();
            second.linearMomentum.zero();
        }


        var firstXfm = PhysUtil.extractBasis(first.transform, new Matrix3d());
        var secondXfm = PhysUtil.extractBasis(second.transform, new Matrix3d());

        // SOLVE ANGULAR SEPARATION
        var firstAxesWS = new Vector3d[] {
                firstXfm.transform(firstSwivelAxis1, new Vector3d()),
                firstXfm.transform(firstSwivelAxis2, new Vector3d())
        };
        var secondAxesWS = new Vector3d[] {
                secondXfm.transform(secondSwivelAxis1, new Vector3d()),
                secondXfm.transform(secondSwivelAxis2, new Vector3d())
        };

        mFirst.set(first.angularMomentum);
        mSecond.set(second.angularMomentum);

        firstWt = mFirst.lengthSquared() + 1e-7;
        secondWt = mSecond.lengthSquared() + 1e-7;

        var firstSolveAxis = new Vector3d();
        var secondSolveAxis = new Vector3d();

        firstXfm.invert();
        secondXfm.invert();

        for (int i = 0; i < 2; i++) {
            var solveRotAxis = secondAxesWS[i].cross(firstAxesWS[i]); // Rotates second to first
            if (solveRotAxis.lengthSquared() > 0) {
                double solveAngle = Math.asin(solveRotAxis.length());

                //System.out.println(secondAxesWS[i] + " CROSS " + firstAxesWS[i]);

                solveRotAxis.normalize();
                firstXfm.transform(solveRotAxis, firstSolveAxis);
                secondXfm.transform(solveRotAxis, secondSolveAxis);

                first.transform.rotate(solveAngle * -(secondWt / (firstWt + secondWt)), firstSolveAxis.x, firstSolveAxis.y, firstSolveAxis.z);
                second.transform.rotate(solveAngle * (firstWt / (firstWt + secondWt)), secondSolveAxis.x, secondSolveAxis.y, secondSolveAxis.z);
            }
        }


        mTotal.set(mFirst).add(mSecond);
        var firstInertia = first.localInverseInertia(new Matrix3d()).invert(new Matrix3d());
        var secondInertia = second.localInverseInertia(new Matrix3d()).invert(new Matrix3d());
        var totalInertiaInverse = firstInertia.add(secondInertia, new Matrix3d()).invert();


        // Final combined angular velocity from inelastic "collision" about the restricted axes
        mTotal.mul(totalInertiaInverse);
        if (first.inverseMass > 0 && second.inverseMass > 0) {
            firstInertia.transform(first.angularMomentum.set(mTotal));
            secondInertia.transform(second.angularMomentum.set(mTotal));
        } else {
            first.angularMomentum.zero();
            second.angularMomentum.zero();
        }
    }

    @Override
    public void getConstraintCentroid(PhysBody first, PhysBody second, Vector3d ctr) {
        var firstPinWS = first.transform.transformPosition(firstPin, new Vector3d());
        var secondPinWS = second.transform.transformPosition(secondPin, new Vector3d());
        ctr.set(firstPinWS).add(secondPinWS).mul(0.5);
    }

    @Override
    public void writeData(ValueOutput out) {
        out.store("first_pin", CFData.VECTOR3D_CODEC, firstPin);
        out.store("first_axis_1", CFData.VECTOR3D_CODEC, firstSwivelAxis1);
        out.store("first_axis_2", CFData.VECTOR3D_CODEC, firstSwivelAxis2);
        out.store("second_pin", CFData.VECTOR3D_CODEC, secondPin);
        out.store("second_axis_1", CFData.VECTOR3D_CODEC, secondSwivelAxis1);
        out.store("second_axis_2", CFData.VECTOR3D_CODEC, secondSwivelAxis2);
    }

    @Override
    public void readData(ValueInput in) {
        in.read("first_pin", CFData.VECTOR3D_CODEC).ifPresent(firstPin::set);
        in.read("first_axis_1", CFData.VECTOR3D_CODEC).ifPresent(firstSwivelAxis1::set);
        in.read("first_axis_2", CFData.VECTOR3D_CODEC).ifPresent(firstSwivelAxis2::set);
        in.read("second_pin", CFData.VECTOR3D_CODEC).ifPresent(secondPin::set);
        in.read("second_axis_1", CFData.VECTOR3D_CODEC).ifPresent(secondSwivelAxis1::set);
        in.read("second_axis_2", CFData.VECTOR3D_CODEC).ifPresent(secondSwivelAxis2::set);
    }

    @Override
    public int getId() {
        return 1;
    }
}
