package foundationgames.classroomfurniture.physics.body;

import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.geometry.PhysCollision;
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

    public double density = 1;
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
    public final Set<PhysCollision> prevCollisions = new HashSet<>();
    public final Set<PhysCollision> collisions = new HashSet<>();

    public boolean sleep = false;
    public boolean wellSupported = false;
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

    public boolean isEffectivelyAtRest() {
        if (!wellSupported) return false;

        var v = new Vector3d();
        return velocityCG(v).lengthSquared() < 0.0005 && angularVelocity(v).lengthSquared() < 0.0005;
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

        m.zero();
        if (inverseMass > 0) m.set(acceleration).div(inverseMass);
        m.add(force).mul(dt);
        this.linearMomentum.add(m);

        m.zero();
        if (inverseMass > 0) inverseInertia.transform(m.set(angularAcceleration));
        m.add(torque).mul(dt);
        this.angularMomentum.add(m);

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

        this.prevCollisions.clear();
        this.prevCollisions.addAll(this.collisions);
        this.collisions.clear();
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
        this.inverseMass = 0;
        for (var s : solids) {
            this.inverseMass += s.shape().volume() * this.density;
        }
        this.inverseMass = 1.0 / this.inverseMass;

        calculateInertia(this.inverseInertia).invert().scale(inverseMass);
        return this;
    }

    /*
        Do you want to know how collision works? I didn't think so

        https://allenchou.net/2013/12/game-physics-resolution-contact-constraints/
        Uses some kind of insane Lagrange multiplier method to derive constraints on the velocities during a collision,
        and also explains Baumgarte stabilization which is a way to "cushion" the impact of colliding so that the bodies
        converge to something stable-ish; it effectively turns the reaction impulse into a disgusting PD control loop
        type thing. If you simplify the insane matrices-of-vectors notation used here you will get the much more common
        impulse-based-collision-reaction expression that you can find pretty much everywhere else such as:
        https://gafferongames.com/post/collision_response_and_coulomb_friction/
        https://en.wikipedia.org/wiki/Collision_response#Impulse-Based_Reaction_Model

        I tried to implement the sequential impulse method here, it is not even remotely stable unless I calculate the
        collision impulses repeatedly like 4 times. Doesn't really matter because the real performance hog seems to be
        the box intersection contact solving

        Most of the open knowledge about collision solvers and numerical rigid body simulations comes from the author
        of Box2D. I did not look very much into Box2D because Minecraft is 3D and Box2D is not. You have to take into
        account more things in 3D, there are more contacts, more inertia terms, etc and you can make way more
        simplifications in 2D. Check it out anyway because it's probably the best thing out there for its purpose:
        https://github.com/erincatto/box2d/tree/main
     */

    public static boolean collideBodies(PhysBody first, PhysBody second, double dt) {
        var fsShapeWorld = new PhysTransformedShape();
        fsShapeWorld.transform.set(first.transform);
        var ssShapeWorld = new PhysTransformedShape();
        ssShapeWorld.transform.set(second.transform);

        var firstCtr = first.transform.getTranslation(new Vector3d());
        var secondCtr = second.transform.getTranslation(new Vector3d());

        var colManifolds = new ArrayList<PhysCollision>();
        double margin = dt * 0.6;

        for (var fs : first.solids) {
            for (var ss : second.solids) {
                fsShapeWorld.shape = fs.shape();
                ssShapeWorld.shape = ss.shape();

                var col = PhysShape.collide(fsShapeWorld, ssShapeWorld, margin);
                if (col == null) continue;

                first.collidingWith.add(second.uuid);
                second.collidingWith.add(first.uuid);

                col.firstSurface = fs.surface();
                for (var i : col.contacts) {
                    i.surface = ss.surface();
                }

                boolean merged = false;

                for (var foundCol : colManifolds) {
                    if (foundCol.mergeOrNull(col) != null) {
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    colManifolds.add(col);
                }
            }
        }

        // TODO: Make warm starting work because it obviously doesn't work clearly obviously

        if (colManifolds.isEmpty()) return false;

        var ctNormal = new Vector3d();
        var ctPoint = new Vector3d();
        var ctTangent = new Vector3d();

        var firstVel = new Vector3d();
        var secondVel = new Vector3d();
        var closingVel = new Vector3d();
        var combinedAcc = new Vector3d();

        var solve = new Vector3d();

        var firstRContact = new Vector3d();
        var secondRContact = new Vector3d();
        var firstInertiaTangent = new Vector3d();
        var secondInertiaTangent = new Vector3d();
        var totalInertiaTangent = new Vector3d();

        var firstInvInertia = first.localInverseInertia(new Matrix3d());
        var secondInvInertia = second.localInverseInertia(new Matrix3d());

        var reactImpulse = new Vector3d();
        var slideImpulse = new Vector3d();

        for (var col : colManifolds) {
            var deepest = col.deepest();
            if (deepest == null) continue;

            double depth = col.deepestDepth();
            double maxPen = margin;
            double slop = 0.005;
            double a = 0.1;
            double b = 18;

            double magicKFrictionConstant = 1.59;
            double contactImpulseWeight = Math.min(1.0, 2.0 / col.contacts.size());

            for (int i = 0; i < 4; i++) for (var ct : col.contacts) {
                ctNormal.set(ct.interpen).normalize();
                ctPoint.set(ct.pos);

                first.velocityAt(ctPoint, firstVel);
                firstRContact.set(firstCtr).sub(ctPoint);
                second.velocityAt(ctPoint, secondVel);
                secondRContact.set(secondCtr).sub(ctPoint);

                closingVel.set(firstVel).sub(secondVel);
                combinedAcc.set(first.acceleration).add(second.acceleration).mul(0.5); // Idk

                firstInvInertia.transform(firstInertiaTangent.set(firstRContact).cross(ctNormal));
                firstInertiaTangent.cross(firstRContact);
                secondInvInertia.transform(secondInertiaTangent.set(secondRContact).cross(ctNormal));
                secondInertiaTangent.cross(secondRContact);

                totalInertiaTangent.set(firstInertiaTangent).add(secondInertiaTangent);

                double normalVel = Math.max(0, closingVel.dot(ctNormal));
                double normalAccel = -combinedAcc.dot(ctNormal);
                double restitution = Math.min(col.firstSurface.restitution(), ct.surface.restitution());

                double impulseMagnitude = ((1 + restitution) * normalVel);
                impulseMagnitude += (Math.max(0, ct.interpen.length() - slop) * b * (1 + normalAccel * a)) * dt;

                impulseMagnitude /= first.inverseMass + second.inverseMass + totalInertiaTangent.dot(ctNormal);

                reactImpulse.set(ctNormal).mul(impulseMagnitude);

                // 3. Calculate the friction impulses
                PhysUtil.getComponentsInPlane(ctNormal, closingVel, ctTangent, 0);

                double staticFrict = 0.5 * (col.firstSurface.staticFriction() + ct.surface.staticFriction());
                double kineticFrict = Math.min(staticFrict * 0.99, 0.5 * (col.firstSurface.kineticFriction() + ct.surface.kineticFriction()));

                double staticFrictImpulse = staticFrict * impulseMagnitude;
                double kineticFrictImpulse = kineticFrict * impulseMagnitude;

                slideImpulse.zero();
                if (ctTangent.lengthSquared() > 0) {
                    ctTangent.normalize();

                    firstInvInertia.transform(firstInertiaTangent.set(firstRContact).cross(ctTangent));
                    firstInertiaTangent.cross(firstRContact);
                    secondInvInertia.transform(secondInertiaTangent.set(secondRContact).cross(ctTangent));
                    secondInertiaTangent.cross(secondRContact);

                    totalInertiaTangent.set(firstInertiaTangent).add(secondInertiaTangent);

                    double tangentImpulse = closingVel.dot(ctTangent) * magicKFrictionConstant;
                    tangentImpulse /= first.inverseMass + second.inverseMass + totalInertiaTangent.dot(ctTangent);

                    if (tangentImpulse > staticFrictImpulse) {
                        tangentImpulse = kineticFrictImpulse;
                    }
                    slideImpulse.set(ctTangent).mul(tangentImpulse);
                }

                reactImpulse.add(slideImpulse);
                second.applyImpulse(ctPoint, reactImpulse.mul(contactImpulseWeight));
                first.applyImpulse(ctPoint, reactImpulse.negate());
            }

            if (depth > maxPen) {
                second.transform.translateLocal(solve.set(ctNormal).mul(depth - maxPen));
                first.transform.translateLocal(solve.set(ctNormal).mul(maxPen - depth));
            }
        }

        return true;
    }

    public static void collideStaticSolids(PhysBody body, List<PhysSolid> solids, double dt) {
        var bsShapeWorld = new PhysTransformedShape();
        bsShapeWorld.transform.set(body.transform);
        var ctr = body.transform.getTranslation(new Vector3d());

        var colManifolds = new ArrayList<PhysCollision>();
        double margin = dt * 0.6;

        for (var bs : body.solids) {
            for (var solid : solids) {
                bsShapeWorld.shape = bs.shape();

                var col = PhysShape.collide(solid.shape(), bsShapeWorld, margin);
                if (col == null) continue;

                col.firstSurface = bs.surface();
                for (var i : col.contacts) {
                    i.surface = solid.surface();
                }

                boolean merged = false;

                for (var foundCol : colManifolds) {
                    if (foundCol.mergeOrNull(col) != null) {
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    colManifolds.add(col);
                }
            }
        }

        for (int i = 0; i < colManifolds.size(); i++) {
            PhysCollision memorized = null;
            for (var contact : body.prevCollisions) {
                if (colManifolds.get(i).roughlyMatches(contact, dt)) {
                    memorized = contact;
                    break;
                }
            }

            if (memorized != null) {
                body.prevCollisions.remove(memorized);
                colManifolds.set(i, memorized);
            }
        }

        body.wellSupported = false;

        if (colManifolds.isEmpty()) return;
        body.collisions.addAll(colManifolds);

        var ctNormal = new Vector3d();
        var ctPoint = new Vector3d();
        var ctTangent = new Vector3d();

        var solve = new Vector3d();

        var vel = new Vector3d();
        var rContact = new Vector3d();
        var inertiaTangent = new Vector3d();
        var invInertia = body.localInverseInertia(new Matrix3d());

        var reactImpulse = new Vector3d();
        var slideImpulse = new Vector3d();

        for (var contact : colManifolds) {
            var deepest = contact.deepest();
            if (deepest == null) continue;

            //System.out.println("CTR:" + colPoint + " DIR:" + col.normal + " WT:" + col.contacts.size() + " PEN:" + col.deepestDepth());
            //System.out.println(col.contacts.size() + " -> " + col.contacts);

            double depth = contact.deepestDepth();
            double maxPen = margin;
            double slop = 0.005;
            double a = 0.1;
            double b = 18;

            // Why do these make it more stable? WHo knows
            double magicKFrictionConstant = 1.59;
            double reactImpulseWeight = Math.min(1.0, 0.8 / contact.contacts.size() * body.inverseMass);
            double slideImpulseWeight = Math.min(1.0, 2.0 / contact.contacts.size() * body.inverseMass);

            for (int i = 0; i < 4; i++) for (var ct : contact.contacts) {
                ctNormal.set(ct.interpen).normalize();
                ctPoint.set(ct.pos);

                body.velocityAt(ctPoint, vel);
                rContact.set(ctr).sub(ctPoint);

                invInertia.transform(inertiaTangent.set(rContact).cross(ctNormal));
                inertiaTangent.cross(rContact);

                double normalVel = Math.max(0, -vel.dot(ctNormal));
                double normalAccel = -body.acceleration.dot(ctNormal);
                double restitution = Math.min(contact.firstSurface.restitution(), ct.surface.restitution());

                double impulseMagnitude = ((1 + restitution) * normalVel);
                impulseMagnitude += (Math.max(0, ct.interpen.length() - slop) * b * (1 + normalAccel * a)) * dt;

                impulseMagnitude /= body.inverseMass + inertiaTangent.dot(ctNormal);

                reactImpulse.set(ctNormal).mul(impulseMagnitude);

                PhysUtil.getComponentsInPlane(ctNormal, vel, ctTangent, 0);

                double staticFrict = 0.5 * (contact.firstSurface.staticFriction() + ct.surface.staticFriction());
                double kineticFrict = Math.min(staticFrict * 0.99, 0.5 * (contact.firstSurface.kineticFriction() + ct.surface.kineticFriction()));

                double staticFrictImpulse = staticFrict * impulseMagnitude;
                double kineticFrictImpulse = kineticFrict * impulseMagnitude;

                slideImpulse.zero();
                if (ctTangent.lengthSquared() > 0) {
                    ctTangent.normalize();

                    invInertia.transform(inertiaTangent.set(rContact).cross(ctTangent));
                    inertiaTangent.cross(rContact);

                    double tangentImpulse = vel.dot(ctTangent) * magicKFrictionConstant;
                    tangentImpulse /= body.inverseMass + inertiaTangent.dot(ctTangent);

                    if (tangentImpulse > staticFrictImpulse) {
                        tangentImpulse = kineticFrictImpulse;
                    }
                    slideImpulse.set(ctTangent).mul(-tangentImpulse);
                }

                reactImpulse.mul(reactImpulseWeight);
                slideImpulse.mul(slideImpulseWeight);

                reactImpulse.add(slideImpulse);

                body.applyImpulse(ctPoint, reactImpulse);
            }

            if (depth > maxPen) {
                body.transform.translateLocal(solve.set(ctNormal).mul(depth - maxPen));
            }

            body.wellSupported |= contact.isPosInManifoldShadow(ctr) && new Vector3d(body.acceleration).normalize().dot(contact.normal) < -0.99;
        }
    }
}
