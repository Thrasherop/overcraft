package dev.overcraft.junkrat;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.ConcussionMineEntity;
import dev.overcraft.entity.FragEntity;
import dev.overcraft.entity.RipTireEntity;
import dev.overcraft.entity.SteelTrapEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
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
import dev.overcraft.registry.OcEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static dev.overcraft.junkrat.JunkratStats.*;

/**
 * Junkrat's kit (#73; numbers in {@link JunkratStats}, the data map tools/reference/junkrat-map.md). His buttons by his data's ids: the
 * left button the Frag Launcher, Shift Concussion Mine (the right button sets it off), E Steel Trap, Q RIP-Tire (either fire button or
 * Q sets it off; Shift is Nitro Boost on it). The server owns everything: his frags, mine, trap and tire are its entities, the mine's
 * launch is its push (sent to his client as any knockback is), and the tire is driven by the server from his keys and facing, his own
 * client turning his walk and jump off (his body still falls) and showing the tire (ClientJunkrat). Total Mayhem: none of his explosions hurt him, and he drops six bombs
 * when he dies.
 */
public final class Junkrat implements HeroKit {
	/** The synced mode's bits. */
	public static final int MODE_TIRE = 1, MODE_MINE_OUT = 2, MODE_TIRE_ARMED = 4, MODE_SILENCED = 8, MODE_NITRO_READY = 16,
		MODE_NITRO = 32, MODE_TRAP_OUT = 64;

