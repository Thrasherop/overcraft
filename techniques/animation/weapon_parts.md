# Weapon parts: what hangs on a gun, and what moves it

Everything the data attaches to a weapon as a model of its own, and the rule that plays a weapon's clips (#91, 2026-11-04). Up: [Techniques](../README.md) ·
beside: [aim_sheets](aim_sheets.md), [additive_clips](additive_clips.md), [animation_import](animation_import.md) · glows:
[rendering](../rendering/rendering.md#glows-emission-from-the-data-2026-11-04-91) · code: `WeaponParts`, `WeaponClips`, `HeroWeapon.parts`

## Parts: a table, not code
`src/main/resources/assets/overcraft/weapon_parts.json`: one row per part: model, parent (weapon or hero), bone, offset, views, window, hidden. `build_asset_pack.py parts <extraction root> [hero]`
makes the `.glb`s from the same table, so a real install's build has them; `HeroModels` loads what the table names. Sources per row (research\<hero>\work\):

| Part | Data | Window | Conf |
|---|---|---|---|
| Ana's vial 1173F on the rifle | child entity 1607, slot 1F5C, on 017E = bone_0071, `models.json` offset (0, -0.04002, -0.07283), q (-0.26049, 0, 0, 0.965447) | always; 3P gone 0.2-0.4 s of the reload (effect 1D3C's Ctl at 0.2, its Model from 0.4) | H place, M when |
| Ana's dart 11740 | child 1605, slot 1F5D, on 017D = bone_0070 (0, 0, -0.05045) | always | H place, M when |
| Ana's vial in her hand | effects 1D40 / 1D82 on 3CE3 / 3CA0, Model on **00A5 = Weapon_L**, 0.1667 + 0.2333 s; `assets.md`'s 000B was an inference | 3P reload | H |
| Sombra's magazine EE06 | child 1637, slot 1FFA, on 01B5 = bone_0955 (zero) | always; 3P gone 0.033-0.2 s (3E60's Ctl, then its Model on 01B5 0.2 + 1.3 s) | H place, L the Ctl |
| Sombra's Translocator 10518 in hand | 3D95's Model on 000B, 0 + 0.033 s; 3DAF / 3EC0 / 3EC8 Ctl at 0 and about 0.06 s | a flick at the throw | H 1P, M 3P |
| Juno's cell 167A9 | child 5ADB, slot E709, on 01B5 = bone_2B9D (body table: zero). No vertex of her body or the preview gun skins to that bone: it was never in the gun (the old comment in `JunoFx` was wrong) | always; the clip scales the bone to 0.01 while it's gone (E9B6 frames 7-25, F6C3 5-7) | H |
| Juno's old and new cell, 1P | E9B6's 08F 5FA4: 5ADB on 01B6 = bone_097F 0.2333 + 0.6333 s, on 000B 0.3333 + 0.5333 s | 1P reload | H |
| Juno's cell, 3P | F6C3's 08F 60BA: 5ADB on **02A9**, 0.1667 + 0.4 s. 02A9 isn't in the body's table and the look isn't dumped; the clip shows 097F (scale 0.01 outside frames 5-28), so `bone_097F` | 3P reload | H window, L bone |
| Sojourn's magazine D7D0 | entity 32A0 (look EBD2): 70D5's 08F 30C4 on the gun's 01B5 = bone_0955 (zero offset), 0.1333 + 1.1667 s; 7058's 37E7 on 000A = Weapon_R, 0.4 + 0.3 s | 3P reload. 1P: the gun's own 60 v submesh on bone_0955 | H |

