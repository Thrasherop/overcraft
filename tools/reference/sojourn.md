# Sojourn in live Overwatch: numbers and sources (research, 2026-11-01)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only, filled in 2026-11-01 from the data pass
([sojourn-map.md](sojourn-map.md); detail in `E:\Overwatch Archive\research\sojourn\work_kit\kit_sojourn.md`). **BZ** =
Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read 2026-11-01:
every month from 2022-10 to 2026-11; 2026-11 is empty: the last patch is **2026-10-20**, a bug fix with nothing
for her). Dates are the patch titles' (the page's date line differs for some: 2024-03-12 shows March 8, 2025-11-11 shows
November 6, 2026-02-10 shows February 9). **The last patch that touched her is BZ 2026-10-06** (Season 5: hit volumes head
14.9 -> 16, torso 20.5 -> 23.5, legs 17 -> 18.5, and the global "large" hitscan size 0.07 -> 0.04 m, which W applies to
Charged Shot); **the last change to her kit is BZ 2026-06-16** (Friction Generators 50 -> 75 energy); **the last change to her
base kit is BZ 2025-11-11** (Disruptor Shot 80 -> 90/s, 30 -> 40 m/s, travel 1 -> 0.75 s). **Nothing for her on 2026-10-13 or
10-20**, so the data's build (10-06) should hold every live value. **W** = the Overwatch wiki on Weird Gloop through its
MediaWiki API (`overwatch.weirdgloop.org/api.php`, read 2026-11-01: Sojourn revision **408636** of 2026-10-31T08:10Z, plus all
266 Sojourn revisions since 2022-09 diffed field by field; Sojourn/Stadium 393897; Sub-Roles 406777 (Sharpshooter redirects
there); Template:Sharpshooter value table 399877; Critical hit 404694; Global Passive Abilities 402069; Hit points 401654;
Movement speed 402311; Melee 402309; Ultimate ability 395578; Perks 354523). Railgun, Power Slide, Disruptor Shot and Overclock
redirect to hers; "Slide Jump" does not exist (the jump is part of **Power Slide**; the secondary fire's page name is **Charged
Shot**, "Railgun Alt Fire" before W rev 278288). The fandom API returned the same text (rev 377641) and no hero options.
Blizzard's hero page and Liquipedia were not read. Raw pages, the revision dump, the field-by-field diff and the patch notes as
text stayed in the scratchpad.

