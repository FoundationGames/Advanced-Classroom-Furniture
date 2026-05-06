package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.PhysUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class PhysAABB extends PhysBox {
    public final Vector3d origin = new Vector3d();

    @Override
    public void getVertex(int vertex, Vector3d vertexPos) {
        super.getVertex(vertex, vertexPos);

        vertexPos.add(origin);
    }

    @Override
    public void inertiaTensor(Matrix3d inertia) {
        super.inertiaTensor(inertia);
        var m = new Matrix3d();
        double v = volume();

        m.scale(origin.lengthSquared() * v);
        inertia.add(m);

        PhysUtil.outerProduct(origin, origin, m).scale(v);
        inertia.sub(m);
    }

    @Override
    protected double facePenetrationThreshold(int face) {
        return super.facePenetrationThreshold(face) + switch (face) {
            case 0 -> origin.y;
            case 1 -> -origin.y;
            case 2 -> origin.x;
            case 3 -> -origin.x;
            case 4 -> origin.z;
            case 5 -> -origin.z;
            default -> 0;
        };
    }

    @Override
    public @Nullable Vector3d clip(Vec3 from, Vec3 to, Vector3d clipped) {
        var maybe = AABB.clip(
                origin.x() - halfSize.x(), origin.y() - halfSize.y(), origin.z() - halfSize.z(),
                origin.x() + halfSize.x(), origin.y() + halfSize.y(), origin.z() + halfSize.z(),
                from, to
        );

        if (maybe.isPresent()) {
            var clip = maybe.get();
            return clipped.set(clip.x, clip.y, clip.z);
        }

        return null;
    }

    @Override
    public Vector3d circumcircleOrigin(Vector3d origin) {
        return origin.set(this.origin);
    }

    public PhysAABB set(AABB aabb) {
        return setAABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public PhysAABB setAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        halfSize.set(
                0.5 * (x2 - x1),
                0.5 * (y2 - y1),
                0.5 * (z2 - z1)
        );

        origin.set(x1, y1, z1).add(halfSize);
        return this;
    }

    public PhysAABB setCornerSize(double x, double y, double z, double xs, double ys, double zs) {
        halfSize.set(0.5 * xs, 0.5 * ys, 0.5 * zs);
        origin.set(x, y, z).add(halfSize);
        return this;
    }

    public PhysAABB setCornerSizePx(double x, double y, double z, double xs, double ys, double zs) {
        double m = 1.0 / 16;
        return setCornerSize(x * m, y * m, z * m, xs * m, ys * m, zs * m);
    }

    public static PhysShape cornerSizePivotRotatedPx(
            double x, double y, double z,
            double xs, double ys, double zs,
            double px, double py, double pz,
            double rx, double ry, double rz) {
        var aabb = new PhysAABB().setCornerSizePx(x, y, z, xs, ys, zs);
        var shape = new PhysTransformedShape();
        shape.shape = aabb;

        double r = Math.PI / 180.0;
        shape.transform.rotateX(rx * r);
        shape.transform.rotateY(ry * r);
        shape.transform.rotateZ(rz * r);

        double m = 1.0 / 16;
        px *= m; py *= m; pz *= m;
        shape.transform.translateLocal(px, py, pz);
        shape.transform.translate(-px, -py, -pz);
        //shape.transform.rotateLocalX(Math.PI);

        return shape;
    }
}
