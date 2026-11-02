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

## Not yet
Steps 4-12 of the plan. Effects, sounds, voice (set 1E5.05F, 1293 subtitled
lines extracted) and his 14 emotes are extracted but not mapped. A Workshop recording (Levitation's rise, Flux flight, the barrier's
glide over slopes) would settle the M items.
