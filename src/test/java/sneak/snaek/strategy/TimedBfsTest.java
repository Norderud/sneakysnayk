package sneak.snaek.strategy;

import org.junit.jupiter.api.Test;
import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.VacateMap;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Board;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Game;
import sneak.snaek.model.GameState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimedBfsTest {

    @Test
    public void testEquivalentToPlainBfsWhenAllZero() {
        BoardGrid grid = new BoardGrid(11, 11, Set.of(), Set.of(), Set.of());
        int[][] vacate = new int[11][11];   // all zero
        int[][] timed = TimedBfs.from(grid, vacate, List.of(new Coord(5, 5)), 1);
        int[][] plain = Bfs.from(grid, List.of(new Coord(5, 5)));
        for (int x = 0; x < 11; x++) {
            for (int y = 0; y < 11; y++) {
                assertEquals(plain[x][y], timed[x][y], "mismatch at " + x + "," + y);
            }
        }
    }

    @Test
    public void testPermanentlyBlockedCellStaysUnreachable() {
        BoardGrid grid = new BoardGrid(11, 11, Set.of(), Set.of(), Set.of());
        int[][] vacate = new int[11][11];
        vacate[5][5] = VacateMap.NEVER;
        int[][] timed = TimedBfs.from(grid, vacate, List.of(new Coord(0, 0)), 1);
        assertEquals(Bfs.UNREACHABLE, timed[5][5]);
        // surrounding cells reachable
        assertTrue(timed[4][5] < Bfs.UNREACHABLE);
        assertTrue(timed[6][5] < Bfs.UNREACHABLE);
    }

    @Test
    public void testReachesCellBehindOwnTailGivenLongPath() {
        // Long snake in a U-shape. Our head is at (5,5), body curls down and back.
        // Cell (4,5) is behind our neck (5,4). With plain BFS it's a 4-hop detour
        // through walls — but if the snake is long, the body vacates in time.
        // L=6 snake: head(5,5) - (5,4)-(5,3)-(5,2)-(4,2)-(4,3)-(4,4)
        // Actually let's build: cell (4,5) is reached from `next` = (5,6) only by going
        // around. Body indices: head=0:(5,5), 1:(5,4), 2:(5,3), 3:(5,2), 4:(4,2), 5:(4,3).
        // vacate(4,3) = 1, vacate(4,2)=2, vacate(5,2)=3, vacate(5,3)=4, vacate(5,4)=5, vacate(5,5)=6.
        // BFS from next=(5,6): up to (5,7)→(4,7)→... or down via (5,4)? No, vacate(5,4)=5,
        // arrival at hop 1 from (5,6)→(5,5)? wait next=(5,6) is our move target (head goes up).
        // Let's just check (4,4): vacate=0 (free). Reachable directly: (5,6)→(4,6)→(4,5)→(4,4). 3 hops.
        // Verify (4,3): vacate=1. From (4,4) at hop 3, arrival turn 1+3=4 >= vacate 1 → reachable at hop 4.
        // Plain Bfs would also reach (4,4) but would NOT pass through (4,3) since blocked.
        // Real test: cell (4,2) — currently blocked by enemy body, vacate=2. Reachable at hop 5
        //   via (5,6)→(4,6)→(4,5)→(4,4)→(4,3)[wait vacate=1, arrival turn 5, OK]→(4,2)[vacate=2, turn 6, OK].
        List<Coord> body = List.of(
                new Coord(5, 5), new Coord(5, 4), new Coord(5, 3),
                new Coord(5, 2), new Coord(4, 2), new Coord(4, 3));
        BattleSnake you = new BattleSnake("you", "you", 80, body, "0", body.get(0), body.size(), "", "", null);
        GameState s = new GameState(new Game("g", null, 500),
                1, new Board(11, 11, List.of(), List.of(you), List.of()), you);
        BoardGrid grid = new BoardGrid(s);
        int[][] vacate = VacateMap.from(s);

        // Plain Bfs: (4,2) blocked by body — unreachable.
        int[][] plain = Bfs.from(grid, List.of(new Coord(5, 6)));
        assertEquals(Bfs.UNREACHABLE, plain[4][2], "plain BFS should treat enemy body as wall");

        // Time-aware: reachable via path that arrives after body vacates.
        int[][] timed = TimedBfs.from(grid, vacate, List.of(new Coord(5, 6)), 1);
        assertTrue(timed[4][2] < Bfs.UNREACHABLE, "time-aware BFS should reach cell once body vacates");
    }

    @Test
    public void testRespectsVacateGuard() {
        // A specific cell only reachable from one neighbour; vacate guard delays it.
        BoardGrid grid = new BoardGrid(5, 5, Set.of(), Set.of(), Set.of());
        int[][] vacate = new int[5][5];
        // Box (0,0) in with walls: only neighbour is (1,0).
        vacate[0][1] = VacateMap.NEVER;
        // (0,0) is a tail-segment that vacates at turn 5.
        vacate[0][0] = 5;
        int[][] timed = TimedBfs.from(grid, vacate, List.of(new Coord(2, 0)), 1);
        // (1,0) is hop 1 from source, arrival turn 2.
        assertEquals(1, timed[1][0]);
        // (0,0) is hop 2 from source, arrival turn 3. vacate=5 > 3 → can't enter.
        assertEquals(Bfs.UNREACHABLE, timed[0][0]);
    }

    @Test
    public void testEnemyReachOwnerCorrectUnderDelay() {
        // Two enemies; one's body is between us and a cell.
        // E1 at (0,5), E2 at (10,5). Cell (5,5) is closer to E1 by 1 step.
        // Add no obstacles: standard race.
        BoardGrid grid = new BoardGrid(11, 11, Set.of(), Set.of(), Set.of());
        int[][] vacate = new int[11][11];
        BattleSnake e1 = new BattleSnake("e1", "e1", 80,
                List.of(new Coord(0, 5), new Coord(0, 4), new Coord(0, 3)), "0", new Coord(0, 5), 3, "", "", null);
        BattleSnake e2 = new BattleSnake("e2", "e2", 80,
                List.of(new Coord(10, 5), new Coord(10, 4), new Coord(10, 3)), "0", new Coord(10, 5), 3, "", "", null);
        Bfs.EnemyReach reach = TimedBfs.computeEnemyReach(grid, vacate, List.of(e1, e2));
        // (4,5) closer to e1 (4 hops) than e2 (6 hops) — owned by e1 (index 0)
        assertEquals(4, reach.dist()[4][5]);
        assertEquals(0, reach.owner()[4][5]);
        // (6,5) closer to e2 — owner 1
        assertEquals(4, reach.dist()[6][5]);
        assertEquals(1, reach.owner()[6][5]);
        // tie cell (5,5): e1 wins by FIFO insertion order (e1 added first)
        assertEquals(5, reach.dist()[5][5]);
    }
}
