package foundationgames.classroomfurniture.physics.geometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.function.ToDoubleFunction;

public class PhysBox implements PhysShape {
    public Vector3d halfSize = new Vector3d();

    private static Vector3dc[] NORMALS = {
            new Vector3d(0, 1, 0),
            new Vector3d(0, -1, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(-1, 0, 0),
            new Vector3d(0, 0, 1),
            new Vector3d(0, 0, -1),
    };

    @Override
    public int faceCount() {
        return 6;
    }

    @Override
    public void getFaceNormal(int face, Vector3d faceNormal) {
        faceNormal.set(NORMALS[face]);
    }

    @Override
    public double getFaceOffsetAlongNormal(int face) {
        return switch (face) {
            case 0, 1 -> halfSize.y;
            case 2, 3 -> halfSize.x;
            case 4, 5 -> halfSize.z;
            default -> 0;
        };
    }

    @Override
    public int vertexCount() {
        return 8;
    }

    @Override
    public void getVertex(int vertex, Vector3d vertexPos) {
        double xm = (vertex & 0b001) > 0 ? 1 : -1;
        double ym = (vertex & 0b010) > 0 ? 1 : -1;
        double zm = (vertex & 0b100) > 0 ? 1 : -1;

        vertexPos.set(halfSize).mul(xm, ym, zm);
    }

    @Override
    public double circumcircleSquaredRadius() {
        return this.halfSize.lengthSquared();
    }

    @Override
    public Vector3d circumcircleOrigin(Vector3d origin) {
        return origin.zero();
    }

    @Override
    public double volume() {
        return 8 * this.halfSize.x * this.halfSize.y * this.halfSize.z;
    }

    @Override
    public boolean interpenFace(int face, Vector3dc vtx, PhysCollision manifold) {
        double threshold = getFaceOffsetAlongNormal(face);

        ToDoubleFunction<Vector3dc> projFunc = switch (face) {
            case 0 -> Vector3dc::y;
            case 1 -> v -> -v.y();
            case 2 -> Vector3dc::x;
            case 3 -> v -> -v.x();
            case 4 -> Vector3dc::z;
            case 5 -> v -> -v.z();
            default -> _ -> Double.POSITIVE_INFINITY;
        };

        double proj = projFunc.applyAsDouble(vtx);

        if (proj < threshold) {
            var axis = new Vector3d();
            getFaceNormal(face, axis);

            double pen = threshold - proj;
            manifold.addContactIfValid(vtx, axis, pen);

            return true;
        }

        return false;
    }

    @Override
    public void inertiaTensor(Matrix3d inertia) {
        double sx = halfSize.x() * 2;
        double sy = halfSize.y() * 2;
        double sz = halfSize.z() * 2;

        inertia.set(
                sy * sy + sz * sz, 0, 0,
                0, sx * sx + sz * sz, 0,
                0, 0, sx * sx + sy * sy
        ).scale(1.0 / 12);
    }

    @Override
    public @Nullable Vector3d clip(Vec3 from, Vec3 to, Vector3d clipped) {
        var maybe = AABB.clip(
                -halfSize.x(), -halfSize.y(), -halfSize.z(),
                halfSize.x(), halfSize.y(), halfSize.z(),
                from, to
        );

        if (maybe.isPresent()) {
            var clip = maybe.get();
            return clipped.set(clip.x, clip.y, clip.z);
        }

        return null;
    }

    public PhysBox set(double xs, double ys, double zs) {
        this.halfSize.set(xs * 0.5, ys * 0.5, zs * 0.5);
        return this;
    }

    public PhysBox setPx(double xs, double ys, double zs) {
        double m = 1.0 / 16;
        return set(xs * m, ys * m, zs * m);
    }
}
