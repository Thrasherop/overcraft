# Sigma: the plan

Numbers: [sigma.md](sigma.md) (BZ, W and D per number) and the game data map [sigma-map.md](sigma-map.md) (D, with its detail in
`E:\Overwatch Archive\research\sigma\work_kit\kit_sigma.md`) -> `SigmaStats`, each number with its source. Branch `feature/sigma` in its
own worktree (issue #68, hero 7 of 12, after Sojourn #2). Written 2026-11-02 from build 154088 (2026-10-06).

## His role and the patches
- **Tank, Stalwart** (D 0347 / 0DA5, category 0850, tag 116B; BZ 2026-02-10): **the first Tank in Overcraft.** `Hero.Role.TANK` exists
  (the select screen already groups by role and has the `overcraft.role.tank` string and `HeroArt.role` badge path) but nothing uses it,
  and nothing in the mod reads a hero's role for gameplay (`Combat`, `Shields`, `LivingEntityMixin` key on the hero or the sub-role
  string, as `Combat.tactician`). Sub-role key `overcraft.subrole.stalwart` is new.
- **No kit change since the data build** (the last, BZ 2026-07-14: shields 250, Hyper Regeneration 30 %, is in build 154088; 2026-10-06
  had no Sigma line), so the data's numbers are the live ones; where the public text disagrees, D decides (it is newer than the lines it
  contradicts), played.

## Where the data and the public sources disagree
- **Barrier regeneration:** D 0.122 of its max a second = **79.3/s** at 650 (BZ's 85 was 0.122 x 700, before the 2026-02-10 cut; W
  Sigma has 79.3). Ours: 79.3.
- **Hypersphere speed:** D **50 m/s** (W 48). **Volley spacing:** D **0.2 s** (W 0.192). **Burst:** D full to 1 m, **x0.5 at 2.85 m**
  (W 40 -> 12 at 2.9 m): ours D (the ring factor read as the damage there, M).
