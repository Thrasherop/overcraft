package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.junkrat.Junkrat;
import dev.overcraft.junkrat.JunkratStats;
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
 * Junkrat's Concussion Mine (entity 0F9B, graph 096B; junkrat-map.md): thrown at 25 m/s falling at 20 m/s², 0.2 m. It sticks to the
 * first block it touches (D restitution 0.05, M) and drops off a creature it meets (W: it bounces off players). It waits until he
 * sets it off (the right button, {@link Junkrat}) or throws another (one at a time); then it goes off 0.1 s later
 * ({@link Junkrat#mineBlast}). 25 health: an enemy can break it, and then it doesn't go off (D 19A8; W).
 */
public class ConcussionMineEntity extends OwProjectile {
	private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(ConcussionMineEntity.class,
		EntityDataSerializers.BOOLEAN);
	/** Ticks to its blast once set off (-1 waiting). */
	private int fuse = -1;
	private double health = JunkratStats.MINE_HEALTH;
	/** The face it stuck to, outward (up until it sticks). */
	private Vec3 normal = new Vec3(0, 1, 0);

	public ConcussionMineEntity(EntityType<? extends ConcussionMineEntity> type, Level level) {
		super(type, level);
	}

	public ConcussionMineEntity(LivingEntity owner, Level level) {
		super(OcEntities.CONCUSSION_MINE, owner, level, new ItemStack(Items.HEAVY_CORE));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STUCK, false);
	}

	public boolean stuck() {
		return entityData.get(STUCK);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.HEAVY_CORE;
	}

	/** Until set off or replaced (D lifetime 999 s). */
	@Override
	protected int lifetime() {
		return Ow.ticks(999);
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(JunkratStats.MINE_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) JunkratStats.MINE_RADIUS;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !stuck() && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile)
			&& !(entity instanceof RipTireEntity) && !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living
			&& !Allies.canHurt(owner, living));
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (stuck()) {
				commonTick();
			} else {
				super.tick();
			}
			return;
		}
		if (fuse < 0 && tickCount > 1 && !Junkrat.stillHis(this, getOwner(), true)) {
			discard();
			return;
		}
		if (fuse >= 0) {
			if (--fuse <= 0) {
				if (getOwner() instanceof ServerPlayer owner && owner.level() == level()) {
					Junkrat.mineBlast(owner, this, blastPoint());
				}
				discard();
			}
			if (stuck()) {
				return;
			}
		}
		if (stuck()) {
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		super.tick();
	}

	/**
	 * Where its blast starts: its middle, 0.15 m off the face it's on and up (found playing, J2a: from its own place, on or in the
	 * floor's face, every sight line started in the block and the husk 1.9 m off took nothing).
	 */
	public Vec3 blastPoint() {
		return getBoundingBox().getCenter().add(normal.scale(0.15)).add(0, 0.15, 0);
	}

	/** Set off: it goes off 0.1 s from now (D 096B #400). */
	public void detonate() {
		if (fuse < 0) {
			fuse = Math.max(1, Ow.ticks(JunkratStats.MINE_DELAY));
		}
	}

	public boolean detonating() {
		return fuse >= 0;
	}

	/** A creature: it drops off it (W: bounces off players; ours: nearly all its speed gone, as the data's 0.05). */
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Vec3 v = getDeltaMovement();
		setDeltaMovement(new Vec3(-v.x * 0.05, Math.min(0, v.y) * 0.05, -v.z * 0.05));
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || stuck()) {
			return;
		}
		Direction face = hit.getDirection();
		normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
		Vec3 at = hit.getLocation().add(face.getStepX() * 0.1, face.getStepY() * 0.1 - (face == Direction.UP ? 0 : getBbHeight() / 2),
			face.getStepZ() * 0.1);
		entityData.set(STUCK, true);
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(at);
		if (getOwner() instanceof ServerPlayer owner) {
			Junkrat.mineStuck(owner, this);
		}
	}

	/** It met an enemy barrier once already: from then on it passes them. */
	private boolean bounced;

	@Override
	public void hitBarrier(Vec3 at) {
		// an enemy barrier: it drops off it there, its speed gone as off a creature (the data's 0.05), and then falls through
		// it: "The mine bounces once upon hitting a barrier for the first time, but can then pass through it" (the Junkrat
		// page rev 410907); the barrier pages: it "will not stick to barriers". Until #104's sweep each tick's crossing stopped
		// it again, so it could hang in a barrier's plane
		bounced = true;
		setPos(at);
		setDeltaMovement(Vec3.ZERO);
		needsSync = true;
	}

	@Override
	public boolean passesBarriers() {
		return bounced;
	}

	/** Stuck, its face and health kept with its chunk. */
	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Stuck", stuck());
		output.putDouble("Health", health);
		output.putDouble("NX", normal.x);
		output.putDouble("NY", normal.y);
		output.putDouble("NZ", normal.z);
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(STUCK, input.getBooleanOr("Stuck", false));
		health = input.getDoubleOr("Health", JunkratStats.MINE_HEALTH);
		normal = new Vec3(input.getDoubleOr("NX", 0), input.getDoubleOr("NY", 1), input.getDoubleOr("NZ", 0));
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** An enemy's damage breaks it at 25: gone, no blast (D #269 -> #281; W). His own and his allies' do nothing. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (fuse >= 0 || isRemoved()) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && getOwner() instanceof LivingEntity owner
			&& (attacker == owner || !Allies.canHurt(owner, attacker))) {
			return false;
		}
		health -= Ow.toOw(amount);
		if (health <= 0) {
			if (getOwner() instanceof ServerPlayer owner) {
				Junkrat.mineBroken(owner, this);
			}
			discard();
		}
		return true;
	}
}
