package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DuelistTest {
    @Test
    public void testSeeksH2HWithSmallerSnake() {
        // You at (5,5), moving toward (5,6)
        // Enemy head at (5,7)
        // Move UP leads to (5,6) which is adjacent to (5,7)
        Coord myHead = new Coord(5, 5);
        List<Coord> myBody = List.of(myHead, new Coord(4, 5), new Coord(3, 5), new Coord(2, 5));
        BattleSnake you = new BattleSnake("you", "you", 100, myBody, "0", myHead, 4, "", "", null);
        
        // Enemy head at (5,7), body at (6,7), (7,7) - length 3
        Coord enemyHead = new Coord(5, 7);
        List<Coord> enemyBody = List.of(enemyHead, new Coord(6, 7), new Coord(7, 7));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, enemyBody, "0", enemyHead, 3, "", "", null);

        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = PersonalityEngineFactory.create(Personality.DUELIST);
        Move move = engine.move(state);

        // UP brings us to (5,6), which is adjacent to enemy head (5,7).
        // Since we are longer (4 vs 3), H2H is favorable.
        // AggressionScorer and DuelistScorer both should favor UP.
        assertEquals(Move.UP, move, "Duelist should prefer moving into H2H with smaller snake");
    }

    @Test
    public void testSeeksH2HWithEqualSnake() {
        // You at (5,5), length 3
        Coord myHead = new Coord(5, 5);
        List<Coord> myBody = List.of(myHead, new Coord(4, 5), new Coord(3, 5));
        BattleSnake you = new BattleSnake("you", "you", 100, myBody, "0", myHead, 3, "", "", null);
        
        // Enemy head at (5,7), length 3.
        Coord enemyHead = new Coord(5, 7);
        List<Coord> enemyBody = List.of(enemyHead, new Coord(6, 7), new Coord(7, 7));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, enemyBody, "0", enemyHead, 3, "", "", null);

        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test_equal", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = PersonalityEngineFactory.create(Personality.DUELIST);
        Move move = engine.move(state);

        // UP brings us to (5,6), which is adjacent to enemy head (5,7).
        // Since we are DUELIST, HeadToHeadFilter should NOT prune (5,6) even though enemy.length == myLength.
        assertEquals(Move.UP, move, "Duelist should prefer moving into H2H even with equal sized snake");
    }
}
