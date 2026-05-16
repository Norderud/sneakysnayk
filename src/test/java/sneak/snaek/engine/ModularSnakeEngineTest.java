package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModularSnakeEngineTest {

    @Test
    public void testAvoidsWall() {
        // Snake at (0,5), facing LEFT (wall at x=-1)
        Coord head = new Coord(0, 5);
        List<Coord> body = List.of(head, new Coord(1, 5), new Coord(2, 5));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);
        
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // LEFT is OOB (-1, 5), RIGHT is body collision (1, 5). 
        // Only UP (0, 6) or DOWN (0, 4) are safe.
        assertTrue(move == Move.UP || move == Move.DOWN, "Expected UP or DOWN but got " + move);
    }

    @Test
    public void testAvoidsSelf() {
        // Head at (5,5). Body loops around to block DOWN and LEFT.
        // Neighbors: UP (5,6), DOWN (5,4), LEFT (4,5), RIGHT (6,5)
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(
            head,
            new Coord(5, 4), // DOWN
            new Coord(4, 4),
            new Coord(4, 5)  // LEFT
        );
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 4, "", "", null);
        
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // DOWN and LEFT are blocked by body.
        // UP and RIGHT are safe.
        assertTrue(move == Move.UP || move == Move.RIGHT, "Expected UP or RIGHT but got " + move);
    }
    
    @Test
    public void testAvoidsOtherSnakes() {
        // You are at (5,5), moving towards another snake at (6,5)
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head, new Coord(5, 4), new Coord(5, 3));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);
        
        // Enemy snake at (6,5)
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, 
            List.of(new Coord(6, 5), new Coord(7, 5), new Coord(8, 5)), 
            "0", new Coord(6, 5), 3, "", "", null);

        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // RIGHT is blocked by enemy. 
        // DOWN is blocked by own body.
        // LEFT (4,5) and UP (5,6) are safe.
        assertTrue(move == Move.LEFT || move == Move.UP, "Expected LEFT or UP but got " + move);
    }
}
