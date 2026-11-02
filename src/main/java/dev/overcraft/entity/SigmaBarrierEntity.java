package dev.overcraft.entity;

import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.sigma.SigmaBarrier;
import dev.overcraft.sigma.SigmaStats;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sigma's Experimental Barrier out in the world (his pet 2D25 running graph 1A2A; model C015 with its four emitter orbs
 * FC88): a 6.70 x 4.18 m upright barrier (its mesh at rest) facing the way he faced, moving along his aim at 20 m/s while
 * sent and still once stopped (dev.overcraft.sigma.SigmaBarrier drives it). It's a {@link Barriers.Shape}: it stops his
 * enemies' projectiles and blasts from its front, takes their damage from his 650 (HeroState.barrierHealth), and lets
 * creatures and players walk through it. Its position is its foot's middle; its centre 2.09 m up. Not saved: it's his while
 * he's here.
 */
public class SigmaBarrierEntity extends Entity implements Barriers.Shape {
	public static final int SENT = 2, STOPPED = 3;

	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SigmaBarrierEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(SigmaBarrierEntity.class, EntityDataSerializers.INT);
	/** Its health, a share of its max (0-1), for its look. */
	private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(SigmaBarrierEntity.class, EntityDataSerializers.FLOAT);
	/** The tick it went out, for its look's clips. */
	private static final EntityDataAccessor<Long> OUT_AT = SynchedEntityData.defineId(SigmaBarrierEntity.class, EntityDataSerializers.LONG);

	public SigmaBarrierEntity(EntityType<? extends SigmaBarrierEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	public SigmaBarrierEntity(ServerLevel level, ServerPlayer owner, Vec3 centre, float yaw) {
		this(OcEntities.SIGMA_BARRIER, level);
		setPos(centre.subtract(0, SigmaStats.BARRIER_HEIGHT / 2, 0));
		setYRot(yaw);
		yRotO = yaw;
		entityData.set(OWNER, owner.getId());
		entityData.set(STATE, SENT);
		entityData.set(OUT_AT, level.getGameTime());
		Barriers.add(this);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OWNER, -1);
		builder.define(STATE, SENT);
		builder.define(HEALTH, 1f);
		builder.define(OUT_AT, 0L);
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	public int state() {
		return entityData.get(STATE);
	}

	public void setState(int state) {
		entityData.set(STATE, state);
	}

	public float health() {
		return entityData.get(HEALTH);
	}

	/** Seconds since it went out (its look). */
	public float outFor(float partialTick) {
		return (level().getGameTime() - entityData.get(OUT_AT) + partialTick) / 20f;
	}

	@Override
	public void onRemoval(Entity.RemovalReason reason) {
		super.onRemoval(reason);
		Barriers.remove(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel)) {
			return;
		}
		ServerPlayer owner = barrierOwner() instanceof ServerPlayer p ? p : null;
		HeroState s = owner == null ? null : HeroManager.state(owner);
		if (owner == null || s == null || s.hero != Hero.SIGMA || s.sigmaBarrierId == null || s.sigmaBarrierId != getId()) {
			// its Sigma is gone (left, another hero): so is it
			discard();
			return;
		}
		if (!owner.isAlive()) {
			// he died: it goes home with him (his kit doesn't tick while he's dead; found playing: it stood until he respawned)
			SigmaBarrier.clear(owner, s);
			s.barrierDownSince = level().getGameTime();
			return;
		}
		if (state() == SENT) {
			SigmaBarrier.move(owner, this);
		}
		entityData.set(HEALTH, (float) (s.barrierHealth / SigmaStats.BARRIER_HEALTH));
		Barriers.intercept(this);
	}

	// ---- Barriers.Shape ----

	@Override
	public @Nullable LivingEntity barrierOwner() {
		return level().getEntity(ownerId()) instanceof LivingEntity owner ? owner : null;
	}

	@Override
	public Level barrierLevel() {
		return level();
	}

	@Override
	public Vec3 barrierCentre() {
		return position().add(0, SigmaStats.BARRIER_HEIGHT / 2, 0);
	}

	@Override
	public Vec3 barrierFacing() {
		float yaw = getYRot() * (float) (Math.PI / 180);
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	@Override
	public double barrierWidth() {
		return SigmaStats.BARRIER_WIDTH;
	}

	@Override
	public double barrierHeight() {
		return SigmaStats.BARRIER_HEIGHT;
	}

	@Override
	public double barrierTake(double ow, Vec3 at) {
		return barrierOwner() instanceof ServerPlayer owner && isAlive() ? SigmaBarrier.take(owner, this, ow, at) : 0;
	}

	@Override
	public boolean barrierAlive() {
		return isAlive() && !isRemoved();
	}

	// ---- an entity no one bumps into ----

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
