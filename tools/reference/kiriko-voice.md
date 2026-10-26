# Kiriko's voice lines: which moment, which target, how often

Which of Kiriko's English lines belong to which moment, which ones need a particular target hero, skin, map or event,
and how Overwatch picks between them. Written 2026-10-26 from the archived build 154088 (2.25.0.0), the public patch
notes and the Overwatch wiki (MediaWiki API only). Overwatch and Battle.net were checked closed before every read; the
archive was only read offline (`Online=false`) through DataTool's TankLib. Nothing was extracted or decoded to disk,
and no audio was copied: only text was written.

Confidence: **H** = read directly in the data or the patch notes; **M** = inferred from the data (one link guessed);
**L** = recollection or an unverified secondary source.

Ids are the sound's .0B2 index: `5EFA4` = `00000005EFA4.0B2`, file
`E:\Overwatch Archive\extracted\154088\HeroVoice\Kiriko\<folder>\00000005EFA4.0B2-<text>.ogg`. Stimuli (.078) are
written `FC0` = `000000000FC0.078`. Weights are the line's own `m_weight` (1 = normal).

## Short answers

1. **"Now I'm having fun" (5EB8F) is a Multikill line, not an On Fire line** (H). Base voice set 215.05F files it under
   stimulus 00C9 with "Fox in a hen house" (5EB82); the wiki's quotes page also lists both under "Multi Kill".
   Kiriko's **On Fire** lines are stimulus 0583: **"I'm on fire" (5EFCF, w 1), "The battle ignites me!" (5EF9C, w 0.3),
   "Aw, I feel all warm inside" (5EFD0, w 0.3)** (H). Why the owner links it to On Fire (M): a burst of kills usually
   sets a player On Fire, and Multikill's priority (70) beats On Fire's (60). When both happen at once, the multikill
   line wins and plays just as the flames appear. In the mod it plays on ordinary kills because `Voice.onDeath` calls
   any second kill within 4 s of the last one (of any mob) a multikill, on every kill of a chain, with no cooldown.
2. **Hero-specific lines are real and must be enforced** (H). 5 of the 12 Swift Step lines in today's soundmap are
   hero-gated: "Rolling with you" (Wrecking Ball), "How's the hunt?" (Widowmaker), "Howdy" (Ashe or Cassidy), "I'll be
   your angel" (Mercy) and "Your pocket ninja's here" (Genji). 5 of the 25 heal lines are gated too, and 4 moment lines
   belong to two skins' voice sets. Details below and in "What today's soundmap gets wrong".
3. **On Fire exists in Overwatch 2.** It was missing at OW2's launch and returned on June 13, 2023 (Season 5) with a
   higher "Blazing" state (H, Blizzard). Blizzard gives what feeds it, but no thresholds or decay numbers. The only
   numbers are OW1's, from the wiki (L-M). See "On Fire".
4. **Multikill: no public number for the window or the count.** Blizzard fixed the multikill voice in Dec 2023 / Jan
   2024 without defining it. The data does not say it either: it is engine-counted, behind unnamed variables. The
   stimulus' own cooldown (15-20 s) is in the data (M). The window has to be ours. See "Multikill".

## How Overwatch picks a line (what the data shows)

- A **moment** is a voice stimulus (.078, `STUVoiceStimulus`). It has its own `m_priority`, `m_radius` (m), `m_volume`,
  `m_interrupt`, optional `m_criteria`, and two unnamed floats `m_A73F2EBE` / `m_DD27AC95`. Those two read as a
  **cooldown range in seconds** (min, max) (M): pain grunts 0.5-1, the melee shout 0.5, "under attack" 60-90,
  communication-wheel lines (Thanks, I need healing, ult status) 0/0 with `m_interrupt` 1.
