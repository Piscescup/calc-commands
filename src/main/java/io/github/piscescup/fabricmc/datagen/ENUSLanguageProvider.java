package io.github.piscescup.fabricmc.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public final class ENUSLanguageProvider extends FabricLanguageProvider {
	public ENUSLanguageProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(output, "en_us", registryLookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
		translationBuilder.add("commands.calc.help.syntax", "/calc <expression> | + - * / %% ^ | constants: pi, e, ans (case-insensitive) | angles use radians; 2^-3 and 2^(-3) are supported.");
		translationBuilder.add("commands.calc.help.functions", "Functions: %s. ln(x): natural log; lg(x): base 10; log2(x): base 2; log(base, x): given base. n(x, digits): decimal approximation. ans preserves the last successful result exactly.");
		translationBuilder.add("commands.calc.error.expected_operand", "Expected a number, constant, function or '(' here");
		translationBuilder.add("commands.calc.error.expected_operator", "Expected an operator here; multiplication needs '*'");
		translationBuilder.add("commands.calc.error.expected_open", "Expected '(' after function '%s'");
		translationBuilder.add("commands.calc.error.expected_close", "Expected ')'");
		translationBuilder.add("commands.calc.error.unexpected_close", "Unexpected ')'");
		translationBuilder.add("commands.calc.error.unexpected_comma", "A comma is only allowed between function arguments");
		translationBuilder.add("commands.calc.error.unexpected_character", "Unexpected character '%s'");
		translationBuilder.add("commands.calc.error.unknown_name", "Unknown function or constant '%s'");
		translationBuilder.add("commands.calc.error.invalid_number", "Invalid or out-of-range number '%s'");
		translationBuilder.add("commands.calc.error.expected_exponent", "Expected digits after the scientific-notation exponent");
		translationBuilder.add("commands.calc.error.parenthesize_exponent", "Put a signed exponent in parentheses, for example 2^(-3)");
		translationBuilder.add("commands.calc.error.argument_count", "Function '%s' received %s arguments; expected %s to %s");
		translationBuilder.add("commands.calc.error.too_long", "Expression exceeds %s characters");
		translationBuilder.add("commands.calc.error.too_many_tokens", "Expression exceeds %s tokens");
		translationBuilder.add("commands.calc.error.too_deep", "Expression exceeds %s nested parentheses or functions");
		translationBuilder.add("commands.calc.error.too_many_arguments", "A function may have at most %s arguments");
		translationBuilder.add("commands.calc.error.division_by_zero", "Cannot divide or take a remainder by zero");
		translationBuilder.add("commands.calc.error.function_domain", "Invalid argument for function '%s' in real-number arithmetic");
		translationBuilder.add("commands.calc.error.undefined_result", "The expression has no defined real-number result");
		translationBuilder.add("commands.calc.error.overflow", "The result is outside the supported numeric range");
		translationBuilder.add("commands.calc.error.evaluation_failed", "The expression could not be evaluated; check its arguments and parentheses");
		translationBuilder.add("commands.calc.error.too_complex", "The calculation exceeds the supported complexity or output limits");
		translationBuilder.add("commands.calc.error.calculation_timeout", "The calculation exceeded its time limit");
		translationBuilder.add("commands.calc.error.calculation_busy", "The calculator is busy; please wait for pending calculations");
		translationBuilder.add("commands.calc.error.invalid_precision", "Precision must be an integer from 1 to %s");
		translationBuilder.add("commands.calc.help.symbolic", "Symbolic commands: /calc solve <equations> <variables> | expand <expression> | factor <expression> | diff <function> <variables> | integrate <function> <variable> <from> <to>");
		translationBuilder.add("commands.calc.help.symbolic_variables", "Separate equations with semicolons and variables with commas (no spaces). diff x,y differentiates successively; x,x gives the second derivative. solve uses real solutions. Bounds may be expressions; wrap bounds containing spaces in parentheses.");
		translationBuilder.add("commands.calc.error.symbolic_arguments", "Missing command arguments. Usage: %s");
		translationBuilder.add("commands.calc.error.invalid_variable", "Invalid variable '%s'; use 1-32 letters, digits or underscores, starting with a letter, and avoid function and constant names");
		translationBuilder.add("commands.calc.error.duplicate_variable", "Duplicate solve variable '%s'");
		translationBuilder.add("commands.calc.error.single_variable", "Definite integration requires exactly one integration variable");
		translationBuilder.add("commands.calc.error.expected_equation", "Expected left=right or left==right; separate equations with ';'");
		translationBuilder.add("commands.calc.error.expected_quote", "Expected a closing double quote");
		translationBuilder.add("commands.calc.error.symbolic_limit", "At most %s equations, distinct symbols or differentiation variables are allowed");
		translationBuilder.add("commands.calc.error.invalid_bounds", "Integration bounds must evaluate to finite real numbers");
		translationBuilder.add("commands.calc.error.symbolic_failed", "Symja could not complete this symbolic calculation; try simplifying the expression");
		translationBuilder.add("commands.calc.feedback.expression", "Calculation");
		translationBuilder.add("commands.calc.feedback.solve", "Solve");
		translationBuilder.add("commands.calc.feedback.expand", "Expand");
		translationBuilder.add("commands.calc.feedback.factor", "Factor");
		translationBuilder.add("commands.calc.feedback.diff", "Derivative");
		translationBuilder.add("commands.calc.feedback.integrate", "Definite integral");
		translationBuilder.add("commands.calc.feedback.solution", "Solution %s: ");
		translationBuilder.add("commands.calc.feedback.no_solutions", "No real solutions");
		translationBuilder.add("commands.calc.feedback.all_reals", "All real values (variables are unrestricted)");
		translationBuilder.add("commands.calc.feedback.variables_label", "Variables: ");
		translationBuilder.add("commands.calc.feedback.equations_label", "Equations: ");
		translationBuilder.add("commands.calc.feedback.function_label", "Function: ");
		translationBuilder.add("commands.calc.feedback.expression_label", "Expression: ");
		translationBuilder.add("commands.calc.feedback.from_label", "Lower bound: ");
		translationBuilder.add("commands.calc.feedback.to_label", "Upper bound: ");
		translationBuilder.add("commands.calc.feedback.derivative_order_label", "Differentiation order: ");
		translationBuilder.add("commands.calc.feedback.solutions_label", "Solutions:");
		translationBuilder.add("commands.calc.feedback.result_label", "Result: ");
		translationBuilder.add("commands.calc.feedback.derivative_label", "Derivative: ");
		translationBuilder.add("commands.calc.feedback.integral_label", "Integral: ");
		translationBuilder.add("commands.calc.feedback.approximate_label", "Approximation: ");
	}
}
