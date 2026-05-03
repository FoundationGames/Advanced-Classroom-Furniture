package foundationgames.classroomfurniture.item;

import foundationgames.classroomfurniture.entity.CFEntities;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import foundationgames.classroomfurniture.physics.constraint.FastenedPinConstraint;
import net.minecraft.world.entity.EntitySpawnReason;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class GlueGrabberItem extends GrabberItem {
    public GlueGrabberItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void onUngrab(PhysicsPropEntity prop) {
        super.onUngrab(prop);

        var level = prop.level();
        var propPos = prop.position();
        var propRotInv = new Matrix3d(prop.rotation).invert();

        if (!level.isClientSide()) for (var touchingID : prop.touchingOtherProps) {
            var touching = level.getEntity(touchingID);

            if (touching instanceof PhysicsPropEntity touchingProp) {
                var touchPos = touchingProp.position();
                var touchRotInv = new Matrix3d(touchingProp.rotation).invert();
                var constraint = CFEntities.CONSTRAINT.create(level, EntitySpawnReason.SPAWN_ITEM_USE);

                var touchingToProp = new Vector3d(touchPos.x - propPos.x, touchPos.y - propPos.y, touchPos.z - propPos.z);
                touchRotInv.transform(touchingToProp.negate());

                var propAx1 = propRotInv.transform(new Vector3d(1, 0, 0));
                var propAx2 = propRotInv.transform(new Vector3d(0, 0, 1));
                var touchAx1 = touchRotInv.transform(new Vector3d(1, 0, 0));
                var touchAx2 = touchRotInv.transform(new Vector3d(0, 0, 1));

                var pin = new FastenedPinConstraint();
                pin.firstPin.set(0, 0, 0);
                pin.secondPin.set(touchingToProp);

                pin.firstSwivelAxis1.set(propAx1);
                pin.firstSwivelAxis2.set(propAx2);
                pin.secondSwivelAxis1.set(touchAx1);
                pin.secondSwivelAxis2.set(touchAx2);

                constraint.firstBody = prop.getUUID();
                constraint.secondBody = touchingProp.getUUID();
                constraint.constraint = pin;
                constraint.setPos(propPos);

                level.addFreshEntity(constraint);
            }
        }
    }
}