- A **statescript voice node** (`STU_40E05F6B`) says the stimulus. Its field `m_8C8C5285` carries the **target
  entity** (H that the field is set, M that it is what "Hero Interaction" tests: it has the same field hash as the
  criterion's hero field). Swift Step passes its target (var 31707), the heal subgraph the healed ally, kill lines
  the victim (var 393). Its float `m_B0263D2D` is FC0 0.05, FC4 0.15, kill lines 0. It is **not a play chance**:
  kill nodes have 0 and still speak. Most likely it is a delay in seconds (M). No random gate sits in front of the
  Swift Step, Suzu or heal voice nodes (H).
- A **line** (`STUVoiceLine` in voice set 215.05F) has its stimulus, `m_criteria` and `m_weight`. 808 of 882 lines
  weigh 1.0; others weigh 0.001-0.85.
- **Pick (M):** a weighted random draw over every line of the stimulus whose criteria pass, lines without criteria
  included. Evidence: lines without criteria have weights below 1 too (Suzu "Cleansed" 0.1, heal "Be healed" 0.1), so
  weight is not a gate. And single hero lines get 0.1, 0.3 or 1.0, which only matters if they compete with the generic
  lines. So a hero line is **only for that hero**, and when it applies it **joins** the generic pool: it is an extra
  chance, never a replacement. The pick itself is engine code and was not read.
- **Skins** swap the whole voice set (H that the sets exist, M that a skin swaps one in): 228.05F (base lines plus
  fortune-teller lines, referenced by skin theme 74DC.0A6) and 236.05F (base plus sun-goddess lines; which skin was not
  found). Skin lines have no "own skin" criterion; they
  only exist in that skin's set. Which skins these are by name was not resolved (L: the texts suggest a tarot/fortune
  skin and the Amaterasu sun-goddess skin).

### Criteria types (raw STU and DataTool's `-criteria.txt` names)

| `m_type` | STU type | Field | DataTool text | Meaning |
|---|---|---|---|---|
| 11 | `STU_D815520F` | `m_8C8C5285` = hero .075 | `Hero Interaction: <hero>` | the moment's target entity is this hero (H) |
| 25 | `STU_3EAADDE8` | `m_hero` = hero .075 | `Hero On Team: <hero>` | this hero is on Kiriko's team (M) |
| 4 | `STU_7C69EA0F` | `m_amount`, `m_criteria[]` | `Nested - n/m Required` | at least n of the m children (H) |
| 6 | `STUCriteria_Statescript` | `m_identifier` .01C; `m_57D96E27` = 1 means NOT | `Scripted Event: <name>(id)`, `NOT (...)` | a state set by game scripts (H; NOT flag: the 79 NOT lines are exactly the 79 with the flag) |
| 7 | `STUCriteria_OnMap` | `m_map` .002 | `On Map: <map>` | map (H) |
| 15 | `STU_C37857A5` | `m_celebrationType` .01C | `Active Celebration: <event>` | seasonal event running (H) |
| 19 / 20 | `STU_0F78DDB0` / `STU_20ABB515` | .10D / .0DA | `On Mission` / `On Mission Objective` | PvE missions (H) |

Each criterion sits in a wrapper `STU_32A19631`. Scripted event names (Headshot 9227, UltShutdown A617, HealedAlly
AABA, EnemyInAir 9268, LongDistance A809, Melee A752, FastMoving A6FC, InAir A776, RespawnAfterKillstreak B404, Winter
2168...) are community labels from DataTool's `Static\GUIDNames.csv`, not Blizzard's (M). `NOT (Scripted Event:
Unknown191E)` sits on every effort sound (death, pain, breathing): treat it as always true (it is the normal case).

## The moments' stimulus settings

From the .078 assets (priority, radius and volume are named fields: H; cooldown = the unnamed pair: M).

