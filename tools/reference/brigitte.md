# Brigitte in live Overwatch: numbers and sources (research, 2026-11-01)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only: **left empty here** (`D _`), filled in by the
data pass. **BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read
2026-11-01: every month from 2022-10 to 2026-11; 2026-11 is empty: the last patch is **2026-10-20**, a bug fix with nothing
for her). **The last patch that touched her kit is BZ 2026-09-08** (health 200 -> 175, armor 50 -> 75); the last fix for her
is BZ 2026-08-11 (Inspiring Strike's instant heal). **Nothing for her on 2026-10-06 (Season 5), 10-13 or 10-20**, so the
data's build (10-06) should hold every live value. **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-11-01: Brigitte revision **408525** of 2026-10-29T05:11Z, plus all 153
Brigitte revisions since 2024-01 diffed field by field; Sub-Roles 406777, Global Passive Abilities 402069, Hit points 401654
(Armor and Overhealth redirect there), Barrier 406358, Movement speed 402311 (Movement redirects there), Melee 402309, Status
effect 408615 (Crowd Control and Knockback redirect there), Ultimate ability 395578, Perks 354523, Brigitte/Stadium 408534).
Inspire, Rocket Flail, Whip Shot, Repair Pack, Barrier Shield, Shield Bash and Rally have no pages of their own (they redirect
to hers). Blizzard's hero page and Liquipedia were not read. Raw pages, the field-by-field revision diff and the patch notes
as text stayed in the scratchpad.

