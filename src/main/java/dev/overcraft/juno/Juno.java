package dev.overcraft.juno;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroKit;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Perks;
import dev.overcraft.net.HeroInputPayload;
import dev.overcraft.net.HeroSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import static dev.overcraft.juno.JunoStats.*;

/**
 * Juno's kit (tools/reference/juno-plan.md "Order"), built step by step, each played before the next. Now: the shell (step 1):
 * her model and her HUD; her Martian Overboots, Glide Boost, the Mediblaster, Pulsar Torpedoes, Hyper Ring and Orbital Ray come
 * next. Her buttons by her data's ids: the left button the Mediblaster (and fires locked torpedoes), the right Pulsar
 * Torpedoes, Shift Glide Boost, E Hyper Ring, Q Orbital Ray, jump her double jump and hover (juno-map.md, "Abilities").
 */
public final class Juno implements HeroKit {
	/** The synced mode's bit: she's gliding (her client moves her: ClientJuno). */
	public static final int MODE_GLIDE = 1;
	/** ... and she's locking Pulsar Torpedoes. */
	public static final int MODE_LOCK = 2;

	@Override
	public void equip(ServerPlayer player, HeroState s) {
		s.weaponRecovery = 0;
		s.primaryAmmo = MEDIBLASTER_AMMO;
		s.junoGlide = -1;
		s.junoRingCast = -1;
		s.junoRayCast = -1;
		Torpedoes.clear(player, s);
	}

	/** Leaving her (another hero, leaving the game): her synced glide and lock off, so her clients' loops stop with them. */
	public static void clear(ServerPlayer player, HeroState s) {
		Torpedoes.clear(player, s);
		s.junoGlide = -1;
		player.setAttached(dev.overcraft.hero.HeroManager.JUNO_GLIDE, false);
		player.setAttached(dev.overcraft.hero.HeroManager.JUNO_LOCK, false);
	}

	@Override
	public void tick(ServerPlayer player, HeroState s, boolean controls) {
		double rate = Combat.cooldownRate(player);
		s.ability1Cooldown = Math.max(0, s.ability1Cooldown - rate);
		s.ability2Cooldown = Math.max(0, s.ability2Cooldown - rate);
		s.ability3Cooldown = Math.max(0, s.ability3Cooldown - rate);
		double attack = Combat.attackRate(player);
		reload(s, attack);
		s.weaponRecovery = Math.max(0, s.weaponRecovery - attack);
		if (s.junoGlide >= 0 && (++s.junoGlide >= Ow.ticks(GLIDE_TIME) || !controls || !player.isAlive())) {
			endGlide(player, s);
		}
		if (!java.util.Objects.equals(player.getAttached(dev.overcraft.hero.HeroManager.JUNO_GLIDE), gliding(s))) {
			player.setAttached(dev.overcraft.hero.HeroManager.JUNO_GLIDE, gliding(s));
		}
		boolean leftPressed = s.primaryHeld && !s.junoLeftWas;
		boolean rightPressed = s.secondaryHeld && !s.junoRightWas;
		s.junoLeftWas = s.primaryHeld;
		s.junoRightWas = s.secondaryHeld;
		if (!controls || !player.isAlive()) {
			s.junoVolley = 0;
			Torpedoes.clear(player, s);
			lockFlag(player, s);
			return;
		}
		if (s.junoLock >= 0 && dev.overcraft.sombra.Hack.silenced(player)) {
			// hacked: the lock ends (W: stun, knockdown or hack interrupt it); her gun still works
			Torpedoes.clear(player, s);
		}
		ring(player, s);
		ray(player, s);
		boolean locking = s.junoLock >= 0;
		Torpedoes.tick(player, s, rightPressed, leftPressed);
		// locking, she can't shoot (W: channeled), and the click that ends the lock isn't a volley, held or not (a tap
		// fired one: found playing)
		if (locking && leftPressed) {
			s.junoLeftSpent = true;
		} else if (!s.primaryHeld) {
			s.junoLeftSpent = false;
		}
		if (s.junoLock < 0 && s.junoFiring.isEmpty() && !s.junoLeftSpent) {
			Mediblaster.tick(player, s, attack);
		}
		lockFlag(player, s);
	}