| Stimulus | Moment | Priority | Cooldown s | Radius m | Own criteria | Fired by |
|---|---|---|---|---|---|---|
| FC0 | Swift Step | 60 | 10 | 25 | - | graph 30DB, after a wait (var 1723); node value 0.05 |
| FC5 | Protection Suzu | 60 | 10 | 35 | - | graph 30D8, 0.5 s after game message 1955 |
| FC4 | healing an ally | 60 | 14-17 | 35 | - | heal subgraph 2128 on the healed ally, inside a 0.2 s wait state; node value 0.15 |
| FC3 / FC2 | ult cast (friendly / enemy) | 95 | 1 | 60 | - | ult graph 30DA |
| 0583 | On Fire | 60 | 2-5 | 35 | scripted event 1AE6 (unnamed) | **no statescript graph** (all 5,687 parsed): the engine (H) |
| 00C9 | Multikill | 70 | 15-20 | 35 | - | shared kill graph 33B4 |
| 0039 | Kill streak | 75 | 10 | 35 | scripted event 0828 | 33B4 |
| 0008 | Elimination | 70 | 15 | 30 | type 2 (`m_level` 0; unresolved) | 33B4 |
| 11D7 | Elimination (special kills) | 70 | 10 | 35 | - | 33B4 |
| 0032 | Melee kill | 65 | 10-14 | 30 | scripted event 069E | 33B4 |
| 0036 | Revenge | 65 | 5 | 35 | - | 33B4 |
| 0584 | Team kill (whole enemy team dead) | 60 | 10 | 50 | - | 33B4 |
| 002C | Respawn | 40 | 15 | 35 | 05D8 and NOT 202F | 01CA |
| 002B | Hero select | 100 | 1 | 35 | 05D4 and NOT 202F | - |
| 014B | Hero change ("Hey, it's Kiriko") | 60 | 1 | 35 | 172F | - |
| 02E7 / 0574 / 05A7 | Ult ready / charging / almost ready | 70 | 0 | 35 | yes | communication wheel (interrupt 1, like Thanks) (M) |
| 0001 | Death | 90 | 2-5 | 60 | 05FA | - |
| 0003 / 0004 | Pain (light / heavy) | 40 | 1-2.5 / 0.5-1 | 35 | yes | - |
| 0023 | Melee shout | 35 | 0.5 | 35 | 0601 | - |

Graph 33B4 says every hero's kill lines (stimulus 0008 is only in 33B4). Before the Multikill and Kill streak nodes
it waits about 0.3 s and tests a flag, and the elimination path has expressions shaped like `random(0,1) < 0.66` and
`< 0.5`. That reading of the VM bytecode is a guess (L). The multikill count and window are engine variables there
(unnamed .01C identifiers), so they can't be read.

## Lines by moment

"Pool chance" is what the weighted-pool model gives when the criterion holds (M).

### Swift Step (FC0), target = the ally she teleports to

Generic (7 lines, total weight 7.0): 5EAB9 "I'm with you", 5EABB "I'm here", 5EB6E "(Japanese) I'm here", 5EB6F
"(Japanese) I'm with you", 5EEFD "(chuckle) Hey", 5EEFE "What's up", 627BB "(Japanese) Are you well?". All weight 1.

| Id | Line | Needs | Weight | Pool chance |
|---|---|---|---|---|
| 5EFA4 | Rolling with you | target is Wrecking Ball (1CA) | 0.1 | 1.4% |
| 5EFA5 | How's the hunt? | target is Widowmaker (00A) | 0.1 | 1.4% |
| 5EFAC | Cute shoulderpads | target is Sigma (23B) | 0.1 | 1.4% |
| 5EFAE | Digging the tunes | target is Lúcio (079) | 0.1 | 1.4% |
| 5EFBE | Hey, big guy | target is Roadhog (040) or Reinhardt (007) | 0.1 | 1.4% |
| 5EFC2 | Hey, queen | target is Junker Queen (236) | 0.1 | 1.4% |
| 5EFC3 | Howdy | target is Ashe (200) or Cassidy (042) | 0.1 | 1.4% |
| 5EFC7 | I'll be your angel | target is Mercy (004) | 0.1 | 1.4% |
| 69284 | Boo! (laughs) | target is Illari (31C) (not seasonal, as was guessed before) | 0.1 | 1.4% |
| 6B568 | Your pocket ninja's here | target is Genji (029) | 0.3 | 3.6% |
| 6C279 | Got your back, Genji | target is Genji | 1.0 | 12.0% |
| 6C27A | I'm here, Hanzo | target is Hanzo (005) | 1.0 | 12.5% |
| 6B55B | (Japanese) I'm here, Mizuki | target is Mizuki (4E3) | 0.3 | 3.6% |
| 6C27B | With you, Mizuki | target is Mizuki | 1.0 | 12.0% |
| 5EFBA | Ho ho hey | Winter Wonderland running (celebration 2168) | 0.1 | 1.4% |

So Swift Stepping to a Wrecking Ball says "Rolling with you" about once in 70 lines (M). Skin lines in the FC0 folder
("Manifesting", "The goddess is here"... ids 64CE9-64CEE, 65B8F-65B99) are only for those skins.

### Protection Suzu (FC5)

