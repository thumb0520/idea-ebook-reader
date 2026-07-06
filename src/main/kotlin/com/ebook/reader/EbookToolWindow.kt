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

        val scrollPane = JBScrollPane(contentArea)
        scrollPane.border = JBUI.Borders.empty()

        // Chapter list (hidden by default, shown on right-click)
        chapterList.model = chapterModel
        chapterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        chapterList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val index = chapterList.selectedIndex
                if (index >= 0) {
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

        val chapterLabel = JLabel("No book loaded")
        chapterLabel.foreground = UIManager.getColor("Label.foreground")
        chapterLabel.font = Font("Dialog", Font.PLAIN, 11)

        prevButton.addActionListener { prevChapter() }
        nextButton.addActionListener { nextChapter() }
        chapterButton.addActionListener { toggleChapterList() }
        openButton.addActionListener { openFile() }
        fontSmaller.addActionListener { changeFontSize(-1) }
        fontLarger.addActionListener { changeFontSize(1) }

        navPanel.add(openButton)
        navPanel.add(chapterButton)
        navPanel.add(prevButton)
        navPanel.add(chapterLabel)
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
        contentArea.getInputMap().put(KeyStroke.getKeyStroke("PAGE_UP"), "prevChapter")
        contentArea.actionMap.put("prevChapter", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) = prevChapter()
        })
        contentArea.getInputMap().put(KeyStroke.getKeyStroke("PAGE_DOWN"), "nextChapter")
        contentArea.actionMap.put("nextChapter", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) = nextChapter()
        })

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

    private fun showChapter(index: Int) {
        val book = bookContent ?: return
        if (index !in book.chapters.indices) return

        currentChapter = index
        contentArea.text = book.chapters[index].content
        contentArea.caretPosition = 0
        chapterList.selectedIndex = index

        // Update label
        val navPanel = (mainPanel.layout as BorderLayout).getLayoutComponent(BorderLayout.NORTH) as JPanel
        val label = navPanel.components.filterIsInstance<JLabel>().firstOrNull()
        label?.text = "${index + 1}/${book.chapters.size}"
    }

    private fun prevChapter() {
        if (currentChapter > 0) showChapter(currentChapter - 1)
    }

    private fun nextChapter() {
        val book = bookContent ?: return
        if (currentChapter < book.chapters.size - 1) showChapter(currentChapter + 1)
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

    private fun saveLastBookPath(path: String) {
        val properties = project.getService(EbookSettings::class.java).state
        properties.lastBookPath = path
        properties.lastChapter = currentChapter
    }

    private fun loadLastBook() {
        val properties = project.getService(EbookSettings::class.java).state
        val path = properties.lastBookPath
        if (path != null) {
            val file = File(path)
            if (file.exists()) {
                loadBook(file)
                val chapter = properties.lastChapter
                if (chapter > 0) showChapter(chapter)
            }
        }
    }
}
