package io.github.larsnoerber.agentsusage.ui.settings

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JButton

internal class RefreshChoice(
    private val title: String,
    private val note: String,
    val seconds: Int
) : JButton() {
    init {
        horizontalAlignment = LEFT
        isFocusPainted = false
        isContentAreaFilled = true
        setSelectedStyle(false)
    }

    fun setSelectedStyle(selected: Boolean) {
        text = "<html><b>$title</b><br><span style='color:#9ca1ab'>$note</span>${if (selected) "<span style='float:right;color:#8bcaff'>✓</span>" else ""}</html>"
        background = if (selected) {
            JBColor(Color(233, 241, 255), Color(45, 59, 82))
        } else {
            JBColor(Color(255, 255, 255), Color(41, 43, 48))
        }
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                if (selected) JBColor(Color(88, 130, 211), Color(75, 118, 201)) else JBColor(Color(226, 229, 234), Color(57, 60, 67))
            ),
            JBUI.Borders.empty(7, 9)
        )
    }
}
