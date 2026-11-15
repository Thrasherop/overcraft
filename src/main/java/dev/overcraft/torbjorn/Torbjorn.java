package dev.overcraft.torbjorn;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.RivetEntity;
import dev.overcraft.entity.SlagEntity;
import dev.overcraft.entity.TurretEntity;
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
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.torbjorn.TorbjornStats.*;

/**
 * Torbjörn's kit (#136; numbers in {@link TorbjornStats}, the data map tools/reference/torbjorn-map.md). His buttons by his data's ids:
 * the left button the Rivet Gun (the right its alt), or with the Forge Hammer out (the 1 / 2 keys or the wheel) a swing; Shift Deploy
 * Turret (one out: a new throw replaces it), E Overload, Q Molten Core (either fire button throws the slag). The server owns it all:
 * his rivets, slag and turret are its entities, his pools its list ({@link #tickPools}).
 */
public final class Torbjorn implements HeroKit {
	/** The synced mode's bits (the HUD). */
	public static final int MODE_TURRET_OUT = 1, MODE_OVERLOAD = 2, MODE_HAMMER = 4, MODE_MOLTEN = 8, MODE_TURRET_BUILDING = 16,
		MODE_SILENCED = 32;
	/** Every client's look of him (HeroManager.TORBJORN_LOOK): the hammer out, Molten Core, Overload. */
	public static final int LOOK_HAMMER = 1, LOOK_MOLTEN = 2, LOOK_OVERLOAD = 4;

	static Blow rivetBlow(boolean crit) {
		return Blow.weapon(crit).by(Hero.TORBJORN);
	}

	static final Blow HAMMER_BLOW = Blow.melee("forge_hammer").by(Hero.TORBJORN);
	static final Blow TURRET_BLOW = Blow.ability("deploy_turret").by(Hero.TORBJORN);
	static final Blow SLAG_BLOW = Blow.ult("molten_core").by(Hero.TORBJORN);
	private static final Identifier OVERLOAD_SPEED_ID = Overcraft.id("overload_speed");
	private static final Identifier HAMMER_TIME_ID = Overcraft.id("hammer_time");

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		// his turret outlives a death (HeroKit.leavePlay): the respawn's fresh kit keeps it his
		UUID turret = s.torbjorn.turret;
		var level = s.torbjorn.level;
		s.torbjorn.clear();
		s.torbjorn.turret = turret;
		s.torbjorn.level = level;
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.ability1Cooldown = 0;
		s.ability2Cooldown = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		TorbjornState t = s.torbjorn;
		long now = player.level().getGameTime();
		if (t.level != null && t.level != player.level().dimension()) {
			clearWorld(player, s, t);
		}
		t.level = player.level().dimension();
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		if (t.overload <= 0) {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		}
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		if (t.swapTicks > 0) {
			t.swapTicks--;
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		tickOverload(player, s, t, now);
		turretCast(player, s, t, able);
		TurretEntity turret = turret(player, t);
		if (turret != null) {
			turret.overloaded(t.overload > 0 && s.has(Perk.TORBJORN_OVERLOADED_TURRET));
		}
		boolean molten = ult(player, s, t, able, now);
		if (!molten) {
			if (t.hammer) {
				hammer(player, s, t, able);
			} else {
				weapon(player, s, t, able, now);
			}
		}
		// Hammer Time: +20 % with the hammer out, not in Molten Core (D 51F6 #422)
		// the Rivet Gun reloads itself with the hammer out 2 s (W: "Automatically reloaded if Torbjörn has his Forge Hammer equipped
		// for 2 seconds"; M: D 1683 not read for it)
		t.hammerTicks = t.hammer ? t.hammerTicks + 1 : 0;
		if (t.hammerTicks >= Ow.ticks(HAMMER_RELOAD) && s.primaryAmmo < AMMO) {
			s.primaryAmmo = AMMO;
			s.primaryReload = -1;
		}
		if (t.hammer && !molten && s.has(Perk.TORBJORN_HAMMER_TIME)) {
			Combat.buff(player, Attributes.MOVEMENT_SPEED, HAMMER_TIME_ID, HAMMER_TIME_SPEED, 0.15);
		}
		t.leftWas = s.primaryHeld;
		int look = (t.hammer ? LOOK_HAMMER : 0) | (molten ? LOOK_MOLTEN : 0) | (t.overload > 0 ? LOOK_OVERLOAD : 0);
		Integer was = player.getAttached(HeroManager.TORBJORN_LOOK);
		if (was == null || was != look) {
			player.setAttached(HeroManager.TORBJORN_LOOK, look);
		}
	}

	// ---- Rivet Gun (1683) ----

