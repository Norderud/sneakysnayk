package sneak.snaek.board;

import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.GameState;

import java.util.List;

/**
 * Per-cell map of "earliest absolute turn from now when this cell becomes free".
 *
 * <p>Free cells are 0. A cell occupied by snake body segment index {@code i}
 * (0 = head, L-1 = tail) of a snake with length {@code L} vacates at
 * absolute turn {@code (L - i) + (justAte ? 1 : 0)} — the snake's tail
 * slithers forward one cell per turn unless it just ate, in which case
 * its growth delays the whole schedule by one.
 *
 * <p>"Just ate" is detected exactly as {@link BoardGrid#markSnakeBody}
 * detects it: stacked tail ({@code body[L-1].equals(body[L-2])}) OR
 * {@code health == 100}.
 *
 * <p>Head cells get {@code L} from the formula. They are NOT marked
 * permanently blocked — by turn L the snake has slithered through and the
 * cell is genuinely free. Head danger (an enemy moving INTO our path) is
 * orthogonal and handled by {@code HeadToHeadFilter}.
 *
 * <p>If two snakes overlap (defensive — shouldn't happen on a live board),
 * we keep the larger vacate value.
 */
public final class VacateMap {

    /** Sentinel for cells that never vacate within the search horizon. Aligns with {@link sneak.snaek.strategy.Bfs#UNREACHABLE}. */
    public static final int NEVER = Integer.MAX_VALUE;

    private VacateMap() {}

    public static int[][] from(GameState state) {
        int w = state.board().width();
        int h = state.board().height();
        int[][] vacate = new int[w][h];   // 0 by default (free)

        for (BattleSnake snake : state.board().snakes()) {
            List<Coord> body = snake.body();
            if (body == null || body.isEmpty()) continue;
            int L = body.size();
            boolean justAte = (L >= 2 && body.get(L - 1).equals(body.get(L - 2)))
                    || snake.health() == 100;
            int growth = justAte ? 1 : 0;
            for (int i = 0; i < L; i++) {
                Coord c = body.get(i);
                if (c.x() < 0 || c.x() >= w || c.y() < 0 || c.y() >= h) continue;
                int turn = L - i + growth;
                if (turn > vacate[c.x()][c.y()]) {
                    vacate[c.x()][c.y()] = turn;
                }
            }
        }
        return vacate;
    }
}
