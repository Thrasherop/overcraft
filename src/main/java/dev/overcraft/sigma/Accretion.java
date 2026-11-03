package dev.overcraft.sigma;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.AccretionEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Tank;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.sigma.SigmaStats.*;

/**
 * Accretion (1BBB on him, the rock 18B7 / 1EB9: sigma-map.md): ability 2 (E). A 0.65 s gather at -75% move speed with no
 * jump, then the rock (AccretionEntity) along his aim: 80 to the creature it hits, knocked down 1.1 s and pushed 6 m/s
 * along its flight and 3 up; a 40 burst where it lands (full to 1.5 m, 12 at its 2.5 m edge: ours, M) pushing 5 away and
 * 3 up. No self-damage (ours, the plan's decision), no block damage. +25 ult on a hit. 10 s cooldown from 0.45 s after
 * the throw. Look and sound from his data (step 10: sigma-effects.md "Accretion", sigma-sounds.md 4).
 */
public final class Accretion {
	private Accretion() {
	}

	/** The gather's slow, his own (a Stalwart doesn't resist it: Tank's OWN_SLOWS). */
	public static final Identifier CAST_SLOW = Overcraft.id("sigma_accretion_cast");

	static boolean casting(HeroState s) {
		return s.sigmaCast >= 0;
	}

	/** E: the gather starts, if ready and his hands are free. */
	static void press(ServerPlayer player, HeroState s) {
		if (casting(s) || s.ability2Cooldown > 0 || KineticGrasp.active(s) || GraviticFlux.active(s) || Sleep.asleep(player)
			|| !player.isAlive()) {
			return;
		}
		s.sigmaCast = Ow.ticks(ACCRETION_CAST);
		Combat.buff(player, Attributes.MOVEMENT_SPEED, CAST_SLOW, -ACCRETION_CAST_SLOW, ACCRETION_CAST);
		// his gather (alias 0x608, #627): HeroAnimator
		Combat.animate(player, "accretion_cast");
		// the gather 2C32 (#639: 5E0B his, 54A0 + 964B others' 70 m; its look 2C64 from SIGMA_STATE: SigmaFx), his line (0B9C, #664)
		SigmaSounds.split(player, "accretion.gather", player.getEyePosition(), 70);
		dev.overcraft.hero.Voice.say(player, "accretion", 1);
	}

	static void tick(ServerPlayer player, HeroState s, boolean controls) {
		if (!casting(s)) {
			return;
		}
		if (!controls || !player.isAlive() || Sleep.asleep(player)) {
			// cut short (a stun, G, death): no rock, no cooldown (ours, M)
			cancel(player, s);
			return;
		}
		if (--s.sigmaCast > 0) {
			return;
		}
		s.sigmaCast = -1;
		throwRock(player);
		// 10 s from 0.45 s after the throw (#669 / #570): the HUD counts the whole 10.45 s
		s.ability2Cooldown = Ow.ticks(ACCRETION_COOLDOWN + ACCRETION_COOLDOWN_DELAY);
	}

	static void cancel(ServerPlayer player, HeroState s) {
		if (casting(s)) {
			s.sigmaCast = -1;
			Combat.buff(player, Attributes.MOVEMENT_SPEED, CAST_SLOW, 0, 0);
			Combat.animate(player, "accretion_end");
		}
	}

	/** The rock from his eyes along his aim (the data's leaves hp 01B6, 1.34 m up his right side: ours, the crosshair's line). */
	private static void throwRock(ServerPlayer player) {
		AccretionEntity rock = new AccretionEntity(player, player.level());
		rock.launch(player.getEyePosition(), player.getViewVector(1.0f), Ow.speed(ROCK_SPEED));
		player.level().addFreshEntity(rock);
		Combat.animate(player, "accretion_throw");
		// the throw 2C33 (#809 -> #879 -> #640: 5E0C his, 964C others' 70 m); its flight loop and fly-by are his clients'
		SigmaSounds.split(player, "accretion.throw", player.getEyePosition(), 70);
	}

	/** The burst's damage at {@code d} m: 40 to 1.5 m, then down to 12 at 2.5 m, none past it. */
	public static double burstDamage(double d) {
		if (d > ROCK_BURST_RADIUS) {
			return 0;
		}
		if (d <= ROCK_BURST_FULL) {
			return ROCK_BURST;
		}
		double t = (d - ROCK_BURST_FULL) / (ROCK_BURST_RADIUS - ROCK_BURST_FULL);
		return ROCK_BURST * (1 - t * (1 - ROCK_BURST_EDGE));
	}

