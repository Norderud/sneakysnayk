package sneak.snaek.engine.scorer;

import org.junit.jupiter.api.Test;
import sneak.snaek.engine.MoveContext;
import sneak.snaek.engine.Personality;
import sneak.snaek.engine.TurnContext;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Board;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Game;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Move;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompactnessScorerTest {

    @Test
    public void testNoEffectOnShortSnake() {
        GameState s = stateWithSnake(new Coord(5, 5), List.of(
                new Coord(5, 5), new Coord(5, 4), new Coord(5, 3)));
        MoveContext ctx = moveCtx(s, Move.LEFT);
        assertEquals(0.0, new CompactnessScorer().score(ctx), 1e-9);
    }

    @Test
    public void testPenalisesCoiledShape() {
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head,
                new Coord(5, 4), new Coord(5, 3), new Coord(4, 3),
                new Coord(4, 4), new Coord(4, 5));
        GameState s = stateWithSnake(head, body);
        MoveContext ctx = moveCtx(s, Move.LEFT);
        double score = new CompactnessScorer().score(ctx);
        assertTrue(score < -100, "expected strong penalty, got " + score);
    }

    @Test
    public void testRewardsStretchedShape() {
        Coord head = new Coord(5, 5);
        List<Coord> body = List.of(head,
                new Coord(4, 5), new Coord(3, 5), new Coord(2, 5),
                new Coord(1, 5), new Coord(0, 5));
        GameState s = stateWithSnake(head, body);
        MoveContext ctx = moveCtx(s, Move.UP);
        double score = new CompactnessScorer().score(ctx);
        assertTrue(score > 0, "expected positive stretch bonus, got " + score);
    }

    private static GameState stateWithSnake(Coord head, List<Coord> body) {
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, body.size(), "", "", null);
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("g", null, 500);
        return new GameState(game, 1, board, you);
    }

    private static MoveContext moveCtx(GameState state, Move move) {
        TurnContext t = TurnContext.from(state, Personality.BULLY);
        return t.createMoveContext(move);
    }
}
