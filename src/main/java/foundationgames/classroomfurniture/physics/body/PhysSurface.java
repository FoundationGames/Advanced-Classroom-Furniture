package foundationgames.classroomfurniture.physics.body;

import net.minecraft.util.Mth;

public record PhysSurface(double restitution, double kineticFriction, double staticFriction) {
    public static PhysSurface EMPTY = new PhysSurface(0, 0, 0);

    public static PhysSurface lerp(PhysSurface a, PhysSurface b, double delta) {
        return new PhysSurface(
                Mth.lerp(a.restitution, b.restitution, delta),
                Mth.lerp(a.kineticFriction, b.kineticFriction, delta),
                Mth.lerp(a.staticFriction, b.staticFriction, delta)
        );
    }
}
