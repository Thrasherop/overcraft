# Kiriko's sound effects in the game data

Which of Kiriko's real Overwatch sounds belongs to which gameplay event, how sure that is, and what is missing. Read
2026-10-26 from the archived build 154088 (prometheus-2_25_0_0-154088): the extracted files in
`E:\Overwatch Archive\extracted\154088\` and, offline and read-only through TankLib (`Online=false`, Overwatch and
Battle.net checked closed before every run), the sound definitions (.02C), Wwise soundbanks (.043), contact sets
(.049/.04A) and clip animation effects (.08F) of `E:\Overwatch Archive\build-154088`. Nothing was extracted, decoded
to disk or copied: only text was written. The audio stays in the owner's archive and reaches the game through the
local asset pack.

The proposed map is `tools/soundmaps/kiriko-proposed.txt` (one comment per id with the evidence). Scripts, raw dumps
and per-file measurements: `E:\Overwatch Archive\research\kiriko-sounds\` (see "How it was done").

Ids below are hex indexes: `7122` = `000000007122.00D` (effect), banks are `.02C`, sound files `.03F` (the extracted
`<id>.03F.ogg`, written with their last 6 digits, `2B08D4` = `0000002B08D4.03F.ogg`).

## How Overwatch wires a sound (what the data shows)

- An **effect** (.00D) holds sound components (`ECSO`) that each name a **sound** (.02C, `STUSound`). Abilities play
  effects from their statescript graphs; projectiles carry theirs in `STUProjectileVisualComponent` (hit on a target,
  world impact, extra) and in child entities (in-flight effects); clips play sounds at set times through their
  **animation effect** (.08F).
- A `STUSound` names a **Wwise soundbank** (.043), a Play event id, a Stop event id (non-zero = a **loop** that the
  game stops), its WEM files (.03F = the extracted .ogg) and some flags:
  - `m_D6EDF627` = **audience**: 1 on the owner's copy, 2 on the world copy. Sounds come in pairs with the same files
    (Ofuda flight 770C=1 / 8259=2, Suzu flight 7C3D=1 / 96F7=2, Suzu burst 76FD=1 / 96F8=2...), and the 1-copies
    have no range and 1P mixing tags. Below, **1P** = what Kiriko's own player hears, **3P** = everyone else. When
    only a 2-copy exists, assume everyone hears it. (On 1P clip sounds the field is sometimes 2; there it does not
    matter.)
  - `m_ACB34EFC` = squared maximum distance (all values are squares: 900 = 30 m, 4900 = 70 m, 10000 = 100 m; -1 =
    no limit). `m_ACA7FBC8` = most likely a retrigger guard in seconds (0.05-0.5; 16 on the torii gate loop).
- The Wwise bank's hierarchy (HIRC) was read for every bank used here. **Event -> Play action -> container -> sounds**.
  A **random container** picks one file (= Minecraft variants); a **layer container** plays all its children at once
  (= several Minecraft sounds played together: the `_b`, `_snap`, `_sub`... ids in the map). The WEM id of each
  sound object was tied to its .03F by matching the 03F asset's byte size to the object's media size (unique per
  bank in all but 3 cases, placed by hand from their measurements).
- **Projectile impacts on surfaces** come from **contact sets** (.049): per contact type and surface material (.03A)
  a contact effect (.04A, an ordinary effect file). Reading the four sets: contact type **0 = body hit, 1 = critical
  hit, 2 = world hit**. Evidence: type 2 lists ~23 world materials for the Kunai; types 0/1 list only six character
  materials; the Suzu's type 0 and 1 entries are identical (it cannot crit) while the Kunai's differ (an extra deep
  layer); the Ofuda (cannot crit) has no type 1. Material 0001 is the default.
  Sets: Kunai 0738, Ofuda 0731, Suzu 0764, the Kitsune fox 0740 (types 10/11 only: most likely its footsteps).

## Results per event

Confidence: **high** = the graph or clip plays exactly this bank at this event; **medium** = right family, one link
inferred; **low** = best available stand-in.

| Event | Map id(s) | Source | Files | Confidence |
|---|---|---|---|---|
| Kunai throw | `kiriko.kunai.throw` + `_b` (1P), `throw_3p` + `_3p_b` (3P), `throw_hand` (1P arm foley) | effect 7122 after the Kunai WeaponVolley nodes; 1P clips A46E/A6DA | 17+15 / 16+8 / 24 | high (hand: medium-high) |
| Kunai hit on a body | `kiriko.kunai.hit` + `hit_sub` | contact type 0 -> 47D0 -> bank 8BCE (generic layers only) | 8 + 7 | low-medium |
| Kunai headshot | `kiriko.kunai.headshot` (+ `hit`, `hit_sub`) | contact type 1 -> 47D6 -> bank 8BD2 | 10 | low-medium |
| Kunai hit on terrain | none | 47DC -> 826E/826F not extracted | - | - |
| Kunai reload | `kiriko.kunai.reload_1`..`_5` (timed steps) | alias 0x004 clips A810 (1P) / ABCF (3P) | 9/9/3/3/2 | medium |
| Kunai in flight (extra) | `kiriko.kunai.flight` (loop) | projectile 4103 child effect 3BF2, bank 7CCD | 1 | medium |
| Ofuda throw | `kiriko.ofuda.throw` (1P, per volley), `throw_3p` + `_3p_b` | effect 409D after the homing volley; 3P start clips | 16 / 17+16 | high / medium |
| Ofuda heal on an ally | `kiriko.ofuda.heal` + `heal_snap` + `heal_sparkle` + `heal_sub`; optional `heal_thump` | effect 3EC4 (weapon graph, on the hit target); 3DA6 (heal subgraph) | 16+20+4+8 / 18 | medium |
| Ofuda hit on terrain | `kiriko.ofuda.hit_block` + `hit_block_snap` | contact type 2 -> 4534 -> bank 7890 | 16+20 | high |
| Ofuda reload | none of its own (shared reload, use the kunai steps) | - | - | - |
| Ofuda in flight (extra) | `kiriko.ofuda.flight`, `flight_seeking` (loops) | effects 2F93 / 7E2D | 7 / 14 | medium |
| Suzu throw | `kiriko.suzu.throw` (1P), `throw_3p` | effect 3D4C after the Suzu volley | 3 / 2 | high |
| Suzu flight | `kiriko.suzu.flight` (one-shot bell) | child entity 4298, effect 3E6F | 2 | high |
| Suzu burst | `kiriko.suzu.burst` + `burst_shimmer` + `burst_bell` (STOPGAP) | real banks 76FD/96F8/BDE2 not extracted; souvenir clip C18A | 4+1+2 | low-medium |
| Suzu allies cleansed | none (visual overlay only) + `kiriko.voice.suzu` | effect 3398 has no sound | - | high that there is none |
| Swift Step cast | `kiriko.swift_step.cast` + `cast_sub` (1P) | effect 3D44 at the depart animation, bank 76F7 | 3 + 1 | medium-high |
| Swift Step departure | `kiriko.swift_step.depart` | effect 3CC7 (and 3D44's 3P bank), bank 76E6 | 3 | high |
| Swift Step arrival | `kiriko.swift_step.arrive` | effect 3CC5, bank 76E5 | 3 | high |
| Wall climb, per hand | `kiriko.climb.step` | 1P climb clip AA7E at 0.267 and 0.469 s | 9 | high |
| Wall climb start | `kiriko.climb.start` | effect 289 at the climb state | 4 | high (generic) |
| Wall climb, over the top | `kiriko.climb.top` | 1P clip AA7C at 0.033 s | 7 | high |
| Kitsune Rush summon | `kiriko.rush.summon`; `rush.cast` + `cast_sub` (1P), `cast_3p` | effects 4513 and 3DBE at the cast | 1; 1+1; 1 | medium-high / medium |
| Kitsune Rush fox | none of its own (closest: `rush.path_start`) | fox entity has no sound; its footsteps (8240/8241) not extracted | 4 | low |
| Kitsune Rush corridor | `kiriko.rush.path` (loop bed) + `path_start` | path effect 2E87, bank 7924 | 4 + 4 | medium |
| Kitsune Rush gates | `kiriko.rush.gate_1`..`_6`; optional `gate_loop` | gate graph 31EE, effects 3DC8..3DCE; gate effect 3C82 | 1 each; 2 | high; medium |
| Kitsune Rush end | none found (`rush.pulse` maybe) | effect 40C6 | 1 | low |
| Quick melee swing | none | 0A2C and the melee clips have no sound | - | - |
| Quick melee hit | `kiriko.melee.hit` + `hit_sub` (generic) | generic graph 0043 effect 6BD4, bank 5D97 | 8 + 7 | medium |
| Voice: Swift Step | `kiriko.voice.swift_step` | stimulus FC0 | 12 | high |
| Voice: Suzu | `kiriko.voice.suzu` | stimulus FC5 | 12 | high |
| Voice: healing | `kiriko.voice.heal` | stimulus FC4 (heal subgraph 2128) | 25 | high |
| (extra) ability not available | `kiriko.ability.denied` | effect 0186 = the "denied" effect of Kiriko's StateAbility nodes | 1 | medium |

### Kunai

- **Throw.** Weapon graph 2171 (shared by both weapons) has four WeaponVolley nodes. #17 and #1335 spawn a projectile
  with a 5 s life and explosion rings (the Kunai; #1335 sits behind a condition: likely the fiery perk variant 5C3A);
  #946 spawns a homing projectile with a 10 s life (the Ofuda). Effect **7122** follows #17/#1335 directly.
  7122 holds two banks: **763B** (audience 1, no range) and **763C** (audience 2, 100 m), each a **layer container of
  two random containers**:
  - 763B: A = 17 files 0.43-0.64 s, bright swish (centroid ~7.1 kHz, 54% of the energy above 6 kHz); B = 15 files
    0.29-0.39 s, a punchier release (4.4 kHz). Both play on every throw.
  - 763C: A = 16 files 0.36-0.57 s (4.3 kHz); B = 8 files 0.20-0.47 s, low thump (65% under 300 Hz).
  - The 1P throw clips (A46E one arm, A6DA the other, 0.47 s each, alias 0x16F alternating) also play an arm foley
    at 0.0 s: banks 78BE / 78BD, each a layer of a 0.40-0.57 s and a 0.19-0.39 s random set, low (~70% under
    300 Hz). The map's `throw_hand` takes the longer layer of both clips. The 3P throw clips have no sound.
- **Hit on a target.** The projectile's hit effect 13B2 only plays sub-effect 4057 = bank **8DE7** (33 files: a layer
  of 10 + 16 + 7, Wwise bank 22156203): **not extracted**. Contact type 0 adds 47D0 = bank 8BCE (+ 8FEB within 12 m,
  8FF3 up to 500 m, both not extracted). 8BCE's extracted parts are generic flesh-impact layers shared with quick
  melee: 8 stereo punches (0.28-0.30 s, peak +2.7 dBFS, 95% under 300 Hz) and, in a second Play action, 7 mono sub
  hits (0.13-0.31 s, centroid 43 Hz). Its two other parts (7 files 2ABC4B..., and 11 files 2AC663... from Kiriko's own
  impact bank) are not extracted. So `kunai.hit` is a believable but generic body hit.
- **Headshot.** Contact type 1 = 47D6 = bank 8BD2: the same punches and subs plus **10 deep stereo thumps** (0.29-0.63
  s, almost all energy under 60 Hz) in place of the 7 unextracted files. That deep layer is the crit-only part:
  `kunai.headshot`. The "crit ding" the shooter hears in Overwatch is a global UI sound, not in Kiriko's data.
- **Hit on terrain.** Contact type 2 on the default material = 47DC = banks 826E/826F (11 files, Kiriko's impact bank
  1758105412), plus 22 per-surface effects (47DD-4FED: metal, wood, glass...): **none extracted**. The terrain effect
  3BF4 itself is visual only.
- **Reload.** Alias 0x004 (3P ABCF 1.33 s, 1P A810 1.47 s) is the reload, not an equip: Kiriko has no weapon switch,
  its length matches `KirikoStats.KUNAI_RELOAD` (1.3 s), and the weapon timing floats in the entity (0.36, 0.57, 0.9)
  match the 1P sound times. Timeline:
  - 1P (A810): 828F at 0.00 s (cloth, quiet), 8290 at 0.21 s (bright tick), 9273 at 0.36 s, 9274 at 0.57 s, 9275 at
    0.91 s.
  - 3P (ABCF): 9605 at 0.16 s, 9606 at 0.34 s, 9607 at 1.14 s. These hold the same files as 9273/9274/9275.
  Map ids `kunai.reload_1`..`_5` are those steps; play them on that schedule (not as one sound).
- **Extras.** In flight: child effect 3BF2 = bank 7CCD, one 6.3 s low hum with a stop event (a loop, 30 m, owner
  copy). Effect 3E87 (projectile field `m_2170A015`, bank 7CCB, 6 files, 60 m, special distance values) is most
  likely the whiz-by heard near a passing kunai: not extracted. The fiery perk kunai's explosion 6099 (banks
  BBE4/BBE5/BBE6, 106 files): not extracted.

### Healing Ofuda

- **Throw.** Effect **409D** follows WeaponVolley #946 (the homing Ofuda) behind a condition: bank **7891**, audience 1,
  one random container of 16 stereo files 0.93-1.48 s (centroid 970 Hz, tonal 150-400 Hz, ~0.5 s above -20 dB). One
  per volley. Others hear bank 78F2 (= 825B, same files), played at 0.0 s of the 3P Ofuda start clips B011 (alias
  0x875) and AD90/AE1B/AE1C (0x16E): a layer of 17 stereo 0.35-0.48 s low parts and 16 mono 0.52-0.87 s airy parts.
  The 1P throw clip B0E1 only adds quiet cloth foley (bank 946E).
- **Heal on an ally.** The dedicated heal-impact effects (projectile hit effects 4004 teal / 40C0 gold -> banks 770E
  owner, 8258 and 78F3 others; 20 + 39 files) are **not extracted**. What is extracted, all from Kiriko's impact
  Wwise bank 1758105412:
  - **3EC4**: client-only, in the weapon graph, started by a game message and aimed at var 1950 = the same target the
    heal subgraph runs on. Bank 7DF9 is a **layer container of 8 parts**: 16 mono tonal tones (0.56-0.83 s, 330-1400
    Hz, slow 0.17 s attack, quiet: peak ~-15 dB), 20 mono paper snaps (0.26-0.39 s, instant attack, 5-10 kHz, peak
    ~-1 dB), 8 stereo sub thumps (0.9-1.15 s, <250 Hz), 4 single stereo shimmer tails (2AC665 1.6 s, 2AC66F 1.65 s,
    2AC7CE 1.8 s, 2AC7B4 0.65 s) and 2 short clicks. This is the richest heal-hit sound found: map ids
    `ofuda.heal` (tones), `heal_snap`, `heal_sparkle` (in Overwatch all four tails play; one Minecraft id can only pick
    one), `heal_sub`.
  - **3DA6**: the heal subgraph 2128 (played on the healed target via PlayScript #781; it also says the FC4 healing
    lines) plays it 0.2 s after the heal: 18 stereo low thumps 0.67-0.86 s, 0.2 s retrigger guard. Optional
    `ofuda.heal_thump`.
  - The same subgraph holds effect 451B and the weapon graph holds 3E00: both play bank 8E5B/792F, a layer of six
    3.8-8 s shimmering beds with a stop event, shared with generic graph 2223: a generic "being healed" loop. Not
    mapped.
  Which audience hears 3EC4 (the healer, the healed, or both) is not resolved: the variable names are hashes.
- **Hit on terrain.** Contact set 0731: type 2 (world) default material -> 4534, type 0 (character) -> 4533, both
  = bank **7890** (audience 1, 90 m): a layer container of exactly the tones (16) + snaps (20) of 3EC4. The files are
  only extracted under 3EC4's folder, so `hit_block` and `heal` share them. Material 0033 has its own effect 4530
  (banks 4870/4871, 12 files): not extracted.
- **Reload.** No Ofuda-only reload: one reload animation (alias 0x004) in the shared weapon graph. The 1P Ofuda chain
  clips (A49D, B088, A84C) only carry the quiet foley banks 828F/946E at 0.07-0.18 s.
- **In flight (extra).** Teal Ofuda 2F93: bank 770C/8259, a layer of 4 low swells (4.1-5.3 s, 1.5 s attack) and 7 airy
  flutters (2.6-3.4 s), very quiet (rms -47 dB), with stop events: a loop to stop at impact, 30 m. Gold (homing)
  Ofuda 7E2D: bank 770D/825A, 14 bright loud flutters (peak -3.5 dB) + the same 4 swells.

### Protection Suzu

- **Throw.** Suzu graph 30D8 plays effect **3D4C** right after WeaponVolley #71 (projectile 3FD3, the bell). Bank 76FF
  (audience 1) is a layer of 2 bell jingles (2AF401/2AF490, 1.2 s, tonal ~900 and 3100 Hz) and 3 whooshes
  (0.58-0.76 s, low-mid). Bank 3850 (audience 2, 70 m, its own Wwise bank) = 2 bright stereo jingles 0.93-1.1 s.
  The map's `suzu.throw` is the whoosh layer only, because the bell is `suzu.flight`.
- **Flight.** The projectile's child entity 4298 plays effect 3E6F: banks 7C3D / 96F7 = the same two bell files. No
  stop event; the sound component lasts 0.40 s of a 6 s effect, so it is a one-shot at launch that follows the
  bell, not a loop.
- **Burst.** The projectile's impact effects 360C (world, with the decal) and 3D07 (the other impact slot) both play
  banks **76FD** (audience 1) and **96F8** (audience 2), and 360C also **BDE2** (150 m range, 0.3 s guard: the bell
  heard across the map). All three are one random container of the same **5 files 2B3253, 2B31AE, 2B324C, 2B3247,
  2B3255: not extracted.** Stopgap: the Protection Suzu **souvenir** clip C18A (1.87 s) re-enacts the ability: at
  0.0 s the in-game throw files (banks 94DC/94DA = the bell and the whooshes), at 0.6 s a decal, a light and banks
  94D9 (layer: 4 low booms 1.9-2.3 s + a 1.36 s shimmer + 2 short tails) and 94DB (2 bell rings 1.7-2.0 s). Those
  are the map's `suzu.burst`, `burst_shimmer`, `burst_bell` (souvenir mix, 15 m range in Overwatch).
- **Cleansed allies.** Effect 3398 is a material overlay only. Effect 6089 (the area entity 781F, 5 s) plays banks
  21D9/21DA: one 6.2 s low hum with a stop event; what the 5 s area is (a perk?) is not resolved, not mapped.
- The Suzu contact set (0764) gives direct body hits the generic impact layers (bank 8E47 etc.) and material 00E1 its
  own effect (banks 4356/435C, not extracted).

### Swift Step

- **Cast.** Graph 30DB plays effect **3D44** when the depart animation (alias 0x7F8 = ABBA 3P / A591 1P, start time
  0.15 s) starts. Bank 76F7 (audience 1): a random set of 3 stereo 2.3-2.7 s low, loud whooshes (peak +0.8 dBFS) and,
  in a second Play action, one mono 1.38 s sub (2B31B4). Its audience-2 bank is 76E6, the departure sound. So the
  owner hears 76F7 and everyone else 76E6 when she casts; the hand sign itself has no separate sound, and the clips
  have none.
- **Departure.** Effect **3CC7** (the petals and rings on Kiriko as she leaves), played after a wait: banks DA8E / 76E6
  / DA8C, all audience 2, all the same 3 stereo files (2.7-2.9 s, low).
- **Arrival.** Effect **3CC5** at the destination: banks DA8D / 76E5 = 3 stereo files 2.1-2.2 s.
- 3CC4 (the flower on the target ally) has no sound. 009B and 5F58 (generic bank, after a UX presenter switch) are
  probably HUD feedback; not mapped.

### Wall climb

- **Per hand.** The 1P climb clip AA7E (alias 0xA0, 1.033 s cycle, loops) plays bank **81E4** at **0.267 s and
  0.469 s** of each cycle: 9 mono 0.34-0.48 s bright scrapes, quiet (peak -13 dB). The 3P climb clip AC1F has no sound.
- **Start.** Graph 30D9 plays effect **289** when the climb state starts: bank 11F9 (4 stereo 0.32-0.39 s low thumps,
  peak +0.8 dBFS) / 11FA (owner: 3 of those + a 0.18 s mono tick). 289 is a low, shared effect id in a generic
  movement Wwise bank: the shared wall-climb sound.
- **Over the top.** The 1P hop clip AA7C (alias 0x9C) plays bank **81E5** at 0.033 s: 7 stereo 0.49-0.71 s.

### Kitsune Rush

- **Cast.** Ult graph 30DA, in the cast's wait state (with the voice line), plays:
  - effect **4513** = banks 8E57/8E58: one 4.5 s sub-bass swell that rises for **2.0 s** to its peak and fades by
    4 s: the summon build-up. Start it at the cast.
  - effect **3DBE** (held 12 s): owner bank 7E29 = 2B3261 (4.7 s, hard onset, ~2.4 s decay) + 2B3278 (2.6 s mono sub)
    played together; others' bank 78F1 = 2B3267 (6 s, 75 m).
  The cast effect 3C5F (smoke) has no sound.
- **Fox.** The fox entity 3F54 carries no Kiriko sound (Combo only finds generic hero banks). Its contact set 0740
  has contact types 10/11 (most likely footsteps) -> banks 8240/8241, 31 files: **not extracted**. The closest
  extracted "rush" sound is the corridor's opening layer (`rush.path_start`).
- **Corridor.** Path effect **2E87** (8 s, held by graph 33DB) = bank 7924, two layer containers with a stop action:
  beds 2B3279 (10.8 s, level flat from start to end: the best single loop), 2B31B1 (10.7 s, slowly rising), 2B3206
  (8.3 s sub), 2B3248 (4 s); and opening one-shots 2B31B3, 2B3277, 2B3243, 2B3250 (2.9-8.3 s, decaying). In Overwatch
  all eight play together.
- **Gates.** Gate graph 31EE switches on the gate's index to one of six effects **3DC8, 3DC9, 3DCA, 3DCB, 3DCD, 3DCE**,
  one 2.0-2.2 s stereo chime each. Their lowest partials rise in that order: **1042, 1242, 1540, 1833, 2057, 2411
  Hz** (an ascending pentatonic-like run), so gate n plays chime n; the switch's case values were not decoded, the
  order comes from the pitch. Each gate also holds effect 3C82 (10.67 s) = bank 78EF, a looped layer of 2B0A3F (6.3 s,
  the kunai's flight hum) and 2B3249 (8.4 s), with a 16 s retrigger guard (one hum for the whole row); a third file
  2B3259 (4.1 s) in that bank is not in its play tree.
- **Slabs / pulse.** The floating talisman slabs 4013/4014 (set through config vars) play bank 8215 (18 mono 1.6-2.7 s
  low). The ring pulse 40C6 (client-only, after three waits) plays 2B327B (1.6 s). No sound marks the end of the rush
  that could be identified.

### Quick melee

- Generic graph **0043** (all heroes). Its cast effect 0A2C has no sound and the melee clips (3P AF57/AFB8/AFB9, 1P
  A529 and the generic 0C63) carry none: **no swing sound found**.
- **Hit.** Effect **6BD4** after the hit tracking (StateTrackTargets): bank 5D97 (generic melee Wwise bank) = a layer of
  8 stereo punches (0.28-0.30 s, peak +2.7 dBFS) and 10 deep thumps, a second action with 7 mono subs, a third with
  one 1.2 s tail (2AFEB1). The map takes the punches (`melee.hit`) and the subs (`melee.hit_sub`). The kunai body
  hits reuse these same punch and sub files.

### Voice

From `voice_by_stimulus` (voice set 215.05F), English lines:
- **FC0, Swift Step** (graph 30DB), 22 lines; mapped 12: "I'm here", "(Japanese) I'm here", "I'm with you",
  "(Japanese) I'm with you", "(chuckle) Hey", "What's up", "I'll be your angel", "Rolling with you", "How's the
  hunt?", "Howdy", "(Japanese) Are you well?", "Your pocket ninja's here". Left out: lines naming a hero (Mizuki,
  Genji, Hanzo), target-specific greetings ("Cute shoulderpads", "Digging the tunes", "Hey, big guy", "Hey, queen")
  and "Ho ho hey" / "Boo! (laughs)" (probably seasonal; unverified).
- **FC5, Suzu** (graph 30D8): all 12 ("Cleansed", "Purified", "Protecting!", "Hear my bell", "Be purified!"...).
- **FC4, healing** (heal subgraph 2128, 28 lines): 25 mapped, the three naming Shimada/Mizuki left out. The voice node
  carries 0.15 / 4 / 3: probably a chance and cooldowns, so the line is only said now and then (unconfirmed).
- The ult lines (FC3/FC2) are already in `kiriko.txt`. FC1 (also in the Suzu graph) has no lines.

## Measurements of the mapped files

Decoded with ffmpeg to mono 48 kHz in memory. Medians per id; peak/rms in dBFS of the file as stored (Wwise volume
offsets on top were not decoded). Attack = time to within 3 dB of the peak (10 ms RMS frames); "-20 dB length" = how
long the sound stays within 20 dB of its peak.

| id | n | ch | dur (s) | peak dB | rms dB | attack s | -20 dB length s | centroid Hz |
|---|---|---|---|---|---|---|---|---|
| kiriko.kunai.throw | 17 | 2 | 0.43-0.64 | -5.5 | -22.9 | 0.05 | 0.31 | 7417 |
| kiriko.kunai.throw_b | 15 | 2 | 0.29-0.39 | -4.6 | -22.0 | 0.04 | 0.18 | 4367 |
| kiriko.kunai.throw_3p | 16 | 2 | 0.36-0.57 | -4.85 | -22.25 | 0.03 | 0.21 | 4337 |
| kiriko.kunai.throw_3p_b | 8 | 2 | 0.20-0.47 | -5.75 | -23.1 | 0.04 | 0.1 | 2204 |
| kiriko.kunai.throw_hand | 24 | 2 | 0.40-0.57 | -5.85 | -22.55 | 0.04 | 0.23 | 1511 |
| kiriko.kunai.flight | 1 | 2 | 6.33 | -8.9 | -24.0 | 0.99 | 6.32 | 499 |
| kiriko.kunai.hit | 8 | 2 | 0.28-0.30 | 2.7 | -16.5 | 0.0 | 0.15 | 224 |
| kiriko.kunai.hit_sub | 7 | 1 | 0.13-0.31 | 0.2 | -9.9 | 0.01 | 0.2 | 43 |
| kiriko.kunai.headshot | 10 | 2 | 0.29-0.63 | -5.95 | -17.05 | 0.02 | 0.37 | 22 |
| kiriko.kunai.reload_1 | 9 | 1 | 0.49-0.56 | -19.5 | -41.4 | 0.07 | 0.21 | 1818 |
| kiriko.kunai.reload_2 | 9 | 1 | 0.17-0.44 | -8.1 | -26.5 | 0.03 | 0.13 | 7256 |
| kiriko.kunai.reload_3 | 3 | 2 | 0.22-0.23 | -10.7 | -26.7 | 0.07 | 0.21 | 10613 |
| kiriko.kunai.reload_4 | 3 | 2 | 0.33 | -5.2 | -19.6 | 0.03 | 0.33 | 3762 |
| kiriko.kunai.reload_5 | 2 | 2 | 0.40-0.59 | -6.25 | -25.6 | 0.01 | 0.21 | 5002 |
| kiriko.ofuda.throw | 16 | 2 | 0.93-1.48 | -4.65 | -28.0 | 0.03 | 0.49 | 966 |
| kiriko.ofuda.throw_3p | 17 | 2 | 0.35-0.48 | -16.0 | -31.8 | 0.03 | 0.19 | 452 |
| kiriko.ofuda.throw_3p_b | 16 | 1 | 0.52-0.87 | -7.85 | -32.55 | 0.03 | 0.26 | 2378 |
| kiriko.ofuda.flight | 7 | 1 | 2.58-3.41 | -17.8 | -47.4 | 0.62 | 2.96 | 6636 |
| kiriko.ofuda.flight_seeking | 14 | 1 | 2.44-3.50 | -3.45 | -23.75 | 0.13 | 3.13 | 8730 |
| kiriko.ofuda.heal / hit_block | 16 | 1 | 0.56-0.83 | -14.9 | -33.45 | 0.17 | 0.38 | 704 |
| kiriko.ofuda.heal_snap / hit_block_snap | 20 | 1 | 0.26-0.39 | -0.9 | -25.6 | 0.01 | 0.07 | 7053 |
| kiriko.ofuda.heal_sparkle | 4 | 2 | 0.66-1.82 | -21.9 | -36.75 | 0.1 | 1.06 | 5449 |
| kiriko.ofuda.heal_sub | 8 | 2 | 0.91-1.15 | -7.15 | -24.65 | 0.14 | 0.35 | 141 |
| kiriko.ofuda.heal_thump | 18 | 2 | 0.67-0.86 | -2.65 | -24.65 | 0.03 | 0.36 | 317 |
| kiriko.suzu.throw | 3 | 1 | 0.58-0.76 | -12.6 | -28.8 | 0.09 | 0.24 | 1163 |
| kiriko.suzu.throw_3p | 2 | 2 | 0.93-1.10 | -19.05 | -38.1 | 0.09 | 0.43 | 9583 |
| kiriko.suzu.flight | 2 | 1 | 1.22-1.24 | -11.35 | -31.2 | 0.04 | 0.54 | 2890 |
| kiriko.suzu.burst (stopgap) | 4 | 2 | 1.91-2.33 | -5.8 | -22.55 | 0.1 | 1.27 | 156 |
| kiriko.suzu.burst_shimmer (stopgap) | 1 | 2 | 1.36 | -13.3 | -36.4 | 0.02 | 0.56 | 5581 |
| kiriko.suzu.burst_bell (stopgap) | 2 | 2 | 1.67-2.02 | -17.95 | -37.1 | 0.11 | 0.97 | 3550 |
| kiriko.swift_step.cast | 3 | 2 | 2.32-2.67 | 0.8 | -18.8 | 0.13 | 0.81 | 273 |
| kiriko.swift_step.cast_sub | 1 | 1 | 1.38 | -8.8 | -23.9 | 0.12 | 0.58 | 47 |
| kiriko.swift_step.depart | 3 | 2 | 2.66-2.92 | -3.2 | -21.9 | 0.13 | 0.66 | 224 |
| kiriko.swift_step.arrive | 3 | 2 | 2.12-2.20 | -3.7 | -23.6 | 0.11 | 0.37 | 345 |
| kiriko.climb.step | 9 | 1 | 0.34-0.48 | -13.4 | -40.4 | 0.0 | 0.09 | 6026 |
| kiriko.climb.start | 4 | 2 | 0.32-0.39 | 0.8 | -16.35 | 0.02 | 0.18 | 154 |
| kiriko.climb.top | 7 | 2 | 0.49-0.71 | -7.0 | -32.9 | 0.0 | 0.25 | 2257 |
| kiriko.rush.summon | 1 | 2 | 4.48 | -4.3 | -23.5 | 2.02 | 2.77 | 117 |
| kiriko.rush.cast | 1 | 2 | 4.71 | -0.9 | -18.5 | 0.07 | 1.48 | 433 |
| kiriko.rush.cast_sub | 1 | 1 | 2.57 | -2.7 | -18.2 | 0.25 | 1.33 | 58 |
| kiriko.rush.cast_3p | 1 | 2 | 6.04 | -2.6 | -21.9 | 0.08 | 1.98 | 529 |
| kiriko.rush.path | 4 | 2 | 3.96-10.77 | -17.4 | -35.15 | 1.67 | 9.5 | 862 |
| kiriko.rush.path_start | 4 | 2 | 2.90-8.31 | -6.05 | -25.2 | 0.29 | 1.73 | 496 |
| kiriko.rush.gate_1..6 | 1 each | 2 | 2.02-2.17 | -9.8 to -11.2 | -28 to -30 | 0.01 | 1.2-1.3 | 2574 -> 5381 |
| kiriko.rush.gate_loop | 2 | 2 | 6.33-8.44 | -12.6 | -27.2 | 0.58 | 7.38 | 1071 |
| kiriko.rush.slab | 18 | 1 | 1.61-2.68 | -6.2 | -24.25 | 0.18 | 1.06 | 384 |
| kiriko.rush.pulse | 1 | 2 | 1.61 | -8.7 | -29.4 | 0.09 | 0.68 | 1393 |
| kiriko.melee.hit | 8 | 2 | 0.28-0.30 | 2.7 | -16.5 | 0.0 | 0.15 | 224 |
| kiriko.melee.hit_sub | 7 | 1 | 0.13-0.31 | 0.2 | -9.9 | 0.01 | 0.2 | 43 |
| kiriko.ability.denied | 1 | 2 | 0.20 | -15.2 | -30.9 | 0.0 | 0.2 | 5512 |

## What the game code must know

- **Variants.** `build_asset_pack.py sounds` writes one file per id (`name.replace(".", "/") + ".ogg"`) and one
  sounds.json entry per id, so with several lines per id only the last file survives. It needs numbered files and a
  list in `sounds`. It also reads every `tools/soundmaps/*.txt`, so `kiriko-proposed.txt` is picked up as soon as it
  exists.
- **Layers.** Ids ending `_b`, `_snap`, `_sub`, `_sparkle`, `_shimmer`, `_bell`, `cast_sub`, `path_start` are parts
  that Overwatch plays at the same instant as their base id. Either play them together or mix them offline at
  pack-build time.
- **1P / 3P.** Main ids are the owner's version where one exists; `_3p` ids are what others hear. In Minecraft that
  means the shooter plays the 1P one locally and the server plays the 3P one for the others (or one of them for all).
- **Stereo.** Most files are stereo (see `ch`). Minecraft only positions mono sounds; stereo ones play at the listener
  at full volume. World sounds (3P throws, impacts, arrival, gates, corridor) need a mono downmix at pack build.
- **Loops** (Overwatch stop events): `kunai.flight`, `ofuda.flight`, `ofuda.flight_seeking`, `rush.path`,
  `rush.gate_loop`. Play them as entity-bound or looping sounds and stop them when the projectile or effect ends.
  Everything else is a one-shot. `suzu.flight` is a one-shot attached to the bell.
- **Timing.**
  - Reload: 1P steps at 0.00 / 0.21 / 0.36 / 0.57 / 0.91 s; 3P steps 3-5 at 0.16 / 0.34 / 1.14 s.
  - Climb: one `climb.step` at 0.267 s and one at 0.469 s of every 1.033 s climb cycle, `climb.start` once when the
    climb starts, `climb.top` 0.033 s into the hop.
  - Kunai hand foley at the start of each throw clip.
  - Ofuda throw once per volley, not per talisman.
  - Swift Step: `cast` (owner) or `depart` (others) at the cast, `arrive` at the destination.
  - Rush: `summon` (2 s rise) and `cast` at the cast; `gate_n` when gate n appears, in order; `path` for the corridor's
    life.
- **Levels.** File levels differ a lot: the loudest are `kunai.hit_sub`/`melee.hit_sub` (rms -10 dB) and the punches,
  `climb.start` and `swift_step.cast`, which **peak above full scale (+0.8 to +2.7 dBFS) and will clip** when decoded
  to 16-bit: lower them ~3 dB at pack build. The quietest are `ofuda.flight` (rms -47), `reload_1` (-41),
  `climb.step` (-40), `heal_sparkle` (-37), `ofuda.heal` tones (-33). In Overwatch the Wwise objects add their own
  volume offsets (not decoded), so set sounds.json `volume` by ear.
- **Retrigger guards** in the data: kunai hit 0.05 s, heal thump 0.2 s, 3EC4 0.1 s, slabs 0.04 s, gate loop 16 s.

## Not extracted (would need DataTool, offline, if the owner wants them)

The banks are named in the archive; their audio is not in `extracted\154088`:

| Event | Banks (.02C) | Files |
|---|---|---|
| Kunai hit on a target (Kiriko's own) | 8DE7 (via effect 13B2 -> 4057) | 33 |
| Kunai body / crit hit, Kiriko layer | in 8BCE / 8BD2: 2AC663... ; 8BCE's 2ABC4B... ; 8FEB, 8FF3, 8FE8, 8FEF | 11 + 7 + 18 x 4 |
| Kunai on terrain | 826E / 826F (default) + 22 per-surface contact effects 47DD-4FED | 11 + ~600 |
| Kunai whiz-by (probably) | 7CCB (effect 3E87) | 6 |
| Kunai fiery-perk explosion | BBE4 / BBE5 / BBE6 (effect 6099) | 106 |
| Ofuda heal hit | 770E (1P), 8258 / 78F3 (3P) (effects 4004 / 40C0) | 20 + 39 |
| Ofuda on material 0033 | 4870 / 4871 | 12 |
| **Suzu burst** | **76FD (1P), 96F8 (3P), BDE2 (far)** (effects 360C / 3D07): files 2B3253 2B31AE 2B324C 2B3247 2B3255 | 5 |
| Suzu on material 00E1 | 4356 / 435C | 17 |
| Kitsune fox footsteps (probably) | 8240 / 8241 (contact set 0740 types 10/11) | 31 |

The Suzu burst is the most important gap; the kunai and heal impacts come next.

## Uncertain

- Which audience hears 3EC4 (the healer's or the healed player's heal confirmation).
- The exact meaning of contact types 0/1/2 (read from the pattern, not named in the data).
- Whether 76F7 is a distinct "hand sign" sound or the owner's mix of the departure.
- The gate order (from the rising pitch, not from decoded switch cases).
- Seasonal or target-gated voice lines (only excluded by their text).
- 5F58 / 009B (generic sounds after a UX presenter: maybe cooldown-ready feedback) and 6089's 5 s hum.

## How it was done (repeat for another hero)

Scripts in `E:\Overwatch Archive\research\kiriko-sounds\scripts\` (system Python with `-I` for the extracted files;
the kiriko-anim venv Python with pythonnet for the archive, through `owinit.py`, which refuses to run while
Overwatch or Battle.net is running):
1. `catalog.py`: every extracted non-voice .ogg grouped by owner / bank. `measure.py`: ffmpeg decode + measurements
   (`measure.json`). `report.py`: per-bank summaries.
2. `effect_index.py`: sound banks of every decoded effect (from the kiriko-effects decodes). `effctx.py`,
   `graphdump.py`, `shownode.py`, `nodetypes.py`: statescript context of each effect node (from kiriko-anim's dumps).
3. `banks.py`: every STUSound (audience, range, stop event, Wwise bank, WEM list) -> `banks\banks.json`;
   `banktable.py` tabulates them.
4. `wwmap.py`: Wwise HIRC trees (event -> action -> layer/random containers -> sounds) with WEM -> .03F by size;
   `wwcondense.py`, `layers.py` summarise them per group.
5. `contact.py`, `contactgroups.py`, `rawchunks.py`: contact sets and contact effects. `animfx.py`: sound times in clip
   animation effects. `soundsof.py`: sound banks under any asset.
6. `genmap.py` + `mapspec.json` write `kiriko-proposed.txt` (checks every path exists); `idstats.py` the table above.
