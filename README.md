# MineChat（编译用 Fork）

> 本仓库是 [Mr060805/MineChat](https://github.com/Mr060805/MineChat) 的 fork，主要用途：**同步上游源码并编译产出 APK**，不包含任何上游之外的功能改动。

## 项目简介

MineChat 是一款安卓端 AI 应用，使用 Kotlin + Jetpack Compose 开发，兼容酒馆（SillyTavern）角色卡、预设与世界书。

- 上游仓库：[Mr060805/MineChat](https://github.com/Mr060805/MineChat)
- 技术栈：Kotlin / Jetpack Compose / Gradle 多模块（app、ai、RAG、core、common、design、search、speech 等）

## 本仓库用途

1. **镜像上游**：与上游保持同步，仅用于拉取源码
2. **编译构建**：拉取后直接跑 Gradle 构建，产出 APK 供自用/测试
3. 不用于商业分发，不修改上游功能逻辑

## 同步上游

```bash
git remote add upstream https://github.com/Mr060805/MineChat.git
git fetch upstream
git merge upstream/main
```

## 编译构建

环境要求：JDK 17+、Android SDK。

```bash
# 初始化子模块（material-color-utilities）
git submodule update --init --recursive

# Debug 包
./gradlew :app:assembleDebug

# Release 包
./gradlew :app:assembleRelease
```

APK 输出目录：`app/build/outputs/apk/`

## 许可

上游采用**分段双重许可（Segmented Dual Licensing）**：

- 非商业 / 个人 / 教育 / 研究 / 不超过 10 人组织：**GNU AGPL v3**
- 商业用途或用户超过 10 人：需联系上游获取商业授权（re_dev@qq.com）

本 fork 遵守上游许可条款，仅用于个人编译与测试。
