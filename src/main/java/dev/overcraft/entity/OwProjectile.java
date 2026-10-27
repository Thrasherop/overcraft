package dev.overcraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

	/**
	 * A block hit does what an arrow's does (#34): Minecraft's own reactions (targets, bells, chorus flowers and pots, the
	 * last two because both are in the impact_projectiles tag), and a wooden button pressed, which an arrow does by
	 * resting in it. A button has no collision box, so the hit lands on the block it's fixed to: the button is the
	 * block in front of the face hit, pressed when the hit point is on it.
	 */
	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ButtonBlock button && state.is(BlockTags.WOODEN_BUTTONS) && !state.getValue(ButtonBlock.POWERED)
			&& state.getShape(level, pos).bounds().move(pos).inflate(getBbWidth() / 2 + 0.05).contains(hit.getLocation())
			&& mayInteract(level, pos)) {
			button.press(state, level, pos, getOwner() instanceof Player player ? player : null);
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
