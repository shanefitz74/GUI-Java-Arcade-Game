package arcade;

import static arcade.Rules.CLIMB_SPEED;
import static arcade.Rules.GRAVITY;
import static arcade.Rules.HEIGHT;
import static arcade.Rules.JUMP_SPEED;
import static arcade.Rules.PLAYER_SPEED;
import static arcade.Rules.WIDTH;
import static arcade.Rules.clamp;
import static arcade.Rules.distance;
import static arcade.Rules.rectIntersects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.prefs.Preferences;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

final class GameEngine {
    final Random random = new Random();
    final Set<KeyCode> keysDown;
    final Set<KeyCode> keysPressed;
    final List<Platform> platforms = new ArrayList<>();
    final List<Ladder> ladders = new ArrayList<>();
    final List<Barrel> barrels = new ArrayList<>();
    final List<Fireball> fireballs = new ArrayList<>();
    final List<Elevator> elevators = new ArrayList<>();
    final List<Rivet> rivets = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final Preferences preferences = Preferences.userNodeForPackage(GameEngine.class);

    GameState state = GameState.TITLE;
    BoardType boardType = BoardType.BARREL_FOUNDRY;
    Player player;
    Pickup hammerPickup;
    Goal goal;
    Boss boss;
    int score;
    int highScore;
    int lives;
    int stage;
    int bonus;
    boolean extraLifeAwarded;
    double bonusTick;
    double hazardTimer;
    double stateTimer;
    double shake;

    GameEngine(Set<KeyCode> keysDown, Set<KeyCode> keysPressed) {
        this.keysDown = keysDown;
        this.keysPressed = keysPressed;
        highScore = preferences.getInt("high-score", 0);
    }

