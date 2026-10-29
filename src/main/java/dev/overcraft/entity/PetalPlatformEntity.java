package dev.overcraft.entity;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.Perk;
import dev.overcraft.lifeweaver.LifeweaverStats;
import dev.overcraft.registry.OcEntities;
import dev.overcraft.registry.OcSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.lifeweaver.LifeweaverStats.*;

/**
 * Lifeweaver's Petal Platform deployed (his model 12D32, entity 4504, graph 368D): a lift solid as a block. Anyone
 * standing on it for 0.15 s (any team: W) sets it off: it rises 9 m in 0.85 s (or to 2.75 m under a ceiling; with less
 * than 3.35 m of room it doesn't rise), carrying its riders, whose jump is 6 m/s stronger during the rise and 0.2 s
 * after. Raised, its 12 s count down (never refilled); no one on it for 2 s, it comes back down (d / 10 s, easing in)
 * with at least 2 s left. 300 health. Its position is a function of its synced phase on both sides, so a rider's own
 * client moves them with it smoothly (Shulker's way: the platform pushes what stands on it).
 */
public class PetalPlatformEntity extends Entity {
	public static final int ARMED = 0, RISING = 1, RAISED = 2, DESCENDING = 3;

	private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> PHASE_AT = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> GROUND_Y = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> FROM_Y = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> TO_Y = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.INT);
	/** Raised time left (ticks), for the HUD and its effects. */
	private static final EntityDataAccessor<Integer> LEFT = SynchedEntityData.defineId(PetalPlatformEntity.class, EntityDataSerializers.INT);

	private @Nullable UUID ownerId;
	private double health = PLATFORM_HEALTH;
	private int raisedLeft = Ow.ticks(PLATFORM_RAISED_TIME);
	/** Ticks to the next last-seconds warning tick. */
	private int warnIn;
	private int empty;
	private final Map<Integer, Integer> standing = new HashMap<>();
	/**
	 * Riders of a platform that wasn't down: their landing doesn't hurt (the owner's rule for ability landings, #35).
	 * Kept until they've been in the air off it and landed (the tick they step off its edge they're still "on the
	 * ground": dropping them then let a 9 m fall take 75 health, found playing).
	 */
	private record Landing(LivingEntity entity, boolean airborne, int grounded) {
	}

	/** On the ground this long, off any platform (the platform re-adds its riders each tick), the guard goes (ours). */
	private static final int SETTLED = Ow.ticks(0.5);

	private static final Map<UUID, Landing> LANDING = new HashMap<>();

	public PetalPlatformEntity(EntityType<? extends PetalPlatformEntity> type, Level level) {
		super(type, level);
	}

	public PetalPlatformEntity(ServerLevel level, ServerPlayer owner, Vec3 at) {
		this(OcEntities.PETAL_PLATFORM, level);
		setPos(at);
		ownerId = owner.getUUID();
		entityData.set(OWNER, owner.getId());
		entityData.set(GROUND_Y, (float) at.y);
		entityData.set(FROM_Y, (float) at.y);
		entityData.set(TO_Y, (float) at.y);
		entityData.set(PHASE_AT, level.getGameTime());
		Effects.play(level, "lifeweaver_platform_deploy", at, owner);
		// 421A: his side's copy and the enemies' (A089, L which is whose)
		HeroSounds.at(level, OcSounds.LW_PLATFORM_DEPLOY, OcSounds.LW_PLATFORM_DEPLOY_ENEMY, owner, at, 70);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PHASE, ARMED);
		builder.define(PHASE_AT, 0L);
		builder.define(GROUND_Y, 0f);
		builder.define(FROM_Y, 0f);
		builder.define(TO_Y, 0f);
		builder.define(OWNER, -1);
		builder.define(LEFT, Ow.ticks(PLATFORM_RAISED_TIME));
	}

	public int phase() {
		return entityData.get(PHASE);
	}

	/**
	 * Ticks into its phase. Never below 0: a client's clock runs a tick or two behind the server's, and a phase "not
	 * started yet" put a rising platform under the ground for a tick, taking its rider down with it (found playing).
	 */
	public long phaseTicks() {
		return Math.max(0, level().getGameTime() - entityData.get(PHASE_AT));
	}

	public int ownerId() {
		return entityData.get(OWNER);
	}

	/** Raised time left (s). */
	public float raisedLeft() {
		return entityData.get(LEFT) / 20f;
	}

	/** Its moves' time: d / 10 s (D 368D #191), the rise 0.05 s shorter. */
	private int moveTicks() {
		double d = Math.abs(entityData.get(TO_Y) - entityData.get(FROM_Y));
		double s = d / PLATFORM_LIFT_SPEED;
		return Math.max(1, Ow.ticks(phase() == RISING ? Math.max(s - 0.05, 0.05) : s));
	}

	/** Its height now, from its phase: the rise linear, the descent easing in (D's curve: ~0.38 slope at its start). */
	private double yNow() {
		float from = entityData.get(FROM_Y);
		float to = entityData.get(TO_Y);
		return switch (phase()) {
			case RISING -> from + (to - from) * Math.min(1, phaseTicks() / (double) moveTicks());
			case DESCENDING -> {
				double u = Math.min(1, phaseTicks() / (double) moveTicks());
				yield from + (to - from) * (0.38 * u + 0.62 * u * u);
			}
			case RAISED -> to;
			default -> entityData.get(GROUND_Y);
		};
	}

	/** Moving: rising or coming down. */
	public boolean moving() {
		int p = phase();
		return (p == RISING || p == DESCENDING) && phaseTicks() < moveTicks();
	}

	@Override
	protected InterpolationHandler createInterpolationHandler() {
		// its height is worked out on both sides from its phase: the server's position packets would only make it stutter
		return new InterpolationHandler.NoOpInterpolationHandler() {
			@Override
			public boolean interpolateTo(@Nullable PositionPath position, float yRot, float xRot, boolean hasRotation) {
				return true;
			}
		};
	}

	/**
	 * Solid, except to players on the server: a player's own client moves them and stands them on its copy of the
	 * platform. The server's copy runs a tick or two ahead of a client's while it moves, and colliding there it rose into
	 * the player and refused their client's lifted position, putting them back (found playing).
	 */
	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return !(other instanceof ServerPlayer);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		double before = getY();
		double y = yNow();
		if (Math.abs(y - before) > 1.0E-6) {
			double dy = y - before;
			if (dy > 0) {
				carry(dy);
			}
			setPos(getX(), y, getZ());
			if (dy < 0) {
				carry(dy);
			}
		}
		unstick();
		if (level() instanceof ServerLevel level) {
			serverTick(level);
		}
	}

	/**
	 * Whoever is inside its thickness (it landed under them, or rose into them) is put on its top, as Overwatch's
	 * collision would: "deployed under a hero" counts as standing on it (D 368D). Each side moves what it moves (carry).
	 */
	private void unstick() {
		AABB box = getBoundingBox();
		for (Entity e : level().getEntities(this, box, EntitySelector.NO_SPECTATORS.and(e -> !e.noPhysics && !(e instanceof PetalPlatformEntity)))) {
			if (level().isClientSide() != e instanceof net.minecraft.world.entity.player.Player || e instanceof ServerPlayer) {
				continue;
			}
			double up = box.maxY - e.getY();
			if (up > 0 && up <= box.getYsize() + 0.05) {
				e.move(MoverType.SHULKER, new Vec3(0, up + 1.0E-3, 0));
				e.setOnGround(true);
				e.resetFallDistance();
			}
		}
	}

	/**
	 * Riders: whoever stands on its top (the data's "ground entity is the platform"). On the server a player's feet may
	 * be up to a tick or two of its rise below it (their client's copy is behind the server's: canBeCollidedWith).
	 */
	public List<Entity> riders() {
		AABB box = getBoundingBox();
		double behind = level().isClientSide() ? 0.1 : PLAYER_LAG;
		AABB top = new AABB(box.minX, box.maxY - behind, box.minZ, box.maxX, box.maxY + 0.3, box.maxZ);
		return level().getEntities(this, top, EntitySelector.NO_SPECTATORS.and(e -> !e.noPhysics && !(e instanceof PetalPlatformEntity)
			&& e.getBoundingBox().minY >= box.maxY - (e instanceof ServerPlayer ? PLAYER_LAG : 0.1)));
	}

	/** How far below its top the server may see a rider player (m): a few ticks of its 10.6 m/s rise. */
	private static final double PLAYER_LAG = 1.6;

	/**
	 * Moves its riders with it, as Shulker pushes what's on its lid: the server moves everyone (players too: left where
	 * they were, the platform rose into them there and the server sent their client's lifted position back, found
	 * playing), each client its own players, with its own copy of the platform.
	 */
	private void carry(double dy) {
		for (Entity rider : riders()) {
			if (level().isClientSide() != rider instanceof net.minecraft.world.entity.player.Player || rider instanceof ServerPlayer) {
				continue;
			}
			rider.move(MoverType.SHULKER, new Vec3(0, dy, 0));
			// still standing on it (the push left them "in the air", where nothing slowed their walk: a rider slid off its
			// edge on the way up, found playing)
			rider.setOnGround(true);
			rider.resetFallDistance();
		}
	}

	/** Someone's feet on it, within the trigger's reach of its middle (2 m shrinking to 0.75 m: D 368D). */
	private boolean onIt(Entity e, double radius) {
		Vec3 feet = e.position();
		double dx = feet.x - getX();
		double dz = feet.z - getZ();
		return Math.abs(feet.y - getBoundingBox().maxY) < 0.15 && dx * dx + dz * dz <= radius * radius;
	}

	private void serverTick(ServerLevel level) {
		ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || !owner.isAlive() && owner.isRemoved()) {
			// its owner gone (left the world): so is it (D 368D #241)
			destroy();
			return;
		}
		List<Entity> riders = riders();
		for (Entity rider : riders) {
			if (rider instanceof LivingEntity living && phase() != ARMED) {
				LANDING.put(living.getUUID(), new Landing(living, false, 0));
			}
			if (rider instanceof ServerPlayer player) {
				// standing on an entity, high up: not floating (Minecraft would kick them after 4 s on a server)
				((dev.overcraft.mixin.ServerGamePacketListenerAccessor) player.connection).overcraft$setAboveGroundTickCount(0);
			}
		}
		petalProtection(level, owner);
		long t = phaseTicks();
		switch (phase()) {
			case ARMED -> {
				// the trigger: 2 m and 0.15 s as it lands, the reach shrinking to 0.75 m by 0.475 s, then 0.05 s
				double age = t / 20.0;
				double radius = age < PLATFORM_SHRINK_FROM ? PLATFORM_TRIGGER_RADIUS : age >= PLATFORM_TRIGGER_LATE_FROM
					? PLATFORM_TRIGGER_RADIUS_LATE : PLATFORM_TRIGGER_RADIUS - (PLATFORM_TRIGGER_RADIUS - PLATFORM_TRIGGER_RADIUS_LATE)
					* (age - PLATFORM_SHRINK_FROM) / (PLATFORM_TRIGGER_LATE_FROM - PLATFORM_SHRINK_FROM);
				int hold = Ow.ticks(age >= PLATFORM_TRIGGER_LATE_FROM ? PLATFORM_TRIGGER_LATE : PLATFORM_TRIGGER);
				Map<Integer, Integer> now = new HashMap<>();
				boolean go = false;
				for (Entity rider : riders) {
					if (rider instanceof LivingEntity && onIt(rider, radius)) {
						int held = standing.getOrDefault(rider.getId(), 0) + 1;
						now.put(rider.getId(), held);
						go |= held >= Math.max(1, hold);
					}
				}
				standing.clear();
				standing.putAll(now);
				if (go) {
					rise(level, owner);
				}
			}
			case RISING -> {
				if (t >= moveTicks()) {
					setPhase(RAISED, entityData.get(TO_Y), entityData.get(TO_Y));
					Effects.play(level, "lifeweaver_platform_top", position(), owner);
					// the rise's end (96D3's Stop) and the raised petals' start (4989 x5)
					HeroSounds.at(level, OcSounds.LW_PLATFORM_RISE_END, position(), 40);
					HeroSounds.at(level, OcSounds.LW_PLATFORM_RAISED, position(), 40);
				}
			}
			case RAISED -> {
				raisedLeft--;
				entityData.set(LEFT, Math.max(0, raisedLeft));
				// the last 5 s: a tick every 0.5 s, quickening to 0.02 s at the end (48F1: 96D2 by the time left, H)
				if (raisedLeft > 0 && raisedLeft <= Ow.ticks(PLATFORM_WARNING_FROM) && --warnIn <= 0) {
					HeroSounds.at(level, OcSounds.LW_PLATFORM_WARNING, position(), 20);
					double left = raisedLeft / 20.0;
					warnIn = Math.max(1, (int) Math.round((0.5 - 0.481 * (1 - left / PLATFORM_WARNING_FROM)) * 20));
				}
				if (raisedLeft <= 0) {
					destroy();
					return;
				}
				empty = riders.stream().anyMatch(r -> r instanceof LivingEntity) ? 0 : empty + 1;
				if (empty >= Ow.ticks(PLATFORM_EMPTY)) {
					// no one on it for 2 s: back down, with at least 2 s left for next time
					raisedLeft = Math.max(raisedLeft, Ow.ticks(PLATFORM_EMPTY));
					entityData.set(LEFT, raisedLeft);
					setPhase(DESCENDING, (float) getY(), entityData.get(GROUND_Y));
					HeroSounds.at(level, OcSounds.LW_PLATFORM_DESCEND, position(), 40);
					empty = 0;
				}
			}
			case DESCENDING -> {
				if (t >= moveTicks()) {
					setPhase(ARMED, entityData.get(GROUND_Y), entityData.get(GROUND_Y));
					HeroSounds.at(level, OcSounds.LW_PLATFORM_LANDED, position(), 40);
					standing.clear();
				}
			}
			default -> {
			}
		}
	}

	/** Each server tick (HeroManager): the landing guard, whether or not the platform is still there. */
	public static void tickLanding() {
		for (Map.Entry<UUID, Landing> entry : new java.util.ArrayList<>(LANDING.entrySet())) {
			Landing l = entry.getValue();
			LivingEntity e = l.entity;
			int grounded = e.onGround() ? l.grounded + 1 : 0;
			if (!e.isAlive() || e.isRemoved() || l.airborne && e.onGround() || grounded > SETTLED) {
				LANDING.remove(entry.getKey());
				continue;
			}
			e.resetFallDistance();
			LANDING.put(entry.getKey(), new Landing(e, l.airborne || !e.onGround(), grounded));
		}
	}

	/**
	 * Sets off: its top is 9 m up, or 2.75 m under a ceiling found by a ray from 0.5 m over 11.75 m; with the ceiling
	 * 3.35 m or less above, it stays down (D 368D #176-#179).
	 */
	private void rise(ServerLevel level, ServerPlayer owner) {
		Vec3 from = position().add(0, 0.5, 0);
		double reach = PLATFORM_HEIGHT + PLATFORM_HEADROOM;
		double r = getBbWidth() / 2 - 0.1;
		double room = reach;
		// four rays at its edges and one at its middle: a Minecraft ceiling is blocks, not a single point
		for (Vec3 o : new Vec3[] {Vec3.ZERO, new Vec3(r, 0, 0), new Vec3(-r, 0, 0), new Vec3(0, 0, r), new Vec3(0, 0, -r)}) {
			var hit = level.clip(new ClipContext(from.add(o), from.add(o).add(0, reach, 0), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, this));
			if (hit.getType() == HitResult.Type.BLOCK) {
				room = Math.min(room, hit.getLocation().y - from.y);
			}
		}
		double top;
		if (room >= reach) {
			top = getY() + PLATFORM_HEIGHT;
		} else if (room > PLATFORM_LOW_CEILING) {
			top = Math.min(getY() + PLATFORM_HEIGHT, from.y + room - PLATFORM_HEADROOM);
		} else {
			return;
		}
		setPhase(RISING, (float) getY(), (float) top);
		standing.clear();
		Effects.play(level, "lifeweaver_platform_rise", position(), owner);
		HeroSounds.at(level, OcSounds.LW_PLATFORM_RISE, position(), 40);
		if (riders().stream().anyMatch(e -> e != owner && e instanceof ServerPlayer)) {
			dev.overcraft.hero.Voice.say(owner, "platform_used", 1);
		}
	}

	private void setPhase(int phase, float from, float to) {
		entityData.set(PHASE, phase);
		entityData.set(PHASE_AT, level().getGameTime());
		entityData.set(FROM_Y, from);
		entityData.set(TO_Y, to);
	}

	/** Petal Protection (minor perk): 20 a second to allies within 3 m, raised or not (D 28B6; him too). */
	private void petalProtection(ServerLevel level, ServerPlayer owner) {
		if (!HeroManager.hasPerk(owner, Perk.LIFEWEAVER_PETAL_PROTECTION)) {
			return;
		}
		double r = getBbWidth() / 2 + PETAL_PROTECTION_RANGE;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, PETAL_PROTECTION_RANGE, r))) {
			if (Allies.isSelfOrAlly(owner, e) && e.distanceToSqr(getX(), getBoundingBox().maxY, getZ()) <= Math.pow(PETAL_PROTECTION_RANGE + 1, 2)) {
				float healed = Combat.heal(e, PETAL_PROTECTION / 20);
				if (healed > 0 && e != owner) {
					Combat.credit(owner, healed);
				}
			}
		}
	}

	/**
	 * The jump bonus a rider has now (m/s): 6 during the rise and 0.2 s after it (D 267A). For whoever stands on one:
	 * LivingEntityMixin adds it to their jump.
	 */
	public static double jumpBonus(LivingEntity jumper) {
		AABB below = jumper.getBoundingBox().move(0, -0.2, 0);
		for (PetalPlatformEntity p : jumper.level().getEntitiesOfClass(PetalPlatformEntity.class, below)) {
			int phase = p.phase();
			if (phase == RISING || phase == RAISED && p.phaseTicks() <= Ow.ticks(PLATFORM_JUMP_AFTER)) {
				return PLATFORM_JUMP_BONUS;
			}
		}
		return 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || isInvulnerableToBase(source)) {
			return false;
		}
		health -= Ow.toOw(amount);
		Effects.play(level, "lifeweaver_platform_hit", position(), null);
		if (health <= 0) {
			destroy();
		}
		return true;
	}

	/** It's gone (felled, out of time, Shift again, a new throw, its owner gone). */
	public void destroy() {
		if (!isRemoved() && level() instanceof ServerLevel level) {
			Effects.play(level, "lifeweaver_platform_destroyed", position().add(0, 0.2, 0), null);
			HeroSounds.at(level, OcSounds.LW_PLATFORM_DESTROYED, position(), 70);
		}
		discard();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
