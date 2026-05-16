package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for tail-related heuristics (rescue and stretch).
 */
public class TailScorer implements Scorer {
    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;
        
        int myLength = ctx.turn().state().you().length();
        double tailRescue = MoveScorer.tailRescueBonus(ctx.canReachTail(), ctx.tailDist(), ctx.owned(), myLength);
        double stretch = MoveScorer.stretchBonus(ctx.canReachTail(), ctx.tailDist(), ctx.owned(), myLength);
        
        return tailRescue + stretch;
    }
}
