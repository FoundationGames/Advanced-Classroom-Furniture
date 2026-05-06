package foundationgames.classroomfurniture.physics.geometry;

import foundationgames.classroomfurniture.physics.PhysUtil;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix4x3d;
import org.joml.Matrix4x3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PhysTransformedShape implements PhysShape {
    public @NotNull PhysShape shape = PhysShape.EMPTY;
    public final Matrix4x3d transform = new Matrix4x3d();

    @Override
    public int faceCount() {
        return shape.faceCount();
    }

    @Override
    public void getFaceNormal(int face, Vector3d faceNormal) {
        shape.getFaceNormal(face, faceNormal);
        transform.transformDirection(faceNormal);
    }

    @Override
    public double getFaceOffsetAlongNormal(int face) {
        var fn = new Vector3d();
        this.getFaceNormal(face, fn);

        return shape.getFaceOffsetAlongNormal(face) + transform.getTranslation(new Vector3d()).dot(fn);
    }

    @Override
    public int vertexCount() {
        return shape.vertexCount();
    }

    @Override
    public void getVertex(int vertex, Vector3d vertexPos) {
        shape.getVertex(vertex, vertexPos);
        transform.transformPosition(vertexPos);
    }

    @Override
    public double circumcircleSquaredRadius() {
        return shape.circumcircleSquaredRadius();
    }

    @Override
    public Vector3d circumcircleOrigin(Vector3d origin) {
        return transform.transformPosition(shape.circumcircleOrigin(origin));
    }

    @Override
    public double volume() {
        return shape.volume();
    }

    @Override
    public boolean interpenFace(int face, Vector3dc vtx, PhysContact manifold) {
        var vtxLocal = new Vector3d(vtx);
        this.transform.invert(new Matrix4x3d()).transformPosition(vtxLocal);

        var manifoldLocal = new PhysContact();
        if (shape.interpenFace(face, vtxLocal, manifoldLocal)) {
            manifoldLocal.transform(this.transform);
            manifold.interpens.addAll(manifoldLocal.interpens);

            return true;
        }

        return false;
    }

    @Override
    public void inertiaTensor(Matrix3d inertia) {
        shape.inertiaTensor(inertia);
        PhysUtil.projectSquareBasisOntoTransformBasis(this.transform, inertia);

        if ((this.transform.properties() & Matrix4x3dc.PROPERTY_IDENTITY) == 0) {
            var m = new Matrix3d();
            var origin = this.transform.getTranslation(new Vector3d());
            double v = shape.volume();

            m.identity().scale(origin.lengthSquared() * v);
            inertia.add(m);

            PhysUtil.outerProduct(origin, origin, m).scale(v);
            inertia.sub(m);
        }
    }

    @Override
    public @Nullable Vector3d clip(Vec3 from, Vec3 to, Vector3d clipped) {
        var xfmInv = this.transform.invert(new Matrix4x3d());

        var fromx = xfmInv.transformPosition(new Vector3d(from.x, from.y, from.z));
        var tox = xfmInv.transformPosition(new Vector3d(to.x, to.y, to.z));

        var clip = shape.clip(
                new Vec3(fromx.x, fromx.y, fromx.z),
                new Vec3(tox.x, tox.y, tox.z),
                clipped
        );

        if (clip != null) {
            return transform.transformPosition(clipped.set(clip));
        }
        return null;
    }
}
