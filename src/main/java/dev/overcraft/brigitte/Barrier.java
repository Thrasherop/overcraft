package dev.overcraft.brigitte;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.brigitte.BrigitteStats.*;

/**
 * Barrier Shield (0EE3 on her, the barrier pet 2284 running 0EE4): held up on the right button (the data's default), it
 * stands in front of her and takes what would cross it: hits on her or an ally behind it from in front, arrows, blasts,
 * melee (ours: Overwatch's barriers stop melee but for Reinhardt's hammer). 300 health; down, it comes back at 85 a second
 * after 2 s; broken, 5 s before it can go up again. Up, she's 30% slower and her flail is still (0EE3 #670). Its size is the
 * deployed shield's (1.51 m x 2.64 m, measured on the mesh 1064F's idle_up: no number in any graph), from the ground up,
 * 0.9 m ahead of her, square to her facing (ours). Here it's her state, not an entity.
 */
public final class Barrier {
	private static final Identifier SLOW = dev.overcraft.Overcraft.id("brigitte_barrier");
	/** The part of a hit the barrier didn't take, landing (LivingEntityMixin): not taken again. */
	public static boolean passing;
	/** A hit was taken by a barrier since this was last cleared (Sojourn's Railgun: +1 energy on a barrier, not +5). */
	public static boolean took;

	private Barrier() {
	}

