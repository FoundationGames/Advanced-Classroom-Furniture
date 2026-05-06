package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.body.PhysSurface;
import org.joml.Matrix4x3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

public class PhysWeirdInterpenCollectionThing {
    public final List<Vector3d> points = new ArrayList<>();
    public final Vector3d direction = new Vector3d();
    public double penetration = Double.NEGATIVE_INFINITY;

    public PhysSurface surfaceA = null;
    public PhysSurface surfaceB = null;

    public void clear() {
        points.clear();
        direction.zero();
        penetration = Double.NEGATIVE_INFINITY;
        surfaceA = null;
        surfaceB = null;
    }

    public PhysWeirdInterpenCollectionThing transform(Matrix4x3dc xfm) {
        for (var point : points) {
            xfm.transformPosition(point);
        }
        xfm.transformDirection(direction);
        return this;
    }

    public PhysWeirdInterpenCollectionThing tryCombine(PhysWeirdInterpenCollectionThing other) {
        if (points.isEmpty()) {
            return other;
        }

        if (other.points.isEmpty()) {
            return this;
        }

        double penetrationDiff = Math.abs(penetration - other.penetration);
        if (penetrationDiff > 1e-3) {
            return null;
        }

        double angleDiff = direction.dot(other.direction);
        if (angleDiff < 1 - 1e-4) {
            return null;
        }

        var result = new PhysWeirdInterpenCollectionThing();
        result.points.addAll(points);

        var r = new Vector3d();
        for (var oPoint : other.points) {
            boolean unmerged = true;

            for (int rpi = 0; rpi < result.points.size(); rpi++) {
                Vector3dc point = result.points.get(rpi);
                r.set(point).sub(oPoint);
                if (Math.abs(r.dot(direction)) > 1e-2 * r.length()) return null;
                if (r.lengthSquared() < 1e-8) {
                    result.points.set(rpi, new Vector3d(point).add(oPoint).mul(0.5));
                    unmerged = false;
                    break;
                }
            }

            if (unmerged) {
                result.points.add(new Vector3d(oPoint));
            }
        }

        double delta = (double) points.size() / result.points.size();

        direction.lerp(other.direction, delta, result.direction);
        result.penetration = Math.max(penetration, other.penetration);

        if (surfaceA != null && other.surfaceA != null) {
            result.surfaceA = PhysSurface.lerp(surfaceA, other.surfaceA, delta);
        }
        if (surfaceB != null && other.surfaceB != null) {
            result.surfaceB = PhysSurface.lerp(surfaceB, other.surfaceB, delta);
        }

        return result;
    }

    public void getCollisionCenter(Vector3dc cg, Vector3d colCenter) {
        final double eps = 1e-4;

        if (this.points.isEmpty()) {
            colCenter.set(cg);
            return;
        }

        if (this.points.size() == 1) {
            colCenter.set(this.points.getFirst());
            return;
        }

        if (this.points.size() == 2) {
            colCenter.set(this.points.getFirst()).add(this.points.getLast()).mul(0.5);
            return;
        }

        var origin = new Vector3d();
        for (var point : points) origin.add(point);
        origin.div(points.size());

        var cgInPlane = new Vector3d(cg);
        PhysUtil.flattenOntoPlane(this.direction, origin, cg, cgInPlane);

        var axis = new Vector3d(cgInPlane).sub(origin);
        if (axis.lengthSquared() <= 1e-4) {
            colCenter.set(cgInPlane);
            return;
        }

        double threshold = axis.length();
        double maxReach = Double.NEGATIVE_INFINITY;
        var maxReachEdgeCtr = new Vector3d();
        int reachingVtxCt = 0;
        axis.normalize();

        var point = new Vector3d();
        for (var wPoint : this.points) {
            point.set(wPoint).sub(origin);
            double reach = point.dot(axis);

            if (reach > threshold) {
                colCenter.set(cgInPlane);
                return;
            }

            if (reach > maxReach + eps) {
                maxReach = reach;
                reachingVtxCt = 1;
                maxReachEdgeCtr.set(wPoint);
            } else if (reach > maxReach - eps) {
                reachingVtxCt++;
                maxReachEdgeCtr.add(wPoint);
            }
        }

        if (reachingVtxCt <= 0) {
            colCenter.set(cg);
            return;
        }

        colCenter.set(maxReachEdgeCtr).div(reachingVtxCt);
    }

    public void getPenetratingNormal(Vector3d out) {
        if (points.isEmpty()) return;
        if (points.size() == 1) {
            out.set(direction);
            return;
        }
        if (points.size() == 2) {
            var tang = points.getFirst().sub(points.getLast(), new Vector3d()).normalize();
            var bitang = tang.cross(direction);
            bitang.cross(tang, out);
            if (out.dot(direction) < 0) {
                out.negate();
            }
            out.normalize();
            return;
        }

        Vector3dc ori = points.getFirst();
        var tang1 = new Vector3d();
        var tang2 = new Vector3d();
        int halfPoints = (points.size() - 1) / 2;
        for (int i = 0; i < halfPoints; i++) {
            for (int j = 0; j < halfPoints; j++) {
                int pt1 = i + 1;
                int pt2 = j + 1 + halfPoints;

                tang1.set(points.get(pt1)).sub(ori);
                tang2.set(points.get(pt2)).sub(ori);

                tang1.cross(tang2);
                if (tang1.dot(direction) < 0) {
                    tang1.negate();
                }
                out.add(tang1);
            }
        }
        out.normalize();
    }

    public PhysWeirdInterpenCollectionThing flipSelf() {
        direction.negate();
        var offset = new Vector3d(direction).mul(penetration);
        for (var point : points) {
            point.add(offset);
        }
        return this;
    }
}