Each number gives D first, then BZ, then W. Confidence marks are the public sources' (as written before the data pass):
**H** BZ and W agree (or W gives it and BZ's history leads to it); **M** a single public source; **L** the sources
disagree, or a guess. Where they differ, both are given here and in "Disagreements"; where D settles one, "Disagreements"
says so, and [sojourn-plan.md](sojourn-plan.md) says which one Overcraft uses.

Left out (not the live game): **Stadium** (BZ from 2025-10-14, tuned 2025-11-11, 2026-02-10, 06-16, 06-18 fix, 06-25; W
Sojourn/Stadium: powers Drill Kick, Fine-Tuned Thrusters, Unconventional Tactics, Zoom Zoom Zoom, Commotion Cycle, Experimental
Tech, Hard Stuck, Conductor Chase, Dynamic Dispatch, Overcharge, Enhanced Targeting System, Aftershock; items Shah-Mat
Strategem, CAF Calibrator, Overclocked Surge, Liao's Logistics Data, Charge Converter, Afterburners, Rocket-Powered Javelin;
removed Lock and Load, Reverberation Rounds, Espresso Shots, Murphy's Motor Module, Energy Leech; Stadium's Charged Shot max is
115, BZ 2026-06-16), **Community Crafted** (BZ 2026-06-30: 250 health, Dual Thrusters base kit, Deceleration Field heals allies
30/s, new major Dual Disruptor (second Disruptor charge), Railgun fire rate +25%, Charged Shot 120 -> 80 and falloff -20 m,
Power Slide knocks enemies up; in that mode only), April Fools (BZ 2024-04-01 Disruptor pulls in and can be deployed early;
2025-04-01 "Raingun" charge to 300% with self-knockback; 2026-04-01 Underwatch "energized" slide and Disruptor), Haunted
Masquerade masks (BZ 2025-10-14: Sojourn Mask +50% ult charge from critical damage, Orisa's javelin makes a Disruptor Orb; Orisa
Mask lets Sojourn attach Disruptor to an allied Orisa), Quick Play Hacked (BZ 2026-07-30: healing reduction 15%, sub-role
tuning), Mystery Heroes (6v6 and perks off since BZ 2026-04-14), **6v6** (no Sojourn-specific line in BZ 2024-12-17's 6v6
Experiment or 2026-04-14's 6v6 changes; "all 5v5 hero changes apply to both modes unless explicitly stated", BZ 2026-04-14),
the 2022 betas (W changelog: 13 -> 15 -> 14 shots/s, 40 -> 45 ammo, alt fire width 0.05 -> 0.1 m, slide 7 -> 6 s). Her
removed perks (Extended Mag, Adhesive Siphon) and the removed Damage role passive are history only.

**Patches that touched her (live 5v5):** **2022-10-04 release** (Overwatch 2 launch; W: energy from non-player targets
-50% more, no BZ line); 2022-12-06 primary 9 -> 10, energy drain delay 8 -> 5 s, Charged Shot falloff start 70 -> 40 m, crit
2 -> 1.5x, damage 30-130 linear with energy, Overclock charge +20%, Damage passive reload 25 -> 35% and no speed; 12-15 spread
+28%, **Disruptor no longer slows**, Power Slide 6 -> 7 s. 2023-01-24 **energy 5 per hit on enemy players** (no longer from
damage), primary 10 -> 9; 03-07 torso hit volume +20% width; 07-11 12 shots to max spread (from 8), spread -12%. 2024-01-09
energy no longer decays below 25; **02-13 (Season 9) health 200 -> 250**, Charged Shot base size 0.1 -> 0.07 m, global
projectile sizes, ult costs +10%, quick melee 30 -> 40, Damage passive becomes healing reduction; 03-12 global size for
>50 m/s projectiles 0.1 -> 0.075 m; **06-20 Charged Shot 30-130 -> 1-100, pierces players without Overclock, 14 -> 16
shots/s, Overclock 100 -> 120/s**; 08-20 spread 2 -> 1.6 degrees, crits 10 energy, decay 33 -> 15/s after 7 s; 10-15 no 25
floor, decay delay 7 -> 12 s, Disruptor 60 -> 80/s and 25% of its damage as energy; **12-10 Charged Shot 1-100 -> 20-120**,
large hitscan 0.08 -> 0.07; 12-16 16 -> 15 shots/s, Disruptor no energy, Overclock 120 -> 100/s. 2025-01-07 decay delay 12 ->
9 s; **02-18 perks added** (Extended Mag, Overcharged minor; Dual Thrusters, Adhesive Siphon major); 03-25 Power Slide 7 -> 8 s,
max duration 1.2 -> 1 s; 06-24 Damage passive 25 -> 30%, regeneration delay 5 -> 6 s; **08-26 health 250 -> 225, 15 -> 16
shots/s, Power Slide 8 -> 7 s, Extended Mag and Adhesive Siphon out, Deceleration Field (minor) and Friction Generators
(major, 50) in**; **11-11 Disruptor 80 -> 90/s, 30 -> 40 m/s, travel 1 -> 0.75 s**. **2026-02-10 (Season 1): Sharpshooter
sub-role, Damage role passive removed, its healing reduction made global**; **06-16 Friction Generators 50 -> 75**; **10-06 hit
volumes** and global hitscan sizes. Nothing on 2026-04-14 (no perk-cost line for her), 10-13 or 10-20. Bug fixes only:
2022-11-17 (flag pickup during slide), 2023-01-24 (skin), 05-09 (Disruptor on the Push bot), 06-13 (Disruptor screen effect),
10-10 (Kinetic Grasp converts Disruptor), 10-31 (rumble on slide jump), 2024-12-19 (Disruptor damage wrong), 2025-02-25 (Dual
Thrusters third slide), 08-26 (Overcharged charge lost after ult), 08-28 (Friction Generators blocked ult charge), 11-11
(Overcharged charge lost after a primary shot).

## Base
- **225 health, no armor or shields** (D 3505 #106: health 04D8 225, armor 04D7 0, shields 04D9 0; BZ 2025-08-26 "Base health
  reduced from 250 to 225"; BZ 2024-02-13 200 -> 250; W infobox 225 since rev 263628) **H**.
- **Damage**, sub-role **Sharpshooter** (D loadout 0DDD Subrole: Sharpshooter; BZ 2026-02-10 "Heroes: Ashe, Cassidy, Hanzo,
  Sojourn, Widowmaker"; W) **H**.
- Speed **5.5 m/s**, backward 4.95, crouched 3 (D mover on 2280 = the hero default, all 53 fields equal to Juno's 5106; default
  falling-damage component, no fall node; W Movement speed, the global walk) **M**. Global jump 5.72 m/s (0.98 m apex), gravity
  17.5 m/s^2 easing to a 30 m/s fall, **no fall damage from any height** (W Movement speed) **M**.
- **Hit volumes** (no units given): **head 16, torso 23.5, legs 18.5** (D not read: hit volumes not mapped; BZ 2026-10-06 14.9
  -> 16, 20.5 -> 23.5, 17 -> 18.5; W copies it) **H**. BZ 2023-03-07: torso width +20% because her stance angles the torso.
  Arms, hands: not given.
- **Quick melee**: the global one, **40 damage, 1 s cooldown, 2.5 m**, active 0.128 s, a very small knockback (D melee 02D0 40; BZ
  2024-02-13 30 -> 40; W Melee) **H/M**.
- Ult charge (W Ultimate ability): 5 points/s passively, 1 point per damage, none from ult damage, half from damage to
  overhealth, -40% from damage on tanks; none while Overclock is active **M**.

## Railgun (primary fire; fast projectiles that build energy)
- "Rapid firing projectiles that generate energy on hit." (W official description).
- **9 damage a shot** (D 04C5 9, volley 1A23 #502; BZ 2023-01-24 10 -> 9; W) **H**. **Crits 2x (18)** (D 1CB4 2, 04EE 1.0; W
  headshot field, Sharpshooter table "18") **M**.
- **16 shots/s** (D 04C6 16; BZ 2025-08-26 15 -> 16; W) **H**. **Ammo 45, 1 a shot; reload 1.2 s** (D mag 04D4 45, reload 04C8
  1.2 s, mag full at 0.96 s, fire locked to 1.2 s (#287 / #283); W since its first revision, 2022-09) **M**. DPS 144 while
  firing, ~100.9 with reloads (W) **M**.
- **Projectile speed 150 m/s** (D 04D3 150, #502; W since rev 285212, 2026-03-12, "Evidence: youtu.be/FUFo0Pu9Btc"; 160 before)
  **M/L**.
- **Projectile size 0.175 m** (0.1 base + 0.075 global) (D 2901 0.1 + global 28A6 0.075, #502; BZ 2024-03-12 global modifier for
  >50 m/s projectiles 0.1 -> 0.075; W) **H/M**.
- **Spread 1.6 degrees vertical, 0.64 horizontal at most** (2.5x more vertical), **max after 12 shots** (D 04C7 1.6 max over
  1E55 12 shots, recovery after 0.3 s in 0.25 s; no vertical / horizontal split in her graph; BZ 2024-08-20 2 -> 1.6, 2023-07-11
  8 -> 12 shots; W) **H** (the horizontal **M**). Recovery rate: not given.
- **Falloff: none** (D: volley #502 has no falloff fields, the charged shot's #450 has them; W lists no falloff for the
  Railgun; no BZ line ever names one) **L** before the data, settled by D.
- **Energy: 5 per hit on an enemy hero, 10 per critical hit, 1 per hit on objects** (barriers, turrets, deployables) (D +5 hero,
  +10 crit, +1 non-hero (5 x 1CB3 0.2; +2 crit), +1 barrier tag B87, railgun damage id 47BE only, none during Overclock (1A23
  #597-#777); BZ 2023-01-24 "Each primary fire hit against an enemy player now grants 5 energy", BZ 2024-08-20 "Critical hits
  now grant 10 energy"; W) **H** (objects **M**: W; its changelog has a 2022-10-04 "reduced by an additional 50%" line with no
  BZ source). Not affected by damage boosts or reductions (BZ 2023-01-24 dev comment; W) **H**. Hitting allies: nothing.
- **Energy cap 100** (150 with Overcharged during Overclock) (D v971 100, +50 with Overcharged (#1485); BZ 2025-02-18; W) **H**.
- **Energy decay: 15/s, starting 9 s after she last gained energy, down to 0** (D 1E15 9 s wait (#596) then 191F 15/s to 0
  (#590); paused in Overclock and in a Friction Generators slide; BZ 2024-08-20 33 -> 15/s, 2024-10-15 no 25 floor, 2025-01-07
  delay 12 -> 9 s; W) **H**.

## Charged Shot (secondary fire; hitscan, spends the energy)
- "High impact shot that consumes stored energy and pierces enemies." (W).
- **Damage = 20 + energy: 21 at 1 energy, 120 at 100** (D 1A23 #745: 1CE8 20 + (04C9 120 - 20) x energy / 100; needs energy > 0
  (#451), spends it all (#1656); BZ 2024-12-10 "1-100 damage to 20-120 damage", BZ 2022-12-06 "1 energy converts to 1 damage
  added"; W) **H**. Needs at least 1 energy; spends all of it (W) **M**.
- **Headshot 1.5x** (180 at 100 energy) (D 04EF 0.5 = x1.5, M; BZ 2022-12-06 2 -> 1.5; W) **H**.
- **Falloff 40 to 60 m, down to 50%** (60 at full charge) (D 1CE6 40 / 1CE5 60 / 1CE7 0.5 on volley #450; BZ 2022-12-06 start 70
  -> 40; W 40-60 m since 2022-12-06, the 50% floor in a W comment) **H/M**. (BZ 2026-06-30's Community Crafted "min and max
  reduced by 20m" confirms a max exists.)
- **Pierces enemy players** at all times (D pierce block m_C5143E2E on #450, count 3: L meaning; the same with or without
  Overclock; BZ 2024-06-20 "no longer requires Overclock to pierce enemy players"; W) **H**. Through barriers or terrain: not
  given.
- **Size 0.11 m** (0.07 base + 0.04 global "large" hitscan) (D 2902 0.07 + global 28A2 0.04, #450; BZ 2024-02-13 base 0.1 ->
  0.07, 2026-10-06 large 0.07 -> 0.04; W rev 401600) **H/M** (which global class applies is W's call). It no longer grows with
  charge (W rev 248920, 2024-12).
- Cast **0 + 0.64 s** recovery (D v2447 0.65 s before either fire can shoot again, 1A23 #438, M meaning; W rev 282755,
  "verified") **M**. No cooldown, no spread (W) **M**.

## Power Slide (ability 1; movement)
- "Ground slide that can cancel into a high jump." (W).
- **Cooldown 7 s** (D 1CE9 7, set at the slide's end (1E6F #368 -> #1035): M; BZ 2025-08-26 8 -> 7; W) **H**. From the start or
  the end of the slide: not given.
- **Slide at most 1 s** (D 04E2 1 s (0.75 with Dual Thrusters); BZ 2025-03-25 "Maximum duration reduced from 1.2 to 1 second";
  W) **H**. **+160% (14.3 m/s) on the ground, +280% in the air** (D 1CFC 1.6 speed bonus, ramping 0.4 -> 1 in 0.2 s and to 0.3
  over the last 0.3 s; in the air 1CFD 1.2 more (M additive); fall capped at 2 m/s; W since 2024-08-02; the air value +230% ->
  +280% on 2025-09-29, unsourced) **M/L**. About **13 m** of slide (W rev 254109) **M**. Not counted against the +75% speed cap
  (W) **M**.
- **The jump (pressing jump during the slide): 17 m/s up, +273% (20.5 m/s) forward, about 11.7 m** (D 1E6F #411: vertical speed
  cancelled, then (dir x 6.5, 1CFA 17, dir x 6.5) added (M), gravity x1.4 for 0.25 s; ground within 2 m or 0.15 s coyote; W
  since 2024-08-02 / 2025-03-26) **M**. Its height: not given (L, ours: 17^2 / (2 x 17.5) = 8.3 m, ignoring the falling
  formula's easing). Only from a walkable surface; the slide itself can start in the air (W) **M**.
- She can **shoot and use abilities while sliding and in the jump** (W) **M**. It breaks fences and other breakables on the
  ground, not in the jump (W) **M**. "Crouch hover": no source mentions one.
- Counts as her **movement ability** for Sharpshooter (W's Sharpshooter table lists her) **M**.

## Disruptor Shot (ability 2; projectile, area)
- "Launch an energy burst that deals damage to enemies within it." (W).
- **90 damage/s** (17.28 every 0.192 s) for **3.5 s = 315** (D 04CB 90/s (alt 100, not live), 04DD 3.5 s, field 1443 starting
  0.1 s after it opens; BZ 2025-11-11 80 -> 90; W, duration fixed 4 -> 3.5 s on 2025-06-20) **H** (dps) / **M** (duration). No
  head hits; **ignores armor's reduction**; fixed damage, not amplified by damage boosts (W, tested 2026-04-13) **M**.
- **Cooldown 15 s** (D 1CE4 15, written 0.5 s after launch (M); W since its first revision; no BZ line ever) **M**.
- **Projectile 40 m/s, 0.75 s at most = 30 m**, size 0.2 m (D 04D5 40 m/s, 10FC 0.75 s, size 2905 0.2 m, from hp 0002 (her left
  wrist); BZ 2025-11-11 "30 to 40", "1 to 0.75 seconds"; W) **H** (size **M**). It becomes the field on hitting an enemy hero,
  an object, terrain, or at the end of its travel (W) **M**.
- **Radius 4 m** (D 04D6 4; W since rev 215923, 2023-11-16, 5 m before; unsourced) **M**.
- **No slow** since BZ 2022-12-15 ("No longer slows enemy movement") **H**; the Deceleration Field perk brings back 25%.
  **No energy** since BZ 2024-12-16 **H**.
- Barriers stop the projectile (it opens there) but the field goes through them; Defense Matrix and similar destroy it and
  Deflect reflects it before it opens (W) **M**. Kinetic Grasp converts it (BZ 2023-10-10 fix) **H**.

## Overclock (ultimate)
- "Railgun energy auto-charges for a short duration." (W; the original text added "and charged shots pierce enemies", but
  piercing is base kit since BZ 2024-06-20).
- **2300 points** (D 0AFB 2300; W since rev 220474, 2024-02-14, from 2100 at launch; BZ only 2024-02-13 "All Ultimate ability
  costs increased by 10%" = 2310) **H/M**.
- **8 s** (D 04CC 8 s, after a 0.75 s cast, then a 0.45 s tail; W since 2022-10-27, 9 s before; no BZ line) **M**.
- **Energy fills at 100/s** (full in 1 s) (D 04DF 100/s from the cast to the end of the tail; BZ 2024-12-16 120 -> 100, after
  2022-12-06 +20% and 2024-06-20 100 -> 120; W) **H**. It starts charging at once; she can't fire until the 0 + 0.736 s cast
  ends (W) **M**. Her Charged Shot can still headshot (W Critical hit) **M**. Max-charge DPS 120/s (W) **M**.
- Not hackable (W Ultimate ability) **M**. W's keyword "reloads" (ammo refilled on use): not stated anywhere else **L**.

## Sharpshooter (sub-role) and the global passives
- **Sharpshooter** (BZ 2026-02-10: "Critical hits reduce movement ability cooldowns. Cooldown Reduction: 0.75% of damage
  dealt"): **0.0075 s off Power Slide per point of critical damage** (W table: a Railgun crit 18 -> 0.135 s, a full Charged Shot
  headshot 180 -> 1.35 s, Overcharged 255 -> 1.91 s) **H** (W's infobox field says "0.75% of base cooldown": see
  Disagreements). Never changed since (BZ 2026-03-10 to 10-20 tune other sub-roles only).
- **The Damage role passive is gone** (BZ 2026-02-10; W) **H**. History: reload on elimination (25 -> 35%, 2022-12-06), then
  healing reduction from 2024-02-13 (20% for 2 s; 15% 03-12; 20% 04-30; half on tanks 07-05; 25% / 3 s 12-10; 30% 2025-06-24).
- **Regeneration: 10 HP/s + 5% of max a second after 6 s without damage**, **21.25 HP/s** for her (225 max) (D regen delay 0B0F
  6; BZ 2025-06-24 5 -> 6 s, 2026-02-10 "unchanged"; W) **H**.
- **Damage dealt cuts the target's healing by 30% (15% on tanks) for 3 s**, global since BZ 2026-02-10 (W Global Passive's
  box flags it "removed": see Disagreements; BZ 2026-07-30's 15% was Quick Play Hacked only) **M**.

## Perks (minor at perk level 2, major at 3)
- **Perk XP 1695 to the minor, 4235 to the major** (D 12D5 1695 then 12D6 2540 = 4235; W rev 265678, 2025-09-17) **M**. BZ never
  changed hers (no Sojourn line in 2025-09-16's or 2026-04-14's perk cost changes). XP ramps from minute 3 to x2 at minute 7 (W
  Perks) **M**.
- Minor, **Deceleration Field** (BZ 2025-08-26): "Enemies hit by Disruptor Shot are slowed by 25%." **-25% (4.125 m/s)**,
  ending as soon as they leave the field (D 1443 #319: move-speed reduction +0.25 on heroes inside, M; W) **H/M**.
- Minor, **Overcharged** (BZ 2025-02-18): "Railgun's maximum energy is increased by 50 while Overclock is active." Charged Shot
  up to **170** (255 headshot) (D v971 + 50 = 150 in Overclock, drained to 100 at 20/s after (1A23 #1485-#1488); 20 + 150 = 170;
  W) **H/M**.
- Major, **Dual Thrusters** (BZ 2025-02-18): "Power Slide gains an additional charge and its jump height shifts to lateral
  movement." **2 charges; slide 0.75 s (-25%); jump 13.75 m/s up, +354% (25 m/s) forward** (D 2 charges (1E6F #1033), slide
  x0.75 (51EB #422), jump (dir x 11, 13.75, dir x 11) (#1031); W rev 401044, 2026-10-04) **H/M**. Charge count fixed at 2 (BZ
  2025-02-25 fixed a third slide).
- Major, **Friction Generators** (BZ 2025-08-26): "While sliding, Power Slide generates up to 75 Energy." (D 51EB #468: +1
  energy 75 times over the slide, decay paused; BZ 2026-06-16 50 -> 75; W) **H**. The rate (per second or per metre): not given.
- Removed: **Extended Mag** (minor: +15 ammo, BZ 2025-02-18 -> 08-26), **Adhesive Siphon** (major: Disruptor sticks to enemies
  and gives energy; W: the stuck target takes 50% less, 25% of damage as energy; 2025-02-18 -> 08-26).

## Controls and hero options (W)
Primary fire: Railgun. Secondary fire: Charged Shot. Ability 1: Power Slide (Jump during it: the high jump). Ability 2:
Disruptor Shot. Ultimate: Overclock. Reload. Quick melee. **Hero options: none listed** by W (no "Hero-specific options"
section, unlike Juno's page), the fandom copy or BZ. Default key letters: not in these sources (BZ's 2024-04-01 April Fools
note "pressing E again" for Disruptor Shot implies E on PC).

## Lore and voice
**Vivian Chase**, 51 (47 before the Overwatch 2 change), born January 12, Canadian; former Canadian Army and special forces,
former Overwatch captain, acting commander, now based in Gothenburg; twin sister Valentine Chase-Chapman; weapon an "Okoton
Kelvin-87 Railgun" (W; BZ 2025-04-01 dev comment). English voice **Cherise Boothe**; released **2022-10-04** with Overwatch 2
(beta 2022-04-26), the 33rd hero (W; BZ 2022-10-04).

## For the Minecraft port to decide (no Minecraft equivalent)
- **Health**: 225 = 18 MC health (9 hearts) at 250 = 20; no armor or shields to model.
- **Sub-tick fire**: 16 shots/s is 0.8 a tick: shots must be batched/accumulated per tick; 150 m/s projectiles move 7.5
  blocks a tick (needs swept collision, not per-tick position checks); 0.175 m size.
- **Energy**: our own meter (0-100, HUD), +5 per mob hit, +10 per "crit", +1 per object; decay 15/s (0.75/tick) after 180
  ticks without gain. Which MC entities count as "heroes" (mobs, players) and which as "objects" (armor stands, end crystals,
  boats, shields?) is ours.
- **Crits**: MC has no head hit box: a head test (eye height band) is ours, for Railgun 2x, Charged Shot 1.5x and Sharpshooter.
- **Charged Shot**: a piercing hitscan ray (through every mob in the line); whether it stops at blocks (surely) and at
  shields/barriers is ours. Falloff 40-60 m is beyond normal MC fights.
- **Power Slide**: a crouch-height hit box at 14.3 m/s (0.715 blocks/tick) for 20 ticks, then the jump at 0.85 blocks/tick up
  and 1.03 forward; MC's 0.6 block step height and slopes need our rules; "breaks breakables" means Overwatch props, so no
  block breaking (ours). Fall damage: none in Overwatch; the owner's rule (#35: no fall damage when an ability lands her)
  covers the slide jump; her safe height for plain falls is ours.
- **Disruptor Shot**: a 4 m sphere entity, damage ticks of 17.28 every 0.192 s (3.84 ticks: round to 4 ticks of 18, or
  accumulate), ignoring armor (MC's bypasses-armor damage type).
- **Overclock**: 100 energy/s = 5 a tick; the HUD shows the meter filling.
- **Sharpshooter**: cooldown cut of 0.0075 s per crit damage point, in ticks (Charged Shot headshot 1.35 s = 27 ticks).
- Healing reduction and regeneration: our own timers (MC's natural regeneration is food-based).

## Not given by any source
The Railgun's falloff (none listed) and spread recovery; whether Overclock refills ammo; Charged Shot through barriers or
shields; whether Power Slide's cooldown starts at the start or end; the slide jump's height (only its 17 m/s); any crouch
hover or hold behaviour; how Power Slide ends early (releasing, jumping, hitting a wall); Friction Generators' rate; Disruptor
Shot's cast time, arc or gravity (none listed), and whether it hurts her; hero options; arm and hand hit volumes and the
units; which Minecraft mobs would count as "heroes", "objects" or "tanks" (ours).

## Disagreements between the sources
- **Sharpshooter's formula:** BZ "0.75% of damage dealt" and W's detail table (0.0075 s per critical damage point) agree; W's
  own infobox field says "0.75% of base cooldown" (7 s x 0.75% = 0.0525 s a crit). D settles it.
- **Railgun projectile speed:** W 150 m/s since 2026-03-12 (a video), 160 m/s from 2022-10 to then; no BZ line.
- **Disruptor Shot:** W duration 4 s until 2025-06-20, then 3.5 s ("fixed"), and total 320 -> 280 -> 360 -> 315 as editors
  caught up; radius 5 -> 4 m on 2023-11-16 with no BZ line; launch W said 20 then 30 m/s (BZ gave 30 only in 2025-11-11).
- **Power Slide in the air:** W +230% (2025-09-06) then +280% (2025-09-29), both unsourced.
- **Ult cost:** W 2300; BZ's +10% on W's launch 2100 gives 2310 (rounding or a later quiet change).
- **Overclock and piercing:** BZ 2024-06-20 made piercing base kit; W's ability summary kept "causes it to pierce" until
  2026-10-04 and the official text W quotes from launch still mentions it.
- **Charged Shot size:** W counts it as "large" hitscan (0.04 m global, 0.11 m total); if it were "small", 0.02 m (0.09 m).
  W also showed it growing with charge (0.08-0.15 m) until 2024-12-17.
- **Healing reduction:** W's Global Passive box flags it "removed" while BZ 2026-02-10 made it baseline for all heroes.
- **Overclock duration:** W 9 s until 2022-10-27, 8 s since; no BZ line.
- **What the data settles** (D, build 154088; detail in [sojourn-map.md](sojourn-map.md)): Sharpshooter is **0.0075 s per
  point of critical damage, Power Slide only** (23D7, 01C7 #2832; BZ and W's table right, the infobox wrong); Railgun **150
  m/s** (04D3) and **no falloff**; Disruptor **3.5 s, 4 m, 90/s, 40 m/s for 0.75 s** (W's current values right); ult **2300**
  (0AFB); Charged Shot **0.07 + the large 0.04 = 0.11 m** (28A2); Power Slide in the air **1.6 + 1.2** (1CFD, M additive: W's
  +280%); the slide jump **adds 6.5 m/s** along the slide to a **17 m/s** rise (M), so W's 20.5 m/s forward is that sum at
  full slide speed; Overclock **8 s after a 0.75 s cast, + a 0.45 s tail, filling 100/s from the cast**; piercing is the same
  with or without Overclock (BZ right, W's old text stale). Not settled by D: the Railgun's 1.6 / 0.64 vertical / horizontal
  split (only 1.6 in her graph) and the hit volumes (not read).
