package sneak.snaek.engine.scorer;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.MoveContext;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;

import java.util.List;

/**
 * Scorer for the Duelist personality.
 * Scores moves that land adjacent to an enemy head, seeking combat.
 */
public class DuelistScorer implements Scorer {
    private final double multiplier;

    public DuelistScorer() {
        this(1.0);
    }

    public DuelistScorer(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;

        List<BattleSnake> enemies = ctx.turn().enemies();
        Coord next = ctx.next();
        double totalBonus = 0.0;

        for (BattleSnake enemy : enemies) {
            if (CoordUtils.manhattanDistance(next, enemy.head()) == 1) {
                // Duelist loves H2H. 
                // HeadToHeadFilter will still prune if it's suicide (length < enemy.length).
                // So this bonus effectively encourages:
                // 1. Favorable H2H (we are longer) - redundant with AggressionScorer but reinforces it.
                // 2. Equal H2H (we are same length) - risky but aggressive.
                totalBonus += ScoringConstants.DUELIST_BONUS;
            }
        }

        return multiplier * totalBonus;
    }
    public String name() {
        return getClass().getSimpleName();
    }
}
