package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.tree.CommandNode;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelpCommandTreeTest {
    @Test
    public void helpAcceptsEveryDocumentedCommand() {
        CommandNode<FabricClientCommandSource> help = CalcCommands.buildHelpCommand().build();
        Set<String> expected = new HashSet<>();
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            expected.add(operation.getValue());
        }
        expected.add("numfmt");
        expected.add("config");

        Set<String> actual = new HashSet<>();
        help.getChildren().forEach(child -> {
            actual.add(child.getName());
            assertNotNull(child.getCommand(), "Detailed help must be executable: " + child.getName());
        });
        assertEquals(expected, actual);
        assertNotNull(help.getCommand());
    }

    @Test
    public void bothLanguagesDescribeEveryCommandAndNewOptions() throws Exception {
        Set<String> englishKeys = null;
        for (String language : new String[] {"en_us", "zh_cn"}) {
            try (var input = getClass().getResourceAsStream("/assets/calc-commands/lang/" + language + ".json")) {
                assertNotNull(input, language);
                JsonObject translations = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                if (englishKeys == null) englishKeys = Set.copyOf(translations.keySet());
                else assertEquals(englishKeys, translations.keySet());
                for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
                    String command = operation.getValue();
                    for (String detail : new String[] {"description", "params", "usage"}) {
                        assertTrue(translations.has("commands.calc.help.detail." + command + "." + detail), command + "/" + detail);
                    }
                    assertTrue(translations.has("commands.calc.help.command." + command), command);
                    assertTrue(translations.has("commands.calc.feedback." + command), command);
                }
                assertTrue(translations.get("commands.calc.help.detail.diff.params").getAsString().contains("--at"));
                assertTrue(translations.get("commands.calc.help.detail.integrate.params").getAsString().contains("--internal"));
                assertTrue(translations.get("commands.calc.feedback.integration_constant").getAsString().contains("+ C"));
            }
        }
    }
}
