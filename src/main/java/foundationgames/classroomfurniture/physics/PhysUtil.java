package foundationgames.classroomfurniture.physics;

import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Matrix4x3d;
import org.joml.Matrix4x3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public enum PhysUtil {;
    public static Matrix3d outerProduct(Vector3dc a, Vector3dc b, Matrix3d outerProduct) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                outerProduct.setRowColumn(r, c, a.get(r) * b.get(c));
            }
        }

        return outerProduct;
    }

    public static Matrix3d extractBasis(Matrix4x3dc xfm, Matrix3d basis) {
        basis.set(
                xfm.m00(), xfm.m01(), xfm.m02(),
                xfm.m10(), xfm.m11(), xfm.m12(),
                xfm.m20(), xfm.m21(), xfm.m22()
        );

        return basis;
    }

    public static Matrix4x3d insertBasis(Matrix3dc basis, Matrix4x3d xfm) {
        xfm.set(
                basis.m00(), basis.m01(), basis.m02(),
                basis.m10(), basis.m11(), basis.m12(),
                basis.m20(), basis.m21(), basis.m22(),
                xfm.m30(), xfm.m31(), xfm.m32()
        );

        return xfm;
    }

    public static Matrix3d transformBasisDirections(Matrix4x3dc xfm, Matrix3d basis) {
        basis.set(
                xfm.m00() * basis.m00 + xfm.m01() * basis.m10 + xfm.m02() * basis.m20,
                xfm.m00() * basis.m01 + xfm.m01() * basis.m11 + xfm.m02() * basis.m21,
                xfm.m00() * basis.m02 + xfm.m01() * basis.m12 + xfm.m02() * basis.m22,

                xfm.m10() * basis.m00 + xfm.m11() * basis.m10 + xfm.m12() * basis.m20,
                xfm.m10() * basis.m01 + xfm.m11() * basis.m11 + xfm.m12() * basis.m21,
                xfm.m10() * basis.m02 + xfm.m11() * basis.m12 + xfm.m12() * basis.m22,

                xfm.m20() * basis.m00 + xfm.m21() * basis.m10 + xfm.m22() * basis.m20,
                xfm.m20() * basis.m01 + xfm.m21() * basis.m11 + xfm.m22() * basis.m21,
                xfm.m20() * basis.m02 + xfm.m21() * basis.m12 + xfm.m22() * basis.m22
        );

        return basis;
    }

    public static Matrix3d projectSquareBasisOntoTransformBasis(Matrix4x3dc xfm, Matrix3d basis) {
        if ((xfm.properties() & Matrix4x3dc.PROPERTY_TRANSLATION) != 0) return basis;

        return transformBasisDirections(xfm, basis.mul(extractBasis(xfm, new Matrix3d()).transpose()));
    }

    public static Vector3d pointOnLineClosestToOtherLine(Vector3dc lineOrigin, Vector3dc lineDir, Vector3dc otherLineOrigin, Vector3dc otherLineDir, Vector3d point) {
        var diagonal = new Vector3d(lineOrigin).sub(otherLineOrigin);

        double lineDiagonal = diagonal.dot(lineDir);
        double otherLineDiagonal = diagonal.dot(otherLineDir);
        double parallelness = lineDir.dot(otherLineDir);

        double perpendicularity = 1 - parallelness * parallelness;

        if (Math.abs(perpendicularity) <= 0) {
            point.set(lineOrigin).add(otherLineOrigin).mul(0.5);
            return point;
        }

        double ext = (parallelness * otherLineDiagonal - lineDiagonal) / perpendicularity;
        return point.set(lineDir).mul(ext).add(lineOrigin);
    }

    public static Vector3d getComponentsInPlane(Vector3dc planeNormal, Vector3dc vec, Vector3d planeComponentsOnly, double margin) {
        double proj = vec.dot(planeNormal);
        if (Math.abs(proj) < margin) {
            proj = margin * Math.signum(proj);
        }
        return planeComponentsOnly.set(vec).add(planeNormal.mul(-proj, new Vector3d()));
    }

    public static Vector3d flattenOntoPlane(Vector3dc planeNormal, Vector3dc planeOrigin, Vector3dc toFlatten, Vector3d flattened) {
        var r = new Vector3d(toFlatten).sub(planeOrigin);
        return flattened.set(planeNormal).mul(r.dot(planeNormal)).negate().add(toFlatten);
    }

    public static Vector3d getComponentOnLine(Vector3dc lineDirection, Vector3dc vec, Vector3d lineComponentOnly, double margin) {
        double proj = vec.dot(lineDirection);
        if (Math.abs(proj) < margin) {
            proj = margin * Math.signum(proj);
        }
        return lineComponentOnly.set(lineDirection).mul(proj);
    }

    public static <V extends Vector3dc> int indexOfNearest(Vector3dc point, List<V> points) {
        if (point == null) return -1;

        double minDist = Double.POSITIVE_INFINITY;
        int minPoint = -1;

        for (int i = 0; i < points.size(); i++) {
            var otherPt = points.get(i);
            if (otherPt == null) continue;

            double sqDist = point.distanceSquared(otherPt);
            if (sqDist < minDist) {
                minPoint = i;
                minDist = sqDist;
            }
        }

        return minPoint;
    }

    // I cannot tell if JOML has a builtin way to do this
    public static Matrix4x3d orthonormalize(Matrix4x3dc mat, Matrix4x3d nmlzd) {
        var z = new Vector3d();
        var y = new Vector3d();
        var x = new Vector3d();

        // Gram-Schmidt process
        nmlzd.setColumn(2, mat.getColumn(2, z).normalize());
        nmlzd.setColumn(1, getComponentsInPlane(z, mat.getColumn(1, y), y, 0).normalize());
        nmlzd.setColumn(0, getComponentsInPlane(z, getComponentsInPlane(y, mat.getColumn(0, x), x, 0).normalize(), x, 0).normalize());
        nmlzd.determineProperties();

        return nmlzd.setTranslation(mat.getTranslation(z));
    }
}
