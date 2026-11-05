# Polish 3 (fix/polish-3, 2026-11-05): the record

Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [harness](harness.md) · issues #116, #117, #115, #109, #85.
Base e6f3cef (0.3.31). One client (`runClient`, port 25599, under `.oc_client_lock`), world `p3a` (flat survival, normal difficulty). **Input: `KeyMapping.set` through DevServer and `look`, not a mouse. Bots are server-side players that can't aim well or see. Nobody has played this by hand.**

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

Played on the final build except as marked, [p116_play](runs/polish3/p116_play.txt):
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

Added beyond the issue: the violet pluses end with the cleanse (`ana_anti_healed_end`; read from code, **no shot of it**), and a phased target takes no anti-heal (W). That case is weak: no hit and no anti-heal after a grenade thrown 0.22 s before the Suzu, but nothing shows the burst landed inside the phase.

## #115: others see the wall ride
His client sends `OwMovement.wallCode` (side and ride yaw) in `HeroInputPayload.wall`. `Lucio.tick` puts it in the `LUCIO_WALL` attachment (in HERO_LOOKS). On other clients `wallRiding`, `wallRideYaw` and `wallRideNormal` read it, so `HeroAnimator`'s clip and lean and `LucioFx`'s sparks follow.

**What was proved, in two halves, because there is one client:**
- **Sending end, real client riding the real wall** ([p115_after](runs/polish3/p115_after.txt)): the server reads side -1 yaw 0 (wall at his left, facing +z), side 1 yaw 180 the other way. 0 after leaving, on G mid-ride. Killed mid-ride it **stayed 3602 on the corpse**: found playing; `Lucio.leavePlay` now clears it. **That clear was not replayed**: the build after it ran pictures and fights, not a mid-ride kill.
- **Seeing end, real client drawing a bot** told by the same input route (`bot wall`): `wallride_left` / `wallride_right`, side, yaw, normal right, the skate spark (`sheet_p3_115_view`: idle, left, right, off, and the local rider from the front). Before (base + dev tool, [p115_before](runs/polish3/p115_before.txt)): `playing` stays `idle` with the flag set.
- **Late arrival:** client teleported 400 m off and back during a ride, twice, with `showstate` on and off: `wallride_left`, code 1 on return. Attachments go on tracking, so no replay and no burst; the sparks' size starts small on arrival (`WALL_SINCE`).

**What remains, plainly:** no second client has seen a moving rider. The bot stands on a ledge, can't turn (the "right" picture has him facing the wrong way for the ride: an artefact), and doesn't travel. The sending and seeing ends share one number but were not run as one chain. Movement interpolation, a remote body's yaw and lean while really riding, and corners (yaw changing mid-ride) are unseen. The airborne clip order isn't in doubt: the local rider goes through the same chain.

## #109: pushes on a real client
Bot attacker, real client victim, standing on flat ground, 2 s `trace`, hero mode and Minecraft mode. Files: [a](runs/polish3/p109_before_a.txt), [b2](runs/polish3/p109_before_b2.txt), [c](runs/polish3/p109_before_c.txt), [after](runs/polish3/p109_after.txt), [carry](runs/polish3/p109_carry.txt).