All 12 lines are generic (total 9.3): 5EB6A "(Japanese) Cleansed" 1, 5EB7B "Cleansed" **0.1**, 5EB97 "Feel better?" 1,
5EE71 "Protecting!" 1, 5EE92 "Purified" 1, 5EF87 "Washed away" 1, 62CDF "Be purified!" **0.1**, 62CE2 "Hear my bell"
1, 62CEF "You're all good" 1, 62CF0 "You have no excuses now" **0.1**, 62CF1 "Now go get 'em" 1, 62CF2 "Kanezaka-jinja
protects you" 1. Said 0.5 s after the Suzu's game message, at most every 10 s.

### Healing (FC4), target = the healed ally

Generic (20 lines, total 15.65): weight 1 for 5EACA "Let me balance you out", 5EAD3 "Like a gentle rain", 5EAEC
"Restoring balance", 5EAFA "Soothing your wounds", 5EB6D "(Japanese) Healing", 5EB74 "(Japanese) Restoring you",
5EB92 "Carried on the wind", 5EE96 "(Japanese) Like komorebi", 5EE98 "My gift to you", 5EE99 "Draw strength from
nature", 627B7 "(Japanese) You'll be fine", 62CDC "Under my protection", 62CDD "Feels good, huh?", 62CDE "(Japanese)
I'll protect you!", 62CEE "(Japanese) Chasing away the evil"; 5EB73 "(Japanese) Protection!" 0.25; 5EA5D "Be
healed", 5EAC7 "I've got you", 5EE95 "Feels like sunlight, huh?", 5EE97 "The breeze brings relief" 0.1 each.

| Id | Line | Needs | Weight | Pool chance |
|---|---|---|---|---|
| 5EB65 | You take a lot of damage, huh? | scripted event HealedAlly (AABA, meaning unresolved) | 0.15 | 0.9% |
| 5EF7F | Like mending a shadow | target is Reaper (002) | 0.5 | 3.1% |
| 5EF84 | Be careful, Shimada | target is Genji or Hanzo | 0.5 | 3.1% |
| 5EFEC | Your wounds run deep | target is Soldier: 76, Ana or Ramattra, **and** HealedAlly ("2 of 4") | 0.1 | 0.6% |
| 5EFEA | A brush of summer | celebration 212F (unnamed; likely Summer Games, L) | 0.5 | 3.1% |
| 6B554 | Dispelling your curse | target is Mizuki | 0.4 | 2.2% |
| 6B555 | (Japanese) Hang in there, Mizuki | target is Mizuki | 1.0 | 5.5% |
| 6B557 | (Japanese) Pull yourself together, Mizuki | target is Mizuki | 1.0 | 5.5% |

At most one heal line every 14-17 s per Kiriko (M). The mod's `sayNowAndThen(... 0.15f, 400)` treats 0.15 as a
chance, but the data's 0.15 is the node value that isn't a chance (see above).

### On Fire (0583)

5EFCF "I'm on fire" 1, 5EF9C "The battle ignites me!" 0.3, 5EFD0 "Aw, I feel all warm inside" 0.3 (total 1.6). The
fortune-teller skin adds 64CDF "My future's bright!". Fired by the engine, not by a graph. Cooldown 2-5 s, priority 60.

### Multikill (00C9) and kill streak (0039)

Multikill: 5EB82 "Fox in a hen house" 1, 5EB8F "Now I'm having fun" 1. No criteria.

Kill streak (H for the lines; what counts as a streak is unknown): 5EB71 "(Japanese) Let's go!", 5EB85 "How many was
that?", 5EB87 "I thought this would be harder", 62D02 "Mom would be proud", 62D03 "Should've been on my team" (all 1).
After a death that ended a streak, the respawn pool adds 5EFFC "I don't blame them. I was going wild" and 627B8
"(laughs) I guess someone had to stop me" (RespawnAfterKillstreak). The wiki's general "kill streak" means several
eliminations without dying (L, no count).

### Elimination (0008), target = the victim

Generic (19 lines, total 15.4): weight 1 for 5EB6B "(Japanese) Cool", 5EB6C "(Japanese) Goodbye", 5EB72 "(Japanese)
My apologies", 5EB8D "Nice", 5EB94 "Take some time off", 5EB9B "Underestimated me, ne?", 5EE93 "Back off", 61A9E
"(Japanese) Bye!", 61A9F "Find peace", 627AF "(Japanese) Incredible!", 627B0 "(Japanese) As expected!", 627B2
"(Japanese) Well done!", 627B3 "(Japanese) Ouch!", 627C8 "I'll pretend I didn't see that", 62CF7 "How embarrassing";
0.1 for 5EB7D "Heh, cool", 5EB80 "Sit down, please", 5EB91 "Ouch", 62CF6 "You were being rude".

