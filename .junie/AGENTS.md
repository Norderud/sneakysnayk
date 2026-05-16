# super-sneyk Development Guide

This document provides essential information for advanced developers working on the `super-sneyk` BattleSnake bot.

## Build & Configuration

The project is a Java 21 application built with Maven.

### Key Build Commands
- `mvn package`: Compiles the project, runs tests, and produces two artifacts in the `target/` directory:
  - `super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar`: The executable fat JAR.
  - `battlesnake.zip`: A deployment archive (assembled via `src/main/assembly/zip.xml`).

### Runtime Configuration
The entry point is `sneak.snaek.FierceBattleSnakeApplication`. It accepts optional command-line arguments to support multi-instance launches (useful for local testing):
```bash
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar [port] [name] [color]
```
Example:
```bash
java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar 8081 "Snake-B" "#00aaff"
```

### Infrastructure Notes
- **Port**: Defaults to 8080.
- **Server**: Uses raw `com.sun.net.httpserver.HttpServer`.
- **JSON**: Handled via Gson.

## Testing Information

The project uses JUnit 5 for unit testing. 

### Adding New Tests
1. JUnit 5 dependencies are already present in `pom.xml`.
2. Create test classes in `src/test/java/`.

### Example Test
The following test demonstrates how to verify the `ModularSnakeEngine` avoids a wall collision:

```java
package sneak.snaek.engine;

import org.junit.jupiter.api.Test;
import sneak.snaek.model.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModularSnakeEngineTest {
    @Test
    public void testAvoidsWall() {
        // Snake at (0,5), facing LEFT (wall at x=-1)
        Coord head = new Coord(0, 5);
        List<Coord> body = List.of(head, new Coord(1, 5), new Coord(2, 5));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);
        
        Board board = new Board(11, 11, List.of(), List.of(you), List.of());
        Game game = new Game("test", null, 500);
        GameState state = new GameState(game, 1, board, you);

        ModularSnakeEngine engine = ModularSnakeEngine.createDefault();
        Move move = engine.move(state);

        // LEFT is OOB, RIGHT is body collision. Only UP or DOWN are safe.
        assertTrue(move == Move.UP || move == Move.DOWN);
    }
}
```

### Running Tests
- `mvn test`: Runs all unit tests.
- `mvn test -Dtest=ClassName`: Runs a specific test class.

## Development & Debugging

### Logging
The application uses SLF4J with Logback. Logs are split for better observability:
- **Standard Logs**: Console output for general application flow.
- **Score Logs** (`logs/scores.log`): One-line summary per game outcome (WIN/LOSS/DRAW).
- **Decision Logs** (`logs/decisions.log`): Detailed breakdown of every turn's move scoring. Check this file to understand why a specific move was chosen or filtered.

### Decision Engine
The bot uses a **one-ply greedy** approach. 
- **Filters** (`MoveFilter`): Prune invalid or dangerous moves (collisions, head-to-head losses).
- **Scorers** (`Scorer`): Assign weights to remaining moves based on Survival Area, Food distance, Tail proximity, and Center positioning.

### Common Pitfalls
- **Java Version**: Ensure `pom.xml` uses `<release>21</release>` for the `maven-compiler-plugin` to avoid compatibility issues with the `java.version` property.
- **Tail-Vacate Rule**: The `BoardGrid` treats the last segment of a snake as a free cell *unless* it ate food in the previous turn. This is critical for BFS-based survival calculations.