**Result: on the base build every push reached the client already (#93's `Combat.send`). The issue's route is fixed; one thing was wrong, Hyper Strike's float.**

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
| Rally: bash + shockwave | 30.4, 6.2, **19.7 m** | 26.1, 3.4, 11.8 m | see owner's list |
| Gravitic Flux | top y -54.60 | top y -54.21 | |
| Catnapper: carry | y -58.89 under a bot on a ledge for its 4 s | y -58.8 | |
| Life Grip | 25 m/s by `GripPayload`, her own route | not played | |
| Lifeline's Jump | 7.8, 9.7, top y -56.44 | not played | |
| **Hyper Strike, before** | 12.9, 3.2, top y -59.52, **5.50 m**, on the ground at 0.45 s | 13.4, 2.4, top y -59.64, 2.88 m | NoAI husk, real Sigma at 2 m: 5.84 m, y -58.91 at about 0.7 s, in the air |
| **Hyper Strike, after** | 12.9, 4.0, top y -56.19, **13.8 m** | 13.4, 3.9, top y -57.12, 4.45 m | (mobs unchanged) |

**The fix (Sleep.stun, H values, M meaning):** D 54A5 #404's gravity x0.01 went only to mobs (`Sleep.slide`). A player's client moves them, so it never saw the float, and friction ate the knock. It now goes as Minecraft's gravity attribute, which Minecraft mode falls by and hero mode reads (`gravityScale`). Fix off and on, same build: `p_final_a.txt`, `## strike_*`. 13.8 m in hero mode is further than a husk's because hero mode's air keeps speed (its own mover). Whether that matches Overwatch is unknown.

**A wrong turn, kept here so nobody repeats it:** the first carry looked broken (top y -60). It was my test. A bot falls to the floor (`/tp` doesn't move it at all), so the hold point was underground. On a ledge the carry lifts with `syncVelocity` alone, fix off and on the same (`p109_carry.txt`), so I reverted my `Combat.send` there. `p_final_a.txt`'s `## carry_*` are that bad geometry; ignore them. Also: `p109_before_c.txt`'s `MOB Hyper Strike` is void: that husk had AI and just walked (y -60 throughout, no sign it was hit); the row's husk is from `p_final_a.txt`'s end. `p109_before_a.txt`'s quick melee (0.00) is `bot press melee` not reaching `QuickMelee`; `bot input` does (b2). `p109_before_b*.txt`'s Hypersphere 0.00 passed over her head: the `/tp` that was meant to aim did nothing, and `_c` was a direct hit only because Sigma's level shot met her body at that range: I can't say what pitch it had.

Not played on a real client: Hyper Strike into water or a wall; Rally's shockwave from a second enemy; Lifeline's rope tow as a force beyond #89's work; pushes on a slope; a victim in Valkyrie or gliding (share read from code only).

## #85: the rest of the crosshair
Scan of every hero's `StateUXPresenter` in the centre and reticle layers (`p3_ux.py`), against [reticle](../hud/reticle.md). Row by row there. Built:
- **Sigma, Hyper Strike:** data 0243 in the *ability bar* (51E0 #425: quick melee's icon and key, hits over 5). Ours: five pips under the crosshair and "[V] HYPER STRIKE" when charged. Name from 25111, which the data doesn't put on this widget. Volleys at a husk: 2, 4, 5 of 5 (a volley is two spheres); melee at 2 m consumes it, pips empty, husk thrown 5.8 m and floating (`sheet_p3_85_after`, `p3_85_sigma_struck`). Before: `p3_85_before_sigma` is in `run/screenshots`.
- **Lúcio: the note was wrong.** "Accelerando's meter (5162 #450)" is **Soundwave Rider's**: under gate #437, v66249 chased to 100 at 20 a second (#439: the wiki's 5 s), #443's 1.25 is its knockback. Accelerando is #391 -> #393 in the same graph and has no widget. So Accelerando gets none, and Rider's charge is drawn: a bar with the perk's icon, gold when full. Ride: 0.06 ... 1.0 at the 13th read, reads about 0.4 s apart by his 7.15 m/s, so about 5 s; held after (`p3_85_lucio_ride_half`, `_end`). Before: `p3_85_before_lucio_ride`, no bar, meter already rising. Icon by perk id: the data's is loadout CA7, not checked to be the same picture. M.

Not built, and why: Sombra's 0164 / 017B / 3692 / 3698 are the health pack entity 0216's (kit_pistol_hotfix.md: no packs in the mod). Ana's 469A / 5E62 sit in `work_voice`, not her kit (L). Sojourn's, Sigma's and Juno's identical 56D6 / 56FB / 5732 are a shared graph (its texts "Light Round / Heavy Round" are no kit of theirs). Lúcio's 0234 #433: a timed bar under a graph with loadout 00CB, undecoded (L). Sigma's barrier box with its broken icon (0241): ability bar, not done. D.Va: her branch, as told.

## Fights
Final tree. [p_fights](runs/polish3/p_fights.txt), `sheet_p3_fights` (one frame each at about 10 s). Midnight, survival, flat ground with no cover, 3 zombies + 2 skeletons + 1 creeper with AI, 22 s, regeneration on. **Harness input:** aim at the nearest each ~0.4 s, left button held, an ability every 2.5 s whatever the moment. Not a player.

| Hero | Deaths | Lowest health | Respawn read 1.5 s later |
|---|---|---|---|
| Kiriko | 1 at 13.8 s | 2.5 | 16 of 20 |
| Brigitte | 0 | 10.0 | |
| Ana | 1 at 18.8 s | 1.0 | 20 |
| Sombra | 1 at 9.8 s | 1.8 | 18 |
| Jetpack Cat | 1 at 11.5 s | 2.1 | 10 of 16 |
| Sigma | 1 at 15.6 s | 1.1 of 48 | 48 |
| Lúcio | 0 | 2.3 | |

Each came back as the same hero in hero mode. The two low respawn reads had mobs at the spot ([#111](hand_play.md#respawning-at-14-16-of-20-111-2026-11-04)'s pattern), not looked into. No errors in the log but the usual start-up ones (auth, Realms, sessionserver). Kill feed and HUD in the frames read at thumbnail size only: **shows they're there, not that they're right.**

## What a critic should check
- `p117_before.txt`'s Suzu lines are void: use `p_final_a.txt`.
- The boost's 28/25 is inference, and no recording since the patch exists.
- No Overwatch side by side for any of this. Pips, bar and prompt are ours in shape, place and colour.
- `leavePlay`'s corpse fix unplayed; `ana_anti_healed_end` unseen; Hotfix's end read from a slope.
- `bot fixoff` covers #117 and the float only. #115's before is the base-plus-dev-tool build.
- A rider with Soundwave Rider at full: Soundwave resets the bar (code), not played. Sigma's pips on a light husk are thin; bright backgrounds and other GUI scales not tried. Night: only in fights' thumbnails, neither new widget looked at there.
- Not tried at all: select screen and H menu as ways into these heroes (`hero` command here); leave and rejoin; water, lava, boats, slopes, the Nether; real mouse.
