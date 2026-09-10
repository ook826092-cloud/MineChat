# 历史 Bug 记录

## 1. Cursor 未 moveToFirst() 就读取数据

- **位置**: `CharacterCardParser.kt:109` (`queryDisplayName` 方法)
- **同模式文件**:
  - `JsonImportParser.kt:22` — 同样的 `queryDisplayName`
  - `FileUtils.kt:57,61`
  - `WorkspaceDetailPage.kt:119`
  - `SettingKnowledgeBaseDetailPage.kt:133`
- **错误信息**: `CursorIndexOutOfBoundsException: Index -1 requested, with a size of 1`
- **根因**: `ContentResolver.query()` 返回的 Cursor 初始位置为 -1，调用 `cursor.getString()` 前必须 `cursor.moveToFirst()`。
- **触发场景**: 导入角色卡时调用 `queryDisplayName` 查询文件显示名，cursor 有 1 条结果但未移动位置，直接 getString 抛异常。
- **修复**: 在 `cursor.getString()` 前加 `if (cursor.moveToFirst())` 判断。
