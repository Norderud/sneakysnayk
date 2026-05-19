package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Board;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Game;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Ruleset;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModeDetectorTest {

    @Test
    public void testDetectsConstrictor() {
        assertEquals(GameMode.CONSTRICTOR, ModeDetector.detect(stateWith("constrictor", 4)));
    }

    @Test
    public void testDetectsWrappedConstrictor() {
        assertEquals(GameMode.WRAPPED_CONSTRICTOR, ModeDetector.detect(stateWith("wrapped-constrictor", 4)));
    }

    @Test
    public void testDetectsWrapped() {
        assertEquals(GameMode.WRAPPED, ModeDetector.detect(stateWith("wrapped", 4)));
    }

    @Test
    public void testDetectsRoyale() {
        assertEquals(GameMode.ROYALE, ModeDetector.detect(stateWith("royale", 4)));
    }

    @Test
    public void testDetectsExplicitSolo() {
        assertEquals(GameMode.SOLO, ModeDetector.detect(stateWith("solo", 1)));
    }

    @Test
    public void testStandardWithOneSnakeIsSolo() {
        assertEquals(GameMode.SOLO, ModeDetector.detect(stateWith("standard", 1)));
    }

    @Test
    public void testStandardWithTwoSnakesIsDuel() {
        assertEquals(GameMode.DUEL, ModeDetector.detect(stateWith("standard", 2)));
    }

    @Test
    public void testStandardWithFourSnakesIsStandard() {
        assertEquals(GameMode.STANDARD, ModeDetector.detect(stateWith("standard", 4)));
    }

    @Test
    public void testEmptyRulesetUsesSnakeCount() {
        assertEquals(GameMode.DUEL, ModeDetector.detect(stateWith("", 2)));
        assertEquals(GameMode.STANDARD, ModeDetector.detect(stateWith("", 4)));
    }

    @Test
    public void testUnknownRulesetReturnsUnknown() {
        assertEquals(GameMode.UNKNOWN, ModeDetector.detect(stateWith("squad", 4)));
    }

    @Test
    public void testCaseInsensitive() {
        assertEquals(GameMode.CONSTRICTOR, ModeDetector.detect(stateWith("CONSTRICTOR", 4)));
        assertEquals(GameMode.ROYALE, ModeDetector.detect(stateWith("Royale", 4)));
    }

    private static GameState stateWith(String rulesetName, int snakeCount) {
        Ruleset ruleset = new Ruleset(rulesetName, "1.0", null);
        Game game = new Game("g", ruleset, 500);
        List<BattleSnake> snakes = new ArrayList<>();
        for (int i = 0; i < snakeCount; i++) {
            Coord head = new Coord(i, 0);
            snakes.add(new BattleSnake("s" + i, "s" + i, 100, List.of(head), "0", head, 1, "", "", null));
        }
        Board board = new Board(11, 11, List.of(), snakes, List.of());
        BattleSnake you = snakes.isEmpty() ? null : snakes.get(0);
        return new GameState(game, 1, board, you);
    }
}

