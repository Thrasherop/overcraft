package dev.overcraft.tracer;

import dev.overcraft.Ow;
import dev.overcraft.entity.PulseBombEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroSounds;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Shields;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.overcraft.tracer.TracerStats.*;

/**
 * Tracer's kit (#71; numbers in {@link TracerStats}, the data map tools/reference/tracer-map.md). Her buttons by her data's ids:
 * the left button the Pulse Pistols, Shift and the right button Blink (ABILITY 1 and SECONDARY, 0024 #27 / #147), E Recall, Q
 * Pulse Bomb. The server owns her charges, cooldowns, shots and Recall; her own client moves her on a Blink (ClientTracer, which
 * finds the place with {@link Blink#destination}) the moment she presses, as Overwatch's does (WS: she's there within the
 * press's tick), and sends the press for the server to take the charge; Recall's hold and its jump back are the server's.
 */
public final class Tracer implements HeroKit {
	/** The synced mode's bits: Recall going (her client holds her still), hacked. */
	public static final int MODE_RECALL = 1, MODE_SILENCED = 2;

	static final Blow PISTOL_BLOW = Blow.weapon(false).by(Hero.TRACER);
	static final Blow BOMB_BLOW = Blow.ult("pulse_bomb").by(Hero.TRACER);

	/** One tick of her history: where she stood, faced and moved (m/s), and her health then (OW). */
	public record Sample(long at, Vec3 pos, float yaw, float pitch, Vec3 velocity, double health) {
	}

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.tracerBlinks = BLINK_CHARGES;
		s.tracerBlinkTimer = 0;
		s.tracerBlinkRun = 0;
		s.tracerRecall = -1;
		s.tracerBombCast = -1;
		s.tracerBloom = 0;
		s.tracerFiring = false;
		s.tracerHold = null;
		s.tracerHistory.clear();
		s.tracerSpawnAt = player.level().getGameTime();
		s.tracerOverAmmoAt = -1000;
		s.tracerVoiceAt = -1;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		s.tracerSilenced = dev.overcraft.sombra.Hack.silenced(player);
		double rate = Combat.cooldownRate(player);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		blinkRecharge(player, s, rate);
		recall(player, s, now);
		bombCast(player, s, able);
		pistols(player, s, able, now);
		overAmmo(s, now);
		if (s.tracerRecall < 0) {
			record(player, s, now);
		}
		if (s.tracerVoiceAt >= 0 && now >= s.tracerVoiceAt) {
			// her Recall line 0.25 s after the rewind (001A #84 -> criteria 05A7)
			s.tracerVoiceAt = -1;
			Voice.say(player, "recall", 1);
		}
	}

	// ---- Blink (0024) ----

	/** A charge back every 3 s, one at a time, while any is missing (D #54 / #143 / #56). */
	private static void blinkRecharge(ServerPlayer player, HeroState s, double rate) {
		if (s.tracerBlinks >= BLINK_CHARGES) {
			s.tracerBlinkTimer = 0;
			return;
		}
		s.tracerBlinkTimer += rate;
		if (s.tracerBlinkTimer >= Ow.ticks(BLINK_RECHARGE)) {
			s.tracerBlinkTimer = 0;
			s.tracerBlinks++;
			// each charge's own chime (0024 #450 -> #449: 0FE1 / 0FDF / 0FE0)
			TracerSounds.own(player, "blink_recharge_" + s.tracerBlinks);
		}
	}

	/** Whether a Blink would go now: what her client asks before it moves her, and what the press checks again. */
	public static boolean canBlink(HeroState s, boolean asleep, boolean silenced, boolean passenger, long now) {
		return s.tracerBlinks > 0 && now >= s.tracerBlinkReady && s.tracerRecall < 0 && !asleep && !silenced && !passenger;
	}

	private static void blink(ServerPlayer player, HeroState s, long now) {
		if (now < s.tracerBlinkReady) {
			// the 0.1 s after a Blink takes no press (D #153)
			return;
		}
		if (!canBlink(s, Sleep.asleep(player), s.tracerSilenced, player.isPassenger(), now)) {
			// a press that can't go: its own sound (0024 #57 false -> #200 -> #77 effect 007F)
			TracerSounds.own(player, "blink_empty");
			return;
		}
		s.tracerBlinks--;
		s.tracerBlinkReady = now + Ow.ticks(BLINK_LOCKOUT);
		// the count in a row (2 s, D #100 / #101-#104) picks the take: 1, 2, 3 and on
		s.tracerBlinkRun = now - s.tracerBlinkAt <= Ow.ticks(2) ? s.tracerBlinkRun + 1 : 1;
		s.tracerBlinkAt = now;
		int take = Math.min(3, s.tracerBlinkRun);
		TracerSounds.split(player, "blink_" + take, player.position(), 70);
		if (take == 3) {
			// her Blink line on the third in a row (criteria 05A6, effect 007B)
			Voice.say(player, "blink", 1);
		}
		// her fall starts again where she lands (WS: the Blink zeroes her vertical speed); no reload cut, no fire cut (D: Blink
		// takes no action-stack slot in the base kit)
		player.resetFallDistance();
		Combat.animate(player, "blink");
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 12, 0.25, 0.5, 0.25, 0.1);
	}

	// ---- Recall (001A) ----

	private static void record(ServerPlayer player, HeroState s, long now) {
		if (!player.isAlive()) {
			s.tracerHistory.clear();
			return;
		}
		s.tracerHistory.addLast(new Sample(now, player.position(), player.getYRot(), player.getXRot(),
			Combat.motion(player).scale(20), Ow.toOw(player.getHealth())));
		while (!s.tracerHistory.isEmpty() && now - s.tracerHistory.peekFirst().at() > Ow.ticks(RECALL_HISTORY)) {
			s.tracerHistory.removeFirst();
		}
	}

	private static void startRecall(ServerPlayer player, HeroState s, long now) {
		if (s.tracerRecall >= 0 || s.ability2Cooldown > 0 || s.tracerBombCast >= 0 || s.tracerSilenced || player.isPassenger()
			|| now - s.tracerSpawnAt < Ow.ticks(RECALL_SPAWN_LOCK) || s.tracerHistory.isEmpty()) {
			return;
		}
		// back to where she was 3 s ago (the oldest kept), her best health of those 3 s, never below now (W; WS 165 -> 175)
		Sample back = s.tracerHistory.peekFirst();
		double best = Ow.toOw(player.getHealth());
		for (Sample sample : s.tracerHistory) {
			best = Math.max(best, sample.health());
		}
		s.tracerRecall = 0;
		s.tracerHold = player.position();
		s.tracerRecallTo = back.pos();
		s.tracerRecallYaw = back.yaw();
		s.tracerRecallPitch = back.pitch();
		s.tracerRecallVelocity = back.velocity();
		s.tracerRecallHealth = best;
		s.tracerHistory.clear();
		// phased the whole 0.9 s (D x74BD9F6F + STU_33A46033; WS phased 56 / 56), unseen; it takes the weapon's slot over a
		// reload (D #224 priority 3 over the reload's 2, M)
		Combat.phase(player, RECALL_TIME);
		Combat.cleanse(player);
		player.setInvisible(true);
		s.interruptReload(0);
		s.tracerFiring = false;
		if (s.has(Perk.TRACER_QUANTUM_ENTANGLEMENT)) {
			quantum(player, s, now);
		}
		Combat.animate(player, "recall");
		TracerSounds.own(player, "recall.start");
		TracerSounds.split(player, "recall.rewind", player.position(), 70);
		player.level().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
	}

	private static void recall(ServerPlayer player, HeroState s, long now) {
		if (s.tracerRecall < 0) {
			return;
		}
		s.tracerRecall++;
		s.softLanding = false;
		player.resetFallDistance();
		if (s.tracerRecall == Ow.ticks(RECALL_ARRIVE) && s.tracerRecallTo != null) {
			// back where she was, facing as she did, her best health of the 3 s (WS: all at once, 0.44 s in)
			double before = Ow.toOw(player.getHealth());
			player.connection.teleport(new PositionMoveRotation(s.tracerRecallTo, Vec3.ZERO, s.tracerRecallYaw, s.tracerRecallPitch),
				java.util.Set.of());
			s.tracerHold = s.tracerRecallTo;
			if (s.tracerRecallHealth > before) {
				player.setHealth((float) Math.min(player.getMaxHealth(), Ow.hp(s.tracerRecallHealth)));
				// a tenth of what it gave back as ult charge (D #147 STU_008D30B1 x0.1, M)
				Combat.addUltCharge(player, (Ow.toOw(player.getHealth()) - before) * RECALL_ULT_SHARE);
			}
			player.level().sendParticles(ParticleTypes.REVERSE_PORTAL, s.tracerRecallTo.x, s.tracerRecallTo.y + 1, s.tracerRecallTo.z, 30, 0.3, 0.6, 0.3, 0.05);
		} else if (s.tracerHold != null && player.position().distanceToSqr(s.tracerHold) > 0.25) {
			// held still (her client does it; this is for what moves her there anyway: water, a piston)
			player.connection.teleport(new PositionMoveRotation(s.tracerHold, Vec3.ZERO, 0, 0), java.util.Set.of(Relative.X_ROT, Relative.Y_ROT));
		}
		if (s.tracerRecall == Ow.ticks(RECALL_AMMO_AT)) {
			// the magazine full (D #95 0.72 s -> 007D)
			s.primaryAmmo = Math.max(s.primaryAmmo, AMMO);
			s.primaryReload = -1;
		}
		if (s.tracerRecall >= Ow.ticks(RECALL_TIME)) {
			endRecall(player, s, now, true);
		}
	}

	private static void endRecall(ServerPlayer player, HeroState s, long now, boolean landed) {
		if (s.tracerRecall < 0) {
			return;
		}
		s.tracerRecall = -1;
		s.tracerHold = null;
		player.setInvisible(player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));
		// 12 s from its end (D #1 onFinished -> #92)
		s.ability2Cooldown = Ow.ticks(RECALL_COOLDOWN);
		if (landed) {
			// she leaves with the speed she had there, and what gravity added while she held still (WS: -1.96 -> -9.48 m/s)
			Vec3 v = s.tracerRecallVelocity == null ? Vec3.ZERO : s.tracerRecallVelocity;
			double held = RECALL_TIME - RECALL_ARRIVE;
			v = new Vec3(v.x, Math.max(-dev.overcraft.hero.OwMovement.TERMINAL_SPEED, v.y - dev.overcraft.hero.OwMovement.GRAVITY_DOWN * held), v.z);
			if (player.onGround() && v.y < 0) {
				v = new Vec3(v.x, 0, v.z);
			}
			player.setDeltaMovement(v.scale(1.0 / 20));
			Combat.send(player);
			player.resetFallDistance();
			TracerSounds.split(player, "recall.end", player.position(), 70);
			s.tracerVoiceAt = now + Ow.ticks(0.25);
			if (s.has(Perk.TRACER_QUANTUM_ENTANGLEMENT)) {
				// refreshed at the end (D #414 onFalse -> #416 again, M)
				quantum(player, s, now);
			}
		}
		s.tracerRecallVelocity = null;
		s.tracerRecallTo = null;
	}

	/** Quantum Entanglement: 50 overhealth held 6 s, then 25 a second; her magazine to 60 (D 51EE #416 / #410). */
	private static void quantum(ServerPlayer player, HeroState s, long now) {
		Combat.overhealthPool(player, QUANTUM_OVERHEALTH, QUANTUM_OVERHEALTH, QUANTUM_HOLD, QUANTUM_DECAY);
		s.primaryAmmo = AMMO + QUANTUM_AMMO;
		s.primaryReload = -1;
		s.tracerOverAmmoAt = now;
		TracerSounds.own(player, "perk.quantum");
	}

	/** Rounds over the magazine stay 6 s, then go (D 0016 #186; the rate ours). */
	private static void overAmmo(HeroState s, long now) {
		if (s.primaryAmmo > AMMO && !s.tracerFiring && now - s.tracerOverAmmoAt > Ow.ticks(QUANTUM_HOLD)
			&& now % Math.max(1, Math.round(20 / QUANTUM_AMMO_DECAY)) == 0) {
			s.primaryAmmo--;
		}
	}

	// ---- Pulse Bomb (0034) ----

	private static void bombCast(ServerPlayer player, HeroState s, boolean able) {
		if (s.tracerBombCast < 0) {
			return;
		}
		if (!able || s.tracerSilenced) {
			// a stun, a sleep or a hack in the 0.15 s: no throw, no charge spent (ours; W: "hackable during the cast")
			s.tracerBombCast = -1;
			return;
		}
		if (++s.tracerBombCast < Ow.ticks(BOMB_CAST)) {
			return;
		}
		s.tracerBombCast = -1;
		if (s.ultCharge < Combat.ultCost(s)) {
			return;
		}
		// spent at the throw (D #133), then 0.2 s before she fires (D #53)
		s.ultCharge = Combat.afterUlt(Hero.TRACER, s.ultCharge);
		s.weaponRecovery = Math.max(s.weaponRecovery, Ow.ticks(BOMB_RECOVERY));
		Vec3 from = fromEyes(player, BOMB_RIGHT, BOMB_UP, BOMB_AHEAD);
		PulseBombEntity bomb = new PulseBombEntity(player, player.level());
		bomb.launch(from, player.getViewVector(1.0f), Ow.speed(BOMB_SPEED));
		player.level().addFreshEntity(bomb);
		HeroSounds.ultLine(player, TracerSounds.of("ult.friendly"), TracerSounds.of("ult.enemy"), 5);
	}

	/**
	 * The bomb goes off at {@code at} ({@code stuck}: what it was on, which takes its blast whatever is between): 350 to 2 m, down
	 * to 70 at 4 m, half on herself, no push (D 0C1B #61 / #155). No ult charge from it (D: only Stadium's 0122 gives it).
	 */
	public static void explode(ServerPlayer owner, PulseBombEntity bomb, Vec3 at, @org.jspecify.annotations.Nullable Entity stuck) {
		ServerLevel level = owner.level();
		LivingEntity stuckTo = stuck instanceof EnderDragonPart part ? part.parentMob : stuck instanceof LivingEntity living ? living : null;
		for (dev.overcraft.hero.Barriers.Shape b : dev.overcraft.hero.Barriers.enemyBarriers(level, owner)) {
			Vec3 near = dev.overcraft.hero.Barriers.nearest(b, at);
			if (near.distanceTo(at) <= BOMB_RADIUS_OUT) {
				b.barrierTake(BOMB_DAMAGE * dev.overcraft.pharah.Pharah.splashFactor(near.distanceTo(at), BOMB_FULL, BOMB_RADIUS_OUT, BOMB_EDGE), near);
			}
		}
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, BOMB_RADIUS_OUT,
			d -> BOMB_DAMAGE * dev.overcraft.pharah.Pharah.splashFactor(d, BOMB_FULL, BOMB_RADIUS_OUT, BOMB_EDGE));
		net.minecraft.world.damagesource.DamageSource source = level.damageSources().explosion(bomb, owner);
		boolean killed = false;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BOMB_RADIUS_OUT + 2))) {
			boolean me = e == owner;
			if (!e.isAlive() || !me && !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > BOMB_RADIUS_OUT) {
				continue;
			}
			if (e != stuckTo && (!Combat.inSight(level, at, e, bomb) || !me
				&& (dev.overcraft.hero.Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null
				|| dev.overcraft.brigitte.Barrier.shields(e, owner, at)))) {
				continue;
			}
			double share = dev.overcraft.pharah.Pharah.splashFactor(d, BOMB_FULL, BOMB_RADIUS_OUT, BOMB_EDGE);
			float dealt = Combat.damage(e, source, BOMB_DAMAGE * share * (me ? BOMB_SELF : 1), BOMB_BLOW);
			if (!me && dealt > 0) {
				Perks.add(owner, dealt);
				Combat.addFireForDamage(owner, dealt, e);
				Effects.hitMarker(owner, e, false);
				killed |= !e.isAlive();
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 40, 1.2, 1.2, 1.2, 0.3);
		TracerSounds.all(level, "pulse_bomb.explode", at, 200);
		if (killed && owner.isAlive()) {
			// "Kaboom!" (0034 #394 -> #372: a kill by the bomb)
			Voice.say(owner, "pulse_bomb_kill", 1);
		}
	}

	/** The bomb stuck to {@code target} (an enemy: 5 to it) or a block (null). */
	public static void stuck(ServerPlayer owner, PulseBombEntity bomb, @org.jspecify.annotations.Nullable LivingEntity target, Vec3 at) {
		ServerLevel level = owner.level();
		if (target != null) {
			float dealt = Combat.damage(target, level.damageSources().thrown(bomb, owner), BOMB_STICK, BOMB_BLOW);
			if (dealt > 0) {
				Perks.add(owner, dealt);
				Effects.hitMarker(owner, target, false);
			}
			TracerSounds.split(owner, "pulse_bomb.stick", at, 75);
			// "That's a stick!" 0.3 s after (0034 #229; ours: at once)
			Voice.say(owner, "pulse_bomb_stick", 1);
		} else {
			TracerSounds.all(level, "pulse_bomb.stick_block", at, 75);
		}
		TracerSounds.all(level, "pulse_bomb.fuse", at, 60);
	}

	// ---- Pulse Pistols (0016) ----

	private static void pistols(ServerPlayer player, HeroState s, boolean able, long now) {
		s.weaponRecovery = Math.max(0, s.weaponRecovery - Combat.attackRate(player));
		if (s.primaryReload >= 0) {
			s.primaryReload += Combat.attackRate(player);
			if (s.primaryReload >= Ow.ticks(RELOAD_AMMO_AT) && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= Ow.ticks(RELOAD)) {
				s.primaryReload = -1;
			}
		}
		boolean wants = able && s.primaryHeld && s.tracerRecall < 0 && s.tracerBombCast < 0 && s.primaryReload < 0;
		if (s.tracerFiring && (!wants || s.primaryAmmo < AMMO_PER_SHOT)) {
			// the burst's over: its tail, the 0.2 s before the next (D #2 -> #12), the dry click when it emptied (#62)
			s.tracerFiring = false;
			s.tracerFireLockUntil = now + Ow.ticks(REFIRE_LOCK);
			TracerSounds.split(player, "pistol.fire_end", player.getEyePosition(), 100);
			if (s.primaryAmmo < AMMO_PER_SHOT) {
				TracerSounds.own(player, "pistol.empty");
			}
		}
		// the bloom: held 0.2 s after the last shot, then down at 4 a second
		if (!s.tracerFiring && now - s.tracerLastShot > Ow.ticks(SPREAD_HOLD)) {
			s.tracerBloom = Math.max(0, s.tracerBloom - SPREAD_RECOVER / 20);
		}
		if (able && s.primaryReload < 0 && s.primaryAmmo < AMMO_PER_SHOT && now >= s.tracerFireLockUntil && s.tracerRecall < 0) {
			// empty: the reload starts by itself after the lock (D #4)
			startReload(player, s);
			return;
		}
		if (!wants || s.primaryAmmo < AMMO_PER_SHOT || s.weaponRecovery > 0 || !s.tracerFiring && now < s.tracerFireLockUntil) {
			return;
		}
		boolean first = !s.tracerFiring;
		s.tracerFiring = true;
		s.primaryAmmo -= AMMO_PER_SHOT;
		s.weaponRecovery += 20 / RATE;
		s.tracerLastShot = now;
		TracerSounds.split(player, first ? "pistol.fire_first" : "pistol.fire", player.getEyePosition(), 200);
		TracerSounds.at(player.level(), "pistol.bullet_3p", player.getEyePosition(), 60, player);
		Combat.animate(player, "fire");
		volley(player, s, SPREAD_MAX * s.tracerBloom);
		s.tracerBloom = Math.min(1, s.tracerBloom + 1 / SPREAD_SHOTS);
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO || s.tracerRecall >= 0) {
			return;
		}
		s.primaryReload = 0;
		s.tracerFiring = false;
		Combat.animate(player, "reload");
		TracerSounds.split(player, "reload", player.getEyePosition(), 45);
	}

	/** A falloff share at {@code metres}: full to 10 m, x0.3 from 20 m, linear between (D; M the line). */
	public static double falloff(double metres) {
		if (metres <= FALLOFF_START) {
			return 1;
		}
		if (metres >= FALLOFF_END) {
			return FALLOFF_MIN;
		}
		return 1 - (1 - FALLOFF_MIN) * (metres - FALLOFF_START) / (FALLOFF_END - FALLOFF_START);
	}

	private static final class Hit {
		double damage;
		int bullets;
		boolean crit;
		Vec3 at;
	}

	/**
	 * One shot: a bullet from each pistol, each turned at random by up to {@code spread} degrees, hitscan from her eyes to the
	 * first enemy within 0.02 m before a block. A shot's bullets on one creature are summed into one hit (Minecraft's hurt
	 * cooldown would drop the second), which armor takes as its bullets (Shields.pellets, as D.Va's Fusion Cannons).
	 */
	private static void volley(ServerPlayer player, HeroState s, double spread) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Map<LivingEntity, Hit> hits = new LinkedHashMap<>();
		Vec3 wall = null;
		for (int i = 0; i < BULLETS; i++) {
			Vec3 dir = dev.overcraft.sombra.Sombra.cone(look, spread, player.getRandom());
			Vec3 end = eye.add(dir.scale(RANGE));
			BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 block = blockHit.getLocation();
			LivingEntity best = null;
			Vec3 bestAt = null;
			double bestDist = eye.distanceToSqr(block);
			for (Entity e : level.getEntities(player, new AABB(eye, block).inflate(1))) {
				LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
				if (target == null || !target.isAlive() || !Allies.canHurt(player, target)) {
					continue;
				}
				java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(RADIUS).clip(eye, block);
				if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
					best = target;
					bestAt = hit.get();
					bestDist = hit.get().distanceToSqr(eye);
				}
			}
			// the stand-in look: a spark from her pistol (left, then right) to where the bullet ended
			Vec3 muzzle = fromEyes(player, i == 0 ? -MUZZLE_SIDE : MUZZLE_SIDE, MUZZLE_UP, MUZZLE_AHEAD);
			Vec3 stop = best != null ? bestAt : block;
			trail(level, muzzle, stop);
			if (best == null) {
				if (blockHit.getType() == HitResult.Type.BLOCK && wall == null) {
					wall = block;
				}
				continue;
			}
			boolean crit = dev.overcraft.hero.Headshot.crossed(best, bestAt, bestAt.subtract(eye), RADIUS);
			Hit h = hits.computeIfAbsent(best, k -> new Hit());
			h.damage += BULLET_DAMAGE * falloff(Math.sqrt(bestDist)) * (crit ? CRIT : 1);
			h.bullets++;
			h.crit |= crit;
			h.at = bestAt;
		}
		if (wall != null) {
			TracerSounds.at(level, "pistol.hit_block", wall, 32);
		}
		for (Map.Entry<LivingEntity, Hit> e : hits.entrySet()) {
			LivingEntity target = e.getKey();
			Hit h = e.getValue();
			float dealt;
			Shields.pellets = h.bullets;
			try {
				dealt = Combat.damage(target, Combat.hitscan(player), h.damage, Blow.weapon(h.crit).by(Hero.TRACER));
			} finally {
				Shields.pellets = 1;
			}
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, target);
			TracerSounds.split(player, "pistol.hit_enemy", h.at, 500);
			if (dealt > 0) {
				if (h.crit) {
					s.critAt = level.getGameTime();
				}
				Effects.hitMarker(player, target, h.crit);
			}
		}
	}

	private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		int n = (int) Math.min(12, Math.max(1, len / 1.5));
		for (int k = 1; k <= n; k++) {
			Vec3 p = from.add(d.scale(k / (double) n * Math.min(1, 18 / Math.max(len, 1e-3))));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 1, 0, 0, 0, 0);
	}

	/** A point from her eyes in her look's frame (right, up, ahead), or her eyes when a wall is between. */
	static Vec3 fromEyes(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 at = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return at;
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		long now = player.level().getGameTime();
		boolean asleep = Sleep.asleep(player);
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			// Shift or the right button: her client sends both as this press (ClientTracer)
			blink(player, s, now);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !asleep) {
			startRecall(player, s, now);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !asleep && s.tracerBombCast < 0 && s.tracerRecall < 0
			&& !s.tracerSilenced && s.ultCharge >= Combat.ultCost(s)) {
			s.tracerBombCast = 0;
			s.tracerFiring = false;
			s.interruptReload(BOMB_CAST + BOMB_RECOVERY);
			Combat.animate(player, "pulse_bomb");
			TracerSounds.split(player, "pulse_bomb.throw", player.getEyePosition(), 75);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.tracerRecall < 0 && s.tracerBombCast < 0) {
			startReload(player, s);
		}
	}

	// ---- the perks' and the sub-role's hooks ----

	/** Her quick melee hit an enemy: Kinetic Reload's 12 rounds (D 51EE #393; an empty magazine's reload ends with it). */
	public static void meleeHit(ServerPlayer player, HeroState s) {
		if (!s.has(Perk.TRACER_KINETIC_RELOAD) || s.primaryAmmo >= AMMO) {
			return;
		}
		s.primaryAmmo = Math.min(AMMO, s.primaryAmmo + KINETIC_AMMO);
		s.primaryReload = -1;
		TracerSounds.own(player, "perk.kinetic_reload");
	}

	/**
	 * A health pack on her (ours: a potion of Healing, Minecraft having no packs; HealOrHarmMixin): Flanker's +50 (D 01C7 #2563)
	 * and Blink Packs' charge (D 51EE #404).
	 */
	public static void healthPack(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != Hero.TRACER || !HeroManager.heroMode(player)) {
			return;
		}
		Combat.heal(player, FLANKER_PACK);
		if (s.has(Perk.TRACER_BLINK_PACKS) && s.tracerBlinks < BLINK_CHARGES) {
			s.tracerBlinks = Math.min(BLINK_CHARGES, s.tracerBlinks + PACK_BLINKS);
			TracerSounds.own(player, "blink_recharge_" + s.tracerBlinks);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		int mode = (s.tracerRecall >= 0 ? MODE_RECALL : 0) | (s.tracerSilenced ? MODE_SILENCED : 0);
		double charging = s.tracerBlinks >= BLINK_CHARGES ? -1 : s.tracerBlinkTimer / Ow.ticks(BLINK_RECHARGE);
		float nextCharge = s.tracerBlinks >= BLINK_CHARGES ? 0 : (float) ((Ow.ticks(BLINK_RECHARGE) - s.tracerBlinkTimer) / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1,
			s.tracerBlinks, (float) charging,
			s.tracerBlinks > 0 ? 0 : nextCharge, (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk),
			nextCharge, mode, s.tracerRecall >= 0 ? (float) ((Ow.ticks(RECALL_TIME) - s.tracerRecall) / 20.0) : 0,
			(float) s.tracerBloom);
	}

	/** Out of play (#100): Recall ends where she is (its cooldown starts), the throw's cast ends, the burst ends. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		endRecall(player, s, player.level().getGameTime(), false);
		s.tracerBombCast = -1;
		s.tracerFiring = false;
		s.tracerHistory.clear();
		s.tracerVoiceAt = -1;
	}
}