	/** How long his reload runs (ticks of his attack rate): 2.0 s, or 2.0 / 1.5 with Specialist's bonus. */
	public static double reloadTicks(boolean fast) {
		return RELOAD * 20 / (fast ? 1 + SPECIALIST_SPEED : 1);
	}

	private static void weapon(ServerPlayer player, HeroState s, TorbjornState t, boolean able, long now) {
		// his attack rate: +43 % in Overload, on the shots and the reload alike (D #96 v21541, 38D0 #10: 2.0 -> 1.4 s)
		double rate = Combat.attackRate(player);
		s.weaponRecovery = Math.max(0, s.weaponRecovery - rate);
		if (s.primaryReload >= 0) {
			s.primaryReload += rate;
			double length = reloadTicks(t.reloadFast);
			if (s.primaryReload >= length * RELOAD_AMMO_SHARE && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= length) {
				s.primaryReload = -1;
				t.reloadFast = false;
			}
		}
		if (!able || t.swapTicks > 0 || t.turretCast >= 0 || t.overloadCast > 0) {
			return;
		}
		if (s.primaryReload < 0 && s.primaryAmmo <= 0 && s.weaponRecovery <= 0 && s.reloadHeld <= 0) {
			startReload(player, s, t, now);
			return;
		}
		if (s.primaryReload >= 0 || s.weaponRecovery > 0) {
			return;
		}
		if (s.primaryHeld && s.primaryAmmo > 0) {
			s.primaryAmmo--;
			s.weaponRecovery += RIVET_INTERVAL * 20;
			Vec3 from = muzzle(player);
			RivetEntity rivet = new RivetEntity(player, player.level(), false);
			rivet.launch(from, aimPoint(player, 200).subtract(from), Ow.speed(RIVET_SPEED));
			player.level().addFreshEntity(rivet);
			Combat.animate(player, "fire");
			TorbjornSounds.split(player, "rivet.fire", from, 80);
		} else if (s.secondaryHeld && s.primaryAmmo >= ALT_AMMO) {
			s.primaryAmmo -= ALT_AMMO;
			s.weaponRecovery += ALT_INTERVAL * 20;
			Vec3 from = muzzle(player);
			Vec3 aim = aimPoint(player, 100).subtract(from).normalize();
			for (Vec3 dir : pelletDirections(aim)) {
				RivetEntity pellet = new RivetEntity(player, player.level(), true);
				pellet.launch(from, dir, Ow.speed(PELLET_SPEED));
				player.level().addFreshEntity(pellet);
			}
			Combat.animate(player, "fire");
			TorbjornSounds.split(player, "rivet.alt_fire", from, 80);
		}
	}

