package io.github.piscescup.fabricmc.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TemplateConfigTest {
    @Test
    public void presetsAndCustomTemplatesRoundTrip() {
        TemplateConfig config = new TemplateConfig();
        for (Options.TemplateOption preset : Options.TemplateOption.values()) {
            assertEquals(preset.template(), config.parse(preset.getValue()));
            assertEquals(preset.getValue(), config.serialize(preset.template()));
        }
        assertEquals("{}盒 + {}组 + {}件", config.parse("{}盒 + {}组 + {}件"));
    }

    @Test
    public void rejectsInvalidOrUnboundedTemplates() {
        TemplateConfig config = new TemplateConfig();
        for (String invalid : new String[] {"text", "{x}", "{", "}", "{}{}{}{}", "x".repeat(256) + "{}"}) {
            assertThrows(IllegalArgumentException.class, () -> config.parse(invalid), invalid);
        }
    }
}
