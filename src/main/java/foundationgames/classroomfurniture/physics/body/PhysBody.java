package foundationgames.classroomfurniture.physics.body;

import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.geometry.PhysContact;
import foundationgames.classroomfurniture.physics.geometry.PhysShape;
import foundationgames.classroomfurniture.physics.geometry.PhysTransformedShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Matrix4x3d;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class PhysBody {
    public static final double GRAVITY = 16; // Blocks per second squared

    /**
     * Each solid's shape should be in body-local space with 0,0,0 being the CG
     */
    public final Set<PhysSolid> solids;

    public double inverseMass = 0;
    private final Matrix3d inverseInertia = new Matrix3d();

    public final Matrix4x3d prevTransform = new Matrix4x3d();
    public final Matrix4x3d transform = new Matrix4x3d();

    public final Vector3d prevLinMomentum = new Vector3d();
    public final Vector3d prevAngMomentum = new Vector3d();
    public final Vector3d linearMomentum = new Vector3d();
    public final Vector3d angularMomentum = new Vector3d();

    public final Vector3d acceleration = new Vector3d();
    public final Vector3d force = new Vector3d();
    public final Vector3d angularAcceleration = new Vector3d();
    public final Vector3d torque = new Vector3d();

    public final Vector3d[] directionalAccelLimit = new Vector3d[] {
            new Vector3d(), new Vector3d(), new Vector3d()
    };

    public double linearDrag = 0.2;
    public double angularDrag = 0.3;

    public Set<UUID> collidingWith = new HashSet<>();
    public final Set<PhysContact> prevContacts = new HashSet<>();
    public final Set<PhysContact> contacts = new HashSet<>();

    public boolean sleep = false;
    public boolean remote = true;

    public UUID uuid = UUID.randomUUID();

    public final Semaphore mutex = new Semaphore(1);

    public PhysBody(Set<PhysSolid> solids) {
        this.solids = solids;

        calculateProperties();
    }

    public PhysBody positioned(Vec3 mcPos) {
        this.transform.setTranslation(mcPos.x(), mcPos.y(), mcPos.z());
        return this;
    }

    public PhysBody rotated(Quaternionfc rot) {
        this.transform.rotateLocal(rot);
        return this;
    }

    public PhysBody rotated(Matrix3dc rot) {
        PhysUtil.insertBasis(rot, this.transform);
        return this;
    }

    public PhysBody withId(UUID id) {
        this.uuid = id;
        return this;
    }

    public Vec3 getMCPosition() {
        var pos = new Vector3d();
        this.transform.getTranslation(pos);

        return new Vec3(pos.x, pos.y, pos.z);
    }

    public Matrix3d getRotation(Matrix3d rot) {
        return PhysUtil.extractBasis(this.transform, rot);
    }

    public Matrix3dc inverseInertia() {
        return this.inverseInertia;
    }

    public Matrix3d localInverseInertia(Matrix3d inertia) {
        return PhysUtil.projectSquareBasisOntoTransformBasis(transform, inertia.set(inverseInertia));
    }

    public AABB bounds() {
        var xfmShape = new PhysTransformedShape();
        xfmShape.transform.set(this.transform);

        double x0 = Double.POSITIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;

        for (var solid : solids) {
            xfmShape.shape = solid.shape();
            var sb = xfmShape.bounds();
            x0 = Math.min(x0, sb.minX);
            y0 = Math.min(y0, sb.minY);
            z0 = Math.min(z0, sb.minZ);
            x1 = Math.max(x1, sb.maxX);
            y1 = Math.max(y1, sb.maxY);
            z1 = Math.max(z1, sb.maxZ);
        }

        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    public Vector3d velocityCG(Vector3d vel) {
        if (sleep) {
            vel.zero();
            return vel;
        }

        vel.set(linearMomentum).mul(inverseMass);
        return vel;
    }

    public Vector3d angularVelocity(Vector3d ang) {
        if (sleep) {
            ang.zero();
            return ang;
        }

        var inverseInertiaLocal = PhysUtil.projectSquareBasisOntoTransformBasis(transform, new Matrix3d(inverseInertia));
        return inverseInertiaLocal.transform(ang.set(angularMomentum));
    }

    public Vector3d velocityAt(Vector3dc at, Vector3d vel) {
        var r = new Vector3d(at).sub(transform.getTranslation(new Vector3d()));

        angularVelocity(vel);
        vel.cross(r);

        vel.add(velocityCG(new Vector3d()));
        return vel;
    }

    public void applyImpulseCG(Vector3dc impulse) {
        if (sleep) return;

        this.linearMomentum.add(impulse);
    }

    public void applyImpulseAngular(Vector3dc impulse) {
        if (sleep) return;

        this.angularMomentum.add(impulse);
    }

    public void applyImpulse(Vector3dc at, Vector3dc impulse) {
        if (sleep) return;

        getMomentumAfterImpulse(at, impulse, this.linearMomentum, this.angularMomentum);
    }

    public void applyVelocityChange(Vector3dc at, Vector3dc velocityChange) {
        var linChange = new Vector3d(velocityChange);
        var angChange = transform.getTranslation(new Vector3d());

        at.sub(angChange, angChange);
        angChange.cross(velocityChange);

        applyImpulseCG(linChange.div(this.inverseMass));
        applyImpulseAngular(this.localInverseInertia(new Matrix3d()).invert().transform(angChange));
    }

    public void getMomentumAfterImpulse(Vector3dc at, Vector3dc impulse, Vector3d linearMomentum, Vector3d angularMomentum) {
        var r = transform.getTranslation(new Vector3d());
        at.sub(r, r);

        this.linearMomentum.add(impulse, linearMomentum);
        this.angularMomentum.add(r.cross(impulse), angularMomentum);
    }

    public void integrateMomenta(double dt) {
        prevTransform.set(transform);
        if (sleep) return;

        var a = new Vector3d();
        var m = new Vector3d();
        var t = new Matrix4x3d();

        this.angularVelocity(a);
        double angularVelocity = a.length();
        if (angularVelocity > 0) {
            a.normalize(m);
            transform.invert(t).transformDirection(m);
            transform.rotate(angularVelocity * dt, m.x(), m.y(), m.z());
        }
        this.velocityCG(m).mul(dt);
        this.transform.translateLocal(m);
    }

    public void clearCollisionMemory() {
        this.collidingWith.clear();
    }

    public void integrateAccelerations(double dt) {
        if (sleep) return;

        var m = new Vector3d();
        boolean wellSupported = false;

        m.zero();
        if (inverseMass > 0) m.set(acceleration).div(inverseMass);
        m.add(force).mul(dt);
        this.linearMomentum.add(m);

        m.zero();
        if (inverseMass > 0) inverseInertia.transform(m.set(angularAcceleration));
        m.add(torque).mul(dt);
        this.angularMomentum.add(m);

        if (wellSupported && this.angularMomentum.length() * this.inverseMass < 0.08) {
            this.angularMomentum.mul(Math.exp(-120 * dt)); // Bad way to stop angular jitter
        }

        linearMomentum.mul(Math.exp(-linearDrag * dt));
        angularMomentum.mul(Math.exp(-angularDrag * dt));
    }

    public void finalizeMotion() {
        for (var limit : this.directionalAccelLimit) {
            limit.zero();
        }

        if (!this.transform.isFinite()) {
            this.transform.set(this.prevTransform);
        }

        this.motionSanityCheck();

        this.prevContacts.clear();
        this.prevContacts.addAll(this.contacts);
        this.contacts.clear();
    }

    public void motionSanityCheck() {
        if (!this.linearMomentum.isFinite()) {
            this.linearMomentum.zero();
        }
        if (!this.angularMomentum.isFinite()) {
            this.angularMomentum.zero();
        }

        if (this.angularMomentum.lengthSquared() > 512 * 512) {
            this.angularMomentum.normalize(512);
        }
        if (this.linearMomentum.lengthSquared() > 4096 / (this.inverseMass * this.inverseMass)) {
            this.linearMomentum.normalize(64 / this.inverseMass);
        }
    }

    private Matrix3d calculateInertia(Matrix3d inertia) {
        var dInertia = new Matrix3d();

        inertia.zero();
        for (var solid : solids) {
            solid.shape().inertiaTensor(dInertia);
            inertia.add(dInertia);
        }

        return inertia;
    }

    public PhysBody calculateProperties() {
        calculateInertia(this.inverseInertia).invert().scale(inverseMass);
        return this;
    }

    public static void collideBodies(PhysBody first, PhysBody second, double dt) {
        var fsShapeWorld = new PhysTransformedShape();
        fsShapeWorld.transform.set(first.transform);
        var ssShapeWorld = new PhysTransformedShape();
        ssShapeWorld.transform.set(second.transform);

        var colNormal = new Vector3d();
        var colPoint = new Vector3d();
        var colTangent = new Vector3d();

        var firstCtr = new Vector3d();
        var secondCtr = new Vector3d();
        var colCtr = new Vector3d();
        var firstVel = new Vector3d();
        var secondVel = new Vector3d();
        var secondIntoFirstVel = new Vector3d();
        var secondIntoFirstAngVel = new Vector3d();

        var solvePush = new Vector3d();

        var firstCtrToContact = new Vector3d();
        var secondCtrToContact = new Vector3d();
        var firstC2CTangent = new Vector3d();
        var secondC2CTangent = new Vector3d();
        var tangentialInertiae = new Vector3d();
        var firstInvInertia = first.localInverseInertia(new Matrix3d());

        var reactImpulse = new Vector3d();
        var firstImpulse = new Vector3d();
        var firstAngImpulse = new Vector3d();
        var secondImpulse = new Vector3d();
        var secondAngImpulse = new Vector3d();
        var secondInvInertia = second.localInverseInertia(new Matrix3d());

        first.transform.getTranslation(firstCtr);
        second.transform.getTranslation(secondCtr);

        var wvel = first.velocityCG(new Vector3d());
        double fw = 1.0 / (wvel.length() + 1e-7);
        second.velocityCG(wvel);
        double sw = 1.0 / (wvel.length() + 1e-7);

        colCtr.set(firstCtr).mul(fw).add(new Vector3d(secondCtr).mul(sw)).div(fw + sw);

        for (var fs : first.solids) {
            fsShapeWorld.shape = fs.shape();

            for (var ss : second.solids) {
//                ssShapeWorld.shape = ss.shape();
//
//                var interpen = PhysShape.interpen(fsShapeWorld, ssShapeWorld);
//                if (interpen == null) continue;
//
//                first.collidingWith.add(second.uuid);
//                second.collidingWith.add(first.uuid);
//
//                colNormal.set(interpen.direction);
//                interpen.getCollisionCenter(colCtr, colPoint);
//
//                first.velocityAt(colPoint, firstVel);
//                second.velocityAt(colPoint, secondVel);
//
//                // 1. Solve the collision by moving the bodies out of each other
//                double firstSolveWt = first.sleep ? 0 : firstVel.lengthSquared() + 1e-7;
//                double secondSolveWt = second.sleep ? 0 : secondVel.lengthSquared() + 1e-7;
//                double totalSolvewt = firstSolveWt + secondSolveWt;
//                if (totalSolvewt <= 0) {
//                    return;
//                }
//                first.transform.translateLocal(solvePush.set(colNormal).mul(-(firstSolveWt * interpen.penetration) / (firstSolveWt + secondSolveWt)));
//                second.transform.translateLocal(solvePush.set(colNormal).mul((secondSolveWt * interpen.penetration) / (firstSolveWt + secondSolveWt)));
//                colPoint.add(solvePush);
//
//                // 2. Calculate the collision reaction impulse
//                secondIntoFirstVel.set(secondVel).sub(firstVel);
//
//                first.transform.getTranslation(firstCtrToContact).sub(colPoint);
//                second.transform.getTranslation(secondCtrToContact).sub(colPoint);
//
//                first.inverseInertia().transform(firstC2CTangent.set(firstCtrToContact).cross(colNormal));
//                firstC2CTangent.cross(firstCtrToContact);
//
//                second.inverseInertia().transform(secondC2CTangent.set(secondCtrToContact).cross(colNormal));
//                secondC2CTangent.cross(secondCtrToContact);
//
//                tangentialInertiae.set(firstC2CTangent).add(secondC2CTangent);
//
//                var movePush = new Vector3d();
//                movePush.set(first.acceleration).sub(second.acceleration);
//
//                double secondIntoFirstNormalVel = secondIntoFirstVel.dot(colNormal);
//                double restitution = Math.min(fs.surface().restitution(), ss.surface().restitution());
//
//                double impulseMagnitude = (1 + restitution) * secondIntoFirstNormalVel;
//                impulseMagnitude /= first.inverseMass + second.inverseMass + tangentialInertiae.dot(colNormal);
//
//                reactImpulse.set(colNormal).mul(impulseMagnitude);
//
//                // 3. Calculate the friction impulses
//                PhysUtil.getComponentsInPlane(colNormal, secondIntoFirstVel, colTangent, 0);
//
//                double staticFrict = 0.5 * (fs.surface().staticFriction() + ss.surface().staticFriction());
//                double kineticFrict = Math.min(staticFrict * 0.99, 0.5 * (fs.surface().kineticFriction() + fs.surface().kineticFriction()));
//
//                double staticFrictImpulse = staticFrict * impulseMagnitude;
//                double kineticFrictImpulse = kineticFrict * impulseMagnitude;
//
//                if (colTangent.lengthSquared() > 0) {
//                    colTangent.normalize();
//
//                    if (first.inverseMass > 0) {
//                        firstInvInertia.transform(firstC2CTangent.set(firstCtrToContact).cross(colTangent));
//                        firstC2CTangent.cross(firstCtrToContact);
//
//                        double tangentImpulse = secondIntoFirstVel.dot(colTangent);
//                        tangentImpulse /= first.inverseMass + firstC2CTangent.dot(colTangent);
//                        if (tangentImpulse > staticFrictImpulse) {
//                            tangentImpulse = kineticFrictImpulse;
//                        }
//                        firstImpulse.set(colTangent).mul(-tangentImpulse);
//                    }
//                    if (second.inverseMass > 0) {
//                        secondInvInertia.transform(secondC2CTangent.set(secondCtrToContact).cross(colTangent));
//                        secondC2CTangent.cross(secondCtrToContact);
//
//                        double tangentImpulse = secondIntoFirstVel.dot(colTangent);
//                        tangentImpulse /= second.inverseMass + secondC2CTangent.dot(colTangent);
//                        if (tangentImpulse > staticFrictImpulse) {
//                            tangentImpulse = kineticFrictImpulse;
//                        }
//                        secondImpulse.set(colTangent).mul(tangentImpulse);
//                    }
//                }
//                if (interpen.points.size() > 1) {
//                    first.angularVelocity(secondIntoFirstAngVel);
//                    secondIntoFirstAngVel.sub(second.angularVelocity(new Vector3d()));
//
//                    if (first.inverseMass > 0) {
//                        double angularImpulse = secondIntoFirstAngVel.dot(colNormal);
//                        angularImpulse /= firstInvInertia.transform(colNormal, firstAngImpulse).dot(colNormal);
//
//                        if (Math.abs(angularImpulse) > staticFrictImpulse) {
//                            angularImpulse = kineticFrictImpulse * Math.signum(angularImpulse);
//                        }
//                        firstAngImpulse.set(colNormal).mul(angularImpulse);
//                    }
//                    if (second.inverseMass > 0) {
//                        double angularImpulse = -secondIntoFirstAngVel.dot(colNormal);
//                        angularImpulse /= secondInvInertia.transform(colNormal, secondAngImpulse).dot(colNormal);
//
//                        if (Math.abs(angularImpulse) > staticFrictImpulse) {
//                            angularImpulse = kineticFrictImpulse * Math.signum(angularImpulse);
//                        }
//                        secondAngImpulse.set(colNormal).mul(angularImpulse);
//                    }
//                }
//
//                // 4. Apply the impulses
//                firstImpulse.add(reactImpulse);
//                secondImpulse.add(reactImpulse.negate());
//
//                first.applyImpulse(colPoint, firstImpulse);
//                second.applyImpulse(colPoint, secondImpulse);
//
//                if (interpen.points.size() >= 3) {
//                    for (var limit : first.directionalAccelLimit) {
//                        if (limit.lengthSquared() <= 1e-7) {
//                            limit.set(colNormal).negate();
//                            break;
//                        }
//                    }
//                    for (var limit : second.directionalAccelLimit) {
//                        if (limit.lengthSquared() <= 1e-7) {
//                            limit.set(colNormal);
//                            break;
//                        }
//                    }
//                }
            }
        }
    }

    public static void collideStaticSolids(PhysBody body, List<PhysSolid> solids, double dt) {
        var bsShapeWorld = new PhysTransformedShape();
        bsShapeWorld.transform.set(body.transform);

        var ctr = body.transform.getTranslation(new Vector3d());
        solids.sort(Comparator.comparingDouble(s -> s.shape().circumcircleOrigin(new Vector3d()).distanceSquared(ctr)));

        var contactManifolds = new ArrayList<PhysContact>();

        for (var bs : body.solids) {
            for (var solid : solids) {
                bsShapeWorld.shape = bs.shape();

                var contact = PhysShape.interpen(solid.shape(), bsShapeWorld);
                if (contact == null) continue;

                contact.firstSurface = bs.surface();
                for (var i : contact.interpens) {
                    i.surface = solid.surface();
                }

                boolean merged = false;

                for (var foundContact : contactManifolds) {
                    if (foundContact.mergeOrNull(contact) != null) {
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    contactManifolds.add(contact);
                }
            }
        }

        for (int i = 0; i < contactManifolds.size(); i++) {
            PhysContact memorized = null;
            for (var contact : body.prevContacts) {
                if (contactManifolds.get(i).roughlyMatches(contact, dt)) {
                    memorized = contact;
                    break;
                }
            }

            if (memorized != null) {
                body.prevContacts.remove(memorized);
                contactManifolds.set(i, memorized);
            }
        }

        if (contactManifolds.isEmpty()) return;
        body.contacts.addAll(contactManifolds);

        var colNormal = new Vector3d();
        var colPoint = new Vector3d();
        var colTangent = new Vector3d();

        var solvePush = new Vector3d();
        var solveTranslate = new Vector3d();
        var rotAxis = new Vector3d();

        var vel = new Vector3d();
        var angVel = new Vector3d();
        var rContact = new Vector3d();
        var rContactTangent = new Vector3d();
        var invInertia = body.localInverseInertia(new Matrix3d());

        var reactImpulse = new Vector3d();
        var totalImpulse = new Vector3d();
        var angImpulse = new Vector3d();

        for (var contact : contactManifolds) {
            var deepest = contact.deepest();
            if (deepest == null) continue;

            //double pointWt = 1.0;// / contact.interpens.size();

            //System.out.println("CTR:" + colPoint + " DIR:" + contact.normal + " WT:" + contact.interpens.size() + " PEN:" + contact.deepestDepth());

            //System.out.println(contact.interpens);

//            if (contact.interpens.size() > 2) {
//                var d1 = new Vector3d(contact.interpens.get(1).pos).sub(contact.interpens.get(0).pos);
//                var d2 = new Vector3d(contact.interpens.get(2).pos).sub(contact.interpens.get(0).pos);
//                var nml = d1.cross(d2).normalize();
//                if (nml.dot(contact.normal) < 0) nml.negate();
//
//                var axis = nml.cross(contact.normal, new Vector3d());
//                if (axis.lengthSquared() > 0) {
//                    axis.normalize();
//
//                    double angle = nml.angle(contact.normal);
//
//                    body.transform.rotate(-angle, axis.x, axis.y, axis.z);
//                }
//            }

            for (var pen : contact.interpens) {
                colNormal.set(pen.interpen).normalize();
                colPoint.set(pen.pos);

                body.velocityAt(colPoint, vel);
                rContact.set(ctr).sub(colPoint);

                // 2. Calculate the collision reaction impulse
                invInertia.transform(rContactTangent.set(rContact).cross(colNormal));
                rContactTangent.cross(rContact);

                double normalVel = Math.max(0, -vel.dot(colNormal));
                double restitution = Math.min(contact.firstSurface.restitution(), pen.surface.restitution());

                double impulseMagnitude = (1 + restitution) * normalVel;
                impulseMagnitude /= body.inverseMass + rContactTangent.dot(colNormal);

                reactImpulse.set(colNormal).mul(impulseMagnitude);

                // 3. Calculate the friction impulses
                PhysUtil.getComponentsInPlane(colNormal, vel, colTangent, 0);

                double staticFrict = 0.5 * (contact.firstSurface.staticFriction() + pen.surface.staticFriction());
                double kineticFrict = Math.min(staticFrict * 0.99, 0.5 * (contact.firstSurface.kineticFriction() + pen.surface.kineticFriction()));

                double staticFrictImpulse = staticFrict * impulseMagnitude;
                double kineticFrictImpulse = kineticFrict * impulseMagnitude;

                totalImpulse.zero();
                if (colTangent.lengthSquared() > 0) {
                    colTangent.normalize();

                    if (body.inverseMass > 0) {
                        invInertia.transform(rContactTangent.set(rContact).cross(colTangent));
                        rContactTangent.cross(rContact);

                        double tangentImpulse = vel.dot(colTangent);
                        tangentImpulse /= body.inverseMass + rContactTangent.dot(colTangent);

                        if (tangentImpulse > staticFrictImpulse) {
                            tangentImpulse = kineticFrictImpulse;
                        }
                        totalImpulse.set(colTangent).mul(-tangentImpulse);
                    }
                }

                // 4. Apply the impulses
                totalImpulse.add(reactImpulse);

                body.applyImpulse(colPoint, totalImpulse);
                angImpulse.add(totalImpulse);
            }

            body.transform.translateLocal(solvePush.set(contact.normal).mul(contact.deepestDepth()));

//            avgStaticFrict *= angImpulse.length();
//            avgKineticFrict *= angImpulse.length();
//
//            if (contact.points.size() > 1 && body.inverseMass > 0) {
//                body.angularVelocity(angVel);
//
//                double angularImpulse = -angVel.dot(colNormal);
//                angularImpulse /= invInertia.transform(colNormal, angImpulse).dot(colNormal);
//
//                if (Math.abs(angularImpulse) > avgStaticFrict) {
//                    angularImpulse = avgKineticFrict * Math.signum(angularImpulse);
//                }
//                angImpulse.set(colNormal).mul(angularImpulse);
//                body.applyImpulseAngular(angImpulse.mul(pointWt));
//            }
//
//            //System.out.println("CTR:" + colPoint + " DIR:" + contact.direction + " WT:" + contact.points.size() + " PEN:" + contact.penetration);
//
//            body.velocityAt(colPoint, vel).negate();
//
//            ctrToContact.set(ctr).sub(colPoint);
//
//            //colPoint.add(solvePush);
//
////            contact.getPenetratingNormal(penNormal);
////            if (penNormal.isFinite()) {
////                penNormal.cross(colNormal, rotAxis);
////                double angle = Math.asin(rotAxis.length());
////                if (angle > 0 && angle < 0.1) {
////                    rotAxis.normalize();
////
////                    if (rotAxis.isFinite()) {
////                        //System.out.println("WT=" + contact.points.size() + " NML=" + colNormal + " PML=" + penNormal + " ANG=" + angle + " AX=" + rotAxis);
////                        //body.transform.rotate(angle, rotAxis.x, rotAxis.y, rotAxis.z);
////                    }
////                }
////            }
//
//            // 2. Calculate the collision reaction impulse
//            invInertia.transform(tangentC2C.set(ctrToContact).cross(colNormal));
//            tangentC2C.cross(ctrToContact);
//
//            var movePush = new Vector3d();
//            movePush.set(body.acceleration);
//
//            double normalVel = Math.max(0, vel.dot(colNormal));
//            double restitution = Math.min(contact.surfaceA.restitution(), contact.surfaceB.restitution());
//            //restitution *= 1 - Math.exp(-Math.abs(normalVel));
//
//            double impulseMagnitude = (1 + slop + restitution) * normalVel;
//            impulseMagnitude /= body.inverseMass + tangentC2C.dot(colNormal);
//
//            reactImpulse.set(colNormal).mul(impulseMagnitude);
//
//            // 3. Calculate the friction impulses
//            colNormal.normalize();
//            PhysUtil.getComponentsInPlane(colNormal, vel, colTangent, 0);
//
//            double staticFrict = 0.5 * (contact.surfaceA.staticFriction() + contact.surfaceB.staticFriction());
//            double kineticFrict = Math.min(staticFrict * 0.99, 0.5 * (contact.surfaceA.kineticFriction() + contact.surfaceB.kineticFriction()));
//
//            double staticFrictImpulse = staticFrict * impulseMagnitude;
//            double kineticFrictImpulse = kineticFrict * impulseMagnitude;
//
//            if (colTangent.lengthSquared() > 0) {
//                colTangent.normalize();
//
//                if (body.inverseMass > 0) {
//                    invInertia.transform(tangentC2C.set(ctrToContact).cross(colTangent));
//                    tangentC2C.cross(ctrToContact);
//
//                    double tangentImpulse = vel.dot(colTangent);
//                    tangentImpulse /= body.inverseMass + tangentC2C.dot(colTangent);
//
//                    if (tangentImpulse > staticFrictImpulse) {
//                        tangentImpulse = kineticFrictImpulse;
//                    }
//                    totalImpulse.set(colTangent).mul(tangentImpulse);
//                }
//            }
//            if (contact.points.size() > 1 && body.inverseMass > 0) {
//                body.angularVelocity(angVel);
//
//                double angularImpulse = -angVel.dot(colNormal);
//                angularImpulse /= invInertia.transform(colNormal, angImpulse).dot(colNormal);
//
//                if (Math.abs(angularImpulse) > staticFrictImpulse) {
//                    angularImpulse = kineticFrictImpulse * Math.signum(angularImpulse);
//                }
//                angImpulse.set(colNormal).mul(angularImpulse);
//            }
//
//            // 4. Apply the impulses
//            totalImpulse.add(reactImpulse);
//
//            body.applyImpulse(colPoint, totalImpulse);
//            body.applyImpulseAngular(angImpulse);
//
//            totalImpulse.sub(solvePush);
//
//            if (body.linearMomentum.dot(colNormal) < 0) {
//                PhysUtil.getComponentsInPlane(colNormal, body.linearMomentum, body.linearMomentum, 0);
//            }
//
//            if (contact.points.size() >= 3) {
//                for (var limit : body.directionalAccelLimit) {
//                    if (limit.lengthSquared() <= 1e-7) {
//                        limit.set(colNormal);
//                        break;
//                    }
//                }
//            }
        }
    }
}
