package dev.overcraft.sombra;

/**
 * Sombra's numbers (tools/reference/sombra.md has every quote, revision and date; the game data map is sombra-map.md, build
 * 154088 of 2026-10-06, with its detail in E:\Overwatch Archive\research\sombra\work_kit\). Sources: the game data (D, with
 * its value id or graph node), Blizzard patch notes (BZ), the Overwatch wiki (W). The data is used where they disagree, as
 * the build's own code, except where a patch after the data build changed a number (BZ 2026-10-13: the pistol's damage,
 * Cyberspace's opening damage and cooldown), and the line says so; where none gives a value, it's ours and says so.
 */
public final class SombraStats {
	private SombraStats() {
	}

	/** Support since BZ 2026-10-06 ("Moved from Damage to Support role", Tactician): 225 health, no armor or shields. */
	public static final int HEALTH = 225;                      // D 3918 (121C); BZ 2026-10-06; W

	// ---- Machine Pistol (primary fire, held) ----
	/** BZ 2026-10-13 (7 -> 6.5), after the data build (D 0B1B: 7). */
	public static final double PISTOL_DAMAGE = 6.5;
	/** Hitscan, this many shots a second while held. */
	public static final double PISTOL_RATE = 20;               // D 0B1B; W
	/** Falloff from 20 m to 35 m, down to 30% (D 0B1B, M; W 1.95 of 6.5). */
	public static final double PISTOL_FALLOFF_START = 20, PISTOL_FALLOFF_END = 35, PISTOL_FALLOFF_MIN = 0.3;
	public static final int PISTOL_AMMO = 60;                  // D 0B1B; W
	/** The reload, the clip full this far in (D 0B1B: 0.48 s of 1.2 s; W 0.544 s: the data used). */
	public static final double PISTOL_RELOAD = 1.2, PISTOL_RELOAD_AMMO = 0.4;
	/** Spread: from 0 to 2 degrees over 10 shots, back to 0 after 0.5 s without firing. */
	public static final double PISTOL_SPREAD_MAX = 2, PISTOL_SPREAD_SHOTS = 10, PISTOL_SPREAD_RESET = 0.5;   // D 0B1B; W
	/** Head hits (D, M; W). */
	public static final double PISTOL_CRIT = 2;
	/** The shot's size (W; the data's hitscan size unread: ours from W). */
	public static final double PISTOL_RADIUS = 0.02;
	/** How far a shot reaches (ours: past the falloff's end the damage stays at its floor). */
	public static final double PISTOL_RANGE = 60;

	// ---- Hotfix (secondary fire, held by default) ----
	public static final int HOTFIX_CHARGES = 2;                // D 614C; BZ 2026-10-06; W
	public static final double HOTFIX_RECHARGE = 8;            // D 614C; W
	/** The ally nearest the crosshair within 17 m and 10 degrees (D 60FB: 10 x sensitivity 1.0). */
	public static final double HOTFIX_RANGE = 17, HOTFIX_CONE = 10;
	/** A 0.5 s channel, then 0.3 s of recovery (D 60FB; BZ 0.5 s; W 0.5 + 0.3). */
	public static final double HOTFIX_CAST = 0.5, HOTFIX_RECOVERY = 0.3;
	/** On an ally: this much at once, then this much a second, until this much in all or this long. */
	public static final double HOTFIX_HEAL_NOW = 65, HOTFIX_HEAL_RATE = 25, HOTFIX_HEAL_TOTAL = 175, HOTFIX_HEAL_TIME = 12;   // D 60FB; W
	/** Hacking (a health pack, an enemy deployable): no charge spent, then its own lockout. */
	public static final double HOTFIX_HACK_LOCKOUT = 6;        // D 614C; W

	// ---- Hacked (the status: D 0B1F) ----
	/** By EMP: abilities locked (silenced) and seen through walls. */
	public static final double HACK_SILENCE = 3;               // D 0B32; BZ; W
	public static final double HACK_REVEAL = 8;                // D 0B1F; W
	/** A hacked health pack (ours: a Minecraft stand-in, see the plan) and a deployable. */
	public static final double HACK_PACK = 30, HACK_DEPLOYABLE = 10;   // D 0B1F; W (pack)

