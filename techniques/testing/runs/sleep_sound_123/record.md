# #123: one sleeper's wake stopped every sleeper's bed (2026-11-13, fix/sleep-sound)

Up: [Techniques](../../../README.md) · [sounds](../../../sound/sounds.md#stops-by-name) · [heroes/ana](../../../heroes/ana.md) · issue #123

**Cause:** `Sleep.wake` sent `ClientboundStopSoundPacket` for `ana.sleep` and `ana.sleep_target` to every player within 53 m. A stop
names a sound, not one copy of it, so every bed that listener had going was cut.
**Fix:** the sleeper's own (`ana.sleep_target`) is stopped for that sleeper only. The others' (`ana.sleep`) is stopped only for listeners
that no other sleeper's bed reaches (`Sleep.stopsBed`; `SleepSoundRuleTest`). The 53 m is now cited: the bed's 45 m (3866), plus the 8 m
past that which `HeroSounds.at` sends it. Same rule as `Anesthetic.cleanse`. Dev: `bot fix 123 off` brings back the old stop.

## The play (this branch, uncommitted on 5010167; one client, fresh flat world `sleep123`, night, `spawn_monsters false`)
Ana was picked (`hero ana`, the payload the select screen sends) and hero mode turned on. The Sleep Dart was fired with the real key
binding (`key ability_1`), aimed by `execute anchored eyes facing`. The targets were two NoAI husks 8 m away, 6 m apart. Darts were
1.3 s apart (`/overcraft reset` between). A was woken with `/damage` 1. `bot playing` (the sound engine's running list) was polled every
0.25 s. Raw: [sleep_play_1.txt](sleep_play_1.txt); script [scripts/sleep_snd.py](scripts/sleep_snd.py); screenshot
`run/screenshots/sleep123_two sleepers_on.png` (gitignored): both husks, Ana's HUD, the subtitle "Target wakes".

| Case | Seen |
|---|---|
| One sleeper, fix on (control) | `ana.sleep` gone by +0.52 (the wake at +0.5); `ana.sleep.wake` played |
| Two sleepers, **fix off** (before) | `ana.sleep` gone by +0.79, though B slept on until about +4.2 (its wake played then): **the bug, reproduced** |
| Two sleepers, **fix on** (after) | `ana.sleep` plays on to +3.73, gone at +3.99 (B's 4.75 s file ending, 0.25 s before its 5 s sleep ends), then B's wake |

`playing` lists names, not copies. The bed still sounding past +2.4 is B's, since A's file, started 1.3 s earlier, would have ended by then.

**Not tried:** a player as the sleeper (their own `sleep_target`), a second real client as the listener, a rifle shot as the waking hit
(`/damage` takes the same `onDamaged` path), a listener between 45 and 53 m.
**Left as it is (ours):** a listener with another bed in reach hears the woken one's bed out to its file's end (at most about 4.25 s past
the wake). A stop per copy would need a per-entity sound and a client-side stop, which we don't have.
