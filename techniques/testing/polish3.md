# Polish 3 (fix/polish-3, 2026-11-05): the record

Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [harness](harness.md) · issues #116, #117, #115, #109, #85.
Base e6f3cef (0.3.31). One client (`runClient`, port 25599, under `.oc_client_lock`), world `p3a` (flat survival, normal difficulty). **An independent critique followed; [its answers](#the-critiques-answers-second-pass) correct parts of what's below, and the "which tree" table there says what ran on what.** **Input: `KeyMapping.set` through DevServer and `look`, not a mouse. Bots are server-side players that can't aim well or see. Nobody has played this by hand.**

Where things are:
- Raw traces (committed): [runs/polish3](runs/polish3/).
- Screenshots and sheets: `run/screenshots/p3_*`, `sheet_p3_*`, gitignored because they show Overwatch models.
- Scripts: `run/p3_raw/`, gitignored, on this machine only.
- Also in `run/p3_raw/`: Blizzard's notes as text (`bz-2026-*.txt`), wiki patch tables, every hero's UX nodes (`p85_ux_nodes.txt`), graph 5162's node dump. Not committed: they are others' text and game data.

Confidence marks: D data, BZ patch notes, W wiki, R recording; H/M/L.

## #116: balance
Read: BZ 2026-08, 09, 10, 11 month pages (the last patch is 2026-11-03); wiki through `overwatch.weirdgloop.org/api.php` (revisions in the code's comments). No personal info in the requests.

| Hero | Nov 3 | Changed | Earlier patches checked against the class |
|---|---|---|---|
| Brigitte | Inspire gate 1.25 -> 1 s; at once 12 -> 15 | both | 09-08 health/armor in; nothing for her 10-06, 10-13, 10-20 |
| Ana | Grenade explosion damage 90 -> 60 (healing untouched) | damage only | 10-06 Local Anesthetic and hitscan size in; hit volumes not modelled |
| Jetpack Cat | Health 225 -> 200; boost acceleration 25 -> 28; fuel back 25% -> 20% a second | all, the boost inferred (below) | 09-08 pawjectiles, 08-14, 08-11 in. 09-10's Purr-through-barriers fix and 09-08's "stun drops Lifeline" not played here |
| Sombra | Hotfix 12 -> 10 s; Weaken 50% -> 40%; EMP overrides enemy hacks on health packs | the two numbers. EMP: nothing to do (the mod has no health packs) | 10-13 and 10-06 in |
| Kiriko | none | header date | 10-13 health, 09-08 four numbers in |
| Lúcio | none | header date | 10-06 Barrier 6 s, Beat Drop 4 s in |
| Mercy, Lifeweaver, Juno, Sigma, Sojourn | none (Sojourn: a Stadium power; Juno: a skin bug) | nothing | 08-11 Life Grip, Torpedoes; 07-14 Sigma; 10-06 sizes. Sigma's and Sojourn's headers were already right |

**Inferred, not read:** BZ says "acceleration 25 -> 28 m/s". D 59E0 #63 holds the 25 as a push. Our boost is shaped from the 2026-11-01 recording (20 then 10.4 m/s/s), made under 25. So I scaled both rates by 28/25 (M). A new Workshop recording should replace that.
**W is behind BZ in one place:** the infobox's 8% carrying refill is a third of the old 25. Ours is 20/3.

Played ([p116_play](runs/polish3/p116_play.txt), **on the tree of 05:44, not the final one**; replays in [c4_suite](runs/polish3/c4_suite.txt) as the table below says). The speed line here is the x1.12 model, since replaced by +3:
- Brigitte's flail held on a NoAI husk, a bot ally (health read every ~0.07 s): jumps of about +1.4 at 0.51, 1.77 and 2.97 s, 1.2-1.26 s apart (two 0.6 s swings). At 1.25 s the gate would pass every third swing (1.8 s): that "before" is arithmetic, **not played**.
- Ana (`look 0 18`): husk -4.80 (60), villager +7.20 (90).
- Cat: HUD max 16 (200). Refill +0.04 per 0.2 s in the trace. Speed 10.95 -> 13.28 per 0.2 s, 11.65 m/s/s, the ramp's 10.4 x 1.12; 17.80 at top.
- Sombra: Hotfix on a bot ally, then `bot set 5` at 8.3 s: slope falls at about 10.5 s (0.5 s cast + 10). The rest of the rise is its own regeneration, so the end is read from a slope. M.
- Weaken: real client inside a bot Sombra's Cyberspace, kunai at a husk: 4.80 outside, 2.88, 2.88 inside (x0.6), 4.80 again after.

## #117: Suzu and Life Grip clear the anti-heal
W is the source (Kiriko rev 408632 "removes any allies' anti-heals", Lifeweaver rev 408779). The data has no cleanse node (lifeweaver-map.md), so M. B064 is the anti-heal's mod (D, Ana's kit).

**A mistake of mine:** the first "before" (`p117_before.txt`, Suzu part) and first "after" sent a wrong key name (`ability2`). The harness answered with an error nobody read, so **no Suzu flew** in them. Those lines prove nothing; the file says so at the top. The valid comparison is `p_final_a.txt`, top section: same build, dev switch `bot fixoff`, real-key Suzu, grenade from a bot Ana. Life Grip's before in `p117_before.txt` is valid (bot press).

| Case | Fix off | Fix on |
|---|---|---|
| Grenade on the real client, then Suzu at her feet | 7.2 -> 7.2, anti-heal stays | 7.2 -> 13.6 (+6.40 = 80), cleared |
| Grenade, then a bot Lifeweaver's Life Grip | 12.8 -> 12.8 (base build) | 9.2 -> 15.2 (+6.00 = 75), cleared ([p117_after](runs/polish3/p117_after.txt)) |
| Anti-healed ally bot in the Suzu | not played | 9.62 -> 16.02, cleared |

Added beyond the issue: the violet pluses end with the cleanse, and a phased target takes no anti-heal (W). **This paragraph's first version claimed a phased test that proved nothing** (a burst before the Suzu gives the same 20: 20 - 4.8 = 15.2, + 6.4, capped). Both now shown in the second pass.

## #115: others see the wall ride
His client sends `OwMovement.wallCode` (side and ride yaw) in `HeroInputPayload.wall`. `Lucio.tick` puts it in the `LUCIO_WALL` attachment (in HERO_LOOKS). On other clients `wallRiding`, `wallRideYaw` and `wallRideNormal` read it, so `HeroAnimator`'s clip and lean and `LucioFx`'s sparks follow.

**What was proved, in two halves, because the machine's current rule is one client at a time** (a rule, not a technical limit: `runClient2` exists and would do this; the owner decides. #115 stays open on it):
- **Sending end, real client riding the real wall** ([p115_after](runs/polish3/p115_after.txt)): the server reads side -1 yaw 0 (wall at his left, facing +z), side 1 yaw 180 the other way. 0 after leaving, on G mid-ride. Killed mid-ride it **stayed 3602 on the corpse**: found playing; `Lucio.leavePlay` now clears it. Replayed in the second pass.
- **Seeing end, real client drawing a bot** told by the same input route (`bot wall`): `wallride_left` / `wallride_right`, side, yaw, normal right, the skate spark (`sheet_p3_115_view`: idle, left, right, off, and the local rider from the front). Before (base + dev tool, [p115_before](runs/polish3/p115_before.txt)): `playing` stays `idle` with the flag set.
- **Late arrival:** client teleported 400 m off and back during a ride, twice, with `showstate` on and off: `wallride_left`, code 1 on return. Attachments go on tracking, so no replay and no burst; the sparks' size starts small on arrival (`WALL_SINCE`).

**What remains, plainly:** no second client has seen a moving rider. The bot stands on a ledge, can't turn (the "right" picture has him facing the wrong way for the ride: an artefact), and doesn't travel. The sending and seeing ends share one number but were not run as one chain. Movement interpolation, a remote body's yaw and lean while really riding, and corners (yaw changing mid-ride) are unseen. The airborne clip order isn't in doubt: the local rider goes through the same chain.

## #109: pushes on a real client
Bot attacker, real client victim, standing on flat ground, 2 s `trace`, hero mode and Minecraft mode. Files: [a](runs/polish3/p109_before_a.txt), [b2](runs/polish3/p109_before_b2.txt), [c](runs/polish3/p109_before_c.txt), [after](runs/polish3/p109_after.txt), [carry](runs/polish3/p109_carry.txt).

**Result: on the base build every push played here reached a standing or moving real client (#93's `Combat.send`). Not covered: a truly flying carrier (a bot carried by hand is the nearest, second pass), two real clients, and small pushes in hero mode, which arrive and are lost to ground friction (Sonic Amplifier, below). One thing was wrong: Hyper Strike's float.**

| Blow | Hero: peak h, peak vy, moved | Minecraft mode | A mob for comparison |
|---|---|---|---|
| Whip Shot | 23.7, 1.2, 5.88 m | 25.0, 0.4, 4.25 m | husk about 5.6 m (polled, then it walked) |
| Whip, victim running at her | 23.7 first, thrown back | 27.0 | |
| Soundwave | 12.8, 9.3, 8.06 m | 13.7, 8.3, 3.85 m | |
| Soundwave, victim jumping | 12.8, 9.3, top y -56.03 | 13.7, 8.3 | |
| Purr's cast | 14.4, 6.2, 8.70 m | 15.0, 5.3, 3.73 m | |
| Quick melee | 3.0, 2.2, 1.24 m | 3.0, 0.6, 0.50 m | |
| Accretion | 6.0, 2.2, 2.51 m | 6.0, 1.4, 1.11 m | |
| Sphere pull | 0.6 (the data's own small pull, direct hit) | 0.87 | |
| Shield Bash | 17.2, 4.2, 7.91 m | 18.0, 3.3, 4.09 m | |
| Rally: bash + shockwave | 30.4, 6.2, **19.7 m** | 26.1, 3.4, 11.8 m | stacks two pushes and 140 damage on the bashed target; the data excludes it: second pass |
| Sonic Amplifier round (`SonicEntity`, its 0.6 m/s is ours) | **0.00** (below the ground's friction in one step, damage 14.1 in the same trace) | 1.24, 0.79 m | [c4_suite](runs/polish3/c4_suite.txt) |
| Catnapper's landing push (`JetpackCat.land`, W 2.8 m/s) | first tick mixed with the carry's first pull, 17.3 then 12.3, so not separable here | 32.2 the same | c4_suite |
| Gravitic Flux | top y -54.60 | top y -54.21 | |
| Catnapper: carry, still carrier | y -58.89 under a bot on a ledge for its 4 s; **a 2.2 cm wobble every second tick (10 Hz, -58.886 / -58.864)** in [p109_carry](runs/polish3/p109_carry.txt), Minecraft mode 3.9 cm for a while: not looked at on screen | y -58.8 | |
| Life Grip | 25.00 m/s by `GripPayload`, its own route (traces in [p117_before](runs/polish3/p117_before.txt) and c4_suite's Grip) | not played | |
| Lifeline's Jump | 7.8, 9.7, top y -56.44 | not played | |
| **Hyper Strike, before** | 12.9, 3.2, top y -59.52, **5.50 m**, on the ground at 0.45 s | 13.4, 2.4, top y -59.64, 2.88 m | NoAI husk, real Sigma at 2 m: 5.84 m, y -58.91 at about 0.7 s, in the air |
| **Hyper Strike, after** | 12.9, 4.0, top y -56.19, **13.8 m** | 13.4, 3.9, top y -57.12, 4.45 m | (mobs unchanged) |

**The fix (Sleep.stun, H values, M meaning):** D 54A5 #404's gravity x0.01 went only to mobs (`Sleep.slide`). A player's client moves them, so it never saw the float, and friction ate the knock. It now goes as Minecraft's gravity attribute, which Minecraft mode falls by and hero mode reads (`gravityScale`). Fix off and on, same build: `p_final_a.txt`, `## strike_*`. 13.8 m in hero mode is further than a husk's because hero mode's air keeps speed (its own mover). Whether that matches Overwatch is unknown.

**A wrong turn, kept here so nobody repeats it:** the first carry looked broken (top y -60). It was my test. A bot falls to the floor (`/tp` doesn't move it at all), so the hold point was underground. On a ledge the carry lifts with `syncVelocity` alone, fix off and on the same (`p109_carry.txt`), so I reverted my `Combat.send` there. `p_final_a.txt`'s `## carry_*` are that bad geometry; ignore them. Also: `p109_before_c.txt`'s `MOB Hyper Strike` is void: that husk had AI and just walked (y -60 throughout, no sign it was hit); the row's husk is from `p_final_a.txt`'s end. `p109_before_a.txt`'s quick melee (0.00) is `bot press melee` not reaching `QuickMelee`; `bot input` does (b2). `p109_before_b*.txt`'s Hypersphere 0.00 passed over her head: the `/tp` that was meant to aim did nothing, and `_c` was a direct hit only because Sigma's level shot met her body at that range: I can't say what pitch it had.

Not played on a real client: Hyper Strike into water or a wall; Rally's shockwave from a second enemy; Lifeline's rope tow as a force beyond #89's work; pushes on a slope; a victim in Valkyrie or gliding (share read from code only).

## #85: the rest of the crosshair
Scan of every hero's `StateUXPresenter` in the centre and reticle layers (`p3_ux.py`), against [reticle](../hud/reticle.md). Row by row there. Built:
- **Sigma, Hyper Strike:** data 0243 in the *ability bar* (51E0 #425: quick melee's icon and key, hits over 5). Ours: five pips under the crosshair and "[V] HYPER STRIKE" when charged. Name from 25111, which the data doesn't put on this widget. Volleys at a husk: 2, 4, 5 of 5 (a volley is two spheres). **`p85_after.txt`'s five "after melee 342.39" lines and `p3_85_sigma_after_melee` are a missed melee (the husk at 4 m, her reach 2.5): void.** Melee at 2 m consumes it, pips empty, husk thrown 5.8 m and floating (`sheet_p3_85_after`, `p3_85_sigma_struck`). Before: `p3_85_before_sigma` is in `run/screenshots`.
- **Lúcio: the note was wrong.** "Accelerando's meter (5162 #450)" is **Soundwave Rider's**: under gate #437, v66249 chased to 100 at 20 a second (#439: the wiki's 5 s), #443's 1.25 is its knockback. Accelerando is #391 -> #393 in the same graph and has no widget. So Accelerando gets none, and Rider's charge is drawn: a bar with the perk's icon, gold when full. Ride: 0.06 ... 1.0 at the 13th read, reads about 0.4 s apart by his 7.15 m/s, so about 5 s; held after (`p3_85_lucio_ride_half`, `_end`). Before: `p3_85_before_lucio_ride`, no bar, meter already rising. Icon by perk id: the data's is loadout CA7, not checked to be the same picture. M.

Not built, and why: Sombra's 0164 / 017B / 3692 / 3698 are the health pack entity 0216's (kit_pistol_hotfix.md: no packs in the mod). Ana's 469A / 5E62 sit in `work_voice`, not her kit (L). Sojourn's, Sigma's and Juno's identical 56D6 / 56FB / 5732 are a shared graph (its texts "Light Round / Heavy Round" are no kit of theirs). Lúcio's 0234 #433: a timed bar under a graph with loadout 00CB, undecoded (L). Sigma's barrier box with its broken icon (0241): ability bar, not done. D.Va: her branch, as told.

## Fights
The first set here (22 s each, no movement) was bad: `near()` also aimed at the hero's own kunai, rounds and spheres, she never moved, and "Killed N" wasn't a mob count. Its file `p_fights.txt` stays as what it was; don't use it.

Second set: [final tree](runs/polish3/c6_fights_final.txt), [base e6f3cef](runs/polish3/c6_fights_base.txt), the same script (`p3_fight2.py`) in fresh clean ground each round; shots `p3_final_fight_*`, `p3_base_fight_*` in `run/screenshots` (the base worktree is removed). Midnight, survival, flat; 3 zombies, 2 skeletons, 1 creeper with AI, two rounds, up to 30 s. **Harness input, seed fixed:** aim at the nearest zombie, skeleton or creeper each ~0.3 s, left button held, one ability every 2.5 s whatever the moment, strafing or backing off when one is within 5 m, a jump sometimes. Mob count by `/execute if entity`; deaths from the chat's death lines. **Not a balance judgement:** 14 scripted rounds, and Jetpack Cat's flight isn't used well. Mobs left are out of 6.

| Hero | Base: mobs left, deaths, lowest hp | Final: mobs left, deaths, lowest hp |
|---|---|---|
| Kiriko | 2, 0, 13.0 / 1, 2 (arrow, creeper), 3.0 | 1, 1 (creeper), 8.0 / 2, 0, 7.4 |
| Brigitte | 3, 0, 16.6 / 2, 0, 6.0 | 3, 0, 16.6 / 2, 0, 17.6 |
| Ana | 5, 1 (zombie), 2.0 / 2, 1 (creeper), 5.0 | 2, 0, 20.0 / 2, 0, 12.0 |
| Sombra | 2, 0, 8.0 / 4, 1 (arrow, at the end: the count missed it), 1.4 | 3, 0, 13.0 / 5, 1 (arrow), 1.0 |
| Jetpack Cat | 6, 0, 10.0 of 18 / 5, 0, 14.0 of 18 | 5, 0, 12.0 of 16 / 5, 0, 12.0 of 16 |
| Sigma | 2, 0, 36.1 / 4, 0, 31.0 | 2, 0, 29.0 / 3, 0, 37.0 |
| Lúcio | 3, 0, 10.0 / 4, 0, 15.0 | 4, 0, 9.8 / 3, 0, 12.5 |

Each came back as the same hero in hero mode. No crash; log errors are start-up auth only. No round cleared: the script keeps backing off and its aim lags. The final tree's deaths: 2; the base's: 5. On rounds this few and this scripted that says nothing beyond "nothing obviously got worse".

## The critique's answers (second pass)
**Which tree ran what.** T0 = 2cf62d2's content at its run times. **T1 = the final code**, built 06:45 and unchanged after (only docs since); client restarted once at 07:05 on the same code.

| Evidence | Tree |
|---|---|
| `p116_play`, `p117_after`, `p115_after`, `p85_after`, `p109_after`, `p109_carry`, `p_final_a`, `p_fights`, `c1_repro`, `c2_repro`, `c3_hyper_up` | older trees (05:35-06:40): before the edits to Lucio, Sleep, DevBot, Combat, JetpackCat, and the second pass's fixes. **My "played on the final build" was false for these.** `c3_hyper_up` is the reproduction, meant to be before the fix |
| `c4_suite` (Suzu, Grip, phased, pluses, float, Sonic, landing, sender side, square-on, corpse, switch, Rider, pips, boost, tow, zombie), `c5_env`, `c6_fights_final`, sheets `sheet_c_*` | **T1** |
| `c6_fights_base`, `p115_before`, `p85_before`, `p109_before_*`, `p117_before` | base (the `before_b/c` and #115's with the dev tool added) |
| [final_build](runs/polish3/final_build.txt) | T1, `gradlew build` with tests, saved |

Not replayed on T1: Brigitte's Inspire timing, Ana's grenade numbers on mobs, Weaken, Hotfix's end, the late-arrival teleport, the #109 blows `Combat.send` carries other than those listed. Their code didn't change after they ran (`Inspire` and the stats only), but Sleep, Combat and Lucio did, so a critic may call those partly stale.

**#116.**
- **Top speed:** W rev 408445 says 17.8, rev 408976 (2026-11-04) 17.43, its note "it seemed like they changed the max speed AGAIN". BZ names acceleration and fuel only. The data holds no top speed (gaps.md: the mover's balance). More push shouldn't lower a top speed. **Kept 17.80 (R, pre-patch), M.** 2% apart; a new recording decides.
- **Boost:** changed to **+3 (kick 23, ramp 13.4)**: the recorded rates are the push less a drag not depending on it. The alternative, x 28/25 (11.65), holds only if drag scaled too. Trace in c4_suite. Inferred either way.
- **Refill carrying:** played, towing a villager, mode 24: 0.5400 -> 0.8067 over 80 ticks, 6.67% a second (20/3). The wiki's 8% is stale.
- **Her health:** a real zombie's bites at 16: 13, 10, 7, 4, 1, 0, in 3s (Minecraft's own number, the mod's 200 OW max).
- **Stale notes** fixed: `Inspire.java`, `tools/reference/brigitte.md`, `jetpack_cat.md`, `sombra.md`.

**#117.**
- Fix off 7.2 -> 7.2; on 7.2 -> 13.6 (c4_suite). Grip 9.2 -> 15.2.
- **Phased, now real:** from 12 health, Suzu first, a grenade from 3 m: 18.4 (12 + 6.4), **no -4.8, no anti-heal**, the burst heard at +0.2 s, so inside the 0.5 s phase. Control, unphased, same spot: 15.2 and anti-healed.
- **Pluses:** fix off (`p3_8c_on_*`: "on" means `fixoff on`) the bot's pluses run on after the Suzu, count 5 -> up to 5 at +1.65 s; fix on (`p3_8c_off_*`) gone: count 0 from +0.65 s. `sheet_c_pluses2`, top row fix on, bottom fix off. The bottom right frame shows pluses, the top right none.
- `onTarget`'s sound now only with the anti-heal: read, not heard.

**#115.** Square-on flip reproduced (`c1_repro`: `wallride_right` / 180, then `wallride_left` / 0 at 0.3 s) and fixed: until the ride proper chooses, the side the camera will choose. c4_suite: no flip for camera +z or -z or dead square. **S held at that moment can still flip it once.** Corpse: dead, code 0; a killed bot, code 0. Hero switch mid-ride (at the bed, screen and `hero`): code 0. G: 0. Rejoining after Save and Leave mid-charge: code 0, Rider's charge 0. The old "one client" wording is corrected above. #115 stays open on a real second client.

**#109.**
- **Hyper Strike aimed up** (`c3_hyper_up`, reproduced first; the victim on the edge of a 2-high block, a bot Sigma at -72): old top y -52.47; **new top y -41.76, 18 m, Brigitte 13.92 -> 0 on landing**, Kiriko 12.8 -> 3.8, Lifeweaver -> 0, Minecraft mode -> 6.92. Old: -4 (Brigitte), 0 (Kiriko). **Decision (ours, by the data: Overwatch has no fall damage, the float is what carries them there):** no fall counts from a float till they land (`Sleep.FLOATED`), players only. After: 13.92 stays, Kiriko 12.8, Minecraft mode 13.92; a plain later 9 m drop still costs 5.44, as the control. Mobs keep their falls. #84's push falls are untouched.
- **Into a wall:** stops at z 298.30, no damage of its own, 13.92. **Into water:** carried 14.5 m, y to the pool's floor, 13.92.
- **Flying carrier** (`c2_repro`): a bot can't fly, so carried by hand each tick with its known movement set. 7 m/s level: the client holds **0.36 m behind the hold point, one tick**, steady, y -59.61; straight up at 5 m/s it follows at about 0.6 m, ground first. So `syncVelocity` keeps up there. It isn't a real flying client's jitter or ping, so "every push" doesn't claim it.
- **Rally, owner's question, the data:** `kit_shield_bash.md`: #877's second hit, "**#890 excludes the first target**", M. So the bashed target should take one, and the mod gives two (140, stacked pushes). One filter would fix it; left for the owner as asked. **Whip Shot:** D 133E 25 m/s, one push (#994); 5.88 m on a hero, about 5.6 m on a husk; "about 8 m" in `BrigitteStats` is an old note with no source I found.

**#85.**
- Rider's bar gated by `reticle`: third person none (`sheet_c_rider`, bottom left).
- **Drain:** nodes read: #477 -> #462 STU_B591EBFE -> #439 charging, or StateWait #463 3 s -> #465 not full -> #464 chase to 0 at 20/s. H values, M path, #462's own fields undecoded. Done: holds 3 s, then 0.2 a second: 0.35 steady to 2.75 s, 0.30 at 3.0, 0 at 4.5.
- **Soundwave at full:** husk -5.4; bar to 0; empty after: -3.6 (x1.5).
- **Pips:** empty 0x66 white on a heavier edge, width 14 for his; at night 720p and 1080p readable with a gap to the sphere (`sheet_c_pips`, `sheet_c_pips1080`). Thin on a light husk still. Melee at 2 m: pips empty, husk thrown.

**CHANGES** says the input packet's shape changed. Version left to the coordinator.

**Ways in and places (`c5_env`, harness clicks and keys, not a mouse; screens `sheet_c_env`):**
- H, tile and READY by `screenclick` for all six changed heroes: each switched, hp right (48, 18, 16, 20, 18, 20), Shift works.
- Save and Leave by click, reopen, the select screen with the last hero, a tile, READY: Ana. Esc on it keeps the hero.
- Water, a 3-deep pool: each walks, jumps, sinks, uses abilities; hp unchanged. Lúcio's ride ends in water by code, not tried. Lava with fire resistance: the same, no damage while it held. **Slope, stairs 12 up:** up at 5.7 m/s, hero's own run clip; the script then walked off the top end, and the fall and burning left over from the lava cost health (script's doing, not read cause by cause). Nether: a floor at y 100 (the first try had no floor and fell: void), each hero walked, jumped, used abilities; nothing odd in the numbers or shots; the last "Lúcio fights a magma cube" line was still Sigma (hero change refused, the spawn point had moved). **First Nether run marked void in the file; it broke the client's local player (harness `respawn`).**

## What a critic should check
- `p117_before.txt`'s Suzu lines are void: use c4_suite.
- The boost's +3 and the kept 17.8 are decisions, not readings: no recording since the patch exists.
- No Overwatch side by side for any of this. Pips, bar and prompt are ours in shape, place and colour.
- Hotfix's end read from a slope, on the older tree.
- `bot fixoff` covers #117 and the float only. #115's before is the base-plus-dev-tool build.
- Sigma's pips on a light husk are thin; bright backgrounds and other GUI scales not tried. Night: only in fights' thumbnails, neither new widget looked at there.
- Not tried at all: boats; a real mouse; two real clients; the changed heroes at a player's pace through a real night (the fights are a script).
