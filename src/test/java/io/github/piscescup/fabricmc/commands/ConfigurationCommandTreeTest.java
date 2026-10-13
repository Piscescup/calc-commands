package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigurationCommandTreeTest {
    @Test
    public void numFmtTreeContainsEveryActionDefaultAndQueryPath() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = LiteralArgumentBuilder.literal("calc");
        NumberFormetCommands.build(root);
        CommandNode<FabricClientCommandSource> numfmt = required(root.build(), "numfmt");

        assertChildren(numfmt, "default", "enable", "disable", "scope", "format", "template");
        assertChildren(required(numfmt, "scope"), "show", "default", "add", "remove");
        assertChildren(required(required(numfmt, "scope"), "add"),
            "expression", "integrate", "diff", "limit", "solve");
        assertChildren(required(required(numfmt, "scope"), "remove"),
            "expression", "integrate", "diff", "limit", "solve");
        assertChildren(required(numfmt, "format"), "default", "integer", "double", "all");

        CommandNode<FabricClientCommandSource> template = required(numfmt, "template");
        assertChildren(template, "default", "template");
        CommandNode<FabricClientCommandSource> templateArgument = required(template, "template");
        assertChildren(templateArgument, "--stack");
        assertChildren(required(templateArgument, "--stack"), "stack");

        assertNotNull(numfmt.getCommand());
        assertNotNull(required(numfmt, "scope").getCommand());
        assertNotNull(required(numfmt, "format").getCommand());
        assertNotNull(template.getCommand());
    }

    @Test
    public void generalConfigTreeContainsQuerySetAndDefaultPaths() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = LiteralArgumentBuilder.literal("calc");
        ConfigCommands.build(root);
        CommandNode<FabricClientCommandSource> config = required(root.build(), "config");

        assertChildren(config, "reset", "power_order", "complex");
        CommandNode<FabricClientCommandSource> powerOrder = required(config, "power_order");
        assertChildren(powerOrder, "default", "ascending", "descending");
        assertNotNull(config.getCommand());
        assertNotNull(powerOrder.getCommand());
        CommandNode<FabricClientCommandSource> complex = required(config, "complex");
        assertChildren(complex, "enable", "disable", "default");
        assertNotNull(complex.getCommand());
        for (String value : new String[] {"enable", "disable", "default"}) {
            assertNotNull(required(complex, value).getCommand());
        }
    }

    private static <S> CommandNode<S> required(CommandNode<S> parent, String name) {
        CommandNode<S> child = parent.getChild(name);
        assertNotNull(child, "Missing command node " + name + " under " + parent.getName());
        return child;
    }

    private static <S> void assertChildren(CommandNode<S> parent, String... expected) {
        Set<String> names = parent.getChildren().stream()
            .map(CommandNode::getName)
            .collect(Collectors.toSet());
        assertEquals(Set.copyOf(Arrays.asList(expected)), names);
    }
}
