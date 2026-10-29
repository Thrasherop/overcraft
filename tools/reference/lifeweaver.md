# Lifeweaver in live Overwatch: numbers and sources (research, 2026-10-29)

**BZ** = Blizzard patch notes (https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/, read 2026-10-29: every
month 2025-01 to 2026-10, and 2023-04/05/06/08/10, 2024-01/02/03/04/08/09/11/12 for the values nobody changed since; "via W"
= an older patch read only as quoted in W's change log), **W** = the Overwatch wiki on Weird Gloop through its MediaWiki API
(`overwatch.weirdgloop.org/api.php`, read 2026-10-29: Lifeweaver revision 406788 of 2026-10-20, with older revisions 379135
of 2026-07-28, 309716 of 06-01, 277036 of 02-08 and 273409 of 2025-12-22 for the ult cost's history; Sub-Roles 406777 of
10-20; Global Passive Abilities 402069 of 10-09; Roles 401659 of 10-07; Movement speed 402311 of 10-10; Ultimate ability
395578 of 09-16; Melee 402309 of 10-10; Hit points 401654 of 10-07). Blizzard's hero page and Liquipedia were not read (this pass's rule: patch notes and
the wiki's API only). Raw pages: scratchpad `lifeweaver\web\` (not kept here).

Left out (not the live game): Underwatch (BZ 2026-04-01: scale -50%, 75 health, a shrinking Life Grip), Community Crafted
(BZ 2026-06-30: Superbloom in the base kit, Arboreal Affliction, Thorn Volley reload 0.75 s, allies steering Petal Platform
with Interact, Tree of Life pulses moving cooldowns 2 s; and a Medic +2.5% healing per allied Medic), Totally Normal
(BZ 2025-04-01: homing thorns), Quick Play Hacked (BZ 2026-07-30), Haunted Masquerade masks (BZ 2025-10), 6v6 (W's 6v6 log:
its Petal Platform 15 s and Tree of Life 12 s / 1400 were folded back into 5v5's numbers on 2025-03-18). He has no Stadium
section in any 2025-2026 patch (not in Stadium). The game data (build 154088 of 2026-10-06, `lifeweaver-map.md`) agrees on the kit, the numbers above
marked so, the ult cost (2400) and the perks; where it differs (Petal Platform 50 m/s, Dash 0.25 s, the Blossom's passive
5.175 s, Life Grip's far-pull curve and others) see the map's "Disagreements".

Patches that touched him in 2026: 01-08 (Moira bug fix only), 02-10 (Season 1: Medic sub-role, Tree of Life conversion
50 -> 100%, the three new perks), 03-10 (Medic 25 -> 40%), 04-14 (Season 2: Blossom 90, Dash 55, Life Grip cleanses, Sow the
Seed 11 s; a weapon-swap animation fix), 06-16 (Season 3: ult +9%, conversion back to 50%), 07-28 (Blossom release fix),
08-11 (Season 4: Life Grip 25 m/s, Jump cancels after 0.6 s), 10-06 (Season 5: hit volumes). Nothing for him on 10-13 or
10-20 (the last patch, read 2026-10-29). W says he was pulled from every mode on 2026-08-20 for a bug (believed to be the new
Life Grip cancel) and back on 08-24; BZ has no note of it.

## Base
- Health **200 + 50 shields** = 250 (BZ 2025-08-26: health 225 -> 200; shields 50 since BZ 2023-08-10; W). Support, sub-role
  **Medic** (BZ 2026-02-10; W). Base speed 5.5 m/s, backward 90% (W Movement speed: the global walk; nothing of his changes it).
- Model and hit volumes -8% (BZ 2025-11-11). Hit volumes (BZ 2026-10-06): head 14.8 -> 17, torso 19.7 -> 25, legs 15.5 -> 19,
  waist 15.9 -> 20 (units not given). His back petals were cut 10% in size and hit volume (BZ 2023-06-13).
- Shields regenerate after 3 s without damage at 30/s (W Hit points); damage takes overhealth, then shields, armor,
  health; healing fills from health up (W Hit points).
- Quick melee 40 (BZ 2024-02-13, global; W Melee). He's one of the heroes who can't cancel quick melee into the ultimate (W
  Melee).
- Weapons: two, swapped (Healing Blossom / Thorn Volley). Swap time 0.125 s (BZ 2025-11-11: 0.25 -> 0.125). The weapon not in
  hand reloads itself after **2 s** (BZ 2025-11-11: 2.5 -> 2; W).

## Healing Blossom (primary fire: hold to charge, release to heal)
- "Hold to charge a healing burst. Release to heal a targeted ally. Passively charges at a reduced rate when not in use." (W
  and the data's text).
- Heal **10 (no charge) to 90 (full)**, landing over 0.3 s (BZ 2026-04-14: max 80 -> 90; W "90 over 0.3 seconds (max
  charge), 10 over 0.3 seconds (min charge)"). Full charge **1.15 s** (BZ 2024-03-12: 1 -> 1.15), then a 0.3 s recovery (W:
  0.69 releases a second at full charge, 3.33 at none). W also says "1.2 seconds to reach maximum charge while actively firing
  and 5.4 seconds while not firing".
- Passive charge while not held (BZ 2024-08-20, "a reduced rate"), +25% (BZ 2025-05-20); W's 5.4 s is the only number. Holding
  a full charge no longer slows him (BZ 2024-08-20; the slow after 1 s at full is BZ 2023-04-25's, now gone).
- **16 blossoms**, reload **1.75 s** (BZ 2023-10-31: 20 -> 16; W reload 1.75). A manual reload resets the charge; the
  automatic one while Thorn Volley is out doesn't (W).
- Projectile 60 m/s at full charge, 30 m/s at none, radius 0.25 m (W). **Targeting range 30 m** (W). It flies toward the
  crosshair, then homes on the target after a short delay; the lock holds even if the target leaves the range or moves, and a
  locked blossom passes through terrain (W). Targetable through barriers but blocked by them (W). The target stays valid a
  moment after leaving the view angle (W, from a Reddit post). Targets head and body (BZ 2023-06-13 bug fix: "Healing
  Blossom and Life Grip targeting now consider both the head ..."). Not affected by damage boost (W).
- Interrupting without losing the charge: quick melee, aiming Tree of Life, swapping to Thorn Volley; his abilities don't
  interrupt charging (W). Kitsune Rush speeds the active charge, not the passive one (W).
- About 62 healing a second at full charge (90 / 1.45), 33.3 at none (W).
- Options (W): Toggle Healing Blossom (off: hold to charge, release fires), Healing Blossom sensitivity (narrows the view
  angle for targeting; the angle isn't given).

## Thorn Volley (secondary fire)
- "Rapidly fires pairs of projectiles." (data); "Rapidly fire a spread of projectiles." (W).
- **6 per thorn, 2 thorns a volley** (12; BZ 2023-06-13: 5 -> 6), **11 volleys a second** (one per 0.091 s; W),
  **100 ammo, 2 a volley** (50 volleys; BZ 2024-09-17: 80 -> 100), reload **1.5 s** (W), **100 m/s** (BZ 2025-04-22: 80 ->
  100), radius **0.2 m** (0.125 base, BZ 2023-06-13, + 0.075 global, BZ 2024-03-12; W). Headshots count (W), at the usual 2x.
- Spread from 0 to 1.2° over 20 thorns (10 volleys, about 0.9 s) (BZ 2023-06-28 "from 0 to 20"; W 1.2° max). One thorn of
  the two has 25% less spread (BZ 2023-08-10; W). No falloff (W strategy).
- 131.9 damage a second firing, 99.2 with reloads (W).
- Controls (BZ 2023-04-25): secondary fire swaps to Thorn Volley and fires; with Thorn Volley out, primary fire swaps back
  and starts charging. Option "Relative Thorn Volley aim sensitivity" 0-500% (W).

## Petal Platform (ability 1)
- "Throw a platform that springs upwards when stepped on. Resets when not in use." (data; W).
- **Cooldown 12 s**, from the throw (W; BZ 2024-12-17 / 2025-03-18 in 6v6 via W). **300 health** (BZ 2024-08-20: 400 ->
  300). Thrown in an arc at **40 m/s**, radius 0.15 m; bounces off barriers and objects and deploys only on the ground (W). An
  invalid spot (tight corner, low ceiling) destroys it and refunds 11 s of the cooldown (W).
- On the ground it lasts until used or destroyed; **12 s** counts down only while raised (BZ 2024-08-20: 10 -> 12, "only
  counts down while the platform is raised"). It **resets (descends) when no one has stood on it for 2 s** (BZ 2024-08-20); a
  reset under 2 s left is raised to 2 s first (W).
- Rises **10 m/s**, **up to 9 m** above its ground, stopping early under a ceiling (W). W's "cast time 3 seconds" isn't
  explained (L: perhaps a rise time; 9 / 10 = 0.9 s). The lift's momentum lets heroes jump higher at the top (W).
- Triggered by any hero standing on it, ally or enemy (W). Trigger radius **2 m** as it lands, then **0.75 m** (BZ
  2023-06-13). A short delay, so jumping on it or passing over fast doesn't trigger it unless it landed under a hero (W). Walkable
  area +15% (BZ 2023-06-13; no base size given). Souls ride it but don't trigger it (W).
- Terrain-like: collides, blocks sight, grapples hook on; the payload passes through it (W). Things placed on it ride it (W's
  list). Destroyed by pressing ability 1 again, or by throwing a new one (BZ 2023-04-25); option "Interact cancels Petal
  Platform" (W). Hacked: can't trigger for 10 s, or a raised one is destroyed (W). Affects CC-immune heroes (W).

## Rejuvenating Dash (jump in the air)
- "Dash towards your traveling direction and lightly heal yourself." (data; W).
- **Cooldown 5 s** (W), **heals 55** (BZ 2026-04-14: 45 -> 55). Horizontal only, ignoring falling speed (W).
- **6.15 m on the ground, 6.9 m in the air**, over **0.3 s**; speed +425% (28.9 m/s) on the ground, +340% (24.25 m/s) in the
  air (W). (28.9 m/s for 0.3 s would be 8.7 m: W's speed and duration don't multiply to its range.)
- Key: Jump while airborne (BZ 2023-04-25: moved from ability 1). With the legacy scheme, ability 1 (W).

## Life Grip (ability 2)
- "Pull an ally to your location, protecting them as they travel." (data; W).
- **Cooldown 18 s** (BZ 2024-11-12: 19 -> 18), **heals the ally 75** (BZ 2024-11-12: 50 -> 75), **range 30 m** (W), heroes
  only (W). Cleanses the ally (BZ 2026-04-14).
- Pull **25 m/s** (BZ 2026-08-11: 30 -> 25), **"Targets pulled from further than 15 meters move slower and accelerate over
  time"** (BZ 2026-08-11; how much slower isn't given). The ally goes to where Lifeweaver cast it, not where he is now (W). It
  can path around corners, never through terrain (W).
- During the pull: no damage or crowd control on the ally; projectiles that hit them are blocked; damage over time pauses
  (W). It doesn't cleanse debuffs from before (W; the 2026-04-14 cleanse is newer than this line).
- The ally can **cancel with Jump after 0.6 s** (BZ 2026-08-11; a hero setting turns that off), and can use movement
  abilities after **0.75 s** to break out (BZ 2023-08-10: lockout 0.45 -> 0.75). It interrupts the ally's own movement
  abilities when it starts (W).
- Can't target allies casting certain ults (W's list); Reinhardt during Earthshatter (BZ 2023-05-09). Breaks tethers (BZ
  2026-02-10 "Break" keyword: Life Grip is an example). Option: Life Grip sensitivity (view angle; not given) (W).

## Tree of Life (ultimate)
- "Place a tree that instantly heals allies upon sprouting and continues healing periodically as it lives." (data; W).
- **2400 points** (W since 2026-06-17; 2200 before, x 1.09 = 2398 for BZ 2026-06-16's "+9%"). Placed up to **30 m** away
  (W). Confirm with primary fire (default), cancel aiming with secondary; options ability 3 press / release (W). Cancel the
  tree with ability 3 (default; option off) (W).
- **150 at once** as it sprouts, then **90 a pulse every 1.75 s, 8 pulses**, radius **12 m**, tree **15 s** (W; pulse
  75 -> 90 BZ 2024-04-16; 15 s via W's 6v6 log 2025-03-18). 870 per ally at most (W). Healing needs line of sight past
  terrain, not past barriers (W).
- **Overhealth: 50% of its overhealing, up to 100** (BZ 2026-06-16: 100% -> 50%; from BZ 2023-08-10), kept **6 s** after
  leaving the radius or the tree ending (BZ 2024-11-12: 2.5 -> 6). Overhealth no longer vanishes when the tree does (BZ
  2025-08 hotfix fix).
- The tree has **1200 health** (BZ 2023-04-25: 1000 -> 1200), collides and blocks sight; it crushes most deployables it lands
  on (W's list); can't be hacked; EMP damages it (BZ 2023-08-10).

## Medic (sub-role passive) and regeneration
- Medic (BZ 2026-02-10): "Healing allies with your weapon also heals you." **40% of healing dealt** (BZ 2026-03-10: 25 ->
  40; W Sub-Roles). Stacks per ally, counts healing modifiers on both, gives no ult charge (W). BZ 2026-02-10 also lists
  "-25% ult charge gain above 100%" under Medic (a copy of Tactician's line; W doesn't have it).
- No support role passive since BZ 2026-02-10. Everyone regenerates 10 HP/s + 5% of max health a second after 6 s without
  damage (W Global Passive Abilities; BZ 2026-02-10 "remains unchanged"). Damaging an enemy cuts their healing 30% (15% on
  tanks) for 3 s (BZ 2026-02-10, global).
- His old passive Parting Gift (a heal dropped on death) was removed (BZ 2023-04-25).

## Perks (minor at level 2, major at level 3; W perk XP 1975 / 4940)
- Minor, **Petal Protection**: "Allies heal 20 health per second while standing on Petal Platform." (BZ 2026-02-10; data).
  3.84 every 0.192 s; includes Lifeweaver; heals whether or not the platform can rise (W).
- Minor, **Dashing Escape**: "Rejuvenating Dash's distance is increased by 30%." (BZ 2026-02-10; data). 8 m on the ground,
  8.97 m in the air (W).
- Major, **Superbloom**: "Thorns detonate for 40 extra damage when enough stick within 2.5 seconds." (data; BZ 2025-03-18:
  30 -> 40, 1.5 -> 2.5 s). **16 thorns** (BZ 2025-05-20: 20 -> 16). Not on deployables or barriers (W).
- Major, **Sow the Seed**: "Quick Melee with Healing Blossom equipped tosses a seed forward. Allies who pick up a seed gain
  75 overhealth and 25% movement speed for 5 seconds." (BZ 2026-02-10). Cooldown **11 s** (BZ 2026-04-14: 14 -> 11). One
  seed at a time; it drifts to an ally within 5 m; Lifeweaver can't take it; its 5 s start when it lands, the last 3 s it
  pulses; Healing Blossom only, no ammo, works while reloading (W).
- Removed: Lifeweaving, Cleansing Grasp, Petal Power (BZ 2026-02-10); Life Cycle (BZ 2025-08-26).

## Controls (default; BZ 2023-04-25, W hero options)
Primary: Healing Blossom (hold, release). Secondary: Thorn Volley (swaps and fires). Ability 1: Petal Platform (again: destroy
it). Jump in the air: Rejuvenating Dash. Ability 2: Life Grip. Ultimate: Tree of Life (aim, primary to confirm; ability 3
again later to destroy it). Quick melee (with Sow the Seed: the seed). Legacy scheme (option): both weapons on primary with
weapon keys, Petal Platform on secondary, Dash on ability 1 (W).

## Lore and voice
- Niran Pruksamanee, "Bua", 31, Thai; English voice Phuwin Tangsakyuen (W). Released in Season 4, 2023-04-11 (W; BZ
  2023-04-11 via W).

## Not given by any source (ours when used)
Healing Blossom's targeting cone and how long a target stays valid outside it; its passive charge rate as a number
(only W's 5.4 s); the homing delay. Thorn Volley's exact spread pattern. Petal Platform's size, its rise time ("cast 3 s" in W
is unexplained), its descent speed, how long the trigger delay is, how long before the trigger radius shrinks. Rejuvenating
Dash's real duration (W's 0.3 s and speeds don't match its ranges). Life Grip's cone, its speed curve past 15 m, its path,
how long it lasts at most. Tree of Life's growth time, size, collision shape and when the first pulse comes. Sow the Seed's
throw speed.

## Disagreements between the sources
- Healing Blossom's full charge: BZ 1.15 s (2024-03-12); W's text "1.2 seconds ... while actively firing" (infobox 1.15 + 0.3).
- Rejuvenating Dash: W's 28.9 / 24.25 m/s over 0.3 s are 8.7 / 7.3 m, not its own 6.15 / 6.9 m.
- Ult cost: W 2400 (2200 x 1.09 = 2398); no BZ number.
- Petal Platform cast time 3 s (W) has no explanation in W or BZ.
- Life Grip: W's "does not cleanse debuffs applied beforehand" predates BZ 2026-04-14's "now cleanses allies when pulled".
- The game data disagrees with several of these: listed in `lifeweaver-map.md` ("Disagreements with lifeweaver.md").
