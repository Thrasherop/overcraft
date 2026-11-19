package dev.overcraft.widowmaker;

/**
 * Widowmaker's numbers (#138). Sources: the game data (D: build 154088 of 2026-10-06, tools/reference/widowmaker-map.md and its
 * node-by-node reading E:\Overwatch Archive\research\widowmaker\work_kit\kit_widowmaker.md: the value id or graph node), Blizzard's
 * patch notes (BZ) and the Overwatch wiki (W rev 409087): tools/reference/widowmaker.md has every date. Her last number change is BZ
 * 2026-04-14 (perk costs) and her last kit change BZ 2026-02-10, both before the data build, so the data's numbers are the live ones
 * (BZ 2026-10-06 only fixed a reload bug; 2026-11 has no line for her). No Workshop recording of her: her movement is the default
 * mover's (D). Confidence: H read in the data and the sources agree, M one link inferred, L a guess; ours where no source gives one.
 */
public final class WidowmakerStats {
	private WidowmakerStats() {
	}

	/** Damage, Sharpshooter: 225 health, no armor or shields (D 0BA9 225, 4BE6 #1; BZ 2025-03-25 200 -> 225; W) H. */
	public static final int HEALTH = 225;

	/** Infra-Sight's cost: 1400 (D 0B01 1400, M: no node reads it; W 1400) H/M. */
	public static final double ULT_COST = 1400;

	/** Perk XP: 1410 to the minor, 1920 more to the major (D 12E4 / 12E5, 4BE6 #8; BZ 2026-04-14 -6 % / -4 %) M. */
	public static final double PERK_MINOR_XP = 1410, PERK_MAJOR_XP = 1410 + 1920;

	// ---- Widow's Kiss from the hip (001B #17: the left button, not scoped) ----
	/** 14 a shot (D 1AAE -> v69694, volley #17; BZ 2025-05-20 13 -> 14; W) H. */
	public static final double HIP_DAMAGE = 14;
	/** Ten a second, automatic (D v8421 10 x v21541, #721 / #17; W) H. */
	public static final double HIP_RATE = 10;
	/** Falloff 20 -> 40 m (D 1AC7 / 1AC6) H, down to x0.3 (W "14 - 4.2"; the data's floor v37674 comes from the shared stats graph, unread) L. */
	public static final double HIP_FALLOFF_START = 20, HIP_FALLOFF_END = 40, HIP_FALLOFF_MIN = 0.3;
	/**
	 * Spread 0 -> 3 degrees over 11 shots (D v229 / 1E4D -> v230 #723, 1E4E -> volley m_AC20B1C1 11; W 3 deg after 11; BZ 2025-05-20's
	 * 2 deg "never went through" per W) H/M; it recovers at 4 a second (all of it in 0.25 s) once 0.2 s have passed since the last hip
	 * shot (D #69, v6618 0.2 #263 - #265) H/M.
	 */
	public static final double HIP_SPREAD = 3, HIP_SPREAD_SHOTS = 11, HIP_SPREAD_RECOVERY = 4, HIP_SPREAD_WAIT = 0.2;
	/** After a hip burst ends, 0.15 s before a scoped charge can start (D v173 0.15 on the volley's end, #60) H/M. */
	public static final double HIP_RECOVERY = 0.15;

