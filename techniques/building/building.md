# Building and editing

Gradle, code edits and git on this checkout. Up: [Techniques](../README.md) · engine: [minecraft_26](../minecraft/minecraft_26.md)

## One local asset pack (2026-11-13)
- **The dev pack is the main checkout's: `E:\claude\mods\overcraft\local-assets\pack`.** Build every hero's assets
  into it (`OC_ASSET_PACK=E:\claude\mods\overcraft\local-assets\pack` from a worktree), and run worktree clients with the
  same `OC_ASSET_PACK`. `local-assets/` is gitignored, so each worktree has its own copy, and copies drift apart.
- How it went wrong: until 2026-11-11 the shared pack was `overcraft_ana/local-assets/pack`, which held Ana, Sombra,
  Brigitte, Juno, Sojourn, Sigma and D.Va. Pharah's, Tracer's, Cassidy's and Junkrat's builders built into the main
  checkout's pack instead. The 0.3.41 release play ran from that partial pack: no D.Va or Sigma model, no portraits, no
  effect or UI textures for those heroes (the owner saw it in the screenshots). The two packs were merged into the main
  checkout's: the shared pack's 834 missing files (models, 444 effect textures, UI icons, 65 glow tables) and its 8
  newer files, keeping main's sounds (all heroes, rebuilt 2026-11-13) and its lang file (a superset). The ana pack is
  now stale: don't build into it or run from it.
- Before a play, glance at the select screen: every hero should have a portrait and its ability icons. A letter instead
  of a portrait means the client is running from a partial pack.

## Gradle
- **`gradlew classes` doesn't compile the client source set.** It reported success on client code that didn't
  compile, three times. Check with `gradlew compileJava compileClientJava`, and `gradlew build` before a commit (it also
  runs the tests and `checkNoOverwatchAssets`).
- **Don't compile while the dev client runs.** It loads classes lazily from the build output: a class first used after
  a recompile is the new one and its neighbours the old, and the game crashed mid-test (`NoSuchFieldError` on a renamed
  field). Stop the client, then build or `runClient`.
- Gradle runs itself on Java 25 (Loom for 26.x needs it); the system Java can be older.

## Resources
- **Resource paths must be lower case.** A prop named by its upper-case id (`prop_105FB.glb`) threw out every resource
  pack at load. Lower-case ids in file names, and build ids with `Identifier.tryBuild` so a bad name skips just that
  model (`HeroModels`).
- Data files from the game (`assets/overcraft/emotes/`, `cloth/`) hold ids and numbers only, never Overwatch's
  assets.

## Editing safely
- **Python edits to Java files: replace exact strings, asserting each occurs once.** Twice a slice cut from one marker
  to the next took a neighbouring method with it. Check `git diff | grep '^-'` before building.
- **Bash heredocs with quotes or backslashes break:** a Windows path in a Python string (`\u...`) or a quote in a
  heredoc. Write the edit script with the Write tool, use forward slashes, and use raw strings for code.
- **Escapes in Windows paths become control characters:** `\a`, `\f`, `\r` and `\000` in comment paths passed through a
  heredoc-fed Python string came out as a bell, a form feed, a carriage return and a NUL, in six files (found
  2026-10-28; git then took mercy-plan.md for binary). Scan after such edits: any byte below 32 but tab, or a CR not
  followed by LF.
- **JOML:** `Matrix4f.scale(s)` is a scale transform, not a scalar multiply. For weighted sums of matrices (skinning),
  use `fma4x3`.

## Git on this checkout
- Use `git -c safe.directory=E:/claude/mods/overcraft` ("dubious ownership").
- Commit messages go in from a file or a quoted heredoc (`git commit -F -`); PowerShell mangles quotes in a here-string.
- Commit only after playing it in game; reference issues (`Fixes #N`).

## Asset packs across worktrees (0.3.17's missing cat, 2026-10-28)
- Each worktree has its own `local-assets/pack`. Merging code branches doesn't merge packs. 0.3.17 and 0.3.18 shipped
  Jetpack Cat's colourless pre-fix models and none of her data pass's sounds (emotes, paw hits, voice lines): the pack
  was assembled by copying one worktree's pack over another's, and the copy (robocopy's default, which also copies
  OLDER files) put the old models back. The owner found it playing (#50).
- Before zipping a release: compare the packs by content, take each file from the worktree that last exported it,
  and rebuild what's generated from merged sources (`build_asset_pack.py sounds`, which rebuilds every hero's sounds
  and sounds.json from tools/soundmaps). Then open the zip and check the files that changed (a model's colour textures,
  the sound events' count).
- And look at the result: a "the cat is visible" check that saw only eyes and a tongue in an empty jetpack passed
  it. Look at the whole model, from every side, against the hero's portrait.

## A real install after fix/polish-2 (0.3.31 needs its pack rebuilt)
`Overcraft.ASSETS_NEEDED` is 0.3.31: an older zip shows its assets' version in red in the pause menu (`f_4_pause`). In this order, each over the player's extraction: `py tools/build_asset_pack.py effects <dirs>` (bakes `mercy_wing_glow`), `py tools/build_asset_pack.py glows <roots>` (`glow_maps.py`: glows and `hero_models/colour/`, the dreads), then `py tools/build_asset_pack.py zip`: **zip doesn't run the others** (critique_parts). `zip` lists what's missing (`missing()`, which now knows these two). With an old pack: Mercy's wing glow falls back to a very faint soft ball; the dreads stay undrawn and the log warns. Played: the old pack's red version here; the fallbacks and a real install's zip were **not** played. The dev pack at `overcraft_ana/local-assets/pack` has these two files, placed by hand and still says 0.3.30.
