package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LookaheadTest {

    @Test
    public void testLookaheadAvoidsTrap() {
        // Simple case: Moving RIGHT (1,0) leads to a state where next turn
        // we are forced into a wall or body if we don't look ahead.
        // Actually let's make it more direct.
        
        Coord myHead = new Coord(1, 1);
        List<Coord> myBody = List.of(myHead, new Coord(1, 2), new Coord(1, 3));
        BattleSnake you = new BattleSnake("you", "you", 100, myBody, "0", myHead, 3, "", "", null);
        
        // Block everything except RIGHT (2,1) and UP (1,2) [UP is body collision]
        // Actually, Neighbors of (1,1) are (1,2), (1,0), (0,1), (2,1).
        // (1,2) is body. (1,0) wall? No 11x11.
        
        // Let's use walls. 
        // We are at (0, 1). Neighbors: (0, 2), (0, 0), (1, 1).
        // If we move to (1, 1), and then next turn all neighbors of (1, 1) are blocked.
        // Neighbors of (1,1): (1,2), (1,0), (0,1), (2,1).
        // (0,1) is where we came from (body).
        // We can block (1,2), (1,0), (2,1) with an enemy.
        
        myHead = new Coord(0, 1);
        myBody = List.of(myHead, new Coord(0, 2), new Coord(0, 3));
        you = new BattleSnake("you", "you", 100, myBody, "0", myHead, 3, "", "", null);
        
        // Enemy blocks the future of (1,1)
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, 
            List.of(new Coord(2, 1), new Coord(2, 2), new Coord(2, 3)), 
            "0", new Coord(2, 1), 3, "", "", null);

        // Turn 1:
        // We move to (1,1).
        // Enemy moves to (1,2) or stays at (2,1)?
        // If we move to (1,1), we are at (1,1).
        // If enemy moves to (1,0).
        // Then at (1,1) we are surrounded by:
        // (0,1) - our body
        // (1,2) - wall/enemy
        // (1,0) - enemy
        // (2,1) - enemy
        
        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();

        engine.config().enableLookahead = true;
        Move move = engine.move(state);
        
        // It should avoid (1,1) if it leads to death.
        // Neighbors of (0,1) are (0,2) [body], (0,0) [safe], (1,1) [trap].
        assertEquals(Move.DOWN, move, "Should have picked DOWN (0,0) to avoid trap at (1,1)");
    }
}
