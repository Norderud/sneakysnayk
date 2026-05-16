package sneak.snaek.engine;

import sneak.snaek.model.Coord;
import sneak.snaek.model.Move;
import sneak.snaek.strategy.SurvivalArea;

/**
 * Context for a candidate move, holding pre-calculated data for that move.
 */
public record MoveContext(
        TurnContext turn,
        Move move,
        Coord next,
        int[][] myDist,
        SurvivalArea.Area owned,
        boolean trapped,
        boolean canReachTail,
        int tailDist
) {}
