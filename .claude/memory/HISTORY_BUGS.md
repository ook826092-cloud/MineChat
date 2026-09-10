# 历史 Bug 记录 / Historical Bug Records

本文档记录项目中修复过的历史 Bug，供后续开发和排查参考。

---

## Bug 1: Cursor 未 moveToFirst 导致导入角色卡崩溃

### 错误信息

```
android.database.CursorIndexOutOfBoundsException: Index -1 requested, with a size of 1
```

### 触发场景

在应用中导入角色卡（PNG 或 JSON 格式）时，抛出上述异常并显示 Toast 提示。

### 完整堆栈

```
CursorIndexOutOfBoundsException: Index -1 requested, with a size of 1
    at AbstractCursor.checkPosition(AbstractCursor.java:514)
    at AbstractWindowedCursor.checkPosition(AbstractWindowedCursor.java:138)
    at AbstractWindowedCursor.getString(AbstractWindowedCursor.java:52)
    at CursorWrapper.getString(CursorWrapper.java:152)
    at CharacterCardParser.queryDisplayName(CharacterCardParser.kt:109)
    at CharacterCardParser.parse(CharacterCardParser.kt:19)
    at CharacterImportService.importCharacterCard(CharacterImportService.kt:66)
    at AssistantImporter.importAssistantFromUri(AssistantImporter.kt:157)
```

### 根因分析

`CharacterCardParser.queryDisplayName()` 方法中使用 `ContentResolver.query()` 查询文件的 DISPLAY_NAME，但**在调用 `cursor.getString()` 之前没有调用 `cursor.moveToFirst()`**。

`ContentResolver.query()` 返回的 `Cursor` 初始位置为 **-1**（第一行之前）。即使查询只有 1 条结果，也必须先 `moveToFirst()` 将位置移到 0，否则读取数据会抛出 `CursorIndexOutOfBoundsException`。

```kotlin
// ❌ 错误写法
resolver.query(uri, null, null, null, null)?.use { cursor ->
    val nameColumn = cursor.getColumnIndex(DISPLAY_NAME)
    if (nameColumn >= 0) cursor.getString(nameColumn) else null  // 没 moveToFirst 就读取！
}

// ✅ 正确写法
resolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
        val nameColumn = cursor.getColumnIndex(DISPLAY_NAME)
        if (nameColumn >= 0) cursor.getString(nameColumn) else null
    } else null
}
```

### 影响范围

导入角色卡功能完全不可用。

### 涉及文件

| 文件 | 行号 | 说明 |
|------|------|------|
| `app/src/main/java/cn/mine/minestars/data/import/CharacterCardParser.kt` | 109 | 主触发点 - queryDisplayName |
| `app/src/main/java/cn/mine/minestars/data/import/JsonImportParser.kt` | 22 | 同模式 - queryDisplayName |
| `app/src/main/java/cn/mine/minestars/data/files/FileUtils.kt` | 57, 61 | 同模式 - 查询文件信息 |
| `app/src/main/java/cn/mine/minestars/ui/pages/workspace/WorkspaceDetailPage.kt` | 119 | 同模式 - 查询文件名 |
| `app/src/main/java/cn/mine/minestars/ui/pages/setting/SettingKnowledgeBaseDetailPage.kt` | 133 | 同模式 - 查询文件名 |

### 发现方式

通过 ADB logcat 抓取运行时日志定位。

### 修复日期

2026-06-17
