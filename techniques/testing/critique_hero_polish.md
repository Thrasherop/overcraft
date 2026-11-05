# Critique: fix/hero-polish (#86 Juno, #87 Sombra, #90 Ana)

Up: [techniques map](../README.md). Records under review: [juno](../heroes/juno.md), [sombra](../heroes/sombra.md), [ana](../heroes/ana.md).
Same kind of pass as [critique_88](critique_88.md) and [critique_sigma](critique_sigma.md).

Independent critic, 2026-11-04. Commits 4cba105, af3ba94, 8e77760, 6cec06c.

**What this critique rests on.** Read-only: no game, no build, so every bug below comes from reading code and screenshots.
None has been reproduced in game. Reproduce each one before fixing it (repo rule). Overwatch and Battle.net were checked as not
running before the archive was read. All 166 `hp*` shots were opened on contact sheets (about 520 px each), and the key ones
cropped at full size. Anything said about Overwatch's look is **from memory, with no frame**, and is marked as such. Nobody on this
branch, me included, has compared it with a real Overwatch frame.

## Verdict

| Issue | Merge to `next`? | Why |
|---|---|---|
| #86 Juno | **No** | H1 (the lock's pose sticks after death, G, sleep or hack) and H2 (`Fixes #86` with half the report unreproduced). |
| #87 Sombra | **No** | H3 (the data's stealth in and out clips aren't played, and that's what the owner reported) and H4 ("no art" is wrong for the fades, a real-data rule breach). M1 and M2 should go with them. |
| #90 Ana | **Yes, with conditions** | The code is sound and I found no bug that blocks it. Play the gaps in M6 first. Say `Refs #90`, or close only after a side-by-side: lens and reticle are from memory. |

## High

### H1. Juno's first-person lock pose never ends if the lock is cut without a fire or a cancel
- **Code:** `ActionTrack` loops `torpedo_lock` with `holdLast` until another action starts (`ActionTrack.java:102-105`, `:195-199`). The only
  actions that end it are `torpedo_fire`, `torpedo_cancel` and a clip that calls `start()`. But `Juno.tick` with `!controls || !isAlive`
  calls `Torpedoes.clear` (`Juno.java:64-67`), and `Torpedoes.clear` (`Torpedoes.java:286-295`) and `Juno.clear` send **no action**.
- **Why it sticks:** `HeroAnimator.BY_ENTITY` is keyed by entity id, and a respawned player keeps their id, so the track outlives death
  unless the animator ages out (10 s unseen). Her Mediblaster's `fire` doesn't end it either: it's `layerOnly`, and `ActionTrack` has an
  empty case for it (`HeroAnimator.java:328`, `ActionTrack.java:86`).
- **What a player would see:** dying, being slept, hacked or stunned, or pressing G during a lock leaves the gun gone and the pods up in
  the top corners, while she shoots from nothing, until a reload, melee, ring, ult or the next lock. W lists stun, knockdown and hack as
  interrupting the lock (`tools/reference/juno.md`), so this is ordinary play.
- **Third person:** `junoLocking` has the same blind spot, bounded: the raised pods and the pods' glow (`JunoFx.java:439`) last up to
  4.1 s, on a corpse or into Minecraft mode.
- **fix/systems doesn't help:** its `Juno.leavePlay` also calls `clear` silently.
- **To reproduce:** lock, `/kill`, respawn, look; lock, G, G; lock, take Ana's Sleep Dart, wake.
- **Likely fix:** send `torpedo_cancel` when `Torpedoes.clear` ends a lock that was open.
- **Not in the record:** juno.md's "Not tried" doesn't list any of these.

### H2. `Fixes #86` closes an issue whose second half wasn't reproduced
- The owner reported two things. The agent says the icon wasn't reproduced, then writes `Fixes #86`, which closes the issue when it
  reaches `main`. CLAUDE.md: "If a bug truly can't be reproduced, say so in its issue ... instead of claiming it's fixed." The issue has
  no comments yet.
