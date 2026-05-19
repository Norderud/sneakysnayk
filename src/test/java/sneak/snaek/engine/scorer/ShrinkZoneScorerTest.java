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
import sneak.snaek.model.Royalty;
import sneak.snaek.model.RuleSettings;
import sneak.snaek.model.Ruleset;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShrinkZoneScorerTest {

    @Test
    public void testInactiveOutsideRoyale() {
        GameState state = royaleState(1, 0);
        MoveContext ctx = moveCtx(state, Move.UP);
        assertEquals(0.0, new ShrinkZoneScorer().score(ctx), 1e-9);
    }

    @Test
    public void testInactiveFarFromNextShrink() {
        GameState state = royaleState(1, 25);
        MoveContext ctx = moveCtx(state, Move.UP);
        assertEquals(0.0, new ShrinkZoneScorer().score(ctx), 1e-9);
    }

    @Test
    public void testPenalisesEdgeCellsWhenShrinkImminent() {
        GameState state = royaleHeadAt(23, 25, new Coord(1, 5));
        ShrinkZoneScorer s = new ShrinkZoneScorer();
        double leftScore  = s.score(moveCtx(state, Move.LEFT));
        double rightScore = s.score(moveCtx(state, Move.RIGHT));
        assertTrue(leftScore < 0, "edge candidate should be penalised, got " + leftScore);
        assertTrue(rightScore >= 0, "inward candidate should not be penalised, got " + rightScore);
        assertTrue(rightScore > leftScore, "inward should score strictly higher than edge");
    }

    @Test
    public void testIgnoresAlreadyHazardCells() {
        GameState base = royaleHeadAt(23, 25, new Coord(1, 5));
        Board oldBoard = base.board();
        List<Coord> hazards = List.of(new Coord(0, 5));
        Board newBoard = new Board(oldBoard.height(), oldBoard.width(), oldBoard.food(), oldBoard.snakes(), hazards);
        GameState withHaz = new GameState(base.game(), base.turn(), newBoard, base.you());
        MoveContext leftIntoHaz = moveCtx(withHaz, Move.LEFT);
        assertEquals(0.0, new ShrinkZoneScorer().score(leftIntoHaz), 1e-9);
    }

    private static GameState royaleState(int turn, int shrinkEveryN) {
        return royaleHeadAt(turn, shrinkEveryN, new Coord(5, 5));
    }

    private static GameState royaleHeadAt(int turn, int shrinkEveryN, Coord head) {
        Royalty royalty = shrinkEveryN > 0 ? new Royalty(shrinkEveryN) : null;
        RuleSettings settings = new RuleSettings(0, 0, 15, royalty, null);
        Ruleset ruleset = new Ruleset("royale", "1.0", settings);
        Game game = new Game("g", ruleset, 500);
        BattleSnake you = new BattleSnake("you", "you", 100,
                List.of(head, new Coord(head.x(), head.y() - 1), new Coord(head.x(), head.y() - 2)),
                "0", head, 3, "", "", null);
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        return new GameState(game, turn, board, you);
    }

    private static MoveContext moveCtx(GameState state, Move move) {
        TurnContext t = TurnContext.from(state, Personality.BULLY);
        return t.createMoveContext(move);
    }
}