	/** Each tick of her kit: up while the right button is held (and not broken), regenerating while down. */
	public static void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		boolean want = controls && player.isAlive() && s.secondaryHeld && now >= s.barrierBrokenUntil && s.barrierHealth > 0;
		if (want && !s.barrierUp) {
			s.barrierUp = true;
			dev.overcraft.hero.Combat.animate(player, "shield_raise");
			boolean rally = s.rallyTicks > 0;
			// in Rally its own raise and loop (6F8D, 2.083 s) replace the plain one's
			BrigitteSounds.split(player, rally ? "barrier.rally_raise" : "barrier.raise", player.getEyePosition(), rally ? 75 : 40);
			BrigitteSounds.split(player, rally ? "barrier.rally.loop" : "barrier.hold.loop", player.getEyePosition(), rally ? 75 : 40);
			s.barrierLowCalled = s.barrierHealth / maxHealth(s) <= 0.25 ? 2 : s.barrierHealth / maxHealth(s) <= 0.5 ? 1 : 0;
			dev.overcraft.hero.Voice.say(player, "barrier_raise", 1);
		} else if (!want && s.barrierUp && s.bashTicks < 0) {
			s.barrierUp = false;
			s.barrierDownSince = now;
			dev.overcraft.hero.Combat.animate(player, "shield_lower");
			lowered(player, s);
		}
		// (broken too: it refills through its 5 s lockout, from 2 s after: 0EE4 #208 has no other condition)
		if (!s.barrierUp && now - s.barrierDownSince >= Ow.ticks(BARRIER_REGEN_DELAY)) {
			s.barrierHealth = Math.min(maxHealth(s), s.barrierHealth + BARRIER_REGEN / 20.0);
		}
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			if (!s.barrierUp) {
				speed.removeModifier(SLOW);
			} else if (!speed.hasModifier(SLOW)) {
				speed.addTransientModifier(new AttributeModifier(SLOW, -BARRIER_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		}
	}

	/** Its lower sound, its loops stopped. */
	private static void lowered(ServerPlayer player, HeroState s) {
		stopLoops(player);
		BrigitteSounds.split(player, s.rallyTicks > 0 ? "barrier.rally_lower" : "barrier.lower", player.getEyePosition(),
			s.rallyTicks > 0 ? 75 : 40);
	}

	private static void stopLoops(ServerPlayer player) {
		BrigitteSounds.stop(player.level(), "barrier.hold.loop", "barrier.hold.loop_3p", "barrier.rally.loop", "barrier.rally.loop_3p");
	}

	public static double maxHealth(HeroState s) {
		return s.rallyTicks > 0 ? BARRIER_HEALTH_RALLY : BARRIER_HEALTH;
	}

	/** Leaving her (another hero, dying): down, its slow off. */
	public static void clear(ServerPlayer player, HeroState s) {
		if (s.barrierUp) {
			stopLoops(player);
		}
		s.barrierUp = false;
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SLOW);
		}
	}

	/**
	 * Every hit passes here (LivingEntityMixin, before armor): what's left of {@code damage} (Minecraft health) after any
	 * Brigitte's barrier that stands between the hit's source and {@code victim} took it. The victim is her or her ally.
	 */
	public static float absorb(LivingEntity victim, DamageSource source, float damage) {
		if (damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return damage;
		}
		// the barriers that are entities (Sigma's) first: dev.overcraft.hero.Barriers
		damage = dev.overcraft.hero.Barriers.absorb(victim, source, damage);
		if (damage <= 0) {
			return 0;
		}
		Vec3 from = from(source, victim);
		if (from == null) {
			return damage;
		}
		Vec3 to = victim.getBoundingBox().getCenter();
		for (ServerPlayer brig : level.players()) {
			HeroState s = HeroManager.state(brig);
			if (s == null || s.hero != Hero.BRIGITTE || !s.barrierUp || brig.distanceToSqr(victim) > 100
				|| !Allies.isSelfOrAlly(brig, victim) || source.getEntity() == brig || !crosses(brig, s, from, to)) {
				continue;
			}
			double ow = Ow.toOw(damage);
			double took = Math.min(ow, s.barrierHealth);
			double before = s.barrierHealth / maxHealth(s);
			s.barrierHealth -= took;
			Barrier.took = true;
			double after = s.barrierHealth / maxHealth(s);
			// a hit on it (her flail's barrier contact 0015, M: anything hitting her barrier), and its calls
			BrigitteSounds.at(level, "hit_barrier", to.add(from.subtract(to).normalize().scale(1)), 100);
			dev.overcraft.hero.Voice.say(brig, "barrier_damaged", 1);
			if (after > 0 && (after <= 0.5 && s.barrierLowCalled < 1 || after <= 0.25 && s.barrierLowCalled < 2)) {
				// below 50 %, then 25 % (0540)
				s.barrierLowCalled = after <= 0.25 ? 2 : 1;
				BrigitteSounds.split(brig, "barrier.low", brig.getEyePosition(), 40);
				dev.overcraft.hero.Voice.say(brig, "barrier_low", 1);
			}
			// 2% of what it absorbs is charge for her ult (0EE4 #149, M)
			dev.overcraft.hero.Combat.addUltCharge(brig, took * BARRIER_ULT_SHARE, false);
			if (s.barrierHealth <= 0) {
				s.barrierHealth = 0;
				s.barrierBrokenUntil = level.getGameTime() + Ow.ticks(BARRIER_BROKEN);
				s.barrierUp = false;
				s.barrierDownSince = level.getGameTime();
				dev.overcraft.hero.Combat.animate(brig, "shield_lower");
				stopLoops(brig);
				BrigitteSounds.split(brig, "barrier.break", brig.getEyePosition(), 40);
				dev.overcraft.hero.Voice.say(brig, "barrier_broken", 1);
				// 268C at the barrier: its outline's flash, hexes and shards
				float yaw = brig.getYRot() * (float) (Math.PI / 180);
				Vec3 facing = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
				dev.overcraft.hero.Effects.play(level, "brigitte_barrier_break", brig.position().add(facing.scale(BARRIER_AHEAD)).add(0, 1.2, 0),
					facing, brig, s.rallyTicks > 0 ? 1 : 0, brig);
			}
			damage = (float) Ow.hp(ow - took);
			if (damage <= 0) {
				return 0;
			}
		}
		return damage;
	}

	/** Whether an up barrier stands between a blast's centre and {@code entity}, her or her ally (ServerExplosionMixin). */
	public static boolean blocksBlast(Entity entity, Vec3 center, @Nullable Entity cause) {
		if (!(entity instanceof LivingEntity victim) || !(entity.level() instanceof ServerLevel level)) {
			return false;
		}
		if (dev.overcraft.hero.Barriers.blocksBlast(entity, center, cause)) {
			return true;
		}
		Vec3 to = victim.getBoundingBox().getCenter();
		for (ServerPlayer brig : level.players()) {
			HeroState s = HeroManager.state(brig);
			if (s != null && s.hero == Hero.BRIGITTE && s.barrierUp && brig.distanceToSqr(victim) <= 100
				&& Allies.isSelfOrAlly(brig, victim) && crosses(brig, s, center, to)) {
				return true;
			}
		}
		return false;
	}

	/** Where a hit came from: a projectile's way in, a blast's centre, an attacker's eyes. */
	private static @Nullable Vec3 from(DamageSource source, LivingEntity victim) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof net.minecraft.world.entity.projectile.Projectile p) {
			Vec3 v = p.getDeltaMovement();
			return v.lengthSqr() < 1.0E-6 ? p.position() : p.position().subtract(v.normalize().scale(6));
		}
		// a blast (TNT, a creeper, a fireball's): its centre. A primed TNT is the hit's direct entity with no attacker, and
		// went through the barrier while only direct-less hits counted as blasts (found playing)
		if (source.getSourcePosition() != null && !(direct instanceof LivingEntity)) {
			return source.getSourcePosition();
		}
		Entity attacker = source.getEntity();
		return attacker != null && attacker != victim ? attacker.getEyePosition() : null;
	}

	/** Whether the way from {@code a} to {@code b} crosses her barrier's rectangle (square to her facing, 0.9 m ahead). */
	static boolean crosses(ServerPlayer brig, HeroState s, Vec3 a, Vec3 b) {
		float yaw = brig.getYRot() * (float) (Math.PI / 180);
		Vec3 facing = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 centre = brig.position().add(facing.scale(BARRIER_AHEAD));
		double da = a.subtract(centre).dot(facing), db = b.subtract(centre).dot(facing);
		// from one side of it to the other, either way (Overwatch's barriers block enemy fire both ways: Barriers)
		if (da == db || da > 0 && db > 0 || da < 0 && db < 0) {
			return false;
		}
		Vec3 at = a.add(b.subtract(a).scale(da / (da - db)));
		Vec3 side = new Vec3(facing.z, 0, -facing.x);
		double across = Math.abs(at.subtract(centre).dot(side));
		double up = at.y - brig.getY();
		boolean rally = s.rallyTicks > 0;
		return across <= (rally ? BARRIER_WIDTH_RALLY : BARRIER_WIDTH) / 2 && up >= -0.2 && up <= (rally ? BARRIER_HEIGHT_RALLY : BARRIER_HEIGHT);
	}
}
