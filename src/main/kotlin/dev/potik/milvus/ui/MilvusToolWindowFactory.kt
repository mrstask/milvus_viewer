package dev.potik.milvus.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.potik.milvus.core.MilvusConnectionService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

class MilvusToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val root = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Connection panel
        val connectionPanel = createConnectionPanel()
        
        // Collections browser panel
        val collectionsPanel = createCollectionsPanel()
        
        // Search panel
        val searchPanel = createSearchPanel()
        
        // Create tabbed pane
        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("Connection", connectionPanel)
        tabbedPane.addTab("Collections", collectionsPanel)
        tabbedPane.addTab("Search", searchPanel)
        
        root.add(tabbedPane, BorderLayout.CENTER)
        toolWindow.component.add(root)
    }
    
    private fun createConnectionPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        val formPanel = JBPanel<JBPanel<*>>(GridLayout(0, 2, 5, 5))
        
        // Connection fields
        val hostField = JBTextField("localhost")
        val portField = JBTextField("19530")
        val userField = JBTextField("")
        val passwordField = JPasswordField("")
        val secureCheckbox = JBCheckBox("Use TLS")
        val connectButton = JButton("Connect")
        val disconnectButton = JButton("Disconnect")
        val statusLabel = JBLabel("Disconnected")
        
        // Add components to form
        formPanel.add(JBLabel("Host:"))
        formPanel.add(hostField)
        formPanel.add(JBLabel("Port:"))
        formPanel.add(portField)
        formPanel.add(JBLabel("User:"))
        formPanel.add(userField)
        formPanel.add(JBLabel("Password:"))
        formPanel.add(passwordField)
        formPanel.add(JBLabel(""))
        formPanel.add(secureCheckbox)
        formPanel.add(connectButton)
        formPanel.add(disconnectButton)
        formPanel.add(JBLabel("Status:"))
        formPanel.add(statusLabel)
        
        // Button actions
        connectButton.addActionListener {
            try {
                MilvusConnectionService.instance().connect(
                    hostField.text,
                    portField.text.toInt(),
                    userField.text.ifBlank { null },
                    String(passwordField.password).ifBlank { null },
                    secureCheckbox.isSelected
                )
                statusLabel.text = "Connected to ${hostField.text}:${portField.text}"
                statusLabel.foreground = JBUI.CurrentTheme.Link.linkColor
            } catch (e: Exception) {
                statusLabel.text = "Error: ${e.message}"
                statusLabel.foreground = JBUI.CurrentTheme.Error.errorForeground
            }
        }
        
        disconnectButton.addActionListener {
            MilvusConnectionService.instance().disconnect()
            statusLabel.text = "Disconnected"
            statusLabel.foreground = JBUI.CurrentTheme.DefaultTabs.underlineColor
        }
        
        panel.add(formPanel, BorderLayout.NORTH)
        return panel
    }
    
    private fun createCollectionsPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val refreshButton = JButton("Refresh Collections")
        val collectionsTable = JBTable()
        val collectionsModel = DefaultTableModel(arrayOf("Collection Name", "Description"), 0)
        collectionsTable.model = collectionsModel
        
        val scrollPane = JBScrollPane(collectionsTable)
        
        refreshButton.addActionListener {
            if (MilvusConnectionService.instance().isConnected()) {
                try {
                    val collections = MilvusConnectionService.instance().listCollections()
                    collectionsModel.setRowCount(0)
                    collections.forEach { collectionName ->
                        val description = MilvusConnectionService.instance().describeCollection(collectionName)
                        collectionsModel.addRow(arrayOf(collectionName, description))
                    }
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(panel, "Error loading collections: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
                }
            } else {
                JOptionPane.showMessageDialog(panel, "Please connect to Milvus first", "Not Connected", JOptionPane.WARNING_MESSAGE)
            }
        }
        
        panel.add(refreshButton, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }
    
    private fun createSearchPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val topPanel = JBPanel<JBPanel<*>>(GridLayout(0, 2, 5, 5))
        
        // Search fields
        val collectionField = JBTextField("")
        val vectorField = JBTextField("embedding")
        val topKField = JBTextField("10")
        val metricCombo = JComboBox(arrayOf("IP", "L2", "COSINE"))
        val exprField = JBTextField("")
        
        val vectorInputArea = JBTextArea(5, 30)
        vectorInputArea.text = "Paste your embedding vector as JSON array, e.g.: [0.1, 0.2, 0.3, ...]"
        
        val searchButton = JButton("Search")
        val resultsTable = JBTable()
        val resultsModel = DefaultTableModel(arrayOf("ID", "Distance", "Score"), 0)
        resultsTable.model = resultsModel
        
        // Add components
        topPanel.add(JBLabel("Collection:"))
        topPanel.add(collectionField)
        topPanel.add(JBLabel("Vector Field:"))
        topPanel.add(vectorField)
        topPanel.add(JBLabel("Top K:"))
        topPanel.add(topKField)
        topPanel.add(JBLabel("Metric:"))
        topPanel.add(metricCombo)
        topPanel.add(JBLabel("Expression:"))
        topPanel.add(exprField)
        
        val vectorPanel = JBPanel<JBPanel<*>>(BorderLayout())
        vectorPanel.add(JBLabel("Vector Input:"), BorderLayout.NORTH)
        vectorPanel.add(JBScrollPane(vectorInputArea), BorderLayout.CENTER)
        
        val buttonPanel = JBPanel<JBPanel<*>>()
        buttonPanel.add(searchButton)
        
        val resultsScrollPane = JBScrollPane(resultsTable)
        
        searchButton.addActionListener {
            if (!MilvusConnectionService.instance().isConnected()) {
                JOptionPane.showMessageDialog(panel, "Please connect to Milvus first", "Not Connected", JOptionPane.WARNING_MESSAGE)
                return@addActionListener
            }
            
            try {
                // Parse vector input
                val vectorText = vectorInputArea.text.trim()
                if (vectorText.startsWith("[") && vectorText.endsWith("]")) {
                    val vector = vectorText.removePrefix("[").removeSuffix("]")
                        .split(",")
                        .map { it.trim().toFloat() }
                    
                    val results = MilvusConnectionService.instance().searchVectors(
                        collectionField.text,
                        listOf(vector),
                        vectorField.text,
                        topKField.text.toInt(),
                        metricCombo.selectedItem.toString(),
                        exprField.text
                    )
                    
                    if (results != null) {
                        resultsModel.setRowCount(0)
                        results.hits.forEach { hit ->
                            resultsModel.addRow(arrayOf(hit.id, hit.distance, hit.score))
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(panel, "Invalid vector format. Please provide a JSON array.", "Invalid Input", JOptionPane.ERROR_MESSAGE)
                }
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(panel, "Search error: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
        
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(vectorPanel, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        panel.add(resultsScrollPane, BorderLayout.SOUTH)
        
        return panel
    }
}
