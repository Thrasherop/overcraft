# Pharah critic 1 (2026-11-11)

Branch `feature/pharah` at c849b15, worktree `E:\claude\mods\overcraft_pharah`. One dev client (`gradlew runClient`), fresh
flat survival world `critic_pharah1`, under the client lock (taken 21:29, released after the client exited). Overwatch and
Battle.net were not running. Raw rows: `critic_1_raw.txt` beside this file. Screenshots: `run/screenshots/critic1_*.png`.

**Input used:** the harness. `press key.keyboard.*` goes through Minecraft's keyboard handler (Space, Shift, E, Q, G, W,
Esc). The mouse buttons went through `key secondary_fire|primary_fire` (`KeyMapping.set`, not a real mouse). There was no
desktop mouse or keyboard. Sleep and stun came from the dev `cut sleep|stun`, not from an enemy Ana or Brigitte. There
was no second player.

## Findings

### 1. Asleep or stunned mid-hover, she keeps hovering on her fuel (MEDIUM, CONFIRMED)
- Code: `src/client/java/dev/overcraft/client/ClientPharah.java:37-38` (`mine` asks `ClientHero.controls()`, which leaves
  out sleep and stun: `ClientHero.java:174-176`). Also line 104: it reads the raw JUMP key, which goes around
  `KeyboardInputMixin`'s sleep block. Server side: `Pharah.hovered` only runs when an input packet arrives
  (`HeroManager.java:620`). The client sends input only when it changes, so `pharahHovering` stays true from before the
  sleep, and `Pharah.java:166` keeps draining the fuel.
- Steps: hold Space from the ground for 0.8 s, run `cut sleep 3`, keep Space held.
- Expected: the hover stops and she falls asleep (W: sleep and stun interrupt her). The server's `controls()` already
  says she has no controls.
- Seen (T11, 21:44:47): after the sleep she kept rising at 2.9 m/s, from y -57.29 to -53.36 (+3.9 m). Her fuel drained
  from 0.625 to 0, then she fell at the empty-hover cap of 8 m/s (vy -0.4 b/t, the fall still slowed). Stun is the same
  (T11b, 21:44:58: -57.29 to -53.36). Screenshot `critic1_T11_asleep_hover.png`.

### 2. Space on a ladder spends her whole tank and gives nothing (LOW-MEDIUM, CONFIRMED)
- Code: `ClientPharah.java:109` leaves out only water (`!entity.isInWater()`). It doesn't check climbables, nor
  `HeroManager.jumpIsNotAirborne` (ladder, lava, cobweb, powder snow, riding), which the repo already has for this.
- Steps: a 12-high ladder; W alone climbs (control); then W+Space.
- Seen (T9, 21:43:47 / 21:43:52): both climb at the same 0.1176 b/t (2.35 m/s). With Space held, the meter goes 1.0 ->
  0.9 -> 0.775 ... -> 0.0 in about 2 s and doesn't refill on the ladder, so she arrives at the top with no fuel.
  Screenshot `critic1_T9_ladder.png`.

