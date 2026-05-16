package sneak.snaek.engine.filter;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.TurnContext;
import sneak.snaek.model.Move;
import java.util.Set;

/**
 * Basic safety filter that prunes moves leading into walls or snake bodies.
 */
public class CollisionFilter implements MoveFilter {
    @Override
    public void filter(TurnContext ctx, Set<Move> moves) {
        moves.removeIf(m -> ctx.grid().isBlocked(CoordUtils.neighbor(ctx.state().you().head(), m)));
    }
}
