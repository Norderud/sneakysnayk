package sneak.snaek.strategy;

import org.junit.jupiter.api.Test;
import sneak.snaek.board.VacateMap;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Board;
import sneak.snaek.model.Coord;
import sneak.snaek.model.Game;
import sneak.snaek.model.GameState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VacateMapTest {

    @Test
    public void testFreeCellsZero() {
        GameState s = singleSnake(List.of(new Coord(5, 5), new Coord(5, 4), new Coord(5, 3)), 80);
        int[][] v = VacateMap.from(s);
        assertEquals(0, v[0][0]);
        assertEquals(0, v[10][10]);
        assertEquals(0, v[2][7]);
    }

    @Test
    public void testTailVacatesTurnOne() {
        // length 3, not just-ate. tail = (5,3) at index 2. vacate = L-i = 3-2 = 1.
        GameState s = singleSnake(List.of(new Coord(5, 5), new Coord(5, 4), new Coord(5, 3)), 80);
        int[][] v = VacateMap.from(s);
        assertEquals(1, v[5][3]);
    }

    @Test
    public void testInteriorSegmentVacateEqualsDistanceFromTail() {
        // length 5. body[i] vacate = 5 - i.
        List<Coord> body = List.of(
                new Coord(5, 5), new Coord(5, 4), new Coord(5, 3),
                new Coord(5, 2), new Coord(5, 1));
        GameState s = singleSnake(body, 80);
        int[][] v = VacateMap.from(s);
        assertEquals(5, v[5][5]);  // head
        assertEquals(4, v[5][4]);
        assertEquals(3, v[5][3]);
        assertEquals(2, v[5][2]);
        assertEquals(1, v[5][1]);  // tail
    }

    @Test
    public void testJustAteDelaysByOneViaHealth100() {
        // Just-ate via health=100: every body segment's vacate delayed by 1.
        List<Coord> body = List.of(new Coord(5, 5), new Coord(5, 4), new Coord(5, 3));
        GameState s = singleSnake(body, 100);
        int[][] v = VacateMap.from(s);
        assertEquals(4, v[5][5]);  // head: L + 1 = 4
        assertEquals(3, v[5][4]);
        assertEquals(2, v[5][3]);  // tail: was 1, now 2
    }

    @Test
    public void testJustAteDelaysByOneViaStackedTail() {
        // Stacked tail: body[L-1] == body[L-2]. Pre-spawn / just-ate state.
        // L=3, tail (5,4) duplicated.
        List<Coord> body = List.of(new Coord(5, 5), new Coord(5, 4), new Coord(5, 4));
        GameState s = singleSnake(body, 80);
        int[][] v = VacateMap.from(s);
        // tail cell vacates at max(L - 1 + 1, L - 2 + 1) = max(3, 2) = 3
        assertEquals(3, v[5][4]);
        assertEquals(4, v[5][5]); // head: L - 0 + 1 = 4
    }

    @Test
    public void testNonNegativeForOutOfBoundsBodyIgnored() {
        // Defensive: out-of-bounds body coords should be silently ignored, not crash.
        List<Coord> body = List.of(new Coord(0, 0), new Coord(-1, 0), new Coord(-2, 0));
        GameState s = singleSnake(body, 80);
        int[][] v = VacateMap.from(s);
        assertEquals(3, v[0][0]);
    }

    private static GameState singleSnake(List<Coord> body, int health) {
        BattleSnake you = new BattleSnake("you", "you", health, body, "0", body.get(0), body.size(), "", "", null);
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("g", null, 500);
        return new GameState(game, 1, board, you);
    }
}
