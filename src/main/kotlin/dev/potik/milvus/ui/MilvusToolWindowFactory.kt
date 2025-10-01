package dev.potik.milvus.ui

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.credentialStore.generateServiceName
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.concurrency.EdtExecutorService
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import dev.potik.milvus.core.MilvusConnectionService
import dev.potik.milvus.core.MilvusConnectionService.CollectionSummary
import dev.potik.milvus.settings.MilvusSettingsState
import dev.potik.milvus.editor.MilvusCollectionVirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.CompletionException
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.table.DefaultTableModel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu
import javax.swing.JMenuItem

class MilvusToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val milvusToolWindow = MilvusToolWindow(project)
        val content = toolWindow.contentManager.factory.createContent(milvusToolWindow.component, "", false)
        toolWindow.contentManager.addContent(content)
        Disposer.register(toolWindow.disposable, milvusToolWindow)
    }
}

private class MilvusToolWindow(private val project: Project) : com.intellij.openapi.Disposable {
    private val connectionService = MilvusConnectionService.instance()
    private val settingsState = MilvusSettingsState.getInstance()
    private val passwordSafe = PasswordSafe.instance

    private val hostField = JBTextField()
    private val portSpinner = JSpinner(SpinnerNumberModel(19530, 1, 65535, 1)).apply {
        (editor as JSpinner.NumberEditor).format.isParseIntegerOnly = true
    }
    private val usernameField = JBTextField()
    private val passwordField = JBPasswordField()
    private val databaseField = JBTextField()
    private val defaultCollectionField = JBTextField()
    private val secureCheckBox = JBCheckBox("Use TLS (HTTPS)")
    private val previewLimitSpinner = JSpinner(SpinnerNumberModel(MilvusConnectionService.DEFAULT_PREVIEW_LIMIT, 10, 1000, 10))

    private val statusLabel = JBLabel("Disconnected")
    private val connectButton = JButton("Connect")
    private val disconnectButton = JButton("Disconnect")
    private val testConnectionButton = JButton("Test Connection")
    private val refreshCollectionsButton = JButton("Refresh")

