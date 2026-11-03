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
	public static final int FORM_BITS = 7, MODE_FIRING = 8, MODE_MATRIX = 16, MODE_BOOSTING = 32;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		clear(player, s);
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
		s.ability1Cooldown = 0;
		s.ability2Cooldown = 0;
		s.weaponRecovery = 0;
	}

	/** Her mech kit stops at once (the wreck, G, another hero): the matrix down, the flight over, the slow off. */
	static void stopMechKit(ServerPlayer player, HeroState s) {
		DefenseMatrix.end(player, s, player.level().getGameTime());
		Boosters.end(player, s);
		FusionCannons.slow(player, false);
	}

	/** Leaving her (another hero, leaving the world): the unmanned mech goes. */
	public static void clear(ServerPlayer player, HeroState s) {
		removeMech(player, s);
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
		DefenseMatrix.tick(player, s, mech);
		Boosters.tick(player, s, mech);
		FusionCannons.tick(player, s, mech && !s.dvaMatrix, Boosters.active(s));
		switch (s.dvaForm) {
			case WRECK -> {
				// no passive ult meanwhile, and frozen where it fell (D 0276 #79)
				s.ultActive = Math.max(s.ultActive, 2);
				if (s.dvaTicks >= Ow.ticks(WRECK_TIME)) {
					eject(player, s);
				}
			}
			case CALLING -> tickCall(player, s);
			default -> {
			}
		}
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.dvaForm == PILOT) {
			callMech(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0 && s.dvaForm == MECH && HeroManager.heroMode(player)) {
			Boosters.press(player, s);
		}
	}

	/** G (HeroManager.setHeroMode): out of hero mode she is the pilot; back in, the mech she left at the health it had. */
	public static void heroMode(ServerPlayer player, HeroState s, boolean on) {
		if (!on) {
			stopMechKit(player, s);
			switch (s.dvaForm) {
				case MECH -> {
					// the mech and its health kept for G back (the plan's default 9); the pilot's own health is full
					s.dvaStoredHealth = player.getHealth();
					s.dvaStoredArmor = s.shield;
				}
				case WRECK -> s.dvaForm = PILOT;
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
		player.setAttached(HeroManager.DVA_FORM, s.dvaForm);
		HeroManager.refreshHealth(player, s, false);
		if (s.dvaForm == MECH) {
			// the mech at the health it had (a mech never left, as after a respawn in Minecraft mode: full)
			float health = s.dvaStoredHealth > 0 ? s.dvaStoredHealth : player.getMaxHealth();
			s.shield = s.dvaStoredHealth > 0 ? Math.min(s.dvaStoredArmor, MECH_ARMOR) : MECH_ARMOR;
			dev.overcraft.hero.Shields.setRaw(player, Math.min(player.getMaxHealth(), health));
			s.dvaStoredHealth = -1;
		}
	}

	// ---- the eject (D 026A #97 / #370 / #880; 0276 #84) ----

	/** The mech's wreck ejects her: the pilot, launched back and up, a full 175, briefly immune; the wreck stays a moment. */
	static void eject(ServerPlayer player, HeroState s) {
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
		// the wreck stays where the mech died for 0.75 s, playing on its destruction (D 0276 #60 / #112)
		DvaMechEntity wreck = new DvaMechEntity(level, player, DvaMechEntity.WRECK, player.position(), player.getYRot());
		level.addFreshEntity(wreck);
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
		// launched away from a point 1 m ahead of the mech: 6 m/s back, 7 up (#97's knockback); her fall from it doesn't hurt
		Vec3 back = facing.scale(-Ow.speed(EJECT_BACK));
		player.setDeltaMovement(back.x, Ow.speed(EJECT_UP), back.z);
		// straight to her client, which moves her (needsSync alone never reached it: found playing, she stayed put)
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
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
		Vec3 spot = callSpot(player);
		if (spot == null) {
			// no ground, or no room for the mech: no cast and the charge kept (D #462 / #461: the error, no cast)
			player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("overcraft.dva.call_mech.no_room"));
			return;
		}
		s.ultCharge = 0;
		s.dvaForm = CALLING;
		s.dvaTicks = 0;
		s.dvaCallSpot = spot;
		s.dvaCallYaw = player.getYRot();
		player.setAttached(HeroManager.DVA_FORM, CALLING);
		player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
		player.needsSync = true;
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
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(CALL_RADIUS))) {
			if (e == player || !e.isAlive() || e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(centre) > CALL_RADIUS + e.getBbWidth() / 2
				|| !Allies.canHurt(player, e)) {
				continue;
			}
			Vec3 before = e.getDeltaMovement();
			Combat.credit(player, Combat.damage(e, level.damageSources().playerAttack(player), CALL_DAMAGE), e);
			Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
			away = away.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, s.dvaCallYaw) : away.normalize();
			double f = Tank.knockbackFactor(e) * Math.max(0, 1 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
			e.setDeltaMovement(before.add(away.scale(Ow.speed(CALL_KNOCKBACK) * f)).add(0, Ow.speed(CALL_KNOCKBACK_UP) * f, 0));
			e.needsSync = true;
		}
	}

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
		player.needsSync = true;
		// the Self-Destruct charge the last mech had (D 0276 #88 / 026A #775); Call Mech's starts again at 0 (#85)
		s.ultCharge = s.dvaMechUlt;
		s.ultActive = 0;
		freshMech(s);
		HeroManager.refreshHealth(player, s, true);
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

	@Override
	public HeroSyncPayload sync(HeroState s) {
		long now = s.dvaNow;
		float lock = (float) Math.max(0, (s.dvaCallLockUntil - now) / 20.0);
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, ultCost(s), s.dvaForm == CALLING,
			-1, -1, -1, s.dvaMatrixCooldown > 0 ? s.dvaMatrixCooldown / 20f : -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0), s.rushed, s.canChangeHero,
			s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk), Perk.index(s.majorPerk), lock,
			s.dvaForm | (s.primaryHeld && FusionCannons.firing(s, now) ? MODE_FIRING : 0) | (s.dvaMatrix ? MODE_MATRIX : 0)
				| (Boosters.active(s) ? MODE_BOOSTING : 0),
			Boosters.active(s) ? (float) ((Ow.ticks(BOOSTERS_TIME) - s.dvaBoost) / 20.0) : 0, (float) (s.dvaMatrixEnergy / MATRIX_ENERGY));
	}
}
