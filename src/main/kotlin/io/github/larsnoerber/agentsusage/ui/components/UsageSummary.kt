package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.ui.components.JBLabel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JPanel
import com.intellij.openapi.Disposable

/** Four compact facts stay visible; the remaining provider details expand on demand. */
internal class UsageSummary(private val usageDetails: ProviderUsageDetails?, private val header: ProviderHeader) :
    JPanel(BorderLayout(0, 2)), Disposable {
    private val facts = JPanel(GridBagLayout()).apply { isOpaque = false }
    private val more = JPanel(GridBagLayout()).apply { isOpaque = false }
    private val expandedContent = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(more, BorderLayout.NORTH)
        usageDetails?.let { add(it, BorderLayout.CENTER) }
    }
    private var expanded = false
    init {
        isOpaque = false
        add(facts, BorderLayout.NORTH)
        add(expandedContent, BorderLayout.CENTER)
        header.bindDetailsToggle {
            expanded = !expanded
            updateExpanded()
            revalidate()
            repaint()
        }
        updateExpanded()
    }

    fun render(title: String, summary: List<Pair<String, String>>, details: List<Pair<String, String>>) {
        fill(facts, summary, columns = 2)
        fill(more, details, columns = 1)
        toolTipText = usageTooltip(title, (summary + details).map { "${it.first}: ${it.second}" })
        updateExpanded()
    }

    private fun updateExpanded() {
        expandedContent.isVisible = expanded
        header.setDetailsExpanded(expanded)
    }

    override fun dispose() { header.bindDetailsToggle(null); usageDetails?.dispose() }

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
