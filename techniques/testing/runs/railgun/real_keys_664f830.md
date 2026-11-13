# #126, real input, 664f830 (code 16d1ca9), 2026-11-06 01:29-01:40

Coordinator's pass. Fresh launch and world. Sojourn picked on the select screen with real clicks. Input: computer-use clicks and keys; for two buttons at once and flicks, OS `mouse_event` from `rk_mouse.ps1`, which refuses unless the game window is in front. Target: `NoAI` zombie at 12 m. Numbers are the press trace's clock, ms; raw in [real_keys_664f830_log.txt](real_keys_664f830_log.txt). 115 fps. **Not a person's hands: feel isn't judged.** Overwatch and Battle.net checked closed before launch and before the mouse scripts.

| Case | Seen |
|---|---|
| Idle, not the session's first | frame +2, server shot +12.5, hit sound +16 |
| **The session's first press** | frame +839, against server shot +13 and hit sound +348. Screenshots were taken either side, so how much is ours isn't separated. The agent's trace has 289 ms. **Needs a clean measure** |
| R, then right click at once | shot on the press, server +1.6, hit +8; magazine stays 35 |
| Left held 0.7 s, then right | on the press, frame +2.2, server +12.4; rounds stop, and resume alone 615 ms on (10 fps not involved) |
| Both held 1.5 s | first on the press; **a second shot at +629, `charged.fire_low`**. Most likely rounds in flight landed and gave energy (not read). The record's "1 shot outside Overclock" is only true with no energy coming |
| Shift, then right | fires on the press, 5.51 (body: the view had dropped) |
| Space, then right | on the press, server +2.4, 8.27 |
| E, then right | on the press, server +23.7 |
| G: right click in Minecraft mode | nothing fired. G back, right at once: fires |
| Flick, 200 counts over about 100 ms, click 15 ms on | **6 of 6 hit** |
| Flick, burst, click 30 ms on | **6 of 6 hit** |
| **Flick, burst, click in the same frame** | **4 of 10 hit (3 of 6, 1 of 4)**, final yaw right on the target. Presumably the press reads the aim before that frame applies the mouse's movement (not read in code) |

**A void row:** computer-use's `left_mouse_down` then `right_click`: the tool let go of the left button first (a send, the rounds stop, 128 ms before the press). The rows above replace it.

**Not played:** a person's hands; melee's recovery; Overclock; an emote; empty magazine; sleep, stun, hack, death; a second client; latency; 60 fps or lower.
