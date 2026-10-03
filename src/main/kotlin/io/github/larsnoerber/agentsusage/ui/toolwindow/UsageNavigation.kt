package io.github.larsnoerber.agentsusage.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

/** Opens the Tool Window on its usage overview from any provider status widget. */
internal fun showUsageOverview(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
    toolWindow.show {
        toolWindow.contentManager.contents
            .firstNotNullOfOrNull { it.component as? AgentsUsagePanel }
            ?.showUsageOverview()
    }
}

private const val TOOL_WINDOW_ID = "Agents Usage"
