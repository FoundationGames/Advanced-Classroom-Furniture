package foundationgames.classroomfurniture.client;

import net.minecraft.client.Minecraft;

public interface MinecraftAccess {
    boolean classroomfurniture$isScrollCaptured();

    static boolean isScrollCaptured(Minecraft mc) {
        return ((MinecraftAccess) mc).classroomfurniture$isScrollCaptured();
    }
}
