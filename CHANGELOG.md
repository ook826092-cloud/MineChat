# 更新日志

## 2026-06-17

### 提供商排序修复
- 新建提供商默认插入到列表首位（displayOrder = 0）
- 已有提供商自动后移一位，保持拖动排序不受影响

### OpenAI 提供商设置对齐 Rikka
- 新增 `includeHistoryReasoning` 开关（默认开启）
- UI 配置页新增"Include history reasoning"选项
- `ChatCompletionsAPI.buildMessages` 根据开关决定是否传递历史 reasoning
