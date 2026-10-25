package dev.overcraft.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** An Overwatch projectile: constant speed (no air drag), its own gravity, and a lifetime. */
public abstract class OwProjectile extends ThrowableItemProjectile {
	protected OwProjectile(EntityType<? extends OwProjectile> type, Level level) {
		super(type, level);
	}

	protected OwProjectile(EntityType<? extends OwProjectile> type, LivingEntity owner, Level level, ItemStack stack) {
		super(type, owner, level, stack);
	}

	/** Lifetime in ticks. */
	protected abstract int lifetime();

	protected void expire() {
		discard();
	}

	/** Launches from {@code from} along {@code direction} at {@code speed} blocks per tick. */
	public void launch(Vec3 from, Vec3 direction, double speed) {
		setPos(from);
		setDeltaMovement(direction.normalize().scale(speed));
		updateRotationNow();
	}

	private void updateRotationNow() {
		Vec3 v = getDeltaMovement();
		double h = v.horizontalDistance();
		setYRot((float) (Math.atan2(v.x, v.z) * 180 / Math.PI));
		setXRot((float) (Math.atan2(v.y, h) * 180 / Math.PI));
		yRotO = getYRot();
		xRotO = getXRot();
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && isAlive() && tickCount >= lifetime()) {
			expire();
		}
	}

	@Override
	protected float getAirDrag() {
		return 1.0f;
	}

	@Override
	protected double getDefaultGravity() {
		return 0;
	}
}
