package dev.overcraft.pharah;

import dev.overcraft.Overcraft;
import dev.overcraft.Ow;
import dev.overcraft.entity.PharahRocketEntity;
import dev.overcraft.entity.PharahRocketEntity.Kind;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Barriers;
import dev.overcraft.hero.Blow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Effects;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Shields;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Voice;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.pharah.PharahStats.*;

/**
 * Pharah's kit (#70; numbers in {@link PharahStats}, the data map tools/reference/pharah-map.md). Her buttons by her data's ids
 * (pharah-map.md "Loadouts"): the left button the Rocket Launcher, the right Jet Dash, Shift Jump Jet, E Concussive Blast, Q
 * Barrage, Jump held Hover Jets. The server owns her fuel, cooldowns and shots; her client moves her (ClientPharah: the hover's
 * push and the jets' launches, OwMovement.pharahHover), told by the synced mode, and says when she hovers (HELD_HOVER).
 */
public final class Pharah implements HeroKit {
	/** The synced mode's bits: a jet is going (her client launches her on its first tick), Barrage, hacked. */
	public static final int MODE_JUMP_JET = 1, MODE_JET_DASH = 2, MODE_BARRAGE = 4, MODE_SILENCED = 8;
	/** HeroManager.PHARAH_STATE's bits, for every client's look of her: hovering, a jet, Barrage. */
	public static final int LOOK_HOVER = 1, LOOK_JET = 2, LOOK_BARRAGE = 4;
	/** Which jet (HeroState.pharahJetKind). */
	public static final int JUMP_JET = 1, JET_DASH = 2;
	/**
	 * OwMovement.pharahHover's answers: none, the fuelled hover (the ease to 4.66), no fuel (a slowed fall), and the same two in
	 * a jet with Jump held (no fuel spent).
	 */
	public static final int HOVER_NONE = 0, HOVER_FUEL = 1, HOVER_EMPTY = 2, HOVER_JET = 3, HOVER_JET_EMPTY = 4;

	static final Blow ROCKET_BLOW = Blow.weapon(false).by(Hero.PHARAH);
	static final Blow CONCUSSIVE_BLOW = Blow.ability("concussive_blast").by(Hero.PHARAH);
	static final Blow BARRAGE_BLOW = Blow.ult("barrage").by(Hero.PHARAH);
	static final Blow SALVO_BLOW = Blow.perk("rocket_salvo").by(Hero.PHARAH);
	private static final Identifier BARRAGE_SLOWED = Overcraft.id("pharah_barrage_slow");

	/**
	 * One tick of her hover: her new vertical speed (blocks a tick) from {@code vy} before it and {@code fallen}, the plain
	 * gravity step's. Fuelled, vy eases toward 4.66 m/s at 6 a second, exactly over the tick, and never below the gravity step
	 * (WS 2026-11-11: PharahStats.HOVER_RISE / HOVER_EASE). With no fuel, the 7.5 m/s² push and the 8 m/s fall (D #655 / 1E54).
	 */
	public static double hoverStep(int hover, double vy, double fallen) {
		if (hoverFast(hover)) {
			double rise = Ow.speed(HOVER_RISE);
			return Math.max(fallen, rise + (vy - rise) * Math.exp(-HOVER_EASE * 0.05));
		}
		return Math.max(fallen + Ow.accel(EMPTY_PUSH), -Ow.speed(EMPTY_FALL));
	}

