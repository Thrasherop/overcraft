# Kiriko vs real Overwatch: picky visual critique, round 3 (2026-10-26)

This third pass covers only the newest captures in `run/screenshots/`:
- HUD: `hud5_*`.
- Select screen: `sel4`.
- Swift Step, low health and ally markers: `r2_*`.
- Kill feed: `kf2_*`, `kf3_*`.
- Kitsune Rush: `foxh_*`, `foxs_*`, `rv2_*`.
- Cleanse glow: `cg_*`.
- Ofuda in flight: `pr_*`.
- Idle and slash arcs: `br_after`, `arc_*`.

Each sheet was checked against the full-size frames behind it. The Overwatch frames are the same as in the earlier
rounds: `ref1080` (1920x1080; it is the owner's mid-Swift-Step frame), `ref_owner_02` and `ref_queued_00..03`. They
stay in the session scratchpad. The other sources are `kiriko-reference.md` and `kiriko-effects.md`. This report is
text only (repo rule).

**Method:** crops at the same 1080p coordinates, Pillow pixel samples, bounding boxes and line profiles. The
reference frames are blurred, so their edges run 1-2 px soft.

**Confidence:**
- **H:** measured, or clearly seen in both.
- **M:** seen once, or partly inferred.
- **L:** general knowledge, or seen poorly.

**References to earlier rounds:** "R1 #n" and "R2 #n" are items from the earlier rounds. Items those rounds marked
done or not ours are repeated only where these captures show them still wrong.

**Not counted, per the brief:**
- The flat test world, the villagers and Steve standing in as allies, and Minecraft's sky, blocks and rain.
- The red command-feedback text, the white death poofs and the tutorial and advancement toasts.
- Ofuda on the right mouse button with kunai on the left, and the kit row under the name on the select screen.
- Rank cosmetics.
- Sombra under SUPPORT: that is the wiki's 2026 role, per `Hero.java`.

## Status (kept up to date)
- **Done, 2026-10-26 (CHANGES.md, "The critique's third round"):** 1, 2 (the smoke; the hands still show the seal
  through the step), 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15.
- **Open:** 11 (where Swift Step lands: needs a video), the Suzu in flight (R1 #37: it is the game's own model).
- **Checked, left as is:** 2's "no hands mid-step": the owner's own mid-step screenshot shows the hand seal. R2 #19
  (cloth): seen running past a second player from the side, the coat splits at her legs and trails behind (`cs2z`);
  "rigid" was read from standing shots. R2 #14: the dome is there (8 m, 0.3 s); from inside it reads as a wash.

## Confirmed fixed (seen in these captures)
- **Weapon panels (R2 #11):** the lines are at the same height as Overwatch's.

  | | Ours | Overwatch |
  |---|---|---|
  | Ofuda panel, top / bottom line | y 924-926 / 985-987 | y 925 / 986 |
  | Kunai panel, top / bottom line | y 939-941 / 1001-1003 | y 940 / 1002 |

  The mouse icons are at (1565, 1010) and (1823, 1026), against Overwatch's (1562, 1009) and (1822, 1026).
- **Ammo text (R2 #15):** ofuda `10|10` spans x 1514-1576 and is 19 px tall, against Overwatch's x 1516-1578 and
  19 px.
- **Ult meter at rest (R2 #10):** it is see-through, with only the tick band darkened (`hud5_rest`/`hud5_spent` vs
  `ref1080`/`ref_queued_02`).
- **Ult ready (R2 #26):** it has a wide halo (#2C40C4 → #2D2A69 → background over about 12 px), the triangles and an
  inverted Q.
- **Ult after the cast (R1 #13):** it reads `0` with no ticks during the rush.
- **Volley bar (R2 #3, partly):**
  - The segments are fat again.
  - Its position is x 889-1030, y 664-687, against Overwatch's x 890-1032, y 664-687.
  - Spent segments are grey outlines over a dark fill.
  - The shape is still off (item 7).
- **Chat (R2 #9):** it is small, at the left, with its bottom near y 700 (what's left is item 12).
- **Ally markers (R2 #8):** pips and an open "v", with no vanilla name tag (`pr_3`, `foxs_*`).
- **Kill feed (R2 #27):** it exists. Rows are 34 px tall at y 36-69 with their right edge at x 1883, against
  Overwatch's y 35-70 and x about 1890. Its look is item 6.
- **Swift Step flash (R2 #5, partly):** it is edge-only with a clear centre (item 2 covers what's left).
- **Idle (R2 #7):** no leg kick in any of the 80 frames of `br_after`.
- **Select screen (R2 #13):** no hard cut at the roster; the model runs on behind the roster under the dark band.
- **Fox (R2 #1):** it is present and readable. Its material is item 1.
- **Slash arcs:** present (`arc_2`, `arc_3`).
- **Cleanse model (R2 #18):** the model stays opaque. `cg_b` is a white wash over an opaque model, and no sky shows
  through.
- **Not counted:** `arc_1`'s roughly 3 m arrival ring predates the 1.4 m fix (its chat is still the old large size).

---

## Ranked list (most noticeable first)

### 1. Kitsune Rush fox: opaque, lit like a mob, a blue plush toy (H)
- **Where:** `foxs_a`/`foxs_b` (first person; the fox summoned standing), `foxh_1` (its head at the camera).
- **Overwatch:**
  - A translucent, glowing ice-blue/cyan crystalline spirit (look 13201).
  - The effects recipe says additive or translucent cyan, and the ground shows through it.
- **Ours:**
  - A solid model, shaded #184764 to #4F87AA, with fur tips about #8ACFF5. No grass shows through it, and it has no
    rim glow or bloom.
  - The body is low with the head at grass level and the hind legs straight back (pose: L).
- **Fix:**
  - Draw the fox's texture at about 50% alpha, plus an additive fresnel rim (#9FF4FF), with no depth write on the
    fur cards.
  - Optionally a slow scrolling noise.
  - Keep the cyan wake.

### 2. Swift Step in first person: a crisp neon frame, and the hands stay up (M-H)
- **Where:** `r2_step_a`, `r2_step_b`, `hud5_cast`, against `ref1080`.
- **Overwatch:**
  - **Mid-step there are no hands on screen.**
  - The edges hold a deep, dark, mottled teal smoke: #066664 to #0D4D4D in the corners, #13302E down the right edge.
    It reaches about 400-600 px into the corners and about 300 px along the sides, and the scene behind looks dimmed
    and smeared.
  - During the 0.2 s cast, cyan wisps flare beside the clasped hands, which glow white-cyan.
- **Ours:**
  - A bright, clean rounded-rectangle rim: #5EC2DE at the edge, gone within about 120-160 px. Three or four long
    white-cyan arcs sweep up from the bottom right.
  - The seal hands stay up through the flash and after the arrival (the villager is already in front).
  - Neither cast frame shows any wisps or hand glow.
- **Fix:**
  - Hide the view model from the flash until about 0.1 s after the arrival, then return to idle.
  - Replace the rim with a noise-masked smoke, alpha-blended toward about #0A6A66: about 65% in the corners, falling
    to 0 at about 35% of the half-width.
  - Keep a few thin streaks.
  - Add two wisp sprites and a hand glow during the cast.

### 3. Low health: a thin pink rim instead of dark crimson corners (M-H)
- **Where:** `r2_low` (50 HP), against `ref_queued_02` (27 HP).
- **Overwatch:**
  - Deep, smoky crimson: #97141C and #731723, with blotches of #AB1A2A.
  - It fills the top-left corner about 400 px wide by 250 px tall, and the top band is about 150-200 px across the
    width.
  - It darkens the scene.
- **Ours:**
  - A band of about 60-100 px.
  - Over the sky it turns lavender, #9581AC at (200, 60), and the scene inside stays bright.
- **Fix:**
  - Darken first, to about 50% in the corners, then add red #8C1420 at about 70% over the same mask.
  - Use a corner falloff radius of about 450 px and mottle it with noise.
  - Keep the top band at about 150 px.

### 4. Locked-on ofuda: pale sage, not gold (H, measured)
- **Where:** `pr_4` (`pr_sheet` tile 4).
- **Overwatch:** gold, #DCB900 / #A18700 to white-gold (Healing Ofuda 7E2D).
- **Ours:**
  - The hottest pixels are #C4CA9D, and the trails #B0B684 and #A0A979: low saturation, with G above R, so it reads
    khaki or olive over grass.
  - Two round glows sit on the villager's body like headlights.
- **Fix:**
  - Alpha-blend a #E8BE18 core at about 80%, with no additive white and no light or fog multiply.
  - Keep the trails thin and spiralling.
  - R1 #1's colour never reached gold.

### 5. Kitsune Rush cast: a 3 m glass bubble around her (H that it reads as one; M on the data's size)
- **Where:** `foxh_1`, front third person: a circle of about 500 px with a crisp rim round her, and the smoke inside.
- **Overwatch:** cast 3C5F has two parts:
  - teal smoke (377D5, 0.5-2 m);
  - a 30/s stream of rings 0F5F at size 1.5. Swift Step's note gives 0F5F size 7 = 1.4 m, so these are about 0.3 m.
- **Ours:** one camera-facing ring growing from 0.3 to 3 m (`KirikoFx.rushCast`, `size(0.3f, 1.5f * 2)`). It is the
  same shield-bubble look that R2 #16 removed from Swift Step.
- **Fix:** small rings of about 0.3 m streamed for about 0.1 s, or flat ones (normal along the cast); drop the
  camera-facing 3 m ring.

### 6. Kill feed: wrong colours, portrait order and chevron (H)
- **Where:** `kf2_k`, `kf3_k`, against `ref_queued_00`, `ref_queued_03` and `ref1080`.
- **Colours:**

  | Box | Ours | Overwatch |
  |---|---|---|
  | Ally | royal blue #437BDB / #3770D0 | teal-blue #277CA6 / #29759D (reference doc #13769E) |
  | Enemy | bright red #C43D41 | crimson #7C2335-#8B293C (reference doc #9C283E) |

- **Layout:**
  - Overwatch, every row: `NAME [portrait]  glyph >  [portrait] NAME`. The names are on the outside and the
    portraits on the inside.
  - Ours: `[portrait] NAME  glyph  NAME`, with the portrait at the far left.
- **Middle:**
  - Overwatch always ends the middle with a thin white ">".
  - Ours shows a filled "▶" when there is no glyph (the pig kill) and no chevron after the kunai glyph.
- **Accents:**
  - Overwatch's team boxes have a bright inner end edge (#26A5CD on the ally box) (M).
  - `kf2_k`'s rows have a 2 px #F9F9F9 outline and `kf3_k`'s don't. If that is the "you were in it" highlight, check
    it against a video (L).
- **Fix:**
  - Use Overwatch's colours.
  - Move the killer's portrait between the name and the middle.
  - Always draw the glyph, then ">": for an unarmed kill, the melee glyph.
  - Add a 3 px light inner edge to each team box.

### 7. Volley bar: flat-topped slashes, not rotated tiles (H)
- **Where:** HUD centre, always on screen (`hud5_mid_pair`, against `ref1080` and `ref_queued_00` at 4x).
- **Overwatch:**
  - Each segment is a rounded rectangle about 30 x 15 px, rotated so its long edges rise about 35-40° to the right.
    Neither its top nor its bottom edge is horizontal.
  - The segments overlap in x: `ref1080`'s amber runs unbroken from x 890 to 1032, and the row reads as a zig-zag
    chain of tiles.
- **Ours:**
  - Parallelograms with horizontal tops and bottoms (y 664 and 687) and sides leaning about 60°.
  - There are clear 3 px column gaps (889-914, 918-943 ...), so the row reads as "/////" hazard stripes.
- **Fix:** a 30 x 15 rect with 4 px corners, rotated -38°, keeping the 29 px pitch and the centre line. R2 #3 fixed the
  thickness, not the orientation.

### 8. Cooldown boxes: the icon is pure black and merges with the ink splash (M-H)
- **Where:** `hud5_cooldown1`/`hud5_cooldown2` and `hud5_br_sheet` tiles 4-8.
- **Overwatch:**
  - The icon is a faint ghost: in `ref_queued_00`'s "6", #88858C on a #BEB4BD box, about 30% darker.
  - In `ref1080`'s "12" no icon shows at all. The only black is the splash behind the digits.
- **Ours:** the icon is #000000 / #050605 on a #4E564F box. With the splash it makes one large black blob, and the
  digit is harder to read.
- **Fix:** draw the icon about 25-30% darker than the box (about #363C37 here, a dark brown on orange), never black.

### 9. Ofuda with no target: glowing cyan capsules, no paper (M)
- **Where:** `pr_1`, `pr_2` (zooms `pr_2z`).
- **Overwatch:** a white paper talisman with the teal glow hugging it.
- **Ours:**
  - The cores clip to cyan (#8FFFFF-#C8FFFF), about 18 x 14 px at about 11 m, inside a mint halo of about 1 m
    (#56F0C0).
  - No white or seal reads, and the shape is a cone or bullet.
- **Fix:** draw the paper unlit and white (#F4F1EA) with the red seal over the glow, not tinted by it. Keep the glow
  behind it at 50% alpha or less.

### 10. Suzu cleanse: a flat white cut-out (M)
- **Where:** `cg_a` (third person), `cg_1pz` (first-person arm), `cg_b` (the fade).
- **Overwatch:** bright white, but with form: a fresnel rim and a noise or tile-grid shimmer (03C3, effect 3398),
  plus a few yellow ribbons.
- **Ours:**
  - The silhouette is clipped to #FFFFFF everywhere. No shading, edges or shimmer show, so it reads as a hole in the
    picture.
  - It then fades as a flat 40% white wash.
- **Fix:** the model at about 0.6 brightness, plus an additive white fresnel (1.0 at the rim, about 0.45 facing the
  camera), plus scrolling noise; keep it below clipping. The same, lighter, on the first-person arms.

### 11. Swift Step arrival: the ally fills the screen (L-M)
- **Where:** `hud5_cooldown1`, `r2_step_b`, `r2_low` and `arc_own`.
- **Ours:** she lands about 0.86 m in front of the villager's face. Its head is 446 px wide (x 737-1183), and its body
  stays in the middle of the screen afterwards.
- **Overwatch:** our references have no arrival frame, and the data only says "computed position".
- **Fix:** check a video. If Overwatch lands beside the ally, offset about 1 m sideways (perpendicular to the
  approach) instead of on the near side.

### 12. Chat: still a vanilla panel (L-M)
- **Where:** `hud5_rest`, `hud5_wall`.
- **Overwatch** (`ref_queued_00`): about 16 px blue team text at x 68-430, y 652, with no panel.
- **Ours:**
  - A 664 px dark panel even for one short line ("No blocks were filled"), in Minecraft's pixel font.
  - Its right end covers the left glove (x 630-664).
- **Fix:** in hero mode, text with a shadow only (or a panel fitted to each line) in the HUD font; show the full
  panel only while typing.

### 13. Health number is a little big (L)
- **Ours:** "250" is 24 px tall.
- **Overwatch:** "199" is 21 px in `ref1080` (blurred; about 22 px true).
- **Fix:** about 8-10% smaller.

### 14. Weapon panel lines: core too thin, halo too short (L)
- **Ours:** a 3 px #2D96DA core and about 2 px of falloff (#2B6687, #285F7D).
- **Overwatch:** a 5 px core (#1E68A9 to #2475B8) with a 4-5 px dark-blue halo (#103C6A, #0B386A).
- **Fix:** a 4-5 px core, with the halo widened to 5 px in a darker blue.

### 15. Kunai ammo text sits 3-4 px left (L)
- **Ours:** x 1772-1832, y 965-985.
- **Overwatch:** x 1775-1836, y 965-983.
- **Fix:** end the text at x 1836, about 1-2 px shorter.

---

## Earlier items seen again, still open (not re-ranked)
- **R2 #17:** the path tiles are plain cyan cards, and the near gate's posts are tubes with open elliptical ends
  (`hud5_rushing` x 1480-1840; `rv2_a`, `rv2_b`).
- **R2 #19:** the cloth is still a rigid A-line panel down to the shoes (`br_after`, `sel4`).
- **R2 #24:** the white reload line on the ofuda panel (`hud5_spent`, x 1430-1495).
- **R1 #37:** the Suzu in flight reads as a brown pouch (`cg_c`).
- **R2 #14:** no Suzu dome yet. No capture shows the burst itself.

## Not checked
- **Wall climb:** `hud5_wall` shows the idle weapons in front of a wall; it isn't clear she is climbing.
- **Reload:** `hud5_reload` shows `10|10`, so no reload is visible.
- **Rush, in clear weather:** her own in-path view (`hud5_rushing` is in rain).
- **Kunai:** in flight and on a hit.
- **Quick melee.**
- **Enemy-view colours.**
- **Swift Step's flash in third person.**
- **The ult's tick density:** `ref1080` is too blurred to count against ours (about 80).
