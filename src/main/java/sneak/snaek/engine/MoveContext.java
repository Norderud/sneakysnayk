package sneak.snaek.engine;

import sneak.snaek.model.Coord;
import sneak.snaek.model.Move;
import sneak.snaek.strategy.SurvivalArea;

/**
 * Context for a candidate move, holding pre-calculated data for that move.
 *
 * <p>{@code myDist} semantics: hops from {@code next}. With time-aware
 * flood enabled (default), distances respect {@link sneak.snaek.board.VacateMap}
 * — cells behind a snake body that will slither out of the way before
 * we arrive are reachable. Without it, snake bodies are static walls
 * (legacy behaviour, fallback only).
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
