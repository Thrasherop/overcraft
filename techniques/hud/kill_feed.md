# The kill feed, the kill marker and the HUD around a kill

Up: [techniques map](../README.md) · beside: [hand_play](../testing/hand_play.md), [harness](../testing/harness.md),
[qa_audit_2026-11-02](../testing/qa_audit_2026-11-02.md) (findings X1-X5, X14) · code: `hero/Blow.java`,
`hero/Combat.java` (`damage`, `blow`, `recentBlow`), `hero/KillFeed.java`, `hero/Effects.java` (`hitMarker`,
`killMarker`), `client/hud/KillFeedHud.java`, `client/HeroHud.java` (`track`, `targetMarker`, `eliminatedCaption`).

## What Overwatch shows (the source)
The wiki's "Elimination Feed" page (read through the MediaWiki API, revision of 2026-06-19) is the rule, and it agrees
with the owner's #33:
- **Weapon kills: only the arrow.** Any fire mode: "Alternate fire ... will not show a unique icon". Ana's GUI export
  does hold a rifle silhouette (`Weapon_AMAR_BioticRifle`, 376x128, `research\ana\work\fx_sheets\gui_ana.png`); it is the
  HUD weapon panel's art (every hero's weapon slug in the pack is such a wide silhouette), not a feed icon. The audit's
  X3 guess ("the data says weapon silhouettes") was wrong; checked 2026-11-02.
- **Quick Melee: the fist.** A hero whose ability changes the melee shows that ability's icon (Jetpack Cat's Claws Out).
- **Abilities and perks that deal their own damage: their icon.** The wiki's list for our heroes: Ana Sleep Dart,
  Biotic Grenade (and its perk); Brigitte Shield Bash, Whip Shot, Whiplash; Jetpack Cat Claws Out, Purr, Catnapper;
  Juno Pulsar Torpedoes; Lifeweaver Superbloom; Lúcio Soundwave; Sojourn Charged Shot, Disruptor Shot, Overclock.
  (Sombra's list names Hack and Virus, which we don't have; her Cyberspace kills show its icon.)
- **Ultimates: the ult icon, animated** like the ready ult (ours: a gold-white pulse, 1.5 Hz).
- **Critical hit: a small red target** after the icon.
- **The world alone: only the arrow and the victim.** When a hero's stun or knockback sent them into it, the hero and
  that ability are shown.

## How it's built: one rule for every hero
Every hero damage goes through `Combat.damage(target, source, ow, Blow)` (or `damagePart`); the old three-argument form
is gone, so a new damage path can't compile without saying what dealt it. `Blow` is the kind (WEAPON, MELEE, ABILITY,
PERK, ULT), the icon slug the HUD already uses, and the crit flag.
- While the hurt runs, `Combat` keeps the blow being dealt; `KillFeed.onDeath` and `Voice.onDeath` (Fabric's
  AFTER_DEATH, called inside `LivingEntity.die`, inside the hurt) read it with `Combat.blow(victim, source)`.
