# Pharah (hub)

Everything learned about Pharah, by subject. Hero 9 of the owner's 2026-10-28 list (issue #70), after D.Va: **Damage, Recon** (BZ
2026-02-10), the second Damage hero after [Sojourn](sojourn.md), and the first whose movement is a fuelled flight. Up:
[Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · built the way [D.Va](dva.md) was;
her flight sits beside [Juno](juno.md)'s hover and glide in `OwMovement`.

## Her notes and sources
- **Public numbers:** [pharah.md](../../tools/reference/pharah.md): Blizzard's live patch notes 2024-01 to 2026-11 (her last change BZ
  2026-08-11, Concussive Blast 7 -> 8 s) and the wiki's revision 408633, with what changed when (Jet Dash added 2024-02-13, perks
  2025-02-18, Recon 2026-02-10, Barrage can move 2026-04-14).
- **The data map:** [pharah-map.md](../../tools/reference/pharah-map.md): hero 0008.075, her graphs (3B6E values, 0023 Rocket Launcher, 0025
  Hover Jets, 0027 Jump Jet, 4B92 Jet Dash, 007B Concussive Blast, 002B Barrage, 51E9 perks, 01CF #1197 Recon), models, muzzles, every
  clip per state, and the table of data against the public numbers. Detail and pipeline in `E:\Overwatch Archive\research\pharah\`
  (D.Va's scripts retargeted; `work_kit\kit_pharah_weapon.md`, `kit_pharah_move.md`).
- **Clip maps:** `tools/clipmaps/pharah.txt` (3P F2A5, 120 clips), `pharah_1p.txt` (1P F2B9, 55), `pharah_weapon.txt` (launcher F131),
  `pharah_clips.txt` (emotes), `pharah_intro_clips.txt` (the highlight intro). Sounds: `tools/soundmaps/pharah.txt` (voice, set 0004.05F)
  and `pharah_sfx.txt` (31 events), both checked to 0 problems.
- **Code:** `dev.overcraft.pharah` (`Pharah` the kit, `PharahStats` every number with its source, `PharahSounds`),
  `entity.PharahRocketEntity` (rocket, Barrage rocket, Salvo mini-rocket, Concussive Blast by kind), `client.ClientPharah` (her hover and
  the jets' launches on her own client), `client.PharahSounds` (the hover loop), `OwMovement.pharahHover`, `HeroManager.PHARAH_STATE`
  (hover / jet / Barrage for every client's animation), `HeroInputPayload.HELD_HOVER`.

- **The Overwatch recording** (the owner's Workshop log, 2026-11-11): [pharah_workshop_2026-11-11](pharah_workshop_2026-11-11.md),
  measured against ours: the hover rises at 4.66 m/s (an ease, not the 2.9 push), the jets don't hover without the key, Barrage
  holds her height, Concussive's cooldown starts after its 0.5 s recovery, Jet Dash keeps 30 % of her speed.

## What her data taught (2026-11-11)
- **Jump Jet is still a press in this build** (0027 #82: no charge or hold state), though the brief expected a held charge: check the
  data before building a mechanic a recent patch "might" have changed.
- **The data and the wiki disagree twice, and the data was used:** the rocket's full-splash ring is 0.5 m (2390, and its own tooltip),
  not the wiki's 0.8; the hover's rise is a 50 m/s² push that stops adding at 2.9 m/s (0B9A / 0B99), not the wiki's 4.66 m/s. Played:
  she rises at 2.90 m/s. The 2.9 is a reading of what the limit field means (M): worth an Overwatch side by side.
- **Her own blast moves her, all of it, and hurts her not at all** (007B m_2FE97560 1, m_8A3E3883 0); her rockets push her 1.875 times
  what they push an enemy (0B69): the rocket jump is in the data, not a side effect.
- **A 1P rest pose can be a T-pose.** D.Va's missile muzzles were read off the 1P model's rest pose; Pharah's 1P model rests in a T-pose,
  so the research posed it in Blender and got a muzzle 0.13 m *above* her eyes. The dev client's own drawn point (`state` "muzzle")
  put it 0.165 m *below* and 0.107 right: the drawn one is used. Read the muzzle where the game draws the launcher.
- **Her head is two geosets** (bare face and helmet), not weapon pieces: the emote tool's weapon-piece rule would have hidden her head
  in Mic Drop ([emote_reproduction](../emotes/emote_reproduction.md)). A ragdoll prop held for its whole window (Rocket Guitar's
  launcher) isn't one that falls.

## Built (step 1, 2026-11-11, commit 469cc93 then the play fixes)
Each number in `PharahStats` with its source (D data node / BZ / W); H / M / L as there.
- **Rocket Launcher** (left button): 6 rockets, 0.8 s apart (0.55 after the last), reload 1.5 s with the rockets back at 0.6 s (D); 40
  m/s, 0.32 m; 40 direct + 80 splash to 0.5 m, down to 24.6 at 2.5 m; 25 % on herself; push 5 m/s away + 8 up (x0.8 inside 0.5 m, to 0
  at 2.5 m), x1.875 on herself.
- **Jet Dash** (right button): 19 m/s along the move keys (forward without) + 8 up, 0.85 s, 9 s cooldown from its end, no fuel (D).
- **Jump Jet** (Shift): 17 m/s up + 7 along the move keys, 0.85 s, 14 s cooldown from its end, +50 fuel over the jet into overfuel (D).
- **Concussive Blast** (E): leaves 0.167 s into the cast, 0.5 s before the next rocket; 60 m/s, 0.225 m; 30 on a direct hit, an 8 m blast
  pushing 20 away + 5 up with no falloff (30 / 8 on the one hit); 8 s cooldown (D, BZ 2026-08-11).
- **Hover Jets** (Jump held): fuel 100, 50 a second; back at 45 a second on the ground only, 0.3 s after; overfuel to 150 (200 with Fuel
  Stores), held 5 s then down at 30 a second (80 while hovering) (D, M for the decay); the push as above; +30 % speed while she hovers
  with fuel; with none, the key held still holds her fall to 8 m/s; from the ground after 0.2 s of holding (D, M).
- **Barrage** (Q, 2200): 2.5 s, 30 rockets a second from her shoulder and hip pods in the data's order, 10 direct + 25 splash to 0.5 m,
  7.5 at 1.5 m, 45 m/s, 8 degrees of spread (the cone's shape ours), half on herself (M), no push; her rockets refilled and her fuel held
  full at the cast; -20 % speed; a stun, sleep or hack ends it, its charge spent.
- **Recon** (sub-role): her damage on an enemy under half health shows it through walls 3.5 s (D 23D8). Ours: Minecraft's glowing, which
  everyone sees. **Damage role passive:** none since BZ 2026-02-10 (its healing cut is everyone's now: Combat's).
- **Perks:** Concussive Force (the blast does 30 to 4 m, 15 at 8 m), Helix Shields (125 of her 225 as shields; a direct rocket hit starts
  their regeneration), Fuel Stores (Jet Dash +50 fuel, overfuel to 200), Rocket Salvo (a jet arms the next rocket, which brings 2
  mini-rockets of 10 + 20 0.1 s later; the 5 s window M).
- **Falls:** her jets, her hover (with or without fuel) and her own blasts land her unhurt (`softLanding`); a plain fall hurts from her
  3 m (no safe-fall height in her data: H by search; the 3 m is the mod's default). G mid-flight drops the soft landing: the rest is a
  plain Minecraft fall.
- **HUD:** Jet Dash / Jump Jet / Concussive Blast boxes (a jet's box lit while it runs), the fuel as a number and band on a Hover Jets
  panel (white past 100), the launcher's 6. Select screen: DAMAGE RECON, 225 HEALTH, six kit icons. Models, 120 + 55 clips, the hover's
  8-way blend, her emotes (15) and the Heroic intro for Play of the Game: from the data (the agents' reports are in this branch's
  history: `pharah-map.md`, the clip and sound maps).
- **Ours (no data):** the Barrage spread's shape; Recon's look; the stand-in blast and explosion particles (her visual effects aren't
  built); the Salvo mini-rockets 0.2 m apart (the data's "spread 0.1" unit unread); jets don't fire in water (Minecraft swims her there).

## Played (2026-11-11, one dev client, `-Xmx3G`, flat survival world `ph1`, harness keys)
Raw stamped output: [runs/pharah/runs.txt](../testing/runs/pharah/runs.txt). Screenshots in `run/screenshots/` (not committed; each
opened): `p02b.png` (select), `p03_hud_1p.png`, `p04_3p_back.png`, `p05_3p_front.png`, `sheet_looks.png` (hover front / forward / back,
firing 1P and 3P, the blast, Barrage), `sheet_fight.png`, `sheet_switch_emotes.png`, `t7_perkmenu.png`. Expected -> seen:
- **Select** (clicked): Pharah under DAMAGE, "DAMAGE RECON", "225 HEALTH", LMB RMB LSHIFT E Q SPACE; picked, 18 / 18 Minecraft
  (225), HUD as above, the select line "Strike from above". Rejoin (client restarted, world reopened): the select screen with Pharah
  chosen, one click in.
- **Rocket** on a zombie 10 m off: 20 -> **10.4** (120); one 1.8 m from the burst 20 -> 16.48 (43.9, expected 41-44 by the box's
  distance). Held 4.2 s from 3 rounds: 4 rockets (3, the 0.55 s, the 1.5 s reload, 1) = 480 on a 200-health target. **Rocket jump**
  (straight down): 18 -> **16.4** (20 to herself), up 4.3 m (12 m/s), landing unhurt. Point blank on a husk 1.5 m off: 0.82 to herself.
- **Jump Jet** standing: straight up 8.9 m (8.3 by 17 m/s and 17.5 gravity; the jet's push holds the last of the rise), fuel 100 ->
  **147**, cooldown 14 s from the jet's end. **Hover** from its top: rose at **2.90 m/s**, fuel 147 -> 79 in 1.1 s (overfuel at 80 then
  50), empty at 2.6 s; held with none, the fall **8.0 m/s**; landed from about 17 m **unhurt**; fuel back 0 -> 100 in 2.3 s on the ground.
- **Jet Dash** forward 16 m, left (A held) 18 m to her left, cooldown 9 s; into a stone wall 3 m off: stopped at it, unhurt.
- **Concussive Blast** (held husks): direct **30** (20 -> 17.6), the husk thrown 6.7 m, the one 3 m behind 4.3 m and unhurt; a villager 2 m
  off untouched; she, 6 m from it, thrown back 8.6 m unhurt. At her feet: thrown back, no damage.
- **Barrage** (dev ult charge, Q) into three 300-health husks from the air: rockets refilled 5 -> 6, fuel 0.53 -> 1.0, mode Barrage 2.5
  s; 13.3 / 37.1 / 13.6 Minecraft health off them (with one rocket before). Its pods open in third person (`v7_barrage_3p.png`).
- **Perks** (level 3 by dev command, picked in the LAlt menu with the clicks): Helix Shields: shield 125; two 50s took 125 -> 25 shields
  (health under them untouched), back from 3 s at 30 a second; damaged then a direct rocket hit 1 s later: back at once (25 -> 46 -> 61).
  Rocket Salvo: Jump Jet then one rocket: **180** (120 + 2 x 30); the control rocket 120. After a death: Concussive Force: 30 at 1.7 m,
  **23.6** at 5.7 m (23.6 expected); Fuel Stores: Jet Dash 100 -> 147, Jump Jet on top -> 194 (cap 200), held then decaying.
- **Ally:** her tamed wolf 1 m beside a rocketed husk: 8 -> 8. **Water** (3 deep): Jump Jet does nothing; jump held swims her up, then
  she hovers out. **Stairs:** run up and over 4 steps, Jump Jet off them. **Falls:** a plain 10 m drop in hero mode 18 -> 12; G mid-hover
  6.5 m up: the server's fall counted from G and hurt (18 -> 15), 2 runs of 3 (below).
- **Night fight** (normal, midnight, monsters spawning; a wave of 12 with 4 skeletons; the left button held, aim at the nearest each
  0.5 s, jets, hover, the blast at a creeper or spider in range, Barrage at full): **4 kills in 20 s** (a husk and 3 zombies, the kill
  feed), Barrage fired, then shot dead by a skeleton; respawned as Pharah ("Pharah is back"), full health on a clean respawn. Two more
  deaths by real mobs (a spear zombie at midnight, a husk) the same.
- **Quick melee** 40 (20 -> 16.8); R mid-magazine: the rounds back; **H mid-game** to Kiriko and back to Pharah (each full health);
  **emotes** Mic Drop (the mic), Rocket Guitar (the launcher as a guitar), Justice Reloaded from the dev command, each with its prop.

**Found playing (fixed):**
1. **Her hover never started** (T3: jump held from the jet's top, she fell and bounced; fuel unspent): `ClientPharah` read Minecraft's own
   jump key, which hero mode doesn't use (`HeroControls.Action.JUMP`: Juno's hover reads that one). After: rose at 2.90 m/s (T3-hoverfix).
2. **The rocket's muzzle** was 0.3 m above where the 1P launcher is drawn (the T-pose reading above): now the drawn point.

**Not shown / not tried:**
- **The real mouse and keys** (owed before the merge): every press here went through the harness's key mappings (`KeyMapping`), not the
  desktop; the owner's idle time wasn't checked because no real input was sent.
- One G-mid-hover run (T4h: 12.7 m up, G, drifting forward) landed unhurt where two others (T4i, T4k) hurt; not reproduced, not chased.
- A second player (her sounds' `_3p` copies, Recon seen by others, her pushes on a player), the Nether, lava, ladders, boats, a slope
  fight; Barrage holding her in the air (she falls through it unless Jump is held: the data's fuel hold is M, the wiki's text says she
  stays still: an Overwatch side by side settles it); her visual effects (stand-in particles); her emote sounds (silent); the intro in
  a real Play of the Game (wired by the agent through `HeroModels`' `<hero>_intro_clips`, not played); the select screen's framing
  (Sojourn's 0.72, not tuned); a critic.

## Critic 1 and fix 1 (2026-11-11)
Findings: [critic_1](../testing/runs/pharah/critic_1.md); each reproduced on c849b15 and shown fixed with the same steps:
[fix_1](../testing/runs/pharah/fix_1.md) (raw rows beside it).
- **Her hover ends while she can't act** (sleep, stun, knock-down, Gravitic Flux: the synced `ASLEEP`), and comes back on waking with
  Jump still held. The server ends its hovering flag itself (`Pharah.tick`): her client sends input only on a change, so a flag set
  before the sleep stayed and drained the fuel.
- **No hover where Jump is Minecraft's or the mount's:** `HeroManager.jumpIsNotAirborne` (ladder, boat, horse, lava, water, cobweb,
  powder snow); before, only water was left out and a ladder, a seat or lava emptied the tank.
- **A rejoin in mid-air doesn't hurt** (every hero, `HeroManager.JOIN_FALLS`): the select screen is open and leaving ended her hover.
  Lesson for any per-player fall flag: the server's onGround read true a tick before the landing's damage check, and the fall distance
  stays 0 while the client is still loading, so the flag waits for the fall to begin and ends when the distance is back to 0.
- **Data checks:** her jets cut a reload (0027 / 4B92 #82 push v33 at 0003 over the reload's 0023 #60 at 0002, M), and Barrage doesn't
  end a jet (no message from 002B reaches 0027 / 4B92; same priority 0003, M). Code kept, cited. By the same reading Concussive (0001)
  wouldn't cut a reload, though the code does: open, for a side by side.
- Still owed: the real mouse and keys, a real Ana / Brigitte, a second player; every cooldown resets on a rejoin (shared, the owner's
  call).

## Calibrated to the Workshop recording (2026-11-12, branch feature/calibration-1111)
The owner's call (2026-11-12): "our game should match overwatch". Applied from [pharah_workshop_2026-11-11](pharah_workshop_2026-11-11.md)
and, for every hero, issue #130. Played with the harness (`dev.py` keys, `trace` per tick, `state`), not a person's hands; fresh flat
survival worlds; the old build (`next` 89289d2) then this one. Raw: the session scratchpad `calib/`.

| Change | Recording | Old build (seen) | This build (seen) |
|---|---|---|---|
| Hover eases to `HOVER_RISE` 4.66 at `HOVER_EASE` 6 /s, never below gravity's step (`Pharah.hoverStep`); `HOVER_PUSH*` retired | settles at 4.66; dv/dt = 6 (4.66 - vy) | ground press: 4.89, 4.05, 3.21, then **2.90** held | 4.99, 4.11, 3.24, 2.36, then 2.96, 3.40, 3.72 ... **4.66** from 1.2 s; fuel out at 2.15 s |
| Hover 0.2 s after a ground press, airborne or not; an air press at once (`ClientPharah.hover`) | 13 ticks (0.21 s) of gravity, then the ease | (the push only acted below 2.9) | 3 ticks of plain gravity after the jump tick (0.2 s), then the ease |
| No lift from a jet without Jump (`JET_PUSH`, `JET_PUSH_EMPTY` retired) | Jump Jet falls at -17.5 to its end; Jet Dash apex 1.72 m | Jump Jet top **9.02 m**; Jet Dash top 3.27 m (2.90 held 9 ticks), **16.1 m** to landing | Jump Jet top **8.25 m** (OW from standing about 8.4: our 20 Hz steps, below); Jet Dash top **1.83 m**, **11.0 m** to landing (OW 10.2 m in 0.85 s) |
| Jump Jet with Jump held | gravity exact above 4.66, then the ease | - | 17.00, 16.13 ... 7.38 (gravity), then 6.67, 6.15 ... 4.66 |
| Jet Dash keeps 0.3 of her horizontal speed (`JET_DASH_KEEP`) | 20.65 from a 5.5 run | (by code: 19, the run dropped) | first tick's average 19.66 from a run against 18.12 standing (19 + 1.65) |
| Barrage cast in the air: speed zeroed, no gravity, a 3D move at 4.40 (`OwMovement.pharahBarrage`, `BARRAGE_UP` / `_DOWN`) | vy -4.81 -> 0; W+Jump 4.15 / +1.4-1.5; W+Crouch -3.11 at 45 deg; Jump+Crouch level; 0 -> 4.4 in 0.29 s | cast falling at -10 m/s: fell on through it to the ground (gravity) | cast at -3.23: 0.00 at once, held still with no key; W+Jump 4.12 / +1.54; W+Crouch 3.11 / -3.11; all three level at 4.40; 0 -> 4.4 in 0.25-0.3 s |
| Concussive's cooldown from cast + recovery (`ability2Cooldown` = 0.5 + 8 at the release) | 8.66 s press to ready | (by code: 8.15 s; not timed in game) | cooldown shows 8.50 at the release, ready 173 ticks (8.65 s) after the press |
| Every hero: `STOP_SPEED` 4.5 -> 5.5, `GRAVITY_UP` 16.7 -> 17.5, `JUMP_SPEED` 5.72 -> 5.86 (`OwMovement`, #130) | stop 0.067 s / 0.14 m; jump top 0.98 m, 0.67 s; 17.5 up and down | stop: 3.00 then 0.25 m/s ticks, 0.1625 m; top 0.979 m, 13 ticks up; 16.7 rising | stop: 2.75 then 0, 0.1375 m; top 0.979 m, 13 ticks; first air tick 4.99 (5.86 - 17.5/20) |

The #130 check, hero by hero (each picked by `hero <id>`, hero mode on, a tap of Jump standing, a 1 s run forward and release), the
same on both builds for Kiriko, D.Va (mech), Mercy, Lifeweaver, Ana, Sombra, Brigitte, Juno, Sojourn, Pharah and Sigma as above.
Tracer (6.0 run): stop 3.37 / 0.38 / 0 old, 3.13 / 0.12 / 0 now (0.1875 -> 0.1625 m; the floor stays 5.5, her run's start isn't
recorded stopping). Lúcio (his own 5.5-floor mover) and Jetpack Cat (she flies off the ground) unchanged, as meant. Illari isn't
selectable in this build. Lúcio's leap: `WALL_JUMP_SPEED` 6.9 -> 7.02 so it still tops at the recording's 1.41 m under 17.5 (unit
test only, not played). Juno's 10.8 double jump now tops at 3.33 m against "about 3.4" (was 3.49): left.

**Mind the tick convention:** our steps move by the average of the speeds before and after (exact for constant gravity), so a launch
v tops at v²/2g; Overwatch's 62.5 Hz steps, showing the speed after gravity, top a touch higher (Jump Jet from standing about 8.4 m
against our 8.25). Only the jump (`JUMP_SPEED`) and Lúcio's leap were retuned to the recorded tops; the jets keep the data's 17 / 8.

**Left as they were (unmeasured):** the fuel refill (45 a second on the ground after 0.3 s: the recording fits 100/s or an air refill
better, from one window: a recording is asked for in the analysis) and whether Concussive Blast cuts a reload (no ammo channel in the
log). Not played: Barrage with Jump alone and no move key (the log hasn't it either), the empty-tank fall's ease, real mouse and keys.
