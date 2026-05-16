package sneak.snaek.sim;

import org.junit.jupiter.api.Test;

public class SimulationTest {
    @Test
    void testSmallSimulation() {
        // Just run the main method with a limit or a small board
        // To keep it simple, we just call a version of it.
        LocalSimulationRunner.main(new String[]{});
    }
}