### 3. Seated in a boat or on a horse, Space drains her fuel (LOW-MEDIUM, CONFIRMED)
- Code: the same line, `ClientPharah.java:109`. `canJet` (`Pharah.java:209`) leaves out passengers, but the hover doesn't.
- Steps: `/ride @s mount` a boat on land, hold Space 1.5 s, let go; then the same on a tamed, saddled horse (Space is
  the horse's jump).
- Seen (T10, 21:44:18 / 21:44:26): in the boat the meter went 1.0 -> 0.275 -> 0.225 with no movement. On the horse it
  went 1.0 -> 0.25 -> 0.225, with no refill while seated. She dismounts without fuel. Screenshots `critic1_T10_boat.png`,
  `critic1_T10_horse.png`. Lava wasn't tried; by the same line, Space in lava would hover her (SUSPECTED).

### 4. Leaving the world while hovering: on rejoin she drops from that height, behind the select screen, and is hurt (MEDIUM, CONFIRMED)
- Code: `HeroManager.onLeave` (`HeroManager.java:511-519`) -> `Pharah.leavePlay` clears `softLanding` (`Pharah.java:704`).
  `onJoin` puts her back mid-air with `HeroSelectScreen` open, and `ClientPharah.java:104` turns the hover off while any
  screen is open. Probably shared with other airborne heroes (Mercy, Jetpack Cat). Pharah is the one who lives up there.
- Steps: Jump Jet + hover to about 11 m, Esc -> Save and Leave, reopen the world.
- Expected: no hurt from a fall she couldn't act on (CLAUDE.md, #35: an ability's landing doesn't hurt).
- Seen (T12, 21:45:40): she rejoined at y -49.17, fell with the select screen up, and health went 18.0 -> 11.0 (about 87
  of 225 OW). From about 25 m it would kill her. T5: rejoined at 4 m, 18 -> 17. Screenshot `critic1_T12_rejoin_fall.png`.
- Also on rejoin: every cooldown is back at 0 (T5: E 6.4 s and Jet Dash 6.75 s -> 0 after the rejoin), so leaving and
  rejoining refreshes the kit. That's `onJoin`'s `state.reset` for every hero, not Pharah's code, but a Pharah player
  gets a free Jump Jet that way.

### 5. Suspected only (code)
- 5a: Jump Jet and Jet Dash cancel a reload (`startJet` -> `s.interruptReload(0)`, `Pharah.java:245`). No data id is
  cited for it. Worth checking in the data that her jets cancel the reload (Concussive's cancel is the usual one).
  `reloadHeld`, which `interruptReload` sets, isn't read by Pharah's `launcher`. That's harmless today.
- 5b: Barrage during Jump Jet keeps the jet running (T2: mode 5 = jet + Barrage). Check that the data doesn't end the
  jet on the cast.

## The builder's open gap: "G mid-hover unhurt landing seen once"
I reproduced an unhurt landing once (T1.2, 21:33:03) and found the cause: a slime (flat world, mobs spawning) hit her
mid-fall at y -58.8, and the knockback reset the fall. With mob spawning off, five more tries (T1b.0-4) all hurt as a
plain Minecraft fall: G at y -48.8 to -48.9, health 18 -> 10.3 to 10.8 each time. So G mid-hover is right. The builder's
one-off was likely the same thing: check for a mob near the landing.

## What worked
- G mid-hover then landing (5 clean runs): hurt from the height of the G, hero mode off.
- Barrage started in the air during Jump Jet: 2.5 s, rockets refilled, fuel held, she falls, and lands unhurt (the
  jet's soft landing). No slow left after it (respawn walk 5.7 m in 1 s; movement_speed 0.1 after the rejoin).
- Death mid-Barrage while hovering (`/kill`), then respawn: mode 0, meter 1.0, normal speed, the hover works again,
  ult charge spent.
- Hero switch mid-hover (to Kiriko at about 11 m): Pharah's hover ends and Kiriko's own fall rule applies (20 -> 17);
  back to Pharah gives a full kit.
- Concussive Blast at the ground between her tamed wolf, a villager and a zombie (AI on): the zombie was thrown (vz
  0.94 b/t = 18.7 m/s, vy +0.17); the wolf and villager weren't touched (Motion polled every 50 ms); she knocked herself
  back (z -5.5 -> -12.2).
- A rocket into water at a zombie 3 blocks deep: it carries on through the water and hits (20 -> 10.73 through a
  leather helmet).
- A rocket into lava from the edge: it bursts on the pool floor, with no self-damage at 2+ m and no fire.
- Jump Jet under a 3-high and a 2-high ceiling, and Jet Dash under the 3-high: pressed against the ceiling, no damage,
  not stuck, the jet's fuel still added (1.0 -> 1.47), lands at once after the jet.

## Not tried
A second player (Concussive Blast on an enemy player or an ally player, Recon's glow seen by others), a real mouse
(clicks went through KeyMapping), a real Ana or Brigitte, the Nether, perks (Helix Shields, Fuel Stores, Rocket Salvo,
Concussive Force), an actual night fight, rocket-jumping (self-knockback) numbers, Barrage damage numbers, and a
side-by-side with Overwatch.
