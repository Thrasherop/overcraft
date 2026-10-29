# Ana: the plan

Numbers: [ana.md](ana.md) (D, BZ, W per number) -> an `AnaStats`, each number with its source (D with its value id or
graph node, BZ with the date, W), as `LifeweaverStats` does; the game data map [ana-map.md](ana-map.md) with its detail in
`work_kit\kit_rifle.md`, `kit_dart_grenade.md`, `kit_nano_perks.md`, `work\assets.md`, `work\clipmap.md`, `work\voice_fx.md`.
Branch `feature/ana` in its own worktree (issue #63, hero 2 of 12). Written 2026-10-29 from build 154088 (2026-10-06); the
patches since (10-13, 10-20) don't touch her. Copy `ana.md` and this plan into `tools/reference/` with the branch, as
Lifeweaver's were, and add her hub `techniques/heroes/ana.md`.

## Where the data differs from the wiki (the data is used unless noted)
- **Rifle hits land over 0.5 s** (heal and damage alike: 75 over 0.5 s in a 0.6 s hit status), not W's 0.592 s in 5 ticks.
  Our ticks: 10 of 7.5 (ours; the engine's pattern isn't in the data).
- **Rifle sizes:** unscoped 0.15 m vs enemies, 0.3 m vs allies (W 0.05 / 0.15); scoped 0.04 / 0.3 (= W). Full-health allies
  are passed through by both, and by the grenade (D; W agrees for the dart and the grenade).
- **Scope 0.25 s in, 0.167 s out** (W 0.24 / 0.16): no shot during either; scoped shots only at full scope; the clip refills
  0.6 s into the 1.5 s reload and firing stays locked to 1.5 s.
- **Cooldowns start at the end of the cast** (Sleep Dart 1.0 s after the press, the dart at 0.3 s; grenade 0.7 s after).
- **Sleep on tanks 3.5 s** (D, the in-game panel) against BZ 2024-07-09's 3 s: **ask the owner** (Open). Waking: any damage
  after 0.5 s of sleep, then 0.5 s to wake; earlier damage is dropped; Local Anesthetic's ticks never wake.
- **Nano Boost:** the charge goes as the cast starts; the boost lands at 0.13 s whatever happens to the syringe (a visual);
  0.75 s cast; nothing spent without a target; confirmation option cancels after 0.5 s without sight (W 1 s).
- **Local Anesthetic:** enemies within **4 m** of the dart's hit, 45 over 3 s in 30 ticks, **slowed 40% easing to 20%**
  (D, M) against W's 50%: data used, flagged for the owner.
- **Tactician:** the stored excess (up to 25% of 2300 = 575) comes back **over 1 s just after the ult starts** (D); the mod
  now gives it back at once (`Combat.afterUlt`): small, change it with Ana or leave it (Open).
- Ult cost 2300 (D 0ADD = W), health 250 (D = BZ), perk XP 2025 / 5060 (D = W).

## What maps onto what Overcraft already has
- **Hero entry:** `Hero.ANA` (support, `"tactician"`, 250, colour from the hero file's 0.282 / 0.412 / 0.620), `Combat.ultCost`,
  `Perks` XP (2025 / 5060), `Perk` (four entries, the game's order: minor Local Anesthetic, Speed Serum; major Biotic Bounce,
  Headhunter), `HeroManager` / `HeroSelectScreen` / `HeroHud` wiring, a `HeroKit` (`dev.overcraft.ana.Ana`), as Lifeweaver's.
- **Tactician** is already shared (Lúcio, Jetpack Cat: `Combat.addUltCharge` by sub-role, the cap and the x0.75 rate in
  `LucioStats`); Ana gets it by her sub-role string. Moving those two numbers out of `LucioStats` into a shared place is ours.
- **Assets: Mercy's arrangement**, not Lifeweaver's: a separate first-person model with its own set, a weapon entity with its
  own set on a hardpoint (Mercy's staff and blaster, Lúcio's gun), weapon clips mapped like `tools/clipmaps/mercy_blaster.txt`.
- **The rifle's darts:** a straight, fast `OwProjectile` like `KunaiEntity` (no drop, its own headshot test, passes through
  allies) and `BlasterShotEntity`; Kunai's head test serves Headhunter (x2).
- **Heal over time:** `Lifeweaver.healOver` (a blossom's heal landing over a time) moved to `Combat` for both heroes; a
  damage-over-time counterpart beside it (Local Anesthetic's ticks too).
- **The grenade's arc:** `PetalThrowEntity` / `SeedEntity` (thrown with gravity, land, act).
- **Nano's target pick:** `Targeting.allyInView(range 40, half angle 10 x sensitivity, sight)` (the data's 20° cones were read
  as the angle from the aim for Mercy and Lifeweaver; the same here), plus "not already boosted"; `Allies.isAlly` for who counts.
- **Nano's +50% damage:** `mercy.Boosts` (the attacker's factor, applied in `LivingEntityMixin` as damage arrives, pets and
  arrows included) generalised to a sum of boosts (Mercy +30% and Nano +50% together; the data adds into a sum, M).
- **Speed Serum, the scoped slow, Local Anesthetic's slow:** `Combat.buff(entity, MOVEMENT_SPEED, ...)` (Foxtrot, Sow the
  Seed, Jetpack Cat's claws), read by `OwMovement`. The data's rule is "the strongest slow wins" (01C7), not a sum: ours to
  copy or not.
- **Locking a player's input from the server:** `GripPayload` / `ClientLifeweaver.grip` hold a gripped player still on their
  own client: the model for sleeping a player.
- Quick melee (`QuickMelee`, 40), the global regeneration (`Combat`, 6 s, 10 + 5%), kill feed (`KillFeed`: "put to sleep"),
  voice (`Voice`), sounds (`HeroSounds`), effects (`Effects`), hit markers, perks (`Perks`).

## What's new (nothing in the mod does it yet)
- **Scoping** (client and server): hold RMB (toggle option off by default); a scope progress 0 -> 1 in 0.25 s, back in 0.167 s;
  at 1 the view zooms (the data's 30: FOV degrees? L, ask), mouse sensitivity x0.3 (a mixin where Minecraft turns the
  player), the 1P rifle hidden and a scope overlay (from 0.8), -65% move speed; no firing while it moves. The server knows the
  scope state (it decides hitscan vs projectile). `HeroFovMixin` fixes the FOV today: the zoom goes there.
- **Hitscan:** the scoped shot is a ray from the eyes (range: the data's 100, L), against entity boxes grown by 0.04 m
  (enemies) or 0.3 m (hurt allies), stopped by blocks; a tracer effect (1615 / 1A80) on clients. No hero has one yet.
- **Healing modifiers on the target:** +50% healing received (grenade on allies, 3 s) and **no healing at all** (grenade on
  enemies, 3 s). One hook where every heal lands (`LivingEntity.heal`, which `Combat.heal` calls) so it covers hero heals,
  our regeneration, potions, golden apples and Minecraft's own regeneration: ours to confirm with the owner (Open). The
  boost doesn't apply to an anti-healed ally. BZ 2026-02-10's global "damage dealt cuts healing 30% (15% tanks) for 3 s" is
  not in the mod either: the same hook would carry it (Open).
- **Damage taken modifier on the target:** Nano's -50% (a `LivingEntityMixin` hook on the victim, beside Mercy's on the
  attacker), before shields take it.
- **Sleep** (a crowd control): the target can't move, look, attack, use items or abilities for up to 5 s (3.5 or 3 on
  "tanks", Open); running abilities stop (Lifeweaver's grip, Mercy's flight, a creeper's fuse, an evoker's cast: ours to list
  and test); gravity still acts (W: momentum kept in the air). Any damage after 0.5 s wakes it 0.5 s later; earlier damage
  is ignored; Local Anesthetic's ticks don't count. Players: the server's state plus a client lock (as Life Grip's); hero
  models play the shared knock-down clips 0x064 / 0x065 / 0x066 (every hero has them: each hero's clip map needs them);
  plain players and mobs need a pose of ours (Open). Mobs: their goals and navigation held, their target kept (ours). Cleansed
  by Kiriko's Suzu (`Combat.cleanse`) and by milk (ours). Not on bosses? (Open).
- **Anti-heal and heal boost shown:** effects 19F4 / 5485 on the target, and a HUD cue on a player who can't be healed.
- **A buff on another entity for 8 s** with its own effects for everyone (5FAA, 1A16) and for the boosted player (7A23), its
  ally voice (05E2 NanoBoosted: each Overcraft hero's own lines from their voice sets), ending when it dies.

## Assets
- 3P model **10562.00C** (entity Gameplay3P 1424, set **05F5**), 1P model **10561.00C** (Gameplay1P 1425, set **05F9**), rifle
  **10B96** (entity 159C, set **05FC**) on hp **00C9** in both views; the Sleep Dart pistol **1175E** (15CA) on hp 0143 (1P)
  / 00A6 (3P), right hand, only during the dart's cast; the grenade **11763** (set 0074); the darts 11741 (rifle) and 11740
  (sleep dart and the rifle's chambered dart); the vial 1173F (the rifle's 017E, her hand 000B during the reload: M); props
  for Tea Time and the menus (teacup 372A, teabag 373A, saucer 3730). Export with `tools/owm_survey.py --glb` from
  `E:\Overwatch Archive\research\ana\extract\Heroes\Ana\Skin\Overwatch\Overwatch` (the source for `build_asset_pack.py add`
  too), with clip maps written from `work\clipmap.md` / `clipmap.json` (`tools/clipmaps/ana.txt`, `ana_1p.txt`, `ana_rifle.txt`).
  Read the repo's `techniques/animation/animation_import.md` first: `4-*` clips are additive (the fire layer 3C3F, the
  spine adds).
- **Hardpoints from the model's own table** (her looks have none: Mercy's case). Muzzles: the rifle's 0006 (0.9 m ahead of
  00C9 at rest), the pistol's 0006; the grenade leaves her left wrist (0002); the syringe her left forearm (0078); targets aim
  at an ally's chest (0001). The rifle's arm IK aims her wrists at her own 000A / 000B (L): check by eye that the hands sit on
  the rifle in every clip (the contact check: `tools/emote_contacts.py`).
