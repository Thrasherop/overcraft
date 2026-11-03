package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sigma.Accretion;
import dev.overcraft.sigma.SigmaStats;
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
 * Sigma's Accretion rock (his projectile 18B7, running graph 1EB9; model C5CA): 37 m/s with 18 m/s² of gravity, 0.9 m
 * from its centre against creatures, 0.3 m against blocks, 30 s at most. On the first enemy or block it touches: {@link Accretion#impact}.
 * Allies and its owner don't stop it; no block breaks.
 */
public class AccretionEntity extends OwProjectile {
	public AccretionEntity(EntityType<? extends AccretionEntity> type, Level level) {
		super(type, level);
	}

	public AccretionEntity(LivingEntity owner, Level level) {
		super(OcEntities.ACCRETION, owner, level, new ItemStack(Items.COBBLESTONE));
	}

	@Override
	public float hitRadius() {
		return (float) SigmaStats.ROCK_SIZE;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBBLESTONE;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(SigmaStats.ROCK_LIFETIME);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(SigmaStats.ROCK_GRAVITY);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		impact(hit.getLocation(), hit.getEntity(), Vec3.ZERO);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		// the burst a little out of the face it hit (from the face itself, its sight checks started inside the block)
		net.minecraft.core.Direction face = hit.getDirection();
		impact(hit.getLocation().add(face.getStepX() * 0.15, face.getStepY() * 0.15, face.getStepZ() * 0.15), null,
			new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()));
	}

	/** An enemy barrier: it takes the 80, and the burst goes off at its front (Accretion.impact gives it the burst). */
	@Override
	public double barrierDamage() {
		return SigmaStats.ROCK_IMPACT;
	}

	@Override
	public void hitBarrier(Vec3 at) {
		impact(at, null, Vec3.ZERO);
	}

	private void impact(Vec3 at, Entity direct, Vec3 face) {
		if (!(level() instanceof ServerLevel) || !isAlive()) {
			return;
		}
		if (getOwner() instanceof ServerPlayer owner) {
			Accretion.impact(owner, this, at, direct, face);
		}
		discard();
	}
}
