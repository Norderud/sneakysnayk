package sneak.snaek;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Move;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonMap;
import static org.slf4j.LoggerFactory.getLogger;

public class FierceBattleSnakeApplication {

    private static final Logger log      = getLogger(FierceBattleSnakeApplication.class);
    /** Dedicated score logger — routed to logs/scores.log via logback.xml. */
    private static final Logger scoreLog = getLogger("score");
    private static final SnakeEngine snakeEngine = new SnakeEngine();
    private static final Gson gson = new Gson();

    // Snake identity – configurable per-instance so two JVMs can run side-by-side
    private final String snakeName;
    private final String snakeColor;

    /** Per-game matchup info captured at /start so /end can write a complete row. */
    private record GameContext(List<String> opponentNames, int initialSnakeCount) {}
    private final ConcurrentHashMap<String, GameContext> games = new ConcurrentHashMap<>();

    //ssh -N -o ServerAliveInterval=60 -R 8091:localhost:8080 -p 2222 battlesnake@devign-snake.dev.mastercard.int

    public FierceBattleSnakeApplication(String snakeName, String snakeColor) {
        this.snakeName  = snakeName;
        this.snakeColor = snakeColor;
    }

    // Usage: java -jar app.jar [port] [name] [color]
    //   java -jar app.jar 8080 "Snake-A" "#ff9900"
    //   java -jar app.jar 8081 "Snake-B" "#00aaff"
    public static void main(String[] args) throws IOException {
        int    port  = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String name  = args.length > 1 ? args[1] : "Sneaksnaek";
        String color = args.length > 2 ? args[2] : "#FFFFFF";

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        FierceBattleSnakeApplication application = new FierceBattleSnakeApplication(name, color);
        server.createContext("/", application::info);
        server.createContext("/start", application::startSignal);
        server.createContext("/move", application::move);
        server.createContext("/end", application::endSignal);
        server.setExecutor(null);
        server.start();
        log.info("Server '{}' started on port {} (color={})", name, port, color);
    }

    public void info(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            log.info("Responding to info request");
            Map<String, String> response = new HashMap<>();
            response.put("apiversion", "1");
            response.put("author", "Sneaksnaek");
            response.put("color", snakeColor);
            response.put("head", "comet");
            response.put("tail", "flame");
            String jsonResponse = gson.toJson(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes());
            os.close();
        }
    }

    public void startSignal(HttpExchange exchange) throws IOException {
        GameState game = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), GameState.class);
        List<String> opponents = game.board().snakes().stream()
                .filter(s -> !s.id().equals(game.you().id()))
                .map(BattleSnake::name)
                .toList();
        games.put(game.game().id(),
                new GameContext(opponents, game.board().snakes().size()));
        log.info("Game started: {} | us={} vs {} ({} snakes)",
                game.game().id(), snakeName, opponents, game.board().snakes().size());
        exchange.sendResponseHeaders(200, -1);
    }

    public void move(HttpExchange exchange) throws IOException {
        GameState game = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), GameState.class);
        Move move = snakeEngine.move(game);
        String jsonResponse = gson.toJson(singletonMap("move", move.name().toLowerCase()));
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(jsonResponse.getBytes());
        os.close();
    }

    public void endSignal(HttpExchange exchange) throws IOException {
        GameState game = gson.fromJson(new InputStreamReader(exchange.getRequestBody()), GameState.class);
        GameContext ctx = games.remove(game.game().id());
        List<String> opponents = ctx != null ? ctx.opponentNames() : List.of();
        int initialCount = ctx != null ? ctx.initialSnakeCount() : game.board().snakes().size();

        // Determine outcome from the final board state.
        //   alive(us) && only snake left → WIN
        //   alive(us) && others alive    → DRAW  (rare: turn-limit / mutual H2H)
        //   !alive(us)                    → LOSS
        String myId = game.you().id();
        List<BattleSnake> survivors = game.board().snakes();
        boolean weSurvived = survivors.stream().anyMatch(s -> s.id().equals(myId));
        String result;
        if (weSurvived && survivors.size() == 1)      result = "WIN";
        else if (weSurvived)                           result = "DRAW";
        else                                           result = "LOSS";

        List<String> winners = survivors.stream().map(BattleSnake::name).toList();
        int finalLength = game.you().length();

        scoreLog.info(
                "result={} gameId={} mode={} turns={} us={} usLength={} opponents={} survivors={}",
                result, game.game().id(),
                initialCount <= 2 ? "DUEL" : (initialCount == 1 ? "SOLO" : "FFA"),
                game.turn(), snakeName, finalLength, opponents, winners);

        log.info("Game {} ended in {} turns | result={} | opponents={}",
                game.game().id(), game.turn(), result, opponents);
        exchange.sendResponseHeaders(200, -1);
    }
}
