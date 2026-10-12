package io.github.piscescup.fabricmc.commands;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.calculation.CalcCommandsException;
import io.github.piscescup.fabricmc.calculation.CalcRequest;

import java.util.function.Supplier;

import io.github.piscescup.fabricmc.commands.argument.ExpressionArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Client-only registration and execution; input parsing never changes the last answer.
 */
public final class CalcCommands {
    private static final CalculatorEngine ENGINE = new CalculatorEngine();
    private static CalculatorEngine.Result answer;
    // Serialize calculations and ans updates; keep expensive symbolic work off the render thread.
    private static final ThreadPoolExecutor CALCULATIONS = new ThreadPoolExecutor(
        1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), task -> {
            Thread thread = new Thread(task, "calc-commands-calculator");
            thread.setDaemon(true);
            return thread;
        }
    );

    private CalcCommands() {
    }

    /**
     * Called once by CLIENT_STARTED; initialization runs on the same worker as calculations.
     */
    public static void warmUp(Minecraft client) {
        CALCULATIONS.execute(() -> {
            long started = System.nanoTime();
            try {
                ENGINE.warmUp();
                References.MOD_LOGGER.debug(
                    "Calculator initialized in {} ms", (System.nanoTime() - started) / 1_000_000);
            } catch (RuntimeException error) {
                // Normal execution can retry initialization; do not change ans or report chat output.
                References.MOD_LOGGER.warn("Calculator background initialization failed", error);
            }
        });
    }

    public static void register(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandBuildContext context
    ) {
        var root = literal("calculate").then(buildHelpCommand());
        for (var operation : CalcRequest.Operation.values()) {
            root.then(literal(operation.command())
                .then(argument("input", new ExpressionArgument(ENGINE, operation)).executes(CalcCommands::execute)));
        }
        ConfigCommands.register(root);
        NumberFormetCommands.register(root);

        LiteralCommandNode<FabricClientCommandSource> registerNode = dispatcher.register(root);
        dispatcher.register(
            literal("calc")
                .requires(root.getRequirement())
                .redirect(registerNode)
        );
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
        CalcRequest request = context.getArgument("input", CalcRequest.class);
        return submit(
            context, () -> ENGINE.evaluate(request, answer),
            result -> CalculationFeedback.format(request, result)
        );
    }

    private static int submit(
        CommandContext<FabricClientCommandSource> context,
        Supplier<CalculatorEngine.Result> calculation,
        java.util.function.Function<CalculatorEngine.Result, Component> feedback
    )
        throws CommandSyntaxException
    {
        FabricClientCommandSource source = context.getSource();
        try {
            CALCULATIONS.execute(() -> {
                try {
                    CalculatorEngine.Result result = calculation.get();
                    answer = result;
                    Component message = feedback.apply(result);
                    Minecraft.getInstance()
                        .execute(() -> source.sendFeedback(message));
                } catch (CalcCommandsException error) {
                    Component message = CalculationFeedback.error(ExpressionArgument.message(error));
                    Minecraft.getInstance()
                        .execute(() -> source.sendError(message));
                } catch (RuntimeException error) {
                    References.MOD_LOGGER.error("Unexpected calculator failure", error);
                    Component message = CalculationFeedback.error(
                        ExpressionArgument.message(CalcCommandsException.error("evaluation_failed")));
                    Minecraft.getInstance()
                        .execute(() -> source.sendError(message));
                }
            });
        } catch (RejectedExecutionException error) {
            throw new SimpleCommandExceptionType(
                ExpressionArgument.message(CalcCommandsException.error("calculation_busy"))).create();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int help(CommandContext<FabricClientCommandSource> context) {
        context.getSource()
            .sendFeedback(helpMessage("commands.calc.help.header"));
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            sendCommandHelp(context, operation.command());
        }
        sendCommandHelp(context, "numfmt");
        sendCommandHelp(context, "config");
        context.getSource()
            .sendFeedback(helpMessage("commands.calc.help.footer"));
        return Command.SINGLE_SUCCESS;
    }

    static LiteralArgumentBuilder<FabricClientCommandSource> buildHelpCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> help = literal("help").executes(CalcCommands::help);
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            help.then(literal(operation.command())
                .executes(context -> detailedHelp(context, operation.command())));
        }
        help.then(literal("numfmt").executes(context -> detailedHelp(context, "numfmt")));
        help.then(literal("config").executes(context -> detailedHelp(context, "config")));
        return help;
    }

    private static int detailedHelp(CommandContext<FabricClientCommandSource> context, String command) {
        MutableComponent message = Component.empty()
            .append(Component.literal("[Calc Commands]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(": /calc help " + command)
                .withStyle(ChatFormatting.WHITE));
        helpDetail(message, "description_label", "detail." + command + ".description");
        message.append("\n- ")
            .append(Component.translatable("commands.calc.help.param_label")
                .withStyle(ChatFormatting.GRAY))
            .append("\n       ")
            .append(Component.translatable("commands.calc.help.detail." + command + ".params"));
        helpDetail(message, "usage_label", "detail." + command + ".usage");
        switch (command) {
            case "expression" -> helpBullet(
                message, "commands.calc.help.functions",
                String.join(", ", ENGINE.functionNames())
            );
            case "solve", "diff", "integrate" -> helpBullet(
                message, "commands.calc.help.symbolic_variables");
            case "numfmt" -> {
                helpBullet(message, "commands.calc.help.numfmt.scope");
                helpBullet(message, "commands.calc.help.numfmt.format");
                helpBullet(message, "commands.calc.help.numfmt.template");
            }
            case "config" -> helpBullet(message, "commands.calc.help.config.power_order");
            default -> {
            }
        }
        context.getSource()
            .sendFeedback(message);
        return Command.SINGLE_SUCCESS;
    }

    private static void helpDetail(MutableComponent message, String label, String value) {
        message.append("\n- ")
            .append(Component.translatable("commands.calc.help." + label)
                .withStyle(ChatFormatting.GRAY))
            .append(Component.translatable("commands.calc.help." + value));
    }

    private static void helpBullet(MutableComponent message, String key, Object... arguments) {
        message.append("\n- ")
            .append(Component.translatable(key, arguments));
    }

    private static void sendCommandHelp(CommandContext<FabricClientCommandSource> context, String command) {
        context.getSource()
            .sendFeedback(helpMessage("commands.calc.help.command." + command));
    }

    private static Component helpMessage(String key, Object... arguments) {
        return Component.empty()
            .append(Component.literal("[Calc Commands]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(" ")
            .append(Component.translatable(key, arguments));
    }
}
