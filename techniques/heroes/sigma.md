# Sigma (hub)

Everything learned about Sigma, by subject. Hero 7 of the owner's 2026-10-28 list (issue #68), after Sojourn: a **Tank, Stalwart**
(BZ 2026-02-10), **the first Tank in Overcraft**. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · researched the way Sojourn's was (commit 879578d, branch feature/sojourn) and
Juno's ([juno](juno.md)) · his barrier builds on Brigitte's ([brigitte](brigitte.md))

## His notes and sources
- **Numbers:** [sigma.md](../../tools/reference/sigma.md): Blizzard's patch notes to 2026-11 and the wiki's revision 408635, each number
  with its game-data value. The last change to him is BZ 2026-07-14, inside the data build (154088, 2026-10-06), so the data's numbers
  are the live ones.
- **The data map:** [sigma-map.md](../../tools/reference/sigma-map.md): his kit and movement node by node (H / M / L); the detail and the
  pipeline in `E:\Overwatch Archive\research\sigma\` (`work_kit\kit_sigma.md`, `work_kit\c_<g>.txt`, `work\clipmap_sigma\`, `extract\`).
- **The plan:** [sigma-plan.md](../../tools/reference/sigma-plan.md): where the sources disagree, **what the Tank role needs that the mod
  lacks** (the role's +150 health, enemy ult -40 %, the 15 % healing reduction, Stalwart's 40 % knockback / slow resistance, a 1.4 x 2.5 m
  body, a barrier that is an entity), what maps onto Overcraft, the order.
- **Effects and sounds:** [sigma-effects.md](../../tools/reference/sigma-effects.md) (every effect, its renderers, LUT rows and
  masks, and how each is rebuilt in `SigmaFx`), [sigma-sounds.md](../../tools/reference/sigma-sounds.md) and
  `tools/soundmaps/sigma.txt` (his sounds and voice, read for wiring): step 10.
- **Clip maps:** `tools/clipmaps/sigma.txt` (3P, model 106F7), `sigma_1p.txt` (106F6), `sigma_weapon.txt` (the hand spheres 106B4),
  `sigma_props.txt` (the barrier C015, its emitter orbs FC88, the Accretion rock C5CA). Written by
  `research\sigma\scripts\clipmaps_sigma.py`, which names each clip by the alias holding it instead of a hand list (below).

## What his data taught (2026-11-02)
- **The public numbers lag the data where a change was indirect.** The barrier's regeneration is a share of its max (0.122 a second, in
  StateModifyHealth's percent mode `m_DD792FF0 = x60671788`): 85/s at 700 health, **79.3/s since the 2026-02-10 cut to 650**, which no
  patch line mentions (BZ still says 85; the wiki's 79.3 was right). Before taking a "per second" number, check the node's mode
  (x00943466 = absolute).
- **A value's main number is `value_m_3E5360A1`;** `values[1]` can be another mode's (his health 047C: 200, values[1] 300; Flux's radius
  1867: 7.5, alt 5.0). `research\sigma\scripts\gvsummary_sigma.py` prints main, alt and readers side by side.
- **Knockback comes as a horizontal / vertical pair** (`m_E33EFEE1` / `m_583C208B`): Accretion's 6 / 3 is the wiki's 6.7 m/s, the
  spheres' -0.75 / 1 their 1.3 m/s inward pull.
- **A skin can swap every model.** The entities name D75A / D7C1 / D7DD / D7C9, the classic skin's `.owentity` files 106F7 / 106F6 /
  106B4 / FC88: map the clips under the skin's model folders (clipinfo's "extracted" prefixes), not the entity's base model.
- **Floating is animation, not movement.** His mover is the default hero's in every field but its size (2.5 m tall, 0.7 m radius,
  crouched 2.05 m), and his locomotion has **one move clip for every direction** (no 8-way run); only the jump is 8-way.
- **The role and sub-role passives live in the shared graph 01C7,** keyed on the hero category (0850 Tank) and sub-role tags (116B
  Stalwart): +150 health, enemy ult x0.6 (x0.8 under a mode flag: 6v6, M), healing reduction x0.5, knockback and slows x(1 - 0.4). The
  next tank (D.Va) reads the same nodes.
- **Writing the clip map from the tree, not by hand:** `clipmaps_sigma.py` names each clip by the innermost alias on its path (the graph
  node that plays it, from `work\animrefs.txt`) plus its path (8-way slot, aim pitch, state child, jump phase), once per clip; the
  shared crouch-walk tree's unbound slots (alias 55) are skipped. Fast and complete; names for the L aliases (the additive layers) still
  want a look at the clips before the code plays them.

## Steps 1-3 (the Tank role, the hero, his movement and body), played 2026-11-02
Code: `dev.overcraft.hero.Tank` (the role for every tank), `HeroBody` (his box), `dev.overcraft.sigma` (`Sigma`, `SigmaStats`),
`AvatarMixin` / `PlayerMixin.tick` (the box), `LivingEntityMixin` / `ServerExplosionMixin` (knockback), `OwMovement.speedFactor`
(slows), `TankMathTest` (the math). Models exported with `tools/owm_survey.py --glb --keep=<clip map>` (Blender 5.2 headless,
Overwatch checked closed) from the research extraction into the shared pack (`overcraft_ana/local-assets/pack/.../hero_models/`):
`sigma.glb` (106F7, 9 meshes, 274 bones, 147 clips, 6 min), `sigma_1p.glb` (106F6 with `--entity=Gameplay1P`, 105 clips),
`sigma_weapon.glb` (the hand spheres 106B4, 41 clips), `sigma_barrier.glb` (C015), `sigma_barrier_orb.glb` (FC88),
`sigma_rock.glb` (C5CA); his UI art with `build_asset_pack.py ui`. Base clips renamed in the clip maps for the animator: 6E65
stand -> `idle`, 70DB move -> `run` (his one move clip; strafe / back fall back to it), 700D / 700E -> `jump` / `fall`, the 8-way
take-offs -> `jump_<dir>`, 7099 (the base tree's 3 s additive) -> `add_breathe` (L: read as his float, not checked frame by
frame), 1P 6F60 / 6F5D / 7094 / 7095, the spheres' 6F79 -> `idle`. Dev client run2, world `sig1` (flat survival), screenshots in
`run2/screenshots/` (not committed), each looked at.

**Data vs plan:** his eye: the head hardpoint 0076 (on bone_0011) in his stand's first frame is **2.06 m** over his feet (the
map's 2.079 is another frame; his run's is 1.995), used as the camera; crouched 1.61 m (ours). His float is in his clips: his
ankles 0.42-0.5 m up in the stand, the rest pose's feet on the ground, so nothing lifts him. The emitter orbs (02FD-0300) are
parked behind his shoulder in his stand: they belong to the held barrier (step 5), not his idle; drawn there they were invisible
(tried, removed). The plan's per-hero box replaced the coordinator's first default (Minecraft's box) the same day, the owner's
decision (sigma-plan.md "Minecraft decisions").

- **Select screen** (`s1_select.png`, `s2_select_sigma.png` -> `f1..f4.png`, `s4_rejoin.png`): a TANK group with Sigma alone,
  "TANK STALWART", "350 HEALTH, 250 SHIELDS", five kit icons (LMB Hyperspheres, RMB Experimental Barrier, LSHIFT Kinetic Grasp,
  E Accretion, Q Gravitic Flux). **Found:** at the others' framing he filled the screen from the hips, head cut off (his head is
  2.12 m up): framed by eye with a live knob (`selectframe`): 0.62 size, 0.85 drop (`f4.png`). The TANK header was wider than its
  one portrait: Sojourn's `groupWidth` fix taken over (same code as her branch). **Found (with the box):** Kiriko previewed
  0.35 m low while the player had his 2.5 m box (Minecraft centres the preview on half the box): corrected, the same framing
  either way (`s4_preview_cmp.png`).
- **HUD** (`s1_front.png`, `s2_1p.png`): 600 | 600, 24 pips (14 white, 10 blue shields), Kinetic Grasp (LShift) and Accretion
  (E) boxes, the barrier and Hyperspheres panels without ammo. A hit took the shields first (the slimes: 523 -> blue pips gone first).
- **His look:** third person front, back, from above and below (`s2_sheet.png`, `s2_front_hands.png`): the two spheres float over
  his right hand (dark faceted cages: their material 7C98 has no colour texture, so no glow yet: M), nothing in the left (right:
  the orbs only come with the barrier). First person (`s2_1p.png`): his right hand, palm up, the two spheres over it. The pause
  card says SIGMA TANK STALWART (`pause.png`).
- **Health:** 48 Minecraft health (600) on picking him, after dying and respawning, after Save and Leave and rejoining.
- **Movement** (`trace`): 5.50 m/s forward, the one move clip (`run`) in every direction (`s5_move_sheet.png`); a jump 0.979 m
  (W 0.98), 13 ticks in the air, `jump` playing.
- **His box (the owner's 2026-11-02 decision):** standing 1.40 x 2.50, eye 2.06; crouched 1.40 x 2.05, eye 1.61, on the client and
  the server alike (`state`'s `box` / `serverBox`). An open oak door (1 wide, 2 high): stopped at its face, z 19.30
  (`s3_door_blocked.png`, `s3_door_sheet.png`: in first person the wall over the doorway); in Minecraft mode (G) the same walk goes
  through (z 27.0). A 2-wide, 3-high opening: through. A 2-wide, 2-high tunnel: stopped, standing and crouched. A 2.125 m gap
  (daylight detectors under top slabs): stopped standing, through crouched (`s3_crouch_tunnel.png`). The camera against a wool
  column (white / red / blue / yellow, 1 m each): hero mode on the blue just over the red (2.06), Minecraft mode on the red's top
  (1.62) (`s4_eye_sheet.png`). **G in a 2-high tunnel:** hero mode on, the box stays Minecraft's (no damage, 48 health), walking
  out it grows (`s4_g_in_tunnel.png`); **G in the 2.125 m gap:** his crouched box, crouched by Minecraft (`s4_g_in_low_tunnel*.png`).
  Switching to Kiriko: Minecraft's box; back: his.
- **Found (fixed):** the client's own player kept Minecraft's box on rejoin (server 1.40 x 2.50, client 0.60 x 1.80) until a crouch
  refreshed it, and kept his after G (the camera stayed at 2.06 in Minecraft mode: `s4_eye_column_mc.png` before the fix). Now the
  box is checked every tick and the local player's hero read from the client's own state; both seen right after a rejoin and G.
- **Knockback (Stalwart 40 %):** a zombie's hit, the first tick after it: Sigma 2.62 m/s along, Kiriko 4.37 (x0.60); vertical
  2.30 / 5.44. A creeper 3 m off (Resistance V): Sigma 4.95 along / 2.56 up, Kiriko 8.58 / 3.92 (x0.58 along, x0.59 in all;
  the rest of the gap is his higher eye, which Minecraft aims the push from). The zombie hits him every second (6 in 6 s).
- **Slows (Stalwart 40 %):** running speed, Sigma / Kiriko: none 5.50 / 5.50; Slowness I 5.00 / 4.67; Slowness II 4.51 / 3.85;
  Local Anesthetic's own modifier (`overcraft:ana_local_anesthetic`, -40 % of the base, put on with `/attribute`: the stand-in
  for a hero's slow, Sojourn's field not being on next) 4.18 / 3.30. Expected 5.005 / 4.51 / 4.18 against 4.675 / 3.85 / 3.30.
- **Not tried:** the enemy ult -40 % in game (it needs a second player hitting him: unit-tested only, the plan's step 12); arrows'
  and a ravager's knockback; water, lava, ladders, boats, the Nether; others' clients seeing his box and model; the 15 % healing
  reduction (not built: the global one isn't in Overcraft).

## Steps 4-6 (Hyperspheres, Experimental Barrier, Kinetic Grasp), played 2026-11-02
Code: `dev.overcraft.sigma.Hyperspheres` + `entity.HypersphereEntity`, `sigma.SigmaBarrier` + `entity.SigmaBarrierEntity`
(drawn by `client.model.SigmaBarrierRenderer`), `sigma.KineticGrasp`, and `dev.overcraft.hero.Barriers`: **a barrier can now be
an entity** (a `Barriers.Shape`: an upright rectangle with a front, its owner's), which Brigitte's `Barrier.absorb` /
`blocksBlast` and `ServerExplosionMixin` ask first, so both kinds live in one path ([barriers](../minecraft/barriers.md)).
Numbers in `SigmaStats`, each with its source; `SigmaMathTest`. Stand-in effects and sounds (vanilla particles and sounds:
his own are step 10). Dev client `runClient` (port 25599, `-Xmx3G`: 2 GB ran out of memory loading every hero's models),
fresh flat survival world `sig4`, screenshots in `run/screenshots/` (not committed), each looked at. Measured with the
harness (`trace` now also logs new spheres `h` and the barrier `b x y z state share`), `data get` for mob health and
`AbsorptionAmount`, husks (no sunburn) and skeletons with a helmet, a bow and `movement_speed` 0.

**Hyperspheres** (expected -> seen):
- Volley timing (`trace`, held 3.3 s): spheres at ticks 2, 6, 32, 36, 62, 66: **0.2 s apart, 1.5 s a volley** (D).
- Two direct hits on a husk 10 m off: 20 -> 10.66 health. Expected 2 x (20 + 40) = 120 = 9.6 Minecraft health, less the
  husk's 2 armour points (Minecraft's armour rule per hit: 1.523 + 3.149 per sphere = 9.344): **exactly 9.344**
  (`hs_fly.png`: the burst's stand-in particles on it).
- The burst and its falloff: no creature in the way, three husks around the end of the spheres' 21.7 m: at 1.0 m from the
  burst 13.70 (expected 13.70: full 40), at 2.0 m 15.49 (expected 15.48: 29.2 each), at 3.0 m (past 2.85) 20.0 (untouched):
  **the 21.7 m / 0.434 s burst point and D's falloff both hold** (`hs_burst.png`).
- Bounce: at 45 degrees into a stone wall 5.5 m ahead, both spheres came off it and hit a husk round the corner (two
  direct hits, 9.344 again; `hs_bounce.png`). The wall is whole: no block breaks.
- Not measured: the pull (0.75 m/s in, 1 m/s up, a few hundredths of a block a tick: lost under Minecraft's own 0.4 hit
  knockback on the direct hit); a sphere against a player (no second player).

**Experimental Barrier** (expected -> seen):
- Out: centre 2.3 m ahead of his eyes (z 2.80 from z 0.50), bottom at his feet (y -60.03), then **1 block a tick = 20 m/s**
  while held (`trace`), still when let go; held into a stone wall it stopped at **13.1 for a wall at 14 (0.9 m short)**.
- Look (`b_enemy_side.png`, `b_edge.png`, `b_near_1p.png`, `b_vis_back.png`): its mesh C015 at its data size (6.70 x 4.18 m,
  checked by its pixel size at 18 m), bulging toward the enemy, its four orbs FC88 on its corners. **Found:** its material
  (7C75) has no texture in the export (a shader in Overwatch) and the model renderer skips untextured pieces: nothing was
  drawn; now a field of added light (`HeroModelRenderer.submitField`). **Found:** at first it was near opaque white (added
  light ignores alpha): scaled to a faint tint.
- Skeleton arrows from the front (skeleton 8 m beyond it, a bow, speed 0): **Sigma 48 -> 48, barrier 650 -> 500** in 8 s;
  his ult +43 (40 passive + 2% of 150). From behind (Sigma on its front side, the skeleton behind it): **Sigma 48 -> 40.1,
  barrier untouched**. A blaze from the front: 3 fireballs, **barrier -187 (62.5 each), Sigma untouched**. A ghast: each
  fireball bursts at its front, **barrier -150 a fireball** (Minecraft's blast rule at 0.3 m: 12 health), Sigma untouched;
  a creeper 2 m before it: **barrier -284, Sigma (1.8 m behind it) untouched and not pushed** (`b_creeper.png`).
- Broken (three creepers): gone, **no second deploy for 5 s** (a press 0.4 s after: nothing), out again after;
  regeneration **79.3/s from 2 s after it came back** (recalled at 313: 376 at 2.6 s, 606 at 5.5 s, full at 6.1 s).
- Recall: a press while it's out brings it back; out again 1.1 s later worked (1 s cooldown; the HUD showed 0.2 s left on
  the panel at 0.7 s, `b_recalled_hud.png`).
- A husk walked through it (z 15.9 -> 12.9 past its 14.8) to Sigma: creatures pass.
- G with it out: it came back; switching to Brigitte: gone. **Found (fixed):** killed with it out, it stood there until he
  respawned (his kit doesn't tick while he's dead); now it goes home when he dies: after the fix, `trace` saw it for the
  ticks before `/kill` landed and never after (8 of 34 ticks), on a fresh client.
- After the brightness fix (`b2_enemy_side.png`, `b2_own_side.png`): a pale see-through blue from both sides, the world
  behind it readable, its orbs on the corners.
- Brigitte's own barrier after the change: a skeleton's arrows taken (300 -> 257), her health untouched.
- **Not tried:** Sojourn's rounds against it from a second player (a second client didn't fit in memory beside this one:
  2 GB free); the Toggle and Reload-recalls options (not built: Overwatch's defaults, both off); slopes and stairs under a
  moving barrier; the Nether.

**Kinetic Grasp** (expected -> seen):
- A skeleton's arrow (4 health = 50) inside the 2 s: **overhealth 2.4 = 30 (60%)**, health untouched while it held;
  **decay from 3 s after the end at 5 a second** (2.32 -> 2.00 in 0.8 s; 0.72 -> 0.40); cooldown **12 s from the end**
  (10.6 at 1.4 s).
- A blaze's volley (3 x 62.5): **9.0 = 112.5 (60% of 187.5)**. A ghast's fireball (6 + its blast's 15 = 262.5): **12.6 =
  157.5** (`g_blaze.png`, `g_ghast2.png`).
- Cut short with G mid-hold after taking fireballs: **no overhealth**, cooldown 12 s started. A stun ends it the same way
  (HeroManager.controls is false while asleep or stunned), not tried in game (no second player with a stun).

**A night fight** (`fight_mid.png`, `fight_end.png`): midnight, three zombies and two bow skeletons (helmets) 16-20 m off,
the barrier out, volleys aimed at the nearest each 0.5 s, Grasp every third volley: in 40 s the three zombies and one
skeleton died (47 volleys, 6 Grasps), Sigma ended at 48 of 48. The last skeleton backed off to 35 m, past the spheres'
21.7 m: they burst short of it, as in Overwatch. Not tried: a raid, water, the Nether, ladders, a second player.

## Steps 7-8 (Accretion, Gravitic Flux), played 2026-11-02
Code: `dev.overcraft.sigma.Accretion` + `entity.AccretionEntity` (drawn by `ProjectileModelRenderer` with his rock's own
model, `=sigma_rock`), `sigma.GraviticFlux` (his phases, the reticle, and every lifted creature: 1DBC), his flight in
`OwMovement.sigmaFly` (fed by the client from his sync bits `MODE_FLUX_FLIGHT` / `MODE_FLUX_RISE`), the gather's jump
lock `OwMovement.noJump` (`LivingEntityMixin.jumpFromGround`), `Sleep.knockdown` (a stun that keeps its push),
damage types `overcraft:gravitic_flux` (the lift: no_knockback, no_impact) and `overcraft:gravitic_slam` (also
bypasses_armor, bypasses_effects: the data's "no modifiers"). Numbers in `SigmaStats`, each with its source;
`SigmaMathTest` (burst falloff, the slam and its cap, the rise). Stand-in effects and sounds (vanilla particles: his
own are step 10). Dev client `runClient`, fresh flat survival world `sig7` at midnight, screenshots in
`run/screenshots/` (`s7_*`, `s8_*`, not committed), each looked at. Measured with a one-connection driver (the
scratchpad's `oc.py`: `c`, `mc`, `data`, `num`), husks / zombies with `movement_speed` 0 or NoAI.

**What the data added to the plan:** Flux has a **0.6 s start** before the aim (1E41 #964 -> #954 -> #967): he rises at
10 m/s² (`xA634D7B9 = up x 10`) while it plays; his flight (#974 -> #1110 -> #977) runs under every phase after it,
including his 0.5 s slam phase, unless he is himself lifted. Each target's lift (1DBC) is 2 s, not the phase's 2.5:
damage and a 1 m/s pull in at #575, held 0.4 s (#586: the same input-off flags as a stun), then (0, 18, 0) up with
gravity x0.1 and a 3 m/s cap from the hold's end (#608 / #615, read as Levitation's accel and cap: M), 5 instead of 18 in
its last 0.2 s; at the end gravity x22 and the slam the moment ground is within 5 m below (#644 -> #577), 0.6 s more
after (#585). That reading gives a ~6 m lift, and the zombies rose 5.96 m. Hyper Strike's target graph 54A5 (for step
9) has its +50 (v476, over 0.2 s) and a 0.8 s float (gravity x0.01, input off).

**Accretion** (expected -> seen):
- The gather (`trace`, running forward): **5.50 -> 1.37 m/s** (x0.25: -75%) for 12-13 ticks, back to 5.50 after; jump
  held through it: **no jump** (y stayed -60.000). Its own slow: a Stalwart doesn't resist it (`Tank` OWN_SLOWS).
- Cooldown: **10.45 at the throw** (10 s from 0.45 s after it, counted as one), the HUD's E box counting from it
  (`s7_rock1_after.png`). The 3P gather and throw clips play (`s7_gather_3p.png`, `s7_rock_fly_a/b.png`).
- A husk 10 m ahead (2 armour): **20 -> 10.5536**, exactly 80 + 40 through Minecraft's armour rule (6.2976 + 3.1488);
  knocked 1.12 m back along the flight; **NoAI 1b** 0.2 s after the hit, gone after 1.4 s (the 1.1 s knockdown);
  +25 ult on the hit.
- A group (husks 1.5, 2.3 and 3.7 m beside the one hit): the hit one 120; **1.5 m: 40 (full)**, **2.3 m: 24** (the
  falloff to 12 at 2.5 m), **3.7 m: untouched**; the two splashed pushed away from the burst (x +2.0 / +0.8 m), not
  knocked down (no NoAI).
- A stone wall 10 m ahead: the rock bursts on it; a husk 1.5 m before it beside the burst took 31 (2.48 health), **one
  1 m behind it untouched** (sight).
- The arc: a husk 30 m off, Sigma level: **pitch -9.5° hits** (the ballistic solution for 37 m/s and 18 m/s² is 9.5°),
  **-5° falls short, -14° flies over**: the speed and gravity hold.
- **Found:** the rock model C5CA is 2 m across, drawn at its data size it dwarfs his head as it leaves (`s7_rock_fly_a.png`);
  with W's / D's 0.9 m read across (as the spheres' size was) it would hit with a 0.45 m radius inside a 2 m rock.
  Read as the radius instead (M), so the rock hits what it visibly touches.
- **Not tried:** a player knocked down (one client only: a second one doesn't fit in memory beside this one); the rock
  against an enemy barrier (no second Sigma); water, lava.

**Gravitic Flux** (expected -> seen):
- Q (`trace`): **rise 1.47 m in its 0.6 s start**, then still in the air with nothing held (no gravity), the aim loop clip
  in first person (`s8_reticle_1p.png`: both hands up). The reticle, his alone: a 7.5 m ring of violet dust where he
  aims and its centre; the left button fixes it, or after **5 s it goes by itself** (flight ended 7.05 s after Q:
  0.6 + 5 + 1 + 0.5 with no one caught).
- His flight: **7.15 m/s** forward and sideways, **+7.15 up** on jump, **-7.15 down** on crouch (`trace`), stopped in a
  tick or two when let go. Flown 13 m up, the flight's end dropped him to the ground: **48 -> 48** (no fall damage,
  `softLanding` as Mercy's). A creeper beside him in flight: **2.07 m/s** pushed, on the ground **4.95** (x0.42: half
  the knockback, the rest his flying mover braking it in the same tick).
- A crowd (NoAI: two zombies, a cow, a ravager; a zombie 10 m off as the control), the point 9.2 m ahead: the four in the
  cylinder lifted and slammed, **zombies 20 -> 6.064** (50 through armour 3.936 + 125 = 10 straight), **cow 10 -> 1**,
  **ravager 100 -> 46** (4 + 50: half of its 1250), the control untouched; a zombie **rose 5.96 m in 1.9 s** after the
  1 s cast, 3 m/s after its hold, pulled 0.48 m in, then slammed down in 0.25 s (`s8_lift_3p.png`). None took fall damage.
- The bosses (NoAI, `mob_griefing` off): **Wither 300 -> 280.32**, **Warden 500 -> 480**, **Ender Dragon 200 -> 180**:
  the slam capped at 200 (16), the lift's 50 (the Wither's armour 4: 3.68), the dragon through its head
  (`s8_bosses_lift.png`: Wither and Warden in the air; the dragon isn't moved: ours).
- With their AI on (three zombies, a cow): the zombies walked in during the cast and were lifted (NoAI 1b while lifted),
  slammed (6.064 each), then got their AI back and chased him (he dropped to 24 of 48 under them: as in a fight).
- An iron golem in the cylinder (an ally in Overcraft: `Allies`) passed by: 100 -> 100, the zombie beside it lifted.
  The left button held through Flux: no spheres (his hands are busy, as in Grasp and the gather).
- **Found (not a bug):** a first run with a ravager (`movement_speed` 0 doesn't hold a ravager) killed Sigma and caught no
  one: the reticle was 25 m out (aimed from 4 m up) and the zombies at 15 m were outside its 7.5 m; with the aim on them,
  every run caught them. Use NoAI ravagers.
- **Not tried:** a player lifted (one client: their lift is sent as their speed each tick and their keys held 0.4 s, as
  asleep: untested); the right-button confirm option and the "rise with ability 3" option (not built: Overwatch's
  defaults, Open); the reticle's HUD timer; Flux over water or a cliff (no ground within 5 m: let go after 4 s).

## Step 9 (perks), played 2026-11-02
Code: `dev.overcraft.sigma.SigmaPerks` (hooks in `KineticGrasp.end`, `Hyperspheres.burst`, `QuickMelee.use`), his
Levitation on his own client (`client.ClientSigma` -> `OwMovement.levitating`; EXTRA_JUMP tells the server, whose
`softLanding` keeps its landing safe), `Sleep.knockdown(entity, seconds, gravity)` for Hyper Strike's float. Numbers in
`SigmaStats` "perks". Each picked through the perk menu (`overcraft perks @s 3`, then Left Alt held and the left button
for a tier's first perk, the right for its second: `s9_perk_menu.png`, the two icons by the ult after:
`s9_perks_hud.png`, `s9_perks_hud2.png`); `overcraft perks @s 3` again clears the picks for the other pair.

**From the data, past the plan:** Hyper Strike's target graph 54A5 holds its numbers: **+50** (v476, dealt over 0.2 s:
ours at once) and a **0.8 s float** (v955; gravity x0.01 and its input off, #404), the knockback **14 m/s along
norm(aim + 0.3 up)** (51E0 #392). Levitation starts on the jump *held* from 0.2 s in the air (#379 -> #375 -> #370).

(expected -> seen, each against its control: the same steps without the perk)
- **Kinetic Cycle** (3 arrows of 50 absorbed by Grasp, Accretion on cooldown): control **no change** at Grasp's end
  (overhealth 7.2 = 90 = 60% of 150); with it **7.65 -> 4.55 s** at the end (3.10 = 150 x 0.02 + one tick).
- **Hyper Regeneration** (barrier out and stopped at 400 after 5 arrows, three volleys into a husk past it, 350 dealt):
  control **400 -> 400**; with it **400 -> 506** (+106; 30% of 350 = 105).
- **Hyper Strike** (a 100-health husk, three volleys = 6 direct hits, then V): control **40** (3.149 through its armour,
  a nudge); with it **90** (7.085: 40 + 50), knocked **5.9 m in 0.8 s** along his aim (14 m/s, Minecraft's air drag)
  and lifted 0.3 m, NoAI through the float.
- **Levitation** (jump held from the ground): control a **0.979 m** jump; with it **3.00 m/s up from 0.2 s after
  take-off for 1.75 s** (35 ticks), **6.38 m** at the top, down with **48 -> 48** (no fall damage); a second hold in the
  same air does nothing, the next landing allows it again (the held key jumped and levitated once more).
- **A night fight** (four zombies with AI, two bow skeletons): the rock on the first (120, knocked down), Flux on the
  three that had closed in (lifted, slammed to 6.064, one killed), spheres finishing them; Sigma down to 18 of 48 under
  them, back to 48 after (`s9_fight_rock.png`, `s9_fight_flux.png`, `s9_fight_end.png`).
- **G in Flux's flight**, 11.7 m up: Minecraft mode, his box back to Minecraft's, the fall landed **48 -> 48**; G again:
  hero mode, his 1.40 x 2.50 box.
- **Not tried:** Hyper Strike on a player, Levitation in water and on a ladder (it stops there: ClientSigma), the perks'
  HUD (no Hyper Strike charge counter yet: the data's UX presenter #425 shows hits / 5).

## Step 10 (his effects, sounds and voice from the data), played 2026-11-02
Merged `next` first (Jetpack Cat's asset work, 06cdd2c: clean). Research in two parallel passes, both offline with
Overwatch checked closed before every TankLib / DataTool run: effects ([sigma-effects](../../tools/reference/sigma-effects.md),
`research\sigma\fx\Sigma\<Ability>`, scripts `research\sigma\scripts\fx\fx_sigma_*`: 135 effects, none missing, 39 drawn)
and sounds ([sigma-sounds](../../tools/reference/sigma-sounds.md), `tools/soundmaps/sigma.txt`: 128 events, 768 files, 29
extra banks in `research\sigma\sounds\ExtraSounds\Sigma`; [sounds](../sound/sounds.md#sigma-sounds)).

Built: `client.effect.SigmaFx` (the server's moments `sigma_*`: volley, burst, absorb, overhealth, rock impact, barrier
break / recall / Hyper Regeneration heal, the reticle (his alone), Flux's cast and targets taken, each lift, each slam,
Hyper Strike's target; the spheres' and rock's flights from `ProjectileModelRenderer`; per tick from his posed points
(`SigmaFx.POINTS`: the cores' bones 0442 / 0444 and hp 007C on 106B4, his 000B / 0002 / 01B6 / 0076 / 0004) and a new synced
`HeroManager.SIGMA_STATE` (Grasp, gather, Flux flight / aim, Levitation, Hyper Strike charged): Grasp's field, the gather,
Flux's glow, Levitation, Hyper Strike's wrist glow, the barrier's corner motes and send streaks); his palettes
(`SigmaPalettes`, 112 named in `EffectArt.Palette`), 18 masks and 6 products in `build_asset_pack.py` (with the baked
barrier field); the barrier drawn through its field texture (`SigmaBarrierRenderer`); the hand spheres' cores as added light
(`HeroModelRenderer.submitModel`, submesh key 6B24EE2D2B083AFE: **the "dark spheres" of steps 1-3 were the cages, textured;
7C98 is the 0.052 m cores' material**). Sounds: `sigma.SigmaSounds` (server one-shots), `client.SigmaSounds` (Grasp's hum,
Flux's flight, Levitation, each sphere's and rock's flight loop and the rock's fly-by, the barrier's travel and standing hum,
the reticle's loop), `OcSounds.SIGMA_SOUNDS` / `SIGMA_VOICE`, his 14 stimuli in `Voice`, his kill lines (`Sigma.killed`), the
melee foley (`QuickMelee`), the ult lines through `HeroSounds.ultLine`, his 43 voice-line unlocks (`VOICELINE_HEROES` /
`VOICELINE_ROOTS`). Every vanilla stand-in particle and sound of steps 4-9 is gone. Built into the shared dev pack
(`overcraft_ana/local-assets/pack`) from a scratch root, merging only `sigma.*` into `sounds.json` / `en_us.json` (backed up
first) and `voicelines/sigma.json`; masks by a runner that keeps only the `sigma_*` entries.

Played (dev client `runClient`, world `sig10`, flat survival at midnight and noon, Sigma picked on the select screen, again
after Save and Leave and rejoin, and through the select screen from Kiriko; `heard` / `playing` read after each step;
screenshots `run/screenshots/s10_*`, `s11_*`, `sigma_s12_*`, not committed, each looked at):
- **Hyperspheres** (a 400-health husk 10 m off): per volley `sphere.volley` + the exert, a `flight.loop` at each sphere,
  `hit` + `hit_enemy` per direct hit, `burst` (first) and `burst2` (second) at the sphere for him; at a wall `hit_block` /
  `hit_block2`. Look (`s11_hs_fly`, `s11_hs_burst`: `/tick rate 4` + `fxslow 0.15`, then turned 55 degrees): a bright
  navy-white core with its blue ribbon, the burst's shell, star flash and dark streaks.
- **Barrier:** deploy + send + "Barrier initialized!", `move.loop` in his head while sent, `stop` and `hum.loop` at it once
  stopped; recall: `stop`, `recall`, `return`, "Return to me!"; arrows of 225: `brigitte.hit_barrier` per hit, `low` under
  50 %, `break` + "The barrier has fallen!" (`sigma_s11_break*`: its shards, sparks and field-shaped flash). Its field
  (`s11_b_night`, `s11_b_day`, `s11_b_1p_night`): see-through blue with a bright rim and hex cells, a husk behind it readable,
  day and night, 1P and 3P; the corner motes round. Hyper Regeneration: barrier 0.769 -> 0.823 after one volley (+35, 30 %),
  its teal heal streaks over the face (`fxnear`: 7-15 live for 0.5 s).
- **Kinetic Grasp** (three summoned arrows): `start`, his line, `loop`, three `absorb` at the box's front, the overhealth
  sound at its end, 7.2 absorption (90); "Hold it together...!" on an absorb when he wasn't already speaking. Look
  (`sigma_s12_g_back`): a blue field before him.
- **Accretion:** `gather`, the exert, `throw`, the fly-by and flight loop following the rock, `hit_enemy` + `impact` on a husk;
  `impact` + `hit_block` on a stone wall with its crater decal (`s10_a_wall`). "Collision!" on a kill 3 s after the throw (at
  1 s the exert still held his voice: one line at a time).
- **Gravitic Flux** (four NoAI zombies): `start` + "What is that melody?" (`ult.friendly`), `flight.loop`, `aim.loop` at the
  reticle, `cast` at the point, `lift` at each, `slam_3p` + `slam_land` + `slam_hit` per target, `end`; "To the void!" on a
  slam kill. Look (`sigma_s12_f_aim`, `_body`, `_cast`, `s10_f_lift1`): the reticle's rings on the ground, a faint glow on
  him, the 15 m column translucent, rings and glows round each lifted zombie.
- **Perks:** Kinetic Cycle's sound at Grasp's end (Accretion 3.35 s left: 3 s cut); Levitation's loop in his head while
  held (stopped on release; landed 48 -> 48); Hyper Strike's `ready` on the fifth direct hit, its wrist glow, then the melee:
  `hit_3p`, the struck husk knocked off wearing the lifted look (`s10_p_strike_hit`).
- **Voice:** select (picked from Kiriko), respawn, death, pain, melee, On Fire; the Voice Lines screen lists his 43, "Hold it
  together" equipped UP and said from the wheel (`s10_wheel_said`: subtitle "Sigma: Hold it together.").
- **Stopping:** G mid-Grasp and with the barrier out stopped their loops within 0.6 s (`playing`); dying mid-ult stopped
  Flux's loops. A scripted night fight (three zombies, two skeletons with AI) ran every ability with its sounds; he died to
  the zombies (the script doesn't dodge), respawned at 48.
- **Found playing (fixed):** masks that are full-frame noise drew as squares (launch cone, corner motes, Grasp's disc, send
  streaks, Flux's glows, Levitation): baked as the data's pairs; the barrier field at first a solid cyan wall, white by day:
  0.22; Flux's cylinders and reticle white-outs and Grasp's screen-filling haze: toned down; the cores pink (the LUT row
  without its emission): RGB x A; sphere core and halo back to the data's 0.22 / 0.32 m; the launch swirl stretched into a
  bar: a spinning swirl. Each re-seen after a restart.
- **Not tried / open:** a second client (no `_3p` copies, enemy red rows, `absorb_shooter`, `flux.lifted` / `slam` / Hyper
  Strike's own copies on a player heard or seen); the `barrier_low` / `barrier_damaged` lines (the arrows broke it within a
  second; their 1 s checks are wired, unheard); the burst slot reading (L; played: it sounds at the sphere where it bursts);
  the EXTRA layers not wired (`fire`, `reform`, `layer`, `hold_3p`, `move_3p`, `ambient.loop`, `throw_crack`, `lift_foley`);
  Grasp's disc size (drawn 2.7 m, the box; the data's 8-9 m read as quads unchecked); the barrier's cracks by health (548D1
  is BC5); the refraction / distortion renderers; the overhealth's body material (a glow stands in); world glows at his hands
  hidden by the 1P view model (the cores themselves now draw in 1P); not listened to for balance by a person.

## Step 11 (his emotes from the data), played 2026-11-02
All 14 from the data ([sigma_emotes](../emotes/sigma_emotes.md)): 64 clips in `hero_models/sigma_clips.glb`, the spheres re-exported
with their 6 emote clips (`sigma_weapon.txt`, `--clips-from`), his data `emotes/sigma.json` (`tools/emote_data.py sigma`, slot
54CE), five props (`prop_d7dd`, `prop_c5ca`, `prop_eaad`, `prop_3b50`, and AE48 added to the shared `prop_1024e`), his emote sounds
(`tools/soundmaps/emotes.txt`, Sigma's section: 30 02Cs, 3 voice stimuli; 23 own banks extracted into
`ExtraSounds\Sigma\Emotes`; [sounds](../sound/sounds.md#sigma-emote-sounds)), built in a scratch root and merged into the dev pack
(only `sigma.emote.*`, their subtitles, `sounds/sigma/emote/`; `sounds.json` and `en_us.json` backed up first; the old
`sigma_weapon.glb` and `prop_1024e.glb` too). What it took:
- **Constellation keys the spheres' root in his space, not on his hand** (it starts at his idle's hand while his Weapon_R starts
  1.4 m higher): `HeroWeapon.place` puts such a clip at his origin with its root applied (`keyedInHisSpace`); the other sphere
  clips keep the hardpoint.
- **Props a prop's clip spawns:** Magic Pumpkins' pumpkins hang on the carrier 1024E's bones, spawned by its clip AE48
  (`EmoteProps.inner`, as Lúcio's booth); `emote_data.py` no longer pairs a spawn with the clip that spawns it.
- **A shared prop keeps every hero's clips:** `build_asset_pack.py props` merges the pack file's clips into the new export
  (`tools/merge_glb_clips.py`).
- **Found playing (fixed):** the emote camera, 2.5 blocks out for every hero, had his head out of the top of the screen (eye 2.06,
  hands to 3.3 m): it now orbits out in proportion to the hero's eye height over 1.62 (3.2 blocks for him, others unchanged,
  `EmoteCamera.reach`). The spheres' copies (D7DD: Dans, Constellation) drew dark: their cores are the same material 7C98 under
  another submesh key (`SIGMA_CORE_BASE`), now lit as the spheres' are.

Played 2026-11-02, dev client (`OC_ASSET_PACK` = the Ana checkout's pack, `-Xmx3G`), fresh creative flat worlds `sigemo`, `sigemo2`,
`sigemo3` (one per client start), noon frozen, mobs off; Sigma picked on each world's select screen (his portrait, READY: 48
health, eye 2.06); Overwatch and Battle.net checked closed before every archive read and launch. Screenshots in `run/screenshots/`
(not committed), each looked at:
- **All 14 by the dev `emote`**, each started from first person (the emote camera takes over), frames at 1.0 / 2.5 / 4.5 / 6.0 s
  (`sb_<emote>_<t>.png`, sheets `sb_sheet_*.png`; before the camera fix `sa_*`), each ended by a held W (0.3 s) or by running out;
  clips and sounds polled every 0.1 s (scratchpad `sigma_play.py`). Chains: Dans 83AA -> loop 83AB, Relaxation 7341 -> loop 7344,
  Stepping Stones AF58 -> loop AF59; Heroic plays 7458 (6E65, 700F, 7349, 7010 are the gallery preview tree's).
- **Held frames** (`emoteat`, `props`): Mic Drop 0.6 (`sc_mic_0.6_zoom.png`: the mic in his left fist; contact: its +Y 16 degrees
  off his left knuckle line through 0-0.93 s, `emote_contacts.py --knuckles`) and 1.6 (on the ground); Dans 3.0 / 4.5 / 6.0 / 8.0
  (the juggled copy round his chest and hands, cores lit after the fix: `sd_core_zoom.png`); Constellation 1.0 / 3.5 / 6.5 from
  below and the side (`sc_con_sheet.png`, `sd_sheet1.png`: the two copies 0.9-1.1 m off his raised hand, the spheres back in his
  hand at 6.5); Observations 4.5 (`sc_obs_4.5_zoom.png`: the sphere's cage opened round its glowing core, the clip's own);
  Stepping Stones 2.92 / 4.6: at each footstep (91C3 on 001C / 001B) the foot is 0.12-0.15 m ahead of and 0.22-0.24 m over a
  rock's origin. Seen: pumpkins rising round him, the baton and the rocks circling in Maestro, his rock gathering under him and
  his seat on it in Relaxation, the spheres away (`weapon` "out": false) in Listen Up / Mic Drop / Salty / Dans / Magic Pumpkins /
  Applause / Inconsequential / Laugh / Maestro / Stepping Stones and in Constellation's 2.0-5.9 s.
- **Sounds** (`heard`, about 0.1 s late): Listen Up E548 + E549; Mic Drop DA44 1.08 (DA4F, 40 %, not in those plays); Salty EEA6;
  Dans 606E, 606D at the loop; Magic Pumpkins 81C1; Applause 5A33; Heroic 5A6A; Inconsequential 5A37; Laugh 5A44 + "(laughs)"
  06A1; Maestro 60C0 + 610B (60D4, the duplicate, never); Observations 5A58; Relaxation 5A5A, then in the loop 5A72 at 0.59 s,
  5A5B, 5A72 at 2.08 s; Stepping Stones 919E + 5A1A + "(humming)" 11B3, 5A23 1.24, 91C3 2.92 / 4.6, 91C2 4.65, then the loop's
  91A0 + 5A23, 919F + "(humming)" 12D3 0.83, 91C3, 5A1A; Constellation 9DD9. No missing-sound warning.
- **Ending** (`playing`): Maestro's 10 s 610B and Observations' 5A58 stop with a held W mid-emote (nothing playing 0.5 s after);
  a one-shot already started (Relaxation's 1.9 s 5A72, Stepping Stones' 919F and hum) plays out, as every hero's. **G mid-emote**
  (Observations at 2 s, the real G key): Minecraft mode, box 0.60 x 1.80, the emote and 5A58 gone; G again: Sigma, hero mode.
  **From third person** (Relaxation): the emote camera, then back to third person after a held W (`se_ends_sheet.png`).
- **The player's way:** Esc -> Voice Lines -> Emotes listed all 14 (`sw_em.png`); Maestro / Constellation / Magic Pumpkins put on
  right / down / left (`sw_em_set.png`); T held, the primary-fire button (`key primary_fire`) to the emote page, the pointer right: Maestro (60C0 + 610B);
  down: Constellation (9DD9); left: Magic Pumpkins (81C1); each ended by a held W (`sw_sheet.png`). (The secondary-fire button is the voice
  page: "Reduce your expectations" heard when tried first.)
- **Not done / doesn't match yet:** no separate agent critiqued the frames; a second player hearing the "others" copies and seeing
  the props not tried; the 0.3 s 6599 and 0.67 s 7004 layers (folder 4, in most of his emotes) and Relaxation's 0.47 s step 7346 are
  never played by our plan (not read what they are); Salty has no salt shaker (the data spawns none, as for every hero); what each
  of his own sounds is was read from timing and measurements, not listened to (L).

## Step 12 (the release play), played 2026-11-02
`next` merged first (9e9d3af: Jetpack Cat's flames, Catnapper and perks, #59; conflicts kept both sides: Catnapper's
per-target credit with its target plus its new hit effect, both harness notes, `bake_sigma_barrier` beside `bake_cat_flame`
under `add_effects`' new `only=` filter). Dev client `runClient` (`OC_ASSET_PACK` = the Ana checkout's pack, `-Xmx3G`),
Overwatch and Battle.net checked not running before every launch. Worlds: `sig12` (flat survival: a bed at -9 -60 0/1 and
the spawn point beside it; a stone-brick wall with an open oak door at 30 -60 20; a 3-wide, 2-high tunnel at x 39-41 z 9-20;
a 2-deep pool x 60-80 z 0-10 with a lava strip x 82-86; a ladder at 100 -60 1; a half-block ramp x 110-117 z 20-24) and
`sig12n` (normal terrain: a savanna shore; a stone platform at -42..-30 79 38..54 with a Nether portal). Scripts over one
harness connection (scratchpad `sig12\oc.py`, `lib.py`, `r*.py`, `n*.py`, `g1.py`, `oc2.py` + `p*.py` for two clients);
health with `data get`, speeds and heights with `trace`, sounds with `heard`. Screenshots in `run/screenshots/` (`r_*`,
`r2_*`, `k_*`, `k2_*`, `f_*`, `iton_*`, `pk_*`, `em_*`, `vl_*`, `b_*`, `env_*`, `boat_*`, `n_*`, `nn*_*`, `nether*_*`,
`g_*`, `qa_*`, `h_*`, and `sheet_*` of them) and `run2/screenshots/` (`v_*`, the second player's), each looked at.

**Every way in.** The new world's select screen: his portrait in TANK (GUI 109 216), "TANK STALWART, 350 HEALTH, 250
SHIELDS", five kit keys, READY: 48 health, 1.40 x 2.50, eye 2.06, 650 barrier (`r_select_sigma`, `r_join_hud`). H at the
bed: Kiriko (20), then H again: Sigma (48), "select" line each time. `/kill`, Respawn: Sigma in hero mode at 48, the
respawn line. Save and Leave, `openworld`: the select screen with Sigma chosen and "ESC KEEP HERO" (`r_rejoin_select`),
READY: Sigma. On `sig12n` from its select screen. After each client restart (Save and Leave first) Sigma was kept.
- **Found (fixed, 9eef40d): respawning at the bed laid him down.** 3 of 3 `/kill` + Respawn put him beside the bed (-8.5
  -60 2.5) in Minecraft's swimming pose: box 0.60 x 0.60, camera 0.40 m, his model lying on the ground (`r_respawn_3p`),
  until he walked away; a `/tp` into the same spot alive did the same. Minecraft places a respawn by its own 0.6 x 1.8 box
  and his kept 1.4 m box overlapped the bed. `HeroBody.update` now keeps his box only while his crouched box fits where he
  is: after the fix (fresh client) 3 of 3 respawns STANDING 0.60 x 1.80 on client and server, his box back after 0.8 s
  walking off (`r2_respawn_3p`); the door (stopped at z 19.30, through in Minecraft mode) and the tunnel (stopped at 9.30;
  G inside: Minecraft's box, 48 health; walked out: his) replayed the same.

**His kit** (a 400-health NoAI husk 12 m off, 2 armour; night; first and third person, `sheet_k1`, `sheet_f`):
spheres 400 -> 381.31 (4 direct, 18.69 through armour), one volley 9.34; the rock 9.45 (80 + 40 through armour), gather,
throw, fly-by, flight loop, `hit_enemy`, `impact`; the barrier out (deploy, send, move, stop, hum, "Barrier
initialized"), a bow skeleton's arrows on it 650 -> 450 in 8 s with Sigma at 48, recall (`stop`, `recall`, `return`, "Return
to me"); Grasp on three summoned arrows: 7.2 overhealth, `absorb`, `overhealth`; Flux on four husks: the reticle rings,
the column, all four 400 -> 196.064 (4 + 200), `start`, "What is that melody?", `flight`, `aim`, `cast`, `lift`, `slam_3p`,
`slam_hit`, `slam_land`, `end`. (A bow skeleton didn't fire inside two 2 s Grasps: no arrows to take; the summoned ones
were.) **Flux aimed at a husk's body from the ground lands past it** (the ray meets the ground ~19 m out, and he rises 1.5
m in the start): aimed at its feet it lifts it, through his own barrier too. As in Overwatch, not a bug.
- **Perks** (`overcraft perks @s 3`, Left Alt + the right button twice, `pk_menu_*`, `pk_hud`): Hyper Regeneration: the
  barrier 450 -> 521 after 234 dealt past it (30 %: 70), `barrier_damaged` heard (the line steps 10-11 never heard);
  Levitation: jump held, 3.00 m/s up for 34 ticks, 6.38 m, 48 -> 48, its loop.
- **Emotes from the wheel** (T held, the right button, the pointer right / down / left): Maestro (60C0, 610B),
  Constellation (9DD9), Magic Pumpkins (81C1), the emote camera each time, each ended by W with nothing left playing; the
  voice page's "Hold it together" (`sheet_em`).
- **Improved Transparency on** (F3+X, options.txt `improvedTransparency:true`): spheres 1P and 3P, the barrier 1P, 3P and
  from in front with a husk behind it, Grasp 1P and 3P, the rock, Flux's reticle and lift 1P and 3P (`sheet_iton`): no dark
  boxes, the same look as off; F3+X again: options.txt ends `improvedTransparency:false` (checked again at the end).

**His body and Minecraft mode.** Door, tunnel and G as above. Minecraft mode on `sig12n` in survival from an empty
inventory: three oak logs punched (3.6 s each, picked up by walking over them), 12 planks and a crafting table in the
inventory grid, the table placed (held in his green first-person hand, `n_held_table_1p`) and used: a chest from 8 planks;
a dirt block dug with the fist; the chest placed (held in his hand 1P, `n_held_chest_1p`), the dirt stored, the chest
closed and reopened with the dirt in it (`sheet_mc`). Two rows of hearts (48).

**Water, lava, slopes, ladders, a boat, the Nether, a night.**
- Water: 5.5 -> 1.93 m/s walking in; jump held swims up; the barrier deployed from the water (its screenshot not looked at). Lava with fire resistance: 0.74 m/s,
  spheres (two new, `volley`) and the rock from in it, 48 -> 48.
- Ladder: W climbed 7.4 m; let go, he slides down at the ladder's speed, 48 -> 48 (2 of 2). (A first run read 48 -> 39:
  my script cleared fire resistance as he left the lava, and he burned.)
- The half-block ramp: hero mode 5.42 m/s to the top, Minecraft mode 4.12 m/s.
- Boat: from 3 m out F did nothing for him and boarded Kiriko (his eye is 0.44 m higher: the boat was past Minecraft's 3
  m interact reach for him, `boat_f3_58.5`); from the water's edge F boarded it. Seated: rowed 7.8 m in 2 s (Kiriko the
  same), Grasp, the rock, the spheres and the barrier all work seated; Flux cast seated (he stayed in the boat: ours, not
  refused as Sojourn's slide is); G seated: Minecraft mode, G again: his box, still seated; crouch: out, Minecraft's box
  until he stepped off (the fix above).
- The Nether: through a portal built on a stone platform (walked through it in hero mode, in his 2.5 m box), the
  arrival portal fits him; spheres, barrier (1P and 3P), the rock, Grasp, Flux in a basalt delta (`sheet_nether4`), back
  through the arrival portal. (Two first tries built the portal on the sea and he drowned: my site.)
- **A night** on `sig12n` (time running, monsters on, eight summoned with their AI on open ground plus the wild ones, 120 s
  of a scripted fight with real keys, `sheet_nn2`): volleys, barrier, Grasp when low, the rock every 10 s, Flux at 40 s
  (`ult.friendly`, the lift on those caught), G at 80 s and back; killed at 38 s (`death`), respawned in hero mode at 48,
  `multikill` heard, 3 of 8 killed. (A first run by a tree aimed into its trunk for 150 s: the script.)

**The general play: every other hero from H at the bed** (one fight each against a 400-health NoAI zombie in an iron
helmet and a 4-health NoAI villager: the left button, the right at the villager, E, Shift, Q + the left button; `sheet_g`),
with Sigma picked after Lucio, Lifeweaver, Brigitte and Sojourn (each time 400 -> 391.42 -> 382.34, 28 of his sounds):
Kiriko (zombie -22.5, villager 4 -> 13.6), Lucio (-18.5, villager to 20), Jetpack Cat (-13.2), Mercy (villager 4 -> 9.3),
Lifeweaver (the script's thorns missed; platform, Tree of Life seen), Ana (-25.3, villager -> 11.2), Sombra (-88.5,
villager -> 18), Brigitte (-5.6, villager -> 8.6, Rally's 28), Juno (-18.9, villager -> 14.8), Sojourn (-36.6). Each hero's
own sounds only, no missing-sound warning, their HUDs and weapons right.

**The QA audit's blocker (fixed, 223aa7f): his sphere-core glow on other heroes.** The cores were matched by submesh key
6B24EE2D2B083AFE alone, a key that untextured pieces of `lucio_weapon`, `sombra_weapon`, `ana_pistol`, `brigitte`,
`brigitte_shield`, `lifeweaver_platform` and `lw_flower_ring` share (read from the pack's .glb files). Reproduced (each
picked from H, 1P and front, noon and midnight, `qa_before_*`): Brigitte's barrier a magenta-white wall. Pieces now carry
their material's name and the cores need material 7C98 too; after (`qa_after_*`): Brigitte's barrier cyan-blue again,
Lucio, Sombra, Ana (pistol out), Lifeweaver (platform) unchanged, his own cores still violet in 1P (the emote copy D7DD
shares that path and material; not re-seen in a clear frame). The audit's other notes: his barrier health and Flux timer
at the reticle (Overwatch's reticle widgets) are **not done: left for #85**; Kiriko's Swift Step flash on Grasp
(`HeroHud.track`, being fixed on `next`) **not seen** at Grasp's end (`qa_grasp_end_a/b`), not watched at its cooldown's end.

**Two players** (port 25600 was taken by another agent's client at the first check, then free; host "Kiriko" as Sigma on
`sig12` opened to LAN, `connect` from runClient2 as "Viewer", Sojourn from her select screen; `overcraft team join red` /
`blue`; peaceful for the first round):
- **The ult -40 %:** Sojourn's railgun held 1 s into Sigma: 132 dealt, her ult +100.6 and +94.7 in 1.7 s; into Kiriko
  (switched from H): 86 and 131 dealt, +116.2 and +152.2. Less her passive (+10.8 in 2.2 s: about 8.3): 0.70 and 0.65 a
  point of damage on Sigma, 1.25 and 1.10 on Kiriko: **x0.57-0.64**, the 0.6 within the noise.
- **His barrier blocks her rounds from its front:** Sigma 48 -> 48, the barrier 650 -> 506 (`brigitte.hit_barrier`,
  `barrier_damaged` heard on her client); from its back (she behind it, Sigma before it): Sigma hit (48 -> 37.8), the
  barrier untouched, as first built (one-sided, the plan's front). **Fixed after (8cec2ca): both sides**, below.
  Seen from her side in the enemy colouring, a warm yellow-orange field (`v_barrier2`). (A first try read no block: the
  barrier wasn't in her line, sent too short.)
- **Grasp eats her rounds:** 48 -> 48, 5.62 overhealth; on her client `absorb_3p`, `absorb_shooter`, `start_3p`,
  `loop_3p`, `overhealth_3p`.
- **Accretion knocks her down:** her speed 0 with W held for 1.1 s, then 5.5; 18 -> 15.6 (peaceful: its burst's explosion
  damage scales to 0 and she regenerated; not a clean number); `gather_3p`, `throw_3p`, `hit_enemy_3p` heard.
- **Found: Flux didn't lift her** (first fix b1a7099 didn't work; the real one dcf800e, below). Caught (`lift`, `lifted`, `slam`,
  `slam_hit_3p`, `ult.enemy` on her client; 18 -> 9), but her own client stayed at y -60.00 for 4 s. The lift sets her
  speed with `needsSync`, which 26.3 sends only to the others watching ([minecraft_26](../minecraft/minecraft_26.md)); now
  `syncVelocity` too, also for the rock's and Hyper Strike's push and the spheres' pull. Rebuilt and both clients started
  again, but another agent's third client had left 1.2 GB free and the restarted second client hung loading.
- She heard his `_3p` copies (barrier, Grasp, rock, Flux) and his voice lines. **Not done with two players:** her view of
  his spheres in flight, the rock, Flux's column, his emotes (and their "others" sounds); Kiriko's kunai into his barrier;
  the spheres' pull and Hyper Strike on a player; a clean Accretion damage number on normal.

**Finishing step 12 (the coordinator's three items), 2026-11-02 evening.** Free memory checked before each start (the
other agents' clients came and went: twice the second client timed out or hung while five clients shared the machine;
waited until only mine ran); runClient2's harness moved to 25610 (`OC_DEV_PORT2`, 723190c) and the LAN game to 25577,
another checkout holding 25600 / 25565.
- **His barrier blocks enemy fire from both sides (8cec2ca).** Overwatch's do: the wiki (Weird Gloop, MediaWiki API)
  Barrier rev 406358 "Barriers only affect the opposing team"; Winston rev 408465: an enemy inside the dome "can still hit
  Winston or his allies if they are also inside", so "move outside of it so that the enemy has to exit the bubble to
  attack you"; Sigma rev 408635: "dance around your Experimental Barrier to block his shots". No side flag in his data
  notes. `Barriers.crossing` (projectiles, hitscan, blasts) and Brigitte's `Barrier.crosses` (same rule, shared through
  `Barriers` on `next`) take either side now. Played (sig12, normal, a bow skeleton held still): Sigma on its front side,
  the skeleton on its back: 48 -> 48, the barrier 650 -> 513 (`bb_back_front`, `bb_back_1p`); from its front 48 -> 48,
  650 -> 500 (`bb_front_3p`); no barrier, the same skeleton: 48 -> 41.2; Brigitte's shield from the front 20 -> 20. Two
  players: Kiriko's kunai into his barrier: Sigma 48 -> 48, the barrier 650 -> 470, `hit_barrier` on her client, the field
  in the enemy's warm colouring (`v8_kunai_barrier`). Not played: Brigitte's shield from behind with an ally before it.
- **A pushed or lifted player now moves on her own client (dcf800e).** b1a7099's `syncVelocity` changed nothing when
  played (vy 0.00 on her client every tick). Two causes: the server's copy of a player's speed is reset by her own
  movement packets, so Flux reading it back never built a rise; and the hit's own `syncVelocity` made the tracker send that
  stale copy a tick after the push. `Accretion.sendSpeed` now sends the motion packet at once and clears the flag; Flux
  keeps a lifted player's speed itself (`Lifted.speed`). On normal difficulty, natural regeneration off, read on HER client:
  **the rock** 18 -> 8.40 (120: 80 + 40), knocked 5.97 m/s along its flight with a small hop, held 1.1 s (W does nothing),
  then 5.5 m/s; **Flux** lifted her from -60.00 to -55.78 (4.2 m; a husk rises 5.5-6 m: her client's own gravity acts
  between the packets) and slammed her back, 18 -> 5 (lift 50 + half of 225); **the spheres' pull** vy 7.97 on her client
  (before: 0.00), 18 -> 13.2. A husk after the change: lifted 5.5 m, slammed, 400 -> 196.064, as before.
  **Not fixed: Hyper Strike on a player** deals its 90 (18 -> 10.8) but its 14 m/s knock is eaten by her client's ground
  friction (3.23 m/s for a tick) and its 0.8 s low-gravity float is the server's for mobs only.
- **Her view:** the rock coming (`v8_rock_incoming`), Flux's rings and column with a husk lifted beside her
  (`v8_flux_column`; `cast_3p`, `lift`, `slam_3p`, `slam_hit_3p`, `slam_land`, `ult.enemy`), herself lifted
  (`v8_flux_lifted_*`, `h8_flux_lift_3p`), his Maestro (`v8_emote_maestro`; 60C0 and 610B heard on her client), the
  spheres' `volley_3p`, `burst_3p`, `impact_3p` (`v8_spheres_incoming`: he drew all red in that frame, not looked into).
- **The inventory doll (2b4a654):** framed by his own height (scaled 1.8 / 2.5, centred on half of it): his whole body
  in the frame in Minecraft mode (`doll_sigma_mc`), Kiriko's unchanged (`doll_kiriko_mc`).

**Not bugs, written down:** Flux's flight does nothing seated in a boat (ours).

**After the independent critique** ([critique_sigma](../testing/critique_sigma.md), 33 findings, 2026-11-03).
- **Integrated first:** `fix/killfeed-hud` (fd827ae) and `fix/brigitte-shield` (a0c1fe9) merged into this branch by rule, not
  by side. Every conflict keeps the kill feed's `Blow` and his `credit(..., target)`. His damage is tagged:
  - spheres: weapon (arrow);
  - Accretion: `ability:accretion`, with `Combat.pushed` after its push;
  - Flux's lift and slam: `ult:gravitic_flux`, pushed at the lift;
  - Hyper Strike: `perk:hyper_strike`.
  One set of barrier rules for both barriers:
  - enemy fire is stopped from both sides;
  - a hit that breaks a barrier stops whole;
  - only marked melee (`Barrier.meleeDepth`) goes through, for his barrier too (`MELEE_REACH` is gone);
  - Soundwave, Purr and Catnapper pushes stop at any barrier (`Barrier.shields` asks `Barriers`);
  - her explosion mixin carries Stalwart's push scaling and `Barriers.blast(..., hurts)` (a wind charge charges nothing);
  - her absorb and blocksBlast ask `Barriers` first.
- **Blockers 1-5 and the rest (5afc4a9):**
  - hitscan is judged from the shooter's eyes and absorbed by Grasp (only hits with no direct entity: area hits pass);
  - `barrierDamage()` for every damaging projectile;
  - strong projectiles fly through Grasp; the Translocator passes barriers; Petal Platform bounces off one;
  - Flux goes through every barrier and never hurts one (`Barriers.piercing`);
  - a hack recalls the barrier and cuts Grasp, the gather and Flux;
  - asleep, stunned or knocked down, the barrier stops in place and stays out;
  - Accretion and Flux cancel Grasp;
  - no spheres, Grasp or rock while he pushes the barrier;
  - `HeroBody.makeRoom` (G in a doorway, a respawn beside a bed): moved to room within 2 m, or kept in Minecraft mode
    with "No room for Sigma here", and the client follows the server's hero mode after 0.5 s;
  - Levitation at W's 2.3 m/s.
- **Played** (sig12, normal difficulty, host Sigma and the LAN guest as each hero from H, red vs blue, through the
  harness; desktop control was denied, so not real input):

  | Viewer's hero | Into his barrier (Sigma 48 -> 48 every time) | Into Grasp (48 -> 48, overhealth at its end) |
  |---|---|---|
  | Sombra | barrier 650 -> 514 | 6.55 |
  | Juno | 650 -> 471 | 8.64 |
  | Ana (rifle and scoped) | 650 -> 501 | scoped: 3.60 = 45, 60% of 75 |
  | Lúcio | 650 -> 474 | 4.22 |
  | Lifeweaver | 650 -> 506 | thorns: 5.18 |
  | Jetpack Cat | 650 -> 506 | 6.91 |
  | Mercy (blaster) | 650 -> 490 | 5.76 |
  | Sojourn | 650 -> 533 | 3.89 |
  | Kiriko | 650 -> 590 | 5.76 |

  - **Strong and pass-through projectiles:**
    - another Sigma's rock went through his Grasp, 48 -> 38.4;
    - Sombra's Translocator flew through his barrier: she went from z 16.5 to 3.8, past the barrier at 10.8;
    - Petal Platform bounced off it and opened on her side.
  - **Hack:** Sombra's EMP from 8 m recalled the barrier within 0.25 s and cut Grasp.
  - **Sleep:** Ana's Sleep Dart while he sent the barrier stopped it in place, and it stayed out.
  - **Flux vs an enemy Brigitte:** with her shield up, her barrier stayed 300 -> 300. She was lifted and slammed
    (20 -> 7.12), and the husk behind her went 400 -> 196.06.
  - **The door rule:** G in the 1-wide doorway moved him to z 19.09 with his box, and W stopped at 19.30. G in the
    2-high tunnel was refused with the message (`h6_tunnel_g`). Respawning at the bed moved him 0.25 m off it, with his
    box and hero mode on.
  - **Grasp and the barrier:** E in Grasp threw the rock with no overhealth; no volleys fired while sending (one with
    it standing).
  - **Levitation:** 5.07 m.
  - **Kill feed:** the spheres show the arrow, the rock its icon, Flux the ult icon (`sheet_kf`).
  - **A creeper 1.5 m beyond his barrier:** 48 -> 48 with the barrier 650 -> 293; without it, 48 -> 46.2.
  - **Not reproduced on the old build first** (the critic read the code; each was played only after the fix).
  - **Not played:** Soundwave, Purr or Catnapper at his barrier; Brigitte's shield from behind with an ally; Flux
    through an enemy Sigma's barrier; Biotic Grenade at it (it's taken whole, no burst: ours).
- **Screenshots the critic found wrong:**
  - `v8_spheres_incoming` is the viewer's own hurt Sojourn, not him;
  - `env_water_barrier` shows no barrier;
  - `v8_flux_column` shows the ring but no column from the enemy side;
  - `v8_emote_maestro` is too far to judge.
  Her view of his spheres in flight is still unseen.

## Not yet
Hyper Strike's knock and float on a player; a lifted player rises 4.2 m against a mob's 6; the critique's 11 (Flux,
Accretion and Hyper Strike can save mobs NoAI), 14 (lifted players keep their abilities after 0.4 s), 17-22 (splash rules,
the slam and temporary health, Accretion's self-damage, the barrier's shape and the void, blasts through walls, Grasp's
box to his feet), 23-28 (screenshots, the enemy barrier's colour, the sleeve, the reticle and Flux's 1P look), 29-30
(late joiners, an ally's shots) and 31-32 (real input, earned perks and ult); the merge with `feature/dva` (#8); his
reticle widgets (#85). Open from steps 7-9: Flux's right-button confirm and "rise with ability 3" options, its
HUD timer, Hyper Strike's charge counter; the rock leaves his eyes (the data's hp 01B6 on his right side: it would
miss the crosshair); a player knocked down, lifted or struck (one client: their lift is their speed sent each tick
and their keys held 0.4 s, untested); the slam's cap on bosses and Accretion's 12-at-the-edge falloff (ours: Open);
the lift's 18 / 3 / 5 fields and Levitation's 25 / 3 read as accel and cap (M: a Workshop recording settles them). Open from steps 4-6: the barrier's Toggle / Reload-recalls options; the barrier's motion fields
1.75 / 2.0 / 4.5 (it goes into the ground when sent downward: M); a blast that breaks the barrier still reaches those behind
it (the break comes first); the 3P fire clips (the clip map holds only alias D5's three pitches: the volley plays fire_b,
the first person cycles fire_a / b / c); the hand-held orbs on his hp 02FD-0300 while the barrier is with him. His emotes: step 11. A Workshop recording (Levitation's rise, Flux flight, the barrier's
glide over slopes) would settle the M items.

## Weapon parts, glows and aim (2026-11-04, #91, #95)
General: [weapon_parts](../animation/weapon_parts.md), [aim_sheets](../animation/aim_sheets.md), [glows](../rendering/rendering.md#glows-emission-from-the-data-2026-11-04-91). What a critic should check is there.

## Hyper Strike reaches a player (#109, #85, 2026-11-05)
The float (gravity x0.01) now goes to a struck player's client as the gravity attribute, and its landing doesn't hurt (aimed up it reached 18 m and killed a Brigitte); its count shows at the reticle: [polish3](../testing/polish3.md).
