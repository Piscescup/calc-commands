package io.github.piscescup.fabricmc.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DisplayConfigTest {
    @TempDir
    Path directory;

    @Test
    public void missingFieldsUseTheirDefaults() throws Exception {
        Path path = directory.resolve("calc_cfg.json");
        assertEquals("descending", DisplayConfig.read(path).get(DisplayConfig.POWER_ORDER_KEY));
        assertEquals("disable", DisplayConfig.read(path).get(DisplayConfig.COMPLEX_KEY));
        Files.writeString(path, "{\"power_order\":\"descending\"}");
        assertEquals("descending", DisplayConfig.read(path).get(DisplayConfig.POWER_ORDER_KEY));
        assertEquals("disable", DisplayConfig.read(path).get(DisplayConfig.COMPLEX_KEY));
        Files.writeString(path, "{\"complex\":\"enable\"}");
        assertEquals("descending", DisplayConfig.read(path).get(DisplayConfig.POWER_ORDER_KEY));
        assertEquals("enable", DisplayConfig.read(path).get(DisplayConfig.COMPLEX_KEY));
    }

    @Test
    public void savesBothOptionsAndReloadsComplexAndRealModes() throws Exception {
        Path path = directory.resolve("nested/calc_cfg.json");
        for (boolean enabled : new boolean[] {true, false}) {
            var settings = Map.of(
                DisplayConfig.POWER_ORDER_KEY, "descending",
                DisplayConfig.COMPLEX_KEY, enabled ? "enable" : "disable");
            DisplayConfig.save(path, settings);
            assertEquals(settings, DisplayConfig.read(path));
            assertFalse(Files.exists(path.resolveSibling("calc_cfg.json.tmp")));
        }
    }
}
