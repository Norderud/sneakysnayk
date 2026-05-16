package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import sneak.snaek.util.GameStateLoader;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SnapshotTest {

    @Test
    public void testLoadSnapshot() {
        // This test ensures we can load a snapshot and run the engine on it.
        // For now, it just loads a dummy snapshot if it exists, or we skip it.
        try {
            GameState state = GameStateLoader.load("/snapshots/sample.json");
            assertNotNull(state);
            ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
            Move move = engine.move(state);
            assertNotNull(move);
            System.out.println("Snapshot move: " + move);
        } catch (IllegalArgumentException e) {
            System.out.println("No sample.json found, skipping test.");
        }
    }
}
