package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JPanel

internal class UsageSurface : JPanel(GridBagLayout()) {
    init {
        background = JBColor(Color(248, 249, 251), Color(43, 45, 48))
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(Color(220, 223, 229), Color(63, 66, 73))),
            JBUI.Borders.empty(8)
        )
    }

    fun addRow(component: Component, top: Int = 0) {
        add(component, GridBagConstraints().apply {
            gridx = 0
            gridy = componentCount
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = Insets(top, 0, 0, 0)
        })
    }
}
