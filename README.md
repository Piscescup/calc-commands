# Calc Commands

适用于 Minecraft 26.3 的 Fabric 客户端计算器，使用 Symja 3.2.0 进行符号计算和高精度数值计算。

```text
/calc expression 1+2*3
/calc expression sin(pi/2)
/calc expression sqrt(3^2+4^2)
/calc expression 2^(-3)
/calc expression log(2,8)
/calc expression sin(100*pi)
/calc expression 1/3+1/6
/calc expression sqrt(8)
/calc expression n(sqrt(2),30)
/calc expression ans*2
/calc help [命令]
```

- 输入时校验语法，`sin(`、`1+`、缺失括号和错误参数数量会交给 Minecraft 原生标红并提示。
- Tab 补全函数、`pi`、`e`、`I`、`ans`，支持大小写不敏感的名称和科学计数法。
- 按回车后才在后台线程调用 Symja 计算；输入校验和补全不改变 `ans`。
- `ans` 初始为 0，精确保留本次游戏运行中最后一次成功结果；计算失败不覆盖答案。
- 三角函数使用弧度。乘法必须写 `*`。支持 `2^-3` 和 `2^(-3)`。
- 支持三角函数、`sqrt`、`atan2(y, x)`、`sum`、`avg` 等；`ln(x)` 为自然对数，`lg(x)` 为常用对数，`log2(x)` 以 2 为底，`log(base, x)` 指定底数。`/calc help expression` 列出全部函数。
- `/calc help <command>` 会显示该命令的介绍、逐项参数说明、用法示例及相关补充规则。
- 整数、分数和有限小数字面量精确保留，π 和 e 保留为符号；例如 `sin(100*pi) = 0`，`0.1+0.2 = 3/10 ≈ 0.3`。
- 分数、根式等同时显示精确形式和 30 位有效数字的近似值；使用 `n(expr, digits)`（也支持 `N`）只求小数近似，精度范围为 1–1000 位。显式近似后的 `ans` 也会是近似值。
- 普通计算只接受计算器函数和常量；符号子命令额外接受变量、方程和符号结果。变量及函数名不区分大小写；`I`（也接受 `i`）为虚数单位，不能用作变量名。不开放变量赋值或 Symja 脚本。
- 非递归校验及显式栈构造表达式树；限制 4096 个字符、2048 个 token、128 层括号或树深度、每个函数 64 个参数，以及整数大小和输出长度。
- Symja 使用递归进行部分化简，配置了递归、迭代和协作式 2 秒计算时限；这些限制不等于对任意第三方算法的硬超时保证。后台串行计算，最多额外排队一个请求，按提交顺序更新 `ans`。客户端启动后会在计算线程中预加载 Symja、积分规则和常用计算路径，不改变 `ans`。预加载完成后，第一条命令无需再等待引擎初始化；若启动后立即提交命令，仍可能等待剩余预加载工作。积分规则最多等待 30 秒，实际计算仍沿用 2 秒时限。

计算核心不依赖 Minecraft。自定义 Brigadier 参数负责诊断位置和补全，Symja 负责化简和执行计算。
原生输入框可能将整个表达式参数标红；错误信息中的光标位置指向具体出错处。

## 实用数值函数

以下函数可用于 `/calc expression`，支持 Tab 补全：

| 函数 | 说明 | 示例结果 |
| --- | --- | --- |
| `ceil(x)` / `floor(x)` | 向上 / 向下取整 | `ceil(1/3)` → `1` |
| `round(x[, digits])` | 按小数位取整；默认 0 位 | `round(3.14159,2)` → `157/50 ≈ 3.14` |
| `ceildiv(a,b)` | 两个整数相除后向上取整；除数不能为 0 | `ceildiv(130,64)` → `3` |
| `gcd(a,b,...)` / `lcm(a,b,...)` | 两个或更多整数的最大公约数 / 最小公倍数 | `gcd(12,18,30)` → `6` |
| `hypot(x,y,...)` | 两个或更多实数分量的距离 | `hypot(2,3,6)` → `7` |
| `rad(degrees)` / `deg(radians)` | 角度与弧度转换，精确保留 π | `sin(rad(30))` → `1/2`；`deg(pi/2)` → `90` |
| `sum(a,b,...)` | 一个或更多数值求和 | `sum(1,2,3)` → `6` |

`round` 的位数必须是 -1000 至 1000 的整数；负位数表示十位、百位等，例如 `round(1250,-2)` → `1300`。沿用原有舍入规则：恰好一半时向正无穷取整，例如 `round(-1.5)` → `-1`，`round(-1.25,1)` → `-6/5`。精确结果仍可能显示为分数，位数控制数值，不补齐末尾零。
`ceildiv` 支持负数，`ceildiv(-130,64)` → `-2`。`gcd/lcm` 支持负整数和零，结果非负；参数必须计算为精确整数。每个多参数函数沿用最多 64 个参数的限制。

## 符号计算

