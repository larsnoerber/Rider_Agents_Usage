package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel

/** Compact provider surface with a stable identity accent, independent of quota warning colors. */
internal class AgentSection(content: JComponent, private val accent: Color) : JPanel(BorderLayout()) {
    init {
        isOpaque = false
        border = JBUI.Borders.empty(6, 10, 6, 7)
        add(content, BorderLayout.CENTER)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val radius = JBUI.scale(8)
            g.color = JBColor(Color(248, 249, 251), Color(43, 45, 48))
            g.fillRoundRect(0, 0, width - 1, height - 1, radius, radius)
            g.color = JBColor(Color(220, 223, 229), Color(63, 66, 73))
            g.drawRoundRect(0, 0, width - 1, height - 1, radius, radius)
            g.color = accent
            g.fillRoundRect(JBUI.scale(3), JBUI.scale(7), JBUI.scale(3), (height - JBUI.scale(14)).coerceAtLeast(0), 3, 3)
        } finally {
            g.dispose()
        }
    }
}
