# Widowmaker: asset and data stage (build 154088, 2026-11-17, #138)

Ids and names only; nothing extracted is in git. Research and scripts: `E:\Overwatch Archive\research\widowmaker\` (`ascripts\` this
stage's scripts, Ashe's retargeted; `work_assets\`, `resolved\`, `dump_anim\`, `dump_ssa\` its outputs; `sounds\` the sound decode).
The kit pass (`work\`, `work_kit\`, [widowmaker-map](widowmaker-map.md)) is a separate agent's; this stage only read its decompiles.

## Names the code uses

### Models (`hero_models/<name>.glb` in the local pack)
| name | model (skin / base) | entity | where it hangs |
|---|---|---|---|
| `widowmaker` | F0DF / 011F, look 012E | hero 001C (Gameplay3P), set 002A | mover 2.0 m, radius 0.5, crouch 1.25 |
| `widowmaker_1p` | F102 / 013E | 001D (Gameplay1P), set 0016 | 1P fields 45 / 1.5 / 1.05 / 1.0 |
| `widowmaker_kiss` | E003 / 01D4, look 01DA | 002A, set 0029 | hardpoint 000A, var 12BB (`EmoteData` slot 12BB, stow) |
| `widowmaker_hook` | 0711 (no skin swap), look 062E | the hook projectile 00D3 (005A #37) | thrown; contact set 003D; no clips |
| `projectile_widowmaker_mine` | 10A75 / 06F2, look 0605 | thrown 00AF (set 0074), stuck pet 00AE (set 0054, health) | contact set 008B |
| `widowmaker_clips`, `widowmaker_intro_clips` | clips only, merged into `widowmaker` | | |

Also on the gun: entity 13F6 (model F64A / 306C) on the Kiss's hardpoint 0068, var 1AFF (L what; not exported on its own).
The Seeker Mine perk's dart 6B75 (model 61CA, set 0074) is not exported.

### Clip names (tools/clipmaps)
- **3P `widowmaker.txt`**: `idle`, `idle_ads` (the held Take Aim stand), `run` + `run_forward_right`, `strafe_right`, `run_back_right`,
  `run_back`, `run_back_left`, `strafe_left`, `run_forward_left`; the same with `_ads` (`run_ads`, `strafe_ads_right`, ...) and their
  `add_` layers; `jump`, `jump_apex`, `fall` (and `jump_ads`, `jump_apex_ads`, `fall_ads`; `add_jump`, `add_jump_ads` the air layers);
  `crouch` (alias 56, over the stand; her crouch-walk tree 55 binds no clip: walk on `run`); `add_fire` (hip, 0.1 s, the 10/s
  interval), `add_fire_sm0` (L which shot); `ads_in`, `ads_out` (0.5 s each); `reload`, `reload_2`, `reload_ads` (1.5 s);
  `grapple_fire` (2D aim blend: `grapple_fire_up`, `_up_left`, `_up_right`, `_left`, `_right`, `_down`), `grapple_pull` (1.63 s) +
  `add_grapple_pull`, `add_grapple_end`; `add_mine_throw`, `add_mine_throw_2`, `add_mine_throw_ads`, `add_mine_throw_ads_2` (additive
  over the stand); `ult_cast`, `ult_cast_ads` (1.1 s); `melee`, `melee_down`, `melee_up`; `hello` + `add_hello_add`; the shared CC
  `knockdown_*`, `pulled_*`. Not extracted under F0DF: the select clips C0C5 / C188 (alias 477), the intro set 42F5's clips.
- **1P `widowmaker_1p.txt`**: `idle`, `run`, `jump`, `fall`, `ads_in` (0.5 s), `ads_hold`; `reload` (1.63 s), `add_reload_move`,
  `add_reload_end`; `grapple_fire` (1 s), `grapple_pull` (cat 52) / `grapple_pull_cat51` / `_cat53` / `_cat54` (005A sets the
  category on v471: #61 51, #59 52, #69 53, #91 54), `add_grapple_pull`, `add_grapple_end`; `mine_throw`, `add_mine_throw_end`;
  `ult_cast`, `add_ult_cast_end`; `melee`, `add_melee_end`; `hello`; `stun`. No 1P fire clip: the df102-weighted layer 140
  (`add_layer_140_*`) is the 1P fire layer (M: df102 also weights the 3P `add_fire`).
- **Widow's Kiss `widowmaker_kiss.txt`**: `ads_in`, `ads_out` (3P), `ads_in_1p`, `ads_out_1p` (the rifle unfolding / folding: the barrel
  bone 006A goes identity -> (-0.525, 0, 0, 0.851) and back, M), `reload`, `reload_2`, `reload_3` (3P, 1.5 s), `add_kiss` (L). The 1P
  reload 0159 is not extracted.
- **Venom Mine `widowmaker_mine.txt`**: `thrown` (in flight), `stick`, `arm` (L for the split).
- **Emotes `widowmaker_clips.txt`** (`--emotes`, `emote_<slug>_<n>`, 19 emotes) and the Heroic intro `widowmaker_intro_clips.txt`:
  `potg_intro`, `potg_intro_2` ... `_5` in the showcase set 0CA3's order (B6EE, BCF8, BD62, BCFA, BCFB).

### Emote data
`src/main/resources/assets/overcraft/emotes/widowmaker.json` (19 emotes; Widow's Kiss stowed by slot 12BB; no emote plays a clip on
the gun; props F935, 1307D, 566A, 136D5, 1366C, the shared 1A146).

### Sound events (tools/soundmaps)
- **Voice `widowmaker.txt`**: `widowmaker.voice.<moment>` as Ashe's (elimination, multikill, melee_kill, on_fire, respawn, melee,
  select, death, pain, resurrected, ult_ready, call_*, ping*, damage_boosted, nano_boosted), plus `widowmaker.voice.mine_kill` (11E3),
  `widowmaker.voice.ultimate` (her own 1869), `widowmaker.ult.friendly` (0085), `widowmaker.ult.enemy` (0492). Collectible voice lines:
  `widowmaker.voiceline.<slug>` (52, `voicelines/widowmaker.json`).
- **Effects `widowmaker_sfx.txt`** (own = in her head; `_3p` = everyone else):
  `widowmaker.kiss.fire` / `_3p` (hip); `widowmaker.kiss.fire_ads` / `_3p` (full charge, entity 0108), `fire_ads_mid` / `_3p` (50-99 %,
  004B), `fire_ads_low` / `_3p` (under 50 %, 0107) (001B #954 picks by c = (v2489 - 1) / 9); `kiss.whiz`, `kiss.whiz_ads`;
  `kiss.hit_enemy` / `_3p`, `kiss.hit_enemy_ads` / `_3p`, `kiss.hit_crit_ads` / `_3p` (L: the 0015 set), `kiss.hit_world`;
  `widowmaker.ads.in` / `in_3p`, `ads.out` / `out_3p`, `ads.charge` (10C9, a 1.55 s one-shot), `ads.charged` (164E, L);
  `widowmaker.reload` / `reload_3p`; `widowmaker.grapple.fire` / `_3p`, `grapple.attach` / `_3p`, `grapple.pull` / `pull_3p` (loops),
  `grapple.rope` (loop, L), `grapple.end` / `end_3p`; `widowmaker.mine.throw` / `_3p`, `mine.stick`, `mine.armed` (loop), `mine.beep`,
  `mine.trigger`, `mine.poison` / `poison_3p` (4.75 s beds); `widowmaker.ult.cast`, `ult.loop` (Infra-Sight held, loop),
  `ult.reveal` (L); `widowmaker.melee`. No Infra-Sight ping sound was found in her graphs.
