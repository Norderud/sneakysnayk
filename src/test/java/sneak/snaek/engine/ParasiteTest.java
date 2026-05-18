package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParasiteTest {
    @Test
    public void testShadowsEnemyTail() {
        // You at (5,5), moving toward (5,6)
        // Enemy tail at (5,7)
        // Move UP leads to (5,6) which is adjacent to (5,7)
        Coord myHead = new Coord(5, 5);
        List<Coord> myBody = List.of(myHead, new Coord(4, 5), new Coord(3, 5));
        BattleSnake you = new BattleSnake("you", "you", 100, myBody, "0", myHead, 3, "", "", null);
        
        // Enemy at (5,9), (5,8), (5,7) - tail at (5,7)
        Coord enemyHead = new Coord(5, 9);
        List<Coord> enemyBody = List.of(enemyHead, new Coord(5, 8), new Coord(5, 7));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, enemyBody, "0", enemyHead, 3, "", "", null);

        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = PersonalityEngineFactory.create(Personality.PARASITE);
        Move move = engine.move(state);

        // UP brings us to (5,6), which is adjacent to enemy tail (5,7).
        assertEquals(Move.UP, move, "Parasite should prefer moving adjacent to enemy tail");
    }
}
