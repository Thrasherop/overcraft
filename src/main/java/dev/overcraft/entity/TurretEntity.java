package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.torbjorn.Torbjorn;
import dev.overcraft.torbjorn.TorbjornSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static dev.overcraft.torbjorn.TorbjornStats.*;

/**
 * Torbjörn's turret (16C5's thrown 2931, then the deployed pet 28B7 with graphs 1688 / 1689 / 38EA; model 1000C). A creature, so
 * Minecraft's mobs can fight it: one it shoots turns on it (ours: Minecraft's mobs pick their targets by kind, and none picks a
 * turret), arrows and blows hurt it, it has 225 health. His side's by {@link OwnableEntity} (Allies: a pet of his), so it shoots only
 * enemies ({@link Allies#lockOn}: never a bystander or an ally) and enemies' area abilities take it. No AI of Minecraft's: it flies,
 * builds, aims and fires here. Nobody heals it but his hammer (Combat.heal). Saved with its chunk; one that isn't the turret he has
 * out goes ({@link Torbjorn#stillHis}).
 */
public class TurretEntity extends Mob implements OwnableEntity {
	/** Its states (synced: the renderer's clips). */
	public static final int FLYING = 0, BUILDING = 1, BUILT = 2, UPGRADING = 3, LEVEL3 = 4;
	private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STATE_AT = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SHOT_AT = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
	private static final net.minecraft.resources.Identifier L3_HEALTH_ID = dev.overcraft.Overcraft.id("turret_level3");

	private @Nullable UUID ownerId;
	private Vec3 flight = Vec3.ZERO;
	/** Anchor Bolts: it sticks where it touches (a wall, a ceiling) and doesn't fall. */
	private boolean anchored, stuck;
	private @Nullable UUID targetId;
	private double fireIn;
	private int missileIn, missilesLeft;
	/** Game times: its last shot, the last hit it took. */
	private long firedAt = -1000, hurtAt = -1000;
	/** Overloaded Turret: level 3 until Overload ends. */
	private boolean wantLevel3;

	public TurretEntity(EntityType<? extends TurretEntity> type, Level level) {
		super(type, level);
		setNoAi(true);
		setPersistenceRequired();
	}

	public TurretEntity(ServerPlayer owner, Level level, boolean anchorBolts) {
		this(OcEntities.TURRET, level);
		ownerId = owner.getUUID();
		anchored = anchorBolts;
		setHealth(getMaxHealth());
	}

