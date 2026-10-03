package io.github.larsnoerber.agentsusage.ui

import io.github.larsnoerber.agentsusage.model.CodexUsage
import io.github.larsnoerber.agentsusage.model.formatSubscriptionPlan
import io.github.larsnoerber.agentsusage.service.CodexUsageService
import io.github.larsnoerber.agentsusage.service.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.service.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Container
import java.awt.event.HierarchyEvent
import java.text.ParseException
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.Timer

class AgentsUsageToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = AgentsUsagePanel()
        val content = toolWindow.contentManager.factory.createContent(panel, "Usage", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}

private class AgentsUsagePanel : JPanel(BorderLayout()), Disposable {
    private val layout = object : CardLayout() {
        override fun preferredLayoutSize(parent: Container): Dimension {
            val size = parent.components.firstOrNull { it.isVisible }?.preferredSize ?: Dimension()
            val insets = parent.insets
            return Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom)
        }
    }
    private val pages = JPanel(layout)
    private val overviewPage: JPanel
    private val settingsPage: JPanel
    private val fiveHour = CompactUsageCard("5 hours")
    private val weekly = CompactUsageCard("This week")
    private val jetBrainsAi = if (JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsagePanel() else null
    private val gitHubCopilot = if (GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsagePanel() else null
    private val stateDot = JBLabel("●")
    private val subscription = JBLabel("Subscription: Unknown")
    private val stateText = JBLabel("Loading Codex usage…")
    private val settingsNotice = JBLabel(" ")
    private lateinit var intervalSpinner: JSpinner
    private lateinit var presetButtons: List<RefreshChoice>
    private var usage = CodexUsage()
    private val listener: (CodexUsage) -> Unit = ::update
    private var disposed = false
    private val timer = Timer(1_000) {
        fiveHour.updateCountdown()
        weekly.updateCountdown()
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(12)
        pages.isOpaque = false
        overviewPage = createOverview()
        settingsPage = createSettings()
        pages.add(overviewPage, OVERVIEW)
        pages.add(settingsPage, SETTINGS)
        add(JBScrollPane(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(pages, BorderLayout.NORTH)
        }).apply { border = BorderFactory.createEmptyBorder() }, BorderLayout.CENTER)
        CodexUsageService.getInstance().addListener(listener)
        update(CodexUsageService.getInstance().current)
        addHierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                if (!disposed && isShowing && overviewPage.isVisible) timer.start() else timer.stop()
            }
        }
    }

    private fun createOverview(): JPanel = compactSurface().apply {
        addRow(this, overviewHeader())
        addRow(this, subscription.apply { foreground = secondaryTextColor() }, top = 6)
        addRow(this, JBLabel("Usage overview").apply {
            foreground = secondaryTextColor()
        }, top = 12)
        addRow(this, JPanel(GridLayout(0, 1, 0, 10)).apply {
            isOpaque = false
            add(fiveHour)
            add(weekly)
        }, top = 8)
        addRow(this, JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            stateDot.font = stateDot.font.deriveFont(10f)
            add(stateDot)
            add(stateText)
        }, top = 10)
        jetBrainsAi?.let { addRow(this, it, top = 16) }
        gitHubCopilot?.let { addRow(this, it, top = 16) }
    }

    private fun createSettings(): JPanel = compactSurface().apply {
        addRow(this, settingsHeader())
        addRow(this, JBLabel("Refresh rate for all agents").apply {
            font = font.deriveFont(Font.BOLD)
        }, top = 14)
        addRow(this, JBLabel("<html>These settings apply to OpenAI, JetBrains AI, and GitHub Copilot.<br>Usage refreshes automatically in the background.<br>You can also refresh all agents manually at any time.</html>").apply {
            foreground = secondaryTextColor()
        }, top = 4)

        presetButtons = listOf(
            presetButton("30 sec", "Frequent updates", 30),
            presetButton("1 min", "Recommended", 60),
            presetButton("5 min", "Reduce background activity", 300)
        )
        addRow(this, JPanel(GridLayout(0, 1, 0, 4)).apply {
            isOpaque = false
            presetButtons.forEach(::add)
        }, top = 10)

        val currentSeconds = AgentsUsageSettings.getInstance().refreshIntervalSeconds
        intervalSpinner = JSpinner(SpinnerNumberModel(currentSeconds, 10, 3600, 10)).apply {
            preferredSize = Dimension(78, preferredSize.height)
            addChangeListener { selectPresetIfMatched() }
        }
        addRow(this, JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            add(JBLabel("Custom"))
            add(intervalSpinner)
            add(JBLabel("sec"))
        }, top = 10)
        addRow(this, JButton("Apply settings").apply {
            border = JBUI.Borders.empty(7, 10)
            background = JBColor(Color(59, 112, 209), Color(59, 112, 209))
            foreground = Color.WHITE
            addActionListener { saveInterval() }
        }, top = 10)
        addRow(this, settingsNotice.apply {
            foreground = successColor()
        }, top = 6)
        selectPresetIfMatched()
    }

    private fun compactSurface(): JPanel = JPanel(GridBagLayout()).apply {
        isOpaque = true
        background = JBColor(Color(248, 249, 251), Color(43, 45, 48))
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(Color(220, 223, 229), Color(63, 66, 73))),
            JBUI.Borders.empty(12)
        )
    }

    private fun addRow(surface: JPanel, component: java.awt.Component, top: Int = 0) {
        surface.add(component, GridBagConstraints().apply {
            gridx = 0
            gridy = surface.componentCount
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = Insets(top, 0, 0, 0)
        })
    }

    private fun overviewHeader(): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(JBLabel("OpenAI").apply { font = font.deriveFont(Font.BOLD, 15f) }, BorderLayout.WEST)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 3, 0)).apply {
            isOpaque = false
            add(refreshButton())
            add(toolbarDivider())
            add(settingsButton())
        }, BorderLayout.EAST)
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
            addActionListener { showOverview() }
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
            addActionListener { showOverview() }
        })
        add(JBLabel("/").apply {
            foreground = secondaryTextColor()
            border = JBUI.Borders.empty(0, 3)
        })
        add(JBLabel("Agent settings").apply { font = font.deriveFont(Font.BOLD, 15f) })
    }

    private fun refreshButton(): JButton = JButton("↻  Refresh").apply {
        font = font.deriveFont(12f)
        toolTipText = "Refresh usage for all available providers immediately"
        isFocusPainted = false
        margin = Insets(0, 0, 0, 0)
        val buttonSize = JBUI.size(96, 28)
        minimumSize = buttonSize
        preferredSize = buttonSize
        maximumSize = buttonSize
        background = JBColor(Color(244, 246, 250), Color(54, 57, 64))
        foreground = JBColor(Color(70, 73, 80), Color(222, 225, 230))
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(Color(210, 214, 222), Color(73, 77, 86))),
            JBUI.Borders.empty(3, 7)
        )
        addActionListener {
            CodexUsageService.getInstance().refresh()
            if (jetBrainsAi != null) JetBrainsAiUsageService.getInstance().refresh()
            if (gitHubCopilot != null) GitHubCopilotUsageService.getInstance().refresh()
        }
    }

    private fun toolbarDivider(): JBLabel = JBLabel("│").apply {
        foreground = JBColor(Color(190, 194, 201), Color(92, 96, 106))
        border = JBUI.Borders.empty(0, 2)
    }

    private fun settingsButton(): JButton = JButton("⚙").apply {
        font = font.deriveFont(15f)
        toolTipText = "Settings for all agents"
        isFocusPainted = false
        isContentAreaFilled = false
        isBorderPainted = false
        margin = Insets(0, 0, 0, 0)
        val buttonSize = JBUI.size(28, 28)
        minimumSize = buttonSize
        preferredSize = buttonSize
        maximumSize = buttonSize
        border = JBUI.Borders.empty(2)
        foreground = secondaryTextColor()
        addActionListener { showSettings() }
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
        settingsNotice.foreground = successColor()
        val seconds = (intervalSpinner.value as Number).toInt().coerceIn(10, 3600)
        AgentsUsageSettings.getInstance().applyRefreshInterval(seconds)
        settingsNotice.text = "All agents refresh every $seconds seconds"
        render()
    }

    private fun showSettings() {
        intervalSpinner.value = AgentsUsageSettings.getInstance().refreshIntervalSeconds
        settingsNotice.text = " "
        layout.show(pages, SETTINGS)
        timer.stop()
        pages.revalidate()
        pages.repaint()
    }

    private fun showOverview() {
        layout.show(pages, OVERVIEW)
        if (isShowing && !disposed) timer.start()
        pages.revalidate()
        pages.repaint()
    }

    private fun update(newUsage: CodexUsage) {
        usage = newUsage
        render()
    }

    private fun render() {
        subscription.text = "Subscription: ${formatSubscriptionPlan(usage.plan)}"
        fiveHour.render(usage.fiveHourLeft, usage.fiveHourReset, usage.fiveHourResetsAt)
        weekly.render(usage.weeklyLeft, usage.weeklyReset, usage.weeklyResetsAt)
        val lowest = listOfNotNull(usage.fiveHourLeft, usage.weeklyLeft).minOrNull()
        stateDot.foreground = if (usage.error == null) usageColor(lowest) else warningColor()
        stateText.foreground = if (usage.error == null) secondaryTextColor() else warningColor()
        val interval = AgentsUsageSettings.getInstance().state.refreshSeconds
        stateText.text = usage.error ?: "Synced · Refreshes every $interval sec · Credits ${usage.credits ?: "—"}"
        pages.revalidate()
    }

    override fun dispose() {
        disposed = true
        timer.stop()
        CodexUsageService.getInstance().removeListener(listener)
        jetBrainsAi?.dispose()
        gitHubCopilot?.dispose()
    }

    private companion object {
        const val OVERVIEW = "overview"
        const val SETTINGS = "settings"
    }
}

