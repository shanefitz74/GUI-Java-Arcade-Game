# APE ASCENT: Ironworks

A JavaFX single-screen climbing arcade game rebuilt from the basic movement
concepts of the original `GUI-Java-Arcade-Game` educational project.

This branch is a clean-room gameplay rebuild. It does **not** use Nintendo
sprites, music, ROM data, protected character names, or source code. All visible
game art is drawn procedurally with JavaFX Canvas.

## Four-board arcade campaign

1. **Barrel Foundry** — climb sloped girders while rolling barrels fall between
   floors or randomly descend complete ladders.
2. **Conveyor Works** — alternating conveyor belts move the player and alter the
   speed of roaming fire hazards.
3. **Elevator Shaft** — cross open gaps using two independently moving lift
   platforms while avoiding hazards on the side decks.
4. **Rivet Reactor** — remove every gold rivet across four floors while fire
   hazards patrol the board; clearing the final rivet wins the game.

## Gameplay features

- Fixed-step 120 Hz simulation for consistent movement
- Run, jump, gravity, platform, ladder, and moving-elevator physics
- Complete and broken ladders
- Rolling, falling, and ladder-descending barrels
- Conveyor belts with directional movement
- Roaming fire hazards
- Moving elevator platforms
- Removable rivet objective
- Hammer pickup with timed hazard destruction
- Jump-over, hammer, rivet, stage-bonus, and extra-life scoring
- Three lives and an extra life at 20,000 points
- Persistent local high score
- Arcade bonus countdown
- Title, pause, death, board-clear, victory, and game-over states
- Original procedural characters, effects, and industrial artwork
- No external image or audio files required

## Project structure

- `ArcadeAscent.java` — JavaFX application and fixed-step loop
- `GameEngine.java` — board setup, physics, hazards, scoring, and game states
- `ArcadeRenderer.java` — Canvas rendering and interface screens
- `GameModel.java` — shared rules, entities, and board data types

## Requirements

- JDK 17 or newer
- Maven 3.9 or the included Maven wrapper

## Run

### Windows PowerShell

```powershell
.\mvnw.cmd clean javafx:run
```

### macOS or Linux

```bash
./mvnw clean javafx:run
```

A system Maven installation also works:

```bash
mvn clean javafx:run
```

## Controls

| Action | Keys |
|---|---|
| Move | Left/Right arrows or A/D |
| Climb | Up/Down arrows or W/S |
| Jump | Space, Z, or J |
| Start / continue | Enter |
| Pause | P or Escape |

## Build check

```bash
mvn --batch-mode --no-transfer-progress clean package
```

## Originality notice

APE ASCENT: Ironworks, Brassback, Nova, the mechanic character, all current
rendered artwork, board layouts, and new gameplay code are original to this
rebuild. Donkey Kong and Nintendo are trademarks of their respective owners and
are not affiliated with this project.
