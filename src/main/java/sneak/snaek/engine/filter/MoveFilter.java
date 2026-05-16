package sneak.snaek.engine.filter;

import sneak.snaek.engine.TurnContext;
import sneak.snaek.model.Move;
import java.util.Set;

/**
 * Filter that prunes moves from the set of available moves.
 */
public interface MoveFilter {
    /**
     * Filters the given set of moves. Implementation should modify the set in-place
     * or return a new set if desired (though in-place is preferred for efficiency).
     */
    void filter(TurnContext ctx, Set<Move> moves);
}
