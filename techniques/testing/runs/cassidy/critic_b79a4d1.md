# Cassidy critic, b79a4d1 (2026-11-12)

Up: [Cassidy hub](../../../heroes/cassidy.md). One round. One dev client (`-Xmx3G`, `gradlew runClient --offline` from this worktree),
fresh flat survival world `crit_b79`, Easy. Overwatch and Battle.net were checked closed before the launch. Input: the harness's key
mappings (`key ability_1 down` ...), **not** the real mouse and keys. Raw stamped log:
[critic_runs_b79a4d1.txt](critic_runs_b79a4d1.txt) (C1..C8). Screenshots in `run/screenshots/` (not committed, each opened):
`crit_c4c_skull.png`, `crit_c3_melee_deadeye.png`, `crit_pause.png`. Quit with Save and Leave, then Quit Game; the lock was released
after the client exited. The wiki was read through its API (Cassidy rev 409375, still current) with a generic user agent.

## Findings, ranked

### 1. High: Combat Roll desync. The client rolls when the server refuses, and the server rolls when the client refuses
`ClientCassidy.extra` (src/client/java/dev/overcraft/client/ClientCassidy.java:36-61) decides from the last sync. The server's
`startRoll` (Cassidy.java:399-403) decides from its own state, and the press is sent either way (ClientHero.java:418-426). This is the
same class of bug as Tracer's Blink (critic_d0c7e1f #1).
- **Client yes, server no (seen, C6):** `presstrace lag 150`, E, then Shift 0.17 s later. He moved **z 0.5 -> 6.66** (a full roll),
  but the server's `ability1Cooldown` stayed **0.0** 1.4 s later and his ammo stayed 5 (no reload). The server never rolled because
  `flashCast >= 0`, while the client hadn't yet seen MODE_CASTING. Result: a free 6 m move with no cooldown, no reload and no 50 %
  damage cut, and he can do it again at once. Expected: the client and the server agree. Without lag (C5, Shift 0.1 s after E) both
  refused, so the window there is about the sync's one tick. Q then Shift, or a hinder or hack landing, opens the same window.
- **Client no, server yes (C7):** lag 150, Shift tapped every ~0.17 s as the cooldown ended. The tap at 37.34 (client showing 0.15 s
  left, so it refused to move) reached the server after the cooldown was over. The server's roll (mode 2) started from that tap, and
  only the next tap happened to move him on the client. With a slower second tap the server spends the cooldown and reloads while he
  stands still. A Sharpshooter crit shortens the server's cooldown before the client knows, which widens this window.
- Also: `s.cassidyRollDir` only updates when the client rolls (HeroManager diff, `extra != 0`), so a server-only roll plays the
  previous roll's direction clip.

### 2. Medium: Deadeye fires with no target and spends the ult (C1b)
`deadeye()` -> `startFire` on any fresh left press (Cassidy.java:617-619, 675-681); `fireTick` with `n == 0` ends it (696-702).
Repro: inside a sealed stone box, `/overcraft ult @s 100`, Q, left button 1 s in. Seen: mode 1 -> 33 -> 0, `deadeye_end`, ult 0.
Expected (W rev 409375): "Deadeye cannot be fired unless there is at least one target with unobstructed line of sight within the
targeting area." The press should do nothing.

