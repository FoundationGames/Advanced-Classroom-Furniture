package foundationgames.classroomfurniture.item;

import foundationgames.classroomfurniture.CFUtil;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import foundationgames.classroomfurniture.network.GrabPropPackets;
import foundationgames.classroomfurniture.physics.PhysUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix3d;
import org.joml.Matrix4x3d;
import org.joml.Vector3d;

public class GrabberItem extends Item implements FullMouseControlItem {
    public GrabberItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean captureAttackKey(Player user, InteractionHand hand, @Nullable HitResult hit) {
        return true;
    }

    @Override
    public boolean captureUseKey(Player user, InteractionHand hand, @Nullable HitResult hit) {
        return GrabInfo.get(user).grabbedProp != null;
    }

    @Override
    public boolean captureMotion(Player user, InteractionHand hand, @Nullable HitResult hit) {
        return GrabInfo.get(user).rotating;
    }

    @Override
    public boolean captureScroll(Player user, InteractionHand hand, @Nullable HitResult hit) {
        return GrabInfo.get(user).grabbedProp != null;
    }

    protected void onUngrab(PhysicsPropEntity prop) {
    }

    @Override
    public void rawAttackKey(boolean down, Player user, InteractionHand hand, @Nullable HitResult hit) {
        var grab = new GrabInfo();
        grab.grabber = user.getUUID();

        if (hit instanceof EntityHitResult ehit && ehit.getEntity() instanceof PhysicsPropEntity prop && down) {
            grab.grabber = user.getUUID();
            grab.grabbedProp = prop.getUUID();

            var loc = hit.getLocation();
            var ePos = prop.position();
            grab.grabPointOnProp.set(loc.x - ePos.x, loc.y - ePos.y, loc.z - ePos.z);

            var bRot = prop.getSideIndependentRotation(new Matrix3d());
            var bInvRot = bRot.invert(new Matrix3d());

            bInvRot.transform(grab.grabPointOnProp);

            var eHeadXfm = CFUtil.getHeadTransform(user, new Matrix4x3d());
            PhysUtil.insertBasis(bRot, grab.grabRelativePose);
            grab.grabRelativePose.setTranslation(ePos.x, ePos.y, ePos.z);

            eHeadXfm.invert().mul(grab.grabRelativePose, grab.grabRelativePose);
        }

        var stack = user.getItemInHand(hand);
        GrabInfo.get(user).updateOnSelfAndProp(user.level(), stack, grab);
        ClientPlayNetworking.send(new GrabPropPackets.ServerboundUpdateGrab(stack, grab));
    }

    @Override
    public void rawUseKey(boolean down, Player user, InteractionHand hand, @Nullable HitResult hit) {
        var grab = GrabInfo.get(user);
        grab.rotating = down;
        var stack = user.getItemInHand(hand);
        ClientPlayNetworking.send(new GrabPropPackets.ServerboundUpdateGrab(stack, grab));
    }

    @Override
    public void rawMotion(double dx, double dy, Player user, InteractionHand hand, @Nullable HitResult hit) {
        var grab = GrabInfo.get(user);
        if (grab.rotating) {
            var tl = grab.grabRelativePose.getTranslation(new Vector3d());
            grab.grabRelativePose.setTranslation(0, 0, 0);
            grab.grabRelativePose.rotateLocalY(-Math.signum(dx) * Math.sqrt(Math.abs(dx)) * 0.008);
            grab.grabRelativePose.rotateLocalX(Math.signum(dy) * Math.sqrt(Math.abs(dy)) * 0.008);
            grab.grabRelativePose.setTranslation(tl);
        }

        var stack = user.getItemInHand(hand);
        ClientPlayNetworking.send(new GrabPropPackets.ServerboundUpdateGrab(stack, grab));
    }

    @Override
    public void rawScroll(int amount, Player user, InteractionHand hand, @Nullable HitResult hit) {

    }
}