Conditional lines a Minecraft port could trigger (the rest name a hero, a map or an ult shutdown):

| Id | Line | Needs | Weight |
|---|---|---|---|
| 5EB69 | (chuckle) | Headshot (9227) | 0.2 |
| 5EB93 | Shouldn't have put your head there | Headshot | 0.2 |
| 5EB98 | That's the spot | Headshot | 0.2 |
| 5EB9D | And what did we learn? | Headshot | 0.1 |
| 5EB7E | Did you think you were safe up there? | EnemyInAir (9268) | 0.8 |
| 5EB9C | Welcome back to earth | EnemyInAir | 1.0 |
| 64270 / 64271 | Yeah, no / It's like you wanted to die | UltShutdown (A617) | 1.0 / 0.1 |
| 6541C | This time, you might want to stay down | Shutdown_Resurrected (AF43) | 1.0 |

Victim-hero lines (for a future PvP with heroes): Genji or Hanzo 5EB7C / 62784 "Come on, Shimada"; Junkrat 5EF6B;
Wrecking Ball 5EF7E "Did you build that ball yourself?" 0.1; Zarya or Pharah 5EF82; D.Va 5EFF3 0.1; Vendetta or
Ramattra 5EF6A; Ana, Soldier: 76, Pharah or Widowmaker 62CF8 0.5; Widowmaker or Ana with a headshot 5EB88 / 5EE94
0.8; Genji with an ult shutdown 62CE0 0.3 / 6541B 0.1; Anran 6B53A 0.2; Wuyang 6B53C; Domina 6B53E; Genji 6B540; Shion
or Domina 6C26B; Shion 6C273, 6C286, 6C287 (with a headshot), 62D22 (with Shion on Kanezaka); Mizuki 6C285.

**Special eliminations (11D7)**, all conditional, weight 1: 6278E "That had some style" (2 of Headshot, InAir A776,
LongDistance A809); 642A0 "How far was that?" (LongDistance and Headshot); 64272 "That's youthful reflexes for you" (2
of Headshot, Melee A752, FastMoving A6FC); 6426F Reinhardt charge stopped; 64273 Sombra found in stealth; 64274
Nano-boosted ult stopped plus a headshot; 6429C / 6429D ult shutdown plus Sojourn / Junkrat. The distance and speed
thresholds behind LongDistance and FastMoving are not in the data that was read.

### Melee kill (0032)

Generic (5): 5EB9E "Your hand-to-hand needs work" 1, 61AA3 "That lesson was free" 1, 62CFC "It's called personal
space" 1, 627BF "Look, don't touch" 0.1, 62CFB "(Japanese) Wow, smart of you to get so close" **0.001** (almost never).
Conditional: 6429F "Nope!" (FastMoving and Melee); Genji or Hanzo 5EFE7 / 5EFF2; Shion 6C26C, 6C26F, 6C271, 6C272.

### Ultimate (FC3 friendly, FC2 enemy)

FC3: 5EB8B "Let the Kitsune guide you!" 1; 627CE "Just aim for the head!" needs (celebration 258A or scripted event
E8CA) and scripted event BB2C (unresolved; L: an event mode). FC2: 62D15 "(Japanese) O fox spirit, unleash your
claws!" 1; 642FE "(fox noises)" with the same criteria. Today's `kiriko.ult.friendly` / `.enemy` are right.

### Respawn (002C), hero select (002B), hero change (014B)

Respawn, generic (14): weight 1 for 5EB7A, 5EB86, 5EB90, 5EB9A, 5EF7B, 5EFF9 "Time to get clever", 6B560 "Well that
was uncalled for", 6C277 "My path doesn't end here", 6C278 "That won't shake my faith", 6C282 "The Fox Spirit is still
with me"; 0.2 for 5EB89 "I licked my wounds. Let's go" and 5EB8C "The spring flows anew"; 0.1 for 5EF6C "That was
interesting" and 5EFFA "Eh. We've got this". Conditional: 5EF7C on Kanezaka, Hanamura or Hanaoka; 5EFFC / 627B8 after a
kill streak; 6C26E / 6C275 Mizuki (as the killer, M).

