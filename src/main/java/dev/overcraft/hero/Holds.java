package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

/**
 * What a hold is and how holds stack, apart from any creature: Sleep's rules, testable offline (HoldsTest). A hold is a dart's sleep, a stun, a knock-down (it slides on its push), or a
 * float (a knock-down with its gravity scaled: Hyper Strike's). Combat.cleanse's table is the reason for cleansing().
 */
final class Holds {
	private Holds() {
	}

	/** One hold, in game time. */
	record Hold(long since, long until, boolean waking, boolean stun, boolean slides, double gravity) {
		boolean knockDown() {
			return stun && slides;
		}
	}

	static Hold sleep(long now, long until) {
		return new Hold(now, until, false, false, false, 1);
	}

	static Hold stun(long now, long until, boolean slides, double gravity) {
		return new Hold(now, until, false, true, slides, gravity);
	}

	/**
	 * The hold after {@code fresh} lands on {@code old}, or null if it doesn't land. <b>All ours:</b> 224E's replace rule isn't read past #130, and the data has no knock-down on a sleeper or a
	 * sleep on a knock-down to go by. The aim is that a stack never makes something cleansable that wasn't, never shortens a hold, and never loses a slide:
	 * <pre>
	 * old \ fresh   sleep          stun                    knock-down               float
	 * none          sleep          stun                    knock-down               float
	 * sleep         a new sleep    refused (was so)        refused (was so)         refused
	 * stun          sleep          stun, longer            knock-down, longer       float, longer
	 * knock-down    refused        knock-down, longer      knock-down, longer       knock-down, longer, no float
	 * float         refused        float, longer           knock-down, longer       float, longer
	 * </pre>
	 * A plain knock-down beats a float, so a mob doesn't float on past the float's own 0.8 s (ours).
	 */
	static @Nullable Hold stacked(@Nullable Hold old, Hold fresh) {
		if (old == null) {
			return fresh;
		}
		if (!fresh.stun) {
			return old.slides ? null : fresh;
		}
		if (!old.stun) {
			return null;
		}
		long until = Math.max(old.until, fresh.until);
		boolean plainKnockDown = old.slides && old.gravity == 1 || fresh.slides && fresh.gravity == 1;
		double gravity = plainKnockDown ? 1 : fresh.gravity != 1 ? fresh.gravity : old.gravity;
		return new Hold(fresh.since, until, false, true, old.slides || fresh.slides, gravity);
	}

	/** What a cleanse does (Combat.cleanse's table). */
	enum Cleansing {
		/** A dart's sleep: the wake damage starts, without damage's "slept 0.5 s" gate: 224E #401 -> #418 -> #204 -> #304, past #332 (H). */
		WAKES,
		/** A stun: ended on the spot (224E #402 -> #416 -> #297). */
		ENDS,
		/**
		 * A knock-down (Accretion, Catnapper: type 6BBE): 224E #400 goes nowhere (H); W "Cleanses never remove"; BZ "No longer cleanses hard knockdown stuns". And Hyper Strike's float: 54A5 reads no flag (M);
		 * W Status effect lists Hyper Strike among what cleanses remove, but in a list mostly of damage over time, and W Sigma rev 409089 has its damage "50 over 0.37 seconds": that's what W means, and ours is dealt at once. W also says cleanses never remove Knockback. Decided, ours: it stays.
		 */
		STAYS
	}

	static Cleansing cleansing(Hold hold) {
		return !hold.stun ? Cleansing.WAKES : !hold.slides ? Cleansing.ENDS : Cleansing.STAYS;
	}

	/** A movement ability's landing (HeroManager.softLanding): set while it moves them, kept until they're down after it; grace: the ground just left doesn't count. Tested: SoftLandingTest. */
	static boolean softLanding(boolean was, boolean moving, boolean onGround, boolean grace) {
		return moving || was && (!onGround || grace);
	}

	/**
	 * Whether it says it was cleansed (1C7D): a sleep (224E #418) and a stun (#416) do; a knock-down's and a float's answer goes nowhere (#400). A sleeper already waking tells again: #401's filter is the type alone and
	 * nothing gates #552 -> #418 on the waking (M: that Entry is live while waking), so a second Kiriko's Suzu says her line; a same Kiriko's is held by her 10 s.
	 */
	static boolean tells(Hold hold) {
		return cleansing(hold) != Cleansing.STAYS;
	}

	/** The hold after a cleanse: null if it's ended now; a sleep with more than its waking left goes to waking; anything else as it was. */
	static @Nullable Hold cleansed(Hold hold, long now, int wakingTicks) {
		return switch (cleansing(hold)) {
			case ENDS -> null;
			case STAYS -> hold;
			case WAKES -> !hold.waking && hold.until - now > wakingTicks ? new Hold(hold.since, now + wakingTicks, true, false, false, 1) : hold;
		};
	}

	/** Whether a player's client may be let go: nothing here still holds them, nor a Flux's first 0.4 s. */
	static boolean letsGo(boolean heldHere, boolean fluxHolding) {
		return !heldHere && !fluxHolding;
	}
}
