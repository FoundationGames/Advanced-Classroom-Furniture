package foundationgames.classroomfurniture.mixin;

import foundationgames.classroomfurniture.physics.PhysSimulation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin implements PhysSimulation.LevelAccess {
    private final PhysSimulation classroomfurniture$simulation = new PhysSimulation((Level) (Object) this);

    @Override
    public PhysSimulation classroomfurniture$getPhysSimulation() {
        return classroomfurniture$simulation;
    }
}
