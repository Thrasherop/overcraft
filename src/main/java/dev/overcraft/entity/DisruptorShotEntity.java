package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sojourn.Disruptor;
import dev.overcraft.sojourn.SojournStats;
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
 * Sojourn's Disruptor Shot in flight (her graph 1441 #353: projectile 31EF, graph 5342): 40 m/s, 0.2 m, no drop; on its first
 * enemy, object or block, or after 0.75 s (30 m), it opens its field there ({@link Disruptor#open}). Allies don't stop it (W:
 * "on hitting an enemy hero, an object, terrain").
 */
public class DisruptorShotEntity extends OwProjectile {
	public DisruptorShotEntity(EntityType<? extends DisruptorShotEntity> type, Level level) {
		super(type, level);
	}

	public DisruptorShotEntity(LivingEntity owner, Level level) {
		super(OcEntities.DISRUPTOR_SHOT, owner, level, new ItemStack(Items.ENDER_PEARL));
	}

	@Override
	public float hitRadius() {
		return (float) SojournStats.DISRUPTOR_SIZE;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.ENDER_PEARL;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(SojournStats.DISRUPTOR_TRAVEL);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	/** Its 0.75 s are up: the field where it is. */
	@Override
	protected void expire() {
		open(position());
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (level() instanceof ServerLevel && isAlive() && getOwner() instanceof ServerPlayer owner) {
			Disruptor.struck(owner, hit.getLocation(), false);
		}
		open(hit.getLocation());
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel && isAlive() && getOwner() instanceof ServerPlayer owner) {
			Disruptor.struck(owner, hit.getLocation(), true);
		}
		// at the face it hit (sojourn-plan.md, ours), its centre a little out of the block
		net.minecraft.core.Direction face = hit.getDirection();
		open(hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05));
	}

	/** An enemy barrier: its field opens there, at the barrier's front (W: "on hitting ... an object"). */
	@Override
	public void hitBarrier(Vec3 at) {
		open(at);
	}

	private void open(Vec3 at) {
		if (!(level() instanceof ServerLevel) || !isAlive()) {
			return;
		}
		if (getOwner() instanceof ServerPlayer owner) {
			Disruptor.open(owner, at);
		}
		discard();
	}
}
