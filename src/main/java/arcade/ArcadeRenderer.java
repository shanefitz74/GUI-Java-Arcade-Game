package arcade;

import static arcade.Rules.HEIGHT;
import static arcade.Rules.HUD_HEIGHT;
import static arcade.Rules.WIDTH;
import static arcade.Rules.clamp;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

final class ArcadeRenderer {
    private final GraphicsContext graphics;
    private final GameEngine game;

    ArcadeRenderer(GraphicsContext graphics, GameEngine game) {
        this.graphics = graphics;
        this.game = game;
    }

    void render() {
        graphics.save();
        double sx = game.shake > 0 ? (game.random.nextDouble() * 2 - 1) * game.shake : 0;
        double sy = game.shake > 0 ? (game.random.nextDouble() * 2 - 1) * game.shake : 0;
        graphics.translate(sx, sy);
        if (game.state == GameState.TITLE) {
            drawTitle();
        } else if (game.state == GameState.GAME_OVER) {
            drawEndScreen(false);
        } else if (game.state == GameState.VICTORY) {
            drawEndScreen(true);
        } else {
            drawGame();
            if (game.state == GameState.PAUSED) {
                drawOverlay("PAUSED", "PRESS P OR ENTER");
            } else if (game.state == GameState.DYING) {
                drawOverlay("CRASH!", game.lives > 0 ? "GET READY" : "GAME OVER");
            } else if (game.state == GameState.STAGE_CLEAR) {
                drawOverlay("BOARD CLEAR", "BONUS " + game.bonus);
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
        drawText("APE ASCENT", WIDTH / 2, 92, 55, Color.web("#fff0bd"), true);
        drawText("IRONWORKS", WIDTH / 2, 154, 34, Color.web("#58e5ff"), true);
        drawText("FOUR-BOARD ARCADE EDITION", WIDTH / 2, 208, 15,
                Color.web("#bbb7d0"), true);
        graphics.save();
        graphics.translate(108, 316);
        graphics.rotate(-3);
        graphics.setStroke(Color.web("#ff5367"));
        graphics.setLineWidth(15);
        graphics.strokeLine(0, 100, 420, 68);
        graphics.restore();
        drawBoss(135, 254, 1.0);
        drawPlayerDemo(335, 341);
        drawText("BARRELS • CONVEYORS • ELEVATORS • RIVETS",
                WIDTH / 2, 454, 15, Color.GOLD, true);
        drawText("ARROWS / WASD  MOVE + CLIMB",
                WIDTH / 2, 500, 16, Color.web("#fff0bd"), true);
        drawText("SPACE / Z / J  JUMP", WIDTH / 2, 530, 16,
                Color.web("#fff0bd"), true);
        drawText("P OR ESC  PAUSE", WIDTH / 2, 560, 16,
                Color.web("#fff0bd"), true);
        drawText("PRESS ENTER TO START", WIDTH / 2, 619, 24,
                ((System.nanoTime() / 450_000_000L) % 2 == 0)
                        ? Color.GOLD : Color.web("#8b6400"), true);
        drawText("ORIGINAL ART • JAVA 17 + JAVAFX",
                WIDTH / 2, 678, 12, Color.web("#5c637a"), true);
    }

    private void drawGame() {
        graphics.setFill(Color.web("#06070c"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        for (int index = 0; index < 55; index++) {
            double x = (index * 137) % WIDTH;
            double y = HUD_HEIGHT + (index * 71) % (HEIGHT - HUD_HEIGHT);
            graphics.setFill(index % 6 == 0 ? Color.web("#292138") : Color.web("#15131f"));
            graphics.fillRect(x, y, 2, 2);
        }
        for (Platform platform : game.platforms) {
            drawPlatform(platform);
        }
        for (Ladder ladder : game.ladders) {
            drawLadder(ladder);
        }
        for (Elevator elevator : game.elevators) {
            drawElevator(elevator);
        }
        for (Rivet rivet : game.rivets) {
            if (rivet.active) {
                drawRivet(rivet);
            }
        }
        if (game.goal.active) {
            drawGoal();
        }
        drawBoss(game.boss.x,
                game.boss.y + Math.sin(game.boss.throwAnimation * 45) * 3,
                game.boardType == BoardType.RIVET_REACTOR ? 1.05 : 0.9);
        if (game.hammerPickup.active) {
            drawHammerPickup();
        }
        for (Barrel barrel : game.barrels) {
            drawBarrel(barrel);
        }
        for (Fireball fireball : game.fireballs) {
            drawFireball(fireball);
        }
        drawPlayer(game.player);
        drawParticles();
        drawHud();
    }

    private void drawHud() {
        graphics.setFill(Color.web("#080913", 0.96));
        graphics.fillRect(0, 0, WIDTH, HUD_HEIGHT);
        graphics.setStroke(Color.web("#33364a"));
        graphics.strokeLine(0, HUD_HEIGHT - 1, WIDTH, HUD_HEIGHT - 1);
        drawText("1UP", 22, 8, 13, Color.web("#ff5367"), false);
        drawText(String.format("%06d", game.score), 22, 25, 18, Color.WHITE, false);
        drawText("HIGH", WIDTH / 2, 8, 13, Color.GOLD, true);
        drawText(String.format("%06d", game.highScore), WIDTH / 2, 25, 18,
                Color.WHITE, true);
        drawText("STAGE " + game.stage, WIDTH - 22, 8, 13, Color.CYAN,
                false, TextAlignment.RIGHT);
        drawText("BONUS " + game.bonus, WIDTH - 22, 25, 14, Color.GOLD,
                false, TextAlignment.RIGHT);
        drawText("LIVES " + "◆".repeat(Math.max(0, game.lives)), WIDTH - 22, 44, 12,
                Color.web("#58e5ff"), false, TextAlignment.RIGHT);
        drawText(game.boardType.displayName, WIDTH / 2, 52, 12,
                Color.web("#b9b2cf"), true);
        if (game.player.hammerTime > 0) {
            double barWidth = 108 * game.player.hammerTime / 8.0;
            graphics.setFill(Color.web("#272a3b"));
            graphics.fillRect(WIDTH / 2 - 54, 67, 108, 6);
            graphics.setFill(Color.GOLD);
            graphics.fillRect(WIDTH / 2 - 54, 67, barWidth, 6);
        }
    }

    private void drawPlatform(Platform platform) {
        double thickness = 15;
        Color base = platform.conveyorDirection == 0
                ? Color.web("#d93249") : Color.web("#c9432d");
        graphics.setFill(base);
        graphics.beginPath();
        graphics.moveTo(platform.x1, platform.y1);
        graphics.lineTo(platform.x2, platform.y2);
        graphics.lineTo(platform.x2, platform.y2 + thickness);
        graphics.lineTo(platform.x1, platform.y1 + thickness);
        graphics.closePath();
        graphics.fill();
        graphics.setStroke(Color.web("#ff6678"));
        graphics.setLineWidth(3);
        graphics.strokeLine(platform.x1, platform.y1 + 1, platform.x2, platform.y2 + 1);
        graphics.setStroke(Color.web("#651221"));
        graphics.setLineWidth(2);
        for (double x = platform.x1 + 15; x < platform.x2 - 18; x += 34) {
            double y = platform.yAt(x);
            graphics.strokeLine(x, y + 4, x + 14, y + 12);
            graphics.strokeLine(x + 14, y + 4, x, y + 12);
        }
        if (platform.conveyorDirection != 0) {
            graphics.setFill(Color.GOLD);
            for (double x = platform.x1 + 25; x < platform.x2 - 18; x += 62) {
                double y = platform.yAt(x) + 5;
                double direction = platform.conveyorDirection;
                graphics.beginPath();
                graphics.moveTo(x + (direction > 0 ? 10 : 0), y);
                graphics.lineTo(x + (direction > 0 ? 0 : 10), y + 5);
                graphics.lineTo(x + (direction > 0 ? 10 : 0), y + 10);
                graphics.closePath();
                graphics.fill();
            }
        }
    }

    private void drawLadder(Ladder ladder) {
        graphics.setStroke(ladder.broken ? Color.web("#35687a") : Color.web("#58d9f4"));
        graphics.setLineWidth(4);
        graphics.strokeLine(ladder.x - 11, ladder.topY, ladder.x - 11, ladder.bottomY);
        graphics.strokeLine(ladder.x + 11, ladder.topY, ladder.x + 11, ladder.bottomY);
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

    private void drawElevator(Elevator elevator) {
        graphics.setStroke(Color.web("#40465c"));
        graphics.setLineWidth(2);
        graphics.strokeLine(elevator.x + 8, HUD_HEIGHT, elevator.x + 8, elevator.y);
        graphics.strokeLine(elevator.x + elevator.width - 8, HUD_HEIGHT,
                elevator.x + elevator.width - 8, elevator.y);
        graphics.setFill(Color.web("#5e647c"));
        graphics.fillRect(elevator.x, elevator.y, elevator.width, elevator.height);
        graphics.setStroke(Color.web("#d8ddeb"));
        graphics.strokeRect(elevator.x, elevator.y, elevator.width, elevator.height);
    }

    private void drawRivet(Rivet rivet) {
        graphics.setFill(Color.GOLD);
        graphics.fillOval(rivet.x - 6, rivet.y - 6, 12, 12);
        graphics.setStroke(Color.web("#fff0a0"));
        graphics.strokeOval(rivet.x - 4, rivet.y - 4, 8, 8);
    }

    private void drawPlayer(Player player) {
        if (player.invulnerable > 0 && ((int) (player.invulnerable * 12)) % 2 == 0) {
            return;
        }
        double x = Math.round(player.centerX());
        double y = Math.round(player.centerY());
        double walk = player.onGround ? Math.sin(player.animation * 13) * 2.5 : 0;
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(player.facing, 1);
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
        Player demo = new Player(x, y);
        demo.onGround = true;
        demo.animation = System.nanoTime() / 1_000_000_000.0;
        drawPlayer(demo);
    }

    private void drawBoss(double x, double y, double scale) {
        graphics.save();
        graphics.translate(x, y);
        graphics.scale(scale, scale);
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
        Goal goal = game.goal;
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
        Pickup pickup = game.hammerPickup;
        graphics.setFill(Color.web("#a06d36"));
        graphics.fillRect(pickup.x + 10, pickup.y, 5, 26);
        graphics.setFill(Color.web("#dfe4ec"));
        graphics.fillRect(pickup.x, pickup.y, 26, 9);
        graphics.setStroke(Color.WHITE);
        graphics.strokeRect(pickup.x, pickup.y, 26, 9);
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

    private void drawFireball(Fireball fireball) {
        double flicker = Math.sin(fireball.phase) * 4;
        double x = fireball.x + fireball.width / 2;
        double y = fireball.y + fireball.height;
        graphics.setFill(Color.web("#ff432f"));
        graphics.beginPath();
        graphics.moveTo(x - 13, y);
        graphics.quadraticCurveTo(x - 17, y - 18, x, y - 29 - flicker);
        graphics.quadraticCurveTo(x + 18, y - 17, x + 13, y);
        graphics.closePath();
        graphics.fill();
        graphics.setFill(Color.web("#ffd23f"));
        graphics.beginPath();
        graphics.moveTo(x - 7, y);
        graphics.quadraticCurveTo(x - 8, y - 12, x, y - 19 + flicker);
        graphics.quadraticCurveTo(x + 9, y - 10, x + 7, y);
        graphics.closePath();
        graphics.fill();
        graphics.setFill(Color.web("#151015"));
        graphics.fillRect(x - 7, y - 10, 4, 4);
        graphics.fillRect(x + 3, y - 10, 4, 4);
    }

    private void drawParticles() {
        for (Particle particle : game.particles) {
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
        drawText(victory ? "ALL FOUR MACHINES DISABLED"
                        : "THE FOUNDRY CLAIMS ANOTHER",
                WIDTH / 2, 215, 17, Color.web("#fff0bd"), true);
        drawText("SCORE  " + String.format("%06d", game.score),
                WIDTH / 2, 320, 28, Color.GOLD, true);
        drawText("HIGH   " + String.format("%06d", game.highScore),
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
}
