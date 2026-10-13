package io.github.piscescup.fabricmc.commands;

import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.calculation.FunctionDefinition;
import io.github.piscescup.fabricmc.commands.argument.ExpressionArgument;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Readable, localized chat help with navigation and editable examples. */
final class HelpFeedback {
    private static final String PREFIX = "commands.calc.help.";
    private static final Pattern SYNTAX_TOKEN = Pattern.compile(
        "<[^>]+>|--[a-z-]+|[\\[\\]]|/calc(?:ulate)?(?:\\s+[a-z-]+)?"
            + "|\\b(?:enable|disable|default|ascending|descending|both|left|right|integer|double|all|complex|power-order|scope|format|template|add|remove|show|pi|e|I|ans)\\b"
            + "|[a-z][a-z0-9]*(?=\\()");
    private static final Set<String> CONSTANTS = Set.of("pi", "e", "I", "ans");

    private HelpFeedback() {}

    static Component overview() {
        MutableComponent message = title(Component.translatable(PREFIX + "header"));
        group(message, "calculation", List.of("expression"));
        group(message, "symbolic", List.of("solve", "expand", "factor", "simplify", "limit", "diff", "integrate"));
        group(message, "settings", List.of("numfmt", "config"));
        return message.append("\n\n  ").append(Component.translatable(PREFIX + "footer")
            .withStyle(ChatFormatting.GRAY));
    }