- **Her stance is low** (pelvis 0.83 m in play against 1.066 at rest): don't "fix" it. Mover 2.0 / 0.5 / 1.25 (defaults).
- Two cloth pieces (the robe 275 points, the hood / cape 140), as Kiriko's cloth. No geosets.
- The rifle shows its ammo on the model (dataflows 0B9A tens, 0B9B units): later, if the model's counter is a texture or
  bones (look at it).
- Select screen: showcase BACC -> BC6F / BC6E / BC70 / BC6D (entity 3A7E, the teacup on 000A, the rifle on 00C9).
- Sounds: banks in the skin export (879 files in 123 banks); voice 1177 lines.

## Controls (Overwatch's, with the owner's layout)
LMB: Biotic Rifle (held: fires every 0.8 s). RMB: Take Aim (hold; Toggle Zoom option). R: reload. **Shift: Sleep Dart.** **E:
Biotic Grenade.** **Q: Nano Boost** (with the confirmation option: LMB or Q confirms, RMB cancels). V: quick melee. The hero
options with the data's defaults (Toggle Zoom off, zoomed sensitivity 30%, Nano Boost Requires Target Confirmation off, Nano
Boost Sensitivity 100%): the defaults only unless the owner wants the options.

## Server (the kit)
- Health 250, no shields. Regeneration: the global one.
- **Biotic Rifle:** 15 rounds; a shot every 0.8 s (the recovery pauses during scope changes and quick melee); unscoped a dart
  at 125 m/s, straight, 5 s, from the rifle's muzzle, hitting enemies within 0.15 m and hurt allies within 0.3 m, passing
  full-health allies; scoped (full scope only) a hitscan ray, 0.04 / 0.3. A hit: an ally healed 75 over 0.5 s, an enemy
  dealt 75 over 0.5 s (x2 on a head hit with Headhunter; never on allies); never Ana. Reload 1.5 s (auto at 0; R when not
  full), the clip full at 0.6 s; cancelled by an ability or quick melee before then.
