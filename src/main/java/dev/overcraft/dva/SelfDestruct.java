package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.entity.DvaMechEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Tank;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.dva.DvaStats.*;

/**
 * Self-Destruct (0B2F; dva-plan.md "Self-Destruct"): Q in the mech with 1700 charged, or in the wreck's 1.0 s (#213 / 0592).
 * She ejects (the eject's launch, 0.4 s immune) and the mech flies on unmanned with the motion it had (a
 * {@link DvaMechEntity} of kind SELF_DESTRUCT: no mover, it keeps its speed and falls), and 3 s later it bursts from 1.3 m
 * above its feet: 1000 to every enemy within 6 m, then down to 100 at 21 m (W; the data reads 50: DvaStats), nothing past, only those in sight of the centre
 * and not behind an enemy barrier; she and her allies spared. Knockback 20 out and 10 up (22.4 m/s) at the centre, down to
 * 7.8 at 21 m (ours, Open 4). No block damage, no fire (Overwatch's). Then her Call Mech is full (#274 -> 026A #887), unless
 * the mech fell out of the world first. Stand-in look and sound (hers: step 11).
 */
public final class SelfDestruct {
	/** What its hits are, for the kill feed (Combat.damage, #84). */
	static final dev.overcraft.hero.Blow BLOW = dev.overcraft.hero.Blow.ult("self_destruct").by(dev.overcraft.hero.Hero.DVA);

	private SelfDestruct() {
	}

	/** The damage at {@code d} m from the centre: 1000 to 6 m, then 100 + 900 x (21 - d) / 15, none past 21 (DvaStats). */
	public static double damage(double d) {
		if (d > SELF_DESTRUCT_RADIUS) {
			return 0;
		}
		if (d <= SELF_DESTRUCT_INNER) {
			return SELF_DESTRUCT_DAMAGE;
		}
		return SELF_DESTRUCT_MIN + (SELF_DESTRUCT_DAMAGE - SELF_DESTRUCT_MIN) * (SELF_DESTRUCT_RADIUS - d) / (SELF_DESTRUCT_RADIUS - SELF_DESTRUCT_INNER);
	}

	/** The knockback's share at {@code d} m: 1 at the centre, down to 7.8 / 22.4 at 21 m (linear: ours, Open 4). */
	public static double knockbackFactor(double d) {
		return 1 - (1 - SELF_DESTRUCT_KNOCKBACK_EDGE) * Math.min(1, d / SELF_DESTRUCT_RADIUS);
	}

	/** She casts it (from the mech, or the wreck): the mech flies on, she ejects. Its charge is spent. */
	static void cast(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		// the motion it had: Boosters' flight along her aim (her client moves her), else what she moved this tick
		Vec3 motion = Boosters.active(s) ? player.getViewVector(1.0f).scale(Ow.speed(BOOSTERS_SPEED))
			: s.dvaForm == Dva.WRECK ? Vec3.ZERO : player.position().subtract(player.xo, player.yo, player.zo);
		s.ultCharge = 0;
		// the ult line: "Activating self destruct sequence!" to her side, "Nerf this!" to her enemies (0B2F #443: 04D1 / 03DC)
		var friendly = DvaSounds.of("ult.friendly");
		var enemy = DvaSounds.of("ult.enemy");
		if (friendly != null && enemy != null) {
			dev.overcraft.hero.HeroSounds.ultLine(player, friendly, enemy, 5);
		}
		DvaMechEntity mech = new DvaMechEntity(level, player, DvaMechEntity.SELF_DESTRUCT, player.position(), player.getYRot());
		mech.setDeltaMovement(motion);
		// cast while boosting, the Boosters stay on for their remaining time (W rev 408749: "If Self-Destruct was activated
		// while Boosters were on, they will remain active for their remaining duration"): no gravity on it until then
		if (Boosters.active(s)) {
			mech.boostTicks = DvaPerks.boostersLeft(s);
		}
		level.addFreshEntity(mech);
		s.dvaSelfDestructId = mech.getId();
		Dva.eject(player, s, false);
		// the arming whine and countdown (#419 effect 1702 on the mech: 2AEA, 5DBF, 2DD5, 75 m)
		DvaSounds.all(level, "self_destruct.arm", mech.position().add(0, 1, 0), 75);
	}

	/** Each tick of the flying mech (DvaMechEntity): the warning, then the blast at 3 s. */
	public static void tick(DvaMechEntity mech, ServerPlayer owner) {
		ServerLevel level = (ServerLevel) mech.level();
		Vec3 c = mech.position().add(0, SELF_DESTRUCT_UP, 0);
		int fuse = Ow.ticks(SELF_DESTRUCT_FUSE);
		if (mech.tickCount < fuse) {
			// stand-in look: a glow growing to the blast
			level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 2 + mech.tickCount / 6, 0.6, 0.6, 0.6, 0.02);
		} else if (mech.tickCount == fuse) {
			blast(owner, mech, c);
		}
	}

	private static void blast(ServerPlayer owner, DvaMechEntity mech, Vec3 c) {
		// the mech's own level: she may be elsewhere by now (a respawn in another dimension; the critic's finding, 2026-11-11)
		ServerLevel level = (ServerLevel) mech.level();
		HeroState s = HeroManager.state(owner);
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, c);
			if (near.distanceTo(c) <= SELF_DESTRUCT_RADIUS) {
				b.barrierTake(damage(near.distanceTo(c)), near);
			}
		}
		// a Brigitte's up barrier the same, once (Accretion's way)
		dev.overcraft.brigitte.Barrier.burst(level, owner, c, SELF_DESTRUCT_RADIUS, SelfDestruct::damage);
		DamageSource source = level.damageSources().explosion(mech, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(SELF_DESTRUCT_RADIUS + 2))) {
			if (e == owner || !e.isAlive() || !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(c));
			if (d > SELF_DESTRUCT_RADIUS || !MicroMissiles.inSight(level, c, e) || Barriers.between(level, c, e.getBoundingBox().getCenter(), owner) != null
				|| dev.overcraft.brigitte.Barrier.shields(e, owner, c)) {
				continue;
			}
			float dealt = Combat.damage(e, source, damage(d), BLOW);
			Combat.credit(owner, dealt, e);
			Combat.addFireForDamage(owner, dealt, e);
			if (dealt > 0) {
				Effects.hitMarker(owner, e, false);
			}
			if (e.isAlive()) {
				// out from the centre and up (a Stalwart's 40 % less, the target's own resistance)
				Vec3 out = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z);
				out = out.lengthSqr() < 1.0E-6 ? Vec3.ZERO : out.normalize();
				// replacing its motion, scaled by its share (Combat.pushShare), sent to a player, credited (#93)
				double f = knockbackFactor(d);
				Combat.launch(e, out.scale(Ow.speed(SELF_DESTRUCT_KNOCKBACK) * f).add(0, Ow.speed(SELF_DESTRUCT_KNOCKBACK_UP) * f, 0), owner, BLOW);
			}
		}
		// her Call Mech is full (#274 message 0592 -> 026A #887), whatever form she's in now
		if (s != null && s.hero == Hero.DVA && owner.isAlive() && (s.dvaForm == Dva.PILOT)) {
			s.ultCharge = PILOT_ULT_COST;
		}
		if (s != null) {
			s.dvaSelfDestructId = null;
		}
		// stand-in look and sound
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 2, 1, 2, 0);
		// the blast (#269 effect 1703: 5DC0 + 2DD8, 120 m)
		DvaSounds.all(level, "self_destruct.blast", c, 120);
	}
}
