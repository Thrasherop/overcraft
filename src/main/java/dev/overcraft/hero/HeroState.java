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
	public boolean wallClimbing;

	// weapons
	public double weaponRecovery;
	public int primaryAmmo;
	/** Ticks into the reload, -1 when not reloading. */
	public double primaryReload = -1;
	public boolean primaryAmmoRestored;
	/** Ticks before an empty clip may start reloading by itself again (an action that cut the reload short still plays). */
	public int reloadHeld;
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
		primaryHeld = secondaryHeld = wallClimbing = false;
		weaponRecovery = 0;
		primaryReload = secondaryReload = -1;
		primaryAmmoRestored = false;
		reloadHeld = 0;
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

	/**
	 * An action cut the weapon's reload short (a quick melee, an ability: its animation replaces the reload's): the reload
	 * is over, and the ammo comes back only if it already had (the owner, 2026-10-27: a cancelled reload gave the ammo
	 * back anyway). An empty clip waits {@code seconds}, the action's own animation, before reloading by itself again.
	 */
	public void interruptReload(double seconds) {
		if (primaryReload >= 0) {
			primaryReload = -1;
			reloadHeld = Math.max(reloadHeld, dev.overcraft.Ow.ticks(seconds));
		}
	}
}
