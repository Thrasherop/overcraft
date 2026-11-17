package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.torbjorn.Torbjorn;
import dev.overcraft.torbjorn.TorbjornStats;
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
 * A ball of Molten Core's slag (16DA #382: projectile 2902): 40 m/s falling at 30, 25 to an enemy it hits; off a wall or a ceiling
 * it bounces (W), on the ground (or under the enemy it hit) it leaves a pool (16DD on pet 2903: Torbjorn.pool). No model in his data
 * (effects only): its look in flight is its effect 2509 (TorbjornFx.slagFlight, from its renderer); the item stands in without the pack.
 */
public class SlagEntity extends OwProjectile {
	private boolean done;

	public SlagEntity(EntityType<? extends SlagEntity> type, Level level) {
		super(type, level);
	}

	public SlagEntity(LivingEntity owner, Level level) {
		super(OcEntities.SLAG, owner, level, new ItemStack(Items.MAGMA_CREAM));
	}

	@Override
	public float hitRadius() {
		return (float) TorbjornStats.SLAG_RADIUS;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.MAGMA_CREAM;
	}

	@Override
	protected int lifetime() {
		return Ow.ticks(10);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(TorbjornStats.SLAG_GRAVITY);
	}

	@Override
	public double barrierDamage() {
		return TorbjornStats.SLAG_DAMAGE;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && entity != getOwner()
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (done) {
			return;
		}
		done = true;
		if (level() instanceof ServerLevel && getOwner() instanceof ServerPlayer owner) {
			Torbjorn.slagHit(owner, this, hit.getEntity());
		}
		discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || done) {
			return;
		}
		Direction face = hit.getDirection();
		if (face == Direction.UP) {
			done = true;
			if (getOwner() instanceof ServerPlayer owner) {
				Torbjorn.slagLanded(owner, this, hit.getLocation());
			}
			discard();
			return;
		}
		// a wall or a ceiling: bounced back off it, losing half its speed into the face (ours: W says only that it bounces)
		Vec3 v = getDeltaMovement();
		Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
		double into = v.dot(n);
		setDeltaMovement(v.subtract(n.scale(into * 1.5)).scale(0.8));
		setPos(hit.getLocation().add(n.scale(0.05)));
		needsSync = true;
	}
}