	/**
	 * The rock lands at {@code at} ({@code direct}: the creature it touched). The one it hit takes the 80, the knockdown and
	 * the push along its flight; then everyone in its 2.5 m (to the nearest point of their box), in sight of it and not
	 * behind an enemy barrier, takes the burst (pushed away from it, but the one it hit), and an enemy barrier in front
	 * of it takes it too. Never him.
	 */
	public static void impact(ServerPlayer owner, AccretionEntity rock, Vec3 at, @Nullable Entity direct, Vec3 face) {
		ServerLevel level = owner.level();
		EnderDragonPart part = direct instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : direct instanceof LivingEntity living ? living : null;
		Vec3 flight = rock.getDeltaMovement();
		Vec3 along = new Vec3(flight.x, 0, flight.z);
		along = along.lengthSqr() < 1.0E-8 ? Vec3.ZERO : along.normalize();
		if (target != null && Allies.canHurt(owner, target)) {
			Vec3 before = target.getDeltaMovement();
			DamageSource hit = level.damageSources().thrown(rock, owner);
			float dealt = part != null ? Combat.damagePart(level, part, hit, ROCK_IMPACT) : Combat.damage(target, hit, ROCK_IMPACT);
			Combat.credit(owner, dealt, target);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(target.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(owner, target, false);
			}
			// the character contact 6DC (8C30 his, 90DC others' 500 m)
			SigmaSounds.own(owner, "accretion.hit_enemy");
			SigmaSounds.at(level, "accretion.hit_enemy_3p", at, 500, owner);
			// +25 on a hit (#683, M)
			Combat.addUltCharge(owner, ROCK_ULT_ON_HIT * Tank.ultFactor(owner, target), false);
			if (target.isAlive()) {
				push(target, before, along, ROCK_KNOCKBACK, ROCK_KNOCKBACK_UP);
				// knocked down 1.1 s (224E, #585 / #590): no moving, no keys, no AI; carried by the push
				Sleep.knockdown(target, ROCK_KNOCKDOWN);
			}
		} else if (direct != null && !(direct instanceof LivingEntity) && part == null) {
			// an object (a boat, an end crystal): hurt as an arrow would
			direct.hurtServer(level, level.damageSources().thrown(rock, owner), Ow.hp(ROCK_IMPACT));
		}
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearestFront(b, at);
			if (near != null && near.distanceTo(at) <= ROCK_BURST_RADIUS) {
				b.barrierTake(burstDamage(near.distanceTo(at)), near);
			}
		}
		DamageSource source = level.damageSources().explosion(rock, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(ROCK_BURST_RADIUS + 2))) {
			if (!e.isAlive() || e == owner || !Allies.canHurt(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > ROCK_BURST_RADIUS || !inSight(level, at, e) || Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null) {
				continue;
			}
			Vec3 before = e.getDeltaMovement();
			float dealt = Combat.damage(e, source, burstDamage(d));
			Combat.credit(owner, dealt, e);
			Combat.addFireForDamage(owner, dealt, Ow.toOw(e.getMaxHealth()));
			if (dealt > 0) {
				Effects.hitMarker(owner, e, false);
			}
			if (e.isAlive()) {
				if (e == target) {
					// the one it hit keeps its push along the flight (Minecraft's blast knockback undone)
					e.setDeltaMovement(before);
				} else {
					Vec3 away = new Vec3(e.getX() - at.x, 0, e.getZ() - at.z);
					away = away.lengthSqr() < 1.0E-6 ? along : away.normalize();
					push(e, before, away, ROCK_BURST_KNOCKBACK, ROCK_BURST_KNOCKBACK_UP);
				}
			}
		}
		// its burst 2C07 (m_6CB564CC: SigmaFx, its crater on the face it hit) and 54A4 for everyone (80 m); on a block the shared
		// world contact 0245 (858A + 85C7, 90 m)
		Effects.play(level, "sigma_rock_impact", at, face, null, direct != null ? 1 : 0, owner);
		SigmaSounds.at(level, "accretion.impact", at, 80);
		if (direct == null && face.lengthSqr() > 0) {
			SigmaSounds.at(level, "accretion.hit_block", at, 90);
		}
	}

	/**
	 * Sets its speed to what it had before the hit plus {@code along} m/s along {@code dir} and {@code up} m/s up (replacing
	 * Minecraft's own hit knockback), less a Stalwart's share, Sigma's in flight, and the creature's own resistance (an
	 * iron golem none: Minecraft's knockback rule).
	 */
	static void push(LivingEntity e, Vec3 before, Vec3 dir, double along, double up) {
		double f = Tank.knockbackFactor(e) * GraviticFlux.knockbackFactor(e)
			* Math.max(0, 1 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		e.setDeltaMovement(before.add(dir.scale(Ow.speed(along) * f)).add(0, Ow.speed(up) * f, 0));
		e.needsSync = true;
		// a player's own client moves them: it needs the speed too (needsSync reaches only the others watching)
		e.syncVelocity = e.syncVelocity || e instanceof net.minecraft.world.entity.player.Player;
	}

	private static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}
}
