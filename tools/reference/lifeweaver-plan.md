# Lifeweaver: the plan

Numbers: [lifeweaver.md](lifeweaver.md) (public sources) -> a `LifeweaverStats`, each number with its source (BZ / W / D and
the value or node id); the game data map `E:\Overwatch Archive\research\lifeweaver\lifeweaver-map.md` (wins where they differ) with its detail in
`E:\Overwatch Archive\research\lifeweaver\work_kit\kit_weapons.md`, `kit_petal_dash.md`, `kit_grip_tree.md` and `work\clipmap.md`. Branch `feature/lifeweaver`
(issue #39). Written 2026-10-29 from build 154088 (2026-10-06); the patches since (10-13, 10-20) don't touch him.

## Where the data differs from the wiki (the data is used unless noted)
- Petal Platform is thrown at **50 m/s** (W 40), from the left wrist (hp 0002), 0.1 s after the press; cooldown from 0.25 s.
- Rejuvenating Dash: **0.25 s at x5.25 = 28.875 m/s**, horizontal, gravity off, by the WASD keys (forward with none); heal
  55 over the 0.25 s (W 0.3 s; W's 24.25 m/s in the air and its 6.15 / 6.9 m are measurements the data doesn't hold).
  Dashing Escape scales the speed x1.3.
- Healing Blossom: charge 1.15 s held, **5.175 s passive** (W 5.4) even holstered; the blossom is spent when the charge
  starts; heal 10 below 10% charge rising to 90 at full; heal over **0.3 x charge** s; speed 30 + 30 x charge, **60 from
  90%**; 0.3 s recovery; 0.175 s draw delay; **a reload refills both weapons**.
- Thorn Volley: spread **0 -> 0.9° (first thorn) and 0.2 -> 1.2° (second) over 10 volleys**; radius 0.2 = 0.125 + the
  global 0.075 (as W).
- Life Grip: the pull starts **0.35 s after the cast** (the vine travelling); **25 m/s within 15 m, else 12 m/s for 0.4 s,
  then 12 -> 35 over 0.6 s, then 35**; Jump cancels from **0.65 s** (BZ 0.6, M); the destination is fixed at the cast,
  **1.1 m in front of him** (raised 1.4-1.6 m), on safe ground if he's airborne, on his platform if he's on it; capped at
  2.5 s. Cast 0.3 s; cooldown from the cast, none without a target.
- Petal Protection heals allies **within 3 m** of the platform (BZ "standing on": the data has no standing test). Keep the
  data's 3 m, but a 3 m disc is almost the platform itself, so the difference is small.
- Sow the Seed: pickup **3 m** (W 5 m, "gravitates": no drift found), the seed lasts **14 s**, the overhealth decays **25/s
  after the 5 s**.
- Tree of Life: spawns **0.15 s after confirming** (the charge goes then); pulses at 1.75 ... 14.0 s; the overhealth pool
  decays **20/s** after 6 s without a pulse.
- **Thorn hits add +1 (max +50) to the next blossom's heal, with no perk gate** (51F4 #466/#470). Not in any public
  source: possibly a remnant of the removed Lifeweaving perk. **Leave it out** until the owner says (L whether it's live).
- Ult cost 2400 (D 0AEC = W). Health 200 + 50 shields (D = BZ).

## Assets
- One model **00BB.118** for both views: third person with look **0472.119** (entity Gameplay3P 35AF, set 0F4C), first
  person with look **07BC.119** (Gameplay1P 35BE, set 0F4B), Jetpack Cat's arrangement. Export with
  `tools/owm_survey.py --glb --entity=<extract>\Entities\Gameplay3P\Gameplay3P.owentity --keep=tools/clipmaps/lifeweaver.txt`
  and `...Gameplay1P\Gameplay1P.owentity --keep=tools/clipmaps/lifeweaver_1p.txt`, from
  `E:\Overwatch Archive\research\lifeweaver\extract\Heroes\Lifeweaver\Skin\Overwatch\Overwatch` (the source for
  `build_asset_pack.py add` too). The clip maps are to be written from `work\clipmap.md` / `clipmap.json` (every clip id there,
  all extracted). **No weapon entity**: the Healing Blossom flower is geoset 2 of the body on his right-hand bones, always
  drawn; Thorn Volley is his gauntlet. Check the export shows the flower and both arms in both looks (the 3P set's override
  D7F5 may hide it with Thorn Volley out: L), and look at the whole model from every side against his portrait (the
  Jetpack Cat lesson: a colourless or missing body).
- Muzzles: **hp 000A** (bone_004A, right hand) for the blossom and the thorns (both); **hp 0002** (Wrist_L) for the
  platform and the seed; the look's own hardpoint table has them (the model's table doesn't: read the look, as Jetpack Cat's
  nozzles). Targets aim at an ally's head point (0012), blossoms home on its chest (0001), the vine ends on its pelvis (0014).
- He's tall: pelvis 1.26 m, head 1.81 m, camera ~1.86 m; mover height 2.17, radius 0.54. Scale him to Minecraft as the other
  heroes (and note BZ 2025-11-11's -8% model size, already in the data).
- Cloth: 5 Domino pieces (collar / shoulders, hair, the chong kben and sash at the hips and thighs), as Kiriko's cloth.
- Props, each its own model with its own set (all clips extracted): **Petal Platform 12D32.00C** (entity 4504, set 0FCC:
  C933 unfold, C934 idle, C935 rise, CB48 raised loop, DA90 descent, F735 landing; five petal hardpoints at 1.79 m),
  **Tree of Life 1267E.00C** (entity 4525, set 0FFC: CB03 normal / CB56 leaning / CAF8 + CAF9 + CAFA enclosed growth, D1CE
  pulse, CBEF / D1DC + D74B ending, D948 / D1CF idle), the platform projectile and the seed **126CC.00C**. Export each with
  `owm_survey.py --glb --entity=<extract>\Entities\00000000<id>.003\...owentity` into the pack (props: `build_asset_pack.py
  props`, or as hero models like Lúcio's gun). The tree's preview (48D8) and the blossom / thorn projectiles have no model:
  effects only (later).
- Select screen: showcase CAC8 -> D341 / D3A1 / D343 (60 fps; entity 4B19 also carries model 118A2: look at it).
- Sounds: his banks are in the skin export (1461 files in 190 banks under `Effects\...\Sounds`); voice 842 lines.

## Controls (Overwatch's, with the owner's layout: the main weapon on the left button)
- LMB: Healing Blossom, hold to charge, release to throw at the ally under the crosshair (with Thorn Volley out, LMB swaps
  back and starts charging). RMB: Thorn Volley, held (with the Blossom out, RMB swaps and fires). R: reload (refills both).
  1 / 2: the weapon keys (Overwatch's), not the hotbar in Minecraft mode (G switches modes).
- Shift: Petal Platform (again, after 0.4 s: destroy it). E: Life Grip. **Space while airborne** (0.1 s after leaving the
  ground): Rejuvenating Dash. Q: Tree of Life: aim, LMB confirms, RMB cancels; Q again later destroys the tree. V: quick melee
  (with Sow the Seed and the Blossom out: the seed).
- The hero options with their defaults (Toggle Healing Blossom off, legacy scheme off, Interact-cancels-platform off, Life
  Grip cancel on for the gripped player): the defaults only, unless the owner wants the options.

## Server (the kit)
- Health 200 + **50 shields**: he's the **first hero with shields**. Damage takes overhealth, then shields, then health;
  healing fills health first (W Hit points); shields regenerate after 3 s without damage at 30/s (W). Ours to build: the
  top 50 of his 250 as a shield segment (in Minecraft health, 4 of 20) with its own regeneration, drawn blue on the HUD;
  overhealth stays absorption (`Combat.overhealth`).
- Healing Blossom: charge 0 -> 1 over 1.15 s held, 5.175 s passive (in hand idle or holstered); spend a blossom at the charge
  start; on release, the tracked ally (cone 20° from the view, 30 m, linger 0.15 s, head point; allies: players on his team,
  tamed pets, villagers, as `Allies`) gets a blossom from hp 000A at 30 + 30c (60 from 0.9) m/s, size 0.25, homing on its
  chest after half the straight flight time (M), passing through blocks once locked, healing 10 + 80 x curve(c) over 0.3c s;
  no target: lost blossom, charge reset. 0.3 s recovery. 16, reload 1.75 s (ammo at 1.3125 s); holstered refill after 2 s.
- Thorn Volley: 11 volleys/s of two thorns (6 each, x2 headshots, 100 m/s, radius 0.2 vs creatures, straight, 5 s), 2 ammo
  a volley, 100 ammo, spreads as above, reload 1.5 s (ammo at 1.125 s), refills both. Swap 0.125 s.
- Medic: 40% of his weapon healing heals him (BZ; not in his graphs). Regeneration: everyone's 10 HP/s + 5%/s after 6 s (W).
- Petal Platform: throw 0.1 s in, 50 m/s, gravity 9.8, radius 0.15; deploy where it lands on ground; cast 2 m up and refund
  11 s if blocked; 12 s cooldown from 0.25 s. The platform: 300 health (24 in Minecraft health at the mod's 250 = 20, i.e. x 0.08; the tree's 1200 = 96);
  trigger by anyone standing on it 0.15 s (0.05 s later), radius 2 -> 0.75 m; rise to 9 m (or ceiling - 2.75 m; none under
  3.35 m) in 0.85 s; riders' jump +6 (L unit) during the rise and 0.2 s after; 12 s raised time, cumulative; no rider 2 s ->
  descend over d/10 s; destroyed at 0 health, out of time, on Shift again, on a new throw, when he leaves hero mode / changes
  hero (ours: his graphs don't end it on death; decide with the owner). Petal Protection: 20/s to allies within 3 m.
- Rejuvenating Dash: Space in the air; 0.25 s at 28.875 m/s along the keys (37.5 with Dashing Escape), gravity off, vertical
  speed zeroed; heal 55 over it; cooldown 5 s from its end; no fall damage from its landing (the owner's rule for abilities).
- Life Grip: pick once (cone 20°, 30 m, an ally hero: in Minecraft, players on his team, tamed pets, villagers); cast 0.3 s;
  cooldown 18 s from the cast (none without a target); not while airborne over a drop (M) or aiming the tree; shared lock with
  the platform (1.05 / 0.75 s). The ally: 75 heal at once, invulnerable and immune to knockback / effects during the pull
  (ours: the data's protection is status-side), inputs and abilities locked, cleansed (BZ: remove negative potion effects,
  ours); 0.35 s later pulled to the fixed point at 25 m/s (or the far curve), avoiding walls, for at most 2.5 s; the ally's
  Jump ends it from 0.65 s; within 3 m of him for 0.4 s ends it; no fall damage from it. **A player can't be moved by the
  server smoothly**: send the gripped player's client the pull (destination, speed curve, end rules) and let it move itself,
  as Mercy's Guardian Angel; mobs are moved by the server each tick.
- Tree of Life: aim with a ground finder up to 30 m (preview where valid), LMB confirm / RMB cancel (free); 0.15 s into a
  0.99 s cast the charge goes and the tree spawns; 1200 health, 15 s, Q destroys it from 0.5 s; sprout 150, pulses 8 x 90
  every 1.75 s, each over 0.15 s, to allies within 12 m in sight of the trunk at +1.8 m; overhealth 0.5 of the overflow into a
  100 pool, kept 6 s after the last pulse, then -20/s (as absorption). Collision: trunk r 1 x 6.5 m, canopy r 3 x 3 m at
  +4.5 m (see Unusual). Pushes allies out of the trunk; crushes our own deployables under it (others' platforms, turrets:
  only ours exist in Minecraft).
- Perks (Perks / Perk): minor Petal Protection, Dashing Escape; major Superbloom (16 thorns on one enemy within 2.5 s -> 40),
  Sow the Seed (V with the Blossom out, 11 s; the seed 25 m/s from hp 0002, gravity 9.8, lasts 14 s, an ally within 3 m takes
  it after 0.3 s: 75 overhealth decaying 25/s after 5 s, +25% speed 5 s). Perk XP 1975 / 4940.

## Client
- Movement: Overwatch's walk (5.5 m/s, the default mover); the dash (client-driven, as Mercy's launches); riding the
  platform (below); the gripped player's pull (client-driven MoveTo).
- Animator: the weapon category on every state (df 0xE4: 7F Blossom, 7D Thorn Volley); idle (two stands), run 8-way, crouch
  (3P only), jump / land (the land 2D's Y- is forward: check by eye, M); the blossom's charge triple (time from the charge),
  full loop, release triple, reload; Thorn Volley's 0x16F shot / loop / settle; the dash 2D by the key direction; Life Grip's
  cast; the tree's aim / loop / cast; knock-down, hit. 1P: the hand state machine 0x16E driven by the charge (df 0xC9), the
  thorn ammo counter layer, swap clips, the 1P dash's four slots (directions L: check by eye).
- HUD: Blossom ammo 16 with the charge (and the passive charge), Thorn ammo 100, Petal Platform (Shift; its raised time
  left), Life Grip (E), Rejuvenating Dash (Space; 5 s), Tree of Life (Q, 2400; the tree's time and the cancel prompt), the
  shield segment. The ally under the crosshair outlined while charging (the data's preview tracker), Life Grip's target too.

## Unusual about him
- **Petal Platform, a lift.** Minecraft carries nobody on a moving entity. Build it as an entity solid to others (as a
  boat or shulker), sized from the data's 1.9 m stacking cylinder (a 3.8 m square box: corners overhang ~0.8 m; ours, M-L)
  and thin; trigger by "standing on it" (feet on its top face). While it rises, the server moves mobs standing on it with
  it, and each rider player's client moves itself with it (the data: carried by collision; the carrying is ours). Give
  riders a no-fall-damage flag until they next land (the owner's rule for ability landings), and the jump bonus while it
  holds. A 9 m rise in 0.85 s is 0.53 blocks a tick: test riders don't fall through or get stuck in a ceiling (the data's
  2.75 m headroom keeps them clear).
- **Life Grip pulls an ally**, often a player: client-driven pull for players (a payload: point, speed, curve, cancel rules),
  server-driven for mobs; the ally invulnerable for its length; the vine drawn from the fixed point to the ally's pelvis.
  Lifeweaver himself doesn't move. The far-pull curve (12 -> 35) and the 2.5 s cap keep it short.
- **Tree of Life is a placed object** with health and collision. Minecraft entities have one box: the trunk (2 x 6.5 m) as
  the entity, and the canopy (6 x 3 m at +4.5 m) either as a second solid child entity or not solid (ask the owner). It
  heals in pulses through its own sight test (rays from the trunk at +1.8 m to each ally), and its overhealth outlives it.
  Placement attaches to whatever's under the point (a minecart, a boat: ours to decide whether it follows).
- **Healing Blossom's charge-up:** a held button with a charge value (0-1) that also fills by itself, even holstered, spent
  at the charge's start; the charge drives the heal, the projectile speed, the heal-over-time, the release effect (three
  sizes by charge) and the 1P hand animation. The blossom homes and passes through blocks once locked: the projectile can't
  use Minecraft's block collision while it has a target.
- **Shields** (first hero with them): see Server.

## Later
Sounds (a sound map from his banks: Blossom 30E1 charging / 48B4 fire / 48F6 full, Thorn 4A9E, platform 4218 / 421A / 4DE3 /
5E50, Dash 4901, Life Grip 3111 / 3110 / 48FE, Tree 4D76 / 4785 / 4248 / 4C96-4C98 / 43C6: `lifeweaver-map.md` "Sounds and
effects"), effects (a research pass like Mercy's `fx\`: the blossom, the vine, the platform's petals, the tree), voice (11F4
"Hop on!", 1200, 11FF, 135A, 176B, 11FD / 11FE / 184F "Life protects life", 135B), emotes (13 extracted).

## Open (2026-10-29)
- Whether the thorn -> blossom +50 bonus is live (not decidable from the data; left out).
- Petal Platform's collision shape and walkable size (not in the files; 1.9 m from its checks, M-L); the +6 jump bonus's unit
  (m/s assumed, L); the projectile's bounce and ground-only rules (ours: deploy on the first ground hit).
- The dash's end (a x0.5 op, L) and the ground distance (the data's 0.25 s x 28.875 = 7.2 m vs W's 6.15 m): measure in play.
- Half or whole angle of the 20° cones and the spreads (read as the angle from the aim, as Mercy's).
- Life Grip's cleanse, CC immunity and projectile blocking: status side, not in his graphs (ours).
- The canopy's collision and whether a tree placed on a moving entity follows it (ask the owner).
- The Medic 40% (shared code, BZ's number used); entity 4F15 (the platform model on hp 01B6, shown by a var none of his
  graphs set: L, perhaps a menu or preview prop).
- Fall damage: his own safe height for ordinary falls isn't defined by any source (ours to choose with the owner, as
  Kiriko's climb height was).
