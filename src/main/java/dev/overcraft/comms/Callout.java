package dev.overcraft.comms;

import org.jspecify.annotations.Nullable;

/**
 * The communication wheel's callouts (comms-reference.md: the owner's Overwatch 2 screenshot), in the wheel's order
 * clockwise from the top, then the tap's GOOD JOB. Each names the hero's voice pool ("<hero>.voice.call_<id>", from
 * the hero's HeroVoice folders) and its chat line ("overcraft.callout.<id>").
 */
public enum Callout {
	NO("no"),
	HELLO("hello"),
	NEED_HEALING("need_healing"),
	GROUP_UP("group_up"),
	ULTIMATE_STATUS("ultimate_status"),
	NEED_HELP("need_help"),
	SORRY("sorry"),
	THANKS("thanks"),
	/** A quick tap of the wheel's key (the owner). Not on the wheel. */
	GOOD_JOB("good_job");

	/** The eight on the wheel, clockwise from the top. */
	public static final Callout[] WHEEL = {NO, HELLO, NEED_HEALING, GROUP_UP, ULTIMATE_STATUS, NEED_HELP, SORRY, THANKS};

	public final String id;

	Callout(String id) {
		this.id = id;
	}

	public static @Nullable Callout byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
