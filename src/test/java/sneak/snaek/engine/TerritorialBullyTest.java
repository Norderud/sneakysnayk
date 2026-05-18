package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.engine.scorer.AggressionScorer;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TerritorialBullyTest {
    @Test
    public void testBullyPressure() {
        // Board 11x11
        // Us at (1, 1). Length 10.
        // Enemy at (0, 0). Length 5.
        
        Coord headA = new Coord(1, 1);
        List<Coord> bodyA = List.of(headA, new Coord(1, 2), new Coord(1, 3), new Coord(1, 4), new Coord(1, 5),
                                    new Coord(1, 6), new Coord(1, 7), new Coord(1, 8), new Coord(1, 9), new Coord(1, 10));
        BattleSnake you = new BattleSnake("you", "you", 100, bodyA, "0", headA, 10, "", "", null);
        
        Coord headB = new Coord(0, 0);
        List<Coord> bodyB = List.of(headB, new Coord(0, 1), new Coord(0, 2), new Coord(0, 3), new Coord(0, 4));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, bodyB, "0", headB, 5, "", "", null);
        
        Board board = new Board(11, 11, List.of(), List.of(you, enemy), List.of());
        GameState state = new GameState(new Game("test", null, 500), 1, board, you);
        
        TurnContext ctx = TurnContext.from(state, Personality.BULLY);
        
        // Candidates from (1,1): (1,0) [DOWN], (2,1) [RIGHT]
        // (1,0) is closer to enemy head (0,0) and blocks their path to the right.
        
        MoveContext ctxDown = ctx.createMoveContext(Move.DOWN);
        MoveContext ctxRight = ctx.createMoveContext(Move.RIGHT);
        
        AggressionScorer scorer = new AggressionScorer();
        double scoreDown = scorer.score(ctxDown);
        double scoreRight = scorer.score(ctxRight);
        
        System.out.println("[DEBUG_LOG] Score Down (Aggression): " + scoreDown);
        System.out.println("[DEBUG_LOG] Score Right (Aggression): " + scoreRight);
        System.out.println("[DEBUG_LOG] Enemy Area after Down: " + ctxDown.owned().enemyRawCounts()[0]);
        System.out.println("[DEBUG_LOG] Enemy Area after Right: " + ctxRight.owned().enemyRawCounts()[0]);

        // Moving DOWN should have a higher aggression score because it's more aggressive towards (0,0).
        assertTrue(scoreDown > scoreRight, "Moving DOWN should be more aggressive than moving RIGHT");
        assertTrue(scoreDown > 0, "Aggression score should be positive");
    }
}
