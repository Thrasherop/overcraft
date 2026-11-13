# Junkrat (hub)

Everything learned about Junkrat, by subject. Issue #73: **Damage, Specialist** (BZ 2026-02-10; his data's tag 116F), the fifth Damage
hero after [Sojourn](sojourn.md), [Pharah](pharah.md), [Tracer](tracer.md) and [Cassidy](cassidy.md), built the way Cassidy was. Up:
[Techniques](../README.md) · the method: [reading_game_data](../game-data/reading_game_data.md) · no Workshop recording of him yet
(the owner will be asked for one): his movement is the data's default mover, the tire's the data's own mover.

## His notes and sources
- **Public numbers:** [junkrat.md](../../tools/reference/junkrat.md): BZ 2024-01 to 2026-11 (his last number change BZ 2026-09-08),
  the wiki's revision 410886. **Total Mayhem is live** (no damage from his own explosives, bombs on death); sub-role **Specialist**.
- **The data map:** [junkrat-map.md](../../tools/reference/junkrat-map.md): graphs 022B (Frag Launcher) / 022A (Total Mayhem) / 096A + 096B
  (Concussion Mine) / 09BA + 09BB (Steel Trap) / 0232 + 0233 + 0234 (RIP-Tire) / 51E8 (perks) / 336B (values), the table of data against
  the public numbers. Node-by-node reading: `E:\Overwatch Archive\research\junkrat\work_kit\kit_junkrat.md`.
- **Clip maps** `tools/clipmaps/junkrat*.txt` (3P 139, 1P 67, launcher 19 + Heroic, mine in hand 9, tire 13, trap 1, Heroic intro); **sounds**
  `tools/soundmaps/junkrat.txt` (voice) and `junkrat_sfx.txt` (61 events).