### 3. Medium: The skull lies against Minecraft armor (C4c)
`sendLocks` sets the share to charge / (health + absorption) (Cassidy.java:662-663), but the shot goes through `target.hurt`, which
armor reduces. Repro: a NoAI zombie with 60 MC health (750 OW) in full iron (17 armor) 8 m ahead; Q, fire ~3.85 s in. Seen: the
**skull shown** (`crit_c4c_skull.png`), and the zombie left at **0.90 MC**. Expected: the skull means the shot kills (W: "a skull
will appear" once the accumulated damage is above the enemy's health). The builder's T4 used `armor 0` husks, which hid this. Every
real zombie has 2 armor, and armored mobs and players get far more: the skull should account for the armor the hit will meet, or the
shot should be sized to match.

### 4. Medium: Deadeye allows jumping and quick melee (C2b, C3b)
Nothing blocks the jump (no client check on MODE_DEADEYE), and HeroManager.java:646-653 runs QuickMelee for any hero.
- C2b: channelling (mode 1), Space. y went -60 -> **-59.02**.
- C3b: channelling, V on a husk 2 m ahead. 100 -> **96.8** (40 OW); Deadeye went on (`melee_3`, `crit_c3_melee_deadeye.png`).
- Expected (W): "Cassidy cannot jump, crouch or use other abilities while channeling Deadeye." Crouching isn't blocked either
  (not tried).

### 5. Medium: Ender Dragon parts: every Cassidy hit lands on the body, quartered
`shot()` maps an `EnderDragonPart` to `parentMob` and calls `Combat.damage(parent…)` (Cassidy.java:281, 302); Deadeye does the same
(717). `EnderDragon.hurtServer` sends that through `this.body` (decompiled 26.3, EnderDragon.java:464-466), and body hits take
`damage / 4 + min(damage, 1)` (:440-441). So a headshot on the dragon's head does 11.2 / 4 + 1 = 3.8 MC instead of 11.2. Every other
hero's shot uses `Combat.damagePart` for exactly this (Pharah:550, MicroMissiles:159, Kunai, Thorn ...). Code-read only (not played).

### 6. Low (code-read unless noted)
- **Deadeye isn't ended by a hack** (`deadeye()` checks only `able`). W: stunned, knocked down **or hacked** ends it without firing.
- **A hinder doesn't cut a roll in progress.** It only blocks a new one. W: the hinder is "interrupting and preventing movement
  abilities".
- **Q again doesn't fire Deadeye** (startDeadeye returns while it runs). W: "press Ultimate again or Primary Fire".
- **Deadeye locks on passive animals 200 m out.** C1 (open flat world): the left press fired at what it had locked (ammo 6 -> 0,
  firing ~3 s at a ring of far-off cows and sheep). W: it locks on heroes only. Whether animals should count is a design call; at
  minimum the 6-round cap and the order matter in a farm.
- **Flashbang goes off past 7 m**: the range check runs after the move (FlashbangEntity.java:84), so at 1.5 m a tick it goes off at
  ~7.5 m (Bang Bang's at ~11.25 m).
- **Self-cut fade conventions differ**: `deadeyeFade` takes its first step at 2.0 s (0 at 3.35 s, a 1.35 s fade), while `giddyBonus`
  takes its first at 0.15 s (0 at 1.5 s). Both read "10 steps of 0.15 s". W says the fade is 1.4 s.
- **Two Cassidys**: the Deadeye slow (-0.7) plus a hinder (-0.5) are ADD_MULTIPLIED_BASE, giving -1.2, clamped to a speed of 0
  (Overwatch multiplies them). The Silver Bullet bleed map is keyed by the target only, so a second Cassidy's bleed replaces the first.
- **Flashbang uses vanilla `explosion`**, which scales with the difficulty against players (Combat.java:94-99 explains why
  `Combat.blast` exists). PvP only: 75 comes off at 38.5 on Easy and at 112.5 on Hard.

## Numbers against their sources
Every `CassidyStats` number carries a source and matches cassidy.md and cassidy-map.md. Wiki rev 409375 is still the latest. Notes:
- `HINDER_TIME` comment: "BZ 2024-06-20 0.9 -> 1.2 s" is the wrong patch. 2024-06-20 introduced 1.2, 2024-09-17 cut it to 0.9, and
  BZ 2025-11-11 set it back to 1.2 (cassidy.md's own history).
- Where the data and W disagree, the data was used, as the rule says: Flashbang recovery 0.35 vs W 0.608, Deadeye shot gap 0.1 vs
  0.112, cancel 0.1 vs 0.288 + 0.4, roll 0.4 s / 6.0 m vs 0.416 s / 6.22 m. Bang Bang's second blast lands ~0.18 s after the first
  (data: 25 m/s, +3.25 m), where W says 0.1 s.
- Flashbang gravity 20 (M) and the range term `STU_FDBB0B67` (L) are still open in the map; `RANGE` 400 / `FAN_RANGE` 100 are L.
  All of these are flagged as such in the code. Fine.

## Mutations (local, reverted; `gradlew test`, the whole suite)
Applied together: Combat Roll's 50 % cut removed (`takenFactor` -> 1); Deadeye's line-of-sight check removed (locks through walls);
Flashbang's line-of-sight check removed (blast through walls). **All tests green.** CassidyMathTest covers constants and pure curves
only, nothing in `Cassidy.tick`/`lock`/`flashBang`. The builder's T2e and T4a would catch the first two in play. Nothing played the
third until C8 here (a wall stopped the blast: correct). Suggest a pure test for "can fire" (no target), and one for the skull with
armor.

## Tried and fine
- C8: Flashbang at a 1-block wall 3.5 m ahead. The husk 1 m behind the wall was untouched (100); the one in sight 2.3 m off took
  6.0 MC (75).
- C5: E then Shift 0.1 s apart with no lag: both refused, no desync.
- C7: the ordinary roll was 6.16 m with 5 s from its end.
- `HeroState.reloadHeld`: the builder **didn't change the shared field**. The HeroState diff only adds `cassidy` / `cassidyRollDir`.
  He added the countdown to Cassidy's own tick, the same as Kiriko, Lucio, Mercy, Lifeweaver, Jetpack Cat and Railgun already do.
  Nothing for other heroes to regress.
- Rejoin and hero switch mid-roll or mid-Deadeye (code): `leavePlay` ends both, and `ClientCassidy.reset` runs on disconnect.

## Not tried
The real mouse and keys (still owed); the dragon in play (finding 5 is code-read); endermen (hitscan isn't `is_projectile`, so they
shouldn't teleport: code-read); dimension change mid-Deadeye (no OutOfPlay reason, so it carries on; stale ids are skipped); a second
player (finding 6's PvP items); ladders, lava, ceilings for the roll; perks; Fan the Hammer's rhythm against the lock; an Overwatch
side by side.
