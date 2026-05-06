package foundationgames.classroomfurniture.entity;

import foundationgames.classroomfurniture.PropDefinition;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PhysicsSeatPropEntity extends PhysicsPropEntity {
    private final Vector3dc localSeatPos;

    public PhysicsSeatPropEntity(EntityType<?> type, Level level, PropDefinition prop, Vector3dc localSeatPos) {
        super(type, level, prop);
        this.localSeatPos = localSeatPos;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.canAddPassenger(player)) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            } else {
                player.startRiding(this);
                return InteractionResult.CONSUME;
            }
        }

        return super.interact(player, hand, location);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        var seatPos = new Vector3d(localSeatPos);
        var os = passenger.getVehicleAttachmentPoint(this);
        seatPos.sub(os.x, os.y, os.z);
        getSideIndependentRotation(new Matrix3d()).transform(seatPos);

        var pos = this.position();

        moveFunction.accept(passenger, pos.x + seatPos.x, pos.y + seatPos.y, pos.z + seatPos.z);
    }
}