- **Sleep Dart:** Shift; a 1.0 s cast, the dart at 0.3 s from the pistol's muzzle, 60 m/s, straight, 0.2 m, 20 s; 5 damage
  and sleep (above) on the first creature hit; cooldown 14 s from the cast's end (1.0 s). Local Anesthetic: on the hit, enemies
  within 4 m (with sight) take 45 over 3 s (1.5 every 0.1 s) and a slow 40% -> 20%; only the hit one sleeps.
- **Biotic Grenade:** E; thrown at once (about 2 ticks in, M) from the left wrist, 30 m/s, gravity 9.8 m/s², contact radius 0.275, passing
  full-health allies; it bursts on its first hit (block or creature): 4 m, allies (Ana too) healed 90 at once then +50%
  healing received for 3 s (unless anti-healed), enemies dealt 90 and no healing for 3 s; cooldown 12 s from 0.7 s. Biotic
  Bounce: it bounces once and bursts again for 60 with the same effects (its bounce distance and timing are ours: Open).
  Whether walls block the burst: the data has a sight profile (L): ours, with sight (as the tree's pulses).
- **Nano Boost:** Q at 2300; the target picked at the press (40 m, 10° half angle, sight, not already boosted; herself when
  her team is only her: Open); none: nothing spent, the fail sound. The charge goes, 0.13 s later the ally gets 250 healing,
  +50% damage dealt and -50% damage taken for 8 s (Speed Serum: +30% speed for both, Ana for the cast and the 8 s); ends early
  when the ally dies; Ana's death doesn't end it. Tactician's excess comes back after.
- Perks: as above; perk XP 2025 / 5060.

## Client
- Movement: Overwatch's walk; the scoped slow; the sleep lock (own client, as Life Grip's hold).
- Animator: idle (two turn-in-place stands by df 0xB7), run 8-way, crouch, jump / land, each with the scoped switch (df 0xE5
  cat 0x81: a cross-fade in 3P, the raise 3B84 and hold 3B92 in 1P); the fire layer (3C3F 3P / 3B8D, 3C9A 1P) on each shot;
  reload 0x004 / 0x084; the dart (pitch triple + the pistol), grenade, nano and quick melee pitch triples; the rifle's own
  idle 3C59 and fire / reload clips; the sleeper's 0x064 / 0x065 / 0x066 on any hero model; hit 3C76.
