# Command Reference

[← Back to README](../README.md)

Use `/calc` or `/calculate`. `<value>` means required; `[value]` means optional.
All commands run on your client and need no operator permissions.

Results include the original expression, such as `sin(2 × π) = 0`. Click an underlined equation to copy exactly what is displayed. Solutions to systems stay on separate lines, and each solution row can be copied separately.

**Jump to:** [Expression](#expression) · [Solve](#solve) · [Algebra](#algebra) · [Diff](#diff) · [Limit](#limit) · [Integrate](#integrate) · [Number Formatting](#numfmt) · [Settings](#config)

---

## Expression

Calculate a number, evaluate a function, or reuse your last answer.

```text
/calc expression <expression>
```

| Example                            | Result                               |
|:-----------------------------------|:-------------------------------------|
| `/calc expression 64*27`           | `1728`                               |
| `/calc expression 1/3+1/6`         | `1/2 ≈ 0.5`                          |
| `/calc expression sin(pi/2)`       | `1`                                  |
| `/calc expression log(2,8)`        | `3`                                  |
| `/calc expression ceildiv(130,64)` | `3`                                  |
| `/calc expression n(sqrt(2),50)`   | √2 to 50 significant digits          |
| `/calc expression ans*2`           | Twice the previous successful answer |

### Functions & constants

| Category                   | Functions                                                 |
|:---------------------------|:----------------------------------------------------------|
| Trigonometry               | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2(y,x)` |
| Hyperbolic                 | `sinh`, `cosh`, `tanh`                                    |
| Roots & absolute values    | `sqrt`, `abs`                                             |
| Logarithms                 | `ln(x)`, `lg(x)`, `log2(x)`, `log(base,x)`                |
| Rounding                   | `ceil(x)`, `floor(x)`, `round(x[,digits])`                |
| Integers & stack counts    | `ceildiv(a,b)`, `gcd(a,b,...)`, `lcm(a,b,...)`            |
| Geometry & angles          | `hypot(x,y,...)`, `rad(degrees)`, `deg(radians)`          |
| Collections                | `min`, `max`, `sum`, `avg`                                |
| Approximation & randomness | `n(expression[,digits])`, `random()`                      |

**Constants:** `pi`, `e`, `I` (imaginary unit), `ans` (last successful answer).

- Operators: `+`, `-`, `*`, `/`, `%`, `^`. Write multiplication explicitly.
- Trigonometric functions use radians; `sin(rad(30))` gives `1/2`.
- `round` accepts −1000 to 1000 decimal places. Halfway values round toward positive infinity: `round(-1.5)` gives `-1`.
- `ceildiv`, `gcd` and `lcm` require integer arguments. `ceildiv` rejects a zero divisor.
- `n` accepts 1–1000 significant digits; its default is 30.

## Solve

Solve an equation or a system of equations.

```text
/calc solve <equations> <variables>
```

| Parameter   | Write it as                                                |
|:------------|:-----------------------------------------------------------|
| `equations` | `left=right` or `left==right`; separate equations with `;` |
| `variables` | Comma-separated unknowns, with no spaces: `x,y`            |

```mcfunction
/calc solve x^2-5*x+6=0 x
/calc solve 2*x+y=10;x+y=7 x,y
```

The first gives `x=2` and `x=3`. The system gives `x=3`, `y=4`.
Solutions appear in separate groups, with each variable on its own line.

By default, only real solutions are returned. Enable complex mode for equations such as `x^2+1=0`:

```mcfunction
/calc config complex enable
/calc solve x^2+1=0 x
```

**Result:** `x=-I` and `x=I`.

<a id="algebra"></a>

## Expand, Factor & Simplify

| Action   | Syntax                        | Example             | Result            |
|:---------|:------------------------------|:--------------------|:------------------|
| Expand   | `/calc expand <expression>`   | `(x+1)^3`           | `1+3*x+3*x^2+x^3` |
| Factor   | `/calc factor <expression>`   | `x^2-1`             | `(-1+x)*(1+x)`    |
| Simplify | `/calc simplify <expression>` | `sin(x)^2+cos(x)^2` | `1`               |

```mcfunction
/calc expand (x+1)^3
/calc factor x^2-1
/calc simplify sin(x)^2+cos(x)^2
```

Simplification can cancel factors and remove excluded points from the displayed expression. For example, `(x^2-1)/(x-1)` simplifies to `x+1`, but the original expression is undefined at `x=1`.

## Diff

Find a derivative, a mixed partial derivative, or a derivative at a point.

```text
/calc diff <function> <variables> [--at <variable=value,...>]
```

| Input          | Meaning                                       |
|:---------------|:----------------------------------------------|
| `x`            | Differentiate once with respect to x          |
| `x,x`          | Differentiate twice with respect to x         |
| `x,y`          | Differentiate first with respect to x, then y |
| `--at x=2,y=3` | Substitute both values after differentiating  |

| Example                               | Result    |
|:--------------------------------------|:----------|
| `/calc diff x^3 x`                    | `3*x^2`   |
| `/calc diff x^3 x,x`                  | `6*x`     |
| `/calc diff x^2*y^3 x,y`              | `6*x*y^2` |
| `/calc diff x^2*y x --at x=2,y=3`     | `12`      |
| `/calc diff sin(x) x --at x=(pi / 3)` | `1/2`     |

Symbols that are not differentiated are treated as parameters. You may substitute other parameters, or only some variables. Keep commas free of spaces; wrap values containing spaces in parentheses. Complex substitutions require complex mode.

The result includes the differentiation operation: `∂ (x^3) / ∂x = 3 × x^2` or `∂ (x^3) / ∂x∂x = 6 × x`. This compact chat notation omits the order superscript. Read the denominator from right to left: `∂y∂x` means first x, then y. Point evaluations follow the result: `∂ (x^3) / ∂x = 12 at [x = 2]`.

## Limit

Find a two-sided limit, a one-sided limit, or a limit at infinity.

```text
/calc limit <function> <variable> <point> [both|left|right]
```

| Parameter | Values                                    |
|:----------|:------------------------------------------|
| `point`   | A finite real expression, `inf` or `-inf` |
| Direction | `both` (default), `left`, `right`         |

| Example                          | Result |
|:---------------------------------|:-------|
| `/calc limit sin(x)/x x 0`       | `1`    |
| `/calc limit abs(x)/x x 0 left`  | `-1`   |
| `/calc limit abs(x)/x x 0 right` | `1`    |
| `/calc limit 1/x x inf`          | `0`    |

A two-sided limit fails when the sides disagree. Limit results may be positive or negative infinity.

## Integrate

Find an antiderivative or calculate a definite integral.

```text
/calc integrate <integrand> <variable> [--internal <lower-limit> <upper-limit>]
```

| Example                                          | Result      |
|:-------------------------------------------------|:------------|
| `/calc integrate x^2 x`                          | `x^3/3 + C` |
| `/calc integrate x^2 x --internal 0 3`           | `9`         |
| `/calc integrate sin(x) x --internal 0 pi`       | `2`         |
| `/calc integrate cos(x) x --internal 0 (pi / 2)` | `1`         |

Both bounds are required with `--internal`, and both must be finite real values. Reversed bounds are supported. For an indefinite integral, `ans` stores the displayed antiderivative without the integration constant `C`.

Results show the integral alongside the answer: `∫(x^2) dx = x^3/3 + C` or `∫(sin(x)) dx = 2  [0 -> π]`. The trailing brackets give the lower and upper bounds in order. The integration constant is included when copying an indefinite integral.

## Numfmt

Show non-negative numerical results as inventory quantities.

```text
/calc numfmt
  enable | disable | default
  scope
    show | default
    add <command>
    remove <command>
  format [integer|double|all|default]
  template [<preset-or-template> [--stack <size>]|default]
```

| Setting    | Default                     | Choices                                             |
|:-----------|:----------------------------|:----------------------------------------------------|
| Enabled    | Off                         | `enable`, `disable`                                 |
| Scope      | All five supported commands | `expression`, `solve`, `diff`, `limit`, `integrate` |
| Value type | `integer`                   | `integer`, `double` (non-integers), `all`           |
| Template   | `shulker-stack-item`        | A preset ID or custom text                          |
| Stack size | `64`                        | Positive integer                                    |

```mcfunction
/calc numfmt enable
/calc numfmt scope remove solve
/calc numfmt format all
/calc numfmt template stack-item --stack 16
```

### Templates

| Preset               | Template                                |
|:---------------------|:----------------------------------------|
| `shulker-stack-item` | `{} S.B.(s) + {} Stack(s) + {} Item(s)` |
| `stack-item`         | `{} Stack(s) + {} Item(s)`              |
| `item`               | `{} Item(s)`                            |

Press **Tab** to select a template, or supply your own:

```mcfunction
/calc numfmt template "{} boxes + {} stacks + {} items" --stack 64
```

Custom templates accept 1–3 `{}` placeholders and up to 256 characters:

| Placeholders | Quantities, in order                             |
|:-------------|:-------------------------------------------------|
| 1            | Total items                                      |
| 2            | Stacks, remaining items                          |
| 3            | Shulker boxes, remaining stacks, remaining items |

The stack size is appended automatically. Selecting a template without `--stack` uses 64 items per stack. One shulker box holds 27 stacks.

With the `stack-item` preset and a stack size of 64:

```text
128=2 Stack(s) + 0 Item(s) at Stack = 64
131.1212=2 Stack(s) + 3.1212 Item(s) at Stack = 64
327803/2500=2 Stack(s) + (3+303/2500) Item(s) at Stack = 64
```

Exact fractions remain exact. Negative numbers, complex numbers, symbolic results and infinities keep their normal display; very large or small numbers may keep scientific notation.

Omit a value to query the setting. Use `default` to restore that setting, or `/calc numfmt default` to restore all number-format settings. Resetting the template also restores the stack size to 64.

## Config

Change calculation and display preferences.

```text
/calc config
  complex [enable|disable|default]
  power-order [ascending|descending|default]
  default
```

| Setting       | Default     | Effect                                                                                   |
|:--------------|:------------|:-----------------------------------------------------------------------------------------|
| `complex`     | Disabled    | Allows complex numerical results, complex solutions and complex derivative substitutions |
| `power-order` | `ascending` | Reverses the displayed order of addition terms when set to `descending`                  |

```mcfunction
/calc config complex enable
/calc config power-order descending
/calc config complex
/calc config default
```

Omitting the value shows the current setting. `default` resets one setting; `/calc config default` resets both. These preferences do not rewrite the stored `ans`. Integral bounds and finite limit points remain real in complex mode.

## Help

```mcfunction
/calc help
/calc help expression
/calc help integrate
```

The overview groups calculation, symbolic math and settings. Click a command to open its guide; click a function or example to fill the chat input. Functions are grouped with their full signatures; hover over one for its description and an example. Each guide includes usage, colored parameters, examples and notes specific to that command.

---

[← Back to README](../README.md)
