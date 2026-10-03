package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.plaf.basic.BasicProgressBarUI

/** Shared compact usage bar for every provider. */
internal open class UsageCard(title: String) : JPanel(BorderLayout(6, 0)) {
    protected val heading = JBLabel(title)
    protected val remaining = JBLabel("—")
    protected val detail = JBLabel("Usage unavailable")
    protected val progress = JProgressBar(0, 100)

    init {
        isOpaque = false
        heading.foreground = JBColor.GRAY
        heading.preferredSize = JBUI.size(100, heading.preferredSize.height)
        remaining.font = remaining.font.deriveFont(Font.BOLD)
        remaining.horizontalAlignment = JBLabel.RIGHT
        remaining.preferredSize = JBUI.size(88, remaining.preferredSize.height)
        add(heading, BorderLayout.WEST)
        add(remaining, BorderLayout.EAST)
        progress.isStringPainted = false
        progress.setUI(BasicProgressBarUI())
        progress.isOpaque = true
        progress.background = JBColor(Color(227, 230, 235), Color(61, 65, 72))
        progress.border = BorderFactory.createEmptyBorder()
        progress.preferredSize = JBUI.size(64, 10)
        progress.minimumSize = JBUI.size(32, 10)
        add(JPanel(GridBagLayout()).apply {
            isOpaque = false
            add(progress, GridBagConstraints().apply {
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
            })
        }, BorderLayout.CENTER)
    }

    protected fun updateProgress(percent: Int?, color: Color) {
        remaining.foreground = color
        progress.foreground = color
        progress.value = percent?.coerceIn(0, 100) ?: 0
        progress.accessibleContext.accessibleName = heading.text
        progress.accessibleContext.accessibleDescription = "${remaining.text}. ${detail.text}"
        updateDetailsTooltip()
    }

    protected fun updateDetailsTooltip(extra: String? = null) {
        toolTipText = listOfNotNull("${heading.text}: ${remaining.text}", detail.text, extra)
            .filter { it.isNotBlank() }.joinToString("<br>", "<html>", "</html>") { StringUtil.escapeXmlEntities(it) }
        listOf(heading, remaining, progress).forEach { it.toolTipText = toolTipText }
    }
}
