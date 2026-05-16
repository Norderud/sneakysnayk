package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AggressionTest {
    @Test
    public void testTrapOpponent() {
        // We are at (1, 1). Enemy is at (0, 0).
        // Enemy body: (0, 0) [head], (1, 0), (2, 0). Length 3.
        // We are length 4: (1, 1) [head], (1, 2), (1, 3), (1, 4).
        
        Coord headA = new Coord(1, 1);
        List<Coord> bodyA = List.of(headA, new Coord(1, 2), new Coord(1, 3), new Coord(1, 4));
        BattleSnake you = new BattleSnake("you", "you", 100, bodyA, "0", headA, 4, "", "", null);
        
        Coord headB = new Coord(0, 0);
        List<Coord> bodyB = List.of(headB, new Coord(1, 0), new Coord(2, 0));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, bodyB, "0", headB, 3, "", "", null);
        
        // Board 11x11
        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);
        
        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // Candidate moves from (1, 1): DOWN (1, 0) [Filtered], LEFT (0, 1), RIGHT (2, 1)
        // (0, 1) should be chosen because it traps the enemy at (0, 0) 
        // by winning the race to its only escape cell (0, 1) via H2H tie-break.
        assertEquals(Move.LEFT, move, "Should move LEFT to trap the opponent");
    }
}
