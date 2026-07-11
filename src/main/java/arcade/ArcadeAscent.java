package arcade;

import static arcade.Rules.FIXED_STEP;
import static arcade.Rules.HEIGHT;
import static arcade.Rules.WIDTH;

import java.util.HashSet;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Launches APE ASCENT: Ironworks, an original four-board JavaFX arcade game.
 */
public final class ArcadeAscent extends Application {
    private final Set<KeyCode> keysDown = new HashSet<>();
    private final Set<KeyCode> keysPressed = new HashSet<>();
    private double accumulator;
    private long previousNanos;

    @Override
    public void start(Stage stageWindow) {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(false);

        GameEngine game = new GameEngine(keysDown, keysPressed);
        ArcadeRenderer renderer = new ArcadeRenderer(graphics, game);

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #05050a;");
        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.BLACK);
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            if (keysDown.add(code)) {
                keysPressed.add(code);
            }
            switch (code) {
                case ENTER -> game.handleStart();
                case P, ESCAPE -> game.togglePause();
                default -> {
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

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (previousNanos == 0) {
                    previousNanos = now;
                }
                double elapsed = Math.min(0.05,
                        (now - previousNanos) / 1_000_000_000.0);
                previousNanos = now;
                accumulator += elapsed;
                while (accumulator >= FIXED_STEP) {
                    game.update(FIXED_STEP);
                    accumulator -= FIXED_STEP;
                }
                renderer.render();
                keysPressed.clear();
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
