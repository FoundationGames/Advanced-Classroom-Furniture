package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.PhysUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;

public interface PhysShape {
    PhysShape EMPTY = new Empty();

    int faceCount();
    void getFaceNormal(int face, Vector3d faceNormal);
    double getFaceOffsetAlongNormal(int face);

    int vertexCount();
    void getVertex(int vertex, Vector3d vertexPos);

    double circumcircleSquaredRadius();
    Vector3d circumcircleOrigin(Vector3d origin);

    double volume();

    boolean interpenFace(int face, Vector3dc vtx, PhysCollision manifold);
    void inertiaTensor(Matrix3d inertia);

    @Nullable Vector3d clip(Vec3 from, Vec3 to, Vector3d clipped);

    default AABB bounds() {
        var vtx = new Vector3d();
        double x0 = Double.POSITIVE_INFINITY;
        double y0 = Double.POSITIVE_INFINITY;
        double z0 = Double.POSITIVE_INFINITY;
        double x1 = Double.NEGATIVE_INFINITY;
        double y1 = Double.NEGATIVE_INFINITY;
        double z1 = Double.NEGATIVE_INFINITY;

        for (int v = 0; v < vertexCount(); v++) {
            this.getVertex(v, vtx);
            x0 = Math.min(x0, vtx.x());
            y0 = Math.min(y0, vtx.y());
            z0 = Math.min(z0, vtx.z());
            x1 = Math.max(x1, vtx.x());
            y1 = Math.max(y1, vtx.y());
            z1 = Math.max(z1, vtx.z());
        }

        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    static boolean shapeCircumcirclesIntersect(PhysShape first, PhysShape second) {
        var fo = first.circumcircleOrigin(new Vector3d());
        var so = first.circumcircleOrigin(new Vector3d());
        double halfDistSquared = fo.distanceSquared(so) * 0.25;

        return halfDistSquared < first.circumcircleSquaredRadius() || halfDistSquared < second.circumcircleSquaredRadius();
    }

    static @Nullable PhysCollision collide(PhysShape first, PhysShape second, double margin) {
        if (!PhysShape.shapeCircumcirclesIntersect(first, second)) return null;

        var ctFaceFS = PhysShape.collideFaceFeature(first, second, margin);
        if (ctFaceFS == null) return null;

        var ctFaceSF = PhysShape.collideFaceFeature(second, first, margin);
        if (ctFaceSF == null) return null;
        ctFaceSF.flip();

        var ctFaceEdges = PhysShape.collideEdges(first, second, margin);
        if (ctFaceEdges == null) return null;

        return PhysCollision.mergedOrLeastPenetrating(ctFaceFS, ctFaceSF, ctFaceEdges);
    }

    static @Nullable PhysCollision collideFaceFeature(PhysShape first, PhysShape second, double margin) {
        PhysCollision result = null;

        var vtx = new Vector3d();

        for (int face = 0; face < first.faceCount(); face++) {
            boolean intersecting = false;
            PhysCollision faceResult = new PhysCollision();
            faceResult.manifoldMargin = margin;

            for (int svi = 0; svi < second.vertexCount(); svi++) {
                second.getVertex(svi, vtx);

                if (first.interpenFace(face, vtx, faceResult)) {
                    intersecting = true;
                }
            }

            if (!intersecting) return null; // Failed separating axis test

            if (result == null || result.deepestDepth() > faceResult.deepestDepth()) {
                result = faceResult;
            }
        }

        if (result == null) return null;

        var axis = new Vector3d();
        for (int face = 0; face < first.faceCount(); face++) {
            first.getFaceNormal(face, axis);
            double faceOffset = first.getFaceOffsetAlongNormal(face);

            result.keepContactsBehindPlaneOnly(axis, faceOffset);
        }

        return result;
    }

    static @Nullable PhysCollision collideEdges(PhysShape first, PhysShape second, double margin) {
        var fvec = new Vector3d();
        var svec = new Vector3d();
        var vtx = new Vector3d();
        var axis = new Vector3d();

        double minFMax = Double.POSITIVE_INFINITY;
        double minSMin = Double.NEGATIVE_INFINITY;
        var minPenAxis = new Vector3d();

        for (int fface = 0; fface < first.faceCount(); fface++) {
            for (int sface = 0; sface < second.faceCount(); sface++) {
                first.getFaceNormal(fface, fvec);
                second.getFaceNormal(sface, svec);

                axis.set(fvec).cross(svec);
                if (axis.length() < 1e-9) continue; // The bodies are aligned on these two faces

                axis.normalize();

                double fMax = Double.NEGATIVE_INFINITY;
                for (int fv = 0; fv < first.vertexCount(); fv++) {
                    first.getVertex(fv, vtx);

                    double t = vtx.dot(axis);
                    if (!Double.isFinite(fMax) || t > fMax) {
                        fMax = t;
                    }
                }

                double sMin = Double.POSITIVE_INFINITY;
                for (int sv = 0; sv < second.vertexCount(); sv++) {
                    second.getVertex(sv, vtx);

                    double t = vtx.dot(axis);
                    if (!Double.isFinite(sMin) || t < sMin) {
                        sMin = t;
                    }
                }

                double penetration = fMax - sMin;
                if (penetration < 0) return null; // Bodies are separated

                if (penetration < minFMax - minSMin) {
                    minFMax = fMax;
                    minSMin = sMin;
                    minPenAxis.set(axis);
                }
            }
        }

        var firstPoints = new ArrayList<Vector3d>();
        var secondPoints = new ArrayList<Vector3d>();
        var woundFirstPoints = new ArrayList<Vector3d>();
        var woundSecondPoints = new ArrayList<Vector3d>();

        for (int fvi = 0; fvi < first.vertexCount(); fvi++) {
            first.getVertex(fvi, vtx);
            if (vtx.dot(minPenAxis) > minSMin) {
                firstPoints.add(new Vector3d(vtx));
            }
        }

        for (int svi = 0; svi < second.vertexCount(); svi++) {
            second.getVertex(svi, vtx);
            if (vtx.dot(minPenAxis) < minFMax) {
                secondPoints.add(new Vector3d(vtx));
            }
        }

        int maxIter = firstPoints.size();
        int pi = 0;
        for (int i = 0; i < maxIter; i++) {
            var pt = firstPoints.get(pi);
            woundFirstPoints.add(pt);
            firstPoints.remove(pi);

            int nextIdx = PhysUtil.indexOfNearest(pt, firstPoints);
            if (nextIdx >= 0) {
                pi = nextIdx;
            } else break;
        }

        maxIter = secondPoints.size();
        pi = 0;
        for (int i = 0; i < maxIter; i++) {
            var pt = secondPoints.get(pi);
            woundSecondPoints.add(pt);
            secondPoints.remove(pi);

            int nextIdx = PhysUtil.indexOfNearest(pt, secondPoints);
            if (nextIdx >= 0) {
                pi = nextIdx;
            } else break;
        }

        var result = new PhysCollision();
        result.manifoldMargin = margin;
        if (woundFirstPoints.isEmpty()) return result;
        if (woundSecondPoints.isEmpty()) return result;

        var fEdgePos = new Vector3d();
        var fEdgeDir = new Vector3d();
        var sEdgePos = new Vector3d();
        var sEdgeDir = new Vector3d();

        double penetration = minFMax - minSMin;

        for (int sEdge = 0; sEdge < woundSecondPoints.size(); sEdge++) {
            var sPt1 = woundSecondPoints.get(sEdge);
            var sPt2 = woundSecondPoints.get((sEdge + 1) % woundSecondPoints.size());

            sEdgePos.set(sPt1);
            sEdgeDir.set(sPt2).sub(sPt1).normalize();

            for (int fEdge = 0; fEdge < woundFirstPoints.size(); fEdge++) {
                var fPt1 = woundFirstPoints.get(fEdge);
                var fPt2 = woundFirstPoints.get((fEdge + 1) % woundFirstPoints.size());

                fEdgePos.set(fPt1);
                fEdgeDir.set(fPt2).sub(fPt1).normalize();

                if (Math.abs(fEdgeDir.dot(sEdgeDir)) > 0.99) continue;

                var colPoint = PhysUtil.pointOnLineClosestToOtherLine(sEdgePos, sEdgeDir, fEdgePos, fEdgeDir, new Vector3d());

                if (colPoint != null) {
                    double fPMin = fPt1.dot(fEdgeDir);
                    double fPMax = fPt2.dot(fEdgeDir);
                    double fPCol = colPoint.dot(fEdgeDir);

                    double sPMin = sPt1.dot(sEdgeDir);
                    double sPMax = sPt2.dot(sEdgeDir);
                    double sPCol = colPoint.dot(sEdgeDir);

                    //var msg = String.format("(%.3f<%.3f<%.3f) and (%.3f<%.3f<%.3f) ", fPMin, fPCol, fPMax, sPMin, sPCol, sPMax);

                    if (fPMin <= fPCol && fPCol <= fPMax && sPMin <= sPCol && sPCol <= sPMax) {
                        result.addContactIfValid(colPoint, minPenAxis, penetration);
                    }
                }
            }
        }

        return result;
    }

    static PhysShape offset(PhysShape shape, Vector3dc offset) {
        var offsetShape = new PhysTransformedShape();
        offsetShape.shape = shape;
        offsetShape.transform.translate(offset);

        return offsetShape;
    }

    class Empty implements PhysShape {
        @Override
        public int faceCount() {
            return 0;
        }

        @Override
        public void getFaceNormal(int face, Vector3d faceNormal) {
            faceNormal.zero();
        }

        @Override
        public double getFaceOffsetAlongNormal(int face) {
            return 0;
        }

        @Override
        public int vertexCount() {
            return 0;
        }

        @Override
        public void getVertex(int vertex, Vector3d vertexPos) {
            vertexPos.zero();
        }

        @Override
        public double circumcircleSquaredRadius() {
            return 0;
        }

        @Override
        public Vector3d circumcircleOrigin(Vector3d origin) {
            return origin.zero();
        }

        @Override
        public double volume() {
            return 0;
        }

        @Override
        public boolean interpenFace(int face, Vector3dc vtx, PhysCollision manifold) {
            return false;
        }

        @Override
        public void inertiaTensor(Matrix3d inertia) {
            inertia.zero();
        }

        @Override
        public @Nullable Vector3d clip(Vec3 from, Vec3 to, Vector3d clipped) {
            return null;
        }
    }
}
