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
import java.awt.Cursor
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

internal class ProviderHeader(private val title: String, actions: JComponent? = null) : JPanel(BorderLayout(6, 0)) {
    private val heading = JBLabel(title)
    private val subscription = object : JButton("Unknown") {
        override fun getPreferredSize(): Dimension = super.getPreferredSize().apply { width = width.coerceAtMost(JBUI.scale(140)) }
    }
    private var planText = "Unknown"
    private var details = emptyList<String>()
    private var detailsExpanded = false
    private var toggleDetails: (() -> Unit)? = null

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
                margin = Insets(0, 0, 0, 0)
                isContentAreaFilled = false
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addActionListener { toggleDetails?.invoke() }
            })
            actions?.let { add(it) }
        }, BorderLayout.EAST)
    }

    fun render(plan: String?, details: List<String> = emptyList()) {
        planText = formatSubscriptionPlan(plan)
        this.details = details
        updateBadge()
    }

    fun bindDetailsToggle(action: (() -> Unit)?) {
        toggleDetails = action
        updateBadge()
    }

    fun setDetailsExpanded(expanded: Boolean) {
        detailsExpanded = expanded
        updateBadge()
    }

    private fun updateBadge() {
        val action = if (detailsExpanded) "Hide details" else "Show details"
        subscription.text = if (toggleDetails != null) "$planText ${if (detailsExpanded) "▴" else "▾"}" else planText
        subscription.getAccessibleContext().accessibleName = "$title · $planText · $action"
        toolTipText = usageTooltip(title, listOf("Subscription: $planText") + details +
            if (toggleDetails != null) listOf("Click the badge to ${action.lowercase()}") else emptyList())
        heading.toolTipText = toolTipText
        subscription.toolTipText = toolTipText
    }
}
