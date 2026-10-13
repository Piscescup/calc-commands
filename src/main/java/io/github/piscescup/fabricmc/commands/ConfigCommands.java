package io.github.piscescup.fabricmc.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.piscescup.fabricmc.config.DisplayConfig;
import io.github.piscescup.fabricmc.config.Configurable;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Builds and executes /calc config settings. */
public final class ConfigCommands {
    private static final SimpleCommandExceptionType SAVE_FAILED = new SimpleCommandExceptionType(
        message("commands.calc.config.error.save_failed", ChatFormatting.RED));

    private ConfigCommands() {}

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        DisplayConfig.initialize();
        build(calcRoot);
    }


    static void build(LiteralArgumentBuilder<FabricClientCommandSource> calcRoot) {
        LiteralArgumentBuilder<FabricClientCommandSource> config = literal("config")
            .executes(ConfigCommands::showSettings)
            .then(literal("reset")
                .executes(ConfigCommands::reset)
            );

        for (String key : DisplayConfig.keys()) {
            LiteralArgumentBuilder<FabricClientCommandSource> setting = literal(key)
                .executes(
                    context -> show(context, key)
                )
                .then(literal("default").executes(context -> update(context, key, "default")));

            for (String value : DisplayConfig.optionsOf(key)) {
                setting.then(
                    literal(value)
                        .executes(context ->
                            update(context, key, value)
                        )
                );
            }
            config.then(setting);
        }
        calcRoot.then(config);
    }

    private static int update(CommandContext<FabricClientCommandSource> context, String key, String value)
            throws CommandSyntaxException {
        Configurable<?> setting = DisplayConfig.configOf(key);
        try {
            if (value.equals("default")) DisplayConfig.reset(key);
            else DisplayConfig.set(key, value);
        } catch (IOException error) {
            throw SAVE_FAILED.create();
        }
        context.getSource().sendFeedback(message(
            "commands.calc.config." + key + "_updated", ChatFormatting.GREEN, setting.stringValue()));
        return Command.SINGLE_SUCCESS;
    }

    private static int showSettings(CommandContext<FabricClientCommandSource> context) {
        for (String key : DisplayConfig.keys()) show(context, key);
        return Command.SINGLE_SUCCESS;
    }

    private static int show(CommandContext<FabricClientCommandSource> context, String key) {
        context.getSource().sendFeedback(message(
            "commands.calc.config." + key, ChatFormatting.AQUA, DisplayConfig.get(key)));
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
