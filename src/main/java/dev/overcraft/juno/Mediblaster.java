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
		if (s.junoVolley == 0 && s.primaryHeld && s.weaponRecovery <= 0 && s.primaryReload < 0 && s.primaryAmmo > 0) {
			// a new volley: its first round at once
			s.junoVolley = VOLLEY;
			s.junoVolleyClock = 1;
			Combat.animate(player, "fire");
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
		}
	}

	/** One round along her aim (its spread a cone), to the first ally or enemy on it before a block. */
	private static void round(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(player.getViewVector(1.0f), SPREAD, player.getRandom());
		Vec3 block = level.clip(new ClipContext(eye, eye.add(dir.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
			.getLocation();
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
		if (best == null) {
			return;
		}
		double metres = Math.sqrt(bestDist);
		double falloff = metres <= FALLOFF_START ? 1 : metres >= FALLOFF_END ? FALLOFF_MIN
			: 1 - (1 - FALLOFF_MIN) * (metres - FALLOFF_START) / (FALLOFF_END - FALLOFF_START);
		if (bestAlly) {
			float healed = Combat.heal(best, ROUND_HEAL * falloff);
			if (healed > 0) {
				Combat.credit(player, healed);
				Combat.addFireForHealing(player, healed);
			}
			return;
		}
		boolean crit = bestAt.y >= best.getEyeY() - HEAD_DEPTH;
		float dealt = Combat.damage(best, source(player), ROUND_DAMAGE * falloff * (crit ? ROUND_CRIT : 1));
		Combat.credit(player, dealt);
		Combat.addFireForDamage(player, dealt, Ow.toOw(best.getMaxHealth()));
		if (dealt > 0) {
			Effects.hitMarker(player, best, crit);
		}
	}

	/** The head: the top of the box, as for the other heroes' shots (ours: 0.25 m under the eyes and up). */
	private static final double HEAD_DEPTH = 0.25;

	/** Damage from her with no projectile of its own (hitscan), so a kill isn't a melee kill. */
	private static DamageSource source(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), null, owner);
	}
}
