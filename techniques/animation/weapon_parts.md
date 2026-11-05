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
Brigitte's flail head (which now poses from the same rule), Sombra's melee and EMP (ult_cast, add_ult_cast_end), Ana's 1P reload and fire, need no case of their own.
By state: Sigma's stance loop and air, Lúcio's `add_move`, Brigitte's `idle_shield`, Mercy's beam (start, loop, end; heal plain, boost `_2`: M which is which), Sombra's held fire (on the burst's clock, then its end).
- **Ours:** Sojourn's energy machine. Its conditions are undecoded (`clipmap.md`: "conditions undecoded"), so empty = state 0, charging = 3, full = 2 and each change plays its data clip.
- **Mercy's blaster's `reload`** is from an additive folder (4-4): played as a layer now. As a pose it collapsed its unkeyed bones.

## On-gun displays
- **Juno's gun's screen** is material FB30, shader 0x32, image 4E48C: **red the frame, green the happy face, blue the angry** (seen by splitting its channels). `glow_maps.py` bakes frame + face twice; `ClientJuno.faceAngry` picks by what she aims at (enemy: angry; the wiki's rule). The screen's colour is ours: its ramp is flat grey. Limit: read from the local player's aim, so another player's Juno shows your own target's face.
- **Ana's counter:** 0B94 #542 / #543 give tens and units, 9 from 100 up; how they draw isn't read. The sight's screen is material BB56 (image 38704). Digits ours, on that piece, facing out (`HeroModelRenderer.anaCounter`).
- **Lifeweaver's first-person thorn counter** is his own bones: overrides D36A ... D361 hold more bones scaled away, eight states (`thorn_count_1..8`, `HeroAnimator.thornCounter`; M that they count thorns left, ours eight even shares). The additive D31B ... and the transitions aren't played.

## What a critic should check
- First person at rest: Ana's orange vial in its bay, Juno's orange cell in its well, Sombra's magazine in the grip; nothing floats.
- Slow a reload (`/tick rate 5`) in both views, side-on with `aimpose`: Sojourn's bar comes off, flies and meets the raised hand; Juno's old cell leaves the gun as the well empties. A part must not sit where the data has it hidden.
- Sigma's spheres leave the hand and come back on each volley; Brigitte's head moves with the swing.
- **Compare with Overwatch:** no frame-by-frame side-by-side was possible (Overwatch stays closed). The placement comes from the data; the "looks right" is ours.
- **Not done:** the additive thorn-counter layers; Ana's 3P `reload_ammo_left` (a remote player's ammo isn't known here); Lúcio's and Kiriko's and the Cat's clip effects (no timed JSON on disk); which slot each Ctl names (a TankLib pass).
