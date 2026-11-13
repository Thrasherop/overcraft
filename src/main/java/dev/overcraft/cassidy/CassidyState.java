package dev.overcraft.cassidy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cassidy's live state on the server (HeroState.cassidy; timers in ticks, game times in game ticks). One object, so his kit's
 * fields don't spread through HeroState.
 */
public final class CassidyState {
	/** Fan the Hammer: shots left in this fan (0 none), ticks to the next. */
	public int fanLeft;
	public double fanNext;
	/** A fan's shots so far (the first is exact). */
	public int fanShot;
	/** Peacekeeper locked until this game time (after a fan). */
	public long primaryLockUntil;
	/** Ticks into Combat Roll (-1 not rolling), the rounds it reloaded. */
	public int roll = -1;
	/** Giddy Up's ticks since the roll ended (-1 none). */
	public int giddy = -1;
	/** Ticks into Flashbang's cast (-1 none), and its recovery's end. */
	public int flashCast = -1;
	/** Ticks into Deadeye (-1 none): its cast, then the channel. */
	public int deadeye = -1;
	/** Deadeye firing: ticks since the press (-1 not firing), and its shots' order. */
	public int deadeyeFire = -1;
	public int[] deadeyeOrder = new int[0];
	/** Deadeye cancelled: ticks left of its 0.1 s (-1 none). */
	public int deadeyeCancel = -1;
	/** Deadeye's targets by entity id: each one's lock time (ticks) and charge (Overwatch damage). */
	public final Map<Integer, Lock> locks = new LinkedHashMap<>();
	/** Silver Bullet: its cooldown (ticks), aimed now (the right button held with it ready). */
	public double silverCooldown;
	public boolean silverAim;
	/** Hacked now (no abilities), hindered now (no roll: Flashbang's status). */
	public boolean silenced, hindered;
	/** When "It's high noon" is said (game time, -1 none). */
	public long lineAt = -1;
	/**
	 * His client's packets the server has seen that could change whether a roll may start (E, Q, or a roll it made), mod 256, and the
	 * rolls the server refused or cut short, mod 256: both synced back (Cassidy.SEEN_SHIFT, CUT_SHIFT) so his client rolls only when the
	 * server will take it and stops a roll the server won't (critic b79a4d1 #1). Kept across clear(): his client's count goes on.
	 */
	public int pressSeen, rollCut;
	/** Deadeye: enemies in the cone and in sight this tick (it can fire only with one: W), Q pressed again (fires, W). */
	public int inSight;
	public boolean ultAgain;

	/** One Deadeye target. */
	public static final class Lock {
		public double ticks;
		public double charge;
	}

	public void clear() {
		fanLeft = 0;
		fanNext = 0;
		fanShot = 0;
		primaryLockUntil = 0;
		roll = -1;
		giddy = -1;
		flashCast = -1;
		deadeye = -1;
		deadeyeFire = -1;
		deadeyeOrder = new int[0];
		deadeyeCancel = -1;
		locks.clear();
		silverCooldown = 0;
		silverAim = false;
		silenced = hindered = false;
		lineAt = -1;
		inSight = 0;
		ultAgain = false;
	}
}
