package io.github.piscescup.fabricmc.config;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.AMDGPUShaderHalfFloat;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IInteger;
import org.matheclipse.core.interfaces.IRational;
import org.matheclipse.core.reflection.system.N;

import java.util.Locale;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class Options {
    private Options() {
    }

    public enum PowerOrder implements Option {
        ASCENDING,
        DESCENDING,
        DEFAULT;

        public static final StringRepresentable.EnumCodec<PowerOrder> CODEC =
            StringRepresentable.fromEnum(PowerOrder::values);

        public static PowerOrder fromValue(String value) {
            return CODEC.byName(value);
        }


        @Override
        public String getValue() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum AbleOption implements Option {
        ENABLE,
        DISABLE,
        DEFAULT;

        public static final StringRepresentable.EnumCodec<AbleOption> CODEC =
            StringRepresentable.fromEnum(AbleOption::values);

        @Override
        public String getValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static AbleOption fromValue(String value) {
            return CODEC.byName(value);
        }
    }

    public enum OperationOption implements Option {
        EXPRESSION("expression"),
        SOLVE("solve"),
        EXPAND("expand"),
        FACTOR("factor"),
        SIMPLIFY("simplify"),
        LIMIT("limit"),
        DIFF("diff"),
        INTEGRATE("integrate");

        public static final StringRepresentable.EnumCodec<OperationOption> CODEC =
            StringRepresentable.fromEnum(OperationOption::values);

        private final String value;

        public static OperationOption fromValue(String value) {
            return CODEC.byName(value);
        }

        OperationOption(String value) {
            this.value = value;
        }

        @NonNull
        @Override
        public String getValue() {
            return value;
        }
    }

    public enum TemplateOption implements Option {
        SB_STACK_ITEM_TEMPLATE("\"{} S.B.(s) + {} Stack(s) + {} Item(s) at Stack = {} \""),
        STACK_ITEM_TEMPLATE("\"{} Stack(s) + {} Item(s) at Stack = {} \""),
        ITEM_TEMPLATE("\"{} Item(s) at Stack = {} \""),
        ;
        public static final StringRepresentable.EnumCodec<TemplateOption> CODEC =
            StringRepresentable.fromEnum(TemplateOption::values);

        public static TemplateOption fromValue(String value) {
            return CODEC.byName(value);
        }

        private final String template;

        public static final int SHULKER_BOK_STACK = 27;

        TemplateOption(String template) {
            this.template = template;
        }

        @Override
        public String getValue() {
            return template;
        }

        public Object[] format(IRational num, int stackSize) {
            if (stackSize < 1) throw new IllegalArgumentException("Stack size must be positive");
            if (num.isNegative()) throw new IllegalArgumentException("Item count must be non-negative");

            IRational fractionalPart = num.fractionalPart();

            IInteger[] stacks = num.floor()
                .divideAndRemainder(F.ZZ(stackSize));

            IRational items = fractionalPart.add(stacks[1])
                .normalize();

            return switch (this) {
                case ITEM_TEMPLATE -> new Object[]{ num.normalize(), stackSize };
                case STACK_ITEM_TEMPLATE -> new Object[]{stacks[0], items, stackSize};
                case SB_STACK_ITEM_TEMPLATE -> {
                    IInteger[] boxes = stacks[0].divideAndRemainder(F.ZZ(SHULKER_BOK_STACK));
                    yield new Object[]{boxes[0], boxes[1], items, stackSize};
                }
            };
        }
    }

    public enum FormatMode implements Option {
        INTEGER,
        DOUBLE,
        ALL;

        public static final StringRepresentable.EnumCodec<FormatMode> CODEC =
            StringRepresentable.fromEnum(FormatMode::values);


        @NonNull
        @Override
        public String getValue() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean allows(IRational num) {
            return switch (this) {
                case INTEGER -> num.isInteger();
                case DOUBLE -> !num.isInteger();
                case ALL -> true;
            };
        }

        public static FormatMode fromValue(String command) {
            return CODEC.byName(command);
        }


    }
}
