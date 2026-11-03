package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sigma.Hyperspheres;
import dev.overcraft.sigma.SigmaStats;
import net.minecraft.core.Direction;
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
 * One of Sigma's Hyperspheres (his weapon graph 187F, projectiles 2D35 / 32B4): 50 m/s, 0.32 m, a slight drop (5.5 m/s²,
 * M), bouncing off blocks (W; the data's contact sets 6CB / 6CC: M), bursting on the first enemy it touches or after its
 * 21.7 m (0.434 s): {@link Hyperspheres#burst}. Never breaks a block. Allies and its owner don't stop it.
 */
public class HypersphereEntity extends OwProjectile {
	/** Its exact lifetime in ticks (8.68: it bursts that far through its ninth step). */
	private static final double LIFE = SigmaStats.SPHERE_LIFETIME * 20;
	/** The volley's second sphere (32B4: its own burst and wall sounds, 5E09 / 591B), the server's to know. */
	public boolean second;

	public HypersphereEntity(EntityType<? extends HypersphereEntity> type, Level level) {
		super(type, level);
	}

	public HypersphereEntity(LivingEntity owner, Level level) {
		super(OcEntities.HYPERSPHERE, owner, level, new ItemStack(Items.ENDER_EYE));
	}

	@Override
	public float hitRadius() {
		return (float) SigmaStats.SPHERE_SIZE / 2;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ENDER_EYE;
	}

	@Override
	protected int lifetime() {
		return (int) Math.ceil(LIFE);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(SigmaStats.SPHERE_GRAVITY);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	/** Its 21.7 m are up: the burst where it was then (part of its last step back). */
	@Override
	protected void expire() {
		double back = lifetime() - LIFE;
		burst(position().subtract(getDeltaMovement().scale(back)), null);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		burst(hit.getLocation(), hit.getEntity());
	}

	/** A block: it bounces off the face it hit (W), none of its speed lost (ours, M), and goes on. */
	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Direction face = hit.getDirection();
		Vec3 v = getDeltaMovement();
		Vec3 bounced = switch (face.getAxis()) {
			case X -> new Vec3(-v.x, v.y, v.z);
			case Y -> new Vec3(v.x, -v.y, v.z);
			case Z -> new Vec3(v.x, v.y, -v.z);
		};
		setDeltaMovement(bounced);
		if (getOwner() instanceof ServerPlayer owner && level() instanceof ServerLevel) {
			dev.overcraft.sigma.Hyperspheres.bounced(owner, second, hit.getLocation());
		}
		// out of the face by its block radius
		setPos(hit.getLocation().add(face.getStepX() * (SigmaStats.SPHERE_BLOCK_RADIUS + 0.01), face.getStepY() * (SigmaStats.SPHERE_BLOCK_RADIUS + 0.01),
			face.getStepZ() * (SigmaStats.SPHERE_BLOCK_RADIUS + 0.01)));
		needsSync = true;
	}

	/** An enemy barrier: it takes the direct 20, and the burst goes off at its front (Hyperspheres.burst gives it the rest). */
	@Override
	public double barrierDamage() {
		return SigmaStats.SPHERE_DIRECT;
	}

	@Override
	public void hitBarrier(Vec3 at) {
		burst(at, null);
	}

	private void burst(Vec3 at, Entity direct) {
		if (!(level() instanceof ServerLevel) || !isAlive()) {
			return;
		}
		if (getOwner() instanceof ServerPlayer owner) {
			Hyperspheres.burst(owner, this, at, direct);
		}
		discard();
	}
}
