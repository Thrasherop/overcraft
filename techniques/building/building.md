# Building and editing

Gradle, code edits and git on this checkout. Up: [Techniques](../README.md) · engine: [minecraft_26](../minecraft/minecraft_26.md)

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
- **JOML:** `Matrix4f.scale(s)` is a scale transform, not a scalar multiply. For weighted sums of matrices (skinning),
  use `fma4x3`.

## Git on this checkout
- Use `git -c safe.directory=E:/claude/mods/overcraft` ("dubious ownership").
- Commit messages go in from a file or a quoted heredoc (`git commit -F -`); PowerShell mangles quotes in a here-string.
- Commit only after playing it in game; reference issues (`Fixes #N`).
