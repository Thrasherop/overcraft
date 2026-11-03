# The kill feed, the kill marker and the HUD around a kill

Up: [techniques map](../README.md) · beside: [hand_play](../testing/hand_play.md), [harness](../testing/harness.md),
[qa_audit_2026-11-02](../testing/qa_audit_2026-11-02.md) (findings X1-X5, X14), the review
[critique_killfeed](../testing/critique_killfeed.md) · code: `hero/Blow.java`, `hero/Combat.java` (`damage`, `blow`,
`pushed`, `recentPush`, `pushDeath`), `hero/KillFeed.java` (`onDeath`, `weapon`, `iconTokens`), `hero/Voice.java`
(`onKill`), `hero/Effects.java` (`hitMarker`, `killMarker`), `client/hud/KillFeedHud.java`, `client/HeroHud.java`
(`track`, `targetMarker`, `eliminatedCaption`) · tests: `hero/KillFeedLogicTest`, `BlowTest`.

## What Overwatch shows (the source)
The wiki's "Elimination Feed" page (read through the MediaWiki API, revision of 2026-06-19; the API refused a re-read
later the same day, HTTP 402) and the owner's #33:
- **Weapon kills: only the arrow**, any fire mode ("Alternate fire such as aiming in with Widowmaker or Ashe ... will
  not show a unique icon"). Ana's GUI export does hold a rifle silhouette (`Weapon_AMAR_BioticRifle`, 376x128,
  `research\ana\work\fx_sheets\gui_ana.png`); it is the HUD weapon panel's art (every hero's weapon slug in the pack is
  such a wide silhouette), not a feed icon. The audit's X3 guess was wrong.
- **The exceptions the wiki lists by name:** "Sojourn via Charged Shot, Disruptor Shot or Overclock" (her Railgun's
  secondary fire has an icon of its own), and "Heroes, like Venture, Junker Queen, Zenyatta and Jetpack Cat, who have
  an ability that modifies their quick melee attack will display the ability icon instead" (Claws Out).
- **Quick Melee: the fist.** For every hero, Brigitte's flail swing on V included.
- **Abilities, perks and ults that deal damage: their icon**; the ult's "animated similar to the one shown when it is
  fully charged" (ours: a gold-white pulse, 1.5 Hz). The list for our heroes: Ana Sleep Dart, Biotic Grenade, Groggy
  (a perk); Brigitte Shield Bash, Whip Shot, Whiplash (a perk); Jetpack Cat Claws Out, Purr, Catnapper; Juno Pulsar
  Torpedoes; Lifeweaver Superbloom (a perk); Lúcio Soundwave; Sojourn Charged Shot, Disruptor Shot, Overclock.
- **Critical hit: a small red target** after the icon.
- **A stun or knockback that the world finishes** ("Any attack which stuns an opponent or knocks them back will be
  shown when a player is eliminated by an environmental hazard because of it"): the hero and that blow.
- **No killer at all: the skull, the victim alone; a mob's kill: its ranged kill only the arrow, its bite the fist.**
  The wiki's "only an arrow" for a death by the world alone is overruled by #33's table, which the owner accepted: the
  owner's default for now (2026-11-02).

## The rule (one for every hero)
Every hero damage goes through `Combat.damage(target, source, ow, Blow)` (or `damagePart`); the three-argument form is
gone, so a new damage path doesn't compile until it says what dealt it. A `Blow` is:
1. **kind** — what the hit is in the game: WEAPON (any fire mode), MELEE (Quick Melee), ABILITY, PERK, ULT.
   - Damage that only exists while the ult runs is the **ult's** (Overclock's charged shots, Rally's bash shockwave).
   - Damage that only exists because of a perk is the **perk's** (Superbloom, Whiplash, Biotic Bounce's second burst,
     Local Anesthetic, Claws Out's wound).
   - Claws Out's *swing* is still Quick Melee (kind MELEE): Voice says its melee-kill line for it, as before this
     branch. Its wound tick is the perk's (no melee line: it isn't a swing).
