# Sprays (#41): which ones, how to extract them, how Overwatch 2 places them

Research of 2026-11-17 against the archived build 154088 (DataTool 2.25.0.0, `--online=false`, Overwatch and Battle.net
closed). Text only: the images live in `local-assets/sprays/` (gitignored), never here. Up:
[comms-reference](comms-reference.md) (the owner's "hold Y" note) · how the data is read:
[reading_game_data](../../techniques/game-data/reading_game_data.md).

## Which sprays every player has

**From the data:** DataTool's `list-unlocks` puts each hero's unlocks in groups; the `LevelUnlocks` group with
`"Level": 0` is what an account owns from the start (the Overwatch skin, Heroic emote / pose / intro, DEFAULT weapon
are all there). `list-general-unlocks` has the same group (`AdditionalUnlocks`, Level 0) for account-wide items.
- **General (any hero), Level 0:** `Logo` (008D.0A5), `You are not prepared!` (0B13), `Día de los Muertos` (0CEF), plus
  29 Overwatch League / esports logos (category `OWL`, left out). The wiki agrees: "3 default general sprays
  automatically unlocked for every player" (Sprays page, revision 150471, 2023) and the "Default" row of
  Template:General Sprays (Logo, Día De Los Muertos, You Are Not Prepared!).
- **Hero sprays, Level 0:** only two in the whole list: Tracer `Lena and Emily` (611F) and Lifeweaver `Dayglow Salim`
  (C6C1). No other hero owns a hero spray at the start. Rarity doesn't tell (every spray is `Common`) and `ProductId`
  is on every unlock, so neither is the rule.

**What a player gets by playing the hero, no currency** (that is our "default" set, 4-5 per hero):
- **Hero progression, levels 3 and 7** (wiki, Player Progression: level 3 "Spray - Hero Icon", level 7 "Spray"). The
  data doesn't name which spray each level gives (the progression reward lists weren't found in the unlock JSON); the
  wiki's per-hero templates (Template:<Hero> Sprays, "Progression" row) name them: the `Icon` and one more. Jetpack Cat
  has none listed; Juno's are `Star Glider` and `Greetings` (no Icon).
