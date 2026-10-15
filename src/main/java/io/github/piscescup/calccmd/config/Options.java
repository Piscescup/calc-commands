package io.github.piscescup.calccmd.config;

import java.util.Locale;
import org.matheclipse.core.interfaces.IRational;

/** Values shared by configuration and command presentation. */
public final class Options {
    private Options() {}

    public enum PowerOrder implements Option {
        ASCENDING, DESCENDING;

        @Override
        public String getValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Stable preset identifiers; command quoting is separate from template text. */
    public enum TemplateOption implements Option {
        SB_STACK_ITEM_TEMPLATE("shulker-stack-item", "{} S.B.(s) + {} Stack(s) + {} Item(s)"),
        STACK_ITEM_TEMPLATE("stack-item", "{} Stack(s) + {} Item(s)"),
        ITEM_TEMPLATE("item", "{} Item(s)");

        private final String id;
        private final String template;

        TemplateOption(String id, String template) {
            this.id = id;
            this.template = template;
        }

        @Override
        public String getValue() {
            return id;
        }

        public String template() {
            return template;
        }
    }

    public enum FormatMode implements Option {
        INTEGER, DOUBLE, ALL;

        @Override
        public String getValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean allows(IRational value) {
            return switch (this) {
                case INTEGER -> value.isInteger();
                case DOUBLE -> !value.isInteger();
                case ALL -> true;
            };
        }
    }
}
