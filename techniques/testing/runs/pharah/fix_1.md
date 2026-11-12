# Pharah fix 1 (2026-11-11): the critic's findings 1-5

Branch `feature/pharah` from c849b15, worktree `E:\claude\mods\overcraft_pharah`. Up: [Pharah's hub](../../../heroes/pharah.md) ·
the findings: [critic_1](critic_1.md) · the harness: [harness](../../harness.md).

One dev client at a time under `E:\claude\mods\.oc_client_lock` (taken 21:59:31, released 22:20:35 after the last client exited).
Overwatch and Battle.net checked closed before each launch and before reading the research folder. Fresh flat survival worlds,
`spawn_mobs` / `spawn_monsters` false, peaceful (no slimes in the falls). Input: the harness (`press key.keyboard.*` through
Minecraft's keyboard handler; Save and Leave by `screenclick` on the pause menu); sleep and stun from the dev `cut sleep|stun`, not a
real Ana or Brigitte. No desktop mouse or keys. Raw rows: [fix_1_raw_old.txt](fix_1_raw_old.txt) (c849b15, the fix stashed),
[fix_1_raw_new.txt](fix_1_raw_new.txt) (the fix). Screenshots `run/screenshots/fix1_old_*.png`, `fix1_new_*.png`. Script: the
critic's steps (T9 / T10 / T11 / T12) plus lava, one script for both builds.

## 1. Asleep or stunned mid-hover she kept hovering on her fuel
- **Fix:** `ClientPharah.live`: her hover (and the ground hold that starts it) also needs the synced `HeroManager.ASLEEP` off (sleep,
  stun, knock-down, Gravitic Flux: what `KeyboardInputMixin` already blocks). Server: `Pharah.tick` ends `pharahHovering` when she can't
  act, instead of waiting for her client's next input packet (it sends only changes).
- **Old (22:00:53, T11):** Space held, `cut sleep 3` at y -57.29, meter 0.6. She kept rising at 0.145 b/t to y -53.36, meter 0.6 ->
  0.0 in 1.3 s, then fell. Stun (T11b, 22:01:04) the same: -57.44 -> up, meter to 0.
- **New (22:04:51, T11):** `cut sleep 3` at -57.44, meter 0.625: the next row (0.25 s) vy -0.109, she falls, lands at 22:04:52.086, the
  meter holds 0.625 then refills on the ground to 1.0. Woken with Space still held (22:04:54.367) she hovers again (vy 0.145, meter
  draining). Stun (T11b, 22:05:02): the same, meter held at 0.65. Woken after 1.5 s (T11c): hover back at 22:05:13.670.

## 2. Space on a ladder drained the tank
- **Fix:** the hover's test is `HeroManager.jumpIsNotAirborne` (water, lava, cobweb, ladder, powder snow, riding) instead of water alone.
- **Old (22:01:24, T9):** W+Space on a 12-high ladder: meter 1.0 -> 0.0 in 2 s, climb 0.118 b/t.
- **New (22:05:30, T9):** the same: meter 1.0 the whole climb, climb 0.118 b/t (y -60 -> -54.76 in 2 s, as W alone).

## 3. Seated (boat, horse) and in lava, Space drained the fuel
- Same fix as 2.
- **Old:** boat (22:01:31) 1.0 -> 0.25; horse (22:01:38) 1.0 -> 0.225; lava (22:01:44, 2 deep, fire resistance; the suspected case,
  now confirmed) 1.0 -> 0.4 while she swam up.
- **New:** boat (22:05:37) 1.0 -> 1.0; horse (22:05:44) 1.0 -> 1.0; lava (22:05:50) 1.0 -> 1.0, she swims up as Minecraft does.
- **Control (T14, new, 22:05:21):** a plain hover from the ground still rises at 0.145 b/t, meter 1.0 -> 0.4 in 1.25 s.

## 4. Leaving mid-hover, she was hurt by the fall on rejoin
- **Fix (shared, every hero):** `HeroManager.JOIN_FALLS`. A player who joins in hero mode with a hero and in mid-air (the saved onGround,
  read in `onJoin`) has the fall distance zeroed and `fallIsSafe` true until the fall has begun and Minecraft has checked its landing
  (the distance back to 0), or they ride, swim, climb, or press G. Minecraft mode's falls stay Minecraft's (not added), and so does a
  join on the ground. Other heroes' rules are untouched: the flag only adds a safe case, and only for that first fall.
- **Old (22:02:43, T12):** hovering at -48.94, Save and Leave, reopen: she fell behind the select screen, health 18.0 -> 11.0.
- **New, three tries before it held** (each in the raw file): (a) added only when `!onGround` at the join, cleared by the server's
  onGround: hurt 18 -> 10 (the first save hung, below; a second world `fix1_new2`); (b) cleared by onGround after 10 ticks: the server read
  onGround true at y -59.06, a tick before the landing's check, 18 -> 11; (c) cleared by the distance after 10 ticks: cleared at y -48.94
  while her client was still loading, 18 -> 10. Final (22:17:51): rejoined at -48.94, fell 11 m, **18.0 -> 18.0**; the log's
  `join fall over onGround=true y=-60.0` came after the landing.
- **The flag ends (22:18:09, T12b):** after that landing, a 15 m drop in hero mode with no ability hurts as before (18 -> 8).
- **Control (22:20:11, T12c):** Minecraft mode (G), 20 m up, Save and Quit, rejoin: a plain Minecraft fall, 18 -> 2 (not added:
  `heroMode=false`). (Its first try missed the vanilla pause menu's button and never left; noted in the raw file.)
- **Not changed, noted:** every cooldown resets on rejoin (`onJoin`'s `state.reset`, every hero), so leaving and rejoining refreshes the
  kit; the owner's call.
- **Limit:** the saved onGround decides. A player teleported into the air and leaving before they move reads onGround true (seen at
  22:19:57 in Minecraft mode); a hero who left that way still takes the fall. Not hit by any hero's flight (they all move).

## 5. The data (research `E:\Overwatch Archive\research\pharah\work_kit\c_*.txt`, offline, Overwatch closed)
- **5a, do Jump Jet / Jet Dash cancel a reload? Yes (H nodes, M meaning); code kept.** 0027 #82 and 4B92 #82 (StateAbility) push the
  shared stack v33 at priority 0003.024; the reload is 0023 #60 (StateStack on v33, priority 0002.024) whose under plug #61 sets
  v51 = 0, the reload over (#36 v51 = 1 starts it). 3 over 2 read as Sojourn's 0x79 over 0x2 (sojourn.md; the 024 assets' order itself
  isn't read). Cited at `Pharah.startJet`.
- **Found by the way, not changed:** Concussive Blast's StateAbility (007B) pushes v33 at 0001.024, under the reload's 0002, so by the
  same reading it would not cut a reload; the code cuts it (`Pharah.press`, `interruptReload`). Barrage's 002B #24 / #137 push at 0003
  (it refills the ammo on cast anyway, #149 -> 007D). Worth a side by side before changing.
- **5b, does casting Barrage end an active Jump Jet? No (M); code kept.** 0027's message entries are 0093 (a kill; 002B only receives
  it too), 21E2 and 2623; 4B92's 21E2, 1BB5, 2623: none is sent by 002B's cast (it sends 0094, 00BB, 0012, 064A, 003E, 007D). 002B
  pushes v33 at 0003, the jets' own priority, not over them. Cited at `Pharah.barrage`.

## Not done / not seen
- A real Ana / Brigitte / Sigma (only the dev `cut`), a second player, the real mouse and keys.
- After the three join revisions only T12 / T12b / T12c ran on the final build; T9-T14 ran on the build with the same
  `ClientPharah` / `Pharah` code (the later changes are in `HeroManager`'s join path only).
- **One unexplained miss (22:06:03):** the first T12 try on the new build left at y -59.54 falling: the Jump Jet fired (meter 0.866)
  but the hover didn't hold her. Not seen again in 5 tries (22:06:41, 22:07:02, 22:07:15, 22:11:39, 22:14:06, 22:15:54, 22:17:46).
- **A hang:** the Save and Leave at 22:07:25 hung the client at "Saving worlds" (server thread looping in
  `MinecraftServer.stopServer -> waitUntilNextTick`, jstack; nothing of Overcraft's on that stack). Killed and relaunched. Not seen in
  the other six leaves. Not chased.