- **Flux radius:** D **7.5 m** (W 8). Ours 7.5.
- **Accretion's burst falloff:** D's two rings both carry factor 1 (40 out to 2.5 m); W 40 -> 12. Ours: D's flat 40 to 1.5 m, then
  linear to 12 at 2.5 m (W's minimum) until a recording settles it (Open, M).
- **Hyper Strike knockback:** D **14 m/s** (W 15). **Levitation:** D up to 3 m/s (W 2.3 m/s, about 5 m): ours D, Open until a Workshop
  recording (as Juno's) measures the rise.
- **Barrier size:** D's mesh 6.70 x 4.18 m (W 6.5 / 5 x 4.5). Ours: the mesh's (the shape its model draws), M.
- **Settled by D, no choice left:** 200 + 150 + 250; ult 2300; Grasp 2 s / 60 % / 400 / 12 s from the end, overhealth decaying after 3 s;
  Accretion 80 + 40, 1.1 s, 37 m/s, 18 m/s², 0.65 s cast at -75 %, 10 s; Flux 5 s / 35 m / 1 s / 2.5 s / 50 / 50 % / 0.6 s / 7.15 m/s /
  -50 % knockback; barrier 650 / 20 m/s / 1 s / 5 s; tank ult -40 %; Stalwart 40 % / 40 %; healing reduction 15 % on tanks.

## What the Tank role needs that Overcraft doesn't have yet (the first tank builds these for every later one: D.Va is next)
1. **`Hero.Role.TANK` gameplay**: today the role is only a label. Add `Hero.tank()` (role == TANK) and read it in:
   - **Role Queue bonus health +150** (01C7 #1106): Sigma's `owHealth` = 200 + 150 + 250 = 600 (48 Minecraft health, 24 hearts:
     the HUD's pip row already scales its pips to a fixed length; check 24 pips fit). No Role Queue in Minecraft: ours always on.
   - **Enemy ult charge -40 %** (x0.6) on damage and healing done to a tank: `Combat.credit` / `addUltCharge` know only the attacker.
     Add the victim: `Combat.credit(hero, points, victim)` scaling by 0.6 when the victim is a tank hero (also perk progress, W). Mobs
     never charge an ult, so it matters for players fighting a tank player (and any future PvP).
   - **Healing reduction 15 %** (the global 30 % halved): if Overcraft has the global anti-heal on hit, halve it for tanks; if not, both
     are new (Combat has `antiHeal`, Ana's grenade's; the global one is not implemented: the plan for it is the role pass's, M).
2. **Stalwart (new sub-role):** **40 % knockback resistance** and **40 % slow resistance**.
   - Knockback: `LivingEntityMixin.overcraft$flightResistance` already scales knockback per hero state (Mercy, Juno, Jetpack Cat):
     add `x0.6` for a Stalwart, multiplying with Flux's own x0.5 in flight. Minecraft's own `Attributes.KNOCKBACK_RESISTANCE` (0.4) is
     the alternative, but it is a chance-free linear factor only for some knockback paths (explosions read it differently): ours the
     mixin, so every source (mobs, arrows, blasts, other heroes) goes through one place.
   - Slows: `Combat.setModifier` (Local Anesthetic's slow), `Mover.slowed`, Deceleration Field (Sojourn's branch), Juno / Brigitte's
     self-slows: a slow *received from others* is scaled x(1 - 0.4); his own (Accretion's cast, Flux's heavy gravity on targets) not.
     Needs a single "apply slow to" entry point; today each slow sets its own modifier.
3. **No crit reduction** for him (Bruiser has it): headshots x2 as for everyone. Nothing to add, but the role pass should leave a hook.
4. **A bigger body:** mover 2.5 m tall, 0.7 m radius (1.4 m wide), crouched 2.05 m (D). Minecraft's player box is 0.6 x 1.8. Ours:
   `Attributes.SCALE`? No: it scales the model and the camera too (his eye would sit at 2.9 m). Use a per-hero box (`Player.getDefaultDimensions`
   mixin, pose STANDING 1.4 x 2.5, CROUCHING 1.4 x 2.05, eye at the model's 2.08 m head hardpoint minus his float, M) so mobs hit him
   more easily and he no longer fits one-block gaps (a tank's real cost). Open: whether to keep doors / 2-high tunnels usable (a
   2.5 m box needs three blocks of headroom; the owner may prefer 1.8 m and only the width).
5. **Shields at 250** (Lifeweaver / Juno's `Shields` handles the layer; 250 regenerating at 30/s after 3 s).
6. **Overhealth that decays after a delay** (Grasp: up to 400, 3 s hold, then 5/s): `Combat.overhealthPool(entity, add, max, hold,
   decay)` already does exactly this (Tree of Life's): reuse.
7. **A barrier that is an entity** (not Brigitte's state-bound rectangle): see below. Shared later by Reinhardt, Winston, Orisa, Ramattra.

## What maps onto what Overcraft has
- **Hero entry:** `Hero.SIGMA("sigma", "Sigma", Role.TANK, "stalwart", SigmaStats.HEALTH + SigmaStats.TANK_HEALTH + SigmaStats.SHIELDS,
  true, 0xFF7C8B8C)` (colour from D's hero colour); `shields()` returns 250; `HeroKit` `dev.overcraft.sigma.Sigma` (built like
  `brigitte/Brigitte.java` and `juno/`), `SigmaStats` (each number with D / BZ / W), `SigmaSounds` later; perks XP 1955 / 4890; four
  `Perk` entries (`SIGMA_KINETIC_CYCLE`, `SIGMA_HYPER_REGENERATION`, `SIGMA_HYPER_STRIKE`, `SIGMA_LEVITATION`). Ult cost 2300.
- **Models:** 3P **106F7** (set 0A7E), 1P **106F6** (0AA1), the **hand spheres 106B4** (set 0AD7) as a held model on hp 000A (Weapon_R) in
  3P / 00C9 in 1P (the held-weapon route Sojourn's railgun / Lúcio's / Mercy's weapons use), the **barrier C015** (0AEF) and its four
  **emitter orbs FC88** (0AF0), the **Accretion rock C5CA**. Clip maps `tools/clipmaps/sigma.txt`, `sigma_1p.txt`, `sigma_weapon.txt`,
  `sigma_props.txt`. `build_asset_pack.py add sigma <file>` for each.
- **Movement:** the stock mover (`OwMovement`'s defaults: his float is animation; one move clip `stance_sm1_move` for all directions, the
  8-way jump). His model hovers: draw it at its rest offset (its feet are above the ground in the clips: check, don't add a lift).
  - **Fall damage:** default 3 m safe height (`Hero.safeFall`), none when Flux's flight or Levitation ends (CLAUDE.md #35).
  - **Gravitic Flux flight:** a new mover state (`OwMovement` hook like `glide` / `dash`): gravity off, 7.15 m/s in every direction, jump
    up / crouch down (and the option: hold ult to rise), for the cast + lift (3.5 s), -50 % knockback (x0.5 in the knockback mixin).
  - **Levitation (perk):** in the air, hold jump 0.2 s after leaving the ground: up at D's 25 m/s² to 3 m/s for 1.75 s, once a landing.
- **Hyperspheres:** projectiles (`OwProjectile`, as Kiriko's kunai / Juno's `BlasterShotEntity`), two a volley 0.2 s (4 ticks) apart,
  1.3 s (26 ticks) before the next; 50 m/s = 2.5 blocks a tick, 0.32 m, a 5.5 m/s² drop (M), bursting after 0.43 s (8.7 ticks) or on
  hitting a creature; **bouncing off blocks** (W; the data's contact sets: M), 20 on a direct hit + a 40 burst full to 1 m, x0.5 at 2.85 m
  (Minecraft explosion damage off: `Combat.damage` per entity in range, as Juno's / Kiriko's AoE), an inward pull of 0.75 m/s (+1 up).
  No ammo (the HUD shows no ammo count), no crits, no self-damage, no block damage.
- **Experimental Barrier:** a **new entity** (`SigmaBarrierEntity`, like `PetalPlatformEntity`: synced owner, state, health), states
  held / sent / stopped / recalled; sent at 20 m/s (1 block a tick) along his look, stopped by a block 0.9 m ahead, no range limit;
  **blocks** what crosses it from the front: Brigitte's `Barrier.absorb` / `blocksBlast` generalised to a list of barrier shapes
  (her rectangle and this curved 6.7 x 4.18 m one, its own facing), so both live in one `LivingEntityMixin` / `ServerExplosionMixin`
  path, plus projectiles hitting it removed (arrows, fireballs, hero projectiles). 650 health, 79.3/s back after 2 s **with him**,
  broken 5 s, recall 1 s; 2 % of what it takes to his ult. Options: Toggle (off), Reload recalls (off): `HeroOptions`.
- **Kinetic Grasp:** a 2 s state; a box (2 m wide, 2.7 m tall, 0.4-2.6 m ahead) that **deletes projectiles** entering it (arrows,
  tridents, fireballs, shulker bullets, potions, snowballs, hero projectiles) and sums their damage (a projectile's damage: arrows' base
  x speed, as Minecraft computes it; potions / snowballs 0: M); at the end, 60 % as overhealth up to 400 (`Combat.overhealthPool`,
  3 s hold, 5/s decay); cut short (stun, Accretion, Flux): none. 12 s from the end. +5 % of the absorbed to his ult.
- **Accretion:** 0.65 s cast (13 ticks; -75 % speed, no jump), then a heavy projectile (37 m/s, 18 m/s² gravity, 0.9 m vs creatures /
  0.3 m vs blocks): 80 to the one it hits + **knockdown 1.1 s** (`Sleep.stun`: the data's 224E is the same knockdown graph Brigitte's
  Rally bash uses), a 40 burst to 2.5 m, knockback 6.7 / 5.8 m/s; his own burst hurts him (W 20 -> 6: Open, D shows no self entry).
  Cooldown 10 s from 0.45 s after the throw; +25 ult on a hit.
- **Gravitic Flux:** a targeting reticle (35 m, 5 s, confirm on the left button or the right by option), 1 s cast, then every enemy in a
  7.5 m radius, 15 m tall cylinder from 5 m below the point, in sight of its centre: 50 damage, held still 0.4 s, lifted (gravity
  x0.1, up 18 m/s easing) for 2 s; then the slam on landing within 5 m of ground: **50 % of the target's max health** (bypassing armor
  and Minecraft's damage reduction: a `bypasses_armor` / `bypasses_effects` damage type), then 0.6 s of heavy gravity (x22, -50 %
  speed). Bosses: the Ender Dragon and the Wither take the 50 % of max (300 / 400 health): Open (Overwatch has no bosses; a cap may be
  wanted).
- **Hyper Strike (perk):** 5 direct sphere hits charge the next quick melee (`QuickMelee`): +50 (W), lift 1 s, 14 m/s along his aim.
- **Hyper Regeneration (perk):** 30 % of Hypersphere damage to "heroes" heals the barrier.
- **Kinetic Cycle (perk):** a normal Grasp end takes absorbed x 0.02 s off Accretion.
- **Clips:** stand `stance_sm1_stand`, move `stance_sm1_move`, the 8-way jump, the three fire anims cycled per volley (`fire_a/b/c`
  pitch triples), barrier send / hold / deploy / recall, Grasp start / hold, Accretion gather / throw, Flux start / aim / cast / lift /
  slam; the hand spheres' own clips on top (`sigma_weapon.txt`); the orbs fly from his hand bones 02FD-0300 to the barrier's corners.

## Minecraft decisions (ours; defaults in place, the owner may change them: "Open")
- **What counts as a "hero"** (Hyper Regeneration, Hyper Strike's count, the role's ult reduction): every living creature and player
  hit, as Sojourn's plan (Overwatch's "enemy heroes" have no Minecraft equivalent but mobs); barrier and Grasp count every projectile.
- **Role Queue:** always on (+150): the mod has no queue; 600 total.
- **Body size:** the data's 1.4 x 2.5 m box (2.05 m crouched): Open (above).
- **What the barrier blocks:** as Brigitte's (arrows, tridents, fireballs, potions, every hero projectile, blasts from the front); not
  melee; mobs walk through it (Overwatch's barriers don't block movement). Its own projectiles and his allies' pass from behind.
- **Grasp against non-damage projectiles** (ender pearls, eggs, snowballs, fireworks): pearls and eggs eaten for 0 (W: healing
  projectiles absorbed for 0), fireworks' blast not (it's an explosion).
- **Flux on bosses / big mobs:** the 50 % slam capped at 200 Overwatch damage on the Ender Dragon, the Wither and the Warden (ours: Open).
- **Hyperspheres and blocks:** bounce (W); never break blocks; burst on a creature or after 0.43 s.
- **Keys:** Accretion ability 2 (E), Grasp ability 1 (LShift), barrier right button, Flux ult (Q), Levitation the jump key, as
  Overwatch's defaults; options: barrier toggle, reload recalls, Flux confirm button, rise with ability 3.

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. **The Tank role shell:** `Hero.tank()`, +150 health, the ult -40 % on damage to a tank, Stalwart's knockback / slow resistance, the body
   size decision; unit tests for the math; the select screen's Tank group (the first hero in it), the badge, 24 pips on the HUD.
2. Hero entry, models (3P, 1P, the hand spheres, the barrier and orbs, the rock), clip maps, select screen and HUD shell: pick him, look
   at him from every side in both views, his float, the spheres in his hands.
3. Movement: walking (the one move clip in every direction), jumping, his size in doors and tunnels; knockback from a creeper, a
   skeleton's Punch arrow, a ravager (x0.6); Local Anesthetic-style slows (x0.6).
4. Hyperspheres on zombies and skeletons: the volley's timing, the burst and its falloff, bounces off walls, the 21.7 m burst, the pull.
5. Experimental Barrier: arrows and fireballs from the front blocked, not from behind; sending, stopping, a wall, recall, breaking it,
   regeneration with him; the options.
6. Kinetic Grasp: skeletons' arrows, a blaze's fireballs, a ghast's; overhealth at the end, none if stunned; the decay.
7. Accretion: the cast's slow, the arc, the knockdown on a zombie / a player, the burst on a group, the cooldown.
8. Gravitic Flux: the reticle (both confirm options), the cylinder on a crowd, the lift and slam on mobs of every size, his flight.
9. Perks (Kinetic Cycle, Hyper Regeneration, Hyper Strike, Levitation).
10. Sounds, voice and effects from his data. 11. Emotes from the data. 12. Two players (the ult -40 %), a night, the general play; release.
