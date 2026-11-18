# Memory: what the local pack costs, and how to measure it (#166, 2026-11-18)

Up: [Techniques](../README.md) · [rendering](rendering.md) · [harness](../testing/harness.md) ·
runs: [two_client_2026-11-18](../testing/runs/two_client_2026-11-18/run.md), [genplay_0395](../testing/runs/genplay_0395/run.md)

## The numbers (one dev client, in a world, full pack)
| | committed | live heap (histogram total) |
|---|---|---|
| empty pack | 2.7 GB | |
| full pack, 0.3.88 | 8.5 GB | heap 3 GB, nearly full |
| 0.3.90 first-person models on first use | 7.5 GB | |
| 0.3.91 emote props on first use | 7.45 GB | |
| 0.3.93 clip key times shared, still bones one key | 7.27 GB | 1.31 GB |
| 0.3.96 rotation keys as shorts | | 1.20 GB |

Committed minus the JVM's own total (Native Memory Tracking, ~3.6 GB) is the graphics driver's copy of every texture:
textures cost twice, on the card and in system memory. A 5-minute night soak at 2 GB on terrain: ~1.7 GB in use after a
full GC, flat (no leak). Players: give the game 3-4 GB (README).

## How to measure (and not fool yourself)
- `jcmd <pid> GC.run`, then `GC.class_histogram` (its **Total** is the live data) and `GC.heap_info`. G1's "used" counts whole
  regions: at a 2 GB heap regions are 1 MB and large arrays waste the rest of theirs, so "used" reads ~0.3 GB over the live data.
- A reading right after a load or a reload is mid-flight (1.95 GB seen once): GC twice, 10 s apart.
- **Committed memory can't show a heap leak:** the JVM commits the whole `-Xmx` early. Count objects instead (`SkinnedModel`
  in the histogram) across reloads and hero switches.
- `-XX:NativeMemoryTracking=summary` (via `JAVA_TOOL_OPTIONS`) + `jcmd VM.native_memory summary` splits heap, metaspace, code.
- Test at what players get: `OC_XMX=2G` (build.gradle) launches client 1 at the launcher's default heap.
- `py glb_tex.py <hero_models>` (session scratchpad, reads only headers) estimates texture memory by kind of model.

## What took the memory, and what was done
- **Textures** (3.1 GiB estimated, x2 with the driver): hero bodies 1.2, first-person arms 0.86 (only the current hero's is
  drawn: now loaded on first use, two kept), kit effects 0.55, emote props 0.18 (on first use, 16 kept), weapons ~0.3.
  Hero bodies and kit effects on first appearance would save ~1.5 GB but show a hero as a plain player for a second: the
  owner's call (#166).
- **Clips on the heap** (800 MB of float arrays): each channel read its own copy of the shared key times (now one per accessor);
  most bones hold still in most clips (now one key: `Skeleton.sample` takes a lone key at any time); rotations are over half
  the keys (now shorts, x32767, 3e-5 off). Rest-filler channels are *not* dead: `HeroAnimator.driven` drops them for layers,
  but in a full-body clip they put bones back to rest.
- **Reloads** (F3+T, a resource pack or language change) held the old and new sets at once: out of memory at 2 GB. Now the old
  set goes first, on the render thread (`HeroModels.dropForReload`), and caches keyed by model are weak or cleared.
- **Model files** carry 35-52 MB of JSON each (~77,000 buffer views and accessors, one per clip channel); as a Gson tree that was
  several hundred MB at once, enough for a third reload at 2 GB to fail. `Glb.read` now streams those two arrays into int tables.

## Traps
- A cache keyed by a model (`Map<SkinnedModel, ...>`, an `IdentityHashMap` keyed by its pieces) keeps every model ever loaded
  alive; with models now freed and loaded again, that's a leak per load. Weak keys for classes with identity equality; a record
  key (value equality) needs clearing instead.
- Something that asks "does this model exist" must not trigger its load: `HeroModels.has`.
