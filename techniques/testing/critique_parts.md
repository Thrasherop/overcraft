# Critique: weapon parts, aim sheets, glows (fix/weapon-parts, #91, #95)

Independent read of 18597f0..ea8923a, 2026-11-04. No game run, no build, nothing fixed. Overwatch and Battle.net were checked closed before the
archive was read. Up: [Techniques](../README.md) · reviews: [aim_sheets](../animation/aim_sheets.md), [weapon_parts](../animation/weapon_parts.md),
[rendering "Glows"](../rendering/rendering.md#glows-emission-from-the-data-2026-11-04-91) · method beside it: [visual_review](visual_review.md)

**Verdict: do not merge yet.** It merges cleanly and nothing here crashes on reading, and most heroes are far better than before. But two sheets are wrong or
rejected, a claimed fix is dead code, one audit item is silently still open, the performance was never measured, and the evidence for reloads and for the final bake
doesn't show what the records say. Keep #91 and #95 open.

## How this was checked, and its limits
- **Re-measured the sheets independently:** a Python copy of `AimSheet.layout` over the `.glb`s in `overcraft_ana/local-assets/pack` (the pack this branch played with). It reads keys rather
  than running `Skeleton.sample`, so run DevServer `aimsheets <hero>` to confirm A1 and A2.
- **Screenshots:** read as native-resolution crops in contact sheets, some full frame. Opened: b1 (all 11 heroes, 3P and 1P), c2 Ana / Sombra / Sojourn / Juno, c3 all, g1 Ana states, h1 Sojourn /
  Juno / shield / Sigma / stealth, e1 Mercy, z1 Lúcio / Mercy / stealth, z0 Lúcio / Mercy, i1_lucio, one g1 reload. **Not opened:** h1 Brigitte states, g1 Sombra states, c2 Brigitte, f1, d1, j1,
  the other i1, the emote, death and Minecraft-mode h1, the other z1 and z0.
- **Overwatch comparisons are from memory.** The client stays closed, so treat them as a critic's opinion.
- `git merge-tree` left one unreferenced tree object (476db14) in the repo. Git only ran here with a per-command `-c safe.directory` (the drive records no ownership); no config changed.

## The stray session (23:04-23:07)
Log `run/logs/2026-11-04-7.log.gz`: the client started 23:04:44; world `rlb2` was live 23:06:45 to 23:07:06 (`/kill`, summons, tp). That session's log shows the commands the coordinator described.
- **No screenshot is dated inside it.** Every b1, c2, c3, g1, h1 and z1 predates 22:45, so none of them is touched.
- **`i1_*` (7 shots, 23:07:28-23:07:44) are outside the window but suspect:** same client process, 22 s after the strays left, world `wp2` rejoined at 23:07:16. I can't tell from the log who drove that rejoin.
- **`j1_*` (23:10:50-56) are clean:** a fresh process. They retake only Sombra's melee and EMP. **`i1_lucio`, `i1_lucio_night`, `i1_mercy` have no clean retake**, and they are the only after-shots for ea8923a's fix (B4).
- No record cites an `i1` or `j1` shot by name. `rendering.md` cites `z1_*` (22:43-22:44), which is clean but predates the fix. Don't count `rlb2` as testing; it sits in `run/saves`.

## A. Wrong now (fix before merge)
**A1. Sojourn's crouch-walk rides a false sheet.** `add_crouch_walk` (42 frames) passes the column rule as frames 17, 3, 31 with chain pitch +39 / 0 / **-3**. The span test (39 + 3 >= 30) passes
though the up end barely moves. So aiming up while crouch-walking, her gun stays about level and her head takes nearly the whole 90 degrees. `aim_sheets.md`'s table doesn't list it, and it isn't "a breath" like `add_crouch`.
The shots took only `crouchwalk_down`, the half that works. Fix: require a minimum on each side of level.

**A2. Brigitte's shield sheet is rejected, while the code says it works.** `AimSheet`'s javadoc claims "her shield's 28". Its rows are pitch +27 (frame 10), 0 (3), -74 (17), -37 (24), so the column is right.
But frame 10's yaw is -16 against `ROW_YAW` 15, so it is dropped; level then sorts to the top and `0 < at` fails. It falls through to `add_aim` with the shield up.
`g1_brig_shield_*` and `h1_brig_shield_*` can't show this either: the h1 pair I opened are filled by the barrier with no body visible. The record lists "Brigitte with her shield up" as a check; it has no evidence.

**A3. Ana's first-person reload fix is dead code.** 60c892b and `weapon_parts.md` claim it. `pick()` tries `reload_1p`, `reload_sm0`, then plain `reload`, which exists, so the `reload_ammo_left_1p` fallback is never reached.
The clip map says plain `reload` is the 3P one (3CE4, df 0x66). In first person she plays the third person's clip, as before the branch. Ammo-left reloads (the common case) also never pick `reload_ammo_left*`.

**A4. Two glows are cut by `SKIP_MODELS` prefixes, one of them an open audit item.**
- `juno_c` catches `juno_cell`: its material FBB8 is lit on her body (strength 2) and the cell has no `_glow.json`. The cell #91 asked for goes dark at night (`b1_juno_1p_night`).
- `brigitte_sh` catches `brigitte_shield` (the audit's "479-v glow is not drawn", #91). Its piece with no colour image still draws nothing, and the shield is unlit at night. `rendering.md` doesn't list it as not done. Prefix lists like these will keep eating real models: use exact names.

**A5. The required play didn't happen.** CLAUDE.md requires hand play before a merge. No record shows a real fight, mobs hitting back, or a second player; the commit messages list no steps. See section E.

**A6. No frame-time number exists for a change that doubles draw work** (C1).

## B. Evidence that doesn't show what's claimed
**B1. The reload shots mostly miss the reload.** `weapon_parts.md` says to shoot side-on with `aimpose`; `c2_*` are from behind the player. `c2_sojourn_*`: the gun is behind her, so the bar that "comes off, flies and meets the raised hand" isn't seen
(one bar enters at the edge of frame 8). `c2_ana_*`: hood and barrel only; the 0.17-0.4 s hand vial is never in frame. `c2_sombra_*` does show her magazine in hand (frames 6, 7). `c2_juno_*`: gun at frame edge, cell not readable, and the L-confidence 3P row is what needed proving.

**B2. `c3_sigma_*`:** spheres visible briefly at frame edges; "leave the hand and come back" isn't shown. The hit effect is a large white-out at the crosshair.

**B3. `c3_brigitte_1p_4/5`: the chain doesn't meet the head.** In 4 the head is at (820, 465) and the chain's tip at (1290, 535). In 5 the head is back at the hand while the chain lies across the field, attached to neither end.
The head's clips hold no root motion (`swing_1p` under 0.01 m), so it's not a doubled fling. Those shots ran at `/tick rate 5`, so my guess is tick-stepped chain particles against frame-stepped bones. **Unresolved:** check at 20 tps, since 60c892b changed the head's pose here.

**B4. The final bake was barely photographed.** The pack was baked at 22:46, after all of z1. Only i1 and j1 run on it, so Lúcio's and Mercy's shader 0x26 fix rests on suspect shots. No third-person shot of any hero, and nothing for Ana, Juno, Sojourn, Sigma, Brigitte, Kiriko, Lifeweaver or the Cat, shows the bake that ships.
Improved Transparency was tried on Juno only, before the bake: Ana's text digits, Sigma, the Cat and Lifeweaver under F3+X are untested.

**B5. `b1_mercy_*` predates her sheet;** they show the staff fixed at any pitch. `e1_mercy_*` (21:36) are the real ones and they look right. Don't cite b1 for Mercy.

**B6. `b1_brigitte_side_*` aren't side views:** they're from behind, so the flail's aim can't be judged. The flail and arms barely change with pitch; only the head does.

**B7. `h1_sombra_stealth_*` are solid, not phased:** the `_stealth` sheet is on, not her real look. `z1_stealth_*` is the phased look, via an invisibility effect.

**B8. Fresh-world rule:** all sessions reuse `wp2` since 21:04, with burning mobs in shot.

## C. Code
**C1. Glow cost is much larger than "107 materials" suggests.** Emission masks live on each hero's main material, so most triangles are skinned and emitted twice:

| View | Share of triangles drawn twice |
|---|---|
| 3P | Sojourn 49%, Lúcio 67%, Sombra 69%, Cat 69%, Brigitte 70%, Ana 72%, Juno 77%, Sigma 78%, Mercy 80%, Lifeweaver 86% |
| 1P | Ana 76%, Cat 81%, Brigitte 85%, Sombra 94%, Lúcio 99%, Juno, Sigma, Mercy, Lifeweaver 100% |
| Weapons | 99-100% |

Lifeweaver in 3P adds 55,672 triangles to 64,550. Three things make it worse:
- `FxRender.additive` has `sortOnUpload()`, so these meshes may also be sorted every frame (built for small effects; cost unmeasured).
- The glow lambda isn't timed, so `Timing.emit` under-reports it. DevServer `timing` will look better than the truth.
- Textures are registered per model: 63 images (42 MB RGBA) loaded 107 times, about 70 MB if sizes average out (my estimate).

A mask-only index (just the lit triangles) would remove most of it. Measure first, with several heroes on screen.

**C2. Weapon pose recomputed per part.** `parts()` calls `place()` (a full `pose()`), then `pose()` again per on-weapon part, plus `rest()` and `skin()` per part. By my reading Ana costs about six weapon poses a frame, each iterating clips with a stream, string concats and arrays in `WeaponClips.play` (`sigma_weapon` has 49 clips). Cache one pose per frame per model. `aimSheetNow` also builds a list of strings per entity per frame: small, but needless.

**C3. `WeaponClips` drops state layers during a pose clip.** `state()` adds energy, beam, move and stance layers first, then `newest` is sampled over them. Keyed bones lose the layer for the clip and pop back at its end. The old code added after sampling. Likely visible on Sojourn's energy during reload and Lúcio's `add_move` during reload.

**C4. Sombra's melee plays in third person.** The clip is named plain `melee`, but the data has it as sm[0] only ("L which view"), so the rule's own sm[1] logic is bypassed. Same shape: Brigitte's `shield_raise` plays in first person while `idle_shield` is excluded there (clip map has no split, so M).

**C5. Stale state in `aimSheets`.**
- Cross-fade: a change mid-fade sets `before` to the fading sheet at full weight, so it pops. Rare.
- `aimSheetAt` isn't reset when `now` goes backward. `BY_ENTITY` is keyed by entity id, and I saw no clear on leaving a world in the lines read. If an animator survives into a world with a lower game time, `w` clamps to 0 until time catches up and that hero has no sheet. **Test: leave a long world, join a new one, same id, pick a hero.** The same reasoning applies to `energyAt`, `beamOnSince`.
- Two models on one animator per frame (a preview) would reset every frame and give weight 0: harmless at pitch 0.

**C6. Line truncation.** "Walk while monotonic" stops early: Ana's `add_aim_jump_scoped` line is 0..16 of 0..20 (up end -39 against -91 unscoped), and `add_aim_scoped` is 45..62. Since +-90 maps to the truncated end, the gun falls short and the head makes up the rest.

**C7. Sheets rejected by `NEUTRAL` 8:** Juno's `add_crouch` and `add_crouch_walk*` (11 frames, her real sheet layout, min 19-21) fall back to the stand's. Same for Sojourn's `add_run_back`, `add_run_right`; Brigitte's `add_aim_run_back_left`; Mercy's `add_idle*`. Either decide they aren't sheets and say so, or allow a level frame with a constant offset. Back slots skip the run fallback, so Sojourn's run_back uses her stand's.

**C8. Caches.** `AimSheet.SHEETS` holds strong references to clips; each resource reload keeps the old ones. `WeaponParts.load` clears a plain `HashMap` on a worker thread while the render thread reads it. Both low.

**C9. Stealth and first person.** In 3P the stealth path draws only the weapon, so her magazine vanishes from the phased pistol. In 1P, `extras()` is submitted solid before the stealth branch, so the magazine and its glow look solid on a phased pistol (from the code; no shot opened shows it). Check.

**C10. `AIM_PITCH` is ours,** and df 0xB6's scale is unread (the record says so). Jetpack Cat's sheet is on a different driver (df 0x1C) and is mapped the same.

### WeaponClips matches, per hero
No false match to another action found: the prefix list and bases don't collide. The wrong-view plays are C4 and A3.

| Model | Plays | Never fires (no such action found) |
|---|---|---|
| ana_weapon | `reload` both views; fire via `fire()` | `reload_ammo_left*` |
| brigitte_weapon | `swing*`, `whip*`, `whip_return`, `_blocked`, `_miss`, `shield_raise` both views, `ult_cast_1p` (ult_start) | `stunned`, `swing_end*`, `swing_link*`, `swing_windup*`, `whip_2_1p`, `whip_return_close*` (no literal found) |
| flail head | as the handle, plus `ult_cast` 3P | same |
| lucio_weapon | `reload`, `reload_1p`, `ult_cast` (sound_barrier), `add_soundwave`; `ult_end` by chain | |
| mercy_blaster | `reload` (as a layer, by a shape heuristic), `add_fire` | |
| mercy_staff | `add_flash_heal`, beam start, loop, end | `add_flash_heal_2`, `add_beam_short*`, `add_beam_swap*` (the audit asked for swap) |
| sigma_weapon | `fire_a/b/c_sm0`, `fire_a_sm1`, `melee_sm0/1`, `accretion_cast/throw_sm0/1`, `flux_start/aim/slam`; `_sm0` in 3P where no sm1 | `a5ac*`, `a65c*`, `flux_lift`, `stun_sm0` |
| sojourn_weapon | `reload`, `reload_1p`, `add_charged_shot_*`, fire by view, energy by state | |
| sombra_weapon | `reload`, `reload_1p`, `melee` both views, `ult_cast`, `add_ult_cast_end` by chain | |

## D. Parts against the data
Checked rows hold, with the timed effects in `fx_sounds_timed.json`: Ana 1D3C, 1D40 and 1D82; Sombra 3E60 and 3D95; Juno 5FA4 and 60BA; Sojourn 30C4 and 37E7. Ana's and Sombra's 1P reload effects (1CF5, 1D86, 1DEB) have no Model, so no 1P hand-off is correct.
- **Juno's 3P row is a guess as claimed:** 02A9 is unresolved, `bone_097F` is by inference. Its L rating is honest; c2 doesn't verify it.
- **`weapon_parts.md`'s Sombra Ctl reading stays L.** Ctl targets aren't read.
- **Rows use the parent bone:** hp 000A / 000B are child bones of Weapon_R / L (Ana's table: bone_002F, bone_002E, "same places"). Fine unless a clip keys the child.
- **Ana's digits:** in `g1_ana_rel2` they read as a scribble on top of the sight's own art at normal view, not as "15". Worse than none. From memory, I can't confirm the real sight shows digits this way; the data's drawing is unread. Fix the size and contrast or remove them.
- **Wrongly shown in 1P:** none found. Ana's vial, dart, Sombra's magazine and Juno's cell sit where they should at rest.
- **Still missing a player would notice:** Brigitte's shield glow (A4); Mercy's `add_aim_blaster` isn't in the pack (`mercy_clips.glb` holds only add_aim, hello, hello_blaster, though the clip map and 18597f0 list 0AC3), so her blaster rides the staff's sheet; the audit's staff rest pose on the select screen not re-checked. Lúcio, Kiriko and the Cat have no timed JSON, so "no parts" there is unknown, not confirmed.
- **Juno's face from the local aim: not acceptable as written.** Any Juno in view shows what *you* aim at. It's cosmetic and low severity, but it's plainly wrong in co-op, with a cheap fix: show the plain face on anyone but the local player. Doing it right means raycasting per Juno.

## E. Looks: hero by hero
| Hero | Aim, 3P | Glow |
|---|---|---|
| Ana | Rifle along the aim, up and down; good. Fully down the barrel reaches the ground and the head is buried in the collar. | Strips hold at night; sight screen fine by day and night. Good. |
| Sombra | Good, gun on aim. Fully up her head rolls far back. | Magenta holds. Good. |
| Juno | Good. | Helmet rim strong and clean. Cell dark (A4). |
| Sojourn | Up and level good. **Down: Railgun inside her thigh** (`b1_sojourn_side_down`, `h1_sojourn_crouch_down`). | Red strips flat but fine. |
| Sigma | **Hands and spheres barely change with pitch;** the head sinks into the armour at both ends. From memory his hands follow more. | Spheres good. |
| Brigitte | Can't judge (B6). | Flail lit, shield dark (A4). |
| Lúcio | Good; amp through the leg fully down. | Amp fine. **Hard yellow discs at the calves** by day and night: `LucioFx.skates` "0.3 m: ours". **Pre-existing** (`z0_lucio_night`) but they look wrong next to the rest. New issue. |
| Mercy | Good in e1. | Wing strips good. **Large white blobs behind her head by day, gold at night; pre-existing** (`z0_mercy_up`). New issue. |
| Lifeweaver | Hands fixed in the stance read; neck takes most of the pitch, and fully down it looks broken (`b1_lifeweaver_side_down`). | Back flower and strips bright; acceptable. |
| Kiriko | Good. | Faint; fine. |
| Cat | Fully down the pack flips over her head (chain +86). Looks extreme; driver unread. | Good. |

Feet stayed planted in everything opened. Spines bend hard at +-90 on all heroes; Overwatch stops at about +-89 too, so it's mostly true to the data's ends, though the `AIM_PITCH` mapping is ours. Juno's arm patch reads as a black square (`c2_juno_*`), not compared with z0.

**Not played that a player hits:** real fights; a second player (other players' aim, faces, parts, glows, cost); Sigma's and Sojourn's emotes after the rule change (the emote path bypasses `WeaponClips` through `staffClip`, but only clips the emote plays; nothing read says a Sigma emote without one is safe); Lifeweaver's thorn reload and thorn counter in a fight; Lúcio's and Mercy's blaster reloads, 1P and 3P, at speed; reload cancels (Kiriko alone sends `reload_cancel`; a cancelled reload keeps its part windows running); hero select preview with glows; rejoin in a new world (C5); F3+T mid-game (C8).

## F. Real-install path
`build_asset_pack.py` alone doesn't produce this branch. A player would need:

| Piece | Command | If missing |
|---|---|---|
| Parts | `parts` (Blender) | Part skipped: no magazine, as before |
| `sombra_beacon` | none: its row has no `id`, so `parts` skips it, and it comes from an earlier hand step | No flick: fine |
| Glows | **separate** `tools/glow_maps.py <roots>` (needs PIL); `zip` doesn't call it | World-lit, cards undrawn: as before |
| Glow roots | undocumented: the archive's `extracted\154088` alone gave "no owmat" for FBB8, AD9F and others, so the roots used aren't recorded | Silent gaps |
| New clips: `add_aim` for Mercy and Cat, `thorn_count_*` | **no command:** `owm_survey.py --clips-only --keep` by hand per clip map | No aim sheet for Mercy and the Cat (head only), no thorn counter |
| `add_aim_blaster` | listed but absent even here | Staff's sheet |
| Ana's model | `add ana` **exits**: `HEROES` lacks ana (also D.Va). Pre-existing | No Ana |

Absent pieces degrade quietly and don't crash, by reading (`HeroModels.get` null, `readGlow` empty, `AimSheet.of(null)`). That quiet is the risk: **neither zip has the parts or the glows** (`overcraft/local-assets/overcraft-assets.zip`: 0 glow files, no vial; `overcraft_ana`'s: 1 glow file, no vial), and `overcraft`'s pack holds 64 model files against `overcraft_ana`'s 228. So a test build with those zips would show the owner **no sign of #91**, while the code looks fixed. Rebuild the zip before a test build, and add a pack-completeness warning.

## G. Merge with next (1feb188)
`git merge-tree --write-tree 1feb188 fix/weapon-parts`: **clean, no conflicts** (tree 476db14). Not compiled.
- `HeroModels.Loaded`: the only `new Loaded(` in next is the line this branch changes. Fine.
- `HeroSyncPayload`: next adds `Widgets`; `primaryAmmo` and `secondaryAmmo`, which this branch reads, are unchanged. Fine.
- `DevServer` and `techniques/README.md` both change; merged textually.
- Next adds no new animation action in what I read of its diff, so no new clip match.
- **Duplicate readouts, by design question:** Ana's on-gun digits and the HUD's ammo; `ReticleHud` shows nothing conflicting that I found.
- After merging, repeat one play pass: next's reticle widgets over a 1P view with brighter guns have not been seen together.

## H. Severity order
1. A1 Sojourn crouch-walk sheet
2. A2 Brigitte shield sheet
3. A4 Juno's cell and Brigitte's shield unlit
4. C1 / A6 glow cost unmeasured
5. A5 / E hand play gates
6. A3 Ana's 1P reload dead code and false claim
7. B1 / B4 reload and final-bake evidence
8. F the zip and the undocumented exports
9. C3 pose drops state layers; C5 time going backward
10. C4, C6, C7, C9, D (digits, `add_aim_blaster`, Juno's face), Sojourn's gun through her leg, Sigma's hands
11. New issues, pre-existing: Lúcio's discs, Mercy's blobs, `add ana`
