package io.github.larsnoerber.agentsusage.ui.settings

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.UsageSurface
import io.github.larsnoerber.agentsusage.ui.components.secondaryTextColor
import io.github.larsnoerber.agentsusage.ui.components.usageBarColor
import io.github.larsnoerber.agentsusage.ui.components.warningColor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.awt.Insets
import java.text.ParseException
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/** Edits the shared refresh interval without owning provider subscriptions or timers. */
internal class RefreshSettingsPanel(private val onBack: () -> Unit) : JPanel(BorderLayout()) {
    private val settingsNotice = JBLabel(" ")
    private val agents = AgentSelectionPanel()
    private lateinit var intervalSpinner: JSpinner
    private lateinit var presetButtons: List<RefreshChoice>

    init {
        isOpaque = false
        add(createSettings(), BorderLayout.NORTH)
    }

    fun resetFromSettings() {
        agents.reset()
        intervalSpinner.value = AgentsUsageSettings.getInstance().refreshIntervalSeconds
        settingsNotice.text = " "
        settingsNotice.foreground = usageBarColor(100)
    }

    private fun createSettings(): JPanel = UsageSurface().apply {
        addRow(settingsHeader())
        addRow(JBLabel("Visible agents").apply { font = font.deriveFont(Font.BOLD) }, top = 10)
        addRow(agents, top = 4)
        addRow(JBLabel("Overview and status bar · installed providers only").apply {
            foreground = secondaryTextColor()
        }, top = 4)
        addRow(JBLabel("Refresh rate for all agents").apply {
            font = font.deriveFont(Font.BOLD)
        }, top = 14)
        addRow(JBLabel("<html>These settings apply to OpenAI, JetBrains AI, and GitHub Copilot.<br>Usage refreshes automatically in the background.<br>You can also refresh all agents manually at any time.</html>").apply {
            foreground = secondaryTextColor()
        }, top = 4)

        presetButtons = listOf(
            presetButton("30 sec", "Frequent updates", 30),
            presetButton("1 min", "Recommended", 60),
            presetButton("5 min", "Reduce background activity", 300)
        )
        addRow(JPanel(GridLayout(0, 1, 0, 4)).apply {
            isOpaque = false
            presetButtons.forEach(::add)
        }, top = 10)

        val currentSeconds = AgentsUsageSettings.getInstance().refreshIntervalSeconds
        intervalSpinner = JSpinner(SpinnerNumberModel(currentSeconds, 10, 3600, 10)).apply {
            preferredSize = Dimension(78, preferredSize.height)
            addChangeListener { selectPresetIfMatched() }
        }
        addRow(JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            add(JBLabel("Custom"))
            add(intervalSpinner)
            add(JBLabel("sec"))
        }, top = 10)
        addRow(JButton("Apply settings").apply {
            border = JBUI.Borders.empty(7, 10)
            background = JBColor(Color(59, 112, 209), Color(59, 112, 209))
            foreground = Color.WHITE
            addActionListener { saveInterval() }
        }, top = 10)
        addRow(settingsNotice.apply {
            foreground = usageBarColor(100)
        }, top = 6)
        selectPresetIfMatched()
    }

    private fun settingsHeader(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
        isOpaque = false
        add(JButton("‹").apply {
            font = font.deriveFont(16f)
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            margin = Insets(0, 0, 0, 0)
            val buttonSize = JBUI.size(24, 28)
            minimumSize = buttonSize
            preferredSize = buttonSize
            maximumSize = buttonSize
            border = JBUI.Borders.empty(2)
            foreground = secondaryTextColor()
            toolTipText = "Back to usage overview"
            addActionListener { onBack() }
        })
        add(JButton("Agents Usage").apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            margin = Insets(0, 0, 0, 0)
            val buttonSize = JBUI.size(100, 28)
            minimumSize = buttonSize
            preferredSize = buttonSize
            maximumSize = buttonSize
            border = JBUI.Borders.empty(2)
            foreground = secondaryTextColor()
            toolTipText = "Back to usage overview"
            addActionListener { onBack() }
        })
        add(JBLabel("/").apply {
            foreground = secondaryTextColor()
            border = JBUI.Borders.empty(0, 3)
        })
        add(JBLabel("Agent settings").apply { font = font.deriveFont(Font.BOLD, 15f) })
    }

    private fun presetButton(title: String, note: String, seconds: Int): RefreshChoice =
        RefreshChoice(title, note, seconds).apply {
            addActionListener { intervalSpinner.value = seconds }
        }

    private fun selectPresetIfMatched() {
        val value = (intervalSpinner.value as Number).toInt()
        presetButtons.forEach { it.setSelectedStyle(it.seconds == value) }
    }

    private fun saveInterval() {
        try {
            intervalSpinner.commitEdit()
        } catch (_: ParseException) {
            settingsNotice.foreground = warningColor()
            settingsNotice.text = "Enter an interval between 10 and 3600 seconds"
            return
        }
        settingsNotice.foreground = usageBarColor(100)
        val seconds = (intervalSpinner.value as Number).toInt().coerceIn(10, 3600)
        agents.applySelection()
        UsageRefreshCoordinator.changeRefreshInterval(seconds)
        settingsNotice.text = "Settings saved · refresh every $seconds seconds"
    }

}