- **Hero challenges:** `Cute` and `Pixel` (data category `Achievement`; the wiki's "Achievements" / "Hero Challenges" row).
- **Credits pool** (not default, 25-100 Legacy Credits each in OW2; wiki "Common" row): data category exactly `["Base"]`,
  in the `Base` loot-box group for older heroes and in `OtherUnlocks` for Juno and Jetpack Cat.

| Hero | Core set (extracted to `local-assets/sprays/<hero>/`) | Credits pool (`.../credits/`) |
|---|---|---|
| kiriko | Icon, Lo-fi, Cute, Pixel | 9 |
| lucio | Icon, Scratch, Cute, Pixel | 22 |
| jetpack_cat | Cute, Pixel, Innocent Fika, Pouting Fika (no progression sprays: its two credit sprays) | 0 |
| juno | Star Glider, Greetings, Cute, Pixel | 8 |
| sojourn | Icon, Back in Action, Cute, Pixel | 8 |
| mercy | Icon, Resurrect, Cute, Pixel | 25 |
| ana | Icon, Guardian (512 x 256), Cute, Pixel | 24 |
| sombra | Icon, 5kull, Cute, Pixel | 25 |
| dva | Icon, Power Up!, Cute, Pixel | 23 |
| sigma | Icon, Symphony, Cute, Pixel | 22 |
| lifeweaver | Icon, Have This Dance, Dayglow Salim (Level 0), Cute, Pixel | 7 |
| brigitte | Icon, Cat Person, Cute, Pixel | 21 |
| tracer | Icon, Cavalry's Here (512 x 256), Lena and Emily (Level 0), Cute, Pixel | 24 |
| pharah | Icon, Rocket Jump, Cute, Pixel | 22 |
| cassidy | Icon, High Noon, Cute, Pixel | 22 |
| junkrat | Icon, Firework, Cute, Pixel | 23 |
| illari | Icon, Burning Regret, Cute, Pixel | 7 |
| torbjorn | Icon, Get Ready, Cute, Pixel | 22 |
| ashe | Icon, Back-to-Back, Cute, Pixel | 21 |
| widowmaker | Icon, In My Sights, Cute, Pixel | 22 |

Totals: 82 core + 357 credits hero sprays, 3 general (`local-assets/sprays/general/`), 442 PNGs, 37.6 MB. All RGBA;
of the 439 hero sprays 427 are 512 x 512 and 12 are 512 x 256 or 256 x 512 (the mod should keep the aspect). File names are the data names in
snake case (`back_to_back.png`, `power_up.png`, `cavalry_s_here.png`). `local-assets/sprays/manifest.json` lists each
with its GUID, tier, reason and source file. The mod's `SprayEntity` reads `textures/sprays/<hero>/<name>.png` from the
local pack, so these still need copying into the pack (`build_asset_pack.py` doesn't know sprays yet).

## Extracting them (offline)
- `DataTool.exe "E:\Overwatch Archive\build-154088" extract-unlocks <out> "Kiriko|spray=*" ... --online=false --language=enUS`
  writes each spray as a PNG (`Heroes\<Hero>\Spray\<theme>\<Name>.png`, 512 px) plus some hero GUI images. Several
  `"<Hero>|spray=*"` queries go in one run (20 heroes: 76 s, 1.1 GB with the GUI images).
- General sprays come only from `extract-general`, which has no filter and goes on to every charm and souvenir model
  (it passed 7 GB before being stopped). It writes `General\Spray\...` first, so the script stops it once the three
  defaults exist.
- Script: `E:\Overwatch Archive\research\sprays\scripts\extract_sprays.py` (Python 3 with Pillow:
  `py "E:\Overwatch Archive\research\sprays\scripts\extract_sprays.py"`, `--skip-datatool` to only rebuild the PNGs). It
  checks that Overwatch and Battle.net are closed before each DataTool run. Inputs: the unlock lists
  (`research\kiriko-anim\unlocks\unlocks_all_heroes.json`, `research\sprays\general_unlocks.json`) and the wiki's
  progression names (`research\sprays\wiki\wiki_sections.json`, from the MediaWiki API).

## In the data: one shared spray effect (build 154088)
- A spray unlock is `STUUnlock_SprayPaint`; its `STUUnlock_SprayPaintEffect` names `m_effect` **000000000663.00D**
  (the same for every spray checked: Kiriko Icon, Tracer Icon, Logo, Lena and Emily) and a per-spray `m_effectLook`
  (0A8: the image). Scripts: `research\sprays\scripts\dump_spray_stu.py`, `dump_spray_effect.py`, `dump_spray_decal.py`.
- Effect 0663: three decal components (`ECDL`, each with a shader setup) of **Duration 5.0 s** from time 0, and a sound
  component at time 0 playing **000000002D8E.02C** (the spray sound bank; not converted or listened to yet). The 5 s is
  the effect's own length; the decal staying after that is the game's doing (inferred: in game sprays stay much longer).
  The decal chunks hold no readable size (0.1 and NaNs): **no size or range was found in the data.**
- Display strings (`dump-strings`): `SPRAY (UP)` 1D30.07C, `SPRAY (DOWN)` 54C2, `SPRAY (LEFT)` 54C3, `SPRAY (RIGHT)`
  54C5, `SPRAY MENU` 54C4, Workshop `Spray Up/Left/Right/Down`, `Is Communicating Any Spray`: four spray slots, one
  menu key (the spray wheel).

## How Overwatch 2 places sprays (public sources)
| What | Answer | Source |
|---|---|---|
| How many at once | One per player: a new spray erases the last one | wiki, Sprays (current page) |
| How long it lasts | Until the player sprays again **or switches heroes**; no timer is given | wiki, Sprays |
| Equipped | Up to 4 per hero, chosen in the Hero Gallery | Jan 24, 2017 patch (wiki page); wiki, Sprays |
| Picking one | 4 slots (up / down / left / right) on a spray wheel opened by a held menu key, or each spray bound to its own key | Jan 24, 2017 patch ("bind individual sprays ... to mouse buttons, unique keys"); data strings above |
| Default key | T in the original game ("Using default key T, the chosen spray appears instantly on most game surfaces") | wiki, Sprays, first revision (2016-06-26) |
| Appears | Instantly, on most surfaces; glass windows included (MEKA Base fix) | wiki 2016 revision; Oct 9, 2018 patch |
| Usable in any form | Wrecking Ball can spray while rolling (fixed) | Aug 9, 2018 patch |
| Some sprays have sounds | "Tracer's Comic Challenge sprays, loaded with their own special sound effects" | Sep 15, 2020 patch |
| Range, size on the wall, cooldown | **Not found** in patch notes, the wiki or the data | — |
| Floors / ceilings | **Not stated** in any source found; inferred yes (the 2016 text says "most game surfaces", nothing excludes floors) | inferred |

For the mod, the range, size and cooldown stay ours (as `SprayEntity` already marks them); the owner's
`comms-reference` "hold Y" stays the key. Lasting until replaced or a hero switch is Overwatch's rule; a long timeout
like ours is a Minecraft addition (a world doesn't end like a match), to keep as ours.

## Sources
- Overwatch wiki through its MediaWiki API (`overwatch.fandom.com/api.php`, `action=parse` / `prop=revisions`): Sprays
  (current, rev 150471, and the first revision 25278), Template:General Sprays, Template:<Hero> Sprays for the 20 heroes,
  Player Progression, Cosmetics, the January 24 2017, August 9 2018, October 9 2018 and September 15 2020 patch pages.
- Data: `list-unlocks` / `list-general-unlocks` JSON, `extract-unlocks`, `extract-general`, TankLib reads of
  0054EF.0A5, 00DF.0A5, 008D.0A5, 611F.0A5 and 0663.00D (build 154088).
