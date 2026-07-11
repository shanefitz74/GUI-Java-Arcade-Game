# APE ASCENT: Ironworks

A JavaFX single-screen climbing arcade game built from the mechanics of the
original `GUI-Java-Arcade-Game` educational project.

This branch is a clean-room gameplay rebuild. It does **not** use Nintendo
sprites, music, ROM data, names, or source code. All visible game art is drawn
procedurally with JavaFX Canvas.

## Current playable build

- Fixed-step game loop for consistent movement
- Sloped girders and climbable ladders
- Broken decorative ladders
- Rolling barrels that fall between floors or randomly descend ladders
- Run, jump, climb, falling, and collision physics
- Hammer pickup and timed barrel smashing
- Jump-over and hammer scoring
- Three lives, extra life at 20,000 points, high-score persistence
- Arcade-style bonus countdown
- Four increasingly difficult stage loops
- Title, pause, death, stage-clear, victory, and game-over states
- Original vector/pixel-style characters and industrial artwork
- No external image or audio files required

## Requirements

- JDK 17 or newer
- Maven 3.9 or the included Maven wrapper

## Run

### Windows

```powershell
.\mvnw.cmd clean javafx:run
```

### macOS or Linux

```bash
./mvnw clean javafx:run
```

You can also use a system Maven installation:

```bash
mvn clean javafx:run
```

## Controls

| Action | Keys |
|---|---|
| Move | Left/Right arrows or A/D |
| Climb | Up/Down arrows or W/S |
| Jump | Space, Z, or J |
| Start | Enter |
| Pause | P or Escape |

## Build check

```bash
mvn --batch-mode clean package
```

## Project status

The current rebuild delivers the complete barrel-board arcade loop. Planned
follow-up boards are conveyors, moving lifts, and a removable-rivet finale.

## Originality notice

APE ASCENT: Ironworks, Brassback, Nova, the mechanic character, all current
rendered artwork, level data, and new gameplay code are original to this
rebuild. Donkey Kong and Nintendo are trademarks of their respective owners
and are not affiliated with this project.
