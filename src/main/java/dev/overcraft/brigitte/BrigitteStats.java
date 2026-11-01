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
