package arcade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.prefs.Preferences;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * APE ASCENT: IRONWORKS
 *
 * An original JavaFX single-screen climbing arcade game inspired by the
 * mechanics of early 1980s arcade platform games. It contains no Nintendo
 * sprites, music, names, ROM data, or source code.
 */
public final class ArcadeAscent extends Application {

    private static final double WIDTH = 640;
    private static final double HEIGHT = 720;
    private static final double HUD_HEIGHT = 70;
    private static final double FIXED_STEP = 1.0 / 120.0;
    private static final double GRAVITY = 1050;
    private static final double PLAYER_SPEED = 155;
    private static final double CLIMB_SPEED = 105;
    private static final double JUMP_SPEED = 390;

    private final Random random = new Random();
    private final Set<KeyCode> keysDown = new HashSet<>();
    private final Set<KeyCode> keysPressed = new HashSet<>();
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Ladder> ladders = new ArrayList<>();
    private final List<Barrel> barrels = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Preferences preferences =
            Preferences.userNodeForPackage(ArcadeAscent.class);

    private Canvas canvas;
    private GraphicsContext graphics;
    private GameState state = GameState.TITLE;

    private Player player;
    private Pickup hammerPickup;
    private Goal goal;
    private Boss boss;

    private int score;
    private int highScore;
    private int lives;
    private int stage;
    private int bonus;
    private boolean extraLifeAwarded;
    private double bonusTick;
    private double barrelTimer;
    private double stateTimer;
    private double shake;
    private double accumulator;
    private long previousNanos;

    private enum GameState {
        TITLE, PLAYING, PAUSED, DYING, STAGE_CLEAR, GAME_OVER, VICTORY
    }

    private enum BarrelState {
        ROLLING, FALLING, LADDER
    }