private class RefreshChoice(
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

private class CompactUsageCard(title: String) : UsageCard(title) {
    private var resetsAt: Long? = null

    fun render(left: Int?, displayedReset: String?, epochSeconds: Long?) {
        remaining.text = left?.let { "$it%" } ?: "—"
        detail.text = displayedReset ?: "Reset time unknown"
        resetsAt = epochSeconds
        updateProgress(left, usageBarColor(left))
        updateCountdown()
    }

    fun updateCountdown() {
        toolTipText = resetsAt?.let { "Reset countdown: ${countdown(it)}" }
    }

    private fun countdown(epochSeconds: Long): String {
        val seconds = (epochSeconds - System.currentTimeMillis() / 1_000).coerceAtLeast(0)
        val hours = seconds / 3_600
        val minutes = (seconds % 3_600) / 60
        return "Resets in %02d:%02d".format(hours, minutes)
    }
}

private fun usageColor(left: Int?): Color = usageBarColor(left)

private val warning = JBColor(Color(182, 118, 0), Color(245, 184, 76))
private val secondaryText = JBColor(Color(105, 108, 115), Color(166, 169, 177))
private fun successColor(): Color = usageBarColor(100)
private fun warningColor(): Color = warning
private fun secondaryTextColor(): Color = secondaryText
