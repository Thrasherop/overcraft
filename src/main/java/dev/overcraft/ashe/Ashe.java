package dev.overcraft.ashe;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.BobEntity;
import dev.overcraft.entity.DynamiteEntity;
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
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.overcraft.ashe.AsheStats.*;

/**
 * Ashe's kit (#137; numbers in {@link AsheStats}, the data map tools/reference/ashe-map.md). Her buttons by her data's ids: the left
 * button The Viper (PRIMARY: from the hip, or scoped while the right is held: SECONDARY, Take Aim), R a shell-at-a-time reload,
 * Shift Coach Gun (ABILITY 1), E Dynamite (ABILITY 2; with Remote Detonator E again sets it off), Q B.O.B. (ULTIMATE, a creature of
 * his own: {@link BobEntity}). Her passive is the sub-role's, Sharpshooter (crits take time off Coach Gun); the Damage role's
 * healing reduction is everyone's (Combat). The server owns all of it; her client only zooms with the synced scope (ClientAna).
 */
public final class Ashe implements HeroKit {
	/** The synced mode's bits: scoped (fully), reloading, a stick out, B.O.B. out, Double-Barreled's window open, casting. */
	public static final int MODE_SCOPED = 1, MODE_RELOAD = 2, MODE_DYNAMITE_OUT = 4, MODE_BOB_OUT = 8, MODE_DOUBLE = 16, MODE_CASTING = 32;

	static final Blow HIP_BLOW = Blow.weapon("the_viper", false).by(Hero.ASHE);
	static final Blow COACH_BLOW = Blow.ability("coach_gun").by(Hero.ASHE);
	public static final Blow DYNAMITE_BLOW = Blow.ability("dynamite").by(Hero.ASHE);
	public static final Blow BOB_BLOW = Blow.ult("b_o_b").by(Hero.ASHE);
	static final Blow STING_BLOW = Blow.perk("vipers_sting").by(Hero.ASHE);

	private static final Identifier ADS_SPEED = Overcraft.id("ashe_scoped");

	/** Damage over time under way (Dynamite's burn, Viper's Sting): on what, by whom, per step, steps left, ticks to the next. */
	private record Dot(LivingEntity target, UUID owner, double step, int[] left, Blow blow) {
	}

