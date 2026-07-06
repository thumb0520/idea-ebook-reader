# 📖 BookWorm — IDEA 电子书阅读器

一个轻量级的 IntelliJ IDEA 插件，在底部 Tool Window 中嵌入电子书阅读器，让你在编码间隙随时阅读，无需离开 IDE。

## ✨ 功能特性

- **多格式支持** — 支持 TXT、EPUB、MOBI 三种常见电子书格式
- **连续滚动阅读** — 章节之间无缝衔接，自动加载上下章，体验如同主流阅读 App
- **阅读进度记忆** — 自动保存上次阅读位置（章节 + 段落偏移），重新打开自动恢复
- **章节导航** — 侧栏章节列表，一键跳转到任意章节
- **字体调节** — 支持 A-/A+ 调整字体大小，适配不同阅读习惯
- **主题适配** — 自动跟随 IDE 深色/浅色主题
- **快捷键支持** — 方向键翻页、切换章节，操作流畅

## 安装

### 方法一：使用 IDEA 直接构建（推荐）

1. 用 IntelliJ IDEA 打开项目目录
2. IDEA 会自动下载 Gradle 和依赖
3. 等待索引完成
4. 运行 Gradle 任务：右侧 Gradle 面板 → Tasks → intellij → buildPlugin
5. 插件生成在 `build/distributions/` 目录

### 方法二：命令行构建

```bash
cd ~/workspace/idea-ebook-reader
./gradlew buildPlugin
```

### 安装插件

- Settings → Plugins → Install Plugin from Disk
- 选择 `build/distributions/idea-ebook-reader-1.0.0.zip`

## 🚀 使用方法

1. 打开底部工具栏的 **BookWorm** 窗口
2. 点击 📂 按钮选择电子书文件（支持 .txt / .epub / .mobi）
3. **上下滚动即可连续阅读**，章节之间会自动加载，无需手动翻章
4. 也可使用 ◀ ▶ 按钮或左右键快速跳转章节
5. 点击 ☰ 显示章节列表，快速跳转到指定章节
6. 使用 A- A+ 调节字体大小

## ⌨️ 快捷键

| 快捷键 | 功能 |
|--------|------|
| `↑` / `↓` | 上下翻页 |
| `←` / `→` | 上一章 / 下一章 |

## 📝 注意事项

- 背景色自动跟随 IDE 深色/浅色主题
- 阅读进度自动保存，重新打开书籍恢复到上次位置
- 适合在编码间隙、等待编译时随手翻阅技术书籍或小说
