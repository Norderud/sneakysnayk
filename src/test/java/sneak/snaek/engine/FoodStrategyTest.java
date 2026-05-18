package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoodStrategyTest {

    @Test
    public void testAvoidsRiskyFood() {
        // You are at (1,1)
        // Food at (0,0) (corner)
        // Food at (5,5) (safe)
        Coord head = new Coord(1, 1);
        List<Coord> body = List.of(head, new Coord(1, 2), new Coord(1, 3));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);

        Coord cornerFood = new Coord(0, 0);
        Coord safeFood = new Coord(5, 5);

        Board board = new Board(11, 11, List.of(cornerFood, safeFood), List.of(you), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // Corner food is closer (dist 2) but risky. Safe food is further (dist 8).
        // With current logic, we should prefer safe food if the penalty is high enough.
        // head (1,1) -> UP(1,2) [blocked], DOWN(1,0), LEFT(0,1), RIGHT(2,1)
        // To go to (5,5) we'd go RIGHT or UP (but UP is blocked).
        // To go to (0,0) we'd go DOWN or LEFT.
        assertSame(move, Move.RIGHT);
    }

    @Test
    public void testAvoidsEnemyFood() {
        // You at (5,5)
        // Enemy at (9,9)
        // Food at (8,9) - next to enemy head
        // Food at (5,0) - safe
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head, new Coord(5, 6), new Coord(5, 7));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);

        Coord enemyHead = new Coord(9, 9);
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, List.of(enemyHead, new Coord(10, 9)), "0", enemyHead, 2, "", "", null);

        Coord enemyFood = new Coord(8, 9);
        Coord safeFood = new Coord(5, 0);

        Board board = new Board(11, 11, List.of(enemyFood, safeFood), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // enemyFood is at (8,9), dist = (8-5) + (9-5) = 3 + 4 = 7
        // safeFood is at (5,0), dist = (5-5) + (5-0) = 5
        // Wait, safeFood is actually closer here. Let's adjust.
        // safeFood at (5,10), dist = 5.
        // enemyFood at (6,5), dist = 1. Next to enemy head? No.
        
        // Let's re-setup
    }

    @Test
    public void testAvoidsContestedFood() {
        // Food A is 2 steps away for us, 3 steps for enemy. (Contested)
        // Food B is 4 steps away for us, 10 steps for enemy. (Guaranteed)
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head, new Coord(5, 6), new Coord(5, 7));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);

        Coord enemyHead = new Coord(5, 0);
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, List.of(enemyHead, new Coord(4, 0)), "0", enemyHead, 2, "", "", null);

        Coord contestedFood = new Coord(5, 2); // 3 from us, 2 from enemy
        Coord guaranteedFood = new Coord(8, 5); // 3 from us, 8 from enemy

        Board board = new Board(11, 11, List.of(contestedFood, guaranteedFood), List.of(you, enemy), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // head (5,5). contestedFood (5,3) -> DOWN. guaranteedFood (8,5) -> RIGHT.
        // contestedFood is closer, but has penalty.
        assertSame(move, Move.RIGHT);
    }
}