	private static final Map<String, Dot> DOTS = new ConcurrentHashMap<>();

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.ashe.clear();
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		speed(player, 0);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		AsheState a = s.ashe;
		long now = player.level().getGameTime();
		a.silenced = dev.overcraft.sombra.Hack.silenced(player);
		double rate = Combat.cooldownRate(player);
		if (a.coachCast < 0) {
			if (a.doubleUntil >= 0) {
				// Double-Barreled's window: the cooldown shows nothing while the real one runs underneath (D 1513 #87 / #545)
				a.heldCooldown = Math.max(0, a.heldCooldown - rate);
				if (now >= a.doubleUntil) {
					// over: what's left, at most the cooldown less the window (D #534)
					a.doubleUntil = -1;
					s.ability1Cooldown = Math.min(a.heldCooldown, Ow.ticks(COACH_COOLDOWN - DOUBLE_WINDOW));
				}
			} else {
				s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
			}
		}
		if (a.dynamiteCast < 0 || a.dynamiteCast >= Ow.ticks(DYNAMITE_COOLDOWN_AT)) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		coachCast(player, s, a, able);
		dynamiteCast(player, s, a, able);
		bobCast(player, s, a, able);
		weapon(player, s, a, able, now);
		if (a.checkTick++ % 20 == 0 && a.bob != null && bob(player, a) == null) {
			a.bob = null;
		}
		if (a.dynamite != null && dynamite(player, a) == null) {
			a.dynamite = null;
		}
		// a Coach Gun jump lands her unhurt (CLAUDE.md, #35), as Junkrat's mine jump
		boolean launched = a.launchGrace > 0;
		if (launched) {
			a.launchGrace = player.onGround() ? a.launchGrace - 1 : 0;
		}
		HeroManager.softLanding(player, s, launched, false);
	}

	// ---- The Viper and Take Aim (14FC) ----

	/** The scope after one more tick toward {@code wanted} (in over 0.2 s, out over 0.1666 s). */
	public static double scopeStep(double scope, boolean wanted) {
		return wanted ? Math.min(1, scope + 1 / (20 * ADS_IN)) : Math.max(0, scope - 1 / (20 * ADS_OUT));
	}

	private static void weapon(ServerPlayer player, HeroState s, AsheState a, boolean able, long now) {
		double attack = Combat.attackRate(player);
		a.hipWait = Math.max(0, a.hipWait - attack);
		boolean moving = a.scope > 0 && a.scope < 1;
		if (!moving) {
			// the scoped shot's timer waits while the scope moves (D #729 / #802)
			a.adsWait = Math.max(0, a.adsWait - attack);
		}
		a.spread = Math.max(0, a.spread - HIP_SPREAD_RECOVERY / 20.0);
		boolean busy = !able || a.coachCast >= 0 || a.bobCast >= 0;
		reload(player, s, a, able, attack);
		// the scope: held on the right button, not while reloading or casting (D #580)
		boolean wanted = able && s.secondaryHeld && a.reloadPhase < 0 && !busy && s.primaryAmmo > 0;
		double before = a.scope;
		a.scope = scopeStep(a.scope, wanted);
		if (before == 0 && a.scope > 0) {
			AsheSounds.split(player, "ads.in", player.getEyePosition(), 20);
			Combat.animate(player, "ads_in");
		} else if (before == 1 && a.scope < 1) {
			AsheSounds.split(player, "ads.out", player.getEyePosition(), 20);
			Combat.animate(player, "ads_out");
		}
		speed(player, a.scope >= 1 ? ADS_MOVE - 1 : 0);
		if (busy || a.reloadPhase >= 0) {
			return;
		}
		if (s.primaryAmmo <= 0) {
			// empty: the reload starts by itself (D #544)
			startReload(player, s, a);
			return;
		}
		if (!s.primaryHeld) {
			return;
		}
		if (a.scope >= 1 && a.adsWait <= 0) {
			s.primaryAmmo--;
			a.adsWait = Ow.ticks(ADS_INTERVAL);
			a.hipWait = Ow.ticks(HIP_INTERVAL);
			AsheSounds.split(player, "viper.fire_ads", player.getEyePosition(), 200);
			Combat.animate(player, "fire_ads");
			boolean hit = shot(player, s, player.getEyePosition(), player.getViewVector(1.0f), ADS_DAMAGE, ADS_FALLOFF_START, ADS_FALLOFF_END,
				true, true);
			if (!hit) {
				// a scoped miss breaks Viper's Sting's chain (D 51E3 #356 -> #358)
				a.stingTarget = null;
			}
		} else if (a.scope <= 0 && a.hipWait <= 0) {
			s.primaryAmmo--;
			a.hipWait = Ow.ticks(HIP_INTERVAL);
			Vec3 dir = dev.overcraft.sombra.Sombra.cone(player.getViewVector(1.0f), HIP_SPREAD * a.spread, player.getRandom());
			a.spread = Math.min(1, a.spread + HIP_SPREAD_PER_SHOT);
			AsheSounds.split(player, "viper.fire", player.getEyePosition(), 200);
			Combat.animate(player, "fire");
			shot(player, s, player.getEyePosition(), dir, HIP_DAMAGE, HIP_FALLOFF_START, HIP_FALLOFF_END, true, false);
		}
	}

	private static void startReload(ServerPlayer player, HeroState s, AsheState a) {
		if (a.reloadPhase >= 0 || s.primaryAmmo >= AMMO || a.coachCast >= 0 || a.bobCast >= 0) {
			return;
		}
		// once the hip shot's wait is within 0.05 s of its end (D #544: v30010 - v30011)
		if (a.hipWait > Ow.ticks(0.05)) {
			a.reloadAsked = true;
			return;
		}
		a.reloadAsked = false;
		a.reloadPhase = 0;
		a.reloadTicks = 0;
		a.reloadCut = false;
		a.scope = Math.min(a.scope, 0);
		Combat.animate(player, "reload_start");
		AsheSounds.split(player, "reload.start", player.getEyePosition(), 30);
	}

	/** Ticks from the reload's start to its {@code n}th shell (1-based): 0.5 s, then 0.25 s each. */
	public static int shellTick(int n) {
		return Ow.ticks(RELOAD_START + (n - 1) * RELOAD_SHELL);
	}

	/** A whole reload from {@code ammo} rounds, its end included (seconds): 3.25 s to the last of 12 from empty, then 0.3 s. */
	public static double reloadSeconds(int ammo) {
		int shells = AMMO - ammo;
		return shells <= 0 ? 0 : RELOAD_START + (shells - 1) * RELOAD_SHELL + RELOAD_END;
	}

	private static void reload(ServerPlayer player, HeroState s, AsheState a, boolean able, double attack) {
		if (a.reloadAsked && a.reloadPhase < 0) {
			startReload(player, s, a);
		}
		if (a.reloadPhase < 0) {
			return;
		}
		if (!able) {
			// a stun or a sleep ends it, the shells in stay (W)
			a.reloadPhase = -1;
			return;
		}
		a.reloadTicks += attack;
		switch (a.reloadPhase) {
			case 0 -> {
				if (a.reloadTicks >= Ow.ticks(RELOAD_START)) {
					// the first shell at the start's end (D #722)
					shell(player, s);
					a.reloadPhase = s.primaryAmmo >= AMMO ? 2 : 1;
					a.reloadTicks = 0;
					if (a.reloadPhase == 2) {
						endPhase(player);
					}
				}
			}
			case 1 -> {
				if ((s.primaryFresh || s.secondaryFresh) && s.primaryAmmo > 0) {
					// a press of either button cuts it into its end (D #695 / #704)
					a.reloadPhase = 2;
					a.reloadTicks = 0;
					endPhase(player);
					return;
				}
				if (a.reloadTicks >= Ow.ticks(RELOAD_SHELL)) {
					shell(player, s);
					a.reloadTicks = 0;
					if (s.primaryAmmo >= AMMO) {
						a.reloadPhase = 2;
						endPhase(player);
					}
				}
			}
			default -> {
				if (a.reloadTicks >= Ow.ticks(RELOAD_END)) {
					a.reloadPhase = -1;
				}
			}
		}
	}

	private static void shell(ServerPlayer player, HeroState s) {
		s.primaryAmmo = Math.min(AMMO, s.primaryAmmo + 1);
		AsheSounds.split(player, "reload.shell", player.getEyePosition(), 20);
		Combat.animate(player, "reload_loop");
	}

	private static void endPhase(ServerPlayer player) {
		Combat.animate(player, "reload_end");
		AsheSounds.split(player, "reload.end", player.getEyePosition(), 20);
	}

	/** A falloff share at {@code metres}: full to {@code start}, x0.3 from {@code end}, linear between (D; M the line). */
	public static double falloff(double metres, double start, double end) {
		if (metres <= start) {
			return 1;
		}
		if (metres >= end) {
			return FALLOFF_MIN;
		}
		return 1 - (1 - FALLOFF_MIN) * (metres - start) / (end - start);
	}

	/** The Viper's damage at {@code metres}, scoped or from the hip, x2 on the head. */
	public static double viperDamage(double metres, boolean scoped, boolean crit) {
		return (scoped ? ADS_DAMAGE * falloff(metres, ADS_FALLOFF_START, ADS_FALLOFF_END) : HIP_DAMAGE * falloff(metres, HIP_FALLOFF_START,
			HIP_FALLOFF_END)) * (crit ? CRIT : 1);
	}

	/**
	 * One hitscan shot of hers from {@code eye} along {@code dir}: her own stick when it's the first thing on the line (it goes off),
	 * else the first creature she can hurt within 0.04 m before a block, its falloff, x2 on the head. Whether it hit someone.
	 */
	private static boolean shot(ServerPlayer player, HeroState s, Vec3 eye, Vec3 dir, double damage, double start, double end, boolean crits,
								boolean scoped) {
		ServerLevel level = player.level();
		Vec3 far = eye.add(dir.scale(RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 block = blockHit.getLocation();
		Hit hit = firstHit(level, player, eye, block, RADIUS);
		DynamiteEntity stick = s.ashe.dynamite == null ? null : dynamite(player, s.ashe);
		if (stick != null) {
			java.util.Optional<Vec3> on = stick.getBoundingBox().inflate(DYNAMITE_SHOOT_BOX / 2).clip(eye, block);
			if (on.isPresent() && (hit == null || on.get().distanceTo(eye) < hit.metres)) {
				// shot: it goes off at once (D 1502: the Viper hits it like a target)
				trail(level, fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), on.get());
				Voice.say(player, "dynamite_shot", 1);
				stick.explode();
				return true;
			}
		}
		if (hit == null) {
			if (blockHit.getType() == HitResult.Type.BLOCK) {
				AsheSounds.at(level, scoped ? "viper.hit_world_ads" : "viper.hit_world", block, 32);
			}
			trail(level, fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), block);
			return false;
		}
		LivingEntity target = hit.target;
		boolean crit = crits && dev.overcraft.hero.Headshot.crossed(target, hit.at, dir, RADIUS);
		double dealtOw = damage * falloff(hit.metres, start, end) * (crit ? CRIT : 1);
		Blow blow = HIP_BLOW.crit(crit);
		float dealt = hit.part != null ? Combat.damagePart(level, hit.part, Combat.hitscan(player), dealtOw, blow)
			: Combat.damage(target, Combat.hitscan(player), dealtOw, blow);
		Combat.credit(player, dealt, target);
		Combat.addFireForDamage(player, dealt, target);
		AsheSounds.split(player, scoped ? "viper.hit_enemy_ads" : "viper.hit_enemy", hit.at, 500);
		if (dealt > 0) {
			Effects.hitMarker(player, target, crit);
			if (Allies.isEnemy(player, target)) {
				s.ashe.lastShot = target.getId();
				s.ashe.lastShotAt = level.getGameTime();
			}
			if (crit) {
				s.critAt = level.getGameTime();
				sharpshooter(s, dealt);
			}
			if (scoped && s.has(Perk.ASHE_VIPERS_STING) && target.isAlive() && Allies.isEnemy(player, target)) {
				sting(player, s.ashe, target, level.getGameTime());
			}
		}
		trail(level, fromEyes(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD), hit.at);
		return true;
	}

	/** A creature on a shot's line: what, where, how far, the dragon's part it crossed. */
	record Hit(LivingEntity target, Vec3 at, double metres, @Nullable EnderDragonPart part) {
	}

	/** The nearest creature {@code shooter} can hurt within {@code radius} of the line {@code from -> to}, or null. */
	static @Nullable Hit firstHit(ServerLevel level, LivingEntity shooter, Vec3 from, Vec3 to, double radius) {
		return firstHit(level, shooter, shooter, from, to, radius);
	}

	/**
	 * The same for a shot from {@code from} that's {@code side}'s (B.O.B.'s are hers): her allies, B.O.B. and she don't stop it (found
	 * reading after N1: by his own rule her wolf and she stood in his line).
	 */
	static @Nullable Hit firstHit(ServerLevel level, Entity source, LivingEntity side, Vec3 from, Vec3 to, double radius) {
		Hit best = null;
		for (Entity e : level.getEntities(source, new AABB(from, to).inflate(1))) {
			LivingEntity target = e instanceof EnderDragonPart part ? part.parentMob : e instanceof LivingEntity living ? living : null;
			if (target == null || !target.isAlive() || target == source || target instanceof BobEntity || !Allies.canHurt(side, target)) {
				continue;
			}
			java.util.Optional<Vec3> on = e.getBoundingBox().inflate(radius).clip(from, to);
			if (on.isPresent()) {
				double d = on.get().distanceTo(from);
				if (best == null || d < best.metres) {
					best = new Hit(target, on.get(), d, e instanceof EnderDragonPart part ? part : null);
				}
			}
		}
		return best;
	}

	/** Sharpshooter (01C7 #2832 -> 1513 #451): crit damage dealt takes 0.0075 s a point off Coach Gun's cooldown. */
	public static double sharpshooterCut(double critDamage) {
		return Math.max(0, critDamage) * SHARPSHOOTER * 20;
	}

	private static void sharpshooter(HeroState s, double critDamage) {
		double cut = sharpshooterCut(critDamage);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - cut);
		s.ashe.heldCooldown = Math.max(0, s.ashe.heldCooldown - cut);
	}

	/** Viper's Sting: two scoped hits in a row on one target within 3 s: 25 more over 1 s (D 51E3 #335 / #344 / #330). */
	private static void sting(ServerPlayer player, AsheState a, LivingEntity target, long now) {
		if (target.getUUID().equals(a.stingTarget) && now - a.stingAt <= Ow.ticks(STING_WINDOW)) {
			dot(player, target, "sting", STING_DAMAGE, STING_TIME, STING_BLOW);
		}
		a.stingTarget = target.getUUID();
		a.stingAt = now;
	}

	static void trail(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		double len = d.length();
		int n = (int) Math.min(16, Math.max(1, len / 1.5));
		for (int k = 1; k <= n; k++) {
			Vec3 p = from.add(d.scale(k / (double) n * Math.min(1, 24 / Math.max(len, 1e-3))));
			level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		level.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 2, 0, 0, 0, 0);
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

	// ---- Coach Gun (1513) ----

	private static void startCoach(ServerPlayer player, HeroState s, AsheState a) {
		boolean second = a.doubleUntil >= 0 && player.level().getGameTime() < a.doubleUntil;
		if (a.coachCast >= 0 || (!second && s.ability1Cooldown > 0) || a.bobCast >= 0 || a.silenced || Sleep.asleep(player)) {
			return;
		}
		a.coachCast = 0;
		a.secondShot = second;
		cutReload(a);
		a.scope = 0;
		Combat.animate(player, "coach_gun");
		Voice.say(player, "coach_gun", 1);
	}

	private static void cutReload(AsheState a) {
		// an ability ends the reload; the shells in stay (W)
		a.reloadPhase = -1;
		a.reloadAsked = false;
	}

	private static void coachCast(ServerPlayer player, HeroState s, AsheState a, boolean able) {
		if (a.coachCast < 0) {
			return;
		}
		a.coachCast++;
		if (a.coachCast == Ow.ticks(COACH_FIRE_AT)) {
			if (!able || a.silenced) {
				a.coachCast = -1;
				return;
			}
			coachBlast(player, s, a);
		}
		if (a.coachCast >= Ow.ticks(COACH_CAST)) {
			a.coachCast = -1;
		}
	}

	/**
	 * The pellets' directions about {@code look}: 1 in the middle, 3 at 0.3, 4 at 0.6, 7 at 1.0 of the spread (D volley #169), each ring
	 * evenly round from straight up (ours: the data's ring angles not read).
	 */
	public static List<Vec3> pellets(Vec3 look, double spreadDegrees) {
		Vec3 f = look.normalize();
		Vec3 side = f.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(f).normalize();
		List<Vec3> out = new ArrayList<>();
		int[] counts = {1, 3, 4, 7};
		double[] rings = {0, 0.3, 0.6, 1.0};
		for (int r = 0; r < counts.length; r++) {
			double angle = Math.toRadians(spreadDegrees * rings[r]);
			for (int k = 0; k < counts[r]; k++) {
				double around = 2 * Math.PI * k / counts[r];
				Vec3 off = side.scale(Math.sin(around)).add(up.scale(Math.cos(around)));
				out.add(f.scale(Math.cos(angle)).add(off.scale(Math.sin(angle))).normalize());
			}
		}
		return out;
	}

	/** Coach Gun's push share on an enemy {@code metres} off: all of it to 8 m, none from 15 m, linear between (D rings; M the line). */
	public static double coachPushShare(double metres) {
		if (metres <= COACH_PUSH_FULL) {
			return 1;
		}
		if (metres >= COACH_PUSH_NONE) {
			return 0;
		}
		return 1 - (metres - COACH_PUSH_FULL) / (COACH_PUSH_NONE - COACH_PUSH_FULL);
	}

	/** Her own launch along {@code look}: 18 m/s back, with 0.1 up on the way (D #141), in m/s. */
	public static Vec3 coachSelf(Vec3 look) {
		return look.normalize().scale(-1).add(0, COACH_SELF_UP, 0).normalize().scale(COACH_SELF);
	}

	private static void coachBlast(ServerPlayer player, HeroState s, AsheState a) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Map<LivingEntity, Double> hitWith = new java.util.LinkedHashMap<>();
		Map<LivingEntity, EnderDragonPart> parts = new java.util.HashMap<>();
		Map<LivingEntity, Double> distance = new java.util.HashMap<>();
		for (Vec3 dir : pellets(look, COACH_SPREAD / 2)) {
			Vec3 far = eye.add(dir.scale(COACH_RANGE));
			BlockHitResult blockHit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Hit hit = firstHit(level, player, eye, blockHit.getLocation(), COACH_PELLET_RADIUS);
			if (hit == null) {
				continue;
			}
			hitWith.merge(hit.target, COACH_PELLET, Double::sum);
			distance.merge(hit.target, hit.metres, Math::min);
			if (hit.part != null) {
				parts.put(hit.target, hit.part);
			}
		}
		boolean enemyPushed = false;
		for (Map.Entry<LivingEntity, Double> en : hitWith.entrySet()) {
			LivingEntity target = en.getKey();
			// the pellets together, one hit (no crit: D m_6256F1D7 1)
			float dealt = parts.containsKey(target) ? Combat.damagePart(level, parts.get(target), Combat.hitscan(player), en.getValue(), COACH_BLOW)
				: Combat.damage(target, Combat.hitscan(player), en.getValue(), COACH_BLOW);
			Combat.credit(player, dealt, target);
			Combat.addFireForDamage(player, dealt, target);
			if (dealt > 0) {
				Effects.hitMarker(player, target, false);
			}
			AsheSounds.split(player, "coach_gun.hit", target.position(), 40);
			double share = coachPushShare(distance.get(target));
			if (target.isAlive() && share > 0) {
				// 20 m/s along her aim (+0.5 up) and 5 up (D #169: m_1376B4DF, the pair 20 / 5)
				Vec3 along = look.add(0, 0.5, 0).normalize();
				Vec3 flat = new Vec3(along.x, 0, along.z);
				flat = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
				Vec3 push = flat.scale(Ow.speed(COACH_PUSH * share)).add(0, Ow.speed(COACH_PUSH_UP * share), 0);
				Combat.push(target, push, player, COACH_BLOW);
				if (Allies.isEnemy(player, target)) {
					enemyPushed = true;
				}
			}
		}
		// her own push, hit or not (W); rooted or hindered she stays (W): Junkrat's root
		if (!dev.overcraft.junkrat.Junkrat.rooted(player) && !dev.overcraft.cassidy.Hinder.hindered(player)) {
			Combat.launch(player, coachSelf(look).scale(1 / 20.0), player, COACH_BLOW);
			s.softLanding = true;
			a.launchGrace = 10;
		}
		AsheSounds.split(player, "coach_gun.fire", eye, 120);
		level.sendParticles(ParticleTypes.CLOUD, eye.x + look.x, eye.y + look.y - 0.2, eye.z + look.z, 8, 0.2, 0.2, 0.2, 0.05);
		level.sendParticles(ParticleTypes.FLAME, eye.x + look.x, eye.y + look.y - 0.2, eye.z + look.z, 6, 0.1, 0.1, 0.1, 0.05);
		long now = level.getGameTime();
		if (a.secondShot) {
			// the second shot of Double-Barreled: the real cooldown from here (D #538)
			a.doubleUntil = -1;
			a.secondShot = false;
			s.ability1Cooldown = a.heldCooldown;
		} else if (enemyPushed && s.has(Perk.ASHE_DOUBLE_BARRELED)) {
			// Double-Barreled: one more within 2 s (D 28B4, #511)
			a.doubleUntil = now + Ow.ticks(DOUBLE_WINDOW);
			a.heldCooldown = Ow.ticks(COACH_COOLDOWN);
			s.ability1Cooldown = 0;
		} else {
			s.ability1Cooldown = Ow.ticks(COACH_COOLDOWN);
		}
	}

	// ---- Dynamite (14FD; the stick 1502) ----

	private static void startDynamite(ServerPlayer player, HeroState s, AsheState a) {
		DynamiteEntity out = a.dynamite == null ? null : dynamite(player, a);
		if (out != null && s.has(Perk.ASHE_REMOTE_DETONATOR)) {
			// Remote Detonator: E again sets it off 0.5 s later (D 51E3 #368)
			out.remote(Ow.ticks(REMOTE_DELAY));
			AsheSounds.at(player.level(), "dynamite.remote", out.position(), 30);
			return;
		}
		if (a.dynamiteCast >= 0 || s.ability2Cooldown > 0 || a.bobCast >= 0 || a.coachCast >= 0 || a.silenced || Sleep.asleep(player)) {
			return;
		}
		a.dynamiteCast = 0;
		cutReload(a);
		Combat.animate(player, "dynamite_throw");
		AsheSounds.split(player, "dynamite.throw", player.getEyePosition(), 40);
		Voice.say(player, "dynamite", 1);
	}

	private static void dynamiteCast(ServerPlayer player, HeroState s, AsheState a, boolean able) {
		if (a.dynamiteCast < 0) {
			return;
		}
		a.dynamiteCast++;
		if (a.dynamiteCast == Ow.ticks(DYNAMITE_THROW_AT)) {
			if (!able || a.silenced) {
				a.dynamiteCast = -1;
				return;
			}
			DynamiteEntity old = a.dynamite == null ? null : dynamite(player, a);
			if (old != null) {
				old.discard();
			}
			DynamiteEntity stick = new DynamiteEntity(player, player.level(), s.has(Perk.ASHE_AIRBURST));
			stick.launch(fromEyes(player, THROW_RIGHT, THROW_UP, THROW_AHEAD), player.getViewVector(1.0f), Ow.speed(DYNAMITE_SPEED));
			player.level().addFreshEntity(stick);
			a.dynamite = stick.getUUID();
			if (s.has(Perk.ASHE_AIRBURST)) {
				// Airburst: 6 rounds back on the throw, up to the magazine (D 51E3 #359 -> 14FC #1938)
				s.primaryAmmo = Math.min(AMMO, s.primaryAmmo + AIRBURST_AMMO);
			}
		}
		if (a.dynamiteCast == Ow.ticks(DYNAMITE_COOLDOWN_AT)) {
			// 12 s from 0.55 s after the press (D 14FD #42 / #87)
			s.ability2Cooldown = Ow.ticks(DYNAMITE_COOLDOWN);
		}
		if (a.dynamiteCast >= Ow.ticks(DYNAMITE_CAST)) {
			a.dynamiteCast = -1;
		}
	}

	private static @Nullable DynamiteEntity dynamite(ServerPlayer player, AsheState a) {
		return player.level().getEntity(a.dynamite) instanceof DynamiteEntity d && d.isAlive() ? d : null;
	}

	/** The blast's damage {@code metres} from it: 50 to 1.5 m, down to 20 at 5 m, none beyond ({@code scale}: Airburst's 1.4). */
	public static double blastDamage(double metres, double scale) {
		double full = BLAST_FULL * scale, radius = BLAST_RADIUS * scale;
		if (metres > radius) {
			return 0;
		}
		if (metres <= full) {
			return BLAST_DAMAGE;
		}
		return BLAST_DAMAGE - (BLAST_DAMAGE - BLAST_MIN) * (metres - full) / (radius - full);
	}

	/**
	 * Her stick goes off at {@code at} (1502 #117): the blast to everyone in reach and sight she can hurt, herself at half (D #178), and
	 * the burn on each: 100 over 5 s, hers 50. No push (D).
	 */
	public static void explode(ServerPlayer owner, Entity stick, Vec3 at, double scale) {
		ServerLevel level = owner.level();
		double radius = BLAST_RADIUS * scale;
		for (dev.overcraft.hero.Barriers.Shape b : dev.overcraft.hero.Barriers.enemyBarriers(level, owner)) {
			Vec3 near = dev.overcraft.hero.Barriers.nearest(b, at);
			if (near.distanceTo(at) <= radius) {
				b.barrierTake(blastDamage(near.distanceTo(at), scale), near);
			}
		}
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, radius, d -> blastDamage(d, scale));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 2))) {
			if (!e.isAlive() || (e != owner && !Allies.areaHits(owner, e))) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > radius || !Combat.inSight(level, at, e, stick) || dev.overcraft.brigitte.Barrier.shields(e, owner, at)) {
				continue;
			}
			boolean self = e == owner;
			double amount = blastDamage(d, scale) * (self ? BLAST_SELF : 1);
			float dealt = Combat.damage(e, Combat.blast(level, stick, owner), amount, DYNAMITE_BLOW);
			if (!self) {
				Combat.credit(owner, dealt, e);
				Combat.addFireForDamage(owner, dealt, e);
				if (dealt > 0) {
					Effects.hitMarker(owner, e, false);
				}
			}
			if (e.isAlive()) {
				dot(owner, e, "burn", BURN_TOTAL * (self ? BLAST_SELF : 1), BURN_TIME, DYNAMITE_BLOW);
				AsheSounds.at(level, "dynamite.ignite", e.position(), 30);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.4, 0.4, 0.4, 0);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, radius / 3, radius / 4, radius / 3, 0.05);
		AsheSounds.split(owner, "dynamite.explode", at, 80);
	}

	/** Damage over time on {@code target} from {@code owner}: {@code total} over {@code seconds} in 0.2 s steps; again restarts it. */
	private static void dot(ServerPlayer owner, LivingEntity target, String kind, double total, double seconds, Blow blow) {
		int steps = burnSteps(seconds);
		DOTS.put(target.getUUID() + "/" + owner.getUUID() + "/" + kind,
			new Dot(target, owner.getUUID(), total / steps, new int[] {steps, Ow.ticks(BURN_STEP)}, blow));
	}

	/** A damage over time's steps: {@code seconds} in steps of 0.2 s (25 for the burn's 5 s). */
	public static int burnSteps(double seconds) {
		return Math.max(1, (int) Math.round(seconds / BURN_STEP));
	}

	/** Every server tick (Overcraft's): the burns' and stings' steps. */
	public static void tickAll(MinecraftServer server) {
		DOTS.values().removeIf(d -> {
			if (!d.target.isAlive() || d.target.isRemoved() || Combat.isPhased(d.target)) {
				// a phase takes it off (W: Protection Suzu, a barrier of one's own)
				return true;
			}
			if (d.target.tickCount % 4 == 0 && d.target.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.FLAME, d.target.getX(), d.target.getY() + d.target.getBbHeight() * 0.6, d.target.getZ(), 1,
					d.target.getBbWidth() / 3, d.target.getBbHeight() / 4, d.target.getBbWidth() / 3, 0.01);
			}
			if (--d.left[1] > 0) {
				return false;
			}
			d.left[1] = Ow.ticks(BURN_STEP);
			ServerPlayer owner = server.getPlayerList().getPlayer(d.owner);
			if (owner != null) {
				float dealt = dev.overcraft.hero.Barriers.pierce(() -> Combat.damage(d.target, Combat.blast(d.target.level(), null, owner), d.step, d.blow));
				if (d.target != owner) {
					Combat.credit(owner, dealt, d.target);
				}
			}
			return --d.left[0] <= 0;
		});
	}

	// ---- B.O.B. (1536) ----

	private static void startBob(ServerPlayer player, HeroState s, AsheState a) {
		if (a.bobCast >= 0 || a.coachCast >= 0 || a.silenced || player.isPassenger() || Sleep.asleep(player)
			|| s.ultCharge < Combat.ultCost(s)) {
			return;
		}
		s.ultCharge = Combat.afterUlt(Hero.ASHE, s.ultCharge);
		a.bobCast = 0;
		cutReload(a);
		a.scope = 0;
		Combat.animate(player, "ult_cast");
		AsheSounds.split(player, "bob.cast", player.position(), 60);
		ultLine(player);
		// he comes at the cast's start, at her feet, facing her way (D 1536 #149 / #115)
		BobEntity old = a.bob == null ? null : bob(player, a);
		if (old != null) {
			old.leave();
		}
		BobEntity bob = new BobEntity(player.level(), player);
		float yaw = player.getYRot();
		bob.snapTo(player.getX(), player.getY(), player.getZ(), yaw, 0);
		bob.setYHeadRot(yaw);
		bob.setYBodyRot(yaw);
		player.level().addFreshEntity(bob);
		a.bob = bob.getUUID();
		s.ultActive = Ow.ticks(BOB_DROP + BOB_CHARGE_TIME + BOB_FIRE_TIME);
	}

	private static void bobCast(ServerPlayer player, HeroState s, AsheState a, boolean able) {
		if (a.bobCast < 0) {
			return;
		}
		a.bobCast++;
		if (a.bobCast >= Ow.ticks(BOB_CAST)) {
			a.bobCast = -1;
		}
	}

	private static @Nullable BobEntity bob(ServerPlayer player, AsheState a) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			if (level.getEntity(a.bob) instanceof BobEntity b && b.isAlive()) {
				return b;
			}
		}
		return null;
	}

	/** B.O.B.'s cast line: hers in her head, the allies' and the enemies' from her (Cassidy's way). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("ashe.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = AsheSounds.of("ult.friendly"), enemy = AsheSounds.of("ult.enemy");
		if (own != null) {
			HeroSounds.toOwner(hero, own);
		}
		for (ServerPlayer listener : hero.level().players()) {
			net.minecraft.sounds.SoundEvent line = Allies.isSelfOrAlly(hero, listener) ? friendly : enemy;
			if (listener != hero && line != null) {
				listener.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
					net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(line), net.minecraft.sounds.SoundSource.PLAYERS,
					hero.getX(), hero.getEyeY(), hero.getZ(), 5, 1.0f, hero.getRandom().nextLong()));
			}
		}
	}

	/** The enemy B.O.B. fires at: one she shot in the last 3 s if he sees it, else the nearest he sees, within 40 m (W; ours the rule). */
	public static @Nullable LivingEntity bobTarget(ServerPlayer owner, BobEntity bob) {
		ServerLevel level = (ServerLevel) bob.level();
		Vec3 gun = bob.position().add(0, BOB_GUN_UP, 0);
		HeroState s = HeroManager.state(owner);
		if (s != null && s.hero == Hero.ASHE && level.getGameTime() - s.ashe.lastShotAt <= Ow.ticks(BOB_PREFER_TIME)
			&& level.getEntity(s.ashe.lastShot) instanceof LivingEntity marked && marked.isAlive() && Allies.lockOn(owner, marked)
			&& marked.distanceTo(bob) <= BOB_RANGE && Combat.inSight(level, gun, marked, bob)) {
			return marked;
		}
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bob.getBoundingBox().inflate(BOB_RANGE))) {
			// an enemy only, never a bystander or an ally (Allies.lockOn, #99)
			if (!e.isAlive() || e == bob || !Allies.lockOn(owner, e)) {
				continue;
			}
			double d = e.distanceTo(bob);
			if (d <= BOB_RANGE && d < bestD && Combat.inSight(level, gun, e, bob)) {
				best = e;
				bestD = d;
			}
		}
		return best;
	}

	/** One of B.O.B.'s cannon shots at {@code target}: 17, 1.45 degrees of spread, hitscan; what it lands on takes him on (ours). */
	public static void bobShot(ServerPlayer owner, BobEntity bob, LivingEntity target) {
		ServerLevel level = (ServerLevel) bob.level();
		Vec3 gun = bob.position().add(0, BOB_GUN_UP, 0);
		Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(gun).normalize();
		Vec3 dir = dev.overcraft.sombra.Sombra.cone(aim, BOB_SPREAD, bob.getRandom());
		Vec3 far = gun.add(dir.scale(BOB_RANGE));
		BlockHitResult blockHit = level.clip(new ClipContext(gun, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bob));
		Hit hit = firstHit(level, bob, owner, gun, blockHit.getLocation(), RADIUS);
		Vec3 end = hit == null ? blockHit.getLocation() : hit.at;
		if (hit != null) {
			float dealt = hit.part != null ? Combat.damagePart(level, hit.part, bob.damageSource(owner), BOB_SHOT, BOB_BLOW)
				: Combat.damage(hit.target, bob.damageSource(owner), BOB_SHOT, BOB_BLOW);
			Combat.credit(owner, dealt, hit.target);
			Combat.addFireForDamage(owner, dealt, hit.target);
			if (dealt > 0) {
				Effects.hitMarker(owner, hit.target, false);
				bob.provoke(hit.target);
			}
			AsheSounds.at(level, "bob.hit_enemy_3p", hit.at, 40);
		}
		trail(level, gun.add(dir.scale(0.8)), end);
	}

	/**
	 * B.O.B.'s uppercut on everyone within 4 m and 90 degrees ahead of him that can be hurt (an area: enemies and bystanders, Allies.areaHits):
	 * 120, knocked up 15 m/s (12 in the air) and 0.5 along, held 1.5 s (0.5 in the air) (D 1537 #404 / #276, 17F6). How many it took.
	 */
	public static int bobUppercut(ServerPlayer owner, BobEntity bob) {
		ServerLevel level = (ServerLevel) bob.level();
		Vec3 ahead = Vec3.directionFromRotation(0, bob.getYRot());
		int n = 0;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, bob.getBoundingBox().inflate(BOB_UPPERCUT_REACH))) {
			if (!e.isAlive() || e == bob || e == owner || !Allies.areaHits(owner, e)) {
				continue;
			}
			Vec3 to = e.position().subtract(bob.position());
			Vec3 flat = new Vec3(to.x, 0, to.z);
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(bob.position().add(0, 1, 0)));
			if (d > BOB_UPPERCUT_REACH || (flat.lengthSqr() > 0.01 && flat.normalize().dot(ahead) < Math.cos(Math.toRadians(BOB_UPPERCUT_CONE / 2)))) {
				continue;
			}
			boolean flying = !e.onGround();
			float dealt = Combat.damage(e, bob.damageSource(owner), BOB_UPPERCUT_DAMAGE, BOB_BLOW);
			Combat.credit(owner, dealt, e);
			Combat.addFireForDamage(owner, dealt, e);
			if (dealt > 0) {
				Effects.hitMarker(owner, e, false);
				bob.provoke(e);
			}
			if (e.isAlive()) {
				Vec3 up = ahead.scale(Ow.speed(BOB_KNOCK_ALONG)).add(0, Ow.speed(flying ? BOB_KNOCK_UP_AIR : BOB_KNOCK_UP), 0);
				// held first, then thrown up: the root takes any upward speed off (Junkrat.root; found playing, A5: no knock-up at all)
				dev.overcraft.junkrat.Junkrat.root(e, flying ? BOB_IMMOBILIZE_AIR : BOB_IMMOBILIZE);
				Combat.launch(e, up, owner, BOB_BLOW);
				AsheSounds.at(level, "bob.immobilize_3p", e.position(), 40);
			}
			n++;
		}
		return n;
	}

	/** Whether one enemy is close enough ahead to start his uppercut: within 2 m and 90 degrees (D #252). */
	public static boolean uppercutStarts(ServerPlayer owner, BobEntity bob) {
		Vec3 ahead = Vec3.directionFromRotation(0, bob.getYRot());
		for (LivingEntity e : bob.level().getEntitiesOfClass(LivingEntity.class, bob.getBoundingBox().inflate(BOB_UPPERCUT_TRIGGER))) {
			if (!e.isAlive() || e == bob || !Allies.isEnemy(owner, e)) {
				continue;
			}
			Vec3 to = e.position().subtract(bob.position());
			Vec3 flat = new Vec3(to.x, 0, to.z);
			if (Math.sqrt(e.getBoundingBox().distanceToSqr(bob.position().add(0, 1, 0))) <= BOB_UPPERCUT_TRIGGER
				&& (flat.lengthSqr() < 0.01 || flat.normalize().dot(ahead) >= Math.cos(Math.toRadians(BOB_UPPERCUT_CONE / 2)))) {
				return true;
			}
		}
		return false;
	}

	/** A modifier on her speed while scoped (ADD_MULTIPLIED_TOTAL, so it multiplies the others), none at 0. */
	private static void speed(ServerPlayer player, double amount) {
		AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (instance == null) {
			return;
		}
		AttributeModifier current = instance.getModifier(ADS_SPEED);
		if (amount == 0) {
			instance.removeModifier(ADS_SPEED);
		} else if (current == null || current.amount() != amount) {
			instance.removeModifier(ADS_SPEED);
			instance.addTransientModifier(new AttributeModifier(ADS_SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		AsheState a = s.ashe;
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			startCoach(player, s, a);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0) {
			startDynamite(player, s, a);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			startBob(player, s, a);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && a.coachCast < 0 && a.bobCast < 0) {
			startReload(player, s, a);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		AsheState a = s.ashe;
		int mode = (a.scope >= 1 ? MODE_SCOPED : 0) | (a.reloadPhase >= 0 ? MODE_RELOAD : 0) | (a.dynamite != null ? MODE_DYNAMITE_OUT : 0)
			| (a.bob != null ? MODE_BOB_OUT : 0) | (a.doubleUntil >= 0 ? MODE_DOUBLE : 0)
			| (a.coachCast >= 0 || a.dynamiteCast >= 0 || a.bobCast >= 0 ? MODE_CASTING : 0);
		float reload = a.reloadPhase < 0 ? -1 : (float) Math.min(1, s.primaryAmmo / (double) AMMO);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, s.ultActive > 0,
			s.primaryAmmo, reload, 0, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed,
			s.canChangeHero, s.onFire, dev.overcraft.hero.Perks.level(s), dev.overcraft.hero.Perks.progress(s),
			dev.overcraft.hero.Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), 0, mode, 0, (float) a.scope);
	}

	/**
	 * Out of play (#100): the scope, the reload and the casts end, her speed goes. B.O.B. and her stick stay through G and a death (what
	 * she put in the world, as every kit's), and go on a switch or leaving (HeroKit.OutOfPlay.forGood).
	 */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		AsheState a = s.ashe;
		a.scope = 0;
		a.reloadPhase = -1;
		a.reloadAsked = false;
		a.coachCast = -1;
		a.dynamiteCast = -1;
		a.bobCast = -1;
		a.doubleUntil = -1;
		speed(player, 0);
		if (why.forGood()) {
			clearWorld(player);
			a.bob = null;
			a.dynamite = null;
		}
	}

	/** Her B.O.B. and sticks, everywhere: gone. */
	public static void clearWorld(ServerPlayer player) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (BobEntity b : level.getEntities(dev.overcraft.registry.OcEntities.BOB, b -> player.getUUID().equals(b.ownerId()))) {
				b.discard();
			}
			for (DynamiteEntity d : level.getEntities(dev.overcraft.registry.OcEntities.DYNAMITE, d -> d.getOwner() == player)) {
				d.discard();
			}
		}
	}
}
