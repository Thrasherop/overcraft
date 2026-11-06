package dev.overcraft.brigitte;

/**
 * Brigitte's numbers (tools/reference/brigitte.md has every quote, revision and date; the game data map is brigitte-map.md,
 * build 154088 of 2026-10-06, with its detail in E:\Overwatch Archive\research\brigitte\work_kit\). Sources: the game data
 * (D, with its value id or graph node), Blizzard patch notes (BZ), the Overwatch wiki (W). One patch has touched her since
 * the data build: BZ 2026-11-03, Inspire's two numbers, marked below (checked again 2026-11-05: BZ's last is that one). The
 * data's numbers are otherwise the live ones; where none gives a value, it's ours and says so. Filled in step by step (brigitte-plan.md, "Order").
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
	/** BZ 2026-11-03 (12 -> 15), after the data build (D 2A2C: 12); W rev 408812 lists the patch. */
	public static final double INSPIRE_HEAL_NOW = 15;
	public static final double INSPIRE_HEAL_OVER_TIME = 45;    // D 0B24; BZ 2026-07-14; W
	public static final double INSPIRE_TIME = 4;               // D 1344; W
	public static final double INSPIRE_RADIUS = 20;            // D 1345; W
	/** The flail can start it at most this often: BZ 2026-11-03 (1.25 -> 1 s), after the data build (D 2ABF: 1.25); W rev 408812 "1 second". */
	public static final double INSPIRE_COOLDOWN = 1;

	// ---- Cooldowns the HUD shows (each ability's step fills in the rest) ----
	// ---- Whip Shot (Shift: 1259) ----
	/** Its cooldown, from the flail's return (D 133C; #1033 -> #688). */
	public static final double WHIP_COOLDOWN = 4;              // D 133C; W
	public static final double WHIP_DAMAGE = 70;               // D 0B38; W
	/** 0.2 s, then a straight shot at 80 m/s to 20 m, 0.5 m round, one target, no gravity (D #593, 2B27, 133D, 2B26). */
	public static final double WHIP_WINDUP = 0.2, WHIP_SPEED = 80, WHIP_RANGE = 20, WHIP_RADIUS = 0.5;
	/**
	 * The push: her level aim at the launch x 25 m/s and 3 m/s up. D: 1259 #638's pair, m_E33EFEE1 = v9275 = 133E's 25 and m_583C208B = 3 x scale, read as horizontal and vertical as
	 * Accretion's 6 / 3 is (reading_game_data.md), W "H:25, V:3" (25.18 m/s). The same node's m_1376B4DF is 25 across + 2 up (25.08 along her aim in its other branch), so the data holds both a 2 and a 3 and
	 * doesn't say which moves them; 25.08 is exactly the length of (25, 2), so that vector is consistent with 2. **Why 3 here: nobody has measured Overwatch.** W says "~8 meter", and our mover on Overwatch's air numbers lands a hero at 8.04 m with 3 up, 5.88 m with 2
	 * (runs/polish4/whip_lift3_experiment.txt). M. Along her level aim: #638's first branch; the second (v417, not read) isn't built. Its scale v14131 is taken as 1 (not read).
	 */
	public static final double WHIP_KNOCKBACK = 25, WHIP_LIFT = 3;
	/**
	 * How long a mob's push is held. **Ours:** the data has one push, and a mob on Minecraft's ground friction stops at 4.66 m. 0.15 s gets it to W's distance: 7.99 m on a husk (same file; 0.1 s 7.5 m, 0.2 s 8.41 m).
	 * A player has no hold: one push, their client moves them (8.04 m in hero mode; 4.66 m in Minecraft mode, Minecraft's friction: left so, ours).
	 */
	public static final double WHIP_PUSH_TIME = 0.15;
	/** The flail's way back: 0.3 s from a hit, a wall or the end of its reach (D 002B's returns, ours the shorter 0.175 s
	 * under 5 m left out). */
	public static final double WHIP_RETURN = 0.3;

	// ---- Repair Pack (E: 0EDD on her, 0EE1 on the ally) ----
	/** 3 charges, each back 5 s after the last, one at a time (D 133B / 133A; W). */
	public static final int REPAIR_CHARGES = 3;
	public static final double REPAIR_RECHARGE = 5;
	/** An ally within 25 m and 10 degrees of her aim, in sight, never herself (D 1CB5, #391; W). */
	public static final double REPAIR_RANGE = 25, REPAIR_CONE = 10;
	/** It flies at 55 m/s, homing (D v6368). */
	public static final double REPAIR_SPEED = 55;
	/** 25 at once, then 100 over 2 s (D 1337, 1338 x 1339; W); another pack on them: 2 s more and another 25 (#466). */
	public static final double REPAIR_HEAL_NOW = 25, REPAIR_TIME = 2, REPAIR_HEAL_RATE = 50, REPAIR_EXTEND = 2;
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
	/** Rally's pulse (219D) starts this long into the cast (12BE #81: after 0.5 s, H). */
	public static final double RALLY_PULSE_AT = 0.5;
	/** Rally's last seconds, when its ending sound plays (5420: the last 3 s, H). */
	public static final double RALLY_ENDING = 3;

	// ---- Shield Bash (0EE3 #495) ----
	/**
	 * The dash: 25.85 m/s for 0.256 s (R: the owner's Workshop recording 2026-11-01, two bashes, 16 ticks at 62.5 a second:
	 * 6.6 m; D #495 StateWait 0.3 and 1C9C 4.0 read as 22 m/s for 0.3 s, the same distance), gravity acting on her all the
	 * while (R: her fall went on at 17.5 m/s/s through it); at its end her speed is cut to 15 m/s, its direction kept (R:
	 * 14.69 / -3.01 and 14.83 / -2.24 a tick later), which her air decay or the ground's friction then takes off.
	 */
	public static final double BASH_TIME = 0.256, BASH_SPEED = 25.85, BASH_EXIT_SPEED = 15;
	public static final double BASH_DAMAGE = 70;               // D 0B20; W
	/** Its knockback, the pair (D 0EE3 #503, and #886 on the shockwave: m_E33EFEE1 v9275 = 0B21's 6, m_583C208B 3, as the whip's and Accretion's), W kbspeed 6 m/s. Its scale v14130 is taken as 1 (not read). */
	public static final double BASH_KNOCKBACK = 6, BASH_LIFT = 3;
	/** Who it hits: one enemy within this reach ahead of her, in this cone (D #501: radius 2.48, ~35 degrees, L). */
	public static final double BASH_REACH = 2.5, BASH_CONE = 35;

	// ---- Rally (Q: 12BE on her, 12C1 on each ally) ----
	/** A 0.7 s cast lock, then 10 s (D #88, 0B13; W). */
	public static final double RALLY_CAST = 0.7, RALLY_TIME = 10;
	/** Her armor +100 and her speed +15% while it lasts (D #204 / #148, M the speed: not on the panel; W). */
	public static final double RALLY_ARMOR = 100, RALLY_SPEED = 0.15;
	/** Every 0.5 s, her and her allies within 8 m in sight get 15 overhealth, up to 100, each lasting 30 s (D #158, 1342,
	 * 1341; W). */
	public static final double RALLY_PULSE = 0.5, RALLY_RADIUS = 8, RALLY_OVERHEALTH = 15, RALLY_OVERHEALTH_MAX = 100,
		RALLY_OVERHEALTH_TIME = 30;
	/** Shield Bash in Rally: a 0.75 s stun, and 0.15 s after its hit a shockwave 8 m round hitting up to 10 enemies with the
	 * bash's 70, push and stun (D 1340, #878 / #877: M the shockwave's shape). */
	public static final double RALLY_STUN = 0.75, RALLY_SHOCKWAVE_AT = 0.15, RALLY_SHOCKWAVE_RADIUS = 8;
	public static final int RALLY_SHOCKWAVE_TARGETS = 10;

	// ---- Perks (the live list D 0063.177) ----
	public static final double PERK_MINOR_XP = 2035, PERK_MAJOR_XP = 2035 + 3055;   // D (her values); W 2035 / 5090
	/** Combat Medic (minor): each enemy her flail damages takes this off Repair Pack's recharge (51F2 #466, 2623 0.75). */
	public static final double COMBAT_MEDIC = 0.75;
	/** Morale Boost (minor): Inspire from Whip Shot lasts this much longer, healing as much more (11B9 #440: 7 s, 78.75). */
	public static final double MORALE_BOOST = 1.75;
	/** Inspiring Strike (major): +30% speed for 2 s after the bash; a bash hit heals 57 at once in 20 m (51F2 #455, #459). */
	public static final double INSPIRING_STRIKE_SPEED = 0.3, INSPIRING_STRIKE_TIME = 2, INSPIRING_STRIKE_HEAL = 57;
	/** Whiplash (major): a whipped enemy meeting a wall within 0.5 s takes 60 (1259 #997, 551F #526; BZ 2026-06-16 60). */
	public static final double WHIPLASH_DAMAGE = 60, WHIPLASH_TIME = 0.5;
}
