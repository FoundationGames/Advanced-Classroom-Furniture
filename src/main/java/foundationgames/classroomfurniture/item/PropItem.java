package foundationgames.classroomfurniture.item;

import foundationgames.classroomfurniture.PropDefinition;
import foundationgames.classroomfurniture.entity.CFEntities;
import foundationgames.classroomfurniture.entity.PhysicsConstraintEntity;
import foundationgames.classroomfurniture.entity.PhysicsPropEntity;
import foundationgames.classroomfurniture.physics.constraint.PhysConstraint;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.function.Supplier;

public class PropItem extends Item {
    public final PropDefinition prop;
    public final @Nullable Supplier<PhysConstraint> constraint;
    public final @Nullable PropDefinition extension;

    public PropItem(Properties properties, PropDefinition prop, @Nullable Supplier<PhysConstraint> constraint, @Nullable PropDefinition extension) {
        super(properties);
        this.prop = prop;
        this.constraint = constraint;
        this.extension = extension;
    }

    public PropItem(Properties properties, PropDefinition prop) {
        this(properties, prop, null, null);
    }

    private PhysicsPropEntity placeProp(ServerLevel level, Vec3 pos, double yaw, PropDefinition prop) {
        var v = new Vector3d();
        double yOffset = 0;

        for (var sld : prop.solids()) {
            var shp = sld.shape();

            for (int i = 0; i < shp.vertexCount(); i++) {
                shp.getVertex(i, v);
                yOffset = Math.min(yOffset, v.y());
            }
        }

        var entity = prop.entity().get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);

        if (entity != null) {
            entity.setPos(pos.add(0, -yOffset, 0));
            entity.rotation.rotateY(Math.round(-16 * yaw / (2*Math.PI)) * 2 * Math.PI / 16);
            level.addFreshEntity(entity);
            entity.forceRotationUpdate();
        }

        return entity;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var lv = ctx.getLevel();
        if (lv instanceof ServerLevel level) {
            double yaw = ctx.getRotation() * (Math.PI / 180.0);
            var propEntity = placeProp(level, ctx.getClickLocation(), yaw, prop);
            if (extension != null) {
                var extEntity = placeProp(level, ctx.getClickLocation(), yaw, extension);

                if (propEntity != null && extEntity != null && constraint != null) {

                    var constraintEntity = new PhysicsConstraintEntity(CFEntities.CONSTRAINT, level);
                    constraintEntity.firstBody = propEntity.getUUID();
                    constraintEntity.secondBody = extEntity.getUUID();
                    constraintEntity.constraint = constraint.get();
                    constraintEntity.setPos(ctx.getClickLocation());

                    level.addFreshEntity(constraintEntity);
                }
            }

            propEntity.itemDrop = ctx.getItemInHand().copy();
            propEntity.itemDrop.setCount(1);

            var player = ctx.getPlayer();
            if (player != null && !player.isCreative()) {
                ctx.getItemInHand().shrink(1);
            }

            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }
}
