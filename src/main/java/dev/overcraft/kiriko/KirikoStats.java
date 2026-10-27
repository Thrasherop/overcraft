package dev.overcraft.kiriko;

/**
 * Kiriko's numbers in Overwatch units (metres, seconds, Overwatch health), live game as of the Oct 20 2026 patch.
 * Sources: Blizzard patch notes (overwatch.blizzard.com/en-us/news/patch-notes/live/) first, the Overwatch wiki where
 * the notes give no number. Never from the game's files. Where a source gave none, the value is ours and says so.
 */
public final class KirikoStats {
	private KirikoStats() {
	}

	// Base (Oct 13 2026 hotfix: 225 -> 250)
	public static final int HEALTH = 250;

	// Kunai (LMB here; secondary fire in Overwatch)
	public static final double KUNAI_DAMAGE = 60;            // Aug 2024
	public static final double KUNAI_CRIT = 2.0;             // Aug 2024
	public static final double KUNAI_RECOVERY = 0.5;         // Nov 2024
	public static final int KUNAI_AMMO = 15;
	public static final double KUNAI_RELOAD = 1.3;           // whole animation
	/**
	 * When the kunai come back during the reload (s): "after 65% of the animation" (May 9, 2023; the reload has been
	 * 1.3 s throughout), 0.845 s. The wiki infobox's "restores ammo after 0.65 seconds" reads the 65% as seconds; at
	 * 0.65 s a throw cut the reload short too early (#28).
	 */
	public static final double KUNAI_RELOAD_AMMO_AT = 0.65 * KUNAI_RELOAD;
	public static final double KUNAI_SPEED = 90;             // m/s
	/**
	 * The hit radius: base 0.12 (Dec 2025) plus Overwatch's 0.075 for travel-time projectiles over 50 m/s (Mar 2024);
	 * the wiki's infobox: 0.19. It was 0.12 alone, the base without the modifier (#15).
	 */
	public static final double KUNAI_RADIUS = 0.195;

	// Healing Ofuda (RMB here; primary fire in Overwatch)
	public static final double OFUDA_HEAL = 12;              // Jul 2026
	public static final int OFUDA_PER_THROW = 2;
	public static final int OFUDA_AMMO = 10;
	public static final double OFUDA_THROWS_PER_SECOND = 7;  // Apr 2026
	public static final double OFUDA_RELOAD = 0.9;
	public static final double OFUDA_SPEED = 18;             // Sep 2026
	public static final double OFUDA_RADIUS = 0.3;           // the wiki's infobox
	public static final double OFUDA_SEEK_RANGE = 30;        // Sep 2026
	/** Ours: no source gives the talismans' lifetime. Long enough to cover the seek range at their speed. */
	public static final double OFUDA_LIFETIME = 2.5;
	/** Ours: the wiki says the lock cone is half of Swift Step's, with no degrees given. */
	public static final double OFUDA_LOCK_HALF_ANGLE = 7.5;

	// Medic sub-role (Feb/Mar 2026): weapon healing also heals Kiriko
	public static final double MEDIC_SELF_HEAL = 0.40;

	// Swift Step (Shift)
	public static final double SWIFT_STEP_RANGE = 30;        // Apr 2026
	public static final double SWIFT_STEP_COOLDOWN = 7;      // Nov 2024
	public static final double SWIFT_STEP_CAST = 0.2;
	public static final double SWIFT_STEP_PHASED = 0.25;
	/** Ours: no source gives the cone. */
	public static final double SWIFT_STEP_HALF_ANGLE = 15;

	// Protection Suzu (E)
	public static final double SUZU_COOLDOWN = 14;           // Nov 2024
	public static final double SUZU_RADIUS = 4;              // Sep 2026
	public static final double SUZU_SPEED = 37;
	/** The thrown Suzu's hit radius (the wiki's infobox). */
	public static final double SUZU_PROJECTILE_RADIUS = 0.2;
	public static final double SUZU_HEAL = 80;               // Dec 2023
	public static final double SUZU_PHASED = 0.5;            // Sep 2026
	/** Thrown straight up it lands after about 7 s (wiki): g = 2v/t. */
	public static final double SUZU_GRAVITY = 2 * SUZU_SPEED / 7.0;

	// Wall climb (passive)
	public static final double CLIMB_SPEED = 8.3;
	public static final double CLIMB_TIME = 1.0;
	/** Ours (the owner, #35): she lands unhurt from as high as she can climb, 8.3 m (Minecraft's own is 3 blocks). */
	public static final double SAFE_FALL = CLIMB_SPEED * CLIMB_TIME;

	// Perks (#36): Blizzard's hero page (2026-10-26) and the patch notes (Ready Step 3 s: Oct 14 2025; Foxtrot 4 s:
	// Feb 10 2026); tools/reference/perks.md
	/**
	 * Perk progress for level 2 and level 3: the wiki's infobox (1900 / 4800 since 2026-04-21; Blizzard publishes only
	 * changes in percent). Whether 4800 counts from the start or after the 1900 isn't said: ours, from the start.
	 */
	public static final double PERK_MINOR_XP = 1900;
	public static final double PERK_MAJOR_XP = 4800;
	/** Urgent Care: Healing Ofuda seeking an ally below half health fly 50% faster. */
	public static final double URGENT_CARE_SPEED = 0.5;
	public static final double URGENT_CARE_BELOW = 0.5;
	/**
	 * Fortune Teller: each kunai hit sends 2 Healing Ofuda (one pair, OFUDA_PER_THROW) to an ally in front of her.
	 * The cone is ours ("in front of you", no angle given): Swift Step's.
	 */
	public static final double FORTUNE_TELLER_HALF_ANGLE = SWIFT_STEP_HALF_ANGLE;
	/** Ready Step: +40% attack and reload speed for 3 s after Swift Step. */
	public static final double READY_STEP_ATTACK = 0.40;
	public static final double READY_STEP_TIME = 3;
	/** Foxtrot: Protection Suzu gives allies +30% movement speed for 4 s. */
	public static final double FOXTROT_SPEED = 0.30;
	public static final double FOXTROT_TIME = 4;

	// Kitsune Rush (Q)
	public static final int ULT_COST = 2725;
	public static final double RUSH_DURATION = 8;
	public static final double RUSH_LINGER = 1;
	public static final double RUSH_LENGTH = 32.2;
	public static final double RUSH_WIDTH = 10;
	/** Ours: no source gives the fox's speed. */
	public static final double RUSH_FOX_SPEED = 20;
	public static final double RUSH_MOVE_SPEED = 0.30;       // Dec 2022
	public static final double RUSH_ATTACK_SPEED = 0.50;
	public static final double RUSH_COOLDOWN_RATE = 1.0;     // +100%: cooldowns recharge twice as fast
}