	public static AttributeSupplier.Builder attributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Ow.hp(TURRET_HEALTH)).add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.MOVEMENT_SPEED, 0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATE, FLYING);
		builder.define(STATE_AT, 0);
		builder.define(SHOT_AT, -1000);
	}

	public int state() {
		return entityData.get(STATE);
	}

	/** Ticks of its own age when its state began, and when it last fired. */
	public int stateAt() {
		return entityData.get(STATE_AT);
	}

	public int shotAt() {
		return entityData.get(SHOT_AT);
	}

	private void setState(int state) {
		entityData.set(STATE, state);
		entityData.set(STATE_AT, tickCount);
	}

	/** Built: it fights (level 2 or 3). */
	public boolean built() {
		return state() == BUILT || state() == LEVEL3;
	}

	public @Nullable UUID ownerUuid() {
		return ownerId;
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return ownerId == null ? null : EntityReference.of(ownerId);
	}

	public void launch(Vec3 from, Vec3 velocity) {
		setPos(from.x, from.y - TURRET_HEIGHT / 2, from.z);
		flight = velocity;
		setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
		setState(FLYING);
	}

	/** Whether it's been in combat: it fired in the last 1 s or was hit in the last 3 s (D 16C5 #509). */
	public boolean inCombat() {
		long now = level().getGameTime();
		return now - firedAt <= Ow.ticks(COMBAT_FIRED) || now - hurtAt <= Ow.ticks(COMBAT_HIT);
	}

	/** His rivet hit {@code e}: it's the turret's target now (W). */
	public void focus(LivingEntity e) {
		targetId = e.getUUID();
	}

	/** Overloaded Turret: level 3 while {@code on} (D 1688 #364 / #362). */
	public void overloaded(boolean on) {
		wantLevel3 = on;
	}

	@Override
	public void tick() {
		super.tick();
		setYHeadRot(getYRot());
		yBodyRot = getYRot();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner != null && !Torbjorn.stillHis(this, owner)) {
			discard();
			return;
		}
		int state = state();
		if (state == FLYING) {
			fly(level, owner);
			return;
		}
		if (!stuck && !onGround()) {
			// the ground under it went (ours: it falls again, as it does from a broken platform: W)
			flight = flight.add(0, -Ow.accel(TURRET_GRAVITY), 0);
			move(MoverType.SELF, flight);
			if (onGround()) {
				flight = Vec3.ZERO;
			}
		}
		int in = tickCount - stateAt();
		if (state == BUILDING) {
			if (in % 10 == 0) {
				TorbjornSounds.all(level, "turret.build_step", position(), 20);
			}
			if (in >= Ow.ticks(TURRET_BUILD)) {
				setState(BUILT);
				TorbjornSounds.all(level, "turret.deploy", position(), 30);
				if (owner != null) {
					dev.overcraft.hero.Voice.say(owner, "turret_build", 1);
				}
			}
			return;
		}
		if (state == BUILT && wantLevel3) {
			setState(UPGRADING);
			level3(true);
			TorbjornSounds.all(level, "turret.upgrade", position(), 40);
			if (owner != null) {
				dev.overcraft.hero.Voice.say(owner, "turret_upgrade", 1);
			}
			return;
		}
		if (state == UPGRADING) {
			if (in >= Ow.ticks(L3_UPGRADE)) {
				setState(wantLevel3 ? LEVEL3 : BUILT);
				missileIn = 0;
			}
			if (!wantLevel3) {
				level3(false);
				setState(BUILT);
			}
			return;
		}
		if (state == LEVEL3 && !wantLevel3) {
			level3(false);
			setState(BUILT);
			TorbjornSounds.all(level, "turret.downgrade", position(), 30);
		}
		if (owner != null) {
			fight(level, owner);
		}
	}

	private void fly(ServerLevel level, @Nullable ServerPlayer owner) {
		flight = flight.add(0, -Ow.accel(TURRET_GRAVITY), 0);
		move(MoverType.SELF, flight);
		if (anchored && (horizontalCollision || verticalCollision && flight.y > 0)) {
			// Anchor Bolts: it holds where it touched a wall or a ceiling (D 16C5 #465's attach fields under 2502)
			stuck = true;
			setNoGravity(true);
			land(level);
			return;
		}
		if (horizontalCollision) {
			// off a wall it drops (W: it lands on the ground); ours: its sideways speed gone
			flight = new Vec3(0, flight.y, 0);
		}
		if (onGround() || tickCount > Ow.ticks(5)) {
			land(level);
		}
	}

	private void land(ServerLevel level) {
		flight = Vec3.ZERO;
		setState(BUILDING);
		TorbjornSounds.all(level, "turret.land", position(), 30);
		TorbjornSounds.all(level, "turret.build", position(), 30);
	}

	private void level3(boolean on) {
		var max = getAttribute(Attributes.MAX_HEALTH);
		if (max == null) {
			return;
		}
		if (on) {
			max.addOrUpdateTransientModifier(new AttributeModifier(L3_HEALTH_ID, Ow.hp(L3_HEALTH), AttributeModifier.Operation.ADD_VALUE));
			heal(Ow.hp(L3_HEALTH));
		} else {
			max.removeModifier(L3_HEALTH_ID);
			setHealth(Math.min(getHealth(), getMaxHealth()));
		}
	}

	/** Its gun, where its shots start. */
	public Vec3 gun() {
		return position().add(0, TURRET_GUN_UP, 0);
	}

	private boolean canShoot(ServerLevel level, ServerPlayer owner, LivingEntity e) {
		Vec3 gun = gun();
		return e.isAlive() && e != this && Allies.lockOn(owner, e) && e.getBoundingBox().getCenter().distanceTo(gun) <= TURRET_RANGE
			&& Combat.inSight(level, gun, e, this);
	}

	/** Its target: the one it has while it can still shoot it, else the closest enemy in sight in 40 m (W). */
	private @Nullable LivingEntity target(ServerLevel level, ServerPlayer owner) {
		if (targetId != null && level.getEntity(targetId) instanceof LivingEntity e && canShoot(level, owner, e)) {
			return e;
		}
		targetId = null;
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		Vec3 gun = gun();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(gun, gun).inflate(TURRET_RANGE))) {
			double d = e.getBoundingBox().getCenter().distanceToSqr(gun);
			if (d < bestD && canShoot(level, owner, e)) {
				best = e;
				bestD = d;
			}
		}
		if (best != null) {
			targetId = best.getUUID();
		}
		return best;
	}

	private void fight(ServerLevel level, ServerPlayer owner) {
		fireIn = Math.max(0, fireIn - 1);
		LivingEntity target = tickCount % 2 == 0 || targetId != null ? target(level, owner) : null;
		if (target == null) {
			missilesLeft = 0;
			return;
		}
		Vec3 gun = gun();
		Vec3 to = target.getBoundingBox().getCenter().subtract(gun);
		float want = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
		float diff = net.minecraft.util.Mth.wrapDegrees(want - getYRot());
		float turn = (float) Math.max(-TURRET_TURN, Math.min(TURRET_TURN, diff));
		setYRot(getYRot() + turn);
		float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
		setXRot(pitch);
		if (Math.abs(diff - turn) > TURRET_FIRE_CONE) {
			return;
		}
		if (fireIn <= 0) {
			fireIn += TURRET_INTERVAL * 20;
			shoot(level, owner, target, TURRET_DAMAGE, false);
		}
		if (state() == LEVEL3) {
			// three missiles every 1.5 s beside its gun (W), one each 0.15 s (ours)
			if (--missileIn <= 0) {
				missileIn = Ow.ticks(MISSILE_EVERY);
				missilesLeft = MISSILES;
			}
			if (missilesLeft > 0 && missileIn % 3 == 0) {
				missilesLeft--;
				shoot(level, owner, target, MISSILE_DAMAGE, true);
			}
		}
	}

	/** One hitscan shot at {@code target}'s middle: an enemy barrier between takes it (Barriers.between). */
	private void shoot(ServerLevel level, ServerPlayer owner, LivingEntity target, double damage, boolean missile) {
		Vec3 gun = gun();
		Vec3 at = target.getBoundingBox().getCenter();
		firedAt = level.getGameTime();
		entityData.set(SHOT_AT, tickCount);
		Barriers.Shape barrier = Barriers.between(level, gun, at, owner);
		if (barrier != null) {
			barrier.barrierTake(damage, at);
		} else if (level.clip(new ClipContext(gun, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS
			|| Combat.inSight(level, gun, target, this)) {
			Torbjorn.turretHit(owner, this, target, damage, missile);
		}
		// its tracer (ours: the data's 2A69 shot is an effect)
		Vec3 step = at.subtract(gun).normalize().scale(0.6);
		int n = (int) Math.min(30, at.distanceTo(gun) / 0.6);
		for (int i = 1; i < n; i++) {
			Vec3 p = gun.add(step.scale(i));
			level.sendParticles(missile ? ParticleTypes.SMOKE : ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		TorbjornSounds.all(level, "turret.fire", gun, 50);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		// his own side's blows don't break it (Allies: it's his); a fall or a cactus doesn't either (ours)
		Entity by = source.getEntity();
		if (by instanceof LivingEntity attacker && ownerId != null && level.getServer().getPlayerList().getPlayer(ownerId) instanceof ServerPlayer owner
			&& (attacker == owner || Allies.isAlly(owner, attacker)) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return false;
		}
		if (by == null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
			&& !source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) {
			hurtAt = level.getGameTime();
			TorbjornSounds.all(level, "turret.damaged", position(), 30);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			Torbjorn.turretGone(this, ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId), true);
		}
	}

	@Override
	protected void tickDeath() {
		if (!level().isClientSide() && !isRemoved()) {
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 0;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (ownerId != null) {
			EntityReference.store(EntityReference.of(ownerId), output, "TurretOwner");
		}
		output.putInt("TurretState", state() == FLYING ? BUILDING : state() == UPGRADING || state() == LEVEL3 ? BUILT : state());
		output.putBoolean("TurretStuck", stuck);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		EntityReference<LivingEntity> owner = EntityReference.read(input, "TurretOwner");
		ownerId = owner == null ? null : owner.getUUID();
		setState(input.getIntOr("TurretState", BUILT));
		stuck = input.getBooleanOr("TurretStuck", false);
		setNoGravity(stuck);
		var max = getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.removeModifier(L3_HEALTH_ID);
		}
		setNoAi(true);
	}

	/** Seconds into its present state, for the renderer. */
	public float stateTime(float partial) {
		return (tickCount - stateAt() + partial) / 20f;
	}
}
