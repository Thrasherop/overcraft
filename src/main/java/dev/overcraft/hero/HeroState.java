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
	/** Jetpack Cat: Frenetic Flight's energy (0-100), ticks since it was last used, ticks boosting. */
	public double fuel = 100;
	public int fuelIdle;
	public int boostTicks;
	/** Jetpack Cat: ticks until Claws Out empowers her quick melee again. */
	public double clawsCooldown;
	/** Jetpack Cat: the paw the next volley leaves from. */
	public boolean pawSide;
	/** Jetpack Cat: the ally Lifeline tows (entity id), or null, and ticks since it attached. */
	public @Nullable Integer lifelineTarget;
	public int lifelineTicks;
	/** Jetpack Cat's Lifeline is switched on (seeking, or towing lifelineTarget), and for how many ticks. */
	public boolean lifelineOn;
	public int lifelineOnTicks;
	/** Jetpack Cat: ticks into Purr (-1 none). */
	public int purrTicks = -1;
	/** Jetpack Cat: Catnapper's phase (JetpackCat.ULT_*), its timer, its landing spot and the enemy carried. */
	public int ultPhase;
	public int ultTimer;
	public net.minecraft.world.phys.Vec3 ultTarget = net.minecraft.world.phys.Vec3.ZERO;
	public @Nullable Integer carried;
	/** Ticks on the wall this ride (his line after 2.2 s, the loop's sound). */
	public int wallRideTicks;
	/** Mercy: the blaster out (else the staff), the swap's ticks left, and the blaster's time put away (its self-reload). */
	public boolean blasterOut;
	public int swapTicks;
	public int holsterTicks;
	/** Mercy: the beam's target (entity id), mode (Mercy.BEAM_*), its ticks out of sight, and the next search's countdown. */
	public @Nullable Integer beamTarget;
	public int beamMode;
	public int beamLost;
	public int beamSearch;
	/** Mercy: which fire buttons were held last tick (1 left, 2 right: the one pressed last picks the mode), the chain's targets. */
	public int beamHeldBefore;
	public java.util.List<Integer> beamChain = java.util.List.of();
	/** Mercy: Guardian Angel's target (entity id, or a soul's player id as -2 - index), its phase and ticks, its momentum. */
	public @Nullable Integer gaTarget;
	public java.util.@Nullable UUID gaSoul;
	/** Where Guardian Angel takes her (fixed as the flight starts). */
	public net.minecraft.world.phys.@Nullable Vec3 gaTo;
	public int gaPhase;
	public int gaTicks;
	public double gaMomentum;
	/** Mercy: an Angelic Descent or a launch is landing her (no fall damage until she's down). */
	public boolean softLanding;
	/** Mercy: Flash Heal's charges and its cast's ticks left (-1 none). */
	public int flashCharges = 1;
	public int flashCast = -1;
	/** Mercy: the soul Resurrect is raising. */
	public java.util.@Nullable UUID resSoul;
	/**
	 * Lifeweaver: Thorn Volley in hand (else Healing Blossom; the swap and the holstered refill use swapTicks and
	 * holsterTicks), the blossom's charge (0-1) and whether it's being charged (the blossom already spent), the ally it's
	 * aimed at and the ticks since it left the cone, the throw's recovery, and the thorns' spread (volleys).
	 */
	public boolean thornsOut;
	public double bloomCharge;
	public boolean charging;
	public @Nullable Integer bloomTarget;
	public int bloomLinger;
	public double bloomRecovery;
	public double thornSpread;
	/** Lifeweaver: his Petal Platform (entity id, thrown or deployed), the throw's ticks to go (-1 none), when it was thrown. */
	public @Nullable Integer platformId;
	public int platformThrow = -1;
	public long platformAt;
	/** Lifeweaver: the lock Life Grip and Petal Platform share (ticks left). */
	public int abilityLock;
	/** Lifeweaver: ticks airborne (the dash waits 0.1 s), and the dash's ticks left (-1 none). */
	public int airTicks;
	public int dashTicks = -1;
	/** Lifeweaver: Life Grip's cast (ticks left, -1 none) and its ally. */
	public int gripCast = -1;
	public @Nullable Integer gripTarget;
	/** Lifeweaver: Tree of Life aimed (Q), its cast (ticks in, -1 none), where it goes, the tree (entity id). */
	public boolean treeAiming;
	public int treeCast = -1;
	public net.minecraft.world.phys.@Nullable Vec3 treeAt;
	public @Nullable Integer treeId;
	/** Lifeweaver: Sow the Seed's cooldown (ticks). */
	public double seedCooldown;
	/** Fire buttons last tick (Lifeweaver: a fresh press of either confirms or cancels the tree's aim). */
	public boolean primaryWas, secondaryWas;
	/** Game time before which his song lines stay quiet (2.5 s after he's picked or spawns: the game's gate). */
	public long songLinesFrom;

	// perks (#36): progress toward the next perk level, and the ones picked
	public double perkXp;
	/** Time played as this hero (ticks): the "match" clock the perk rate ramps up with. */
	public long perkTicks;
	public @Nullable Perk minorPerk;
	public @Nullable Perk majorPerk;

	public long lastDamagedTick = -10000;
	/** Shields left (Overwatch points: the top of the health, dev.overcraft.hero.Shields). */
	public double shield;
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
		shield = hero == null ? 0 : hero.shields();
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
		blasterOut = false;
		swapTicks = holsterTicks = 0;
		thornsOut = charging = treeAiming = false;
		bloomCharge = bloomRecovery = thornSpread = seedCooldown = 0;
		bloomTarget = platformId = gripTarget = treeId = null;
		bloomLinger = abilityLock = airTicks = 0;
		platformThrow = dashTicks = gripCast = treeCast = -1;
		treeAt = null;
		primaryWas = secondaryWas = false;
		beamTarget = gaTarget = null;
		gaSoul = resSoul = null;
		gaTo = null;
		beamMode = beamLost = beamSearch = gaPhase = gaTicks = 0;
		gaMomentum = 0;
		softLanding = false;
		flashCharges = 1;
		flashCast = -1;
		ampFree = false;
		ampDelay = 0;
		wallRideTicks = 0;
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
