# The widgets at the reticle (#85)

Up: [techniques map](../README.md) · beside: [kill_feed](kill_feed.md), [qa_audit_2026-11-02](../testing/qa_audit_2026-11-02.md)
(findings X6, X7, each hero's rows), the review [critique_reticle](../testing/critique_reticle.md), [hand_play](../testing/hand_play.md) · code: `client/hud/ReticleHud.java`, `client/HeroHud.java`
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

Logical buttons: `xD128396E` primary, `xB42D90B7` secondary, `x4F81E1BD` jump, `x19C8495B` interact, `x2ADDA7B1` ability 2, `xCBF0F815` the weapon's.

**Prompt wording is the data's.** DataTool's `dump-strings` works offline on the archived build (`DataTool.exe --online=false "E:\Overwatch Archive\build-154088" dump-strings`, 14 s, Overwatch and Battle.net closed), giving `research\strings\strings-154088.txt`, lines `00000001B3C0.07C: CANCEL`. Used here: 1B3C0 CANCEL (Juno's glide), 177F0 FIRE and 04CC CANCEL (torpedoes), 1701B BUILD and 1701C CANCEL (the tree), 4ED9 and 1DE77 CONFIRM (Flux), 2B2F6 POUNCE and 2B2F7 CANCEL (Catnapper), 161DC BASH, 2A226 GRAPPLED, 2A472 Detach, 1894B "Resurrect %1$s", F7A FUEL, 16195 NANO BOOST and 16196 CANCEL (Ana's confirm flow, not built), 31330 CANCEL (Life Grip's held ally, not built), 5FCE a number format.

**Buttons in code:** the server's primary is `Action.SECONDARY_FIRE`, the left button (the names follow Kiriko), so a data "primary" prompt shows `SECONDARY_FIRE`.

## The system
`ReticleHud.render` runs after the hero's target markers in hero mode only (`HeroHud.render` returns before it in Minecraft mode), and not while an emote holds the view. A cursor `y` stacks widgets downward so two can't overlap: reticle row from +46, bars from +92 at a 30 pitch, prompts from +160 (Kiriko below her counter, Lúcio above his circles), ELIMINATED at +250. `crosshair` draws each hero's. Values the sync didn't have go in `HeroSyncPayload.Widgets` (`HeroKit.widgets`; `HeroManager` adds it, so a kit's `sync` is unchanged and a new kit gets `NONE`).

**Markers and the stack, what wins:** a 013C marker is on the ally, and an ally near the aim puts it under the crosshair. The stack wins: `HeroHud.targetMarker` asks `ReticleHud.markerRoom` (the stack's box last frame, with Kiriko's and Lúcio's fixed widgets), and if it would land there, it closes up to 30 half width above the stack, around the crosshair, its key beside it. Ours.

**Previews are the server's pick:** `Widgets.preview` is the same call the button makes (`Ana.startNano`'s filter, `Lifeweaver.pick` and its refusals, `repairPack`, `hotfix`), sent each tick, so a marker can't name someone the button won't take. Swift Step's and Guardian Angel's are still client-side copies.

To add a widget: read its node, draw it with `timedBar`, `resourceBar`, `panel`, `prompt` or `targetMarker`, cite the node in the javadoc. Don't place by pixel: the cursor does it. Juno's timers are in here now (the merge of `next` had a second copy in `HeroHud`).

## Per hero
"Bound" is the data's (graph #node); the look of every row is ours.

| Hero (reticle) | Added | Bound to |
|---|---|---|
| Sojourn (044C, 142E #418) | Railgun energy as two arcs and a plated number; gold from 100 with Overcharged; on the panel when there's no reticle; Overclock's 8 s | 044D, 1A23 #1179; 0237, 2159 #727 |
| Sigma (023F: its role unconfirmed, see below) | barrier's bar and health while out; Flux's aim bar and "CONFIRM"; Grasp's hold bar (ours: no widget in the data) | 0275, 2155 #12; 0237, 1E41 #1152; 00DB, #1283 |
| Brigitte (00F2, 145B #51) | Barrier Shield's box with health band and broken seconds, turning into Bash while it's up; Repair Pack's box with charges and the next one's recharge; charges at the reticle; Inspire icon (with Inspire on her alone too) and count; barrier's bar; Rally's bar; "BASH"; Repair's preview | 0243 / 0240, 0EE3 #1150, #558; 00F3, 0EDD #472; 0C7A, 12D8 #479; 0275, 2152 #7; 0237, 12BE #188; 00DB, 0EE3 #848; 013C, 0EDD #469 |
| Mercy (0256, 0071 #443) | beam target's panel with health, a strip gold or blue by mode (ours), the edge the ally cyan; "RESURRECT X" with key and soul count, a hero's face for a player's soul; Guardian Angel's momentum; Valkyrie's bar. Resurrect's cast bar is ours: 0693 (00CA #1072) is a panel with no fraction | 0258, 006B #1263; 019C, 006B #1315; 052B, 00CA #306; 0275, 006E #910; 0237, 10DA #486 |
| Sombra (00A3, 018D #595) | stealth ring in place of her crosshair's (0277 is in her reticle's own layer), share of the time set, so full after a refill; two target slots with health: Hotfix's allies, or what she's hacking; lockout bar; Hotfix's preview | 0277 / 008B, 420C #1057; 0D81, 614C #1798, #1831 (v77182[n], else v536); 00B1, #2775; 013C, #593 |
| Juno (091C, 4441 #563) | glide and lock bars (moved here from `HeroHud`); "[Shift] CANCEL"; both buttons' "FIRE": the data has FIRE and CANCEL, swapped by an option, but in the mod either fires | 0237, 47B7 #641, 4695 #1225, #1283; 00DB, 47B7 #637, 4695 #867, #1051 |
| Jetpack Cat (0BA2, 59DC #43) | Lifeline's panel with health; fuel bar under full; Purr's and Catnapper's bars; "POUNCE / CANCEL" (the ult key cancels in the mod); towed ally's GRAPPLED, and "Detach" only from 0.6 s, only on Lifeline; crosshair from her volley (the wiki) | 0BA3, 5A8F #1097; 019C, #1098; 0275, 214F #5; 0237, 5A7E #320, 5B7B #63; 00DB, 5B7B #34; 00CF, 5A8F #1225 |
| Lifeweaver (072D, 21C6 #844) | while the blossom is held: a charge ring and plated heal number; on the panel when there's no reticle; blossom and Life Grip previews; "BUILD / CANCEL" | 072E, 21C6 #1122; 013C, 21C6 #913, 21EF #704; 00DB, 36A4 #901 |
| Lúcio (027B, 010C #311) | allies in his song, on a disc between its circles; Amp It Up's bar | 0263, 0266 #374; 0237, 0277 #396 |
| Ana (0987, 0C6B #34) | Nano's preview, only over the lens when scoped (ours). No panel: #373, #381 and #384 bind v8070, set by #324 after the press, with the texts NANO BOOST / CANCEL: the confirm option v29052, off by default, not in the mod | 013C, 0B9E #377 |
| Kiriko (05BF) | nothing new: the "+" is measured from Overwatch, her counter and target were already drawn. The data shows her no bar (30D8 is not decompiled, so unproven) | |
| D.Va | on her branch: [her list](../heroes/dva.md) | |

## What a critic should check
- **Against Overwatch, side by side:** still not done. No frames of the real game for these heroes. Every shape is from memory or invented; Kiriko's crosshair alone is measured.
- **Lifeweaver's hold-only is a choice:** 072E's condition `STU_9D240539{self}` still isn't understood. The critique's reading (the hold) is what's drawn.
- **A closed-up marker** around the crosshair (`rt6_brigitte_inspire`, `rt6n_mercy_valkyrie`) is our rule, unseen in the game. Its cyan is the weakest thing on a light target.
- **The critique's "gold ring lost to observers" doesn't stand:** that `LifeweaverFx.markers` block ran only for the local player (`mc.player`, his own client). Nothing to restore.
- **The hit marker** still crosses a ring crosshair for 0.2 s.
- **Sigma (023F):** the table shows 023F as an ability-bar widget on many graphs, so "his reticle" (the audit) is unconfirmed.
- **Overcharged:** the data's fraction is v87 / v38252, with v38252 set at 1A23 line 1419, and v971 the perk's cap at #1485. Whether it can pass 1 wasn't settled. The gold overfill is ours, and not played (needs the perk in Overclock).

## Played
Keys through the harness (`key`, Minecraft's key mappings), **not the physical mouse**: desktop control needs the owner. One client, so no second player. Screenshots in `overcraft_reticle/run/screenshots`, read as mid-screen crops in sheets, full frames where named.

**Reproduced on 016d4cf (`next` merged, before these fixes), `rtb_*`:** Catnapper's carried zombie as a cyan ally panel (`rtb_cat_carry1`); Ana's panel across the scope's line (`rtb_ana_scoped_center`, `rtb_ana_scoped_8deg`); Overclock's bar at 8.9 at the start (`rtb_overclock_start`); Sombra's double ring (`rtb_stealth_first`). **Not reproduced:** a Catnapper victim's false prompt (needs a second player: it's from reading `towed`).

| Pass | What | Seen |
|---|---|---|
| Day, 1920 (`rt4_*`, `rt6_*`) | every hero but D.Va and Kiriko's own widgets | `rt4_cat_carry1` no panel; `rt4_ana_scoped_*` marker on the lens, no panel; `rt4_overclock_start` 8.0; `rt4_stealth_*` ring in place of hers; `rt6_hotfix_two` two slots with health; `rt4_lockout` the bar (set by the dev command `devwidget lockout`: no enemy deployable exists with one client); `rt4_mercy_beam_boost` health, strip, "RESURRECT VILLAGER" |
| Broken barrier (`rt7_broke_a`, `_b`, `_c`) | `devwidget barrier1`, then `/damage @s 2 mob_attack by` a zombie (a command, not a swing; the game answered "invulnerable" with the barrier taking it) | barrier gone, box counting 5 then 2, then ready |
| Bright: white wall, sky (`rt4_wall_*`, `rt4_sky_*`, `rt6_sky_cat`) | Brigitte, Sigma, Mercy, Ana, Lifeweaver, Cat | crosshairs and the barrier bar read. `wall_jetpack_cat` and `sky_jetpack_cat` are Brigitte (the switch was refused just after being hit), so the Cat's is `rt6_sky_cat` only; no snow biome |
| Night, 1280 (`rt6n_*`) | Sojourn, Brigitte, Mercy, Juno, on the final build | legible |
| Real terrain, night, real mobs (`rt5_fight_*`) | Kiriko, Sojourn, Brigitte, Lúcio, Sombra, Juno, about 8 s each: two zombies and a skeleton, aim by a `tp` facing the nearest, no resistance | health before and after (OW-scale hearts in Minecraft health): 20 to 10, 18 to 13, 20 to 18.8, 16.5 to 17.7, 15.5 to 18, 18 to 6. No deaths. A scripted scrap, not a night in survival |
| No reticle (`rt6_back_sojourn`, `rt6_back_weaver`) | F5 | the reading on the panel |
| Ways in (`rt6_after_select`, `rt6_after_death`) | select screen's READY, `kill @s` and respawn | widgets back |
| Minecraft mode (`rt4_mc_mode_*`), an emote (`rt7_emote`) | | none (Kiriko's emote: she has no stack, the ofuda counter's gone) |
| Towed (`rt4_towed_*`) | `devtowed` | GRAPPLED alone at first, "[SPACE] Detach" after 0.6 s, clear of Kiriko's counter and Lúcio's circles |

Found playing the fixes: the markers' room was cleared before the hero's markers read it, so they never closed up (`rt4_brigitte_inspire`, fixed in `rt6_brigitte_inspire`).

**Not tried:** a real tow, a real Catnapper victim, Resurrect on a player's soul (`hero:` face), so a second player; the physical mouse; Overcharged; water, lava, boats, the Nether; GUI scales; a night in survival.

## Still open
World widgets, not built: souls' markers (08F3), icons over the Cat for allies (0C3D / 011F), over a repaired or Hotfixed ally (011F), Hotfix's band on their health bar (0D86), Juno's lock marker 0727. Sigma's Barrier as an ability box with its broken icon (0241, 1FC8A); his Hyper Strike counter (0243). Sombra's 00DB 017B #596 (CANCEL / CANCEL), 0237 #528 and 0275 0164 #447: not matched to anything in the mod. Lúcio's Accelerando meter (0275, 5162 #450). Juno's and Life Grip's cancels: in the data (04CC, 31330), not in the mod, so not prompted: worth a mechanics issue. 013C's "Disabled" text (319B2). A mob with no `MobFaces` entry gets a plus: 1BDC2 is in `extracted\154088\UITextures` but not the pack.