**Offsets:** read them from `models.json` (`pos`, `rot_xyzw`, the bone's frame) or `work\lookhp_<model>.txt`'s "offset". The `.owmdl` empties and that file's "world" are places at rest, not
offsets: taken as offsets, Sojourn's magazine would sit 25 cm off and Ana's vial 16 cm.

**What the timed effects are:** `work\fx_sounds_timed.json`, the `.08F` of each clip: timed Model and Entity components. That's where reload parts live, not in the entity tree.
**Not parts:** Lúcio's cartridge is submeshes 1 + 3 of his amp on bone_008B, already drawn (C52 / DBA4, its loose copy, has no referrer found). Sojourn's resting magazine is submesh 1 of her gun.
The 0-component children (2860, 2B1E, 2B21, 2862, 1B86) draw nothing.

## Clips: a rule
`WeaponClips`: a clip name is `[add_]<base>[_1p|_3p|_sm0|_sm1]`; `<base>` is the action it answers (Combat.animate's name, or an alias: `ult_cast` <- sound_barrier, ult_cast, ult_start;
`melee` <- melee, melee_2, melee_3; `fire_a/b/c` <- spheres_0/1/2), the suffix the data's two-state machines (sm[0] first person, sm[1] third). The newest pose clip plays; layers add. So Sigma's spheres,
Brigitte's flail head (which now poses from the same rule), Sombra's EMP (ult_cast, add_ult_cast_end), Ana's 1P fire, need no case of their own.
**Ana's reloads, corrected** ([critique_parts](../testing/critique_parts.md) A3; 60c892b claimed this too early): the rule found plain `reload` first, which is the third person's clip. Now in first person she plays `reload_ammo_left_1p` 3C99 (the empty one 3C5A isn't extracted), in third `reload_ammo_left` 3D87 when she has ammo (known for the local player; assumed for others, the common case) else `reload` 3CE4. **Sombra's melee** is sm[0] only in the data, so the clip is `melee_1p` now and doesn't play in third person.
State layers go on after the pose clip: first they were lost on its keyed bones for its whole length (critique C3).
By state: Sigma's stance loop and air, Lúcio's `add_move`, Brigitte's `idle_shield`, Mercy's beam (start, loop, end; heal plain, boost `_2`: M which is which), Sombra's held fire (on the burst's clock, then its end).
- **Ours:** Sojourn's energy machine. Its conditions are undecoded (`clipmap.md`: "conditions undecoded"), so empty = state 0, charging = 3, full = 2 and each change plays its data clip.
- **Mercy's blaster's `reload`** is from an additive folder (4-4): played as a layer now. As a pose it collapsed its unkeyed bones.

## On-gun displays
- **Juno's gun's screen** is material FB30, shader 0x32, image 4E48C: **red the frame, green the happy face, blue the angry** (seen by splitting its channels). `glow_maps.py` bakes frame + face twice; `ClientJuno.faceAngry(juno)` picks by what that Juno aims at, raycast from her own eyes once a tick (enemy: angry; the wiki's rule). The screen's colour is ours: its ramp is flat grey.
- **Ana's counter:** 0B94 #542 / #543 give tens and units, 9 from 100 up; how they draw isn't read. The sight's screen is material BB56 (image 38704). Digits ours, on that piece, facing out, on a dark plate (`HeroModelRenderer.anaCounter`): without the plate they read as a scribble over the screen's art at play distance (critique D).
- **Lifeweaver's first-person thorn counter** is his own bones: overrides D36A ... D361 hold more bones scaled away, eight states (`thorn_count_1..8`, `HeroAnimator.thornCounter`; M that they count thorns left, ours eight even shares). The additive D31B ... and the transitions aren't played.

## Building it: what a real install needs
`py tools/build_asset_pack.py weapons <root> [<root> ...]`, then `zip`. A root holds `Heroes/<Hero>/Skin/...` (and `Unlocks/...` for emote clips): on this machine `E:\Overwatch Archive\extracted\154088` (Jetpack Cat, Kiriko, Lúcio, Mercy) and `E:\Overwatch Archive\research\<hero>\extract` for ana, sombra, brigitte, juno, sojourn, sigma, lifeweaver.
- `parts <root> [hero]`: the models of `weapon_parts.json` (Blender). `clips <root> [hero]`: every `tools/clipmaps/*_clips.txt`, from the model folder that holds its clips (Blender; checked: the same clip names as the hand-built pack for Mercy, the Cat, Lifeweaver and Sojourn). `glows <root> ...`: `glow_maps.py` (Pillow, no Blender).
- `zip` warns of what the pack lacks, and the game logs "Asset pack incomplete" per missing part: without them the player quietly sees none of #91.

## What a critic should check
- First person at rest: Ana's orange vial in its bay, Juno's orange cell in its well, Sombra's magazine in the grip; nothing floats.
- Slow a reload (`/tick rate 5`) in both views, side-on with `aimpose`: Sojourn's bar comes off, flies and meets the raised hand; Juno's old cell leaves the gun as the well empties. A part must not sit where the data has it hidden.
- Sigma's spheres leave the hand and come back on each volley; Brigitte's head moves with the swing.
- **Compare with Overwatch:** no frame-by-frame side-by-side was possible (Overwatch stays closed). The placement comes from the data; the "looks right" is ours.
- **Not done:** the additive thorn-counter layers; Ana's 3P `reload_ammo_left` (a remote player's ammo isn't known here); Lúcio's and Kiriko's and the Cat's clip effects (no timed JSON on disk); which slot each Ctl names (a TankLib pass).

## Hand play, 2026-11-05 (fresh worlds wpk1-wpk5, each session started after checking the machine had no other client, `DevServer on` read in its own log)
Shots in `run/screenshots/` of this worktree. Steps, what was seen:
- **`aimsheets` for all heroes, `aimcheck` per pitch:** the tables in [aim_sheets](aim_sheets.md).
- **`timing glow N`:** the table in [rendering](../rendering/rendering.md#glows-emission-from-the-data-2026-11-04-91).
- **Final bake, 3P, all 11 heroes, up / level / down, front, back, night (`k2_<hero>_*`), 1P day and night (`k2_<hero>_1p*`):** glows hold; no piece lit that wasn't meant to be seen.
- **Reloads side on, 1/4 speed (`k2_sojourn_3p_*`, `k2b_*`):** Sojourn's bar leaves the gun, rises and meets her raised hand (frames 6-8). Ana's reload in 3P and her hand-off at this size: the hand vial is small in frame (`k2b_ana_3p_*`); Juno's 3P cell (`k2b_juno_3p_*`): the well empties, the part reads only at full size. Ana's 1P reload (`k2_ana1p_*`): the 3C99 clip, with her vial.
- **Real fights through the asset zip (`-Dovercraft.assetPack`=overcraft-assets.zip, 1.3 GB, 64 glow images, 48 glow tables, all five parts), night, normal difficulty, zombies and a skeleton hitting back (`k3_<hero>_fight*`):** Kiriko (the first pick missed), Sojourn, Ana, Juno, Brigitte, Lifeweaver: health 20 to 11, 18 to 11, 20 to 11, 18 to 11, 20 to 18, 20 to 3; kill feed and ELIMINATED showed. Reload cancelled by an ability (`k3_*_cancel_*`): no stuck part or pose.
- **A new world under the same client (`k3_newworld_up`):** the sheet plays at once. `BY_ENTITY` was already cleared on disconnect, and `aimSheets` now also survives time going backward.
- **Hero select, each portrait (`k5_sel_*`):** weapons and glows right; Sigma's spheres, Sojourn's Railgun, Mercy's staff.
- **Brigitte's barrier up with her body in view (`k5_shield_*`, the ability's view off under `aimpose`):** she stands behind her plate, the shield sheet (`aimsheets`: 10, [3], 24, 17) moves her head and torso little, as its keys do.
- **Sombra's real stealth (`k4_stealth_*_b`):** the magazine phases with the pistol, both views.
- **Improved Transparency on the final bake (`k2_it_*`):** Ana with digits, Sigma, the Cat, Lifeweaver, both views: no crash, nothing black. **F3+T (`k5_f3t_*`):** back in play with Juno's face and Sojourn's sheet; it took over 2 minutes and first ran out of memory at 3 GB (the model's PNG bytes were kept: freed now; OOM lines 0).
- **Juno's face** by her own aim: `g1_juno_enemy`, `g1_juno_ally`; per entity since, with one Juno in the world.

**Not tried:** a second player (another Juno's face, other players' sheets and cost); hero fights for Lúcio, Mercy, Sigma, the Cat; water, lava, boats, the Nether with these parts; Sigma's and Sojourn's emotes after the rule. **GPU cost** isn't measured: the client stays at its 30 fps background cap.
**Brigitte's chain (`k2_brig_c`, 20 tps):** in her first-person fling the chain's ends don't meet the head at the frame shot. Not from this work: the head's clips have no root motion (keys 0) and the chain's code and ends are unchanged. Open.
**Lúcio's yellow discs and Mercy's white blobs:** effects (`LucioFx.skates`, `MercyFx.wingGlows`), in `next`'s shots too (`z0_*`): not glows. Not touched.
