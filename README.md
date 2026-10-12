# Calc Commands

适用于 Minecraft 26.3 的 Fabric 客户端计算器，使用 Symja 3.2.0 进行符号计算和高精度数值计算。

```text
/calc 1+2*3
/calc sin(pi/2)
/calc sqrt(3^2+4^2)
/calc 2^(-3)
/calc log(2,8)
/calc sin(100*pi)
/calc 1/3+1/6
/calc sqrt(8)
/calc n(sqrt(2),30)
/calc ans*2
/calc help
```

- 输入时校验语法，`sin(`、`1+`、缺失括号和错误参数数量会交给 Minecraft 原生标红并提示。
- Tab 补全函数、`pi`、`e`、`ans`，支持大小写不敏感的名称和科学计数法。
- 按回车后才在后台线程调用 Symja 计算；输入校验和补全不改变 `ans`。
- `ans` 初始为 0，精确保留本次游戏运行中最后一次成功结果；计算失败不覆盖答案。
- 三角函数使用弧度。乘法必须写 `*`。支持 `2^-3` 和 `2^(-3)`。
- 支持三角函数、`sqrt`、`atan2(y, x)`、`sum`、`avg` 等；`ln(x)` 为自然对数，`lg(x)` 为常用对数，`log2(x)` 以 2 为底，`log(base, x)` 指定底数。`/calc help` 列出全部函数。
- 整数、分数和有限小数字面量精确保留，π 和 e 保留为符号；例如 `sin(100*pi) = 0`，`0.1+0.2 = 3/10 ≈ 0.3`。
- 分数、根式等同时显示精确形式和 30 位有效数字的近似值；使用 `n(expr, digits)`（也支持 `N`）只求小数近似，精度范围为 1–1000 位。显式近似后的 `ans` 也会是近似值。
- 普通计算只接受计算器函数和常量；符号子命令额外接受变量、方程和符号结果。变量及函数名不区分大小写。不开放变量赋值或 Symja 脚本。
- 非递归校验及显式栈构造表达式树；限制 4096 个字符、2048 个 token、128 层括号或树深度、每个函数 64 个参数，以及整数大小和输出长度。
- Symja 使用递归进行部分化简，配置了递归、迭代和协作式 2 秒计算时限；这些限制不等于对任意第三方算法的硬超时保证。后台串行计算，最多额外排队一个请求，按提交顺序更新 `ans`。客户端启动后会在计算线程中预加载 Symja、积分规则和常用计算路径，不改变 `ans`。预加载完成后，第一条命令无需再等待引擎初始化；若启动后立即提交命令，仍可能等待剩余预加载工作。积分规则最多等待 30 秒，实际计算仍沿用 2 秒时限。

计算核心不依赖 Minecraft。自定义 Brigadier 参数负责诊断位置和补全，Symja 负责化简和执行计算。
原生输入框可能将整个表达式参数标红；错误信息中的光标位置指向具体出错处。

## 符号计算

```text
/calc solve x^2-5*x+6=0 x
/calc solve 2*x+y=10; x+y=7 x,y
/calc expand (x+1)^3
/calc factor x^2-1
/calc diff x^3 x
/calc diff x^2*y^3 x,y
/calc diff x^3 x,x
/calc integrate x^2 x 0 3
/calc integrate sin(x) x 0 pi
/calc integrate cos(x) x 0 (pi / 2)
```

| 子命令 | 用法 | 说明 |
| --- | --- | --- |
| 解方程、方程组 | `/calc solve <equations> <variables>` | 用 `=` 或 `==` 表示等式，多条方程以 `;` 分隔，变量用逗号分隔；求实数解 |
| 展开 | `/calc expand <expression>` | 如 `(x+1)^3` → `1+3*x+3*x^2+x^3` |
| 因式分解 | `/calc factor <expression>` | 如 `x^2-1` → `(-1+x)*(1+x)` |
| 求导 | `/calc diff <function> <variables>` | `x,y` 表示先对 x、再对 y 求导；`x,x` 表示对 x 求二阶导数 |
| 定积分 | `/calc integrate <function> <variable> <from> <to>` | 如 `x^2 x 0 3` → `9`；上下限可用 `pi` 等数值表达式 |

- 表达式可包含空格，也可整体用双引号包住，例如 `/calc diff "x^3 + 2*x" x`。
- 变量列表中的逗号两侧不要加空格，例如写 `x,y`。变量以字母开头，最多 32 位，可包含字母、数字和下划线；不能使用函数名或 `pi`、`e`、`ans`。
- 未作为求导变量的符号视为参数，例如 `/calc diff x*y x` → `y`。
- 积分上下限须为有限实数，含空格的上下限要加括号；支持反向积分。
- 方程组输出 `{{x->3,y->4}}` 这样的解集；`->` 表示变量取值，`{}` 表示没有实数解，`{{}}` 表示方程恒成立、未限制变量。
- `ans` 也保留最后一次成功的符号结果，例如先 `/calc diff x^3 x`，再 `/calc diff ans x` → `6*x`。解集也可存入 `ans`，但不应将解集当作标量进行四则运算。
- 每个请求最多 16 条方程、16 个不同符号、16 次连续求导，并沿用原有输入、表达式树、输出及后台计算限制。求解和积分能力取决于 Symja；无法完成的计算会显示错误，保留原 `ans`。

## Build and test

使用 JDK 25：

```powershell
.\gradlew.bat build
```

分发文件在 `build/libs/`。Symja 及其运行依赖随 mod 以独立嵌套 jar 打包，无需单独安装。当前发布包约 27.5 MB。
测试覆盖精确分数、π 和根式、指定精度、运算规则、方程与方程组、展开与因式分解、多变量求导、定积分、未完成输入位置、补全、精确答案复用和资源限制。

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