2. **icon** — the art slug, or none. Weapon and melee show none, except the two the wiki names (Charged Shot,
   `weapon:charged_shot`; Claws Out, `melee:claws_out`). Anything branching on kind (Voice, later ult charge or stats)
   still sees a weapon or a melee.
3. **crit** — the red target.
4. **hero** — who dealt it, fixed when it was made (`.by(Hero.X)` at every hero file's call site; QuickMelee fills it
   from the attacker). A hero switch before it lands (a dose, a torpedo, a field, a fall) keeps the dealing hero's
   portrait and icon.

Wire format (`KillFeedPayload.weapon`, 32 chars max, checked by `BlowTest`): `<kind>` or `<kind>:<slug>`. The client draws
`KillFeed.iconTokens(...)` (unit-tested): skull (no killer), fist (`melee`), the hero's art for a slug (abilities, then
perks; with no art, the name in small capitals), nothing for `weapon`, then the red target.

| Hero | Weapon (arrow, crit where it can) | Melee | Abilities / perks / ult (icon) |
|---|---|---|---|
| Kiriko | kunai (crit) | fist | none deal damage |
| Lúcio | Sonic Amplifier (crit) | fist | Soundwave |
| Jetpack Cat | paws (crit, not the centre shot) | fist; with Claws Out ready: `melee:claws_out` | Purr; Claws Out wound (perk); Catnapper (ult) |
| Mercy | blaster (crit) | fist | none |
| Lifeweaver | thorns (crit) | fist | Superbloom (perk) |
| Ana | rifle dart and scoped shot, the 0.5 s dose (Headhunter crit) | fist | Sleep Dart; Biotic Grenade; Biotic Bounce's second burst (perk); Local Anesthetic (perk) |
| Sombra | pistol (crit) | fist | Cyberspace; EMP (ult: 40% of current health, can't kill) |
| Brigitte | Rocket Flail (left button) | fist (the flail swing on V, `HeroState.flailMelee`) | Shield Bash; Rally's bash shockwave (ult); Whip Shot; Whiplash (perk) |
| Juno | Mediblaster (crit) | fist | Pulsar Torpedoes |
| Sojourn | Railgun (crit); Charged Shot `weapon:charged_shot` (crit) | fist | Disruptor Shot; charged shots in Overclock (ult, crit) |

### The kill marker and ELIMINATED
Sent from the death itself (`KillFeed.onDeath` → `Effects.killMarker`), after the feed line, to the credited hero when
the kill was a hero blow or they're in hero mode. `Effects.hitMarker` sends nothing for a dead target. Before, kind 2
came only from code that marked its own hits, so Soundwave, Purr, Claws Out, Catnapper, the Disruptor field and Ana's
doses (75 over the 0.5 s after the hit's marker) never marked a kill (#106). The killer always gets their own line,
however far, and the client's ELIMINATED takes the victim's name from it when the victim is past tracking range.

### A death the world deals
`Combat.pushed` records a hero's push: a hit that **landed** (dealt more than 0, so a barrier that took it all or a
cancelled hit doesn't count) and **moved** the target (its knockback inside the hurt), or an ability's own push after
its damage (Soundwave, Sonic, Purr, Catnapper, Shield Bash, Rally's shockwave, Whip Shot). A death within 100 ticks
(Minecraft's own credit window) is that hero's, with that blow (crit cleared), only if:
- its type is one a push can cause (`Combat.pushDeath`): a fall (IS_FALL), the void, lava, a wall (`in_wall`,
  `fly_into_wall`). Not sunburn or fire, drowning, starving, cramming, a dispenser, `/kill`;
- no other attacker hurt it after the push (`getLastHurtByMob` / its timestamp): a skeleton's arrow after the boop
  takes the kill.
The credit goes to Voice too (`Voice.onKill`, called by KillFeed): the elimination line, the multikill chain,
Overclock's count.

### The Ender Dragon
Its death runs inside the hurt too: 26.3's `LivingEntity.die` calls `EnderDragon.handleKillingBlow`, which sets health 1
and the DYING phase, and Fabric's AFTER_DEATH fires there (VICTORY, #18, hangs on it). So the final blow is told
with its icon like any other; the 200-tick death animation doesn't matter. Read from the decompiled source; not played.

## The HUD around it
- **Swift Step's teal flash** is Kiriko's alone (`track(s, hero)`) (#107). Sigma's Kinetic Grasp (feature/sigma) needs a
  look after that merge.
- **`targetMarker` takes the ability's key** (#108): both callers today are Ability 1, so no player saw a wrong key.
- **"ELIMINATED" at +250 under the crosshair** (ours): at +205 it ran through Lúcio's song indicator (+191). Not checked
  against Kiriko's Swift Step marker or Juno's and Jetpack Cat's meters.

## Merging into feature/sigma and feature/dva (for whoever merges)
Both branches also changed `Combat.credit(hero, dealt)` to `credit(hero, dealt, target)` next to every damage line, so
14 files conflict (Ana, Anesthetic, Brigitte, SleepDartEntity, QuickMelee, JetpackCat, Mediblaster, Torpedoes,
Lifeweaver, Lucio, Disruptor, Railgun, Cyberspace, Sombra): keep both changes. Then these 3-argument calls no longer
compile; give them these blows (line numbers from the critique's `git merge-tree`, 2026-11-02):

| Branch, file:line | Call | Blow |
|---|---|---|
| sigma `Hyperspheres.java` 118 (`damagePart` and `damage`) | direct hit | `Blow.WEAPON.by(Hero.SIGMA)` (his primary: no icon) |
| sigma `Hyperspheres.java` 149 | burst | `Blow.WEAPON.by(Hero.SIGMA)` |
| sigma `Accretion.java` 126 (both) | impact | `Blow.ability("accretion").by(Hero.SIGMA)` + `Combat.pushed(...)` after its knockdown push |
| sigma `Accretion.java` 162 | burst | `Blow.ability("accretion").by(Hero.SIGMA)` |
| sigma `GraviticFlux.java` 314 (both) | the slam | `Blow.ult("gravitic_flux").by(Hero.SIGMA)` + `Combat.pushed(...)` for the lift (the void case: Voice's "To the void!" now gets the credited killer through `Voice.onKill`) |
| sigma `SigmaPerks.java` 77 | Hyper Strike | `Blow.perk("hyper_strike").by(Hero.SIGMA)` |
| dva `FusionCannons.java` 169 | pellets | `Blow.weapon(crit).by(Hero.DVA)` |
| dva `LightGunShotEntity.java` 78-79 (both) | Light Gun | `Blow.weapon(crit).by(Hero.DVA)` |
| dva `Boosters.java` 115 | ram | `Blow.ability("boosters").by(Hero.DVA)` + `Combat.pushed(...)` |
| dva `MicroMissiles.java` 138 (both), 166 | missiles | `Blow.ability("micro_missiles").by(Hero.DVA)` |
| dva `Dva.java` 422 | Call Mech landing | `Blow.ult("call_mech").by(Hero.DVA)` |
| dva `SelfDestruct.java` 104 | blast | `Blow.ult("self_destruct").by(Hero.DVA)` |
| dva (Sigma files too, slightly different lines) | as above | as above |

The slugs are the pack's (`heroes/sigma/abilities/accretion.png`, `gravitic_flux`, `perks/hyper_strike`;
`heroes/dva/abilities/boosters`, `micro_missiles`, `call_mech`, `self_destruct`). Add them to `BlowTest`'s expected list.
Check that `Hero.SIGMA` / `Hero.DVA` are the enum names on those branches.

## Play records
Overwatch and Battle.net checked closed before each session. `runClient2` (port 25600, `run2/`), the Ana checkout's
pack. All input went through the harness (`tools/dev.py`, scratchpad `kf_run.py` / `kf_*.txt`). **Not real input:**
desktop control of the game window (java.exe) was requested for the second pass and the request was denied, so no
real-mouse play was done. That is the critique's blocker, still open.

### First pass, 2026-11-02 (`kf_*`, sheets `kf_sheet_*`)
- **Reproduced on the unchanged branch** (`kf_sheet_repro*.png`; the flash in `kf_r107_ana_flash_1.png`): a Disruptor
  kill showed the fist and no ELIMINATED, an Ana rifle kill the same, a Railgun kill the fist, and the teal flash
  appeared at Sleep Dart 7.0 s.
- **After the fix:** every hero and path against NoAI 1-health zombies (`kf_sheet_kiriko … _soj11`). Two sheets are
  unreliable as evidence, per the critique: `kf_sheet_soj3` (empty frames whose file names were reused) and `kf_sheet_107`
  (before and after frames mixed, unlabelled).
- `kf_sheet_brig` shows the Brigitte melee **bug** (the arrow alone), fixed in the second pass.

### Second pass, after the critique (`kf2_*`, sheets `kf2_sheet_*`)
**Ways in:**
- Brigitte was picked from the select screen when rejoining (clicking her portrait, then READY).
- Sojourn, Jetpack Cat, Lúcio and Kiriko were picked from the H menu at a bed (`kf2_hmenu.png`).

**Rule changes, each seen:**
- Brigitte's Quick Melee showed the fist; Shield Bash its icon; Rally's shockwave the pulsing Rally icon
  (`kf2_sheet_brig`).
- Charged Shot showed its own icon (`kf2_sheet_soj`).
- Claws Out's swing showed the claws icon with ELIMINATED (`kf2_sheet_cat`).

**World credit:**
- A Soundwave boop off a 29-block pillar was credited "VIEWER [Soundwave] > ZOMBIE" with ELIMINATED (`kf2_sheet_fall`).
- The same boop, then a switch to Kiriko through the H menu while the zombie fell: the line kept Lúcio's portrait and
  Soundwave, ELIMINATED included (`kf2_sheet_sw`; Kiriko's arms are visible in the frames).
- A hit confirmed by a health read (10 → 5.28), then `/kill`: no line (`kf2_genkill_*`).
- A kunai hit confirmed by a health read (7 → 2.28), then `/damage ... minecraft:on_fire`: no line. Then
  `/damage ... minecraft:fall` after a kunai hit: credited, the arrow alone (`kf2_sheet_types`).

**Deaths of the hero (#33):**

| Killer | What the feed showed | Sheet |
|---|---|---|
| A zombie with AI, biting | ZOMBIE [fist] > VIEWER | `kf2_bite_*` |
| A skeleton with a bow, shooting ("was shot by Skeleton") | the arrow alone | `kf2_sheet_skel` |
| A bowless skeleton (`/summon` gives it none), hitting | the fist | — |
| A fall | [skull] > VIEWER | `kf2_fallself_1`, `kf2_skel_*` earlier |

**AI mobs:**
- A slowed zombie with AI was killed by the Disruptor field (`kf2_sheet_fight`).
- The scripted creeper exploded before its melee kill, twice, and left a crater; scripted aim at moving mobs is
  unreliable (harness trap).

**Minecraft mode:**
- A kill dealt by `/damage ... minecraft:player_attack by @s` in Minecraft mode sent the line (the fist, with the
  hero's portrait) and no ELIMINATED (`kf2_sheet_mc`).
- This is a command, not a swing. Whether the hero's portrait belongs on a Minecraft-mode kill is open.

**Seen in passing, not this branch's:**
- After respawning, the hero came back with 14-16 of 20 health on several respawns. Possibly a bug; worth an issue
  check.

## Not tried, and what a critic should check
- **Real input** (the blocker above).
- **A second player's view:** colours, the `byYou` frame, a hero killing a hero, assists. One client only.
- **Not played:**
  - a real creeper kill;
  - the Ender Dragon;
  - Biotic Bounce's second burst kill (the bounce's path can't be aimed by script);
  - a delayed kill (dose or field) after the killer died;
  - a kill while emoting;
  - lava, drowning or the void after a boop.
- **Voice credit for a world kill** isn't confirmed by ear. `heard` didn't capture a line in the scripted runs.
- **Display, open:**
  - the ult pulse, seen only in stills;
  - perk icons are drawn smaller than ability icons in the same slot (their art's padding);
  - the text fallback's width;
  - whether Overwatch adds new lines at the top and frames your own deaths: not sourced.
- **#110:** Charged Shot's false headshots (not this branch).
