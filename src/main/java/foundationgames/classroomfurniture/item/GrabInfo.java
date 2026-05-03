package foundationgames.classroomfurniture.item;

import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3d;
import org.joml.Vector3d;

import java.util.UUID;

public class GrabInfo {
    public @Nullable UUID grabbedProp;
    public @Nullable UUID grabber;
    public boolean rotating = false;
    public final Vector3d grabPointOnProp = new Vector3d();
    public final Matrix4x3d grabRelativePose = new Matrix4x3d();

    public static final StreamCodec<FriendlyByteBuf, GrabInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GrabInfo decode(FriendlyByteBuf buf) {
            return new GrabInfo(buf);
        }

        @Override
        public void encode(FriendlyByteBuf buf, GrabInfo grab) {
            grab.writePacket(buf);
        }
    };

    public GrabInfo() {
    }

    public GrabInfo(FriendlyByteBuf buf) {
        this.grabbedProp = buf.readNullable(RegistryFriendlyByteBuf::readUUID);
        this.grabber = buf.readNullable(RegistryFriendlyByteBuf::readUUID);
        this.rotating = buf.readBoolean();

        for (int r = 0; r < 3; r++) {
            grabPointOnProp.setComponent(r, buf.readDouble());
        }

        for (int c = 0; c < 4; c++) {
            this.grabRelativePose.setColumn(c, new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
    }

    public void clear() {
        grabbedProp = null;
        rotating = false;
        grabPointOnProp.zero();
        grabRelativePose.identity();
    }

    public void set(GrabInfo other) {
        grabber = other.grabber;
        grabbedProp = other.grabbedProp;
        rotating = other.rotating;
        grabPointOnProp.set(other.grabPointOnProp);
        grabRelativePose.set(other.grabRelativePose);
    }

    public void updateOnSelfAndProp(Level level, ItemStack stack, GrabInfo other) {
        if (other.grabbedProp != null) {
            var grabbedEntity = level.getEntity(other.grabbedProp);

            if (grabbedEntity instanceof PhysicsPropEntity prop) {
                if (this.grabbedProp == null) {
                    prop.grabs.add(this);
                }

                this.set(other);
            }
        } else if (this.grabber != null && this.grabbedProp != null) {
            var grabbedEntity = level.getEntity(this.grabbedProp);

            if (grabbedEntity instanceof PhysicsPropEntity prop) {
                prop.grabs.removeIf(info -> this.grabber.equals(info.grabber));

                if (stack.getItem() instanceof GrabberItem item) {
                    item.onUngrab(prop);
                }
            }

            clear();
        }
    }

    public void writePacket(FriendlyByteBuf buf) {
        buf.writeNullable(this.grabbedProp, RegistryFriendlyByteBuf::writeUUID);
        buf.writeNullable(this.grabber, RegistryFriendlyByteBuf::writeUUID);
        buf.writeBoolean(this.rotating);

        for (int r = 0; r < 3; r++) {
            buf.writeDouble(grabPointOnProp.get(r));
        }

        var col = new Vector3d();
        for (int c = 0; c < 4; c++) {
            this.grabRelativePose.getColumn(c, col);
            buf.writeDouble(col.x);
            buf.writeDouble(col.y);
            buf.writeDouble(col.z);
        }
    }

    public static GrabInfo get(Player player) {
        return ((Access) player).classroomfurniture$getGrabStatus();
    }

    public interface Access {
        GrabInfo classroomfurniture$getGrabStatus();
    }
}
