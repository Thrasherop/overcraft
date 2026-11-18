package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.ashe.Ashe;
import dev.overcraft.ashe.AsheSounds;
import dev.overcraft.ashe.AsheStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ashe's Dynamite (entity 2533, graphs 0033 + 1502; ashe-map.md): thrown at 25 m/s falling at 8 m/s², 0.1 m. Set down on a floor it lights
 * and goes off 2 s later (D 1D41, M "from landing"); off a wall or a ceiling it bounces (ours, L), off a creature it drops. It goes off
 * at once when she shoots it (Ashe.shot), 0.5 s after E again with Remote Detonator, or at 30 s (D lifetime). Airburst: x1.4 reach while
 * it hasn't landed. Not saved (2 s on the ground).
 */
public class DynamiteEntity extends OwProjectile {
	private static final EntityDataAccessor<Boolean> LANDED = SynchedEntityData.defineId(DynamiteEntity.class, EntityDataSerializers.BOOLEAN);
	/** Ticks to the blast once lit or set off (-1 not yet). */
	private int fuse = -1;
	private boolean airburst;
	private boolean done;

	public DynamiteEntity(EntityType<? extends DynamiteEntity> type, Level level) {
		super(type, level);
	}

	public DynamiteEntity(LivingEntity owner, Level level, boolean airburst) {
		super(OcEntities.DYNAMITE, owner, level, new ItemStack(Items.TNT));
		this.airburst = airburst;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(LANDED, false);
	}

	public boolean landed() {
		return entityData.get(LANDED);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.TNT;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(AsheStats.DYNAMITE_LIFETIME);
	}

	@Override
	protected void expire() {
		explode();
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(AsheStats.DYNAMITE_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) AsheStats.DYNAMITE_RADIUS;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !landed() && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile) && !(entity instanceof BobEntity)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !dev.overcraft.hero.Allies.canHurt(owner, living));
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (landed()) {
				commonTick();
			} else {
				super.tick();
			}
			return;
		}
		if (fuse >= 0 && --fuse <= 0) {
			explode();
			return;
		}
		if (landed() && level().getBlockState(net.minecraft.core.BlockPos.containing(position().subtract(0, 0.05, 0)))
			.getCollisionShape(level(), net.minecraft.core.BlockPos.containing(position().subtract(0, 0.05, 0))).isEmpty()) {
			// the block under it went (mined, burnt): it falls again, still lit (ours; it floated in the air: critique 2026-11-17)
			entityData.set(LANDED, false);
			setNoGravity(false);
		}
		if (landed()) {
			setDeltaMovement(Vec3.ZERO);
			if (tickCount >= lifetime()) {
				explode();
			}
			return;
		}
		super.tick();
	}

	/** Remote Detonator: it goes off {@code ticks} from now (sooner if its fuse is already nearer). */
	public void remote(int ticks) {
		if (fuse < 0 || fuse > ticks) {
			fuse = Math.max(1, ticks);
		}
	}

	/** It goes off now (its fuse, her shot, Remote Detonator, its lifetime). */
	public void explode() {
		if (done || !(level() instanceof ServerLevel)) {
			return;
		}
		done = true;
		if (getOwner() instanceof ServerPlayer owner && owner.level() == level()) {
			Ashe.explode(owner, this, getBoundingBox().getCenter().add(0, 0.1, 0), airburst && !landed() ? AsheStats.AIRBURST_RADIUS : 1);
		}
		discard();
	}

	/** A creature: it drops off it (ours, as Junkrat's mine). */
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Vec3 v = getDeltaMovement();
		setDeltaMovement(new Vec3(-v.x * 0.05, Math.min(0, v.y) * 0.05, -v.z * 0.05));
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || landed() || done) {
			return;
		}
		Direction face = hit.getDirection();
		Vec3 v = getDeltaMovement();
		if (face != Direction.UP) {
			// off a wall or a ceiling: it bounces, slowed (ours, L)
			double b = AsheStats.DYNAMITE_BOUNCE;
			setDeltaMovement(face.getAxis() == Direction.Axis.X ? new Vec3(-v.x * b, v.y * b, v.z * b)
				: face.getAxis() == Direction.Axis.Z ? new Vec3(v.x * b, v.y * b, -v.z * b) : new Vec3(v.x * b, -v.y * b, v.z * b));
			setPos(hit.getLocation().add(face.getStepX() * 0.12, face.getStepY() * 0.12, face.getStepZ() * 0.12));
			needsSync = true;
			return;
		}
		entityData.set(LANDED, true);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(hit.getLocation().add(0, 0.01, 0));
		// lit: off 2 s from now unless something sets it off first (D 1502 #180)
		if (fuse < 0) {
			fuse = Ow.ticks(AsheStats.DYNAMITE_FUSE);
		}
		AsheSounds.at((ServerLevel) level(), "dynamite.land", position(), 30);
		AsheSounds.at((ServerLevel) level(), "dynamite.fuse", position(), 30);
	}

	/** Shootable by its owner's rifle (Ashe.shot); nothing else's damage reaches it (D: its health component has no value, L). */
	@Override
	public boolean isPickable() {
		return false;
	}
}
