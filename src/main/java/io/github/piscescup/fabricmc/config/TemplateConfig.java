package io.github.piscescup.fabricmc.config;

/** A validated quantity template, stored as a preset ID or custom text. */
public final class TemplateConfig extends AbstractConfig<String> {
    public static final int MAX_LENGTH = 256;

    public TemplateConfig() {
        super(Options.TemplateOption.SB_STACK_ITEM_TEMPLATE.template());
    }

    @Override
    public String parse(String value) {
        String template = value.strip();
        for (Options.TemplateOption preset : Options.TemplateOption.values()) {
            if (preset.getValue().equals(template)) return preset.template();
        }
        validate(template);
        return template;
    }

    @Override
    public String serialize(String value) {
        for (Options.TemplateOption preset : Options.TemplateOption.values()) {
            if (preset.template().equals(value)) return preset.getValue();
        }
        return value;
    }

    @Override
    public void validate(String value) {
        if (value == null || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Template exceeds " + MAX_LENGTH + " characters");
        }
        int placeholders = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '{' && index + 1 < value.length() && value.charAt(index + 1) == '}') {
                placeholders++;
                index++;
            } else if (current == '{' || current == '}') {
                throw new IllegalArgumentException("Only paired empty placeholders {} are supported");
            }
        }
        if (placeholders < 1 || placeholders > 3) {
            throw new IllegalArgumentException("Expected one to three quantity placeholders");
        }
    }
}
