# Real keys on a6609b6 (2026-11-05, 16:00-16:16)

First real-input play of polish-3 and polish-4, by desktop control with the owner's approval. Fresh launch (`run/p4_launch_tree.txt`: a6609b6, 0 dirty src files), fresh world. This doesn't meet the merge gate on its own: the code is changing again, so it has to be played again on the final commit.

**Real:** keys through Windows (E, Q, H, G, 2, Shift, Space); left clicks, and one right click, through Windows; every hero picked by H, a portrait click and READY.
**Not real:** aim (`look` and `/tp`: the mouse can't turn the view), setup by commands, reads through the harness. Bots are server-side players.
**Not possible with this input:** two keys at once, a held right button, so no barrier with bash, no W with Space, no scoped shot.

| Step | Expected | Seen |
|---|---|---|
| Join, select screen, READY with Kiriko | Kiriko, HUD with her keys | yes |
| Kiriko, E at nobody | throw and burst, no voice line (#121) | `suzu.throw` and burst, no `voice.suzu`, cooldown running |
| Kiriko, E at a bot ally slept by a bot Ana's dart | wakes, line after | `asleep` false, `voice.suzu` then `sleep.wake` |
| H at spawn, hero menu | opens | yes |
| Brigitte, Shift at a `NoAI` husk | our setup was wrong | 70 damage, 0 m: `NoAI` mobs don't take pushes |
| Brigitte, Shift at a husk with AI, speed 0 | about 7.75 m | 6.50 -> 14.25, 7.75 m, 194.4 |
| Brigitte, left click at 2.5 m | flail hit | 200 -> 196.4, Inspire pulse |
| Brigitte, E at full health, no ally | nothing (never herself) | nothing, by design of `REPAIR_RANGE`'s note |
| Brigitte, Q | Rally | cast, loop, armor; HUD 450/350, see #125 |
| G out and back during Rally | plain Minecraft, then hero | both modes yes; after G the HUD read 309/250, so her armor was gone, while overhealth stayed. Whether G should end Rally wasn't checked |
| Sigma, click, E, Shift, Q, click | spheres, rock, grasp, flux | all fired; 200 -> 181.2; Flux landed past the husk, aim is 0 pitch, so its lift isn't tested |
| Ana, click, Shift, E, right click, Q | rifle, dart, grenade, scope tap, Nano on herself | all; dart slept the husk, grenade's damage woke it; two voice lines are the cast's and the boost's, in code |
| Lúcio, Space held 1.2 m from a wall | hops only | hops, our setup |
| Lúcio, Space held against a wall | wall ride | start, 7.15 m/s along it, ended off the wall's end at x 10, **pain line on landing**, twice |
| Jetpack Cat, click, Space, Shift, E, Q | paw, climb at 5.5 m/s, flight, Purr, Catnapper's aim | all; peak y -50.53 |
| Sombra, click, E, E, Shift, Q | pistol, Translocator, stealth, Cyberspace, EMP | all; stealth ended by Cyberspace; 200 -> 157.65 |
| Juno, click, Shift, E | blaster, glide, ring | all |
| Mercy, click, 2, click | staff, blaster | swap and fire |
| Lifeweaver, click, E | blossom, then platform key | blossom heard; platform not confirmed in the shots |
| Sojourn, click, Shift | railgun, slide | fire, slide 12.8 m with cooldown 5.85 |

**Bug found:** Lúcio dropping off a wall ride takes fall damage from 4.48 m: `Hero.safeFall`'s default 3 m gives 1 health. The healing song hid it from health reads; the pain line didn't. CLAUDE.md, #35: no fall damage when an ability lands them.
**Open question found:** Rally's overhealth on Brigitte herself, #125.
**Not bugs, checked:** "KIRIKO" under each portrait is the dev account's name. Health 150/225 on picking Sombra was a 10 m fall from switching while flying as the Cat. H does nothing away from the bed (Juno glided about 29 m out, and the next two clicks fired her weapon).
**My error:** about 12 clicks at the roster's right half went into the Claude app's window, hidden from screenshots: [hand_play](../../hand_play.md#real-input). The roster was re-tried after moving the game clear of it.

**Not played yet:** death and respawn on real keys, leave and rejoin, a real fight with real mobs, water, ladders, boats, the Nether, plain survival in Minecraft mode, an ally healed by real keys other than the Suzu, Kiriko's kunai and ofuda, every ult but Rally, Flux, Nano, Catnapper's aim and EMP, a second client.

Shots were looked at as they were taken and not kept in the repo.
