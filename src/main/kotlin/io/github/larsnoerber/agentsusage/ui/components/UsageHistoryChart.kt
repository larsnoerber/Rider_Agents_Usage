package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.core.history.UsageHistory
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JPanel

/** Step lines reflect observations; resets and long observation gaps are not connected. */
internal class UsageHistoryChart(private val provider: String, private val percent: Boolean) : JPanel() {
    var metrics: List<UsageDetailMetric> = emptyList()
    var hours: Int = 6
    private val history = UsageHistory.getInstance()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    init {
        isOpaque = false
        preferredSize = JBUI.size(260, 165)
        minimumSize = JBUI.size(120, 100)
        toolTipText = "Usage observations"
        getAccessibleContext().accessibleName = "Measured usage history"
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.font = font.deriveFont((font.size2D - 2).coerceAtLeast(9f))
            val left = JBUI.scale(42); val top = JBUI.scale(8); val bottom = height - JBUI.scale(22)
            val right = width - JBUI.scale(8)
            if (right <= left || bottom <= top) return
            val end = System.currentTimeMillis()
            val start = end - hours * 3_600_000L
            val series = metrics.map { history.points("$provider.${it.id}").filter { point -> point.at >= start } }
            val maximum = if (percent) 100.0 else (series.flatten().maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0) * 1.1
            fun x(at: Long): Int = left + ((at - start).toDouble() / (end - start) * (right - left)).toInt()
            fun y(value: Double): Int = bottom - (value.coerceIn(0.0, maximum) / maximum * (bottom - top)).toInt()
            for (tick in 0..4) {
                val value = maximum * tick / 4
                val yy = y(value)
                g.color = Color(128, 128, 128, 45)
                g.drawLine(left, yy, right, yy)
                g.color = secondaryTextColor()
                val label = if (percent) "${value.toInt()}%" else compactNumber(value)
                g.drawString(label, left - g.fontMetrics.stringWidth(label) - 5, yy + 4)
                val at = start + (end - start) * tick / 4
                val xx = x(at)
                g.color = Color(128, 128, 128, 35)
                g.drawLine(xx, top, xx, bottom)
                g.color = secondaryTextColor()
                val time = timeFormat.format(Instant.ofEpochMilli(at))
                val labelX = (xx - g.fontMetrics.stringWidth(time) / 2).coerceIn(0, (width - g.fontMetrics.stringWidth(time)).coerceAtLeast(0))
                g.drawString(time, labelX, height - 4)
            }
            g.stroke = BasicStroke(JBUI.scale(2).toFloat())
            series.forEachIndexed { index, points ->
                g.color = detailSeriesColor(index)
                points.forEachIndexed { pointIndex, point ->
                    val previous = points.getOrNull(pointIndex - 1)
                    if (previous != null && previous.reset == point.reset && point.at - previous.at <= 15 * 60_000) {
                        g.drawLine(x(previous.at), y(previous.value), x(point.at), y(previous.value))
                        g.drawLine(x(point.at), y(previous.value), x(point.at), y(point.value))
                    } else g.fillOval(x(point.at) - 2, y(point.value) - 2, 4, 4)
                }
            }
            if (series.all { it.isEmpty() }) {
                g.color = secondaryTextColor()
                val text = "History appears after usage is observed"
                g.drawString(text, left + 8, (top + bottom) / 2)
            }
        } finally { g.dispose() }
    }

    override fun getToolTipText(event: MouseEvent): String {
        val end = System.currentTimeMillis()
        val start = end - hours * 3_600_000L
        val fraction = ((event.x - JBUI.scale(42)).toDouble() / (width - JBUI.scale(50)).coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val at = start + ((end - start) * fraction).toLong()
        val lines = metrics.mapNotNull { metric ->
            val point = history.points("$provider.${metric.id}").lastOrNull { it.at <= at && it.at >= start } ?: return@mapNotNull null
            "${metric.title}: ${if (percent) "${point.value.toInt()}% consumed" else "${compactNumber(point.value)} tokens"} · ${timeFormat.format(Instant.ofEpochMilli(point.at))}"
        }
        return usageTooltip("Observed usage", lines.ifEmpty { listOf("No observation at this time") })
    }

    private fun compactNumber(value: Double): String = when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        value >= 1_000 -> "%.1fk".format(value / 1_000)
        else -> "%.0f".format(value)
    }
}
