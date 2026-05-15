package sneak.snaek.board;

import sneak.snaek.model.*;

import java.util.ArrayList;
import java.util.List;

public final class CoordUtils {

    private CoordUtils() {}

    
    public static Coord neighbor(Coord c, Move move) {
        return switch (move) {
            case UP    -> new Coord(c.x(),     c.y() + 1);
            case DOWN  -> new Coord(c.x(),     c.y() - 1);
            case LEFT  -> new Coord(c.x() - 1, c.y());
            case RIGHT -> new Coord(c.x() + 1, c.y());
        };
    }

    
    public static List<Coord> neighbors(Coord c) {
        List<Coord> result = new ArrayList<>(4);
        for (Move m : Move.values()) {
            result.add(neighbor(c, m));
        }
        return result;
    }

    
    public static int manhattanDistance(Coord a, Coord b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    
    public static Move moveFrom(Coord from, Coord to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (dx ==  1 && dy == 0) return Move.RIGHT;
        if (dx == -1 && dy == 0) return Move.LEFT;
        if (dy ==  1 && dx == 0) return Move.UP;
        if (dy == -1 && dx == 0) return Move.DOWN;
        throw new IllegalArgumentException("Coords are not adjacent: " + from + " → " + to);
    }
}

