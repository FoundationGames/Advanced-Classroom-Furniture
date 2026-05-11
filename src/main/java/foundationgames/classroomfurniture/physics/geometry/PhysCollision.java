package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.body.PhysSurface;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class PhysCollision {
    public PhysSurface firstSurface = PhysSurface.EMPTY;
    public final List<Contact> contacts = new ArrayList<>();
    public final Vector3d normal = new Vector3d();
    public double manifoldOrigin = 0; // Offset of the contact manifold plane along the normal direction
    public double manifoldMargin = 5e-2; // Half-thickness of contact manifold plane

    public boolean minimizing = true;

    public void computeManifold() {
        normal.zero();
        for (var i : contacts) {
            normal.add(i.interpen);
        }
        normal.normalize();

        manifoldOrigin = 0;
        for (var i : contacts) {
            manifoldOrigin += normal.dot(i.pos);
        }
        if (!contacts.isEmpty()) manifoldOrigin /= contacts.size();
    }

    public boolean isInManifoldPlane(Vector3dc pos) {
        double proj = normal.dot(pos);
        double diff = Math.abs(manifoldOrigin - proj);

        return diff < manifoldMargin;
    }

    public void flip() {
        for (var i : this.contacts) {
            i.pos.add(i.interpen);
            i.interpen.negate();
        }

        this.computeManifold();
    }

    public static PhysCollision mergedOrLeastPenetrating(PhysCollision first, PhysCollision... others) {
        for (var other : others) {
            first = first.mergeOrChooseLessPenetrating(other);
        }

        return first;
    }

    public boolean roughlyMatches(PhysCollision other, double margin) {
        if (this.contacts.size() != other.contacts.size()) return false;

        var ips = new HashSet<>(other.contacts);
        for (var i : this.contacts) {
            ips.removeIf(o -> o.roughlyMatches(i, margin));
        }

        return ips.isEmpty();
    }

    public PhysCollision transform(Matrix4x3dc xfm) {
        for (var i : this.contacts) {
            xfm.transformPosition(i.pos);
            xfm.transformDirection(i.interpen);
        }
        this.computeManifold();

        return this;
    }

    public @Nullable PhysCollision mergeOrNull(PhysCollision other) {
        if (this.normal.dot(other.normal) < 0.99) {
            return null;
        }

        for (var o : other.contacts) {
            if (!isInManifoldPlane(o.pos)) {
                return null;
            }
        }

        this.contacts.addAll(other.contacts);
        this.computeManifold();
        return this;
    }

    public PhysCollision mergeOrChooseLessPenetrating(PhysCollision other) {
        if (this.contacts.isEmpty()) return other;
        if (other.contacts.isEmpty()) return this;

        boolean chooseLP = this.normal.dot(other.normal) < 0.98;

        if (!chooseLP) for (var o : other.contacts) {
            if (!isInManifoldPlane(o.pos)) {
                chooseLP = true;
                break;
            }
        }

        if (chooseLP) {
            return this.deepestDepth() < other.deepestDepth() ? this : other;
        }

        for (var i : other.contacts) {
            double dpth = i.interpen.length();
            if (i.interpen.dot(normal) / dpth < 0.9) {
                this.contacts.add(new Contact(i.pos, new Vector3d(normal).mul(dpth)));
            } else {
                this.contacts.add(i);
            }
        }
        this.contacts.addAll(other.contacts);

        this.computeManifold();
        return this;
    }

    public @Nullable PhysCollision.Contact deepest() {
        return contacts.stream().max(Comparator.comparingDouble(Contact::depthSquared)).orElse(null);
    }

    public double deepestDepth() {
        return Math.sqrt(contacts.stream().map(Contact::depthSquared).max(Comparator.comparingDouble(d -> d)).orElse(minimizing ? Double.POSITIVE_INFINITY : 0.0));
    }

    public void addContactIfValid(Vector3dc pos, Vector3dc normal, double penetration) {
        if (!this.contacts.isEmpty()) {
            double depth = deepestDepth();
            if (penetration > depth + manifoldMargin) {
                this.contacts.clear();
            } else if (penetration < depth - manifoldMargin) {
                return;
            }
        }

        this.contacts.add(new Contact(new Vector3d(pos), new Vector3d(normal).mul(penetration)));
        computeManifold();
    }

    public void keepContactsBehindPlaneOnly(Vector3dc planeNormal, double planeOrigin) {
        this.contacts.removeIf(i -> i.pos.dot(planeNormal) > planeOrigin);
        computeManifold();
    }

    public boolean isPosInManifoldShadow(Vector3dc pos) {
        final double eps = 1e-4;

        if (this.contacts.size() < 3) {
            return false;
        }

        var origin = new Vector3d();
        for (var ct : contacts) origin.add(ct.pos);
        origin.div(contacts.size());

        var posInManifoldPlane = new Vector3d(pos);
        PhysUtil.flattenOntoPlane(this.normal, origin, pos, posInManifoldPlane);

        var axis = new Vector3d(posInManifoldPlane).sub(origin);
        if (axis.lengthSquared() <= eps) {
            return true;
        }

        double threshold = axis.length();
        axis.normalize();

        var point = new Vector3d();
        for (var ct : this.contacts) {
            point.set(ct.pos).sub(origin);
            double reach = point.dot(axis);

            if (reach > threshold) {
                return true;
            }
        }

        return false;
    }

    // interpen is the displacement to be applied to the second body to solve the contact
    public class Contact {
        public final Vector3d pos;
        public final Vector3d interpen;
        public PhysSurface surface = PhysSurface.EMPTY;

        public Contact(Vector3d pos, Vector3d interpen) {
            this.pos = pos;
            this.interpen = interpen;
        }

        public double depthSquared() {
            return interpen.lengthSquared();
        }

        public boolean roughlyMatches(Contact other, double margin) {
            double m2 = margin * margin;
            return other.pos.distanceSquared(pos) < m2 && other.interpen.distanceSquared(interpen) < m2;
        }

        @Override
        public String toString() {
            return String.format("CONTACT AT:[%.4f, %.4f, %.4f] DEPTH:[%.4f, %.4f, %.4f] ROUGH:%.2f FS:%.2f FK:%.2f",
                    pos.x, pos.y, pos.z, interpen.x, interpen.y, interpen.z,
                    surface.restitution(), surface.staticFriction(), surface.kineticFriction());
        }
    }
}
