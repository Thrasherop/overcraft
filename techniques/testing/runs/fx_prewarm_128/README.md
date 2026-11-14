# #128: each hero's effect art made ahead, before and after (2026-11-13)

Branch `fix/fx-prewarm` from `next` 04147b9 (0.3.46). Up: [the map](../../../README.md). The lesson is in
[rendering.md, "First use of an effect"](../../../rendering/rendering.md#first-use-of-an-effect-2026-11-06-sojourn).

**How.** One fresh `gradlew runClient` per hero (the five heroes with no effect art shared one), `OC_ASSET_PACK` the main
checkout's pack, a fresh flat survival world each, hero picked with the harness's `hero` (not the select screen), a `NoAI`
zombie at 9 m and a villager at 3.5 m. Each button pressed once by harness keys (`key <action> down/up`; the ult after
`ult 100`, then a primary-fire tap for one that waits for a confirm), 2 s of `presstrace frames on` around each. Script
[fx128.py](fx128.py), launcher [fx128_run.ps1](fx128_run.ps1). **Before** is the same build with `OC_FX_AHEAD=0`
(nothing made ahead, Sojourn's #126 list off too); **after** is with it on. Overwatch and Battle.net checked closed before
each launch. 30 fps (the hidden-console launch caps the window there), so a normal frame is 33-36 ms.

The client's own log line (`EffectArt.frame`, new here) gives each frame that made art on first use: how many, the ms
spent making them, the whole frame. All of them: [first_use_log_lines.txt](first_use_log_lines.txt). Raw tables:
[fx128_before.txt](fx128_before.txt), [after](fx128_after.txt), [after2](fx128_after2.txt), [after3](fx128_after3.txt)
(after2/after3 rerun heroes whose lists were then filled in; the table uses each hero's last run).

**Longest frame of each button's first use, ms (before -> after).** `-` = no frame over 50 ms either way.

| Hero | Primary | Secondary | Ability 1 | Ability 2 | Ultimate | Art made on first use, after |
|---|---|---|---|---|---|---|
| Kiriko | **543 -> 47** | 107 -> 57 | 69 -> 63 | **348 -> 36** | 170 -> 44 | none |
| Lúcio | 147 -> 66 | 78 -> 37 | 92 -> 36 | 59 -> 36 | 52 -> 36 | none |
| Jetpack Cat | - | 134 -> 53 | 175 -> 116 | 95 -> 36 | 155 -> 36 | none |
| Mercy | **280 -> 109** | 86 -> 36 | - | - | - | none |
| Lifeweaver | 149 -> 74 | 92 -> 53 | 68 -> 41 | 58 -> 38 | - | none |
| Ana | - | **277 -> 107** | 65 -> 36 | 118 -> 36 | 62 -> 36 | none |
| Sombra | - | 204 -> 72 | 232 -> 53 | 142 -> 127 | **286 -> 35** | none |
| Brigitte | 249 -> 109 | 78 -> 37 | **338 -> 81** | - | 98 -> 36 | none |
| Juno | 116 -> 50 | 100 -> 37 | - | 157 -> 52 | 77 -> 40 | none |
| Sojourn | 70 -> 48 | 200 -> 70 | - | 130 -> 66 | 193 -> 36 | none |
| Sigma | 71 -> 49 | 89 -> 52 | - | 178 -> 40 | 175 -> 39 | none |
| D.Va, Pharah, Tracer, Cassidy, Junkrat | no effect art: nothing made on first use, before or after | | | | | |

Kiriko's Ofuda frame before: 54 masks made in 445 ms of a 542 ms frame (the issue measured 305). After, none.

**The pick.** Jetpack Cat's jetpack flame (25 + 9 masks: 217-256 ms of her pick's 473-493 and 360-382 ms frames), Lúcio's
aura (2 masks, 54-82 ms of a 466-673 ms frame) and Sigma's hand cores (1, 40 ms) are drawn the moment the hero is picked,
before any wait after it: they are now made as the world loads (`HeroFxArt.ON_PICK`); the after-runs show none at the
pick. The pick's own frames without them weren't measured separately.

**What made ahead costs.** Texture memory per hero (the harness's `artBytes`, mips included): Kiriko 25 MB, Lúcio 11,
Jetpack Cat 33, Mercy 9, Lifeweaver 8, Ana 9, Sombra 32, Brigitte 27, Juno 26, Sojourn 61 (#126's list), Sigma 30; each
includes the pick list's few MB. All made within the 8 s after the pick ("art left 0").
Turning them into textures went over 10 ms in a frame twice in all runs (Sigma: 16 and 11 ms).

**Not settled / not tried.**
- The 8 s after a pick had longer frames after than before in some runs (Ana 111 and 114 ms against 60; Lúcio 104 once,
  46 the second time; Jetpack Cat 82-100 against 70; others alike). Not from textures (logged under 10 ms but twice). The
  colouring worker's allocations or CPU are the likely cause; not separated. One run each, at 30 fps.
- Some first uses are still over 100 ms with nothing made (Mercy's beam 109, Ana's scope 81-107, Brigitte's flail 109,
  Jetpack Cat's Lifeline 116-127, Sombra's Translocator 127): something else on first use (sounds, models, shaders), not
  art. Not looked into.
- Harness keys only, one target layout: effects these buttons didn't reach (Mercy's Valkyrie and Resurrect, hits on
  allies, a homing ofuda) come from the code-read half of each list (`fx128_scan.py`), not from a measured miss.
- Another player's hero: its list is made when they're seen, and the enemy colours only where an `_ENEMY` palette is named
  literally (Kiriko's derived from hers). Not played with a second client.
- Not a person's mouse and keys; no select-screen click; not at 60 fps or higher.