- **What should happen:**
  1. Use `Refs #86` in the commit (it's not on a remote yet, so it can be reworded).
  2. Comment on #86 with what was tried, and ask the owner one question: which icon, in which state, on which build?
  3. Close the torpedo half and leave the icon open, or split it into its own issue.
- **The candidates in juno.md need updating:**
  - The "owner's installed zip" is weaker than the note says. `overcraft/local-assets/current_overcraft.zip`, the 0.3.31 playtest
    package, was made 2026-11-02 18:36 -0700, about an hour before the issue was filed (19:39 -0700). I opened its inner
    `overcraft-assets.zip`: `glide_boost.png` is there (3950 bytes), the same as the rest of her icons. It only stays a candidate if he
    didn't replace his zip.
  - **Nothing was tried on a real install.** The agent played the dev client, which reads `local-assets/pack/`. The owner plays the jar
    plus the zip through `LocalAssetPack`, a different path. Try the 0.3.31 jar and zip before calling it not reproduced.
- The glide timer's icon is a fair guess at what he meant, but a guess: it's new UI, not a repro.

### H3. Sombra's stealth in and out clips are mapped and still not played
- The owner's words: "animations not active in translocate". The clip map has them:
  `Stealth (420C #868 0x486 blend 0.525, #869 0x487 priority 0xDD)`: 3P **3F49** / **3F5A**, 1P **1B253** (0.9 s) + onFinished **1B255**, 1P out
  **3EB9** (1.0 s) (`research\sombra\work\clipmap.md:52`).
- `research\sombra\work_kit\kit_translocator_stealth.md:51,56` places them: state 0 (fading out of sight) plays alias 0486, state 2
  (fading back) plays alias 0487.
- `stealth_486` and `stealth_487` appear **nowhere in `src`**. sombra.md files them as "Hidden first-person clips left L". The L in the
  research is "which enters", and the kit reading gives its answer. At the least, try it and look, as the real-data rule asks.
- As built, a shot snaps from the pumping hands back to the pistol, where the data has a 1 s clip for that.
- Also mapped and unused: the run's transitions 3E91 / 3E92, and the stealthed Hotfix clips B1CC, B1A1, B1CA (`clipmap.md:41,51`).

### H4. "The data holds no art for it" is wrong for the fades
Commit af3ba94 and `submitStealth`'s comment say so. It's true of the held state (16EA is empty, STU_33A46033 is a renderer state). It's
false of the transitions:
- 1AD9 C4 is a material effect on mat 2E8 with **A7BF, a 256x512 6-frame diagonal wipe**, and its ramp **8009F378** (`fx_digest.txt:3944-3955`); 1AD8 is
  the same wipe, twice (`:3992-4003`).
- The repo's own `tools/reference/sombra-effects.md:460-463, 483` calls it `sombra_dissolve`, gives the edge colours (#6D0000 -> #CE0000 -> #F6502B ->
  #00DFB6) and has a recipe: "her model wiped away along a diagonal".
- sombra.md mentions the wipe, then builds a cross-fade instead, and doesn't say why. `sombra_dissolve` isn't in the pack, so it needs
  extracting from the research folder first. If the renderer truly can't do it, write down what was tried.
- Result: mid-fade she washes out to pale pink-white and reads as **more** visible (`hp5_s1c`), with no red-to-cyan edge. The data's fade
  is the opposite of a glow-up.
- 1AD9 also has 500 sparkles at 0.77 s, the fade's end (ours: 60, in the existing `SombraFx.stealth`; I didn't trace when it fires): check the timing.

## Medium

### M1. The phased-out body reads as broken, not as a cloak
`hp5_s2c`, `hp5_s3c`: her torso almost gone, her hips and legs saturated purple. Per-piece flicker at 70-115% on a model with few large
pieces makes whole body parts change at once. At 12 steps a second this has never been judged in motion: stills are all the record has.
From memory, Overwatch's ally view is a uniform refractive silhouette with digital glints, not a tinted copy of her texture.
Try: one opacity for all pieces, with the shimmer coming from a mask (the wipe texture is a candidate). The note's "lower the glow
first" doesn't touch this.

