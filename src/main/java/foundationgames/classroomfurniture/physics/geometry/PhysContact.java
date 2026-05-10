package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.body.PhysSurface;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class PhysContact {
    public PhysSurface firstSurface = PhysSurface.EMPTY;
    public final List<Interpen> interpens = new ArrayList<>();
    public final Vector3d normal = new Vector3d();
    public double manifoldOrigin = 0; // Offset of the contact manifold plane along the normal direction
    public double manifoldMargin = 5e-2; // Half-thickness of contact manifold plane

    public Vector3d faceResolvingAxis = new Vector3d();
    public Vector3d faceResolvingAngle = new Vector3d();

    public boolean minimizing = true;

    public void computeManifold() {
        normal.zero();
        for (var i : interpens) {
            normal.add(i.interpen);
        }
        normal.normalize();

        manifoldOrigin = 0;
        for (var i : interpens) {
            manifoldOrigin += normal.dot(i.pos);
        }
        if (!interpens.isEmpty()) manifoldOrigin /= interpens.size();
    }

    public boolean isInManifoldPlane(Vector3dc pos) {
        double proj = normal.dot(pos);
        double diff = Math.abs(manifoldOrigin - proj);

        return diff < manifoldMargin;
    }

    public void flip() {
        for (var i : this.interpens) {
            i.pos.add(i.interpen);
            i.interpen.negate();
        }

        this.computeManifold();
    }

    public static PhysContact mergedOrLeastPenetrating(PhysContact first, PhysContact ... others) {
        for (var other : others) {
            first = first.mergeOrChooseLessPenetrating(other);
        }

        return first;
    }

    public boolean roughlyMatches(PhysContact other, double margin) {
        if (this.interpens.size() != other.interpens.size()) return false;

        var ips = new HashSet<>(other.interpens);
        for (var i : this.interpens) {
            ips.removeIf(o -> o.roughlyMatches(i, margin));
        }

        return ips.isEmpty();
    }

    public PhysContact transform(Matrix4x3dc xfm) {
        for (var i : this.interpens) {
            xfm.transformPosition(i.pos);
            xfm.transformDirection(i.interpen);
        }
        this.computeManifold();

        return this;
    }

    public @Nullable PhysContact mergeOrNull(PhysContact other) {
        if (this.normal.dot(other.normal) < 0.99) {
            return null;
        }

        for (var o : other.interpens) {
            if (!isInManifoldPlane(o.pos)) {
                return null;
            }
        }

        this.interpens.addAll(other.interpens);
        this.computeManifold();
        return this;
    }

    public PhysContact mergeOrChooseLessPenetrating(PhysContact other) {
        if (this.interpens.isEmpty()) return other;
        if (other.interpens.isEmpty()) return this;

        return this.deepestDepth() < other.deepestDepth() ? this : other;

//        boolean chooseLP = this.normal.dot(other.normal) < 0.98;
//
//        if (!chooseLP) for (var o : other.interpens) {
//            if (!isInManifoldPlane(o.pos)) {
//                chooseLP = true;
//                break;
//            }
//        }
//
//        if (chooseLP) {
//            return this.deepestDepth() < other.deepestDepth() ? this : other;
//        }
//
//        for (var i : other.interpens) {
//            double dpth = i.interpen.length();
//            if (i.interpen.dot(normal) / dpth < 0.9) {
//                this.interpens.add(new Interpen(i.pos, new Vector3d(normal).mul(dpth)));
//            } else {
//                this.interpens.add(i);
//            }
//        }
//        this.interpens.addAll(other.interpens);
//
//        this.computeManifold();
//        return this;
    }

    public @Nullable Interpen deepest() {
        return interpens.stream().max(Comparator.comparingDouble(Interpen::depthSquared)).orElse(null);
    }

    public double deepestDepth() {
        return Math.sqrt(interpens.stream().map(Interpen::depthSquared).max(Comparator.comparingDouble(d -> d)).orElse(minimizing ? Double.POSITIVE_INFINITY : 0.0));
    }

    public void addContactIfValid(Vector3dc pos, Vector3dc normal, double penetration) {
        if (!this.interpens.isEmpty()) {
            double depth = deepestDepth();
            if (penetration > depth + manifoldMargin) {
                this.interpens.clear();
            } else if (penetration < depth - manifoldMargin) {
                return;
            }
        }

        this.interpens.add(new Interpen(new Vector3d(pos), new Vector3d(normal).mul(penetration)));
        computeManifold();
    }

    public void keepContactsBehindPlaneOnly(Vector3dc planeNormal, double planeOrigin) {
        this.interpens.removeIf(i -> i.pos.dot(planeNormal) > planeOrigin);
        computeManifold();
    }

    // interpen is the displacement to be applied to the second body to solve the contact
    public class Interpen {
        public final Vector3d pos;
        public final Vector3d interpen;
        public PhysSurface surface = PhysSurface.EMPTY;

        public Interpen(Vector3d pos, Vector3d interpen) {
            this.pos = pos;
            this.interpen = interpen;
        }

        public double depthSquared() {
            return interpen.lengthSquared();
        }

        public boolean roughlyMatches(Interpen other, double margin) {
            double m2 = margin * margin;
            return other.pos.distanceSquared(pos) < m2 && other.interpen.distanceSquared(interpen) < m2;
        }

        @Override
        public String toString() {
            return String.format("INTERPEN AT:[%.4f, %.4f, %.4f] DEPTH:[%.4f, %.4f, %.4f] ROUGH:%.2f FS:%.2f FK:%.2f",
                    pos.x, pos.y, pos.z, interpen.x, interpen.y, interpen.z,
                    surface.restitution(), surface.staticFriction(), surface.kineticFriction());
        }
    }
}