Hero select: only 5F846 "I'll find my own path". Hero change mid-match: 6428C "Hey, it's Kiriko".

### Other moments a Minecraft port could trigger

| Stimulus | Moment | Lines | Cooldown s | Minecraft trigger (ours) |
|---|---|---|---|---|
| 0036 | Revenge | 5 generic (5EB83, 5EB99, 627B4, 62CFD, 62CFE) | 5 | kill the mob that last killed her |
| 0584 | Team kill | 5 (5EB96 0.1) | 10 | last mob of a raid wave |
| 002A | Health pack | 6 generic (5EF77, 5EF79, 5EFFB, 62D07-62D09) | 10 | instant health / golden apple |
| 0029 | Healed by an ally | 7 generic ("Thanks for the heal"...) | 30 | another hero heals her |
| 001F / 0021 / 0020 | Low health: breath start / loop / end | 1 / 5 / 3 efforts | 5 / 3-4 / 2-5 | low health |
| 0054 / 0026 | Jump grunt / landing exhale | 3 / 4 | 2 / 1 | jump / land |
| 05B2 | Falling to death | 3 screams | 2-5 | falling into the void |
| 0283-0285 | Damage over time (3 kinds) | 5 each | 1-2 | burning, poison, freezing (L which is which) |
| 05ED | Unwell | 4 ("Ugh, I feel sick"...) | 30 | poison, nausea |
| 09C6 | Under attack | 3 ("They're targeting me!") | 60-90 | several mobs on her |
| 01E7 | Enemies gather | 2 (62799 0.1, 6279A "That's a handful") | 30-60 | many hostiles in view |
| 0A26 | Enemy direction | 12 (left/right/above/below/behind/ahead, by scripted events 2733-2738) | 20-30 | a mob attacks from that side |

## On Fire in Overwatch 2

- **Removed, then back (H).** Blizzard, June 13, 2023 (Season 5) "On Fire Returns": a new **Blazing** state for a
  team's most exceptional playmakers, harder to keep than On Fire; fire on the portrait, scoreboard and kill feed;
  **killing an enemy who is On Fire grants Fire**. Sources: eliminations and damage, mitigated damage, saves, healing,
  assists, capturing/contesting objectives, boosted damage, crowd control. The wiki adds that it was absent from OW2's
  launch because the UI was rebuilt, while the voice lines stayed in the files (M).
- **Tuned (H).** Blizzard, July 11, 2023: On Fire's **cooldown reduced to 3 s** (fewer delayed On Fires after a
  positive play); the score **decays slightly more quickly when not On Fire**; staying On Fire a long time is harder;
  On Fire and Blazing are harder early in a round; a bug that gave On Fire at the start of a round was fixed.
- **Numbers (L-M, OW1 era).** The wiki's On Fire page, citing Mein-MMO (2016): 250 points to catch fire, meter capped
  at 350 on screen, decay 5 points/s; damage gives points in proportion to the victim's max health (60% of it = 60
  points); healing 1 point per 5 HP; mitigation 1 point per 25 blocked; payload escort 10 points/s. Blizzard never
  published OW2's thresholds, decay or the Blazing threshold.
- **The voice (M / L).** The data plays On Fire lines from the engine with a 2-5 s cooldown. The wiki says a bug from
  Season 5 let On Fire lines override elimination lines, and that the April 14, 2026 patch made them play only on
  reaching Blazing (source: a Sept 2025 forum thread). **Blizzard's April 2026 notes say nothing about it**: unverified
  (L).
- **For the mod (ours):** a fire score per hero from damage (100 per victim's full health), healing (1 per 5 OW HP = 2.5
  per Minecraft health point) and blocked damage, decaying 5/s, On Fire at 250, no new On Fire within 3 s of losing it;
  say the On Fire stimulus on entering On Fire (or only at a Blazing threshold of ours), at most every 2-5 s. Kill lines
  outrank it (70 > 60).

## Multikill

- **Overwatch:** the wiki defines it as "multiple eliminations in a very short amount of time" and gives no number (L).
  Athena's announcer lines go "Double kill", "Triple kill", "Multikill", which suggests two kills already count (L).
  Blizzard: Dec 19,
  2023 "Fixed a bug that caused the multi-kill voice over to fail to play"; Jan 9, 2024 "Fixed missing Multikill
  voice-overs from activating when criteria is met" (H, neither defines it).
