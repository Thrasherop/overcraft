# Overwatch side by side, 2026-11-05: Sombra, Ana, Juno

The owner recorded the practice range (clips 19.16.58.02, 19.17.10.03, 19.17.30.04) and sent frames. Sombra's and Ana's are in `local-assets/raw/side_by_side_2026-11-05/` (`1.webp` Ana, `2.webp` the throw, `3.webp`, `4.webp` stealthed); Juno's came only through the chat, so their positions are written below. The frames are shots of a media player, whose bar covers the game below about ndc y -0.84. Issues: #87, #90, #86. Up: [Techniques](../README.md) · [hand_play](hand_play.md) · [reticle](../hud/reticle.md) · heroes: [sombra](../heroes/sombra.md), [ana](../heroes/ana.md), [juno](../heroes/juno.md)

Data: build 154088, read offline, nothing played in this read. H read directly, M read plus a measured match, L inference. ndc: screen space, -1..1 from the centre. Our shots: `sbs_*`, `sbs2_*` on ac69e1a (harness keys, for looking).

## The lesson
**A live frame beats our reading of a widget's schema.** Three of our "from the data" claims were inferences the data never held: "ring" (008B is a bool and a share), "the pistol is put away" (the wrist drops), "the tint is at the edges" (a node beside the one we read does it). Compare with the real game before calling a look data-driven.

## A compiled UI library exists: `000000000003.10F`
[reticle](../hud/reticle.md) says the layout isn't in the data: that's now in doubt. The `.116` files are only schemas (H), but `.10F` is one file of 20,608,408 bytes, header DUTS, 21,724 table entries: 1,334 textures, 3,330 strings, 545 effects, 261 sounds, with tagged floats and colours and framework test pages ("Shapes", "Flipbook"). It reads as a compiled retained-mode UI (M).
- Format as read (M): `01 TT 00 00`, u32 relative offset, value. `06` float, `19` / `24` resource, `02` colour (A, R, G, B), `03` / `0B` four floats, `0D` transform. GUID type bits: `hi = reverse12(type - 1)`.
- **Unknown: how a `.116` finds its elements.** No `.116` GUID is in it, and no binding id appears as `.01C` (E615, E77D, 9C89, 67AF, 009A, 646D, 5BC2). Today elements are found by what they hold (a string, a texture). Linking the two is the next step for every HUD item.

## Sombra
1. **Stealth HUD.** 420C #1057 shows 0277.116 with slot 67AF = 008B.116 (`009A = v215 / v567`). 0277 is her reticle's status container; 008B is a bool and a share (H). Live: her small crosshair stays; a bar centred about 122 px below it (at 1080 high), about 279 px wide, 8-9 px tall, round ends, fill (240, 100, 250) anchored left, draining left, translucent grey track; label about 148 px below, white caps, cap height about 14 px. Label string `250D4.07C` "Invisible" (M). Bar's numbers are measured: not found in the data.
2. **Screen tint: 420C #851 STU_1B93D61B** (H node, M role), beside 1A75 under #129: colour R .8431 G .5451 B 1.0, amount 0.7, own first person only. Linear multiply by lerp(1, colour, 0.7) predicts white -> (241, 214, 255); a white wall in frame 4 is (249, 215, 255). In Minecraft: a whole-screen multiply of about (0.945, 0.843, 1.0) in display values, under the HUD. Fields `m_2D8ADFF8` 200, `m_2068FD7A` 0.2, `m_EC1A7839` 1.2 unread (L). 1A75 stays a dim detail.
3. **First-person run.** The clips (3D8C, 3DE2, 3DE4) and ARM_SCALE 1.4 are right (H at two moments): 3D8C at 0.083 s puts the pistol at x .30...81, top -.18; live x .37...83, top about -.2. At 0.417 s the left hand's box reaches x -.99..-.49, top -.30; live fingertips about (-.64, -.37). **About half of each cycle shows neither hand, by the data** (0.167-0.333, 0.5-0.667): screenshots land in those gaps. The pistol is never hidden: 1DDA, 1DDC, 1DDD hold no Ctl or Model and `Weapon_R` keys are flat; `heroes/sombra.md`'s "put away" was wrong.
   **Why ours reads as a ghost (H code, M effect):** `submitStealth` at phase 1 has no base pass: only `entityTranslucentEmissive`, full bright, tinted 0xC8B4FF, at `SEE_THROUGH_OWN` 0.6. Live her pistol is near solid in its own colours under the wash, and her hand glassy but dark.
   **The look's data:** 420C #24 / #28 / #30 / #31 STU_7D71FDF3 set looks 48A9 (self), 48AC, 48F0 (weapon), 4900; 48AC is material 35C4 with `.127` material effects (7DEB, B52B-B52E, 7DEC, 7DED). Which one stealth uses wasn't read.
