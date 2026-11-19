package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.ashe.Ashe;
import dev.overcraft.ashe.AsheSounds;
import dev.overcraft.ashe.AsheStats;
import dev.overcraft.hero.Allies;
import dev.overcraft.registry.OcEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Ashe's B.O.B. (pet 25C3, graphs 1537 / 153B / 35B3; ashe-map.md): a creature of his own, so Minecraft's mobs can fight him. He comes
 * at her feet facing her way, drops in 0.5 s, charges 4 s at 15 m/s until a wall stops him or he uppercuts an enemy (120, knocked up,
 * held: Ashe.bobUppercut), stands and fires his cannons for 10 s at the enemies he sees (Ashe.bobTarget / bobShot), then goes in 0.5 s.
 * <ul>
 * <li><b>Health:</b> 1150 (1000 and the Tank role's 150, W), Minecraft's 92; her side's damage doesn't reach him (no friendly fire).</li>
 * <li><b>Who he is to others:</b> her pet (OwnableEntity): her allies' ally, her enemies' enemy (Allies). <b>Mobs</b> take him on when he
 * hits them (ours: the mob's last attacker is him, so Minecraft's own retaliation goals go for him), and hostile ones that see him
 * with no one nearer go for him unprovoked (drawMobs, ours).</li>
 * <li><b>Cleanup:</b> he stays through her G and her death (W: he fights on), and is gone on a hero switch, her leaving, or when she
 * can't be found (Ashe.leavePlay / clearWorld). Not saved: a world reloaded has no B.O.B.</li>
 * </ul>
 */
public class BobEntity extends PathfinderMob implements OwnableEntity {
	/** His phases; STOP a charge a wall or being stuck ended (its clip 05AE), TIMED_OUT one that ran its 4 s (0492: D 1537 #472). */
	public static final int DROP = 0, CHARGE = 1, UPPERCUT = 2, STOP = 3, FIRE = 4, DESPAWN = 5, TIMED_OUT = 6;
	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(BobEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID owner;
	/** Ticks into the phase (server); on the client, the tick the phase was seen to start. */
	private int phaseTicks;
	private int clientPhaseAt;
	private double shots;
	private Vec3 lastPos = Vec3.ZERO;
	private int stuckTicks;
	private boolean uppercutDone;

	public BobEntity(EntityType<? extends BobEntity> type, Level level) {
		super(type, level);
	}

	public BobEntity(Level level, LivingEntity owner) {
		this(OcEntities.BOB, level);
		this.owner = owner.getUUID();
	}

	/** His attributes: 1150 health (Minecraft's 92), no pushing him about, not moved by his own AI (the kit moves him). */
	public static AttributeSupplier.Builder attributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Ow.hp(AsheStats.BOB_HEALTH)).add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PHASE, DROP);
	}

	public int phase() {
		return entityData.get(PHASE);
	}

	/** Seconds into his phase, on either side (the renderer's clip time). */
	public float phaseSeconds(float partial) {
		return ((level().isClientSide() ? tickCount - clientPhaseAt : phaseTicks) + partial) / 20f;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (PHASE.equals(key)) {
			clientPhaseAt = tickCount;
		}
	}

	private void phase(int phase) {
		entityData.set(PHASE, phase);
		phaseTicks = 0;
	}

	public @Nullable UUID ownerId() {
		return owner;
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return owner == null ? null : EntityReference.of(owner);
	}

	private @Nullable ServerPlayer ownerPlayer() {
		return owner != null && level() instanceof ServerLevel level && level.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer p
			&& p.level() == level() ? p : null;
	}

	/** His hits: a hitscan type, from him, by her (her kill, her charge; Combat.damage). */
	public DamageSource damageSource(ServerPlayer owner) {
		return new DamageSource(level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
				dev.overcraft.Overcraft.id("hitscan"))), this, owner);
	}

	/** A mob he hit takes him on (ours: Minecraft's retaliation goals go for its last attacker). */
	public void provoke(LivingEntity hit) {
		if (hit instanceof Mob mob && !(hit instanceof OwnableEntity o && o.getOwner() != null) && hit.isAlive()) {
			mob.setLastHurtByMob(this);
			mob.setTarget(this);
		}
	}

	/** How far around him (blocks) mobs are looked at for drawMobs: the furthest follow range of Minecraft's hostiles (a zombie's 35). */
	private static final double DRAW_REACH = 35;

	/**
	 * Minecraft's hostile mobs go for players (their target goals look for players and villagers), so on his own he'd stand ignored. Once a
	 * second (staggered by id), each hostile mob of her enemies within its own follow range that sees him and has no target nearer than
	 * him takes him on, as if he were a player in its sight (ours: Overwatch has no mobs; Torbjorn's turret only provokes on a hit). Neutral
	 * mobs (endermen, piglins) stay neutral; another's pet is left alone; when he's going he draws no one.
	 */
	private void drawMobs(ServerPlayer owner) {
		if ((tickCount + getId()) % 20 != 0 || phase() == DESPAWN) {
			return;
		}
		for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(DRAW_REACH), m -> m instanceof Enemy
			&& !(m instanceof NeutralMob) && !(m instanceof OwnableEntity o && o.getOwner() != null) && m.isAlive() && !m.isNoAi())) {
			double d = mob.distanceTo(this);
			LivingEntity had = mob.getTarget();
			if (had == this || d > mob.getAttributeValue(Attributes.FOLLOW_RANGE) || !Allies.isEnemy(owner, mob)
				|| had != null && had.isAlive() && mob.distanceTo(had) <= d || !mob.getSensing().hasLineOfSight(this)) {
				continue;
			}
			mob.setTarget(this);
		}
	}

	/** His kills this time out (D 14FF v41446): her farewell line says thanks for them or tells him to step it up. */
	private int kills;

	/** A kill of his (Ashe.killed). */
	public void killed() {
		kills++;
	}

	/** B.O.B. destroyed (D 12A4, 14FF #187 message 1E6F): "They got Bob!" from her. */
	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide() && ownerPlayer() instanceof ServerPlayer owner) {
			dev.overcraft.hero.Voice.say(owner, "bob_destroyed", 1);
		}
	}

	/** Called away early (a second B.O.B.): straight to his going. */
	public void leave() {
		if (phase() != DESPAWN) {
			phase(DESPAWN);
		}
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			serverTick();
		}
		super.tick();
	}

	private void serverTick() {
		ServerPlayer owner = ownerPlayer();
		if (owner == null) {
			// she has left (or is in another world): he goes (ours)
			discard();
			return;
		}
		phaseTicks++;
		drawMobs(owner);
		Vec3 ahead = Vec3.directionFromRotation(0, getYRot());
		switch (phase()) {
			case DROP -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				if (phaseTicks >= Ow.ticks(AsheStats.BOB_DROP)) {
					AsheSounds.at((ServerLevel) level(), "bob.land", position(), 40);
					AsheSounds.at((ServerLevel) level(), "bob.charge", position(), 60);
					lastPos = position();
					phase(CHARGE);
				}
			}
			case CHARGE -> {
				double v = Ow.speed(AsheStats.BOB_CHARGE_SPEED);
				setDeltaMovement(ahead.x * v, getDeltaMovement().y, ahead.z * v);
				if (!uppercutDone && Ashe.uppercutStarts(owner, this)) {
					uppercutDone = true;
					Ashe.bobUppercut(owner, this);
					AsheSounds.at((ServerLevel) level(), "bob.uppercut_3p", position(), 60);
					phase(UPPERCUT);
					return;
				}
				// stuck: moved under 0.05 m in a tick, or a wall 2 m ahead at his feet or 0.75 m up (D #640 / #641 / #649)
				boolean stuck = phaseTicks > 2 && position().distanceTo(lastPos) < 0.05;
				lastPos = position();
				if (stuck || wallAhead(ahead)) {
					AsheSounds.at((ServerLevel) level(), "bob.charge_stop", position(), 60);
					phase(STOP);
				} else if (phaseTicks >= Ow.ticks(AsheStats.BOB_CHARGE_TIME)) {
					phase(TIMED_OUT);
				}
			}
			case UPPERCUT -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				if (phaseTicks >= Ow.ticks(AsheStats.BOB_AFTER_UPPERCUT)) {
					phase(FIRE);
				}
			}
			case STOP, TIMED_OUT -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				if (phaseTicks >= Ow.ticks(0.5)) {
					phase(FIRE);
				}
			}
			case FIRE -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				LivingEntity target = Ashe.bobTarget(owner, this);
				if (target != null) {
					Vec3 to = target.position().subtract(position());
					float yaw = (float) (Math.atan2(-to.x, to.z) * 180 / Math.PI);
					setYRot(yaw);
					setYBodyRot(yaw);
					setYHeadRot(yaw);
					shots += AsheStats.BOB_RATE / 20.0;
					while (shots >= 1) {
						shots--;
						Ashe.bobShot(owner, this, target);
					}
					if (tickCount % 4 == 0) {
						AsheSounds.at((ServerLevel) level(), "bob.fire", position().add(0, AsheStats.BOB_GUN_UP, 0), 60);
					}
				} else {
					shots = 0;
				}
				if (phaseTicks >= Ow.ticks(AsheStats.BOB_FIRE_TIME)) {
					AsheSounds.at((ServerLevel) level(), "bob.fire_stop", position(), 40);
					phase(DESPAWN);
					// his time up: her line, by whether he got kills (D 0A93, 14FF #151 message 1C22, v41446 >= 1 -> A1E8, else A1E9)
					dev.overcraft.hero.Voice.say(owner, kills > 0 ? "bob_end_kills" : "bob_end_no_kills", 1);
				}
			}
			default -> {
				setDeltaMovement(0, getDeltaMovement().y, 0);
				if (phaseTicks >= Ow.ticks(AsheStats.BOB_DESPAWN)) {
					discard();
				}
			}
		}
	}

	private boolean wallAhead(Vec3 ahead) {
		for (double up : new double[] {0.1, AsheStats.BOB_WALL_RAY_UP}) {
			Vec3 from = position().add(0, up, 0);
			// from his front: the ray's 2 m from his middle reach 1.3 m past his 0.7 m (ours: the data's ray origin not read)
			Vec3 to = from.add(ahead.scale(getBbWidth() / 2 + 0.3));
			if (level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	/** Her side's damage doesn't reach him (no friendly fire); anyone else's does. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		LivingEntity o = getOwner();
		if (source.getEntity() instanceof LivingEntity attacker && o != null && (attacker == o || attacker == this
			|| Allies.isSelfOrAlly(o, attacker))) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return AsheSounds.of("bob.death");
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}
}
