package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import sneak.snaek.model.Move;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrapSensitivityTest {

    @Test
    public void testVoronoiPressureVsFood() {
        // Snake A (us) is at (9, 1), moving towards food at (10, 1).
        // Snake B (enemy) is at (6, 4), moving to cut us off.
        // Board is 11x11.
        
        Coord headA = new Coord(9, 1);
        // Long snake (length 25) to trigger Voronoi pressure
        List<Coord> bodyA = new ArrayList<>();
        bodyA.add(headA);
        for (int i = 1; i < 25; i++) bodyA.add(new Coord(Math.max(0, 9-i), 1));
        
        BattleSnake you = new BattleSnake("you", "you", 100, bodyA, "0", headA, bodyA.size(), "", "", null);
        
        Coord headB = new Coord(6, 4);
        // Long snake (length 25)
        List<Coord> bodyB = new ArrayList<>();
        bodyB.add(headB);
        for (int i = 1; i < 25; i++) bodyB.add(new Coord(Math.max(0, 6-i), 4));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, bodyB, "0", headB, bodyB.size(), "", "", null);
        
        Coord food = new Coord(10, 1);
        Board board = new Board(11, 11, List.of(food), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);
        
        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);
        
        // With improved trap detection (rawCount < length), it should avoid RIGHT (10,1)
        // because it's a high-pressure zone even if not physically blocked yet.
        assertTrue(move != Move.RIGHT, "Should avoid RIGHT due to Voronoi pressure (trapped by enemy)");
    }
}
