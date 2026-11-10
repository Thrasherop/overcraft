package dev.overcraft.dva;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Shields;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.overcraft.dva.DvaStats.*;

/**
 * The Fusion Cannons (050A; dva-plan.md "What's new" 8): the mech's left button, a hitscan shotgun. Every 0.15 s a volley of
 * 11 pellets from her eyes along her aim, in the data's pattern (1 in the middle, 4 at half the 3.375 degree cone, 6 at its
 * edge: D #444; the pattern turned at random each volley, ours), 2 each, falling off from 10 m to x0.3 at 20 m, x2 on the
 * head. A volley's pellets on one creature are <b>summed into one hit</b> (Minecraft's hurt cooldown would drop all but the
 * first) that armor takes as its pellets (Shields.pellets: each 7 smaller, at most halved). No ammo, no reload; -30 % move
 * speed while firing, none while boosting; not while Defense Matrix is up. Stand-in look and sound (hers: step 11).
 */
public final class FusionCannons {
	private FusionCannons() {
	}

	private static final Identifier SLOW = Overcraft.id("dva_fusion_slow");

	/** Each tick in the mech (Dva.tick): the left button held fires a volley whenever the last one's 0.15 s are over. */
	static void tick(ServerPlayer player, HeroState s, boolean canFire, boolean boosting) {
		long now = player.level().getGameTime();
		s.weaponRecovery = Math.max(0, s.weaponRecovery - Combat.attackRate(player));
		boolean firing = canFire && s.primaryHeld;
		if (firing && s.weaponRecovery <= 0) {
			volley(player, s);
			// Precision Fusion: twice the rate (514D #635)
			s.weaponRecovery += Ow.ticks(FUSION_INTERVAL) / (DvaPerks.precision(s) ? PRECISION_RATE : 1);
			s.dvaLastVolley = now;
		}
		// the slow holds while she fires (#376 while v233), not while boosting (gated by v87: 047E #241)
		slow(player, firing && !boosting);
	}

	/** Whether she's firing now (the HUD's and the slow's: a volley within the last interval). */
	static boolean firing(HeroState s, long now) {
		return now - s.dvaLastVolley <= Ow.ticks(FUSION_INTERVAL);
	}

