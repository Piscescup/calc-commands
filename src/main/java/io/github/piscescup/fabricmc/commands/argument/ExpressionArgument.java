package io.github.piscescup.fabricmc.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.calculation.CalcCommandsException;
import io.github.piscescup.fabricmc.calculation.ExpressionValidator;
import io.github.piscescup.fabricmc.calculation.CalcRequest;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A terminal command argument: invalid syntax becomes a native Minecraft input error.
 */
public final class ExpressionArgument implements ArgumentType<CalcRequest> {
    private final CalculatorEngine engine;
    private final CalcRequest.Operation operation;

    public ExpressionArgument(CalculatorEngine engine, CalcRequest.Operation operation) {
        this.engine = engine;
        this.operation = operation;
    }

    @Override
    public CalcRequest parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        // Check before substring allocation, including when this argument is called outside chat.
        if (reader.getRemainingLength() > CalculatorEngine.MAX_LENGTH) {
            reader.setCursor(start + CalculatorEngine.MAX_LENGTH);
            CalcCommandsException error = CalcCommandsException.at(
                "too_long", CalculatorEngine.MAX_LENGTH, CalculatorEngine.MAX_LENGTH);
            throw new SimpleCommandExceptionType(message(error)).createWithContext(reader);
        }
        try {
            CalcRequest request = engine.validate(operation, reader.getRemaining());
            reader.setCursor(reader.getTotalLength());
            return request;
        } catch (CalcCommandsException error) {
            reader.setCursor(Math.min(reader.getTotalLength(), start + error.offset()));
            throw new SimpleCommandExceptionType(message(error)).createWithContext(reader);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (builder.getRemaining()
            .length() > CalculatorEngine.MAX_LENGTH) {
            return builder.buildFuture();
        }
        ExpressionValidator.Completion completion = engine.complete(operation, builder.getRemaining());
        SuggestionsBuilder replacement = builder.createOffset(builder.getStart() + completion.offset());
        for (String candidate : completion.candidates()) replacement.suggest(candidate);
        return replacement.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return switch (operation) {
            case EXPRESSION -> List.of("1+2*3", "sin(pi/2)", "2^(-3)", "ans*2");
            case SOLVE -> List.of("x^2-4=0 x", "2*x+y=10; x+y=7 x,y");
            case EXPAND -> List.of("(x+1)^3");
            case FACTOR -> List.of("x^2-1");
            case SIMPLIFY -> List.of("(x^2-1)/(x-1)", "sin(x)^2+cos(x)^2");
            case LIMIT -> List.of("sin(x)/x x 0", "1/x x inf", "abs(x)/x x 0 left");
            case DIFF -> List.of("sin(x) x", "x^2*y x,y", "x^2*y x --at x=2,y=3");
            case INTEGRATE -> List.of("x^2 x", "x^2 x --internal 0 3", "sin(x) x --internal 0 pi");
        };
    }

    public static Component message(CalcCommandsException error) {
        return Component.translatable("commands.calc.error." + error.key(), error.arguments());
    }
}
