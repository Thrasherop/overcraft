# Critique of fix/reticle-ui (#85), 2026-11-04

Up: [techniques map](../README.md) · beside: [reticle](../hud/reticle.md), [qa_audit_2026-11-02](qa_audit_2026-11-02.md), [hand_play](hand_play.md)

Independent read of branch `fix/reticle-ui` (430fc9e, 24104e2, 031058b). No game, no build, no fix. Overwatch and Battle.net were checked closed before reading `E:\Overwatch Archive`.

**How to read the evidence.**
- "D" is a node I opened in the decompiled graphs.
- "Code" is what I read in the source.
- "Shot" is a screenshot I opened at full size.
- "(OW memory)" is my memory of Overwatch, with no frame behind it. Treat it as a question, not a fact.
- Nothing here was reproduced in game. CLAUDE.md requires that before each fix.

**Merge: NO.** The system is sound: the stack, the `Widgets` record and `HeroKit.widgets` should stay. The blockers below are small to fix, but the required play hasn't happened.

## Blockers

### B1. A Catnapper victim is told they can detach, and the carried enemy shows as an ally
- **Code:** `JetpackCatFx.towed` returns true for any entry in `TETHERS`. Catnapper's entries carry the flag `tether[2] = 1` ("catnapper_tether", "carry_state"), and `towed` doesn't read it.
- **Effect:** a player carried by an enemy Cat sees "GRAPPLED / [SPACE] DETACH". Only Lifeline reads jump (`JetpackCat.java:462`), so the prompt is false.
- **D:** 00CF exists only as 5A8F #1225, the Lifeline graph. Nothing in the table gives a carried victim this prompt.
- **Same cause on the Cat's own HUD:** `JetpackCat.widgets` returns `s.carried` when carrying, and `ReticleHud.jetpackCat` draws that enemy in the `ALLY` cyan with the Lifeline panel. D 0BA3 #1097 runs on `v1216 == 1` with `v59143`, the towed ally. So the carried-enemy panel has no data behind it, and its colour is wrong.
- **Timing:** the prompt also shows for the first 0.6 s of a tow, when jump does nothing (`s.lifelineTicks >= Ow.ticks(0.6)`).
- **Never played:** the prompt was only seen through `devtowed`, and no Catnapper carry was shot at all.

### B2. Ana's target panel is bound to the wrong state
The record cites "013C #373; 0257 #381" for "Nano's preview and target panel with it ready".
- **D, 0B9E:** #373 and #381 both bind `v8070`. `v8070` is set by #324 (`ActionSetVar`, `v375[0]`), which comes after the press's target track.
- **D, the pre-press preview:** that is **#377**. It binds `v1691[0]` and the key (loadout 00E4).
- **`tools/reference/ana-map.md:201`:** the confirm option `v29052` gives a 3 s window with prompts. #384 is that prompt, and it sits with #373 and #381.
- **Reading:** the panel belongs to the target-confirmation flow, which is off by default and not in the mod. I did not follow the state edges to the end, so confirm that before removing it.
- **Shot:** `rt_ana_nano_preview` shows a VILLAGER panel, sitting on the marker's left bracket, that the data suggests a default Ana never sees.
- **Target mismatch (code):** the server picks with `e -> !Nano.boosted(e)` (`Ana.java:179`), the client with no filter (`ReticleHud.java:382`). With a boosted ally near the aim, the preview can name someone other than the one Q boosts.

### B3. Merged with `next`, Ana's widgets draw over the scope, undoing critique M6
Merged render, read from trial tree 41a1da8:
- `allyMarkers` is scissored to the lens, so marks don't land on the black surround. That is the point of ede87a9 ("sits under the hurt tints and ally marks").
- `ReticleHud.render` runs later with no scissor and no scope check. `ana()` isn't gated either.
- So scoped with Nano ready, the marker can land on the black surround, and the panel (cy+92) lies across the scope's vertical line (cy+44 to +154).
- The server's cone is world-angle, so at zoom the target can be well outside the lens.
- The record lists this as not tried. It has to be gated in the merge, not after it.

