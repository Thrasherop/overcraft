# Lúcio (hub)

Everything learned about Lúcio, by subject. The second hero: where Kiriko's methods were tried on someone else, and
where some of what her research guessed got settled. Up: [Techniques](../README.md) · the method:
[animation_import](../animation/animation_import.md) · his gun: [model_data](../game-data/model_data.md)

## His notes and sources
- **The data map:** `E:\Overwatch Archive\research\lucio\lucio-map.md` and `map.json` (states, clips, abilities, game
  values; read 2026-10-27). **Its skating and jump are swapped** (below); its sibling notes `voice_sounds_effects.md`
  and `emotes.md` cover his voice, songs and emotes.
- **Clip maps:** `tools/clipmaps/lucio.txt` (body), `lucio_1p.txt` (arms), `lucio_weapon.txt` (the gun), each line
  saying where it came from.
- **Numbers:** `LucioStats`, each with its source; `tools/reference/lucio.md` (the public sources),
  `tools/reference/lucio-plan.md` (the port's plan).

## Facts that come up often
- **Models:** the skin's `DB3F` (245 bones, 8 of them thigh cloth), not the base `11D7`; first person `DB40` (88 bones,
  arms); the Sonic Amplifier `DBA3` (16 bones), its own entity (0401) on his hardpoint `000A` = Weapon_R, turned 180
  degrees about X in third person and not at all in first (each model's hardpoint table). `HeroWeapon` hangs it there
  and plays its own clips (reload, ult, the volley's and Soundwave's additives) with his.
- **States, as the clips really are** (2026-10-27, measured in game and from the clips' feet):
  - category 005 = **skating**: 8 ways, 0.667 s pushes, feet on the ground (0.17 m, as standing). The data map called
    it "airborne". First person: 4 ways, 0.9 s strides on the clock times the speed.
  - alias 0x006 (category 006) = **the jump**: a 4-slot node, takeoff (0.167 s: the feet leave the ground) then a
    0.333 s rise with the legs tucked. The map called it skating.
  - alias 0x01B = **the stop**, skating to standing: 0.4 s, the feet braced ahead the way he went. The map called it a
    landing.
  - aliases 0x64-0x66 = a knock-down (his pelvis lies on the ground); his death plays its fall (Overwatch ragdolls).
- **4-slot directions:** the slots (m_09FE31A5, m_7A2F0A64, m_CDA1FD6A, m_E15BD392) are forward, back, left, right:
  the feet trail the way he goes, the hips turn as the 8-way clips' do, the stop's feet brace the same way, and
  Crossfade's skating clips pair 1-2 and 3-4. The 8-way slots are named by Kiriko's measured runs (the same field
  hashes).
- **Clips that lift him:** the jump's rise (pelvis 1.10 -> 1.43 m) and Sound Barrier's cast (feet up 1.63 m) carry the
  height in the pelvis. Minecraft moves him already: the lift comes off the pose, and the ult's leap speed (7.4 m/s) is
  set to reach the clip's height.
- **Crossfade:** graph 0266's song switch plays 0x266 entering the speed song (it holds the speed visual CD2) and 0x28E
  entering the heal song (CAB): `crossfade_speed` / `crossfade_heal`. 0.2 s after the press, 0.25 s cooldown. Its
  first-person clip only moves the left hand: actions play over the pose under them, or the gun's arm drops away.
- **Wall ride:** `wallride_left` (2915) has his feet on a wall at his left; in first person the left hand trails the
  wall (2518). The lean pose sheets (2927 / 29B3, 1P 2519 / 251E: the camera rolls -20..+20 degrees) aren't sampled
  yet: their layout isn't read.
- **His dreads' hair cards** (material 8052) come out pure black: Overwatch's hair shader colours them. Not drawn.
- **Not settled:** what df 0x2B (the jump's time) and df 0x1AD (skating's spring) are; which weapon reload (248D at 30
  fps, 26E8 at 60) is which view's; his aim sheet 265E's layout (74 frames, unlike Kiriko's 63); the alternate value
  set in his game values (a mode's).

- **Emotes:** [lucio_emotes](../emotes/lucio_emotes.md) (his weapon is an entity: stow windows, geoset 1's cable).
- **Effects:** [lucio-effects](../../tools/reference/lucio-effects.md), built in `LucioFx`
  ([rendering](../rendering/rendering.md)).
- **Reload with abilities (data, graphs 0776 / 0277 / 0266 / 073A):** the reload holds the weapon state (var 33) at
  priority -10; Amp It Up (0, for 0.7 s) and Sound Barrier take it and end the reload; Crossfade never does.
  Animation priorities (024 assets): Crossfade -1, the reload 0 (none set), Amp It Up 3.
- **His song ends on death:** a hero's tick doesn't run while dead, so state synced from it (the song) must be
  cleared there (`HeroManager`).

## Still to do
- His effects on allies (amped speed lines 4ADD, the barrier's shield 0D86), the wall ride's sparks, the amp on the
  DJ booth (nested spawns), the wall ride's lean sheets, his collectible voice lines on the wheel.
- Kiriko could get the same: her own alias 0x006 4-slot set (AFCB-AFD7) is her directional jump, now unused.
