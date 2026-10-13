package io.github.piscescup.fabricmc;

import io.github.piscescup.fabricmc.config.Options;
import org.junit.jupiter.api.Test;
import org.matheclipse.core.expression.BigFractionSym;
import org.matheclipse.core.expression.IntegerSym;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IInteger;
import org.matheclipse.core.interfaces.IRational;

import java.util.Arrays;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class Test1 {
    @Test
    public void test() {
        IRational iInteger = BigFractionSym.valueOf(132343242, 54);

        System.out.println(Arrays.toString(Options.TemplateOption.SB_STACK_ITEM_TEMPLATE.format(iInteger, 64)));
        System.out.println(Arrays.toString(Options.TemplateOption.SB_STACK_ITEM_TEMPLATE.format(iInteger, 16)));
        System.out.println(Arrays.toString(Options.TemplateOption.SB_STACK_ITEM_TEMPLATE.format(iInteger, 1)));
    }
}
