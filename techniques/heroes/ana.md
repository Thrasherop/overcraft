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

## Building her (2026-10-29: step 3, Take Aim)
- **Slowed movement never reached its speed** (every hero, not only hers). The mover's friction works on at least
  `STOP_SPEED` (4.5 m/s), and acceleration is 35 x the wish speed. At a 65% slow the acceleration can't beat the floor's
  friction, so a scoped walk settled at 35 x 1.925 / 60 = 1.12 m/s instead of the wiki's 1.925. `Mover.slowed(factor)`
  scales the floor with the speed factor; `OwMathTest.slowedRunReachesItsSlowedSpeed` covers it. Read speeds from the
  velocity (`vz` x 20), not from positions between harness calls: their timing is loose.
- **Her scoped stands are turn-in-place sheets too** (3C5D / 3C9C, like her plain ones): on the clock they turned her
  120 degrees away. The animator holds `idle_scoped` / `crouch_idle_scoped` at frame 0 as it does `idle`.
- **Zoom and sensitivity live on the client:** `ClientAna` scales the FOV toward 30 degrees with the synced scope and the
  mouse turn to 30% fully scoped (`HeroFovMixin`, `MouseHandlerMixin`), in first person only.
- **No scope overlay yet:** its texture isn't extracted (her effects step). Built since: [#90](#90-her-scope-2026-11-04).

## Building her (2026-10-29: step 4, Biotic Grenade)
- **Healing modifiers live in `Combat.heal`,** so every hero's healing is boosted or blocked (Ana's grenade affects all
  healing in Overwatch), and Minecraft's own (food, potions) isn't: the owner's open question, defaulted.
- **Shields regenerating isn't healing:** an anti-healed Lifeweaver still regains his shields 3 s after the hit (they set
  health directly). A gain right after a test can be that: log health every 0.2 s through a test to tell a heal's steps
  from the shields' steady 30 a second.
- **Superflat test worlds:** kill every slime (`/kill @e[type=minecraft:slime]`, no distance) before precise tests. They
  pushed her off her spot, so her aim missed the target, and they hurt her. `spawn_monsters false` stops new ones only.
- **`/publish false 25565`** opens the world to the second client in 26.3; `/publish false survival 25565` is rejected.

## Building her (2026-10-29: step 5, Sleep Dart)
- **Sleep is its own shared system** (`hero/Sleep.java`, the data's crowd-control graph 224E): a mob's AI is switched off
  and its target cleared (both restored on waking), a player gets the synced `ASLEEP` attachment: their client zeroes
  movement (`KeyboardInputMixin`) and the server ignores their hero's keys (`HeroManager.controls`). Damage wakes only
  after 0.5 s asleep (earlier hits are dropped, not saved) and the waking takes 0.5 s. Played: a walking zombie stopped
  about 5 s; a hit 0.3 s in was ignored, one at 2.4 s woke it 0.5 s later.
- **Held buttons survive the sleep:** a player holding fire through it fires again on waking, as in Overwatch. The
  server first dropped held buttons whenever the keys weren't live (that's for G leaving hero mode), so a sleeper woke
  idle. Now `onInput` records held buttons before the controls check, and `tick` clears them only when not asleep.
  Played against a second client holding forward and fire: frozen at one spot with its ammo unchanged for about 5 s,
  then walking and firing again with no new press.
- **The pistol is a second held model** (`HeroWeapon.sidearm`): model 1175E on her hardpoint 0143 (1P) / 00A6 (3P), both
  Weapon_R with no offset or turn in the models' own tables, out from the cast's start until 0.7 s (0BDD #105 / #116).
  Its own clip 3DC2 isn't in the extract: it holds its rest pose. Without it the cast showed a bare fist at the screen's
  edge. Played: 1P it comes up aimed at the crosshair for the 0.3 s shot, kicks up, and is gone by 0.7 s; 3P the same
  from the front.
- **A new model in the pack loads only if `HeroModels` lists it:** `ana_pistol.glb` sat in the pack unused until it was
  added to the names loaded for Ana. The log line `Hero model <name>: ...` at start is the check.

## Building her (2026-10-29: step 6, Nano Boost)
- **Her ult cost was missing from `Combat.ultCost`** (its `default -> 1`): her charge stopped at 1 point until Nano was
  built. A new hero needs its case there, or the HUD's meter (from the sync's own cost) never fills.
- **The buff is `ana/Nano`:** +50% damage dealt added into Mercy's boost as one sum (`Boosts.damageFactor`; Mercy is still
  credited only her own share) and half damage taken (the same `hurtServer` hook in `LivingEntityMixin`, on the victim).
  It ends at 8 s or when the target dies; Ana's death doesn't end it. Played: a wolf boosted took 1 from a 2 hit, a hit
  "by" it took 5.92 from a zombie (6 less armour) and 3.94 after the 8 s; a villager, a teammate player and herself the
  same; Ana killed mid-boost, the teammate still took 2 from a 4 hit.
- **The pick:** `Targeting.allyInView(..., allowed)` skips allies already boosted; none in view, herself (the owner's
  default); both boosted, nothing is spent. Played in that order with three Q presses at a boosted wolf.
