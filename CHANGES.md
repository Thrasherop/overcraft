# Overcraft: changes by day

A day-by-day log of development: releases, features, bugs fixed, investigations, and what's waiting on the owner.
Newest day first. Issue numbers are on https://github.com/Thrasherop/overcraft/issues.

## 2026-10-25

**Kiriko, first pass** (branch `feature/kiriko`, not on `main` yet: not played in-game). Compiles, unit tests pass
(units, run speeds, jump apex 0.98 m, 30 m/s fall cap, 0.1 s ground acceleration, Suzu's 7 s straight-up flight).
The dev client launched with the mod loaded, but couldn't be played this session (desktop control needs the owner's
approval in a live message), so **none of this has been played by hand yet**.
- Hero select screen on joining a world (Kiriko playable; Lúcio, Sombra, Illari shown as coming soon); H within 5
  blocks of your bed (or world spawn without one) to change heroes (owner); the pick on joining is free.
- Hero mode: G toggles (owner), or hotbar slot 1. The hero weapon lives in slot 1; vanilla hotbar, hearts, food,
  armor and XP bar give way to the hero HUD (health in Overwatch numbers, ammo, cooldowns, ult %).
- Overwatch movement in hero mode: 5.5 m/s (90% back, 93.3% diagonal back), ~0.1 s accel/stop, jump 5.72 m/s
  (0.98 m), gravity 16.7 up / 17.5 down easing to 30 m/s, Ctrl crouch 3 m/s, no sprint, no fall damage. Air control
  value is ours (none published). The jump uses trapezoid integration: Minecraft's gravity-then-move order peaked at
  0.83 m (caught by the unit test).
- Kiriko (numbers from the Oct 2026 patch notes and the wiki, cited in KirikoStats): kunai LMB (60, 2x head, 0.5 s,
  15 ammo, 90 m/s), Healing Ofuda RMB (12 x2, 7/s, 10 ammo, 0.9 s reload, 18 m/s, 30 m homing lock), Medic 40%
  self-heal, Swift Step (30 m, 7 s, 0.2 s cast, through walls), Protection Suzu (14 s, 4 m, 80 heal, cleanse, 0.5 s
  invulnerable), wall climb (8.3 m/s, 1 s, once per airtime), Kitsune Rush (2725 points, 32.2 x 10 m corridor for 8 s:
  +30% speed, +50% attack/reload, 2x cooldowns, 1 s linger). Global regen after 6 s.
- Ult charge: 5/s, 1 per damage/healing, and advancements (owner): task 10%, goal 25%, challenge 50% of the cost.
- Placeholder art (tools/gen_textures.py) and vanilla sounds until hero assets come from the owner's install.
- Local Overwatch asset pack (#6): the mod mounts `<game dir>/overcraft/overcraft-assets.zip` (dev: `local-assets/pack/`)
  as an always-on resource pack; `tools/build_asset_pack.py` converts a model with Blender into it (checked with a
  test cube); `gradlew build` fails if the jar contains a model/mesh/audio file (checked: a .glb in resources failed
  the build). Nothing renders the hero model yet: that comes with the first real model. DataTool v2.25.0.0 can't read
  the installed build 154504 yet (supports up to 154088): waiting on upstream.
- Issues #1-#5 filed (Juno, Sojourn, communication wheel, emotes, voice lines).

Project started. Fabric for Minecraft 26.3 (Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19); 26.x ships
unobfuscated, so there are no mappings to pick. CLAUDE.md carries over Lethalcraft's working rules.