    void handleStart() {
        if (state == GameState.TITLE || state == GameState.GAME_OVER
                || state == GameState.VICTORY) {
            newGame();
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    void newGame() {
        score = 0;
        lives = 3;
        stage = 1;
        extraLifeAwarded = false;
        initializeStage();
    }

    void initializeStage() {
        platforms.clear();
        ladders.clear();
        barrels.clear();
        fireballs.clear();
        elevators.clear();
        rivets.clear();
        particles.clear();

        boardType = BoardType.values()[Math.max(0, Math.min(stage - 1, 3))];
        switch (boardType) {
            case BARREL_FOUNDRY -> setupBarrelFoundry();
            case CONVEYOR_WORKS -> setupConveyorWorks();
            case ELEVATOR_SHAFT -> setupElevatorShaft();
            case RIVET_REACTOR -> setupRivetReactor();
        }

        bonus = 5_000;
        bonusTick = 0;
        hazardTimer = boardType == BoardType.BARREL_FOUNDRY ? 1.2 : 2.0;
        stateTimer = 0;
        shake = 0;
        state = GameState.PLAYING;
    }

    private void setupBarrelFoundry() {
        platforms.add(new Platform(18, 654, 622, 632, 0));
        platforms.add(new Platform(18, 538, 622, 564, 0));
        platforms.add(new Platform(18, 466, 622, 438, 0));
        platforms.add(new Platform(18, 348, 622, 376, 0));
        platforms.add(new Platform(18, 278, 622, 248, 0));
        platforms.add(new Platform(122, 142, 520, 142, 0));
        addLadder(548, 1, 0, false);
        addLadder(112, 2, 1, false);
        addLadder(498, 3, 2, false);
        addLadder(174, 4, 3, false);
        addLadder(438, 5, 4, false);
        addLadder(324, 1, 0, true);
        addLadder(348, 3, 2, true);
        addLadder(298, 5, 4, true);
        Platform bottom = platforms.get(0);
        player = new Player(58, bottom.yAt(70) - 34);
        boss = new Boss(145, 75);
        goal = new Goal(475, 94, true);
        Platform hammerPlatform = platforms.get(2);
        hammerPickup = new Pickup(410, hammerPlatform.yAt(410) - 24, true);
    }

    private void setupConveyorWorks() {
        platforms.add(new Platform(20, 650, 620, 650, 1));
        platforms.add(new Platform(42, 548, 598, 548, -1));
        platforms.add(new Platform(20, 446, 620, 446, 1));
        platforms.add(new Platform(42, 344, 598, 344, -1));
        platforms.add(new Platform(20, 242, 620, 242, 1));
        platforms.add(new Platform(160, 145, 500, 145, 0));
        addLadder(530, 1, 0, false);
        addLadder(120, 2, 1, false);
        addLadder(520, 3, 2, false);
        addLadder(145, 4, 3, false);
        addLadder(430, 5, 4, false);
        addLadder(330, 2, 1, true);
        addLadder(340, 4, 3, true);
        player = new Player(55, 650 - 34);
        boss = new Boss(170, 80);
        goal = new Goal(460, 98, true);
        hammerPickup = new Pickup(260, 446 - 24, true);
        fireballs.add(new Fireball(480, 548 - 23, 1, 1));
        fireballs.add(new Fireball(120, 344 - 23, -1, 3));
        fireballs.add(new Fireball(360, 242 - 23, 1, 4));
    }

    private void setupElevatorShaft() {
        platforms.add(new Platform(20, 650, 220, 650, 0));
        platforms.add(new Platform(420, 650, 620, 650, 0));
        platforms.add(new Platform(20, 520, 205, 520, 0));
        platforms.add(new Platform(435, 520, 620, 520, 0));
        platforms.add(new Platform(20, 390, 220, 390, 0));
        platforms.add(new Platform(420, 390, 620, 390, 0));
        platforms.add(new Platform(20, 260, 205, 260, 0));
        platforms.add(new Platform(435, 260, 620, 260, 0));
        platforms.add(new Platform(150, 145, 500, 145, 0));
        ladders.add(new Ladder(120, 520, 650, false));
        ladders.add(new Ladder(520, 390, 520, false));
        ladders.add(new Ladder(120, 260, 390, false));
        ladders.add(new Ladder(465, 145, 260, false));
        elevators.add(new Elevator(250, 590, 72, 14, 255, 620, -78));
        elevators.add(new Elevator(330, 335, 72, 14, 210, 575, 92));
        player = new Player(55, 650 - 34);
        boss = new Boss(178, 80);
        goal = new Goal(455, 98, true);
        hammerPickup = new Pickup(512, 520 - 24, true);
        fireballs.add(new Fireball(75, 390 - 23, 1, 4));
        fireballs.add(new Fireball(505, 260 - 23, -1, 7));
    }

    private void setupRivetReactor() {
        platforms.add(new Platform(30, 650, 610, 650, 0));
        platforms.add(new Platform(30, 520, 610, 520, 0));
        platforms.add(new Platform(30, 390, 610, 390, 0));
        platforms.add(new Platform(30, 260, 610, 260, 0));
        platforms.add(new Platform(170, 145, 470, 145, 0));
        ladders.add(new Ladder(90, 520, 650, false));
        ladders.add(new Ladder(550, 390, 520, false));
        ladders.add(new Ladder(90, 260, 390, false));
        ladders.add(new Ladder(450, 145, 260, false));
        ladders.add(new Ladder(320, 390, 520, true));
        for (int platformIndex = 0; platformIndex < 4; platformIndex++) {
            Platform platform = platforms.get(platformIndex);
            for (int column = 0; column < 4; column++) {
                double x = 115 + column * 135;
                rivets.add(new Rivet(x, platform.yAt(x) - 5));
            }
        }
        player = new Player(55, 650 - 34);
        boss = new Boss(290, 75);
        goal = new Goal(320, 106, false);
        hammerPickup = new Pickup(500, 390 - 24, true);
        fireballs.add(new Fireball(185, 520 - 23, 1, 1));
        fireballs.add(new Fireball(465, 390 - 23, -1, 2));
        fireballs.add(new Fireball(210, 260 - 23, 1, 3));
    }

    private void addLadder(double x, int topPlatform, int bottomPlatform, boolean broken) {
        Platform top = platforms.get(topPlatform);
        Platform bottom = platforms.get(bottomPlatform);
        ladders.add(new Ladder(x, top.yAt(x), bottom.yAt(x), broken));
    }

    void update(double dt) {
        switch (state) {
            case PLAYING -> updatePlaying(dt);
            case DYING -> {
                stateTimer -= dt;
                updateParticles(dt);
                if (stateTimer <= 0) {
                    if (lives <= 0) {
                        state = GameState.GAME_OVER;
                    } else {
                        initializeStage();
                    }
                }
            }
            case STAGE_CLEAR -> {
                stateTimer -= dt;
                updateParticles(dt);
                if (stateTimer <= 0) {
                    stage++;
                    if (stage > 4) {
                        state = GameState.VICTORY;
                    } else {
                        initializeStage();
                    }
                }
            }
            default -> {
            }
        }
    }

    private void updatePlaying(double dt) {
        bonusTick += dt;
        if (bonusTick >= 1.5) {
            bonusTick -= 1.5;
            bonus = Math.max(0, bonus - 100);
            if (bonus == 0) {
                loseLife();
                return;
            }
        }
        updateElevators(dt);
        updatePlayer(dt);
        if (state != GameState.PLAYING) {
            return;
        }
        switch (boardType) {
            case BARREL_FOUNDRY -> updateBarrels(dt);
            case CONVEYOR_WORKS -> updateFireballs(dt, true);
            case ELEVATOR_SHAFT, RIVET_REACTOR -> updateFireballs(dt, false);
        }
        if (state != GameState.PLAYING) {
            return;
        }
        updateParticles(dt);
        shake = Math.max(0, shake - 24 * dt);
        updateHazardSpawner(dt);
        if (!extraLifeAwarded && score >= 20_000) {
            extraLifeAwarded = true;
            lives++;
            burst(player.centerX(), player.y, Color.CYAN, 28);
        }
    }

    private void updateHazardSpawner(double dt) {
        hazardTimer -= dt;
        if (hazardTimer > 0) {
            return;
        }
        if (boardType == BoardType.BARREL_FOUNDRY) {
            spawnBarrel();
            hazardTimer = 2.25 * (0.82 + random.nextDouble() * 0.35);
            return;
        }
        if (fireballs.size() >= 5) {
            hazardTimer = 3.0;
            return;
        }
        int platformIndex;
        if (boardType == BoardType.CONVEYOR_WORKS) {
            platformIndex = 1 + random.nextInt(4);
        } else if (boardType == BoardType.ELEVATOR_SHAFT) {
            int[] choices = {2, 3, 4, 5, 6, 7};
            platformIndex = choices[random.nextInt(choices.length)];
        } else {
            platformIndex = 1 + random.nextInt(3);
        }
        Platform platform = platforms.get(platformIndex);
        double x = platform.x1 + 30;
        fireballs.add(new Fireball(x, platform.yAt(x) - 23,
                random.nextBoolean() ? 1 : -1, platformIndex));
        hazardTimer = 5.0;
    }

    private void updateElevators(double dt) {
        for (Elevator elevator : elevators) {
            double oldY = elevator.y;
            elevator.y += elevator.speed * dt;
            if (elevator.y <= elevator.minY) {
                elevator.y = elevator.minY;
                elevator.speed = Math.abs(elevator.speed);
            } else if (elevator.y >= elevator.maxY) {
                elevator.y = elevator.maxY;
                elevator.speed = -Math.abs(elevator.speed);
            }
            if (player != null && player.riding == elevator) {
                player.y += elevator.y - oldY;
            }
        }
    }

    private void updatePlayer(double dt) {
        player.invulnerable = Math.max(0, player.invulnerable - dt);
        player.hammerTime = Math.max(0, player.hammerTime - dt);
        player.animation += dt;
        boolean left = isDown(KeyCode.LEFT, KeyCode.A);
        boolean right = isDown(KeyCode.RIGHT, KeyCode.D);
        boolean up = isDown(KeyCode.UP, KeyCode.W);
        boolean down = isDown(KeyCode.DOWN, KeyCode.S);
        boolean jump = wasPressed(KeyCode.SPACE, KeyCode.Z, KeyCode.J);

        Ladder nearby = findNearbyLadder();
        if (!player.onLadder && player.hammerTime <= 0 && nearby != null) {
            double feet = player.y + player.height;
            boolean canEnterFromBottom = up && feet >= nearby.bottomY - 18;
            boolean canEnterFromTop = down && Math.abs(feet - nearby.topY) < 18;
            boolean alreadyWithin = feet > nearby.topY + 10 && feet < nearby.bottomY - 5;
            if (canEnterFromBottom || canEnterFromTop || (alreadyWithin && (up || down))) {
                player.onLadder = true;
                player.ladder = nearby;
                player.riding = null;
                player.vx = 0;
                player.vy = 0;
                player.x = nearby.x - player.width / 2;
            }
        }

        if (player.onLadder) {
            player.vx = 0;
            player.vy = (down ? CLIMB_SPEED : 0) - (up ? CLIMB_SPEED : 0);
            player.x += ((player.ladder.x - player.width / 2) - player.x) * 0.35;
            player.y += player.vy * dt;
            double feet = player.y + player.height;
            if (feet <= player.ladder.topY + 2) {
                player.y = player.ladder.topY - player.height;
                player.onLadder = false;
                player.ladder = null;
                player.onGround = true;
            } else if (player.y >= player.ladder.bottomY - 3) {
                player.y = player.ladder.bottomY - player.height;
                player.onLadder = false;
                player.ladder = null;
                player.onGround = true;
            }
        } else {
            double desired = 0;
            if (left) {
                desired -= PLAYER_SPEED;
                player.facing = -1;
            }
            if (right) {
                desired += PLAYER_SPEED;
                player.facing = 1;
            }
            double response = player.onGround ? 15 : 5;
            player.vx += (desired - player.vx) * Math.min(1, response * dt);
            if (jump && player.onGround && player.hammerTime <= 0) {
                player.vy = -JUMP_SPEED;
                player.onGround = false;
                player.riding = null;
            }
            player.vy += GRAVITY * dt;
            double previousFeet = player.y + player.height;
            player.x += player.vx * dt;
            player.y += player.vy * dt;
            player.onGround = false;
            player.riding = null;
            if (player.vy >= 0 && !landOnStaticPlatform(previousFeet, dt)) {
                landOnElevator(previousFeet);
            }
        }

        player.x = clamp(player.x, 8, WIDTH - player.width - 8);
        if (player.y > HEIGHT + 60) {
            loseLife();
            return;
        }
        if (hammerPickup.active && intersects(player, hammerPickup)) {
            hammerPickup.active = false;
            player.hammerTime = 8.0;
            addScore(300);
            burst(hammerPickup.x + 10, hammerPickup.y + 8, Color.GOLD, 18);
        }
        if (boardType == BoardType.RIVET_REACTOR) {
            updateRivets();
        } else if (goal.active && distance(player.centerX(), player.centerY(), goal.x, goal.y) < 48) {
            clearStage();
        }
    }

    private boolean landOnStaticPlatform(double previousFeet, double dt) {
        for (Platform platform : platforms) {
            if (!platform.containsX(player.centerX())) {
                continue;
            }
            double platformY = platform.yAt(player.centerX());
            double currentFeet = player.y + player.height;
            if (previousFeet <= platformY + 5 && currentFeet >= platformY) {
                player.y = platformY - player.height;
                player.vy = 0;
                player.onGround = true;
                player.x += platform.conveyorDirection * 42 * dt;
                return true;
            }
        }
        return false;
    }

    private boolean landOnElevator(double previousFeet) {
        for (Elevator elevator : elevators) {
            if (player.centerX() < elevator.x || player.centerX() > elevator.x + elevator.width) {
                continue;
            }
            double currentFeet = player.y + player.height;
            if (previousFeet <= elevator.y + 6 && currentFeet >= elevator.y) {
                player.y = elevator.y - player.height;
                player.vy = 0;
                player.onGround = true;
                player.riding = elevator;
                return true;
            }
        }
        return false;
    }

    private void updateRivets() {
        int active = 0;
        for (Rivet rivet : rivets) {
            if (!rivet.active) {
                continue;
            }
            active++;
            if (Math.abs(player.centerX() - rivet.x) < 18
                    && Math.abs((player.y + player.height) - rivet.y) < 12) {
                rivet.active = false;
                active--;
                addScore(100);
                burst(rivet.x, rivet.y, Color.GOLD, 10);
            }
        }
        if (active == 0) {
            clearStage();
        }
    }

    private Ladder findNearbyLadder() {
        Ladder closest = null;
        double best = 999;
        double feet = player.y + player.height;
        for (Ladder ladder : ladders) {
            if (ladder.broken || feet < ladder.topY - 15 || player.y > ladder.bottomY + 8) {
                continue;
            }
            double candidate = Math.abs(player.centerX() - ladder.x);
            if (candidate < best && candidate < 20) {
                best = candidate;
                closest = ladder;
            }
        }
        return closest;
    }

    private void spawnBarrel() {
        Platform top = platforms.get(platforms.size() - 1);
        Barrel barrel = new Barrel(boss.x + 62, top.yAt(boss.x + 62) - 12);
        barrel.platformIndex = platforms.size() - 1;
        barrel.vx = 76;
        barrels.add(barrel);
        boss.throwAnimation = 0.35;
    }

    private void updateBarrels(double dt) {
        boss.throwAnimation = Math.max(0, boss.throwAnimation - dt);
        Iterator<Barrel> iterator = barrels.iterator();
        while (iterator.hasNext()) {
            Barrel barrel = iterator.next();
            barrel.angle += Math.abs(barrel.vx) / Math.max(1, barrel.radius) * dt;
            if (barrel.state == BarrelState.LADDER) {
                barrel.y += 92 * dt;
                barrel.angle += 4 * dt;
                if (barrel.y + barrel.radius >= barrel.ladder.bottomY) {
                    barrel.state = BarrelState.FALLING;
                    barrel.vy = 40;
                    barrel.vx = 0;
                    barrel.ladder = null;
                    barrel.platformIndex = -1;
                }
            } else if (barrel.state == BarrelState.ROLLING) {
                Platform platform = platforms.get(barrel.platformIndex);
                barrel.x += barrel.vx * dt;
                barrel.y = platform.yAt(clamp(barrel.x, platform.x1, platform.x2)) - barrel.radius;
                Ladder descent = ladderAtBarrel(barrel, platform);
                if (descent != null && random.nextDouble() < 0.014) {
                    barrel.state = BarrelState.LADDER;
                    barrel.ladder = descent;
                    barrel.x = descent.x;
                    barrel.vx = 0;
                } else if (barrel.x < platform.x1 - barrel.radius
                        || barrel.x > platform.x2 + barrel.radius) {
                    barrel.state = BarrelState.FALLING;
                    barrel.vy = 35;
                    barrel.x = clamp(barrel.x, 4, WIDTH - 4);
                }
            } else {
                double previousBottom = barrel.y + barrel.radius;
                barrel.vy += 780 * dt;
                barrel.y += barrel.vy * dt;
                Landing landing = findBarrelLanding(barrel, previousBottom);
                if (landing != null) {
                    barrel.state = BarrelState.ROLLING;
                    barrel.platformIndex = landing.platformIndex();
                    barrel.y = landing.y() - barrel.radius;
                    barrel.vy = 0;
                    Platform landed = platforms.get(landing.platformIndex());
                    barrel.vx = landed.y2 >= landed.y1 ? 76 : -76;
                }
            }
            if (barrel.y > HEIGHT + 40) {
                iterator.remove();
                continue;
            }
            if (player.hammerTime > 0 && hammerHits(barrel.x, barrel.y, barrel.radius)) {
                addScore(500);
                burst(barrel.x, barrel.y, Color.ORANGE, 24);
                shake = 6;
                iterator.remove();
                continue;
            }
            if (circleIntersectsRect(barrel.x, barrel.y, barrel.radius, player)) {
                loseLife();
                return;
            }
            if (!barrel.jumpScored && !player.onGround && player.vy < 80
                    && Math.abs(player.centerX() - barrel.x) < 36
                    && player.y + player.height < barrel.y - 4) {
                barrel.jumpScored = true;
                addScore(100);
            }
        }
    }

    private void updateFireballs(double dt, boolean conveyorBoost) {
        Iterator<Fireball> iterator = fireballs.iterator();
        while (iterator.hasNext()) {
            Fireball fireball = iterator.next();
            fireball.phase += dt * 9;
            Platform platform = platforms.get(fireball.platformIndex);
            double speed = 60;
            if (conveyorBoost) {
                speed += platform.conveyorDirection * fireball.direction * 28;
            }
            fireball.x += fireball.direction * Math.max(32, speed) * dt;
            if (fireball.x < platform.x1 + 14) {
                fireball.x = platform.x1 + 14;
                fireball.direction = 1;
            } else if (fireball.x + fireball.width > platform.x2 - 14) {
                fireball.x = platform.x2 - fireball.width - 14;
                fireball.direction = -1;
            }
            fireball.y = platform.yAt(fireball.x + fireball.width / 2) - fireball.height;
            if (player.hammerTime > 0
                    && hammerHits(fireball.x + fireball.width / 2,
                            fireball.y + fireball.height / 2, 12)) {
                addScore(300);
                burst(fireball.x, fireball.y, Color.ORANGE, 18);
                iterator.remove();
                continue;
            }
            if (rectIntersects(player.x, player.y, player.width, player.height,
                    fireball.x, fireball.y, fireball.width, fireball.height)) {
                loseLife();
                return;
            }
        }
    }

    private Ladder ladderAtBarrel(Barrel barrel, Platform platform) {
        for (Ladder ladder : ladders) {
            if (ladder.broken || Math.abs(ladder.topY - platform.yAt(ladder.x)) > 5) {
                continue;
            }
            if (Math.abs(barrel.x - ladder.x) < 3.5) {
                return ladder;
            }
        }
        return null;
    }

    private Landing findBarrelLanding(Barrel barrel, double previousBottom) {
        Landing best = null;
        double nearest = Double.MAX_VALUE;
        for (int index = 0; index < platforms.size(); index++) {
            Platform platform = platforms.get(index);
            if (!platform.containsX(barrel.x)) {
                continue;
            }
            double platformY = platform.yAt(barrel.x);
            double currentBottom = barrel.y + barrel.radius;
            if (previousBottom <= platformY + 4 && currentBottom >= platformY) {
                double candidate = Math.abs(platformY - currentBottom);
                if (candidate < nearest) {
                    nearest = candidate;
                    best = new Landing(index, platformY);
                }
            }
        }
        return best;
    }

    private boolean hammerHits(double x, double y, double radius) {
        double phase = (player.animation * 10) % (Math.PI * 2);
        if (Math.sin(phase) <= -0.15) {
            return false;
        }
        double hammerX = player.centerX() + player.facing * 27;
        double hammerY = player.y + 5;
        return distance(hammerX, hammerY, x, y) < radius + 24;
    }

    private void loseLife() {
        if (state != GameState.PLAYING || player.invulnerable > 0) {
            return;
        }
        lives--;
        state = GameState.DYING;
        stateTimer = 1.25;
        shake = 12;
        burst(player.centerX(), player.centerY(), Color.RED, 34);
    }

    private void clearStage() {
        if (state != GameState.PLAYING) {
            return;
        }
        addScore(bonus);
        state = GameState.STAGE_CLEAR;
        stateTimer = 1.8;
        burst(goal.x, goal.y, Color.CYAN, 42);
    }

    private void addScore(int value) {
        score += value;
        if (score > highScore) {
            highScore = score;
            preferences.putInt("high-score", highScore);
        }
    }

    private void burst(double x, double y, Color color, int count) {
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 55 + random.nextDouble() * 190;
            particles.add(new Particle(x, y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 70,
                    0.45 + random.nextDouble() * 0.55,
                    color));
        }
    }

    private void updateParticles(double dt) {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.life -= dt;
            particle.vy += 420 * dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            if (particle.life <= 0) {
                iterator.remove();
            }
        }
    }

    private boolean isDown(KeyCode... codes) {
        for (KeyCode code : codes) {
            if (keysDown.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private boolean wasPressed(KeyCode... codes) {
        for (KeyCode code : codes) {
            if (keysPressed.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(Player player, Pickup pickup) {
        return rectIntersects(player.x, player.y, player.width, player.height,
                pickup.x, pickup.y, pickup.width, pickup.height);
    }

    private static boolean circleIntersectsRect(double cx, double cy,
            double radius, Player rectangle) {
        double nearestX = clamp(cx, rectangle.x, rectangle.x + rectangle.width);
        double nearestY = clamp(cy, rectangle.y, rectangle.y + rectangle.height);
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }
}