    static Component detail(String command, List<String> functions) {
        MutableComponent message = title(coloredSyntax("/calc " + command, ChatFormatting.AQUA));
        message.append("\n\n  ").append(Component.translatable(PREFIX + "description_label")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        message.append("\n    ").append(Component.translatable(PREFIX + "detail." + command + ".description")
            .withStyle(ChatFormatting.GRAY));

        section(message, "syntax_label");
        message.append("\n    ").append(coloredSyntax(syntax(command).replace(" [", "\n      ["), ChatFormatting.WHITE));
        message.append("\n    ").append(translatedSyntax("legend", ChatFormatting.DARK_GRAY));

        section(message, "param_label");
        message.append("\n    ").append(translatedSyntax("detail." + command + ".params", ChatFormatting.GRAY));

        section(message, "usage_label");
        for (String example : examples(command)) {
            message.append("\n    ").append(coloredSyntax(example, ChatFormatting.GREEN).withStyle(ChatFormatting.GREEN)
                .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(example))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable(PREFIX + "insert_example")))));
        }

        if (command.equals("expression")) {
            section(message, "functions_label");
            functions(message, functions);
        }

        List<String> notes = switch (command) {
            case "expression" -> List.of("functions", "numeric_functions");
            case "solve", "diff", "integrate", "limit" -> List.of("notes." + command);
            case "numfmt" -> List.of("numfmt.scope", "numfmt.format", "numfmt.template");
            case "config" -> List.of("config.power_order", "config.complex");
            default -> List.of();
        };
        if (!notes.isEmpty()) {
            section(message, "notes_label");
            for (String note : notes) {
                message.append("\n    ").append(translatedSyntax(note, ChatFormatting.GRAY));
            }
        }
        return message.append("\n\n  ").append(Component.translatable(PREFIX + "back")
            .withStyle(ChatFormatting.AQUA)
            .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/calc help"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("/calc help")))));
    }

    private static Component translatedSyntax(String key, ChatFormatting color) {
        return coloredSyntax(Component.translatable(PREFIX + key).getString(), color);
    }

    /** Keep command syntax, required values and optional values visually distinct. */
    private static MutableComponent coloredSyntax(String text, ChatFormatting base) {
        MutableComponent result = Component.empty();
        var tokens = SYNTAX_TOKEN.matcher(text);
        int cursor = 0;
        int optional = 0;
        while (tokens.find()) {
            result.append(Component.literal(text.substring(cursor, tokens.start()))
                .withStyle(optional > 0 ? ChatFormatting.GREEN : base));
            String token = tokens.group();
            ChatFormatting color;
            if (token.startsWith("<")) color = ChatFormatting.YELLOW;
            else if (token.startsWith("--")) color = ChatFormatting.GOLD;
            else if (token.equals("[") || token.equals("]")) color = ChatFormatting.GREEN;
            else if (token.startsWith("/calc") || FunctionDefinition.find(token) != null) color = ChatFormatting.AQUA;
            else if (CONSTANTS.contains(token)) color = ChatFormatting.LIGHT_PURPLE;
            else color = ChatFormatting.GREEN;
            result.append(Component.literal(token).withStyle(color));
            if (token.equals("[")) optional++;
            else if (token.equals("]")) optional = Math.max(0, optional - 1);
            cursor = tokens.end();
        }
        return result.append(Component.literal(text.substring(cursor)).withStyle(optional > 0 ? ChatFormatting.GREEN : base));
    }

    private static void functions(MutableComponent message, List<String> names) {
        List<FunctionDefinition> definitions = names.stream().map(FunctionDefinition::find).toList();
        for (FunctionDefinition.Category category : FunctionDefinition.Category.values()) {
            List<FunctionDefinition> group = definitions.stream().filter(function -> function.category() == category).toList();
            if (group.isEmpty()) continue;
            message.append("\n    ").append(Component.translatable(category.translationKey())
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
            for (int index = 0; index < group.size(); index++) {
                message.append(index % 3 == 0 ? "\n      " : "  ·  ");
                FunctionDefinition function = group.get(index);
                message.append(Component.empty().append(Component.literal(function.name()).withStyle(ChatFormatting.AQUA))
                    .append(coloredSyntax("(" + function.parameters() + ")", ChatFormatting.YELLOW))
                    .withStyle(style -> style.withUnderlined(true)
                        .withClickEvent(new ClickEvent.SuggestCommand("/calc expression " + function.insertion()))
                        .withHoverEvent(new HoverEvent.ShowText(ExpressionArgument.functionTooltip(function)))));
            }
        }
    }

    private static MutableComponent title(Component subtitle) {
        return Component.empty()
            .append(Component.literal("[Calc Commands]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(" ")
            .append(subtitle.copy().withStyle(ChatFormatting.GRAY))
            .append(Component.literal("\n────────────────────────").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void section(MutableComponent message, String label) {
        message.append("\n\n  ").append(Component.translatable(PREFIX + label)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static void group(MutableComponent message, String group, List<String> commands) {
        section(message, "group." + group);
        for (String command : commands) {
            String help = "/calc help " + command;
            message.append("\n    ").append(Component.literal("/calc " + command).withStyle(ChatFormatting.AQUA)
                .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(help))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable(PREFIX + "open_command", help)))))
                .append(Component.literal("  —  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(PREFIX + "command." + command).withStyle(ChatFormatting.WHITE));
        }
    }

    private static String syntax(String command) {
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            if (operation.getValue().equals(command)) return operation.syntax();
        }
        return switch (command) {
            case "numfmt" -> "/calc numfmt [enable|disable|scope|format|template|default]";
            case "config" -> "/calc config [complex|power-order|default]";
            default -> throw new IllegalArgumentException("Unknown help command: " + command);
        };
    }

    static List<String> examples(String command) {
        return switch (command) {
            case "expression" -> List.of("/calc expression 64*27", "/calc expression sqrt(2)", "/calc expression n(pi,50)");
            case "solve" -> List.of("/calc solve x^2-5*x+6=0 x", "/calc solve 2*x+y=10;x+y=7 x,y");
            case "expand" -> List.of("/calc expand (x+1)^3");
            case "factor" -> List.of("/calc factor x^2-1");
            case "simplify" -> List.of("/calc simplify sin(x)^2+cos(x)^2");
            case "limit" -> List.of("/calc limit sin(x)/x x 0", "/calc limit abs(x)/x x 0 left", "/calc limit 1/x x inf");
            case "diff" -> List.of("/calc diff x^3 x", "/calc diff x^2*y^3 x,y", "/calc diff x^2*y x --at x=2,y=3");
            case "integrate" -> List.of("/calc integrate x^2 x", "/calc integrate sin(x) x --internal 0 pi");
            case "numfmt" -> List.of("/calc numfmt enable", "/calc numfmt template stack-item --stack 64", "/calc numfmt format all");
            case "config" -> List.of("/calc config complex enable", "/calc config power-order descending");
            default -> throw new IllegalArgumentException("Unknown help command: " + command);
        };
    }
}
