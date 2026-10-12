package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.piscescup.fabricmc.config.DisplayConfig;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Builds and executes general /calc config presentation settings. */
public final class ConfigCommands {
    private static final SimpleCommandExceptionType SAVE_FAILED = new SimpleCommandExceptionType(
        message("commands.calc.config.error.save_failed", ChatFormatting.RED));

    private ConfigCommands() {}

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        DisplayConfig.initialize();
        build(calcRoot);
    }

    static void build(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        LiteralArgumentBuilder<FabricClientCommandSource> powerOrder = literal("power-order")
            .executes(ConfigCommands::showPowerOrder);
        powerOrder.then(literal("default").executes(ConfigCommands::reset));
        for (DisplayConfig.PowerOrder order : DisplayConfig.PowerOrder.values()) {
            powerOrder.then(literal(order.command()).executes(context -> setPowerOrder(context, order)));
        }
        calcRoot.then(literal("config")
            .executes(ConfigCommands::showSettings)
            .then(literal("default").executes(ConfigCommands::reset))
            .then(powerOrder));
    }

    private static int setPowerOrder(CommandContext<FabricClientCommandSource> context,
            DisplayConfig.PowerOrder order) throws CommandSyntaxException {
        try {
            DisplayConfig.setPowerOrder(order);
        } catch (IOException error) {
            throw SAVE_FAILED.create();
        }
        context.getSource().sendFeedback(message(
            "commands.calc.config.power_order_updated", ChatFormatting.GREEN, order.command()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showSettings(CommandContext<FabricClientCommandSource> context) {
        return showPowerOrder(context);
    }

    private static int showPowerOrder(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(message(
            "commands.calc.config.power_order", ChatFormatting.AQUA,
            DisplayConfig.powerOrder().command()));
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(CommandContext<FabricClientCommandSource> context)
            throws CommandSyntaxException {
        try {
            DisplayConfig.reset();
        } catch (IOException error) {
            throw SAVE_FAILED.create();
        }
        context.getSource().sendFeedback(message(
            "commands.calc.config.reset", ChatFormatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static Component message(String key, ChatFormatting color, Object... arguments) {
        return Component.empty()
            .append(Component.literal("[Calc Commands]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(" ")
            .append(Component.translatable(key, arguments).withStyle(color));
    }
}
