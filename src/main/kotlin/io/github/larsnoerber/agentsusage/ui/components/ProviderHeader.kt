package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.core.format.formatSubscriptionPlan
import java.awt.BorderLayout
import java.awt.Font
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal class ProviderHeader(private val title: String, actions: JComponent? = null) : JPanel(BorderLayout(6, 0)) {
    private val heading = JBLabel(title)
    private val subscription = object : JBLabel("Unknown") {
        override fun getPreferredSize(): Dimension = super.getPreferredSize().apply { width = width.coerceAtMost(JBUI.scale(140)) }
    }

    init {
        isOpaque = false
        add(heading.apply { font = font.deriveFont(Font.BOLD) }, BorderLayout.WEST)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            isOpaque = false
            add(subscription.apply {
                foreground = secondaryTextColor()
                background = JBColor(Color(231, 234, 240), Color(56, 59, 65))
                isOpaque = true
                font = font.deriveFont((font.size2D - 1f).coerceAtLeast(10f))
                border = JBUI.Borders.empty(2, 6)
            })
            actions?.let { add(it) }
        }, BorderLayout.EAST)
    }

    fun render(plan: String?, details: List<String> = emptyList()) {
        subscription.text = formatSubscriptionPlan(plan)
        toolTipText = usageTooltip(title, listOf("Subscription: ${subscription.text}") + details)
        heading.toolTipText = toolTipText
        subscription.toolTipText = toolTipText
    }
}