	// ---- Take Aim (001B: the right button held, or toggled; scoped shots on the left) ----
	/** Into the scope in 0.333 s, out in 0.333 s, no shot during either (D v169 ChaseVar rate 3, #120 / #123, #136; W 0.32 s) H. */
	public static final double SCOPE_IN = 1 / 3.0, SCOPE_OUT = 1 / 3.0;
	/** The scope's view: 30 degrees (D v172 30, #44 -> #32 at full scope; L as a vertical field of view, as Ana's 30 and Ashe's 40). */
	public static final double SCOPE_FOV = 30;
	/** Fully scoped, the mouse turns at 30 % (D 001B #115 0.3 / 0.3, the hero setting "zoomed aim sensitivity" v7648 / v24086) H. */
	public static final double SCOPED_SENSITIVITY = 0.3;
	/** Fully scoped she moves 65 % slower: 1.925 m/s (D v23184 += 0.65, value 1E5A, #370, M the field; W -65 %) H/M. */
	public static final double SCOPE_SLOW = 0.65;
	/**
	 * The charge: v174 from 1 to 10 at 10 a second (0.9 s), linear, starting once fully scoped and the 0.5 s recovery is over; back to
	 * 1 after each scoped shot and on unscoping (D 1AC0 / 1ABB / 1E77, #124 / #126 / #132 / #125 / #597; W 0.9 s, linear) H.
	 */
	public static final double CHARGE_MIN = 1, CHARGE_MAX = 10, CHARGE_RATE = 10;
	/** A scoped shot deals 12 x the charge: 12 at none, 120 at full (D 1ABA x v2489; W 12 - 120) H. */
	public static final double SCOPED_DAMAGE_PER = 12;
	/** Falloff 50 -> 70 m to x0.5 (D 1AC4 / 1AC3 / 1AD5; BZ 2024-02-21; W) H. */
	public static final double SCOPED_FALLOFF_START = 50, SCOPED_FALLOFF_END = 70, SCOPED_FALLOFF_MIN = 0.5;
	/** x2.5 on the head (D crit field 1CEA 1.25 x the default 2, M; W 2.5x) H/M; the hip shot's x2 (D default, M; W) H. */
	public static final double SCOPED_CRIT = 2.5, HIP_CRIT = 2;
	/** 0.5 s from a scoped shot before the next charge starts (D #2 v173 0.5 on the volley's end; W) H. */
	public static final double SCOPED_RECOVERY = 0.5;
	/** A scoped shot takes 5 rounds (D 1ACC -> v65941; W) H; 3 with Scoped Efficiency (D 51EF #343) H. */
	public static final int SCOPED_COST = 5, SCOPED_COST_EFFICIENT = 3;
	/** Under half a charge the scoped shot is the softer one with a thinner trail (D entity 0107 under 50 %, 004B to 99 %, 0108 full, #729) H. */
	public static final double CHARGE_SOFT = 0.5;
	/** After a jump while scoped the scope stays down 0.5 s (D #188 / #189 / #192) H. */
	public static final double JUMP_UNSCOPE = 0.5;

	/** Both: hitscan; the hip shot 0.04 m across (D 28A2 -> v54890; BZ 2024-12-10), the scoped 0 m (D; BZ 2025-01-07) H. */
	public static final double HIP_RADIUS = 0.04, SCOPED_RADIUS = 0.0;
	/** The longest shot (D range 400; ours 200 m, as far as Minecraft draws). */
	public static final double RANGE = 200;
	/** 35 rounds (D 1AC5 -> v198, #699; W) H. */
	public static final int AMMO = 35;
	/**
	 * The reload, 1.5 s: the rounds are back at 0.825 s (1.5 x 0.55), no shot until 1.5 s, no scope for 1.35 s; R or an empty
	 * magazine; nothing cuts it (D 1ABD, anim 0004 #172, #273 -> #271, #269, #303, #10 / #4; W 1.5 s, ammo at 0.816 s) H.
	 */
	public static final double RELOAD = 1.5, RELOAD_AMMO_AT = 0.825, RELOAD_NO_SCOPE = 1.35;

	// ---- Grappling Hook (005A, Shift) ----
	/** 0.15 s from the press to the hook's throw (D #101 category 0052) H. */
	public static final double HOOK_WINDUP = 0.15;
	/** The hook flies at 40 m/s to 20 m (D 1AB0, #15) H; W 20 m. */
	public static final double HOOK_SPEED = 40, HOOK_RANGE = 20;
	/** It pulls her at 20 m/s (D 212B -> v1002, MoveTo #21; W 20 m/s) H. */
	public static final double PULL_SPEED = 20;
	/** Only from 2 m off or more: nearer, it reels back as a miss (D #266) H. */
	public static final double PULL_MIN = 2;
	/** A ledge within 3 m of the hook's point pulls her onto it (D #138) H. */
	public static final double LEDGE_REACH = 3;
	/** The pull ends within 0.3 m of the point (D MoveTo field 0.3, L as the tolerance), or at 1.5 s (ours: no pull longer than 20 / 20 m/s). */
	public static final double PULL_ARRIVE = 0.3, PULL_MAX_TIME = 1.5;
	/** On arrival a 0.5 s end, the last 0.2 s push carrying her on (D #30, STU_1F5ED928 0.2, M as the kept momentum) M; ours: 0.35 of the pull's speed. */
	public static final double PULL_END = 0.5, PULL_KEEP = 0.35;
	/** Jump cancels it from 0.02 s in (D #178 -> #24) H. */
	public static final double PULL_JUMP_FROM = 0.02;
	/** A miss reels the hook back at 120 m/s with no cooldown (D #288 / #162 / #22) H/M; W "returns, no cooldown". */
	public static final double HOOK_REEL_SPEED = 120;
	/** 10 s, from the pull's end, a jump cancel or an abort (D 1AAF, #160 / #179 / #187; BZ 2025-09-16 12 -> 10) H. */
	public static final double HOOK_COOLDOWN = 10;
	/** The hook's size against blocks (ours: 0.2 m; the data's projectile size unread). */
	public static final double HOOK_RADIUS = 0.1;

