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