- **Code:** `dev.overcraft.junkrat` (`Junkrat` the kit, `JunkratStats` every number with its source, `JunkratState`, `JunkratSounds`),
  `entity.FragEntity` (frags and the Total Mayhem bombs), `entity.ConcussionMineEntity`, `entity.SteelTrapEntity`, `entity.RipTireEntity`,
  `client.ClientJunkrat` (held still on the tire, the tire's view in `CameraMixin.overcraft$tireView`), `LifeweaverPropRenderer.tire`.
- **Models** (export script `research\junkrat\scripts\export_models.sh`): 3P 1170A, 1P 11709, launcher 1022D, frag 118EA, mine 10093, trap
  100DC, tire 118D8. The set trap's own 29B2 exports empty (1 KB): the trap draws as 100DC throughout.

## Built (commit 4adf3b8, then the play fixes)
Each number in `JunkratStats` with its source (D / BZ / W) and H / M / L.
- **Frag Launcher** (left button): 45 direct + 80 splash to 0.5 m, falling linearly to 10 at 2 m, no crit; 30 m/s at 17 m/s² (Frag Cannon
  37.5); off a block it keeps 0.5 of its speed into the face and 0.75 along it; it goes off on an enemy, on a ground bounce after an earlier
  bounce, or at 4.5 s (W's rule, M: the data's bounce block isn't named); push 5 away / 8 up (x0.75 at 2 m), half on him; 5 frags, 0.667 s
  apart (Bomb Voyage x1.35), reload 1.5 s with the frags back at 0.867 s. **Ours:** the 0.3 s freeze before a timed-out frag goes off isn't
  built; the halved size after the second bounce only matters for wall bounces.
- **Concussion Mine** (Shift; the right button sets it off): 2 charges, 7 s each; 0.144 s throw, 25 m/s at 20, sticks to a block, drops off
  a creature; from 0.1 s after the throw the right button sets it off 0.1 s later; 120 to 1 m down to 55 at 3 m, push 15 / 15 on enemies;
  within 3.5 m he's launched 15 away and 15 up, unhurt, and lands soft; one out at a time; 25 health. **Ours:** his launch replaces his
  speed (Combat.launch), the enemies' push adds to theirs.
- **Steel Trap** (E): 15 m/s at 20, sets down on the ground, off walls; armed 0.75 s after; the first enemy within 1 m: 100 and a 2.5 s root
  (speed and jump strength to nothing: a creature's and a player's, the client's noJump reads the jump strength); lets go at the root's end,
  gone 8 s later; 10 s from 0.5 s after the throw; one at a time; 100 health. **Ours:** no pull to the trap's middle, no "seen through walls"
  for him, the root doesn't block other movement abilities, the jaws don't close on a catch (no clip played).
- **RIP-Tire** (Q, 2100): the tire comes at the cast in front of him; the server drives it from his keys and facing (12 / 6 / 3 m/s at once,
  ours), Jump on the ground jumps 9 m/s (L), Jump held into a wall climbs 6 m/s (ours); he's held still and the view sits 3.5 m behind it
  (ours); either fire button or Q sets it off from 1.6 s; at 10 s it goes off by itself; 600 to 2 m down to 50 at 10 m, push 6 / 6;
  broken (100 health): no blast. **Ours:** G, death or a switch takes the tire away with no blast (W says his death doesn't stop it).
- **Total Mayhem:** none of his blasts damage him (`Junkrat.blastDamages`); dying in hero mode drops six 50 bombs within 2 m, off 0.8-1.3 s.
- **Specialist:** a kill gives the next reload within 3 s +50 % speed (1.0 s), the frags still back at 0.867 s (D: that wait has no scalar).
- **Perks:** Nitro Boost (Shift on the tire from 1.6 s, once: +100 % easing to +25 % over 2 s, 2.5 s; its blast halved), Bomb Voyage
  (+0.35 attack speed 3 s after a mine launch), Frag Cannon (x1.25 speed), Mine Recycling (a mine kill gives a charge back).
- **HUD:** the mine with its charges and recharge band (lit while one is out), the trap, the launcher's 5. **Select screen:** DAMAGE
  SPECIALIST, 250 HEALTH, LMB LSHIFT E Q PASSIVE.

## Played (2026-11-13, one dev client, `-Xmx3G`, fresh flat survival worlds `jr1` / `jr2`, harness keys)
The critic's defects, reproduced and fixed (2026-11-13): [runs/junkrat/fix_1.md](../testing/runs/junkrat/fix_1.md) (the tire
in mid-air, the root on a baby, frag pushes and falls, the tire across dimensions, the mine jump through a wall kept from the data).

Raw stamped output: [runs/junkrat/runs.txt](../testing/runs/junkrat/runs.txt), scripts in [runs/junkrat/scripts](../testing/runs/junkrat/scripts/)
(`drive.py`, `fight.py`, `j1..j8`). Screenshots in `run/screenshots/` (not committed; each opened): `jr_select1.png`, `jr2_select.png`,
`jr_hud0.png`, `jr_1p.png`, `jr2_1p.png`, `jr_front_big.png`, `jr2_back_big.png`, `jr_j3_trap.png`, `jr_j4_drive.png`, `jr2_rejoin.png`,
`jr_j8c_trap.png`. Targets: NoAI husks (zombies burn at noon), armor 0, 100 MC = 1250 OW. Expected -> seen:
- **Select** (clicked, new world): Junkrat 5th under DAMAGE, his model with the tire on his back; READY: 250, the HUD as above (mine "2").
  **Rejoin** (Save and Leave, Open world): the select screen with Junkrat chosen; READY: perk level 3 kept, 5 frags, 2 mines.
- **Frag** (J1): direct on a husk at 6 m 10 MC (125) ✓; a neighbour 1 m to its side 4.13 MC (51.6, splash at ~1.3 m) ✓; held 5 s: 3 left
  at 1 s, 0 at 3 s, the reload started by itself, 5 back 0.87 s in, done at 1.5 s, firing again ✓; R from 0: 5 at 1.0 s, done 1.6 s ✓.
  Against a wall 2 m ahead (J1f): off within 0.1 s next to him, pushed 0.8 m back and up, **health 20 -> 20** ✓ (Total Mayhem). At the floor
  1.5 m ahead (J1d): the first ground bounce bounced it 15 m on (the rule: only a second bounce sets it off).
- **Mine** (J2): stuck; set off 1 s later; 2 -> 1 -> 0 charges, one back 7 s after the first throw ✓; launch from his feet: 6.4 m up, 14 m
  back. **Two bugs, below.** After the fixes: the husk 1.9 m off 6.94 MC (86.7 OW against 90 by the falloff) ✓, landing **20 -> 20** ✓.
- **Trap** (J3, J8c): set down 2.9 m ahead; a walking husk stopped at 0.94 m from it, held ~2.5 s, then walked on; 100 -> 92 (100 OW) ✓;
  cooldown 10.0 shown, 9.55 at 0.87 s (from 0.5 s) ✓; gone 8 s after letting go ✓.
- **RIP-Tire** (J4): at 1.0 m ahead; W 1.5 s: 18.6 m (12.4 m/s) ✓; him unmoved ✓; the view behind the tire (`jr_j4_drive.png`) ✓; two husks:
  1.5 m off 48 MC (600) ✓, 6.5 m off 23.25 MC (290.6, the falloff's own number) ✓; Q at 0.5 s: nothing, at 2.2 s: the blast ✓; untouched: off
  at 10 s ✓; into a 3-high wall with Jump held: up and over ✓; a jump on flat ground ✓; a left-button tap set it off ✓.
- **Leaving play** (J5): G mid-tire: Minecraft mode, the tire gone, no blast, ult spent ✓; death with three husks round him: six bombs, each
  husk 100 -> 88 (3 bombs each) ✓; hero switch (H at spawn): Tracer 175, back to Junkrat 5 / 2, the mine and trap gone ✓.
- **Perks** (J6, LAlt menu clicks): Bomb Voyage: 5 shots in 2 s after a mine launch against 3 without ✓; Frag Cannon 1.87 blocks a tick
  (37.4 m/s) against 1.50 ✓.
- **Night fight** (J7, normal, midnight, monsters on; 3 zombies, 3 skeletons, a spider, a creeper, then what spawned; `fight.py`): 75 s,
  1 death (zombies at melee range), a tire cast, driven 3.3 s and set off among five, 3 mines, 3 traps, the launcher cycling, respawned as
  Junkrat.

**Found playing (fixed, each reproduced first):**
1. **No launcher in first person** (`jr_1p.png`: an empty view) and no idle pose: the clip map named his 1P stand clip 167C "crouch" and the
   launcher's 1C88 "base_85_crouch" (the sets play one clip for the stand and the crouch: `compact_019D.txt`, `compact_01E3.txt`). Renamed
   `idle` (and 1684 `run`), re-exported: the launcher in his hands (`jr2_1p.png`).
2. **The mine's blast hurt nothing** (J2a: a husk 1.9 m off took 0): it started at the mine's own place, on or in the floor's face, so every
   sight line began inside the block. Now from its middle, 0.15 m off its face and up (`ConcussionMineEntity.blastPoint`): 86.7 OW.
3. **A mine jump hurt him** (J2d: 20 -> 17, 37.5 from a 6.4 m fall): the server still had him on the ground the tick after the launch, which
   ended the soft landing at once. A launch now counts as moving him until the server sees him leave the ground (0.5 s at most,
   `Junkrat.LAUNCH_GRACE`): 20 -> 20.
4. **His mine and trap were gone after a death** (J5d/J5e: there 1.0 s after /kill, gone by 1.5 s, with no removal call): dead, he no longer
   kept his chunk loaded, it unloaded, and an entity type that isn't saved is lost. Both are now saved with their chunk (their stuck state
   and health too), and a placed one that isn't the one he has out (a newer throw, another hero, a session that ended without a leave)
   removes itself (`Junkrat.stillHis`); the respawn's fresh kit keeps them his. J8: both there after the respawn, the right button set the
   mine off.
5. **The select screen left out his passive**: Total Mayhem now listed (PASSIVE), as Kiriko's wall climb is.

**Not fixed (open):**
- **No launcher in third person or on the select screen** (`jr_front_big.png`, `jr2_back_big.png`): his launcher's clips key its root in the
  hero's space (idle at 1.62 m, reload_3p at 0.94 m: `glbroot.py` on junkrat_weapon.glb), so `HeroWeapon.bakedRoot` puts the root back at
  rest in third person, and it isn't drawn in his hands. Needs the hardpoint read for the third-person rig (as Ana's rifle and Sombra's pistol
  were); not done here.
- No `junkrat_glow.json` (the glows run found 427 materials and wrote no Junkrat glow table): `build_asset_pack.py zip` will list it missing.
- The mine in his left hand (10095) isn't drawn; the tire's drive / end stances and the trap's closing jaws aren't played.

**Not shown / not tried:**
- **The real mouse and keys** (owed before the merge): every press here went through the harness's key mappings.
- A critic; an Overwatch side by side (the frag's arc and bounces, the mine jump's arc, the tire's speed, climb and camera).
- Nitro Boost in play (Bomb Voyage was the minor picked) and Mine Recycling in play (no mine kill read); `presstrace lag 150` (no
  client-moved ability: the tire is server-driven, the mine's launch a server push; the tire under lag feels the round trip, not tried).
- Slopes, water, lava, ladders, the Nether (dimension change: code-read only, `Junkrat.clearWorld`), a boat, a second player (a rooted
  player's no-jump, the enemy's view of the tire, the trap and mine broken by an enemy's damage), the dragon (code-read only:
  `Junkrat.hurtArea` through the nearest part), his emotes and the intro in a real Play of the Game, the 0.1 s mine arming by hand (unit
  test only: J2e's press landed after 0.1 s).
- Sound events: the pack's `sounds.json` 2451 -> 2591, the 140 added all `junkrat.*`, every name the code plays among them; heard in play
  not checked event by event.
