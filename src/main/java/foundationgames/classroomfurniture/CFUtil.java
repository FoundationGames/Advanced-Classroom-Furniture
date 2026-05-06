package foundationgames.classroomfurniture;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import foundationgames.classroomfurniture.physics.PhysUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Matrix4x3d;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public enum CFUtil {;
    public static final List<WoodType> WOOD = ImmutableList.of(
            WoodType.OAK, WoodType.BIRCH, WoodType.SPRUCE, WoodType.JUNGLE, WoodType.ACACIA, WoodType.DARK_OAK,
            WoodType.MANGROVE, WoodType.PALE_OAK, WoodType.CHERRY, WoodType.BAMBOO,
            WoodType.CRIMSON, WoodType.WARPED
    );

    public static Matrix4x3d getHeadTransform(Entity entity, Matrix4x3d xfm) {
        var zMc = entity.getHeadLookAngle();
        var z = new Vector3d(zMc.x, zMc.y, zMc.z).normalize();
        var y = new Vector3d(0, 1, 0);
        var x = z.cross(y, new Vector3d());
        z.cross(x, y);
        xfm.setColumn(0, x);
        xfm.setColumn(1, y);
        xfm.setColumn(2, z);

        var o = entity.getEyePosition();
        xfm.setTranslation(o.x, o.y, o.z);

        return PhysUtil.orthonormalize(xfm, xfm);
    }

    public static <K, V> HashMap<K, V> buildMapFromStream(Stream<K> stream, Function<K, V> mapper) {
        return stream.map(k -> new Pair<>(k, mapper.apply(k)))
                .collect(
                        HashMap::new,
                        (map, pair) -> map.put(pair.getFirst(), pair.getSecond()),
                        HashMap::putAll
                );
    }
}
