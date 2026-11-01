package dev.overcraft.brigitte;

/**
 * Brigitte's numbers (tools/reference/brigitte.md has every quote, revision and date; the game data map is brigitte-map.md,
 * build 154088 of 2026-10-06, with its detail in E:\Overwatch Archive\research\brigitte\work_kit\). Sources: the game data
 * (D, with its value id or graph node), Blizzard patch notes (BZ), the Overwatch wiki (W). No patch has touched her since
 * the data build (BZ's last: 2026-09-08), so the data's numbers are the live ones; where none gives a value, it's ours and
 * says so. Filled in step by step (brigitte-plan.md, "Order").
 */
public final class BrigitteStats {
	private BrigitteStats() {
	}

	/** Support, Survivor (BZ 2026-02-10): 175 health and 75 armor (BZ 2026-09-08: 200 -> 175, 50 -> 75). */
	public static final int HEALTH = 175;                      // D 4411; BZ 2026-09-08; W
	public static final int ARMOR = 75;                        // D 4411; BZ 2026-09-08; W

	/** Rally's cost (D 0AE1; W 2700: BZ gives only percentages). */
	public static final double ULT_COST = 2700;

	// ---- Rocket Flail (the left button held; also her quick melee: 1248) ----
	public static final double FLAIL_DAMAGE = 45;              // D 0B36 (1248 #780); W
	/** Its reach (D 113A x her scale, #805; W 6 m). */
	public static final double FLAIL_RANGE = 6;
	/** A swing: 0.2 s to the hit, then 0.4 s (D v670 / v567): one every 0.6 s. */
	public static final double FLAIL_WINDUP = 0.2, FLAIL_RECOVERY = 0.4;
	/** The hit sweeps from 0.2 s to 0.4 s into the swing (1248 #679-#81, M on the chain). */
	public static final double FLAIL_SWEEP = 0.2;
	/** The sweep's arc, eased (yaw -17.5 + 35 t^2; the backhand mirrored: 1248 #715 / #717, H expressions). */
	public static final double FLAIL_ARC = 35;
	/** How far above or below her aim the sweep reaches (ours: the data's radius operand isn't read, L). */
	public static final double FLAIL_PITCH = 30;

	// ---- Inspire (12D8 -> 11B9) ----
	public static final double INSPIRE_HEAL_NOW = 12;          // D 2A2C; BZ 2026-07-14; W
	public static final double INSPIRE_HEAL_OVER_TIME = 45;    // D 0B24; BZ 2026-07-14; W
	public static final double INSPIRE_TIME = 4;               // D 1344; W
	public static final double INSPIRE_RADIUS = 20;            // D 1345; W
	/** The flail can start it at most this often (D 2ABF; W). */
	public static final double INSPIRE_COOLDOWN = 1.25;

	// ---- Cooldowns the HUD shows (each ability's step fills in the rest) ----
	/** Whip Shot (ABILITY 1), from the flail's return (D 1259). */
	public static final double WHIP_COOLDOWN = 4;              // D 1259; W
	/** Repair Pack (ABILITY 2): 3 charges, each back 5 s after (D 0EDD; W). */
	public static final int REPAIR_CHARGES = 3;
	public static final double REPAIR_RECHARGE = 5;
	/** Shield Bash, from the dash's end (D 0EE3; W). */
	public static final double BASH_COOLDOWN = 5;
	/** Barrier Shield's health (BZ 2026-04-14 300; D 2284). */
	public static final double BARRIER_HEALTH = 300;

	// ---- Perks (the live list D 0063.177) ----
	public static final double PERK_MINOR_XP = 2035, PERK_MAJOR_XP = 2035 + 3055;   // D (her values); W 2035 / 5090
}