	/**
	 * The alt's ten along {@code aim}: one at the centre, four at half the spread, five at the edge, each ring turned evenly
	 * (D #178's ring pattern; ours the rings' turn).
	 */
	public static List<Vec3> pelletDirections(Vec3 aim) {
		List<Vec3> out = new ArrayList<>();
		Vec3 side = aim.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(aim).normalize();
		out.add(aim);
		double edge = Math.tan(Math.toRadians(PELLET_SPREAD));
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 2 * i + Math.PI / 4;
			out.add(aim.add(side.scale(Math.cos(a) * edge / 2)).add(up.scale(Math.sin(a) * edge / 2)).normalize());
		}
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * 2 / 5 * i;
			out.add(aim.add(side.scale(Math.cos(a) * edge)).add(up.scale(Math.sin(a) * edge)).normalize());
		}
		return out;
	}

	/** A pellet's share at {@code d} m: 1 to 10 m, down to 0.3 at 20 m and past (D #178) linear (M). */
	public static double pelletFalloff(double d) {
		if (d <= PELLET_FALL_START) {
			return 1;
		}
		if (d >= PELLET_FALL_END) {
			return PELLET_FALL_MIN;
		}
		return 1 - (d - PELLET_FALL_START) / (PELLET_FALL_END - PELLET_FALL_START) * (1 - PELLET_FALL_MIN);
	}

	private static void startReload(ServerPlayer player, HeroState s, TorbjornState t, long now) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO || t.hammer) {
			return;
		}
		s.primaryReload = 0;
		t.reloadFast = now < t.specialistUntil;
		if (t.reloadFast) {
			t.specialistUntil = -1000;
		}
		Combat.animate(player, "reload");
		TorbjornSounds.split(player, "reload", player.getEyePosition(), 30);
	}

	/**
	 * A rivet or pellet of his hit {@code hit}: 70 (12.5 a pellet, with its falloff), x2 on the head; a rivet on an enemy makes it
	 * his turret's target (W).
	 */
	public static void rivetHit(ServerPlayer owner, RivetEntity shot, Entity hit, Vec3 at) {
		ServerLevel level = owner.level();
		EnderDragonPart part = hit instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : hit instanceof LivingEntity living ? living : null;
		DamageSource source = level.damageSources().thrown(shot, owner);
		double base = shot.pellet() ? PELLET_DAMAGE * pelletFalloff(shot.travelled()) : RIVET_DAMAGE;
		if (target == null) {
			// not a creature (an end crystal, a boat): hit as an arrow does
			hit.hurtServer(level, source, Ow.hp(base));
			return;
		}
		if (!Allies.canHurt(owner, target)) {
			return;
		}
		boolean crit = part != null ? part == part.parentMob.head : dev.overcraft.hero.Headshot.crossed(target, at, shot.getDeltaMovement(), shot.hitRadius());
		double damage = base * (crit ? CRIT : 1);
		HeroState s = HeroManager.state(owner);
		if (crit && s != null) {
			// known before the damage: a killing blow's kill feed and voice line look at it (KunaiEntity's way)
			s.critAt = level.getGameTime();
		}
		float dealt = part != null ? Combat.damagePart(level, part, source, damage, rivetBlow(crit)) : Combat.damage(target, source, damage, rivetBlow(crit));
		dealt(owner, dealt, target, crit);
		if (dealt > 0) {
			TorbjornSounds.split(owner, shot.pellet() ? "rivet.alt_hit_enemy" : "rivet.hit_enemy", at, 80);
		}
		if (!shot.pellet() && s != null && Allies.lockOn(owner, target)) {
			TurretEntity turret = turret(owner, s.torbjorn);
			if (turret != null) {
				turret.focus(target);
			}
		}
	}

	// ---- Forge Hammer (168C) ----

	/** The 1 / 2 keys or the wheel (HeroInputPayload.EXTRA_STAFF / BLASTER / SWAP: the weapon keys every hero with two shares). */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		TorbjornState t = s.torbjorn;
		if (!HeroManager.controls(player) || t.ultCast >= 0 || t.ultLeft > 0) {
			return;
		}
		boolean want = t.hammer;
		if ((extra & HeroInputPayload.EXTRA_STAFF) != 0) {
			want = false;
		} else if ((extra & HeroInputPayload.EXTRA_BLASTER) != 0) {
			want = true;
		} else if ((extra & HeroInputPayload.EXTRA_SWAP) != 0) {
			want = !t.hammer;
		}
		if (want != t.hammer) {
			// (a swing under way is cut: holding the left button with the hammer out blocked the swap back to the gun, the critic of
			// 385d464; W gives the swap no wait on a swing, ours)
			swap(player, s, t, want);
		}
	}

	private static void swap(ServerPlayer player, HeroState s, TorbjornState t, boolean hammer) {
		t.hammer = hammer;
		t.swapTicks = Ow.ticks(SWAP);
		t.swing = -1;
		s.interruptReload(SWAP);
		Combat.animate(player, hammer ? "hammer_draw" : "draw");
		TorbjornSounds.split(player, hammer ? "draw_hammer" : "draw_rivet", player.getEyePosition(), 20);
	}

	private static void hammer(ServerPlayer player, HeroState s, TorbjornState t, boolean able) {
		double rate = Combat.attackRate(player);
		if (t.swing >= 0) {
			// (in ticks of his attack rate: +43 % in Overload, D 168C #149 / #215)
			t.swingAt += rate;
			if (!t.swingHit && t.swingAt >= HAMMER_HIT_AT * 20) {
				t.swingHit = true;
				hammerHit(player, s, t);
			}
			if (t.swingAt < HAMMER_INTERVAL * 20) {
				return;
			}
			// its 0.6 s are up: the next swing may start this same tick (held, one every 0.6 s, not every 0.65: T3b)
			t.swing = -1;
		}
		if (able && s.primaryHeld && t.swapTicks <= 0 && t.turretCast < 0 && t.overloadCast <= 0) {
			startSwing(player, s, t);
		}
	}

	/** A swing (the left button with the hammer out, or his quick melee then: W). */
	public static void startSwing(ServerPlayer player, HeroState s, TorbjornState t) {
		if (t.swing >= 0) {
			return;
		}
		t.swing = 0;
		t.swingAt = 0;
		t.swingHit = false;
		Combat.animate(player, "hammer_swing");
		TorbjornSounds.split(player, "hammer.swing", player.getEyePosition(), 30);
	}

	/**
	 * Its hit: 70 to every enemy (and bystander) whose box is within 1 m of the point 1.5 m ahead of him and 1.2 m up, in sight; his
	 * own built, hurt turret there takes 50 back (D 168C #256 / #237).
	 */
	private static void hammerHit(ServerPlayer player, HeroState s, TorbjornState t) {
		ServerLevel level = player.level();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 centre = player.position().add(0, HAMMER_UP, 0).add(look.scale(HAMMER_AHEAD));
		Vec3 eye = player.getEyePosition();
		boolean hit = false;
		TurretEntity turret = turret(player, t);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(HAMMER_RADIUS + 1))) {
			if (Math.sqrt(e.getBoundingBox().distanceToSqr(centre)) > HAMMER_RADIUS) {
				continue;
			}
			if (e == turret) {
				if (turret.built() && turret.getHealth() < turret.getMaxHealth()) {
					turret.heal(Ow.hp(HAMMER_REPAIR));
					TorbjornSounds.split(player, turret.getHealth() >= turret.getMaxHealth() ? "hammer.repair_full" : "hammer.repair",
						turret.position(), 30);
					Voice.say(player, "turret_repair", 0.3f);
				}
				continue;
			}
			if (!Allies.canHurt(player, e) || !Combat.inSight(level, eye, e, player)) {
				continue;
			}
			dev.overcraft.brigitte.Barrier.meleeDepth++;
			float dealt;
			try {
				dealt = Combat.damage(e, player.damageSources().playerAttack(player), HAMMER_DAMAGE, HAMMER_BLOW);
			} finally {
				dev.overcraft.brigitte.Barrier.meleeDepth--;
			}
			dealt(player, dealt, e, false);
			hit |= dealt > 0;
		}
		if (hit) {
			TorbjornSounds.split(player, "hammer.hit", centre, 30);
		} else if (level.clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			TorbjornSounds.all(level, "hammer.hit_world", centre, 30);
		}
	}

	// ---- Deploy Turret (16C5) ----

	private static void startTurret(ServerPlayer player, HeroState s, TorbjornState t) {
		t.turretCast = 0;
		t.thrown = false;
		t.swing = -1;
		s.interruptReload(TURRET_CAST);
		Combat.animate(player, "turret_throw");
		TorbjornSounds.split(player, "turret.throw", player.getEyePosition(), 40);
	}

	private static void turretCast(ServerPlayer player, HeroState s, TorbjornState t, boolean able) {
		if (t.turretCast < 0) {
			return;
		}
		t.turretCast++;
		if (!t.thrown && t.turretCast >= Ow.ticks(TURRET_THROW_AT)) {
			t.thrown = true;
			if (!able) {
				t.turretCast = -1;
				return;
			}
			throwTurret(player, s, t);
		}
		if (t.turretCast >= Ow.ticks(TURRET_CAST)) {
			t.turretCast = -1;
		}
	}

	/**
	 * Whether Shift may throw now: no turret out, or one not in combat (D 16C5 #495's condition: with v10467 "in combat" set, only while
	 * fewer than the one turret are out; W "cannot be redeployed while it is in combat"). In combat is the data's (#509): it fired in the
	 * last 1 s or was hit in the last 3 s (W says dealt or took damage in 3 s: D kept).
	 */
	public static boolean canRedeploy(@Nullable TurretEntity out) {
		return out == null || !out.inCombat();
	}

	/** It leaves his hand at 17 m/s (x1.25 with Anchor Bolts); one out: the old one goes (D #615 / #614), and the cooldown's 6 s start. */
	private static void throwTurret(ServerPlayer player, HeroState s, TorbjornState t) {
		TurretEntity old = turret(player, t);
		t.turret = null;
		if (old != null) {
			// replaced: it goes as a destroyed one does (D 1688 #423 -> 006D -> 16C5 #531)
			turretGone(old, player, false);
			old.discard();
		}
		boolean anchor = s.has(Perk.TORBJORN_ANCHOR_BOLTS);
		Vec3 from = offset(player, 0.3, -0.3, 0.6);
		TurretEntity turret = new TurretEntity(player, player.level(), anchor);
		Vec3 dir = aimPoint(player, 60).subtract(from).normalize();
		turret.launch(from, dir.scale(Ow.speed(TURRET_THROW_SPEED * (anchor ? ANCHOR_THROW : 1))));
		player.level().addFreshEntity(turret);
		t.turret = turret.getUUID();
		// 6 s from the cast (D #466: 12 / 2); the turret's going sets it again (turretGone)
		s.ability1Cooldown = Math.max(s.ability1Cooldown, Ow.ticks(TURRET_COOLDOWN_CALM));
		t.turretCooldownTotal = TURRET_COOLDOWN_CALM;
	}

	/**
	 * His turret is gone (broken, replaced, recalled): its cooldown 12 s if it was in combat, else 6 s (D 16C5 #531), and his line
	 * when an enemy broke it.
	 */
	public static void turretGone(TurretEntity turret, @Nullable ServerPlayer owner, boolean broken) {
		if (turret.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.EXPLOSION, turret.getX(), turret.getY() + 0.5, turret.getZ(), 1, 0, 0, 0, 0);
			TorbjornSounds.all(level, "turret.destroyed", turret.position(), 60);
		}
		if (owner == null) {
			return;
		}
		HeroState s = HeroManager.state(owner);
		if (s == null || s.hero != Hero.TORBJORN || !turret.getUUID().equals(s.torbjorn.turret) && broken) {
			return;
		}
		if (turret.getUUID().equals(s.torbjorn.turret)) {
			s.torbjorn.turret = null;
		}
		double cooldown = Ow.ticks(turret.inCombat() ? TURRET_COOLDOWN : TURRET_COOLDOWN_CALM);
		if (cooldown >= s.ability1Cooldown) {
			s.ability1Cooldown = cooldown;
			s.torbjorn.turretCooldownTotal = cooldown / 20;
		}
		if (broken) {
			Voice.say(owner, "turret_destroyed", 1);
		}
	}

	/** A turret shot of his landed on {@code target}: its damage, credited to him; a mob it hits fights the turret (ours). */
	public static void turretHit(ServerPlayer owner, TurretEntity turret, LivingEntity target, double damage, boolean missile) {
		float dealt = Combat.damage(target, Combat.hitscan(owner), damage, TURRET_BLOW);
		dealt(owner, dealt, target, false);
		if (target instanceof Mob mob && !(target instanceof Player) && mob.isAlive()) {
			mob.setLastHurtByMob(turret);
			mob.setTarget(turret);
		}
		if (!target.isAlive()) {
			Voice.say(owner, "turret_kill", 0.5f);
		}
	}

	/** Whether {@code turret} is still the one {@code owner} has out: not once he's another hero or has thrown a newer one. */
	public static boolean stillHis(TurretEntity turret, ServerPlayer owner) {
		HeroState s = HeroManager.state(owner);
		return s != null && s.hero == Hero.TORBJORN && turret.getUUID().equals(s.torbjorn.turret);
	}

	public static @Nullable TurretEntity turret(ServerPlayer player, TorbjornState t) {
		return t.turret != null && player.level().getEntity(t.turret) instanceof TurretEntity turret && turret.isAlive() ? turret : null;
	}

	// ---- Overload (1721) ----

	private static void startOverload(ServerPlayer player, HeroState s, TorbjornState t, boolean preHeated) {
		long now = player.level().getGameTime();
		t.overload = Ow.ticks(OVERLOAD_TIME);
		t.preHeated = preHeated;
		if (!preHeated) {
			t.overloadCast = Ow.ticks(OVERLOAD_CAST);
			s.interruptReload(OVERLOAD_CAST);
			Combat.animate(player, "overload");
		}
		// from the press: 100 overhealth held whole, +30 % move, +43 % attack speed (D #88 -> #71 / #79 / #171; 38D0 #194), +6 rivets
		Combat.overhealthPool(player, OVERLOAD_OVERHEALTH, OVERLOAD_OVERHEALTH, OVERLOAD_TIME, 10000);
		Combat.buff(player, Attributes.MOVEMENT_SPEED, OVERLOAD_SPEED_ID, OVERLOAD_SPEED, OVERLOAD_TIME);
		s.attackBuff = OVERLOAD_ATTACK;
		s.attackBuffUntil = now + Ow.ticks(OVERLOAD_TIME);
		s.primaryAmmo = Math.min(AMMO, s.primaryAmmo + OVERLOAD_AMMO);
		TorbjornSounds.split(player, "overload", player.getEyePosition(), 40);
		Voice.say(player, "overload", 1);
	}

	private static void tickOverload(ServerPlayer player, HeroState s, TorbjornState t, long now) {
		if (t.overloadCast > 0) {
			t.overloadCast--;
		}
		if (t.overload <= 0) {
			return;
		}
		if (--t.overload == 0) {
			endOverload(player, s, t);
		} else if (t.overload % 20 == 0) {
			TorbjornSounds.all(player.level(), "overload.loop", player.position(), 20);
		}
	}

	private static void endOverload(ServerPlayer player, HeroState s, TorbjornState t) {
		t.overload = 0;
		t.overloadCast = 0;
		Combat.buff(player, Attributes.MOVEMENT_SPEED, OVERLOAD_SPEED_ID, 0, 0);
		// its overhealth goes with it (D #79: the pool is removed at its end; found playing T7a: G left the 100 on him)
		Combat.endOverhealth(player);
		if (s.attackBuff == OVERLOAD_ATTACK) {
			s.attackBuff = 0;
			s.attackBuffUntil = 0;
		}
		// the 10 s start at its end; not after Pre-Heated's (D 1721 #243 / #297)
		if (!t.preHeated) {
			s.ability2Cooldown = Ow.ticks(OVERLOAD_COOLDOWN);
		}
		t.preHeated = false;
		TorbjornSounds.all(player.level(), "overload.end", player.position(), 30);
	}

	// ---- Molten Core (16DA / 16DD / 16DE) ----

	private static void castUlt(ServerPlayer player, HeroState s, TorbjornState t) {
		s.ultCharge = Combat.afterUlt(Hero.TORBJORN, s.ultCharge);
		if (t.hammer) {
			t.hammer = false;
			t.swapTicks = 0;
		}
		t.swing = -1;
		t.ultCast = 0;
		// (no charge while it's cast; after, his turret and rivets charge him again, while the ult's own damage never does: ultDealt)
		s.ultActive = Ow.ticks(ULT_CAST);
		// the Rivet Gun full at the cast (W, M)
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		Combat.animate(player, "ult_cast");
		TorbjornSounds.split(player, "ult.cast", player.getEyePosition(), 60);
		ultLine(player);
		if (s.has(Perk.TORBJORN_PRE_HEATED) && t.overload <= 0) {
			// Pre-Heated: Overload with it, no cooldown after (D 1721 #284-#297)
			startOverload(player, s, t, true);
		}
	}

	/** Molten Core's line: his own copy in his head, the allies' and the enemies' from him (Junkrat's way). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("torbjorn.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = TorbjornSounds.of("ult.friendly"), enemy = TorbjornSounds.of("ult.enemy");
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

	/** Molten Core's cast and fire; whether it's on (his weapons are its). */
	private static boolean ult(ServerPlayer player, HeroState s, TorbjornState t, boolean able, long now) {
		if (t.ultCast >= 0) {
			if (++t.ultCast >= Ow.ticks(ULT_CAST)) {
				t.ultCast = -1;
				t.slag = SLAG_SHOTS;
				t.ultLeft = Ow.ticks(ULT_TIME);
				t.slagRecovery = 0;
			}
			return true;
		}
		if (t.ultLeft <= 0) {
			return false;
		}
		t.ultLeft--;
		t.slagRecovery = Math.max(0, t.slagRecovery - 1);
		if (able && (s.primaryHeld || s.secondaryHeld) && t.slagRecovery <= 0 && t.slag > 0) {
			t.slag--;
			t.slagRecovery += 20 / SLAG_RATE;
			Vec3 from = muzzle(player);
			SlagEntity slag = new SlagEntity(player, player.level());
			slag.launch(from, aimPoint(player, 80).subtract(from), Ow.speed(SLAG_SPEED));
			player.level().addFreshEntity(slag);
			Combat.animate(player, "fire");
			TorbjornSounds.split(player, "molten.fire", from, 60);
		}
		if (t.slag <= 0 || t.ultLeft <= 0) {
			endUlt(player, s, t);
		}
		return true;
	}

	private static void endUlt(ServerPlayer player, HeroState s, TorbjornState t) {
		boolean was = t.ultLeft > 0 || t.slag > 0 || t.ultCast >= 0;
		t.ultCast = -1;
		t.ultLeft = 0;
		t.slag = 0;
		s.ultActive = 0;
		if (was) {
			Combat.animate(player, "ult_end");
		}
	}

	/** A slag ball hit {@code hit}: 25 to an enemy, and its pool on the ground under the hit (W: pools where they land). */
	public static void slagHit(ServerPlayer owner, SlagEntity slag, Entity hit) {
		ServerLevel level = owner.level();
		LivingEntity target = hit instanceof EnderDragonPart p ? p.parentMob : hit instanceof LivingEntity living ? living : null;
		if (target != null && Allies.canHurt(owner, target)) {
			float dealt = Combat.damage(target, level.damageSources().thrown(slag, owner), SLAG_DAMAGE, SLAG_BLOW);
			ultDealt(owner, dealt, target);
			TorbjornSounds.split(owner, "molten.hit", slag.position(), 60);
		}
		Vec3 at = slag.position();
		var down = level.clip(new ClipContext(at, at.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, slag));
		if (down.getType() != HitResult.Type.MISS) {
			pool(owner, level, down.getLocation());
		}
	}

	public static void slagLanded(ServerPlayer owner, SlagEntity slag, Vec3 at) {
		pool(owner, owner.level(), at);
	}

	/** A pool of slag: formed 0.5 s after it lands, then 10 s (D 16DD #44 / #277). */
	private record Pool(ServerLevel level, Vec3 at, UUID owner, long formedAt, long endsAt) {
	}

	private static final List<Pool> POOLS = new ArrayList<>();
	/** Who burned last and when (game time): pools don't add up (W "do not stack"). */
	private static final Map<UUID, Long> BURNED = new HashMap<>();
	/** The burn's step: 0.2 s (W: 36.48 every 0.192 s; ours the tick-sized step). */
	private static final int BURN_EVERY = 4;

	private static void pool(ServerPlayer owner, ServerLevel level, Vec3 at) {
		long now = level.getGameTime();
		POOLS.add(new Pool(level, at, owner.getUUID(), now + Ow.ticks(POOL_FORM), now + Ow.ticks(POOL_FORM + POOL_TIME)));
		TorbjornSounds.all(level, "molten.impact", at, 50);
		TorbjornSounds.all(level, "molten.pool", at, 40);
	}

	/** Every server tick: the pools burn the enemies in them (190 a second, +100 on a hero's armor) and are drawn. */
	public static void tickPools(MinecraftServer server) {
		if (POOLS.isEmpty()) {
			return;
		}
		POOLS.removeIf(p -> p.level.getGameTime() >= p.endsAt);
		BURNED.values().removeIf(at -> server.overworld().getGameTime() - at > 200);
		for (Pool p : new ArrayList<>(POOLS)) {
			long now = p.level.getGameTime();
			if (now % 4 == 0) {
				p.level.sendParticles(now < p.formedAt ? ParticleTypes.SMOKE : ParticleTypes.LAVA, p.at.x, p.at.y + 0.05, p.at.z,
					now < p.formedAt ? 2 : 3, POOL_RADIUS * 0.5, 0.02, POOL_RADIUS * 0.5, 0);
				p.level.sendParticles(ParticleTypes.FLAME, p.at.x, p.at.y + 0.1, p.at.z, 4, POOL_RADIUS * 0.5, 0.05, POOL_RADIUS * 0.5, 0.01);
			}
			if (now < p.formedAt || now % BURN_EVERY != 0) {
				continue;
			}
			if (!(server.getPlayerList().getPlayer(p.owner) instanceof ServerPlayer owner) || owner.level() != p.level) {
				continue;
			}
			for (LivingEntity e : p.level.getEntitiesOfClass(LivingEntity.class, new AABB(p.at, p.at).inflate(POOL_RADIUS, 1.5, POOL_RADIUS))) {
				Vec3 feet = e.position();
				double flat = Math.hypot(feet.x - p.at.x, feet.z - p.at.z);
				if (flat > POOL_RADIUS + e.getBbWidth() / 2 || feet.y < p.at.y - 0.5 || feet.y > p.at.y + 1.0 || !e.isAlive()
					|| !Allies.areaHits(owner, e) || e instanceof TurretEntity t && t.getUUID().equals(HeroManager.state(owner) == null ? null
					: HeroManager.state(owner).torbjorn.turret)) {
					continue;
				}
				Long last = BURNED.get(e.getUUID());
				if (last != null && last == now) {
					continue;
				}
				BURNED.put(e.getUUID(), now);
				double step = BURN_EVERY / 20.0;
				HeroState hs = e instanceof ServerPlayer hit ? HeroManager.state(hit) : null;
				boolean armored = hs != null && hs.hero != null && dev.overcraft.hero.Shields.armorLeft((ServerPlayer) e, hs) > 0;
				double damage = (POOL_DPS + (armored ? POOL_ARMOR_DPS : 0)) * step;
				// a damage over time: no damage boost raises it (W Molten Core: "the damage over time cannot" be amplified): the boost
				// the hurt would add is taken off first (Boosts.amplify, LivingEntityMixin)
				float boost = dev.overcraft.mercy.Boosts.amplify(owner, 1f, e);
				float dealt = Combat.damage(e, Combat.blast(p.level, null, owner), damage / (boost > 0 ? boost : 1), SLAG_BLOW);
				ultDealt(owner, dealt, e);
			}
		}
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		TorbjornState t = s.torbjorn;
		if (Sleep.asleep(player)) {
			return;
		}
		boolean silenced = dev.overcraft.sombra.Hack.silenced(player);
		boolean busy = t.ultCast >= 0 || t.turretCast >= 0 || t.overloadCast > 0;
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !silenced && !busy && s.ability1Cooldown <= 0 && canRedeploy(turret(player, t))) {
			startTurret(player, s, t);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !silenced && !busy && s.ability2Cooldown <= 0 && t.overload <= 0) {
			startOverload(player, s, t, false);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !silenced && t.ultCast < 0 && t.ultLeft <= 0 && t.turretCast < 0
			&& s.ultCharge >= ULT_COST) {
			castUlt(player, s, t);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && !t.hammer && t.ultLeft <= 0) {
			startReload(player, s, t, player.level().getGameTime());
		}
	}

	// ---- kills ----

	/** He killed an enemy (Voice.onKill): Specialist's +50 % reload speed for 3 s (D 01C7 #2567), a hammer kill's line. */
	public static void killed(ServerPlayer player, HeroState s, @Nullable Blow blow) {
		s.torbjorn.specialistUntil = player.level().getGameTime() + Ow.ticks(SPECIALIST_TIME);
		if (blow == HAMMER_BLOW) {
			Voice.say(player, "hammer_kill", 1);
		}
	}

	/**
	 * Molten Core's damage: the hit marker and On Fire, but no ult charge (W Ultimate ability: an ult's damage charges no ult; the
	 * critic of 385d464 saw 108 -> 1357 in 6.5 s from his pools) nor perk progress.
	 */
	private static void ultDealt(ServerPlayer owner, float dealt, LivingEntity e) {
		Combat.addFireForDamage(owner, dealt, e);
		if (dealt > 0) {
			Effects.hitMarker(owner, e, false);
		}
	}

	private static void dealt(ServerPlayer owner, float dealt, LivingEntity e, boolean crit) {
		Combat.credit(owner, dealt, e);
		Combat.addFireForDamage(owner, dealt, e);
		if (dealt > 0) {
			Effects.hitMarker(owner, e, crit);
		}
	}

	// ---- aim ----

	private static Vec3 muzzle(ServerPlayer player) {
		return offset(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
	}

	private static Vec3 offset(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 top = side.cross(look).normalize();
		Vec3 from = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return from;
	}

	/** What his crosshair is on (a creature first, then a block), else {@code range} m out (Junkrat's way). */
	private static Vec3 aimPoint(ServerPlayer player, double range) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(range));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		net.minecraft.world.phys.EntityHitResult onIt = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,
			player, eye, target, new AABB(eye, target).inflate(1.0), e -> e != player && e.isPickable() && !e.isSpectator()
				&& !(e instanceof dev.overcraft.entity.OwProjectile) && !(e instanceof TurretEntity), 0.0f);
		if (onIt != null) {
			target = onIt.getLocation();
		}
		return target.distanceTo(eye) < 1.5 ? far : target;
	}

	// ---- sync and leaving play ----

	@Override
	public HeroSyncPayload sync(HeroState s) {
		TorbjornState t = s.torbjorn;
		boolean molten = t.ultCast >= 0 || t.ultLeft > 0;
		int mode = (t.turret != null ? MODE_TURRET_OUT : 0) | (t.overload > 0 ? MODE_OVERLOAD : 0) | (t.hammer ? MODE_HAMMER : 0)
			| (molten ? MODE_MOLTEN : 0);
		float active = t.overload > 0 ? t.overload / 20f : 0;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, molten,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / reloadTicks(t.reloadFast)) : -1,
			t.slag, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), (float) t.turretCooldownTotal, mode, active,
			molten ? t.ultLeft / (float) Ow.ticks(ULT_TIME) : 0);
	}

	/**
	 * Out of play (#100): casts cut, Overload and Molten Core end (the ult's charge stays spent), the Rivet Gun back in hand. His turret
	 * stays through G and death and goes with him (forGood), as do his pools.
	 */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		TorbjornState t = s.torbjorn;
		t.turretCast = -1;
		t.swing = -1;
		TurretEntity turret = turret(player, t);
		if (turret != null) {
			// Overloaded Turret's level 3 ends with Overload (D 1688 #444): his kit doesn't tick while he's dead, so it's said here (the
			// critic of 385d464: level 3 held through his death)
			turret.overloaded(false);
		}
		if (t.overload > 0) {
			endOverload(player, s, t);
		}
		endUlt(player, s, t);
		t.hammer = false;
		t.specialistUntil = -1000;
		t.reloadFast = false;
		Combat.buff(player, Attributes.MOVEMENT_SPEED, HAMMER_TIME_ID, 0, 0);
		player.setAttached(HeroManager.TORBJORN_LOOK, 0);
		if (why.forGood()) {
			clearWorld(player, s, t);
		}
	}

	/** His turret and pools in every world: gone. */
	private static void clearWorld(ServerPlayer player, HeroState s, TorbjornState t) {
		UUID me = player.getUUID();
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity e : level.getAllEntities()) {
				if (e instanceof TurretEntity turret && me.equals(turret.ownerUuid())) {
					turret.discard();
				}
			}
		}
		POOLS.removeIf(p -> p.owner.equals(me));
		t.turret = null;
	}

	/** The world stopped. */
	public static void clear() {
		POOLS.clear();
		BURNED.clear();
	}
}