    @Override
    public void start(Stage stageWindow) {
        highScore = preferences.getInt("high-score", 0);

        canvas = new Canvas(WIDTH, HEIGHT);
        graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(false);

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #05050a;");
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);

        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (keysDown.add(code)) {
                keysPressed.add(code);
            }
            switch (code) {
                case ENTER -> handleStart();
                case P, ESCAPE -> togglePause();
                default -> {
                    // Movement is handled by the fixed-step game loop.
                }
            }
            event.consume();
        });
        scene.setOnKeyReleased(event -> {
            keysDown.remove(event.getCode());
            event.consume();
        });

        stageWindow.setTitle("APE ASCENT: Ironworks");
        stageWindow.setResizable(false);
        stageWindow.setScene(scene);
        stageWindow.sizeToScene();
        stageWindow.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (previousNanos == 0) {
                    previousNanos = now;
                }
                double elapsed = Math.min(0.05, (now - previousNanos) / 1_000_000_000.0);
                previousNanos = now;
                accumulator += elapsed;

                while (accumulator >= FIXED_STEP) {
                    update(FIXED_STEP);
                    accumulator -= FIXED_STEP;
                }
                render();
                keysPressed.clear();
            }
        };
        timer.start();
    }

    private void handleStart() {
        if (state == GameState.TITLE
                || state == GameState.GAME_OVER
                || state == GameState.VICTORY) {
            newGame();
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    private void togglePause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        } else if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    private void newGame() {
        score = 0;
        lives = 3;
        stage = 1;
        extraLifeAwarded = false;
        initializeStage();
    }

    private void initializeStage() {
        platforms.clear();
        ladders.clear();
        barrels.clear();
        particles.clear();

        // Six alternating sloped girders form the classic climbing route.
        platforms.add(new Platform(18, 654, 622, 632));
        platforms.add(new Platform(18, 538, 622, 564));
        platforms.add(new Platform(18, 466, 622, 438));
        platforms.add(new Platform(18, 348, 622, 376));
        platforms.add(new Platform(18, 278, 622, 248));
        platforms.add(new Platform(122, 142, 520, 142));

        addLadder(548, 1, 0, false);
        addLadder(112, 2, 1, false);
        addLadder(498, 3, 2, false);
        addLadder(174, 4, 3, false);
        addLadder(438, 5, 4, false);

        // Decorative/broken ladders create arcade-like route reading.
        addLadder(324, 1, 0, true);
        addLadder(348, 3, 2, true);
        addLadder(298, 5, 4, true);

        Platform bottom = platforms.get(0);
        player = new Player(58, bottom.yAt(70) - 34);
        boss = new Boss(145, 75);
        goal = new Goal(475, 94);

        Platform hammerPlatform = platforms.get(2);
        double hammerX = 410;
        hammerPickup = new Pickup(hammerX, hammerPlatform.yAt(hammerX) - 24);

        bonus = 5_000;
        bonusTick = 0;
        barrelTimer = 1.25;
        stateTimer = 0;
        shake = 0;
        state = GameState.PLAYING;
    }

    private void addLadder(double x, int topPlatform, int bottomPlatform, boolean broken) {
        Platform top = platforms.get(topPlatform);
        Platform bottom = platforms.get(bottomPlatform);
        double topY = top.yAt(x);
        double bottomY = bottom.yAt(x);
        ladders.add(new Ladder(x, topY, bottomY, broken));
    }

    private void update(double dt) {
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
                // Title, pause, game over, and victory are render-only states.
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

        updatePlayer(dt);
        if (state != GameState.PLAYING) {
            return;
        }

        updateBarrels(dt);
        updateParticles(dt);
        shake = Math.max(0, shake - 24 * dt);

        barrelTimer -= dt;
        if (barrelTimer <= 0) {
            spawnBarrel();
            double base = Math.max(1.15, 2.75 - (stage - 1) * 0.35);
            barrelTimer = base * (0.82 + random.nextDouble() * 0.35);
        }

        if (!extraLifeAwarded && score >= 20_000) {
            extraLifeAwarded = true;
            lives++;
            burst(player.x + player.width / 2, player.y, Color.CYAN, 28);
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
            }

            player.vy += GRAVITY * dt;
            double previousFeet = player.y + player.height;
            player.x += player.vx * dt;
            player.y += player.vy * dt;
            player.onGround = false;

            if (player.vy >= 0) {
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
                        break;
                    }
                }
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

        if (distance(player.centerX(), player.centerY(), goal.x, goal.y) < 48) {
            clearStage();
        }
    }

    private Ladder findNearbyLadder() {
        Ladder closest = null;
        double best = 999;
        double feet = player.y + player.height;
        for (Ladder ladder : ladders) {
            if (ladder.broken) {
                continue;
            }
            if (feet < ladder.topY - 15 || player.y > ladder.bottomY + 8) {
                continue;
            }
            double distance = Math.abs(player.centerX() - ladder.x);
            if (distance < best && distance < 20) {
                best = distance;
                closest = ladder;
            }
        }
        return closest;
    }

    private void spawnBarrel() {
        Platform top = platforms.get(platforms.size() - 1);
        Barrel barrel = new Barrel(boss.x + 62, top.yAt(boss.x + 62) - 12);
        barrel.platformIndex = platforms.size() - 1;
        barrel.vx = 72 + (stage - 1) * 11;
        barrels.add(barrel);
        boss.throwAnimation = 0.35;
    }

    private void updateBarrels(double dt) {
        boss.throwAnimation = Math.max(0, boss.throwAnimation - dt);

        Iterator<Barrel> iterator = barrels.iterator();
        while (iterator.hasNext()) {
            Barrel barrel = iterator.next();
            barrel.angle += (Math.abs(barrel.vx) / Math.max(1, barrel.radius)) * dt;

            if (barrel.state == BarrelState.LADDER) {
                barrel.y += (86 + stage * 6) * dt;
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
                barrel.y = platform.yAt(clamp(barrel.x, platform.x1, platform.x2))
                        - barrel.radius;

                Ladder descent = ladderAtBarrel(barrel, platform);
                if (descent != null && random.nextDouble() < 0.012 + stage * 0.002) {
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
                    barrel.platformIndex = landing.platformIndex;
                    barrel.y = landing.y - barrel.radius;
                    barrel.vy = 0;
                    Platform landed = platforms.get(landing.platformIndex);
                    double speed = 72 + (stage - 1) * 11;
                    barrel.vx = landed.y2 >= landed.y1 ? speed : -speed;
                }
            }

            if (barrel.y > HEIGHT + 40) {
                iterator.remove();
                continue;
            }

            if (player.hammerTime > 0 && hammerHits(barrel)) {
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

            if (!barrel.jumpScored
                    && !player.onGround
                    && player.vy < 80
                    && Math.abs(player.centerX() - barrel.x) < 36
                    && player.y + player.height < barrel.y - 4) {
                barrel.jumpScored = true;
                addScore(100);
            }
        }
    }

    private Ladder ladderAtBarrel(Barrel barrel, Platform platform) {
        for (Ladder ladder : ladders) {
            if (ladder.broken) {
                continue;
            }
            if (Math.abs(ladder.topY - platform.yAt(ladder.x)) > 5) {
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
                double distance = Math.abs(platformY - currentBottom);
                if (distance < nearest) {
                    nearest = distance;
                    best = new Landing(index, platformY);
                }
            }
        }
        return best;
    }

    private boolean hammerHits(Barrel barrel) {
        double phase = (player.animation * 10) % (Math.PI * 2);
        boolean activeSwing = Math.sin(phase) > -0.15;
        if (!activeSwing) {
            return false;
        }
        double hammerX = player.centerX() + player.facing * 27;
        double hammerY = player.y + 5;
        return distance(hammerX, hammerY, barrel.x, barrel.y) < barrel.radius + 24;
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
            particles.add(new Particle(
                    x,
                    y,
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

    private void render() {
        graphics.save();
        double sx = shake > 0 ? (random.nextDouble() * 2 - 1) * shake : 0;
        double sy = shake > 0 ? (random.nextDouble() * 2 - 1) * shake : 0;
        graphics.translate(sx, sy);

        if (state == GameState.TITLE) {
            drawTitle();
        } else if (state == GameState.GAME_OVER) {
            drawEndScreen(false);
        } else if (state == GameState.VICTORY) {
            drawEndScreen(true);
        } else {
            drawGame();
            if (state == GameState.PAUSED) {
                drawOverlay("PAUSED", "PRESS P OR ENTER");
            } else if (state == GameState.DYING) {
                drawOverlay("CRASH!", lives > 0 ? "GET READY" : "GAME OVER");
            } else if (state == GameState.STAGE_CLEAR) {
                drawOverlay("STAGE CLEAR", "BONUS " + bonus);
            }
        }

        graphics.restore();
    }

    private void drawTitle() {
        graphics.setFill(Color.web("#06070c"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        for (int y = 90; y < HEIGHT; y += 48) {
            graphics.setStroke(y % 96 == 0 ? Color.web("#21142f") : Color.web("#141220"));
            graphics.setLineWidth(2);
            graphics.strokeLine(0, y, WIDTH, y);
        }

        drawText("APE ASCENT", WIDTH / 2, 105, 55, Color.web("#fff0bd"), true);
        drawText("IRONWORKS", WIDTH / 2, 168, 34, Color.web("#58e5ff"), true);
        drawText("AN ORIGINAL ARCADE CLIMBING GAME",
                WIDTH / 2, 222, 14, Color.web("#bbb7d0"), true);

        graphics.save();
        graphics.translate(108, 320);
        graphics.rotate(-3);
        graphics.setStroke(Color.web("#ff5367"));
        graphics.setLineWidth(15);
        graphics.strokeLine(0, 100, 420, 68);
        graphics.restore();

        drawBoss(135, 258, 1.0);
        drawPlayerDemo(335, 345);

        drawText("ARROWS / WASD  MOVE + CLIMB",
                WIDTH / 2, 485, 16, Color.web("#fff0bd"), true);
        drawText("SPACE / Z / J  JUMP",
                WIDTH / 2, 516, 16, Color.web("#fff0bd"), true);
        drawText("P OR ESC  PAUSE",
                WIDTH / 2, 547, 16, Color.web("#fff0bd"), true);
        drawText("PRESS ENTER TO START",
                WIDTH / 2, 613, 24,
                ((System.nanoTime() / 450_000_000L) % 2 == 0)
                        ? Color.GOLD : Color.web("#8b6400"),
                true);
        drawText("NO EXTERNAL ASSETS • JAVA 17 + JAVAFX",
                WIDTH / 2, 674, 12, Color.web("#5c637a"), true);
    }

    private void drawGame() {
        graphics.setFill(Color.web("#06070c"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        // Sparse industrial stars/sparks.
        for (int index = 0; index < 55; index++) {
            double x = (index * 137) % WIDTH;
            double y = HUD_HEIGHT + (index * 71) % (HEIGHT - HUD_HEIGHT);
            graphics.setFill(index % 6 == 0
                    ? Color.web("#292138")
                    : Color.web("#15131f"));
            graphics.fillRect(x, y, 2, 2);
        }

        for (Platform platform : platforms) {
            drawPlatform(platform);
        }
        for (Ladder ladder : ladders) {
            drawLadder(ladder);
        }

        drawGoal();
        drawBoss(boss.x, boss.y + Math.sin(boss.throwAnimation * 45) * 3, 0.9);
        if (hammerPickup.active) {
            drawHammerPickup();
        }
        for (Barrel barrel : barrels) {
            drawBarrel(barrel);
        }
        drawPlayer();
        drawParticles();
        drawHud();
    }

    private void drawHud() {
        graphics.setFill(Color.web("#080913", 0.96));
        graphics.fillRect(0, 0, WIDTH, HUD_HEIGHT);
        graphics.setStroke(Color.web("#33364a"));
        graphics.strokeLine(0, HUD_HEIGHT - 1, WIDTH, HUD_HEIGHT - 1);

        drawText("1UP", 24, 9, 14, Color.web("#ff5367"), false);
        drawText(String.format("%06d", score), 24, 27, 19, Color.WHITE, false);

        drawText("HIGH", WIDTH / 2, 9, 14, Color.GOLD, true);
        drawText(String.format("%06d", highScore), WIDTH / 2, 27, 19, Color.WHITE, true);

        drawText("STAGE " + stage, WIDTH - 24, 9, 14, Color.CYAN, false, TextAlignment.RIGHT);
        drawText("BONUS " + bonus, WIDTH - 24, 29, 15, Color.GOLD, false,
                TextAlignment.RIGHT);
        drawText("LIVES " + "◆".repeat(Math.max(0, lives)),
                WIDTH - 24, 49, 12, Color.web("#58e5ff"), false, TextAlignment.RIGHT);

        if (player.hammerTime > 0) {
            double barWidth = 120 * player.hammerTime / 8.0;
            graphics.setFill(Color.web("#272a3b"));
            graphics.fillRect(WIDTH / 2 - 60, 56, 120, 8);
            graphics.setFill(Color.GOLD);
            graphics.fillRect(WIDTH / 2 - 60, 56, barWidth, 8);
        }
    }

    private void drawPlatform(Platform platform) {
        double thickness = 15;
        graphics.setFill(Color.web("#d93249"));
        graphics.beginPath();
        graphics.moveTo(platform.x1, platform.y1);
        graphics.lineTo(platform.x2, platform.y2);
        graphics.lineTo(platform.x2, platform.y2 + thickness);
        graphics.lineTo(platform.x1, platform.y1 + thickness);
        graphics.closePath();
        graphics.fill();

        graphics.setStroke(Color.web("#ff6678"));
        graphics.setLineWidth(3);
        graphics.strokeLine(platform.x1, platform.y1 + 1,
                platform.x2, platform.y2 + 1);

        graphics.setStroke(Color.web("#651221"));
        graphics.setLineWidth(2);
        for (double x = platform.x1 + 15; x < platform.x2 - 18; x += 34) {
            double y = platform.yAt(x);
            graphics.strokeLine(x, y + 4, x + 14, y + 12);
            graphics.strokeLine(x + 14, y + 4, x, y + 12);
        }
    }

    private void drawLadder(Ladder ladder) {
        graphics.setStroke(ladder.broken
                ? Color.web("#35687a")
                : Color.web("#58d9f4"));
        graphics.setLineWidth(4);
        graphics.strokeLine(ladder.x - 11, ladder.topY,
                ladder.x - 11, ladder.bottomY);
        graphics.strokeLine(ladder.x + 11, ladder.topY,
                ladder.x + 11, ladder.bottomY);

        graphics.setLineWidth(3);
        int rung = 0;
        for (double y = ladder.topY + 10; y < ladder.bottomY; y += 13) {
            if (ladder.broken && (rung == 2 || rung == 3)) {
                rung++;
                continue;
            }
            graphics.strokeLine(ladder.x - 10, y, ladder.x + 10, y);
            rung++;
        }
    }

    private void drawPlayer() {
        if (player.invulnerable > 0
                && ((int) (player.invulnerable * 12)) % 2 == 0) {
            return;
        }

        double x = Math.round(player.centerX());
        double y = Math.round(player.centerY());
        double walk = player.onGround ? Math.sin(player.animation * 13) * 2.5 : 0;

        graphics.save();
        graphics.translate(x, y);
        graphics.scale(player.facing, 1);

        // Original mechanic character: cyan hard hat, blue overalls, cream shirt.
        graphics.setFill(Color.web("#20bfd8"));
        graphics.fillRect(-12, -18, 24, 8);
        graphics.fillRect(-9, -23, 18, 6);

        graphics.setFill(Color.web("#f3bb82"));
        graphics.fillRect(-9, -12, 18, 13);
        graphics.setFill(Color.web("#16111a"));
        graphics.fillRect(3, -8, 4, 4);

        graphics.setFill(Color.web("#3f56dd"));
        graphics.fillRect(-12, 1, 24, 17);
        graphics.setFill(Color.web("#fff0bd"));
        graphics.fillRect(-15, 2, 5, 14);
        graphics.fillRect(10, 2, 5, 14);

        graphics.setFill(Color.web("#75371f"));
        graphics.fillRect(-11, 18 + walk, 9, 5);
        graphics.fillRect(2, 18 - walk, 9, 5);

        if (player.hammerTime > 0) {
            double swing = Math.sin(player.animation * 10) * 0.75 - 0.45;
            graphics.rotate(Math.toDegrees(swing));
            graphics.setFill(Color.web("#a06d36"));
            graphics.fillRect(8, -32, 5, 35);
            graphics.setFill(Color.web("#dfe4ec"));
            graphics.fillRect(1, -37, 25, 11);
            graphics.setStroke(Color.WHITE);
            graphics.strokeRect(1, -37, 25, 11);
        }

        graphics.restore();
    }

    private void drawPlayerDemo(double x, double y) {
        Player previous = player;
        player = new Player(x, y);
        player.onGround = true;
        player.animation = System.nanoTime() / 1_000_000_000.0;
        drawPlayer();
        player = previous;
    }

    private void drawBoss(double x, double y, double scale) {
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(scale, scale);

        // Original brass-and-rust foundry ape boss.
        graphics.setFill(Color.web("#361d18"));
        graphics.fillRoundRect(-28, 18, 92, 45, 15, 15);
        graphics.setFill(Color.web("#8f4a2b"));
        graphics.fillRoundRect(-16, 0, 68, 55, 12, 12);
        graphics.setFill(Color.web("#be7041"));
        graphics.fillRoundRect(-2, -16, 43, 38, 10, 10);
        graphics.setFill(Color.web("#171012"));
        graphics.fillRect(6, -4, 7, 7);
        graphics.fillRect(28, -4, 7, 7);
        graphics.setFill(Color.web("#f5d49a"));
        graphics.fillRect(11, 15, 20, 7);
        graphics.setFill(Color.web("#8f4a2b"));
        graphics.fillRoundRect(-35, 15, 20, 58, 9, 9);
        graphics.fillRoundRect(48, 15, 20, 58, 9, 9);
        graphics.setFill(Color.web("#d49a43"));
        graphics.fillRect(-18, 51, 68, 11);

        graphics.restore();
    }

    private void drawGoal() {
        graphics.save();
        graphics.translate(goal.x, goal.y);
        graphics.setFill(Color.web("#895cff"));
        graphics.fillRect(-13, 3, 26, 38);
        graphics.setFill(Color.web("#f4c69a"));
        graphics.fillRect(-10, -18, 20, 23);
        graphics.setFill(Color.web("#fff0bd"));
        graphics.fillRect(-12, -23, 24, 8);
        graphics.setFill(Color.web("#14101a"));
        graphics.fillRect(-5, -10, 4, 4);
        graphics.fillRect(3, -10, 4, 4);
        graphics.setFill(Color.web("#58e5ff"));
        graphics.fillRect(-20, 12, 7, 20);
        graphics.fillRect(13, 12, 7, 20);
        graphics.restore();
        drawText("NOVA", goal.x, goal.y - 45, 12, Color.CYAN, true);
    }

    private void drawHammerPickup() {
        graphics.setFill(Color.web("#a06d36"));
        graphics.fillRect(hammerPickup.x + 10, hammerPickup.y, 5, 26);
        graphics.setFill(Color.web("#dfe4ec"));
        graphics.fillRect(hammerPickup.x, hammerPickup.y, 26, 9);
        graphics.setStroke(Color.WHITE);
        graphics.strokeRect(hammerPickup.x, hammerPickup.y, 26, 9);
    }

    private void drawBarrel(Barrel barrel) {
        graphics.save();
        graphics.translate(barrel.x, barrel.y);
        graphics.rotate(Math.toDegrees(barrel.angle));
        graphics.setFill(Color.web("#a84d26"));
        graphics.fillOval(-barrel.radius, -barrel.radius,
                barrel.radius * 2, barrel.radius * 2);
        graphics.setStroke(Color.web("#ffb14e"));
        graphics.setLineWidth(3);
        graphics.strokeOval(-barrel.radius + 3, -barrel.radius + 3,
                (barrel.radius - 3) * 2, (barrel.radius - 3) * 2);
        graphics.setStroke(Color.web("#35140d"));
        graphics.setLineWidth(2);
        graphics.strokeLine(-barrel.radius, 0, barrel.radius, 0);
        graphics.strokeLine(0, -barrel.radius, 0, barrel.radius);
        graphics.restore();
    }

    private void drawParticles() {
        for (Particle particle : particles) {
            graphics.setGlobalAlpha(clamp(particle.life / particle.maxLife, 0, 1));
            graphics.setFill(particle.color);
            graphics.fillRect(particle.x, particle.y, 4, 4);
        }
        graphics.setGlobalAlpha(1);
    }

    private void drawOverlay(String title, String subtitle) {
        graphics.setFill(Color.web("#03040a", 0.78));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        drawText(title, WIDTH / 2, 296, 43, Color.GOLD, true);
        drawText(subtitle, WIDTH / 2, 355, 18, Color.web("#fff0bd"), true);
    }

    private void drawEndScreen(boolean victory) {
        graphics.setFill(Color.web("#06070c"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        drawText(victory ? "IRONWORKS SAVED!" : "SHIFT OVER",
                WIDTH / 2, 145, 45,
                victory ? Color.CYAN : Color.web("#ff5367"), true);
        drawText(victory ? "BRASSBACK HAS BEEN DEFEATED"
                        : "THE FOUNDRY CLAIMS ANOTHER",
                WIDTH / 2, 215, 17, Color.web("#fff0bd"), true);
        drawText("SCORE  " + String.format("%06d", score),
                WIDTH / 2, 320, 28, Color.GOLD, true);
        drawText("HIGH   " + String.format("%06d", highScore),
                WIDTH / 2, 365, 22, Color.CYAN, true);
        drawText("PRESS ENTER TO PLAY AGAIN",
                WIDTH / 2, 530, 20, Color.web("#fff0bd"), true);
    }

    private void drawText(String text, double x, double y, double size,
            Color color, boolean centered) {
        drawText(text, x, y, size, color, centered,
                centered ? TextAlignment.CENTER : TextAlignment.LEFT);
    }

    private void drawText(String text, double x, double y, double size,
            Color color, boolean centered, TextAlignment alignment) {
        graphics.setFont(Font.font("Monospaced", FontWeight.BOLD, size));
        graphics.setTextBaseline(VPos.TOP);
        graphics.setTextAlign(alignment);
        graphics.setFill(Color.BLACK);
        graphics.fillText(text, x + 2, y + 2);
        graphics.setFill(color);
        graphics.fillText(text, x, y);
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
        return player.x < pickup.x + pickup.width
                && player.x + player.width > pickup.x
                && player.y < pickup.y + pickup.height
                && player.y + player.height > pickup.y;
    }

    private static boolean circleIntersectsRect(double cx, double cy,
            double radius, Player rectangle) {
        double nearestX = clamp(cx, rectangle.x, rectangle.x + rectangle.width);
        double nearestY = clamp(cy, rectangle.y, rectangle.y + rectangle.height);
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static final class Platform {
        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;

        private Platform(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private boolean containsX(double x) {
            return x >= x1 - 2 && x <= x2 + 2;
        }

        private double yAt(double x) {
            double t = (clamp(x, x1, x2) - x1) / (x2 - x1);
            return y1 + (y2 - y1) * t;
        }
    }

    private static final class Ladder {
        private final double x;
        private final double topY;
        private final double bottomY;
        private final boolean broken;

        private Ladder(double x, double topY, double bottomY, boolean broken) {
            this.x = x;
            this.topY = topY;
            this.bottomY = bottomY;
            this.broken = broken;
        }
    }

    private static final class Player {
        private double x;
        private double y;
        private final double width = 24;
        private final double height = 34;
        private double vx;
        private double vy;
        private int facing = 1;
        private boolean onGround;
        private boolean onLadder;
        private Ladder ladder;
        private double hammerTime;
        private double invulnerable = 1.0;
        private double animation;

        private Player(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private double centerX() {
            return x + width / 2;
        }

        private double centerY() {
            return y + height / 2;
        }
    }

    private static final class Barrel {
        private double x;
        private double y;
        private final double radius = 12;
        private double vx;
        private double vy;
        private double angle;
        private int platformIndex;
        private BarrelState state = BarrelState.ROLLING;
        private Ladder ladder;
        private boolean jumpScored;

        private Barrel(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Pickup {
        private final double x;
        private final double y;
        private final double width = 26;
        private final double height = 26;
        private boolean active = true;

        private Pickup(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Goal {
        private final double x;
        private final double y;

        private Goal(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Boss {
        private final double x;
        private final double y;
        private double throwAnimation;

        private Boss(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Particle {
        private double x;
        private double y;
        private double vx;
        private double vy;
        private double life;
        private final double maxLife;
        private final Color color;

        private Particle(double x, double y, double vx, double vy,
                double life, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }

    private record Landing(int platformIndex, double y) {
    }
}
