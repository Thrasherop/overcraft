# Brigitte (hub)

Everything learned about Brigitte, by subject. The eighth hero (issue #65), fourth on the owner's 2026-10-28 list: a Support,
Survivor (BZ 2026-02-10). Up: [Techniques](../README.md) · the method:
[reading_game_data](../game-data/reading_game_data.md) · built the way Sombra was: [sombra](sombra.md)

## Her notes and sources
- **Numbers:** `tools/reference/brigitte.md` (Blizzard's patch notes to 2026-11 and the wiki's revision 408525, each number
  with its history). No patch has touched her since the data build (BZ 2026-09-08 was the last), so the data's numbers
  are the live ones.
- **The data map:** `tools/reference/brigitte-map.md` (her kit from her graphs, node by node, H / M / L); the detail in
  `E:\Overwatch Archive\research\brigitte\` (`work_kit\`, `work\assets.md`, `work\clipmap.md`, `extract\`, build 154088).
- **The plan:** `tools/reference/brigitte-plan.md` (what maps onto the mod: armor and a barrier are new; the order).
- **Clip maps:** `tools/clipmaps/brigitte.txt` (3P), `brigitte_1p.txt`, `brigitte_flail.txt` (the handle, exported as
  `hero_models/brigitte_weapon.glb`), `brigitte_flail_head.txt`, `brigitte_shield.txt`.

## What step 1 taught
- **The `--entity` import crashed headless Blender** for every one of her models (the add-on raised an error popup,
  `popup_menu_end` with no window: an access violation). Without it every model exported; her first-person arms got
  their materials anyway (no "no colour texture" line). Run one export alone first when a hero's exports all fail.
- **Three models besides her body:** the flail's head sits on the handle's socket 0254 (bone_0FCD), and her shield
  folded on her back (01D8, bone_0F74). `HeroWeapon.extras` now returns every extra model (Ana's sidearm was one).
- **Her view model has no walk clip:** idle, jump, fall and land, with movement additives; Ana's first-person rule
  fits (the idle while she moves).
- **No showcase clips extracted** (C06C / C07D / C0B8): the select screen poses her in her idle, and the weapon and
  extras are drawn from that pose as her showcase entity has them (the flail on 000A, the shield folded on 01D8).

Played (step 1, dev client, fresh creative worlds `brig1`-`brig3`): picked from the select screen (her model, "Support,
Survivor, 250 health", her seven kit icons; after the fix the flail in her hand and the shield on her back); in play, first
person (the flail in her right hand, Repair Pack's 3 charges, Whip Shot and Shield Bash icons), third person front and
back (the flail and its head, the shield folded on her back), running, jumping, crouching. Lucio's and Jetpack Cat's
select screens unchanged. Screenshots b_*.png, sel_*.png, not committed.

## What step 2 (armor) taught
- **Armor is a layer like shields** (`Hero.layer()`, kept in `HeroState.shield`): hits come off it first, heals fill the
  health under it first; it never regenerates; a hit while any armor is left is 7 smaller, at most halved
  (`Shields.armorReduce`, in the damage mixin after the boosts). Ours: the whole hit is reduced where Overwatch reduces only
  the part landing on armor (M); beams and damage over time aren't told apart yet.
- **A fresh flat survival world has mobs from generation** (205 killed, slimes among them): `spawn_mobs false` stops new
  ones only. A slime's unseen hits looked like a bug in the armor until `kill @e[type=!player]` ran first.
- **Adding a hero shifts the select screen's portrait row** (9 portraits now: Kiriko 142 ... Brigitte 266 in GUI
  coordinates at 854x480): the harness clicks for each hero moved.

Played (step 2, dev client, survival, flat world `armor1`, natural regeneration off, every mob killed first; health polled
every 50 ms): a zombie's 37.5 hits landed as 30.5, 30.5, 30.5 (armor 75 -> 44.5 -> 14 -> 0), then 37.5, 37.5; a
skeleton's arrows 62.5 and 50 as 55.5 and 43; `/damage 12` (150) as 143. HUD: seven white and three yellow pips at full;
after a heal to 158, the health pips filled and the armor's stayed empty; full again, yellow back. The select screen says
"175 health, 75 armor". Lifeweaver after the change: a hit of 37.5 in full, his shields back from 3 s at 30 a second.

## What step 3 (Rocket Flail, Inspire) taught
- **The flail is a sweep, not a ray:** each tick of the hit window (0.2-0.4 s) widens the arc covered (-17.5 + 35 t^2
  degrees, the backhand mirrored), and every enemy within 6 m (nearest point of its box) whose side is in the arc, in
  sight, takes 45 once per swing. Her quick melee starts the same swing. Magic damage: Overwatch's flail pushes no one.
- **An iron golem is an ally** in the mod (`Allies`): a flail test on one did nothing, rightly; use a ravager for a big
  target.
- **Open for step 8:** the flail's head flies out on its chain in a swing (entity 2283 flung, chain beam 216C / 33C1); the
  swing clips play on her, not yet on the head.

Played (step 3, dev client, survival, flat world `flail1`, every mob killed first, NoAI targets; health read with `data
get`): one swing at a zombie 4 m ahead: 45; one swing at three zombies in the arc (4-5 m): 45 each, a zombie at 7.5 m and
one 90 degrees to her side untouched; the button held 3 s on a ravager: 225 (five swings, one per 0.6 s); a zombie behind
a stone wall: untouched; the quick-melee key: 45. Inspire from one swing: a villager 10 m off at 10 health gained 57
(12 at once, 45 over 4 s), one 25 m off nothing, she healed herself too. First-person and third-person swings looked at.

## What step 4 (Barrier Shield, Shield Bash) taught
- **The barrier is her state, not an entity:** a rectangle 0.9 m ahead, square to her facing, 1.51 x 2.64 m (the deployed
  shield mesh measured: no number in any graph). A hit on her or her ally crosses it when the way from its source to them
  passes through: a projectile's way in (6 m back along its flight), a blast's centre, an attacker's eyes.
- **A fully stopped hit must not happen at all:** with its damage cut to 0 at `hurtServer`'s head, Minecraft still knocked
  her 3 m back, and the next arrow reached the villager behind her. Now a cancellable hook takes it first: all of it, no
  hit and the projectile gone; part of it, the rest lands as its own hit (`Barrier.passing` stops it counting twice).
- **TNT is a blast with a direct entity and no attacker:** it went through until any non-creature hit with a source
  position counted as a blast.
- **Explosions push apart from their damage** (`ServerExplosion.hurtEntities`: `pushFromExplosion`, and a player's push
  sent to their client through `hitPlayers`): `ServerExplosionMixin` zeroes both behind an up barrier.
- **Broken, it refills through its lockout** (from 2 s after, at 85/s: 0EE4 has no other condition): it first came back
  up with 5 health.
- **Shown where it blocks:** hung on her hand's bone it stood along her arm over her head; drawn at the rectangle instead,
  upright, the frame unfolding (its deploy clip then idle_up). In first person drawn in the camera's frame, ahead.
- **Shield Bash on her client** (as Lifeweaver's dash, `OwMovement.dash` now asks both): 0.3 s at 22 m/s; the server ends
  it early on a hit ("brigitte_bash_end"). Measured 5.3 m on open ground (D reading about 6.6 m, M; W 8.5 m).

Played (step 4, dev client, survival, flat world `shield1`, every mob killed first, the barrier refilled between cases):
- Arrows from a skeleton 12 m ahead, shield up: absorbed (300 -> 257), no knockback; down: hit (43 after armor) and
  pushed back; from behind with it up: hit (earlier run). An allied villager behind her: untouched over 9 s (two arrows
  absorbed).
- A zombie walking up: its hits into the barrier (37.5 each); once it walked round to her side, its hits reached her.
- A creeper's blast 3 m ahead: absorbed whole (300 -> 98), no push after the fix (4 m before it); down: 202 to her.
- Two TNT ahead: through it before the fix; after, the barrier broke and the rest killed her (beyond 300).
- `/damage 30 explosion at` 3 m ahead (375): the barrier 300 -> 0 and broken, 68 to her (75 less armor); held after: down
  for 5 s while refilling (56, 145, 234), up at 5 s with 255.
- Speed: 5.48 m/s down, 3.89 up (30% slower: 3.85).
- Shield Bash at a NoAI zombie 4 m ahead: she stopped on it, 70 to it, 5 s cooldown; a press on cooldown: nothing; into open
  air: 5.3 m; at a live husk: 70 and pushed 2.8 m.
- Seen: the unfolded frame before her in third person (front) and first person.
