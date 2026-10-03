package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.core.UsageSource
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

internal data class StatusBarPart(val text: String, val color: Color? = null)

internal data class StatusBarPresentation(
    val parts: List<StatusBarPart>,
    val tooltip: String,
    val dimmed: Boolean = false
)

/** Owns clicks, listener cleanup, and layout; provider factories only format their snapshot. */
internal class UsageStatusBarWidget<T>(
    private val project: Project,
    private val id: String,
    private val source: UsageSource<T>,
    private val presentation: (T) -> StatusBarPresentation
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)), CustomStatusBarWidget {
    private val labels = mutableListOf<JBLabel>()
    private var disposed = false
    private var installedStatusBar: StatusBar? = null
    private val listener: (T) -> Unit = { if (!disposed) render(it) }
    private val mouseHandler = object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(event)) {
                ToolWindowManager.getInstance(project).getToolWindow("Agents Usage")?.show()
            }
        }
    }

    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(mouseHandler)
        source.addListener(listener)
        render(source.current)
    }

    override fun ID(): String = id
    override fun getComponent(): JComponent = this
    override fun install(statusBar: StatusBar) {
        installedStatusBar = statusBar
        UsageStatusBarGroup.schedule(statusBar)
    }

    private fun render(usage: T) {
        val view = presentation(usage)
        if (labels.size != view.parts.size) {
            removeAll()
            labels.clear()
            view.parts.forEach { _ ->
                val label = JBLabel().also {
                    it.addMouseListener(mouseHandler)
                    add(it)
                }
                labels += label
            }
        }
        labels.zip(view.parts).forEach { (label, part) ->
            label.text = part.text
            label.foreground = if (view.dimmed) JBColor.GRAY else part.color
            label.toolTipText = view.tooltip
        }
        toolTipText = view.tooltip
        revalidate()
        repaint()
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        source.removeListener(listener)
        installedStatusBar?.let(UsageStatusBarGroup::schedule)
        installedStatusBar = null
    }
}
