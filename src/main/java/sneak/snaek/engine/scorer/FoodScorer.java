package sneak.snaek.engine.scorer;

import sneak.snaek.engine.MoveContext;

/**
 * Scorer for food proximity.
 */
public class FoodScorer implements Scorer {
    private final double multiplier;

    public FoodScorer() {
        this(1.0);
    }

    public FoodScorer(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double score(MoveContext ctx) {
        return multiplier * MoveScorer.foodBonus(
                ctx.myDist(),
                ctx.turn().enemyReach(),
                ctx.turn().enemies(),
                ctx.turn().food(),
                ctx.turn().foodWeight(),
                ctx.trapped(),
                ctx.canReachTail(),
                ctx.owned(),
                ctx.turn().state().you().length(),
                ctx.turn().state().you().health(),
                ctx.turn().grid()
        );
    }
}
