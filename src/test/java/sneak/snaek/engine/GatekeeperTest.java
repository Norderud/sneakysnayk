package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatekeeperTest {
    @Test
    public void testInterceptEnemyOnPathToFood() {
        // We are at (7, 3). Length 5.
        Coord headA = new Coord(7, 3);
        List<Coord> bodyA = List.of(headA, new Coord(7, 2), new Coord(7, 1), new Coord(7, 0), new Coord(6, 0));
        BattleSnake you = new BattleSnake("you", "you", 100, bodyA, "0", headA, 5, "", "", null);
        
        // Enemy is at (5, 5). Length 3.
        Coord headB = new Coord(5, 5);
        List<Coord> bodyB = List.of(headB, new Coord(4, 5), new Coord(3, 5));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, bodyB, "0", headB, 3, "", "", null);
        
        // Food at (8, 5).
        // Enemy shortest path to food: (6, 5), (7, 5), (8, 5).
        // Enemy arrivals: (6, 5)@1, (7, 5)@2, (8, 5)@3.
        Coord food = new Coord(8, 5);
        
        Board board = new Board(11, 11, List.of(food), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);
        
        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // If we move UP to (7, 4), our head is at (7, 4).
        // From (7, 4), we can reach (7, 5) in 1 turn. Total 2 turns.
        // Enemy reaches (7, 5) in 2 turns. 
        // We are longer (5 > 3), so we can intercept at (7, 5).
        // This should trigger the GATEKEEPER_BONUS for the UP move.
        assertEquals(Move.UP, move, "Should move UP to intercept the enemy on its path to food");
    }
}
