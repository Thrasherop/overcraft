# D.Va (hub)

Everything learned about D.Va, by subject. Hero 8 of the owner's 2026-10-28 list (issue #69), after Sigma: a **Tank, Initiator** (BZ
2026-02-10), **the second Tank** and **the first hero with two bodies**. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · researched the way Sigma's was (commit 1fb1dc2, branch feature/sigma:
[sigma](sigma.md), whose Tank role code she builds on) and Sojourn's ([sojourn](sojourn.md))

## Her notes and sources
- **The owner's Workshop recording (2026-11-11):** [dva_workshop_2026-11-11](dva_workshop_2026-11-11.md): 150 s of her measured
  against ours; two disagreements (Boosters' push direction, the eject dropping the mech's momentum) and the generic stop floor.
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
The owner: "Sound effects now". Sigma's pipeline retargeted, in `E:\Overwatch Archive
esearch\dva\scripts\sounds\` (the order of
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

## Step 12: her emotes from the data (2026-11-11)
All 15 from the data, on the MEKA ([dva_emotes](../emotes/dva_emotes.md)): 57 clips in `hero_models/dva_clips.glb` (FBB0's rig,
`tools/clipmaps/dva_clips.txt`), `emotes/dva.json` (`tools/emote_data.py dva`), six props (`prop_38ef`, `prop_38f0`, `prop_38f4`,
`prop_399f`, `prop_2cde`, `prop_162ca`) in the dev pack (`overcraft_ana\local-assets\pack`, not committed). **Which body:** every body
clip drives the MEKA's skeleton (87 bones the mech's alone), none the pilot's: the pilot can't emote (`Comms.emote`). Tool fixes: her
name, a prop's clips from its own model folder, a ragdoll entity falls. Found playing (fixed): the emote camera now frames by height too.

**Played** (fresh `runClient`, flat creative worlds `dvaemo`, `dvaemo2`; D.Va clicked on the select screen, then READY by a second
click; noon; keys through the harness's key mappings, not the desktop: **real mouse and keys still owed**). Each emote equipped on the
wheel's top slot (`equip`, dev: what the loadout screen does), then the wheel the normal way: comms key held, right button to the
emote page (`x_wheel_mic_drop.png`: MIC DROP at the top), the pointer up, the key let go. Burst screenshots at 0.6 / 1.5 / 3.0 / 5.0 /
7.5 s (`w_<emote>_<t>.png`, sheets `ws_0..4.png`), each ended by W:
- All 15 started from the wheel, the emote camera on, in third person (0 first-person frames), each ended by W. Chains in
  [dva_emotes](../emotes/dva_emotes.md#contacts-tools-emote_bonespy-emote_contactspy-on-dva_clipsglb).
- Held frames (`emoteat`): Mic Drop 0.6 (the mic on the raised left arm, from above: `x_mic_0.6_up.png`), 1.6 (on the ground);
  Heartbreaker 2.0 front and side (the heart in front of her); Game On 4.0 front and side (seated, the screen and sprites); Make it Rain
  (a bill in flight at 1.5 s).
- Camera: Bow's antennae left the top at 2.69 blocks (`w_bow_0.6.png`); in at 2.92 (`v_bow_0.6.png`).
- **The pilot:** survival, `/damage` 40 x 3: wreck, eject, the pilot 14 / 14 (0.6 x 1.8): Bow from the wheel: no emote, first person
  (`y_pilot_bow.png`). Not shown: whether the old build (no gate) put the pilot's camera in third person.
- **Then (same day):** her emote sounds and voice lines built and heard, the shared Listen Up / Mic Drop / Salty ones too; the
  canopy drawn as glass from its shader, so she's seen inside (world `dvaemo3`, `gs.png`):
  [dva_emotes](../emotes/dva_emotes.md#sounds-and-voice-lines-2026-11-11).
- **Not shown:** a critique agent, a second player (the "others" sound copies), emotes in water / on slopes, hologram colours
  ([dva_emotes](../emotes/dva_emotes.md#open)).

## Step 13: two players (2026-11-11)
Two dev clients under one lock: `runClient` "Kiriko" (A, D.Va, host of flat survival `dva13`, `/publish false 25565`) and
`runClient2` "Viewer" (B, Kiriko, `connect 127.0.0.1:25565`), each `-Xmx3G` (`JAVA_TOOL_OPTIONS`), `OC_ASSET_PACK` the Ana checkout's
pack, 1280x720, subtitles on, F3+B hit boxes on B. Both picked on the select screen by clicks (`screenclick`). **Input is the harness's
(`KeyMapping.set`, dev `emote`, `/damage`, `/kill`, `/kick` for B's leave), not a real mouse and keys: those are still owed before the
merge.** Both clients driven from one script (scratchpad `d13\two.py`, row files `r1..r24.txt`, not committed); trees: d062dd3 for
r1-r21, then A rebuilt with the fixes below (B's client stayed on d062dd3 until r25: the fixes are server code, which runs on A); r25
both on the fixed tree. Screenshots in `run2/screenshots/b*.png` (B's view) and `run/screenshots/a*.png`, not committed; each opened.

| Row | Result |
| --- | --- |
| Her forms as B sees them | Mech, box 1.4 x 2.3 (`b1_mech_front/side`); the eject in the air over the fallen wreck, then the pilot 0.6 x 1.8, nothing left where the mech was (`sheet_b6`); Call Mech: the empty landing box at 0.4 s, the mech landing at 1.0 s, standing at 3 s (`sheet_b5`); Self-Destruct's mech arming (`b4_callmech_drop`, `b15_sd_arming`) and blast. 1 run each. |
| "Heard by others" copies (B) | `matrix.on/chirp/loop/off/absorb_3p`, `fusion.fire_first/fire/fire_end/hit_enemy_3p`, `boosters.start/loop_3p` (+ `chirp`; `end` is hers only), `missiles.cast/volley/volley_end/hit_enemy_3p`, `perk.precision.start/end_3p` (its hum is hers only, by design), `call_mech.drop_3p`, `wreck_3p`, `wreck.chirp_3p`, `eject`, `self_destruct.arm/blast`, `ult.enemy` (enemy) / `ult.friendly` (A), her voice lines (matrix, boosters, call mech, eject, pain, death, elimination, respawn). Listen Up: **E54A / E54B on B, E548 / E549 on A, 2 of 2** (r25, `b25_listen_up`). |
| Emotes, held loops | Mic Drop, Game On, Dance, Listen Up seen and heard on B (`b7_micdrop_0.8`, `dva.emote.*`); Matrix (stand-in sparks), Boosters (`b3_boosters_ally`), Precision Fusion (`b10_precision`, subtitle "Precision Fusion on"). Mic Drop's DA4F (chance 0.4) played on A and not on B: each client rolls its own chance (not chased). |
| Matrix vs Kiriko's kunai | Enemy: every kunai eaten, A 54 -> 54, `absorb_3p` on B, 3 runs (r11, r23 x2). Control without Matrix: 54 -> 35.92. Ally: the kunai passed through her and the matrix (A 54 -> 54, no absorb; they killed a slime behind her). |
| Her hits on B | Enemy: Fusion 0.5 s 20 -> 18.88; Missiles (aimed at the body) 20 -> 9.2, 18 `hit_enemy_3p`; Boosters 20 -> 18 (25) and thrown 6.7 m, up then down, on B's own client; Self-Destruct in the open 20 -> dead (`ult.enemy`, kill feed); behind a stone wall 20 -> 20. Ally: Fusion 20 -> 20 (rounds hit the floor past B), Missiles 20 -> 20 (`hit_block` x18), Boosters flew through B (20 -> 20, unmoved), Self-Destruct 250 / 250 on B's HUD. 1 run each. |
| The ult -40 % | Kiriko's two kunai into D.Va: 226 dealt, her ult +135 after her passive (~5/s, read before): **0.60 a point**; run 2 the same +135 (201 to health, the rest to Shield System's shields). Into a Kiriko bot on D.Va's team: 240 dealt, +241: **1.00** (1 run). x0.60 (`Tank.ultFactor`). D.Va's own charge: up for two eaten kunai (Matrix's 4 % with her passive), full (1700) after hitting B; Self-Destruct's charge carried to the next mech, Call Mech full after a Self-Destruct (as in step 8). |
| Joining late, leaving | B kicked (the server's leave) and back: during Dance it saw the emote and heard its sounds (`b8_late_dance`); a Game On join heard the same remaining events A did, no burst of the missed ones; as the pilot it saw the pilot and her box (`b17_rejoin_pilot`). **Held loops on a late join not shown:** Matrix, Boosters and Precision Fusion end within 3.1 s, a rejoin takes about 8. |
| G, death, switch while B watches | G: the pilot, no mech left (`sheet_b18`); G back: the mech. The mech destroyed then the pilot killed: no wreck or mech left, kill feed (`b20_dead`). Switch to Kiriko mid-Matrix: `matrix.off_3p`, nothing heard in the next 9 s, no mech entity (`b21_switched`). |

**Found and fixed** (each reproduced on d062dd3 first, then replayed on the fixed tree):
1. **Matrix held into G kept humming on every client** while she was out of hero mode (`matrix.loop` / `loop_3p` restarted twice in 9 s,
   2 of 2: r18, r19): `Dva.tick` stops writing `DVA_STATE` out of hero mode, so its Matrix bit stayed. `Dva.heroMode(false)` now clears
   it. After: nothing in 9 s, 3 of 3 (r22 x2, r24).
2. **The Matrix's stand-ins blinded the shooter:** the box reaches 10.25 m, so a kunai eaten at Kiriko's hand put its smoke over her whole
   screen, and the sparks drew at her face (`b11_enemy_matrix`, `b2_fusion_ally`). Neither particle now spawns within 2.5 m of another
   player's eyes (`DefenseMatrix.nearOthersEyes`). After: B's view clear, both kunai still eaten, A 54 -> 54, 2 runs (`sheet_b23`).
3. **G gave her the mech's health in Minecraft mode** (54 / 54 with the pilot's box, 2 of 2: r18, r22; step 3 had read 14 / 14): since
   the next merge her `leavePlay` runs while the hero-mode flag is still on, so the refresh measured the mech. `HeroManager.setHeroMode`
   refreshes her health once the flag is off. After: 14 / 14, 2 of 2 (r24); G back: the mech (53.9 a second after 46.6 was stored:
   peaceful regenerates about 1 a second, seen without G too; not measured on normal).

**Listed, not fixed (not hers):** every hero hit plays the target's pain line (`PlayerMixin` swaps Minecraft's hurt sound): Micro
Missiles on Kiriko gave 36 `kiriko.voice.pain` in about 1 s. Wants a rate limit for all heroes.

**Not tried:** a real mouse and keys (owed before the merge); a dedicated server and real latency; B's view of Fusion's slow and of
her Light Gun against B; Defense Matrix against a beam; the late join during a held loop (above); B leaving by its own pause menu
(kicked instead); the Nether, water, a night; an Overwatch side by side; a critic on the fixes. Void: r3's Boosters (A teleported
0.5 s before the press; stopped after 2.8 m); r3b / r3c with a wait flew the full 24.4 m, through B.

### The critic's round on d062dd3 (2026-11-11)
A fresh critic read the branch (no game). Fixes 1 and 3 above covered two of its findings; the rest, one client (`runClient`, flat
survival `dva14`, peaceful, harness input), each reproduced on deb0aed (rows `f*.txt`), fixed, replayed twice on the fixed tree (`g*.txt`;
scratchpad `d13`). `state` now also reports `ultCost` and `shield` (dev only). Health in Minecraft units (12.5 Overwatch each).

| # | Finding | Before (deb0aed) | Fix | After, 2 runs each |
| --- | --- | --- | --- | --- |
| 1 | G back threw away damage taken in Minecraft mode | pilot 14 -> 5 in Minecraft mode, G back: the mech 54 / 54 | **Decided (ours):** the plan's default 9 ("the mech at the health it had") less what she lost while out of it, at least 1: damage taken as the pilot in Minecraft mode is hers, so a near-dead player never comes back whole (`Dva.heroMode`) | mech 40.56, G off, pilot 14 -> 5, G back: 32.56 (40.56 - 9, +1 peaceful regeneration) both runs |
| 2 | G in the wreck skipped the eject | wreck (0.01) at 909 ult, G: a pilot at 0.01 (4.01 after regen), ult 909 against Call Mech's 280: Q called a mech at once | G in the wreck is the eject (`eject(player, s, true)`): full 175, Self-Destruct's charge kept, Call Mech from 0, immunity and the call lock | G: ejected back 2.3 m, 14 / 14, ult 6 of 280, Q did nothing |
| 3 | Dying or leaving as the pilot lost the mech's charge | mech 1498, eject, pilot 200 / 280, killed, respawned: 210 / 1700 | `Dva.mechUlt`: `onRespawn` and `onLeave` keep the mech's charge for D.Va (026A #775); she respawns and rejoins in a mech | the same steps: respawned at 1512 / 1700 |
| 4 | `heromode on` while on healed the mech | 40.56 / shield 157, `heromode on` x2: 54 / 325 | `setHeroMode`'s D.Va hooks only on a change | 40.56 -> 33.56 (G steps before), shield 157 kept |
| 5 | Respawning within 3 s cancelled a flying Self-Destruct | cast, the pilot killed, respawn 0.3 s later: no blast in 4 s | `equip` removes the unmanned mech only; a Self-Destruct in flight goes on (a switch away still takes it: `clear`) | killed 0.4 s in, respawned 0.8 s later: `self_destruct.blast` heard |
| 6 | The blast searched her level, not the mech's | **not reproduced:** three tries; once she left for the Nether the overworld's chunk stopped ticking and the mech never went off (needs a player near the mech); two were void (my teleports mid-eject; she fell) | `SelfDestruct.blast` uses `mech.level()` (by reading) | not shown |
| 7 | Call Mech cast mid-air | 0.96 m up mid-jump: cast | W rev 408749 "Has to be grounded to activate" (BZ silent): no cast unless on the ground, the charge kept | mid-jump (0.95 / 0.98 m up): ult stays 280; on the ground 2.5 s later: cast |
| 8 | Self-Destruct's minimum 50 | (a number) | BZ names no minimum (2024-02-13: max 900 -> 1000, inner 4 -> 6 m); W rev 408749 "1000 - 100": **100**, the data's 50 noted (`DvaStats`, `tools/reference/dva.md`); `DvaMathTest` 640 / 280 / 100 | unit test |
| 9a | G back clamped her layer to 325 (Shield System's 100 lost) | by reading (`Math.min(..., MECH_ARMOR)`): not run on the old tree | the layer as stored, up to `layer(player)`, never above her health | Shield System (perk menu), G off/on twice: shield 425 -> 425 -> 425 |
| 9b | Self-Destruct while boosting fell at once | boosting 10 deg up, cast: the mech dropped to the floor and stopped 15.7 m on | W rev 408749: Boosters "remain active for their remaining duration": the mech flies on at its speed without gravity for the flight's ticks left (`DvaMechEntity.boostTicks`) | y -58.4 -> -56.0 and z 13.4 -> 28.1 over 1.2 s, along the aim, both runs |

**Known, left:** `/kill` doesn't kill her through the wreck (it destroys the mech; a second `/kill` kills the pilot).
**Not tried:** the old tree for 9a; 6 in game; a second player for any of these; real keys; normal difficulty (peaceful's regeneration
is in 1's number); Extended Boosters' longer flight with 9b.

### The real-input pass's four findings (2026-11-11)
From the coordinator's real-input pass on 09a6f4c ([real_keys_09a6f4c](../testing/runs/dva/real_keys_09a6f4c.md)). One client
(`runClient`, flat survival `dva15`, easy, noon), harness input, on 5e8df3d, then the fix; rows `h*.txt` in scratchpad `d13`. Health in
Minecraft units (12.5 Overwatch each).

| # | Finding | Reproduced? | Cause / fix | After |
| --- | --- | --- | --- | --- |
| 1 | Micro Missiles mostly missed a zombie at 12 m | **Yes, worse:** a 1000-health NoAI husk with the crosshair on its middle, at 5, 12 and 20 m: 0 of 18 hit at each, `hit_block` x18, 1000 -> 1000 (no splash either) | The data has no spread, arc or homing (D 1128 #357: a straight 50 m/s projectile 1E8A, no spread field; W none). The aim was the bug: each missile flew from its shoulder muzzle (0.88 m to the side) to the **block** behind the crosshair, ignoring the creature on it, so both muzzles passed it by and burst on the ground beyond. Now the aim point is the first creature on the crosshair, else the block (`MicroMissiles.shoot`). | 18 of 18 `hit_enemy` at 5, 12 and 20 m, the husk 1000 -> 989.98 (125 of 135: a husk's armour and the splash's edge), 2 runs |
| 2 | G back gave 658 / 675 with nothing lost | **No:** full mech, G out (14 / 14), G back at once: 54 / 54, twice; G out, walk and jump 3 s, G back: 54 / 54 and the HUD 675 / 675 (`h2_gback.png`) | Not found. The log shows 54.0 before G and 14.0 after; the 658 was read on the HUD, not in `state` | none |
| 3 | The pilot lost 25 about 1 s after the eject's landing | **No:** `/damage 80` into the mech, a NoAI zombie 2 m beside, in front and behind (burning at noon), and with none: wreck, eject, landing, 14 / 14 for 2.5 s after, 4 runs. The pass's trace shows a rise to 1.27 m (above her 0.98 m jump) with the loss: a hit with knockback, source not read | Not found; not ours as far as shown | none |
| 4 | Boosters took 2.98 from a zombie | Read: 25 Overwatch is 2.00; a zombie's and a husk's natural armour (2) takes 4 %: **1.92**, 2 of 2 on a husk, on a zombie with Fire Resistance and on a burning one. In the sun a zombie burns 1 a second, so a read across the bump adds that: 997.02 = 1000 - 1.92 - about 1 of fire. No perk was on (fresh world, none picked; `equip` sets emotes only) | Right as it is: mob armour applies to hero damage (ours, as for every hero) | - |

**What's drawn against where they go (the coordinator's follow-up: "they still look high and wide to the right").** One client,
2527ebf plus a dev readout `ents <class>` (this client's copies: position, motion), E at a husk 12 m out, read 0.25 / 0.35 / 0.45 s
after the press against the server's `/data get ... Pos` (row `m1.txt`): **the client's missiles sit where the server's are**
(9.20 -58.66 12.29 against 9.197 -58.658 12.286; 10.00 / 9.995; 8.81 / 8.812), same motion; the renderer draws the entity's own
position (`ProjectileModelRenderer`), and there is no client-side aim of its own. Screenshots `m1_000/015/030.png` (`sheet_m1.png`):
at 0 s none in view, at 0.15 and 0.3 s small, either side of the husk and closing on it. So what's drawn is where they fly: from
the shoulder pods (hp 0075 / 0077: 0.88 m to either side, 0.24 m above her eyes) in to the target, which in the first metres reads as
**up and to one side** of the crosshair (2.5 m out a right-pod missile is about 16 deg right and 4 deg up). A burst caught on
right-pod missiles shows them up and right. **No fix:** this is the data's launch geometry. Not tried: the pass's own fps (117 against
about 30 here) and FOV, and an Overwatch side by side of the launch.

**Not tried:** the pass's exact keys for 2 and 3 (its client and world); real keys for the missile fix; missiles at a moving target, at an
ally on the crosshair (they aim at it and pass through, as allies don't stop them: not played); a crosshair on a wall nearer than 1.5 m.

## Self-Destruct's animation (#132) and the first-person Call Mech (#133), 2026-11-13
Branch `fix/dva-anims` (from `next` 363f198, 0.3.41). The owner, playing 0.3.41: "the dva bomb animation isn't correct" and "the remech
happens in front of the pilot ... there's two pilots. In Overwatch, you are first person as baby dva and climb into the machine, then a
brief black fade into mech UI." Every clip below was already in the pack (no Blender run); found with the clip maps and
`research\dva\work\clipmap_dva\compact_<set>.txt`, timed by the graphs' StateAnim nodes.

**From the data:**
- **Self-Destruct** is alias **0x175** (0B2F #300: m_86701C3E 3.5, state length v215 = the 3 s fuse). On the 3P mech (set 0282) it is
  **3644**, an additive 5.03 s clip (`add_self_destruct_sm0`): its panels and arms open by stages, the last at 4.0 s; played over **3.5 s**
  (`DvaStats.SELF_DESTRUCT_CLIP_TIME`, M: that m_86701C3E is the play length; the same field is 2 / 1.66 / 0.2 on the call's clips) over
  her stand, so it balls up just before the blast. The 1P mech has its own 0x175 (33CF, 0.6 s), unplayed: she is already the pilot
  then. The pilot's eject 0x439 (3382) plays on any 016C, and Self-Destruct sends 016C at its cast (0B2F #271): it now plays on that eject
  too, not only after the wreck (`HeroAnimator`, MECH -> PILOT).
- **Call Mech, hers in first person:** the cast 0x433 (pilot 1P **3531**, 0.2 s: 026A #347), then 0xE5 (pilot 1P **3134**, 2 s from the
  spawn: #405). 3134's `Camera` bone is laid out about the mech's spot: it starts 2.06 m behind it at 1.43 m, backs off, then climbs 2.9 m
  forward into it, looking down up to 29 deg (`camtrack.py` in the session scratchpad). The link is spawn + 1.75 s (#431). Then the
  mech's own 1P spawn **0x435** (mech 1P **3329**, its 1 s over 1.2 s: 0276 #176), whose camera starts at 1.37 m pitched down 21 deg
  (where the climb left off) and rises to the mech's eye.
- **No fade in the data:** no fade or screen node in 026A or 0276 (0276 #175 STU_A90C5991, right before #176, is unread: L). The fade is
  ours, from the clip's camera track: black from 1.4 s into the climb (the camera reaching the cockpit) to the link, out over 0.35 s
  (`DvaStats.CALL_FADE_START` / `CALL_FADE_OUT`).

**Code:** `DvaMechRenderer` (the additive layer on the SELF_DESTRUCT mech), `HeroAnimator.dvaView` (the 1P clips by form and time),
`client.DvaCallView` (the view: the clip camera anchored on her called mech's spot and yaw, blended in from her eyes over 0.3 s; the
spawn view anchored on her feet, blended back to her own look over its last 0.4 s; the fade), `CameraMixin.overcraft$dvaCall`, the black
in `HeroHud.render`. First person only; her third person and everyone else's view of her are unchanged.

**Played** (fresh `runClient`, flat survival `dvaanim1` on 363f198, `dvaanim2` on the fix; `spawn_monsters` off, noon; harness keys,
not the desktop; a D.Va bot (`bot add Dee dva`) for the side views; screenshots in `run/screenshots/`, not committed, each opened):

| Moment | Expected | Before (363f198) | After |
| --- | --- | --- | --- |
| Call Mech, 1P (`/damage 60`, eject, `ult 100`, Q) | the view climbs into the mech, black, mech view and HUD | the pilot's arms idle with the Light Gun; the mech lands 1.5 m ahead with a pilot already in its cockpit; at 1.95 s a cut to the cockpit (`sheet_r133_1p.png`) | gun away, her hands reach out (0.4 s), the mech lands ahead (0.7-1.0 s), she climbs onto its seat, hands on the cockpit (1.3-1.7 s), black (1.9 s), the mech's view pitched down rising into the cockpit with the 675 HUD (2.1-2.6 s), the cannons up (3.2 s) (`sheet_a133.png`) |
| Call Mech seen by others (bot, from the side) | 3120 run and dive, 311F | as expected | unchanged: she runs, dives into the falling mech, it stands (`sheet_a133_3p.png`) |
| Self-Destruct from the side (bot) | the mech's own animation to the blast | the mech in its plain stand for 3 s, stand-in sparks (`sheet_r132b.png`) | it crouches, opens, curls into a ball by 2.8 s (`sheet_a132.png`) |
| G at 1.2 s into the call | back to her own view, no black | - | Minecraft mode at her eyes, no black; G back: the pilot, Call Mech at 5 (spent, as before) (`sheet_e_g.png`) |
| Death at 1.2 s into the call (`/kill`) | normal death | - | killed; she is back in a mech at once (54); its spawn view and the fade-out play then (the animator sees CALLING -> MECH; the data's respawn also starts the mech's spawn, 0276: L) (`e_death2.png`) |
| Call Mech after that respawn | the same climb | - | the climb at 1.3 s, black at 2.05, the mech at 3.1 (`e_resp_call*.png`) |
| Hero switch (Kiriko) at 1.6 s into the call | Kiriko, her own view | - | Kiriko 250, normal view, no black (`sheet_e_switch3.png`) |
| An eject after the mech dies (`/damage 60`) | the `eject` clip | `eject` | `eject` (state at 1.1 / 1.25 s) |

**Not tried / open:** real mouse and keys (harness keys only); an Overwatch side by side of the climb and of the bomb; the mech model's
own pilot inside the cockpit during 311F (FBB0 carries her body; in first person the climb now covers it; in third person whether the diving
pilot and a seated one overlap was not checked: open); the 1P mech's 0x175 (33CF); Self-Destruct's visual effects (the sparks
are still the stand-in, step 11's effects run); whether 0276 #175 is the engine's own fade; a call on a slope or a ledge (the clip's camera
assumes the spot's ground is level with hers); two clients (the bot stood in for the other player).

### The critic's round on 83b6bfa (2026-11-13)
A critic reproduced five things with harness keys; each reproduced here on 83b6bfa (world `dvaanim3`), fixed, then replayed on the fixed
tree (`dvaanim4`), one client, harness keys:

| # | Finding | Before (83b6bfa) | Fix | After |
| --- | --- | --- | --- | --- |
| 1 | The Light Gun hung mid-view during the 1P call | gun and keychain at the centre at 1.1 / 1.5 s (`b_gun_11.png`, `b_gun_15.png`), and in the End after a level change (`b_dim_12.png`) | `HeroWeapon.place`: no gun in her 1P view while CALLING. Data: the gun is the weapon graph's cosmetic entity (02B8 #73, hp 000A); no node in 026A was found hiding it during the call, so this follows the clips (3531 / 3134 key no gun bone): ours | no gun at 1.1 / 1.5 s (`c_gun_11.png`, `c_gun_15.png`) nor in the End at 1.2 s (`c_dim_12.png`); back in her hand after the cancel (`c_dim_32.png`) |
| 2a | The climb's camera went into a wall at her back | with her back 0.05 m from stone, the anchored camera's start (2.06 m behind the spot) was inside the wall by position; the forward view looked normal (`b_wall2_05.png`): not seen on screen | `DvaCallView.inOpenAir`: the view is pulled in along the ray from her eyes, eight rays round a 0.2 m cube, 0.1 m off the face (Minecraft's own third-person rule) | forward view unchanged (`c_wall_05.png`); the camera kept on her side of the wall by construction (not measured on screen) |
| 2b | Off a 2-block ledge the view sank 2 m into the block | at 0.5 s inside the ledge, stone faces round the view (`b_ledge_05.png`) | a spot more than a step (0.6 m) above or below her keeps her height (ours), plus 2a | at 0.5 / 1.0 / 1.5 s her own height, looking down onto the mech landing below (`c_ledge_05.png`, `c_ledge_10.png`, `c_ledge_15.png`) |
| 2c | A slope | - | - | a half-slab spot 0.5 m up: the climb onto the slab, linked at y -59.5 (`c_slope_10.png`, `c_slope_15.png`) |
| 3 | A level change mid-call dropped her into the void | `/execute in minecraft:the_end run tp @s 100 50 0` at 0.7 s: linked in the End at the old spot, y -114, 54 -> 40 (`b_dim_32.png`) | `Dva.tickCall`: a call whose level (`HeroState.dvaCallLevel`) isn't hers is cut short as G does: the mech removed from the old level, she's the pilot, the charge spent | the pilot, 14 / 14, standing at y 49 on the End island, Call Mech charging from 0 (`c_dim_32.png`) |
| 4 | 0276 #175 STU_A90C5991 | - | **Not decided; our fade kept.** Its three uses: here (1 / 1 / 1, on the mech's spawn, before #176), Junkrat's RIP-Tire 0233 #343 (m_5C22817C 1 only, on the tire entity) and Steel Trap 09BB #577 (m_2B82763B 0.25, m_5C22817C 0, after an 8 s wait on the trap). A trap has no camera of its own, so a screen fade doesn't fit all three; an entity's own appear / fade (a dissolve over m_2B82763B s) fits them better (L). | - |
| 5 | Small guards | - | `DvaCallView.camera`: a pose time in the future (a rejoin into a world whose clock is behind) is stale. **F1 not done:** the black is drawn in the HUD, so a hidden GUI hides it (open) | by reading |

### The built-in pilot on the unmanned mech (2026-11-13)
The coordinator's real-key pass on 0764843: the climb, gun and HUD right, but the dropping mech's open cockpit already held a pilot
(the owner's "two pilots"). **The MEKA model FBB0 carries her body:** pieces skinned to the subtree under `bone_0887` (Neck / Head, both
arms and hands, hair), the same pieces drawn with the pilot model F2FF's materials (98C0, 98D0, 98C1, 8226, 8210, 82FB, 98C3, 9FF8) plus her
9FF7 gloves (submesh 5); the mech's own 9FF7 parts hang on mech bones. **Overwatch's rule for hiding them wasn't found:** the pet 0407's
model 11CE / look 14B0 hold her, no visibility node was read in 026A / 0276 / 0B2F, and 311F scales none of her bones (only bone_05F0).
So, ours by the skeleton: `DvaMechRenderer` leaves out the pieces mostly weighted to that subtree (`HeroModelRenderer.submitModel`'s new
piece filter) on every unmanned mech (the call's drop, Self-Destruct, the wreck). The player's own mech, the select screen and the POTG
card don't use that renderer: unchanged.

Played (one client, harness keys; before on 0764843 `dvaanim5`, after `dvaanim6` / `dvaanim7`): the wreck 1.2 s after a bot's mech died:
before a pilot figure in the cockpit (`crop_d2.png`), after, with the ejected bot moved 25 m off and the camera 2 m from it, an empty seat
and controls (`f_wreck_close.png`); the call's drop and Self-Destruct from a bot (`sheet_f2.png`), nothing of her on them; her own linked
mech from the front still shows her in it, and her hands in first person (`sheet_f_keep.png`). **Not shown:** the 1P drop at 0.15-0.25 s
(the mech was above the view's top looking up at 55 deg, `sheet_f_up.png`); the select screen's MEKA (the harness click closed it) and
the POTG card (not reached): unchanged by code only.

**Still open:** the F1 black (above); the wall row was not seen on screen either way; whether 0276 #175 is an entity fade; real mouse
and keys.

## Not yet
Her **visual effects** (111) and the firing / matrix clips; the hologram colours (step 12's open list); real mouse-and-keys play of step 13. Open
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

## Calibrated to the Workshop recording (2026-11-12, branch feature/calibration-1111)
The owner's call (2026-11-12): "our game should match overwatch". Applied from [dva_workshop_2026-11-11](dva_workshop_2026-11-11.md).
Played with the harness (`dev.py` keys and state, not a person's hands), one dev client at a time, fresh flat survival worlds, the old
build (`next` 89289d2) first, then this one. Driver and raw output: the session scratchpad `calib/` (old_* / new_*).

| Change | Recording | Old build (seen) | This build (seen) |
|---|---|---|---|
| Boosters' push: the flat direction from her to the target (`Boosters.away`) | 17.0 level + 4 up, along her-to-the-bot | NoAI zombie 1.4 m to the side, 5.5 m ahead, flown straight past: server Motion (0, 4.0, 17.0) m/s, along her look | the same flight: (7.18, 4.0, 15.41), 17.0 level, 25 deg toward the zombie's side |
| Eject adds 6 back and 7 up to her motion (`Dva.eject`, Combat.motion) | the mech's velocity kept + 6.0 back + 7.00 up | Boosters at 12 m/s, Self-Destruct at 0.6 s: 5.97 m/s back, 6.16 up (the 12 dropped) | the same: +5.96 m/s forward (12 - 6), 6.12 up (7 less the tick's gravity) |
| `MISSILES_CAST` 0.25 confirmed (comment only) | first missile 0.25 s after the press | - | - |
| Every hero's stop and jump (#130): see [pharah](pharah.md) | stop 0.067 s, 0.14 m; top 0.98 m, 0.67 s | mech: stop 3.00 then 0.25 m/s ticks, 0.1625 m; top 0.979 m | stop 2.75 then 0, 0.1375 m; top 0.979 m, first air tick 4.99 |

**The ~2-health drop after the eject (real-input pass 09a6f4c): not reproduced, and not ours as far as can be seen.** Tried on the old
build: `/damage 80` into a called mech with no mob near (pilot 14/14 for 6 s after); the same after G out and back first (14/14); a
Boosted Self-Destruct eject (14/14 for 4 s); and with a live zombie near on normal / easy, where the pilot lost exactly the zombie's
hits (3 / 2.5 each), every one with a hit's knockback. The pass's own log (`dva_log.txt`) has the drop at 2.15 s with her lifted
(`y -58.73`, clip `jump`) while backing up from its 1000-health zombie: a mob hit's knockback. Its 2.0 (not 3 or 2.5) is unexplained;
no code path of ours was found that takes 2 from the pilot. The Workshop D.Va was never hit, so its "no drop" doesn't make this a bug.
Seen in passing, not chased: one Boosters hit took a zombie 20 -> 17.08 on the old build and 20 -> 16.08 on this one (25 OW is 2.0).
