# Mercy: the plan

Numbers: `tools/reference/mercy.md` (public sources) -> a `MercyStats`; the game data map
`E:\Overwatch Archive\research\mercy\mercy-map.md` (wins where they differ, except where the map says a branch isn't the
live rule set). Branch `feature/mercy`.

## Where the data differs from the wiki (the data is used unless noted)
- Flash Heal cast 0.5 s (BZ / W 0.4 s). Resurrect range 5 m (W 5.5; BZ 2017 5).
- Damage boost 25% and Medic self-heal 40% are the live numbers (BZ); the data holds 0.15 / 0.4 under a shared rule-set
  flag and 0.25 / 0.15 without it: use 25% and 40%.
- The beam's lose-sight linger: 1.0 s in the data (its lock tracker's field, 1EA7); W says 1.3 s. 1BE6 = 1.3, once read
  as the linger, is Valkyrie's Guardian Angel launch factor (kit_details.md, 2026-10-28). The data's 1.0 is used.
- Guardian Angel (kit_details.md): stops 3.7 m short of the ally, doesn't follow them, no time limit or lost-target end,
  not under 4 m; the launch formula and the 0.35 s window after arriving; momentum full after 1 s (0.68 in Valkyrie).
- Ult cost isn't in her data: W's 2150.

## Assets
- Third person `DBEA.00C` (408 bones incl. 138 cloth; look of Gameplay3P), first person `DBEB.00C` (arms + wing bones),
  Caduceus Staff `DA56.00C`, Caduceus Blaster `DA57.00C`: each weapon its own model, hung on hardpoint 000A (Weapon_R)
  in both views, like Lúcio's gun (`HeroWeapon`). Export with `tools/owm_survey.py --glb --entity=... --keep=
  tools/clipmaps/mercy.txt` / `mercy_1p.txt` / `mercy_staff.txt` / `mercy_blaster.txt`.
- Which weapon is out is the data's category: every animation state has a staff clip and a `_blaster` clip.
- Muzzles: the staff's hp 0006 (its head: both beams start there), the blaster's hp 0006. The wings are bones on her
  back, always drawn; the flight clips spread them. Cloth: skirt and coat tails (Domino, as Kiriko's).
- Two clips aren't in the extraction (the Valkyrie cast's additive 4A5F, the select intro ACA3): commented in the maps.

## Controls (Overwatch's, with the owner's layout: the main weapon on the left button)
- Staff: LMB heal beam (held), RMB damage-boost beam (held; switching keeps the target). Blaster: LMB fire (held).
- Shift Guardian Angel (while flying: Space launches forward, Crouch launches up; again to cancel). E Resurrect. R Flash
  Heal (its loadout is on Overwatch's Reload button). The blaster's reload: when empty and when holstered (the data's
  1.4 s self-reload); whether R also reloads it with the blaster out isn't in the data or the sources: ask the owner.
  Q Valkyrie. V melee. Space held in the air: Angelic Descent (no fall damage while it holds: the owner's rule for
  abilities that land the hero).
- Weapon swap: the scroll wheel or 1 / 2 (Overwatch's), not the hotbar keys in Minecraft mode (G switches modes).

## Server (the kit)
- Beam: lock the ally nearest the crosshair within a 40° cone and 15 m (players on her team, tamed pets, villagers),
  re-checked 12 times a second; line of sight lost or out of range: linger 1.3 s, then drop. Heal 55/s (65 in
  Valkyrie); boost +25% to the target's damage while held. Medic: 40% of her beam healing heals her.
- Blaster: projectiles at 50 m/s (100 in Valkyrie), 20 damage, 5 a second, 25 rounds, reload 1.4 s, hit size 0.2;
  reloads itself 1.4 s after being put away.
- Guardian Angel: to an ally (or a dead ally's soul) within 30 m (60 in Valkyrie, +30% with Winged Reach) at 17 m/s
  (25); Space launches 11-17 m/s by the momentum built (x0.8 holding back), Crouch launches straight up x1.2; after a
  launch 1 s (0.5 s in Valkyrie) before the 1.5 s cooldown.
- Angelic Descent: Space held while falling caps the fall at 2 m/s; never in Valkyrie. No fall damage while it holds.
- Resurrect: a dead ally within 5 m (Minecraft: a downed player of her team; for pets / villagers ours to decide), 1.75 s
  cast at 25% speed, then the ally back at full health, invulnerable 2.25 s; 30 s cooldown. Interrupted by stuns (and
  past 7 m: M).
- Flash Heal: 0.5 s, 60 to the beam target (x2 below half health), 12 s; Double Dose 50 and two charges.
- Valkyrie: 0.5 s cast, 15 s of flight at 9 m/s (air accel 8, Space up / Crouch down: vertical speed ours), knockback
  taken -50%, beam chains to allies within 10 m of the target, beam range 30 m.
- Swift Recovery: regeneration starts 3 s after damage instead of 6 (10 HP/s + 5% of max a second, the global rule).
- Perks: Angelic Resurrection (100 overhealth after Resurrect), Winged Reach, Chain Boost (+5% and a second ally),
  Double Dose. Stadium powers in her data are not ported.

## Client
- Movement: Overwatch's walk (5.5 m/s); Valkyrie's flight in OwMovement like Jetpack Cat's (`mayfly` while in play;
  speed 9, air accel 8); Angelic Descent a fall cap; Guardian Angel a move-to driven from the client with the server's
  target.
- Animator: the weapon category on every state; idle / run 8-way / crouch / jump / land; additive beam and shot layers;
  Guardian Angel's start / launch / flight phases; the 9-point flight blend for Valkyrie and Angelic Descent (from
  0.2 s); Valkyrie cast, Resurrect, Flash Heal, reload, melee, knock-down for death; the weapons' own clips.
- HUD: staff beam target and mode, blaster ammo 25, Guardian Angel (Shift), Resurrect (E), Flash Heal (R, charges),
  Valkyrie (Q).

## Later
Sounds (a sound map from her banks: beam 0197 / 0198, Guardian Angel 019B, Resurrect 030F, Valkyrie 1FA0, Flash Heal
64E1), effects (the beams, wings' glow, Resurrect's halo), voice (beam lines 00B9 / 00BA, "Heroes never die!" 08CB),
emotes (20 extracted).

## Open (2026-10-28)
- The half-or-whole angle of her cones (GA 30, beam 40, Resurrect 60): read as the angle from the aim (M).
- What her velocity is when Guardian Angel arrives without a launch (engine side): ours, she stops.
- Valkyrie: vertical speed (not in the data; ours: the flight's 9 m/s), and GA's post-launch overrides in Valkyrie
  (786680F6 1.4444, F808ACE8 0: meaning not known). Holding Crouch after a Valkyrie crouch launch brakes it (her
  crouch sinks her in flight).
- A x1.15 launch under an unnamed condition (26EF.134); effect 5B82 on a launch above 0.75 momentum (not mapped).
- The soul's lifetime (not in her graphs; ours 10 s).
- emote_data.py should read the EntityControls raw itself (techniques/emotes/mercy_emotes.md); today
  tools/emote_weapon_slots.py patches the windows and the staff's clips in from the research's raw read.
- Guardian Angel's target preview (the ready-state tracker, 6 a second) isn't drawn.
