# The widgets at the reticle (#85)

Up: [techniques map](../README.md) · beside: [kill_feed](kill_feed.md), [qa_audit_2026-11-02](../testing/qa_audit_2026-11-02.md)
(findings X6, X7, each hero's rows), [hand_play](../testing/hand_play.md) · code: `client/hud/ReticleHud.java`, `client/HeroHud.java`
(`crosshair`, the hook after `hitMarker`), `net/HeroSyncPayload.Widgets`, `hero/HeroKit.widgets`, each kit's `widgets` · data:
`E:\Overwatch Archive\research\<hero>\work_kit\decomp_XXXX.txt` (Overwatch closed, `Get-Process` first).

The owner, 2026-11-02: "most heros have some kind of UI under/around/near the crosshair, but are missing them."

## What the data has, and what it doesn't
A `StateUXPresenter` node shows one view model (`m_F2160520`, `.116`) in a layer (`m_D44EB181`) with named bindings (`m_77CF91F7`):
**the data says which widget, in which layer, bound to what, and when (the node's state and its `m_condition`).**
The `.116` assets themselves are empty at every depth: no art, no position, no size, no colour. So **every shape, size, colour and place in
`ReticleHud` is ours**, built from frames and memory of the game, and marked so. Don't go looking for the layout again: the 2026-11-02 audit
and this round both looked. Quick way to read one: print the node's `m_EC32138E` (the scratch `rt_ux.py` filtered every hero's by id).

Layers: `v11821` a hero's reticle container (`v40842` for Kiriko and Jetpack Cat); `v40843` the centre layer; `v40845` the ability bar;
`<...>.v36881` the world (on an entity). Shared widgets and their bindings, one meaning across heroes:

| Widget | What it is | Bindings |
|---|---|---|
| 0237 | a timed ability's bar | `9A` share, `66B1` time (`op109` rounds it), `24D` icon |
| 0275 | a resource's bar | `9A` / `6655` share, `9A0` text, `201` icon, `639A` dimmed |
| 00DB | one or two button prompts | `6441` / `6442` first button and text, `6672` / `6673` second |
| 00CF | a prompt with a state over it, on the HUD of whoever it happens to | `1D1` button, `616E` / `A522` texts |
| 013C | a target's preview, in the world on them | `68E9` where, `1D1` button, `6181` text |
| 0258, 052B, 0BA3, 0D81, 0257 | a target's panel | `4447` / `67DA` portrait (a hero's, or the texture 1BDC2 for a non-hero), `2E4B` / `67DC` name, `406` the team colours 0252 |

Logical buttons: `xD128396E` primary, `xB42D90B7` secondary, `x4F81E1BD` jump, `x19C8495B` interact, `x2ADDA7B1` ability 2, `xCBF0F815` the weapon's. Text ids
(`.07C`) aren't readable with what's extracted, so **what prompts say is ours**, and says what the button does in the mod.

**Buttons in code:** the server's primary is `Action.SECONDARY_FIRE`, the left button (the names follow Kiriko), so a data "primary" prompt shows `SECONDARY_FIRE`.