```text
/calc solve x^2-5*x+6=0 x
/calc solve 2*x+y=10; x+y=7 x,y
/calc expand (x+1)^3
/calc factor x^2-1
/calc diff x^3 x
/calc diff x^2*y^3 x,y
/calc diff x^3 x,x
/calc diff x^2*y x --at x=2,y=3
/calc diff x^3 x,x --at x=2
/calc simplify sin(x)^2+cos(x)^2
/calc limit sin(x)/x x 0
/calc limit abs(x)/x x 0 left
/calc limit 1/x x inf
/calc integrate x^2 x
/calc integrate x^2 x --internal 0 3
/calc integrate sin(x) x --internal 0 pi
/calc integrate cos(x) x --internal 0 (pi / 2)
```

| 子命令 | 用法 | 说明 |
| --- | --- | --- |
| 解方程、方程组 | `/calc solve <equations> <variables>` | 用 `=` 或 `==` 表示等式，多条方程以 `;` 分隔，变量用逗号分隔；默认求实数解，开启 complex 后求复数解 |
| 展开 | `/calc expand <expression>` | 如 `(x+1)^3` → `1+3*x+3*x^2+x^3` |
| 因式分解 | `/calc factor <expression>` | 如 `x^2-1` → `(-1+x)*(1+x)` |
| 化简 | `/calc simplify <expression>` | 如 `sin(x)^2+cos(x)^2` → `1`；按 Symja 默认符号假设化简，约分可能去除原式的奇点 |
| 求极限 | `/calc limit <function> <variable> <point> [both\|left\|right]` | 默认求双侧极限；点可为有限实数表达式、`inf` 或 `-inf`，也接受 `infinity` |
| 求导、偏导及代入 | `/calc diff <function> <variables> [--at <variable=value,...>]` | `x,y` 依次求混合偏导；`x,x` 求二阶导；`--at x=2,y=3` 在求导后同时代入 |
| 积分 | `/calc integrate <integrand> <variable> [--internal <lower-limit> <upper-limit>]` | 不带选项时求不定积分，如 `x^2 x` → `x^3/3 + C`；加 `--internal 0 3` 时求定积分，结果为 `9` |

- 表达式可包含空格，也可整体用双引号包住，例如 `/calc diff "x^3 + 2*x" x`。
- 变量列表中的逗号两侧不要加空格，例如写 `x,y`。变量以字母开头，最多 32 位，可包含字母、数字和下划线；不能使用函数名或 `pi`、`e`、`ans`。
- 未作为求导变量的符号视为参数，例如 `/calc diff x*y x` → `y`。
- 积分变量必填。`--internal` 后必须同时提供上下限；上下限须为有限实数，含空格时要加括号，支持反向积分。仍兼容旧的直接填写上下限语法。
- 不定积分在聊天中提示 `+ C`，`ans` 只保存返回的原函数，不保存积分常数。
- `--at` 的代入项用逗号分隔，逗号两侧不加空格；数值可为 `pi`、`ans`、`max(1,2)` 等有限实数表达式，开启 complex 后也可代入复数（如 `--at x=I`），含空格时加括号。支持给其他参数代入值，也允许只代入部分变量；变量名不能重复。
- 极限可选 `left`（左侧）或 `right`（右侧），默认 `both`（双侧）；双侧不一致时显示错误。允许极限结果为正、负无穷，普通数值计算要求有限数值，复数结果需开启 complex。
- 方程组在聊天中按解组换行，每个变量独占一行并缩进；内部结果和 `ans` 仍为 `{{x->3,y->4}}` 这样的解集；`->` 表示变量取值，`{}` 表示当前求解范围内无解，`{{}}` 表示方程恒成立、未限制变量。
- `ans` 也保留最后一次成功的符号结果，例如先 `/calc diff x^3 x`，再 `/calc diff ans x` → `6*x`。解集也可存入 `ans`，但不应将解集当作标量进行四则运算。
- 每个请求最多 16 条方程、16 个不同符号、16 次连续求导，并沿用原有输入、表达式树、输出及后台计算限制。求解和积分能力取决于 Symja；无法完成的计算会显示错误，保留原 `ans`。

## 物品数量格式

数量格式功能默认关闭，只转换所选计算类型（`expression`、`integrate`、`diff`、`limit`、`solve`）的非负数值结果；`factor` 不参与格式化。默认只格式化整数；可切换为只格式化非整数或格式化全部数值。每个潜影盒按 27 组计算，每组物品数默认为 64。

```text
/calc numfmt enable
/calc numfmt disable
/calc numfmt default
/calc numfmt scope add expression
/calc numfmt scope add diff
/calc numfmt scope add limit
/calc numfmt scope remove solve
/calc numfmt scope [default|show]
/calc numfmt format [integer|double|all|default]
/calc numfmt template
/calc numfmt template "{} S.B.(s) + {} Stack(s) + {} Item(s)" --stack 64
/calc numfmt template default
```

新建配置或恢复默认 scope 时包含上述五种类型；已有 scope 保持原设置，可用 `scope add diff`、`scope add limit` 单独添加。导数和极限仅格式化符合当前数值类型设置的非负有限数值；符号表达式及无穷仍使用普通显示。

