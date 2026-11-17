package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.widowmaker.Widowmaker;
import dev.overcraft.widowmaker.WidowmakerStats;
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
 * Widowmaker's Venom Mine (thrown entity 00AF, stuck 00AE with graph 0057; widowmaker-map.md): thrown at 20 m/s falling at 9.8,
 * 0.2 m. It sticks to the first block it touches (W: surfaces, not creatures; it drops off one), arms 1 s later, and an enemy within
 * 3 m sets it off: poison on every enemy within 4 m in sight ({@link Widowmaker#mineBurst}). With Seeker Mine it stays and fires
 * darts instead (59BF; {@link Widowmaker#seekerTick}). 1 health: an enemy's damage breaks it. One at a time (her kit replaces it).
 * Kept with its chunk, as Junkrat's mine (W: it stays until it's set off or replaced).
 */
public class VenomMineEntity extends OwProjectile implements SurfaceStuck {
	private static final EntityDataAccessor<Boolean> STUCK = SynchedEntityData.defineId(VenomMineEntity.class, EntityDataSerializers.BOOLEAN);
	/** The face it's stuck to, for its clients' look (SurfaceStuck: -1 flying). */
	private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(VenomMineEntity.class, EntityDataSerializers.INT);
	/** Armed (its clients' look: the armed glow). */
	private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(VenomMineEntity.class, EntityDataSerializers.BOOLEAN);
	private double health = WidowmakerStats.MINE_HEALTH;
	/** The face it stuck to, outward (up until it sticks). */
	private Vec3 normal = new Vec3(0, 1, 0);
	/** Ticks since it stuck (-1 flying). */
	private int stuckTicks = -1;
	/** Seeker Mine: ticks to its next volley (0 ready). */
	public int seekerWait;
	private boolean done;
	/** Ticks it has spent stuck, all told: they don't count toward its flight life (it falls again if its block goes). */
	private int heldTicks;

	public VenomMineEntity(EntityType<? extends VenomMineEntity> type, Level level) {
		super(type, level);
	}

	public VenomMineEntity(LivingEntity owner, Level level) {
		super(OcEntities.VENOM_MINE, owner, level, new ItemStack(Items.SPIDER_EYE));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STUCK, false);
		builder.define(FACE, -1);
		builder.define(ARMED, false);
	}

	@Override
	public @org.jspecify.annotations.Nullable Direction stuckFace() {
		return stuck() ? SurfaceStuck.decode(entityData.get(FACE)) : null;
	}

	public boolean stuck() {
		return entityData.get(STUCK);
	}

	public boolean armed() {
		return entityData.get(ARMED);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SPIDER_EYE;
	}

	/** In flight 10 s at most (D volley life); stuck it stays (lifetime() isn't counted once it sticks). */
	@Override
	protected int lifetime() {
		return Ow.ticks(WidowmakerStats.MINE_FLIGHT_LIFE) + heldTicks;
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(WidowmakerStats.MINE_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) (WidowmakerStats.MINE_SIZE / 2);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !stuck() && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile)
			&& !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && !Allies.canHurt(owner, living));
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
		if (tickCount > 1 && !Widowmaker.stillHers(this, getOwner())) {
			discard();
			return;
		}
		if (stuck() && behindGone()) {
			// the block it was on went (mined, burnt): it falls and sticks again where it lands, still armed if it was (ours; it
			// hung in the air: critique 2026-11-17)
			entityData.set(STUCK, false);
			setNoGravity(false);
		}
		if (stuck()) {
			setDeltaMovement(Vec3.ZERO);
			stuckTicks++;
			heldTicks++;
			if (!armed() && stuckTicks >= Ow.ticks(WidowmakerStats.MINE_ARM)) {
				entityData.set(ARMED, true);
				if (getOwner() instanceof ServerPlayer owner) {
					Widowmaker.mineArmed(owner, this);
				}
			}
			if (armed() && !done && getOwner() instanceof ServerPlayer owner && owner.level() == level()) {
				Widowmaker.mineTick(owner, this);
			}
			return;
		}
		super.tick();
	}

	/** Whether the block it's stuck to is gone: the point just behind the face it's on has nothing solid. */
	private boolean behindGone() {
		net.minecraft.core.BlockPos behind = net.minecraft.core.BlockPos.containing(
			getBoundingBox().getCenter().subtract(normal.scale(0.15 + getBbHeight() / 2)));
		return level().getBlockState(behind).getCollisionShape(level(), behind).isEmpty();
	}

	/** Its middle, a little off the face it's on: where its burst and darts start (so a sight line doesn't begin in the block). */
	public Vec3 burstPoint() {
		return getBoundingBox().getCenter().add(normal.scale(0.15));
	}

	/** Set off (Widowmaker.mineBurst): gone 0 s later here (the data's 1.5 s is its burst effect, M). */
	public void spent() {
		done = true;
		discard();
	}

	/** A creature: it drops off it (W: it doesn't stick to players). */
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
		entityData.set(FACE, SurfaceStuck.encode(face));
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(at);
		if (!armed()) {
			stuckTicks = 0;
		}
		if (getOwner() instanceof ServerPlayer owner) {
			Widowmaker.mineStuck(owner, this);
		}
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Stuck", stuck());
		output.putBoolean("Armed", armed());
		output.putDouble("NX", normal.x);
		output.putDouble("NY", normal.y);
		output.putDouble("NZ", normal.z);
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(STUCK, input.getBooleanOr("Stuck", false));
		entityData.set(ARMED, input.getBooleanOr("Armed", false));
		normal = new Vec3(input.getDoubleOr("NX", 0), input.getDoubleOr("NY", 1), input.getDoubleOr("NZ", 0));
		entityData.set(FACE, SurfaceStuck.encode(Direction.getApproximateNearest(normal)));
		stuckTicks = stuck() ? Ow.ticks(WidowmakerStats.MINE_ARM) : -1;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** An enemy's damage breaks it at 1 (D #59; W): gone, no burst. Hers and her allies' do nothing. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (done || isRemoved()) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && getOwner() instanceof LivingEntity owner
			&& (attacker == owner || !Allies.canHurt(owner, attacker))) {
			return false;
		}
		health -= Ow.toOw(amount);
		if (health <= 0) {
			done = true;
			if (getOwner() instanceof ServerPlayer owner) {
				Widowmaker.mineBroken(owner, this);
			}
			discard();
		}
		return true;
	}
}
