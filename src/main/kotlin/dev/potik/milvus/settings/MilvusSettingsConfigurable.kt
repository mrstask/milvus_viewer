package dev.potik.milvus.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.*

class MilvusSettingsConfigurable : Configurable {
    private var settingsState = ApplicationManager.getApplication().getService(MilvusSettingsState::class.java)
    private var panel: JPanel? = null
    private var connectionList: JBList<MilvusSettingsState.SavedConnection>? = null
    private var nameField: JBTextField? = null
    private var hostField: JBTextField? = null
    private var portField: JBTextField? = null
    private var userField: JBTextField? = null
    private var passwordField: JPasswordField? = null
    private var secureCheckbox: JBCheckBox? = null
    
    override fun getDisplayName(): String = "Milvus Connector"
    
    override fun createComponent(): JComponent {
        panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // Left panel - connection list
        val leftPanel = JBPanel<JBPanel<*>>(BorderLayout())
        leftPanel.add(JBLabel("Saved Connections:"), BorderLayout.NORTH)
        
        connectionList = JBList<MilvusSettingsState.SavedConnection>()
        connectionList?.selectionMode = ListSelectionModel.SINGLE_SELECTION
        connectionList?.addListSelectionListener {
            updateFormFromSelection()
        }
        
        val listScrollPane = JBScrollPane(connectionList)
        leftPanel.add(listScrollPane, BorderLayout.CENTER)
        
        val listButtonPanel = JBPanel<JBPanel<*>>()
        val addButton = JButton("Add")
        val removeButton = JButton("Remove")
        
        addButton.addActionListener { addNewConnection() }
        removeButton.addActionListener { removeSelectedConnection() }
        
        listButtonPanel.add(addButton)
        listButtonPanel.add(removeButton)
        leftPanel.add(listButtonPanel, BorderLayout.SOUTH)
        
        // Right panel - connection form
        val rightPanel = JBPanel<JBPanel<*>>(BorderLayout())
        rightPanel.add(JBLabel("Connection Details:"), BorderLayout.NORTH)
        
        val formPanel = JBPanel<JBPanel<*>>(GridLayout(0, 2, 5, 5))
        
        nameField = JBTextField()
        hostField = JBTextField()
        portField = JBTextField()
        userField = JBTextField()
        passwordField = JPasswordField()
        secureCheckbox = JBCheckBox("Use TLS")
        
        formPanel.add(JBLabel("Name:"))
        formPanel.add(nameField)
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
        
        rightPanel.add(formPanel, BorderLayout.CENTER)
        
        val saveButton = JButton("Save Connection")
        saveButton.addActionListener { saveCurrentConnection() }
        rightPanel.add(saveButton, BorderLayout.SOUTH)
        
        // Split pane
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
        splitPane.dividerLocation = 300
        panel?.add(splitPane, BorderLayout.CENTER)
        
        loadConnections()
        return panel!!
    }
    
    private fun loadConnections() {
        val listModel = DefaultListModel<MilvusSettingsState.SavedConnection>()
        settingsState.savedConnections.forEach { connection ->
            listModel.addElement(connection)
        }
        connectionList?.model = listModel
    }
    
    private fun updateFormFromSelection() {
        val selectedConnection = connectionList?.selectedValue
        if (selectedConnection != null) {
            nameField?.text = selectedConnection.name
            hostField?.text = selectedConnection.host
            portField?.text = selectedConnection.port.toString()
            userField?.text = selectedConnection.user
            passwordField?.text = selectedConnection.password
            secureCheckbox?.isSelected = selectedConnection.secure
        }
    }
    
    private fun addNewConnection() {
        val newConnection = MilvusSettingsState.SavedConnection()
        settingsState.savedConnections.add(newConnection)
        loadConnections()
        connectionList?.selectedIndex = settingsState.savedConnections.size - 1
    }
    
    private fun removeSelectedConnection() {
        val selectedIndex = connectionList?.selectedIndex ?: -1
        if (selectedIndex >= 0) {
            settingsState.savedConnections.removeAt(selectedIndex)
            loadConnections()
        }
    }
    
    private fun saveCurrentConnection() {
        val selectedIndex = connectionList?.selectedIndex ?: -1
        if (selectedIndex >= 0) {
            val connection = settingsState.savedConnections[selectedIndex]
            connection.name = nameField?.text ?: ""
            connection.host = hostField?.text ?: "localhost"
            connection.port = portField?.text?.toIntOrNull() ?: 19530
            connection.user = userField?.text ?: ""
            connection.password = String(passwordField?.password ?: charArrayOf())
            connection.secure = secureCheckbox?.isSelected ?: false
            
            loadConnections()
        }
    }
    
    override fun isModified(): Boolean = false
    
    @Throws(ConfigurationException::class)
    override fun apply() {
        // Settings are automatically saved through PersistentStateComponent
    }
    
    override fun reset() {
        loadConnections()
    }
}