	/** The synced lock flag, for her clients' lock sounds (dev.overcraft.client.JunoSounds). */
	private static void lockFlag(ServerPlayer player, HeroState s) {
		boolean locking = s.junoLock >= 0;
		if (!java.util.Objects.equals(player.getAttached(dev.overcraft.hero.HeroManager.JUNO_LOCK), locking)) {
			player.setAttached(dev.overcraft.hero.HeroManager.JUNO_LOCK, locking);
		}
	}

	private static void reload(HeroState s, double attack) {
		if (s.primaryReload < 0) {
			return;
		}
		s.primaryReload += attack;
		if (s.primaryReload >= Ow.ticks(RELOAD_FULL_AT)) {
			s.primaryAmmo = MEDIBLASTER_AMMO;
		}
		if (s.primaryReload >= Ow.ticks(RELOAD)) {
			s.primaryReload = -1;
		}
	}

	/** R (not full, not reloading), or the magazine empty: 1.5 s, full at 0.75 s. */
	static void startReload(ServerPlayer player, HeroState s) {
		if (s.primaryReload >= 0 || s.primaryAmmo >= MEDIBLASTER_AMMO) {
			return;
		}
		s.primaryReload = 0;
		s.junoVolley = 0;
		Combat.animate(player, "reload");
		// her 1P clip E9B6's foley (5FA4) in her head, the 3P clip F6C3's (60BA, 45 m) for the rest; her own line 176F (4445
		// #1334 / #1336)
		JunoSounds.split(player, "mediblaster.reload", player.getEyePosition(), 45);
		dev.overcraft.hero.Voice.say(player, "reload", 1);
	}

	@Override
	public void press(ServerPlayer player, HeroState s, int pressed) {
		if ((pressed & HeroInputPayload.PRESS_ABILITY_1) != 0) {
			glide(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_RELOAD) != 0) {
			startReload(player, s);
		}
		if ((pressed & HeroInputPayload.PRESS_ABILITY_2) != 0 && s.ability2Cooldown <= 0 && s.junoRingCast < 0) {
			s.junoRingCast = 0;
			Combat.animate(player, "ring");
			// the press (4857 #807's activation -> 52FE / 5D47, her clips' B503 / B4FF; 70 m), and her line as the cast begins
			// (#1097 onBegin -> #1203: 16B0)
			JunoSounds.split(player, "ring.cast", player.getEyePosition(), 70);
			dev.overcraft.hero.Voice.say(player, "ring", 1);
		}
		if ((pressed & HeroInputPayload.PRESS_ULTIMATE) != 0 && s.ultCharge >= ULT_COST && s.junoRayCast < 0) {
			s.ultCharge = Combat.afterUlt(dev.overcraft.hero.Hero.JUNO, s.ultCharge);
			s.ultActive = Ow.ticks(RAY_LAND + RAY_TIME);
			s.junoRayCast = 0;
			// it ends her lock (W)
			Torpedoes.clear(player, s);
			Combat.animate(player, "ult");
			// the cast (54F8 held for 4A44 #430's 0.5 s, her 1P clip EA1C's B4FC; 75 m), and the line raised at the press (#853):
			// 16A9 to her and her team, 16AA to her enemies (80 m; 184D, her own copy of 16A9's text, isn't played as well)
			JunoSounds.split(player, "ult.cast", player.getEyePosition(), 75);
			dev.overcraft.hero.HeroSounds.ultLine(player, JunoSounds.of("ult.friendly"), JunoSounds.of("ult.enemy"), 5);
		}
	}

