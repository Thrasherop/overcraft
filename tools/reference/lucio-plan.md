# Lúcio (#37): the plan

Numbers: `tools/reference/lucio.md` -> `LucioStats`. Branch `feature/lucio` (from feature/kiriko after 0.3.5).

## Assets (from the archived build, `E:\Overwatch Archive\extracted\154088\Heroes\Lúcio`, extracted 2026-10-26)
- `Skin\Overwatch\Overwatch\Models\00000000DB3F.00C` = Lúcio, third person (59.7k tris, 245 bones, 1.79 m; 172 clips).
- `00000000DB40` = first-person arms (78 clips). `00000000DBA3` = the Sonic Amplifier. `3E14` lion-dance and `5ED7`
  drum are emote props. Previews: `E:\Overwatch Archive\work\lucio\previews`; clip survey: `...\work\lucio\survey`.
- To do: clip map (`tools/clipmaps/lucio.txt`: idle, run 8 ways, jump/fall/land, wall ride left/right, shoot,
  soundwave, crossfade, amp it up, sound barrier, reload, melee, death, select_intro/hold), the 1P arms + gun clip
  map, `build_asset_pack.py add lucio`, sounds (weapon, abilities, the two songs' music loops) and voice lines
  (soundmaps), effects (the sonic rounds, the soundwave, the song auras, the barrier).

## Controls (Overwatch's, with the owner's mouse choice for Kiriko kept)
- LMB (the action bound to the left button): Sonic Amplifier, held for volleys. RMB: Soundwave. So both heroes attack
  with the left button. (Overwatch calls the Sonic Amplifier "primary fire"; the options screen's labels follow
  Kiriko's swap: ask the owner whether controls should be per hero.)
- Shift Crossfade, E Amp It Up, Q Sound Barrier, R reload, V melee, Space wall ride (hold, jumping at a wall).

## Server (the kit)
- Volleys of 4 rounds 0.125 s apart, a volley per 0.935 s, 20 ammo, reload 1.5 s (ammo back at 1.088 s).
- Soundwave: 45 damage, knockback 17 m/s along his aim, 7 m arc (ours 45° half angle), 5 s cooldown, 0.45 s lockout.
- Songs every 0.2 s to allies in 12 m with line of sight, lingering 1 s: heal 20/s (himself 12/s), speed +25%; Amp
  It Up 3 s: heal 56/s (himself too), speed +60%; 12 s cooldown after it ends. Crossfade swaps after 0.4 s.
- Wall ride (client movement, like Kiriko's climb): flag sent to the server for Soundwave Rider and animations.
- Sound Barrier: 2900 points, a small leap, 240% gravity, on landing 625 overhealth (absorption) to allies in 30 m,
  decaying over 6 s.
- Tactician: ult charge to 125%, at 75% rate above 100% (not passive charge), the excess kept after casting.
- Perks: Soundwave Rider (minor), Beat Drop (minor), Noise Violation (major), Accelerando (major); 2020 / 5050 XP.

## HUD
- Weapon panel: Sonic Amplifier ammo 20. Ability boxes: Crossfade (Shift: the current song's icon), Amp It Up (E),
  Soundwave (RMB). Ult: Sound Barrier. The HUD's abilities and weapons become per hero (now Kiriko's are hard-coded).
