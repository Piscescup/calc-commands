package io.github.piscescup.calccmd.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.github.piscescup.calccmd.calculation.CalcRequest;
import io.github.piscescup.calccmd.calculation.CalculatorEngine;
import io.github.piscescup.calccmd.calculation.FunctionDefinition;
import io.github.piscescup.calccmd.commands.argument.ExpressionArgument;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionSuggestionsTest {
    private CommandDispatcher<Object> dispatcher() {
        var dispatcher = new CommandDispatcher<Object>();
        var root = dispatcher.register(LiteralArgumentBuilder.<Object>literal("calculate")
            .then(LiteralArgumentBuilder.<Object>literal("expression")
                .then(RequiredArgumentBuilder.<Object, CalcRequest>argument("input",
                    new ExpressionArgument(new CalculatorEngine(), CalcRequest.Operation.EXPRESSION)))));
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("calc").redirect(root));
        return dispatcher;
    }

    @Test
    public void bothCommandNamesSuggestAllFunctionsWithTooltips() throws Exception {
        var dispatcher = dispatcher();
        for (String prefix : List.of("calc expression ", "calculate expression ")) {
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(prefix, new Object())).get().getList();
            for (String name : new CalculatorEngine().functionNames()) {
                FunctionDefinition function = FunctionDefinition.find(name);
                var suggestion = suggestions.stream().filter(item -> item.getText().equals(function.insertion()))
                    .findFirst().orElseThrow();
                assertNotNull(suggestion.getTooltip(), name);
                assertTrue(suggestion.getTooltip().getString().contains(function.syntax()), name);
                assertEquals(prefix + function.insertion(), suggestion.apply(prefix));
            }
        }
    }

    @Test
    public void nestedAndUppercasePrefixesReplaceOnlyTheFunctionName() throws Exception {
        var dispatcher = dispatcher();
        for (String prefix : List.of("calc expression SQ", "calc expression 1+sq", "calc expression sin(sq")) {
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(prefix, new Object())).get().getList();
            var squareRoot = suggestions.stream().filter(item -> item.getText().equals("sqrt(")).findFirst().orElseThrow();
            assertEquals(prefix.substring(0, prefix.length() - 2) + "sqrt(", squareRoot.apply(prefix));
            assertNotNull(squareRoot.getTooltip());
        }
    }

    @Test
    public void functionExamplesAndZeroArgumentCompletionAreValid() {
        CalculatorEngine engine = new CalculatorEngine();
        for (String name : engine.functionNames()) {
            FunctionDefinition function = FunctionDefinition.find(name);
            assertDoesNotThrow(() -> engine.validate(CalcRequest.Operation.EXPRESSION, function.example()), name);
        }
        assertTrue(engine.complete(CalcRequest.Operation.EXPRESSION, "rand").candidates().contains("random()"));
        assertDoesNotThrow(() -> engine.validate(CalcRequest.Operation.EXPRESSION, "random()"));
    }
}
