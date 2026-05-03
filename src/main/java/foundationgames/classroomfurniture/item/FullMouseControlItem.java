package foundationgames.classroomfurniture.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public interface FullMouseControlItem {
    boolean captureAttackKey(Player user, InteractionHand hand, @Nullable HitResult hit);
    boolean captureUseKey(Player user, InteractionHand hand, @Nullable HitResult hit);
    boolean captureMotion(Player user, InteractionHand hand, @Nullable HitResult hit);
    boolean captureScroll(Player user, InteractionHand hand, @Nullable HitResult hit);

    void rawAttackKey(boolean down, Player user, InteractionHand hand, @Nullable HitResult hit);
    void rawUseKey(boolean down, Player user, InteractionHand hand, @Nullable HitResult hit);
    void rawMotion(double dx, double dy, Player user, InteractionHand hand, @Nullable HitResult hit);
    void rawScroll(int amount, Player user, InteractionHand hand, @Nullable HitResult hit);
}