- HUD: rifle ammo 15, the scope overlay, Sleep Dart (Shift, 14 s), Biotic Grenade (E, 12 s), Nano Boost (Q, 2300, Tactician's
  orange extra bar as Lúcio's), the boosted ally outlined while Q is held and the ult ready (the data's preview 013C), a sleep
  and an anti-heal cue on a player who has them.
- Effects first by id (sounds from the banks in `ana-map.md` "Sounds and effects"), visuals later as for Lifeweaver.

## Unusual about her
- **The scope** changes the camera, the sensitivity, the speed and the weapon's kind at once, on a timer the server must
  agree with (no scoped shot before full scope). The client's zoom must never fight Overcraft's fixed-FOV mixin.
- **Sleep in Minecraft** touches every mob type: a creeper mid-fuse, a skeleton drawing, an enderman teleporting, a flying
  phantom or bee (falls? the data keeps momentum), a mob in water, bosses. Each needs a test in hand play.
- **Anti-heal** has to stop every way Minecraft heals (regeneration effect, saturation, golden apples, potions, our
  regeneration), or the owner decides it stops only hero heals.
- **Nano on a pet** (a wolf deals +50% bites, takes half damage): Minecraft allies are pets and villagers; on a villager it
  does nothing useful.
- **Two rifles of numbers:** unscoped projectile and scoped hitscan differ in size against enemies (0.15 vs 0.04) but not
  against allies (0.3): healing allies is as easy scoped or not.

## Order (each step played by hand before the next; CLAUDE.md's hand play rules)
1. Hero entry, models and clip maps, select screen and HUD shell (no kit): pick her, look at her from every side in both
   views, the rifle in her hands in every clip.
2. Biotic Rifle unscoped (dart entity, heal / damage over 0.5 s, ally pass-through, ammo, reload) against zombies, a wolf, a
   villager.
3. Take Aim (zoom, sensitivity, slow, hitscan, the transitions).
4. Biotic Grenade, with the healing modifiers (boost, anti-heal) and their HUD cues.
5. Sleep Dart and the sleep (players and every mob kind), its voice and kill feed.
6. Nano Boost (target pick, cast, the buff on players and pets, the confirmation option), Tactician's refill.
7. Perks (Local Anesthetic, Speed Serum, Biotic Bounce, Headhunter).
8. Sounds, voice (the ability lines and each hero's NanoBoosted), effects.
9. Emotes (16, from the data: `techniques/emotes/emote_reproduction.md`), the select-screen showcase.
10. The general play of the whole build (a night in survival with her, a skeleton farm, switching modes with G, dying, rejoining).

## Later
Visual effects decoded (the darts' trails, the grenade burst, the scope overlay texture, nano's glow), intros and victory
poses (not extracted yet: extract them first, Overwatch closed), the rifle's ammo counter on the model, the hero options
beyond the defaults, Stadium (not planned).

## Open (2026-10-29)
- **Tank sleep:** 3.5 s (the data and the in-game panel) or 3 s (BZ 2024-07-09)? And which Minecraft mobs count as "tanks"
  (bosses, ravagers, iron golems, wardens?), or none.
- **Sleep's look:** plain players and vanilla mobs have no knock-down clips: lie them down, freeze them standing with an
  effect, or Minecraft's sleeping pose? Can bosses (the Wither, the Ender Dragon, the Warden) be slept?
- **Healing modifiers' reach:** do the grenade's boost and anti-heal (and the global 30% reduction from damage, BZ
  2026-02-10, not yet in the mod) apply to Minecraft's own healing (regeneration, food, potions, golden apples), or to hero
  heals only? Add the global reduction with Ana?
- **Self-nano:** the data nanos herself whenever her team has one player. In a single-player world that is always: should Q
  then nano her even with a wolf in view, or pick a pet / villager in view first and herself only when none?
- **The zoom:** how far to zoom in Minecraft (the data's 30, unit unclear: perhaps a 30° FOV), and whether Minecraft's FOV
  setting scales it.
- Local Anesthetic's slow: the data's 40% easing to 20% (used) or W's 50%.
- Tactician's refill over 1 s after the ult starts (the data) or at once (the mod today).
- Biotic Bounce's bounce (how far, when the second burst comes): not in any source; ours to measure in a recording or choose.
- Fall damage: her safe height for ordinary falls (3 m, the default, unless the owner says).
