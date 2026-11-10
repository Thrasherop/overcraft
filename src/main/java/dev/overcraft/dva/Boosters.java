package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Tank;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.dva.DvaStats.*;

/**
 * Boosters (047E; dva-plan.md "What's new" 9): Shift in the mech. She flies at 12 m/s along her aim, no gravity, turning with
 * the mouse (no turn-rate node found: D), for 2 s, or until Shift again from 0.4 s on. Her own client moves her
 * (OwMovement.dvaBoosters, from the sync's {@link Dva#MODE_BOOSTING}, as Sigma's flight); here the rest: each enemy once a
 * flight in a 1.5 m sphere 1 m up and 2 m ahead takes 25 and is thrown 17 m/s along her flight and 4 up; she takes a tenth of
 * any knockback meanwhile; her start is the Initiator's trigger (50 over 1 s: Dva.initiator); the 4 s cooldown starts 0.3 s
 * after it ends; landing from it doesn't hurt (#35). Fusion fires without its slow, the matrix works. Stand-in look and sound.
 */
public final class Boosters {
	/** What its hits are, for the kill feed (Combat.damage, #84). */
	static final dev.overcraft.hero.Blow BLOW = dev.overcraft.hero.Blow.ability("boosters").by(dev.overcraft.hero.Hero.DVA);

	private Boosters() {
	}

	public static boolean active(HeroState s) {
		return s.dvaBoost >= 0;
	}

	/** Shift in the mech: starts it if ready; from 0.4 s into a flight, ends it (#179 -> #180). */
	static void press(ServerPlayer player, HeroState s) {
		if (active(s)) {
			if (s.dvaBoost >= Ow.ticks(BOOSTERS_MIN)) {
				end(player, s);
			}
			return;
		}
		if (s.ability1Cooldown > 0 || s.dvaBoostCooldownIn >= 0 || Sleep.asleep(player)) {
			return;
		}
		s.dvaBoost = 0;
		s.dvaBoostExtra = 0;
		s.dvaBoostHit.clear();
		s.softLanding = true;
		// "Mobilizing!" (03DA)
		dev.overcraft.hero.Voice.say(player, "boosters", 1);
		// the Initiator's trigger (047E #487 message 2B3B -> 01C7 #2953)
		Dva.initiator(player, s);
		// her flight clips (alias 0x3BB: 34BB start -> 307C flight, 1P 3313 -> 318A)
		Combat.animate(player, "boosters_se0");
		player.level().playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 0.8f);
	}

	/** Each tick in hero mode (Dva.tick); {@code can}: she's in her mech with her controls. */
	static void tick(ServerPlayer player, HeroState s, boolean can) {
		if (s.dvaBoostCooldownIn >= 0 && --s.dvaBoostCooldownIn < 0) {
			// its 4 s from 0.3 s after the flight (#168 -> #171)
			s.ability1Cooldown = Ow.ticks(BOOSTERS_COOLDOWN);
		}
		if (!active(s)) {
			return;
		}
		if (!can) {
			end(player, s);
			return;
		}
		s.dvaBoost++;
		if (s.dvaBoost == Ow.ticks(0.43)) {
			// the start clip's 0.43 s are over: the flight (307C)
			Combat.animate(player, "boosters_se1");
		}
		hits(player, s);
		ServerLevel level = player.level();
		// stand-in thrusters: small flames low on her back (big ones filled the third-person camera: found playing)
		Vec3 back = player.position().add(0, 0.9, 0).subtract(Vec3.directionFromRotation(0, player.getYRot()).scale(0.6));
		level.sendParticles(ParticleTypes.SMALL_FLAME, back.x, back.y, back.z, 2, 0.15, 0.1, 0.15, 0.005);
		if (DvaPerks.boostersLeft(s) <= 0) {
			end(player, s);
		}
	}

	static void end(ServerPlayer player, HeroState s) {
		if (!active(s)) {
			return;
		}
		s.dvaBoost = -1;
		s.dvaBoostEndedAt = player.level().getGameTime();
		s.dvaBoostCooldownIn = Ow.ticks(BOOSTERS_COOLDOWN_DELAY);
		// the fall after it lands her safely (#35)
		s.softLanding = true;
		Combat.animate(player, "boosters");
	}

	/** The impact: a 1.5 m sphere 1 m up and 2 m ahead of her, each enemy in it once a flight (#172 / #173). */
	private static void hits(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 centre = player.position().add(0, BOOSTERS_HIT_UP, 0).add(look.scale(BOOSTERS_HIT_AHEAD));
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(BOOSTERS_HIT_RADIUS + 2))) {
			if (e == player || !e.isAlive() || s.dvaBoostHit.contains(e.getId()) || !Allies.areaHits(player, e)
				|| e.getBoundingBox().distanceToSqr(centre) > BOOSTERS_HIT_RADIUS * BOOSTERS_HIT_RADIUS) {
				continue;
			}
			s.dvaBoostHit.add(e.getId());
			Vec3 before = Combat.motion(e);
			// 25, Extended Boosters' 35, and its +0.5 s (514D #310, 047E #458)
			float dealt = Combat.damage(e, level.damageSources().playerAttack(player), DvaPerks.boostersDamage(s), BLOW);
			DvaPerks.boostersHit(s);
			Combat.credit(player, dealt, e);
			if (dealt > 0) {
				Effects.hitMarker(player, e, false);
			}
			// thrown along her flight, 17 m/s and 4 up (the target's own resistance and a Stalwart's 40 % less count)
			// its fall cut first (the push lifts it); Combat.push scales, sends and credits it (#93)
			if (before.y < 0) {
				e.setDeltaMovement(before.x, 0, before.z);
			}
			Combat.push(e, flat.scale(Ow.speed(BOOSTERS_KNOCKBACK)).add(0, Ow.speed(BOOSTERS_KNOCKBACK_UP), 0), player, BLOW);
			level.playSound(null, e.getX(), e.getY() + 1, e.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.6f);
		}
	}

	/** Knockback on her while she flies: a tenth (#243 1E09 -0.9); 1 otherwise. */
	public static double knockbackFactor(LivingEntity entity) {
		if (entity instanceof Player player && HeroManager.state(player) instanceof HeroState s && s.hero == dev.overcraft.hero.Hero.DVA
			&& active(s)) {
			return BOOSTERS_KNOCKBACK_TAKEN;
		}
		return 1;
	}
}
