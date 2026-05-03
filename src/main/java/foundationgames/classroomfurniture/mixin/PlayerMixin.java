package foundationgames.classroomfurniture.mixin;

import foundationgames.classroomfurniture.item.GrabInfo;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public class PlayerMixin implements GrabInfo.Access {
    private GrabInfo classroomfurniture$grab = new GrabInfo();

    @Override
    public GrabInfo classroomfurniture$getGrabStatus() {
        classroomfurniture$grab.grabber = ((Player) (Object) this).getUUID();
        return classroomfurniture$grab;
    }
}
