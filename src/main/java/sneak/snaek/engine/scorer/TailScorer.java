package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for tail-related heuristics (rescue and stretch).
 */
public class TailScorer implements Scorer {
    private final double multiplier;

    public TailScorer() {
        this(1.0);
    }

    public TailScorer(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;
        
        int myLength = ctx.turn().state().you().length();
        double tailRescue = MoveScorer.tailRescueBonus(ctx.canReachTail(), ctx.tailDist(), ctx.owned(), myLength);
        double stretch = MoveScorer.stretchBonus(ctx.canReachTail(), ctx.tailDist(), ctx.owned(), myLength);
        
        return multiplier * (tailRescue + stretch);
    }
}
