package io.github.piscescup.fabricmc.commands;

import io.github.piscescup.fabricmc.calculation.CalcRequest;
import io.github.piscescup.fabricmc.calculation.CalculatorEngine;
import io.github.piscescup.fabricmc.calculation.FunctionDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HelpFeedbackTest {
    private final Language previousLanguage = Language.getInstance();

    @AfterEach
    public void restoreLanguage() {
        Language.inject(previousLanguage);
    }

    @Test
    public void overviewGroupsAndLinksEveryCommandInBothLanguages() throws Exception {
        for (String locale : List.of("en_us", "zh_cn")) {
            Map<String, String> translations = useLanguage(locale);
            Component message = HelpFeedback.overview();
            String text = message.getString();
            for (String group : List.of("calculation", "symbolic", "settings")) {
                assertTrue(text.contains("\n\n  " + translations.get("commands.calc.help.group." + group)));
            }
            Set<String> targets = new HashSet<>();
            for (Component part : parts(message)) {
                if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand event) {
                    targets.add(event.command());
                    assertNotNull(part.getStyle().getHoverEvent());
                }
            }
            Set<String> expected = new HashSet<>(Set.of("/calc help numfmt", "/calc help config"));
            for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
                expected.add("/calc help " + operation.getValue());
                assertTrue(text.contains("\n    /calc " + operation.getValue()));
            }
            assertEquals(expected, targets);
            assertFalse(text.contains("commands.calc.help."), locale);
        }
    }

    @Test
    public void detailsSeparateSectionsAndSuggestExamplesWithoutRunningThem() throws Exception {
        for (String locale : List.of("en_us", "zh_cn")) {
            Map<String, String> translations = useLanguage(locale);
            for (String command : List.of("expression", "solve", "expand", "factor", "simplify", "diff",
                    "limit", "integrate", "numfmt", "config")) {
                Component message = HelpFeedback.detail(command, new CalculatorEngine().functionNames());
                String text = message.getString();
                for (String label : List.of("syntax_label", "param_label", "usage_label")) {
                    assertTrue(text.contains("\n\n  " + translations.get("commands.calc.help." + label)), command);
                }
                List<String> examples = new ArrayList<>();
                for (Component part : parts(message)) {
                    if (part.getStyle().getClickEvent() instanceof ClickEvent.SuggestCommand event) {
                        if (HelpFeedback.examples(command).contains(event.command())) {
                            examples.add(event.command());
                            assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), part.getStyle().getColor());
                        } else {
                            assertEquals("expression", command);
                            assertTrue(event.command().startsWith("/calc expression "));
                        }
                    } else if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand event) {
                        assertEquals("/calc help", event.command());
                    }
                }
                assertEquals(HelpFeedback.examples(command), examples);
                assertFalse(text.contains("commands.calc.help."), locale + "/" + command);
            }
        }
    }

    @Test
    public void requiredOptionalAndDescriptionStylesAreDistinct() throws Exception {
        useLanguage("en_us");
        List<Component> pieces = parts(HelpFeedback.detail("diff", List.of()));
        assertTrue(pieces.stream().anyMatch(part -> part.getString().equals("<variables>")
            && TextColor.fromLegacyFormat(ChatFormatting.YELLOW).equals(part.getStyle().getColor())));
        assertTrue(pieces.stream().anyMatch(part -> part.getString().equals("[")
            && TextColor.fromLegacyFormat(ChatFormatting.GREEN).equals(part.getStyle().getColor())));
        assertTrue(pieces.stream().anyMatch(part -> part.getString().equals("--at")
            && TextColor.fromLegacyFormat(ChatFormatting.GOLD).equals(part.getStyle().getColor())));
        assertTrue(pieces.stream().anyMatch(part -> part.getString().equals("Description")
            && TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE).equals(part.getStyle().getColor())));
    }

    @Test
    public void everyFunctionHasItsOwnInsertActionAndLocalizedTooltip() throws Exception {
        CalculatorEngine engine = new CalculatorEngine();
        for (String locale : List.of("en_us", "zh_cn")) {
            useLanguage(locale);
            List<Component> pieces = parts(HelpFeedback.detail("expression", engine.functionNames()));
            for (String name : engine.functionNames()) {
                FunctionDefinition function = FunctionDefinition.find(name);
                Component entry = pieces.stream().filter(part -> part.getStyle().getClickEvent()
                    instanceof ClickEvent.SuggestCommand event && event.command().equals("/calc expression " + function.insertion()))
                    .findFirst().orElseThrow();
                assertEquals(function.syntax(), entry.getString());
                assertTrue(entry.getStyle().isUnderlined());
                assertTrue(entry.getStyle().getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowText);
                var tooltip = (net.minecraft.network.chat.HoverEvent.ShowText) entry.getStyle().getHoverEvent();
                assertTrue(tooltip.value().getString().contains(function.example()));
                assertFalse(tooltip.value().getString().contains("commands.calc."), locale + "/" + name);
            }
        }
    }

    @Test
    public void limitHelpDoesNotIncludeDerivativeEquationOrIntegralNotes() throws Exception {
        for (String locale : List.of("en_us", "zh_cn")) {
            useLanguage(locale);
            String text = HelpFeedback.detail("limit", List.of()).getString();
            assertFalse(text.contains("求导") || text.contains("方程") || text.contains("积分"), text);
            assertFalse(text.contains("differentiat") || text.contains("equations") || text.contains("antiderivative"), text);
            assertTrue(text.contains("both") && text.contains("left") && text.contains("right"));
        }
    }

    @Test
    public void headingsHaveDistinctStylesAndParametersKeepTheirIndent() throws Exception {
        useLanguage("en_us");
        Component message = HelpFeedback.detail("diff", List.of());
        assertTrue(message.getString().contains("\n    <variables>"));
        assertTrue(message.getString().contains("\n    [--at"));
        assertTrue(parts(message).stream().anyMatch(part -> part.getString().equals("[Calc Commands]")
            && part.getStyle().isBold()
            && TextColor.fromLegacyFormat(ChatFormatting.AQUA).equals(part.getStyle().getColor())));
        assertTrue(parts(message).stream().anyMatch(part -> part.getString().equals("Usage")
            && part.getStyle().isBold()
            && TextColor.fromLegacyFormat(ChatFormatting.GOLD).equals(part.getStyle().getColor())));
    }

    @Test
    public void calculationExamplesAreAcceptedByTheActualParser() {
        CalculatorEngine engine = new CalculatorEngine();
        for (CalcRequest.Operation operation : CalcRequest.Operation.values()) {
            String prefix = "/calc " + operation.getValue() + " ";
            for (String example : HelpFeedback.examples(operation.getValue())) {
                assertTrue(example.startsWith(prefix));
                assertDoesNotThrow(() -> engine.validate(operation, example.substring(prefix.length())), example);
            }
        }
    }

    private Map<String, String> useLanguage(String locale) throws Exception {
        Map<String, String> translations = new HashMap<>();
        try (var input = getClass().getResourceAsStream("/assets/calc-commands/lang/" + locale + ".json")) {
            assertNotNull(input);
            Language.loadFromJson(input, translations::put);
        }
        Language.inject(new Language() {
            @Override public String getOrDefault(String key, String fallback) { return translations.getOrDefault(key, fallback); }
            @Override public boolean has(String key) { return translations.containsKey(key); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
            }
        });
        return translations;
    }

    private List<Component> parts(Component message) {
        List<Component> parts = new ArrayList<>();
        parts.add(message);
        for (int index = 0; index < parts.size(); index++) parts.addAll(parts.get(index).getSiblings());
        return parts;
    }
}