    private val collectionsModel = object : DefaultTableModel(arrayOf("Collection", "Fields", "Loaded", "Description"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val collectionsTable = JBTable(collectionsModel)


    private val rootPanel = JPanel(BorderLayout())

    init {
        initialiseComponentState()
        rootPanel.border = JBEmptyBorder(8)
        rootPanel.add(buildConnectionPanel(), BorderLayout.NORTH)
        rootPanel.add(buildDataPanel(), BorderLayout.CENTER)
        attachListeners()
        loadStateIntoForm()
    }

    val component: JComponent
        get() = rootPanel

    private fun buildConnectionPanel(): JComponent {
        statusLabel.font = JBFont.small()

        // Create inline panel for host and port
        val hostPortPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(JBLabel("Host:"))
            add(hostField.apply { preferredSize = java.awt.Dimension(150, preferredSize.height) })
            add(JBLabel("Port:"))
            add(portSpinner.apply { preferredSize = java.awt.Dimension(80, preferredSize.height) })
        }

        // Create inline panel for username and password
        val userPassPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(JBLabel("User:"))
            add(usernameField.apply { preferredSize = java.awt.Dimension(100, preferredSize.height) })
            add(JBLabel("Password:"))
            add(passwordField.apply { preferredSize = java.awt.Dimension(100, preferredSize.height) })
        }

        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(hostPortPanel)
            .addComponent(userPassPanel)
            .addLabeledComponent("Database", databaseField, true)
            .addLabeledComponent("Default Collection", defaultCollectionField, true)
            .addComponent(secureCheckBox)
            .addLabeledComponent("Preview Limit", previewLimitSpinner, true)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(testConnectionButton)
            add(connectButton)
            add(disconnectButton)
            add(statusLabel)
        }

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 8, 12, 8)
            add(formBuilder.panel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }
    }

    private fun buildDataPanel(): JComponent {
        configureCollectionsTable()

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 8, 8, 8)
            add(buildCollectionsHeader(), BorderLayout.NORTH)
            add(JBScrollPane(collectionsTable), BorderLayout.CENTER)
        }
    }

    private fun buildCollectionsHeader(): JComponent {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(4)
            add(JBLabel("Collections"), BorderLayout.WEST)
            add(refreshCollectionsButton, BorderLayout.EAST)
        }
    }

    private fun configureCollectionsTable() {
        collectionsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
        collectionsTable.autoCreateRowSorter = true
        collectionsTable.tableHeader.reorderingAllowed = false
        collectionsTable.emptyText.text = "Connect to Milvus to load collections"
        
        // Add double-click and right-click listeners
        collectionsTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && connectionService.isConnected()) {
                    val row = collectionsTable.rowAtPoint(e.point)
                    if (row >= 0) {
                        val collectionName = collectionsTable.getValueAt(row, 0).toString()
                        openCollectionInEditor(collectionName)
                    }
                }
            }
            
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }
            
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    showContextMenu(e)
                }
            }
        })
    }


    private fun initialiseComponentState() {
        disconnectButton.isEnabled = false
        refreshCollectionsButton.isEnabled = false
    }

    private fun attachListeners() {
        connectButton.addActionListener { connect(false) }
        testConnectionButton.addActionListener { connect(true) }
        disconnectButton.addActionListener { disconnect() }
        refreshCollectionsButton.addActionListener { refreshCollections() }


        previewLimitSpinner.addChangeListener {
            settingsState.updatePreviewLimit(currentPreviewLimit())
        }
    }

    private fun loadStateIntoForm() {
        val config = settingsState.toConnectionConfig()
        hostField.text = config.host
        portSpinner.value = config.port
        usernameField.text = config.username.orEmpty()
        databaseField.text = config.databaseName.orEmpty()
        defaultCollectionField.text = config.defaultCollection.orEmpty()
        secureCheckBox.isSelected = config.secure
        previewLimitSpinner.value = config.previewLimit

        loadStoredPassword(config)
    }

    private fun loadStoredPassword(config: MilvusConnectionService.ConnectionConfig) {
        val attrs = credentialAttributes(config.host, config.port, config.username)
        val password = passwordSafe.getPassword(attrs)
        if (!password.isNullOrEmpty()) {
            passwordField.text = password
        }
    }

    private fun connect(testOnly: Boolean) {
        val validationError = validateForm()
        if (validationError != null) {
            notify(validationError, NotificationType.WARNING)
            return
        }

        val config = buildConnectionConfig()
        val passwordChars = passwordField.password
        val passwordForStorage = passwordChars.concatToString()
        val passwordCopy = passwordForStorage.toCharArray()

        setConnectingState(true)

        connectionService.connect(config, passwordCopy)
            .whenComplete { _, throwable ->
                invokeOnEdt {
                    setConnectingState(false)
                    clearArray(passwordCopy)
                    clearArray(passwordChars)
                    if (throwable != null) {
                        notify("Connection failed: ${unwrap(throwable).message}", NotificationType.ERROR)
                        statusLabel.text = "Disconnected"
                        return@invokeOnEdt
                    }

                    if (testOnly) {
                        connectionService.disconnect()
                        statusLabel.text = "Connection successful"
                    } else {
                        onConnected(config, passwordForStorage)
                    }
                }
            }
    }

    private fun onConnected(config: MilvusConnectionService.ConnectionConfig, password: String) {
        settingsState.updateFrom(config)
        if (password.isNotEmpty()) {
            passwordSafe.setPassword(credentialAttributes(config.host, config.port, config.username), password)
        }
        statusLabel.text = "Connected to ${config.host}:${config.port}"
        disconnectButton.isEnabled = true
        refreshCollectionsButton.isEnabled = true
        connectButton.isEnabled = false
        testConnectionButton.isEnabled = false

        refreshCollections(config.defaultCollection)
    }

    private fun disconnect() {
        connectionService.disconnect()
        statusLabel.text = "Disconnected"
        connectButton.isEnabled = true
        disconnectButton.isEnabled = false
        refreshCollectionsButton.isEnabled = false
        testConnectionButton.isEnabled = true
        collectionsModel.setRowCount(0)
    }

    private fun refreshCollections(preselect: String? = null) {
        if (!connectionService.isConnected()) {
            notify("Connect to Milvus first", NotificationType.WARNING)
            return
        }
        setCollectionsLoading(true)
        connectionService.listCollections()
            .whenComplete { summaries, throwable ->
                invokeOnEdt {
                    setCollectionsLoading(false)
                    if (throwable != null) {
                        notify("Failed to load collections: ${unwrap(throwable).message}", NotificationType.ERROR)
                        return@invokeOnEdt
                    }

                    updateCollectionsTable(summaries ?: emptyList(), preselect)
                }
            }
    }

    private fun updateCollectionsTable(summaries: List<CollectionSummary>, preselect: String?) {
        collectionsModel.setRowCount(0)
        summaries.forEach { summary ->
            collectionsModel.addRow(arrayOf(
                summary.name,
                summary.fieldCount,
                if (summary.loaded) "Yes" else "No",
                summary.description.orEmpty()
            ))
        }

        if (summaries.isEmpty()) {
            return
        }

        val targetName = preselect
            ?: settingsState.toConnectionConfig().defaultCollection
            ?: summaries.first().name

        val rowIndex = summaries.indexOfFirst { it.name == targetName }.takeIf { it >= 0 } ?: 0
        collectionsTable.selectionModel.setSelectionInterval(rowIndex, rowIndex)
    }



    private fun validateForm(): String? {
        if (hostField.text.isNullOrBlank()) {
            return "Host is required"
        }
        val port = (portSpinner.value as Number).toInt()
        if (port !in 1..65535) {
            return "Port must be between 1 and 65535"
        }
        return null
    }

    private fun buildConnectionConfig(): MilvusConnectionService.ConnectionConfig =
        MilvusConnectionService.ConnectionConfig(
            host = hostField.text.trim(),
            port = (portSpinner.value as Number).toInt(),
            username = usernameField.text.trim().takeIf { it.isNotEmpty() },
            secure = secureCheckBox.isSelected,
            databaseName = databaseField.text.trim().takeIf { it.isNotEmpty() },
            defaultCollection = defaultCollectionField.text.trim().takeIf { it.isNotEmpty() },
            previewLimit = currentPreviewLimit()
        )

    private fun currentPreviewLimit(): Int = (previewLimitSpinner.value as Number).toInt()

    private fun setConnectingState(connecting: Boolean) {
        connectButton.isEnabled = !connecting
        testConnectionButton.isEnabled = !connecting
        disconnectButton.isEnabled = connectionService.isConnected() && !connecting
        statusLabel.text = if (connecting) "Connecting…" else statusLabel.text
    }

    private fun setCollectionsLoading(loading: Boolean) {
        refreshCollectionsButton.isEnabled = !loading
        refreshCollectionsButton.text = if (loading) "Loading…" else "Refresh"
    }




    private fun credentialAttributes(host: String, port: Int, username: String?): CredentialAttributes {
        val userPart = username?.takeIf { it.isNotBlank() } ?: "anonymous"
        val serviceName = generateServiceName("MilvusConnector", "$host:$port:$userPart")
        return CredentialAttributes(serviceName)
    }

    private fun notify(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Milvus Connector")
            .createNotification(message, type)
            .notify(project)
    }

    private fun unwrap(throwable: Throwable): Throwable =
        if (throwable is CompletionException && throwable.cause != null) throwable.cause!! else throwable

    private fun invokeOnEdt(action: () -> Unit) {
        if (ApplicationManager.getApplication().isDispatchThread) {
            action()
        } else {
            EdtExecutorService.getInstance().execute(action)
        }
    }

    private fun clearArray(array: CharArray) {
        array.fill('\u0000')
    }

    private fun showContextMenu(e: MouseEvent) {
        if (!connectionService.isConnected()) return
        
        val row = collectionsTable.rowAtPoint(e.point)
        if (row >= 0) {
            collectionsTable.setRowSelectionInterval(row, row)
            val collectionName = collectionsTable.getValueAt(row, 0).toString()
            
            val popupMenu = JPopupMenu()
            val openInEditorItem = JMenuItem("Open in Editor")
            openInEditorItem.addActionListener {
                openCollectionInEditor(collectionName)
            }
            popupMenu.add(openInEditorItem)
            
            val refreshItem = JMenuItem("Refresh Collection")
            refreshItem.addActionListener {
                refreshCollections()
            }
            popupMenu.add(refreshItem)
            
            popupMenu.show(collectionsTable, e.x, e.y)
        }
    }

    private fun openCollectionInEditor(collectionName: String) {
        val config = connectionService.getActiveConfig() ?: return
        val virtualFile = MilvusCollectionVirtualFile(collectionName, config)
        
        invokeOnEdt {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }

    override fun dispose() {
        // nothing to dispose
    }
}
