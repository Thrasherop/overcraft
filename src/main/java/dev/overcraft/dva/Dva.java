package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.entity.DvaMechEntity;
import dev.overcraft.hero.Allies;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.Hero;
import dev.overcraft.hero.HeroBody;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.hero.Sleep;
import dev.overcraft.hero.Tank;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.dva.DvaStats.*;

/**
 * D.Va's kit (tools/reference/dva-plan.md "Order"), built step by step, each played before the next. Now: the two bodies as
 * one player (steps 1-3): her forms, each with its own health, armor, ult meter, body box, eye, model and HUD; the mech's
 * death caught as the wreck and the eject; Call Mech's drop and link.
 * <p>
 * In her data the hero is the pilot and the MEKA a pet entity she drives (D 0407; the link 026A #98: control and camera to the
 * mech, the pilot attached, hidden and untargetable). Overcraft's player <i>is</i> whichever body she's in (ours, M:
 * Minecraft's riding would split the hits, the camera and the input), and a {@link DvaMechEntity} stands for the mech only
 * while nobody drives it: falling in on Call Mech, the wreck after the eject.
 */
public final class Dva implements HeroKit {
	/** Her forms (HeroManager.DVA_FORM, synced to every client). */
	public static final int MECH = 0, PILOT = 1, CALLING = 2, WRECK = 3;
	/**
	 * The sync's mode: her form in the low bits ({@link #FORM_BITS}), then her mech kit's state for the HUD and her client: the
	 * Fusion Cannons firing, Defense Matrix up, Boosters flying (her client moves her: OwMovement.dvaBoosters).
	 */
	public static final int FORM_BITS = 7, MODE_FIRING = 8, MODE_MATRIX = 16, MODE_BOOSTING = 32, MODE_PRECISION = 64;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		// a fresh kit: the unmanned mech goes, but a Self-Destruct already flying still goes off (a respawn within its 3 s:
		// leavePlay's death rule; a switch away already took it with clear)
		removeMech(player, s);
		stopMechKit(player, s);
		// a fresh kit (picked, respawned, joined) is in her mech (D 026A #763: a respawn links at once, no drop)
		s.dvaForm = MECH;
		s.dvaTicks = 0;
		s.dvaMechUlt = 0;
		s.dvaStoredHealth = -1;
		s.dvaStoredArmor = 0;
		s.dvaCallLockUntil = -1;
		s.dvaImmuneUntil = -1;
		s.dvaEjectedAt = -1000;
		s.dvaInitiatorReadyAt = -1;
		s.dvaCallSpot = null;
		LightGun.refill(s);
		freshMech(s);
		player.setAttached(HeroManager.DVA_FORM, MECH);
		HeroManager.refreshHealth(player, s, true);
	}

	/** A new mech's kit (each call is a new pet entity with its own vars: D 0407, M): the matrix full, nothing cooling down. */
	static void freshMech(HeroState s) {
		s.dvaMatrix = false;
		s.dvaMatrixEnergy = MATRIX_ENERGY;
		s.dvaMatrixTicks = 0;
		s.dvaMatrixCooldown = 0;
		s.dvaMatrixDownAt = -1000;
		s.dvaBoost = -1;
		s.dvaBoostCooldownIn = -1;
		s.dvaBoostHit.clear();
		s.dvaMissileTicks = -1;
		s.dvaMissilesLeft = 0;
		s.ability1Cooldown = 0;
		s.ability2Cooldown = 0;
		s.weaponRecovery = 0;
	}

	/** Her mech kit stops at once (the wreck, G, another hero): the matrix down, the flight over, the slow off. */
	static void stopMechKit(ServerPlayer player, HeroState s) {
		DefenseMatrix.end(player, s, player.level().getGameTime());
		Boosters.end(player, s);
		MicroMissiles.stop(player, s);
		FusionCannons.slow(player, false);
		DvaPerks.end(player, s);
	}

	/** Leaving her (another hero, leaving the world): the unmanned mech goes. */
	public static void clear(ServerPlayer player, HeroState s) {
		removeMech(player, s);
		// a Self-Destruct still flying goes with her (another hero, leaving the world)
		if (s.dvaSelfDestructId != null && player.level().getEntity(s.dvaSelfDestructId) instanceof DvaMechEntity mech) {
			mech.discard();
		}
		s.dvaSelfDestructId = null;
		stopMechKit(player, s);
		player.removeAttached(HeroManager.DVA_FORM);
	}

	// ---- reading the form (both sides: the attachment is synced) ----

	/** Her form (MECH .. WRECK), or -1 when the player isn't D.Va. */
	public static int form(@Nullable Player player) {
		if (player == null) {
			return -1;
		}
		Integer form = player.getAttached(HeroManager.DVA_FORM);
		return form == null ? -1 : form;
	}

	/**
	 * Whether she's in the pilot's body: as the pilot, while calling a mech, and in Minecraft mode (G: the mech isn't
	 * there, the plan's default 9). Her health, box and model are the pilot's then.
	 */
	public static boolean pilotBody(@Nullable Player player) {
		int form = form(player);
		return form == PILOT || form == CALLING || form >= 0 && !HeroManager.heroMode(player);
	}

	/** The mech's body (its box, eye and model): in the mech or its wreck, in hero mode. */
	public static boolean mechBody(@Nullable Player player) {
		int form = form(player);
		return (form == MECH || form == WRECK) && HeroManager.heroMode(player);
	}

	/** Her health and armor in the body she's in: 675 (200 + 150 + 325) in the mech, 175 as the pilot. */
	public static int owHealth(@Nullable Player player) {
		return pilotBody(player) ? PILOT_HEALTH : Hero.DVA.owHealth;
	}

	public static int armor(@Nullable Player player) {
		return pilotBody(player) ? 0 : MECH_ARMOR;
	}

	/**
	 * The mech's Self-Destruct charge, whatever body she's in: the meter in the mech or its wreck, the kept one otherwise (D 026A
	 * #775: it carries to the next mech). A respawn and a rejoin put her in a mech, so they keep this one.
	 */
	public static double mechUlt(HeroState s) {
		return s.dvaForm == MECH || s.dvaForm == WRECK ? s.ultCharge : s.dvaMechUlt;
	}

	/** The current body's ult (Self-Destruct 1700 in the mech, Call Mech 280 as the pilot). */
	public static int ultCost(HeroState s) {
		return (int) (s.dvaForm == PILOT || s.dvaForm == CALLING ? PILOT_ULT_COST : MECH_ULT_COST);
	}

	/** She can't be hurt: the wreck, a call's last second, the 0.4 s after the eject (D 026A #880 / #351, M meanings). */
	public static boolean untargetable(Player player) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != Hero.DVA || !HeroManager.heroMode(player)) {
			return false;
		}
		long now = player.level().getGameTime();
		return s.dvaForm == WRECK || s.dvaForm == CALLING && s.dvaTicks >= callTick(CALL_UNTARGETABLE) || now < s.dvaImmuneUntil;
	}

	/** Her inputs are off (no moving, no jumping): the whole call (D 026A #105) and the wreck (D 0276 #79, frozen). */
	public static boolean held(@Nullable Player player) {
		int form = form(player);
		return (form == CALLING || form == WRECK) && player != null && HeroManager.heroMode(player);
	}

	// ---- the mech's death (LivingEntityMixin's setHealth: every loss passes here) ----

	/**
	 * A loss that would take the mech to 0 leaves it at a sliver and makes it the wreck instead: the mech's death is no
	 * death (W; D 0276 #55: the wreck, then the eject). Damage past its 0 doesn't reach the pilot (W; D: the eject is a new
	 * state, her health a fresh 175). Only the pilot's 0 is a real death.
	 */
	public static float catchDeath(ServerPlayer player, float health) {
		HeroState s = HeroManager.state(player);
		if (s == null || s.hero != Hero.DVA || health > 0 || s.dvaForm != MECH || !HeroManager.heroMode(player)
			|| !player.isAlive()) {
			return health;
		}
		s.dvaForm = WRECK;
		s.dvaTicks = 0;
		s.shield = 0;
		// MEKA destroyed (0276 #86 effect 1572) and its failing chirp (#143 1633)
		DvaSounds.split(player, "wreck", player.position().add(0, 1, 0), 70);
		DvaSounds.split(player, "wreck.chirp", player.position().add(0, 1, 0), 50);
		stopMechKit(player, s);
		player.setAttached(HeroManager.DVA_FORM, WRECK);
		return 0.01f;
	}

	// ---- the kit ----

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		long now = player.level().getGameTime();
		s.dvaNow = now;
		s.dvaTicks++;
		if (s.softLanding) {
			// the eject's launch and Boosters land her (no fall damage: CLAUDE.md #35). Her fall is forgotten each tick until
			// she's down, as Sigma's Flux does: the server saw her on the ground 2 m up two ticks before she landed (her
			// client's word comes ahead of her position), cleared this, and the landing counted the whole 12.8 m (found playing)
			player.resetFallDistance();
			if (player.onGround() && now - s.dvaEjectedAt > 2 && !Boosters.active(s) && now - s.dvaBoostEndedAt > 2) {
				s.softLanding = false;
			}
		}
		double rate = Combat.cooldownRate(player);
		if (!Boosters.active(s)) {
			s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		}
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		if (!HeroManager.heroMode(player)) {
			return;
		}
		// the mech's kit (steps 4-6): Defense Matrix first (no Fusion while it's up), Boosters, the Fusion Cannons
		boolean mech = s.dvaForm == MECH && controls && player.isAlive() && !Sleep.asleep(player);
		if (s.dvaMatrixLineAt >= 0 && now >= s.dvaMatrixLineAt) {
			s.dvaMatrixLineAt = -1;
			dev.overcraft.hero.Voice.say(player, "matrix_absorb", 1);
		}
		// her perks (step 10): Precision Fusion's time and cooldown, Shield System's shields
		DvaPerks.tickPrecision(player, s, mech);
		if (s.dvaForm == MECH) {
			DvaPerks.tick(player, s);
		}
		DefenseMatrix.tick(player, s, mech);
		// the held states others hear (client.DvaSounds)
		int held = (s.dvaMatrix ? MODE_MATRIX : 0) | (Boosters.active(s) ? MODE_BOOSTING : 0) | (DvaPerks.precision(s) ? MODE_PRECISION : 0);
		Integer was = player.getAttached(HeroManager.DVA_STATE);
		if (was == null || was != held) {
			player.setAttached(HeroManager.DVA_STATE, held);
		}
		Boosters.tick(player, s, mech);
		MicroMissiles.tick(player, s, mech);
		// the pilot's Light Gun (step 9)
		LightGun.tick(player, s, s.dvaForm == PILOT && controls && player.isAlive() && !Sleep.asleep(player));
		FusionCannons.tick(player, s, mech && !s.dvaMatrix, Boosters.active(s));
		switch (s.dvaForm) {
			case WRECK -> {
				// no passive ult meanwhile, and frozen where it fell (D 0276 #79)
				s.ultActive = Math.max(s.ultActive, 2);
				if (s.dvaTicks >= Ow.ticks(WRECK_TIME)) {
					eject(player, s, true);
				}
			}
			case CALLING -> tickCall(player, s);
			default -> {
			}
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if (!HeroManager.heroMode(player)) {
			return;
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0) {
			if (s.dvaForm == PILOT) {
				callMech(player, s);
			} else if ((s.dvaForm == MECH || s.dvaForm == WRECK) && s.ultCharge >= MECH_ULT_COST && !Sleep.asleep(player)) {
				// Self-Destruct: from the mech, or from the wreck's 1.0 s (0276 #213 -> message 0592)
				SelfDestruct.cast(player, s);
			}
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && s.dvaForm == MECH) {
			Boosters.press(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.dvaForm == MECH) {
			MicroMissiles.press(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.dvaForm == PILOT) {
			LightGun.reload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0 && s.dvaForm == MECH && !Sleep.asleep(player)) {
			// Precision Fusion, the mech's Reload (514D #615, #605: mech only)
			DvaPerks.press(player, s);
		}
	}

	/** G (HeroManager.setHeroMode): out of hero mode she is the pilot; back in, the mech she left at the health it had. */
	public static void heroMode(ServerPlayer player, HeroState s, boolean on) {
		if (!on) {
			stopMechKit(player, s);
			// the held states others hear go quiet too: tick stops writing them out of hero mode, so a Matrix held into G
			// left its hum restarting on every client until G back (found in the two-player pass, 2026-11-11)
			player.setAttached(HeroManager.DVA_STATE, 0);
			switch (s.dvaForm) {
				case MECH -> {
					// the mech and its health kept for G back (the plan's default 9); the pilot's own health is full
					s.dvaStoredHealth = player.getHealth();
					s.dvaStoredArmor = s.shield;
				}
				// G in the wreck is the eject, as its second would have been: the pilot at a full 175, Self-Destruct's charge kept
				// for the next mech, Call Mech's from 0, the immunity and the call lock (the critic's finding, 2026-11-11)
				case WRECK -> eject(player, s, true);
				case CALLING -> {
					// a call cut short is spent (D 026A #431 onAbort removes the new mech; the charge went at the press)
					removeMech(player, s);
					s.dvaForm = PILOT;
				}
				default -> {
				}
			}
			player.setAttached(HeroManager.DVA_FORM, s.dvaForm);
			HeroManager.refreshHealth(player, s, s.dvaForm == MECH);
			return;
		}
		// what the pilot lost in Minecraft mode (her max there is the pilot's, not yet refreshed)
		float lost = Math.max(0, player.getMaxHealth() - player.getHealth());
		player.setAttached(HeroManager.DVA_FORM, s.dvaForm);
		HeroManager.refreshHealth(player, s, false);
		if (s.dvaForm == MECH) {
			// the mech at the health it had (the plan's default 9), less what she took while out of it: damage in Minecraft
			// mode is hers, so a near-dead player doesn't come back whole (ours, the critic's finding, 2026-11-11); a mech
			// never left (a respawn in Minecraft mode) is full. Its layer as it was, Shield System's shields with its armor.
			boolean stored = s.dvaStoredHealth > 0;
			float health = (stored ? s.dvaStoredHealth : player.getMaxHealth()) - lost;
			s.shield = stored ? Math.min(s.dvaStoredArmor, s.layer(player)) : s.layer(player);
			dev.overcraft.hero.Shields.setRaw(player, Math.max(1, Math.min(player.getMaxHealth(), health)));
			// (the layer is the top of her health: no more of it than she has)
			s.shield = Math.min(s.shield, dev.overcraft.Ow.toOw(player.getHealth()));
			s.dvaStoredHealth = -1;
		}
	}

	// ---- the eject (D 026A #97 / #370 / #880; 0276 #84) ----

	/**
	 * The mech's wreck (or Self-Destruct: 0B2F #271's 016C, the same message) ejects her: the pilot, launched back and up, a
	 * full 175, briefly immune, the Light Gun full; {@code wreck}: the wreck stays a moment.
	 */
	static void eject(ServerPlayer player, HeroState s, boolean wreck) {
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
		stopMechKit(player, s);
		if (wreck) {
			// the MEKA breaks apart: its destroyed clip 31C3's 78BF (70 m)
			DvaSounds.all(level, "eject", player.position().add(0, 1, 0), 70);
			// the wreck stays where the mech died for 0.75 s, playing on its destruction (D 0276 #60 / #112)
			level.addFreshEntity(new DvaMechEntity(level, player, DvaMechEntity.WRECK, player.position(), player.getYRot()));
		}
		// she ejects with a full gun (026A #152 refilled it while she drove)
		LightGun.refill(s);
		// the mech's Self-Destruct charge is kept for the next mech (D 026A #775), Call Mech's starts at 0 (#85)
		s.dvaMechUlt = s.ultCharge;
		s.ultCharge = 0;
		s.ultActive = 0;
		s.dvaForm = PILOT;
		s.dvaTicks = 0;
		s.dvaEjectedAt = now;
		s.dvaImmuneUntil = now + Ow.ticks(EJECT_IMMUNE);
		s.dvaCallLockUntil = now + Ow.ticks(EJECT_CALL_LOCK);
		s.dvaStoredHealth = -1;
		player.setAttached(HeroManager.DVA_FORM, PILOT);
		// the pilot's own 175, full (the 9999 heal of #97)
		HeroManager.refreshHealth(player, s, true);
		// Bunny Power: overhealth as she lands (514D #440)
		DvaPerks.ejected(player, s);
		if (wreck) {
			// "Pilot disengaging!" (0497)
			dev.overcraft.hero.Voice.say(player, "eject", 1);
		}
		// launched away from a point 1 m ahead of the mech: 6 m/s back, 7 up (#97's knockback), added to the mech's motion
		// (WS 2026-11-11, dva_workshop_2026-11-11.md: all four ejects kept the mech's velocity, 0.3-0.9 m/s off, plus 5.7-6.15 back
		// along her facing and 7.00 up; a boosted Self-Destruct flies on); her fall from it doesn't hurt
		Vec3 back = facing.scale(-Ow.speed(EJECT_BACK));
		player.setDeltaMovement(dev.overcraft.hero.Combat.motion(player).add(back.x, Ow.speed(EJECT_UP), back.z));
		// straight to her client, which moves her (needsSync alone never reached it: found playing, she stayed put; Combat.send, #109)
		Combat.send(player);
		// (her eject clip is her base state's: every client sees the form change, HeroAnimator)
		s.softLanding = true;
	}

	// ---- Call Mech (D 026A #87 .. #98) ----

	/** Ticks into the call (counted from the press) at which a moment after the mech's spawn comes. */
	static int callTick(double afterSpawn) {
		return Ow.ticks(CALL_CAST + afterSpawn);
	}

	/** Q as the pilot with Call Mech charged: the mech falls in where there's room for it, or nothing happens (no cost). */
	static void callMech(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		if (s.ultCharge < PILOT_ULT_COST || now < s.dvaCallLockUntil || Sleep.asleep(player)) {
			return;
		}
		if (!player.onGround()) {
			// "Has to be grounded to activate" (W rev 408749; BZ says nothing): no cast and the charge kept, as with no room
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("overcraft.dva.call_mech.no_room"));
			return;
		}
		Vec3 spot = callSpot(player);
		if (spot == null) {
			// no ground, or no room for the mech: no cast and the charge kept (D #462 / #461: the error, no cast)
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("overcraft.dva.call_mech.no_room"));
			return;
		}
		s.ultCharge = 0;
		// "Calling in the big guns!" (0479)
		dev.overcraft.hero.Voice.say(player, "call_mech", 1);
		// the call, the drop and its landing (026A #417 effect 1573: 5DC1 hers, 9656 others' 70 m)
		DvaSounds.split(player, "call_mech.drop", spot, 70);
		s.dvaForm = CALLING;
		s.dvaTicks = 0;
		s.dvaCallSpot = spot;
		s.dvaCallYaw = player.getYRot();
		s.dvaCallLevel = player.level().dimension();
		player.setAttached(HeroManager.DVA_FORM, CALLING);
		player.setDeltaMovement(0, Math.min(0, Combat.motion(player).y), 0);
		Combat.send(player);
	}

	/**
	 * Where Call Mech puts the mech: 1.5 m ahead along her flat facing, on the ground found within 4 m under a point 1.5 m
	 * up (D 026A #256); else her own spot if there's ground within 2 m under her (#455); and (ours: the box rule, the plan's
	 * "Minecraft decisions") only where the mech's 1.4 x 2.3 m box fits. Null: nowhere.
	 */
	public static @Nullable Vec3 callSpot(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 flat = Vec3.directionFromRotation(0, player.getYRot());
		Vec3 ahead = player.position().add(flat.scale(CALL_AHEAD));
		Vec3 ground = ground(level, player, ahead.add(0, CALL_PROBE_UP, 0), CALL_PROBE_UP + CALL_PROBE_DOWN);
		if (ground != null && fits(level, player, ground)) {
			return ground;
		}
		Vec3 below = ground(level, player, player.position().add(0, 1, 0), 1 + CALL_GROUND_BELOW);
		return below != null && fits(level, player, below) ? below : null;
	}

	private static @Nullable Vec3 ground(ServerLevel level, Entity player, Vec3 from, double down) {
		BlockHitResult hit = level.clip(new ClipContext(from, from.subtract(0, down, 0), ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player));
		return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
	}

	private static boolean fits(ServerLevel level, Entity player, Vec3 feet) {
		AABB box = HeroBody.DVA_MECH.makeBoundingBox(feet).deflate(1.0E-7);
		return level.noBlockCollision(player, box);
	}

	private static void tickCall(ServerPlayer player, HeroState s) {
		ServerLevel level = (ServerLevel) player.level();
		s.ultActive = Math.max(s.ultActive, 2);
		if (s.dvaCallLevel != null && player.level().dimension() != s.dvaCallLevel) {
			// she changed level mid-call (a portal, a /tp): the spot is the old level's, so linking there dropped her into the
			// void (the critic of 83b6bfa, #133). Cut short as G does: the new mech goes, the charge stays spent (026A #431 onAbort)
			ServerLevel old = ((ServerLevel) player.level()).getServer().getLevel(s.dvaCallLevel);
			if (old != null && s.dvaMechId != null && old.getEntity(s.dvaMechId) instanceof DvaMechEntity mech) {
				mech.discard();
			}
			s.dvaMechId = null;
			s.dvaForm = PILOT;
			player.setAttached(HeroManager.DVA_FORM, PILOT);
			return;
		}
		if (Sleep.asleep(player) || s.dvaCallSpot == null) {
			// a stun before the link: the new mech is removed and the charge is spent (D 026A #431 onAbort -> #121, M)
			removeMech(player, s);
			s.dvaForm = PILOT;
			player.setAttached(HeroManager.DVA_FORM, PILOT);
			return;
		}
		if (s.dvaTicks == callTick(0)) {
			// the cast's 0.2 s are over: the mech spawns at the spot and plays its landing (D #94, 0276 #309 / #316 0xE5)
			DvaMechEntity mech = new DvaMechEntity(level, player, DvaMechEntity.CALL, s.dvaCallSpot, s.dvaCallYaw);
			level.addFreshEntity(mech);
			s.dvaMechId = mech.getId();
		}
		if (s.dvaTicks == callTick(CALL_LAND)) {
			landingHit(player, s);
		}
		if (s.dvaTicks >= callTick(CALL_LINK)) {
			link(player, s);
		}
	}

	/** The landing: 250 to every enemy within 3 m of the mech, thrown 15 m/s away and 3 up (D 026A #241 / #242). */
	private static void landingHit(ServerPlayer player, HeroState s) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 centre = s.dvaCallSpot.add(0, 0.5, 0);
		// the push comes from a point 2 m ahead of her (D m_C0139FA6: about the mech's centre, M)
		Vec3 from = player.position().add(Vec3.directionFromRotation(0, s.dvaCallYaw).scale(2));
		boolean bunny = s.has(Perk.DVA_BUNNY_POWER);
		double radius = DvaPerks.callRadius(s);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius))) {
			double d = Math.max(0, e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(centre) - e.getBbWidth() / 2);
			if (e == player || !e.isAlive() || d > radius || !Allies.areaHits(player, e)) {
				continue;
			}
			// 250 inside 3 m; Bunny Power's 4.5 m falls off past 3 (026A #908)
			Combat.credit(player, Combat.damage(e, level.damageSources().playerAttack(player), DvaPerks.callDamage(d, bunny), CALL_BLOW), e);
			Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, s.dvaCallYaw) : away.normalize();
			// on top of what it's doing, scaled by its share (Combat.pushShare), sent to a player, credited (#93)
			Combat.push(e, away.scale(Ow.speed(CALL_KNOCKBACK)).add(0, Ow.speed(CALL_KNOCKBACK_UP), 0), player, CALL_BLOW);
		}
	}

	private static final dev.overcraft.hero.Blow CALL_BLOW = dev.overcraft.hero.Blow.ult("call_mech").by(Hero.DVA);

	/** She drives the mech (D #98): the unmanned one goes, she is the mech at its spot, a new one at full 675. */
	private static void link(ServerPlayer player, HeroState s) {
		Vec3 spot = s.dvaCallSpot;
		removeMech(player, s);
		s.dvaForm = MECH;
		s.dvaTicks = 0;
		s.dvaCallSpot = null;
		player.setAttached(HeroManager.DVA_FORM, MECH);
		if (spot != null) {
			player.teleportTo((ServerLevel) player.level(), spot.x, spot.y, spot.z, java.util.Set.of(), s.dvaCallYaw, player.getXRot(), true);
		}
		player.setDeltaMovement(Vec3.ZERO);
		Combat.send(player);
		// the Self-Destruct charge the last mech had (D 0276 #88 / 026A #775); Call Mech's starts again at 0 (#85)
		s.ultCharge = s.dvaMechUlt;
		s.ultActive = 0;
		freshMech(s);
		HeroManager.refreshHealth(player, s, true);
		// "D.Va back in action!" (049D)
		dev.overcraft.hero.Voice.say(player, "mech_back", 1);
	}

	/**
	 * She killed an enemy (Voice.onKill), before the shared lines: Call Mech's landing "Squished!" (11CE, 026A #854), or a kill
	 * as the pilot "No mech? No problem!" (10AF, 02B8 #353's message, 0.9 of the time).
	 */
	public static void killed(ServerPlayer player, HeroState s, dev.overcraft.hero.@Nullable Blow blow) {
		if (blow != null && blow.kind() == dev.overcraft.hero.Blow.Kind.ULT && "call_mech".equals(blow.icon())) {
			dev.overcraft.hero.Voice.say(player, "call_mech_kill", 1);
		} else if (s.dvaForm == PILOT && blow != null && blow.kind() == dev.overcraft.hero.Blow.Kind.WEAPON) {
			dev.overcraft.hero.Voice.say(player, "pilot_kill", 0.9f);
		}
	}

	private static void removeMech(ServerPlayer player, HeroState s) {
		if (s.dvaMechId != null && player.level().getEntity(s.dvaMechId) instanceof DvaMechEntity mech) {
			mech.discard();
		}
		s.dvaMechId = null;
	}

	// ---- the Initiator sub-role (D 01C7 #2953 .. #2889), triggered by Boosters (step 6) ----

	/**
	 * Initiator's heal, on a movement ability's start (hers: Boosters, D 047E #380 / #487): 50 over 1 s if she isn't at full
	 * health and the 5 s since the last one are over; true when it heals.
	 */
	public static boolean initiator(ServerPlayer player, HeroState s) {
		long now = player.level().getGameTime();
		if (!Tank.initiatorReady(now, s.dvaInitiatorReadyAt, player.getHealth(), player.getMaxHealth())) {
			return false;
		}
		s.dvaInitiatorReadyAt = now + Ow.ticks(INITIATOR_LOCKOUT);
		// over 1 s (Lifeweaver's heals in flight carry it)
		dev.overcraft.lifeweaver.Lifeweaver.healOver(player, player, INITIATOR_HEAL, INITIATOR_HEAL_TIME, false);
		return true;
	}

	/**
	 * Out of play (#100: HeroKit.leavePlay). G: what she's doing ends and she's the pilot, the mech kept for G back
	 * ({@link #heroMode}); death: what she's doing ends (a Self-Destruct already flying still goes off, as it would in
	 * Overwatch); a switch or a leave: the unmanned mech and a flying Self-Destruct go too ({@link #clear}).
	 */
	@Override
	public void leavePlay(ServerPlayer player, HeroState s, OutOfPlay why) {
		if (why.forGood()) {
			clear(player, s);
		} else if (why == OutOfPlay.MINECRAFT_MODE) {
			heroMode(player, s, false);
		} else {
			stopMechKit(player, s);
		}
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		long now = s.dvaNow;
		float lock = (float) Math.max(0, (s.dvaCallLockUntil - now) / 20.0);
		boolean pilot = s.dvaForm == PILOT || s.dvaForm == CALLING;
		float reload = pilot && s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(LIGHT_GUN_RELOAD)) : -1;
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ultCost(s), s.dvaForm == CALLING,
			pilot ? s.primaryAmmo : -1, reload, -1, s.dvaMatrixCooldown > 0 ? s.dvaMatrixCooldown / 20f : -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero,
			s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk),
			// the third slot: Call Mech's lock as the pilot, Precision Fusion's cooldown in the mech
			pilot ? lock : (float) (s.ability3Cooldown / 20.0),
			s.dvaForm | (s.primaryHeld && FusionCannons.firing(s, now) ? MODE_FIRING : 0) | (s.dvaMatrix ? MODE_MATRIX : 0)
				| (Boosters.active(s) ? MODE_BOOSTING : 0) | (DvaPerks.precision(s) ? MODE_PRECISION : 0),
			Boosters.active(s) ? (float) (DvaPerks.boostersLeft(s) / 20.0) : 0, (float) (s.dvaMatrixEnergy / MATRIX_ENERGY));
	}
}