配置保存在 `config/calc-commands/num_fmt_cfg.json`，命令成功后立即写入磁盘并更新当前运行时配置。输入模板时可按 Tab 选择 `SUGGESTION_TEMPLATES` 中的预设；花括号只能以 `{}` 成对出现。
每条格式化结果都会先显示原值，例如 `1234=0 S.B.(s) + 19 Stack(s) + 18 Item(s) at Stack=64`。非整数的精确结果会把整数部分套入模板，并在最后追加真分数，例如 `327803/2500=2 Stack(s) + 3 Item(s) + 303/2500 at Stack=64`；近似结果会合并为小数，例如 `131.1212=2 Stack(s) + 3.1212 Item(s) at Stack=64`。

## 计算和显示配置

复数模式默认关闭；开启后，`solve` 在复数范围求解，普通计算允许有限复数结果，求导的 `--at` 支持复数代入。积分上下限和极限趋近点仍须为有限实数。复数值不参与物品数量格式化。

```text
/calc config complex
/calc config complex enable
/calc solve x^2+1=0 x
/calc expression I^2
/calc expression sqrt(-1)
/calc diff x^2 x --at x=I
/calc config complex disable
/calc config complex default
```

开启后 `x^2+1=0` 得到 `x=-I` 和 `x=I`，`sqrt(-1)` 得到 `I`。关闭时，该方程无实数解，复数数值结果会提示开启 complex；`I^2=-1` 仍可计算，因为最终结果为实数。

符号结果的加法项顺序可以动态切换：

```text
/calc config power-order ascending
/calc config power-order descending
/calc config power-order default
/calc config default
```

`ascending` 为默认值，普通单变量多项式按升幂显示为 `1 + x + x^2`；`descending` 按降幂显示为 `x^2 + x + 1`，并同样处理括号中的加法项。排列直接使用 Symja 的 `OutputFormFactory`：ascending 保留 Symja 的标准项顺序，descending 使用 `plusReversed` 反向输出加法项；多变量及其他表达式遵循 Symja 的项序规则。配置命令或具体配置项后不带参数时会显示当前值，使用 `default` 可恢复默认值。power-order 只改变聊天显示；complex 决定后续请求的数值范围，更改配置不会改写已有 `ans`。具体配置项的 `default` 只重置该项（complex 恢复关闭）；`/calc config default` 重置所有配置。配置保存在 `config/calc-commands/calc_cfg.json`，排列方式使用 `power_order` 字段。旧的 `powerOrder` 和 `termOrder` 配置会自动映射到新的排列方式；旧文件缺少 `complex` 时视为 `false`，新文件仍保存该布尔字段。

代码中通过 `DisplayConfig.get(key)`、`set(key, value)` 和 `reset(key)` 统一读写配置；`reset()` 重置所有配置。也可通过 `DisplayConfig.option(key)` 获取 `Configurable`，调用 `getValue()`、`setValue(value)` 和 `resetToDefault()`。`POWER_ORDER_KEY` 的值来自 `Options.PowerOrder`，`COMPLEX_KEY` 的值来自 `Options.AbleOption`。设置值经过校验，保存成功后才更新内存。新增配置时定义可选值枚举，用 `register(key, 默认枚举值)` 注册，并提供对应翻译；默认值和可选值从枚举获取，配置命令自动生成（key 中的下划线转换为命令中的连字符）。

## Build and test

使用 JDK 25：

```powershell
.\gradlew.bat build
```

分发文件在 `build/libs/`。Symja 及其运行依赖随 mod 以独立嵌套 jar 打包，无需单独安装。当前发布包约 27.5 MB。
JUnit 测试覆盖命令树、默认值与无参数查询路径、输入校验和补全、精确数值与 `ans`、展开/因式分解/化简/极限/求导及代入/不定积分/定积分/求解、实用数值函数、数量格式、升降幂显示，以及中英文帮助资源完整性。

## 翻译数据生成

中英文翻译分别维护在 `fabricmc.datagen` 包中的 `ENUSLanguageProvider` 和 `ZHCNLanguageProvider`。
修改 Provider 后，使用 JDK 25 执行：

```powershell
.\gradlew.bat runDatagen
```

生成的 `en_us.json` 和 `zh_cn.json` 位于 `src/main/generated/assets/calc-commands/lang/`，
Gradle 会自动将它们打包。请保留生成的语言文件，不要直接编辑；新增翻译时同步更新两个 Provider。

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

The mod's own code remains under CC0. The bundled [Symja](https://github.com/axkr/symja_android_library)
`matheclipse-core`, `matheclipse-parser`, and `matheclipse-external` modules use LGPL-3.0.
Their licenses and the licenses of their transitive dependencies apply to their respective nested jars.
Corresponding Symja 3.2.0 sources are available from [Maven Central](https://repo.maven.apache.org/maven2/org/matheclipse/).
Symja class files remain unchanged; Loom adds Fabric metadata to the separate nested jars.
The GPL-only Symja modules are not included. License texts and source links are in `META-INF/licenses/`.
