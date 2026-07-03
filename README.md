# IDEA 摸鱼电子书阅读器

一个隐秘的 IntelliJ IDEA 插件，在 Terminal 区域显示电子书阅读器。

## 功能特性

- 支持 txt、epub、mobi 格式电子书
- 在底部 Tool Window 显示，伪装为 "Output"
- 快捷键切换显示/隐藏 (Command+P)
- 章节导航
- 字体大小调节
- 自动记住上次阅读位置

## 安装

### 方法一：使用 IDEA 直接构建（推荐）

1. 用 IntelliJ IDEA 打开 `~/workspace/idea-ebook-reader` 目录
2. IDEA 会自动下载 Gradle 和依赖
3. 等待索引完成
4. 运行 Gradle 任务：右侧 Gradle 面板 → Tasks → intellij → buildPlugin
5. 插件生成在 `build/distributions/` 目录

### 方法二：命令行构建

1. 需要先安装 Gradle 或使用 Gradle Wrapper
2. 下载 gradle-wrapper.jar：
   ```bash
   cd ~/workspace/idea-ebook-reader
   curl -L -o gradle/wrapper/gradle-wrapper.jar https://services.gradle.org/distributions/gradle-8.2-bin.zip
   ```
3. 运行构建：
   ```bash
   ./gradlew buildPlugin
   ```

### 安装插件

- Settings → Plugins → Install Plugin from Disk
- 选择 `build/distributions/idea-ebook-reader-1.0.0.zip`

## 使用

1. 按 `Command+P` 显示/隐藏阅读器
2. 点击 📂 按钮打开电子书文件
3. 使用 ◀ ▶ 翻页，或按 PageUp/PageDown
4. 点击 ☰ 显示章节列表
5. 使用 A- A+ 调节字体大小

## 自定义快捷键

如需修改快捷键：
1. Settings → Keymap
2. 搜索 "Toggle Output Viewer"
3. 右键添加快捷键

## 注意事项

- 插件伪装为 "Output" 窗口，不显眼
- 背景色与 Terminal 一致
- 阅读进度自动保存