4. **Beacon: a name bug (H).** `OvercraftClient.java:196` passes `"sombra_beacon"`; `ProjectileModelRenderer:81` looks up `projectile_sombra_beacon`, which doesn't exist, so it draws the ender eye. Fix: `"=sombra_beacon"`, as `"=sigma_rock"`. Check `ana_dart`, `sleep_dart`, `biotic_grenade`, `sombra_cyberspace` the same way.
5. **The throw.** 3D95's right arm is two flat keys (a fixed low pose); pistol top about -.84 at 1.4, exactly where the player's bar begins, so frame 2 can't show it gone (M): not a bug until a frame without the bar is seen. Live hand top about -.13 matches t about 0.15; ours at the same moment tops about +.9, t about 0-0.04, with the beacon already flying. Possible cause (L): 0C19 #38's `m_86701C3E` 0.5 and blend 12/30 = 0.4 s; ours blends 0.1 s from 0. `m_86701C3E` isn't decoded. Press to arrival on ours 0.25-0.38 s at 23 fps, Workshop 0.22-0.23 s: not shown wrong.
6. **CRITICAL silhouettes are general,** not hers and not tied to stealth (H): BZ 2026-02-10, all heroes "see allied health states through walls". Art (M): `.10F` 0x245298-0x245818: texture 39DD.004 (128 x 128), colour (.949, .094, .310), a 45 in the transform (the diamond), `2690B.07C` "Critical". Condition (L): 01CF #953, 012D.116, share between 0 and 0.35. Silhouette not found. Separate work from #87.

## Ana's scope
When it shows is H: 0B94 #278 shows 0613.116 from `op73(v169, 0.8)`. **The art isn't found.** 0613 / 05BE / 0A82 hold no texture; 276 `.10F` textures of 160 px or more and 128 symmetric thin UI textures were looked at; rifle 10B96's 30 textures; her effects. The wash: STU_1B93D61B over all `.01B` gives 41 graphs, none of Ana's.
Not excluded: **72 textures named by `.10F` not in the UI dump** (7A9, 840, 85C, 868, B74, B7C, BA0, 1344, 13CD, 2384, 23A0, 35FE, 37B3, 3F3E, 1DE4C, 1EF26, 2DFF5, 39E9A, 3A51B, 4209B, 47520, 56204, 56940 and others), pieces under 160 px, vector "Shapes".

Live layout, a fallback only once these are searched (frame 1, 2000 x 1121, crosshair (1001, 562), M; colours include the wash):

| Part | Place | Read |
|---|---|---|
| Crosshair | 4 ticks about 6 px, gap about 4 | green to (28, 230, 37) |
| Lens arc | apex (1000, 75), through (650, 310), (1350, 310), hooks to (680, 355), (1322, 355) | pale yellow, thin |
| Big circle | on the crosshair, r about 330 | low-alpha blue |
| Inner arcs | x 735, 1268, y 505-618, 6-8 px, tapered | to (255, 255, 250) |
| Struts | capsules (875, 565) to (965, 685), mirrored | (60, 79, 134) on dark |
| Small circles | (688, 820), (1315, 820), r 32 and 10, lines toward (880, 625), mirrored | (52, 58, 92) |
| Chevrons | x 1000, five bars y 725-825, widths 120 to 80, last solid | (60, 63, 96) |
| Corners | thick (497, 815), (497, 850), (635, 997), mirrored | (190, 195, 150) |
| Thin yellow | (595, 635) to (505, 790); (652, 997), (770, 997), (810, 957); ticks (910, 858) to (876, 916), mirrored | pale |
| Blue band | (465, 850) to (590, 1015), mirrored | (90, 102, 159) |
| Wash | whole view, no surround; white about (236, 236, 255), sky about (155, 170, 240) | frame only |