	static void slow(ServerPlayer player, boolean on) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (!on) {
			speed.removeModifier(SLOW);
		} else if (!speed.hasModifier(SLOW)) {
			speed.addTransientModifier(new AttributeModifier(SLOW, -FUSION_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	/** The 11 directions of a volley around {@code dir}: the middle, 4 at half the cone, 6 at its edge, turned by {@code turn}. */
	public static List<Vec3> pattern(Vec3 dir, double degrees, double turn) {
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		List<Vec3> out = new ArrayList<>(FUSION_PELLETS);
		out.add(dir);
		ring(out, dir, side, up, Math.tan(Math.toRadians(degrees * 0.5)), FUSION_INNER, turn);
		ring(out, dir, side, up, Math.tan(Math.toRadians(degrees)), FUSION_OUTER, turn);
		return out;
	}

	private static void ring(List<Vec3> out, Vec3 dir, Vec3 side, Vec3 up, double r, int n, double turn) {
		for (int i = 0; i < n; i++) {
			double a = turn + i * Math.PI * 2 / n;
			out.add(dir.add(side.scale(r * Math.cos(a))).add(up.scale(r * Math.sin(a))).normalize());
		}
	}

	/** A pellet's damage at {@code metres}: 2 to 10 m, down to x0.3 at 20 m, the floor past it. */
	public static double falloff(double metres) {
		return falloff(metres, 1);
	}

	/** The same with its distances x{@code scale} (Precision Fusion's 1.5: 15-30 m, 514D #632). */
	public static double falloff(double metres, double scale) {
		double start = FUSION_FALLOFF_START * scale, end = FUSION_FALLOFF_END * scale;
		if (metres <= start) {
			return 1;
		}
		if (metres >= end) {
			return FUSION_FALLOFF_MIN;
		}
		return 1 - (1 - FUSION_FALLOFF_MIN) * (metres - start) / (end - start);
	}

	/** A creature's pellets this volley: their summed damage, how many, and whether any was on the head. */
	private static final class Hit {
		double damage;
		int pellets;
		boolean crit;
		Vec3 at;
	}

	private static void volley(ServerPlayer player, HeroState s) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		// Precision Fusion: spread -75 % (ramped), damage x0.5, falloff x1.5 (514D #636 / #637 / #632)
		boolean precise = DvaPerks.precision(s);
		double spread = FUSION_SPREAD * (1 - DvaPerks.spreadCut(s, level.getGameTime()));
		double pellet = FUSION_DAMAGE * (precise ? PRECISION_DAMAGE : 1);
		double falloffScale = precise ? PRECISION_FALLOFF : 1;
		List<Vec3> dirs = pattern(look, spread, player.getRandom().nextDouble() * Math.PI * 2);
		// the creatures any pellet could reach: one search a volley, not one a pellet
		double widen = Math.tan(Math.toRadians(FUSION_SPREAD)) * FUSION_RANGE + 2;
		AABB area = new AABB(eye, eye.add(look.scale(FUSION_RANGE))).inflate(widen);
		List<Entity> near = level.getEntities(player, area, e -> e instanceof LivingEntity || e instanceof EnderDragonPart);
		Map<LivingEntity, Hit> hits = new LinkedHashMap<>();
		for (Vec3 dir : dirs) {
			Vec3 end = eye.add(dir.scale(FUSION_RANGE));
			Vec3 block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
			LivingEntity best = null;
			Vec3 bestAt = null;
			double bestDist = eye.distanceToSqr(block);
			for (Entity e : near) {
				LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : (LivingEntity) e;
				if (!target.isAlive() || !Allies.canHurt(player, target)) {
					// allies are passed through (it doesn't heal)
					continue;
				}
				java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(FUSION_RADIUS).clip(eye, block);
				if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
					best = target;
					bestAt = hit.get();
					bestDist = hit.get().distanceToSqr(eye);
				}
			}
			Vec3 stop = best != null ? bestAt : block;
			// stand-in look: a spark where each pellet stops
			level.sendParticles(best != null ? ParticleTypes.CRIT : ParticleTypes.SMOKE, stop.x, stop.y, stop.z, 1, 0, 0, 0, 0);
			if (best == null) {
				continue;
			}
			// the head: the top of the box, as Sombra's pistol, Kiriko's kunai and Ana's darts (ours: 0.25 m under the eyes and up)
			boolean crit = bestAt.y >= best.getEyeY() - 0.25;
			Hit h = hits.computeIfAbsent(best, k -> new Hit());
			h.damage += pellet * falloff(Math.sqrt(bestDist), falloffScale) * (crit ? FUSION_CRIT : 1);
			h.pellets++;
			h.crit |= crit;
			h.at = bestAt;
		}
		// stand-in sound, from both cannons
		level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.35f, 1.9f);
		for (Map.Entry<LivingEntity, Hit> e : hits.entrySet()) {
			LivingEntity target = e.getKey();
			Hit h = e.getValue();
			float dealt;
			Shields.pellets = h.pellets;
			try {
				dealt = Combat.damage(target, shotSource(player), h.damage, dev.overcraft.hero.Blow.weapon(h.crit).by(dev.overcraft.hero.Hero.DVA));
			} finally {
				Shields.pellets = 1;
			}
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, target);
			if (dealt > 0) {
				if (h.crit) {
					s.critAt = level.getGameTime();
				}
				Effects.hitMarker(player, target, h.crit);
			}
		}
	}

	/** Her shots' damage: an arrow's type (a shot, not a melee blow), from her and by her, so an enemy barrier takes it. */
	static DamageSource shotSource(ServerPlayer owner) {
		return new DamageSource(owner.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
			.getOrThrow(DamageTypes.ARROW), owner, owner);
	}
}
