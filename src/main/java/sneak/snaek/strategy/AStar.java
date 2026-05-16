package sneak.snaek.strategy;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.Coord;

import java.util.*;

/**
 * A* Pathfinding implementation.
 * 
 * Unlike BFS which computes a distance map to all cells, A* is optimized 
 * for finding the shortest path to a specific target.
 */
public final class AStar {

    private AStar() {}

    /**
     * Finds the shortest path from start to goal.
     * Returns a list of coordinates from start (exclusive) to goal (inclusive),
     * or an empty list if no path exists.
     */
    public static List<Coord> findPath(BoardGrid grid, Coord start, Coord goal) {
        if (start.equals(goal)) return List.of();
        if (!grid.inBounds(goal) || grid.isBlocked(goal)) return List.of();

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Coord, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, CoordUtils.manhattanDistance(start, goal), null);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.coord.equals(goal)) {
                return reconstructPath(current);
            }

            for (Coord neighbor : CoordUtils.neighbors(current.coord)) {
                if (!grid.inBounds(neighbor) || grid.isBlocked(neighbor)) continue;

                int newG = current.g + 1;
                Node neighborNode = allNodes.get(neighbor);

                if (neighborNode == null || newG < neighborNode.g) {
                    int h = CoordUtils.manhattanDistance(neighbor, goal);
                    if (neighborNode == null) {
                        neighborNode = new Node(neighbor, newG, h, current);
                        allNodes.put(neighbor, neighborNode);
                        openSet.add(neighborNode);
                    } else {
                        openSet.remove(neighborNode);
                        neighborNode.g = newG;
                        neighborNode.parent = current;
                        openSet.add(neighborNode);
                    }
                }
            }
        }

        return List.of();
    }

    private static List<Coord> reconstructPath(Node node) {
        List<Coord> path = new ArrayList<>();
        Node curr = node;
        while (curr.parent != null) {
            path.add(0, curr.coord);
            curr = curr.parent;
        }
        return path;
    }

    private static class Node implements Comparable<Node> {
        final Coord coord;
        int g; // cost from start
        int h; // heuristic to goal
        Node parent;

        Node(Coord coord, int g, int h, Node parent) {
            this.coord = coord;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        int f() { return g + h; }

        @Override
        public int compareTo(Node o) {
            int cmp = Integer.compare(this.f(), o.f());
            if (cmp == 0) return Integer.compare(this.h, o.h);
            return cmp;
        }
    }
}