	// ---- Venom Mine (0056 the throw, 0057 the mine, 0073 the poison; E) ----
	/** Let go 0.2 s after the press (anim 001E, 1 s) (D #41) H; W "0 s cast" M. */
	public static final double MINE_RELEASE = 0.2, MINE_CAST = 1.0;
	/** 20 m/s, falling at 9.8, 0.2 m (D volley #109, #21) H. */
	public static final double MINE_SPEED = 20, MINE_GRAVITY = 9.8, MINE_SIZE = 0.2;
	/** 15 s, starting 1.0 s after the press (D 1AD7, #42 -> #87) H. */
	public static final double MINE_COOLDOWN = 15, MINE_COOLDOWN_AT = 1.0;
	/** Armed 1 s after it sticks (D v2277, 0057 #44; W 1.4 s, likely from the press) M. */
	public static final double MINE_ARM = 1;
	/** An enemy within 3 m sets it off; the burst takes enemies within 4 m (D 1E96 #54 / 1E97 #39; W) H. */
	public static final double MINE_TRIGGER = 3, MINE_BURST = 4;
	/** 1 health (D #59; W) H. */
	public static final double MINE_HEALTH = 1;
	/** The poison: 125 over 5 s, no crit, through armor (D 1AD8 25 x 1B7E 5, 0073 #48; BZ 2025-09-16 75 -> 125; W) H; ours in 0.2 s steps (W 0.192 s). */
	public static final double POISON_TOTAL = 125, POISON_TIME = 5, POISON_STEP = 0.2;
	/** Those it poisons are revealed to her while the poison runs, and 1 s on (D effect 02FF #62 / #69: 5.1 s) M. */
	public static final double MINE_REVEAL = 5.1;
	/** Its lifetime before it sticks (D volley life 10 s) H. */
	public static final double MINE_FLIGHT_LIFE = 10;

	// ---- Infra-Sight (0067, Q) ----
	/** 0.65 s cast, the charge spent at its start (D #76 / #75, #105, anim 0029; W 0.64 s) H. */
	public static final double ULT_CAST = 0.65;
	/** 15 s (D 1AD6 -> v567, #58; W) H; every enemy within 200 m of her (D #13 TrackTargets 200; W "global") H, shown to her team (D #116, M). */
	public static final double ULT_TIME = 15, ULT_RANGE = 200;

	// ---- Sharpshooter (01C7 #2832, tag 1188) ----
	/** A critical hit on an enemy takes 0.0075 s a point off Grappling Hook (D 01C7 #2832 -> 005A #405 / #407; BZ 2026-02-10; W) H. */
	public static final double SHARPSHOOTER = 0.0075;

	// ---- Perks (51EF; her live list 0C32 Scoped Efficiency, 0D3A Sniper's Instinct; 0E39 Widow's Bite, 0D1E Seeker Mine) ----
	/** Sniper's Instinct (minor): the charge twice as fast for 2 s from the pull's end (D 51EF #354 / #355, v37621 +1; W) H/M. */
	public static final double INSTINCT_TIME = 2, INSTINCT_RATE = 2;
	/** Widow's Bite (major): the charge goes to 12.5 (1.15 s, 150 damage) and pierces enemies above 100 % (D 001B #742, #740 / #746) H/M; W "at 125 %". */
	public static final double BITE_CHARGE_MAX = 12.5;
	/**
	 * Seeker Mine (major): the mine stays and, armed, shoots a dart at each enemy within 10 m in sight, at 15 m/s, 75 over 5 s each
	 * (D 59BF #164 / #165 / #175, 59C0 #48; BZ 2026-02-10 50 -> 75), every 2 s (D #184, M), re-armed 8 s after it fires (D #82; W 8.2) H/M.
	 */
	public static final double SEEKER_RANGE = 10, SEEKER_DART_SPEED = 15, SEEKER_TOTAL = 75, SEEKER_REPEAT = 2, SEEKER_REARM = 8;

	/** Where her shots' look leaves from (right, up, ahead from her eyes, m): the Kiss at the hip; ours. */
	public static final double MUZZLE_RIGHT = 0.18, MUZZLE_UP = -0.12, MUZZLE_AHEAD = 0.75;
	/** Where the hook leaves her (her left wrist's launcher, hp 0055; ours as a point from her eyes). */
	public static final double HOOK_RIGHT = -0.25, HOOK_UP = -0.2, HOOK_AHEAD = 0.5;
	/** Where the mine leaves her hand (hp 002A; ours). */
	public static final double THROW_RIGHT = -0.2, THROW_UP = -0.15, THROW_AHEAD = 0.5;
}
