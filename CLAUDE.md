# Working rules for this repo

Overcraft: Overwatch heroes in Minecraft 26.3 (Fabric). Rules carried over from the owner's Lethalcraft project
(E:\claude\mods\lethal_minecraft\CLAUDE.md) where they apply, plus Overcraft's own.

## Overwatch: never get the owner banned (owner's rule, 2026-10-25)
- **Overwatch must never be running** while the mod runs, while its assets are being read, or while anything here
  looks at the Overwatch install. Check first (`Get-Process Overwatch, Battle.net -ErrorAction SilentlyContinue`);
  if it's running, stop and say so. Don't close it yourself: the owner may be playing.
- Never attach to, debug, read the memory of, inject into or modify the Overwatch client, Battle.net or their files.
  Reading the install is read-only and offline (Overwatch closed).
- Research goes as far as it can without dealing with the tamper protection or reverse-engineering the encryption
  (owner, 2026-10-25): game data through DataTool/TankLib, and static reading of an archived copy of the executable
  (strings, type names, readable sections) are fine; unpacking or decrypting its protected parts, bypassing
  anti-tamper, or running it for analysis are not.
- Numbers (damage, cooldowns, speeds, ranges, ult cost) come from public sources: Blizzard's patch notes first, then
  the Overwatch wiki (read through its MediaWiki API: the HTML is Cloudflare-blocked). Each number in the code says
  where it came from, or that it's ours when no source gives one (see `KirikoStats`). Check the latest patch notes
  before changing a hero: Overwatch rebalances often.
- Overwatch assets (models, textures, sounds) are never committed or shipped. Like Lethalcraft's approach, they are
  read from the player's own install on their machine; the repo only ever holds original or placeholder assets.
  They live in `local-assets/` (gitignored) and reach the game as a local resource pack (`LocalAssetPack`): the dev
  client reads `local-assets/pack/`, a real install `<.minecraft>/overcraft/overcraft-assets.zip`. Build it with
  `py tools/build_asset_pack.py add <hero> <model file>` (Blender, headless) and `... zip`. `gradlew build` fails if
  the jar holds any model/mesh/audio file (`checkNoOverwatchAssets`): never weaken that check to get a build through.
- Until DataTool supports the installed build (#6), the owner may download ripped models as stopgaps (owner,
  2026-10-25). The owner downloads them, not Claude; they go in `local-assets/raw/` and follow the same rules.
- Don't put personal info (the owner's email) in request headers or URLs when fetching sources.

