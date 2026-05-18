package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersonalityTest {

    @Test
    public void testPersonalities() {
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head, new Coord(5, 4), new Coord(5, 3));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        for (Personality p : Personality.values()) {
            ModularSnakeEngine engine = PersonalityEngineFactory.create(p);
            Move move = engine.move(state);
            assertNotNull(move, "Move should not be null for personality " + p);
        }
    }
}
