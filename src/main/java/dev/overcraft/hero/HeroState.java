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

	public long lastDamagedTick = -10000;
	/** When the hero last spoke a voice line (game time), to space them out. */
	public long voiceAt = -10000;
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
		primaryHeld = secondaryHeld = false;
		weaponRecovery = 0;
		primaryReload = secondaryReload = -1;
		primaryAmmoRestored = false;
		secondaryFiredSinceReload = false;
		ability1Cooldown = ability2Cooldown = 0;
		meleeCooldown = 0;
		castTicks = -1;
		castTarget = null;
	}
}
