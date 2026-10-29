# Ana (hub)

Everything learned about Ana, by subject. The sixth hero (issue #63), second on the owner's 2026-10-28 list, the first
Tactician built from her data and the first with a scope. Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · read the way Lifeweaver's kit was: [lifeweaver](lifeweaver.md)

## Her notes and sources
- **Numbers and kit:** `tools/reference/ana.md` (her kit from the data, the patch notes to 2026-10-20 and the wiki's
  revision 408527, each number with its source and H / M / L, and where the sources disagree).
- **The data map:** `tools/reference/ana-map.md` (her graphs, effects, models, clips, sounds and voice by id);
  the research's detail in `E:\Overwatch Archive\research\ana\` (`work_kit\`, `work\clipmap.md`, `extract\`, build 154088).
- **The plan:** `tools/reference/ana-plan.md` (what maps onto the mod, what's new, the order, the owner's open questions).
- **Clip maps:** `tools/clipmaps/ana.txt` (third person, 137 clips with the Hello wave), `ana_1p.txt` (33),
  `ana_rifle.txt` (the rifle, 6; it's `hero_models/ana_weapon.glb` in the pack).

## Building her (2026-10-29: step 1, her models)
- **Her rifle's clips key its root in her space, not the hardpoint's** (as Mercy's staff does): its idle 3C59 puts
  bone_0001 1.44 m up. Her own clips already carry the hardpoint 00C9 (bone_0606) to her hands, so applying the root put
  the rifle 1.4 m above her head, out of view in both cameras. `HeroWeapon` now keeps the root of any weapon whose idle
  moves it half a metre or more (`bakedRoot`). Only the staff and her rifle do; Lúcio's gun and Mercy's blaster don't.
- **Her research had the 1P jump as the scope-out:** the 1P pair 3B93 / 3B94 is her jump (timed by the jump's df).
- **Her stance is low** (pelvis 0.83 m in play against 1.07 at rest): the data's, not a bug.
- **A thin rod hangs from her hands to her knee in third person.** It's part of her body model, present without the
  rifle too. Not yet identified.
- **Her hip glint material AC4A** has shader 0x26 (Lifeweaver's petals' shader) with another blank (000B) in the colour
  slot: a 2 cm highlight, not art.

## Building her (2026-10-29: step 2, the Biotic Rifle unscoped)
- **Two hit radii on one projectile:** the base class gives every entity one margin (`ProjectileUtilMixin`). Her dart
  takes enemies by Minecraft's own hit test at 0.15 m (its `hitRadius`), and hurt allies by its own sweep each tick at
  0.3 m (`AnaDartEntity.hurtAllyAhead`). Full-health allies and Ana herself are skipped, so it flies through them.
- **Heal and damage over 0.5 s** (`Ana.dose`, ticked from `HeroManager`). Split into ten ticks, Minecraft armor takes a
  little more than from one hit: a zombie (2 armor) loses 5.59 of the 6, not 5.9. The mod lets Minecraft armor count
  against heroes' damage everywhere; this is the same rule, made a little stronger by the split.
- **Test spots near a village lie:** a zombie summoned 10 m ahead stood half inside a house wall and suffocated, and
  looked like the dart killing it in one shot. Test on open ground away from buildings, and read the target's health
  before firing.