- **Data (M):** multikill is decided by the engine and passed to graph 33B4 through unnamed variables. The voice
  waits ~0.3 s, then says Multikill (priority 70, cooldown 15-20 s) over a plain elimination line.
- **For the mod (ours):** count only enemy kills (hostile mobs, enemy players; not animals or villagers); a multikill is
  the 2nd kill within a short window (the window is ours: 4 s today); say it **once per burst** (not on every later kill
  of the chain), respect 15-20 s between multikill lines, and let it replace that kill's elimination line.

## What today's soundmap gets wrong (tools/soundmaps/kiriko.txt)

| Event | Wrong lines | Why |
|---|---|---|
| `kiriko.voice.swift_step` | 5EFA4 Rolling with you, 5EFA5 How's the hunt?, 5EFC3 Howdy, 5EFC7 I'll be your angel, 6B568 Your pocket ninja's here | hero-gated (Wrecking Ball, Widowmaker, Ashe/Cassidy, Mercy, Genji) |
| `kiriko.voice.heal` | 5EB65, 5EF7F, 5EFEC, 5EFEA, 6B554 | HealedAlly event, Reaper, S76/Ana/Ramattra, a celebration, Mizuki |
| `kiriko.voice.melee_kill` | 64CCB Palm read this, 64CCC My divine gift, 65B8A Knocked your lights out | skin voice sets 228 / 236 |
| `kiriko.voice.melee_kill` | 62CFB (Japanese) Wow, smart of you... | weight 0.001: Overwatch almost never says it; here 1 in 8 |
| `kiriko.voice.select` | 65B81 I'll light my own path | the sun-goddess skin's set |
| `kiriko.voice.multikill` | (lines right) | the trigger is too loose (see Multikill) |
| `kiriko.voice.ult_ready` | (lines right) | in Overwatch these are communication lines (said on request), not an automatic announcement (M data, L behaviour) |
| every voice event | - | weights ignored (sounds.json has none), `MAX_VARIANTS = 8` cuts the 20-line heal and 19-line elimination pools |

## Proposed soundmap shape and choice

```
# Swift Step (FC0). Pool = the generic lines + the lines of the target's hero. "| w=" = Overwatch's m_weight.
kiriko.voice.swift_step = HeroVoice/Kiriko/Unknown/FC0.078/00000005EABB.0B2-I'm here.ogg | w=1
...
kiriko.voice.swift_step.wrecking_ball = HeroVoice/Kiriko/Unknown/FC0.078/00000005EFA4.0B2-Rolling with you.ogg | w=0.1
kiriko.voice.swift_step.genji = HeroVoice/Kiriko/Unknown/FC0.078/00000006B568.0B2-Your pocket ninja's here.ogg | w=0.3
kiriko.voice.swift_step.genji = HeroVoice/Kiriko/Unknown/FC0.078/00000006C279.0B2-Got your back, Genji.ogg | w=1
kiriko.voice.swift_step.ashe = .../00000005EFC3.0B2-Howdy.ogg | w=0.1        # "1 of 2": listed under each hero
kiriko.voice.swift_step.cassidy = .../00000005EFC3.0B2-Howdy.ogg | w=0.1
kiriko.voice.elimination.headshot = .../00000005EB98.0B2-That's the spot.ogg | w=0.2   # scripted events by name
```

- Suffix = the mod's hero id for "Hero Interaction", or an event name (`headshot`, `enemy_in_air`, `long_distance`)
  for scripted events. Sound event ids allow `a-z0-9._-`, so a dot works and the build's `.` -> `/` gives
  `kiriko/voice/swift_step/wrecking_ball/0.ogg`. Leave out composite criteria at first (5EFEC), or key them on the
  event only.