### M2. Her first-person stealth run barely shows her hands, and they don't read as hands
- Over one cycle (`hp2_t1`-`t9`) a clear arm shows in about 2 of 9 frames, slivers in others, nothing in the rest. The note calls this "the
  sprint the owner means" without asking whether `ARM_SCALE` 1.4 is pushing it off screen. juno.md raises exactly this doubt for the pods;
  sombra.md doesn't.
- `hp5_n2`: a dark faceted smear with no readable fingers, under the HUD portrait. These are translucent types writing depth, with their
  inner surfaces showing.
- The final build's 30 % tint has **no daytime first-person shot**: `hp5_s*` are all third person, and the `hp2_r*` / `hp2_t*` frames show the old, stronger
  tint. The record's "played day and night, 1P and 3P" isn't shown for that case.
- The stride is on the clock (0.667 s) at 8.8 m/s, and the agent says so. `research\sombra\work\assets.md:94-95` notes the stealthed run's slot speeds are 1.75x the normal ones
  (third-person trees): check the 1P tree for the same.

### M3. Third-person pods snap on a cancel; first person fades
`additive` drops as soon as `junoLocking` goes false, so a cancel or an empty window moves the pods from +0.55 m to rest in one frame.
The commit's 1P path fades (`ActionTrack.cancel`); 3P has no equivalent. Also: a fire inside the first 0.43 s adds the start and the fire
together (each about +0.5 m), roughly doubling the offset for those frames. No shot of either. Look for an exit clip before writing a fade.

### M4. One cited shot contradicts "the glow grows on them"
`hp4_j3c` (2.1 s left): the burst sits on her **chest and neck**, and the pods hang above her with no glow. `hp4_j0c`-`j2c` and `hp4_b*`
show it on the pods. `hp7_f1`, the final build, has the glow engulfing her head as well as the pods. So it's either a fixed earlier
state the note doesn't mention, or a live bug when turning (`hp4_j3` is the one with the camera turned). Find out which, and look at it
from behind while turning fast.

### M5. Her own launch flash isn't scaled like the pods' glow
The lock's glow got `k = 0.25` in her own view because the pods are half a metre from her eyes; `launch()`'s flares (0.65 / 1.5 m) didn't.
`hp3_jf1` shows about a quarter of the screen covered as a torpedo leaves, right where she's aiming. Torpedoes go 0.08 s apart, and only
1-3 targets were tried. Lock six and watch at real speed: `fxslow 0.08` hides how it stacks.

### M6. Ana: conditions for merging
Play these (the first three are the note's own "not tried"):
1. Nano Boost's cast while scoped. 2. Ultrawide, and a 4:3 window. 3. A second player.
4. **A scoped shot through the lens.** No cited frame shows one. `ana_tracer` is a 0.2 m gold band from the server's muzzle point; with the rifle no longer drawn
   and a 30 degree view, look at where it starts.
5. Hurt while scoped: `screenEffects` draws before the 96 % surround, so edge-of-screen damage tints are covered. Decide if that's right.
6. An ally marker outside the lens, drawn over the black.
Dying while scoped is fine by reading: `heroView` requires `isAlive`.

### M7. The lock takes the whole screen
Noted by the agent, "not changed". From memory, Overwatch shows a limited lock area on the reticle. The cone is L in the data, and players
will notice: the torpedoes "took slimes at the screen's far left". Look for the real value; not blocking.

### M8. Improved Transparency: statically safe, never played
The crash rule passes: `RenderTypes.entityTranslucent` and `entityTranslucentEmissive` have OIT sets (`RenderTypes.java:292-307`, 26.3 source), and
`FxRender.additive` has `ADDITIVE_OIT`. The HUD lens isn't a world type. But how Sombra's arms and body look under F3+X wasn't
tried, and that path layers translucent and additive on the same surface. Play it once: 1P and 3P, and Juno's pods in 1P.

## Low
- **#90's fade-in is dead code.** The scope syncs per tick (0.2, 0.4, 0.6, 0.8, 1.0) and isn't interpolated, so `shown` is 0 at 0.8 and 1 at 1.0. It's a pop, and
  the 0.8 tick has neither crosshair nor lens. Interpolate, or draw the crosshair until `shown > 0`.
- **Reveal's end snaps** to fully phased (`ClientSombra.phase` jumps to 1 once she's invisible again). No fade.
- **A teleport while stealthed restarts the fade:** the data refills the timer without one (`kit_translocator_stealth.md:47-48`). The server's `Stealth.start` already did this; now she also visibly fades from solid. Rare with an 8.5 s
  effective cooldown against 7.25 s of stealth.
