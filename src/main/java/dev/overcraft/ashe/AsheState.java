package dev.overcraft.ashe;

import java.util.UUID;

/** Ashe's live state on the server (HeroState.ashe; timers in ticks, game times in game ticks). */
public final class AsheState {
	/** The scope: 0 hip .. 1 scoped, and which way it's going (+1 in, -1 out, 0 still). */
	public double scope;
	/** The hip spread's share now (0 .. 1). */
	public double spread;
	/** Ticks to the next hip shot, to the next scoped shot (the scoped one waits while the scope moves). */
	public double hipWait, adsWait;
	/** The reload's phase: -1 none, 0 its start, 1 shells, 2 its end; ticks into that phase. */
	public int reloadPhase = -1;
	public double reloadTicks;
	/** A shot asked for during the shells: the reload ends, then it fires. */
	public boolean reloadCut;
	/** R or an empty magazine asked for it while the hip shot's wait ran: it starts once it can. */
	public boolean reloadAsked;
	/** A Coach Gun launch counts as moving her until the server sees her leave the ground (Junkrat's LAUNCH_GRACE). */
	public int launchGrace;
	/** Ticks counted for the once-a-second check that B.O.B. is still there. */
	public int checkTick;
	/** Ticks into Coach Gun's cast (-1 none). */
	public int coachCast = -1;
	/** Double-Barreled: its window's end (game time, -1 none), and whether the next cast is that second one. */
	public long doubleUntil = -1;
	public boolean doubleHit, secondShot;
	/** The cooldown Coach Gun really has during Double-Barreled's window (ticks). */
	public double heldCooldown;
	/** Ticks into Dynamite's throw (-1 none). */
	public int dynamiteCast = -1;
	/** Ticks into B.O.B.'s cast (-1 none). */
	public int bobCast = -1;
	/** Her B.O.B. and dynamite out (their entity UUIDs), or null. */
	public UUID bob, dynamite;
	/** Viper's Sting: the last scoped hit's target and when (game time). */
	public UUID stingTarget;
	public long stingAt = -1000;
	/** The last enemy she shot and when (B.O.B.'s pick, W). */
	public int lastShot = -1;
	public long lastShotAt = -1000;
	/** Hacked now. */
	public boolean silenced;

	public void clear() {
		scope = 0;
		spread = 0;
		hipWait = adsWait = 0;
		reloadPhase = -1;
		reloadTicks = 0;
		reloadCut = false;
		reloadAsked = false;
		launchGrace = 0;
		coachCast = -1;
		doubleUntil = -1;
		doubleHit = secondShot = false;
		heldCooldown = 0;
		dynamiteCast = -1;
		bobCast = -1;
		stingTarget = null;
		stingAt = -1000;
		lastShot = -1;
		lastShotAt = -1000;
		silenced = false;
	}
}
