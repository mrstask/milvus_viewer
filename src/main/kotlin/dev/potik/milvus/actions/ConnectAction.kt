package dev.potik.milvus.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dev.potik.milvus.core.MilvusConnectionService

class ConnectAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // This action can be used to quickly connect to a default Milvus instance
        // or open the tool window
        val toolWindow = project.getService(com.intellij.openapi.wm.ToolWindowManager::class.java)
            .getToolWindow("Milvus")
        
        toolWindow?.show()
    }
}