- Each victim also remembers its last hero blow for 100 ticks (Minecraft's own kill-credit window). A death with no
  attacker (a fall, the void, lava, suffocation) goes to that hero with that icon, crit cleared. `/kill`
  (`GENERIC_KILL`) is excluded: a command isn't the world.
- **The kill marker and "ELIMINATED" come from the death, not the hit:** `KillFeed.onDeath` sends `Effects.killMarker`
  to the hero in hero mode; `Effects.hitMarker` no longer sends kind 2 and sends nothing for a dead target. Before,
  kind 2 came only from code that called `hitMarker` after its damage, so Soundwave, Purr, Claws Out, Catnapper, the
  Disruptor field and every Ana dose tick (her rifle lands 75 over 0.5 s, after the hit's marker) never marked a kill.
- Wire format (`KillFeedPayload.weapon`, max 32 chars, checked by `BlowTest`): `weapon`, `melee`, `ability:<slug>`,
  `perk:<slug>` (drawn from `textures/gui/heroes/<hero>/perks/`), `ult:<slug>`. No art in the pack: the name in small
  capitals in the icon's place.
- Non-hero killers: a blow by the killer's own hand (a zombie's bite) is the fist; anything else the arrow alone.

Which blow each path deals (the table a critic should check against the wiki list above):

| Hero | Weapon (arrow, crit where it can) | Melee | Abilities / perks / ult (icon) |
|---|---|---|---|
| Kiriko | kunai (crit) | fist | none deal damage |
| Lúcio | Sonic Amplifier (crit) | fist | Soundwave |
| Jetpack Cat | paws (crit, not the centre shot) | fist; Claws Out swing and wound: claws_out | Purr; Catnapper (ult) |
| Mercy | blaster (crit) | fist | none |
| Lifeweaver | thorns (crit) | fist | Superbloom (perk) |
| Ana | rifle dart and scoped shot, the 0.5 s dose (Headhunter crit) | fist | Sleep Dart; Biotic Grenade (and Biotic Bounce); Local Anesthetic (perk) |
| Sombra | pistol (crit) | fist | Cyberspace; EMP (ult: 40% of current health, can't kill) |
| Brigitte | flail, her melee too (no shared melee in her data) | (the flail) | Shield Bash and Rally's bash shockwave; Whip Shot; Whiplash (perk) |
| Juno | Mediblaster (crit) | fist | Pulsar Torpedoes |
| Sojourn | Railgun (crit) | fist | Charged Shot (crit); in Overclock the Overclock icon (L: which shots count as the ult's is our reading); Disruptor Shot |

## The HUD around it
- **Swift Step's teal flash** is Kiriko's alone (`track(s, hero)`): it fired 7 s before the end of any hero's ability 1
  cooldown (Ana's Sleep Dart, Sombra's Cyberspace, Lifeweaver's Petal Platform) (#107).
- **`targetMarker` takes the ability's key** (#108). Both callers today are Ability 1 (Swift Step, Guardian Angel), so
  no player saw a wrong key; the fix is for the next callers (Nano Boost, Repair Pack, Life Grip previews).
- **"ELIMINATED" sits at +250 under the crosshair** (reference px): at +205 it ran through Lúcio's song indicator (+191).

## Play record, 2026-11-02 (`runClient2`, `run2/screenshots/kf_*`, contact sheets `kf_sheet_*`)
Overwatch and Battle.net checked closed. Flat survival world `kf_play`, midnight, no spawns; targets NoAI zombies of
1 health at 8 m (body pitch 5.8°, head pitch -0.9°) unless said.
- **Reproduced first, on the unchanged branch** (`kf_sheet_repro*.png`, `kf_r107_ana_flash_1.png`): Disruptor kill =
  fist, no ELIMINATED; Ana rifle kill = fist, no ELIMINATED; Railgun kill = fist; the teal flash at Sleep Dart 7.0 s.
- **After, every hero and path** (`kf_sheet_kiriko, _lucio, _cat, _cat2, _cat3, _mercy_lw, _lw2, _ana, _ana2, _ana3,
  _ana6, _sombra, _brig, _brig5, _brig6, _juno, _soj, _soj2, _soj3, _soj6, _soj11`): each weapon kill arrow-only, each
  head kill with the red target, each melee the fist, each ability/perk/ult its icon, ELIMINATED and the kill marker on
  each (Soundwave, Purr, Claws Out wound, Catnapper, Disruptor, Ana rifle and Headhunter, Local Anesthetic included).
- **Environment:** a 5-health zombie booped off a 9-block pillar by Soundwave died of the fall: "VIEWER [Soundwave] >
  ZOMBIE" and ELIMINATED (`kf_sheet_env5.png`). A `/kill` 0.6 s after a Sonic shot at it: no line (that the shot hit wasn't confirmed by a health read).
- **Deaths and ways in:** Kiriko from the select screen on joining; every other hero by the dev `hero` command (the
  feed doesn't depend on how the hero was picked); the select screen on rejoin, Esc kept the hero; a zombie killing the
  hero ("ZOMBIE [fist] > VIEWER" with her portrait, `kf_death_respawn.png`); a kill after respawning.
- **Flash:** Ana and Sombra crossing 7 s: no flash (`kf_sheet_107.png`); Kiriko's step still flashes
  (`kf_107_kiriko_after_0.png`). Target marker: Swift Step and Guardian Angel show LSHIFT (`kf_108_*`).

## What was not tried, and what a critic should check
- **A second player's view** (the feed's ally/enemy colours, `byYou` frame, a hero killing a hero): one client only.
- **Minecraft mode (G):** the feed isn't drawn there; whether a Minecraft-mode sword kill should reach other heroes'
  feeds with the hero's portrait is open. The scripted attack didn't land.
- **The ult pulse** was only seen in still frames: check it moves, and that it reads as Overwatch's.
- **Lifeweaver's Life Grip, Juno's Orbital Ray, Mercy's Valkyrie, Kitsune Rush** deal no damage here, so no icon.
- One Charged Shot through a 1000-health zombie flagged a crit on a zombie 12.5 m behind at leg height
  (`kf_soj6`), but a leg shot without the farm didn't (`kf_soj11`). Not explained: Railgun's charged hit point,
  pre-existing, worth a look.
- Screenshot timing: a line and its ELIMINATED can land on different frames (a single shot once showed the line
  without the caption, `kf_ana_an_d`); read bursts, not one frame.
- Brigitte's first-person model reaches into the top-right corner under the feed (pre-existing, not the feed's).
- **Harness trap found:** with `send_command_feedback` off, a `/summon` with a bad NBT fails silently; and a `/kill`
  of a mob just hit used to credit the hero. Clear the world with the hero 5 s idle, or rely on the GENERIC_KILL rule.
