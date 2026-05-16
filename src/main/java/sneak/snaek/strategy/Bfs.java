package sneak.snaek.strategy;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Breadth-first search utilities used throughout move scoring.
 *
 * Two flavours:
 *   - {@link #from(BoardGrid, List)} — plain BFS distance grid from one or
 *     more sources (used for our distance from a candidate cell).
 *   - {@link #computeEnemyReach(BoardGrid, List)} — multi-source BFS from
 *     every enemy head that also tracks which enemy "owns" each cell
 *     (first arrival wins). Owner indices index into the {@code enemies}
 *     list so callers can compare lengths for tied-distance contests.
 *
 * All methods treat blocked cells (per {@link BoardGrid#isBlocked}) as
 * walls and respect the tail-vacate rule encoded in {@code BoardGrid}.
 */
public final class Bfs {

    /** Sentinel BFS distance for "no path" / out-of-bounds. */
    public static final int UNREACHABLE = Integer.MAX_VALUE;

    /** Per-cell BFS result from all enemy heads at once. */
    public record EnemyReach(int[][] dist, int[][] owner, int[] enemyLengths) {}

    private Bfs() {}

    /** Plain BFS distance grid from one or more sources. */
    public static int[][] from(BoardGrid grid, List<Coord> sources) {
        int w = grid.getWidth(), h = grid.getHeight();
        int[][] dist = new int[w][h];
        for (int[] row : dist) Arrays.fill(row, UNREACHABLE);

        Deque<Coord> queue = new ArrayDeque<>();
        for (Coord src : sources) {
            if (!grid.inBounds(src)) continue;
            if (dist[src.x()][src.y()] == UNREACHABLE) {
                dist[src.x()][src.y()] = 0;
                queue.add(src);
            }
        }
        while (!queue.isEmpty()) {
            Coord c = queue.poll();
            int cx = c.x(), cy = c.y();
            int d = dist[cx][cy];

            // Inline neighbors
            for (int i = 0; i < 4; i++) {
                int nx = cx + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int ny = cy + (i == 2 ? 1 : i == 3 ? -1 : 0);

                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (grid.isBlocked(nx, ny)) continue;
                if (dist[nx][ny] > d + 1) {
                    dist[nx][ny] = d + 1;
                    queue.add(new Coord(nx, ny));
                }
            }
        }
        return dist;
    }

    /** Multi-source BFS from every enemy head, recording first-arrival owner. */
    public static EnemyReach computeEnemyReach(BoardGrid grid,
                                               List<BattleSnake> enemies) {
        int w = grid.getWidth(), h = grid.getHeight();
        int[][] dist  = new int[w][h];
        int[][] owner = new int[w][h];
        for (int[] row : dist)  Arrays.fill(row, UNREACHABLE);
        for (int[] row : owner) Arrays.fill(row, -1);

        int[] enemyLengths = new int[enemies.size()];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < enemies.size(); i++) {
            BattleSnake enemy = enemies.get(i);
            enemyLengths[i] = enemy.length();
            Coord src = enemy.head();
            if (!grid.inBounds(src)) continue;
            if (dist[src.x()][src.y()] == UNREACHABLE) {
                dist[src.x()][src.y()]  = 0;
                owner[src.x()][src.y()] = i;
                queue.add(new int[]{src.x(), src.y(), i});
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], oi = cur[2];
            int d  = dist[cx][cy];

            // Inline neighbors
            for (int i = 0; i < 4; i++) {
                int nx = cx + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int ny = cy + (i == 2 ? 1 : i == 3 ? -1 : 0);

                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                if (grid.isBlocked(nx, ny)) continue;
                if (dist[nx][ny] > d + 1) {
                    dist[nx][ny]  = d + 1;
                    owner[nx][ny] = oi;
                    queue.add(new int[]{nx, ny, oi});
                }
            }
        }
        return new EnemyReach(dist, owner, enemyLengths);
    }

    /** Bounds-checked lookup that returns {@link #UNREACHABLE} for OOB. */
    public static int at(int[][] dist, Coord c) {
        if (c.x() < 0 || c.y() < 0 || c.x() >= dist.length || c.y() >= dist[0].length) {
            return UNREACHABLE;
        }
        return dist[c.x()][c.y()];
    }
}