	/** Whether this hover answer is the fuelled one, +30 % speed (D 0025 #160: while she has fuel). */
	public static boolean hoverFast(int hover) {
		return hover == HOVER_FUEL || hover == HOVER_JET;
	}

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.primaryAmmo = AMMO;
		s.primaryReload = -1;
		s.pharahRecovery = 0;
		s.pharahFuel = FUEL_MAX;
		s.pharahJet = -1;
		s.pharahJetFuel = 0;
		s.pharahConcussive = -1;
		s.pharahBarrage = -1;
		s.pharahSalvoUntil = -1;
		s.pharahSalvoDue = -1;
		s.pharahHovering = false;
		s.ability3Cooldown = 0;
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		s.pharahSilenced = dev.overcraft.sombra.Hack.silenced(player);
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		s.ability3Cooldown = Math.max(0, s.ability3Cooldown - rate);
		s.pharahRecovery = Math.max(0, s.pharahRecovery - Combat.attackRate(player));
		boolean able = controls && player.isAlive() && !Sleep.asleep(player);
		if (!able && s.pharahHovering) {
			// asleep, stunned, G, dead: the hover is over here, not when her client next sends its input (it sends only a change,
			// so the flag stayed and the fuel drained on: critic_1 #1). Her client stops asking while she can't act and asks
			// again once she can.
			hovered(player, s, false);
		}
		jets(player, s, able);
		fuel(player, s, now);
		concussive(player, s, able);
		barrage(player, s, able, now);
		launcher(player, s, able, now);
		salvo(player, s, now);
		helix(player, s);
		// a jet, the hover and her own blasts land her unhurt (CLAUDE.md, #35): her fall doesn't count until she's down after
		// them (as D.Va's Boosters: the server forgets it each tick while it holds)
		boolean moving = s.pharahJet >= 0 || s.pharahHovering;
		HeroManager.softLanding(player, s, moving, false);
		if (s.softLanding) {
			player.resetFallDistance();
		}
		// the right button's press (Jet Dash), from its held state
		boolean right = s.secondaryHeld;
		if (right && !s.pharahRightWas && able) {
			jetDash(player, s);
		}
		s.pharahRightWas = right;
		int look = (s.pharahHovering ? LOOK_HOVER : 0) | (s.pharahJet >= 0 ? LOOK_JET : 0) | (s.pharahBarrage >= 0 ? LOOK_BARRAGE : 0);
		if (!java.util.Objects.equals(player.getAttached(HeroManager.PHARAH_STATE), look)) {
			player.setAttached(HeroManager.PHARAH_STATE, look);
		}
	}

	/** Her client's hover this tick (HeroInputPayload.HELD_HOVER), from HeroManager.onInput. */
	public static void hovered(ServerPlayer player, HeroState s, boolean hovering) {
		if (hovering && !s.pharahHovering) {
			PharahSounds.split(player, "hover.start", player.position(), 40);
		} else if (!hovering && s.pharahHovering) {
			PharahSounds.split(player, "hover.end", player.position(), 40);
		}
		s.pharahHovering = hovering;
	}

	// ---- Hover Jets' fuel (0025) ----

	/** The fuel's most: 150, 200 with Fuel Stores (D #730). */
	public static double fuelCap(HeroState s) {
		return s.has(Perk.PHARAH_FUEL_STORES) ? OVERFUEL_MAX_STORES : OVERFUEL_MAX;
	}

	private static void fuel(ServerPlayer player, HeroState s, long now) {
		if (s.pharahBarrage >= 0) {
			// Barrage holds it at its most (D 0025 #634)
			s.pharahFuel = Math.max(s.pharahFuel, FUEL_MAX);
			return;
		}
		if (s.pharahJet >= 0 && s.pharahJetFuel > 0) {
			// a jet's +50 over its 0.85 s, into overfuel (D #764 / #762)
			double add = Math.min(s.pharahJetFuel, JET_FUEL / JET_TIME / 20);
			s.pharahJetFuel -= add;
			s.pharahFuel = Math.min(fuelCap(s), s.pharahFuel + add);
			if (s.pharahFuel > FUEL_MAX) {
				s.pharahOverfuelAt = now;
			}
		}
		if (s.pharahHovering && s.pharahJet < 0) {
			// 50 a second (D 1A6A); overfuel goes at 80 (D #688)
			double drain = (s.pharahFuel > FUEL_MAX ? OVERFUEL_DECAY_HOVER : FUEL_DRAIN) / 20;
			if (s.pharahFuel > 0 && s.pharahFuel <= drain) {
				// the drain runs out: 0025 #30 StateChaseVar onFinished -> #724 effect 5939 (B050, pharah_sfx.txt)
				PharahSounds.all(player.level(), "hover.empty", player.position(), 40);
			}
			s.pharahFuel = Math.max(0, s.pharahFuel - drain);
		} else if (s.pharahFuel > FUEL_MAX && now - s.pharahOverfuelAt > Ow.ticks(OVERFUEL_HOLD)) {
			s.pharahFuel = Math.max(FUEL_MAX, s.pharahFuel - OVERFUEL_DECAY / 20);
		}
		if (!player.onGround() || s.pharahHovering) {
			s.pharahAirAt = now;
		} else if (now - s.pharahAirAt >= Ow.ticks(FUEL_REGEN_DELAY) && s.pharahFuel < FUEL_MAX) {
			// back on the ground only, after 0.3 s (D 1E4F / 1E52)
			s.pharahFuel = Math.min(FUEL_MAX, s.pharahFuel + FUEL_REGEN / 20);
		}
	}

	// ---- Jump Jet (0027) and Jet Dash (4B92) ----

	private static void jets(ServerPlayer player, HeroState s, boolean able) {
		if (s.pharahJet < 0) {
			return;
		}
		if (!able || ++s.pharahJet >= Ow.ticks(JET_TIME)) {
			endJet(player, s);
		}
	}

	private static void endJet(ServerPlayer player, HeroState s) {
		// the cooldown from its end (D 0027 #379 / 4B92 #523)
		if (s.pharahJetKind == JUMP_JET) {
			s.ability1Cooldown = Ow.ticks(JUMP_JET_COOLDOWN);
		} else {
			s.ability3Cooldown = Ow.ticks(JET_DASH_COOLDOWN);
		}
		s.pharahJet = -1;
		s.pharahJetFuel = 0;
	}

	private static boolean canJet(ServerPlayer player, HeroState s) {
		// (not in Barrage, not in water: Minecraft swims her there; not seated)
		return s.pharahBarrage < 0 && !player.isInWater() && !player.isPassenger();
	}

	private static void jumpJet(ServerPlayer player, HeroState s) {
		if (s.ability1Cooldown > 0 || s.pharahJet >= 0 && s.pharahJetKind == JUMP_JET || !canJet(player, s)) {
			return;
		}
		if (s.pharahJet >= 0) {
			endJet(player, s);
		}
		startJet(player, s, JUMP_JET, JET_FUEL);
		Combat.animate(player, "jump_jet");
		PharahSounds.split(player, "jump_jet", player.position(), 60);
		Voice.say(player, "jump_jet", 1);
	}

	private static void jetDash(ServerPlayer player, HeroState s) {
		if (s.ability3Cooldown > 0 || s.pharahJet >= 0 && s.pharahJetKind == JET_DASH || !canJet(player, s)) {
			return;
		}
		if (s.pharahJet >= 0) {
			endJet(player, s);
		}
		// no fuel of its own; Fuel Stores gives it 50 (D 0025 #758)
		startJet(player, s, JET_DASH, s.has(Perk.PHARAH_FUEL_STORES) ? STORES_FUEL : 0);
		// the same clips as Jump Jet (D 4B92 #65 / #346 / #509)
		Combat.animate(player, "jump_jet");
		PharahSounds.split(player, "jet_dash", player.position(), 60);
		Voice.say(player, "jet_dash", 1);
	}

	private static void startJet(ServerPlayer player, HeroState s, int kind, double fuel) {
		s.pharahJet = 0;
		s.pharahJetKind = kind;
		s.pharahJetFuel = fuel;
		s.softLanding = true;
		// a jet cuts a reload (D, H nodes, M meaning): 0027 #82 and 4B92 #82 (StateAbility) push the shared stack v33 at
		// priority 0003.024, over the reload's StateStack 0023 #60 at 0002.024, whose under plug #61 sets v51 = 0 (the reload
		// over, its ammo at 0.6 s not given). The order 3 over 2 is read as Sojourn's 0x79 over 0x2 (sojourn.md: the 024
		// assets' order itself isn't read). Rockets already in at 0.6 s stay.
		s.interruptReload(0);
		if (s.has(Perk.PHARAH_ROCKET_SALVO)) {
			// a movement ability arms the next rocket's salvo (D 0027 #367 / 4B92 #108: message 1E66)
			s.pharahSalvoUntil = player.level().getGameTime() + Ow.ticks(SALVO_WINDOW);
		}
	}

	// ---- Rocket Launcher (0023) ----

	private static void launcher(ServerPlayer player, HeroState s, boolean able, long now) {
		if (!able) {
			return;
		}
		if (s.primaryReload >= 0) {
			s.primaryReload += Combat.attackRate(player);
			if (s.primaryReload >= Ow.ticks(RELOAD * RELOAD_AMMO_AT / RELOAD) && s.primaryAmmo < AMMO) {
				s.primaryAmmo = AMMO;
			}
			if (s.primaryReload >= Ow.ticks(RELOAD)) {
				s.primaryReload = -1;
			}
			return;
		}
		if (s.primaryAmmo <= 0 && s.pharahRecovery <= 0) {
			// the auto-reload after the last rocket's 0.55 s (D #4)
			startReload(player, s);
			return;
		}
		if (!s.primaryHeld || s.primaryAmmo <= 0 || s.pharahRecovery > 0 || s.pharahBarrage >= 0 || s.pharahConcussive >= 0) {
			return;
		}
		s.primaryAmmo--;
		s.pharahRecovery = Ow.ticks(s.primaryAmmo <= 0 ? RECOVERY_LAST : RECOVERY);
		Vec3 aim = aimPoint(player, 200);
		Vec3 from = muzzle(player, MUZZLE_RIGHT, MUZZLE_UP, MUZZLE_AHEAD);
		shoot(player, Kind.ROCKET, from, aim.subtract(from), ROCKET_SPEED);
		Combat.animate(player, "fire");
		PharahSounds.split(player, "rocket.fire", from, 80);
		if (s.pharahSalvoUntil >= now) {
			// Rocket Salvo: 0.1 s after this rocket, two mini-rockets (D 51E9 #77)
			s.pharahSalvoUntil = -1;
			s.pharahSalvoDue = Ow.ticks(SALVO_DELAY);
			s.pharahSalvoAim = aim;
		}
	}

	private static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= AMMO || s.pharahBarrage >= 0) {
			return;
		}
		s.primaryReload = 0;
		Combat.animate(player, "reload");
		PharahSounds.split(player, "reload", player.getEyePosition(), 30);
	}

	private static void salvo(ServerPlayer player, HeroState s, long now) {
		if (s.pharahSalvoDue < 0 || --s.pharahSalvoDue > 0) {
			return;
		}
		s.pharahSalvoDue = -1;
		Vec3 aim = s.pharahSalvoAim != null ? s.pharahSalvoAim : aimPoint(player, 200);
		for (int i = 0; i < SALVO_COUNT; i++) {
			double side = (i == 0 ? -1 : 1) * SALVO_APART / 2;
			Vec3 from = muzzle(player, MUZZLE_RIGHT + side, MUZZLE_UP, MUZZLE_AHEAD);
			shoot(player, Kind.SALVO, from, aim.add(sideOf(player).scale(side)).subtract(from), SALVO_SPEED);
		}
		PharahSounds.split(player, "perk.salvo", player.getEyePosition(), 60);
	}

	// ---- Concussive Blast (007B) ----

	private static void concussive(ServerPlayer player, HeroState s, boolean able) {
		if (s.pharahConcussive < 0) {
			return;
		}
		if (!able) {
			// G, death, a stun in the cast: no blast, nothing spent (ours)
			s.pharahConcussive = -1;
			return;
		}
		if (++s.pharahConcussive >= Ow.ticks(CONCUSSIVE_CAST)) {
			s.pharahConcussive = -1;
			// it leaves 0.167 s in, then 0.5 s before a rocket (D #257 / #61); the cooldown from the recovery's end (WS 2026-11-11:
			// "in use" 0.66 s, then 8 s: CONCUSSIVE_COOLDOWN)
			s.ability2Cooldown = Ow.ticks(CONCUSSIVE_RECOVERY + CONCUSSIVE_COOLDOWN);
			s.pharahRecovery = Math.max(s.pharahRecovery, Ow.ticks(CONCUSSIVE_RECOVERY));
			Vec3 aim = aimPoint(player, 300);
			Vec3 from = muzzle(player, -CONCUSSIVE_LEFT, CONCUSSIVE_UP_OFF, CONCUSSIVE_AHEAD);
			shoot(player, Kind.CONCUSSIVE, from, aim.subtract(from), CONCUSSIVE_SPEED);
		}
	}

	// ---- Barrage (002B) ----

	private static int barrageTicks() {
		return Ow.ticks(BARRAGE_TIME);
	}

	private static void barrage(ServerPlayer player, HeroState s, boolean able, long now) {
		// A jet under way runs on through the cast (D, M): nothing 002B sends reaches 0027 / 4B92 (their message entries are
		// 0093, a kill, which 002B also only receives, 21E2, 2623 and 1BB5), and 002B #24 / #137 push v33 at 0003.024, the
		// jets' own priority, not over it. So the jet's 0.85 s, its fuel and its cooldown from its end are kept.
		if (s.pharahBarrage < 0) {
			return;
		}
		if (!able || dev.overcraft.sombra.Hack.silenced(player) || ++s.pharahBarrage > barrageTicks()) {
			// a stun, a hack or the end: over, its charge spent (W: knocked down, stunned or hacked ends it)
			endBarrage(player, s);
			return;
		}
		s.pharahBarrageClock += BARRAGE_RATE / 20.0;
		Vec3 aim = aimPoint(player, 200);
		while (s.pharahBarrageClock >= 1) {
			s.pharahBarrageClock -= 1;
			double[] pod = BARRAGE_PODS[s.pharahPod++ % BARRAGE_PODS.length];
			Vec3 from = pod(player, pod);
			Vec3 dir = spread(aim.subtract(from).normalize(), BARRAGE_SPREAD, player.getRandom());
			shoot(player, Kind.BARRAGE, from, dir, BARRAGE_SPEED);
		}
		if (s.pharahBarrage % 4 == 0) {
			PharahSounds.split(player, "barrage.fire", player.getEyePosition(), 100);
		}
	}

	private static void endBarrage(ServerPlayer player, HeroState s) {
		if (s.pharahBarrage < 0) {
			return;
		}
		s.pharahBarrage = -1;
		s.ultActive = 0;
		slow(player, false);
		PharahSounds.split(player, "barrage.end", player.getEyePosition(), 100);
	}

	private static void slow(ServerPlayer player, boolean on) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (!on) {
			speed.removeModifier(BARRAGE_SLOWED);
		} else if (!speed.hasModifier(BARRAGE_SLOWED)) {
			speed.addTransientModifier(new AttributeModifier(BARRAGE_SLOWED, -BARRAGE_SLOW, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		boolean busy = s.pharahBarrage >= 0 || Sleep.asleep(player);
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && !busy) {
			jumpJet(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && !busy && s.ability2Cooldown <= 0 && s.pharahConcussive < 0) {
			s.pharahConcussive = 0;
			s.interruptReload(CONCUSSIVE_CAST + CONCUSSIVE_RECOVERY);
			Combat.animate(player, "concussive");
			PharahSounds.split(player, "concussive.cast", player.getEyePosition(), 60);
			Voice.say(player, "concussive", 1);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && !busy && s.ultCharge >= ULT_COST) {
			s.ultCharge = Combat.afterUlt(Hero.PHARAH, s.ultCharge);
			s.pharahBarrage = 0;
			s.pharahBarrageClock = 1;
			s.ultActive = barrageTicks();
			s.pharahConcussive = -1;
			// her rockets back on the cast (D #149 -> 0023 #32), her fuel held full (0025 #634)
			s.primaryAmmo = AMMO;
			s.primaryReload = -1;
			s.pharahFuel = Math.max(s.pharahFuel, FUEL_MAX);
			slow(player, true);
			Combat.animate(player, "barrage");
			PharahSounds.split(player, "barrage.cast", player.getEyePosition(), 100);
			dev.overcraft.hero.HeroSounds.ultLine(player, PharahSounds.of("ult.friendly"), PharahSounds.of("ult.enemy"), 5);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.pharahBarrage < 0) {
			startReload(player, s);
		}
	}

	// ---- shots ----

	private static Vec3 sideOf(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = look.cross(new Vec3(0, 1, 0));
		return side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
	}

	/** A point from her eyes in her look's frame (right, up, ahead), or her eyes when a wall is between. */
	private static Vec3 muzzle(ServerPlayer player, double right, double up, double ahead) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		Vec3 side = sideOf(player);
		Vec3 top = side.cross(look).normalize();
		Vec3 from = eye.add(side.scale(right)).add(top.scale(up)).add(look.scale(ahead));
		ServerLevel level = player.level();
		if (level.clip(new ClipContext(eye, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return from;
	}

	/** A Barrage pod (OW axes, from her feet: x her left, y up, z ahead) in the world, by her facing. */
	private static Vec3 pod(ServerPlayer player, double[] pod) {
		double yaw = Math.toRadians(player.getYRot());
		Vec3 ahead = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3 at = player.position().add(left.scale(pod[0])).add(0, pod[1], 0).add(ahead.scale(pod[2] + 0.3));
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		if (level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
			return eye;
		}
		return at;
	}

	/** What her crosshair is on (a creature first, then a block), else {@code range} m out. */
	private static Vec3 aimPoint(ServerPlayer player, double range) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 far = eye.add(player.getViewVector(1.0f).scale(range));
		Vec3 target = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
		net.minecraft.world.phys.EntityHitResult onIt = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,
			player, eye, target, new AABB(eye, target).inflate(1.0), e -> e != player && e.isPickable() && !e.isSpectator()
				&& !(e instanceof PharahRocketEntity), 0.0f);
		if (onIt != null) {
			target = onIt.getLocation();
		}
		return target.distanceTo(eye) < 1.5 ? far : target;
	}

	/** {@code dir} turned at random up to {@code degrees} off (uniform over the cone's face: ours, the data's spread shape L). */
	private static Vec3 spread(Vec3 dir, double degrees, net.minecraft.util.RandomSource random) {
		Vec3 a = dir.cross(new Vec3(0, 1, 0));
		a = a.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : a.normalize();
		Vec3 b = a.cross(dir).normalize();
		double r = Math.tan(Math.toRadians(degrees)) * Math.sqrt(random.nextDouble());
		double t = random.nextDouble() * Math.PI * 2;
		return dir.add(a.scale(r * Math.cos(t))).add(b.scale(r * Math.sin(t))).normalize();
	}

	private static void shoot(ServerPlayer player, Kind kind, Vec3 from, Vec3 dir, double speed) {
		PharahRocketEntity rocket = new PharahRocketEntity(player, player.level(), kind);
		rocket.launch(from, dir, Ow.speed(speed));
		player.level().addFreshEntity(rocket);
	}

	// ---- bursts ----

	/** A two-ring splash's share at {@code d} m: full to {@code full}, down to {@code edge} at {@code radius}, none past (linear: M). */
	public static double splashFactor(double d, double full, double radius, double edge) {
		if (d > radius) {
			return 0;
		}
		if (d <= full) {
			return 1;
		}
		return 1 - (d - full) / (radius - full) * (1 - edge);
	}

	/** A rocket's push share at {@code d} m: x0.8 to 0.5 m, then down to 0 at 2.5 m (D 1D9D / 1D9E: M linear). */
	public static double rocketKnockFactor(double d) {
		if (d > ROCKET_SPLASH_RADIUS) {
			return 0;
		}
		if (d <= ROCKET_SPLASH_FULL) {
			return ROCKET_KNOCK_RING;
		}
		return ROCKET_KNOCK_RING * (1 - (d - ROCKET_SPLASH_FULL) / (ROCKET_SPLASH_RADIUS - ROCKET_SPLASH_FULL));
	}

	/**
	 * A projectile of hers bursts at {@code at} ({@code direct}: what it hit). Rockets, Barrage's and Salvo's: their direct hit,
	 * then splash on every enemy and herself in sight within their radius (to the box's nearest point), herself at her share;
	 * the launcher's rockets push (herself the most). Concussive Blast: 30 to what it hits, then its 8 m push on enemies and her
	 * (Concussive Force: its splash too).
	 */
	public static void burst(ServerPlayer owner, PharahRocketEntity rocket, Vec3 at, @Nullable Entity direct) {
		ServerLevel level = owner.level();
		HeroState s = HeroManager.state(owner);
		Kind kind = rocket.kind();
		EnderDragonPart part = direct instanceof EnderDragonPart p ? p : null;
		LivingEntity target = part != null ? part.parentMob : direct instanceof LivingEntity living ? living : null;
		double directDamage = switch (kind) {
			case ROCKET -> ROCKET_DIRECT;
			case BARRAGE -> BARRAGE_DIRECT;
			case SALVO -> SALVO_DIRECT;
			case CONCUSSIVE -> CONCUSSIVE_DIRECT;
		};
		Blow blow = switch (kind) {
			case ROCKET -> ROCKET_BLOW;
			case BARRAGE -> BARRAGE_BLOW;
			case SALVO -> SALVO_BLOW;
			case CONCUSSIVE -> CONCUSSIVE_BLOW;
		};
		if (target != null && Allies.canHurt(owner, target)) {
			DamageSource hit = level.damageSources().thrown(rocket, owner);
			float dealt = part != null ? Combat.damagePart(level, part, hit, directDamage, blow) : Combat.damage(target, hit, directDamage, blow);
			dealt(owner, dealt, target);
			if (kind == Kind.ROCKET && s != null && s.has(Perk.PHARAH_HELIX_SHIELDS)) {
				// Helix Shields: a direct rocket hit starts the shields' regeneration (D 51E9 #361 / #366, M)
				s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(owner) - Ow.ticks(Shields.DELAY));
			}
		} else if (direct != null && !(direct instanceof LivingEntity) && part == null) {
			direct.hurtServer(level, level.damageSources().thrown(rocket, owner), Ow.hp(directDamage));
		}
		if (kind == Kind.CONCUSSIVE) {
			concussiveBurst(owner, s, rocket, at, target);
			return;
		}
		double splash = kind == Kind.ROCKET ? ROCKET_SPLASH : kind == Kind.BARRAGE ? BARRAGE_SPLASH : SALVO_SPLASH;
		double full = kind == Kind.ROCKET ? ROCKET_SPLASH_FULL : BARRAGE_FULL;
		double radius = kind == Kind.ROCKET ? ROCKET_SPLASH_RADIUS : BARRAGE_RADIUS;
		double edge = kind == Kind.ROCKET ? ROCKET_SPLASH_EDGE : BARRAGE_EDGE;
		double self = kind == Kind.BARRAGE ? BARRAGE_SELF : ROCKET_SELF;
		for (Barriers.Shape b : Barriers.enemyBarriers(level, owner)) {
			Vec3 near = Barriers.nearest(b, at);
			if (near.distanceTo(at) <= radius) {
				b.barrierTake(splash * splashFactor(near.distanceTo(at), full, radius, edge), near);
			}
		}
		dev.overcraft.brigitte.Barrier.burst(level, owner, at, radius, d -> splash * splashFactor(d, full, radius, edge));
		DamageSource source = dev.overcraft.hero.Combat.blast(level, rocket, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 2))) {
			boolean me = e == owner;
			if (!e.isAlive() || !me && !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			if (d > radius || !inSight(level, at, e) || !me && (Barriers.between(level, at, e.getBoundingBox().getCenter(), owner) != null
				|| dev.overcraft.brigitte.Barrier.shields(e, owner, at))) {
				continue;
			}
			float dealt = Combat.damage(e, source, splash * splashFactor(d, full, radius, edge) * (me ? self : 1), blow);
			if (kind == Kind.ROCKET) {
				// the launcher's push, away from the burst and up (D 1D9B / 1D9C), x1.875 on herself (D 0B69)
				Vec3 away = e.getBoundingBox().getCenter().subtract(at);
				away = new Vec3(away.x, 0, away.z);
				away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize();
				double k = rocketKnockFactor(d) * (me ? ROCKET_KNOCK_SELF : 1);
				Combat.push(e, away.scale(Ow.speed(ROCKET_KNOCK_AWAY * k)).add(0, Ow.speed(ROCKET_KNOCK_UP * k), 0), owner, blow);
				// soft only when it leaves her going up (a push that just slows a fall is still a fall: HeroManager.pushLandsSoft)
				if (me && s != null && HeroManager.pushLandsSoft(owner.getDeltaMovement().y)) {
					s.softLanding = true;
				}
			}
			if (!me) {
				dealt(owner, dealt, e);
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		if (kind != Kind.BARRAGE) {
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.02);
		}
		PharahSounds.all(level, kind == Kind.BARRAGE ? "barrage.burst" : "rocket.burst", at, 150);
		if (target != null && kind == Kind.ROCKET) {
			PharahSounds.split(owner, "rocket.hit_enemy", at, 100);
		} else if (direct == null && kind == Kind.ROCKET) {
			PharahSounds.all(level, "rocket.hit_block", at, 60);
		}
	}

	private static void concussiveBurst(ServerPlayer owner, @Nullable HeroState s, PharahRocketEntity rocket, Vec3 at,
										@Nullable LivingEntity hitDirect) {
		ServerLevel level = owner.level();
		boolean force = s != null && s.has(Perk.PHARAH_CONCUSSIVE_FORCE);
		DamageSource source = dev.overcraft.hero.Combat.blast(level, rocket, owner);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(CONCUSSIVE_BLAST + 2))) {
			boolean me = e == owner;
			if (!e.isAlive() || !me && !Allies.areaHits(owner, e)) {
				continue;
			}
			double d = Math.sqrt(e.getBoundingBox().distanceToSqr(at));
			// any enemy barrier between, Brigitte's too (Barrier.shields asks Barriers.between first): one behind hers was
			// thrown (#104's sweep); Concussive Blast isn't among what pierces barriers (the wiki's Barrier page, rev 406358)
			if (d > CONCUSSIVE_BLAST || !inSight(level, at, e) || !me && dev.overcraft.brigitte.Barrier.shields(e, owner, at)) {
				continue;
			}
			if (force && !me) {
				// Concussive Force: 30 to 4 m, 15 at 8 m (D 51E9 #707 / #356)
				float dealt = Combat.damage(e, source, FORCE_SPLASH * splashFactor(d, FORCE_FULL, CONCUSSIVE_BLAST, FORCE_EDGE), CONCUSSIVE_BLOW);
				dealt(owner, dealt, e);
			}
			// 20 m/s away and 5 up anywhere in it, 10 / 3 more on a direct hit (D 1E2D / 1E2E, 1E32 / 1E33); her own all of it
			Vec3 away = e.getBoundingBox().getCenter().subtract(at);
			away = new Vec3(away.x, 0, away.z);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize();
			boolean onIt = e == hitDirect;
			double out = CONCUSSIVE_AWAY + (onIt ? CONCUSSIVE_DIRECT_AWAY : 0);
			double up = CONCUSSIVE_UP + (onIt ? CONCUSSIVE_DIRECT_UP : 0);
			Combat.push(e, away.scale(Ow.speed(out)).add(0, Ow.speed(up), 0), owner, CONCUSSIVE_BLOW);
			if (me && s != null && HeroManager.pushLandsSoft(owner.getDeltaMovement().y)) {
				s.softLanding = true;
			}
		}
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 30, 1.5, 1.0, 1.5, 0.15);
		PharahSounds.all(level, "concussive.burst", at, 150);
	}

	/** Her damage to {@code e}: the credit, the fire meter, the hit marker and Recon. */
	private static void dealt(ServerPlayer owner, float dealt, LivingEntity e) {
		Combat.credit(owner, dealt, e);
		Combat.addFireForDamage(owner, dealt, e);
		if (dealt > 0) {
			Effects.hitMarker(owner, e, false);
			recon(e);
		}
	}

	/**
	 * Recon (her sub-role, 0DAD): her damage on an enemy under half its health shows it through walls for 3.5 s (D 23D8; W
	 * Sub-Roles). Ours: Minecraft's glowing, which everyone sees (Overwatch shows it to her team only).
	 */
	static void recon(LivingEntity e) {
		if (e.isAlive() && e.getHealth() < e.getMaxHealth() * RECON_BELOW) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, Ow.ticks(RECON_TIME), 0, false, false));
		}
	}

	static boolean inSight(ServerLevel level, Vec3 at, LivingEntity e) {
		for (Vec3 to : new Vec3[] {e.getBoundingBox().getCenter(), e.getEyePosition()}) {
			if (level.clip(new ClipContext(at, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS) {
				return true;
			}
		}
		return false;
	}

	// ---- Helix Shields ----

	/** With Helix Shields picked mid-life, the 125 health over the rest that she has are shields now (as D.Va's Shield System). */
	private static void helix(ServerPlayer player, HeroState s) {
		if (!s.has(Perk.PHARAH_HELIX_SHIELDS)) {
			return;
		}
		double health = Ow.toOw(player.getHealth()) - s.shield;
		double healthMax = s.owHealth(player) - s.layer(player);
		if (health > healthMax + 0.01) {
			s.shield = Math.min(s.layer(player), s.shield + health - healthMax);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		int mode = (s.pharahJet >= 0 && s.pharahJetKind == JUMP_JET ? MODE_JUMP_JET : 0)
			| (s.pharahJet >= 0 && s.pharahJetKind == JET_DASH ? MODE_JET_DASH : 0)
			| (s.pharahBarrage >= 0 ? MODE_BARRAGE : 0) | (s.pharahSilenced ? MODE_SILENCED : 0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, s.pharahBarrage >= 0,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1, -1, -1,
			(float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero, s.onFire,
			Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk),
			(float) (s.ability3Cooldown / 20.0), mode,
			s.pharahBarrage >= 0 ? (float) ((barrageTicks() - s.pharahBarrage) / 20.0) : 0, (float) (s.pharahFuel / FUEL_MAX));
	}

	/** Out of play (#100): a jet (its cooldown starts), the cast, Barrage (its charge spent) and the hover end. */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, HeroKit.OutOfPlay why) {
		if (s.pharahJet >= 0) {
			endJet(player, s);
		}
		s.pharahConcussive = -1;
		s.pharahSalvoDue = -1;
		endBarrage(player, s);
		s.pharahHovering = false;
		// G mid-fall: a plain Minecraft fall from here (the hover's soft landing doesn't carry into Minecraft mode)
		s.softLanding = false;
		player.setAttached(HeroManager.PHARAH_STATE, 0);
	}
}