Each number gives D first (empty for now), then BZ, then W. Confidence (public sources only, to be revised once D is in):
**H** BZ and W agree (or W gives it and BZ's history leads to it); **M** a single public source; **L** the sources
disagree, or a guess. Where they differ, both are given here and in "Disagreements".

Left out (not the live game): **Stadium** (BZ from 2025-08-26 "Added to the Stadium Hero roster", tuned 2025-09-04, 10-01,
10-14, 11-11, 12-18, 2026-04-23, 06-16; W Brigitte/Stadium: powers Burst Aid, Righteous Cleave, Whirlwhip, Optimizer,
Packstacker, God Ray, Maces to Faces, Mender Bender, Skoldkastning, Lindholm Wall, Aura Farming, Consecrated Ground; items
Omni-Boost, Nullifier Neuroworks, Haste Paste, Mitzi's Medicine, Chain Extender, Scavenged Steel, Squire's Satchel,
Valkyrian Core, Dragon's Incinerator, Hardlight Weave), **Community Crafted** (BZ 2026-06-30: Whip Shot propels her back off
walls, Shield Bash dashes into the air, Rally drops her from the sky; Survivor +10 max health a minute alive, up to +100, in
that mode only), April Fools (BZ 2024-04-01 "Bigitte", 2025-04-01 Shield Throw, 2026-04-01 shield surfing), Story Missions /
PvE (BZ 2023-08-10: Shield Bash 60, Rally 15 s, Inspire 55; W's co-op notes), Overwatch Classic GOATS (BZ 2025-03-18, her OW1
kit), Haunted Masquerade masks (BZ 2025-10-14: Brigitte Mask / Reinhardt Mask), Hero Mastery (BZ 2024-10-15), Mystery
Heroes (perks off since BZ 2026-04-14), **6v6** (W Hit points has a 6v6 row; no Brigitte 6v6 line in BZ). Her removed perks
(Barrier Restoration, Quick Fix) and her OW1 kit (Shield Bash stun, Repair Pack overhealth) are history only.

**Patches that touched her (2025-2026, live 5v5):** 2025-02-18 perks added (Barrier Restoration, Morale Boost minor; Quick
Fix, Whiplash 50 major); 03-18 Barrier Restoration +30% speed 1.5 s; **08-26 Barrier Restoration and Quick Fix out, Combat
Medic (0.5 s) and Inspiring Strike in**; **09-16 Shield Bash 50 -> 70**, Combat Medic no longer triggers on non-player
entities; **11-11 Repair Pack cooldown 5.5 -> 5 s**. 2026-01-20 fix (losing her abilities); **02-10 (Season 1): Survivor
sub-role, support passive removed, healing reduction made global**; **04-14 Barrier Shield 250 -> 300, Combat Medic 0.5 ->
0.75 s**; 05-12 Inspire UI (allies being healed, hero option); **06-16 Whiplash 50 -> 60**, Inspire UI fixed for Morale
Boost, Survivor fix (no trigger before damage); **07-14 Inspire: +12 instant, over time 60 -> 45**; 08-11 Inspiring Strike's
instant heal fixed, Survivor fix (no trigger without a movement ability); **09-08 health 200 -> 175, armor 50 -> 75**.
Nothing on 2026-10-06, 10-13 or 10-20.

## Base
- **175 health + 75 armor = 250, no shields** (D _; BZ 2024-02-13 150 -> 200 health, BZ 2026-09-08 200 -> 175 health and 50
  -> 75 armor; W 175 / 75, rev 394139) **H**. **Support**, sub-role **Survivor** (D _; BZ 2026-02-10; W) **H**.
- **Armor** (W Hit points; BZ 2024-09-17 "7 per impact", BZ 2023-08-10 cap 50% additive with other reductions): each hit
  loses **7 damage, at most 50%** of it (hits under 14 halved); beams **-30%**; damage over time not reduced; armor is spent
  after shields and before health, and heals back like health **H/M**.
- Speed **5.5 m/s**, backward 4.95 (D _; W Movement speed, the global walk) **M**. Jump and gravity: the global ones (W: 5.72
  m/s jump, 0.98 m apex, 17.5 m/s^2 to a 30 m/s fall; no fall damage) **M**.
- Hit volumes: **not published** (BZ 2026-10-06's hit volume list has no Brigitte line; W gives none).
- **No quick melee**: the quick melee key swings Rocket Flail (D _; W Brigitte and W Melee) **M**.
- Ult charge (W Ultimate ability): 5 points/s passively, 1 point per damage or healing, none from the global regeneration or
  ult damage/healing, half from damage to overhealth, -40% from damage/healing on tanks; none while Rally is active **M**.

## Rocket Flail (primary fire; melee)
- "Melee weapon with extended range." (W).
- **45 damage a swing** (D _; BZ 2024-02-13 35 -> 45; W) **H**, **hits every enemy in the swing** (W overview: "strike
  multiple close-range targets") **M**; no head hits (W) **M**; goes through barriers (melee) (W) **M**.
- **Range 6 m** (D _; W) **M**; at 5-6 m the sweep reaches **3 m to the side she swings toward, 2 m to the other** (W) **M**.
- **0.6 s a swing** (1.67 a second): **0.208 s to the hit + 0.392 s recovery**, hit window **0.24 s** (D _; W) **M**. DPS
  75 (W) **M**. The swings alternate sides (W's 3 m / 2 m note; L which side first).
- Deflect blocks it without reflecting it (W) **M**.

## Inspire (passive)
- "Dealing damage to enemies heals nearby allies." (W).
- **12 healing at once + 45 over 4 s (11.25 HP/s)** to herself and every ally **within 20 m** in sight when it triggers (D _;
  BZ 2026-07-14 "Now grants 12 instant healing on trigger. Healing over time reduced from 60 to 45."; BZ 2024-12-10 duration
  5 -> 4 s "healing per second remains unchanged"; W 12 + 11.25/s, ticks of 2.16 every 0.192 s, rev 376973) **H**.
- It is applied once, at the trigger, not a moving aura: allies who leave the 20 m keep healing; blocked by sight and enemy
  barriers; it doesn't stack, a new trigger resets the duration (W) **M**.
- **Triggered by damage from Rocket Flail, Whip Shot or Shield Bash** (D _; W) **M**, on heroes (and B.O.B., Minefield)
  only (W) **M**. **Rocket Flail can trigger it once every 1.25 s**; Whip Shot and Shield Bash always trigger it (W) **M**.
  So steady swings trigger it every third swing (W: 3 x 0.6 > 1.25 > 2 x 0.6) **M**. During Rally a multi-hit Shield Bash
  triggers it once (W) **M**.
- Hero option: Inspire UI (allies being healed under the crosshair) **on** (BZ 2026-05-12; W) **H**.

## Whip Shot (ability 1; projectile, extended melee)
- "Launch your flail forward to knock an enemy away from you." (W).
- **70 damage** (D _; BZ 2023-10-10 70 -> 80, 2023-12-05 80 -> 70; W) **H**, no head hits (W) **M**.
- **Cooldown 4 s** (D _; W) **M**. **Range 20 m**, flail **80 m/s**, out for up to **0.43 s** (D _; W) **M**. Size **0.5 m
  against enemies** (no global size bonus), **0 against walls and barriers** (W rev 401611) **M**.
- Cast **0.192 s + 0.176 s recovery** under 6 m, **+0.304 s recovery** at 6 m or more (W) **M**. **She moves 50% slower
  while it's out** (2.75 m/s) (W) **M**.
- **Knockback 25 m/s back + 3 m/s up** (25.18 m/s), about **8 m** on flat ground (D _; W) **M**. Deflect blocks it without
  reflecting it (W) **M**.

## Repair Pack (ability 2; targeted projectile)
- "Heals an ally for a short duration." (W).
- **25 healing on impact + 100 over 2 s (50 HP/s)** (D _; BZ 2023-02-07 "instantly heals for 25", over time 55 -> 50/s; W 25 +
  100, ticks of 9.98 every 0.192 s) **H**. **No overhealth** (W lists none; the old overheal and Quick Fix are gone) **M**.
- A second pack on the same ally doesn't stack: it **adds 2 s** to the heal (W) **M**.
- **3 charges, cooldown 5 s** (D _; BZ 2024-03-12 6 -> 5, 2024-10-15 5 -> 5.5, **2025-11-11 5.5 -> 5**; W) **H**. **0.5 s
  between throws** (W) **M**.
- **Range 25 m** (D _; BZ 2023-04-11 30 -> 25; W) **H**; projectile **45 m/s**, homing on the target, ignores barriers (W) **M**.
  Cast **0 + 0.256 s** recovery (W) **M**. Allies only (not herself, W "target ally") **M**.
- Hero options: Repair Pack sensitivity **100%** (1-100%: narrows the targeting angle), Repair Pack UI **on** (BZ 2023-05-09;
  W) **M/H**.

## Barrier Shield (secondary fire; held)
- "Hold to deploy a frontal energy barrier." (W).
- **300 health** (D _; BZ 2026-04-14 250 -> 300; W) **H**; **750 during Rally** (D _; BZ 2023-04-11 300 -> 750 then; W +450)
  **H/M** (see Disagreements).
- **Regenerates 85 HP/s after 2 s lowered** (D _; W) **M**. **Destroyed: 5 s before it can come up again** (D _; W) **M**. No
  cooldown otherwise, no cast time (W) **M**.
- **-30% speed while up** (3.85 m/s) (D _; W) **M**. Shape: a vertical rectangle curving in at the bottom; it follows her aim
  but can't point straight down (W) **M**. Hold by default; Toggle Barrier Shield option **off** (W) **M**. Relative aim
  sensitivity during Barrier Shield **100%** (100-500%) (BZ 2023-05-09; W) **H**.

## Shield Bash (primary fire with the shield up)
- "Available when Barrier Shield is deployed. Dash forward to knock back an enemy." (W).
- **70 damage** (D _; BZ 2025-09-16 50 -> 70; W) **H**. **Cooldown 5 s** (D _; W) **M**.
- **Dash 8.5 m in 0.3 s**: +400% speed (+370% with the shield's -30%: **25.85 m/s**), not counted against the speed cap (D _; W
  rev 266702) **M**. Cast 0.016 s + 0.432 s recovery (W) **M**. Facing locked forward; she can't act, lower the shield or turn
  until it ends (W) **M**. W also lists it as a primary/secondary fire key option (W rev 286286) **M**.
- **Hit: one enemy** (all of them in Rally), a **1.4 m hitbox in a 60 degree cone**; knockback **6 m/s** (D _; W) **M**. Hitting
  an enemy or a wall stops the movement but not the ability (W) **M**. It passes through enemy barriers without damaging them
  and breaks destructible props (W) **M**. Two charging abilities colliding knock both down **1.7 s** (BZ 2024-06-20 2 ->
  1.7; W) **H**.
- **No stun outside Rally** (W, its change log: OW2 beta 2022-04-26 "No longer stuns enemies") **M**; **0.75 s stun during Rally** (D _; BZ 2023-04-11 "briefly stuns";
  W 0.75) **M**.
- **It is her movement ability** for Survivor (W Movement speed lists Shield Bash as her only one) **M**.

## Rally (ultimate)
- "Gain armor, empower Barrier Shield and provide extra health to nearby allies." (W).
- **2700 points** (D _; W since rev 225498, 2024-03-15; BZ only in percent: 2023-02-07 -10%, 2023-08-10 +6%, 2024-02-13
  +10% global, 2024-03-12 -8%) **M** (see Disagreements).
- **10 s** (D _; W) **M**. Cast **0 + 0.704 s** recovery; usable at any time, cancelling whatever she's doing (W) **M**.
- Herself: **+100 armor** for the duration (healable, not regenerating) (D _; BZ 2023-04-11 "100 restorable armor"; W) **H**;
  **+15% speed** (6.33 m/s by arithmetic), not against the speed cap (D _; BZ 2023-04-11 30 -> 15%; W) **H**. No overhealth
  for her (BZ 2023-04-11; W) **H**.
- Allies **within 8 m**: **15 overhealth every 0.5 s (30/s), up to 100** (D _; W rev 266702 8.5 -> 8 m) **M**. The overhealth
  **lasts 30 s** from the first tick, even outside the radius or after her death, then decays quickly; losing all of it
  restarts the timer (W) **M**.
- **Resets Shield Bash's cooldown** (BZ 2024-05-14; W) **H**, and Barrier Shield's (refilled to full) (W) **M**. Barrier wider
  and **750** HP; Shield Bash hits several enemies and **stuns 0.75 s** (BZ 2023-04-11; W) **H/M**.

## Survivor (sub-role) and the global passives
- **Survivor** (BZ 2026-02-10: "Using a movement ability activates passive health regeneration."; W Sub-Roles: "Movement
  abilities start passive health regeneration."): using **Shield Bash** starts the global regeneration at once, which then
  runs for **at least 0.25 s** even if she's hit (W) **M**. BZ 2026-06-16 and 08-11 fixed it triggering before damage and
  without a movement ability. Survivors now: Brigitte, Doctrine, Illari, Juno, Mizuki, Wuyang (W).
- **The support role passive is gone** (BZ 2026-02-10; W) **H**.
- **Regeneration: 10 HP/s + 5% of max a second after 6 s without damage**, **22.5 HP/s** for her (250 max) (D _; BZ
  2025-06-24 5 -> 6 s, 2026-02-10 "unchanged"; W) **H**. Whether it refills armor too: not given (L; armor heals like health).
- **Damage dealt cuts the target's healing by 30% (15% on tanks) for 3 s**, global since BZ 2026-02-10 (W Global Passive's
  box flags it "removed": see Disagreements) **M**.

## Perks (minor at perk level 2, major at 3)
- **Perk XP 2035 to the minor, 5090 to the major** (D _; W rev 265552 / 288178) **M**. BZ never changed hers (her 2026-04-14
  entry has no perk cost line); W's 5080 -> 5090 edit of 2026-04-21 has no source. XP ramps from minute 3 to x2 at minute 7
  (W Perks) **M**.
- Minor, **Combat Medic** (BZ 2025-08-26): "Melee attacks reduce the cooldown of Repair Pack by 0.75 seconds." **0.75 s per
  enemy hero hit** by Rocket Flail (D _; BZ 2026-04-14 0.5 -> 0.75; W) **H**; Whip Shot doesn't count, nor deployables except
  B.O.B. (W; BZ 2025-09-16 "no longer triggers on damaging non-player entities") **H/M**.
- Minor, **Morale Boost** (BZ 2025-02-18): "Inspire lasts 3 seconds longer when activated by Whip Shot." **4 -> 7 s** (D _;
  W) **H**. Whether the 45 stretches or the 11.25/s runs 3 s longer: not given (L: per second unchanged, as BZ 2024-12-10's
  wording suggests, = 78.75 + 12).
- Major, **Inspiring Strike** (BZ 2025-08-26): "Shield Bash grants 30% increased movement speed for 2 seconds. Inspire's
  healing is instant when activated by Shield Bash." **+30% = 7.15 m/s for 2 s** (D _; W) **H**; the instant heal **60** in W
  (20 m) vs **57** (12 + 45) by BZ 2026-07-14 **L**; a Shield Bash no longer resets an Inspire already running, and fires once
  however many it hits in Rally (W) **M**. BZ 2026-08-11 fixed its instant heal not applying.
- Major, **Whiplash** (BZ 2025-02-18): "Whip Shot's knockback can slam enemies into walls, dealing 60 extra damage." **+60 =
  130** with the wall (D _; BZ 2026-06-16 50 -> 60; W) **H**. What counts as a wall slam (speed, distance): not given.
- Removed: Barrier Restoration (minor: bash +100 barrier HP and +30% speed 1.5 s) and Quick Fix (major: +35 instant to allies
  at critical health) (BZ 2025-08-26).

## Controls and hero options (W)
Primary fire: Rocket Flail (Shield Bash with the shield up). Secondary fire: Barrier Shield (hold). Ability 1: Whip Shot.
Ability 2: Repair Pack. Ultimate: Rally. Quick melee: swings Rocket Flail. No reload (no ammo). Options and defaults: Toggle
Barrier Shield **off**; Repair Pack sensitivity **100%**; Relative aim sensitivity during Barrier Shield **100%** (gyro too,
BZ 2023-05-09); Repair Pack UI **on**; Inspire UI **on**. Default key letters: not in these sources.

## Lore and voice
**Brigitte Lindholm**, 25 (23 in Overwatch 1), Swedish, mechanical engineer and Reinhardt's squire, Torbjorn's daughter;
English voice **Matilda Smedius**; released **2018-03-20**, the 27th hero (W).

## For the Minecraft port to decide (no Minecraft equivalent)
- **Armor as its own pool**: 75 (175 + 75 = 250 = 20 MC health, so 14 + 6) with the flat -7 / 50% cap per hit, separate from
  MC's armor points. Rally's +100 armor (8 MC) on top. MC "absorption" fits Rally's ally overhealth (100 = 8 MC), not armor.
- **Barrier Shield**: a held, directional 300 HP barrier that stops arrows, fireballs and mob melee from the front (MC's shield
  item only blocks fully, has no HP and no regeneration). Which mob attacks a barrier stops (creeper blasts, area damage).
- **Rocket Flail's 6 m sweep** against MC's 3 m reach; which mobs count as "heroes" for Inspire and Combat Medic (ours: hostile
  mobs?), and which allies Inspire / Repair Pack / Rally can heal (players, tamed pets, villagers, as for the other supports).
- **Knockback in m/s** (Whip Shot 25 + 3 up, Shield Bash 6) into MC velocity; Whiplash's "slammed into a wall" test (a block
  collision during the knockback).
- **Stun** (Rally Shield Bash 0.75 s) and the bash's knockdown collision with other charges: no MC equivalent.
- Survivor: the regeneration timer has to be ours (MC's own natural regeneration is food-based).

## Not given by any source
Rocket Flail's hit shape in degrees and which side the first swing goes; Whip Shot's arc (straight, by its 80 m/s and short
time) and whether it stops on the first enemy; Repair Pack's targeting angle and homing; Barrier Shield's size in metres
(normal and Rally); Shield Bash's behaviour on slopes and ledges; Rally's ally overhealth decay rate after 30 s; Inspire's
sight rule from where (her or the target); Morale Boost's total healing; Whiplash's wall rule; whether regeneration restores
armor; hit volumes; which Minecraft mobs would count as "heroes", "deployables" or "tanks" (ours).

## Disagreements between the sources
- **Inspiring Strike's instant heal:** W 60 (added 2026-09-09) is the old over-time total (15/s x 4 s); after BZ 2026-07-14
  Inspire is 12 + 45, so 57 if the perk gives the whole Inspire at once. D settles it.
- **Rally's speed:** W's tooltip says 3.625 m/s for +15%, which fits no base (5.5 x 1.15 = 6.325; with the shield's -30%,
  5.5 x 0.85 = 4.675). Use +15%.
- **Rally's cost:** W 2700 (2900 between its 2024-02-14 and 03-15 edits). BZ gives only percentages: 2900 x 0.92 = 2668, and
  W's earlier 2700 x 1.10 = 2970, so W's figures are rounded or from the game. D settles it.
- **Rally barrier health:** BZ 2023-04-11 "300 to 750"; W showed 700 from 2024-09 (when the base was 250) and 750 since the
  2026-04-14 change (base 300 + 450). So the Rally bonus is a flat +450 (W), not a fixed 750 (BZ's old line).
- **Rally radius:** W 8.5 m until 2025-09-23, 8 m since (no BZ line).
- **Shield Bash dash:** W 12 m until 2025-09-23 (W's change log, OW2 beta 2022-04-26: 7 -> 12 m), 8.5 m since; W also had +500% / 31.35 m/s
  until 2024-07-31, +400% since. No BZ line for either.
- **Perk XP:** W 2035 / 5090 (5080 until a 2026-04-21 edit; BZ has no Brigitte perk cost change). D settles it.
- **Healing reduction:** W's Global Passive box flags it "removed" while BZ treats it as live (as for Ana and Sombra).
