# Ana in live Overwatch: numbers and sources (research, 2026-10-29)

**D** = the game data, build 154088 (2026-10-06), read offline and read-only (the map: [ana-map.md](ana-map.md); a graph
node is `graph #uid`, a game value its `.130` id). **BZ** = Blizzard's live patch notes
(`https://overwatch.blizzard.com/en-us/news/patch-notes/live/YYYY/MM/`, read 2026-10-29: every month from 2022-10 to
2026-10, plus 2026-11, which is empty: the last patch is **2026-10-20**, with nothing for Ana). **W** = the Overwatch wiki on
Weird Gloop through its MediaWiki API (`overwatch.weirdgloop.org/api.php`, read 2026-10-29: Ana revision **408527** of
2026-10-29T05:13Z, plus 262 older Ana revisions scanned for the history of her numbers; Sub-Roles 406777, Global Passive
Abilities 402069, Perks 354523, Ultimate ability 395578, Hit points 401654, Movement speed 402311, Melee 402309, Roles 401659,
Status effect 403929 (Sleep and Anti-heal redirect there), Healing 401540, Critical hit 404694, Ana/Stadium 400935). Nano
Boost, Sleep Dart and Biotic Grenade have no pages of their own (they redirect to Ana's). Blizzard's hero page and
Liquipedia were not read (patch notes and the wiki's API only). Every revision, date and old value:
`work\public_sources.md`; raw pages stayed in the scratchpad.

Each number gives D first (where the data holds it), then BZ, then W. Confidence: **H** the data holds it and a public source
agrees (or the data reads it directly and no source disagrees); **M** one link inferred (a data field read by its use), or a
single public source the data doesn't hold; **L** a guess, or the sources disagree and nothing settles it. Where the data and
the public sources differ, both are given here and in "Disagreements"; the plan ([ana-plan.md](ana-plan.md)) says which is used.

Left out (not the live game): **Stadium** (BZ from 2025-04-22; her powers Artsy Dartsy, Magnifier Scope, Eye of Horus,
Falconer, the Biotic Bounce power, items like Sleep Regimen; several of them sit in her graphs as branches that don't run in
role queue), **6v6** (BZ 2024-12-17: 12 ammo, grenade 10 s, Sleep Dart 12 s, no shorter sleep on tanks; BZ 2025-03-25 grenade
12 s; the data's rule flag v66788 gives the same 12 s / no tank cut: M that it is 6v6), Overwatch Classic Moth Meta (BZ
2025-01-21, 02-10), April Fools (BZ 2024-04-01, 2025-04-01, 2026-04-01 Underwatch), Community Crafted (BZ 2024-06-20,
2026-06-30), Quick Play Hacked (BZ 2024-12-04, 2026-07-30), Pickable Passives (BZ 2024-07-15), Haunted Masquerade masks (BZ
2025-10-14).

**Patches that touched her (2025-2026, live 5v5):** 2025-01-21 grenade 90 -> 75; 02-18 perks added (Groggy, Biotic Bounce
50 minor; Headhunter, Shrike major); 02-25 Headhunter fix (Fortified Orisa); 03-25 grenade cooldown 12 -> 14, Shrike 8 -> 6 s;
04-22 Groggy slow 75 -> 50% and +50 damage, Biotic Bounce 50 -> 25; 06-24 global regen delay 5 -> 6 s, healing reduction
25 -> 30%; 08-26 Speed Serum added (40%), Shrike removed, Biotic Bounce made major at 60; 10-14 Headhunter 1.5x -> 2x; 10-28
Headhunter respects the tank crit reduction (fix); 11-11 Groggy's slow now cleansable (fix); 12-09 / 12-18 console aim assist.
2026-02-10 (Season 1): Tactician sub-role, support passive removed, healing reduction made global; **04-14 grenade cooldown
14 -> 12**; 05-14 / 05-26 Nano Boost effect fixes; **06-16 Speed Serum 40 -> 30%**; **07-14 grenade 75 -> 90**; 08-12 Nano Boost
stays on Moira through Fade (fix); **10-06 Groggy removed, Local Anesthetic added, head hit volume 16.9 -> 18, global hitscan
sizes 0.07 -> 0.04 (large) and 0.04 -> 0.02 (small)**. Nothing on 10-13 or 10-20. The data's build (10-06) has all of these.

## Base
- **250 health, no armor, no shields** (D 4BE4 #3: 0BA7 250, the shield slot empty; BZ 2024-02-13: 200 -> 250; W) **H**.
  Support, sub-role **Tactician** (D her tag 1172 picks it in 01C7 #2596; BZ 2026-02-10; W) **H**.
- Speed **5.5 m/s**, backward **4.95** (90%) (D her mover = the hero default, 202B 5.5; W Movement speed) **H**. Mover
  height 2.0, radius 0.5, crouch 1.25 (D, the defaults) **H**. Jump and gravity: the global ones (W: 5.72 m/s jump, 0.98 m
  apex, 17.5 m/s² to a 30 m/s fall; D mover 1.75 x 10, terminal 30) **H**.
- Hit volumes: head **16.9 -> 18** (BZ 2026-10-06; units not given) **M**.
- **Quick melee 40** (D 0043 #352, 3361 #16 02D0; BZ 2024-02-13 30 -> 40, global; W) **H**: hit window 0.2 s, ability 0.53 s,
  cooldown **1 s** (D 0043 #137 / #139 / #379; W 1 s, 0.128 s active, 0.528 s recovery) **H**. She can cancel quick melee
  into Nano Boost (W) **M**.
- Ult charge (W Ultimate ability): 5 points/s passively, 1 point per damage or healing, none from the global regeneration or
  from ult damage/healing, -40% from damage/healing on tanks **M** (engine side: no graph accrues it, D 004B).

## Biotic Rifle (primary fire; unscoped it fires a projectile)
- "Long-range rifle that heals allies and damages enemies." (D loadout E3).
- **75 damage / 75 healing a shot** (D 4BE4 #39 1037 / #40 1038; BZ 2024-11-12: 70 -> 75; W) **H**. Both land **over time,
  not at once**: D 0B95 #193 / #205 spread each over **0.5 s** (`m_95F1FB0B`, read as the change's duration: M), inside a
  0.6 s hit status (1A88, the panel's "Effect Duration": H); W: over 0.592 s in 5 ticks, damage and heal alike; BZ silent
  **M**.
- **15 rounds** (D 4BE4 #41 106E; W) **H**, **reload 1.5 s** (D 102D, 0B94 #33; W) **H**: the clip is full at **0.6 s**
  (0.4 x 1.5, D 0B94 #72 / #70) and firing stays locked to 1.5 s (D #68) **H** (W: ammo back at 0.592 s). Automatic reload at
  0, manual when not full, both only after the shot recovery; a quick melee or ability cancels it (D 0B94 #4 / #10 / #61)
  **H/M**.
- **0.8 s between shots** (1.25 a second, holding the button refires) (D 0B94 #390 literal, #2 / #64; W) **H**. The
  recovery pauses while scoping in or out and during quick melee (D #63 / #172; W) **H**.
- **Projectile 125 m/s** (D 2934, 0B94 #390; W) **H**, straight (no gravity), lifetime 5 s (D projectile 15B7, #17) **H/M**.
- Size **0.15 m against enemies** = 0.1 base (D 2933; BZ 2023-08-10: 0 -> 0.1) + a global 0.05 (D 01C7 #1198, 28A5) and
  **0.3 m against allies** (D 2935, 0B94 #245) (H values; M which field is which, by the Take Aim panel row). W says 0.05 /
  0.15: see Disagreements.
- **Full-health allies don't stop it**; a hurt ally or an enemy does (D 0B94 #17 hit filter, M; W, since OW1 2018-05-22) **H**.
  It can't heal Ana (D no self path, M; W) **H**. No spread (D, W) **H**. No crits unless Headhunter (D 0B95 #269; W) **H**.
- Recoil: the camera kicks and recovers (D fields `m_C5143E2E`, L meanings; W "by 2 units"), with the Recoil Recovery Aim
  Compensation option on by default (D v10563) **L/H**.

## Take Aim (secondary fire; scoped, hitscan)
- "Hold to zoom in." (D loadout E5). **Hold by default**, a Toggle Zoom option (D v8173 = 0; W) **H**.
- **Scope in 0.25 s, out 0.167 s** (D 0B94 #80 at 4.0/s = 1366, #79 at 6/s; W 0.24 / 0.16) **H**. Scoped shots only once
  fully scoped; **no shot at all during the transitions** (D #143 / #475) **H**. The scope overlay shows from 80% (D #278).
- **Hitscan** (D tracer entity 1615 has no speed or lifetime, M; W) **H**: **0.04 m against enemies** (D 28A2 via 01C7
  #1198; BZ 2026-10-06 global large hitscan 0.07 -> 0.04; W), **0.3 m against allies** (D 2935; W) **H**. Range: D
  `m_FEC435C6` 100 (L: metres?), not in public sources.
- Same 75 / 75, ammo cost 1 and 0.8 s recovery as unscoped (D 0B95 for both; W) **H**.
- **Move speed -65% while fully scoped**: 0.35 x 5.5 = **1.93 m/s** (D 1E61 0.65 into the shared slow channel, 0B94 #239 ->
  01C7 #1158: M; W -65%, 1.925 m/s) **H**. Not during the 0.25 s scope-in (D) **M**.
- Zoom: D v172 = **30** (0B94 #83; unit L, perhaps the zoomed FOV in degrees); not in BZ or W **L**. **Relative aim
  sensitivity while zoomed 30%** by default (D v8174 0.3; W) **H**.
- Unscopes: letting go (or pressing again with Toggle Zoom), a reload, the clip reaching 0, another ability taking the weapon
  (Nano Boost's cast); **a shot does not unscope**; she can't scope with an empty clip; a reload turns Toggle Zoom off (D 0B94
  #135 / #91 / #189 / #272) **H/M**.

## Sleep Dart (ability 1)
- "Fires a dart that puts an enemy to sleep." (D loadout E1).
- **Cooldown 14 s** (D 4BE4 #60 0F77 14; 16E7 12 only under the rule flag v66788 = BZ's 6v6 12 s; BZ 2022-12-06: 15 -> 14; W)
  **H**, starting **1.0 s after the press**, at the cast's end, not on the hit (D 0BDD #87) **H**.
- Cast: a **1.0 s** state; the pistol appears in her right hand, **the dart leaves at 0.3 s**, the pistol goes at 0.7 s (D 0BDD
  #107 / #109 / #116, 0F0F 1.0) **H**; W: 0.24 s startup + 0.08 s + 0.752 s recovery **M** (agrees).
- **5 damage** (D 0F22, 0BDD #108; W) **H**. Projectile **60 m/s** (D 0F0E; W), radius **0.2 m** (D 0F0D; W, no global size
  bonus), straight (no gravity field: M), lifetime 20 s (D 15E8) **H**.
- **Sleep up to 5 s** (D 0F10 via 0BDE #363 -> shared CC graph 224E; W; OW1 2020-04-29 6 -> 5) **H**. **On tanks: D 3.5 s**
  (1245, 224E #554; the in-game panel says "3.5 s (tank)"; W's infobox 3.5) **vs BZ 2024-07-09 3.5 -> 3 s** (and W's change
  log): **L**, see Disagreements. Tanks: the tank role, B.O.B. too (W; D role 0850 or tags 0037 / 0DE2, 224E #455, L).
- **Damage wakes the target, any amount, but only after 0.5 s of sleep** (D 1D80 0.5, 224E #332; W) **H**; damage in the
  first 0.5 s is ignored, not saved (D) **H**. **Waking takes 0.5 s** (D 1D7F 0.5, 224E #304; W) **H**. A natural wake plays
  its wake-up over the last second (D 224E #210: M). Local Anesthetic's damage doesn't wake (D tag 191F, 224E #203; W) **H**.
- What sleep does: abilities and inputs locked (D 224E #153 / #334 / #319, M), running abilities cancelled and ults
  interrupted (D 224E #193, the string "ULT WAS SLEPT": M); it can be cleansed (W; BZ 2022-12-06 dev comment) **M**. A slept
  target keeps its momentum in the air (W) **M**.

## Biotic Grenade (ability 2)
- "Throws a grenade that heals and increases healing on allies, while damaging and preventing healing on enemies." (D E2).
- **Cooldown 12 s** (D 1246, 4BE4 #44; BZ 2026-04-14: 14 -> 12; W) **H**, starting 0.7 s after the press (D 0B97 #87) **H**.
- Throw: a 0.7 s state, the grenade leaving about 2 ticks in from her **left wrist** (D 0B97 #41 / #160 / #21, hp 0002: M
  timing) (W recovery 0.686 s) **M**.
- **90 damage / 90 healing, at once** on the burst (D 0F25, 0B99 #214 / 0C72 #213; BZ 2026-07-14: 75 -> 90; W) **H**.
  **Radius 4 m, no falloff** (D 119B; W) **H**; whether the burst needs line of sight: D has a sight profile (0019.097), L.
- **Allies: +50% healing received for 3 s** (D 1DE6 0.5, 0F12 3, 0B99 #318 / #310; BZ 2024-01-09: 3.5 -> 3 s; W) **H**; not
  on an anti-healed ally, and the burst's own heal isn't boosted (D 0B99 #260; W) **H**. **Enemies: no healing at all for 3 s**
  (D 0C72 #262 healing x 0 for 3 s x (1 - their CC reduction): M; W "-100%", no health packs) **H**.
- **It heals Ana** (D 0B99 self branch: M; BZ dev comments 2023-05-09 / 2026-04-14 / 2026-07-14; W: her only self-heal
  besides health packs) **H**.
- Projectile **30 m/s**, arcing under gravity **9.8** (D 0F13; 0B97 #96: M), lifetime 60 s (D), contact radius **0.275 m**
  (D 2936 0.2 + 01C7 28A6 0.075; W) **H**. It **passes through full-health allies** (D hit filter: M; W, OW1 2020-11-19) **H**.
  Without Biotic Bounce it bursts on its first contact (D 0B97 bounce block off: M).

## Nano Boost (ultimate)
- "Increases an ally's damage, while reducing damage taken." (D E4).
- **2300 points** (D 0ADD "Ultimate Charge Max" 2300, passed to the shared ult graph 004B; W since 2024-02-14, 2100 before;
  BZ 2024-02-13 "+10%" to every ult = 2310) **H**. (0ADD's second value, 3000, is also the slider's maximum: L meaning.)
- Target: **an allied hero within 40 m** (D 1B4F, 0B9E #137; W) **H**, in a **10° cone x the Nano Boost Sensitivity option**
  (D; half or full angle L; W: sensitivity narrows it) **M**, **in sight** (D three rays to its chest, centre and feet, any
  one clear: M) **H**; not one already boosted (D v8193: M); picked once, at the press (D) **H**. **Herself only when her
  team is one player (or free-for-all), never in the Practice Range** (D 0B9E #286; W) **H**. **No target: nothing spent**
  (D #140) **H**.
- **Cast 0.13 s + 0.62 s recovery = 0.75 s** (D 0B9E #176 / #175; W 0.128 + 0.608) **H**. **The charge is spent as the cast
  starts** (D #352; W), so a stun inside the first 0.13 s wastes it (D; W) **H**. The syringe she fires is only a visual: the
  boost starts at 0.13 s whether or not it arrives (D 15E9 has no script) **M** (W calls it a homing projectile that can't
  miss, wasted if the target dies first).
- The ally gets: **250 healing at once** (D 125C, 0B9F #265; W; OW1 2019-03-19 300 -> 250) **H**; **+50% damage dealt** (D
  125B, 0B9F #257; W) **H**; **50% less damage taken** (D 125A -0.5, #256; W) **H**; for **8 s** (D 0F17; W) **H**. Nothing
  else (no healing or speed change without Speed Serum) (D) **H**. Anti-heal cancels the 250 (W) **M**.
- It ends early only if the ally dies or the status is taken off; Ana dying or being stunned after 0.13 s ends nothing (D
  #510 / #489: M). BZ 2026-08-12: it now persists through Moira's Fade.
- Option "Nano Boost Requires Target Confirmation", off by default (D v29052; W): then the locked target waits up to **3 s**
  for primary fire (D: also the ultimate key) to confirm or secondary fire to cancel, and cancels itself after **0.5 s** with
  the sight blocked (D 0B9E #327 / #360; W says 1 s); a cancel spends nothing (D) **H/M**.

## Tactician (sub-role) and the global passives
- **Tactician** (BZ 2026-02-10; W Sub-Roles; D shared 01C7): "Excess ultimate charge is retained after using your ultimate
  ability." **Bonus cap +25%** of the cost = **575** for Ana (D 23DB 0.25, 01C7 #2587 / #2586; BZ) **H**; gain **x0.75 at or
  above 100%** (D 23DC 0.75, #2839; BZ -25%) **H**; nothing stored during her own ult (D #2591) **H**; the stored excess
  refills her meter **over 1 s right after the ult starts** (D #2835: M). W: the 5/s passive charge doesn't feed the extra
  25% (D: charge gain is engine side, L). Tacticians now: Ana, Baptiste, Jetpack Cat, Lúcio, Sombra, Zenyatta (W).
- **The support role passive is gone** (BZ 2026-02-10; W). D: the old one (regeneration from 3 s) still sits in 01C7, run
  only when sub-roles are off (#2703 / #1184) **H**.
- **Regeneration (everyone): 10 HP/s + 5% of max health a second after 6 s without damage**, 22.5 HP/s for Ana (D 01C7 #966
  / #963: 0B0F 6, 1CAE 10, 1CAD 0.05; BZ 2025-06-24 5 -> 6 s, 2026-02-10 "unchanged"; W) **H**.
- **Damage dealt cuts the target's healing by 30% (15% on tanks) for 3 s**, global since BZ 2026-02-10 (W: doesn't stack with
  itself; multiplies with other modifiers; adds to her grenade's anti-heal) **M** (not read in the data).
- All heroes see allied health through walls (BZ 2026-02-10) **M**.

## Perks (minor at perk level 2, major at 3)
- **Perk XP 2025 to the minor, 5060 in all to the major** (D 4BE4 #55: 12B2 2025 then 1291 3035; W) **H**. XP comes like
  ult charge, ramping from minute 3 to x2 at minute 7 (W Perks; BZ 2025-02-18 without numbers) **M**.
- **The live four** (D her perk list 0061.177; BZ 2026-10-06): minor **Local Anesthetic**, **Speed Serum**; major **Biotic
  Bounce**, **Headhunter** **H**.
- Minor, **Local Anesthetic** (BZ 2026-10-06, new, replacing Groggy): "Sleep dart explodes on impact, slowing enemies and
  dealing 45 damage over 3 seconds." (D 0F9C). **45 over 3 s as 30 ticks of 1.5** (D 6123 #374 / #372) **H**, to **enemies
  within 4 m** of where the dart hits (D 0BDD #297 splash 4 m; and 51F0 #451, 0.15 s after the dart's damage, a 4 m search
  with sight) **H**; **slowed 40% easing to 20% over the 3 s** (D 6123 #380: M) vs W **-50%** **L**; its damage never wakes
  a sleeper (D; W) **H**. Only the dart's own target sleeps (D: the splash carries no sleep) **H**. Cleansable: W says no,
  BZ 2025-11-11 made Groggy's cleansable (L for this one).
- Minor, **Speed Serum**: "Nano Boost grants a 30% movement speed boost to both Ana and her target." (D 0D0A). **+30%** (D
  51F0 #401, 0B9F #317; BZ 2026-06-16: 40 -> 30%; W 7.15 m/s) **H**; the ally for the boost's 8 s, Ana from the cast to its
  end (D: M; no public duration).
- Major, **Biotic Bounce**: "After exploding, Biotic Grenade bounces and explodes again for 60 damage and healing." (D 0BB5).
  **60** (D 1549, 0B99 #313 / 0C72 #309; BZ 2025-08-26: 25 -> 60; W) **H**; the bounce is turned on by the perk (D 0B97 #176)
  **H**; the second burst re-applies the 3 s boost / anti-heal (D: M) with the same 4 m (W Healing; D: M). How far it
  bounces: not given (D bounce fields unread) **L**.
- Major, **Headhunter**: "Biotic Rifle can crit enemies." (D 0BF1). **x2 on head hits** (D 0B95 #269 / #265 / #272, 150 a
  shot; BZ 2025-10-14: 1.5x -> 2x; W) **H**, scoped and unscoped (D one script) **H**; on some targets x2 x (1 - a
  reduction) (D #272: L; BZ 2025-10-28 "respects the tank headshot reduction", now Bruiser's -25% crit, BZ 2026-02-10). Allies
  can't be crit (D) **H**.
- Removed: **Groggy** (BZ 2026-10-06; slowed and 50 damage over 2 s after waking; D: still in the hero's old list 254C, no
  graph reads it) and **Shrike** (BZ 2025-08-26; Nano also on Ana for 6 s; D: a dead branch, 0B9E #495, its file gone).
  Pinpoint Prescription, Artsy Dartsy, Dream Field, Falconer are Stadium powers, not perks (W).

## Controls and hero options (D Ana's hero settings 0126.054; W)
Primary fire: Biotic Rifle. Secondary fire: Take Aim (hold). Ability 1: Sleep Dart. Ability 2: Biotic Grenade. Ultimate: Nano
Boost. Reload. Quick melee. Options and defaults (D, = W): Toggle Zoom **off**; Relative Aim Sensitivity While Zoomed **30%**
(gyro too); Recoil Recovery Aim Compensation **on**; Nano Boost Requires Target Confirmation **off**; Nano Boost Sensitivity
**100%** (10-100%); Weapon Mode (which reticle); camera behaviour while scoping in third person; console friendly aim assist
100% under 99% health (BZ 2025-12-09). Default key letters: not in these sources.

## Lore and voice
**Ana Amari**, 62, Egyptian (Overwatch's co-founder and second-in-command; mother of Pharah); English voice **Aysha Selim**;
released **2016-07-19**, the 22nd hero (W). Voice set 012A.05F (D): [ana-map.md](ana-map.md) "Voice".

## Not given by any source
The zoom (D's 30, unit unknown); the scoped hitscan's range (D 100: L); Local Anesthetic's slow (D 40% -> 20% vs W 50%) and
whether it's cleansable; Speed Serum's duration on Ana (D: the cast + 8 s, M); the grenade's arc (D 9.8 gravity: M), Biotic
Bounce's bounce distance and timing; whether the grenade burst needs line of sight; the rifle's recoil as numbers; the
exact tick pattern of the rifle's 0.5 s heal and damage; hit volume units; which Minecraft mobs would count as "tanks" (ours).

## Disagreements between the sources
- **Sleep on tanks:** D **3.5 s** (1245 = 3.5 in a build of 2026-10-06; the in-game panel's text reads it; W's infobox 3.5
  since a 2025-09-29 edit) vs **BZ 2024-07-09 "3.5 to 3 seconds"** (W's change log the same; no later patch note brings 3.5
  back: every month to 2026-10 searched). Not settled: the data is the live code, but 3 s is Blizzard's word.
- **Rifle projectile sizes (unscoped):** D 0.15 m vs enemies (0.1 + 0.05) and 0.3 m vs allies; W 0.05 vs enemies and 0.15 vs
  allies; BZ's own arithmetic (0.1 base + the 0.075 fast-projectile bonus) would make 0.175. D's fields are named by the Take
  Aim panel row (M), its values H.
- **Rifle heal and damage over time:** D 75 over 0.5 s inside a 0.6 s status; W 0.592 s in 5 ticks (its tooltip also says
  74.4 though its ticks add up to 75); BZ never says.
- **Scope times:** D 0.25 / 0.167 s; W 0.24 / 0.16 (frame rounding: M).
- **Nano confirmation:** D cancels after 0.5 s without sight; W 1 s. **Nano's syringe:** D a visual only; W a homing
  projectile.
- **Local Anesthetic's slow:** D 40% easing to 20% (M); W 50%. Cleansing it: W no; BZ 2025-11-11 fixed Groggy's to be
  cleansable.
- **Headhunter:** one W detail line still says 1.5x; W's own field, BZ 2025-10-14 and D say 2x.
- **Grenade size:** D and W 0.275 m (0.2 + the 0.075 bonus BZ gives projectiles over 50 m/s, though it flies at 30); BZ's
  slower-projectile bonus would make 0.32.
- **Ult cost:** D and W 2300; BZ's "+10%" of 2024-02-13 on 2100 is 2310.
- **Healing reduction:** W's Global Passive box flags it "removed" while its own page and BZ treat it as live.
