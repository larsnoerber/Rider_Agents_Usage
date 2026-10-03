package io.github.larsnoerber.agentsusage.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.plaf.basic.BasicProgressBarUI

/** Shared compact usage bar for every provider. */
internal open class UsageCard(title: String) : JPanel(BorderLayout(0, 5)) {
    protected val heading = JBLabel(title)
    protected val remaining = JBLabel("—")
    protected val detail = JBLabel("Usage unavailable")
    protected val progress = JProgressBar(0, 100)

    init {
        isOpaque = false
        heading.foreground = JBColor.GRAY
        remaining.font = remaining.font.deriveFont(Font.BOLD, 15f)
        add(JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(heading, BorderLayout.CENTER)
            add(remaining, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        progress.isStringPainted = false
        progress.setUI(BasicProgressBarUI())
        progress.isOpaque = true
        progress.background = JBColor(Color(227, 230, 235), Color(61, 65, 72))
        progress.border = BorderFactory.createEmptyBorder()
        progress.preferredSize = JBUI.size(-1, 14)
        add(progress, BorderLayout.CENTER)
        add(detail.apply { foreground = JBColor.GRAY }, BorderLayout.SOUTH)
    }

    protected fun updateProgress(percent: Int?, color: Color) {
        remaining.foreground = color
        progress.foreground = color
        progress.value = percent?.coerceIn(0, 100) ?: 0
        progress.accessibleContext.accessibleName = heading.text
        progress.accessibleContext.accessibleDescription = "${remaining.text}. ${detail.text}"
    }
}
