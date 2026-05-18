package sneak.snaek.engine.filter;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.Personality;
import sneak.snaek.engine.TurnContext;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Move;

import java.util.HashSet;
import java.util.Set;

/**
 * Filter that prunes moves where an enemy snake could move to the same square
 * and we would lose the head-to-head collision (enemy is equal or longer).
 */
public class HeadToHeadFilter implements MoveFilter {
    @Override
    public void filter(TurnContext ctx, Set<Move> moves) {
        int myLength = ctx.state().you().length();
        Coord myHead = ctx.state().you().head();
        
        int[] enemyLengths = ctx.enemyReach().enemyLengths();
        // Normally prune if enemy is equal or longer.
        // Duelist only prunes if enemy is strictly longer (allows equal H2H).
        boolean isDuelist = ctx.personality() == Personality.DUELIST;
        
        Set<Coord> danger = new HashSet<>();
        for (int i = 0; i < ctx.enemies().size(); i++) {
            int enemyLen = enemyLengths[i];
            boolean dangerous = isDuelist ? (enemyLen > myLength) : (enemyLen >= myLength);
            
            if (dangerous) {
                BattleSnake enemy = ctx.enemies().get(i);
                for (Move m : Move.values()) {
                    Coord neighbor = CoordUtils.neighbor(enemy.head(), m);
                    if (ctx.grid().inBounds(neighbor) && !ctx.grid().isBlocked(neighbor)) {
                        danger.add(neighbor);
                    }
                }
            }
        }
        
        Set<Move> filtered = new HashSet<>(moves);
        filtered.removeIf(m -> danger.contains(CoordUtils.neighbor(myHead, m)));
        
        // Only apply if it doesn't leave us with zero moves
        if (!filtered.isEmpty()) {
            moves.retainAll(filtered);
        }
    }
}
