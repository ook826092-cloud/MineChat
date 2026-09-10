# 更新日志

## 2026-06-21

### 正则系统重构：删除旧 Rikka 正则，全面迁移至 Tavern 正则引擎

- **删除旧系统**：移除 `Assistant.regexes`、`AssistantRegex`、`AssistantAffectScope`、`replaceRegexes()` 扩展函数
- **创建新工具类**：`TavernRegexParser.kt` — 从 `assistant.regexScriptsJson` 解析 Tavern 格式正则脚本
- **修复执行点缺失**：在以下位置使用 `TavernRegexRunner.apply()` 替换旧的 `replaceRegexes()`：
  - `ChatService.preprocessUserInputParts()` — 发送前用户输入处理（USER_INPUT, isPrompt=false）
  - `ChatMessage.kt` — 显示时消息渲染（USER_INPUT/AI_OUTPUT, isMarkdown=true）
  - `ChatMessageReasoning.kt` — 推理过程显示（REASONING, isMarkdown=true）
  - `TavernRegexOutputTransformer.kt` — AI 输出存储时处理（AI_OUTPUT/REASONING, isPrompt=false）
- **对齐 SillyTavern 逻辑**：`TavernRegexRunner.appliesTo()` 修复 default 脚本仅运行于常规上下文（非 markdownOnly 非 promptOnly）
- **删除旧文件**：`RegexOutputTransformer.kt`（已被 `TavernRegexOutputTransformer` 取代）

## 2026-06-17

### 提供商排序修复
- 新建提供商默认插入到列表首位（displayOrder = 0）
- 已有提供商自动后移一位，保持拖动排序不受影响

### OpenAI 提供商设置对齐 Rikka
- 新增 `includeHistoryReasoning` 开关（默认开启）
- UI 配置页新增"Include history reasoning"选项
- `ChatCompletionsAPI.buildMessages` 根据开关决定是否传递历史 reasoning
