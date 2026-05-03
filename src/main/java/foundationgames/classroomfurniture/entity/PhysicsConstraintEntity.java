package foundationgames.classroomfurniture.entity;

import foundationgames.classroomfurniture.CFData;
import foundationgames.classroomfurniture.physics.PhysSimulation;
import foundationgames.classroomfurniture.physics.constraint.PhysConstraint;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class PhysicsConstraintEntity extends Entity {
    public PhysConstraint constraint;
    public UUID firstBody;
    public UUID secondBody;

    private int deathTimer = 5;

    public PhysicsConstraintEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level) {
            var sim = PhysSimulation.get(level);

            if (constraint != null && firstBody != null && secondBody != null) {
                sim.addConstraintForTick(getUUID(), constraint, firstBody, secondBody);

                var first = level().getEntity(firstBody);
                var second = level().getEntity(secondBody);

                if (first == null) {
                    if (second != null) {
                        setPos(second.position());
                    }
                } else if (second == null) {
                    setPos(first.position());
                } else {
                    setPos(second.position().add(first.position()).scale(0.5));
                }

                if (
                        (first != null && first.getRemovalReason() == RemovalReason.KILLED) ||
                        (second != null && second.getRemovalReason() == RemovalReason.KILLED)
                ) {
                    this.deathTimer--;
                } else {
                    this.deathTimer = 5;
                }

                if (this.deathTimer < 0) {
                    this.remove(RemovalReason.KILLED);
                }
            }
        }
        super.tick();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        firstBody = input.read("first_body", CFData.UUID_CODEC).orElse(null);
        secondBody = input.read("second_body", CFData.UUID_CODEC).orElse(null);

        constraint = null;
        int type = input.getIntOr("constraint_type", -1);
        if (type >= 0 && type < PhysConstraint.BY_ID.size()) {
            constraint = PhysConstraint.BY_ID.get(type).get();
            constraint.readData(input.childOrEmpty("constraint_data"));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (firstBody != null) output.store("first_body", CFData.UUID_CODEC, firstBody);
        if (secondBody != null) output.store("second_body", CFData.UUID_CODEC, secondBody);

        if (constraint != null) {
            output.putInt("constraint_type", constraint.getId());
            constraint.writeData(output.child("constraint_data"));
        }
    }
}
