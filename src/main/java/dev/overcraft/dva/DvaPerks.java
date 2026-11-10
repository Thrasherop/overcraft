package dev.overcraft.dva;

import dev.overcraft.Ow;
import dev.overcraft.hero.Combat;
import dev.overcraft.hero.HeroManager;
import dev.overcraft.hero.HeroState;
import dev.overcraft.hero.Perk;
import dev.overcraft.hero.Shields;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import static dev.overcraft.dva.DvaStats.*;

/**
 * D.Va's perks (step 10; 514D, tools/reference/dva.md "Perks", kit_dva_mech.md / kit_dva_pilot.md in research\dva\work_kit):
 * Bunny Power and Extended Boosters (minor), Shield System and Precision Fusion (major). "Enemy heroes" in the data's
 * filters are every creature she can hurt (dva-plan.md "Minecraft decisions"). Numbers in {@link DvaStats}, each cited.
 */
public final class DvaPerks {
	private DvaPerks() {
	}

	// ---- Bunny Power ----

	/** She has just ejected (the link dropped: 514D #440): 75 overhealth, held 10 s, then 25 a second (#424-#427). */
	static void ejected(ServerPlayer player, HeroState s) {
		if (s.has(Perk.DVA_BUNNY_POWER)) {
			Combat.overhealthPool(player, BUNNY_OVERHEALTH, BUNNY_OVERHEALTH, BUNNY_HOLD, BUNNY_DECAY);
		}
	}

	/** Call Mech's landing radius: 3 m, 4.5 with Bunny Power (026A #941). */
	static double callRadius(HeroState s) {
		return s.has(Perk.DVA_BUNNY_POWER) ? BUNNY_CALL_RADIUS : CALL_RADIUS;
	}

	/** The landing's damage at {@code d} m: 250 inside 3 m; with Bunny Power 250 x (4.5 - d) / 1.5 past it, 0 at 4.5 (026A #908). */
	public static double callDamage(double d, boolean bunny) {
		if (d <= CALL_RADIUS) {
			return CALL_DAMAGE;
		}
		return bunny ? CALL_DAMAGE * Math.max(0, BUNNY_CALL_RADIUS - d) / (BUNNY_CALL_RADIUS - CALL_RADIUS) : 0;
	}

	// ---- Extended Boosters ----

	/** Boosters' hit: 25, 35 with Extended Boosters (514D #310). */
	static double boostersDamage(HeroState s) {
		return s.has(Perk.DVA_EXTENDED_BOOSTERS) ? EXTENDED_BOOSTERS_DAMAGE : BOOSTERS_DAMAGE;
	}

	/** A Boosters hit landed: +0.5 s of flight, at most 2 s left (047E #458-#463). */
	static void boostersHit(HeroState s) {
		if (!s.has(Perk.DVA_EXTENDED_BOOSTERS)) {
			return;
		}
		int left = boostersLeft(s);
		int added = Math.min(Ow.ticks(EXTENDED_BOOSTERS_TIME), Math.max(0, Ow.ticks(EXTENDED_BOOSTERS_CAP) - left));
		s.dvaBoostExtra += added;
	}

	/** The flight's ticks left (its 2 s and what Extended Boosters added). */
	static int boostersLeft(HeroState s) {
		return Ow.ticks(BOOSTERS_TIME) + s.dvaBoostExtra - s.dvaBoost;
	}

	// ---- Shield System ----

	/** Her shields: Shield System's 100 in the mech (514D #399 / #400, mech only: 0276 #241), else none. */
	public static int shields(@Nullable Player player, HeroState s) {
		return s.has(Perk.DVA_SHIELD_SYSTEM) && !Dva.pilotBody(player) ? SHIELD_SYSTEM_SHIELDS : 0;
	}

	/**
	 * Each tick in the mech: the 100 health that are shields now are the layer's, so a mech at full health has full shields
	 * when the perk is picked mid-life (its 675 doesn't change: 250 health, 325 armor, 100 shields).
	 */
	static void tick(ServerPlayer player, HeroState s) {
		if (shields(player, s) == 0) {
			return;
		}
		double health = Ow.toOw(player.getHealth()) - s.shield;
		double healthMax = s.owHealth(player) - s.layer(player);
		if (health > healthMax + 0.01) {
			s.shield = Math.min(s.layer(player), s.shield + health - healthMax);
		}
	}

	/** Defense Matrix ate {@code damage}: Shield System gives 25 % of it back as shields (514D #402, message 2638). */
	static void matrixAte(ServerPlayer player, HeroState s, double damage) {
		if (shields(player, s) > 0) {
			Shields.restore(player, s, damage * SHIELD_SYSTEM_SHARE);
		}
	}

	// ---- Precision Fusion ----

	public static boolean precision(HeroState s) {
		return s.dvaPrecision >= 0;
	}

	/** Reload in the mech: starts it when ready; from 0.4 s in, ends it (#615 / #650). */
	static void press(ServerPlayer player, HeroState s) {
		if (!s.has(Perk.DVA_PRECISION_FUSION)) {
			return;
		}
		if (precision(s)) {
			if (s.dvaPrecision >= Ow.ticks(PRECISION_CANCEL_AFTER)) {
				end(player, s);
			}
			return;
		}
		if (s.ability3Cooldown > 0) {
			return;
		}
		s.dvaPrecision = 0;
		DvaSounds.own(player, "perk.precision_fusion");
	}

	/** Each tick in hero mode: its 3.1 s, then its 7.5 s cooldown (from its end: #613). */
	static void tickPrecision(ServerPlayer player, HeroState s, boolean mech) {
		if (!precision(s)) {
			s.ability3Cooldown = Math.max(0, s.ability3Cooldown - Combat.cooldownRate(player));
			return;
		}
		if (!mech) {
			end(player, s);
			return;
		}
		if (++s.dvaPrecision >= Ow.ticks(PRECISION_TIME)) {
			end(player, s);
		}
	}

	static void end(ServerPlayer player, HeroState s) {
		if (!precision(s)) {
			return;
		}
		s.dvaPrecision = -1;
		s.dvaPrecisionEndedAt = player.level().getGameTime();
		s.ability3Cooldown = Ow.ticks(PRECISION_COOLDOWN);
	}

	/** The share of the spread cut now: 0.75 in 0.1 s, back to 0 in 0.15 s after it ends (#636 / #639 / #643). */
	static double spreadCut(HeroState s, long now) {
		if (precision(s)) {
			return PRECISION_SPREAD_CUT * Math.min(1, s.dvaPrecision / (PRECISION_RAMP_IN * 20));
		}
		double out = (now - s.dvaPrecisionEndedAt) / (PRECISION_RAMP_OUT * 20);
		return out >= 1 ? 0 : PRECISION_SPREAD_CUT * (1 - out);
	}

	/** Whether this player is D.Va in her mech with Precision Fusion on (for anything outside her kit). */
	public static boolean precision(Player player) {
		HeroState s = HeroManager.state(player);
		return s != null && s.hero == dev.overcraft.hero.Hero.DVA && precision(s);
	}
}
