# Overcraft

Overwatch heroes in Minecraft 26.3 (Fabric).

When you join a world, the **Select your hero** screen opens. Picking a hero sets your kit: the hero's health
(scaled so 250 Overwatch health = 10 hearts) and the hero's weapon in hotbar slot 1. **With slot 1 selected, the
hero's controls are live** and you move like the hero; any other slot plays plain Minecraft.

## Heroes
| Hero | Status |
|---|---|
| Kiriko | In progress |
| Lúcio | Planned |
| Sombra | Planned |
| Illari | Planned |

### Kiriko (slot 1)
| Input | Ability |
|---|---|
| Left click | Kunai (60 damage, 2x headshots) |
| Right click (hold) | Healing Ofuda (homes on the ally you aim at) |
| Shift | Swift Step (teleport to an ally, through walls) |
| E | Protection Suzu (heal, cleanse, brief invulnerability) |
| Q | Kitsune Rush (ultimate) |
| R | Reload |
| Hold jump at a wall | Wall climb |
| H | Hero select (any slot) |

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
