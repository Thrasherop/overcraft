# D.Va: the plan

Numbers: [dva.md](dva.md) (BZ, W and D per number) and the game data map [dva-map.md](dva-map.md) (D, with its detail in
`E:\Overwatch Archive\research\dva\work_kit\kit_dva_mech.md` / `kit_dva_pilot.md`) -> `DvaStats`, each number with its source. Branch
`feature/dva` in its own worktree (issue #69, hero 8 of 12, after Sigma #68). Written 2026-11-02 from build 154088 (2026-10-06). **She is
the second Tank: she builds on Sigma's branch** (`feature/sigma`, `E:\claude\mods\overcraft_sigma`: [sigma-plan.md](sigma-plan.md) and
[techniques/heroes/sigma.md](../../techniques/heroes/sigma.md)), so her code starts after Sigma's merges into `next`, or from his branch
(Open below).

## Her role and the patches
- **Tank, Initiator** (D 0347 / 0DA7, tag 116D; BZ 2026-02-10): the second Tank, **the first Initiator** (Stalwart is Sigma's). The role's
  parts already built on Sigma's branch apply unchanged: `Tank.BONUS_HEALTH` (+150), `Tank.ultFactor` (enemies x0.6), `Tank.tank(hero)`. Not
  hers: Stalwart's knockback / slow resistance (`Tank.stalwart` is false for "initiator"). New: **Initiator** (below).
- **Her last live change is BZ 2026-10-06** (missiles 11 -> 14 a second), and **the data has it** (1128 reads 0404 = 14): the data's numbers
  are the live ones; where the public text disagrees, D decides, played.

## Where the data and the public sources disagree (ours in bold)
- **Eject immunity:** BZ 0.4 s, W 1.8 s; D **0.4 s** (026A #880). The 1.0 s in the wreck before it is untargetable too (D, M).
- **Self-Destruct minimum:** W 100; D **50 at 21 m** (linear from 1000 at 6 m).
- **Call Mech:** lands at **0.9 s** (W 0.896), she drives at **1.95 s** (W 2.93); radius **3 m** (W's history 2.5); Bunny Power **4.5 m falling
  off from 3 m** (W flat); placement **ground within 2 m**, a failed one costs nothing (W "must be grounded").
- **Precision Fusion:** **3.1 s, cooldown 7.5 s** (W 3 / 8).
- **Missiles' start:** **0.25 s** (W 0.16).
- **Fusion floor:** W 0.6 a pellet (x0.3); D's floor var unwritten in the graphs read: **W's 0.3** (0303 0.3 likely the same, L).
- **Self-Destruct knockback:** D 20 / 10 (22.4 m/s) at the centre, ring factors 0.75 / 0.75 / 0.25 at 21 m (axis order M); W 7.8 -> 22.4.
  **Ours: 22.4 at the centre, linear to 7.8 at 21 m** (W's minimum, until the rings are read: Open, M).
- **Settled by D, no choice left:** 200 + 150 + 325; pilot 175; Fusion 11 x 2, 10-20 m, 0.15 s, 3.375°, -30 %; matrix 100 / 33 / 16 / 0.75 s /
  1 s / 10.25 m / 0.5 s minimum; Boosters 12 m/s, 0.4-2 s, 4 s, 25, 17 + 4, -90 % knockback; missiles 18 at 14/s, 2 + 5.5, 0.1-1.5 m (x0.25),
  50 m/s, 7 s; Self-Destruct 1700, 3 s, 1000 within 6 m; Light Gun 14, 7/s, 20, 1.4 s (full at 0.91 s), 50 m/s, 0.275 m; Call Mech 280, 250
  within 3 m, 15 + 3; Initiator 50 over 1 s, 5 s, not at full; perks (dva.md).

## What D.Va needs that Overcraft doesn't have yet
1. **Two bodies (the big one).** In the data the hero is the pilot and the MEKA a pet entity she drives (D 0407; 026A #98). Overcraft has no
   vehicle, no rider, no second health pool. **Ours: one player, two forms** (`HeroState.dvaForm`: MECH / PILOT / CALLING / WRECK), the
   player *being* whichever body she's in (as the data's link does: control and camera to the mech, the pilot hidden and untargetable),
   plus a **mech entity only while it's unmanned** (`DvaMechEntity`: falling in on Call Mech, the wreck, the Self-Destruct mech flying
   on). Not Minecraft's riding: it would split the hits, the camera and the input between two entities, which the data's link exists to
   avoid. Per form: the body box, eye, health pool, ult meter, keys, model, clip maps, HUD.
2. **Per-form health.** `Hero.owHealth` is one number per hero. Hers: **mech 200 + 150 + 325 armor = 675** (54 Minecraft health, 27
   hearts), **pilot 175** (14 Minecraft health). Each body keeps its own: entering a mech gives the full 675 (a new mech each call, D); the eject gives
   the pilot a full 175 (D 026A #97's 9999 heal). Damage past the mech's 0 doesn't pass to the pilot (W; D: the eject is a new state).
   Needs `Hero.owHealth(player)` / `layer(player)` read from the form wherever the fixed numbers are read today (Shields, HeroManager's
   max-health, the HUD's pip row: 27 hearts for the mech is the most yet; check the row fits).
3. **Death that isn't death.** The mech's 0 is caught like a totem (LivingEntityMixin's lethal-damage path): no death, the WRECK form for
   **1.0 s** (frozen in place, untargetable (D: the pilot already is), Q = Self-Destruct if charged), then the **eject**: the pilot's form,
   a launch **6 m/s back + 7 up** from the mech's facing, **0.4 s invulnerable**, Call Mech locked 1 s, its charge 0, Light Gun full. Only the
   pilot's 0 is a real death. Bunny Power's 75 overhealth on the eject: `Combat.overhealthPool` (10 s hold, 25/s: M).
4. **Two ult meters.** Self-Destruct's charge (1700) belongs to the mechs and carries from one to the next (D 0276 #88 / 026A #775, M);
   Call Mech's (280) to the pilot, from 0 at each eject, +5 a second while a pilot (W; D: engine) plus damage. `HeroState` has one meter:
   add the second (`dvaMechUlt`), the HUD shows the current form's.
5. **The Initiator sub-role** (new): Boosters' start heals **50 over 1 s**, not at full health, then a **5 s** lockout (D 01C7 #2539 / #2857 /
   #2889). `Tank.initiator(hero)`, a heal-over-time (`Combat` has heal ticks: reuse Lifeweaver's / Juno's), its own cooldown in `HeroState`.
   The next Initiators (Winston, Doomfist, Wrecking Ball's movement abilities) reuse it with their own trigger.
6. **Armor and shields together** (the Shield System perk: 100 health -> 100 shields on top of 325 armor). Overcraft's `Shields` keeps one
   layer, "no hero has both". The order, top down (W Hit points): **overhealth, shields, armor, health**; shields regenerate (30/s after 3 s),
   armor never. Without the perk her layer is armor only (Brigitte's code path: each hit 7 smaller, at most halved: `Shields.armorReduce`).
7. **A projectile eater that is a box ahead of her** (Defense Matrix): Sigma's Kinetic Grasp is the nearest (on his branch, being built:
   `sigma/KineticGrasp.java` with `Barriers.takes` / `Barriers.projectileDamage`, uncommitted there on 2026-11-02). Generalise its absorb
   box into `Barriers.absorbBox(player, box, onEaten)`: Grasp's 2 x 2.7 x 0.4-2.6 m, the matrix's **2.5 x 2 x 0.8-10.25 m** along her aim,
   eaten projectiles removed with their damage counted (Grasp's overhealth / the matrix's 4 % ult and Shield System's 25 %).
8. **A hitscan shotgun.** Sombra's pistol is hitscan with a cone (`Sombra.hitscan`, `cone`); the Fusion Cannons are 11 rays a volley (1 in
   the middle, 4 at half the cone, 6 at its edge: D #444) every 3 ticks. **Sum a volley's pellets per target into one hit** (Minecraft's
   hurt cooldown would drop all but the first) and give armor (Brigitte, Sigma later) the per-pellet 7 (a 2-damage pellet on armor does 1: W).
9. **Flight along the aim** (Boosters): `OwMovement.dash` (a velocity hook) at **12 m/s along her look, no gravity**, 0.4-2 s, knockback taken
   x0.1, bumping each creature once (25 + 17 / 4 m/s), turning with the mouse (no turn-rate limit found: D).
10. **Her real size** (the owner, 2026-11-02, on Sigma: "unable to fit through a door; that's funny. And the camera height should match"):
    `HeroBody` keyed on the hero; hers needs the form (mech box only in MECH / WRECK / CALLING after the link) and **no crouched box**.

## What maps onto what Overcraft has (Sigma's branch first)
- **Hero entry:** `Hero.DVA("dva", "D.Va", Role.TANK, "initiator", ...)` (colour from D's hero colour (0.988, 0.475, 0.741) = 0xFFFC79BD),
  `HeroKit` `dev.overcraft.dva.Dva`, `DvaStats` (each number with D / BZ / W), `DvaSounds` later; perk XP 1615 / 3940; four `Perk`s
  (`DVA_BUNNY_POWER`, `DVA_EXTENDED_BOOSTERS` minor; `DVA_SHIELD_SYSTEM`, `DVA_PRECISION_FUSION` major); the lang key
  `overcraft.subrole.initiator`. The select screen's TANK group gets its second portrait (Sigma's `groupWidth` fix covers it).
- **Models:** MEKA 3P **FBB0** (set 0282), MEKA 1P **10354** (019B), pilot 3P **F2FF** (048A), pilot 1P **F32B** (0476), Light Gun **10246**
  (048B) on the pilot's hp 000A. Clip maps `tools/clipmaps/dva.txt`, `dva_1p.txt`, `dva_pilot.txt`, `dva_pilot_1p.txt`, `dva_weapon.txt`.
  `tools/owm_survey.py --glb --keep=<clip map>` per model (Blender headless, Overwatch checked closed), as Sigma's. The pilot in the
  cockpit: the mech's hp 0012 Head (1.39 m) is where her head sits (M); 3P draws the mech model alone with the pilot model posed seated
  (category 0283, D 026A #832) at the mech's seat (find the attach hardpoint before drawing: Open), 1P the 10354 cockpit.
- **Movement:** both forms the stock mover (D: the default hero's but the mech's size). The mech: no crouch (the sneak key does nothing in the
  mech, ours: no Minecraft edge-guard either, M), 8-way move clip set (alias 5), the 8-way jump. The pilot: Minecraft's box as every other
  hero, crouch and crouch-walk clips. **Fall damage:** the default 3 m in both forms; none when the eject or Boosters lands her (CLAUDE.md #35).
- **Fusion Cannons (LMB, mech):** 11 hitscan pellets every 3 ticks (0.15 s), 2 each (0.16 Minecraft), falloff 10 -> 20 m to x0.3, cone
  3.375° (Precision Fusion x0.25), -30 % speed while held (none boosting), no reload, no ammo, crits x2 on the head; blocks not broken.
- **Defense Matrix (RMB, mech):** hold (Toggle option), up to 3.03 s, 0.5 s minimum, back at 16/s after 0.75 s, 1 s cooldown; no Fusion
  meanwhile; the box above; eats arrows, tridents, fireballs (ghast / blaze / dragon / wither skulls), shulker bullets, llama spit, potions,
  snowballs / eggs / pearls (0 damage), every hero projectile; not explosions, melee, guardian / elder-guardian beams, the warden's sonic
  boom (a beam), lightning, fire. 4 % of eaten damage to Self-Destruct; Shield System 25 % back as shields.
- **Boosters (LShift, mech):** as above; Initiator's heal on the start; Extended Boosters +0.5 s a hit (cap 2 s left), 35 damage.
- **Micro Missiles (E, mech):** after 0.25 s, 18 projectiles at 14 a second (an accumulator over 20 ticks: 0.7 a tick), from the shoulder
  pods (hp 007B / 007D / 007E / 0080, L) along her aim, 50 m/s (2.5 blocks a tick), 0.175 m, 2 on impact + 5.5 splash (x0.25 at 1.5 m),
  her own splash hurts her (W; ours: on), no knockback, no block damage; 7 s from the volley's end; usable with everything else.
- **Self-Destruct (Q, mech, 1700):** she ejects (the eject above) and a `DvaMechEntity` flies on with the mech's motion (no mover: it keeps
  its speed and falls), 3 s, then 1000 within 6 m -> 50 at 21 m to everything in sight of its centre (1.3 m up), her and her allies spared,
  knockback 22.4 -> 7.8 m/s; **no block damage** (ours, Overwatch's; Open); Call Mech full after the blast (not if it fell out of the world).
- **Quick melee (V):** 40 in both forms (`QuickMelee`).
- **Pilot: Light Gun (LMB or by option RMB / both):** projectiles (`OwProjectile`, as Juno's blaster), 50 m/s, 0.275 m, 14, 7 a second (an
  accumulator: 0.35 a tick), 20 rounds, reload 1.4 s (full at 0.91 s), crits x2, no falloff, 5 s life.
- **Call Mech (Q, pilot, 280):** 0.2 s cast (no move, no jump), the falling `DvaMechEntity` 1.5 m ahead (her spot if no ground there: a 4 m
  probe; ground within 2 m needed, else nothing spent), landing at 0.9 s: 250 within 3 m (Bunny Power 4.5 m, falling off past 3), knockback
  15 + 3; from 1.0 s she's untargetable and drawn to 1.27 m behind it; at 1.95 s the link: the entity removed, the MECH form at full 675.
  A stun (`Sleep`) before then: the mech removed, the charge spent (D onAbort, M).
- **Perks:** Bunny Power (eject overhealth 75, radius 4.5), Extended Boosters, Shield System (the 100 health -> shields), Precision Fusion
  (R in the mech: 3.1 s, rate x2, damage x0.5, falloff x1.5, cone x0.25, 7.5 s from the end).
- **Clips:** stand / 8-way move / 8-way jump for each form, Fusion's three-step fire (ABE1 -> AACD -> 13525) with its additive layer, the
  1P matrix arms (334E ..), Boosters (34BB -> 307C), missiles (additive 33B7 / 33B6 -> 33B8), Self-Destruct (3644 additive, 1P 33CF),
  destroyed (31C3), Call Mech (311F mech / 3120 pilot), eject (3382), reload (3234), melee, hello, select (6247 / 6249).

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **Body size (the owner's wish for tanks, applied to her):** the MEKA in hero mode is its mover's **1.4 x 2.3 m**, the camera at **1.74 m**
  (D 041F's first-person eye, M meaning; its model is 2.43 m tall, 2.62 wide with the cannons). **No crouched box** (it can't crouch): it
  fits no 1-wide door and no 2-high tunnel; a 2-wide, 3-high opening, yes. The pilot: Minecraft's 0.6 x 1.8 (her mover is the default
  hero's, like every non-tank in Overcraft). The box grows only where it fits (HeroBody's rule): **Call Mech's spot needs room for the
  mech box**, else the call fails with no cost (as the data's failed placement); in a tunnel she stays a pilot.
- **The two bodies as one player** (above): the mech is never a separate entity while she's in it; mobs target the player either way; the
  unmanned mech (calling, wreck, Self-Destruct) is a non-living entity mobs ignore (ours, M).
- **G (Minecraft mode) in the mech:** she leaves hero mode as the pilot would (the mech gone, its health and Self-Destruct charge kept for
  the next G back, ours, M: no mech to leave behind in Minecraft mode); Minecraft health is the pilot's while in Minecraft mode. Open.
- **Dying as the pilot** is death; the mech's 0 never is (the eject). **Respawn:** in her mech (D 026A #763: a respawn link at her feet,
  no drop).
- **Self-Destruct and blocks:** no block damage (Overwatch's), fire none. On bosses: the full 1000 (80 Minecraft health: the Wither's
  300 and the Ender Dragon's 200 survive it), uncapped, unlike Sigma's Flux (it's damage, not a share of max health).
- **What counts as a "hero"** (Extended Boosters' hits, Initiator's not-at-full check, the ult's 4 %): every living creature and player, as
  Sigma's plan.
- **Camera:** first person by default (Overwatch's "Default"); the option "Camera perspective while in the MEKA" (First / Third) as a hero
  option, ours wired to Minecraft's F5 view.
- **Keys:** Fusion LMB, matrix RMB, Boosters LShift, missiles E, Self-Destruct / Call Mech Q, Precision Fusion R (the mech only), Light Gun
  LMB (option: RMB / both), as Overwatch's defaults; options: Toggle Defense Matrix, Hold To Use Boosters, Light Gun Input.
- **The +150 on the pilot:** no (BZ / W "mech only"; D doesn't show the gate: Open).

## Open questions (each with its default)
1. **Build on Sigma's branch or wait for his merge?** Default: start her branch from `feature/sigma` once his Kinetic Grasp step is committed
   (the matrix reuses its absorb box), and rebase onto `next` when he merges.
2. **Where does the pilot sit in 3P?** The attach point isn't read yet (026A #95 / #99's hardpoint). Default: the mech's hp 0012 Head minus
   her own head height, posed with category 0283's clip; read the node's hardpoint before drawing.
   **Settled 2026-11-02 (step 2): not drawn.** The MEKA model already has her hands on its controls and the data hides the
   pilot entity while linked (026A #794); drawn at #99's attach (no hardpoint, no offset) her legs hung under the mech.
3. **Fusion damage floor:** W 0.3 x (0.6 a pellet) until 3361 #132 is shown to feed v37674.
4. **Self-Destruct knockback falloff:** linear 22.4 -> 7.8 m/s (W's minimum) until the ring fields are read on a second ring-using ability.
5. **Missiles' self-damage:** on (W), the flag not found in D.
6. **Defense Matrix's ult share** (D #255's 4 %): read as ult charge (M); default on.
7. **Self-Destruct charge across mechs** (and its 5/s while a pilot, M): default: carried, not growing (the growth's reading is the weakest).
8. **Call Mech's passive 5/s** (W; engine side in D): default 5/s plus damage dealt as the pilot.
9. **The mech in Minecraft mode (G):** default: no mech, the pilot's health; G back puts her in a mech at the health it had.
10. **Ladders / water / boats / minecarts:** the mech climbs ladders (a hero does: ours) but enters no boat / minecart (too big: ours); swims
    as a hero does (no OW rule). Default as written; play will decide.
11. **Hit volumes:** D's mover box (1.4 x 2.3) is also her hit box; Overwatch's mech hitboxes aren't in her graphs (engine). Default: the box.

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. **The two-body shell:** the forms, per-form health / layer / ult meter, the mech's death caught (wreck 1 s, eject), Call Mech's drop and
   link, the mech box (1.4 x 2.3, eye 1.74, no crouch), the Initiator sub-role; unit tests for the math; the select screen's second tank,
   the HUD in both forms (the mech's 27 pips: 14 health, 13 armor; the pilot's 7).
2. Hero entry, models (MEKA 3P / 1P, pilot 3P / 1P, Light Gun), clip maps, the pilot drawn in the cockpit: pick her, look from every side,
   both forms, both views; eject and Call Mech by key.
3. Movement: both forms (8-way move, jump, the mech's size in doors and tunnels, the pilot's crouch), fall damage, the eject's launch.
4. Fusion Cannons on zombies, skeletons, a group: the pellets summed per target, falloff, the slow, armor on another tank later.
5. Defense Matrix: skeleton arrows, blaze / ghast fireballs, potions from a witch; not a creeper's blast or a guardian beam; resource and regen.
6. Boosters: flight, bumping mobs, walls, water, Initiator's heal; with missiles and the matrix during it.
7. Micro Missiles: the volley's rate, splash, self-damage, cooldown.
8. Self-Destruct: from the mech and from the wreck, the 3 s, falloff, line of sight, Call Mech charged after; Call Mech: placement, the
   landing hit, stun aborts, no room for the mech.
9. Light Gun and the pilot's fight: ammo, reload, crits; dying as the pilot.
10. Perks (Bunny Power, Extended Boosters, Shield System, Precision Fusion).
11. Sounds, voice (1129 subtitled lines) and effects from her data. 12. Emotes (15) from the data. 13. Two players (the ult -40 %), a night,
    the general play; release.
