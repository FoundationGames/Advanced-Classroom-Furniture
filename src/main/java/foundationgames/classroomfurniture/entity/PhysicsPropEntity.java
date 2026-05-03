package foundationgames.classroomfurniture.entity;

import com.mojang.serialization.Codec;
import foundationgames.classroomfurniture.CFUtil;
import foundationgames.classroomfurniture.PropDefinition;
import foundationgames.classroomfurniture.item.CFItems;
import foundationgames.classroomfurniture.item.GrabInfo;
import foundationgames.classroomfurniture.physics.PhysSimulation;
import foundationgames.classroomfurniture.physics.PhysUtil;
import foundationgames.classroomfurniture.physics.body.PhysBody;
import foundationgames.classroomfurniture.physics.geometry.PhysTransformedShape;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix4x3d;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PhysicsPropEntity extends Entity {
    private static final EntityDataAccessor<Quaternionfc> ROTATION = SynchedEntityData.defineId(PhysicsPropEntity.class, EntityDataSerializers.QUATERNION);

    public final PropDefinition prop;

    private final Quaternionf prevRemoteRotation = new Quaternionf();
    private final Quaternionf remoteRotation = new Quaternionf();
    private int rotInterpSteps = 0;
    private boolean firstRotUpdate = true;

    public ItemStack itemDrop = null;

    private final InterpolationHandler posInterpolator = new InterpolationHandler(this, getType().updateInterval());

    public final Matrix3d rotation = new Matrix3d();
    public final Vector3d externalImpulse = new Vector3d();
    public final Vector3d externalAngularImpulse = new Vector3d();

    private @Nullable PhysBody physicsBody = null;

    public final List<GrabInfo> grabs = new ArrayList<>();
    public final List<UUID> touchingOtherProps = new ArrayList<>();

    private boolean authorityOverBody = true;

    public PhysicsPropEntity(EntityType<?> type, Level level, PropDefinition prop) {
        super(type, level);
        this.prop = prop;
    }

    protected final PhysBody createPhysicsBody() {
        var body = new PhysBody(prop.solids()).rotated(this.rotation).positioned(this.position());
        body.inverseMass = 1.0 / prop.mass();
        body.remote = level().isClientSide();
        body.calculateProperties();

        return body;
    }

    protected final boolean canFindBody() {
        return PhysSimulation.get(level()).hasBody(getUUID());
    }

    protected PhysBody getPhysicsBody() {
        if (!canFindBody()) {
            var sim = PhysSimulation.get(level());
            if (this.physicsBody != null) {
                sim.removeBodyByReference(this.physicsBody);
            }

            this.physicsBody = sim.getOrCreateBody(getUUID(), this::createPhysicsBody);
        }

        return this.physicsBody;
    }

    public @Nullable Vec3 clip(Vec3 from, Vec3 to) {
        var closest = new Vector3d();
        double closestDist = Double.POSITIVE_INFINITY;

        if (canFindBody()) {
            var body = getPhysicsBody();

            if (body != null) {
                var clipped = new Vector3d();
                var xfmShape = new PhysTransformedShape();
                xfmShape.transform.set(body.transform);

                try {
                    body.mutex.acquire();

                    for (var solid : body.solids) {
                        xfmShape.shape = solid.shape();
                        var result = xfmShape.clip(from, to, clipped);

                        if (result != null) {
                            double dist = result.distanceSquared(from.x, from.y, from.z);
                            if (dist < closestDist) {
                                closestDist = dist;
                                closest.set(result);
                            }
                        }
                    }

                    body.mutex.release();
                } catch (InterruptedException ignored) {}
            }
        }

        if (Double.isFinite(closestDist)) {
            return new Vec3(closest.x, closest.y, closest.z);
        }

        return null;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        if (canFindBody()) {
            var body = getPhysicsBody();
            AABB bounds = null;

            try {
                body.mutex.acquire();
                bounds = body.bounds();
                body.mutex.release();
            } catch (InterruptedException ignored) {
            }

            if (bounds != null) {
                return bounds;
            }
        }

        return super.makeBoundingBox(position);
    }

    public void forceRotationUpdate() {
        var body = getPhysicsBody();
        PhysUtil.insertBasis(this.rotation, body.transform);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isRemoved()) return;

        var level = this.level();
        if (level.isClientSide()) {
            interpolateTransformToRemote();

            var body = getPhysicsBody();
            body.transform.identity();
            body.positioned(position());
            body.transform.rotate(remoteRotation);
        } else {
            var body = getPhysicsBody();
            var targetPos = position();
            try {
                body.mutex.acquire();
                authorityOverBody = false;

                targetPos = body.getMCPosition();
                body.getRotation(this.rotation).normal();
                getEntityData().set(ROTATION, this.rotation.getNormalizedRotation(new Quaternionf()));

                if (!grabs.isEmpty()) for (var grab : grabs) {
                    var grabberEntity = level().getEntity(grab.grabber);
                    if (grabberEntity != null) {
                        var headPose = CFUtil.getHeadTransform(grabberEntity, new Matrix4x3d());
                        var targetPose = headPose.mul(grab.grabRelativePose, new Matrix4x3d());

                        var targetCG = targetPose.getTranslation(new Vector3d());
                        var targetAxisZ = targetPose.getColumn(2, new Vector3d());
                        var targetAxisY = targetPose.getColumn(1, new Vector3d());

                        var currentCG = body.transform.getTranslation(new Vector3d());
                        var travel = targetCG.sub(currentCG, new Vector3d()).mul(24);
                        body.linearMomentum.set(travel).div(body.inverseMass);

                        if (travel.lengthSquared() > PhysBody.GRAVITY * PhysBody.GRAVITY) {
                            travel.normalize(PhysBody.GRAVITY);
                        }

                        var currentAxisZ = body.transform.getColumn(2, new Vector3d());
                        var currentAxisY = body.transform.getColumn(1, new Vector3d());

                        var rotateToZ = currentAxisZ.cross(targetAxisZ, new Vector3d());
                        var rotateToY = currentAxisY.cross(targetAxisY, new Vector3d());

                        body.localInverseInertia(new Matrix3d()).invert()
                                .transform(body.angularMomentum.set(rotateToZ).add(rotateToY).mul(16));
                        body.acceleration.zero();
                        body.angularAcceleration.zero();
                    }
                } else {
                    body.acceleration.set(0, -PhysBody.GRAVITY, 0);
                }

                if (externalImpulse.lengthSquared() > 0) {
                    body.applyImpulseCG(externalImpulse);
                    externalImpulse.zero();
                }

                if (externalAngularImpulse.lengthSquared() > 0) {
                    body.applyImpulseAngular(externalAngularImpulse);
                    externalAngularImpulse.zero();
                }

                touchingOtherProps.clear();
                touchingOtherProps.addAll(body.collidingWith);

                authorityOverBody = true;
                body.mutex.release();
            } catch (InterruptedException ignored) {}

            setPos(targetPos);
        }
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.itemDrop;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof LivingEntity attacker &&
                attacker.getItemInHand(InteractionHand.MAIN_HAND).is(CFItems.HAMMER) &&
                (!(attacker instanceof Player player) || player.getAbilities().mayBuild)) {
            var rng = level.getRandom();
            level.playSeededSound(null, getX(), getY(), getZ(),
                    SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS,
                    0.8f, 1.8f + 0.2f * rng.nextFloat(),
                    rng.nextInt());
            level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);

            if (this.itemDrop != null && (!(attacker instanceof Player player) || !player.isCreative())) {
                var drop = new ItemEntity(level, getX(), getY(), getZ(), this.itemDrop);
                level.addFreshEntity(drop);
            }

            this.kill(level);
            return true;
        }

        var pos = source.getSourcePosition();
        if (pos != null) {
            var body = getPhysicsBody();
            try {
                body.mutex.acquire();

                Vec3 dir = null;
                var attacker = source.getEntity();
                if (attacker != null) {
                    dir = attacker.getHeadLookAngle();
                    pos = pos.add(0, attacker.getEyeHeight(), 0);
                }

                damage = (float) (Math.sqrt(damage) * 2 / body.inverseMass);

                if (dir != null) {
                    body.applyImpulse(new Vector3d(pos.x, pos.y, pos.z), new Vector3d(dir.x, dir.y, dir.z).mul(damage));
                } else {
                    dir = position().subtract(pos).normalize();
                    body.applyImpulseCG(new Vector3d(dir.x, dir.y, dir.z).mul(damage));
                }

                body.mutex.release();
            } catch (InterruptedException ignored) {}
        }

        return true;
    }

