package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's Mediblaster (her weapon 4445): the left button fires volleys of 12 hitscan rounds at 34 a second (a volley always
 * finishes once started), then 0.45 s before the next; each round damages the first enemy (7.5) or heals the first ally (6)
 * on its line before a block: allies are easier to hit (0.22 m against 0.02). 180 rounds, 1.5 s reload.
 */
final class Mediblaster {
	private Mediblaster() {
	}

	static void tick(ServerPlayer player, HeroState s, double attack) {
		if (s.primaryAmmo == 0 && s.primaryReload < 0 && s.junoVolley == 0) {
			Juno.startReload(player, s);
		}
		if (Juno.gliding(s) && dev.overcraft.hero.HeroManager.hasPerk(player, dev.overcraft.hero.Perk.JUNO_FASTER_BLASTER)) {
			// Faster Blaster: gliding, it fires on while the button is held, 22 rounds a second, no volleys
			s.junoVolley = 0;
			if (s.primaryHeld && s.primaryReload < 0 && s.primaryAmmo > 0) {
				s.junoVolleyClock += FASTER_BLASTER_RATE / 20 * attack;
				while (s.junoVolleyClock >= 1 && s.primaryAmmo > 0) {
					s.junoVolleyClock -= 1;
					s.primaryAmmo--;
					round(player);
				}
			} else {
				s.junoVolleyClock = 1;
			}
			return;
		}
		if (s.junoVolley == 0 && s.primaryHeld && s.weaponRecovery <= 0 && s.primaryReload < 0 && s.primaryAmmo > 0) {
			// a new volley: its first round at once
			s.junoVolley = VOLLEY;
			s.junoVolleyClock = 1;
			Combat.animate(player, "fire");
			// the volley's stream (5C2D held for #913's volley: one Play sounds all 12, its 3p copy B92C 150 m) and its 12
			// projectiles' "sound only" slot (538E's 5C5F, 60 m, built as the whole volley: H slot, M role), once a volley
			JunoSounds.split(player, "mediblaster.volley", player.getEyePosition(), 150);
			JunoSounds.at(player.level(), "mediblaster.projectile", player.getEyePosition(), 60);
		}
		if (s.junoVolley == 0) {
			return;
		}
		s.junoVolleyClock += VOLLEY_RATE / 20 * attack;
		while (s.junoVolleyClock >= 1 && s.junoVolley > 0 && s.primaryAmmo > 0) {
			s.junoVolleyClock -= 1;
			s.junoVolley--;
			s.primaryAmmo--;
			round(player);
		}
		if (s.junoVolley == 0 || s.primaryAmmo == 0) {
			s.junoVolley = 0;
			s.weaponRecovery = Ow.ticks(VOLLEY_RECOVERY);
			// the volley ends: 5C2D stops, B3DC's Stop event and its ring-out (H)
			JunoSounds.split(player, "mediblaster.volley_end", player.getEyePosition(), 150);
		}
	}

	/** One round along her aim (its spread a cone), to the first ally or enemy on it before a block. */
	private static void round(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(player.getViewVector(1.0f), SPREAD, player.getRandom());
		net.minecraft.world.phys.BlockHitResult blockHit = level.clip(new ClipContext(eye, eye.add(dir.scale(RANGE)), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		LivingEntity best = null;
		Vec3 bestAt = null;
		boolean bestAlly = false;
		double bestDist = eye.distanceToSqr(block);
		for (net.minecraft.world.entity.Entity e : level.getEntities(player, new AABB(eye, block).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive()) {
				continue;
			}
			boolean ally = Allies.isAlly(player, target);
			if (!ally && !Allies.canHurt(player, target)) {
				continue;
			}
			java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(ally ? ROUND_RADIUS_ALLY : ROUND_RADIUS_ENEMY).clip(eye, block);
			if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
				best = target;
				bestAt = hit.get();
				bestAlly = ally;
				bestDist = hit.get().distanceToSqr(eye);
			}
		}
		// the round for her clients (JunoFx): muzzle 51DA, the round 53B6 to where it stopped, the ally hit 58B4 (variant 1;
		// 2 an enemy, whose hit 5841 is empty), the world hit 53B7 on the block's face
		Effects.play(level, "juno_round", eye, (best != null ? bestAt : block).subtract(eye), player, best == null ? 0 : bestAlly ? 1 : 2,
			player);
		if (best == null) {
			if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
				Effects.play(level, "juno_round_block", block, Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i()), null, 0, player);
				// contact set 07DC, type 2 (the world), for everyone (899F 90 m + 89A1, both in the file)
				JunoSounds.at(level, "mediblaster.hit_block", block, 90);
			}
			return;
		}
		double metres = Math.sqrt(bestDist);
		double falloff = metres <= FALLOFF_START ? 1 : metres >= FALLOFF_END ? FALLOFF_MIN
			: 1 - (1 - FALLOFF_MIN) * (metres - FALLOFF_START) / (FALLOFF_END - FALLOFF_START);
		// a character hit: 538E's "on a target" slot 51DA -> 4010 -> 8DE7, the shooter's confirmation (M, Ana's reading)
		JunoSounds.own(player, "mediblaster.hit");
		if (bestAlly) {
			float healed = Combat.heal(best, ROUND_HEAL * falloff);
			if (healed > 0) {
				Combat.credit(player, healed);
				Combat.addFireForHealing(player, healed);
				// "Let me help!" (1727: 4445 #1216 -> #1213, M healing an ally with it; its 20 s cooldown in Voice)
				dev.overcraft.hero.Voice.say(player, "heal", 1);
			}
			return;
		}
		// an enemy: contact set 07DC type 0 (M: characters) -> B38E hers, 9058 at the hit for the rest (500 m)
		JunoSounds.own(player, "mediblaster.hit_enemy");
		JunoSounds.at(level, "mediblaster.hit_enemy_3p", bestAt, 500, player);
		boolean crit = dev.overcraft.hero.Headshot.crossed(best, bestAt, bestAt.subtract(eye));
		float dealt = Combat.damage(best, source(player), ROUND_DAMAGE * falloff * (crit ? ROUND_CRIT : 1), dev.overcraft.hero.Blow.weapon(crit).by(dev.overcraft.hero.Hero.JUNO));
		Combat.credit(player, dealt, best);
		Combat.addFireForDamage(player, dealt, Ow.toOw(best.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(player, best, crit);
		}
	}


	/** Damage from her with no projectile of its own (hitscan), so a kill isn't a melee kill. */
	private static DamageSource source(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), null, owner);
	}
}
