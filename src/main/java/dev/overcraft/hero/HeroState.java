package dev.overcraft.hero;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** A player's hero and its live state on the server. Timers are in ticks (doubles: buffs speed them up). */
public final class HeroState {
	public @Nullable Hero hero;

	public double ultCharge;
	/** Ticks left of an ultimate in progress (no ult charge meanwhile). */
	public int ultActive;

	// input, from the client (cleared whenever slot 1 isn't selected)
	public boolean primaryHeld;
	public boolean secondaryHeld;
	public boolean wallRiding;
	public boolean wallClimbing;

	// weapons
	public double weaponRecovery;
	public int primaryAmmo;
	/** Ticks into the reload, -1 when not reloading. */
	public double primaryReload = -1;
	public boolean primaryAmmoRestored;
	public int secondaryAmmo;
	public double secondaryReload = -1;
	public boolean secondaryFiredSinceReload;

	// abilities
	public double ability1Cooldown;
	public double ability2Cooldown;
	public int castTicks = -1;
	public @Nullable UUID castTarget;
	/** Quick melee, every hero's (QuickMelee). */
	public double meleeCooldown;
	/** A timed attack and reload speed bonus (Ready Step: 0.4) until a game time. */
	public double attackBuff;
	public long attackBuffUntil;

	// Lúcio: the song (false = healing, true = speed), a swap under way (ticks left, -1 none), Amp It Up's ticks left,
	// Soundwave's cooldown, the volley's rounds still to fire and the time to the next, Soundwave Rider's charge (0..1),
	// Sound Barrier's cast (ticks in, -1 none)
	public boolean speedSong;
	public int songSwap = -1;
	public int ampUp;
	public double ability3Cooldown;
	public int volleyLeft;
	public double volleyNext;
	public double riderCharge;
	public int barrierCast = -1;
	/** Amp It Up given by Beat Drop: when it ends, no cooldown starts. */
	public boolean ampFree;
	/** Amp It Up pressed: ticks until it starts (0 none). */
	public int ampDelay;
	/** Ticks on the wall this ride (his line after 2.2 s, the loop's sound). */
	public int wallRideTicks;
	/** Game time before which his song lines stay quiet (2.5 s after he's picked or spawns: the game's gate). */
	public long songLinesFrom;

	// perks (#36): progress toward the next perk level, and the ones picked
	public double perkXp;
	/** Time played as this hero (ticks): the "match" clock the perk rate ramps up with. */
	public long perkTicks;
	public @Nullable Perk minorPerk;
	public @Nullable Perk majorPerk;

	public long lastDamagedTick = -10000;
	/** When the hero last spoke a voice line (game time). */
	public long voiceAt = -10000;
	/** Voice (Voice.say): each moment's next time it may play, and the line now playing (until when, how high it ranks). */
	public final java.util.Map<String, Long> voiceReadyAt = new java.util.HashMap<>();
	public long speakingUntil = -10000;
	public int speakingPriority;
	public net.minecraft.resources.@Nullable Identifier speakingLine;
	/** When the hero last killed an enemy (game time), and how many in a row within the chain: multikills. */
	public long lastKillAt = -10000;
	public int killChain;
	/** When the hero last landed a critical hit (game time): a kill on that tick was a headshot. */
	public long critAt = -10000;
	/** On Fire (Combat.addFire): the score, whether On Fire, and when it may start again (game time). */
	public double fireScore;
	public boolean onFire;
	public long fireReadyAt;
	/** When a ping line was last said (Pings: a plain ping's line at most once a second). */
	public final java.util.Map<String, Long> saidAt = new java.util.HashMap<>();
	/** In (or just out of) a Kitsune Rush corridor this tick. */
	public boolean rushed;
	/** May pick a hero now: the pick on joining, or near their bed. */
	public boolean canChangeHero;
	/** The free pick on joining the world hasn't been used yet. */
	public boolean freePick;

	public void reset(@Nullable Hero hero) {
		this.hero = hero;
		ultCharge = 0;
		ultActive = 0;
		primaryHeld = secondaryHeld = wallRiding = wallClimbing = false;
		speedSong = false;
		songSwap = -1;
		ampUp = 0;
		ability3Cooldown = 0;
		volleyLeft = 0;
		volleyNext = 0;
		riderCharge = 0;
		barrierCast = -1;
		ampFree = false;
		ampDelay = 0;
		wallRideTicks = 0;
		weaponRecovery = 0;
		primaryReload = secondaryReload = -1;
		primaryAmmoRestored = false;
		secondaryFiredSinceReload = false;
		ability1Cooldown = ability2Cooldown = 0;
		meleeCooldown = 0;
		castTicks = -1;
		castTarget = null;
		attackBuff = 0;
		attackBuffUntil = 0;
		perkXp = 0;
		perkTicks = 0;
		minorPerk = majorPerk = null;
	}

	public boolean has(Perk perk) {
		return minorPerk == perk || majorPerk == perk;
	}
}
