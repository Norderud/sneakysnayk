package sneak.snaek.engine.scorer;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.MoveContext;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;

import java.util.List;

/**
 * Scorer for the Parasite personality.
 * Scores moves that land adjacent to an enemy's tail.
 */
public class ParasiteScorer implements Scorer {
    private final double multiplier;

    public ParasiteScorer() {
        this(1.0);
    }

    public ParasiteScorer(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double score(MoveContext ctx) {
        if (ctx.trapped()) return 0.0;

        List<BattleSnake> enemies = ctx.turn().enemies();
        Coord next = ctx.next();
        double totalBonus = 0.0;

        for (BattleSnake enemy : enemies) {
            // Shadow snakes that are similar size or larger
            if (enemy.length() >= ctx.turn().state().you().length() - 2) {
                Coord enemyTail = enemy.body().getLast();
                if (CoordUtils.manhattanDistance(next, enemyTail) == 1) {
                    totalBonus += ScoringConstants.PARASITE_BONUS;
                }
            }
        }

        return multiplier * totalBonus;
    }
}
