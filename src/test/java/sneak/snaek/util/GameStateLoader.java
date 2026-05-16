package sneak.snaek.util;

import com.google.gson.Gson;
import sneak.snaek.model.GameState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GameStateLoader {
    private static final Gson gson = new Gson();

    public static GameState load(String resourcePath) {
        InputStream is = GameStateLoader.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), GameState.class);
    }

    public static GameState loadFromJson(String json) {
        return gson.fromJson(json, GameState.class);
    }
}
