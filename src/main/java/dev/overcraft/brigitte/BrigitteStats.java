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

	// ---- Barrier Shield (0EE3 on her, 0EE4 on the barrier pet 2284) ----
	/** Its health (BZ 2026-04-14 300; D 0EEF), 750 in Rally (D; W). */
	public static final double BARRIER_HEALTH = 300, BARRIER_HEALTH_RALLY = 750;
	/** Down, it comes back from 2 s at 85 a second (D 0EF0 / 0EF1; W). */
	public static final double BARRIER_REGEN_DELAY = 2, BARRIER_REGEN = 85;
	/** Broken: this long before it can go up again (D 1136; W). */
	public static final double BARRIER_BROKEN = 5;
	/** Up, she's this much slower (D v23184 0.3; her shield-walk clips are 0.7 x her run). */
	public static final double BARRIER_SLOW = 0.3;
	/** 2% of what it takes is ult charge for her (0EE4 #149, M). */
	public static final double BARRIER_ULT_SHARE = 0.02;
	/** Its size: the deployed shield mesh 1064F (idle_up 1.51 x 2.64 m; Rally's idle_up_rally 3.75 x 2.35 m), measured. */
	public static final double BARRIER_WIDTH = 1.51, BARRIER_HEIGHT = 2.64, BARRIER_WIDTH_RALLY = 3.75, BARRIER_HEIGHT_RALLY = 2.35;
	/** How far ahead of her it stands (ours: on her left hand's bone 002C in the data, about there). */
	public static final double BARRIER_AHEAD = 0.9;

	// ---- Shield Bash (0EE3 #495) ----
	/** A 0.3 s dash at 4x her speed (D #495 StateWait 0.3, 1C9C 4.0: about 6.6 m, M). */
	public static final double BASH_TIME = 0.3, BASH_SPEED = 5.5 * 4;
	public static final double BASH_DAMAGE = 70;               // D 0B20; W
	/** Its knockback (D 0B21 6 m/s). */
	public static final double BASH_KNOCKBACK = 6;
	/** Who it hits: one enemy within this reach ahead of her, in this cone (D #501: radius 2.48, ~35 degrees, L). */
	public static final double BASH_REACH = 2.5, BASH_CONE = 35;

	// ---- Perks (the live list D 0063.177) ----
	public static final double PERK_MINOR_XP = 2035, PERK_MAJOR_XP = 2035 + 3055;   // D (her values); W 2035 / 5090
}
