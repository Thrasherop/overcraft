package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.dva.DvaStats;
import dev.overcraft.dva.MicroMissiles;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
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
 * One of D.Va's Micro Missiles in flight (1128 #357: projectile 1E8A): 50 m/s, 0.175 m, no drop, gone at 100 m. On its
 * first enemy, block or barrier it bursts ({@link MicroMissiles#burst}). Allies and she don't stop it.
 */
public class DvaMissileEntity extends OwProjectile {
	public DvaMissileEntity(EntityType<? extends DvaMissileEntity> type, Level level) {
		super(type, level);
	}

	public DvaMissileEntity(LivingEntity owner, Level level) {
		super(OcEntities.DVA_MISSILE, owner, level, new ItemStack(Items.FIREWORK_ROCKET));
	}

	@Override
	public float hitRadius() {
		return (float) DvaStats.MISSILES_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.FIREWORK_ROCKET;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(DvaStats.MISSILES_RANGE / DvaStats.MISSILES_SPEED);
	}

	@Override
	public double barrierDamage() {
		return DvaStats.MISSILES_IMPACT + DvaStats.MISSILES_SPLASH;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		// it bursts where it touches: the point of the creature's box nearest its centre (its centre is 0.175 m out, and the
		// burst measured from there lost the full 5.5 on a direct hit: found playing, 131 of 135)
		Vec3 at = hit.getLocation();
		net.minecraft.world.phys.AABB box = hit.getEntity().getBoundingBox();
		burst(new Vec3(Math.clamp(at.x, box.minX, box.maxX), Math.clamp(at.y, box.minY, box.maxY), Math.clamp(at.z, box.minZ, box.maxZ)),
			hit.getEntity());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		// at the face it hit, a little out of the block (so the burst's sight lines start in the air)
		Direction face = hit.getDirection();
		burst(hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05), null);
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
			MicroMissiles.burst(owner, this, at, direct);
		}
		discard();
	}
}
