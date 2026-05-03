package foundationgames.classroomfurniture;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.util.UUID;

public enum CFData {;
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final Codec<Matrix3d> MATRIX3D_CODEC = Codec.list(Codec.DOUBLE, 9, 9)
            .xmap(
                    l -> {
                        var a = new double[9];
                        for (int i = 0; i < a.length; i++) {
                            a[i] = l.get(i);
                        }
                        return new Matrix3d().set(a);
                    },
                    m -> {
                        var a = m.get(new double[9]);
                        var l = new ImmutableList.Builder<Double>();
                        for (double d : a) l.add(d);
                        return l.build();
                    }
            );
    public static final Codec<Vector3d> VECTOR3D_CODEC = Codec.list(Codec.DOUBLE, 3, 3)
            .xmap(
                    l -> {
                        var a = new double[3];
                        for (int i = 0; i < a.length; i++) {
                            a[i] = l.get(i);
                        }
                        return new Vector3d().set(a);
                    },
                    m -> {
                        var l = new ImmutableList.Builder<Double>();
                        for (int i = 0; i < 3; i++) l.add(m.get(i));
                        return l.build();
                    }
            );


}
