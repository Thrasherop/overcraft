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

## Not yet
Steps 11-12 of the plan. Open from steps 7-9: Flux's right-button confirm and "rise with ability 3" options, its
HUD timer, Hyper Strike's charge counter; the rock leaves his eyes (the data's hp 01B6 on his right side: it would
miss the crosshair); a player knocked down, lifted or struck (one client: their lift is their speed sent each tick
and their keys held 0.4 s, untested); the slam's cap on bosses and Accretion's 12-at-the-edge falloff (ours: Open);
the lift's 18 / 3 / 5 fields and Levitation's 25 / 3 read as accel and cap (M: a Workshop recording settles them). Open from steps 4-6: the barrier's Toggle / Reload-recalls options; the barrier's motion fields
1.75 / 2.0 / 4.5 (it goes into the ground when sent downward: M); a blast that breaks the barrier still reaches those behind
it (the break comes first); the 3P fire clips (the clip map holds only alias D5's three pitches: the volley plays fire_b,
the first person cycles fire_a / b / c); the hand-held orbs on his hp 02FD-0300 while the barrier is with him. His 14 emotes are extracted but not
mapped (effects, sounds and voice: step 10). A Workshop recording (Levitation's rise, Flux flight, the barrier's
glide over slopes) would settle the M items.
