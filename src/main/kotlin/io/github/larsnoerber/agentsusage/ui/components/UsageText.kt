package io.github.larsnoerber.agentsusage.ui.components

import com.intellij.openapi.util.text.StringUtil
import java.awt.Color

internal data class TooltipUsageBar(
    val label: String,
    val percent: Int,
    val summary: String,
    val color: Color
)

internal fun usageTooltip(
    title: String,
    lines: List<String>,
    bars: List<TooltipUsageBar> = emptyList()
): String = buildString {
    append("<html><b>")
    append(StringUtil.escapeXmlEntities(title))
    append("</b>")
    bars.forEach { bar ->
        val percent = bar.percent.coerceIn(0, 100)
        val filled = ((percent + 5) / 10).coerceIn(0, 10)
        val color = "#%02x%02x%02x".format(bar.color.red, bar.color.green, bar.color.blue)
        append("<br><br><b>")
        append(StringUtil.escapeXmlEntities(bar.label))
        append("</b>  ")
        append(StringUtil.escapeXmlEntities(bar.summary))
        append("<br><font color=\"")
        append(color)
        append("\">")
        append("\u2588".repeat(filled))
        append("</font><font color=\"#888888\">")
        append("\u2591".repeat(10 - filled))
        append("</font>")
    }
    lines.filter(String::isNotBlank).forEach {
        append("<br>")
        append(StringUtil.escapeXmlEntities(it))
    }
    append("</html>")
}

internal fun compactUsageMessage(message: String): String =
    if (message.length > 42) message.take(41) + "…" else message