	static final Blow FRAG_BLOW = Blow.weapon(false).by(Hero.JUNKRAT);
	static final Blow MINE_BLOW = Blow.ability("concussion_mine").by(Hero.JUNKRAT);
	static final Blow TRAP_BLOW = Blow.ability("steel_trap").by(Hero.JUNKRAT);
	static final Blow TIRE_BLOW = Blow.ult("rip_tire").by(Hero.JUNKRAT);
	static final Blow MAYHEM_BLOW = Blow.ability("total_mayhem").by(Hero.JUNKRAT);
	/** How long a launch of his counts as moving him before the server sees him off the ground (ticks, ours: 0.5 s). */
	static final int LAUNCH_GRACE = 10;
	private static final Identifier ROOT_SPEED = Overcraft.id("steel_trap_root");
	private static final Identifier ROOT_JUMP = Overcraft.id("steel_trap_root_jump");

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		// his mine and trap outlive a death (HeroKit.leavePlay): the respawn's fresh kit keeps them his (a switch has already cleared
		// them: clearWorld)
		UUID mine = s.junkrat.mine, trap = s.junkrat.trap;
		var level = s.junkrat.level;
		s.junkrat.clear();
		s.junkrat.mine = mine;
		s.junkrat.trap = trap;
		// and the world they're in: a respawn in another dimension clears them on the next tick (forgotten, they stayed "out" on his
		// HUD while out of his reach: the critic, 2026-11-13)
		s.junkrat.level = level;
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.weaponRecovery = 0;
		s.ability2Cooldown = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		JunkratState j = s.junkrat;
		long now = player.level().getGameTime();
		j.silenced = dev.overcraft.sombra.Hack.silenced(player);
		if (j.level != null && j.level != player.level().dimension()) {
			// another dimension: his mine, trap and tire are out of his reach (they'd be left there for good)
			clearWorld(player, s, j);
		}
		j.level = player.level().dimension();
		double rate = Combat.cooldownRate(player);
		if (j.mineCharges < MINE_CHARGES) {
			// one charge back every 7 s while he has fewer than two (D #333 / #659)
			j.mineRecharge += rate;
			if (j.mineRecharge >= Ow.ticks(MINE_RECHARGE)) {
				j.mineCharges++;
				j.mineRecharge = 0;
			}
		} else {
			j.mineRecharge = 0;
		}
		if (j.trapCooldownIn >= 0) {
			if (--j.trapCooldownIn < 0) {
				// its 10 s from 0.5 s after the throw (D 09BA #446 / #448)
				s.ability2Cooldown = Ow.ticks(TRAP_COOLDOWN);
			}
		} else {
			s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		}
		if (s.reloadHeld > 0) {
			s.reloadHeld--;
		}
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		mineCast(player, s, j, able);
		RipTireEntity tire = tire(player, j);
		if (tire != null && tireOverdue(now - j.tireAt)) {
			// out past the simulation distance it's loaded but doesn't tick (its own 10 s never comes): it goes off on his clock
			tireBlast(player, s, tire);
			tire = null;
		}
		if (j.tire != null && tire == null) {
			// the tire went without us (unloaded, killed off by the game): the ult is over
			endTire(player, s, j, null);
		}
		boolean leftPress = s.primaryHeld && !j.leftWas, rightPress = s.secondaryHeld && !j.rightWas;
		j.leftWas = s.primaryHeld;
		j.rightWas = s.secondaryHeld;
		if (tire != null) {
			// on the tire: either fire button sets it off once armed (D 0233 #374 / #375 / #455); a stun or his death doesn't stop it
			// (W: "it can still be detonated"), so this asks only that he's here
			if ((leftPress || rightPress) && controls && tireArmed(tire)) {
				tireBlast(player, s, tire);
			}
		} else {
			weapon(player, s, j, able, now);
			if (rightPress && able && !j.silenced) {
				detonateMine(player, s, j, now);
			}
		}
		// a launch of his own (the mine's, his frag's push) lands him unhurt (CLAUDE.md, #35): it counts as moving until the server
		// sees him leave the ground, half a second at most (found playing, J2d: the tick after the launch the server still had him on
		// the ground, which ended the soft landing at once, and a 6 m mine jump took 37.5)
		boolean launched = j.launchGrace > 0;
		if (launched) {
			j.launchGrace = player.onGround() ? j.launchGrace - 1 : 0;
		}
		HeroManager.softLanding(player, s, launched, false);
	}

	// ---- Frag Launcher (022B) ----

	/** His fire interval's rate: his attack rate, +0.35 under Bomb Voyage (D 51E8 #485: v21541 += 0.35). */
	static double fireRate(ServerPlayer player, JunkratState j, long now) {
		return Combat.attackRate(player) * (now < j.voyageUntil ? 1 + VOYAGE_SPEED : 1);
	}

	/** How long his reload runs (ticks): 1.5 s, or 1.5 / (1 + 0.5) with Specialist's bonus (D #90 divides the animation by v32187). */
	public static double reloadTicks(boolean fast) {
		return RELOAD * 20 / (fast ? 1 + SPECIALIST_SPEED : 1);
	}

	private static void weapon(ServerPlayer player, HeroState s, JunkratState j, boolean able, long now) {
		s.weaponRecovery = Math.max(0, s.weaponRecovery - fireRate(player, j, now));
		if (s.primaryReload >= 0) {
			s.primaryReload += Combat.attackRate(player);
			// the frags back at 0.867 s, whatever Specialist does to the animation (D #87: that wait has no speed scalar, M)
			if (s.primaryReload >= RELOAD_AMMO_AT * 20 && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= reloadTicks(j.reloadFast)) {
				s.primaryReload = -1;
				j.reloadFast = false;
			}
		}
		if (!able || j.mineCast >= 0) {
			return;
		}
		if (s.primaryReload < 0 && s.primaryAmmo <= 0 && s.weaponRecovery <= 0 && s.reloadHeld <= 0) {
			// empty: the reload starts by itself (D #94)
			startReload(player, s, j, now);
			return;
		}
		if (s.primaryReload >= 0 || s.primaryAmmo <= 0 || s.weaponRecovery > 0 || !s.primaryHeld) {
			return;
		}
		s.primaryAmmo--;
		// 0.667 s to the next (D #102), the exact fraction kept (the next shot doesn't wait for a whole tick more)
		s.weaponRecovery += FRAG_INTERVAL * 20;
		Vec3 from = muzzle(player);
		Vec3 aim = aimPoint(player, 200);
		double speed = FRAG_SPEED * (s.has(Perk.JUNKRAT_FRAG_CANNON) ? FRAG_CANNON : 1);
		FragEntity frag = new FragEntity(player, player.level(), false, Ow.ticks(FRAG_FUSE));
		frag.launch(from, aim.subtract(from), Ow.speed(speed));
		player.level().addFreshEntity(frag);
		Combat.animate(player, "fire");
		JunkratSounds.split(player, "frag.fire", from, 80);
		if (s.has(Perk.JUNKRAT_FRAG_CANNON)) {
			// its own sound on the shot (D #463 effect 6599)
			JunkratSounds.all(player.level(), "frag.cannon", from, 60);
		}
	}

	private static void startReload(ServerPlayer player, HeroState s, JunkratState j, long now) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO) {
			return;
		}
		s.primaryReload = 0;
		// Specialist's bonus: this reload takes it, and it's used up (D #225 zeroes it at the reload's end, M)
		j.reloadFast = now < j.specialistUntil;
		if (j.reloadFast) {
			j.specialistUntil = -1000;
		}
		Combat.animate(player, "reload");
		JunkratSounds.split(player, "reload", player.getEyePosition(), 30);
	}

	/**
	 * Whether a frag's bounce off a face sets it off (W, M): a ground face after any earlier bounce (two ground bounces, or a wall then
	 * the ground). A wall never does.
	 */
	public static boolean bounceBursts(int bouncesBefore, boolean ground) {
		return ground && bouncesBefore >= 1;
	}

	/** A frag's speed off a face of normal {@code n}: x0.5 into it (restitution), x0.75 along it (D: M / L). */
	public static Vec3 bounce(Vec3 v, Vec3 n) {
		double into = v.dot(n);
		Vec3 normal = n.scale(into);
		Vec3 along = v.subtract(normal);
		return along.scale(FRAG_FRICTION).subtract(normal.scale(FRAG_RESTITUTION));
	}

	/** A frag bounced off a block: its clatter. */
	public static void fragBounced(ServerPlayer owner, FragEntity frag, Vec3 at) {
		JunkratSounds.all((ServerLevel) frag.level(), "frag.bounce", at, 40);
	}

	/**
	 * A frag (or a Total Mayhem bomb) goes off at {@code at}, {@code direct} what it touched: a frag's 45 to an enemy it hit, then
	 * its splash (80 to 0.5 m, 10 at 2 m) on every enemy in sight and its push (5 away, 8 up) on them and, at half, on him (no damage
	 * to him: Total Mayhem). A bomb: 50 to every enemy within 2 m, no falloff, no push.
	 */
	public static void fragBurst(ServerPlayer owner, FragEntity frag, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = (ServerLevel) frag.level();
		if (frag.bomb()) {
			blast(owner, frag, at, null, 0, MAYHEM_DAMAGE, MAYHEM_RADIUS, MAYHEM_RADIUS, 1, 0, 0, 1, 0, MAYHEM_BLOW);
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			JunkratSounds.all(level, "mayhem.bomb", at, 80);
			return;
		}
		boolean hit = blast(owner, frag, at, direct, FRAG_DIRECT, FRAG_SPLASH, FRAG_SPLASH_FULL, FRAG_SPLASH_RADIUS,
			FRAG_SPLASH_EDGE, FRAG_KNOCK_AWAY, FRAG_KNOCK_UP, FRAG_KNOCK_EDGE, FRAG_KNOCK_SELF, FRAG_BLOW);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.02);
		JunkratSounds.blast(owner, level, "frag.explode", at, 120);
		if (hit) {
			JunkratSounds.split(owner, "frag.hit_enemy", at, 80);
		} else if (direct == null) {
			JunkratSounds.all(level, "frag.explode_world", at, 60);
		}
	}

	// ---- Concussion Mine (096A / 096B) ----

	private static void mineCast(ServerPlayer player, HeroState s, JunkratState j, boolean able) {
		if (j.mineCast < 0) {
			return;
		}
		if (!able) {
			// cut before it leaves his hand: nothing spent (W: "interrupted before the mine exists")
			j.mineCast = -1;
			return;
		}
		if (++j.mineCast < Ow.ticks(MINE_CAST)) {
			return;
		}
		j.mineCast = -1;
		if (j.mineCharges <= 0) {
			return;
		}
		// one mine out: the old one goes, silently (D #292 -> 096B #263)
		ConcussionMineEntity old = mine(player, j);
		if (old != null) {
			old.discard();
		}
		j.mineCharges--;
		Vec3 from = leftHand(player);
		ConcussionMineEntity mine = new ConcussionMineEntity(player, player.level());
		mine.launch(from, aimPoint(player, 60).subtract(from), Ow.speed(MINE_SPEED));
		player.level().addFreshEntity(mine);
		j.mine = mine.getUUID();
		j.mineThrownAt = player.level().getGameTime();
		JunkratSounds.split(player, "mine.throw", from, 40);
	}

	/** The mine landed on a block (D: it sticks). */
	public static void mineStuck(ServerPlayer owner, ConcussionMineEntity mine) {
		JunkratSounds.all((ServerLevel) mine.level(), "mine.stick", mine.position(), 30);
	}

	public static void mineBroken(ServerPlayer owner, ConcussionMineEntity mine) {
		HeroState s = HeroManager.state(owner);
		if (s != null && mine.getUUID().equals(s.junkrat.mine)) {
			s.junkrat.mine = null;
		}
	}

	/**
	 * The right button: the mine goes off 0.1 s from now, once 0.1 s has passed since the throw (D #230); if he's within 3.5 m of it
	 * now he's launched at once, 15 away and 15 up, unhurt (D #264 / #262), and Bomb Voyage starts (D 51E8 #485).
	 */
	private static void detonateMine(ServerPlayer player, HeroState s, JunkratState j, long now) {
		ConcussionMineEntity mine = mine(player, j);
		if (!canDetonate(mine != null && !mine.detonating(), now - j.mineThrownAt)) {
			return;
		}
		Combat.animate(player, "mine_detonate");
		JunkratSounds.split(player, "mine.detonate", player.getEyePosition(), 30);
		Vec3 at = mine.position();
		Vec3 centre = player.getBoundingBox().getCenter();
		// distance only, through a wall too: the data's whole test is #264 / #438 op71(op94(self, mine), 3.5) after #306 (the mine
		// exists) and #297 (not hacked); no sight check (c_096A.txt, read 2026-11-13 on the critic's report), unlike the blast's
		// TrackTargets on enemies
		if (mineLaunches(centre.distanceTo(at))) {
			Vec3 away = horizontalAway(at, centre);
			// set, not added (ours, M: the data's STU_FF3471E9 is read as a launch; a fall's speed doesn't eat it)
			Combat.launch(player, away.scale(Ow.speed(MINE_SELF_AWAY)).add(0, Ow.speed(MINE_SELF_UP), 0), player, MINE_BLOW);
			s.softLanding = true;
			j.launchGrace = LAUNCH_GRACE;
			JunkratSounds.all(player.level(), "mine.jump", player.position(), 40);
			Voice.say(player, "mine_jump", 1);
			if (s.has(Perk.JUNKRAT_BOMB_VOYAGE)) {
				j.voyageUntil = now + Ow.ticks(VOYAGE_TIME);
			}
		}
		mine.detonate();
		j.mine = null;
	}

	/** Whether the right button sets the mine off: one is out, not already going, thrown 0.1 s ago or more (D #230). */
	public static boolean canDetonate(boolean mineOut, long ticksSinceThrow) {
		return mineOut && ticksSinceThrow >= Ow.ticks(MINE_ARM);
	}

	/** Whether its press launches him: his middle within 3.5 m of the mine (D #264 / #438). */
	public static boolean mineLaunches(double distance) {
		return distance < MINE_SELF_RANGE;
	}

	/** The mine's blast: 120 inside 1 m down to 55 at 3 m and its push (15 away, 15 up) on every enemy in sight in 3 m (D 096B #251). */
	public static void mineBlast(ServerPlayer owner, ConcussionMineEntity mine, Vec3 at) {
		ServerLevel level = (ServerLevel) mine.level();
		blast(owner, mine, at, null, 0, MINE_DAMAGE, MINE_FULL, MINE_RADIUS_BLAST, MINE_EDGE_DAMAGE / MINE_DAMAGE,
			MINE_KNOCK_AWAY, MINE_KNOCK_UP, 1, 0, MINE_BLOW);
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 12, 0.8, 0.5, 0.8, 0.08);
		JunkratSounds.blast(owner, level, "mine.explode", at, 120);
	}

	// ---- Steel Trap (09BA / 09BB) ----

	private static void throwTrap(ServerPlayer player, HeroState s, JunkratState j) {
		// one trap: the old one goes (D #473)
		SteelTrapEntity old = trap(player, j);
		if (old != null) {
			old.discard();
		}
		Vec3 from = muzzle(player);
		SteelTrapEntity trap = new SteelTrapEntity(player, player.level());
		trap.launch(from, aimPoint(player, 40).subtract(from), Ow.speed(TRAP_SPEED));
		player.level().addFreshEntity(trap);
		j.trap = trap.getUUID();
		j.trapCooldownIn = Ow.ticks(TRAP_COOLDOWN_FROM);
		Combat.animate(player, "trap_throw");
		JunkratSounds.split(player, "trap.throw", from, 40);
	}

	public static void trapLanded(ServerPlayer owner, SteelTrapEntity trap) {
		ServerLevel level = (ServerLevel) trap.level();
		JunkratSounds.all(level, "trap.land", trap.position(), 30);
		JunkratSounds.all(level, "trap.arm", trap.position(), 20);
	}

	/** The first enemy whose box comes within 1 m of the armed trap (D 09BB #447: enemies), alive and here. */
	public static @Nullable LivingEntity trapVictim(ServerPlayer owner, SteelTrapEntity trap, AABB around) {
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		Vec3 at = trap.position().add(0, 0.1, 0);
		for (LivingEntity e : trap.level().getEntitiesOfClass(LivingEntity.class, around)) {
			if (e == owner || !e.isAlive() || !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d <= TRAP_TRIGGER && d < bestD) {
				best = e;
				bestD = d;
			}
		}
		return best;
	}

	/** How long it holds {@code e}: 2.5 s (D 0353 x (1 - CC resist): no Minecraft creature resists, ours). */
	public static double rootSeconds(LivingEntity e) {
		return TRAP_ROOT;
	}

	/** It springs on {@code victim}: 100, and a 2.5 s root (no moving, no jumping), and his "Trapped!" (D 09BB #881 / #882 / #791). */
	public static void trapSprung(ServerPlayer owner, SteelTrapEntity trap, LivingEntity victim) {
		ServerLevel level = (ServerLevel) trap.level();
		DamageSource source = level.damageSources().thrown(trap, owner);
		float dealt = Combat.damage(victim, source, TRAP_DAMAGE, TRAP_BLOW);
		dealt(owner, dealt, victim);
		root(victim, rootSeconds(victim));
		JunkratSounds.all(level, "trap.trigger", trap.position(), 40);
		JunkratSounds.split(owner, "trap.caught", trap.position(), 40);
		Voice.say(owner, "trap", 1);
	}

	/**
	 * A root: its speed and jump to nothing for {@code seconds} (attribute buffs, so creatures and players both: OwMovement.speedFactor).
	 * x0 of the total ({@link #ROOT_OP}), not -1 of the base: a base share leaves whatever else adds to the base (a baby's +50 % speed:
	 * a baby husk walked through it at 0.115, the critic, 2026-11-13). Jump Boost's power is added past the attribute
	 * (LivingEntity.getJumpPower), so a rooted jump is cancelled outright too (LivingEntityMixin, {@link #rooted}).
	 */
	public static void root(LivingEntity victim, double seconds) {
		Combat.buff(victim, Attributes.MOVEMENT_SPEED, ROOT_SPEED, -1, seconds, ROOT_OP);
		Combat.buff(victim, Attributes.JUMP_STRENGTH, ROOT_JUMP, -1, seconds, ROOT_OP);
		victim.setDeltaMovement(0, Math.min(0, victim.getDeltaMovement().y), 0);
		Combat.send(victim);
	}

	/** The root's operation: the whole value x (1 - 1) = 0, after every other modifier (AttributeInstance.calculateValue). */
	public static final net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation ROOT_OP =
		net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

	/** The root's speed modifier (its id, for a Stalwart's slow resistance to leave alone: Tank.OWN_SLOWS). */
	public static Identifier rootSpeedId() {
		return ROOT_SPEED;
	}

	/** Whether Steel Trap holds {@code e} now (its jump modifier is on it). */
	public static boolean rooted(LivingEntity e) {
		var jump = e.getAttribute(Attributes.JUMP_STRENGTH);
		return jump != null && jump.getModifier(ROOT_JUMP) != null;
	}

	/** Life Grip pulls them out (#124; W Junkrat rev 380944 "Lifeweaver can pull the target free"): the trap lets go, the root ends. */
	public static void free(LivingEntity victim) {
		for (SteelTrapEntity trap : victim.level().getEntitiesOfClass(SteelTrapEntity.class, victim.getBoundingBox().inflate(TRAP_TRIGGER + 2),
			t -> t.caught() == victim)) {
			trap.letGo();
		}
		unroot(victim);
	}

	private static void unroot(LivingEntity victim) {
		Combat.buff(victim, Attributes.MOVEMENT_SPEED, ROOT_SPEED, 0, 0);
		Combat.buff(victim, Attributes.JUMP_STRENGTH, ROOT_JUMP, 0, 0);
	}

	public static void trapReleased(ServerPlayer owner, SteelTrapEntity trap, LivingEntity victim) {
		unroot(victim);
		if (trap.level() instanceof ServerLevel level) {
			JunkratSounds.all(level, "trap.release", trap.position(), 30);
		}
	}

	public static void trapBroken(ServerPlayer owner, SteelTrapEntity trap) {
		HeroState s = HeroManager.state(owner);
		if (s != null && trap.getUUID().equals(s.junkrat.trap)) {
			s.junkrat.trap = null;
		}
	}

	// ---- RIP-Tire (0232 / 0233 / 0234) ----

	private static void castTire(ServerPlayer player, HeroState s, JunkratState j) {
		s.ultCharge = Combat.afterUlt(Hero.JUNKRAT, s.ultCharge);
		ServerLevel level = player.level();
		double yaw = Math.toRadians(player.getYRot());
		Vec3 ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		// in front of him, 0.05 m off his box (D #317 v625), or at his feet when that's in a wall (ours)
		Vec3 at = player.position().add(ahead.scale(player.getBbWidth() / 2 + TIRE_WIDTH / 2 + TIRE_AHEAD));
		RipTireEntity tire = new RipTireEntity(OcEntities.RIP_TIRE, level);
		tire.setPos(at);
		if (!level.noCollision(tire, tire.getBoundingBox())) {
			tire.setPos(player.position());
		}
		tire.setYRot(player.getYRot());
		tire.setOwner(player);
		level.addFreshEntity(tire);
		j.tire = tire.getUUID();
		j.tireAt = level.getGameTime();
		j.nitroUsed = false;
		j.mineCast = -1;
		s.ultActive = Ow.ticks(TIRE_TIME);
		// "Reloads": the Frag Launcher full at the cast (W, M)
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		Combat.animate(player, "tire_cast");
		JunkratSounds.split(player, "tire.cast", player.getEyePosition(), 60);
		JunkratSounds.split(player, "tire.loop", at, 60);
		ultLine(player);
	}

	/** RIP-Tire's line: his own copy in his head, the allies' and the enemies' from him (Cassidy's way; D 0232 #572). */
	private static void ultLine(ServerPlayer hero) {
		net.minecraft.sounds.SoundEvent own = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Overcraft.id("junkrat.voice.ultimate"));
		net.minecraft.sounds.SoundEvent friendly = JunkratSounds.of("ult.friendly"), enemy = JunkratSounds.of("ult.enemy");
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

	/** Whether a tire out {@code ticks} of game time is past its 10 s by more than half a second (one that stopped ticking: ours). */
	public static boolean tireOverdue(long ticks) {
		return ticks > Ow.ticks(TIRE_TIME) + 10;
	}

	public static boolean tireArmed(RipTireEntity tire) {
		return tire.age >= Ow.ticks(TIRE_ARM);
	}

	/** Nitro Boost's speed share {@code t} ticks into it: +100 % easing to +25 % over 2 s, held to 2.5 s, then none (D #438 / #397, M linear). */
	public static double nitroShare(double t) {
		double s = t / 20.0;
		if (s < 0 || s >= NITRO_TIME) {
			return 0;
		}
		return s >= NITRO_FADE ? NITRO_END : NITRO_START + (NITRO_END - NITRO_START) * s / NITRO_FADE;
	}

	/**
	 * One tick of the tire, from his keys and facing (the server's copy of his input: ServerPlayer.getLastClientInput): 12 m/s ahead,
	 * 6 back, 3 to the side, at once (ours: the data's acceleration isn't read); Jump on the ground jumps it (9 m/s up, L), Jump held
	 * into a wall climbs it (6 m/s, ours), else it falls at Overwatch's 17.5. Out of time: it goes off.
	 */
	public static void driveTire(ServerPlayer owner, RipTireEntity tire) {
		HeroState s = HeroManager.state(owner);
		if (s == null || s.hero != Hero.JUNKRAT || !tire.getUUID().equals(s.junkrat.tire) || owner.level() != tire.level()) {
			tire.discard();
			return;
		}
		JunkratState j = s.junkrat;
		if (tire.age >= Ow.ticks(TIRE_TIME)) {
			// 10 s: it goes off on its own (D 0233 #396 -> #415)
			tireBlast(owner, s, tire);
			return;
		}
		Input in = owner.getLastClientInput();
		boolean keys = HeroManager.controls(owner) && owner.isAlive();
		double yaw = Math.toRadians(owner.getYRot());
		Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		double ahead = !keys ? 0 : in.forward() && !in.backward() ? TIRE_FORWARD : in.backward() && !in.forward() ? -TIRE_BACK : 0;
		double side = !keys ? 0 : in.left() && !in.right() ? TIRE_STRAFE : in.right() && !in.left() ? -TIRE_STRAFE : 0;
		long now = owner.level().getGameTime();
		double boost = j.nitroUsed ? nitroShare(now - j.nitroAt - Ow.ticks(0.15)) : 0;
		tire.setBoosted(boost > 0);
		Vec3 h = forward.scale(ahead).add(left.scale(side)).scale(Ow.speed(1 + boost));
		boolean jump = keys && in.jump();
		double vy = tire.getDeltaMovement().y;
		if (jump && tire.horizontalCollision && ahead > 0) {
			// up the wall it's driven into (W: "climbs walls"; hold Jump, the hero option's default)
			vy = Ow.speed(TIRE_CLIMB);
		} else if (jump && !tire.jumpWas && tire.onGround()) {
			vy = Ow.speed(TIRE_JUMP);
			JunkratSounds.all((ServerLevel) tire.level(), "tire.jump", tire.position(), 40);
		} else {
			vy -= Ow.accel(dev.overcraft.hero.OwMovement.GRAVITY_DOWN);
		}
		tire.jumpWas = jump;
		tire.setDeltaMovement(h.x, vy, h.z);
		tire.move(MoverType.SELF, tire.getDeltaMovement());
		if (tire.onGround() && tire.getDeltaMovement().y < 0) {
			tire.setDeltaMovement(tire.getDeltaMovement().x, 0, tire.getDeltaMovement().z);
		}
		if (tire.onGround() && !tire.wasDown && tire.age > 2) {
			JunkratSounds.all((ServerLevel) tire.level(), "tire.land", tire.position(), 30);
		}
		tire.wasDown = tire.onGround();
		tire.setYRot(owner.getYRot());
		j.tireLeft = Ow.ticks(TIRE_TIME) - tire.age;
		// (his body's fall counts as any fall: a tire cast in mid-air leaves him falling, and that fall hurts as it would; the critic,
		// 2026-11-13: forgotten each tick, a 20 m drop during the tire was erased)
	}

	/** It goes off: 600 inside 2 m down to 50 at 10 m (x0.5 after Nitro Boost) and its push on every enemy in sight (D 0233 #432-#444). */
	public static void tireBlast(ServerPlayer owner, HeroState s, RipTireEntity tire) {
		ServerLevel level = (ServerLevel) tire.level();
		Vec3 at = tire.position().add(0, TIRE_HEIGHT / 2, 0);
		double share = s.junkrat.nitroUsed ? NITRO_DAMAGE : 1;
		boolean killed = blastKills(owner, tire, at, share);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 40, 2, 1, 2, 0.1);
		JunkratSounds.blast(owner, level, "tire.explode", at, 200);
		endTire(owner, s, s.junkrat, tire);
		if (killed && owner.isAlive()) {
			Voice.say(owner, "tire_kill", 1);
		}
	}

	private static boolean blastKills(ServerPlayer owner, RipTireEntity tire, Vec3 at, double share) {
		return blast(owner, tire, at, null, 0, TIRE_DAMAGE * share, TIRE_FULL, TIRE_RADIUS, TIRE_EDGE_DAMAGE / TIRE_DAMAGE,
			TIRE_KNOCK_AWAY, TIRE_KNOCK_UP, TIRE_KNOCK_EDGE, 0, TIRE_BLOW) && lastKilled;
	}

	/** Broken by enemies: gone, no blast (D 0233 #518). */
	public static void tireBroken(ServerPlayer owner, RipTireEntity tire) {
		HeroState s = HeroManager.state(owner);
		JunkratSounds.all((ServerLevel) tire.level(), "tire.destroyed", tire.position(), 60);
		if (s != null && tire.getUUID().equals(s.junkrat.tire)) {
			endTire(owner, s, s.junkrat, tire);
		} else {
			tire.discard();
		}
	}

	private static void endTire(ServerPlayer owner, HeroState s, JunkratState j, @Nullable RipTireEntity tire) {
		if (tire != null) {
			tire.discard();
		}
		if (j.tire == null) {
			return;
		}
		j.tire = null;
		j.nitroUsed = false;
		s.ultActive = 0;
		Combat.animate(owner, "tire_end");
		JunkratSounds.split(owner, "tire.end", owner.getEyePosition(), 40);
	}

	// ---- presses ----

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		JunkratState j = s.junkrat;
		RipTireEntity tire = tire(player, j);
		boolean asleep = Sleep.asleep(player);
		if (tire != null) {
			if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && tireArmed(tire)) {
				tireBlast(player, s, tire);
				return;
			}
			if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && s.has(Perk.JUNKRAT_NITRO_BOOST) && !j.nitroUsed
				&& tire.age >= Ow.ticks(NITRO_FROM)) {
				// Nitro Boost: once, from 1.6 s (D 51E8 #425 / #434); its blast halved from now (0233 #682)
				j.nitroUsed = true;
				j.nitroAt = player.level().getGameTime();
				JunkratSounds.all(player.level(), "nitro.boost", tire.position(), 60);
			}
			return;
		}
		if (asleep) {
			return;
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !j.silenced && j.mineCharges > 0 && j.mineCast < 0) {
			j.mineCast = 0;
			Combat.animate(player, "mine_throw");
			Voice.say(player, "mine_throw", 1);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !j.silenced && s.ability2Cooldown <= 0 && j.trapCooldownIn < 0) {
			throwTrap(player, s, j);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !j.silenced && s.ultCharge >= ULT_COST) {
			castTire(player, s, j);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s, j, player.level().getGameTime());
		}
	}

	// ---- kills ----

	/**
	 * He killed an enemy (Voice.onKill): Specialist's +50 % reload speed for 3 s (D 01C7 #2525), a mine kill's charge back with Mine
	 * Recycling (D 51E8 #494-#501) and its line, a tire kill's line (tireBlast says it once for the blast).
	 */
	public static void killed(ServerPlayer player, HeroState s, @Nullable Blow blow) {
		JunkratState j = s.junkrat;
		j.specialistUntil = player.level().getGameTime() + Ow.ticks(SPECIALIST_TIME);
		if (blow != null && blow.kind() == Blow.Kind.ABILITY && "concussion_mine".equals(blow.icon())) {
			if (s.has(Perk.JUNKRAT_MINE_RECYCLING)) {
				j.mineCharges = Math.min(MINE_CHARGES, j.mineCharges + 1);
			}
			Voice.say(player, "mine_kill", 1);
		}
	}

	// ---- the blasts ----

	/** Whether the last blast killed something (tireBlast's line). */
	private static boolean lastKilled;

	/**
	 * A blast of his at {@code at}: first {@code directDamage} to {@code direct} when it's an enemy (a frag's hit), then {@code damage}
	 * x the falloff (full to {@code full} m, {@code edge} of it at {@code radius} m) on every enemy whose box is within {@code radius}
	 * and in sight, not behind an enemy barrier, and its push ({@code away} / {@code up} m/s, x1 to {@code full} easing to
	 * {@code knockEdge} at the radius); on him only the push at {@code self} (0: none), never damage (Total Mayhem). Enemy barriers in
	 * reach take the splash. The dragon takes it through its nearest part. Returns whether an enemy was hit.
	 */
	static boolean blast(ServerPlayer owner, Entity from, Vec3 at, @Nullable Entity direct, double directDamage,
						 double damage, double full, double radius, double edge, double away, double up, double knockEdge, double self,
						 Blow blow) {
		ServerLevel level = (ServerLevel) from.level();
		lastKilled = false;
		boolean hit = false;
		EnderDragonPart part = direct instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : direct instanceof LivingEntity living ? living : null;
		if (target != null && directDamage > 0 && Allies.canHurt(owner, target)) {
			DamageSource hitSource = level.damageSources().thrown(from, owner);
			float dealt = part != null ? Combat.damagePart(level, part, hitSource, directDamage, blow) : Combat.damage(target, hitSource, directDamage, blow);
			dealt(owner, dealt, target);
			hit = true;
			lastKilled |= !target.isAlive();
		}
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, at);
			if (near.distanceTo(at) <= radius) {
				b.barrierTake(damage * falloff(near.distanceTo(at), full, radius, edge), near);
			}
		}
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, radius, d -> damage * falloff(d, full, radius, edge));
		DamageSource source = Combat.blast(level, from, owner);
		for (Map.Entry<LivingEntity, Double> entry : reached(level, at, radius).entrySet()) {
			LivingEntity e = entry.getKey();
			double d = entry.getValue();
			boolean me = e == owner;
			boolean enemy = !me && Allies.areaHits(owner, e);
			if (!e.isAlive() || !me && !enemy || me && self <= 0) {
				continue;
			}
			if (!(e instanceof EnderDragon) && !Combat.inSight(level, at, e, owner) || !me && (Barriers.between(level, at,
				e.getBoundingBox().getCenter(), owner) != null || dev.overcraft.brigitte.Barrier.shields(e, owner, at))) {
				continue;
			}
			if (blastDamages(me, enemy)) {
				float dealt = hurtArea(level, e, at, source, damage * falloff(d, full, radius, edge), blow);
				dealt(owner, dealt, e);
				hit |= dealt > 0;
				lastKilled |= !e.isAlive();
			}
			if (away > 0 || up > 0) {
				double k = knockShare(d, full, radius, knockEdge) * (me ? self : 1);
				Vec3 dir = horizontalAway(at, e.getBoundingBox().getCenter());
				Combat.push(e, dir.scale(Ow.speed(away * k)).add(0, Ow.speed(up * k), 0), owner, blow);
				if (me && HeroManager.pushLandsSoft(owner.getDeltaMovement().y)) {
					// (Combat.push set his speed from the one his client last reported, plus the push)
					HeroState s = HeroManager.state(owner);
					if (s != null) {
						s.softLanding = true;
						s.junkrat.launchGrace = LAUNCH_GRACE;
					}
				}
			}
		}
		return hit;
	}

	/** Whether a blast of his damages one it reaches: an enemy, never him (Total Mayhem: "no damage to self with explosives", W). */
	public static boolean blastDamages(boolean me, boolean enemy) {
		return !me && enemy;
	}

	/** Every creature with a box within {@code radius} of {@code at}, the dragon by its nearest part, with how far. */
	static Map<LivingEntity, Double> reached(ServerLevel level, Vec3 at, double radius) {
		Map<LivingEntity, Double> out = new LinkedHashMap<>();
		AABB around = new AABB(at, at).inflate(radius + 2);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, around)) {
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d <= radius) {
				out.merge(e, d, Math::min);
			}
		}
		for (EnderDragonPart p : level.getEntitiesOfClass(EnderDragonPart.class, around)) {
			double d = Math.sqrt(p.getBoundingBox().distanceToSqr(at));
			if (d <= radius) {
				out.merge(p.parentMob, d, Math::min);
			}
		}
		return out;
	}

	/** Area damage on {@code e}: the dragon through its part nearest the blast (Combat.damagePart: its head and neck take it all). */
	static float hurtArea(ServerLevel level, LivingEntity e, Vec3 at, DamageSource source, double ow, Blow blow) {
		if (e instanceof EnderDragon dragon) {
			EnderDragonPart nearest = null;
			double best = Double.MAX_VALUE;
			for (EnderDragonPart p : dragon.getSubEntities()) {
				double d = p.getBoundingBox().distanceToSqr(at);
				if (d < best) {
					best = d;
					nearest = p;
				}
			}
			if (nearest != null) {
				return Combat.damagePart(level, nearest, source, ow, blow);
			}
		}
		return Combat.damage(e, source, ow, blow);
	}

	/** A blast's share at {@code d} m: 1 to {@code full}, down to {@code edge} at {@code radius}, none past (linear: M). */
	public static double falloff(double d, double full, double radius, double edge) {
		if (d > radius) {
			return 0;
		}
		if (d <= full) {
			return 1;
		}
		return 1 - (d - full) / (radius - full) * (1 - edge);
	}

	/** Its push's share: 1 to {@code full}, down to {@code edge} at the radius (D rings). */
	public static double knockShare(double d, double full, double radius, double edge) {
		return falloff(d, full, radius, edge);
	}

	/** Flat, from {@code from} toward {@code to}, unit (none when right above or below). */
	static Vec3 horizontalAway(Vec3 from, Vec3 to) {
		Vec3 away = new Vec3(to.x - from.x, 0, to.z - from.z);
		return away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize();
	}

	/** His damage to {@code e}: the credit (ult charge, perks), the fire meter, the hit marker. */
	private static void dealt(ServerPlayer owner, float dealt, LivingEntity e) {
		Combat.credit(owner, dealt, e);
		Combat.addFireForDamage(owner, dealt, e);
		if (dealt > 0) {
			Effects.hitMarker(owner, e, false);
		}
	}

	// ---- Total Mayhem (022A) ----

	/** He died: six bombs drop from him, 1.5 m/s within 90 degrees of straight down, each off 0.8 ... 1.3 s later (D 022A #76). */
	static void mayhem(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 from = player.position().add(0, player.getBbHeight() * 0.6, 0);
		net.minecraft.util.RandomSource random = player.getRandom();
		for (int i = 0; i < MAYHEM_BOMBS; i++) {
			// within 45 degrees of straight down, any way round (ours: the data's 90 degree spread read as the cone's full width)
			double tilt = Math.toRadians(MAYHEM_SPREAD / 2) * Math.sqrt(random.nextDouble());
			double turn = random.nextDouble() * Math.PI * 2;
			Vec3 dir = new Vec3(Math.sin(tilt) * Math.cos(turn), -Math.cos(tilt), Math.sin(tilt) * Math.sin(turn));
			FragEntity bomb = new FragEntity(player, level, true, Ow.ticks(MAYHEM_FIRST + MAYHEM_STEP * i));
			bomb.launch(from, dir, Ow.speed(MAYHEM_SPEED));
			level.addFreshEntity(bomb);
		}
		JunkratSounds.all(level, "mayhem.drop", from, 40);
	}

	// ---- what's his in the world ----

	/**
	 * Whether a placed mine or trap of {@code owner}'s is still the one he has out: not once he's another hero, or has thrown a newer one
	 * (one left from a session that ended without his leaving: they're saved with their chunks). Unknown while he's away: kept.
	 */
	public static boolean stillHis(Entity placed, @Nullable Entity owner, boolean mine) {
		if (!(owner instanceof ServerPlayer player)) {
			return true;
		}
		HeroState s = HeroManager.state(player);
		return s != null && s.hero == Hero.JUNKRAT && placed.getUUID().equals(mine ? s.junkrat.mine : s.junkrat.trap);
	}

	static @Nullable ConcussionMineEntity mine(ServerPlayer player, JunkratState j) {
		return j.mine != null && player.level().getEntity(j.mine) instanceof ConcussionMineEntity m && m.isAlive() ? m : null;
	}

	static @Nullable SteelTrapEntity trap(ServerPlayer player, JunkratState j) {
		return j.trap != null && player.level().getEntity(j.trap) instanceof SteelTrapEntity t && t.isAlive() ? t : null;
	}

	public static @Nullable RipTireEntity tire(ServerPlayer player, JunkratState j) {
		return j.tire != null && player.level().getEntity(j.tire) instanceof RipTireEntity t && t.isAlive() ? t : null;
	}

	/** Every mine, trap, frag and tire of his, in every world: gone (the tire's ult ended with it, no blast). */
	private static void clearWorld(ServerPlayer player, HeroState s, JunkratState j) {
		// the ult over first: its tire forgotten without it, ultActive stayed set and his charge didn't build for the rest of the
		// 10 s (the critic, 2026-11-13: Q, then the Nether, ult 0 for 12 s)
		endTire(player, s, j, null);
		UUID me = player.getUUID();
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity e : level.getAllEntities()) {
				if (e instanceof FragEntity || e instanceof ConcussionMineEntity || e instanceof SteelTrapEntity) {
					if (e instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() != null && me.equals(p.getOwner().getUUID())) {
						e.discard();
					}
				} else if (e instanceof RipTireEntity t && t.ownerId() == player.getId()) {
					t.discard();
				}
			}
		}
		j.mine = null;
		j.trap = null;
		j.tire = null;
	}

	// ---- aim ----

	private static Vec3 sideOf(ServerPlayer player) {
		Vec3 side = player.getViewVector(1.0f).cross(new Vec3(0, 1, 0));
		return side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
	}

	/** The launcher's mouth in his look's frame, or his eyes when a wall is between. */
	private static Vec3 muzzle(ServerPlayer player) {
		return offset(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
	}

	/** His left hand, where the mine leaves from (D hp 000B; ours the place). */
	private static Vec3 leftHand(ServerPlayer player) {
		return offset(player, -0.3, -0.2, 0.4);
	}

	private static Vec3 offset(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = sideOf(player);
		Vec3 top = side.cross(look).normalize();
		Vec3 from = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		if (player.level().clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return from;
	}

	/** What his crosshair is on (a creature first, then a block), else {@code range} m out (Pharah's way). */
	private static Vec3 aimPoint(ServerPlayer player, double range) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(range));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		net.minecraft.world.phys.EntityHitResult onIt = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,
			player, eye, target, new AABB(eye, target).inflate(1.0), e -> e != player && e.isPickable() && !e.isSpectator()
				&& !(e instanceof dev.overcraft.entity.OwProjectile) && !(e instanceof RipTireEntity), 0.0f);
		if (onIt != null) {
			target = onIt.getLocation();
		}
		return target.distanceTo(eye) < 1.5 ? far : target;
	}

	// ---- sync and leaving play ----

	@Override
	public HeroSyncPayload sync(HeroState s) {
		JunkratState j = s.junkrat;
		boolean tire = j.tire != null;
		int mode = (tire ? MODE_TIRE : 0) | (j.mine != null ? MODE_MINE_OUT : 0) | (j.silenced ? MODE_SILENCED : 0)
			| (j.trap != null ? MODE_TRAP_OUT : 0) | (tire && j.nitroUsed ? MODE_NITRO : 0);
		// for the tire's prompts (ReticleHud: 0233 #658 / #675): armed from 1.6 s (tireArmed), Nitro Boost's press open from 1.6 s too
		int age = tire ? Ow.ticks(TIRE_TIME) - j.tireLeft : 0;
		if (tire && age >= Ow.ticks(TIRE_ARM)) {
			mode |= MODE_TIRE_ARMED;
		}
		if (tire && s.has(Perk.JUNKRAT_NITRO_BOOST) && !j.nitroUsed && age >= Ow.ticks(NITRO_FROM)) {
			mode |= MODE_NITRO_READY;
		}
		float rechargeLeft = j.mineCharges >= MINE_CHARGES ? 0 : (float) ((Ow.ticks(MINE_RECHARGE) - j.mineRecharge) / 20.0);
		float recharge = j.mineCharges >= MINE_CHARGES ? -1 : (float) (j.mineRecharge / Ow.ticks(MINE_RECHARGE));
		float trapLeft = j.trapCooldownIn >= 0 ? (float) TRAP_COOLDOWN : (float) (s.ability2Cooldown / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, tire,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / reloadTicks(j.reloadFast)) : -1,
			j.mineCharges, recharge, j.mineCharges > 0 ? 0 : rechargeLeft, trapLeft, s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), rechargeLeft, mode,
			tire ? (float) Math.max(0, j.tireLeft / 20.0) : 0, 0);
	}

	/**
	 * Out of play (#100): the throw is cut, the tire goes without a blast (ours: W says his death doesn't stop it, but nothing of the
	 * hero carries past it here), Bomb Voyage and Specialist's bonus end. His mine and trap stay through G and death and go with him
	 * (forGood). Dying in hero mode drops Total Mayhem's bombs.
	 */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		JunkratState j = s.junkrat;
		j.mineCast = -1;
		RipTireEntity tire = tire(player, j);
		endTire(player, s, j, tire);
		j.voyageUntil = -1000;
		j.specialistUntil = -1000;
		j.reloadFast = false;
		if (why == HeroKit.OutOfPlay.DEATH && HeroManager.heroMode(player)) {
			mayhem(player);
		}
		if (why.forGood()) {
			clearWorld(player, s, j);
		}
	}
}
