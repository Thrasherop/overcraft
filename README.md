# Overcraft

Overwatch heroes in Minecraft 26.3 (Fabric).

When you join a world, the **Select your hero** screen opens. Picking a hero sets your kit: the hero's health
(scaled so 250 Overwatch health = 10 hearts) and the hero's weapon in hotbar slot 1. **Hero mode** (G, or hotbar
slot 1) makes the hero's controls live and you move like the hero (5.5 m/s, no sprint, Overwatch's jump, no fall
damage); the Minecraft hotbar gives way to the hero's HUD. G again (or any other slot) plays plain Minecraft.
To change heroes, press H within 5 blocks of your bed.

Ultimate charge comes as in Overwatch (5 points a second, 1 per damage dealt or healing done) and also from
Minecraft advancements: 10% of the ult for a task, 25% for a goal, 50% for a challenge.

## Heroes
| Hero | Status |
|---|---|
| Kiriko | In progress |
| Lúcio | Planned |
| Sombra | Planned |
| Illari | Planned |

### Kiriko (hero mode)
| Input | Ability |
|---|---|
| Left click | Kunai (60 damage, 2x headshots) |
| Right click (hold) | Healing Ofuda (homes on the ally you aim at) |
| Shift | Swift Step (teleport to an ally, through walls) |
| E | Protection Suzu (heal, cleanse, brief invulnerability) |
| Q | Kitsune Rush (ultimate) |
| R | Reload |
| Ctrl | Crouch |
| Hold jump + forward at a wall | Wall climb |
| G | Hero mode on/off |
| H (near your bed) | Change hero |

Numbers follow the live game (sources: Blizzard's patch notes and the Overwatch wiki).

## Requirements
- Minecraft 26.3, Fabric Loader 0.20.1+, Fabric API.
- Needed on both the client and the server.

## Building
```
gradlew build
```
The jar is in `build/libs/`. Gradle runs itself on Java 25 and downloads it if needed.

Overcraft is a fan project, not affiliated with or endorsed by Blizzard Entertainment or Mojang. Overwatch is a
trademark of Blizzard Entertainment. The repository contains no Overwatch game files.
