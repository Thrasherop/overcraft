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
	/**
	 * A fire button pressed since the last tick (no tick has seen it held yet), and let go before one did: the release waits
	 * for the next tick (HeroManager.held), so a quick click is never lost.
	 */
	public boolean primaryFresh, secondaryFresh, primaryLetGo, secondaryLetGo;
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
	/** Ana's Sleep Dart cast, ticks into it (-1 none). */
	public int sleepCast = -1;
	/** Ana's Nano Boost cast, ticks into it (-1 none), and the entity id it's for. */
	public int nanoCast = -1;
	public int nanoTarget = -1;
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
	/** Ana's Take Aim: how far scoped in, 0 (out) to 1 (fully scoped). */
	public double scope;
	/** Whether Take Aim was wanted last tick: its in and out sounds play as that changes. */
	public boolean scopeWanted;
	/** Sombra's Machine Pistol: shots in the current burst (its spread grows with them), and when the last one left. */
	public int burstShots;
	public long lastShotAt = -10000;
	/** Sombra's Hotfix: charges left, ticks until the next comes back (-1: none recharging), the channel (ticks in, -1 none). */
	public int hotfixCharges;
	public double hotfixRecharge = -1;
	public int hotfixChannel = -1;
	public @org.jspecify.annotations.Nullable Integer hotfixTarget;
	public boolean hotfixHeld;
	/**
	 * Sombra's Translocator: her beacon in flight (its entity id, null none), and stealth:
	 * when it began (her landing, game time), when it ends, and a brief reveal (taking damage, Hotfix).
	 */
	public @org.jspecify.annotations.Nullable Integer beaconId;
	public long stealthFrom = -1, stealthUntil = -1, revealedUntil = -1;
	/** EMP's cast (ticks in, -1 none); when Hotfix may hack again (game time). */
	public int empCast = -1;
	public long hackLockoutUntil = -1;
	/** Sombra: the enemy players last sent to her as having their ultimate ready (Conspirator). */
	public int[] conspiracy = new int[0];
	/** Sombra: her pistol burst under way (its tail when it ends); stealth on last tick (its end sounds). */
	public boolean sombraFiring, sombraStealthed;
	/** Brigitte: her flail's swing (ticks into it, -1 none), which side it's on (0 forehand, 1 backhand), whom it hit. */
	public int flailSwing = -1, flailCombo;
	public final java.util.Set<Integer> flailHit = new java.util.HashSet<>();
	/** Brigitte: when the flail may start Inspire again (game time). */
	public long inspireReadyAt = -1;
	/** Brigitte: her barrier up, its health (OW), when it went down / when it can go up again; Shield Bash's dash (ticks
	 * into it, -1 none) and whether it has hit; Rally's ticks left; the left button's last state (the bash's press). */
	public boolean barrierUp, bashHit, leftWas;
	public double barrierHealth;
	public long barrierDownSince = -10000, barrierBrokenUntil = -1;
	public int bashTicks = -1, rallyTicks;
	/** Juno: ticks into Glide Boost (-1 not gliding). */
	public int junoGlide = -1;
	/** Sojourn: ticks into Power Slide (-1 not sliding), when it last ended (game time), her Railgun's energy (0-100, 150). */
	public int sojournSlide = -1;
	public long sojournSlideEndAt = -1000;
	/** Sojourn: ticks on the ground in a row since her slide's soft landing could end (it ends once she has settled). */
	public int sojournSettled;
	public double sojournEnergy;
	/**
	 * Sojourn's Railgun: the share of a shot owed (16 a second at 20 ticks), the shots into the spread's ramp, when she last
	 * fired it and last gained energy (game time), and her right button last tick.
	 */
	public double sojournShotClock = 1, sojournSpread;
	public long sojournShotAt = -1000, sojournGainAt = -1000;
	public boolean sojournRightWas;
	/** Sojourn: a Charged Shot pressed and waiting (ticks of its buffer left), 0 none. */
	public int sojournCharged;
	/**
	 * Sojourn: ticks into Disruptor Shot's cast (-1 none), ticks until its cooldown is written (-1 none); ticks into Overclock (-1
	 * none: cast, active, tail); Power Slide's charges (Dual Thrusters: 2, otherwise unused).
	 */
	public int sojournDisruptor = -1, sojournDisruptorWrite = -1, sojournUlt = -1, sojournSlideCharges = 2;
	/**
	 * Sojourn's sounds and lines (step 8): her Railgun firing a burst (its first shot and its ring-out), her "Railgun charged"
	 * said for this full charge and its "still at full power" (10 s on), a Charged Shot's damage being dealt now (its kill
	 * line); when her energy reached full, when her slide jump left the ground (the in-air kill line), when Overclock's end line
	 * is due (game time, -1 none); her kills in this Overclock.
	 */
	public boolean sojournFiring, sojournFullSaid, sojournHoldingSaid, sojournCharging;
	public long sojournFullAt = -1, sojournSlideJumpAt = -1000, sojournOcEndSay = -1;
	public int sojournOcKills;
	/** Juno: rounds left in the Mediblaster's volley, and the share of a round owed (34 a second at 20 ticks). */
	/**
	 * Sigma (dev.overcraft.sigma.Sigma): ticks to his volley's second sphere (-1: none due), ticks left of the 1.3 s before
	 * the next volley, the left button's 0.15 s buffer, the volleys fired (his three fire clips cycle); his barrier's entity
	 * (out: sent or stopped), the right button last tick, when it can go out again; Kinetic Grasp's ticks left (-1: off) and
	 * the damage it has absorbed (Overwatch points). The barrier's health is barrierHealth, as Brigitte's.
	 */
	public int sigmaSecondIn = -1, sigmaBuffer, sigmaVolleys;
	public double sigmaReload;
	public @Nullable Integer sigmaBarrierId;
	public boolean sigmaRightWas;
	public long sigmaBarrierReadyAt = -1;
	public int sigmaGrasp = -1;
	public double sigmaGraspAbsorbed;
	/** The game time of his kit's last tick (his HUD's cooldowns). */
	public long sigmaNow;
	/**
	 * Sigma's Accretion: ticks left of its gather (-1: none); Gravitic Flux's phase (dev.overcraft.sigma.GraviticFlux.NONE ..
	 * SLAM), ticks into it, the aimed point; Hyper Strike's direct hits counted and whether his next quick melee carries it.
	 */
	public int sigmaCast = -1;
	public int sigmaFluxPhase, sigmaFluxTicks;
	public net.minecraft.world.phys.@Nullable Vec3 sigmaFluxPoint;
	public int sigmaDirectHits;
	public boolean sigmaStrikeReady;
	/**
	 * His barrier's voice checks each second (1B55 #967 / #971): its health a second ago, and whether it was under 34 % then;
	 * Levitation's end, for others' look and sound (the server is told it began; ours: 1.75 s on, or a landing).
	 */
	public double sigmaBarrierSample = -1;
	public boolean sigmaBarrierUnder;
	public long sigmaLevitateUntil = -1;
	/** When Hyper Regeneration's heal on his barrier was last drawn (6E81: 0.5 s long, so once a 0.5 s at most). */
	public long sigmaHealFxAt = -100;
	public int junoVolley;
	public double junoVolleyClock;
	/** Juno: ticks into Pulsar Torpedoes' lock (-1 none), each target's lock progress (ticks), the locked, those still to fire. */
	public int junoLock = -1;
	public final java.util.Map<Integer, Integer> junoLockProgress = new java.util.HashMap<>();
	public final java.util.List<Integer> junoLocked = new java.util.ArrayList<>();
	public final java.util.List<Integer> junoFiring = new java.util.ArrayList<>();
	public double junoFireClock;
	/** Juno: each target whose lock tick is sounding, and when its file was started (game time; Torpedoes.ticking). */
	public final java.util.Map<Integer, Long> junoLockTicking = new java.util.HashMap<>();
	/** Juno: Locked On's cooldown taken off for this firing (ticks, at most 5 s). */
	public double junoLockedOn;
	/** Juno: ticks into Hyper Ring's cast and Orbital Ray's (-1 none). */
	public int junoRingCast = -1, junoRayCast = -1;
	/** Juno: her buttons last tick (their presses' edges). */
	public boolean junoLeftWas, junoRightWas;
	/** Juno: the left click that ended her lock, still held: it fires nothing. */
	public boolean junoLeftSpent;
	/** Brigitte: Whip Shot (ticks into it, -1 none) and where it is along its flight; Repair Pack's recharge (ticks left). */
	public int whipTicks = -1, whipBackAt;
	/** Brigitte: Rally's cast (ticks into it, -1 none); a shockwave due (ticks left, -1 none); the extra armor it gives. */
	public int rallyCast = -1, shockwaveIn = -1;
	/** Brigitte: when Inspiring Strike's speed ends (game time, -1 none). */
	public long strikeFastUntil = -1;
	/** Brigitte: which of her barrier's low calls (50 %, 25 %) this raise has made (0, 1, 2). */
	public int barrierLowCalled;
	public double extraArmor;
	public double whipOut, repairRecharge = -1;
	public net.minecraft.world.phys.@org.jspecify.annotations.Nullable Vec3 whipFrom, whipDir;
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
	/** Where (and in which dimension) the player last respawned: their spawn room too (HeroManager.nearRespawn). */
	public net.minecraft.world.phys.@org.jspecify.annotations.Nullable Vec3 respawnedAt;
	public net.minecraft.resources.@org.jspecify.annotations.Nullable ResourceKey<net.minecraft.world.level.Level> respawnedIn;

	public void reset(@Nullable Hero hero) {
		this.hero = hero;
		shield = hero == null ? 0 : hero.layer();
		ultCharge = 0;
		ultActive = 0;
		primaryHeld = secondaryHeld = wallRiding = wallClimbing = false;
		primaryFresh = secondaryFresh = primaryLetGo = secondaryLetGo = false;
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
		bloomCharge = bloomRecovery = thornSpread = seedCooldown = scope = 0;
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
		sojournSettled = 0;
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
		sleepCast = -1;
		nanoCast = -1;
		empCast = -1;
		nanoTarget = -1;
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
