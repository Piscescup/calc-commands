package io.github.piscescup.calccmd.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.piscescup.calccmd.config.TemplateConfig;
import io.github.piscescup.calccmd.config.Options;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ConfigurationCommandTreeTest {
    @Test
    public void numFmtTreeContainsEveryActionDefaultAndQueryPath() {
        LiteralArgumentBuilder<FabricClientCommandSource> root = LiteralArgumentBuilder.literal("calc");
        NumberFormatCommands.build(root);
        CommandNode<FabricClientCommandSource> numfmt = required(root.build(), "numfmt");

        assertChildren(numfmt, "reset", "default", "enable", "disable", "scope", "format", "template");
        assertChildren(required(numfmt, "scope"), "show", "default", "add", "remove");
        assertChildren(required(required(numfmt, "scope"), "add"),
            "expression", "integrate", "diff", "limit", "solve");
        assertChildren(required(required(numfmt, "scope"), "remove"),
            "expression", "integrate", "diff", "limit", "solve");
        assertChildren(required(numfmt, "format"), "default", "integer", "double", "all");

        CommandNode<FabricClientCommandSource> template = required(numfmt, "template");
        assertChildren(template, "reset", "default", "template");
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
        ConfigCommands.build(root, java.util.Map.of(
            "power_order", java.util.List.of("ascending", "descending"),
            "complex", java.util.List.of("enable", "disable")));
        CommandNode<FabricClientCommandSource> config = required(root.build(), "config");

        assertChildren(config, "default", "reset", "power-order", "power_order", "complex");
        CommandNode<FabricClientCommandSource> powerOrder = required(config, "power-order");
        assertChildren(powerOrder, "default", "ascending", "descending");
        assertSame(powerOrder, required(config, "power_order").getRedirect());
        assertNotNull(config.getCommand());
        assertNotNull(powerOrder.getCommand());
        CommandNode<FabricClientCommandSource> complex = required(config, "complex");
        assertChildren(complex, "enable", "disable", "default");
        assertNotNull(complex.getCommand());
        for (String value : new String[] {"enable", "disable", "default"}) {
            assertNotNull(required(complex, value).getCommand());
        }
        var dispatcher = new CommandDispatcher<FabricClientCommandSource>();
        dispatcher.register(root);
        for (String input : new String[] {"calc config power-order descending", "calc config power_order ascending",
                "calc config default", "calc config reset", "calc config complex enable"}) {
            var parsed = dispatcher.parse(input, null);
            assertTrue(parsed.getExceptions().isEmpty(), input);
            assertEquals(parsed.getReader().getTotalLength(), parsed.getReader().getCursor(), input);
        }
    }

    @Test
    public void templateSuggestionsSurviveBrigadierQuotingAndValidation() throws Exception {
        var root = LiteralArgumentBuilder.<FabricClientCommandSource>literal("calc");
        NumberFormatCommands.build(root);
        var dispatcher = new CommandDispatcher<FabricClientCommandSource>();
        dispatcher.register(root);
        String input = "calc numfmt template ";
        var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(input, null)).get().getList();
        TemplateConfig template = new TemplateConfig();
        for (Options.TemplateOption preset : Options.TemplateOption.values()) {
            String quoted = StringArgumentType.escapeIfRequired(preset.template());
            assertTrue(suggestions.stream().anyMatch(suggestion -> suggestion.getText().equals(quoted)));
            assertEquals(preset.template(), template.parse(StringArgumentType.string().parse(new StringReader(quoted))));
            var parsed = dispatcher.parse(input + quoted + " --stack 16", null);
            assertTrue(parsed.getExceptions().isEmpty());
            assertEquals(parsed.getReader().getTotalLength(), parsed.getReader().getCursor());
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
