package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.components.JBLabel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPanel

/** Four compact facts stay visible; the remaining provider details expand on demand. */
internal class UsageSummary : JPanel(BorderLayout(0, 2)) {
    private val facts = JPanel(GridBagLayout()).apply { isOpaque = false }
    private val more = JPanel(GridBagLayout()).apply { isOpaque = false }
    private var expanded = false
    private val toggle = JButton().apply {
        isContentAreaFilled = false
        isBorderPainted = false
        margin = Insets(0, 0, 0, 0)
        foreground = secondaryTextColor()
        font = font.deriveFont((font.size2D - 1f).coerceAtLeast(10f))
        addActionListener {
            expanded = !expanded
            updateExpanded()
            this@UsageSummary.revalidate()
            this@UsageSummary.repaint()
        }
    }

    init {
        isOpaque = false
        add(facts, BorderLayout.NORTH)
        add(more, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(toggle)
        }, BorderLayout.SOUTH)
        updateExpanded()
    }

    fun render(title: String, summary: List<Pair<String, String>>, details: List<Pair<String, String>>) {
        fill(facts, summary, columns = 2)
        fill(more, details, columns = 1)
        toggle.isVisible = details.isNotEmpty()
        toolTipText = usageTooltip(title, (summary + details).map { "${it.first}: ${it.second}" })
        updateExpanded()
    }

    private fun updateExpanded() {
        more.isVisible = expanded
        toggle.text = if (expanded) "Fewer details" else "More details"
        toggle.accessibleContext.accessibleName = if (expanded) "Collapse agent details" else "Expand agent details"
    }

    private fun fill(target: JPanel, entries: List<Pair<String, String>>, columns: Int) {
        target.removeAll()
        entries.forEachIndexed { index, (name, value) ->
            val tooltip = usageTooltip(name, listOf(value))
            val cell = JPanel(BorderLayout(4, 0)).apply {
                isOpaque = false
                add(JBLabel("<html>${StringUtil.escapeXmlEntities(name)}:</html>").apply {
                    foreground = secondaryTextColor()
                    font = font.deriveFont((font.size2D - 1f).coerceAtLeast(10f))
                    toolTipText = tooltip
                }, BorderLayout.WEST)
                add(object : JBLabel("<html>${StringUtil.escapeXmlEntities(value)}</html>") {
                    override fun getPreferredSize(): Dimension = super.getPreferredSize().apply { width = 0 }
                }.apply {
                    font = font.deriveFont(Font.BOLD, (font.size2D - 1f).coerceAtLeast(10f))
                    toolTipText = tooltip
                }, BorderLayout.CENTER)
            }
            target.add(cell, GridBagConstraints().apply {
                gridx = index % columns
                gridy = index / columns
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(JBUI.scale(2), 0, 0, if (gridx < columns - 1) JBUI.scale(10) else 0)
            })
        }
    }
}
