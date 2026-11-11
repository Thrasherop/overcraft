# D.Va (hub)

Everything learned about D.Va, by subject. Hero 8 of the owner's 2026-10-28 list (issue #69), after Sigma: a **Tank, Initiator** (BZ
2026-02-10), **the second Tank** and **the first hero with two bodies**. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · researched the way Sigma's was (commit 1fb1dc2, branch feature/sigma:
[sigma](sigma.md), whose Tank role code she builds on) and Sojourn's ([sojourn](sojourn.md))

## Her notes and sources
- **Numbers:** [dva.md](../../tools/reference/dva.md): Blizzard's patch notes to 2026-11 and the wiki's revision 408749, each number with its
  game-data value. Her last change is BZ 2026-10-06 (missiles 11 -> 14 a second), the data build's own day, and the data has 14: the
  data's numbers are the live ones.
- **The data map:** [dva-map.md](../../tools/reference/dva-map.md): both bodies' kits and movers node by node (H / M / L); the detail and
  the pipeline in `E:\Overwatch Archive\research\dva\` (`work_kit\kit_dva.md` the pipeline, `kit_dva_mech.md` / `kit_dva_pilot.md` the
  readings, `work_kit\c_<g>.txt`, `work\clipmap_dva\`, `extract\`).
- **The plan:** [dva-plan.md](../../tools/reference/dva-plan.md): where the sources disagree, **what she needs that the mod lacks** (two bodies
  as one player, per-form health and ult meters, the mech's death as an eject, the Initiator sub-role, armor and shields together, a
  projectile-eating box, a hitscan shotgun, flight along the aim, the mech's box), what maps onto Sigma's Tank code, the order.
- **Clip maps:** `tools/clipmaps/dva.txt` (MEKA 3P, FBB0, 139 clips), `dva_1p.txt` (MEKA 1P, 10354, 75), `dva_pilot.txt` (pilot 3P, F2FF,
  100), `dva_pilot_1p.txt` (pilot 1P, F32B, 52), `dva_weapon.txt` (Light Gun, 10246, 11). Written by
  `research\dva\scripts\clipmaps_dva.py` (Sigma's writer: each clip named by the alias holding it), which also lists the clips a set names
  on the other body's model.

## What her data taught (2026-11-02)
- **A hero can be two entities.** The hero entity (040A) is the pilot; the MEKA is a **pet** (0407, STUPetComponent) with its own mover,
  health, graphs and weapon manager, spawned by Call Mech (026A #94). While linked, control and camera pass to the pet and the pilot rides
  attached, hidden and untargetable (026A #98). Crawl from the hero file and **every** entity it spawns (the pet's graphs aren't on the hero
  entity's list), and give each body its own values graph (33D9 the mech's, 348C the pilot's: `varvalues.py` now takes G from the
  environment).
- **The value table's names lie; the readers don't.** 1185 "11" is the Fusion pellet count, not a missile rate; 0404 "Micro Missile
  Cooldown" 14 is the missile fire rate (the 2026-10-06 patch); 03BE "Self-Destruct Damage Min" 250 is Call Mech's landing; 1B57 / 1B58
  "Boosters Duration" are the wreck's 1.0 s and 0.75 s; 0C6A "Self-Destruct Outer Radius" 3 is the fuse; 12C3 / 12C4 are perk XP. Name a
  value by the node that reads it (the reading_game_data rule, proven again).
- **StateModifyHealth's x60671788 mode is not always "percent".** Self-Destruct's 1000 is that mode as a flat amount (0B2F #280); Sigma's
  barrier regeneration read it as a share of max. Check the amount's scale (a 9999 heal, a 1000 blast) before calling it a share (M).
- **The two readings disagreed once, and the one that read the graph whole was right:** the mech pass found "no launch" for the eject; the
  pilot pass, reading 026A from its start, found #97 (a 9999 heal with 6 / 7 m/s knockback from a point ahead of the mech): heals carry
  knockback too. Split a hero's graphs between readers by body, then let the one that owns a transition settle it.
- **The mech is the hero default mover but its size** (2.3 m tall, 0.7 m radius); its 1P eye field is 1.74 m standing and crouched (the
  same number: no crouch). Sigma's 1P field was 2.1 against his head hardpoint's 2.06, so the field is a fair stand-in for the eye (M).

## Steps 1-3 (the two-body shell, the hero and her models, movement), played 2026-11-02
Branch `feature/dva` on Sigma's Tank code (merged `feature/sigma`, then `next`). Code: `dev.overcraft.dva` (`Dva`: the forms,
the wreck, the eject, Call Mech; `DvaStats`, each number with its source), `entity.DvaMechEntity` (the unmanned mech) and
`client.model.DvaMechRenderer`, `HeroManager.DVA_FORM` (her form, synced), `HeroManager.refreshHealth` (max health per
body), `HeroState.owHealth / armor / layer(player)` and `Hero.owHealth / armor / layer(player)` (per body, read by Shields,
the HUD), `Combat.ultCost(state)` (Self-Destruct 1700 / Call Mech 280), `Tank.initiator` / `initiatorReady` and
`Dva.initiator` (the heal, for Boosters in step 6), `HeroBody.DVA_MECH`, `Shields.setRaw`, `DvaMathTest`. Models exported
with `tools/owm_survey.py --glb --keep=<clip map> --entity=<owentity>` (Blender 5.2 headless, Overwatch checked closed) from
`research\dva\extract\Heroes\D.Va\Skin\Overwatch\Overwatch` into the shared pack: `dva.glb` (FBB0, entity 0407, 139
clips, 3 min), `dva_1p.glb` (10354, 041F, 75), `dva_pilot.glb` (F2FF, Gameplay3P, 100, 4 min), `dva_pilot_1p.glb` (F32B,
Gameplay1P, 52), `dva_weapon.glb` (10246, Light Gun-5402, 11); her icons with `build_asset_pack.py ui <extracted> dva` (the
hero filter is new). Dev client run2, world `dva1` (flat survival), screenshots in `run2/screenshots/` (not committed), each
looked at.

**Clips from the data, not by eye:** her default locomotion is the dfE5 *default* branch; dfE5 category 1A5 is what Fusion
Cannons' firing state sets (050A #333: H), so the 3048 move set (1.07 s) and the 3451 / 3452 stands are the firing ones.
Renamed in the clip maps: MEKA 3450 `idle`, the 30F8 set `run` / `run_*` / `strafe_*`, 2376 / 2377 `jump` / `fall`; 1P
237D / 318B; pilot 322D `idle`, the 3229 set, the 16896 crouch-walk set, 169CD `crouch_idle`, 3216 / 3219 `jump` / `fall`
(3219 is also her seated pose: set 048A's dfE5 cat283, 026A #832); pilot 1P 30B6 / 30C0 / 30B3 / 30B4 / 30AA; the gun's 3121.

**Data vs plan:**
- **The pilot in the cockpit (Open 2) is settled: she isn't drawn.** The mech model FBB0 already holds the pilot's hands
  on its controls (`s3_noseat.png`), and the data hides the pilot entity while linked (026A #794). The plan's default
  (the pilot model seated at the attach point) hung her legs out under the mech (`s3_3p_front.png`): removed.
- **Call Mech's drop is the clip's own:** the mech entity sits on its spot and 311F brings it down from the sky; her 3120
  runs and flips her into it (`s13_call_a.png`, `s13_call_c.png`). No drop height of ours.
- Select screen: the MEKA, framed at 0.42 / 0.55 (by eye with `selectframe dva`; the others' framing filled the screen
  from the cannons down: `s2_select_dva.png` -> `s2_frame_a.png`).

**Played (expected -> seen):**
- **Select** (`s1_select.png`, `s2_frame_a.png`, `s7_rejoin_select.png`): TANK has Sigma and D.Va; "TANK INITIATOR",
  "350 HEALTH, 325 ARMOR", seven kit icons (LMB Fusion Cannons, RMB Defense Matrix, LSHIFT Boosters, E Micro Missiles, Q
  Self-Destruct, LMB Light Gun, Q Call Mech). Pause card D.VA TANK INITIATOR (`pause.png`).
- **Mech HUD and body** (`s3_1p_mech.png`, `s3_3p_front.png`, `s4_tunnel_back.png`): 675 | 675, 27 pips (14 white, 13
  armor yellow), Boosters / Micro Missiles boxes, the matrix and cannon panels; the cockpit and cannons in first person;
  `state` box 1.40 x 2.30, eye 1.74 on client and server; health 54 Minecraft. A 10-health hit (125) took 118 (armor's 7):
  54 -> 45.56.
- **Doors and tunnels:** the mech stopped at a 1-wide, 2-high door (z 5.30 = the wall at 6 less 0.7) and a 2-wide, 2-high
  tunnel (also with the crouch key held: still standing, `crouching` false), went through a 2-wide, 3-high opening (z 16.4).
  The pilot went through the door and the tunnel standing (z 19.4 / 19.1) and crouched (`s9_pilot_door.png`).
- **G (Minecraft mode):** the mech at 45.56 -> G: the pilot's 14 / 14, Minecraft's box, through the tunnel; G back outside:
  45.56 / 54 again (`s4_g_pilot.png`). G back inside the tunnel: hero mode, the small box kept until she walked out, then
  1.40 x 2.30; no suffocation.
- **The mech's death:** `/damage 200`: health 0.01 (no death screen), form WRECK, its 31C3 destruction playing, inputs
  held, 1.0 s, then the eject: 14 / 14, pilot box, `eject` clip (`s5_eject.png`: 175, 7 pips, Light Gun 20 | 20, the wreck
  beside her for 0.75 s). Self-Destruct's charge kept for the next mech (553.5 carried), Call Mech's from 0.
- **Found (fixed): no eject launch.** `needsSync` alone left her standing; the motion packet sent straight to her client
  launched her 1.4 m up and 4.8 m back over 0.8 s (expected 1.40 m, 4.8 m: 7 up, 6 back, gravity 17.5).
  [minecraft_26](../minecraft/minecraft_26.md).
- **Call Mech** (`overcraft ult @s 100`, Q): 0.2 s idle, then 3120 / the mech's 311F; she is the mech 1.95 s after the
  press, 1.5 m ahead (z -9.5 -> -8.0), at 54 / 54. A 100-health husk 1 m from the spot took 19.68 (250 = 20, less its 2
  armour points), one 3.8 m off none. Q 0.3 s after an eject: nothing (the 1 s lock); at 1.6 s: the call. Q in a 2-wide,
  2-high tunnel: "No room for the mech here", the 280 kept (`s11_noroom.png`).
- **Pilot:** 5.49 m/s, jump 0.98 m (W 0.98), crouch-walk about 3 m/s, Call Mech charging 5/s, the Light Gun in her hand in
  both views (`s9_pilot_1p.png`, `s12_pilot_front.png`). **Mech:** 5.49 m/s forward, 4.78 back (expected 4.95: 90 %), `run`,
  `run_back`, `strafe_left`, `run_forward_left`, `jump` / `fall` playing; jump 0.98 m (`s10_mech_run.png`).
- **Falls:** the mech from 6 m: 18 Overwatch (2 Minecraft less armor's 7), the pilot from 6 m: 2; the eject from 12 m: none.
- **Death and respawn:** the mech killed, then the pilot: the death screen; respawned in the mech at 54 / 54, its box.
  **Rejoin** (Save and Leave, a new client): D.Va on the select, in the mech at 54.
- **Not tried:** two players (others seeing her forms), the wreck in water / lava / on ladders, boats (default 10: the mech
  enters none), a stun during Call Mech (Ana's Sleep Dart from a second player), the Initiator heal in game (it needs
  Boosters: step 6), the call on a slope or a ledge's edge (the 4 m probe), the HUD's ult icon change seen only as pilot.

## Step 4: the Fusion Cannons, played 2026-11-02
Code: `dva.FusionCannons` (each number in `DvaStats` with its source), `Shields.pellets` / `Shields.armorReduce(ow, hits)`.
A **hitscan shotgun**: every 0.15 s (3 ticks, Combat.attackRate) 11 rays from her eyes in the data's pattern (1 in the middle,
4 at half the 3.375 degree cone, 6 at its edge: D 050A #444; the pattern turned at random each volley: ours, the data's
"jitter 0.7" unread, L), one entity search a volley. **A volley's pellets on one creature are summed into one hit** (one
`Combat.damage`): Minecraft's hurt cooldown is cleared per call anyway, but eleven calls a volley would be eleven hurt
sounds, knockbacks and kill credits. Armor (another D.Va, Brigitte) takes the summed hit **as its pellets** (`Shields.pellets`
set around the call: each 7 smaller, each at most halved: 22 -> 11, as W's "a 2-damage pellet on armor does 1"). Damage
source: an arrow's type with her as both the direct and the causing entity, so `Barriers.absorb` sees a hitscan shot (Sombra's
pistol passes a null direct entity, so an enemy barrier never took it: worth fixing there). The -30 % slow is an attribute
modifier while the button is held and she can fire, off while boosting (D: gated by Boosters' v87). Not while the matrix is up.
Stand-in look: a crit spark where a pellet hits, smoke where it ends; a firework-blast sound a volley.

**Played** (world `dva46`, flat, midnight, NoAI targets with armor 0; harness driver `oc.py`: one socket, chat-command
answers read from the log). Expected -> seen, Overwatch points:
- **One volley** (the button held 0.06 s) on a 3.6 m wide giant: slant 6.6 m 22 -> **22.00**; 15.6 m 13.39 -> **13.57**
  (the hits land on the box's near face, a little closer); 25.4 m 6.60 (the 0.3 floor) -> **6.60**; 35.3 m -> 5.40 (9 of 11:
  the cone, 2.1 m wide there, outgrows the giant). The falloff counts the 3D distance to the hit.
- **Rate:** held 2.03 s: 286 = **13 volleys** (2.03 / 0.15 = 13.5): 6.4 a second, 141 damage a second on one target.
- **Headshot** (a zombie 5 m off, aimed at 1.72 m): body 22, head **38** (8 pellets x2 on the head, 3 on the body).
- **A group:** three zombies 0.8 m apart at 5 m: 0 / 22 / 0 (the cone is 0.6 m wide there); at 12 m: **1.72 / 5.16 / 1.72**
  (1 / 3 / 1 pellets at x0.86, each creature one summed hit) (`s46_group12.png`).
- **An ally:** a villager 4 m ahead of a giant: villager 20 -> 20, the giant behind it 22 (passed through).
- **A wall:** stone between: 1 s held, the giant took 0, the wall whole (`execute if blocks`: 66 blocks the same).
- **Slow:** forward 5.50 m/s, firing **3.85** (x0.70) from the first volley's tick, 5.50 again on release (trace).
- First and third person while firing (`s46_fire_1p.png`, `s46_fire_front.png`): hits flash the target, HUD unchanged; ult
  charge from her damage (199 -> 229 for 22 dealt, plus the 5 a second passive).
- **Not tried / open:** the firing clips (3P ABE1 -> AACD -> 13525 and its additive layer, 1P 352D / 334B) don't play yet: the
  mech stands in `idle` while firing (with the effects, step 11); armor on another tank (needs a second player: per-pellet
  armor is unit-tested only); Precision Fusion (step 10); an enemy Sigma barrier in the way (no second player).

## Step 5: Defense Matrix, played 2026-11-02
Code: `dva.DefenseMatrix`, and **`Barriers.absorbBox(player, Box, Eater)`**: Kinetic Grasp's projectile-eating box made general
(a `Box` of side / down / up / near / far in the aim's frame; the last and next step of each projectile sampled 9 times; each
eaten one handed to the `Eater` with its damage, then stopped whole). **Grasp itself still runs its own copy of the loop:**
Sigma's branch has uncommitted step-10 edits inside that loop (2026-11-02), so moving it onto `absorbBox` here would
conflict; do it after his effects merge. The matrix: the right button held (`secondaryHeld`), 0.5 s minimum, 33 a second, 1 s
cooldown after each use, 16 a second back from 0.75 s after it's down; 4 % of what it eats to her ult; no Fusion meanwhile;
down at once in the wreck, on G and asleep. HUD: the energy as a number and a band on its panel (pink while up, white full,
grey cooling down). Stand-in look: glow sparks through the box past 3 m, a poof and a chime where it eats one.

**Played** (`dva46`; mobs held with `movement_speed` / `flying_speed` 0, so they still shoot; she at full health each window):
- **Resource** (trace of the synced energy): from full, empty about 3.0 s after the press (expected 3.03); back **15 ticks**
  after empty (0.75 s), **8.0 in 10 ticks** (16 / s), full 124 ticks (6.2 s) later; a 0.05 s tap costs **16.5** (the 0.5 s
  minimum). Let go and held again at once: it rises a little through the 1 s cooldown, then drains again. (The traced
  numbers come in bunches at 10-26 fps: read totals, not single ticks.)
- **Three skeletons 8 m ahead:** matrix up 3 s **lost 0** (ult +23.5 against the 16.8 passive: 4 % of about 170 eaten); down
  3 s lost 6.88 (`s5_skeleton_matrix.png`: the energy 0 at the end).
- **Two blazes:** up 3 s **lost 0** (ult +31.8: about six fireballs eaten); down 6 s lost 4.94 and she burned.
- **A witch 6 m ahead:** up lost 0; down lost 1.0 (a potion); potions count 0 damage, so no ult from them.
- **A ghast 18 m off:** up **lost 0**, ult +10.7 over the passive = 4 % of one fireball's 262 (6 + its blast's 15); down lost
  5.44 (`s5_ghast_matrix.png`).
- **Not stopped:** an ignited creeper 3 m ahead with the matrix up: **lost 16.19** (blasts aren't projectiles); two skeletons
  at her side, the matrix up ahead: **lost 5.88** (the box is only ahead).
- **Not reproduced:** a guardian's beam: in a glass pool it never saw her, in a pool sunk in the ground it never attacked
  (no beam drawn either time). By code the matrix only takes `Projectile` entities, so a beam passes; unplayed.
- **Not tried:** the Toggle Defense Matrix option (the hold is Overwatch's default; the option is not built); Shield System's
  25 % (step 10); hero projectiles from a second player.

## Step 6: Boosters, played 2026-11-02
Code: `dva.Boosters` (server: the flight's state, hits, cooldown, Initiator), `OwMovement.dvaBoosters` (her client: 12 m/s
along her look each tick, no gravity, turning with the mouse; also through water, by the `travelInFluid` hook Juno's glide
uses), the sync's `Dva.MODE_BOOSTING` (as Sigma's Flux flight: the server decides, her client moves her),
`Boosters.knockbackFactor` in LivingEntityMixin (x0.1 while flying). Shift starts it (ready, in the mech, awake); Shift again
from 0.4 s ends it; 2 s at most; the 4 s cooldown is set 0.3 s after the end; the start is Initiator's trigger
(`Dva.initiator`). Each enemy once a flight in a 1.5 m sphere 1 m up and 2 m ahead: 25, and 17 m/s along her flight + 4 up
(a player's push sent straight to their client). A new mech (Call Mech, a respawn) starts with its matrix full and nothing
cooling down (each mech is a new pet entity with its own vars: D 0407, M). Stand-in: small flames on her back, a
fire-charge sound; her 0x3BB clips are triggered (`boosters_se0` -> `boosters_se1`, `boosters` at the end).

**Found playing (fixed): Boosters' landing hurt her.** Aimed 30 degrees up she rose 12 m and **lost 8.44** landing (a plain
12 m fall's damage). A log at both ends showed why: the server took her client's "on the ground" **2 m above the ground, two
ticks before she landed**, cleared `softLanding`, and the landing counted the whole 12.8 m. The lesson was already written
([minecraft_26](../minecraft/minecraft_26.md) "A fall is checked after the ground", from Mercy) and steps 1-3 hadn't used it.
Fix: while `softLanding` is set the server forgets her fall each tick (`resetFallDistance`, as Sigma's Flux does). After:
30 degrees up 54 -> **54**, 60 degrees up (17 m) 54 -> **54**, twice, on two builds; the control, a plain 12 m fall in the
mech, still **8.44**. The eject's launch goes through the same path (it passed in steps 1-3 by timing).

**Played** (`dva46`, traces of her client's speed):
- **Level:** 12.0 m/s from the 3rd tick after the press, 2 s, **23.8 m** (expected 24); the cooldown appears 0.3 s after
  the end at 3.95 s (4 s).
- **Aimed up 30 degrees:** 10.38 across, **5.99 up** (12 cos / sin 30); 60 degrees: 5.99 / 10.38.
- **Shift again** at 0.8 s: ends it (19 ticks, 11.8 m); at 0.2 s: ignored (the full 23.8 m).
- **Three zombies in a row** (held by `movement_speed` 0, armor 0): each took **25 once** (200 -> 198) and was thrown 3.2-3.7
  m along her flight (`s6_hit_1p.png`); they punched her for 4.9 meanwhile.
- **Initiator:** hurt, a boost healed **+4.00 = 50** over about 1 s; another 4.8 s later (hurt again) **0** (the 5 s
  lockout); one past the lockout **50** again; at full health nothing. (A first try read 5.25 inside the lockout: the tank's
  own regeneration had started 6 s after the hurt. Hurt her again right before each boost.)
- **A wall** 8 m ahead: she stopped at **z 7.30** (its face less her 0.7 m radius), no damage (`s6_wall.png`).
- **Water** (3 deep): **12.0 m/s** throughout, level (`s6_water.png`). **1-block steps:** a level flight stops at the first
  (z 4.30): a full block is a 1 m wall to a flight, as in Overwatch; aim up to climb.
- **Fusion while flying:** mode 40 (boosting + firing), **12.0 m/s throughout** (no slow); **the matrix while flying:** mode
  48 (boosting + matrix) (`s6_matrix_boost.png`).
- **G mid-flight:** the flight ends, Minecraft mode; G back: the mech, mode 0.
- Both views (`s6_boost_3p_b.png`, `s6_boost_1p.png`): the LSHIFT box lit while flying, its cooldown counting after. Found:
  the first stand-in flames (big, at chest height) filled the third-person camera; now small flames low on her back.
- **Not tried:** the knockback she takes while flying (x0.1, no push measured mid-flight); Extended Boosters (step 10);
  Hold To Use Boosters (the option isn't built); hitting a second player.

## The night fight after steps 4-6 (2026-11-02)
`dva46` at midnight, monsters spawning, a wave of 12 around her (6 zombies and a husk, 4 skeletons, a creeper, a spider)
14-22 m off, she in a full mech. The harness drove it as a player would: the left button held throughout, her aim turned to
the nearest of the wave every 0.5 s (`execute ... anchored eyes facing entity ... eyes run tp @s ~ ~ ~ ~ ~`), the matrix toward
a skeleton within 16 m now and then, Boosters every ~12 s. **8 of 12 dead in 20 s** (the kill feed: zombies, a skeleton, the
creeper: `s7_night_30.png`, `s7_night_3p.png`); about 30 s in the mech's 675 ran out (a creeper's blast took 30 in one), the
wreck and the eject in the middle of the crowd, and the pilot, with no Light Gun until step 9, died 9 s later. Respawned in
the mech. Nothing in the HUD broke (the low-health vignette, the kill feed and the Boosters cooldown all right).
- **Harness lessons** (worth keeping, [harness](../testing/harness.md)): `tp ... facing` aims from the **feet**: for the 2.3
  m mech every nearby zombie's eyes were above her and she fired at the sky (0 kills); anchor at the eyes. And a fight loop
  that fires 0.8 s per 2.5 s loop (counting, reading state, aiming between) spread its damage over whichever monster was
  nearest and killed nothing in 40 s: hold the button and only aim in the loop. Monsters that spawned naturally during a fight
  stay after `spawn_monsters` goes off and hit her after a respawn: clear them before the next run.

## Steps 7-9: Micro Missiles, Self-Destruct, the Light Gun, played 2026-11-02
Code: `dva.MicroMissiles` + `entity.DvaMissileEntity`, `dva.SelfDestruct` (the `DvaMechEntity` kind SELF_DESTRUCT flies
on and calls it each tick), `dva.LightGun` + `entity.LightGunShotEntity`; each number in `DvaStats` with its source;
`Dva.eject(player, s, wreck)` now serves both the wreck and Self-Destruct (the data's one eject message 016C). Stand-in
look and sound throughout (firework / snowball items, glow and explosion particles): her own are step 11.

**From the data, not by eye:** the missiles' muzzles are hp 0075 / 0077 (1128 #357's m_muzzles), read off the 1P model
10354's empties: 0.88 m either side, 1.976 m up, 0.32 m ahead (the 3P model's are posed and asymmetric: the 1P rest pose
is the one to read). #357's lifetime field is 0 and its m_FEC435C6 100: read as a 100 m range (2 s), M.

**Found playing (fixed): a direct missile hit lost a quarter of its splash.** A giant took **131 of 135** every volley.
Logging each burst showed it at the missile's centre, **0.175 m off the box** (Minecraft's entity hit location is the
projectile's position, not the contact), so the splash read d = 0.175 (x0.96). The burst now sits at the box point nearest
the missile: **135.0**, three volleys. Block hits were right already (the clip is the centre ray to the face).
[minecraft_26](../minecraft/minecraft_26.md).

**Played** (world `dva79`, fresh flat, midnight; targets held with `movement_speed` 0, armor 0; driver `dva_oc.py`):
- **Missiles:** 18 on a giant 10 m off: **135.0** (18 x (2 + 5.5)), the same with the matrix up (her own aren't eaten) and
  while boosting (modes 16 / 32). The cooldown appears **1.44 s** after E (0.25 + 17/14 = 1.46) at 7.0, ready **8.46 s**
  after; E inside it does nothing. **Splash** at a wall: zombies 0.3 / 1.0 / 1.7 m from the burst took **88.4 / 51.3 / 0**
  (expected 88.4 / 51.3 / 0). **Self-damage:** her front 0.5 m from the burst, **39.4** (77.8 halved by armor a missile: 38.9).
- **Self-Destruct** (`overcraft ult @s 100`, Q): she ejects at once (pilot 175, Light Gun full); 3 s later zombies with
  their nearest point 3 / 6 / 12 / 18 / 21 / 21.6 m from the centre took **1000 / 1000 / 620 / 240 / 50 / 0** (D's formula
  exactly); one behind a stone wall **0**, one in the open at 7.7 m **892.3** (892.3). Knockback: thrown **4.98 / 4.2 / 3.0 /
  1.9 / 1.4 m**, against a Minecraft-physics simulation of 22.4 -> 7.8 m/s linear: 4.82 / 4.03 / 2.84 / 1.76 / 1.29.
  **Call Mech full (280)** right after. **From the wreck:** `/damage 200` with Self-Destruct charged, Q within the second:
  pilot at once, a zombie 3.7 m off took 1000. **While boosting:** the mech carried on 2.4 m past her and stopped (on the
  ground its friction halves its speed a tick: Overwatch's own slide isn't in the data, M).
- **Light Gun:** held 2.0 s: **14 rounds** (7/s), a giant took **196** (14 each); **body 14, head 28**; R from 6 left: the
  rounds back between **0.88 and 0.95 s** (0.91), the reload over at **1.41 s** (1.4); held 3.2 s from full: empty and
  reloading by itself. HUD `6 | 20` (`s9_gun_1p.png`).
- **Call Mech's charge:** +20.3 in 4 s after an eject (5/s), +61.5 for four rounds and a second (56 + 5). **On a ledge:**
  1.15 m from a 3 m drop's edge, facing off it: the mech came down below (y -60, z 8.15), and she drove it.
- **Dying as the pilot:** the death screen; respawned in the mech at 54 / 54 (a real death, the only one).
- **Night fight** (a wave of 14: zombies, a husk, four skeletons with bows, a creeper, a spider; monsters spawning; 70 %
  Self-Destruct charge to start; the harness held the left button and aimed at the nearest each 0.5 s, E whenever ready,
  Boosters every 12 s, Q when charged): the charge filled from Fusion and missiles by **7.6 s**, **Self-Destruct killed six**
  (the kill feed, `night_15.png`); as the pilot the Light Gun fought on (ammo 16 -> 1), Call Mech charged in 3 s, she called
  it at 31 s and fought on in the new mech to 75 s, ending at 50.6 / 54. **Harness trap, not a bug:** the call said "No room
  for the mech here" for 20 s because the eject had put her against the splash test's leftover wall (the mech's 1.4 m box
  didn't fit): clear the arena between runs.
- **Not tried / open:** a stun during Call Mech (Ana's Sleep Dart needs a second player); two players (her missiles and
  Self-Destruct on another hero, armor and shields under them, the warning seen by others); Self-Destruct falling out of the
  world (a flat world has a floor); the Light Gun Input option (Primary only, the default); the Light Gun's muzzle is
  Mercy's measured place, not hers (M); in the night fight her ammo stayed at 16 for several seconds after the eject while
  the button was held through it (unconfirmed: she may have been aiming at the wall; worth a look); the missile, Self-Destruct
  (3644 additive) and fire / reload clips are triggered but the additive layers don't play on her yet (step 11).

## Step 10, perks, and step 11, voice (2026-11-10)
Sources: BZ patch notes to 2026-11-05 (no D.Va change since 2026-10-06 but a Stadium fix) and W revision 408749 (the plan's): her
four perks are unchanged. Code: `DvaPerks` (numbers in `DvaStats`, each cited: 514D, 026A #908 / #941, 047E #458-#463), `Shields`
(a hero with shields over armor: `armorLost` keeps the two apart; damage takes shields then armor, healing mends armor then
shields, only shields regenerate, a hit that lands on shields isn't armor-reduced: ours for the split hit, M), the HUD (blue pips
over the yellow, Precision Fusion's slot on R lit while on). Voice: `tools/soundmaps/dva.txt` from
`research\dva\scripts\sounds\dva_voice_gen.py` (Sigma's voice pipeline: `voiceset_dva.py` (TankLib, offline) -> `dva_voice.py`),
her stimuli in `Voice`, `OcSounds.DVA_VOICE`; built into the shared pack (`overcraft_ana\local-assets\pack`) by the scratch-root
method of [sounds](../sound/sounds.md) (backups `sounds.json.before_dva`, `lang\en_us.json.before_dva`).

**Her own voice moments, from the data:** Boosters 03DA, Defense Matrix 03DB, its absorb line 06C1 0.75 s after (02CC #343), Call
Mech 0479, its kill 11CE (026A #854), the mech lost 0497, back in a mech 049D, the pilot's kill 10AF (02B8 #353, 0.9), Self-Destruct
04D1 to her side / 03DC to enemies (0B2F #443). 03DA / 03DB / 0479 / 0497 / 049D are named in no graph node read (the voice set
lists them; M the trigger). Her 41 voice-line unlocks are in the pack. **Not mapped (listed in voice_lines.json):** the match,
objective, PvE, CTF, Stadium and hero-interaction lines (the mod triggers none), 0EC6 (an environmental kill), 0FE5 ("Flying
Tokki!"), 1893 / 1894 (perk unlock lines: the mod's perk pick has no voice hook yet), Revenge, KillStreak, HealthPack.

**Played** (fresh `runClient`, worlds `dva10` / `dva11` flat; picked on the select screen; keys through the harness's key mappings,
not the desktop keyboard; screenshots in `run/screenshots/`, not committed, each looked at):
- Select: D.VA TANK INITIATOR, 350 + 325 (`p_select_dva.png`). Perk menu (LAlt): Shield System / Precision Fusion cards (`p_perkmenu.png`).
- **Shield System:** HUD 675, four blue pips over the yellow (`p_hud_shield.png`). `/damage` 50 then 150: **525** (100 shields, 50
  on the armor), "D.Va grunts" subtitle (`p_ss_hit150.png`); shields back from 3 s at 30/s (603 at 5.6 s; the global 6 s regen fills
  the rest, so the 100 cap itself wasn't isolated). Matrix (RMB) eating three arrows from 525: **625** (+100, the shields' cap);
  "dva.voice.matrix" heard.
- **Bunny Power:** mech killed: wreck, "D.Va: Pilot disengaging!", the pilot at **250 | 175** (75 overhealth, green pips,
  `p_bunny_eject.png`). Call Mech: "Calling in the big guns!"; a zombie about 3.5 m from the landing took **160** (falloff past 3 m;
  none at all past 3 m without the perk). Its exact distance wasn't pinned (the zombies were gone at the second read).
- **Extended Boosters:** a husk hit for **34.2** (35 less its 2 armor points; 25 without); a late in-line hit ran the flight to z
  22.9 / 21.1 against 17.2 with no hit (two runs each: +0.45 s); an early hit adds only up to the 2 s cap, as the data says.
- **Precision Fusion:** R lights its slot, then its 7.5 s countdown (`p_pf_active.png`, `p2_pf_cd_2.png`). **Bug found and fixed:**
  2 s on a giant at 10 m dealt 220 against 304 normal (two runs): the rate was x1.5, not x2 (`FusionCannons` clamped the recovery at
  0 and lost the half tick: 10 volleys a second). After the fix: **297 vs 304** (equal DPS at 10 m, as 2x rate x 0.5 damage gives).
- **Voice heard:** pain, matrix, eject, call_mech, boosters, the Self-Destruct line (`dva.ult.friendly`, her own copy).
- **Not shown:** the real desktop keyboard (harness keys only); Precision Fusion's R-again cancel and its spread cut by eye; falloff
  x1.5 at 15-30 m; Shield System's shields after G and back, and its armor reduction rows; the enemy's copy of the ult line (needs a
  second player); multikill, on_fire, call_mech_kill, pilot_kill, matrix_absorb (dropped while the matrix line held her voice: the
  one-line rule), mech_back (likewise after the call line), the wheel lines; a night fight with the perks.

## Step 11, her sound effects (2026-11-11)
The owner: "Sound effects now". Sigma's pipeline retargeted, in `E:\Overwatch Archiveesearch\dva\scripts\sounds\` (the order of
[sounds](../sound/sounds.md#sojourn-sounds)): `fx_graph_dva.py` -> `fx_sounds2_dva.py` (her kit graphs' effects, 450 read) ->
`clip_sounds_dva.py`, `fx_nodes.py` / `node_up.py` (which node plays each effect) -> `fx_timed.py` on her entities' slot effects and
`contacts.py` on her contact sets -> `ww.py` (128 02C) -> `dump_banks.py` (17 banks) -> `extract_banks.py` (30 banks her export lacked,
into `research\dva\sounds\ExtraSounds\D.Va`) -> `measure_dva.py` -> `stmg.py --offline` -> `overview.py` -> `dva_table.py` (the event
table, each event's node cited) -> `dva_gen.py --write` -> `check_map.py` (58 events, 538 files, 0 problems). The map:
`tools/soundmaps/dva_sfx.txt`; its pack build by the scratch-root method (backups `*.before_dva_sfx`). Code: `dva.DvaSounds` (the
server's one-shots), `client.DvaSounds` (the loops: matrix hum, Boosters roar, Precision Fusion hum, from `HeroManager.DVA_STATE`),
`OcSounds.DVA_SOUNDS`; every vanilla stand-in sound in her kit replaced.

**Read from the data (not by ear):** Fusion 050A #379 effect 34DA (5DC5 hers / DBAB others' 100 m: a held stream, first volley,
a trigger a volley, spin-down on its Stop); hits by the pellet's contact set 4B3. Matrix 02CC: 5F57 held (2AA8 / 2AA9: start,
4.07 s hum, Stop), 1637 MEKA chirp at the press, 1506 per eaten projectile. Boosters 047E: 1492 held (5DC3 / 2AED / 2AEF), 1635
chirp, 68E6 on an enemy hit. Missiles 1128: 1F99 cast, 4562 the volley, 1FA5 each burst, contact sets 783 / 772. Self-Destruct 0B2F:
1702 arming, 1703 blast. Call Mech 026A #417 1573. The wreck 0276 #86 1572 and #143 1633; the eject: the destroyed clip 31C3's 78BF.
Light Gun: its round's slot F03 (no own fire 02C: 02B8's 3398 holds none), 147B on a target, contact 49D; reload: the 1P clip 30BB's six
and the 3P clip 3234's 2E14. Perks 514D: 6DB5 / 6E7C / 6DB6 Precision Fusion, 6420 Shield System's restore, 6730 Bunny Power. The
Stadium powers' variants (34F5's twin aside, 7FFB / 7FFC, 5F5B-5F5D, 3C64, 674D, 641F) are left out.

**Played** (fresh `runClient`, world `dva21`, D.Va picked on the select screen; keys through the harness's key mappings: desktop
control was granted but the owner was at the PC and both real-key attempts were interrupted, so no real-mouse play this run; the
client's `heard` list and the subtitles; screenshots `s1_fusion.png` .. `s11_eject.png` in `run/screenshots/`, not committed):
- Fusion 1.6 s: `fusion.fire_first` once, `fusion.fire` x10, `fusion.fire_end` once; at a zombie `fusion.hit_enemy` x7, at the
  floor `fusion.hit_block`. Precision Fusion 1.2 s: `fire` x16 (13.3/s), `perk.precision.start` / `.loop` / `.end`.
- Matrix with two arrows: `matrix.on`, `.loop`, `.chirp`, `.absorb` x2, `.off`; with Shield System `perk.shield_system`.
- Boosters into zombies: `boosters.start`, `.loop`, `.chirp`, `.hit`, `.end`.
- Missiles at a husk: `missiles.cast`, `.volley`, `.burst` x18, `.hit_enemy` x18, `.volley_end`.
- Self-Destruct: `self_destruct.arm` (subtitle "Self-Destruct arms" with the ult line, `s7_selfdestruct.png`), `.blast`.
- As the pilot: `gun.fire` x7, `gun.hit` x7, `gun.reload`; Call Mech `call_mech.drop`.
- Mech killed: `wreck`, `wreck.chirp`, `eject`; with Bunny Power `perk.bunny_power`.
- **Not shown:** the real keyboard and mouse (above); every `_3p` copy and the others' loops (need a second player: they are built
  and wired, unheard); `missiles.hit_block` and `gun.hit_block` (all hit targets); listening quality by ear against Overwatch (the
  harness lists what played, not how it sounds); footsteps (no hero has its own: Minecraft's steps serve, as Sigma's map notes;
  hers are contact sets 48A / 4ED, read but not mapped); the ult-ready sound (no hero has one beyond the voice line).
- The firing and matrix clips left open in step 11 are animation, not this pass: still open. Her visual effects (the 111) are not
  started: the effect ids per ability are in `research\dva\workx_sounds.txt` (the same read), for the effects run.

## Not yet
Her **visual effects** (111) and the firing / matrix clips; steps 12-13: emotes, two players (and with them the `_3p` sounds). Open
from steps 4-6: Kinetic Grasp onto `Barriers.absorbBox`; the Toggle Defense Matrix and Hold To Use Boosters options; a guardian's beam
against the matrix. Her reticle widgets (#85, below).

## The merge of next (2026-11-10): what fix/systems asked of her, done
next brought `HeroKit.leavePlay`, `Combat.push`/`launch`, `Blow`, `Allies.areaHits`, `Barriers.nearest` and the Brigitte barrier `burst`/`shields(e, owner, at)`. Done in the merge: `Dva.leavePlay` (G: `heroMode(false)`; death: `stopMechKit`, a flying Self-Destruct still goes off; switch/leave: `clear`), the G hook kept for "on" only, `DVA_FORM` in `HERO_LOOKS`, Call Mech, Boosters and Self-Destruct push through `Combat.push`/`launch` (credit and transport), the dead "undo Minecraft's push" in the missiles gone, every hit with its `Blow` (`call_mech`, `boosters`, `micro_missiles`, `self_destruct`, weapon), her splashes on Brigitte's barrier like Accretion's, Boosters' knockback taken in `Combat.knockbackTaken`. Still open from that list: her reticle widgets (#85, below).


Written from `fix/systems` for whoever merges `feature/dva` (worktree `overcraft_dva`, branched before these commits, not edited from here). Her branch has its own
`techniques/heroes/dva.md`, her hub: on merge keep both, and put this list under it until it's done. From the critique's M8
([critique_systems](../testing/critique_systems.md)), read against `overcraft_dva` on 2026-11-04. Up: [Techniques](../README.md) · the rules:
[minecraft_26](../minecraft/minecraft_26.md#hero-hits-dont-push-93-2026-11-03) · [hand_play](../testing/hand_play.md#leaving-play-one-hook-four-doors-100-2026-11-04)

### The list
- **`leavePlay` on her kit** (`HeroKit.leavePlay`, `HeroManager.outOfPlay`): today's body of `Dva.heroMode(player, s, false)` (`stopMechKit`, the mech's health stored, Call Mech cut), and
  `Dva.clear` when `why.forGood()`. On death: the pilot's state, and a flying Self-Destruct (`dvaSelfDestructId`). The rule, ours and open for the owner: G ends whatever is running and charge
  already spent stays spent, so a Call Mech or Self-Destruct under way ends on G.
- **`HeroManager` conflicts:** her G hook in `setHeroMode` (lines 207-209 on her branch) and her two `if (state.hero == Hero.DVA)` branches (291, 403) go: `select` and `onLeave` call `outOfPlay` only.
  The G hook for "on" (getting back in her mech) has no `leavePlay` to go to: keep it, for `on` alone.
- **`DVA_FORM` goes in `HeroManager.HERO_LOOKS`,** so a switch or a leave takes it off.
- **Every push goes through `Combat.push` (or `launch`):** it applies knockback resistance and every state's share (`Combat.pushShare`), none while phased, sends it to a player's own client
  (`Combat.send`) and records the kill credit (`Combat.pushed`). Don't hand-roll `Tank.knockbackFactor`, `setDeltaMovement` and `needsSync`:
  - `Dva.java:426-427` (Call Mech's landing): `needsSync` only; it relied on `markHurt`, which hero hits no longer do, so a player landed on wouldn't move.
  - `Boosters.java:124`, `SelfDestruct.java:116`: the same transport question; and no `Combat.pushed` anywhere in `dva/`, while Boosters, Self-Destruct and Call Mech are all on the wiki's knockback
    list. Without it, a fall or the void after her push isn't her kill (#84).
- **`MicroMissiles.java:140, 167` "undo Minecraft's push" is dead:** hero hits take no Minecraft knockback now. Remove it.
- **The `Blow` argument:** next's `Combat.damage(target, source, owDamage, blow)` takes what dealt it (kill feed); her calls (`FusionCannons:169`, `Dva:422`, `Boosters:115`, `SelfDestruct:104`,
  `MicroMissiles:138, 166`) don't pass one yet.

### What a critic should check
A mech pushed off a ledge and an ejected pilot through G, death, a switch and a leave, each one played; her body box after each (`HeroBody`). Whether a held AI (`Sleep.aiHeld`) on her `WRECK` makes sense: she's a player, so it doesn't apply.

### What she needs from the reticle widgets (#85, fix/reticle-ui)
Written from `fix/reticle-ui`, not built here: her widgets are on her own branch. The system is `client/hud/ReticleHud` ([reticle](../hud/reticle.md)); add a `case DVA` to its `render` and `crosshair`, and her values to `Dva.widgets` (`HeroKit.widgets`). From the audit, D in `research\dva\work_kit`:
- **Defense Matrix under the crosshair:** 04F0 with resource 02CF (`decomp_02CC.txt` 557, 888); today a weapon panel. `resourceBar` with its share.
- **Boosters:** 0237 and 00DB, "[Shift] cancel" (`decomp_047E.txt` 720, 733): `timedBar` from `activeLeft` (`Dva.java:486`, synced, unread) and `prompt`.
- **Self-Destruct warning:** 0031 at the mech + 2 m for anyone within 21 m, texts 947F / AAD9, texture 4EA7 (`decomp_0B2F.txt` 476-553): a world widget, for every player near, not only hers: not in `ReticleHud`; project it as `targetMarker` does.
- **Crosshairs:** separate in the mech and as the pilot: Fusion CF, Light Gun E0; the Light Gun's ammo widget 02AB (`decomp_050A.txt` 382, `decomp_02B8.txt` 516 / 533). `crosshair` falls to the "+" until she has a case.
- **As the pilot:** the mech's saved Self-Destruct charge (a second 01EE, `decomp_026A.txt` 1264) and the Call Mech 1 s lock.
