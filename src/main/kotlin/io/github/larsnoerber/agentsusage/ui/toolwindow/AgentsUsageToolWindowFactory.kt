package io.github.larsnoerber.agentsusage.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

/** IntelliJ entry point; composition and provider views live in separate files. */
class AgentsUsageToolWindowFactory : ToolWindowFactory {
    override fun init(toolWindow: ToolWindow) {
        toolWindow.title = "AgentMeter"
        toolWindow.stripeTitle = "AgentMeter"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = AgentsUsagePanel()
        val content = toolWindow.contentManager.factory.createContent(panel, "Usage", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}