	// ---- Cyberspace (ability 1: Shift; D 610E ABILITY 1) ----
	/** A 0.45 s cast, the projectile leaving 0.1 s in; the cooldown starts at the cast's end. */
	public static final double CYBER_CAST = 0.45, CYBER_FIRE_AT = 0.1;   // D 610E
	/** BZ 2026-10-13 (12 -> 13 s), after the data build (D 6102: 12). */
	public static final double CYBER_COOLDOWN = 13;
	/** Straight, no gravity (M), this fast and this wide. */
	public static final double CYBER_SPEED = 50, CYBER_RADIUS = 0.3;   // D 6102; W
	/** The zone it opens: this wide (radius) and this long. */
	public static final double ZONE_RADIUS = 5, ZONE_TIME = 3; // D 6102; W
	/** Enemies: on opening (BZ 2026-10-13 40 -> 20, after the data build), then a second. */
	public static final double ZONE_DAMAGE_NOW = 20, ZONE_DAMAGE_RATE = 25;   // D 25/s; W
	/** Allies: on opening, then a second. */
	public static final double ZONE_HEAL_NOW = 40, ZONE_HEAL_RATE = 30;       // D 6102; W
	/** Weakened enemies deal and heal this much less (D 6102, M on which channels; BZ 2026-10-06; W). */
	public static final double WEAKEN = 0.5;
	/** How long it lasts after leaving the zone: enemies (D 0.25 s; W 0.5 s: the data used), allies. */
	public static final double ZONE_LINGER_ENEMY = 0.25, ZONE_LINGER_ALLY = 0.75;

	// ---- Translocator (ability 2: E to throw, E again to teleport; D 0C19 ABILITY 2) ----
	/** The beacon: this fast, under this gravity (M), lasting this long, with this much health. */
	public static final double BEACON_SPEED = 72, BEACON_GRAVITY = 20, BEACON_LIFETIME = 15, BEACON_HEALTH = 5;   // D 0C1A
	/** It can be used this long after the throw (D, M; W 0.17 s). */
	public static final double BEACON_READY = 0.17;
	/** The teleport: protected over this long, to the beacon and this far above it. */
	public static final double TELEPORT_TIME = 0.4, TELEPORT_UP = 0.5;   // D 0C19
	/** The cooldown starts this long after landing (D 0C19). */
	public static final double TRANSLOCATOR_COOLDOWN = 7.5, TRANSLOCATOR_COOLDOWN_DELAY = 1;   // D; BZ; W

	// ---- Stealth (after every teleport: D 420C; no longer an ability of its own) ----
	public static final double STEALTH_FADE_IN = 0.75, STEALTH_TIME = 6.5, STEALTH_FADE_OUT = 0.3;   // D 420C; BZ
	/** Faster while stealthed (D, M; W +60%). */
	public static final double STEALTH_SPEED = 0.6;
	/** An enemy this near, in sight, sees her (D 420C: 4 m; the panel's 12 m is Lúcio's). */
	public static final double STEALTH_DETECT = 4;

	// ---- EMP (ultimate) ----
	public static final double ULT_COST = 3100;                // D 3918; W (1850 x 1.675)
	/** A 0.65 s cast, the pulse at 0.35 s, this wide. */
	public static final double EMP_CAST = 0.65, EMP_PULSE_AT = 0.35, EMP_RADIUS = 15;   // D 0B31; W
	/** Enemies take this share of their current health and are hacked. */
	public static final double EMP_SHARE = 0.2;                // D 0B32; W

	// ---- Perks (the live list D 005B.177) ----
	public static final double PERK_MINOR_XP = 2375, PERK_MAJOR_XP = 2375 + 3335;   // D 3918; W 2375 / 5710
	/** Life Hack (minor): EMP gives Hotfix to allies this near. */
	public static final double LIFE_HACK_RADIUS = 15;          // D 0B31
	/** CTRL ALT ESC (minor): teleporting below this share of health starts her regeneration at once (D, M on the node). */
	public static final double CTRL_ALT_ESC_BELOW = 0.5;
	/** Data Packet (major): each hack cuts Hotfix's recharge this much, the rest carried to the next charge. */
	public static final double DATA_PACKET = 4;                // D 614C; BZ; W
	/** Cybersecurity (major): Cyberspace heals allies below half health this much more. */
	public static final double CYBERSECURITY = 2, CYBERSECURITY_BELOW = 0.5;   // D 6102; W
}