//    @Override
//    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
//        var result = super.interact(player, hand, location);
//        if (!result.consumesAction()) {
//            if (!level().isClientSide()) {
//                if (level() instanceof ServerLevel slevel) {
//                    var pt = location.add(position());
//                    slevel.sendParticles(ParticleTypes.FLAME, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
//                }
//                result = InteractionResult.CONSUME;
//            } else {
//                result = InteractionResult.SUCCESS;
//            }
//        }
//        return result;
//    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);

        if (authorityOverBody) {
            var body = getPhysicsBody();

            try {
                body.mutex.acquire();
                body.transform.setTranslation(x, y, z);
                body.mutex.release();
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        PhysSimulation.get(this.level()).removeBody(this.getUUID());

        super.remove(reason);
    }

    protected void interpolateTransformToRemote() {
        this.posInterpolator.interpolate();

        this.prevRemoteRotation.set(this.remoteRotation);
        if (this.rotInterpSteps > 0) {
            float delta = 1 / (float) rotInterpSteps;
            this.remoteRotation.slerp(this.getEntityData().get(ROTATION), delta);
            this.rotInterpSteps--;
        } else {
            this.remoteRotation.set(this.getEntityData().get(ROTATION));
        }
    }

    @Override
    public @org.jspecify.annotations.Nullable InterpolationHandler getInterpolation() {
        return this.posInterpolator;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);

        if (data.equals(ROTATION)) {
            if (this.firstRotUpdate) {
                this.firstRotUpdate = false;
                this.remoteRotation.set(getEntityData().get(ROTATION));
                this.prevRemoteRotation.set(this.remoteRotation);
            }
            this.rotInterpSteps = this.getType().updateInterval();
        }
    }

    public void getClientRemoteRotation(Quaternionf q, float delta) {
        this.prevRemoteRotation.slerp(this.remoteRotation, delta, q);
    }

    public Matrix3d getSideIndependentRotation(Matrix3d rot) {
        if (level().isClientSide()) {
            rot.identity().rotate(this.remoteRotation);
        } else {
            rot.set(this.rotation);
        }
        return rot;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(ROTATION, new Quaternionf());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.itemDrop = input.read("prop_drop_item", ItemStack.CODEC).orElse(null);

        var rotMatMaybe = input.list("body_orientation", Codec.DOUBLE);
        if (rotMatMaybe.isPresent()) {
            var rotMat = rotMatMaybe.get();

            int r = 0;
            int c = 0;
            for (double el : rotMat) {
                this.rotation.setRowColumn(r, c, el);

                c++;
                if (c >= 3) {
                    c = 0;
                    r++;
                }

                if (r >= 3) {
                    break;
                }
            }
        }

        var lin = new Vector3d();
        var ang = new Vector3d();

        var motionMaybe = input.list("body_motion", Codec.DOUBLE);
        if (motionMaybe.isPresent()) {
            var motion = motionMaybe.get();

            int i = 0;
            for (var el : motion) {
                if (i >= 6) break;

                if (i >= 3) {
                    ang.setComponent(i - 3, el);
                } else {
                    lin.setComponent(i, el);
                }

                i++;
            }
        }

        if (canFindBody()) {
            var body = getPhysicsBody();
            try {
                body.mutex.acquire();
                body.transform.rotate(this.rotation.getNormalizedRotation(new Quaterniond()));
                body.linearMomentum.set(lin);
                body.angularMomentum.set(ang);
                body.mutex.release();
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (this.itemDrop != null) {
            output.store("prop_drop_item", ItemStack.CODEC, this.itemDrop);
        }

        var rotMat = output.list("body_orientation", Codec.DOUBLE);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                rotMat.add(this.rotation.get(c, r));
            }
        }

        var motion = output.list("body_motion", Codec.DOUBLE);

        if (canFindBody()) {
            var body = getPhysicsBody();
            try {
                body.mutex.acquire();

                for (int i = 0; i < 3; i++) motion.add(body.linearMomentum.get(i));
                for (int i = 0; i < 3; i++) motion.add(body.angularMomentum.get(i));

                body.mutex.release();
            } catch (InterruptedException ignored) {}
        }
    }
}