	/**
	 * Her client's extra input: EXTRA_JUMP, her double jump (OwMovement moves her there): its sound for everyone else, at
	 * her (4441 #570 -> #274 53EB: AD52, 60 m); hers played in her head by her client.
	 */
	public static void extra(ServerPlayer player, HeroState s, int extra) {
		if ((extra & HeroInputPayload.EXTRA_JUMP) != 0 && player.isAlive()) {
			dev.overcraft.hero.HeroSounds.at(player.level(), JunoSounds.of("boots.double_jump_3p"), player.getEyePosition(), 60, player);
		}
	}

	/** Hyper Ring's cast: the ring at 0.25 s, the cooldown from its 0.55 s end. */
	private static void ring(ServerPlayer player, HeroState s) {
		if (s.junoRingCast < 0) {
			return;
		}
		if (s.junoRingCast == Ow.ticks(RING_THROW)) {
			HyperRing.thrown(player);
		}
		if (++s.junoRingCast >= Ow.ticks(RING_CAST)) {
			s.junoRingCast = -1;
			s.ability2Cooldown = Ow.ticks(RING_COOLDOWN);
		}
	}

	/** Orbital Ray's cast: the ray lands 0.5 s in. */
	private static void ray(ServerPlayer player, HeroState s) {
		if (s.junoRayCast < 0) {
			return;
		}
		if (s.junoRayCast == Ow.ticks(RAY_LAND)) {
			OrbitalRay.land(player);
		}
		if (++s.junoRayCast >= Ow.ticks(RAY_CAST)) {
			s.junoRayCast = -1;
		}
	}

	// ---- Glide Boost (47B7) ----

	/**
	 * Shift: the glide (her client gives the burst and moves her, ClientJuno), or, 0.4 s into one, its end (the toggle, her
	 * data's default). Survivor's regeneration starts with it (message 26FD: her only one). The cooldown runs from its end.
	 */
	private static void glide(ServerPlayer player, HeroState s) {
		if (s.junoGlide >= 0) {
			if (s.junoGlide >= Ow.ticks(GLIDE_CANCEL_FROM)) {
				endGlide(player, s);
			}
			return;
		}
		if (s.ability1Cooldown > 0) {
			return;
		}
		s.junoGlide = 0;
		s.lastDamagedTick = Math.min(s.lastDamagedTick, Combat.now(player) - Combat.REGEN_DELAY);
		Combat.animate(player, "glide");
	}

	private static void endGlide(ServerPlayer player, HeroState s) {
		s.junoGlide = -1;
		s.ability1Cooldown = Ow.ticks(GLIDE_COOLDOWN);
		Combat.animate(player, "glide_end");
	}

	/** Gliding (knockback taken halved: LivingEntityMixin). */
	public static boolean gliding(HeroState s) {
		return s.hero == dev.overcraft.hero.Hero.JUNO && s.junoGlide >= 0;
	}

	@Override
	public HeroSyncPayload sync(HeroState s) {
		return new HeroSyncPayload(s.hero == null ? "" : s.hero.id, (float) s.ultCharge, (int) ULT_COST, false,
			s.primaryAmmo, s.primaryReload >= 0 ? (float) Math.min(1, s.primaryReload / Ow.ticks(RELOAD)) : -1, -1, -1, (float) (s.ability1Cooldown / 20.0), (float) (s.ability2Cooldown / 20.0),
			s.rushed, s.canChangeHero, s.onFire, Perks.level(s), Perks.progress(s), Perks.ready(s), Perk.index(s.minorPerk),
			Perk.index(s.majorPerk), (float) (s.ability3Cooldown / 20.0), (s.junoGlide >= 0 ? MODE_GLIDE : 0) | (s.junoLock >= 0 ? MODE_LOCK : 0),
			s.junoGlide >= 0 ? (float) ((Ow.ticks(GLIDE_TIME) - s.junoGlide) / 20.0) : 0, 0);
	}
}
