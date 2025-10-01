package dev.potik.milvus.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.potik.milvus.core.MilvusConnectionService
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JDialog
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class MilvusCollectionEditor(
    private val project: Project,
    private val virtualFile: MilvusCollectionVirtualFile
) : UserDataHolderBase(), FileEditor {

    private val connectionService = MilvusConnectionService.instance()
    private val rootPanel = JPanel(BorderLayout())
    private val loadingPanel = JBLoadingPanel(BorderLayout(), this)
    
    private var currentPage = 0
    private val pageSize = 100
    private var totalRecords = 0L
    private val totalPages: Int get() = ((totalRecords + pageSize - 1) / pageSize).toInt()
    
    private val paginationPanel = JPanel(FlowLayout())
    private val firstButton = JButton("First")
    private val prevButton = JButton("Previous")
    private val nextButton = JButton("Next")
    private val lastButton = JButton("Last")
    private val pageLabel = JLabel("Page 1 of 1")
    
    private val tableModel = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel)

    init {
        initializeUI()
        setupPagination()
        loadTotalRecords()
    }

    private fun initializeUI() {
        rootPanel.border = JBUI.Borders.empty(8)
        
        val headerPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(8)
            add(JLabel("Collection: ${virtualFile.getCollectionName()}"), BorderLayout.WEST)
        }
        
        table.autoCreateRowSorter = true
        table.tableHeader.reorderingAllowed = false
        
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = table.rowAtPoint(e.point)
                    val col = table.columnAtPoint(e.point)
                    if (row >= 0 && col >= 0) {
                        val value = table.getValueAt(row, col)
                        if (isDictOrObjectData(value)) {
                            showExpandedDataDialog(value)
                        }
                    }
                }
            }
        })
        
        val scrollPane = JBScrollPane(table)
        
        loadingPanel.add(headerPanel, BorderLayout.NORTH)
        loadingPanel.add(scrollPane, BorderLayout.CENTER)
        loadingPanel.add(paginationPanel, BorderLayout.SOUTH)
        
        rootPanel.add(loadingPanel, BorderLayout.CENTER)
    }

    private fun loadTotalRecords() {
        val collectionName = virtualFile.getCollectionName()
        
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            connectionService.getCollectionCount(collectionName)
                .whenComplete { count, throwable ->
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        if (throwable == null && count != null) {
                            totalRecords = count
                            updatePaginationControls()
                            loadCollectionData()
                        } else {
                            showError("Failed to get record count: ${throwable?.message}")
                        }
                    }
                }
        }
    }
    
    private fun loadCollectionData() {
        loadingPanel.startLoading()
        
        val collectionName = virtualFile.getCollectionName()
        val offset = currentPage * pageSize
        
        // Run in background thread to avoid blocking UI
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            connectionService.previewCollectionPaginated(collectionName, offset, pageSize)
                .whenComplete { preview, throwable ->
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        loadingPanel.stopLoading()
                        
                        if (throwable != null) {
                            showError("Failed to load collection data: ${throwable.message}")
                            return@invokeLater
                        }
                        
                        if (preview != null) {
                            updateTable(preview)
                        }
                    }
                }
        }
    }

    private fun updateTable(preview: MilvusConnectionService.CollectionPreview) {
        val fieldNames = preview.schema.fields.map { it.name }.toTypedArray()
        tableModel.setColumnIdentifiers(fieldNames)
        tableModel.setRowCount(0)
        
        preview.rows.forEach { row ->
            val values = preview.schema.fields.map { field -> 
                formatValue(row[field.name])
            }.toTypedArray()
            tableModel.addRow(values)
        }
        
        table.revalidate()
        table.repaint()
    }

    private fun formatValue(value: Any?): Any? {
        return when (value) {
            null -> null
            is FloatArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is DoubleArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is IntArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is LongArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is BooleanArray -> value.joinToString(prefix = "[", postfix = "]") { it.toString() }
            is ByteArray -> value.joinToString(prefix = "[", postfix = "]") { (it.toInt() and 0xFF).toString() }
            is List<*> -> formatList(value, expanded = false)
            is Array<*> -> formatList(value.toList(), expanded = false)
            else -> value
        }
    }


    private fun showError(message: String) {
        rootPanel.removeAll()
        rootPanel.add(JLabel(message), BorderLayout.CENTER)
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    override fun getComponent(): JComponent = rootPanel

    override fun getPreferredFocusedComponent(): JComponent? = table

    override fun getName(): String = "Milvus Collection"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun getCurrentLocation(): FileEditorLocation? = null

    private fun setupPagination() {
        paginationPanel.add(firstButton)
        paginationPanel.add(prevButton)
        paginationPanel.add(pageLabel)
        paginationPanel.add(nextButton)
        paginationPanel.add(lastButton)
        
        firstButton.addActionListener { goToPage(0) }
        prevButton.addActionListener { goToPage(currentPage - 1) }
        nextButton.addActionListener { goToPage(currentPage + 1) }
        lastButton.addActionListener { goToPage(totalPages - 1) }
        
        updatePaginationControls()
    }
    
    private fun updatePaginationControls() {
        val pageText = if (totalPages > 0) "Page ${currentPage + 1} of $totalPages (${totalRecords} records)" else "No records"
        pageLabel.text = pageText
        
        firstButton.isEnabled = currentPage > 0
        prevButton.isEnabled = currentPage > 0
        nextButton.isEnabled = currentPage < totalPages - 1
        lastButton.isEnabled = currentPage < totalPages - 1
    }
    
    private fun goToPage(page: Int) {
        if (page >= 0 && page < totalPages && page != currentPage) {
            currentPage = page
            updatePaginationControls()
            loadCollectionData()
        }
    }

    private fun isDictOrObjectData(value: Any?): Boolean {
        return when (value) {
            is String -> {
                val trimmed = value.trim()
                // Check for JSON object
                (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                // Check for JSON array (even single element arrays)
                (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
                // Check for potential structured data (contains quotes and colons/commas)
                (trimmed.contains("\"") && (trimmed.contains(":") || trimmed.contains(",")))
            }
            is Map<*, *> -> true
            is List<*> -> true
            is Array<*> -> true
            is FloatArray, is DoubleArray, is IntArray, is LongArray, is BooleanArray, is ByteArray -> true
            else -> {
                // Check if it's a JsonObject (or similar JSON type)
                val className = value?.javaClass?.simpleName
                if (className == "JsonObject" || className == "JsonArray" || className?.contains("Json") == true) {
                    true
                } else {
                    // Unknown data type - not expandable
                    false
                }
            }
        }
    }
    
    private fun showExpandedDataDialog(value: Any?) {
        
        val formattedContent = when (value) {
            is String -> {
                val trimmed = value.trim()
                when {
                    trimmed.startsWith("{") && trimmed.endsWith("}") -> formatJsonObject(trimmed)
                    trimmed.startsWith("[") && trimmed.endsWith("]") -> formatArrayString(trimmed)
                    trimmed.contains("\"") && (trimmed.contains(":") || trimmed.contains(",")) -> {
                        // Try to format as JSON even if not perfect
                        formatJsonObject(if (!trimmed.startsWith("{")) "{$trimmed}" else trimmed)
                    }
                    else -> value
                }
            }
            is Map<*, *> -> formatMap(value)
            is List<*> -> formatList(value, expanded = true)
            is Array<*> -> formatList(value.toList(), expanded = true)
            is FloatArray -> value.joinToString("\n") { it.toString() }
            is DoubleArray -> value.joinToString("\n") { it.toString() }
            is IntArray -> value.joinToString("\n") { it.toString() }
            is LongArray -> value.joinToString("\n") { it.toString() }
            is BooleanArray -> value.joinToString("\n") { it.toString() }
            is ByteArray -> value.joinToString("\n") { (it.toInt() and 0xFF).toString() }
            else -> {
                // Handle JsonObject and other JSON types
                val className = value?.javaClass?.simpleName
                if (className == "JsonObject" || className == "JsonArray" || className?.contains("Json") == true) {
                    formatJsonObject(value.toString())
                } else {
                    value.toString()
                }
            }
        }
        
        val parentWindow = SwingUtilities.getWindowAncestor(rootPanel)
        val dialog = if (parentWindow is java.awt.Frame) {
            JDialog(parentWindow, "Data Viewer", true)
        } else {
            JDialog(parentWindow as? java.awt.Dialog, "Data Viewer", true)
        }
        dialog.setSize(600, 400)
        dialog.setLocationRelativeTo(rootPanel)
        
        val textArea = JTextArea(formattedContent)
        textArea.isEditable = false
        textArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        
        val scrollPane = JBScrollPane(textArea)
        scrollPane.border = JBUI.Borders.empty(8)
        
        dialog.add(scrollPane)
        dialog.isVisible = true
    }
    
    private fun formatJsonObject(jsonString: String): String {
        return try {
            // Simple JSON formatting - remove braces and format key-value pairs
            val content = jsonString.removePrefix("{").removeSuffix("}")
            val pairs = mutableListOf<String>()
            var current = ""
            var inQuotes = false
            var depth = 0
            
            for (char in content) {
                when (char) {
                    '"' -> {
                        inQuotes = !inQuotes
                        current += char
                    }
                    '[', '{' -> {
                        if (!inQuotes) depth++
                        current += char
                    }
                    ']', '}' -> {
                        if (!inQuotes) depth--
                        current += char
                    }
                    ',' -> {
                        if (!inQuotes && depth == 0) {
                            pairs.add(current.trim())
                            current = ""
                        } else {
                            current += char
                        }
                    }
                    else -> current += char
                }
            }
            if (current.trim().isNotEmpty()) {
                pairs.add(current.trim())
            }
            
            pairs.joinToString("\n") { pair ->
                val parts = pair.split(":", limit = 2)
                if (parts.size == 2) {
                    "${parts[0].trim().removeSurrounding("\"")}: ${parts[1].trim()}"
                } else {
                    pair
                }
            }
        } catch (e: Exception) {
            // Fallback to original string if parsing fails
            jsonString
        }
    }
    
    private fun formatArrayString(arrayString: String): String {
        return arrayString.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapIndexed { index, item -> "$index: ${item.trim()}" }
            .joinToString("\n")
    }
    
    private fun formatMap(map: Map<*, *>): String {
        return map.entries.joinToString("\n") { entry ->
            "${entry.key}: ${entry.value}"
        }
    }
    
    private fun formatList(values: List<*>, expanded: Boolean): String {
        return if (expanded) {
            values.mapIndexed { index, value ->
                "$index: ${value?.toString().orEmpty()}"
            }.joinToString("\n")
        } else {
            if (values.isEmpty()) return "[]"
            val preview = values.take(10).joinToString { it?.toString().orEmpty() }
            if (values.size > 10) "[$preview, …]" else "[$preview]"
        }
    }

    override fun dispose() {}

    override fun getFile(): VirtualFile = virtualFile
}