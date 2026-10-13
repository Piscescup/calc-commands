package io.github.piscescup.fabricmc.config;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class DisplayConfigTest {
    @TempDir
    Path directory;

    @Test
    public void missingFieldsUseTheirDefaults() throws Exception {
        Path path = directory.resolve("calc_cfg.json");
        ConfigStore store = DisplayConfig.createStore(path);
        assertEquals(Map.of("power_order", "ascending", "complex", "false"), store.read(path));
        Files.writeString(path, "{\"power_order\":\"descending\"}");
        assertEquals(Map.of("power_order", "descending", "complex", "false"), store.read(path));
        Files.writeString(path, "{\"complex\":\"enable\"}");
        assertEquals(Map.of("power_order", "ascending", "complex", "true"), store.read(path));
    }

    @Test
    public void savesComplexAsBooleanAndReloadsBothModes() throws Exception {
        Path path = directory.resolve("nested/calc_cfg.json");
        ConfigStore store = DisplayConfig.createStore(path);
        for (boolean enabled : new boolean[] {true, false}) {
            var settings = Map.of("power_order", "descending", "complex", Boolean.toString(enabled));
            store.save(path, settings);
            assertEquals(settings, store.read(path));
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            assertTrue(json.getAsJsonPrimitive("complex").isBoolean());
            assertEquals(enabled, json.get("complex").getAsBoolean());
        }
    }

}