### B4. The health bars the data gives Mercy's and the Cat's panels are not drawn
- **D, Mercy:** 0258 #1263 exposes `m_45017CEA = v7126`, and #1315 puts **019C, the health bar,** into `v7126`. Table row: `19C 006B #1315 v7126`.
- **D, Cat:** 0BA3 #1097 exposes `v2390`, and #1098 puts 019C (for `v59143`) into it. Table row: `19C 5A8F #1098 v2390`.
- **Code:** both pass `health = -1`, though `panel` already supports a bar (Sombra's uses it).
- **Why it matters:** the owner's complaint is missing crosshair UI. A healer's target health is the most useful number on this panel, and it's in the table the branch was built from. It isn't in the record's "Still open", so nobody noticed it.

### B5. The required play hasn't happened
The record says so itself, and these are CLAUDE.md gates rather than extras:
- **Input:** keys went through the harness `key`, "not the physical mouse". The rule says the real mouse and keys.
- **Not played:** a real fight, a second player, hero picked from the menu or after death, water, lava, Resurrect on a player, a broken barrier, truly low energy.
- **No Overwatch side-by-side:** the owner's QA rule (2026-11-02) requires one for every check, and the record says "nobody has held these up".
- **Overclaim:** "each widget above, at the state that shows it" isn't true. There is no shot of Catnapper's carry bar and panel, the lockout bar (admitted) or a broken barrier's seconds (admitted). No `rt*` file names carry, lockout or broken.
- **On top of `next`:** it needs playing there. Juno's timers beside her prompts have never been on screen together.

### B6. The wording guesses are avoidable: the texts are probably one command away
- **Claim:** the record says `.07C` texts "aren't readable with what's extracted", and that the prompts' wording is therefore ours.
- **Found:** `E:\Overwatch Archive\tools\DataTool-2.25.0.0\DataTool.dll` contains the string `dump-strings`. No dump is in `research/`, and nothing in `techniques/` or `tools/` mentions the mode.
- **What it could settle:** 1B3C0 (Juno's "END"), 177F0 and 04CC (torpedoes), 1701B and 1701C (the tree), 161DC (the bash), 4ED9 and 2B2F6 / 2B2F7 (confirm and cancel), 2A472 and 2D604 (Grappled and Detach), 1894B (Resurrect's panel text).
- **Not verified:** I did not run it, so whether it supports this build and maps these ids is untested (#6 is about build support).
- **Rule:** "Use the real data whenever possible" means this gets tried before shipping a guess. Try it with Overwatch closed, and record the result either way.

## High

### H1. Previews that disagree with the button
- **Lifeweaver:** the marker uses `Targeting.allyInView` (nearest body point), while the server uses `Lifeweaver.pick` (eye or centre angle, and refuses `LifeGrip.gripped`). The marker can show on an ally E won't take, and with two allies can show the wrong one.
- **Brigitte:** the Repair marker is hidden while the barrier is up ("ours"). `repairPack` has no barrier check, so E still works unmarked, and D 0EDD #469's condition has no barrier in it either. The hide exists only to clear the bash prompt's row. The cause is that markers aren't part of the stack (H5).
- **Ana:** see B2.

### H2. Mercy's Resurrect panel on a real player shows a plus
- **Code:** the soul panel calls `face(g, null, d.face(), ...)`. With `who == null` the `Player` branch can't run, and `minecraft:player` isn't in `MobFaces`, so it falls through to `HudShapes.PLUS`.
- **D:** 00CA #306's 67DA is the hero portrait. Players are the main case for Resurrect, and it's "not tried".
- **Fix direction:** the server knows `soul.body`, so it can send the hero id or UUID.
- **Shot:** `rt_mercy_beam_boost` shows the other problem: two identical "VILLAGER" panels stacked, offset 14 px, the only cues a thin edge colour and a key. At a glance you can't tell the beam target from the soul.

### H3. Removing the panels loses information outside first person
- **Code:** Sojourn's energy and Lifeweaver's charge are gated on `reticle` (first person, or an ability camera from behind), and their old panels are gone. In F5 or an emote there is no reading of either.
- **Shot, Sojourn:** the number moved from a 26 px panel figure to a 20 px italic at cy+66, against the gun in first person (`rt_sojourn_energy`, `rt2n_sojourn_energy`). The deleted comment warned about her number and her gun. The right arc also runs onto the gun.
- **Decide:** keep the data's place, but choose what shows in third person.

### H4. Lifeweaver's arc and number are always on and read badly
- **Shots:** the idle arc under the ring reads as a **smiley face** (`rt3_weaver_preview`, `rt3_weaver_charge2`). The pale pink "62" and "34" on a brown villager are barely legible (`rt3_weaver_preview`, `rt3_weaver_tree_aim`). The top chevron of the marker lands on the arc at normal range.
- **D:** 072E's condition `STU_9D240539{self}` is not understood. It also binds B665 (`STU_E72F6348{m_entity=v375[0]}`), which the branch ignores.
- **Plain reading:** it may belong to the hold only. Until that's known, the permanent version is the noisiest guess on the branch.
- **(OW memory):** I recall nothing permanent under his crosshair, with low confidence.

### H5. "Two can't overlap" holds only inside the cursor
These aren't in the stack: `targetMarker` (013C), `guardianAngelTarget`, `lifelinePrompt` (+150), `songIndicator`, `ofudaCounter`, and on `next` `junoTimers`.
- **Marker centre:** an ally at normal range centres the marker near +85, so its side chevrons fall on the bar row at +92 and its key lands near the prompt row.
- **Shots:** `rt_mercy_valkyrie` (chevrons on the bar), `rt_mercy_beam_boost` (two panels, chevrons, LSHIFT, staff), `rt_ana_nano_preview`, `rt2n_mercy_beam_heal`.
- **Consequence:** Mercy, the hero who most needs a clear centre, is the most cluttered. Nothing has decided what wins there.

### H6. Crosshair and widget legibility
- **Jetpack Cat:** six 2.2 px dots are grey on pale sky by day (`rt_cat_purr`, `rt_cat_ult_aim`). At 1280 they're a smudge on a dark target (`rt2n_cat_idle`). No dot says "aim here". The earlier "too faint" fix wasn't enough.
- **Brigitte:** "- . -" is lost on her bright barrier top (`rt2_brigitte_barrier`), so while shielding the crosshair nearly vanishes.
- **Sigma:** the r8 ring and dot make a tiny bullseye, about 5 px radius at 1280 (`rt2n_sigma_barrier`).
- **Rings:** Mercy's and Sombra's sit on the target's face, and the kill marker crosses them.
- **Sigma's barrier bar:** pale blue on his own pale barrier by day (`rt_sigma_barrier`), and the same 650 is repeated on the panel.
- **Lúcio's count:** a thin italic "1" floating between the circles reads as a stray tick (`rt_lucio_amp`, `rt2n_lucio_count`).
- **Prompts:** 12 cap (8 px at 1280), no backing. "SHIELD BASH" and "GRAPPLED" sit on busy pixels (`rt2_brigitte_barrier`, `rt2_towed_sojourn`).
- **Keys:** prompts say "LMB", while the rest of the HUD draws a mouse glyph.
- **Night and day:** night is mostly fine, since white on dark is easy. Day is the weak case, the opposite of what "ten of them read: all legible" suggests.

### H7. Sombra's stealth may be the wrong kind of widget
- **D:** 0277 (420C #1057) is in **`v29677.v1897`**, the layer of each hero's reticle (044C, 0256, 00F2, 00A3, 091C...), with its own `m_45017CEA = v11821`. So the data's shape is a stealth *reticle*, not a ring added around her normal one.
- **Code:** the branch keeps the r16 ring and dot and adds an r40 ring (`rt_sombra_stealth1`), giving a double target.
- **Record:** it calls 0277 "in her reticle". Whether it replaces 00A3 wasn't checked.

## Medium

- **Overclock's bar is wrong.** `activeLeft` is `(ultTicks() - t) / 20`, and `ultTicks` is 0.75 + 8 + 0.45 = 9.2 s. The bar divides by 8. It sits full for about 1.2 s reading up to 9.2, against her 8 s ult. The shot `rt_sojourn_overclock` was taken at 7.8, which hides it.
- **Overcharged saturates.** The meter is `energy / ENERGY_MAX`, up to 1.5 with the perk (`Railgun.java:150`). The arcs clamp at 100 and the number reads 150. D 044D's fraction is `v87 / v38252`, so check whether `v38252` is the live cap. The perk isn't in the record.
- **Juno's lock has two buttons in the data.** D 4695 #867 and #1051 both have two pairs: loadout 03E2 with one text and loadout 0408 with the other (04CC, 177F0). The record reads this as "fire to launch" and draws one. `Torpedoes.java:65` fires on either button, so a second prompt is real.
- **Hack's portrait is missing.** #85 says "Hotfix/hack portrait (0D81)". D has two slots, #1798 (`v3544`) and #1831 (`v11652`). Only Hotfix's is drawn, and which slot is which wasn't shown.
- **Mercy's edge colours aren't the data's.** 0258 binds 0252's team colours for the target (406), and 2EC2 = `v11970` is a separate binding. Gold and blue edges are ours, and the record says so only under "by eye".
- **Mis-citations in the record:**
  - 0693 (00CA #1072) is called "its cast bar", but it has no fraction or time binding. It's a panel on the centre layer, so the bar is ours.
  - Ana's #373 versus #377 (B2).
  - Both undermine "bound is the data's" for a future reader.
- **Inspire can be active and invisible.** The widget shows only at `count > 0`. D 12D8 #479 has no condition on 067C, and its 7CCD binding (`v31949`, StateStack #438) is ignored. Inspire healing only herself shows nothing.
- **Stealth share after a refill.** On `next`, `Stealth.start` refills to `STEALTH_TIME` without a new fade, but the ring divides by `FADE_IN + TIME`. After a refill it never returns to full.
- **Gold ring removed for everyone.** The `LifeweaverFx` block ran for any Lifeweaver a client saw, and the replacement is on his HUD only. Observers lost it. D #913's condition (`v2172`, `v2102`) suggests a preview beyond the hold, but the code marks the blossom target only while charging. Until then, the marker on that ally says Life Grip's key.
- **Brigitte's row:**
  - Bash and flail both show the left mouse side by side, and Bash looks available with no barrier up.
  - Bash's box lost its ready flash; the new boxes pass `readyAt = -1`.
  - Repair's box shows recharge only at 0 charges, so with 1 or 2 left there's no sign of the next.
  - Charges appear twice: pips and a tiny corner digit.
- **Not gated while emoting.** Bars, prompts, markers and Ana's panel aren't gated by the emote camera or third person, unlike the rest of the reticle row.

## Low

- **Bandwidth is not a problem.** `withWidgets` goes every server tick to the owner only. It adds about 20 bytes (the -1 varint is 5, three floats 12, the rest 3) on a roughly 70 byte payload: about 400 B/s per hero. A soul in reach adds about 30 bytes a tick. Strings are capped at 64. It's also computed while dead or in Minecraft mode, which is harmless.
- **Per-frame cost.** `allyInView` runs per frame for Brigitte, Sombra, Lifeweaver (nearly always) and Ana (40 m box). It's the same pattern as Swift Step, but unmeasured among many villagers.
- **Dev-only is clean.** `devtowed` is reachable only through `DevServer`, gated on the system property `overcraft.dev` (`DevServer.startIfEnabled`). The public flag is harmless.
- **Minecraft mode is clean.** `rt2_mc_mode_brigitte` shows no widgets. `HeroHud.render` returns before the hook.
- **GUI scale.** The layout is in reference pixels through `scale()`, so scale should matter little. Untested; thin strokes are the risk.
- **Towed in Minecraft mode.** The prompt is never shown, since the hook is past the early return, but jump still detaches.
- **`logs/` is untracked and not ignored.** A `git add -A` would commit it.
- **No version bump or CHANGES.md yet.** Needed before `main`.
- **Unresolved.** "Sigma (023F)": in the table 023F is an ability-bar widget on many graphs. I didn't confirm it's his reticle. The table also has no rows from Sigma's kit, so it isn't complete.

## Against Overwatch (OW memory, low confidence)
No frames were available to me. These are questions for the required side-by-side.
- **Wording:** "END" is the weakest guess. I'd expect "CANCEL" or the ability's name. "PLACE" and "LAUNCH" are plausible. "GRAPPLED" has the wiki behind it.
- **Sojourn:** I recall energy tight round the reticle with a number, but not a red fill. The colour came from her hero colour, not her meter.
- **Bars:** I recall duration bars shorter than 180 px at 1080. The place looks right.
- **Crosshairs:** rings for Sombra and Mercy feel right. Brigitte's "- . -" and Juno's ring with ticks I can't vouch for. Every one except Kiriko's is invented, per the record.
- **Mercy's panel:** I can't confirm "a name beside a square".

## Merge with `next` (257dae0)
`git merge-tree --write-tree HEAD next`: **no textual conflicts**, tree 41a1da8. Auto-merged: `HeroHud.java`, `DevServer.java`, `HeroManager.java`, `Sombra.java`. I read the merged render and the Sombra diff and saw no API break, but nothing was compiled.

Semantic overlaps:
1. **Ana's scope and her widgets:** B3.
2. **Juno:**
   - `junoTimers` at +92 and +122 and the prompts at +160 fit, but the stack doesn't know about the timers.
   - The prompt runs on `MODE_LOCK`, the timer on `MODE_LOCK && junoLocking`, so they can disagree.
   - There are now two copies of `timedBar` and its constants, which will drift. The record calls this fine; I'd call `ReticleHud`'s from `junoTimers`.
3. **Sombra:** the refill (above). The stealth tint and the ring coexist unplayed.
4. **Order:** branch first, then `next`, then play on the result. The branch has only been played without these.

## Still missing from #85
- **Drawn nowhere:**
  - Mercy's and the Cat's panel health (B4)
  - Sombra's hack portrait
  - Juno's second torpedo prompt
  - Sigma's broken-barrier icon (0241, 1FC8A) and Hyper Strike counter (0243)
  - Lúcio's Accelerando meter (0275, 5162 #450)
  - Sombra's 00DB 017B #596, 0237 017B #528 and 0275 0164 #447, not even identified
- **World widgets:** soul markers (08F3), icons over the Cat for allies (0C3D / 011F), over a repaired or Hotfixed ally (011F), Hotfix's band on their health bar (0D86), Juno's 0727 lock marker (audit).
- **D.Va:** on her branch, so #85 stays open after this merges. Don't close it with `Fixes #85`.

## What fixed looks like
Fix B1, B2, B3, B4 and H1 to H2. Try `dump-strings` (B6). Merge `next` into the branch. Then play by hand with the real mouse, a second player (a tow and a Catnapper carry), a real fight, and a side-by-side against reference frames, with each case written down.
