package arcade;

import javafx.scene.paint.Color;

final class Rules {
    static final double WIDTH = 640;
    static final double HEIGHT = 720;
    static final double HUD_HEIGHT = 76;
    static final double FIXED_STEP = 1.0 / 120.0;
    static final double GRAVITY = 1050;
    static final double PLAYER_SPEED = 160;
    static final double CLIMB_SPEED = 108;
    static final double JUMP_SPEED = 395;

    private Rules() {
    }

    static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    static boolean rectIntersects(double ax, double ay, double aw, double ah,
            double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}

enum GameState {
    TITLE, PLAYING, PAUSED, DYING, STAGE_CLEAR, GAME_OVER, VICTORY
}

enum BoardType {
    BARREL_FOUNDRY("BARREL FOUNDRY"),
    CONVEYOR_WORKS("CONVEYOR WORKS"),
    ELEVATOR_SHAFT("ELEVATOR SHAFT"),
    RIVET_REACTOR("RIVET REACTOR");

    final String displayName;

    BoardType(String displayName) {
        this.displayName = displayName;
    }
}

enum BarrelState {
    ROLLING, FALLING, LADDER
}

final class Platform {
    final double x1;
    final double y1;
    final double x2;
    final double y2;
    final int conveyorDirection;

    Platform(double x1, double y1, double x2, double y2, int conveyorDirection) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.conveyorDirection = conveyorDirection;
    }

    boolean containsX(double x) {
        return x >= x1 - 2 && x <= x2 + 2;
    }

    double yAt(double x) {
        double t = (Rules.clamp(x, x1, x2) - x1) / (x2 - x1);
        return y1 + (y2 - y1) * t;
    }
}

final class Ladder {
    final double x;
    final double topY;
    final double bottomY;
    final boolean broken;

    Ladder(double x, double topY, double bottomY, boolean broken) {
        this.x = x;
        this.topY = topY;
        this.bottomY = bottomY;
        this.broken = broken;
    }
}

final class Player {
    double x;
    double y;
    final double width = 24;
    final double height = 34;
    double vx;
    double vy;
    int facing = 1;
    boolean onGround;
    boolean onLadder;
    Ladder ladder;
    Elevator riding;
    double hammerTime;
    double invulnerable = 1.0;
    double animation;

    Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double centerX() {
        return x + width / 2;
    }

    double centerY() {
        return y + height / 2;
    }
}

final class Barrel {
    double x;
    double y;
    final double radius = 12;
    double vx;
    double vy;
    double angle;
    int platformIndex;
    BarrelState state = BarrelState.ROLLING;
    Ladder ladder;
    boolean jumpScored;

    Barrel(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

final class Fireball {
    double x;
    double y;
    final double width = 24;
    final double height = 24;
    int direction;
    final int platformIndex;
    double phase;

    Fireball(double x, double y, int direction, int platformIndex) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.platformIndex = platformIndex;
    }
}

final class Elevator {
    final double x;
    double y;
    final double width;
    final double height;
    final double minY;
    final double maxY;
    double speed;

    Elevator(double x, double y, double width, double height,
            double minY, double maxY, double speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minY = minY;
        this.maxY = maxY;
        this.speed = speed;
    }
}

final class Rivet {
    final double x;
    final double y;
    boolean active = true;

    Rivet(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

final class Pickup {
    final double x;
    final double y;
    final double width = 26;
    final double height = 26;
    boolean active;

    Pickup(double x, double y, boolean active) {
        this.x = x;
        this.y = y;
        this.active = active;
    }
}

final class Goal {
    final double x;
    final double y;
    final boolean active;

    Goal(double x, double y, boolean active) {
        this.x = x;
        this.y = y;
        this.active = active;
    }
}

final class Boss {
    final double x;
    final double y;
    double throwAnimation;

    Boss(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

final class Particle {
    double x;
    double y;
    double vx;
    double vy;
    double life;
    final double maxLife;
    final Color color;

    Particle(double x, double y, double vx, double vy, double life, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
    }
}

record Landing(int platformIndex, double y) {
}