- **Tactician:** healing while full raised her charge 2300 -> 2412.5 (150 healed x 0.75), and Q left 112.5 of it.
- **Harness keys:** `secondary_fire` is the left mouse button and `primary_fire` the right (Ana's scope): holding
  "primary_fire" to shoot only scoped her. Look at `"key"` in the reply before trusting a run.
- **A wolf summoned with `Owner` is tame but has 8 health** (a tamed wolf's 40 comes from taming): a 20 hit killed it.
  NoAI zombies still burn in daylight, which shows in health read seconds apart.

## Building her (2026-10-29: step 7, perks)
- **Local Anesthetic** (`ana/Anesthetic`): the dart bursts where it hits (a creature or a block); 0.15 s later every enemy
  within 4 m in sight takes 45 in 30 ticks over 3 s and is slowed 40% easing to 20%. Its damage runs inside
  `Sleep.quietly`, so it never wakes a sleeper. Played: a walking zombie darted slept its full 5 s through the ticks
  (5 + 45 taken); one 1.5 m away took 45, its speed 0.142 -> 0.181 over the 3 s then 0.23; one 5.5 m away nothing.
- **A changing buff amount never applied:** `Combat.setModifier` only added a modifier that wasn't there, so the slow
  stayed at 40%. It now replaces one whose amount differs. That also fixed Lucio's Amp It Up on the speed song: the
  amped +60% never replaced the +25% already on (played after the fix: 0.125 -> 0.16 -> 0.125).
- **Speed Serum:** `Combat.buff` +30%, Ana from the cast to the boost's end, the target for its 8 s. Played: her speed
  0.13 then 0.10, a 1 s run 7.46 m against 5.61 after; a villager 0.5 -> 0.65 -> 0.5.
- **Biotic Bounce:** the first burst bounces it (off a block mirrored and slowed, off a creature up: how far is ours, no
  source has it) and its next contact bursts for 60 with the same 4 m and 3 s. Played: a zombie 20 -> 12.92 -> 8.19.
- **Headhunter:** x2 when the hit point is in the top of the target's box (0.25 m under the eyes and up, as Kiriko's
  kunai), darts and scoped shots alike, enemies only. Played: body 5.59, head 11.33, the same scoped.
- **Picking perks in the harness:** `cmd /overcraft perks`, then hold `perk_menu` and press `secondary_fire` (left
  choice) or `primary_fire` (right). The state doesn't list the picks: check them by their effects. Heroes switch only
  at the spawn point (`hero <id>` elsewhere is ignored).
- **Not reproduced:** once, a dart missed a zombie walking straight at her (no sleep, no damage); the same setup run again
  hit, and later runs did too.
- Emotes: [ana_emotes](../emotes/ana_emotes.md).

## #90: her scope (2026-11-04)
The owner's playtest: "still no scoped in UI". Reproduced on `next` (`hp_a_scoped`, worktree `run/screenshots`): zoomed, the generic
"+", nothing else.
- **The overlay (`HeroHud.anaScope`, `HudShapes.SCOPE` / `SCOPE_RIM`):** the data shows UX view model 0613.116 on the HUD root from
  scope 0.8 (0B94 #278) with its style 9C89, its skin reticle E615 (base 0A82.116, `CVGetSkinOverride`) and E77D; 0613, its base 05BE
  and 0A82 hold bindings only, no texture at any depth, and none is in her GUI export. So when it shows is the data's; the look is ours:
  a round lens 470 px in radius at 1080 high, dark past a soft edge, a thin cool rim, her reticle (fine ring, centre dot, lines to
  the sides and down), fading in between 80 % and 100 %, under every HUD widget. A UX `.116` is a schema of bindings; its art is elsewhere
  in the client's UI, not in what DataTool gives.
- **Zoom: 30 is vertical, and it was right.** Overwatch keeps the vertical view and widens with the screen, and its 103 setting is
  horizontal at 16:9 (70.5 vertical). Players' measurements give Ana and Widowmaker 51 degrees horizontal at 16:9 scoped, one thread saying
  outright "30 vertical", and 2 atan(tan 15 x 16/9) = 50.9. The audit's "17 vertical" reading would zoom about twice as far.
  Sources (community, not Blizzard; the wiki has no number): [Blizzard forum](https://us.forums.blizzard.com/en/overwatch/t/ashe-has-60scoped-and-widow-ana-have-30-please-explain/243164),
  [mouse-sensitivity.com](https://www.mouse-sensitivity.com/forums/topic/268-overwatch-2/page/9/). Minecraft's FOV is vertical: the
  multiplier takes it to 30 from any FOV setting.
- **The zoom followed Minecraft's easing** (`Camera.tickFov`, halfway a tick, meant for the speed widening), trailing her 0.25 s by
  2-3 ticks: `ScopeFovMixin` sets it straight while scoped. `trace` read the sync: 0.2, 0.4, 0.6, 0.8, 1.0 over five ticks, out in 4.
- **The view model isn't drawn fully scoped** (0B94 #199, M); 3B92 already takes the arms out of view, this keeps any edge from
  swinging into the lens.
- **A trap in timing it:** each `screenshot` stalls the game about 0.2 s, so spaced screenshots made the scope look 0.5 s late. Time with `trace`.

Played (`hp3_a*`, `hp6_a*`; dev client, picked from the menu, rejoined, day and night): scope on a NoAI zombie at 30 m: 20 -> 14.41, ammo
15 -> 14; a villager at 20 m 4 -> 10 (75 = 6) through the lens; a scoped walk 1.9 m/s; a jump; R and Sleep Dart take it down; G while
scoped: plain Minecraft, no lens, normal zoom, and back; third person: no lens; at 1280x720 the lens fits.

What a critic should check:
- **Side by side with Overwatch: the lens's size and the reticle's shape are by memory, not a frame.** They're one method to change.
  Skins change the reticle in the game (E615); we have one.
- Not tried: a second player, dying while scoped, Nano Boost's cast while scoped (it unscopes by code, not looked at), ultrawide.
