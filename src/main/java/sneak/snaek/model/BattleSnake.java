package sneak.snaek.model;

import java.util.List;

public record BattleSnake(String id,
                          String name,
                          int health,
                          List<Coord> body,
                          String latency,
                          Coord head,
                          int length,
                          String shout,
                          String squad,
                          Customizations customizations) {
}
