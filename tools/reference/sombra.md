# Sombra in live Overwatch: numbers and sources (research, 2026-10-31)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only: **left empty here** (`D _`), filled in by the
data pass. **BZ** = Blizzard's live patch notes (`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read
2026-10-31: every month from 2022-10 to 2026-11; 2026-11 is empty: the last patch is **2026-10-20**, a bug fix with nothing
for her kit). **One patch after the data build touched her: the 2026-10-13 hotfix** (Machine Pistol 7 -> 6.5, Cyberspace
explosion damage 40 -> 20, Cyberspace cooldown 12 -> 13 s): **D will show the 2026-10-06 values for those three**. **W** =
the Overwatch wiki on Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`, read 2026-10-31: Sombra
revision **408595** of 2026-10-30T05:49Z, plus all 350 Sombra revisions since 2022-09 diffed field by field; Sub-Roles
406777, Global Passive Abilities 402069, Perks 354523, Ultimate ability 395578, Hit points 401654, Movement speed 402311,
Melee 402309, Roles 401659, Status effect 408615 (Hacked and Invisibility redirect there; no Weaken section), Health pack
402003, Healing 401540, Critical hit 404694, Deployables 406329). Hotfix, Cyberspace, Conspirator and Weaken have no pages
of their own; Hack, EMP, Machine Pistol, Stealth redirect to hers. Blizzard's hero page and Liquipedia were not read.
Every revision, date and old value: `E:\Overwatch Archive\research\sombra\work\public_sources.md`; raw pages stayed in
the scratchpad.

Each number gives D first (empty for now), then BZ, then W. Confidence (public sources only, to be revised once D is in):
**H** BZ and W agree (or W gives it and BZ's history leads to it); **M** a single public source; **L** the sources
disagree, or a guess. Where they differ, both are given here and in "Disagreements".

Left out (not the live game): **Community Crafted** (BZ 2026-06-30: Hack removed, Stealth on ability 1 for 8 s / +75%,
Translocator persisting on the ground with 5 HP, Virus hacking, reworked Encrypted Upload / Viral Replication, and Tactician
"no longer allows storage of excess ultimate charge" in that mode only), April Fools (BZ 2024-04-01 Virus spreading,
2025-04-01 stacking Hack, 2026-04-01 Disguise), Quick Play Hacked (BZ 2024-01-12, 2024-03-18, 2024-07-15, 2026-07-30), Hacks
& Hijinx (BZ 2025-06-24), Overwatch Classic Moth Meta (BZ 2025-01-21), Mystery Madness (BZ 2026-10-06), **6v6** (W Hit
points has a 6v6 row; no Sombra 6v6 line in BZ), **Stadium** (no Sombra entry in BZ or W). Her old kit (Hack, Virus,
Opportunist, Stealth as its own ability, the old perks) is history only.

**Role change (the owner asked):** **BZ 2026-10-06**, Season 5, "Hero Reworks": "Moved from Damage to Support role." and
"Moved from Recon subrole to Tactician subrole." Developer comment: "Sombra is being brought into the Support role with this
latest rework." W logs it under 2026-10-07 and flipped her role in rev 401340 (2026-10-06T18:25Z). Same patch: Hack, Virus
and Opportunist removed; Hotfix, Cyberspace, Conspirator and the Weaken status added; Translocator, EMP, Machine Pistol
retuned; perks Encrypted Upload, High-Speed Bandwidth, Viral Replication out, Life Hack, Data Packet, Cybersecurity in.
Before that she was Damage since release, sub-role Recon from BZ 2026-02-10.

**Patches that touched her (2025-2026, live 5v5):** 2025-01-21 EMP / Jagged Wall fix; 02-18 perks added; 03-18 perk tuning;
04-22 perks swapped; **05-20 stealth fade in 0.15 -> 0.3 s, damage reveal 1 -> 1.3 s**; 08-26 Healthy Hacker; 11-11 perk
tuning. 2026-02-10 (Season 1): Recon sub-role, Opportunist reworked, support passive removed, healing reduction made global,
heroes call out an invisible hero nearby; **04-14 perk costs -6% / -5%, stealth speed bonus -50% while revealed by damage;
04-23 that cut 50 -> 33%**; 06-16 Recon ping fix; **10-06 the Support rework**; **10-13 pistol 6.5, Cyberspace explosion 20,
cooldown 13** (after D's build); 10-20 a challenge fix only.

## Base
- **225 health, no armor, no shields** (D _; BZ 2024-02-13 200 -> 250, 2024-08-20 250 -> 225, nothing since; W 225) **H**.
  **Support**, sub-role **Tactician** (D _; BZ 2026-10-06; W) **H**.
- Speed **5.5 m/s**, backward 4.95 (D _; W Movement speed, the global walk; nothing of hers changes it) **M**. Jump and
  gravity: the global ones (W: 5.72 m/s jump, 0.98 m apex, 17.5 m/s² to a 30 m/s fall) **M**.
- Hit volumes: **head 14 -> 16, torso 25 -> 23, legs 15 -> 18** (D _; BZ 2026-10-06, units not given; W) **M**.
- **Quick melee 40**, cooldown 1 s (D _; BZ 2024-02-13 global 30 -> 40; W Melee) **H**.
- Ult charge (W Ultimate ability): 5 points/s passively, 1 point per damage or healing, none from the global regeneration or
  ult damage/healing, -40% from damage/healing on tanks; **none from hacked health packs** (their healing is credited to
  her) **M**. She starts gaining charge again **5 s after using EMP** (W) **M**.

## Machine Pistol (primary fire; hitscan)
- "Short-range automatic weapon." (W).
- **6.5 damage a bullet** (D _ (10-06 build: 7); BZ 2026-10-06 8 -> 7, **2026-10-13 7 -> 6.5**; W) **H**, falling off **from
  20 m to 35 m** (D _; BZ 2026-10-06 min falloff 15 -> 20 m; W 20 - 35) **H** to **1.95 at 35 m** (30%) (D _; W 6.5 - 1.95,
  rev 404768) **M**. Head hits **x2** (D _; W headshot ✓, the general 2x on Critical hit) **M**.
- **20 rounds a second** (0.048 s a round) (D _; W) **M**; **60 rounds** (D _; W) **M**; **reload 1.2 s**, the ammo back at
  **0.544 s** (D _; BZ 2023-10-10 1.4 -> 1.2; W) **H/M**. After letting go of fire, **0.16 s** before it can fire again (W)
  **M**.
- **Spread 0 rising to 2° max after 10 shots** (D _; BZ 2023-10-10 min 0.5 -> 0, BZ 2026-10-06 max 2.3 -> 2°, shots to max 6
  -> 10; W 2°, 10 shots) **H**. Recovery speed of the spread: not given.
- Hitscan size **0.02 m** (D _; BZ 2026-10-06 global "small" hitscan 0.04 -> 0.02; W 0 base + 0.02 global) **H**.
- DPS 130 while firing (W's calculator, 6.5 x 20) **M**.

## Hotfix (secondary fire; targeted)
- "Heal an ally over time for a set amount of health. Can also freely hack health packs and most enemy deployables." (W).
- **Heal: 65 at once, then 25 HP/s, up to 175 in all or 10 s (BZ 2026-11-03; 12 before and in D), whichever first** (D _; BZ 2026-10-06; W: "always heals a total
  of 175, unless it expires"; so 65 + 110 over 4.4 s at the base rate) **H**. Healing modifiers change its rate, not the 175
  capacity; re-applying resets duration and capacity, it doesn't stack; no limit on how many allies carry one (W) **M**.
  Targets allied heroes and B.O.B. only (W) **M**. Damage doesn't interrupt the heal (W) **M**.
- **2 charges, cooldown 8 s** (D _; BZ 2026-10-06; W; BZ's Data Packet: -4 s) **H**. **Range 17 m** (D _; BZ; W) **H**.
  **Cast 0.5 s** (D _; BZ "Cast time is 0.5 seconds"; W 0.5 s startup + 0.3 s recovery, rev 401697) **H/M**. Ignores barriers
  (W) **M**. Hotfix sensitivity option, 10-100%, default 100%: narrows the targeting angle (W) **M**.
- **Hacking** (what hack does now: W Status effect "Hacked", under EMP below; for a health pack or deployable, the
  effects listed here): health packs and most enemy deployables, **no charge spent, its own 6 s cooldown** (D _; BZ; W) **H**; **taking
  damage while hacking cancels it and starts the 6 s** (D _; BZ; W) **H**. Hacked **health packs: 30 s** (D _; BZ 2026-10-06
  45 -> 30; W) **H**: enemies can't use them, they respawn **4x as fast** (2.5 s small, 3.75 s large; normal 10 / 15 s), allies
  see them through walls within 30 m (W) **M**. Deployables (W): Immortality Field and Anchor Drone destroyed; Healing Pylon
  and Deploy Turret disabled 10 s; B.O.B. 3 s; Petal Platform destroyed if lifted, else 10 s **M**.
- Usable while stealthed: it **reveals her but doesn't end stealth** (D _; BZ; W) **H**.

## Cyberspace (ability 1; projectile)
- "Launch a projectile that explodes on impact, creating a zone that weakens enemies and heals allies." (BZ; W).
- **Explosion: 20 damage** (D _ (10-06 build: 40); BZ 2026-10-06 40, **2026-10-13 40 -> 20**; W 20) **H**, **40 healing**
  (D _; BZ 2026-10-06 40, untouched on 10-13; W) **H**.
- **Zone: 5 m radius, 3 s, 25 damage/s to enemies, 30 healing/s to allies** (D _; BZ; W) **H**. Effects linger **0.5 s on
  enemies, 0.75 s on allies** after they leave (W) **M**. (W Healing lists Hotfix and Cyberspace as ally heals, and
  Cyberspace's Weaken among the healing modifiers.)
- **Weaken: enemies in the zone deal 50% less damage and healing** (D _; BZ 2026-10-06 "reduces the target's own damage
  and healing output by 50%"; W) **H**. It doesn't stack, and also cuts projectiles fired before it landed; deployables only
  when the deployable is inside (W) **M**.
- **Cooldown 13 s** (D _ (10-06 build: 12); BZ 2026-10-13 12 -> 13; W) **H**. Cast **0.448 s**, no recovery (W) **M**.
- Projectile **50 m/s**, size **0.3 m** (0.225 + 0.075 global), range 30 m (W) **M**. Arc or gravity: not given.
- Stealth: using it during the fade-out doesn't break stealth; fully stealthed, it breaks stealth at once with no fade-in
  (W) **M**.

## Translocator (ability 2) and stealth
- "Throw a beacon and teleport to it. Become stealthed for a short duration after teleporting." (W).
- **Cooldown 7.5 s**, starting when she teleports (D _; BZ 2026-10-06 6 -> 7.5; W) **H**.
- Beacon **72 m/s** (D _; BZ 2023-10-10 25 -> 72; W) **H**, arcing; she teleports **on its first contact with terrain or after
  0.17 s**: **12.25 m** at most (D _; BZ 2026-10-06 "projectile range reduced from 18 meters to 12.25 meters"; W 0.17 s, 12.25)
  **H**. It can't be damaged, passes through heroes and deployables, breaks breakables without stopping (BZ 2024-03-12; W);
  a death plane destroys it and refunds the cooldown (W) **M**. Stunned / hacked / rooted / hindered: can't throw; if it is
  already flying she still teleports and those effects come off (W) **M**.
- After the teleport: **phased out 0.4 s** (immune to damage, most effects cleared) (W) **M**.
- **Stealth 6.5 s** (D _; BZ 2026-10-06 5 -> 6.5; W) **H**, after a **0.75 s fade-out** (D _; BZ "Time to enter Stealth
  increased from 0.3 seconds to 0.75 seconds"; W) **H**; leaving stealth (end or break) fades **in over 0.25 s** (W) vs BZ
  2025-05-20 **0.3 s** **L**; no fade-in when broken during the fade-out (W) **M**.
- **Speed +60% = 8.8 m/s from the moment she teleports** (D _; BZ 2024-08-20 45 -> 60%, BZ 2026-10-06 "active immediately
  after teleporting"; W) **H**; **+40% (7.7 m/s) while revealed by damage** (D _; BZ 2026-04-14 -50%, 04-23 50 -> 33%: 60 x 0.67
  = 40; W 40%) **H**. Not counted against the global speed cap; lost when stealth ends (W) **M**.
- **What ends stealth**: primary fire, EMP (the press only starts the fade-in), Cyberspace when fully stealthed, quick melee
  when fully stealthed (it starts a fade like firing) (W) **M**.
- **What reveals her without ending it**: **taking damage, for 1.3 s** (D _; BZ 2025-05-20 1 -> 1.3; W) **H**; using Hotfix
  (BZ; W) **H**; reveal abilities such as Sonic Arrow, Infra-Sight (W) **M**. Revealed = seen by the whole enemy team.
  **Detected**: any enemy hero **within 4 m** sees her (that hero only; B.O.B. can't) (W) **M**; heroes call out an invisible
  hero nearby by voice line (BZ 2026-02-10) **M**.
- While stealthed and undetected: immune to the Recon passive; targeted auto-aims (Deadeye, Tactical Visor, Biotic Grasp alt,
  Biotic Orb), Sentry Turret and B.O.B. can't lock her; can't contest objectives (W) **M**.
- Options: hide stealth detection ring (off), disable translocator camera lock (off) (W) **M**.

## EMP (ultimate)
- "Deal damage to all nearby enemies based on their current health, hacking them and destroying barriers around you." (W).
- **3100 points** (D _; BZ 2026-10-06 "+67.5%" on 1850 = 3099; W 3100 since rev 401340) **H**.
- **Radius 15 m**, blocked by terrain sight (Ice Wall blocks; Tree of Life, Petal Platform don't) (D _; W) **M**.
- **20% of current health** as damage (D _; BZ 2026-10-06 25 -> 20%; W) **H**; ignores armor and damage reduction (W keywords)
  **M**.
- **Hacks enemies: abilities locked 3 s, seen through walls 8 s** (D _; BZ 2023-12-05 lockout 1.5 -> 3 s, BZ 2024-10-15 "EMP
  duration unchanged"; W 3 s / 8 s) **H/M**. Hacked = no abilities (normal, ultimate, some passives), movement and channelled
  abilities interrupted, weapon and movement still work (W Status effect) **M**. Hacked enemies show their ult status to her
  team (W) **M**.
- **Destroys all barriers** (D _; W) **M**; hacks deployables (10 s disable, health packs 30 s) and destroys Blizzard and
  Deliverance (W; BZ 2023-04-11 Blizzard) **M**.
- Cast **0.304 s + 0.336 s** recovery; a small upward boost (1.25 m/s) (W) **M**. Her whole team hears "EMP activated" (W) **M**.

## Conspirator (passive)
- **Fully charged enemy ultimates are shown to her**: an ult icon above those enemies (D _; BZ 2026-10-06; W) **H**. Pinging
  one prints "Watch out! (hero) has their ultimate (name) ready!" (W) **M**.

## Tactician (sub-role) and the global passives
- **Tactician** (BZ 2026-02-10; W Sub-Roles, Sombra listed): "Excess ultimate charge is retained after using your ultimate
  ability." **Bonus cap +25%** = **775** for her; gain **-25% above 100%**; the passive 5/s doesn't fill the extra 25% (W)
  (D _) **H/M**. (BZ 2026-06-30's "no longer allows storage" is Community Crafted only.)
- **The support role passive is gone** (BZ 2026-02-10; W) **H**.
- **Regeneration: 10 HP/s + 5% of max a second after 6 s without damage**, 21.25 HP/s for her (D _; BZ 2025-06-24 5 -> 6 s,
  2026-02-10 "unchanged"; W) **H**.
- **Damage dealt cuts the target's healing by 30% (15% on tanks) for 3 s**, global since BZ 2026-02-10 (W Global Passive's
  box flags it "removed": see Disagreements) **M**.

## Perks (minor at perk level 2, major at 3)
- **Perk XP 2375 to the minor, 5710 to the major** (D _; BZ 2026-10-06 +67% / +69.5%; W rev 401632) **M** (see
  Disagreements). XP ramps from minute 3 to x2 at minute 7 (W Perks) **M**.
- Minor, **Life Hack** (BZ 2026-10-06): "EMP applies Hotfix to all nearby allies." Herself too (W) **M**. Which radius: not
  given (L: EMP's 15 m?).
- Minor, **CTRL ALT ESC** (BZ 2025-02-18, kept): "Teleporting with Translocator while below half health initiates passive
  health regeneration." (W; BZ 2025-02-18 said "at critical health") **H**.
- Major, **Data Packet** (BZ 2026-10-06): "Hacking health packs with Hotfix reduces its main cooldown by 4 seconds." (8 -> 4 s
  per charge, W) **H**. Deployables don't count by the wording (L).
- Major, **Cybersecurity** (BZ 2026-10-06): "Cyberspace's healing is doubled on allies below half health." **80 explosion,
  60/s zone** (W) **H**.
- Removed with the rework: Encrypted Upload, High-Speed Bandwidth, Viral Replication (BZ 2026-10-06); earlier Viral
  Efficacy, Stack Overflow, White Hat, Healthy Hacker (BZ 2025-04-22 / 08-26 / 2026-02-10).

## Controls and hero options (W)
Primary fire: Machine Pistol. Secondary fire: Hotfix. Ability 1: Cyberspace. Ability 2: Translocator. Ultimate: EMP.
Reload. Quick melee. Options and defaults: Hotfix sensitivity **100%** (10-100%); Toggle Hotfix **off** (hold); Hide stealth
detection ring **off**; Disable translocator camera lock **off**. (Toggle Hack, BZ 2023-05-09, went with Hack.) Default key
letters: not in these sources.

## Lore and voice
**Olivia Colomar**, 32, Mexican, a hacker (Los Muertos, Talon, formerly; Talon Loyalists); English voice **Carolina
Ravassa**; released **2016-11-15**, the 23rd hero (W).

## Not given by any source
Cyberspace's arc (gravity) and whether its zone needs sight; Hotfix's targeting angle in degrees and whether it needs sight
held for the whole heal; the spread's recovery rate; Translocator's arc and gravity; Life Hack's radius and whether its
Hotfix uses a charge; Weaken's interaction with other damage modifiers beyond "doesn't stack"; how EMP's 20% rounds;
hit volume units; which Minecraft mobs would count as "deployables" or "tanks" (ours).

## Disagreements between the sources
- **Stealth fade-in:** W 0.25 s vs BZ 2025-05-20 "0.15 to 0.3 seconds" (no later BZ change). Also BZ 2026-10-06's "from 0.3"
  for the time to enter stealth matches neither its own 2024-10-29 0.33 s entry delay nor the fade-in exactly.
- **Patch timing vs D:** the pistol (6.5), Cyberspace explosion (20) and cooldown (13) are BZ 2026-10-13, after the data's
  build: D should read 7 / 40 / 12. Use BZ's.
- **Perk XP:** W 2375 / 5710. 1420 x 1.67 = 2371 fits BZ's minor +67%; the major fits +67.5% (3410 -> 5712), not BZ's +69.5%
  (5780). W's April edit (1310 / 3725 -> 1420 / 3410) doesn't match BZ 2026-04-14's -6% / -5% either way: W's older values
  are doubtful. D settles it.
- **Machine Pistol spread:** W's own history wavers 2.16 / 2.3 / 2.5° before the rework (an edit war, 2025-10-28/29,
  "the advanced info panel uses rounded numbers"); BZ's 2.3 -> 2 settles today's 2° only as a rounded figure.
- **Pistol minimum damage:** W shows 6.5 - 1.95 (30%) since rev 404768; for one day (404624) it read 6.5 - 2.1.
- **Hotfix cast:** BZ 0.5 s; W 0.5 s startup + 0.3 s recovery (its first value, before launch, was 0.65 + 0.39).
- **Healing reduction:** W's Global Passive box flags it "removed" while BZ treats it as live (as for Ana).
- **Rework date:** BZ titles it 2026-10-06; W's change log says 2026-10-07 (time zone of the release).
- **CTRL ALT ESC:** BZ 2025-02-18 "at critical health"; W's official text "below half health" (the game's critical health is
  half health, so likely the same).