## The system
`ReticleHud.render` runs after the hit marker in hero mode only (`HeroHud.render` returns before it in Minecraft mode). A cursor `y` stacks widgets downward so two
can't overlap: reticle row from +46, bars from +92 at a 30 pitch, prompts from +160, ELIMINATED at +250. `crosshair` draws each hero's. Values the sync didn't have go in
`HeroSyncPayload.Widgets` (`HeroKit.widgets`; `HeroManager` adds it, so a kit's `sync` is unchanged and a new kit gets `NONE`).

To add a widget: read its node, draw it with `timedBar`, `resourceBar`, `panel`, `prompt` or `targetMarker`, cite the node in the javadoc. Don't place by pixel: the cursor does it.

**Merging with fix/hero-polish:** its `HeroHud.timedBar` / `junoTimers` (Juno's 0237s, +92, 30 pitch, 180 x 6) match `ReticleHud`'s bars on purpose. Its lines in `render` sit beside the hook without touching it. After that merge, call `ReticleHud`'s bar from `junoTimers`, or leave it: same look.

## Per hero
"Bound" is the data's (graph #node); the look of every row is ours.

| Hero (reticle) | Added | Bound to |
|---|---|---|
| Sojourn (044C, 142E #418) | Railgun energy as two arcs and a number at the reticle, off the panel; Overclock's bar | 044D, 1A23 #1179; 0237, 2159 #727 |
| Sigma (023F) | barrier's bar and health while out; Flux's aim bar and "[LMB] CONFIRM"; Grasp's hold bar (ours: the data has no widget for it) | 0275, 2155 #12; 0237, 1E41 #1152; 00DB, #1283 |
| Brigitte (00F2, 145B #51) | Barrier Shield's box with its health band and broken seconds; Repair Pack as a box with charges, and charges at the reticle; Inspire icon and count; barrier's bar; Rally's bar; "[LMB] SHIELD BASH"; Repair's preview | 0243 / 0240, 0EE3 #1150, #558; 00F3, 0EDD #472; 0C7A, 12D8 #479; 0275, 2152 #7; 0237, 12BE #188; 00DB, 0EE3 #848; 013C, 0EDD #469 |
| Mercy (0256, 0071 #443) | beam target's panel, edge gold or blue by mode; Resurrect's soul panel with its key and soul count; its cast bar; Guardian Angel's momentum; Valkyrie's bar | 0258, 006B #1263; 052B, 00CA #306; 0693, #1072; 0275, 006E #910; 0237, 10DA #486 |
| Sombra (00A3, 018D #595) | stealth ring; Hotfix's target panel with health; lockout bar; Hotfix's preview | 0277 / 008B, 420C #1057; 0D81, 614C #1798; 00B1, #2775; 013C, #593 |
| Juno (091C, 4441 #563) | glide and torpedo prompts. Her 0237 timers come with fix/hero-polish, not here | 00DB, 47B7 #637, 4695 #867 |
| Jetpack Cat (0BA2, 59DC #43) | Lifeline's panel; Purr's and Catnapper's bars; "[LMB] CONFIRM [Q] CANCEL"; towed ally's GRAPPLED and "[SPACE] DETACH"; crosshair from her volley (one shot below the aim, four in an arc above: the wiki) | 0BA3, 5A8F #1097; 0237, 5A7E #320, 5B7B #63; 00DB, 5B7B #34; 00CF, 5A8F #1225 |
| Lifeweaver (072D, 21C6 #844) | blossom's charge as an arc and heal number at the reticle, off the panel; blossom and Life Grip previews (the world's gold ring is gone); "[LMB] PLACE [RMB] CANCEL" | 072E, 21C6 #1122; 013C, 21C6 #913, 21EF #704; 00DB, 36A4 #901 |
| Lucio (027B, 010C #311) | allies in his song, between its circles; Amp It Up's bar | 0263, 0266 #374; 0237, 0277 #396 |
| Ana (0987, 0C6B #34) | Nano's preview and target panel with it ready | 013C, 0B9E #373; 0257, #381 |
| Kiriko (05BF) | nothing new: the "+" is measured from Overwatch, her counter and target were already drawn. The data shows her no bar (30D8 is not decompiled, so unproven) | |
| D.Va | on her branch: [her list](../heroes/dva.md) | |

## What a critic should check
- **Against Overwatch, side by side:** nobody has held these up against the real game yet, and nothing here came from frames of it for these heroes. Every shape is from memory or invented. Most doubtful: Sojourn's arcs, each hero's crosshair (Kiriko's alone is measured), the panels' look and place (is Mercy's target really a name beside a square?), the bars' place.
- **Words:** the prompts' wording is ours (the texts are ids). "END", "PLACE" and "LAUNCH" are guesses at Overwatch's.
- **World markers over widgets:** a 013C marker sits on the ally, and an ally near the aim puts it right under the crosshair, where the stack is. Widgets now draw over it, so they stay readable, but they do overlap (`rt2n_ana_nano_preview`, `rt2_brigitte_inspire`). Repair's is hidden while the barrier's up (ours). Overwatch's own overlap was not checked.
- **Passive charge:** Lifeweaver's arc shows whenever the blossom has charge, also not held, since it fills by itself; 072E's condition `STU_9D240539{self}` isn't understood. It may belong only to the hold. It also stays up while he aims the tree (`rt3_weaver_tree_aim`).
- **Hit marker:** its strokes cross a ring crosshair (Mercy's r14, Sombra's r16) for 0.2 s; they stop at +28, so no widget below is touched.
- **Colours** are by eye: Mercy's gold and blue, Sombra's purple, the bars' white.

## Played
Build: `fix/reticle-ui`, `runClient`, pack from `overcraft_ana`, keys through the harness (`key`, which goes through Minecraft's key mappings; not the physical mouse), a flat world, a real villager as the ally and real zombies as targets. Screenshots in `overcraft_reticle/run/screenshots`, opened and read (as crops of the middle in sheets, full frames for the rows named).

**Reproduced first on `next` (75d95f9):** `rtbase_sojourn_energy` (the energy in the panel, nothing at the reticle), `rtbase_brigitte_barrier` (no barrier box, no health, Repair as a panel). The rest of the baseline set (Mercy, Sombra, Jetpack Cat) was shot but not all opened; the Lifeweaver one failed to switch hero.

| Pass | Heroes | Seen |
|---|---|---|
| 1920x1080, day (`rt_*`, `rt2_*`) | all but D.Va | each widget above, at the state that shows it |
| 1280x720, night (`rt2n_*`) | all but D.Va | same; ten of them read: all legible, same layout |
| Brigitte from behind (`rt2_brigitte_barrier_back`) | | her barrier camera; widgets and crosshair on it |
| Minecraft mode (`rt2_mc_mode_sojourn`, `rt2_mc_mode_brigitte`) | | every widget gone, only the small ult meter |
| A kill (`rt_kiriko_kill1`, `rt2n_sojourn_energy`) | | ELIMINATED at +250 clear of everything |

Found playing and fixed: target markers drew over the stack (widgets now after them); Life Grip's key landed in the tree's prompts (`rt2_weaver_tree_aim`, fixed: `rt3_weaver_tree_aim`); Jetpack Cat's dots too faint.

**Not tried:** a real fight or a night in survival with these up; a second player, so **the towed prompt was only seen through the dev command `devtowed` (no real tow, no real detach press)**; the hack lockout bar (needs an enemy deployable: never on screen); a broken barrier's seconds; Sojourn at truly low energy (`rt2_sojourn_low` reads 90); Resurrect on a player; water, lava, boats, the Nether; hero picked from the menu or after death (the harness's `hero` command each time); other GUI scales; fix/hero-polish merged in (Juno's timers beside her prompts, Ana's scope over her panel).

## Still open
World widgets, not built: souls' markers (08F3), icons over the Cat for allies (0C3D / 011F), over a repaired or Hotfixed ally (011F), Hotfix's band on their health bar (0D86). Sigma's Barrier as an ability box with its broken icon (0241, 1FC8A); his Hyper Strike counter (0243). Sombra's prompts 00DB 017B #596 and bar 0237 #528, 0275 0164 #447: what they belong to wasn't worked out. Lucio's Accelerando meter (0275, 5162 #450). Life Grip's held ally's prompt (00DB, 21EE #617): the mod has no way to let go, so no prompt. Mercy's and Sombra's panels don't show team colours beyond the ally cyan, since 0252's values aren't readable. The data's non-hero portrait 1BDC2 isn't in the pack: a mob with no `MobFaces` entry gets a plus.