## Juno
Frames (2000 x about 1090, crosshair (1000, 543)), read by eye from the chat's copies: lock at "4s": glass spheres centred about (290, 310), (1705, 305), r about 190, bright star inside. At "3s": about (240, 315), (1755, 315), r about 195, plus a faint yellow rim r about 330. Timer: blue crescent about (430, 630) with "4s" at (482, 610); at 3 s about (440, 660). Prompts level with the crosshair: FIRE (700, 543) with its mouse icon (745, 543); CANCEL (1320, 543), icon (1253, 543); thin vertical ticks x 772, 1228. Blue curves in both lower corners, from about (35, 760) downward.
7. **Pods' glow: spawned, not seen (H from code and notes; not re-run).** `Fx::render` is in the world pass; the arms draw later with depth cleared, so the pod paints over the particle ([rendering](../rendering/rendering.md), Sojourn's; cure: `BrigitteFx.viewChain`, `submitCustomGeometry` with `FxRender.additive`). `k = 0.25` (`JunoFx` 459, 466, 475) also shrinks it to smaller than the mesh. `fxnear` can't settle this: it lists them either way. Data 5254.00D (1.67 s, held), 4695 #214 on 048D, #862 on 048E: burst 37C36 x 364C 0 to 1 m over F6 .769-1; star E221 x 627C (#FBFF8B to #8BE0FF) to .727 at .49; ring 724 x 0558 (peak 5) from .5 to .282; soft 568C; flare 37A79 x 75E0 (#0062FF to #FFF785); sparks 12F7D, ribbons 2FCA7. Bones in view (±.264, .083, .307), at 1.4 ndc (±.97, .54): half of anything centred there is off-screen, so place against the frames.
8. **Marks:** 4B99 #158 0727.116, world layer, `v40 == local player`; `646D = v10349 / v1723` progress, `5BC2` ally or enemy (M). Art not located. Build a HUD mark at hp 0001 with that progress; the plus and diamond may reuse Critical's pieces (L).
9. **Timer:** 0237 at 4695 #1225, #1283: a slot of her reticle 091C, not a bar under it (H). Whole seconds and arc from the frames (M). `ReticleHud.juno`'s `timedBar` is wrong in shape and place. **Corner curves: not found** in 4695's presenters or 5254.
10. **Prompts:** 00DB, 4695 #1051: one widget, two slots (H); placement from the frame (`PROMPT_BELOW` 160 is wrong here). **Locked icon:** 47B7 #500, #860 and Sombra's 0C19 #597 bind `0786`, `630E`: a general "blocked" flag on each ability box (M). Frame 2: red (245, 55, 87), slashed, dark red box, the active one orange. **Floating 85s:** `2A8BD` "Floating Combat Text", a Settings option (M): the mod has no such system. A new feature: ask the owner before building.

