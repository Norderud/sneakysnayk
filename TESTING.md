# Local Testing Guide

This guide describes how to test your BattleSnake bot locally, reducing the need to publish to Replit for every change.

## 1. Snapshot Testing (Unit Tests)

If you find an interesting or problematic turn in a game, you can save the JSON request and test it locally.

1.  Capture the JSON request sent to `/move`.
2.  Save it to `src/test/resources/snapshots/your-scenario.json`.
3.  Use `GameStateLoader` in a test:

```java
@Test
void testScenario() {
    GameState state = GameStateLoader.load("/snapshots/your-scenario.json");
    Move move = ModularSnakeEngine.createDefault().move(state);
    assertEquals(Move.UP, move);
}
```

A sample is provided in `src/test/resources/snapshots/sample.json` and `SnapshotTest.java`.

## 2. In-Memory Simulation

You can run a quick, headless simulation to see how the snake behaves over multiple turns without networking overhead.

Run the provided script:
```powershell
.\scripts\run-sim.ps1
```
This runs `sneak.snaek.sim.LocalSimulationRunner`, which you can modify to change the board setup or snake count.

## 3. Official BattleSnake CLI (Best for Local Games)

The project includes the official Battlesnake CLI rules engine in the `rules` directory.

1.  **Build/Update the CLI**:
    If `battlesnake.exe` is missing from the project root, you can build it from source (requires Go):
    ```powershell
    cd rules
    go build -o ../battlesnake.exe ./cli/battlesnake/main.go
    ```
2.  **Start your bot**:
    ```bash
    mvn package
    java -jar target/super-sneyk-1.0-SNAPSHOT-jar-with-dependencies.jar
    ```
3.  **Run a local game**:
    Use the provided script:
    ```powershell
    .\scripts\run-local-cli.ps1 -Browser
    ```
    The script is configured to prioritize the `battlesnake.exe` in the project root.

## 4. Multiple Local Instances

You can test different versions or configurations against each other by launching them on different ports:

```bash
# Terminal 1: Version A
java -jar target/app.jar 8080 "Version-A" "#ff0000"

# Terminal 2: Version B
java -jar target/app.jar 8081 "Version-B" "#0000ff"

# Run CLI against both
battlesnake play --name "A" --url http://localhost:8080 --name "B" --url http://localhost:8081
```

## 5. Decision Logs

Always check `logs/decisions.log` after a move to see the scores assigned to each candidate. This is the best way to understand *why* a move was chosen.
