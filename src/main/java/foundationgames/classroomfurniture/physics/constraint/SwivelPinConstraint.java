package foundationgames.classroomfurniture.physics.constraint;

import foundationgames.classroomfurniture.CFData;
import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.body.PhysBody;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class SwivelPinConstraint implements PhysConstraint {
    public Vector3d firstPin = new Vector3d();
    public Vector3d secondPin = new Vector3d();
    public Vector3d firstSwivelAxis = new Vector3d(0, 1, 0);
    public Vector3d secondSwivelAxis = new Vector3d(0, 1, 0);

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
        double firstWt = 0;//mFirst.lengthSquared() + 1e-7;
        double secondWt = 1;//mSecond.lengthSquared() + 1e-7;

        first.transform.translateLocal(p.set(solvePush).mul(firstWt / (firstWt + secondWt)));
        second.transform.translateLocal(p.set(solvePush).negate().mul(secondWt / (firstWt + secondWt)));

        // Final combined linear velocity from inelastic "collision"
        mTotal.set(mFirst).add(mSecond).div((1.0 / first.inverseMass) + (1.0 / second.inverseMass));

        if (first.inverseMass > 0 && second.inverseMass > 0) {
            first.linearMomentum.set(mTotal).div(first.inverseMass);
            second.linearMomentum.set(mTotal).div(second.inverseMass);
        } else {
            first.linearMomentum.zero();
            second.linearMomentum.zero();
        }


        // SOLVE ANGULAR SEPARATION
        var firstXfm = PhysUtil.extractBasis(first.transform, new Matrix3d());
        var secondXfm = PhysUtil.extractBasis(second.transform, new Matrix3d());

        var firstAxisWS = firstXfm.transform(firstSwivelAxis, new Vector3d());
        var secondAxisWS = secondXfm.transform(secondSwivelAxis, new Vector3d());
        mFirst.set(first.angularMomentum);
        mSecond.set(second.angularMomentum);

        firstWt = mFirst.lengthSquared() + 1e-7;
        secondWt = mSecond.lengthSquared() + 1e-7;

        var firstSolveAxis = new Vector3d();
        var secondSolveAxis = new Vector3d();

        firstXfm.invert();
        secondXfm.invert();

        var solveRotAxis = secondAxisWS.cross(firstAxisWS); // Rotates second to first
        if (solveRotAxis.lengthSquared() > 0) {
            double solveAngle = Math.asin(solveRotAxis.length());
            solveRotAxis.normalize();

            solveRotAxis.normalize();
            firstXfm.transform(solveRotAxis, firstSolveAxis);
            secondXfm.transform(solveRotAxis, secondSolveAxis);

            first.transform.rotate(solveAngle * -(secondWt / (firstWt + secondWt)), firstSolveAxis.x, firstSolveAxis.y, firstSolveAxis.z);
            second.transform.rotate(solveAngle * (firstWt / (firstWt + secondWt)), secondSolveAxis.x, secondSolveAxis.y, secondSolveAxis.z);
        }

        // The solved axis of freedom
        var axisWS = firstXfm.transformTranspose(firstSwivelAxis, new Vector3d());
        PhysUtil.getComponentOnLine(axisWS, second.angularMomentum, second.angularMomentum, 0);

        // Motion around the axis of freedom (swivel axis), don't alter this
        var mFirstResidual = PhysUtil.getComponentOnLine(axisWS, mFirst, new Vector3d(), 0);
        var mSecondResidual = PhysUtil.getComponentOnLine(axisWS, mSecond, new Vector3d(), 0);

        // Set motion vectors to only the motion around the restricted axes
        PhysUtil.getComponentsInPlane(axisWS, mFirst, mFirst, 0);
        PhysUtil.getComponentsInPlane(axisWS, mSecond, mSecond, 0);

        mTotal.set(mFirst).add(mSecond);
        var firstInertia = first.localInverseInertia(new Matrix3d()).invert();
        var secondInertia = second.localInverseInertia(new Matrix3d()).invert();
        var totalInertiaInverse = firstInertia.add(secondInertia, new Matrix3d()).invert();

        // Final combined angular velocity from inelastic "collision" about the restricted axes
//        mTotal.mul(totalInertiaInverse);
//        if (first.inverseMass > 0 && second.inverseMass > 0) {
//            firstInertia.transform(first.angularMomentum.set(mTotal).add(mFirstResidual));
//            secondInertia.transform(second.angularMomentum.set(mTotal).add(mSecondResidual));
//        } else {
//            PhysUtil.getComponentOnLine(axisWS, first.angularMomentum, first.angularMomentum, 0);
//            PhysUtil.getComponentOnLine(axisWS, second.angularMomentum, second.angularMomentum, 0);
//        }

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
        out.store("first_axis", CFData.VECTOR3D_CODEC, firstSwivelAxis);
        out.store("second_pin", CFData.VECTOR3D_CODEC, secondPin);
        out.store("second_axis", CFData.VECTOR3D_CODEC, secondSwivelAxis);
    }

    @Override
    public void readData(ValueInput in) {
        in.read("first_pin", CFData.VECTOR3D_CODEC).ifPresent(firstPin::set);
        in.read("first_axis", CFData.VECTOR3D_CODEC).ifPresent(firstSwivelAxis::set);
        in.read("second_pin", CFData.VECTOR3D_CODEC).ifPresent(secondPin::set);
        in.read("second_axis", CFData.VECTOR3D_CODEC).ifPresent(secondSwivelAxis::set);
    }

    @Override
    public int getId() {
        return 0;
    }
}
