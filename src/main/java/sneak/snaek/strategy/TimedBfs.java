package sneak.snaek.strategy;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.VacateMap;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Time-aware BFS: respects per-cell vacate times so a flood can reach
 * cells that are currently behind another snake's body, provided we'd
 * arrive after the body has slithered out of the way.
 *
 * <p>Returns "hops from source" identical to {@link Bfs#from(BoardGrid, List)}
 * so it's a drop-in replacement.  Internally the guard is:
 * <pre>
 *   arrival_turn(n) = (hops_from_source(n)) + sourceArrivalTurn
 *   enter n only if arrival_turn(n) &gt;= vacate[n]
 * </pre>
 *
 * <p>Known minor approximation: a cell whose only neighbour is reached
 * too early may be marked unreachable even though a longer detour
 * through other cells would arrive in time. Standard BFS doesn't
 * re-enqueue nodes, and modelling "snake waits at u" is structurally
 * unsupported (snakes always move). In practice this case is rare on
 * an 11×11 board with multi-neighbour topology, and the under-count is
 * conservative — we never claim a cell is reachable when it isn't.
 *
 * <p>Reuses {@link Bfs#UNREACHABLE} and {@link Bfs.EnemyReach} so
 * callers can swap between {@code Bfs} and {@code TimedBfs} without
 * type changes.
 */
public final class TimedBfs {

    private TimedBfs() {}

    /**
     * @param sourceArrivalTurn absolute turn (from now) at which we are
     *                          at each source cell.  Use 1 for "our BFS
     *                          from {@code next}" (we'll be there after
     *                          our move) and 0 for "enemy BFS from
     *                          enemy head" (they are there now).
     */
    public static int[][] from(BoardGrid grid,
                               int[][] vacate,
                               List<Coord> sources,
                               int sourceArrivalTurn) {
        int w = grid.getWidth(), h = grid.getHeight();
        int[][] dist = new int[w][h];
        for (int[] row : dist) Arrays.fill(row, Bfs.UNREACHABLE);

        Deque<int[]> queue = new ArrayDeque<>();
        for (Coord src : sources) {
            if (!grid.inBounds(src)) continue;
            int v = vacate[src.x()][src.y()];
            if (v >= VacateMap.NEVER) continue;
            if (sourceArrivalTurn < v) continue;
            if (dist[src.x()][src.y()] == Bfs.UNREACHABLE) {
                dist[src.x()][src.y()] = 0;
                queue.add(new int[]{src.x(), src.y()});
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];
            int d = dist[cx][cy];
            for (int i = 0; i < 4; i++) {
                int nx = cx + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int ny = cy + (i == 2 ? 1 : i == 3 ? -1 : 0);
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                int v = vacate[nx][ny];
                if (v >= VacateMap.NEVER) continue;
                int newDist = d + 1;
                int arrival = newDist + sourceArrivalTurn;
                if (arrival < v) continue;                 // not vacated yet
                if (newDist < dist[nx][ny]) {
                    dist[nx][ny] = newDist;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return dist;
    }

    /**
     * Multi-source timed BFS from every enemy head, recording first-arrival
     * owner per cell.  Sources arrive at turn 0 (enemies are already there).
     * <p>The source's own head vacate value is bypassed (the snake is
     * <em>at</em> its head right now); downstream expansion respects
     * vacate normally.
     */
    public static Bfs.EnemyReach computeEnemyReach(BoardGrid grid,
                                                   int[][] vacate,
                                                   List<BattleSnake> enemies) {
        int w = grid.getWidth(), h = grid.getHeight();
        int[][] dist  = new int[w][h];
        int[][] owner = new int[w][h];
        for (int[] row : dist)  Arrays.fill(row, Bfs.UNREACHABLE);
        for (int[] row : owner) Arrays.fill(row, -1);

        int[] enemyLengths = new int[enemies.size()];
        Deque<int[]> queue = new ArrayDeque<>();
        for (int i = 0; i < enemies.size(); i++) {
            BattleSnake e = enemies.get(i);
            enemyLengths[i] = e.length();
            Coord src = e.head();
            if (!grid.inBounds(src)) continue;
            if (dist[src.x()][src.y()] == Bfs.UNREACHABLE) {
                dist[src.x()][src.y()]  = 0;
                owner[src.x()][src.y()] = i;
                queue.add(new int[]{src.x(), src.y(), i});
            }
        }
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], oi = cur[2];
            int d = dist[cx][cy];
            for (int i = 0; i < 4; i++) {
                int nx = cx + (i == 0 ? 1 : i == 1 ? -1 : 0);
                int ny = cy + (i == 2 ? 1 : i == 3 ? -1 : 0);
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                int v = vacate[nx][ny];
                if (v >= VacateMap.NEVER) continue;
                int newDist = d + 1;
                if (newDist < v) continue;                 // enemy source arrives at turn 0
                if (newDist < dist[nx][ny]) {
                    dist[nx][ny]  = newDist;
                    owner[nx][ny] = oi;
                    queue.add(new int[]{nx, ny, oi});
                }
            }
        }
        return new Bfs.EnemyReach(dist, owner, enemyLengths);
    }
}
