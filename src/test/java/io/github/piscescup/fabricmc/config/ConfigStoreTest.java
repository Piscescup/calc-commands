package io.github.piscescup.fabricmc.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigStoreTest {
    @TempDir
    Path directory;

    @Test
    public void invalidFieldDoesNotDiscardValidFields() throws Exception {
        Path path = directory.resolve("settings.json");
        Files.writeString(path, "{\"enabled\":true,\"stack\":0}");
        ConfigStore store = new ConfigStore(path).register("enabled", new BooleanConfig(false))
            .register("stack", new IntConfig(64, 1));
        store.load();
        assertEquals(Map.of("enabled", "true", "stack", "64"), store.snapshot());
    }

    @Test
    public void validationFailurePublishesAndWritesNothing() {
        Path path = directory.resolve("settings.json");
        ConfigStore store = new ConfigStore(path).register("enabled", new BooleanConfig(false))
            .register("stack", new IntConfig(64, 1));
        assertThrows(IllegalArgumentException.class,
            () -> store.update(Map.of("enabled", "true", "stack", "0")));
        assertEquals(Map.of("enabled", "false", "stack", "64"), store.snapshot());
        assertFalse(Files.exists(path));
    }

    @Test
    public void failedSavePreservesMemoryAndCleansTemporaryFile() throws Exception {
        Path target = directory.resolve("settings.json");
        Files.createDirectory(target);
        Files.writeString(target.resolve("keep.txt"), "existing data");
        ConfigStore store = new ConfigStore(target).register("enabled", new BooleanConfig(false));
        assertThrows(IOException.class, () -> store.set("enabled", "true"));
        assertEquals("false", store.get("enabled"));
        assertEquals("existing data", Files.readString(target.resolve("keep.txt")));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
