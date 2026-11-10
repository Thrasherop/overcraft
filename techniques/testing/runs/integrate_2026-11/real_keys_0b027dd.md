# Real-key pass of integrate/2026-11 (polish-3, polish-4, watcher, railgun), 2026-11-10 16:27-16:41

Up: [polish_method](../../polish_method.md) · [harness](../../harness.md). Tree 0b027dd, clean `src`, fresh launches, fresh worlds. Coordinator's pass. Input: computer-use clicks and keys (keys held 0.1 s, as a person's tap), OS mouse events from `rk_mouse.ps1` for flicks; the harness only set things up and read state. Overwatch and Battle.net checked closed before each launch. **Not a person's hands.** Raw log: [int_log.txt](int_log.txt).

| Step | Expected | Seen |
|---|---|---|
| Join a fresh world, pick Sojourn on the select screen by mouse | her HUD | yes |
| Charged Shot, idle, real click | fires on the press | client sound +7 ms (session's first shot), server shot +24 ms, 8.27 to a zombie |
| Flick, 200 counts over about 100 ms, click 15 ms on | hits | 5 of 5 (a 6th void: the first mouse movement after focusing didn't land, crosshair 3.75° off) |
| Lifeweaver off a 14 m pillar: W off the edge, Space (dash), no G | lands unhurt (ability's landing) | 20 -> 20, z 17.5 |
| The same with G 0.3 s after the dash | lands as Steve, hurt (owner, 2026-11-10) | 20 -> 10.48, in Minecraft mode |
| Lifeweaver in deep water, Space twice | swims, no dash | z 7.5 unchanged, cooldown 0 |
| Minecraft mode: place a dirt block, hold left to break it | plain Minecraft | placed (4 -> 3), broken and picked up (-> 4) |
| `/kill`, Respawn by mouse | vanilla death screen, back in the mode died in | yes, Minecraft mode |
| G back, pause menu, Change Hero, Mercy, READY | Mercy, her HUD | yes |
| Save and Leave, Singleplayer, open the world | select screen with the hero kept; READY | Mercy kept, 18/18, hero mode |

**Traps met:** with `pauseOnLostFocus` on, the Claude app taking focus opened the pause menu and swallowed input: set it off in this worktree's `run/options.txt`. The computer-use `key` tap is shorter than a tick and missed a dash: hold keys 0.1 s. Rows run with the menu open are void and not above.

**Noted, not a bug:** Minecraft mode's first person shows the hero's arms holding Minecraft's items, as the owner asked on 2026-10-27 (an option). **Minor:** the select screen opened from the pause menu says "near your bed" with no bed near.

**Not played here:** a fight with mobs at night, healing an ally, water/lava/ladders/boats/the Nether beyond the above, a second player, Brigitte, Kiriko, Sombra, Ana, Lúcio, Juno, Sigma, Jetpack Cat by real keys.
