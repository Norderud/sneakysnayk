package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for aggression (H2H threats and trapping opponents).
 */
public class AggressionScorer implements Scorer {
    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;
        
        int myLength = ctx.turn().state().you().length();
        return MoveScorer.aggressionBonus(ctx.next(), ctx.turn().enemies(), myLength, ctx.trapped(), ctx.owned());
    }
}
