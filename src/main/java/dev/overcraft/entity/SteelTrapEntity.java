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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Junkrat's Steel Trap (in flight entity 11A9, then the trap 103A with graph 09BB; junkrat-map.md): thrown at 15 m/s falling at
 * 20 m/s², 0.3 m. It sets down on the first ground it touches (W: it sticks to walkable surfaces) and bounces off walls and
 * creatures (D restitution 0.15). 0.75 s after it lands it's armed; the first enemy within 1 m springs it: 100 and a 2.5 s root
 * ({@link Junkrat#trapSprung}). It lets go at the root's end and goes 8 s later. 100 health: an enemy can break it (D 0355; W).
 */
public class SteelTrapEntity extends OwProjectile {
	/** 0 in flight, 1 set down, 2 sprung (synced: its look). */
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(SteelTrapEntity.class, EntityDataSerializers.INT);
	private int landed = -1;
	private int sprung = -1;
	private @Nullable LivingEntity caught;
	private double health = JunkratStats.TRAP_HEALTH;

	public SteelTrapEntity(EntityType<? extends SteelTrapEntity> type, Level level) {
		super(type, level);
	}

	public SteelTrapEntity(LivingEntity owner, Level level) {
		super(OcEntities.STEEL_TRAP, owner, level, new ItemStack(Items.IRON_BARS));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STAGE, 0);
	}

	public int stage() {
		return entityData.get(STAGE);
	}

	public boolean armed() {
		return stage() == 1 && landed >= Ow.ticks(JunkratStats.TRAP_ARM);
	}

	public @Nullable LivingEntity caught() {
		return caught;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.IRON_BARS;
	}

	/** Until sprung or replaced once down (D: 5 s in flight, L that it's gone then; ours: an hour down). */
	@Override
	protected int lifetime() {
		return stage() == 0 ? Ow.ticks(JunkratStats.TRAP_FLIGHT) : Integer.MAX_VALUE;
	}

	@Override
	protected double getDefaultGravity() {
		return Ow.accel(JunkratStats.TRAP_GRAVITY);
	}

	@Override
	public float hitRadius() {
		return (float) JunkratStats.TRAP_RADIUS;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return stage() == 0 && super.canHitEntity(entity) && entity != getOwner() && !(entity instanceof OwProjectile)
			&& !(entity instanceof RipTireEntity);
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			if (stage() != 0) {
				commonTick();
			} else {
				super.tick();
			}
			return;
		}
		if (stage() == 0) {
			super.tick();
			return;
		}
		setDeltaMovement(Vec3.ZERO);
		if (!(getOwner() instanceof ServerPlayer owner) || owner.level() != level()) {
			// (no owner here: it springs on no one)
			return;
		}
		if (stage() == 1) {
			landed++;
			if (armed()) {
				LivingEntity victim = Junkrat.trapVictim(owner, this, new AABB(position(), position()).inflate(JunkratStats.TRAP_TRIGGER + 2));
				if (victim != null) {
					entityData.set(STAGE, 2);
					sprung = 0;
					caught = victim;
					Junkrat.trapSprung(owner, this, victim);
				}
			}
			return;
		}
		sprung++;
		if (caught != null && (!caught.isAlive() || sprung >= Ow.ticks(Junkrat.rootSeconds(caught)))) {
			Junkrat.trapReleased(owner, this, caught);
			caught = null;
		}
		if (sprung >= Ow.ticks(JunkratStats.TRAP_ROOT + JunkratStats.TRAP_LINGER)) {
			discard();
		}
	}

	/** A creature in flight: it bounces off (D restitution 0.15). */
	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Vec3 v = getDeltaMovement();
		setDeltaMovement(new Vec3(-v.x * 0.15, v.y * 0.15, -v.z * 0.15));
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!(level() instanceof ServerLevel) || stage() != 0) {
			return;
		}
		Direction face = hit.getDirection();
		if (face != Direction.UP) {
			// a wall or a ceiling: off it (D 0.15), falling on
			setDeltaMovement(Junkrat.bounce(getDeltaMovement(), new Vec3(face.getStepX(), face.getStepY(), face.getStepZ())).scale(0.3));
			setPos(hit.getLocation().add(face.getStepX() * 0.32, face.getStepY() * 0.32, face.getStepZ() * 0.32));
			needsSync = true;
			return;
		}
		entityData.set(STAGE, 1);
		landed = 0;
		setNoGravity(true);
		setDeltaMovement(Vec3.ZERO);
		setPos(hit.getLocation().add(0, 0.01, 0));
		if (getOwner() instanceof ServerPlayer owner) {
			Junkrat.trapLanded(owner, this);
		}
	}

	@Override
	public void hitBarrier(Vec3 at) {
		setDeltaMovement(Vec3.ZERO);
	}

	@Override
	protected void expire() {
		discard();
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	/** An enemy's damage breaks it at 100 (D 0355; W); a trap that broke lets go. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved()) {
			return false;
		}
		if (source.getEntity() instanceof LivingEntity attacker && getOwner() instanceof LivingEntity owner
			&& (attacker == owner || !Allies.canHurt(owner, attacker))) {
			return false;
		}
		health -= Ow.toOw(amount);
		if (health <= 0) {
			if (getOwner() instanceof ServerPlayer owner) {
				if (caught != null) {
					Junkrat.trapReleased(owner, this, caught);
					caught = null;
				}
				Junkrat.trapBroken(owner, this);
			}
			discard();
		}
		return true;
	}

	@Override
	public void remove(RemovalReason reason) {
		// replaced, broken, switched away: what it holds goes free
		if (caught != null && getOwner() instanceof ServerPlayer owner && !level().isClientSide()) {
			Junkrat.trapReleased(owner, this, caught);
			caught = null;
		}
		super.remove(reason);
	}
}
