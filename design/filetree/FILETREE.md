MR/  (D:\Mine\MR)
│   · 项目名 minechat（多模块 Android AI 聊天客户端，包名 cn.mine.minestars）
│
├── settings.gradle.kts             · 模块清单：:app :ai :common :core:tavern :core:workspace :document :highlight :material3 :rag :search :speech
├── build.gradle.kts                · 根构建配置（[构建配置·不展开]）
├── gradle.properties               · Gradle / AndroidX / ObjectBox 版本与开关
├── gradlew(.bat)                   · Gradle Wrapper 启动器
├── gradle/wrapper/                 · Wrapper 版本锁定
├── keystore/                       · 签名密钥（release keystore）
├── keystore.properties             · 签名密钥路径/别名/口令（本地不提交）
├── LICENSE                         · 开源许可
├── CHANGELOG.md                    · 更新日志
├── HISTORY_BUGS.md                 · 历史 bug 记录
├── RIKKA_ALIGN_LOG.md              · Rikka 对齐记录
│
├── app/                            · ① 主应用模块（唯一入口 + UI + 业务 + 数据）
│   ├── build.gradle.kts            · [构建配置·不展开]
│   └── src/main/
│       ├── AndroidManifest.xml       · App 主清单（Application/Activity/权限/FileProvider/快捷方式）
│       ├── baseline-prof.txt         · 启动基线性能配置
│       ├── assets/                   · 打包资源（运行时读取）
│       │   ├── banner/               · 捐赠横幅图（3）
│       │   ├── emoji/                · 表情数据
│       │   ├── html/                 · 内置 HTML 页（2）
│       │   ├── icons/                · 品牌 logo（约 51 个，模型/提供商彩色图标）[不逐一列举]
│       │   └── simple_dict/          · 精简词典（pos_dict 词性标注，供 FTS 分词）
│       ├── java/cn/mine/minestars/   · 根包
│       │   ├── MineChatApp.kt        · Application 入口：装配全局依赖 / 崩溃处理 / 服务初始化
│       │   ├── RouteActivity.kt      · 唯一 Activity（启动入口）：Compose 导航挂载 + 系统分享/快捷方式意图分发
│       │   ├── data/                 · 数据层（DB / DataStore / AI 编排 / 网络 / 导入导出）
│       │   │   ├── ai/               · AI 生成编排（在 ai 模块之上做应用级封装）
│       │   │   │   ├── GenerationHandler.kt · 生成主流程：transformers 前置 → 流式生成 → 后置处理
│       │   │   │   ├── GenerationPrompts.kt · 生成提示词模板（压缩/摘要/续写 etc.）
│       │   │   │   ├── AIRequestInterceptor.kt · 请求拦截器（鉴权/改包）
│       │   │   │   ├── RequestLoggingInterceptor.kt · 请求日志
│       │   │   │   ├── AILogging.kt   · AI 日志
│       │   │   │   ├── prompts/       · 各类提示词
│       │   │   │   │   ├── CompressPrompt.kt · 上下文压缩提示词
│       │   │   │   │   ├── OcrPrompt.kt · OCR 提示词
│       │   │   │   │   ├── Suggestion.kt · 追问/建议提示词
│       │   │   │   │   ├── TitleSummary.kt · 会话标题摘要提示词
│       │   │   │   │   └── Translation.kt · 翻译提示词
│       │   │   │   ├── mcp/           · MCP（Model Context Protocol）客户端
│       │   │   │   │   ├── McpConfig.kt · MCP 配置模型
│       │   │   │   │   ├── McpManager.kt · MCP 服务器生命周期管理
│       │   │   │   │   ├── McpStatus.kt · MCP 连接状态
│       │   │   │   │   └── transport/ · 传输层
│       │   │   │   │       ├── SseClientTransport.kt · SSE 客户端传输
│       │   │   │   │       └── StreamableHttpClientTransport.kt · Streamable HTTP 客户端传输
│       │   │   │   ├── tools/         · 内置工具（给模型调用 / 本地执行）
│       │   │   │   │   ├── LocalTools.kt · 本地工具集合
│       │   │   │   │   ├── SearchTools.kt · 搜索工具
│       │   │   │   │   ├── KnowledgeBaseTool.kt · 知识库检索工具
│       │   │   │   │   ├── MemoryTools.kt · 记忆工具
│       │   │   │   │   ├── SkillsTools.kt · 技能工具
│       │   │   │   │   ├── WorkspaceTools.kt · 工作区工具
│       │   │   │   │   └── MoegirlTools.kt · 萌娘百科检索
│       │   │   │   └── transformers/  · 消息/提示词变换器（生成前/后管线）
│       │   │   │       ├── Transformer.kt · 变换器接口
│       │   │   │       ├── PlaceholderTransformer.kt · 占位符替换
│       │   │   │       ├── TemplateTransformer.kt · 模板展开
│       │   │   │       ├── ThinkTagTransformer.kt · 思考标签处理
│       │   │   │       ├── OcrTransformer.kt · OCR 变换
│       │   │   │       ├── DocumentAsPromptTransformer.kt · 文档→提示词
│       │   │   │       ├── Base64ImageToLocalFileTransformer.kt · 图片 Base64→本地文件
│       │   │   │       ├── WorkspaceFileReferenceTransformer.kt · 工作区文件引用
│       │   │   │       ├── TavernRegexInputTransformer.kt · Tavern 正则（输入向）
│       │   │   │       ├── TavernRegexOutputTransformer.kt · Tavern 正则（输出向）
│       │   │   │       └── TimeReminderTransformer.kt · 时间提醒注入
│       │   │   ├── api/              · 后端 API 客户端
│       │   │   │   ├── MineChatAPI.kt · 主后端接口
│       │   │   │   └── SponsorAPI.kt · 赞助方（爱发电/帕特隆）接口
│       │   │   ├── datastore/        · Preferences DataStore 设置
│       │   │   │   ├── PreferencesStore.kt · 通用偏好存储
│       │   │   │   ├── AiSettingsStore.kt · AI 设置存储
│       │   │   │   ├── DefaultProviders.kt · 默认提供商预置
│       │   │   │   └── migration/    · DataStore 迁移（空）
│       │   │   ├── db/               · Room / ObjectBox 数据库
│       │   │   │   ├── AppDatabase.kt · 数据库装配（连接全部 DAO）
│       │   │   │   ├── AssistantMapper.kt · 助手映射
│       │   │   │   ├── ProviderMapper.kt · 提供商映射
│       │   │   │   ├── DatabaseMigrationTracker.kt · 迁移追踪
│       │   │   │   ├── entity/       · 实体（30 个，Table 定义）[不逐一列举]
│       │   │   │   │   ├── ConversationEntity.kt ~ MessageNodeEntity.kt · 会话/消息树
│       │   │   │   │   ├── AssistantEntity.kt / ProviderEntity.kt / ModelSelectionEntity.kt · 助手/提供商/模型选择
│       │   │   │   │   ├── MemoryEntity.kt / KnowledgeBaseEntity.kt / McpServerEntity.kt · 记忆/知识库/MCP
│       │   │   │   │   ├── WorldBookEntity.kt ~ PresetEntity.kt ~ RegexScriptEntity.kt · Tavern 系（世界书/预设/正则）
│       │   │   │   │   ├── CharacterCardEntity.kt / TagEntity.kt / UserPersonaEntity.kt · 角色卡/标签/人设
│       │   │   │   │   ├── TTSProviderEntity.kt / ASRProviderEntity.kt / SearchServiceEntity.kt · 语音/搜索服务
│       │   │   │   │   └── WorkspaceEntity.kt / ManagedFileEntity.kt / GenMediaEntity.kt … · 其余
│       │   │   │   ├── dao/          · 数据访问对象（30 个，与 entity 一一对应）[不逐一列举]
│       │   │   │   ├── fts/          · 全文检索
│       │   │   │   │   ├── MessageFtsManager.kt · 消息全文检索
│       │   │   │   │   └── SimpleDictManager.kt · 精简词典管理器
│       │   │   │   └── migrations/   · 数据库迁移脚本（空）
│       │   │   ├── event/            · 事件总线
│       │   │   │   ├── AppEvent.kt   · 应用事件定义
│       │   │   │   └── AppEventBus.kt · 事件总线（发布/订阅）
│       │   │   ├── export/           · 导出
│       │   │   │   ├── CharacterCardExporter.kt · 角色卡导出
│       │   │   │   ├── ExportSerializer.kt · 导出序列化
│       │   │   │   └── ExportHooks.kt · 导出钩子
│       │   │   ├── favorite/         · 收藏适配
│       │   │   │   ├── FavoriteAdapter.kt · 收藏适配器
│       │   │   │   └── NodeFavoriteAdapter.kt · 节点收藏适配器
│       │   │   ├── files/            · 文件管理
│       │   │   │   ├── FilesManager.kt · 文件管理器
│       │   │   │   ├── FileUtils.kt  · 文件工具
│       │   │   │   ├── SkillManager.kt · 技能安装/管理
│       │   │   │   └── SkillPaths.kt · 技能目录路径
│       │   │   ├── import/           · 导入
│       │   │   │   ├── CharacterCardParser.kt · 角色卡解析（PNG/JSON）
│       │   │   │   ├── CharacterImportService.kt · 角色卡导入服务
│       │   │   │   ├── JsonImportParser.kt · 通用 JSON 导入
│       │   │   │   └── DefaultPresetManager.kt · 默认预设管理
│       │   │   ├── model/            · 领域模型
│       │   │   │   ├── Conversation.kt · 会话（含消息树）
│       │   │   │   ├── Assistant.kt  · 助手（人设/系统提示词/绑定）
│       │   │   │   ├── Favorite.kt / Folder.kt / Tag.kt · 收藏/文件夹/标签
│       │   │   │   ├── Avatar.kt     · 头像
│       │   │   │   └── Sponsor.kt    · 赞助方
│       │   │   ├── provider/         · 内容提供者
│       │   │   │   └── WorkspaceDocumentsProvider.kt · 工作区文档 ContentProvider
│       │   │   ├── repository/       · 仓库
│       │   │   │   ├── ConversationRepository.kt · 会话仓库
│       │   │   │   ├── FavoriteRepository.kt / FolderRepository.kt · 收藏/文件夹仓库
│       │   │   │   ├── MemoryRepository.kt · 记忆仓库
│       │   │   │   ├── FilesRepository.kt · 文件仓库
│       │   │   │   ├── GenMediaRepository.kt · 生成媒体仓库
│       │   │   │   └── WorkspaceRepository.kt · 工作区仓库
│       │   │   ├── sync/             · 同步 / 备份合并
│       │   │   │   ├── LocalBackupSync.kt · 本地备份同步
│       │   │   │   └── importer/     · 第三方迁移导入
│       │   │   │       ├── ChatboxImporter.kt · Chatbox 导入
│       │   │   │       └── CherryStudioProviderImporter.kt · CherryStudio 提供商导入
│       │   │   └── tavern/           · Tavern / SillyTavern 系支持（酒馆格式兼容）
│       │   │       └── pipeline/     · 提示词组装管线
│       │   │           ├── TavernDataTypes.kt · Tavern 数据类型
│       │   │           ├── TavernDataLoader.kt · Tavern 数据加载
│       │   │           ├── TavernContext.kt · Tavern 上下文
│       │   │           ├── PromptOrderAssembler.kt · 提示词顺序组装
│       │   │           ├── SquashSystemMessages.kt · 系统消息合并
│       │   │           ├── TavernRegexParser.kt · Tavern 正则解析
│       │   │           └── TavernRegexRunner.kt · Tavern 正则执行
│       │   ├── di/                   · 装配层（依赖注入）
│       │   │   ├── AppModule.kt      · 应用级依赖
│       │   │   ├── DataSourceModule.kt · 数据源绑定
│       │   │   ├── RepositoryModule.kt · 仓库绑定
│       │   │   ├── ViewModelModule.kt · ViewModel 装配
│       │   │   └── WorkspaceModule.kt · 工作区依赖
│       │   ├── feature/              · 功能页（Tavern 系）
│       │   │   ├── character/        · 角色卡
│       │   │   │   ├── CharacterCardListPage.kt · 角色卡列表页
│       │   │   │   └── CharacterCardVM.kt · 角色卡 VM
│       │   │   └── tavern/           · 酒馆功能（预设/正则/世界书）
│       │   │       ├── PresetListPage.kt / PresetListVM.kt · 预设列表
│       │   │       ├── PresetDetailScreen.kt / PresetDetailVM.kt · 预设详情
│       │   │       ├── PresetEntryDetailScreen.kt · 预设条目详情
│       │   │       ├── RegexListPage.kt / RegexListVM.kt · 正则列表
│       │   │       ├── RegexGroupDetailPage.kt / RegexGroupDetailVM.kt · 正则组详情
│       │   │       ├── RegexScriptDetailScreen.kt · 正角脚本详情
│       │   │       ├── WorldBookListPage.kt / WorldBookListVM.kt · 世界书列表
│       │   │       ├── WorldBookDetailScreen.kt / WorldBookDetailVM.kt · 世界书详情
│       │   │       ├── WorldBookEntryDetailScreen.kt · 世界书条目详情
│       │   │       ├── AssistantRegexListVM.kt / AssistantRegexScriptListPage.kt / AssistantRegexScriptDetailScreen.kt · 助手级正则/脚本
│       │   │       ├── AssistantWorldBookListPage.kt / AssistantWorldBookListVM.kt · 助手级世界书
│       │   │       └── CustomIcons.kt · 自定义图标
│       │   ├── service/              · 后台服务
│       │   │   ├── ChatService.kt    · 聊天服务
│       │   │   └── ConversationSession.kt · 会话会话管理
│       │   ├── ui/                   · 表现层（Compose）
│       │   │   ├── activity/         · 额外 Activity
│       │   │   │   ├── SafeModeActivity.kt · 安全模式（崩溃兜底）
│       │   │   │   └── ShortcutHandlerActivity.kt · 快捷方式分发
│       │   │   ├── context/          · CompositionLocal 上下文
│       │   │   │   ├── LocalSettings.kt · 设置上下文
│       │   │   │   ├── LocalASRState.kt / LocalTTSState.kt · 语音状态上下文
│       │   │   │   ├── NavContext.kt · 导航上下文
│       │   │   │   ├── SharedElement.kt · 共享元素转场
│       │   │   │   └── ToasterContext.kt · 全局 Toast
│       │   │   ├── hooks/            · 可复用 Compose Hooks
│       │   │   │   ├── UseAssistant.kt · 当前助手
│       │   │   │   ├── ChatInputState.kt · 输入框状态
│       │   │   │   ├── ASR.kt / TTS.kt · 语音输入/输出
│       │   │   │   ├── ColorMode.kt / Settings.kt · 颜色模式/设置
│       │   │   │   ├── Debounce.kt / Lifecycle.kt / ImeAutoScroller.kt · 防抖/生命周期/输入法滚动
│       │   │   │   ├── SharedPreferences.kt · SharedPreferences 读取
│       │   │   │   ├── ChatInputState.kt / UseEditState.kt · 输入/编辑态
│       │   │   │   ├── AvatarShape.kt / HeroAnimation.kt / PlayStore.kt · 头像形状/转场动画/商店
│       │   │   │   └── …
│       │   │   ├── modifier/         · Modifier 扩展
│       │   │   │   ├── Clickable.kt  · 可点击
│       │   │   │   └── Shimmer.kt    · 微光占位
│       │   │   ├── theme/            · 主题（颜色/字体/预设）
│       │   │   │   ├── Theme.kt      · 主题入口
│       │   │   │   ├── Color.kt      · 颜色定义
│       │   │   │   ├── Type.kt       · 字体排版
│       │   │   │   ├── ChatFont.kt   · 聊天字体
│       │   │   │   ├── CodeColor.kt  · 代码配色
│       │   │   │   ├── PresetTheme.kt / CustomTheme.kt · 预设/自定义主题
│       │   │   │   └── presets/      · 7 套预设主题（每套明/暗）
│       │   │   │       ├── AutumnTheme.kt · 秋意
│       │   │   │       ├── BlackTheme.kt · 纯黑
│       │   │   │       ├── ClaudeTheme.kt · Claude
│       │   │   │       ├── MinimalTheme.kt · 极简
│       │   │   │       ├── OceanTheme.kt · 海洋
│       │   │   │       ├── SakuraTheme.kt · 樱花
│       │   │   │       └── SpringTheme.kt · 新春
│       │   │   ├── components/       · 公共组件
│       │   │   │   ├── ai/           · AI 交互组件
│       │   │   │   │   ├── ChatInput.kt · 聊天输入框（主输入）
│       │   │   │   │   ├── ModelList.kt · 模型选择列表
│       │   │   │   │   ├── AssistantPicker.kt · 助手选择器
│       │   │   │   │   ├── ReasoningPicker.kt · 推理强度选择
│       │   │   │   │   ├── SearchPicker.kt · 搜索服务选择
│       │   │   │   │   ├── McpPicker.kt · MCP 选择
│       │   │   │   │   ├── WorkspaceSelectSheet.kt · 工作区选择弹层
│       │   │   │   │   ├── AttachmentChips.kt / FilesPicker.kt / CropLauncher.kt · 附件/文件/裁剪
│       │   │   │   │   ├── AsrButton.kt · 语音按钮
│       │   │   │   │   ├── ExtensionContent.kt · 扩展内容
│       │   │   │   │   ├── CompressContextDialog.kt · 压缩上下文中弹窗
│       │   │   │   │   └── ProviderBalanceText.kt · 提供商余额文本
│       │   │   │   │   └── completion/ · 输入补全
│       │   │   │   │       ├── ChatCompletionProvider.kt · 聊天输入补全
│       │   │   │   │       ├── WorkspaceCompletionProvider.kt · 工作区补全
│       │   │   │   │       └── WorkspaceIgnoreMatcher.kt · 工作区忽略匹配
│       │   │   │   ├── message/      · 消息渲染
│       │   │   │   │   ├── ChatMessage.kt · 消息容器
│       │   │   │   │   ├── ChatMessageActions.kt · 消息操作（复制/重试/翻译）
│       │   │   │   │   ├── ChatMessageAvatar.kt · 头像
│       │   │   │   │   ├── ChatMessageBranch.kt · 消息分支（多候选）
│       │   │   │   │   ├── ChatMessageReasoning.kt / ChatMessageCot.kt · 思考/思维链
│       │   │   │   │   ├── ChatMessageTools.kt · 工具调用展示
│       │   │   │   │   ├── ChatMessageEditedFiles.kt · 已编辑文件
│       │   │   │   │   ├── ChatMessageNerdLine.kt · 状态栏（tokens/耗时）
│       │   │   │   │   ├── ChatMessageTranslation.kt · 翻译
│       │   │   │   │   ├── ChatMessageCopySheet.kt · 复制弹层
│       │   │   │   │   └── tools/    · 工具 UI
│       │   │   │   │       ├── ToolUI.kt · 工具 UI 接口
│       │   │   │   │       ├── BuiltinToolUIs.kt · 内置工具 UI
│       │   │   │   │       └── WorkspaceToolUIs.kt · 工作区工具 UI
│       │   │   │   ├── richtext/     · 富文本渲染
│       │   │   │   │   ├── Markdown.kt / MarkdownNew.kt / MarkdownWeb.kt · Markdown
│       │   │   │   │   ├── HighlightCodeBlock.kt · 代码高亮块
│       │   │   │   │   ├── LatexText.kt / MathBlock.kt · LaTeX 公式
│       │   │   │   │   ├── Mermaid.kt · Mermaid 图表
│       │   │   │   │   ├── DiffView.kt · Diff 对比
│       │   │   │   │   ├── SimpleHtmlBlock.kt · 内嵌 HTML
│       │   │   │   │   └── ZoomableAsyncImage.kt · 可缩放图片
│       │   │   │   ├── table/        · DataTable.kt（表格渲染）
│       │   │   │   ├── nav/          · BackButton.kt（返回按钮）
│       │   │   │   ├── webview/      · WebView.kt（内嵌 WebView）
│       │   │   │   ├── easteregg/    · EmojiBurst.kt（彩蛋·表情爆发）
│       │   │   │   └── ui/           · 通用 UI 基础组件（36 个）
│       │   │   │       ├── AIIcon.kt / UIAvatar.kt / Emoji.kt · 图标/头像/表情
│       │   │   │       ├── CardGroup.kt / ToggleSurface.kt / ListSelectableItem.kt · 布局容器
│       │   │   │       ├── Input.kt / TextArea.kt / Select.kt / MultiSelect.kt / Switch.kt · 表单
│       │   │   │       ├── ConfirmDialog.kt / ImagePreviewDialog.kt / ShareSheet.kt · 弹窗
│       │   │   │       ├── BottomCreateToolbar.kt / StickyHeader.kt · 导航/吸顶
│       │   │   │       ├── DotLoading.kt / ChainOfThought.kt · 加载/思考
│       │   │   │       ├── JsonTree.kt / ViewText.kt / QRCode.kt / DataTable.kt · 展示
│       │   │   │       ├── Tag.kt / TagList.kt / ErrorCard.kt / Greeting.kt · 杂项
│       │   │   │       ├── FloatingWindow.kt / KeepScreenOn.kt / TTSController.kt · 系统能力
│       │   │   │       ├── BackupReminderCard.kt / UpdateCard.kt · 提醒卡片
│       │   │   │       ├── Tooltip.kt / Export.kt / Favicon.kt · 工具
│       │   │   │       ├── icons/    · 自定义矢量图标（Heart/Discord/腾讯 QQ/Reasoning）
│       │   │   │       └── permission/ · 运行时权限（PermissionManager + 状态 + 弹窗 + README）
│       │   │   ├── pages/            · 页面（一功能一页）
│       │   │   │   ├── chat/         · 聊天核心
│       │   │   │   │   ├── ChatPage.kt · 聊天主页
│       │   │   │   │   ├── ChatVM.kt · 聊天 VM
│       │   │   │   │   ├── ChatList.kt · 消息列表
│       │   │   │   │   ├── ChatDrawer.kt / ChatDrawerVM.kt · 侧边栏抽屉
│       │   │   │   │   ├── ConversationList.kt · 会话列表
│       │   │   │   │   ├── ConversationSystemPromptCard.kt · 系统提示词卡片
│       │   │   │   │   ├── Background.kt / MeshGradientBackground.kt · 背景/网格渐变
│       │   │   │   │   ├── ChatSizeChecker.kt · 尺寸响应
│       │   │   │   │   ├── Export.kt  · 聊天导出
│       │   │   │   │   └── TTSAutoPlay.kt · TTS 自动播放
│       │   │   │   ├── assistant/    · 助手管理
│       │   │   │   │   ├── AssistantPage.kt / AssistantVM.kt · 助手列表
│       │   │   │   │   ├── AssistantExportDialog.kt · 导出弹窗
│       │   │   │   │   └── detail/   · 助手详情（多页签）
│       │   │   │   │       ├── AssistantDetailPage.kt / AssistantDetailVM.kt · 详情壳
│       │   │   │   │       ├── AssistantBasicPage.kt · 基础信息
│       │   │   │   │       ├── AssistantPromptPage.kt · 提示词
│       │   │   │   │       ├── AssistantRequestPage.kt · 请求参数
│       │   │   │   │       ├── AssistantAlternateGreetingsPage.kt · 备选开场白
│       │   │   │   │       ├── AssistantMemoryPage.kt · 记忆
│       │   │   │   │       ├── AssistantMcpPage.kt · MCP
│       │   │   │   │       ├── AssistantLocalToolPage.kt · 本地工具
│       │   │   │   │       ├── AssistantWorkspacePage.kt · 工作区
│       │   │   │   │       ├── AssistantTavernBindingsPage.kt · Tavern 绑定
│       │   │   │   │       ├── AssistantExtensionsPage.kt · 扩展
│       │   │   │   │       ├── AssistantCardSubPages.kt · 角色卡子页
│       │   │   │   │       ├── AssistantImporter.kt · 导入
│       │   │   │   │       ├── BackgroundPicker.kt · 背景选择
│       │   │   │   │       └── PropertyEditor.kt · 属性编辑
│       │   │   │   ├── setting/      · 设置（多页）
│       │   │   │   │   ├── SettingPage.kt / SettingVM.kt · 设置主页
│       │   │   │   │   ├── SettingModelPage.kt / SettingModelPromptPage.kt · 模型 / 默认模型提示词
│       │   │   │   │   ├── SettingProviderPage.kt / SettingProviderDetailPage.kt · 提供商（列表/详情）
│       │   │   │   │   ├── SettingSearchPage.kt / SettingSearchDetailPage.kt · 搜索服务
│       │   │   │   │   ├── SettingSpeechPage.kt · 语音（TTS/ASR）
│       │   │   │   │   ├── SettingMcpPage.kt · MCP
│       │   │   │   │   ├── SettingKnowledgeBasePage.kt / SettingKnowledgeBaseDetailPage.kt · 知识库
│       │   │   │   │   ├── SettingFilesPage.kt · 文件
│       │   │   │   │   ├── SettingThemePage.kt · 主题
│       │   │   │   │   ├── SettingPreferencesPage.kt / General / Notification / UIPage / ThemePage · 偏好（分类）
│       │   │   │   │   ├── SettingAboutPage.kt · 关于
│       │   │   │   │   ├── UserPersonaListPage.kt / UserPersonaListVM.kt / UserPersonaDetailPage.kt · 用户人设
│       │   │   │   │   └── components/ · 设置页私有子组件
│       │   │   │   │       ├── ProviderConfigure.kt · 提供商配置表单
│       │   │   │   │       ├── ProviderConnectionTester.kt · 连接测试
│       │   │   │   │       ├── TTSProviderConfigure.kt / ASRProviderConfigure.kt · 语音提供商配置
│       │   │   │   │       ├── BalanceOption.kt · 余额选项
│       │   │   │   │       ├── PresetThemeButton.kt / CustomThemeButton.kt · 主题按钮
│       │   │   │   │       └── …
│       │   │   │   ├── backup/       · 备份
│       │   │   │   │   ├── BackupPage.kt / BackupVM.kt · 备份页
│       │   │   │   │   ├── components/BackupDialog.kt · 备份弹窗
│       │   │   │   │   └── tabs/ImportExportTab.kt / ReminderTab.kt · 导入导出/提醒页签
│       │   │   │   ├── extensions/   · 扩展
│       │   │   │   │   ├── ExtensionsPage.kt · 扩展列表
│       │   │   │   │   ├── SkillsPage.kt / SkillsVM.kt · 技能
│       │   │   │   │   ├── SkillDetailPage.kt / SkillDetailVM.kt · 技能详情
│       │   │   │   │   └── QuickMessagesPage.kt / QuickMessagesVM.kt · 快捷消息
│       │   │   │   ├── workspace/    · 工作区（Proot/Linux 终端）
│       │   │   │   │   ├── WorkspacePage.kt / WorkspaceVM.kt · 工作区主页
│       │   │   │   │   ├── WorkspaceDetailPage.kt / WorkspaceDetailVM.kt · 工作区详情
│       │   │   │   │   ├── WorkspaceFileEditorPage.kt / WorkspaceFileType.kt · 文件编辑
│       │   │   │   │   ├── WorkspaceTerminalPage.kt / WorkspaceTerminalSession.kt · 终端
│       │   │   │   │   └── … 
│       │   │   │   ├── search/       · SearchPage.kt / SearchVM.kt（搜索）
│       │   │   │   ├── history/      · HistoryPage.kt / HistoryVM.kt（历史）
│       │   │   │   ├── favorite/     · FavoritePage.kt / FavoriteVM.kt（收藏）
│       │   │   │   ├── translator/   · TranslatorPage.kt / TranslatorVM.kt（翻译）
│       │   │   │   ├── imggen/       · ImgGenPage.kt / ImgGenVM.kt（图片生成）
│       │   │   │   ├── stats/        · StatsPage.kt / StatsVM.kt（统计）
│       │   │   │   ├── debug/        · DebugPage.kt / DebugVM.kt（调试）
│       │   │   │   ├── developer/    · DeveloperPage.kt / DeveloperVM.kt（开发者）
│       │   │   │   ├── log/          · LogPage.kt（日志）
│       │   │   │   ├── webview/      · WebViewPage.kt（内嵌网页）
│       │   │   │   └── share/        · 系统分享
│       │   │   │       └── handler/ShareHandlerPage.kt / ShareHandlerVM.kt · 分享处理
│       │   │   └── (计划) …
│       │   └── utils/                · 工具类（21 个）
│       │       ├── ChatUtil.kt / MarkdownUtils.kt / EmojiUtils.kt · 聊天/Markdown/表情
│       │       ├── AIIconMatcher.kt · AI 图标匹配
│       │       ├── CacheUtil.kt / ClipboardUtil.kt · 缓存/剪贴板
│       │       ├── CollectionUtils.kt / StringUtils.kt / Json.kt · 集合/字符串/JSON
│       │       ├── ComposeExt.kt / ContextUtil.kt · Compose/上下文扩展
│       │       ├── CoroutineUtils.kt · 协程工具
│       │       ├── CrashHandler.kt · 崩溃处理
│       │       ├── DatabaseUtil.kt · 数据库工具
│       │       ├── ImageUtils.kt · 图片工具
│       │       ├── NotificationUtil.kt · 通知
│       │       ├── PlayStoreUtil.kt / UpdateChecker.kt · 商店/更新检查
│       │       ├── SoundEffectPlayer.kt · 音效
│       │       ├── TimeUtil.kt · 时间
│       │       └── UiState.kt · UI 状态基类
│       ├── res/                      · 静态资源
│       │   ├── drawable(-nodpi)/      · 矢量图标 + 品牌 logo PNG（约 22 个品牌）
│       │   ├── font/jetbrains_mono.ttf · 等宽字体
│       │   ├── mipmap-*/             · 启动图标（自适应）
│       │   ├── values/               · strings / colors / themes（六语言）
│       │   ├── xml/                  · backup_rules / file_paths / shortcuts
│       │   └── raw/                  · asr_start.mp3 / asr_stop.mp3（语音提示音）
│       └── …
│
├── ai/                              · ② AI 提供商抽象层（模型/提供商/流式/工具，无 UI）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/ai/
│       ├── core/                    · 核心类型
│       │   ├── MessageRole.kt       · 消息角色
│       │   ├── Reasoning.kt         · 推理
│       │   ├── Tool.kt              · 工具定义
│       │   └── Usage.kt             · 用量统计
│       ├── provider/                · 提供商框架
│       │   ├── Provider.kt          · 提供商抽象
│       │   ├── ProviderManager.kt   · 提供商管理器
│       │   ├── ProviderSetting.kt   · 提供商设置
│       │   ├── Model.kt             · 模型定义
│       │   ├── ParamDef.kt          · 参数定义
│       │   └── providers/           · 具体提供商实现
│       │       ├── OpenAIProvider.kt · OpenAI（含 openai/ 子包）
│       │       ├── openai/          · OpenAI 双 API
│       │       │   ├── OpenAIImpl.kt · OpenAI 实现
│       │       │   ├── ChatCompletionsAPI.kt · Chat Completions 端点
│       │       │   ├── ResponseAPI.kt · Responses 端点
│       │       │   └── ProviderMessageUtils.kt · 消息转换
│       │       ├── ClaudeProvider.kt · Anthropic Claude
│       │       ├── GoogleProvider.kt · Google Gemini
│       │       └── vertex/ServiceAccountTokenProvider.kt · Vertex AI 服务账号令牌
│       ├── registry/                · 模型注册表
│       │   ├── ModelRegistry.kt     · 模型注册（内置全部厂商模型）
│       │   └── ModelDsl.kt          · 模型注册 DSL
│       ├── ui/                      · UI 无关的数据包装
│       │   ├── Message.kt           · 消息
│       │   └── Image.kt             · 图片
│       └── util/                    · 工具
│           ├── Request.kt / SSE.kt  · 请求 / SSE 流解析
│           ├── Json.kt / Serializer.kt · JSON / 序列化
│           ├── KeyRoulette.kt       · API Key 轮询
│           ├── FileEncoder.kt       · 文件编码
│           └── ErrorParser.kt       · 错误解析
│
├── common/                          · ③ 基础工具模块（缓存 + HTTP + Android 工具，无业务）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/common/
│       ├── android/                 ·  Android 工具
│       │   ├── ContextUtil.kt       · 上下文工具
│       │   └── Logging.kt           · 日志
│       ├── cache/                   · 磁盘缓存框架
│       │   ├── CacheStore.kt        · 缓存存储接口
│       │   ├── CacheEntry.kt        · 缓存条目
│       │   ├── KeyCodec.kt          · Key 编码
│       │   ├── LruCache.kt          · LRU 淘汰
│       │   ├── FileIO.kt            · 文件 IO
│       │   ├── PerKeyFileCacheStore.kt · 每键单文件缓存
│       │   └── SingleFileCacheStore.kt · 单文件缓存
│       └── http/                    · HTTP 工具
│           ├── Request.kt           · 请求
│           ├── SSE.kt               · SSE
│           ├── Json.kt / JsonExpression.kt · JSON / JSON 表达式
│           └── AcceptLang.kt        · Accept-Language 处理
│
├── core/                            · ④ 核心模块（两个子模块）
│   ├── tavern/                      · Tavern（酒馆）底层库：宏/正则/预设/世界书
│   │   └── src/main/java/cn/mine/minestars/core/tavern/
│   │       ├── macro/               · Tavern 宏引擎（{{…}} 求值）
│   │       │   ├── Engine.kt        · 宏引擎
│   │       │   ├── Parser.kt        · 宏解析
│   │       │   ├── Executor.kt      · 宏执行
│   │       │   ├── FieldResolver.kt · 字段解析
│   │       │   ├── Context.kt       · 上下文
│   │       │   ├── BuiltinSupport.kt · 内置宏支持
│   │       │   ├── TavernMacroResolver.kt · Tavern 宏解析器
│   │       │   ├── TemplateFieldBag.kt · 模板字段包
│   │       │   ├── Vars.kt / Random.kt / Time.kt / Token.kt · 变量/随机/时间/Token 宏
│   │       │   └── …
│   │       ├── regex/RegexSupport.kt · 正则支持
│   │       ├── worldbook/           · 世界书
│   │       │   ├── Activator.kt     · 触发
│   │       │   ├── Parser.kt        · 解析
│   │       │   ├── MessageSupport.kt · 消息支持
│   │       │   └── WorldBookSupport.kt · 世界书支持
│   │       ├── preset/              · 预设
│   │       │   ├── DefaultPresetFactory.kt · 默认预设工厂
│   │       │   ├── PresetModelParams.kt · 参数模型
│   │       │   └── PresetParser.kt  · 解析
│   │       ├── mapper/CharacterImportMapper.kt · 角色导入映射
│   │       └── budget/TokenBudget.kt · Token 预算
│   └── workspace/                   · 工作区（Proot + 终端模拟器，含 JNI）
│       └── src/main/
│           ├── cpp/                · JNI 原生（termux_pty.cpp / workspace.cpp / CMakeLists.txt）
│           ├── jniLibs/            · 预编译 .so（arm64 / x86_64）
│           └── java/
│               ├── cn/mine/minestars/core/workspace/
│               │   ├── Workspace.kt / WorkspaceManager.kt · 工作区/管理
│               │   ├── WorkspaceFileSystem.kt · 文件系统
│               │   ├── RootfsInstaller.kt / RootfsPatcher.kt · rootfs 安装/补丁
│               │   ├── ProotShellRunner.kt / WorkspaceShellRunner.kt · Proot/Shell 运行
│               │   └── …
│               └── com/termux/      · Termux 终端模拟器（terminal + view + textselection）
│                   ├── terminal/    · 终端模拟器内核（TerminalEmulator/Session/Buffer…）
│                   ├── view/        · 终端视图（TerminalView/Renderer…）
│                   └── view/textselection/ · 文本选择控点
│
├── document/                        · ⑤ 文档解析模块（DOCX/EPUB/PPTX/PDF）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/
│       ├── java/cn/mine/document/   · 解析器（统一 → 文本/结构化）
│       │   ├── DocxParser.kt        · Word
│       │   ├── EpubParser.kt        · EPUB
│       │   ├── PptxParser.kt        · PowerPoint
│       │   └── PdfParser.kt         · PDF（走 MuPDF 绑定）
│       ├── java/com/artifex/mupdf/fitz/ · MuPDF JNI 绑定（约 90 个 Java 类）[不逐一列举·见 CLAUDE.md]
│       └── jniLibs/                 · libmupdf_java.so（arm64 / x86_64）
│
├── highlight/                       · ⑥ 代码高亮引擎（Kotlin 版 highlight.js，无 Compose）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/highlight/
│       ├── Highlighter.kt / HighlighterPreview.kt · 高亮入口 / 预览
│       ├── HighlightToken.kt / HighlightStyle.kt · Token / 样式
│       ├── core/                    · 引擎核心（Mode/ModeCompiler/MultiRegex/Regexes/Keywords/CommonModes/TokenEmitter/HighlightEngine）
│       └── languages/               · 30 种语言（bash/c/cpp/csharp/css/dart/diff/dockerfile/glsl/go/ini/java/javascript/json/kotlin/latex/lua/markdown/php/powershell/properties/python/ruby/rust/sql/swift/typescript/xml/yaml… + Languages.kt 注册表）[不逐一列举]
│
├── material3/                       · ⑦ Material3 扩展 + color-utilities（子模块）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   ├── src/main/java/cn/mine/material3/DynamicSchemeExt.kt · DynamicColorScheme 扩展
│   └── material-color-utilities/    · Google material-color-utilities（git 子模块，多语言实现）
│
├── rag/                             · ⑧ RAG 模块（检索增强，`RAG/` 目录）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/minestars/rag/
│       ├── EmbeddingService.kt      · 向量嵌入服务
│       ├── KnowledgeBase.kt         · 知识库
│       ├── TextChunker.kt           · 文本分块
│       └── db/                      · 向量数据库（Room）
│           ├── RagDatabase.kt       · RAG 数据库
│           ├── EmbeddingEntity.kt   · 嵌入实体
│           └── EmbeddingDAO.kt      · 嵌入 DAO
│
├── search/                          · ⑨ 联网搜索模块（18 个搜索服务后端）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/search/
│       ├── SearchService.kt          · 搜索服务抽象
│       ├── MineChatSearchService.kt  · 官方搜索
│       ├── Tavily/SearXNG/Bocha/Metaso/Bing/Brave/Zhipu/Perplexity/Grok/Exa/Jina/LinkUp/Firecrawl/Ollama/CustomJs/Tinyfish… · 第三方搜索（各一个文件）
│       └── …（18 个服务文件）
│   └── res/values-*/                · 本地化（zh/en/ja/ko/ru/zh-rTW）
│
├── speech/                          · ⑩ 语音模块（ASR 语音识别 + TTS 语音合成）
│   ├── build.gradle.kts             · [构建配置·不展开]
│   └── src/main/java/cn/mine/
│       ├── asr/                     · 语音识别
│       │   ├── ASRController.kt / ASRState.kt / ASRProviderSetting.kt · ASR 控制器/状态/设置
│       │   ├── AudioAmplitude.kt    · 音频振幅
│       │   └── providers/           · 提供商实现
│       │       ├── OpenAIRealtimeASRController.kt · OpenAI 实时
│       │       ├── DashScopeASRController.kt · 阿里 DashScope
│       │       └── VolcengineASRController.kt · 火山引擎
│       └── tts/                     · 语音合成
│           ├── provider/            · TTS 框架
│           │   ├── TTSProvider.kt / TTSManager.kt / TTSProviderSetting.kt · 抽象/管理/设置
│           │   └── providers/       · 8 个提供商
│           │       ├── OpenAITTSProvider.kt / GeminiTTSProvider.kt / GroqTTSProvider.kt · OpenAI/Gemini/Groq
│           │       ├── MiniMaxTTSProvider.kt / MiMoTTSProvider.kt / QwenTTSProvider.kt · MiniMax/MiMo/Qwen
│           │       ├── XAITTSProvider.kt · xAI
│           │       └── SystemTTSProvider.kt · 系统 TTS
│           ├── controller/          · 播放控制
│           │   ├── TtsController.kt / TtsSynthesizer.kt · 控制器/合成器
│           │   ├── AudioPlayer.kt / TextChunker.kt · 播放器/长文本分块
│           └── model/               · PlaybackState.kt / TTSRequest.kt / TTSResponse.kt · 模型
│
├── locale-tui/                      · ⑪ 本地化终端工具（Python Textual TUI）
│   ├── pyproject.toml / config.yml / uv.lock · 配置
│   ├── CLAUDE.md / README.md        · 说明
│   └── src/
│       ├── app.py / main.py / config.py · 入口/配置
│       ├── models/entry.py          · 翻译条目模型
│       ├── screens/                 · 界面（module_select / translation_table）
│       ├── services/                · xml_parser / translator / dead_entry_finder
│       ├── widgets/edit_modal.py    · 编辑弹窗
│       └── styles/app.tcss          · Textual 样式
│
├── design/                          · 外来物仓库（设计草稿，不进主模块）
│   ├── filetree/                    · 文件树文档（本目录）
│   │   └── FILETREE.md              · 本文件（项目文件树）
│   └── …                            · 其它设计素材
│
├── docs/                            · 文档（截图 + 图标）
│   ├── icon.png / donate.png        · 图标 / 捐赠图
│   └── img/                         · 功能截图（chat/assistants/models/providers/desktop）
│
├── tts/                             · （空目录·遗留，TTS 已并入 speech 模块）
│
├── .claude/                         · Claude Code 配置（agents/commands/memory/skills/worktrees）
├── .agents/                         · Agent 技能
├── .gradle/ .kotlin/ build/ node_modules/ · 构建产物/依赖缓存 [不展开]
│
└── (构建根) build.gradle.kts / gradle.properties / settings.gradle.kts / gradlew · 见顶部根级