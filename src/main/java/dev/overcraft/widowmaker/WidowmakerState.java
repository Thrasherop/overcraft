package dev.overcraft.widowmaker;

import java.util.UUID;

/** Widowmaker's live state on the server (HeroState.widowmaker; timers in ticks, game times in game ticks). */
public final class WidowmakerState {
	/** The scope: 0 hip .. 1 fully scoped. */
	public double scope;
	/** Take Aim's charge: 1 (none) .. 10 (full; 12.5 with Widow's Bite) (D v174). */
	public double charge = WidowmakerStats.CHARGE_MIN;
	/** Ticks before the charge can start again (the 0.5 s after a scoped shot, the 0.15 s after a hip burst: D v173). */
	public double recovery;
	/** Ticks to the next hip shot. */
	public double hipWait;
	/** Hip shots fired in the current spread (0 .. 11), and ticks before it starts to recover. */
	public double spreadShots;
	public int spreadWait;
	/** Ticks the scope stays down after a jump while scoped (D #188). */
	public int jumpUnscope;
	/** Ticks into the reload (-1 none), and whether its rounds are back yet. */
	public double reloadTicks = -1;
	public boolean reloadFilled;
	/** Grappling Hook: its phase (0 none, 1 wind-up, 2 the hook flying, 3 the pull, 4 the end, 5 reeling back), ticks into it. */
	public int hookPhase;
	public double hookTicks;
	/** The hook entity (flying or holding), the pull's point. */
	public UUID hook;
	public double pullX, pullY, pullZ;
	/** The pull's direction last tick (for the momentum kept at its end). */
	public double pullDX, pullDY, pullDZ;
	/** Sniper's Instinct: until when the charge runs twice as fast (game time, -1 none). */
	public long instinctUntil = -1;
	/** Ticks into Venom Mine's throw (-1 none). */
	public int mineCast = -1;
	/** Her mine out (its entity UUID), or null. */
	public UUID mine;
	/** Ticks into Infra-Sight's cast (-1 none), and when it ends (game time, -1 none). */
	public int ultCast = -1;
	public long ultUntil = -1;
	/** A launch (the pull's end) counts as moving her until she's down again: lands soft (CLAUDE.md, #35). */
	public int launchGrace;
	/** Ticks counted for the once-a-second check that her mine is still there. */
	public int checkTick;
	/** Hacked now. */
	public boolean silenced;
	/** The jump key last tick (a jump's edge: it drops the scope, cancels the pull). */
	public boolean jumpWas;

	public void clear() {
		scope = 0;
		charge = WidowmakerStats.CHARGE_MIN;
		recovery = 0;
		hipWait = 0;
		spreadShots = 0;
		spreadWait = 0;
		jumpUnscope = 0;
		reloadTicks = -1;
		reloadFilled = false;
		hookPhase = 0;
		hookTicks = 0;
		hook = null;
		instinctUntil = -1;
		mineCast = -1;
		ultCast = -1;
		ultUntil = -1;
		launchGrace = 0;
		silenced = false;
		jumpWas = false;
	}
}
