# Rikka 对齐日志

MR 基于 RikkaHub 2.2.5 fork，此文件记录每次将 Rikka 上游更新适配到 MR 的改动日志。

---

## 2026-06-16

### 工作区列表气泡图标
- **WorkspacePage.kt**: 卡片图标从 `Folder01` 改为 `File02`，对齐 Rikka 的带气泡文档图标

### 工具审批开关逻辑修复
- **WorkspaceDetailPage.kt**: Switch `checked = overrides[toolName] ?: toolApprovalAll` → `checked = overrides[toolName] ?: false`，总开关不再联动 UI 显示

### 终端全屏状态栏重叠修复
- **WorkspaceTerminalPage.kt**: 给 `Surface` 加了 `.windowInsetsPadding(systemBars)`，避免内容被系统状态栏覆盖

### 工作区列表状态气泡
- **WorkspacePage.kt**: 卡片添加 `shellStatus` 状态文字显示，使用 `toShellStatusLabel()` 映射
- **strings.xml**: 添加 `workspace_page_shell_disabled` / `workspace_page_shell_installing` / `workspace_page_shell_ready` / `workspace_page_shell_broken`

### 工作区详情页底部导航
- **WorkspaceDetailPage.kt**: 用 `HorizontalPager` + `NavigationBar` 替换 `PrimaryTabRow` + `Tab` 实现底部导航

### 终端全屏化
- **WorkspaceTerminalPage.kt**: 移除 Scaffold/TopAppBar/BackButton，终端从全屏 Activity 启动
- **WorkspaceTerminalSession.kt**: 添加 `prepareWorkspaceTerminalSession()` 函数

### 启动崩溃修复
- **DataSourceModule.kt**: `seedDefaultAssistant()` 中补充 `mcp_server_ids` 字段入 ContentValues
- **AppDatabase.kt**: 添加 `MIGRATION_11_12` 添加 `mcp_server_ids` 列
- **WorkspaceEntity.kt**: 添加 `toolApprovals` 字段

## 2026-06-17

### 读图片支持
- **WorkspaceTools.kt**: `createReadFileTool` 增加图片检测，自动识别 png/jpg/gif/webp 等，读取后通过 `FilesManager` 生成 URI 以 `UIMessagePart.Image` 渲染到聊天

### 路径越界检测
- **WorkspaceTools.kt**: `createWriteFileTool` / `createEditFileTool` 增加 `requireSafePath()` 校验，阻止 `../` 和绝对路径遍历逃逸工作区目录

---

## 2026-06-17 (第二轮)

### 工具审批语义对齐：移除 `__all__`
- **WorkspaceTools.kt**: `resolveWorkspaceToolApproval` 去掉 `__all__` 特殊处理
- **WorkspaceRepository.kt**: 删除 `setToolApprovalAll` / `getToolApprovalAll`
- **WorkspaceDetailVM.kt**: 删除 `_toolApprovalAll` flow / `toggleToolApprovalAll`
- **WorkspaceDetailPage.kt**: 删除 `toolApprovalAll` Switch 和传参

### WorkspaceSelectSheet 独立组件
- **WorkspaceSelectSheet.kt** (新增): 聊天底部选工作区弹窗，支持 shell 状态显示、选中高亮、管理入口
- **FilesPicker.kt**: 用新的 `WorkspaceSelectSheet` 替换旧的内联 `WorkspaceSelectorSheet`

### 聊天中显示编辑的文件
- **ChatMessageEditedFiles.kt** (新增): 在聊天消息中显示 AI 编辑过的文件列表，支持导出/分享
- **ChatMessage.kt**: 在 assistant 消息中集成 `EditedFilesList`

### SAF DocumentsProvider 暴露工作区文件
- **WorkspaceDocumentsProvider.kt** (新增): 通过系统文件管理器访问工作区文件目录，支持 CRUD
- **WorkspaceDAO.kt**: 添加 `getAll()` 供 Provider 使用
- **AndroidManifest.xml**: 注册 `WorkspaceDocumentsProvider`
