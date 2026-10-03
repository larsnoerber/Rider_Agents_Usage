package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.core.format.formatSubscriptionPlan
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel

internal class ProviderHeader(private val title: String, actions: JComponent? = null) : JPanel(BorderLayout(6, 0)) {
    private val heading = JBLabel(title)
    private val subscription = JBLabel("Unknown")

    init {
        isOpaque = false
        add(heading.apply { font = font.deriveFont(Font.BOLD) }, BorderLayout.WEST)
        add(subscription.apply { foreground = JBColor.GRAY }, BorderLayout.CENTER)
        actions?.let { add(it, BorderLayout.EAST) }
    }

    fun render(plan: String?, details: List<String> = emptyList()) {
        subscription.text = formatSubscriptionPlan(plan)
        toolTipText = usageTooltip(title, listOf("Subscription: ${subscription.text}") + details)
        heading.toolTipText = toolTipText
        subscription.toolTipText = toolTipText
    }
}
