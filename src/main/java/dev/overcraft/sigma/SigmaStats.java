package dev.overcraft.sigma;

/**
 * Sigma's numbers (tools/reference/sigma.md, each with its source: D = his game data, build 154088; BZ = Blizzard's patch
 * notes; W = the Overwatch wiki; H / M / L = confidence), built step by step (sigma-plan.md "Order"). Nothing has changed
 * his kit since the data build (the last change BZ 2026-07-14 is in it), so D's numbers are the live ones.
 */
public final class SigmaStats {
	private SigmaStats() {
	}

	// ---- base (sigma.md "Base") ----
	/** Health without the role's bonus (D Hero Stats 0DA0 047C 200, alt 300; W 200) H. The Tank role adds 150 (Tank). */
	public static final int HEALTH = 200;
	/** Shields (D 047D 250; BZ 2026-07-14 275 -> 250; W 250) H. */
	public static final int SHIELDS = 250;
	/** Ult cost, Gravitic Flux (D 0AFA 2300, alt 3800; W 2300) H. */
	public static final double ULT_COST = 2300;
	/** Perk XP: the minor at 1955, the major 2935 more (4890) (D 12AA / 12AB on 34C3 #99; W 1955 / 4890) H. */
	public static final double PERK_MINOR_XP = 1955, PERK_MAJOR_XP = 4890;

	// ---- the cooldowns the HUD shows (the abilities come in later steps) ----
	/** Kinetic Grasp: 12 s from its end (D 0494; W 12 s) H. */
	public static final double GRASP_COOLDOWN = 12;
	/** Accretion: 10 s from 0.45 s after the throw (D 0498, #669; W 10 s) H. */
	public static final double ACCRETION_COOLDOWN = 10;

	// ---- his size (sigma.md "Size") ----
	/**
	 * His mover: 2.5 m tall, 0.7 m radius, crouched 2.05 m (D 2AB1, the default hero's 2.0 / 0.5 / 1.25) H. His body box in
	 * hero mode (dev.overcraft.hero.HeroBody: the owner, 2026-11-02, "Sigma should be unable to fit through a door"); his
	 * model is drawn at its data size (2.14 m: D 106F7 bounds), floating 0.3 m up in his stand (his clips).
	 */
	public static final double MOVER_HEIGHT = 2.5, MOVER_RADIUS = 0.7, MOVER_CROUCH_HEIGHT = 2.05;
	/** Falls hurt him from Minecraft's 3 m (Hero.safeFall's default): his data has the default falling component (D) L. */
	public static final double SAFE_FALL = 3;
}