## The fixes (branch `fix/side-by-side`, 2026-11-05)
**Harness input, not a person: this isn't the merge gate.** `KeyMapping.set` through DevServer, aim by `look` and `/tp`, bots as targets, 1920 x 1080, noon and midnight, worlds `sbsf1`. Shots `sbsf_*` in the worktree's `run/screenshots` (not in git). The first capture is build cb8ab68. **The last three commits (held pieces uncull, pods' axes, scope wash) came after the client was handed over and are compiled only: not looked at in game.** Reproductions: `sbs_*`, `sbs2_*` from the earlier capture, on ac69e1a.

| Item | Expected | Seen on cb8ab68 | Still different or not tried |
|---|---|---|---|
| Beacon (#87) | her model, not an ender eye | `day_throw3`: dark rounded body in flight. Repro `sbs2_throw*` | `day_throw4`, `5`: our 1B0F trail, a white band, nearly hides it; frame 2 shows none. Its size: 0.166 m at about 60 px means about 2.1 m away in frame 2; not compared at a known distance. Tumble -720 a second is D, its role M, its axis ours. Upright is from one frame |
| Other thrown (sweep) | no Minecraft items | `thrown_dart*`, `thrown_sleep*`, `thrown_grenade*`, `thrown_cyber*`: no item seen | **The models weren't caught in flight in any of these** (only their effects at the target). So: Ana's and Cyberspace's fall-back removed by the code; the models themselves not visually verified |
| Tint #851 (#87) | whole screen, about (.945, .843, 1), under the HUD | white concrete 165, 170, 171 (`day_rest`) to 157, 143, 170 (`day_stand`): (.951, .841, .994). HUD not tinted | Fade timing ours. Fields `m_2D8ADFF8`, `m_2068FD7A`, `m_EC1A7839` unread. F1 hides the wash with the HUD |
| Bar, label (#87) | as frames 3, 4 | `day_cycle12`, `night_stand`: bar, INVISIBLE, small crosshair | Side by side by eye only |
| Her own look (#87) | her textures near solid, tinted | Hand: `day_cycle*`. **Pistol: not drawn in any of 16 cycle shots, 0.048 s of clip apart at tick rate 4.** Cause read and fixed after: culling on held pieces (`submitStealth`'s `cull`). Weapon_R's keys are flat at scale 1 (read from the glb); `sbsfix_clip2.py` puts its grip at 0.083 s where the hand is | **Fix not looked at.** The mid-fade (`fade04`, `night_stand`) is pale cyan shards at night: the data's edge row, or the same culling: look again. 0.8 is from the frames. Enemies past 4 m, an ally's and an enemy's view: not played (one client); that path is unchanged but for the held pieces' culling |
| Throw (#87) | 0C19 #38 decoded | **Open.** m_86701C3E / m_2B98273A read as speed (M): 0043 #117's 0.76 and 0.76 / (v21541 + v42056) is the melee's rate mods; 420C #868's 1 / 0.525. A second reading isn't ruled out. 1.25 x and a 0.05 s swing (ours) applied | `day_throw3` to `5`: the hand still high when the beacon is out; frame 2's is lower at about 2 m (M). Volley and clip both start at #160's begin (edges), so the data shows no delay. Needs the owner's clip frame-stepped |
| Pods (#86) | glow over the pods, at the frames' places | **Not drawn at all** in `day_juno_lock*`: the pass's pose ends in world axes, not the view's, and a z test skipped her facing south. Fixed after | **Not looked at.** All sizes from the note's positions; brightness ours |
| Marks (#86) | HUD, chevrons, plus, diamond | `day_juno_lock3`: blue plus on allies, red diamond on enemies, closing chevrons; world ring gone | Art ours, frames only via the note. Top chevron can sit behind a health bar's arrow |
| Timer, prompts, corners, box (#86) | as the note | `day_juno_lock0`-`4`: arc shrinking, "3s", FIRE and CANCEL level with the crosshair, ticks, corners, glide in red. Torpedo count in its panel | Arc length, colour and corner radius inferred from two positions. Which is data: the widgets (0237, 00DB) and flags (0786, 630E). Which is frame: every place and shape. Not checked against the chat's frames (not available as files). Blocked state: Juno's glide, Sombra's Cyberspace in the throw; **a hack's silence isn't included**. `hud_kiriko_cd`, `hud_brigitte_cd` and three other heroes' files: boxes as before (those two read; lifeweaver, mercy, sojourn taken, not read) |
| Scope (#90) | no surround, wash, line art | `day_ana_scoped`, `night_ana_scoped`; a scoped hit 20 -> 14, one shot. Repro `sbs_ana_scoped` | Wash stronger since, not looked at. Zoom read not recorded (`state` has no `fov`): the code isn't touched. Line art thinner than the frame's, by eye; chevrons and struts low contrast by day |

Second look needed: `night_stand` first read as a bug; `fade00`-`23` show it's in the fade.

## Ana's scope: what was searched
- **Linking `.116` to `.10F`:** every 4- and 8-byte value of 0613, 05BE, 0A82 searched in `.10F`: only generic STU table words hit (`0400000024000000` and such), the type hash `EA2BDC35` and the bindings none. No link found. Not tried: decoding `.10F`'s tree itself.
- **`.10F`'s textures:** the 8-byte scan finds 1,334 ids. 1,262 in the UI dump: all seen on contact sheets (308 under 160 px, 954 larger, thumbnails at 64 px, so a very thin line could be missed). No lens arc, struts, small circles or chevron stack. Thin arcs 181C, 31E5, 343C, 371AC, 37EAC are generic, not opened at size.
- **The "72 undumped":** 50 decoded and seen: masks, terrain, noise. **22 aren't files in the build as `.004`** (840, 85C, 868, BA0, 23AC, 2430, 2464, 24F0, 250C, 2518, 2524, 2530, 25BC, 2DB4, 315C, 3168, 3490, 349C, 35FE, 3F3E, 2DFF5, 7B2D0: FileNotFound): probably false hits of the scan, not proven.
- **Not excluded:** vector "Shapes" in `.10F`, which would hold exactly this kind of line art (L). So "not in the data" is too strong: not found.

## The look's data, read as far as it goes
420C #29 (STU_9D240539{self}: true -> #28, false -> #24): 48AC on `STU_9521A27E{}` (M: her first-person self), one material, 35C4; 48A9 on self, eight materials; #30 48F0 on her weapon v49, four materials; #31 4900 on its 1FFA child. Each look has about 50 `m_materialEffect` ids, 0x26000000000xxxx, each to a `.127` of 144 bytes: a shader group (2C, D9, 32, 36, 22 `.088`), a slot (`.03A`), a hash, zeros. **No colour, no opacity, no texture.** Nothing read names which id stealth uses. So 0.8 is the frames'.

## Not built: what it would take
- **CRITICAL markers:** server: every hero's allies below 35 % (L) as a synced list, through walls. Client: a HUD mark from 39DD.004 (not in the pack yet), the "Critical" string, and a silhouette: an outline pass through walls, which the mod has only as Minecraft's glow. About the size of the Conspirator marker plus a render pass. It's every hero's, so check the rule against the patch notes first.
- **Floating 85s:** `2A8BD` is a Settings option. A per-hit client message and a HUD text list: small, and a new feature the owner should ask for.

## The cost of the first reading
Twice this pass a note's claim hid the bug: "the clips put the pistol away" hid a culled pistol, and "bones in view" hid world axes. Read the raw before believing either.