- **`ClientSombra`'s maps** are never cleared. A stale `LAST` can play a phantom fade when she's next seen, or on a reused id.
- **The stealth branch skips** emote props (`EmoteProps.HELD`), `SIDEARM`, the outline and the shadow, so mid-fade the shadow pops and an emote's prop vanishes.
- **4:3:** the lens stays at radius 470 of 1440 reference high, about 65 % of the height. **4K:** the 512 px mask is stretched to about 2090 px.
- **Other heroes with a potion:** anyone seen while invisible now takes `submitStealth`: lilac at full brightness, with no flicker since `time` is 0.
- **Nits:** `timedBar`'s `ability` parameter is unused; `HudShapes`' STREAK comment is now stranded above `SCOPE`; `Stealth.end`'s comment still says the fade-out is
  "shown at once".
- **Unexplained frames:** `hp_a3_1`-`4` (not cited) show the screen full of flat red, then black polygons, with rifle and HUD on top. Perhaps sky before chunks loaded after a teleport; the record doesn't say.

## Compared with Overwatch (from memory, not frames; confidence stated)

| Thing | Ours | Overwatch as I recall it | Confidence |
|---|---|---|---|
| Ana's lens | Radius 470 of 540, 70 px short of the top and bottom (`hp6_a0`) | Round lens with a dark surround: right idea. I recall it meeting or passing top and bottom | Medium |
| Surround | Flat near-black | Scope housing with some shading, not a flat fill | Low |
| Reticle | Ring, dot, lines left, right and down | Small centred reticle; exact shape not recalled. Skins vary it (E615) | Low |
| HUD scoped | Stays (health, ult, abilities, ammo) | Stays | Medium |
| Sombra, ally's view | Her texture, tinted, in pieces (M1) | Uniform translucent shape with digital shimmer | Medium |
| Sombra, own view | Hands rarely visible, faceted (M2); purple edge | Purple edges, translucent hands; a timer near the crosshair (#85) | Medium |
| Detection ring | None | Ring around her while stealthed, on by default: data 4DD9 (`tools/reference/sombra-effects.md:430`). EffectArt has only a palette entry. Not in sombra.md's "not built" list | High that the data has it |
| Juno's lock | Pods in the top corners, cut by the edge, no gun (`hp2_lock_b`) | Not recalled well enough to judge | None |
| Lock UI | World rings, a bar, no prompts | HUD brackets and prompts (agent admits, #85) | Medium |

**The gap this leaves:** the owner has asked for side-by-sides and none exist, and Claude can't run Overwatch. Ask the owner for a few
frames or a short clip of: Ana scoped; Sombra stealthed in her own view and as an ally sees her; Juno's lock in her own view.

## Searching the data for what the agent says doesn't exist

- **Ana's scope and reticle art: the claim mostly holds, more widely than the agent checked.** It looked at her GUI export and `research\ana\fx\Ana\Scope\textures\`. I
  scanned the full UI dump (`extracted\154088\UITextures`, 21,567 PNGs; 18,241 at 128 px or more) for clear-centre round masks, quarter and half lens masks (the filter passed a synthetic control), and thin
  ring reticles. No Ana lens came up. Closest: **007364 / 007376**, a red scope reticle that sits among Widowmaker's spray set, so a spray, and **0444D7**, a 2000 px double ring beside other logo-sized art, unidentified.
  **Limits:** thresholds can miss things, thin reticles at small sizes and atlases weren't covered, and game-texture folders outside the UI dump weren't scanned.
- **Sombra's stealth material:** wrong for the fades (H4), right for the held state.
- **Juno's prompts:** `tools/reference/juno.md:199` lists an option, "Hide Glide Boost cancel text", so at least one 00DB prompt is **Glide Boost's cancel text**. That gives its meaning even without string 1B3C0's text. juno.md mentions the prompt only among the lock's items. W also says secondary fire again cancels the lock; the code fires what's locked instead (`Torpedoes.java:65`). Check the option's default.

## Checked and fine
- **Lock-hold rule and other heroes:** `torpedo_lock_view` / `torpedo_cancel` only come from the `a.juno` block; the 3P hold is under `if (juno)`; `start()` resets `holdLast`. No other hero's clips change.
- **`ScopeFovMixin`:** other heroes, Minecraft mode, spyglass and sprint are unaffected (`viewScope` is 0 unless Ana is scoped in hero first person). One snap to 1 on leaving, then normal easing. The hero-mode spyglass was already overridden by `HeroFovMixin`, before this branch.
- **Zoom's arithmetic:** 2 atan(tan 15 x 16/9) = 50.9. The source is community measures, and the note says so.
- **`SOMBRA_STEALTH`:** `syncWith ... all()` covers late joiners by reading (not played). It isn't persistent, so nothing is stale on rejoin. Death and switch clear it through `Stealth.clear`; so does leaving.
- **Enemies past 4 m:** `submit` returns at `isInvisibleToPlayer` before the new branch, so she stays hidden.
- **Lens seams:** no break where texture meets fill at 1080 or 720 (pixels sampled at the joins in `hp6_a0`, `hp6_a6`). `w >= 1920` and `h >= 1080` always, so no negative strips. Other GUI scales weren't sampled.
- **`hp*` claims I checked against shots:** pods held through the window, gun back on a cancel, timers' seconds, lens at 720p, G while scoped, the night-fight damage numbers.
  The icon does show in `hp_juno_hud`, `hp_juno_glide`, `hp_juno_glidecd`, and it matches the data's icon `000000049433.png`.

## fix/systems
- `git merge-tree` of the two branches on 9f1c001: HeroManager and juno.md change in both and merge with **no conflict markers**. Nothing was built or run merged.
- **On hero-polish alone,** G while stealthed keeps her phased for up to 6.5 s (the #100 bug, now lilac). **Merged,** `Sombra.leavePlay` clears it. `SOMBRA_STEALTH` isn't in `HERO_LOOKS`, which is fine because `Stealth.clear` sets it to -1.
- **Not solved by merging:** H1. `Juno.leavePlay` adds another silent way in.
- Play the merged pair: G, death, switch and leave while stealthed, while scoped, while locked.

## Not tried, that a player hits
- **Juno:** everything in H1; a glide during a lock (W allows it); Orbital Ray cutting a lock (the agent's own list); F5 mid-lock (the particle keys change); two Junos near each other (`podLaunch` takes the first within 1.5 m); water, ladders, boats; the real-install path.
- **Sombra:** Hotfix while stealthed in 1P; an emote while stealthed; crouching and swimming while stealthed; a real mob's hit for the reveal (the record used `/damage`: "Applied 2.0 damage"); a real fight on the final build (Juno got one, Sombra didn't).
- **Ana:** M6's list; a real fight on the final build; emote or ability camera while scoped; F1 (zoom without lens).
- **All three:** a second player (one client, after the crash). The play ran through the harness socket (`hp_seq.py`) with dev commands such as `fxslow` and `damage`. The rule asks for real keys and real mobs, so say which steps used which.
