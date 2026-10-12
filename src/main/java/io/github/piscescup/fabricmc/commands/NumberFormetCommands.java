package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.config.NumFormatConfig;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Builds and executes the /calc numfmt configuration branch. */
public final class NumberFormetCommands {
    private static final SimpleCommandExceptionType INVALID_TEMPLATE = new SimpleCommandExceptionType(
        message("commands.calc.numfmt.error.invalid_template", ChatFormatting.RED));
    private static final SimpleCommandExceptionType SAVE_FAILED = new SimpleCommandExceptionType(
        message("commands.calc.numfmt.error.save_failed", ChatFormatting.RED));

    private NumberFormetCommands() {}

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        NumFormatConfig.initialize();
        build(calcRoot);
    }

    static void build(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        LiteralArgumentBuilder<FabricClientCommandSource> numfmt = literal("numfmt")
            .executes(NumberFormetCommands::showSettings);
        numfmt.then(literal("default").executes(NumberFormetCommands::resetAll));
        numfmt.then(literal("enable").executes(context -> setEnabled(context, true)));
        numfmt.then(literal("disable").executes(context -> setEnabled(context, false)));

        LiteralArgumentBuilder<FabricClientCommandSource> scope = literal("scope")
            .executes(NumberFormetCommands::showScope);
        scope.then(literal("show").executes(NumberFormetCommands::showScope));
        scope.then(literal("default").executes(NumberFormetCommands::resetScope));
        scope.then(scopeChange("add", true));
        scope.then(scopeChange("remove", false));
        numfmt.then(scope);

        LiteralArgumentBuilder<FabricClientCommandSource> format = literal("format")
            .executes(NumberFormetCommands::showFormatMode);
        format.then(literal("default").executes(NumberFormetCommands::resetFormatMode));
        for (NumFormatConfig.FormatMode mode : NumFormatConfig.FormatMode.values()) {
            format.then(literal(mode.command()).executes(context -> setFormatMode(context, mode)));
        }
        numfmt.then(format);

        numfmt.then(literal("template")
            .executes(NumberFormetCommands::showTemplate)
            .then(literal("default").executes(NumberFormetCommands::resetTemplate))
            .then(argument("template", StringArgumentType.string())
                .suggests((context, builder) -> {
                        NumFormatConfig.SUGGESTION_TEMPLATES.forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                .executes(context -> setTemplate(context, NumFormatConfig.DEFAULT_STACK_SIZE))
                .then(literal("--stack")
                    .then(argument("stack", IntegerArgumentType.integer(1))
                        .executes(context -> setTemplate(
                            context, IntegerArgumentType.getInteger(context, "stack")))))));
        calcRoot.then(numfmt);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> scopeChange(String command, boolean add) {
        LiteralArgumentBuilder<FabricClientCommandSource> branch = literal(command);
        for (CalcRequest.Operation operation : List.of(
                CalcRequest.Operation.EXPRESSION,
                CalcRequest.Operation.INTEGRATE,
                CalcRequest.Operation.SOLVE)
        ) {
            branch.then(literal(operation.command()).executes(context -> changeScope(context, operation, add)));
        }
        return branch;
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> context, boolean enabled)
            throws CommandSyntaxException {
        save(() -> NumFormatConfig.setEnabled(enabled));
        context.getSource().sendFeedback(message(
            enabled ? "commands.calc.numfmt.enabled" : "commands.calc.numfmt.disabled",
            enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    private static int changeScope(CommandContext<FabricClientCommandSource> context,
            CalcRequest.Operation operation, boolean add) throws CommandSyntaxException {
        save(() -> NumFormatConfig.setScope(operation, add));
        context.getSource().sendFeedback(message(
            add ? "commands.calc.numfmt.scope_added" : "commands.calc.numfmt.scope_removed",
            ChatFormatting.GREEN, operation.command()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showScope(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(message(
            "commands.calc.numfmt.scope", ChatFormatting.AQUA, scopeText()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showSettings(CommandContext<FabricClientCommandSource> context) {
        String scope = scopeText();
        context.getSource().sendFeedback(message("commands.calc.numfmt.settings", ChatFormatting.AQUA,
            NumFormatConfig.enabled(), scope, NumFormatConfig.formatMode().command(),
            NumFormatConfig.template(), NumFormatConfig.stackSize()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showFormatMode(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(message("commands.calc.numfmt.format", ChatFormatting.AQUA,
            NumFormatConfig.formatMode().command()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showTemplate(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(message("commands.calc.numfmt.template", ChatFormatting.AQUA,
            NumFormatConfig.template(), NumFormatConfig.stackSize()));
        return Command.SINGLE_SUCCESS;
    }

    private static int resetAll(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        save(NumFormatConfig::reset);
        return resetFeedback(context, "all");
    }

    private static int resetScope(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        save(NumFormatConfig::resetScope);
        return resetFeedback(context, "scope");
    }

    private static int resetFormatMode(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        save(NumFormatConfig::resetFormatMode);
        return resetFeedback(context, "format");
    }

    private static int resetTemplate(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        save(NumFormatConfig::resetTemplate);
        return resetFeedback(context, "template");
    }

    private static int resetFeedback(CommandContext<FabricClientCommandSource> context, String setting) {
        context.getSource().sendFeedback(message(
            "commands.calc.numfmt.reset", ChatFormatting.GREEN, setting));
        return Command.SINGLE_SUCCESS;
    }

    private static String scopeText() {
        String scope = NumFormatConfig.scope().stream()
            .map(CalcRequest.Operation::command).sorted().collect(Collectors.joining(", "));
        return scope.isEmpty() ? "-" : scope;
    }

    private static int setFormatMode(CommandContext<FabricClientCommandSource> context,
            NumFormatConfig.FormatMode mode) throws CommandSyntaxException {
        save(() -> NumFormatConfig.setFormatMode(mode));
        context.getSource().sendFeedback(message(
            "commands.calc.numfmt.format_updated", ChatFormatting.GREEN, mode.command()));
        return Command.SINGLE_SUCCESS;
    }

    private static int setTemplate(CommandContext<FabricClientCommandSource> context, int stackSize)
            throws CommandSyntaxException {
        String template = StringArgumentType.getString(context, "template");
        try {
            NumFormatConfig.setTemplate(template, stackSize);
        } catch (IllegalArgumentException error) {
            throw INVALID_TEMPLATE.create();
        } catch (IOException error) {
            throw SAVE_FAILED.create();
        }
        context.getSource().sendFeedback(message(
            "commands.calc.numfmt.template_updated", ChatFormatting.GREEN, template, stackSize));
        return Command.SINGLE_SUCCESS;
    }

    private static Component message(String key, ChatFormatting color, Object... arguments) {
        return Component.empty()
            .append(Component.literal("[Calc Commands]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(" ")
            .append(Component.translatable(key, arguments).withStyle(color));
    }

    private static void save(IOSave action) throws CommandSyntaxException {
        try {
            action.run();
        } catch (IOException error) {
            throw SAVE_FAILED.create();
        }
    }

    @FunctionalInterface
    private interface IOSave {
        void run() throws IOException;
    }

    /** A quoted string argument with live brace validation and built-in template suggestions. */
    private static final class TemplateArgument implements ArgumentType<String> {
        private static final StringArgumentType STRING = StringArgumentType.string();

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String template = STRING.parse(reader);
            try {
                NumFormatConfig.validateTemplate(template);
                return template;
            } catch (IllegalArgumentException error) {
                reader.setCursor(start);
                throw INVALID_TEMPLATE.createWithContext(reader);
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(
                CommandContext<S> context, SuggestionsBuilder builder) {
            String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
            for (String template : NumFormatConfig.SUGGESTION_TEMPLATES) {
                String suggestion = StringArgumentType.escapeIfRequired(template);
                if (suggestion.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(suggestion);
                }
            }
            return builder.buildFuture();
        }

        @Override
        public Collection<String> getExamples() {
            return NumFormatConfig.SUGGESTION_TEMPLATES;
        }
    }
}