- **Choice (server, in `Voice`):** with U = total weight of the generic lines and C = total weight of the matching
  conditional events (target's hero, and any scripted events that hold), say the conditional event with chance
  C / (U + C), else the generic one. Within an event, the client draws by sounds.json `weight` (Overwatch weight x 10,
  rounded, at least 1; drop the 0.001 line). This gives the same odds as Overwatch's pool (M).
- The server can't read the client's pack, so U and C per moment must live in the mod (a small committed table, numbers
  only). They come from the game data, not a public source. CLAUDE.md asks for public sources first, so the owner
  should OK data-mined voice weights and cooldowns (they are presentation, not balance).
- Per moment, keep Overwatch's cooldown (random in the min-max) and priority instead of today's chances (Swift Step
  0.6, Suzu 0.5, elimination 0.35 and the 3 s gap are ours). When two moments fire within ~0.3 s, say only the higher
  priority one.
- The build needs: `| w=` parsing in `read_soundmaps`, `{"name", "weight"}` entries in sounds.json, and a higher
  `MAX_VARIANTS` for voice events (they are copied, not mixed).
- Mobs never match a Hero Interaction. Until other heroes are playable, only the generic pools play (Kiriko has no
  Kiriko-on-Kiriko lines).

## How the existing voice section was made

- Lines 533-585 (FC0, FC5, FC4) come from the sound research (`kiriko-sounds.md`, "Voice"). It read
  `research\kiriko-anim\map.json` `voice_by_stimulus`, which has no criteria, and left lines out **by their text
  only**. Its own "Uncertain" section says so. That is how the five unnamed hero lines got in.
- Lines 587+ ("Picked by a script", commit 4daf62d, 2026-10-26): the script was not committed and is not in the
  archive's research folders. Its comment counts match a pass over **DataTool's HeroVoice folders** that took any line
  without a `-criteria.txt` sidecar as unconditional (H by arithmetic). Elimination "36 of 77": the folder holds
  stimulus 0008 (51, of which 19 have no criteria) + 11D7 (8, all with criteria) + 18 skin lines (17 without
  criteria): 19 + 17 = 36. Melee kill "8 of 15" = 5 base + 3 skin. Respawn "23 of 28" = 14 + 9 skin. Select "2 of 2"
  = 1 + 1 skin. The folders mix the base set with the two skin sets, and the script ignored `-weight.txt`.
- The sidecars come from the existing extraction, so no DataTool run was needed
  (`extract-kiriko-voice.log`): `DataTool.exe "E:\Overwatch Archive\build-154088" extract-hero-voice
  "E:\Overwatch Archive\extracted\154088" Kiriko --online=false --language=enUS --subtitles-with-sounds`. Each line can
  have `<id>-criteria.txt` and `<id>-weight.txt` (UTF-16; weight only when not 1).

## How this was done (repeat for another hero)

Scripts and outputs: `E:\Overwatch Archive\research\kiriko-voice\` (scripts in `scripts\`; system Python `py -I` for
the extracted files, the kiriko-anim venv Python `-I` with `owinit.py` for the archive; owinit refuses to run while
Overwatch or Battle.net is running).

1. `sidecars.py` -> `sidecars.json`: every HeroVoice line with its DataTool criteria and weight.
2. `dumplines.py` -> `all_lines.txt`: every `STUVoiceLine` of voice set 215.05F (from
   `research\kiriko-anim\dump_lo\000000000215.05F.json`) with stimulus, weight, decoded criteria, plus the sidecar text.
   `moments.py <stimuli>` prints one moment's lines; `stimuli.txt` is the per-stimulus summary.
3. `stim078.py <stimuli>` -> `stim078_all.json`, `stim078_more.json`: the stimulus assets (priority, cooldown pair,
   radius, criteria), read from the archive.
4. `scan_stim2.py` -> `graph_stimuli.json`: every statescript graph (.01B, 5,687) and the stimuli it says (this is how
   0583 was found in none). `voicenodes.py`, `trace2.py`, `node.py`: voice node parameters and what leads to them
   (from `research\kiriko-anim\dump_ss\`).
5. `skins3.py`: the voice sets holding the skin lines (228, 236) and the skin theme using 228.

## Not resolved

- The engine's pick rule (weighted pool, inferred), the cooldown pair's meaning (inferred) and `m_B0263D2D`'s
  (delay, inferred).
- Multikill window and count, kill-streak count, LongDistance / FastMoving thresholds: engine variables or unread.
- Unnamed scripted events: 191E (on every effort sound), 1AE6 (On Fire and the perk-picked lines 1893/1894), 0828
  (kill streak), 069E (melee kill), HealedAlly's exact meaning, celebrations 212F / 258A.
- Which skins use voice sets 228 and 236 by name; 236's skin theme was not found by the scan.
- Whether On Fire lines now play only at Blazing (wiki claim, not in Blizzard's notes).
