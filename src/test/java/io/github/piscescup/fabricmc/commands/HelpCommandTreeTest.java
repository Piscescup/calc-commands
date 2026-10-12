package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.tree.CommandNode;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HelpCommandTreeTest {
    @Test
    public void helpAcceptsEveryDocumentedCommand() {
        CommandNode<FabricClientCommandSource> help = CalcCommands.buildHelpCommand().build();
        Set<String> expected = new HashSet<>();
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            expected.add(operation.command());
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
}
