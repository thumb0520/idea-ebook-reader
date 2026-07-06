package com.ebook.reader

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

class EbookToolWindow(private val project: Project, private val toolWindow: ToolWindow) {
    private val mainPanel = JPanel(BorderLayout())
    private val contentArea = JTextArea()
    private val chapterList = JList<String>()
    private val chapterModel = DefaultListModel<String>()
    private var bookContent: BookContent? = null
    private var currentChapter = 0
    private lateinit var scrollPane: JBScrollPane

    // 连续滚动模式：记录当前显示的章节范围和各章在文档中的位置
    private var firstVisibleChapter = 0
    private var lastVisibleChapter = 0
    // 各章起始位置在 contentArea document 中的 offset
    private val chapterOffsets = mutableListOf<Int>()

    init {
        setupUI()
    }

    fun getContent(): JComponent = mainPanel

    private fun setupUI() {
        mainPanel.background = UIManager.getColor("Panel.background")
        mainPanel.minimumSize = Dimension(100, 0)
        mainPanel.preferredSize = Dimension(200, 0)

        // Content area - follow IDE theme
        contentArea.isEditable = false
        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true
        contentArea.font = Font("Monospaced", Font.PLAIN, 12)
        contentArea.background = UIManager.getColor("TextArea.background")
        contentArea.foreground = UIManager.getColor("TextArea.foreground")
        contentArea.border = EmptyBorder(5, 10, 5, 10)
        contentArea.caretColor = UIManager.getColor("TextArea.foreground")

        scrollPane = JBScrollPane(contentArea)
        scrollPane.border = JBUI.Borders.empty()

        // Chapter list (hidden by default, shown on right-click)
        chapterList.model = chapterModel
        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val index = chapterList.selectedIndex
                if (index >= 0 && index != currentChapter) {
                    saveReadingPosition()
                    showChapter(index)
                }
            }
        }

        val chapterScroll = JBScrollPane(chapterList)
        chapterScroll.preferredSize = Dimension(150, 0)
        chapterScroll.isVisible = false

        // Navigation panel
        val navPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        navPanel.background = UIManager.getColor("Panel.background")

        val prevButton = createSmallButton("◀")
        val nextButton = createSmallButton("▶")
        val chapterButton = createSmallButton("☰")
        val openButton = createSmallButton("📂")
        val fontSmaller = createSmallButton("A-")
        val fontLarger = createSmallButton("A+")

        prevButton.addActionListener { prevChapter() }
        nextButton.addActionListener { nextChapter() }
        chapterButton.addActionListener { toggleChapterList() }
        openButton.addActionListener { openFile() }
        fontSmaller.addActionListener { changeFontSize(-1) }
        fontLarger.addActionListener { changeFontSize(1) }

        navPanel.add(openButton)
        navPanel.add(chapterButton)
        navPanel.add(prevButton)
        navPanel.add(nextButton)
        navPanel.add(fontSmaller)
        navPanel.add(fontLarger)

        // Split pane
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chapterScroll, scrollPane)
        splitPane.dividerSize = 1
        splitPane.border = JBUI.Borders.empty()

        mainPanel.add(navPanel, BorderLayout.NORTH)
        mainPanel.add(splitPane, BorderLayout.CENTER)

        // Keyboard navigation
        val im = contentArea.getInputMap(JComponent.WHEN_FOCUSED)
        val am = contentArea.actionMap

        // 上下键翻页
        im.put(KeyStroke.getKeyStroke("UP"), "scrollUp")
        am.put("scrollUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                scrollPageUp()
            }
        })
        im.put(KeyStroke.getKeyStroke("DOWN"), "scrollDown")
        am.put("scrollDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                scrollPageDown()
            }
        })

        // 左右键切换章节
        im.put(KeyStroke.getKeyStroke("LEFT"), "prevChapter")
        am.put("prevChapter", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) = prevChapter()
        })
        im.put(KeyStroke.getKeyStroke("RIGHT"), "nextChapter")
        am.put("nextChapter", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) = nextChapter()
        })

        // 滚动时检查是否需要追加/前置章节
        val viewport = scrollPane.viewport
        viewport.addChangeListener {
            checkAppendNextChapter()
            checkPrependPrevChapter()
            updateCurrentChapterFromScroll()
        }

        loadLastBook()
    }

    private fun createSmallButton(text: String): JButton {
        val button = JButton(text)
        button.font = Font("Dialog", Font.PLAIN, 11)
        button.margin = Insets(2, 4, 2, 4)
        button.isFocusPainted = false
        return button
    }

    private fun openFile() {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "E-books (txt, epub, mobi)", "txt", "epub", "mobi"
        )

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            loadBook(fileChooser.selectedFile)
        }
    }

    private fun loadBook(file: File) {
        val parser = ParserFactory.getParser(file.name)
        if (parser == null) {
            contentArea.text = "Unsupported file format"
            return
        }

        try {
            bookContent = parser.parse(file)
            updateChapterList()
            showChapter(0)
            saveLastBookPath(file.absolutePath)
        } catch (e: Exception) {
            contentArea.text = "Error loading book: ${e.message}"
        }
    }

    private fun updateChapterList() {
        chapterModel.clear()
        bookContent?.chapters?.forEach { chapterModel.addElement(it.title) }
    }

    private fun showChapter(index: Int, scrollPosition: Int = 0) {
        val book = bookContent ?: return
        if (index !in book.chapters.indices) return

        currentChapter = index
        firstVisibleChapter = index
        lastVisibleChapter = index
        chapterOffsets.clear()

        // 加载当前章及前后各一章，实现连续滚动
        val sb = StringBuilder()
        val startChapter = (index - 1).coerceAtLeast(0)
        val endChapter = (index + 1).coerceAtMost(book.chapters.size - 1)

        for (i in startChapter..endChapter) {
            chapterOffsets.add(sb.length)
            if (i > startChapter) {
                sb.append("\n\n")
                sb.append("─".repeat(40))
                sb.append("\n\n")
            }
            sb.append(book.chapters[i].content)
        }
        firstVisibleChapter = startChapter
        lastVisibleChapter = endChapter

        contentArea.text = sb.toString()
        // 滚动到目标章节的位置（scrollPosition 是章节内的相对偏移）
        val chapterBase = chapterOffsets[index - startChapter]
        val targetOffset = (chapterBase + scrollPosition).coerceAtMost(contentArea.document.length)
        contentArea.caretPosition = targetOffset
        try {
            contentArea.scrollRectToVisible(contentArea.modelToView(targetOffset))
        } catch (e: Exception) {
            // ignore
        }
        chapterList.selectedIndex = index
    }

    /**
     * 根据文档中的 offset 判断当前阅读的是哪一章
     */
    private fun getChapterAtOffset(offset: Int): Int {
        for (i in chapterOffsets.indices.reversed()) {
            if (offset >= chapterOffsets[i]) {
                return firstVisibleChapter + i
            }
        }
        return firstVisibleChapter
    }

    /**
     * 检查是否需要追加下一章内容（滚动到底部附近时）
     */
    private fun checkAppendNextChapter() {
        val book = bookContent ?: return
        if (lastVisibleChapter >= book.chapters.size - 1) return

        val viewport = (contentArea.parent as? javax.swing.JViewport) ?: return
        val viewRect = viewport.viewRect
        val docLength = contentArea.document.length

        // 当可见区域底部接近文档末尾时，追加下一章
        try {
            val bottomPos = contentArea.viewToModel(java.awt.Point(viewRect.x, viewRect.y + viewRect.height))
            if (bottomPos > docLength - 200) {
                val nextChapter = lastVisibleChapter + 1
                val separator = "\n\n" + "─".repeat(40) + "\n\n"
                chapterOffsets.add(docLength + separator.length)
                contentArea.document.insertString(docLength, separator + book.chapters[nextChapter].content, null)
                lastVisibleChapter = nextChapter
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * 检查是否需要前置上一章内容（滚动到顶部附近时）
     */
    private fun checkPrependPrevChapter() {
        val book = bookContent ?: return
        if (firstVisibleChapter <= 0) return

        val viewport = (contentArea.parent as? javax.swing.JViewport) ?: return
        val viewRect = viewport.viewRect

        try {
            val topPos = contentArea.viewToModel(java.awt.Point(viewRect.x, viewRect.y))
            if (topPos < 200) {
                val prevChapter = firstVisibleChapter - 1
                val separator = "\n\n" + "─".repeat(40) + "\n\n"
                val insertText = book.chapters[prevChapter].content + separator
                val oldCaret = contentArea.caretPosition
                contentArea.document.insertString(0, insertText, null)
                // 调整所有 offset
                val shift = insertText.length
                for (i in chapterOffsets.indices) {
                    chapterOffsets[i] += shift
                }
                chapterOffsets.add(0, 0)
                firstVisibleChapter = prevChapter
                // 保持阅读位置不动
                contentArea.caretPosition = oldCaret + shift
                try {
                    contentArea.scrollRectToVisible(contentArea.modelToView(oldCaret + shift))
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun saveReadingPosition() {
        val properties = project.getService(EbookSettings::class.java).state
        properties.lastChapter = currentChapter
        // 保存相对于当前章节的阅读位置
        val chapterIdx = currentChapter - firstVisibleChapter
        if (chapterIdx in chapterOffsets.indices) {
            val relativePos = contentArea.caretPosition - chapterOffsets[chapterIdx]
            properties.lastScrollPosition = relativePos.coerceAtLeast(0)
        }
    }

    private fun prevChapter() {
        if (currentChapter > 0) {
            saveReadingPosition()
            showChapter(currentChapter - 1)
        }
    }

    private fun nextChapter() {
        val book = bookContent ?: return
        if (currentChapter < book.chapters.size - 1) {
            saveReadingPosition()
            showChapter(currentChapter + 1)
        }
    }

    private fun toggleChapterList() {
        val splitPane = mainPanel.components.filterIsInstance<JSplitPane>().first()
        val chapterScroll = splitPane.leftComponent
        chapterScroll.isVisible = !chapterScroll.isVisible
        splitPane.dividerLocation = if (chapterScroll.isVisible) 150 else 0
    }

    private fun changeFontSize(delta: Int) {
        val current = contentArea.font
        val newSize = (current.size + delta).coerceIn(8, 24)
        contentArea.font = current.deriveFont(newSize.toFloat())
    }

    private fun scrollPageUp() {
        val viewport = (contentArea.parent as? javax.swing.JViewport) ?: return
        val rect = viewport.viewRect
        val lineHeight = contentArea.getFontMetrics(contentArea.font).height
        val linesPerPage = (rect.height / lineHeight).coerceAtLeast(1)
        val newPos = (contentArea.caretPosition - linesPerPage * 3).coerceAtLeast(0)
        contentArea.caretPosition = newPos
        contentArea.scrollRectToVisible(java.awt.Rectangle(0, newPos, 1, 1))
        checkPrependPrevChapter()
    }

    private fun scrollPageDown() {
        val viewport = (contentArea.parent as? javax.swing.JViewport) ?: return
        val rect = viewport.viewRect
        val lineHeight = contentArea.getFontMetrics(contentArea.font).height
        val linesPerPage = (rect.height / lineHeight).coerceAtLeast(1)
        val newPos = (contentArea.caretPosition + linesPerPage * 3).coerceAtMost(contentArea.document.length)
        contentArea.caretPosition = newPos
        contentArea.scrollRectToVisible(java.awt.Rectangle(0, newPos, 1, 1))
        checkAppendNextChapter()
    }

    /**
     * 滚动时更新当前章节索引（用于章节列表高亮）
     */
    private fun updateCurrentChapterFromScroll() {
        if (chapterOffsets.isEmpty()) return
        val viewport = (contentArea.parent as? javax.swing.JViewport) ?: return
        val viewRect = viewport.viewRect
        try {
            val midPos = contentArea.viewToModel(java.awt.Point(viewRect.x, viewRect.y + viewRect.height / 2))
            val chapter = getChapterAtOffset(midPos)
            if (chapter != currentChapter) {
                currentChapter = chapter
                chapterList.selectedIndex = chapter
                saveReadingPosition()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun saveLastBookPath(path: String) {
        val properties = project.getService(EbookSettings::class.java).state
        properties.lastBookPath = path
        properties.lastChapter = currentChapter
        properties.lastScrollPosition = contentArea.caretPosition
    }

    private fun loadLastBook() {
        val properties = project.getService(EbookSettings::class.java).state
        val path = properties.lastBookPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                val parser = ParserFactory.getParser(file.name)
                if (parser == null) {
                    contentArea.text = "Unsupported file format"
                    return
                }

                try {
                    bookContent = parser.parse(file)
                    updateChapterList()
                    val chapter = properties.lastChapter.coerceIn(0, (bookContent?.chapters?.size ?: 1) - 1)
                    val scrollPos = properties.lastScrollPosition.coerceIn(0, Int.MAX_VALUE)
                    showChapter(chapter, scrollPos)
                    saveLastBookPath(file.absolutePath)
                } catch (e: Exception) {
                    contentArea.text = "Error loading book: ${e.message}"
                }
            }
        }
    }
}
