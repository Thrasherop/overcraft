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

	private Barrier() {
	}

	/** Each tick of her kit: up while the right button is held (and not broken), regenerating while down. */
	public static void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		boolean want = controls && player.isAlive() && s.secondaryHeld && now >= s.barrierBrokenUntil && s.barrierHealth > 0;
		if (want && !s.barrierUp) {
			s.barrierUp = true;
			dev.overcraft.hero.Combat.animate(player, "shield_raise");
		} else if (!want && s.barrierUp && s.bashTicks < 0) {
			s.barrierUp = false;
			s.barrierDownSince = now;
			dev.overcraft.hero.Combat.animate(player, "shield_lower");
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

	public static double maxHealth(HeroState s) {
		return s.rallyTicks > 0 ? BARRIER_HEALTH_RALLY : BARRIER_HEALTH;
	}

	/** Leaving her (another hero, dying): down, its slow off. */
	public static void clear(ServerPlayer player, HeroState s) {
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
			s.barrierHealth -= took;
			// 2% of what it absorbs is charge for her ult (0EE4 #149, M)
			dev.overcraft.hero.Combat.addUltCharge(brig, took * BARRIER_ULT_SHARE, false);
			if (s.barrierHealth <= 0) {
				s.barrierHealth = 0;
				s.barrierBrokenUntil = level.getGameTime() + Ow.ticks(BARRIER_BROKEN);
				s.barrierUp = false;
				s.barrierDownSince = level.getGameTime();
				dev.overcraft.hero.Combat.animate(brig, "shield_lower");
			}
			damage = (float) Ow.hp(ow - took);
			if (damage <= 0) {
				return 0;
			}
		}
		return damage;
	}

	/** Whether an up barrier stands between a blast's centre and {@code entity}, her or her ally (ServerExplosionMixin). */
	public static boolean blocksBlast(Entity entity, Vec3 center) {
		if (!(entity instanceof LivingEntity victim) || !(entity.level() instanceof ServerLevel level)) {
			return false;
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
		// from in front of it to behind it
		if (da <= 0 || db > 0) {
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