## Heroes: it should feel like Overwatch, not Steve (owner, 2026-10-25)
- With a hero selected and hero mode on, movement and controls are Overwatch's: 5.5 m/s with no sprint, near
  instant acceleration, Overwatch's jump and gravity, the hero's passives (Kiriko's wall climb). Falls hurt only from
  the hero's own safe height (Kiriko: her climb height), and not at all when an ability lands them (Winston's leap,
  Juno's glide) (owner, 2026-10-26, #35).
  The G key alone switches between hero mode and plain Minecraft, not the hotbar (owner, 2026-10-26, #26). Health
  scales 250 Overwatch HP = 20 Minecraft health (10 hearts).
- **Use the real data whenever possible (owner's rule and requirement, 2026-10-27).** Before implementing, assuming or
  inferring anything, look for the answer directly in the pulled game data (DataTool/TankLib, offline). Don't
  reassemble something (an emote's clip chain, a prop's attachment, a timing) by measuring or by eye when the data has
  it spelled out; measuring is the fallback only when the data has been searched and doesn't hold it. Emote work:
  `EMOTES.md`. The owner, on the data-driven emote props: "That is SO much higher quality ... Always do that for
  emotes and animations if at all possible." Anything borrowed or inferred gets checked against a contact the
  animation must make (`tools/emote_contacts.py`).
- Reimplement mechanics from the numbers and from how they behave in the real game, not by guessing. For
  Minecraft's side, read its decompiled source (`gradlew genSources`; 26.x is unobfuscated, Mojang's own names).

## Bugs: reproduce first, in the game
- **Reproduce every bug before fixing it.** Make it happen in the real game first (real input: the real mouse and
  keys, screenshots), and confirm what you see matches the report. Only then write the fix.
- **Validate the fix the same way:** the same in-game steps now behave correctly. Where possible, also show the old
  build (or the fix switched off) still fails, so the fix is what made the difference.
- A fix that's only been compiled, unit-tested or reasoned about isn't done. If a bug truly can't be reproduced, say
  so in its issue (what was tried) instead of claiming it's fixed.
- **Play every new feature by hand, like a player, before any test runs (owner, 2026-10-13).** Scripted tests and unit
  tests are not evidence that a feature works: on Lethalcraft a batch passed both, and the owner found about twenty
  bugs in a few minutes of play. For each new hero or ability: pick it on the select screen, check the HUD (ammo,
  cooldowns, ult charge), then use every button at real targets: zombies, skeletons, a
  villager or tamed wolf as the ally, on flat ground, on slopes, against walls, in water, at night. Compare the
  timings and numbers with the hero's sources (above) and Minecraft's decompiled code for Minecraft behaviour.
  Ten features take ten times one feature's play time. Write down what was tried, with screenshots.
  Then just play with them, as a player would: a real fight (a night in survival, a skeleton farm, a raid) with the
  hero, switching in and out of hero mode (G), because that's where a player finds the bugs.

## Hand play: before every merge, and before every release (owner's rule, 2026-10-15)
The owner, after Lethalcraft's 1.45.x work: "That manual playtesting you did made a MASSIVE difference." It is
required, not optional:
- **Before a feature or fix merges** (into `next` or `main`): play that one change by hand in the real game.
- **Before a version goes to `main`**: a general play of the whole build (not just what changed): join a world,
  pick each playable hero, fight, heal, die and respawn, switch heroes, leave and rejoin, play in normal survival in
  Minecraft mode (mine, craft, build, chests) to see plain Minecraft still works, at a player's speed and inventory (not
  god mode the whole time).

What that play looked like when it worked (do at least this, then go a step further each time: one more case, one
more angle, one more combination than the last person tried):
- **The real thing, not a stand-in.** Real mobs hitting the player and being hit, health read before and after each
  hit; real allies being healed; the real mouse buttons and keys, not a dev command.
- **Every way in.** Each hero picked from the select screen on join, from the hero menu key mid-game, after a death,
  after rejoining.
- **Look at it.** Take a screenshot at each step and actually open and read it: the HUD fits and says the right key;
  nothing else on screen broke. Compare with real Minecraft's behaviour and decompiled code.
- **The interactions around it.** Whatever it touches: abilities against each kind of mob, allies (players, pets,
  villagers), water, lava, ladders, boats, the Nether, a second player.
- **Write down exactly what was done**, in the commit or the issue: each step, what was expected, what was seen, the
  screenshots' names. Someone reading it must be able to repeat it, and must see what was *not* tried.
- When a step fails, that's the bug: reproduce it, fix it, then play the whole list again, not just the failing step.

## Tracking
- GitHub issues are the tracker (https://github.com/Thrasherop/overcraft/issues): `bug` (with a severity label or
  `easy fix`), `enhancement`, `balance`, `performance`. Reference the issue in the commit that fixes it (`Fixes #N`).

## Everything new must be reachable (owner's rule, 2026-10-12)
- Everything a batch adds can be got the normal way in a real game, not only through dev commands: a hero from the
  select screen, its abilities from its keys. Anything dev-only says so.
- **Complete, not just present (owner's rule, 2026-10-13):** a hero comes with its whole kit (weapons, abilities,
  passives, ultimate, and the role/sub-role passive), with its real numbers.

## Testing
- **Suite length (owner's rule, 2026-10-13):** a suite that takes more than 30 minutes is no longer practical: optimize
  it or drop tests until it fits. Anything over 45 minutes may run at most once a day.
- Offline unit tests: `gradlew test` (the hero math: units, timings, ult charge).
- **Batch work on branches; test in batches.** Each feature or fix goes on its own branch in its own worktree
  (`git worktree add ../overcraft_<name> -b feature/<name>`). While the game is busy (the owner playing), write and
  compile-check other branches (`gradlew build`). Each branch gets its hand play on its own. Once several are done,
  merge them into one integration branch (`next`) and test the batch once. Only `next` goes to `main`, after it passes.
- Start test sessions from a freshly launched game (`gradlew runClient`) and a fresh test world, so earlier runs'
  leftovers don't change the result.

## Shipping
- Every push to `main` makes a **test build** (a GitHub pre-release with the mod jar). Only push what has been tested
  in-game, and bump the version (`gradle.properties` `version`) plus CHANGES.md when behaviour changes.
- The **`production`** branch is for real releases. **Never push or merge to `production`**: the owner tests a test
  build and moves it there themselves.
- Don't run the game while the owner is playing on their other machine (same Minecraft account).

## Build notes
- Gradle runs itself on Java 25 (`gradle/gradle-daemon-jvm.properties`: Gradle downloads it into its cache), since
  Loom for 26.x needs it. The system Java can be older.
- `gradlew genSources` decompiles Minecraft (Mojang names); `gradlew runClient` starts a dev client.
